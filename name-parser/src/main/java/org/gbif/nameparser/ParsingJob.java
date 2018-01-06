package org.gbif.nameparser;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.*;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.gbif.nameparser.api.*;
import org.gbif.nameparser.util.RankUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Set;
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

  private static final Pattern ET_PATTERN = Pattern.compile(" et ", Pattern.CASE_INSENSITIVE);
  private static final Splitter AUTHORTEAM_SPLITTER = Splitter.on(CharMatcher.anyOf(",&")).trimResults().omitEmptyStrings();
  private static final Splitter AUTHORTEAM_SEMI_SPLITTER = Splitter.on(";").trimResults().omitEmptyStrings();
  private static final Pattern AUTHOR_INITIAL_SWAP = Pattern.compile("^([^,]+) *, *([^,]+)$");
  private static final Pattern NORM_PUNCT = Pattern.compile("\\. +");

  // name parsing
  static final String NAME_LETTERS = "A-ZÏËÖÜÄÉÈČÁÀÆŒ";
  static final String name_letters = "a-zïëöüäåéèčáàæœ";
  static final String AUTHOR_LETTERS = NAME_LETTERS + "\\p{Lu}"; // upper case unicode letter, not numerical
  // (\W is alphanum)
  static final String author_letters = name_letters + "\\p{Ll}-?"; // lower case unicode letter, not numerical
  // (\W is alphanum)
  private static final String AUTHOR_PREFIXES =
    "(?:v\\. " +
        "|[vV][ao]n(?:[ -](?:den|der|dem) )? ?" +
        "|De" +
        "|[dD](?:e|el|es|i|a)(?:[' ]l[ae])?[' ]" +
        "|[dDN]'" +
        "|Mac|Mc|Le|St\\.? ?" +
        "|Ou|O'" +
        "|'t " +
    ")?";
  static final String AUTHOR_CAP = "[" + AUTHOR_LETTERS + "]+[" + author_letters + "']";
  private static final String AUTHOR_TOKEN_DOT  = AUTHOR_CAP + "*\\.?";
  private static final String AUTHOR_TOKEN_LONG = AUTHOR_CAP + "{2,}";
  private static final String AUTHOR = "(?:" +
      "(?:" +
        // optional author initials
        "(?:[" + AUTHOR_LETTERS + "]{1,3}(?:[" + author_letters + "]{0,2})\\.?[ -]?){0,3}" +
        // or up to 2 full first names
        "|" + AUTHOR_TOKEN_LONG + "(?: "+AUTHOR_TOKEN_LONG+")?" +
      " )?" +
      // optional common prefixes
      AUTHOR_PREFIXES +
      // regular author name
      AUTHOR_TOKEN_DOT +
      // potential double names, e.g. Solms-Laub.
      // space will be added to dots preceding a capital letter like in Müll.Arg. -> Müll. Arg.
      // otherwise the AUTHOR_TEAM regex will become 10 times slower!!!
      "(?:[- '](?:d[eau][- ])?" + AUTHOR_TOKEN_DOT + ")?" +
      // common name suffices (ms=manuscript, not yet published)
      "(?: ?(?:f|fil|filius|j|jr|jun|junior|sr|sen|senior|ms)\\.?)?" +
      ")";
  private static final String AUTHOR_TEAM = AUTHOR +
      "(?:(?: ?(?:&| et |,|;) ?)+" + AUTHOR + ")*" +
      "(?:(?: ?& ?| et )al\\.?)?";
  static final String AUTHORSHIP =
      // ex authors
      "(?:(" +
      AUTHOR_TEAM +
      ") ex\\.? )?" +
      // main authors
      "(" + AUTHOR_TEAM + ")" +
      // 2 well known sanction authors for fungus, see POR-2454
      "(?: *: *(Pers\\.?|Fr\\.?))?" +
      // superfluous in authors
      "(?: in " +
      AUTHOR_TEAM +
      ")?";
  static final Pattern AUTHORSHIP_PATTERN = Pattern.compile("^" + AUTHORSHIP + "$");
  static final String YEAR = "[12][0-9][0-9][0-9?]";
  static final String YEAR_LOOSE = YEAR + "[abcdh?]?(?:[/-][0-9]{1,4})?";
  // protected static final String YEAR_RANGE = YEAR+"(?: ?-? ?"+YEAR+")?";

  private static final String NOTHO = "notho";
  private static final String RANK_MARKER_SPECIES =
    "(?:"+NOTHO+")?(?:(?<!f[ .] ?)sp|" + StringUtils.join(RankUtils.RANK_MARKER_MAP_INFRASPECIFIC.keySet(), "|") + ")\\.?";

  private static final Function<Rank,String> REMOVE_RANK_MARKER = new Function<Rank, String>() {
    @Override
    public String apply(Rank rank) {
      return rank.getMarker().replaceAll("\\.", "\\\\.");
    }
  };

  static final String RANK_MARKER_MICROBIAL =
    "(?:bv\\.|ct\\.|f\\. ?sp\\.|"
    + StringUtils.join(Lists.transform(Lists.newArrayList(RankUtils.INFRASUBSPECIFIC_MICROBIAL_RANKS), REMOVE_RANK_MARKER
    ), "|") + ")";

  private static final String EPHITHET_PREFIXES = "van|novae";
  private static final String GENETIC_EPHITHETS = "bacilliform|coliform|coryneform|cytoform|chemoform|biovar|serovar|genomovar|agamovar|cultivar|genotype|serotype|subtype|ribotype|isolate";
  static final String EPHITHET = "(?:[0-9]+-?|[doml]')?"
            + "(?:(?:" + EPHITHET_PREFIXES + ") [a-z])?"
            + "[" + name_letters + "+-]{1,}(?<! d)[" + name_letters + "]"
            // avoid epithets ending with the unallowed endings, e.g. serovar
            + "(?<!(?:\\bex|"+GENETIC_EPHITHETS+"))(?=\\b)";
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
    "(?:" + "\\( ?([" + NAME_LETTERS + "][" + name_letters + "-]+) ?\\)" + "|" + "(" + StringUtils
      .join(RankUtils.RANK_MARKER_MAP_INFRAGENERIC.keySet(), "|") + ")\\.? ?([" + NAME_LETTERS + "][" + name_letters + "-]+)"
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
  public static final Pattern HYBRID_FORMULA_PATTERN = Pattern.compile(" " + HYBRID_MARKER + " ");
  public static final String EXTINCT_MARKER = "†";
  private static final Pattern EXTINCT_PATTERN = Pattern.compile(EXTINCT_MARKER + "\\s*");

  @VisibleForTesting
  protected static final Pattern CULTIVAR = Pattern.compile("(?: cv\\.? ?)?[\"'] ?((?:[" + NAME_LETTERS + "]?[" + name_letters + "]+[- ]?){1,3}) ?[\"']");
  private static final Pattern CULTIVAR_GROUP = Pattern.compile("(?<!^)\\b[\"']?((?:[" + NAME_LETTERS + "][" + name_letters + "]{2,}[- ]?){1,3})[\"']? (Group|Hybrids|Sort|[Gg]rex|gx)\\b");

  // TODO: replace with more generic manuscript name parsing: https://github.com/gbif/name-parser/issues/8
  private static final Pattern INFRASPEC_UPPER = Pattern.compile("(?<=forma? )([A-Z])\\b");
  private static final Pattern STRAIN = Pattern.compile("([a-z]\\.?) +([A-Z]+[ -]?(?!"+YEAR+")[0-9]+T?)$");
  // this is only used to detect whether we have a virus name
  public static final Pattern IS_VIRUS_PATTERN = Pattern.compile("virus(es)?\\b|\\b(viroid|(bacterio|viro)?phage(in|s)?|(alpha|beta) ?satellites?|particles?|ictv$)\\b", CASE_INSENSITIVE);
  // NPV=Nuclear Polyhedrosis Virus
  // GV=Granulovirus
  public static final Pattern IS_VIRUS_PATTERN_CASE_SENSITIVE = Pattern.compile("\\b(:?[MS]?NP|G)V\\b");
  private static final Pattern IS_VIRUS_PATTERN_POSTFAIL = Pattern.compile("(\\b(vector)\\b)", CASE_INSENSITIVE);
  // RNA or other gene markers
  public static final Pattern IS_GENE = Pattern.compile("(RNA|DNA)[0-9]*(?:\\b|_)");
  // detect known OTU name formats
  // SH  = SH000003.07FU
  // BIN = BOLD:AAA0003
  private static final Pattern OTU_PATTERN = Pattern.compile("(BOLD:[0-9A-Z]{7}$|SH[0-9]{6}\\.[0-9]{2}FU)", CASE_INSENSITIVE);
  // spots a Candidatus bacterial name
  private static final String CANDIDATUS = "(Candidatus\\s|Ca\\.)\\s*";
  private static final Pattern IS_CANDIDATUS_PATTERN = Pattern.compile(CANDIDATUS);
  private static final Pattern IS_CANDIDATUS_QUOTE_PATTERN = Pattern.compile("\"" + CANDIDATUS + "(.+)\"", CASE_INSENSITIVE);
  private static final Pattern SUPRA_RANK_PREFIX = Pattern.compile("^(" + StringUtils.join(
      ImmutableMap.builder()
          .putAll(RankUtils.RANK_MARKER_MAP_SUPRAGENERIC)
          .putAll(RankUtils.RANK_MARKER_MAP_INFRAGENERIC)
          .build().keySet()
      , "|") + ")[\\. ] *");
  private static final Pattern RANK_MARKER_AT_END = Pattern.compile(" " +
      RANK_MARKER_ALL.substring(0,RANK_MARKER_ALL.lastIndexOf(')')) +
      "|" +
      RANK_MARKER_MICROBIAL.substring(3) + "\\.?" +
      // allow for larva/adult life stage indicators: http://dev.gbif.org/issues/browse/POR-3000
      " ?(?:Ad|Lv)?\\.?" +
      "$");
  // name normalising
  private static final String SENSU =
      "(s\\.(?:l\\.|str\\.)|sensu\\s+(?:latu|strictu?)|(sec|sensu|auct|non)((\\.|\\s)(.*))?)";
  private static final Pattern EXTRACT_SENSU = Pattern.compile(",?\\s+(" + SENSU + "$|\\(" + SENSU + "\\))");
  private static final String NOV_RANKS = "fam|gen|sp|ssp|var|forma";
  private static final Pattern NOV_RANK_MARKER = Pattern.compile("(" + NOV_RANKS + ")");
  protected static final Pattern EXTRACT_NOMSTATUS = Pattern.compile("(?:, ?| )"
      + "\\(?"
      + "("
      + "(?:comb|"+NOV_RANKS+")?[\\. ] ?nov[\\. $](?: ?ined\\.?)?"
      + "|ined\\."
      + "|nom(?:\\s+|\\.\\s*|en\\s+)"
      + "(?:utiq(?:ue\\s+|\\.\\s*))?"
      + "(?:ambig|alter|alt|correct|cons|dubium|dub|herb|illeg|invalid|inval|negatum|neg|novum|nov|nudum|nud|oblitum|obl|praeoccup|prov|prot|transf|superfl|super|rejic|rej)\\.?"
      + "(?:\\s+(?:prop|proposed)\\.?)?"
      + ")"
      + "\\)?");
  private static final Pattern EXTRACT_REMARKS = Pattern.compile("\\s+(anon\\.?)(\\s.+)?$");
  private static final Pattern EXTRACT_YEAR = Pattern.compile("(" + YEAR_LOOSE + "\\s*\\)?)");

  private static final Pattern COMMA_BEFORE_YEAR = Pattern.compile("(,+|[^0-9\\(\\[\"])\\s*(\\d{3})");
  private static final Pattern COMMA_AFTER_BASYEAR = Pattern.compile("("+YEAR+")\\s*\\)\\s*,");
  private static final Pattern REPLACE_QUOTES = Pattern.compile("(^\\s*[\"',]+)|([\"',]+\\s*$)");

  private static final Pattern NORM_APOSTROPHES = Pattern.compile("([\u0060\u00B4\u2018\u2019]+)");
  private static final Pattern NORM_QUOTES = Pattern.compile("([\"'`´]+)");
  private static final Pattern NORM_UPPERCASE_WORDS = Pattern.compile("\\b(\\p{Lu})(\\p{Lu}{2,})\\b");
  private static final Pattern NORM_WHITESPACE = Pattern.compile("(?:\\\\[nr]|\\s)+");
  private static final Pattern NORM_NO_SQUARE_BRACKETS = Pattern.compile("\\[(.*?)\\]");
  private static final Pattern NORM_BRACKETS_OPEN = Pattern.compile("([{(\\[])\\s*,?");
  private static final Pattern NORM_BRACKETS_CLOSE = Pattern.compile(",?\\s*([})\\]])");
  private static final Pattern NORM_BRACKETS_OPEN_STRONG = Pattern.compile("( ?[{(\\[] ?)+");
  private static final Pattern NORM_BRACKETS_CLOSE_STRONG = Pattern.compile("( ?[})\\]] ?)+");
  private static final Pattern NORM_AND = Pattern.compile(" (and|et|und) ");
  private static final Pattern NORM_ET_AL = Pattern.compile("(?:& )+al\\.?");
  private static final Pattern NORM_AMPERSAND_WS = Pattern.compile("&");
  private static final Pattern NORM_HYPHENS = Pattern.compile("\\s*-\\s*");
  private static final Pattern NORM_SUBGENUS = Pattern.compile("(" + MONOMIAL + ") (" + MONOMIAL + ") ([" + name_letters + "+-]{5,})");
  private static final Pattern NO_Q_MARKS = Pattern.compile("([" + author_letters + "])\\?+");
  private static final Pattern NORM_COMMAS = Pattern.compile("\\s*,+");
  // TODO: this next regex gets real slow with long list of authors - needs fixing, avoid lookbehind? !!!
  private static final Pattern NORM_ORIG_AUTH  = Pattern.compile(" (" + AUTHORSHIP + ") ?\\( ?(" + YEAR_LOOSE + ") ?\\)");
  private static final Pattern NORM_ORIG_AUTH2 = Pattern.compile("\\((" + AUTHORSHIP + ")\\) ?,? ?(" + YEAR_LOOSE + ")");
  private static final Pattern NORM_IMPRINT_YEAR = Pattern.compile("(" + YEAR_LOOSE + ")\\s*(?:\\(\"?[\\s0-9-_,?]+\"?\\)|\\[\"?[0-9 -,]+\"?\\]|\"[0-9 -,]+\")");
  // √ó is an utf garbaged version of the hybrid cross found in IPNI. See http://dev.gbif.org/issues/browse/POR-3081
  private static final Pattern NORM_HYBRIDS_GENUS = Pattern.compile("^\\s*(?:[+×xX]|√ó)\\s*([" + NAME_LETTERS + "])");
  private static final Pattern NORM_HYBRIDS_EPITH = Pattern.compile("^\\s*(×?" + MONOMIAL + ")\\s+(?:×|√ó|[xX]\\s)\\s*(" + EPHITHET + ")");
  private static final Pattern NORM_HYBRIDS_FORM = Pattern.compile(" ([×xX]|√ó) ");
  private static final Pattern NORM_INDET = Pattern.compile("((^| )(undet|indet|aff|cf)[#!?\\.]?)+(?![a-z])");
  private static final Pattern NORM_DOTS = Pattern.compile("\\.(?![ ,\\)])");
  private static final Pattern NORM_TF_GENUS = Pattern.compile("^([" + NAME_LETTERS + "])\\(([" + name_letters + "-]+)\\)\\.? ");
  private static final Pattern NORM_IN_COMMA = Pattern.compile(", in ", CASE_INSENSITIVE);
  private static final Pattern NORM_PREFIXES = Pattern.compile("^(sub)?(fossil|" +
      StringUtils.join(RankUtils.RANK_MARKER_MAP_SUPRAGENERIC.keySet(), "|") + ")\\.?\\s+", CASE_INSENSITIVE);
  private static final Pattern NORM_SUFFIXES = Pattern.compile("[,;:]? (sp|anon|spp|hort|ms|&|[a-zA-Z][0-9])?\\.? *$", CASE_INSENSITIVE);
  // removed not|indetermin[a-z]+
  private static final Pattern NO_LETTERS = Pattern.compile("^[^a-zA-Z]+$");
  private static final String PLACEHOLDER_AUTHOR = "(?:unknown|unspecified|uncertain|\\?)";
  private static final Pattern REMOVE_PLACEHOLDER_AUTHOR = Pattern.compile("\\b"+PLACEHOLDER_AUTHOR+"[, ] ?(" + YEAR_LOOSE + ")$", CASE_INSENSITIVE);
  private static final Pattern PLACEHOLDER_GENUS = Pattern.compile("^(Missing|Dummy|Temp|Unknown|Unplaced|Unspecified) (?=[a-z]+)\\b");
  private static final String PLACEHOLDER_NAME = "(?:allocation|awaiting|dummy|incertae sedis|mixed|not assigned|temp|unaccepted|unallocated|unassigned|uncertain|unclassified|uncultured|undetermined|unknown|unnamed|unplaced|unspecified)";
  private static final Pattern REMOVE_PLACEHOLDER_INFRAGENERIC = Pattern.compile("\\b\\( ?"+PLACEHOLDER_NAME+" ?\\) ", CASE_INSENSITIVE);
  private static final Pattern PLACEHOLDER = Pattern.compile("\\b"+PLACEHOLDER_NAME+"\\b", CASE_INSENSITIVE);
  private static final Pattern DOUBTFUL = Pattern.compile("^[" + AUTHOR_LETTERS + author_letters + HYBRID_MARKER + "\":;&*+\\s,.()/'`´0-9-†]+$");
  private static final Pattern DOUBTFUL2 = Pattern.compile("\\bnull\\b");
  private static final Pattern BAD_NAME_SUFFICES = Pattern.compile(" (author|unknown|unassigned|not_stated)$", CASE_INSENSITIVE);
  private static final Pattern XML_ENTITY_STRIP = Pattern.compile("&\\s*([a-z]+)\\s*;");
  // matches badly formed amoersands which are important in names / authorships
  private static final Pattern AMPERSAND_ENTITY = Pattern.compile("& *amp +");

  private static final Pattern XML_TAGS = Pattern.compile("< */? *[a-zA-Z] *>");
  private static final Pattern FIRST_WORD = Pattern.compile("^([×xX]\\s+)?([×x][A-Z])?([a-zA-Z])([a-zA-Z]+) ");
  private static final String WEIRD_CHARS = "[§$%/#+!;:_|\"=*]";
  private static final Pattern NORM_WEIRD_CHARS = Pattern.compile(WEIRD_CHARS);
  private static final Pattern FORM_SPECIALIS = Pattern.compile("\\bf\\.sp(?:ec)?\\b");
  private static final Pattern SENSU_LATU = Pattern.compile("\\bs\\.l\\.\\b");

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

  public static final Pattern NAME_PATTERN = Pattern.compile("^" +
             // #1 genus/monomial
             "(×?(?:\\?|" + MONOMIAL + "))" +
             // #2 or #4 subgenus/section with #3 infrageneric rank marker
             "(?:(?<!ceae) " + INFRAGENERIC + ")?" +
             // #5 species
             "(?: (×?" + EPHITHET + "))?" +

             "(?:" +
               // #6 superfluent intermediate (subspecies) epithet in quadrinomials
               "( " + EPHITHET + ")??" +
               "(?:" +
                 // strip out intermediate, irrelevant authors
                 "(?: .+)??" +
                 // #7 infraspecies rank
                 "( " + RANK_MARKER_SPECIES + ")" +
               ")?" +
               // #8 infraspecies epitheton
               "(?: (×?\"?" + EPHITHET + "\"?))" +
             ")?" +

             "(?: " +
               // #9 microbial rank
               "(" + RANK_MARKER_MICROBIAL + ")[ .]" +
               // #10 microbial infrasubspecific epithet
               "(\\S+)" +
             ")?" +

             // #11 entire authorship incl basionyms and year
             "(,?" +
               "(?: ?\\(" +
                 // #12/13/14 basionym authorship (ex/auth/sanct)
                 "(?:" + AUTHORSHIP + ")?" +
                 // #15 basionym year
                 ",?( ?" + YEAR_LOOSE + ")?" +
               "\\))?" +

               // #16/17/18 authorship (ex/auth/sanct)
               "(?: " + AUTHORSHIP + ")?" +
               // #19 year with or without brackets
               "(?: ?\\(?,? ?(" + YEAR_LOOSE + ")\\)?)?" +
             ")" +
             "$");

  static ParsedName unparsable(NameType type, String name) throws UnparsableNameException {
    throw new UnparsableNameException(type, name);
  }

  private final String scientificName;
  private final Rank rank;
  private final ParsedName pn;

  /**
   * @param scientificName the full scientific name to parse
   * @param rank the rank of the name if it is known externally. Helps identifying infrageneric names vs bracket authors
   */
  ParsingJob(String scientificName, Rank rank) {
    this.scientificName = Preconditions.checkNotNull(scientificName);
    this.rank = Preconditions.checkNotNull(rank);
    pn =  new ParsedName();
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
    Matcher m = OTU_PATTERN.matcher(name);
    if (m.find()) {
      pn.setUninomial(m.group(1));
      pn.setType(NameType.OTU);
      pn.setRank(rank == null || rank.otherOrUnranked() ? Rank.SPECIES : rank);
      pn.setState(ParsedName.State.NAME_ONLY);

    } else {
      // do the main incremental parsing
      parse(name);
    }

    LOG.debug("Parsing time: {}", (System.currentTimeMillis() - start));
    // build canonical name
    return pn;
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

    // normalize bacterial rank markers
    name = TYPE_TO_VAR.matcher(name).replaceAll("$1var");

    // TODO: parse manuscript names properly
    m = INFRASPEC_UPPER.matcher(name);
    String infraspecEpithet = null;
    if (m.find()) {
      // we will replace the infraspecific one later!
      name = m.replaceFirst("vulgaris");
      infraspecEpithet = m.group(1);
      pn.setType(NameType.INFORMAL);
    }

    // parse out species/strain names with numbers found in Genebank/EBI names, e.g. Advenella kashmirensis W13003
    m = STRAIN.matcher(name);
    if (m.find()) {
      name = m.replaceFirst(m.group(1));
      pn.setType(NameType.INFORMAL);
      pn.setStrain(m.group(2));
      LOG.debug("Strain: {}", m.group(2));
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
      unparsable(NameType.PLACEHOLDER, scientificName);
    }

    if (IS_VIRUS_PATTERN.matcher(name).find() || IS_VIRUS_PATTERN_CASE_SENSITIVE.matcher(name).find()) {
      unparsable(NameType.VIRUS, scientificName);
    }

    // detect RNA/DNA gene/strain names and flag as informal
    if (IS_GENE.matcher(name).find()) {
      pn.setType(NameType.INFORMAL);
    }

    // normalise name
    name = normalize(name);
    if (Strings.isNullOrEmpty(name)) {
      unparsable(NameType.NO_NAME, null);
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
      pn.setCultivarEpithet(m.group(1));
      name = m.replaceFirst(" ");
      pn.setRank(Rank.CULTIVAR);
    }

    // name without any latin char letter at all?
    if (NO_LETTERS.matcher(name).find()) {
      unparsable(NameType.NO_NAME, scientificName);
    }

    if (HYBRID_FORMULA_PATTERN.matcher(name).find()) {
      unparsable(NameType.HYBRID_FORMULA, scientificName);
    }

    m = IS_CANDIDATUS_PATTERN.matcher(name);
    if (m.find()) {
      pn.setCandidatus(true);
      name = m.replaceFirst("");
    }

    // extract nom.illeg. and other nomen status notes
    m = EXTRACT_NOMSTATUS.matcher(name);
    if (m.find()) {
      pn.setNomenclaturalNotes(StringUtils.trimToNull(m.group(1)));
      name = m.replaceFirst("");
      // if there was a rank given in the nom status populate the rank marker field
      if (pn.getNomenclaturalNotes() != null) {
        Matcher rm = NOV_RANK_MARKER.matcher(pn.getNomenclaturalNotes());
        if (rm.find()) {
          ParsingJob.setRank(pn, rm.group(1));
        }
      }
    }

    // extract sec reference
    m = EXTRACT_SENSU.matcher(name);
    if (m.find()) {
      pn.setSensu(StringUtils.trimToNull(m.group(1)));
      name = m.replaceFirst("");
    }
    // extract other remarks
    m = EXTRACT_REMARKS.matcher(name);
    if (m.find()) {
      pn.setRemarks(StringUtils.trimToNull(m.group(1)));
      name = m.replaceFirst("");
    }

    // check for indets unless we already have a cultivar
    if (pn.getCultivarEpithet() == null) {
      m = RANK_MARKER_AT_END.matcher(name);
      // f. is a marker for forms, but more often also found in authorships as "filius" - son of.
      // so ignore those
      if (m.find() && !(name.endsWith(" f.") || name.endsWith(" f"))) {
        pn.setType(NameType.INFORMAL);
        ParsingJob.setRank(pn, m.group(2));
        name = m.replaceAll("");
      }
      m = NORM_INDET.matcher(name);
      if (m.find()) {
        pn.setType(NameType.INFORMAL);
        name = m.replaceAll(" ");
      }

    }

    name = normalizeStrong(name);
    if (Strings.isNullOrEmpty(name)) {
      unparsable(NameType.NO_NAME, scientificName);
    }

    // remember current rank for later reuse
    Rank preparsingRank = pn.getRank();

    // try regular parsing
    boolean parsed = parseNormalisedName(name);
    if (!parsed) {
      // try again with stronger normalisation, maybe that helps...
      LOG.debug("Can't parse, use dirty normalizer");
      final String deDirtedName = cleanStrong(name);
      parsed = parseNormalisedName(deDirtedName);
      if (!parsed) {
          // we just cant parse this one
          // try to spot a virus name once we know its not a scientific name
          m = IS_VIRUS_PATTERN_POSTFAIL.matcher(name);
          if (m.find()) {
            unparsable(NameType.VIRUS, scientificName);
          }
          unparsable(NameType.NO_NAME, scientificName);

      } else {
        pn.setDoubtful(true);
        LOG.warn("PARSED DIRTY: {}  ---  {}", scientificName, deDirtedName);
      }
    }

    // did we parse a infraspecic manuscript name?
    if (infraspecEpithet != null) {
      pn.setInfraspecificEpithet(infraspecEpithet);
    }
    // if we established a rank during preparsing make sure we use this not the parsed one
    if (preparsingRank != null && preparsingRank.notOtherOrUnranked()) {
      pn.setRank(preparsingRank);
    }

    // determine name type
    determineNameType(pn, name);

    // flag names that match doubtful patterns
    applyDoubtfulFlag(pn, scientificName);

    // determine rank if not yet assigned
    if (pn.getRank() == null || pn.getRank().otherOrUnranked()) {
      pn.setRank(RankUtils.inferRank(pn));
    }

    // determine code if not yet assigned
    determineCode(pn);
  }

  /**
   * A very optimistic cleaning intended for names potentially very very dirty
   *
   * @param name To normalize
   *
   * @return The normalized name
   */
  private static String cleanStrong(String name) {
    if (name != null) {
      // remove final & which causes long parse times

      // test for known bad suffixes like in Palythoa texaensis author unknown
      Matcher m = BAD_NAME_SUFFICES.matcher(name);
      if (m.find()) {
        name = m.replaceAll("");
      }

      // replace weird chars
      name = NORM_WEIRD_CHARS.matcher(name).replaceAll(" ");
      // swap comb and basionym authors if comb authorship is clearly identified by a &

      // TODO: improve regex - some names take minutes to parse, so we cant use it as it is !
      // Matcher m = COMB_BAS_AUTHOR_SWAP.matcher(name);
      // if (m.find() && m.group(1)!=null && m.group(1).contains("&") && m.group(3)!=null ){
      // System.out.println("SWAP:"+m.group(1)+"|"+m.group(2)+"|"+m.group(3)+"|"+m.group(4));
      // name = m.replaceFirst(StringUtils.defaultString("("+m.group(3))+StringUtils.trimToEmpty(m.group(4))+")" +
      // m.group(1)+StringUtils.trimToEmpty(m.group(2)));
      // }
      // uppercase the first letter, lowercase the rest of the first word
      m = FIRST_WORD.matcher(name);
      if (m.find() && m.group(2) == null) {
        // System.out.println(m.group(1)+"|"+m.group(2)+"|"+m.group(3)+"|"+m.group(4));
        name = m.replaceFirst(
            StringUtils.defaultString(m.group(1)) + m.group(3).toUpperCase() + m.group(4).toLowerCase() + " ");
      }

      // normalize genus hybrid marker again
      m = NORM_HYBRIDS_GENUS.matcher(name);
      if (m.find()) {
        name = m.replaceFirst("×$1");
      }
    }

    return name;
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
  static String normalize(String name) {
    if (name == null) {
      return null;
    }

    // normalise usage of rank marker with 2 dots, i.e. forma specialis and sensu latu
    Matcher m = FORM_SPECIALIS.matcher(name);
    if (m.find()) {
      name = m.replaceAll("fsp");
    }
    m = SENSU_LATU.matcher(name);
    if (m.find()) {
      name = m.replaceAll("sl");
    }

    // normalise usage of dots making sure its followed by a space, a bracket or a comma
    m = NORM_DOTS.matcher(name);
    if (m.find()) {
      name = m.replaceAll(". ");
    }
    // remove commans after basionym brackets
    m = COMMA_AFTER_BASYEAR.matcher(name);
    if (m.find()) {
      name = m.replaceFirst("$1)");
    }

    // use commas before years
    // ICZN §22A.2 http://www.iczn.org/iczn/includes/page.jsp?article=22&nfv=
    m = COMMA_BEFORE_YEAR.matcher(name);
    if (m.find()) {
      name = m.replaceAll("$1, $2");
    }
    // no whitespace around hyphens
    name = NORM_HYPHENS.matcher(name).replaceAll("-");
    // use whitespace with &
    name = NORM_AMPERSAND_WS.matcher(name).replaceAll(" & ");

    // whitespace before and after brackets, keeping the bracket style
    m = NORM_BRACKETS_OPEN.matcher(name);
    if (m.find()) {
      name = m.replaceAll(" $1");
    }
    m = NORM_BRACKETS_CLOSE.matcher(name);
    if (m.find()) {
      name = m.replaceAll("$1 ");
    }
    // remove whitespace before commas and replace double commas with one
    m = NORM_COMMAS.matcher(name);
    if (m.find()) {
      name = m.replaceAll(", ");
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
    // capitalize all entire upper case words
    m = NORM_UPPERCASE_WORDS.matcher(name);
    while (m.find()) {
      name = name.replaceFirst(m.group(0), m.group(1) + m.group(2).toLowerCase());
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
  static String normalizeStrong(String name) {
    if (name == null) {
      return null;
    }
    // normalize all quotes to single "
    name = NORM_QUOTES.matcher(name).replaceAll("'");
    // enclosing quotes
    name = REPLACE_QUOTES.matcher(name).replaceAll("");
    // no question marks after words (after years they should remain!)
    Matcher m = NO_Q_MARKS.matcher(name);
    if (m.find()) {
      name = m.replaceAll("$1");
    }

    // remove prefixes
    name = NORM_PREFIXES.matcher(name).replaceAll("");

    // remove brackets inside the genus, the kind taxon finder produces
    m = NORM_TF_GENUS.matcher(name);
    if (m.find()) {
      name = m.replaceAll("$1$2 ");
    }
    // replace imprint years. See ICZN §22A.2.3 http://www.iczn.org/iczn/index.jsp?nfv=&article=22
    // Ctenotus alacer Storr, 1970 ["1969"] -> Ctenotus alacer Storr, 1970
    // C. Flahault 1887 ("1886-1888") -> C. Flahault 1887
    m = NORM_IMPRINT_YEAR.matcher(name);
    if (m.find()) {
      // System.out.println(m.group(0));
      // System.out.println(m.group(1));
      name = m.replaceAll("$1");
    }

    // replace bibliographic in authorship
    name = NORM_IN_COMMA.matcher(name).replaceFirst(" in ");
    /*
     * m = NORM_IN_BIB.matcher(name);
     * if (m.find()) {
     * // keep year if it only exists in IN reference
     * Matcher mIN = EXTRACT_YEAR.matcher(m.group(0));
     * name = m.replaceAll("");
     * Matcher mNAME = EXTRACT_YEAR.matcher(name);
     * if (mIN.find() && !mNAME.find()) {
     * name = name + ", " + mIN.group(1);
     * }
     * }
     */

    // This is redundant, as it is done in the regular normalize function already
    // BUT somehow all upper case authors slow down parsing so much that it can even come to an hold in the next step
    // so we pay the price and do it twice
    // capitalize all entire upper case words
    m = NORM_UPPERCASE_WORDS.matcher(name);
    while (m.find()) {
      name = name.replaceFirst(m.group(0), m.group(1) + m.group(2).toLowerCase());
    }

    /*
     * ICBN §46.2, Note 1.
     * When authorship of a name differs from authorship of the publication in which it was validly published, both are
     * sometimes cited,
     * connected by the word "in". In such a case, "in" and what follows are part of a bibliographic citation and are
     * better omitted
     * unless the place of publication is being cited.
     */

    // normalise original name authorship, putting author AND year in brackets
    // with long authorships this gets slow. Test if year exists first:
    //TODO: is this really needed for some names??? activate if we find evidence and add to tests
    //m = EXTRACT_YEAR.matcher(name);
    //if (m.find() && name.length() < 80) {
    //  m = NORM_ORIG_AUTH.matcher(name);
    //  if (m.find()) {
    //    name = m.replaceAll(" ($1 $2)");
    //  }
    //  m = NORM_ORIG_AUTH2.matcher(name);
    //  if (m.find()) {
    //    name = m.replaceAll("($1 $2)");
    //  }
    //}

    // replace square brackets, keeping content (or better remove all within?)
    name = NORM_NO_SQUARE_BRACKETS.matcher(name).replaceAll(" $1 ");
    // replace different kind of brackets with ()
    name = NORM_BRACKETS_OPEN_STRONG.matcher(name).replaceAll(" (");
    name = NORM_BRACKETS_CLOSE_STRONG.matcher(name).replaceAll(") ");
    // normalise different usages of ampersand, and, et &amp;
    name = NORM_AND.matcher(name).replaceAll(" & ");
    // but keep "et al." instead of "& al."
    name = NORM_ET_AL.matcher(name).replaceAll(" et al.");

    // // add commas between authors in space delimited list
    // m = NORM_AUTH_DELIMIT.matcher(name);
    // if (m.find()){
    // name = m.replaceAll("$1, $2");
    // }
    // Bryozoan indet. 1
    // Bryozoa sp. 2
    // Bryozoa sp. E

    name = NORM_SUFFIXES.matcher(name).replaceAll("");

    // add parenthesis around subgenus if missing
    m = NORM_SUBGENUS.matcher(name);
    if (m.find()) {
      name = m.replaceAll("$1 ($2) $3");
    }

    // finally whitespace and trimming
    name = normalize(name);
    // name = NORM_WHITESPACE.matcher(name).replaceAll(" ");
    return StringUtils.trimToEmpty(name);
  }

  /**
   * basic careful cleaning, trying to preserve all parsable name parts
   */
  @VisibleForTesting
  static String preClean(String name) {
    // remove bad whitespace in html entities
    Matcher m = XML_ENTITY_STRIP.matcher(name);
    if (m.find()) {
      name = m.replaceAll("&$1;");
    }
    // unescape html entities
    name = StringEscapeUtils.unescapeHtml4(name);
    // finally remove still existing bad ampersands missing the closing ;
    name = AMPERSAND_ENTITY.matcher(name).replaceAll("& ");
    // replace xml tags
    name = XML_TAGS.matcher(name).replaceAll("");
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
  private void determineNameType(ParsedName pn, String normedName) {
    // all rules below do not apply to unparsable names
    if (pn.getType() == null || pn.getType().isParsable()) {

      // if we only match a monomial in the 3rd pass its suspicious
      if (pn.getUninomial() != null && Character.isLowerCase(normedName.charAt(0))) {
        pn.addWarning("lower case monomial match");
        pn.setDoubtful(true);
        setTypeIfNull(pn, NameType.INFORMAL);

      } else if (pn.getRank() != null && pn.getRank().notOtherOrUnranked()) {
        if (pn.getRank().equals(Rank.CULTIVAR) && pn.getCultivarEpithet() == null) {
          pn.addWarning("indetermined cultivar witout cultivar epithet");
          pn.setType(NameType.INFORMAL);

        } else if (pn.getRank().isSpeciesOrBelow() && pn.getRank().isRestrictedToCode()!= NomCode.CULTIVARS && !pn.isBinomial()) {
          pn.addWarning("indetermined species without specific epithet");
          pn.setType(NameType.INFORMAL);

        } else if (pn.getRank().isInfraspecific() && pn.getRank().isRestrictedToCode()!= NomCode.CULTIVARS && pn.getInfraspecificEpithet() == null) {
          pn.addWarning("indetermined infraspecies without infraspecific epithet");
          pn.setType(NameType.INFORMAL);

        } else if (!pn.getRank().isSpeciesOrBelow() && pn.isBinomial()) {
          pn.addWarning("binomial with rank higher than species");
          pn.setDoubtful(true);
        }
      }

      if (pn.getType() == null) {
        // a placeholder epithet only?
        if (pn.getGenus() != null && pn.getGenus().equals("?")  ||  pn.getUninomial() != null && pn.getUninomial().equals("?")) {
          pn.setType(NameType.PLACEHOLDER);

        } else {
          pn.setType(NameType.SCIENTIFIC);
        }
      }
    }
  }

  private void applyDoubtfulFlag(ParsedName pn, String scientificName) {
    // all rules below do not apply to unparsable names
    Matcher m = DOUBTFUL.matcher(scientificName);
    if (!m.find()) {
      pn.setDoubtful(true);
      pn.addWarning("doubtful letters");

    } else if (pn.getType().isParsable()){
      m = DOUBTFUL2.matcher(scientificName);
      if (m.find()) {
        pn.setDoubtful(true);
        pn.addWarning("doubtful epithet with literal value null");
      }
    }
  }

  private void determineCode(ParsedName pn) {
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

      } else if (pn.isCandidatus() || pn.getStrain() != null) {
        pn.setCode(NomCode.BACTERIAL);
      }
    }
  }

  /**
   * Tries to parse a name string with the full regular expression.
   * In very few, extreme cases names with very long authorships might cause the regex to never finish or take hours
   * we run this parsing in a separate thread that can be stopped if it runs too long.
   * @param scientificName
   * @return  true if the name could be parsed, false in case of failure
   */
  boolean parseNormalisedName(String scientificName) {
    LOG.debug("Parse normed name string: {}", scientificName);
    Matcher matcher = NAME_PATTERN.matcher(scientificName);
    if (matcher.find()) {
      if (!matcher.group(0).equals(scientificName)) {
        LOG.info("{} - matched only part of the name: {}", matcher.group(0), scientificName);
        pn.setState(ParsedName.State.NAME_AND_AUTHOR);

      } else {
        pn.setState(ParsedName.State.COMPLETE);
        if (LOG.isDebugEnabled()) {
          logMatcher(matcher);
        }
        // the match can be the genus part of a bi/trinomial or a uninomial
        setUninomialOrGenus(matcher, pn);
        boolean bracketSubrankFound = false;
        if (matcher.group(2) != null) {
          bracketSubrankFound = true;
          pn.setInfragenericEpithet(StringUtils.trimToNull(matcher.group(2)));
        } else if (matcher.group(4) != null) {
          setRank(pn, matcher.group(3));
          pn.setInfragenericEpithet(StringUtils.trimToNull(matcher.group(4)));
        }
        pn.setSpecificEpithet(StringUtils.trimToNull(matcher.group(5)));
        if (matcher.group(6) != null && matcher.group(6).length() > 1 && !matcher.group(6).contains("null")) {
          // 4 parted name, so its below subspecies
          pn.setRank(Rank.INFRASUBSPECIFIC_NAME);
        }
        if (matcher.group(7) != null && matcher.group(7).length() > 1) {
          setRank(pn, matcher.group(7));
        }
        pn.setInfraspecificEpithet(StringUtils.trimToNull(matcher.group(8)));

        // microbial ranks
        if (matcher.group(9) != null) {
          setRank(pn, matcher.group(9));
          pn.setInfraspecificEpithet(matcher.group(10));
        }

        // #11 is entire authorship, not stored in ParsedName
        if (matcher.group(11) != null) {
          // #12/13/14/15 basionym authorship (ex/auth/sanct/year)
          pn.setBasionymAuthorship(parseAuthorship(matcher.group(12), matcher.group(13), matcher.group(15)));
          if (bracketSubrankFound && infragenericIsAuthor(pn, rank)) {
            // rather an author than a infrageneric rank. Swap
            pn.setBasionymAuthorship(parseAuthorship(null, pn.getInfragenericEpithet(), null));
            pn.setInfragenericEpithet(null);
            // check if we need to move genus to uninomial
            if (pn.getSpecificEpithet() == null) {
              pn.setUninomial(pn.getGenus());
              pn.setGenus(null);
            }
            LOG.debug("swapped subrank with bracket author: {}", pn.getBasionymAuthorship());
          }

          // #16/17/18/19 authorship (ex/auth/sanct/year)
          pn.setCombinationAuthorship(parseAuthorship(matcher.group(16), matcher.group(17), matcher.group(19)));
          // sanctioning author
          if (matcher.group(18) != null) {
            pn.setSanctioningAuthor(matcher.group(18));
          }
        }

        // make sure (infra)specific epithet is not a rank marker!
        lookForIrregularRankMarker(pn);
        // 2 letter epitheta can also be author prefixes - check that programmatically, not in regex
        checkEpithetVsAuthorPrefx(pn);

        // if no rank was parsed but given externally use it!
        if (rank != null && pn.getRank().otherOrUnranked()) {
          pn.setRank(rank);
        }
        return true;
      }

    }

    return false;
  }

  private static String cleanYear(String matchedYear) {
    if (matchedYear != null && matchedYear.length() > 2) {
      return matchedYear.trim();
    }
    return null;
  }

  /**
   * Sets the parsed names rank based on a found rank marker
   * Potentially also sets the notho field in case the rank marker indicates a hybrid
   * @param pn
   * @param rankMarker
   */
  static void setRank(ParsedName pn, String rankMarker) {
    rankMarker = StringUtils.trimToNull(rankMarker);
    Rank rank = RankUtils.inferRank(rankMarker);
    pn.setRank(rank);
    if (rank != null && rankMarker.startsWith(NOTHO)) {
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

    private static boolean infragenericIsAuthor(ParsedName pn, Rank rank) {
        return pn.getBasionymAuthorship().isEmpty()
            && pn.getSpecificEpithet() == null
            && (
                rank != null && !(rank.isInfrageneric() && !rank.isSpeciesOrBelow())
                   //|| pn.getInfraGeneric().contains(" ")
                || rank == null && !LATIN_ENDINGS.matcher(pn.getInfragenericEpithet()).find()
            );
    }

    private void setUninomialOrGenus(Matcher matcher, ParsedName pn) {
      // the match can be the genus part of a bi/trinomial or a uninomial
      if (matcher.group(2) != null || matcher.group(4) != null || matcher.group(5) != null) {
        pn.setGenus(StringUtils.trimToNull(matcher.group(1)));
      } else {
        pn.setUninomial(StringUtils.trimToNull(matcher.group(1)));
      }
    }

  /**
   * if no rank marker is set, inspect epitheta for wrongly placed rank markers and modify parsed name accordingly.
   * This is sometimes the case for informal names like: Coccyzus americanus ssp.
   *
   * @param cn the already parsed name
   */
  private void lookForIrregularRankMarker(ParsedName cn) {
    if (cn.getRank() == null) {
      if (cn.getInfraspecificEpithet() != null) {
        Matcher m = RANK_MARKER_ONLY.matcher(cn.getInfraspecificEpithet());
        if (m.find()) {
          // we found a rank marker, make it one
          setRank(cn, cn.getInfraspecificEpithet());
          cn.setInfraspecificEpithet(null);
        }
      } else if (cn.getSpecificEpithet() != null) {
        Matcher m = RANK_MARKER_ONLY.matcher(cn.getSpecificEpithet());
        if (m.find()) {
          // we found a rank marker, make it one
          setRank(cn, cn.getSpecificEpithet());
          cn.setSpecificEpithet(null);
        }
      }
    } else if(cn.getRank() == Rank.SPECIES && cn.getInfraspecificEpithet() != null) {
      // sometimes sp. is wrongly used as a subspecies rankmarker
      cn.setRank(Rank.SUBSPECIES);
      cn.addWarning("Name was considered species but contains infraspecific epithet");
    }
  }

  /**
   * 2 letter epitheta can also be author prefixes - check that programmatically, not in regex
   */
  private void checkEpithetVsAuthorPrefx(ParsedName cn) {
    if (cn.getRank() == null) {
      if (cn.getInfraspecificEpithet() != null) {
        // might be subspecies without rank marker
        // or short authorship prefix in epithet. test
        String extendedAuthor = cn.getInfraspecificEpithet() + " " + cn.getCombinationAuthorship();
        Matcher m = AUTHORSHIP_PATTERN.matcher(extendedAuthor);
        if (m.find()) {
          // matches author. Prefer that
          LOG.debug("use infraspecific epithet as author prefix");
          cn.setInfraspecificEpithet(null);
//TODO
//          cn.setAuthorship(parseAuthorship(extendedAuthor));
        }
      } else {
        // might be monomial with the author prefix erroneously taken as the species epithet
        String extendedAuthor = cn.getSpecificEpithet() + " " + cn.getCombinationAuthorship();
        Matcher m = AUTHORSHIP_PATTERN.matcher(extendedAuthor);
        if (m.find()) {
          // matches author. Prefer that
          LOG.debug("use specific epithet as author prefix");
          cn.setSpecificEpithet(null);
//TODO
//          cn.setAuthorship(parseAuthorship(extendedAuthor));
        }
      }
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
    // normalize & and et
    team = ET_PATTERN.matcher(team).replaceAll(" & ");
    // treat semicolon differently. Single author name can contain a comma now!
    if (team.contains(";")) {
      List<String> authors = Lists.newArrayList();
      for (String a : AUTHORTEAM_SEMI_SPLITTER.split(team)) {
        Matcher m = AUTHOR_INITIAL_SWAP.matcher(a);
        if (m.find()) {
          authors.add(normAuthor(m.group(2) + " " + m.group(1)));
        } else {
          authors.add(normAuthor(a));
        }
      }
      return authors;

    } else {
      return AUTHORTEAM_SPLITTER.splitToList(normAuthor(team));
    }
  }

  /**
   * Author strings are normalized by removing any whitespace following a dot.
   * See IPNI author standard form recommendations: http://www.ipni.org/standard_forms_author.html
   */
  private static String normAuthor(String authors) {
    return StringUtils.trimToNull(NORM_PUNCT.matcher(authors).replaceAll("\\."));
  }

  static void logMatcher(Matcher matcher) {
    int i = -1;
    while (i < matcher.groupCount()) {
      i++;
      LOG.debug("  {}: >{}<", i, matcher.group(i));
    }
  }

}
