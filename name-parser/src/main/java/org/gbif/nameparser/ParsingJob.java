package org.gbif.nameparser;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.*;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.gbif.nameparser.api.*;
import org.gbif.nameparser.util.RankUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

/**
 * Core parser class of the name parser that tries to take a clean name into its pieces by using regular expressions.
 * It runs the actual regex matching in another thread that stops whenever the configured timeout is reached.
 *
 *
 * Fully parse the supplied name also trying to extract authorships, a conceptual sec reference, remarks or notes
 * on the nomenclatural status. In some cases the authorship parsing proves impossible and this nameparser will
 * return null.
 *
 * For strings which are no scientific names and scientific names that cannot be expressed by the ParsedName class
 * the parser will throw an UnparsableException with a given NameType and the original, unparsed name. This is the
 * case for all virus names and proper hybrid formulas, so make sure you catch and process this exception.
 */
class ParsingJob implements Callable<ParsedName> {
  static Logger LOG = LoggerFactory.getLogger(ParsingJob.class);

  private static final Splitter WHITESPACE_SPLITTER = Splitter.on(" ").trimResults().omitEmptyStrings();
  private static final CharMatcher AUTHORTEAM_DELIMITER = CharMatcher.anyOf(",&");
  private static final Splitter AUTHORTEAM_SPLITTER = Splitter.on(AUTHORTEAM_DELIMITER).trimResults().omitEmptyStrings();
  private static final Splitter AUTHORTEAM_SEMI_SPLITTER = Splitter.on(";").trimResults().omitEmptyStrings();
  private static final Pattern AUTHOR_INITIAL_SWAP = Pattern.compile("^([^,]+) *, *([^,]+)$");
  private static final Pattern NORM_EX_HORT = Pattern.compile("\\b(?:hort(?:usa?)?|cv)[. ]ex ", CASE_INSENSITIVE);
  private static final String SPACE_AUTHOR = "\\p{Lu}\\p{Ll}+ \\p{Lu}+";
  private static final Pattern SPACE_AUTHORTEAM = Pattern.compile("^" + SPACE_AUTHOR +"(?: " + SPACE_AUTHOR + ")*$");

  // name parsing
  static final String NAME_LETTERS = "A-ZÏËÖÜÄÉÈČÁÀÆŒ";
  static final String name_letters = "a-zïëöüäåéèčáàæœ";
  static final String AUTHOR_LETTERS = NAME_LETTERS + "\\p{Lu}"; // upper case unicode letter, not numerical
  // (\W is alphanum)
  static final String author_letters = name_letters + "\\p{Ll}-?"; // lower case unicode letter, not numerical
  // common 3 char or longer name suffices
  private static final String AUTHOR_TOKEN_3 = "fil|filius|hort|jun|junior|sen|senior";
  // common name suffices (ms=manuscript, not yet published)
  private static final String AUTHOR_TOKEN = "(?:(?:\\p{Lu}|-[a-z])[\\p{Lu}\\p{Ll}'-]*" +
      "|" + AUTHOR_TOKEN_3 +
      "|al|f|j|jr|ms|sr|v|v[ao]n|bis|d[aeiou]?|de[nrmls]?|degli|e|l[ae]s?|s|ter|'?t|y" +
    ")\\.?";
  @VisibleForTesting
  static final String AUTHOR = AUTHOR_TOKEN + "(?:[ '-]?" + AUTHOR_TOKEN + ")*";
  private static final String AUTHOR_TEAM = AUTHOR + "(?:[&,;]+" + AUTHOR + ")*";
  static final String AUTHORSHIP =
      // ex authors
      "(?:(" + AUTHOR_TEAM + ") ?\\bex[. ])?" +
      // main authors
      "(" + AUTHOR_TEAM + ")" +
      // 2 well known sanction authors for fungus, see POR-2454
      "(?: *: *(Pers\\.?|Fr\\.?))?";
  static final Pattern AUTHOR_TEAM_PATTERN = Pattern.compile("^" + AUTHOR_TEAM + "$");
  private static final String YEAR = "[12][0-9][0-9][0-9?]";
  private static final String YEAR_LOOSE = YEAR + "[abcdh?]?(?:[/,-][0-9]{1,4})?";

  private static final String NOTHO = "notho";
  static final String RANK_MARKER = ("(?:"+NOTHO+")?(?:(?<!f[ .])sp|" +
        StringUtils.join(RankUtils.RANK_MARKER_MAP_INFRASPECIFIC.keySet(), "|") +
    ")")
    // avoid hort.ex matches
    .replace("|hort|", "|hort(?!\\.ex)|");

  static final String RANK_MARKER_MICROBIAL =
    "(?:bv\\.|ct\\.|f\\.sp\\.|" +
        StringUtils.join(Lists.transform(Lists.newArrayList(RankUtils.INFRASUBSPECIFIC_MICROBIAL_RANKS), new Function<Rank, String>() {
              @Nullable
              @Override
              public String apply(@Nullable Rank r) {
                return r.getMarker().replaceAll("\\.", "\\\\.");
              }
            }
        ), "|") +
    ")";
  
  private static final String UNALLOWED_EPITHETS = "the|and|of|where|from";
  private static final String UNALLOWED_EPITHET_ENDING =
      "bacilliform|coliform|coryneform|cytoform|chemoform|biovar|serovar|genomovar|agamovar|cultivar|genotype|serotype|subtype|ribotype|isolate";
  // allow for cf/aff markers before epithets
  static final String EPI_QUALIFIER = "(?:\\b(aff|cf|nr)[?. ])?";
  static final String EPITHET = "(?:[0-9]+-?|[a-z]-|[doml]'|(?:van|novae) [a-z])?"
            // avoid matching to rank markers
            + "(?!"+RANK_MARKER+"\\b)"
            + "[" + name_letters + "+-]{1,}(?<! d)[" + name_letters + "]"
            // avoid epithets and those ending with the unallowed endings, e.g. serovar and author suffices like filius
            + "(?<!(?:\\b(?:ex|l[ae]|v[ao]n|"+AUTHOR_TOKEN_3+")\\.?|\\b(?:"+UNALLOWED_EPITHETS+")|"+ UNALLOWED_EPITHET_ENDING +"))(?=\\b)";
  static final String MONOMIAL =
    "[" + NAME_LETTERS + "](?:\\.|[" + name_letters + "]+)(?:-[" + NAME_LETTERS + "]?[" + name_letters + "]+)?";
  // a pattern matching typical latin word endings. Helps identify name parts from authors
  private static final Pattern LATIN_ENDINGS;
  static {
      try (BufferedReader br = new BufferedReader(new InputStreamReader(
          ParsingJob.class.getResourceAsStream("/nameparser/latin-endings.txt")
      ))) {
        Set<String> endings = br.lines().collect(Collectors.toSet());
        LATIN_ENDINGS = Pattern.compile("(" + Joiner.on('|').skipNulls().join(endings) + ")$");
      } catch (IOException e) {
        throw new IllegalStateException("Failed to read latin-endings.txt from classpath resources", e);
      }
  }
  private static final String INFRAGENERIC =
    "(?:\\(([" + NAME_LETTERS + "][" + name_letters + "-]+)\\)" +
        "| ((?:"+NOTHO+")?(?:" +
          StringUtils.join(RankUtils.RANK_MARKER_MAP_INFRAGENERIC.keySet(), "|") +
        "))[. ]([" + NAME_LETTERS + "][" + name_letters + "-]+)"
    + ")";

  static final String RANK_MARKER_ALL = "("+NOTHO+")? *(" + StringUtils.join(RankUtils.RANK_MARKER_MAP.keySet(), "|") + ")\\.?";
  private static final Pattern RANK_MARKER_ONLY = Pattern.compile("^" + RANK_MARKER_ALL + "$");

  private static char[] QUOTES = new char[4];

  static {
    QUOTES[0] = '"';
    QUOTES[1] = '\'';
    QUOTES[2] = '"';
    QUOTES[3] = '\'';
  }

  // this is only used to detect whether we have a hybrid formula. If not, all markers are normalised
  public static final String HYBRID_MARKER = "×";
  public static final Pattern HYBRID_FORMULA_PATTERN = Pattern.compile("[. ]" + HYBRID_MARKER + " ");
  public static final String EXTINCT_MARKER = "†";
  private static final Pattern EXTINCT_PATTERN = Pattern.compile(EXTINCT_MARKER + "\\s*");

  @VisibleForTesting
  protected static final Pattern CULTIVAR = Pattern.compile("(?:([. ])cv[. ])?[\"'] ?((?:[" + NAME_LETTERS + "]?[" + name_letters + "]+[- ]?){1,3}) ?[\"']");
  private static final Pattern CULTIVAR_GROUP = Pattern.compile("(?<!^)\\b[\"']?((?:[" + NAME_LETTERS + "][" + name_letters + "]{2,}[- ]?){1,3})[\"']? (Group|Hybrids|Sort|[Gg]rex|gx)\\b");
  
  private static final Pattern BOLD_PLACEHOLDERS = Pattern.compile("^([A-Z][a-z]+)_?([A-Z]{1,5}(_\\d+)?)$");
  private static final Pattern UNPARSABLE_GROUP  = Pattern.compile("^([A-Z][a-z]+)( [a-z]+?)?( species)?[ _-]group$");
  // TODO: replace with more generic manuscript name parsing: https://github.com/gbif/name-parser/issues/8
  private static final Pattern INFRASPEC_UPPER = Pattern.compile("(?<=forma? )([A-Z])\\b");
  private static final Pattern STRAIN = Pattern.compile("([a-z]\\.?) +([A-Z]+[ -]?(?!"+YEAR+")[0-9]+T?)$");
  // this is only used to detect whether we have a virus name
  public static final Pattern IS_VIRUS_PATTERN = Pattern.compile("virus(es)?\\b|" +
      "\\b(" +
          "(bacterio|viro)?phage(in|s)?|" +
          "particles?|" +
          "prion|" +
          "replicon|" +
          "(alpha|beta|circular) ?satellites|" +
          "[a-z]+satellite|" +
          "vector|" +
          "viroid|" +
          "ictv$" +
      ")\\b", CASE_INSENSITIVE);
  // NPV=Nuclear Polyhedrosis Virus
  // GV=Granulovirus
  public static final Pattern IS_VIRUS_PATTERN_CASE_SENSITIVE = Pattern.compile("\\b(:?[MS]?NP|G)V\\b");
  private static final Pattern IS_VIRUS_PATTERN_POSTFAIL = Pattern.compile("(\\b(vector)\\b)", CASE_INSENSITIVE);
  // RNA or other gene markers
  public static final Pattern IS_GENE = Pattern.compile("(RNA|DNA)[0-9]*(?:\\b|_)");
  // detect known OTU name formats
  // SH  = SH000003.07FU
  // BIN = BOLD:AAA0003
  private static final Pattern OTU_PATTERN = Pattern.compile("(BOLD:[0-9A-Z]{7}$|SH[0-9]{6,8}\\.[0-9]{2}FU)", CASE_INSENSITIVE);
  // spots a Candidatus bacterial name
  private static final String CANDIDATUS = "(Candidatus\\s|Ca\\.)";
  private static final Pattern IS_CANDIDATUS_PATTERN = Pattern.compile(CANDIDATUS);
  private static final Pattern IS_CANDIDATUS_QUOTE_PATTERN = Pattern.compile("\"" + CANDIDATUS + "(.+)\"", CASE_INSENSITIVE);
  @VisibleForTesting
  static final Pattern FAMILY_PREFIX = Pattern.compile("^[A-Z][a-z]*(?:aceae|idae) +(" +
        StringUtils.join(RankUtils.RANK_MARKER_MAP_FAMILY_GROUP.keySet(), "|") +
      ")\\b");
  private static final Pattern SUPRA_RANK_PREFIX = Pattern.compile("^(" + StringUtils.join(
      ImmutableMap.builder()
          .putAll(RankUtils.RANK_MARKER_MAP_SUPRAGENERIC)
          .putAll(RankUtils.RANK_MARKER_MAP_INFRAGENERIC)
          .build().keySet()
      , "|") + ")[\\. ] *");
  private static final Pattern RANK_MARKER_AT_END = Pattern.compile("[ .]" +
      RANK_MARKER_ALL.substring(0,RANK_MARKER_ALL.lastIndexOf(')')) +
      "|" +
      RANK_MARKER_MICROBIAL.substring(3) +
      // allow for larva/adult life stage indicators: http://dev.gbif.org/issues/browse/POR-3000
      "[. ]?(?:Ad|Lv)?\\.?" +
      "$");
  private static final Pattern FILIUS_AT_END = Pattern.compile("[ .]f\\.?$");
  // name normalising
  static final Pattern EXTRACT_SENSU = Pattern.compile("[;, ]?\\b" +
      "(" +
        "(?:(?:excl[. ](?:gen|sp|var)|mut.char|p.p)[. ])?" +
        "\\(?(?:" +
          "ss?[. ](?:(?:ampl|l|s|str)[. ]|(?:ampl|lat|strict)(?:[uo]|issimo)?)" +
          "|(?:(?:ss[. ])?[aA]uctt?|[eE]mend|[fF]ide|[nN]on|[nN]ec|[sS]ec|[sS]ensu|[aA]ccording to)[. ].+" +
        ")\\)?" +
      ")");
  private static final String NOV_RANKS = "((?:[sS]ub)?(?:[fF]am|[gG]en|[sS]s?p(?:ec)?|[vV]ar|[fF](?:orma?)?))";
  private static final Pattern NOV_RANK_MARKER = Pattern.compile("(" + NOV_RANKS + ")");
  static final String MANUSCRIPT_STATUS = "(?:ined|ms).?($|\\s)";
  static final Pattern MANUSCRIPT_STATUS_PATTERN = Pattern.compile(MANUSCRIPT_STATUS);
  static final Pattern EXTRACT_NOMSTATUS = Pattern.compile("[;, ]?"
      + "\\(?"
      + "\\b("
        + "(?:comb|"+NOV_RANKS+")[. ]nov\\b[. ]?(?:ined[. ])?"
        + "|"+MANUSCRIPT_STATUS
        + "|nom(?:en)?[. ]"
          + "(?:utiq(?:ue)?[. ])?"
          + "(?:ambig|alter|alt|correct|cons|dubium|dub|herb|illeg|invalid|inval|negatum|neg|novum|nov|nudum|nud|oblitum|obl|praeoccup|prov|prot|transf|superfl|super|rejic|rej)\\b[. ]?"
          + "(?:prop[. ]|proposed\\b)?"
      + ")"
      + "\\)?");
  private static final Pattern NORM_ANON = Pattern.compile("\\b(anon\\.?)(\\b|\\s|$)");
  private static final Pattern COMMA_AFTER_BASYEAR = Pattern.compile("("+YEAR+")\\s*\\)\\s*,");
  private static final Pattern NORM_APOSTROPHES = Pattern.compile("([\u0060\u00B4\u2018\u2019]+)");
  private static final Pattern NORM_QUOTES = Pattern.compile("([\"'`´]+)");
  private static final Pattern REPL_GENUS_QUOTE = Pattern.compile("^' *(" + MONOMIAL + ") *'");
  private static final Pattern REPL_ENCLOSING_QUOTE = Pattern.compile("^$");//Pattern.compile("^[',\\s]+|[',\\s]+$");
  private static final Pattern NORM_UPPERCASE_WORDS = Pattern.compile("\\b(\\p{Lu})(\\p{Lu}{2,})\\b");
  private static final Pattern NORM_LOWERCASE_BINOMIAL = Pattern.compile("^(" + EPITHET + ") (" + EPITHET + ")");
  private static final Pattern NORM_WHITESPACE = Pattern.compile("(?:\\\\[nr]|\\s)+");
  private static final Pattern REPL_UNDERSCORE = Pattern.compile("_+");
  private static final Pattern NORM_NO_SQUARE_BRACKETS = Pattern.compile("\\[(.*?)\\]");
  private static final Pattern NORM_BRACKETS_OPEN = Pattern.compile("\\s*([{(\\[])\\s*,?\\s*");
  private static final Pattern NORM_BRACKETS_CLOSE = Pattern.compile("\\s*,?\\s*([})\\]])\\s*");
  private static final Pattern NORM_BRACKETS_OPEN_STRONG = Pattern.compile("( ?[{\\[] ?)+");
  private static final Pattern NORM_BRACKETS_CLOSE_STRONG = Pattern.compile("( ?[}\\]] ?)+");
  private static final Pattern NORM_AND = Pattern.compile("\\b *(and|et|und|\\+|,&) *\\b");
  private static final Pattern NORM_SUBGENUS = Pattern.compile("(" + MONOMIAL + ") (" + MONOMIAL + ") (" + EPITHET + ")");
  private static final Pattern NO_Q_MARKS = Pattern.compile("([" + author_letters + "])\\?+");
  private static final Pattern NORM_PUNCTUATIONS = Pattern.compile("\\s*([.,;:&(){}\\[\\]-])\\s*\\1*\\s*");
  private static final Pattern NORM_YEAR = Pattern.compile("[\"'\\[]+\\s*(" + YEAR_LOOSE + ")\\s*[\"'\\]]+");
  private static final Pattern NORM_IMPRINT_YEAR = Pattern.compile("(" + YEAR_LOOSE + ")\\s*" +
      "([(\\[,&]? *(?:not|imprint)? *\"?" + YEAR_LOOSE + "\"?[)\\]]?)");
  // √ó is an utf garbaged version of the hybrid cross found in IPNI. See http://dev.gbif.org/issues/browse/POR-3081
  private static final Pattern NORM_HYBRIDS_GENUS = Pattern.compile("^\\s*(?:[+×xX]|√ó)\\s*([" + NAME_LETTERS + "])");
  private static final Pattern NORM_HYBRIDS_EPITH = Pattern.compile("^\\s*(×?" + MONOMIAL + ")\\s+(?:×|√ó|[xX]\\s)\\s*(" + EPITHET + ")");
  private static final Pattern NORM_HYBRIDS_FORM = Pattern.compile("\\b([×xX]|√ó) ");
  private static final Pattern NORM_TF_GENUS = Pattern.compile("^([" + NAME_LETTERS + "])\\(([" + name_letters + "-]+)\\)\\.? ");
  private static final Pattern REPL_FINAL_PUNCTUATIONS = Pattern.compile("[,;:]+$");
  private static final Pattern REPL_IN_REF = Pattern.compile("[, ]?\\b(?:in|IN|apud) (" + AUTHOR_TEAM + ")");
  private static final Pattern REPL_RANK_PREFIXES = Pattern.compile("^(sub)?(fossil|" +
      StringUtils.join(RankUtils.RANK_MARKER_MAP_SUPRAGENERIC.keySet(), "|") + ")\\.?\\s+", CASE_INSENSITIVE);
  private static final Pattern MANUSCRIPT_NAMES = Pattern.compile("\\b(indet|spp?)[. ](?:nov\\.)?[A-Z0-9][a-zA-Z0-9-]*(?:\\(.+?\\))?");
  private static final Pattern NO_LETTERS = Pattern.compile("^[^a-zA-Z]+$");
  private static final Pattern REMOVE_PLACEHOLDER_AUTHOR = Pattern.compile("\\b"+
      "(?:unknown|unspecified|uncertain|\\?)" +
      "[, ] ?(" + YEAR_LOOSE + ")$", CASE_INSENSITIVE
  );
  private static final Pattern PLACEHOLDER_GENUS = Pattern.compile("^(In|Dummy|Missing|Temp|Unknown|Unplaced|Unspecified) (?=[a-z]+)\\b");
  private static final String PLACEHOLDER_NAME = "(?:allocation|awaiting|deleted?|dummy|incertae sedis|indetermined|mixed|not assigned|not stated|place ?holder|temp|tobedeleted|unaccepted|unallocated|unassigned|uncertain|unclassed|unclassified|uncultured|undescribed|undet(?:ermined)?|unknown|unnamed|unplaced|unspecified)";
  private static final Pattern REMOVE_PLACEHOLDER_INFRAGENERIC = Pattern.compile("\\b\\( ?"+PLACEHOLDER_NAME+" ?\\) ", CASE_INSENSITIVE);
  private static final Pattern PLACEHOLDER = Pattern.compile("^N\\.\\s*N\\.|\\b"+PLACEHOLDER_NAME+"\\b", CASE_INSENSITIVE);
  private static final Pattern DOUBTFUL = Pattern.compile("^[" + AUTHOR_LETTERS + author_letters + HYBRID_MARKER + "\":;&*+\\s,.()\\[\\]/'`´0-9-†]+$");
  private static final Pattern DOUBTFUL_NULL = Pattern.compile("\\bnull\\b", CASE_INSENSITIVE);
  private static final Pattern XML_ENTITY_STRIP = Pattern.compile("&\\s*([a-z]+)\\s*;");
  // matches badly formed amoersands which are important in names / authorships
  private static final Pattern AMPERSAND_ENTITY = Pattern.compile("& *amp +");

  private static final Pattern XML_TAGS = Pattern.compile("< */? *[a-zA-Z] *>");
  private static final Pattern STARTING_EPITHET = Pattern.compile("^\\s*(" + EPITHET + ")\\b");
  private static final Pattern FORM_SPECIALIS = Pattern.compile("\\bf\\. *sp(?:ec)?\\b");
  private static final Pattern SENSU_LATU = Pattern.compile("\\bs\\.l\\.\\b");

  
  private static final Pattern NOM_REFS = Pattern.compile("[,;.]?[\\p{Lu}\\p{Ll}\\s]*\\b(?:Proceedings|Journal|Annals|Bulletin|Systematics|Taxonomy|Series|Memoirs|Mitteilungen|Berichte)\\b.+$");
  // 4(2): 611
  private static final Pattern NOM_REF_VOLUME = Pattern.compile("[,;.]?[\\p{Lu}\\p{Ll}\\s]*\\b\\d+\\s*(//(\\d+//))?:\\s*\\d+\\b.+$");
  // many names still use outdated xxxtype rank marker, e.g. serotype instead of serovar
  private static final Pattern TYPE_TO_VAR;
  static {
    StringBuilder sb = new StringBuilder();
    sb.append("\\b(");
    for (Rank r : RankUtils.INFRASUBSPECIFIC_MICROBIAL_RANKS) {
      if (r.name().endsWith("VAR")) {
        if (sb.length()>4) {
          sb.append("|");
        }
        sb.append(r.name().toLowerCase().substring(0, r.name().length()-3));
      }
    }
    sb.append(")type\\b");
    TYPE_TO_VAR = Pattern.compile(sb.toString());
  }
  private static final Set<String> BLACKLIST_EPITHETS;
  static {
    try (BufferedReader br = new BufferedReader(new InputStreamReader(
        ParsingJob.class.getResourceAsStream("/nameparser/blacklist-epithets.txt")
    ))) {
      Set<String> blacklist = br.lines()
          .map(String::toLowerCase)
          .map(String::trim)
          .filter(x -> !StringUtils.isBlank(x))
          .collect(Collectors.toSet());
      BLACKLIST_EPITHETS = ImmutableSet.copyOf(blacklist);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read epithet blacklist from classpath resources", e);
    }
  }
  
  
  @VisibleForTesting
  static final Pattern POTENTIAL_NAME_PATTERN = Pattern.compile("^×?" + MONOMIAL + "\\b");
  private static final Pattern REMOVE_INTER_RANKS = Pattern.compile("\\b((?:subsp|ssp|var)[ .].+)\\b("+RANK_MARKER+")\\b");
  // allow only short lower case tokens to avoid matching to a real epithet
  private static final String SKIP_AUTHORS = "(?:\\b[ \\p{Ll}'(-]{0,3}\\p{Lu}.*?\\b)??";
  public static final Pattern NAME_PATTERN = Pattern.compile("^" +
             // #1 genus/monomial
             "(×?(?:\\?|" + MONOMIAL + "))" +
             // #2 or #4 subgenus/section with #3 infrageneric rank marker
             "(?:(?<!ceae)" + INFRAGENERIC + ")?" +
             // #5+6 species
             "(?:(?:\\b| )"+EPI_QUALIFIER+"(×?" + EPITHET + ")" +
                "(?:" +
                // any superfluous intermediate bits before terminal epithets, e.g. species authors
                "(?:.*?)" +
                // #7 superfluous subspecies epithet
                "( ×?" + EPITHET + ")?" +
                // #8 infraspecies rank
                "[. ]?(" + RANK_MARKER + ")?" +
                // #9+10 infraspecies epitheton, avoid matching to degli which is part of Italian author names
                "[. ]"+ EPI_QUALIFIER +"(×?\"?(?!(?:degli|de[rn]?)\\b)" + EPITHET + "\"?)" +
                ")?" +
              ")?" +

             "(?: " +
               // #11 microbial rank
               "(" + RANK_MARKER_MICROBIAL + ")[ .]" +
               // #12 microbial infrasubspecific epithet
               "(\\S+)" +
             ")?" +

             // #13 indet rank marker after epithets
             "([. ]" + RANK_MARKER + ")?" +

             // #14 entire authorship incl basionyms and year
             "([., ]?" +
               "(?:\\(" +
                 // #15/16/17 basionym authorship (ex/auth/sanct)
                 "(?:" + AUTHORSHIP + ")?" +
                 // #18 basionym year
                 "[, ]?(" + YEAR_LOOSE + ")?" +
               "\\))?" +

               // #19/20/21 authorship (ex/auth/sanct)
               "(?:" + AUTHORSHIP + ")?" +
               // #22 year with or without brackets
               "(?: ?\\(?,?(" + YEAR_LOOSE + ")\\)?)?" +
             ")" +

             // #23 any remainder
             "(\\b.*?)??$");

  static Matcher interruptableMatcher(Pattern pattern, String text) {
    return pattern.matcher(new InterruptibleCharSequence(text));
  }

  private final Rank rank;
  private final NomCode code;
  private final String scientificName;
  private final ParsedName pn;
  private boolean ignoreAuthorship;

  /**
   * @param scientificName the full scientific name to parse
   * @param rank the rank of the name if it is known externally. Helps identifying infrageneric names vs bracket authors
   */
  ParsingJob(String scientificName, Rank rank, NomCode code) {
    this.scientificName = Preconditions.checkNotNull(scientificName);
    this.rank = Preconditions.checkNotNull(rank);
    this.code = code;
    pn =  new ParsedName();
    pn.setRank(rank);
  }

  private ParsedName unparsable(NameType type) throws UnparsableNameException {
    throw new UnparsableNameException(type, scientificName);
  }

  /**
   * Fully parse the supplied name also trying to extract authorships, a conceptual sec reference, remarks or notes
   * on the nomenclatural status. In some cases the authorship parsing proves impossible and this nameparser will
   * return null.
   *
   * For strings which are no scientific names and scientific names that cannot be expressed by the ParsedName class
   * the parser will throw an UnparsableException with a given NameType and the original, unparsed name. This is the
   * case for all virus names and proper hybrid formulas, so make sure you catch and process this exception.
   *
   * @throws UnparsableNameException
   */
  @Override
  public ParsedName call() throws UnparsableNameException {
    long start = 0;
    if (LOG.isDebugEnabled()) {
      start = System.currentTimeMillis();
    }

    // clean name, removing seriously wrong things
    String name = preClean(scientificName);

    // before further cleaning/parsing try if we have known OTU formats, i.e. BIN or SH numbers
    // test for special cases they do not need any further parsing
    if (!specialCases(name)) {
      // do the main incremental parsing
      parse(name);
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("Parsing time: {} for {}", (System.currentTimeMillis() - start), pn);
    }

    // build canonical name
    return pn;
  }

  private String stripNomRef(Matcher m) {
    pn.setUnparsed(m.group());
    pn.addWarning(Warnings.NOMENCLATURAL_REFERENCE);
    return m.replaceFirst("");
  }
  
  /**
   * Parse very special cases and return true if the parsing succeeded and has thereby ended completely!
   */
  private boolean specialCases(String name) throws UnparsableNameException {
    // BOLD/UNITE OTU names
    Matcher m = OTU_PATTERN.matcher(name);
    if (m.find()) {
      pn.setUninomial(m.group(1).toUpperCase());
      pn.setType(NameType.OTU);
      pn.setRank(Rank.UNRANKED);
      pn.setState(ParsedName.State.COMPLETE);
      return true;
    }
    
    // BOLD style placeholders
    // https://github.com/gbif/name-parser/issues/45
    m = BOLD_PLACEHOLDERS.matcher(name);
    if (m.find()) {
      pn.setUninomial(m.group(1));
      pn.setStrain(m.group(2));
      pn.setState(ParsedName.State.COMPLETE);
      checkBlacklist(); // check blacklist
      pn.setType(NameType.PLACEHOLDER);
      determineCode();
      determineRank();
      return true;
    }
    // unparsable BOLD style placeholder: Iteaphila-group
    m = UNPARSABLE_GROUP.matcher(name);
    if (m.find()) {
      // can we parse out a species group?
      if (m.group(2) != null) {
        pn.setGenus(m.group(1));
        pn.setSpecificEpithet(m.group(2).trim());
        pn.setState(ParsedName.State.COMPLETE);
        pn.setRank(Rank.SPECIES_AGGREGATE);
        pn.setType(NameType.SCIENTIFIC);
        checkBlacklist(); // check blacklist
        determineCode();
        return true;
        
      } else {
        unparsable(NameType.PLACEHOLDER);
      }
    }
    return false;
  }
  
  private void parse(String name) throws UnparsableNameException {
    // remove extinct markers
    name = EXTINCT_PATTERN.matcher(name).replaceFirst("");

    // before any cleaning test for properly quoted candidate names
    Matcher m = IS_CANDIDATUS_QUOTE_PATTERN.matcher(scientificName);
    if (m.find()) {
      pn.setCandidatus(true);
      name = m.replaceFirst(m.group(2));
    }
  
    // preparse nomenclatural references
    ParsedName.State preParseState = null;
    m = NOM_REFS.matcher(name);
    if (m.find()) {
      name = stripNomRef(m);
      preParseState = ParsedName.State.PARTIAL;
    } else {
      m = NOM_REF_VOLUME.matcher(name);
      if (m.find()) {
        name = stripNomRef(m);
        preParseState = ParsedName.State.PARTIAL;
      }
    }

    // normalize bacterial rank markers
    name = TYPE_TO_VAR.matcher(name).replaceAll("$1var");

    // TODO: parse manuscript names properly
    m = INFRASPEC_UPPER.matcher(name);
    String infraspecEpithet = null;
    if (m.find()) {
      // we will replace is later again with infraspecific we memorized here!!!
      name = m.replaceFirst("vulgaris");
      infraspecEpithet = m.group(1);
      pn.setType(NameType.INFORMAL);
    }

    // remove placeholders from infragenerics and authors and set type
    m = REMOVE_PLACEHOLDER_AUTHOR.matcher(name);
    if (m.find()) {
      name = m.replaceFirst(" $1");
      pn.setType(NameType.PLACEHOLDER);
    }
    m = REMOVE_PLACEHOLDER_INFRAGENERIC.matcher(name);
    if (m.find()) {
      name = m.replaceFirst("");
      pn.setType(NameType.PLACEHOLDER);
    }

    // resolve parsable names with a placeholder genus only
    m = PLACEHOLDER_GENUS.matcher(name);
    if (m.find()) {
      name = m.replaceFirst("? ");
      pn.setType(NameType.PLACEHOLDER);
    }

    // detect further unparsable names
    if (PLACEHOLDER.matcher(name).find()) {
      unparsable(NameType.PLACEHOLDER);
    }

    if (IS_VIRUS_PATTERN.matcher(name).find() || IS_VIRUS_PATTERN_CASE_SENSITIVE.matcher(name).find()) {
      unparsable(NameType.VIRUS);
    }

    // detect RNA/DNA gene/strain names and flag as informal
    if (IS_GENE.matcher(name).find()) {
      pn.setType(NameType.INFORMAL);
    }

    // normalise name
    name = normalize(name);

    if (Strings.isNullOrEmpty(name)) {
      unparsable(NameType.NO_NAME);
    }

    // remove family in front of subfamily ranks
    m = FAMILY_PREFIX.matcher(name);
    if (m.find()) {
      name = m.replaceFirst("$1");
    }

    // check for supraspecific ranks at the beginning of the name
    m = SUPRA_RANK_PREFIX.matcher(name);
    if (m.find()) {
      pn.setRank(RankUtils.RANK_MARKER_MAP.get(m.group(1).replace(".", "")));
      name = m.replaceFirst("");
    }

    // parse cultivar names first BEFORE we strongly normalize
    // this will potentially remove quotes needed to find cultivar names
    // this will potentially remove quotes needed to find cultivar group names
    m = CULTIVAR_GROUP.matcher(name);
    if (m.find()) {
      pn.setCultivarEpithet(m.group(1));
      name = m.replaceFirst(" ");
      String cgroup = m.group(2);
      if (cgroup.equalsIgnoreCase("grex") || cgroup.equalsIgnoreCase("gx")) {
        pn.setRank(Rank.GREX);
      } else {
        pn.setRank(Rank.CULTIVAR_GROUP);
      }
    }
    m = CULTIVAR.matcher(name);
    if (m.find()) {
      pn.setCultivarEpithet(m.group(2));
      name = m.replaceFirst("$1");
      pn.setRank(Rank.CULTIVAR);
    }

    // name without any latin char letter at all?
    if (NO_LETTERS.matcher(name).find()) {
      unparsable(NameType.NO_NAME);
    }

    if (HYBRID_FORMULA_PATTERN.matcher(name).find()) {
      unparsable(NameType.HYBRID_FORMULA);
    }

    m = IS_CANDIDATUS_PATTERN.matcher(name);
    if (m.find()) {
      pn.setCandidatus(true);
      name = m.replaceFirst("");
    }

    // extract nom.illeg. and other nomen status notes
    // includes manuscript notes, e.g. ined.
    m = EXTRACT_NOMSTATUS.matcher(name);
    if (m.find()) {
      StringBuffer sb = new StringBuffer();
      do {
        String note = StringUtils.trimToNull(m.group(1));
        if (note != null) {
          pn.addNomenclaturalNote(note);
          m.appendReplacement(sb, "");
          // if there was a rank given in the nom status populate the rank marker field
          Matcher rm = NOV_RANK_MARKER.matcher(note);
          if (rm.find()) {
            setRank(rm.group(1), true);
          }
          // was this a manuscript note?
          Matcher man = MANUSCRIPT_STATUS_PATTERN.matcher(note);
          if (man.find()) {
            pn.setManuscript(true);
          }
        }
      } while (m.find());
      m.appendTail(sb);
      name = sb.toString();
    }

    // manuscript (unpublished) names  without a full scientific name, e.g. Verticordia sp.1
    // http://splink.cria.org.br/documentos/appendix_j.pdf
    m = MANUSCRIPT_NAMES.matcher(name);
    String epithet = null;
    if (m.find()) {
      pn.setType(NameType.INFORMAL);
      epithet = m.group(0);
      setRank(m.group(1).replace("indet", "sp"));
      name = m.replaceFirst("");
      pn.setManuscript(true);
    }

    // parse out species/strain names with numbers found in Genebank/EBI names, e.g. Advenella kashmirensis W13003
    m = STRAIN.matcher(name);
    if (m.find()) {
      name = m.replaceFirst(m.group(1));
      pn.setType(NameType.INFORMAL);
      pn.setStrain(m.group(2));
      LOG.debug("Strain: {}", m.group(2));
    }

    // extract sec reference
    m = EXTRACT_SENSU.matcher(name);
    if (m.find()) {
      pn.setTaxonomicNote(lowerCaseFirstChar(normNote(m.group(1))));
      name = m.replaceFirst("");
    }
    // check for indets
    m = RANK_MARKER_AT_END.matcher(name);
    // f. is a marker for forms, but more often also found in authorships as "filius" - son of.
    // so ignore those
    if (m.find() && !FILIUS_AT_END.matcher(name).find()) {
      // use as rank unless we already have a cultivar
      ignoreAuthorship = true;
      if (pn.getCultivarEpithet() == null) {
        setRank(m.group(2));
      }
      name = m.replaceAll("");
    }

    // replace bibliographic in references
    m = REPL_IN_REF.matcher(name);
    if (m.find()) {
      pn.addNomenclaturalNote(normNote(m.group(0)));
      name = m.replaceFirst("");
    }

    // remove superflous epithets with rank markers
    m = REMOVE_INTER_RANKS.matcher(name);
    if (m.find()) {
      pn.addWarning("Intermediate classification removed: " + m.group(1));
      name = m.replaceFirst("$2");
    }

    // remember current rank for later reuse
    final Rank preparsingRank = pn.getRank();

    String nameStrongly = normalizeStrong(name);

    if (Strings.isNullOrEmpty(nameStrongly)) {
      // we might have parsed out remarks already which we treat as a placeholder
      if (pn.hasName()) {
        // stop here!
        pn.setState(ParsedName.State.COMPLETE);
        pn.setType(NameType.PLACEHOLDER);
        return;
      } else {
        unparsable(NameType.NO_NAME);
      }
    }

    // try regular parsing
    boolean parsed = parseNormalisedName(nameStrongly);
    if (!parsed) {
      // try to spot a virus name once we know its not a scientific name
      m = IS_VIRUS_PATTERN_POSTFAIL.matcher(nameStrongly);
      if (m.find()) {
        unparsable(NameType.VIRUS);
      }

      // cant parse it, fail!
      // Does it appear to be a genuine name starting with a monomial?
      if (POTENTIAL_NAME_PATTERN.matcher(name).find()) {
        unparsable(NameType.SCIENTIFIC);
      } else {
        unparsable(NameType.NO_NAME);
      }
    }

    // any manuscript epithet (species or infraspecies?)
    if (epithet != null) {
      if (pn.getSpecificEpithet() == null) {
        pn.setSpecificEpithet(epithet);
      } else {
        pn.setInfraspecificEpithet(epithet);
      }
    }
    // did we parse a infraspecic manuscript name?
    if (infraspecEpithet != null) {
      pn.setInfraspecificEpithet(infraspecEpithet);
    }
    // if we established a rank or state during preparsing make sure we use this not the parsed one
    if (preparsingRank != null && this.rank != preparsingRank) {
      pn.setRank(preparsingRank);
    }
    if (preParseState != null) {
      pn.setState(preParseState);
    }
    
    // determine name type
    determineNameType(name);

    // raise warnings for blacklisted epithets
    checkBlacklist();
    
    // flag names that match doubtful patterns
    applyDoubtfulFlag(scientificName);

    // determine rank if not yet assigned
    determineRank();

    // determine code if not yet assigned
    determineCode();
  }
  
  private static String lowerCaseFirstChar(String x) {
    if (x != null && x.length() > 0) {
      return x.substring(0, 1).toLowerCase() + x.substring(1);
    }
    return x;
  }
  
  private static String normNote(String note) {
    if (note.startsWith("(") && note.endsWith(")")) {
      note = note.substring(1, note.length()-1);
    }
    return StringUtils.trimToNull(
        note
        // punctuation followed by a space, dots are special because of author initials
        .replaceAll("([,;)])(?!= )", "$1 ")
        // opening brackets with space
        .replaceAll("(?<! )([(])", " $1")
        // dots before years and after lower case words should have a space
        .replaceAll("(?:\\.(?=" + YEAR + ")|(?<=\\b[a-z]{2,})\\.(?! ))", ". ")
        // ands with space
        .replaceAll("&", " & ")
    );
  }
  /**
   * Carefully normalizes a scientific name trying to maintain the original as close as possible.
   * In particular the string is normalized by:
   * - adding commas in front of years
   * - trims whitespace around hyphens
   * - pads whitespace around &
   * - adds whitespace after dots following a genus abbreviation, rank marker or author name
   * - keeps whitespace before opening and after closing brackets
   * - removes whitespace inside brackets
   * - removes whitespace before commas
   * - normalized hybrid marker to be the ascii multiplication sign
   * - removes whitespace between hybrid marker and following name part in case it is NOT a hybrid formula
   * - trims the string and replaces multi whitespace with single space
   * - capitalizes all only uppercase words (authors are often found in upper case only)
   *
   * @param name To normalize
   *
   * @return The normalized name
   */
  @VisibleForTesting
  String normalize(String name) {
    if (name == null) {
      return null;
    }

    // translate some very similar characters that sometimes get misused instead of real letters
    name = StringUtils.replaceChars(name, "¡", "i");

    // normalise usage of rank marker with 2 dots, i.e. forma specialis and sensu latu
    Matcher m = FORM_SPECIALIS.matcher(name);
    if (m.find()) {
      name = m.replaceAll("fsp");
    }
    m = SENSU_LATU.matcher(name);
    if (m.find()) {
      name = m.replaceAll("sl");
    }

    // cleanup years
    name = NORM_YEAR.matcher(name).replaceAll("$1");

    // remove imprint years. See ICZN §22A.2.3 http://www.iczn.org/iczn/index.jsp?nfv=&article=22
    m = NORM_IMPRINT_YEAR.matcher(name);
    if (m.find()) {
      LOG.debug("Imprint year {} removed", m.group(2));
      name = m.replaceAll("$1");
    }

    // replace underscores
    name = REPL_UNDERSCORE.matcher(name).replaceAll(" ");

    // normalise punctuations removing any adjacent space
    m = NORM_PUNCTUATIONS.matcher(name);
    if (m.find()) {
      name = m.replaceAll("$1");
    }
    // normalise different usages of ampersand, and, et &amp; to always use &
    name = NORM_AND.matcher(name).replaceAll("&");

    // remove commans after basionym brackets
    m = COMMA_AFTER_BASYEAR.matcher(name);
    if (m.find()) {
      name = m.replaceFirst("$1)");
    }

    // no whitespace before and after brackets, keeping the bracket style
    m = NORM_BRACKETS_OPEN.matcher(name);
    if (m.find()) {
      name = m.replaceAll("$1");
    }
    m = NORM_BRACKETS_CLOSE.matcher(name);
    if (m.find()) {
      name = m.replaceAll("$1");
    }
    // normalize hybrid markers
    m = NORM_HYBRIDS_GENUS.matcher(name);
    if (m.find()) {
      name = m.replaceFirst("×$1");
    }
    m = NORM_HYBRIDS_EPITH.matcher(name);
    if (m.find()) {
      name = m.replaceFirst("$1 ×$2");
    }
    m = NORM_HYBRIDS_FORM.matcher(name);
    if (m.find()) {
      name = m.replaceAll(" × ");
    }
    // capitalize Anonumous author
    m = NORM_ANON.matcher(name);
    if (m.find()) {
      name = m.replaceFirst("Anon.");
    }
    // capitalize all entire upper case words
    m = NORM_UPPERCASE_WORDS.matcher(name);
    if (m.find()) {
      StringBuffer sb = new StringBuffer();
      m.appendReplacement(sb, m.group(1) + m.group(2).toLowerCase());
      while (m.find()) {
        m.appendReplacement(sb, m.group(1) + m.group(2).toLowerCase());
      }
      m.appendTail(sb);
      name = sb.toString();
    }

    // Capitalize potential owercase genus in binomials
    m = NORM_LOWERCASE_BINOMIAL.matcher(name);
    if (m.find()) {
      name = m.replaceFirst(StringUtils.capitalize(m.group(1)) + " " + m.group(2));
    }

    // finally whitespace and trimming
    name = NORM_WHITESPACE.matcher(name).replaceAll(" ");
    return StringUtils.trimToEmpty(name);
  }

  /**
   * Does the same as a normalize and additionally removes all ( ) and "und" etc
   * Checks if a name starts with a blacklisted name part like "Undetermined" or "Uncertain" and only returns the
   * blacklisted word in that case
   * so its easy to catch names with blacklisted name parts.
   *
   * @param name To normalize
   *
   * @return The normalized name
   */
  @VisibleForTesting
  String normalizeStrong(String name) {
    if (name == null) {
      return null;
    }
    // normalize ex hort. (for gardeners, often used as ex names) spelled in lower case
    name = NORM_EX_HORT.matcher(name).replaceAll("hort.ex ");

    // normalize all quotes to single "
    name = NORM_QUOTES.matcher(name).replaceAll("'");
    // remove quotes from genus
    name = REPL_GENUS_QUOTE.matcher(name).replaceFirst("$1 ");
    // remove enclosing quotes
    Matcher m = REPL_ENCLOSING_QUOTE.matcher(name);
    if (m.find()) {
      name = m.replaceAll("");
      pn.addWarning(Warnings.REPL_ENCLOSING_QUOTE);
    }

    // no question marks after letters (after years they should remain)
    m = NO_Q_MARKS.matcher(name);
    if (m.find()) {
      name = m.replaceAll("$1");
      pn.setDoubtful(true);
      pn.addWarning(Warnings.QUESTION_MARKS_REMOVED);
    }

    // remove prefixes
    name = REPL_RANK_PREFIXES.matcher(name).replaceAll("");

    // remove brackets inside the genus, the kind taxon finder produces
    m = NORM_TF_GENUS.matcher(name);
    if (m.find()) {
      name = m.replaceAll("$1$2 ");
    }

    // TODO: replace square brackets, keeping content (or better remove all within?)
    //name = NORM_NO_SQUARE_BRACKETS.matcher(name).replaceAll(" $1 ");

    // replace different kind of brackets with ()
    name = NORM_BRACKETS_OPEN_STRONG.matcher(name).replaceAll("(");
    name = NORM_BRACKETS_CLOSE_STRONG.matcher(name).replaceAll(")");

    // add ? genus when name starts with an epithet
    m = STARTING_EPITHET.matcher(name);
    if (m.find()) {
      name = m.replaceFirst("? $1");
      pn.addWarning(Warnings.MISSING_GENUS);
    }

    // add parenthesis around subgenus if missing
    m = NORM_SUBGENUS.matcher(name);
    if (m.find()) {
      // make sure epithet is not a rank mismatch
      if (parseRank(m.group(3)) == null){
        name = m.replaceAll("$1($2)$3");
      }
    }

    // finally NORMALIZE PUNCTUATION AND WHITESPACE again
    name = NORM_PUNCTUATIONS.matcher(name).replaceAll("$1");
    name = NORM_WHITESPACE.matcher(name).replaceAll(" ");
    name = REPL_FINAL_PUNCTUATIONS.matcher(name).replaceAll("");
    return StringUtils.trimToEmpty(name);
  }

  /**
   * basic careful cleaning, trying to preserve all parsable name parts
   */
  @VisibleForTesting
  String preClean(String name) {
    // remove bad whitespace in html entities
    Matcher m = XML_ENTITY_STRIP.matcher(name);
    if (m.find()) {
      name = m.replaceAll("&$1;");
    }
    // unescape html entities
    int length = name.length();
    name = StringEscapeUtils.unescapeHtml4(name);
    if (length > name.length()) {
      pn.addWarning(Warnings.HTML_ENTITIES);
    }
    // finally remove still existing bad ampersands missing the closing ;
    m = AMPERSAND_ENTITY.matcher(name);
    if (m.find()) {
      name = m.replaceAll("&");
      pn.addWarning(Warnings.HTML_ENTITIES);
    }

    // replace xml tags
    m = XML_TAGS.matcher(name);
    if (m.find()) {
      name = m.replaceAll("");
      pn.addWarning(Warnings.XML_TAGS);
    }

    // trim
    name = name.trim();
    // remove quotes in beginning and matching ones at the end
    for (char c : QUOTES) {
      int idx = 0;
      while (idx < name.length() && (c == name.charAt(idx) || Character.isWhitespace(name.charAt(idx)))) {
        idx++;
      }
      if (idx > 0) {
        // check if we also find this char at the end
        int end = 0;
        while (c == name.charAt(name.length() - 1 - end) && (name.length() - idx - end) > 0) {
          end++;
        }
        name = name.substring(idx, name.length() - end);
      }
    }
    name = NORM_WHITESPACE.matcher(name).replaceAll(" ");
    // replace various single quote apostrophes with always '
    name = NORM_APOSTROPHES.matcher(name).replaceAll("'");

    return StringUtils.trimToEmpty(name);
  }

  private void setTypeIfNull(ParsedName pn, NameType type) {
    if (pn.getType() == null) {
      pn.setType(type);
    }
  }

  /**
   * Identifies a name type, defaulting to SCIENTIFIC_NAME so that type is never null
   */
  private void determineNameType(String normedName) {
    // all rules below do not apply to unparsable names
    if (pn.getType() == null || pn.getType().isParsable()) {

      // if we only match a monomial in the 3rd pass its suspicious
      if (pn.getUninomial() != null && Character.isLowerCase(normedName.charAt(0))) {
        pn.addWarning(Warnings.LC_MONOMIAL);
        pn.setDoubtful(true);
        setTypeIfNull(pn, NameType.INFORMAL);

      } else if (pn.getRank().notOtherOrUnranked()) {
        if (pn.isIndetermined()) {
          pn.setType(NameType.INFORMAL);
          pn.addWarning(Warnings.INDETERMINED);

        } else if (pn.getRank().isSupraspecific() && (pn.getSpecificEpithet() != null || pn.getInfraspecificEpithet() != null)) {
          pn.addWarning(Warnings.RANK_MISMATCH);
          pn.setDoubtful(true);
          pn.setType(NameType.INFORMAL);

        } else if (!pn.getRank().isSpeciesOrBelow() && pn.isBinomial()) {
          pn.addWarning(Warnings.HIGHER_RANK_BINOMIAL);
          pn.setDoubtful(true);
        }
      }

      if (pn.getType() == null) {
        // an abbreviated name?
        if (pn.isAbbreviated() || pn.isIncomplete()) {
          pn.setType(NameType.INFORMAL);

        } else if ("?".equals(pn.getUninomial()) || "?".equals(pn.getGenus()) || "?".equals(pn.getSpecificEpithet())) {
          // a placeholder epithet only
          pn.setType(NameType.PLACEHOLDER);

        } else {
          // anything else looks fine!
          pn.setType(NameType.SCIENTIFIC);
        }
      }
    }
  }
  
  private void checkBlacklist() {
    // check epithet blacklist
    for (String epi : pn.listEpithets()) {
      if (BLACKLIST_EPITHETS.contains(epi)) {
        pn.addWarning(Warnings.BLACKLISTED_EPITHET);
        pn.setDoubtful(true);
      }
    }
    // also look at genus
    if (pn.getGenus() != null && BLACKLIST_EPITHETS.contains(pn.getGenus().toLowerCase())) {
      pn.addWarning(Warnings.BLACKLISTED_EPITHET);
      pn.setDoubtful(true);
    }
  }
  
  private void applyDoubtfulFlag(String scientificName) {
    // all rules below do not apply to unparsable names
    Matcher m = DOUBTFUL.matcher(scientificName);
    if (!m.find()) {
      pn.setDoubtful(true);
      pn.addWarning(Warnings.UNUSUAL_CHARACTERS);

    } else if (pn.getType().isParsable()){
      m = DOUBTFUL_NULL.matcher(scientificName);
      if (m.find()) {
        pn.setDoubtful(true);
        pn.addWarning(Warnings.NULL_EPITHET);
      }
    }
  }
  
  private void determineRank() {
    if (pn.getRank().otherOrUnranked()) {
      pn.setRank(RankUtils.inferRank(pn, code));
    }
  }
  
  private void determineCode() {
    if (pn.getCode() == null) {
      // does the rank tell us sth?
      if (pn.getRank().isRestrictedToCode() != null) {
        pn.setCode(pn.getRank().isRestrictedToCode());

      } else if (pn.getCultivarEpithet() != null) {
        pn.setCode(NomCode.CULTIVARS);

      } else if (pn.getSanctioningAuthor() != null) {
        // sanctioning is only for Fungi
        pn.setCode(NomCode.BOTANICAL);

      } else if (pn.getType() == NameType.VIRUS) {
        pn.setCode(NomCode.VIRUS);

      } else if (pn.isCandidatus()) {
        pn.setCode(NomCode.BACTERIAL);

      } else if (!pn.isManuscript() && (pn.getBasionymAuthorship().getYear() != null || pn.getCombinationAuthorship().getYear() != null)) {
        // if years are given its a zoological name
        pn.setCode(NomCode.ZOOLOGICAL);

      } else if (!pn.isManuscript() && !pn.getBasionymAuthorship().isEmpty()) {
        if (pn.getCombinationAuthorship().isEmpty()) {
          // if only the basionym authorship is given its likely a zoological name!
          pn.setCode(NomCode.ZOOLOGICAL);

        } else {
          // if both the basionym and combination authorship is given its a botanical name!
          pn.setCode(NomCode.BOTANICAL);
        }
      } else if (pn.getNomenclaturalNotes() != null && pn.getNomenclaturalNotes().contains("illeg")) {
        pn.setCode(NomCode.BOTANICAL);
      
      } else {
        // otherwise use code given
        pn.setCode(code);
      }
    }
  }

  /**
   * Tries to parse a name string with the full regular expression.
   * In very few, extreme cases names with very long authorships might cause the regex to never finish or take hours
   * we run this parsing in a separate thread that can be stopped if it runs too long.
   * @param name
   * @return  true if the name could be parsed, false in case of failure
   */
  private boolean parseNormalisedName(String name) {
    LOG.debug("Parse normed name string: {}", name);
    Matcher matcher = interruptableMatcher(NAME_PATTERN, name);
    if (matcher.find()) {
      if (StringUtils.isBlank(matcher.group(23))) {
        pn.setState(ParsedName.State.COMPLETE);
      } else {
        LOG.debug("Partial match with unparsed remains \"{}\" for: {}", matcher.group(23), name);
        pn.setState(ParsedName.State.PARTIAL);
        pn.addUnparsed(matcher.group(23).trim());
      }
      if (LOG.isDebugEnabled()) {
        logMatcher(matcher);
      }
      // the match can be the genus part of an infrageneric, bi- or trinomial, the uninomial or even the infrageneric epithet!
      setUninomialOrGenus(matcher, pn);
      boolean bracketSubrankFound = false;
      if (matcher.group(2) != null) {
        bracketSubrankFound = true;
        pn.setInfragenericEpithet(StringUtils.trimToNull(matcher.group(2)));
      } else if (matcher.group(4) != null) {
        setRank(matcher.group(3));
        pn.setInfragenericEpithet(StringUtils.trimToNull(matcher.group(4)));
      }
      setEpithetQualifier(NamePart.SPECIFIC, matcher.group(5));
      pn.setSpecificEpithet(StringUtils.trimToNull(matcher.group(6)));
      if (matcher.group(7) != null && matcher.group(7).length() > 1 && !matcher.group(7).contains("null")) {
        // 4 parted name, so its below subspecies
        pn.setRank(Rank.INFRASUBSPECIFIC_NAME);
      }
      setRank(matcher.group(8));
      setEpithetQualifier(NamePart.SPECIFIC, matcher.group(9));
      pn.setInfraspecificEpithet(StringUtils.trimToNull(matcher.group(10)));

      // microbial ranks
      if (matcher.group(11) != null) {
        setRank(matcher.group(11));
        pn.setInfraspecificEpithet(matcher.group(12));
      }

      // indet rank markers
      if (matcher.group(13) != null) {
        setRank(matcher.group(13));
        ignoreAuthorship = true;
      }

      // make sure (infra)specific epithet is not a rank marker!
      lookForIrregularRankMarker();

      if (pn.isIndetermined()) {
        ignoreAuthorship = true;
      }

      // entire authorship, not stored in ParsedName
      if (!ignoreAuthorship && matcher.group(14) != null) {
        // #19/20/21/22 authorship (ex/auth/sanct/year)
        pn.setCombinationAuthorship(parseAuthorship(matcher.group(19), matcher.group(20), matcher.group(22)));
        // sanctioning author
        if (matcher.group(21) != null) {
          pn.setSanctioningAuthor(matcher.group(21));
        }
        // #15/16/17/18 basionym authorship (ex/auth/sanct/year)
        pn.setBasionymAuthorship(parseAuthorship(matcher.group(15), matcher.group(16), matcher.group(18)));
        if (bracketSubrankFound && infragenericIsAuthor(pn)) {
          // rather an author than a infrageneric rank. Swap in case of monomials
          pn.setBasionymAuthorship(parseAuthorship(null, pn.getInfragenericEpithet(), null));
          pn.setInfragenericEpithet(null);
          // check if we need to move genus to uninomial
          if (pn.getGenus() != null && pn.getSpecificEpithet() == null && pn.getInfraspecificEpithet() == null) {
            pn.setUninomial(pn.getGenus());
            pn.setGenus(null);
          }
          LOG.debug("swapped subrank with bracket author: {}", pn.getBasionymAuthorship());
        }
      }

      return true;
    }

    return false;
  }

  private void setEpithetQualifier(NamePart part, String qualifier) {
    if (qualifier != null) {
      pn.setType(NameType.INFORMAL);
      pn.setEpithetQualifier(part, qualifier);
    }
  }
  
  private static String cleanYear(String matchedYear) {
    if (matchedYear != null && matchedYear.length() > 2) {
      return matchedYear.trim();
    }
    return null;
  }

  private void setRank(String rankMarker) {
    setRank(rankMarker, false);
  }

  /**
   * Sets the parsed names rank based on a found rank marker
   * Potentially also sets the notho field in case the rank marker indicates a hybrid
   * and the code to botanical in case of subsp. found
   * if the rankMarker cannot be interpreted or is null nothing will be done.
   * @param rankMarker
   */
  private void setRank(String rankMarker, boolean force) {
    Rank rank = parseRank(rankMarker);
    if (rank != null && rank.notOtherOrUnranked()) {
      if (force) {
        pn.setRank(rank);
      } else {
        setRankIfNotContradicting(rank);
      }
      if (rankMarker.toLowerCase().startsWith("subsp")) {
        pn.setCode(NomCode.BOTANICAL);
      }
      if (rankMarker.startsWith(NOTHO)) {
        if (rank.isInfraspecific()) {
          pn.setNotho(NamePart.INFRASPECIFIC);
        } else if (rank == Rank.SPECIES) {
          pn.setNotho(NamePart.SPECIFIC);
        } else if (rank.isInfrageneric()) {
          pn.setNotho(NamePart.INFRAGENERIC);
        } else if (rank == Rank.GENUS) {
          pn.setNotho(NamePart.GENERIC);
        }
      }
    }
  }

  /**
   * Sets the rank if the current rank of the parsed name is not contradicting
   * to the given one. Mostly this is the case to define a Unranked rank.
   */
  private void setRankIfNotContradicting(Rank rank) {
    if (pn.getRank().isUncomparable()) {
      switch (pn.getRank()) {
        case INFRAGENERIC_NAME:
          if (!rank.isInfragenericStrictly()) {
            break;
          }
        case INFRASPECIFIC_NAME:
          if (!rank.isInfraspecific()) {
            break;
          }
        case INFRASUBSPECIFIC_NAME:
          if (!rank.isInfrasubspecific()) {
            break;
          }
        default:
          pn.setRank(rank);
      }
    }
  }

  private static Rank parseRank(String rankMarker) {
    return RankUtils.inferRank(StringUtils.trimToNull(rankMarker));
  }

  private static boolean infragenericIsAuthor(ParsedName pn) {
      return pn.getBasionymAuthorship().isEmpty()
          && pn.getSpecificEpithet() == null
          && pn.getInfraspecificEpithet() == null
          && !pn.getRank().isInfragenericStrictly()
          && !LATIN_ENDINGS.matcher(pn.getInfragenericEpithet()).find();
  }

  /**
   * The first Capitalized word can be stored in 3 different places in ParsedName.
   * Figure out where to best keep it:
   *  a) as the genus part of an infrageneric, bi- or trinomial
   *  b) the uninomial for names of rank genus or higher
   *  c) the infrageneric epithet in case its a standalone infrageneric name (which is hard to detect)
   */
  private void setUninomialOrGenus(Matcher matcher, ParsedName pn) {
    // the match can be the genus part of a bi/trinomial or a uninomial
    String monomial = StringUtils.trimToNull(matcher.group(1));
    if (matcher.group(2) != null
        || matcher.group(4) != null
        || matcher.group(6) != null
        || matcher.group(10) != null
        || (pn.getRank().isSpeciesOrBelow()) ) { //  && pn.getRank().isRestrictedToCode() != NomCode.CULTIVARS
      pn.setGenus(monomial);

    } else if (pn.getRank().isInfragenericStrictly()){
      pn.setInfragenericEpithet(monomial);

    } else {
      pn.setUninomial(monomial);
    }
  }

  /**
   * if no rank marker is set, inspect epitheta for wrongly placed rank markers and modify parsed name accordingly.
   * This is sometimes the case for informal names like: Coccyzus americanus ssp.
   */
  private void lookForIrregularRankMarker() {
    if (pn.getRank().otherOrUnranked()) {
      if (pn.getInfraspecificEpithet() != null) {
        Matcher m = RANK_MARKER_ONLY.matcher(pn.getInfraspecificEpithet());
        if (m.find()) {
          // we found a rank marker, make it one
          setRank(pn.getInfraspecificEpithet());
          pn.setInfraspecificEpithet(null);
        }
      }
      if (pn.getSpecificEpithet() != null) {
        Matcher m = RANK_MARKER_ONLY.matcher(pn.getSpecificEpithet());
        if (m.find()) {
          // we found a rank marker, make it one
          setRank(pn.getSpecificEpithet());
          pn.setSpecificEpithet(null);
        }
      }
    } else if(pn.getRank() == Rank.SPECIES && pn.getInfraspecificEpithet() != null) {
      // sometimes sp. is wrongly used as a subspecies rankmarker
      pn.setRank(Rank.SUBSPECIES);
      pn.addWarning(Warnings.SUBSPECIES_ASSIGNED);
    }
  }

  @VisibleForTesting
  static Authorship parseAuthorship(String ex, String authors, String year) {
    Authorship a = new Authorship();
    if (authors != null) {
      a.setAuthors(splitTeam(authors));
    }
    if (ex != null) {
      a.setExAuthors(splitTeam(ex));
    }
    a.setYear(cleanYear(year));
    return a;
  }

  /**
   * Splits an author team by either ; or ,
   */
  private static List<String> splitTeam(String team) {
    // treat semicolon differently. Single author name can contain a comma now!
    if (team.contains(";")) {
      List<String> authors = Lists.newArrayList();
      for (String a : AUTHORTEAM_SEMI_SPLITTER.split(team)) {
        Matcher m = AUTHOR_INITIAL_SWAP.matcher(a);
        if (m.find()) {
          authors.add(normAuthor(m.group(2) + " " + m.group(1), true));
        } else {
          authors.add(normAuthor(a, false));
        }
      }
      return authors;

    } else if(AUTHORTEAM_DELIMITER.matchesAnyOf(team)) {
      return sanitizeAuthors(AUTHORTEAM_SPLITTER.splitToList(team));
  
    } else if (SPACE_AUTHORTEAM.matcher(team).find()){
      // we sometimes see space delimited authorteams with the initials consistently at the end of a single author:
      // Balsamo M Fregni E Tongiorgi MA
      return sanitizeAuthors(WHITESPACE_SPLITTER.splitToList(team));
    
    } else {
      return Lists.newArrayList(normAuthor(team, true));
    }
  }
  
  /**
   * test if we have initials as authors following directly clear surnames
   * See https://github.com/gbif/name-parser/issues/28
   */
  private static List<String> sanitizeAuthors(List<String> tokens) {
    final List<String> authors = new ArrayList<>();
    final Iterator<String> iter = tokens.iterator();
    while (iter.hasNext()) {
      String author = iter.next();
      if (iter.hasNext() && author.length()>3 && !author.endsWith(".")) {
        String next = iter.next();
        if (next.length()<3) {
          // consider an initial and merge with last author
          authors.add(normInitials(next) + author);
        } else {
          authors.add(author);
          authors.add(next);
        }
      } else {
        authors.add(author);
      }
    }
    return authors;
  }
  
  private static String normInitials(String initials) {
    if (initials.endsWith(".")) {
      return initials;
    }
    // treat each character as an initial
    StringBuilder sb = new StringBuilder();
    for (char in : initials.toCharArray()) {
      sb.append(in);
      sb.append('.');
    }
    return sb.toString();
  }
    
    /**
     * Author strings are normalized by removing any whitespace following a dot.
     * See IPNI author standard form recommendations:
     * http://www.ipni.org/standard_forms_author.html
     */
  private static String normAuthor(String authors, boolean normPunctuation) {
    if (normPunctuation) {
      authors = NORM_PUNCTUATIONS.matcher(authors).replaceAll("$1");
    }
    return StringUtils.trimToNull(authors);
  }

  static void logMatcher(Matcher matcher) {
    int i = -1;
    while (i < matcher.groupCount()) {
      i++;
      LOG.debug("  {}: >{}<", i, matcher.group(i));
    }
  }

}
