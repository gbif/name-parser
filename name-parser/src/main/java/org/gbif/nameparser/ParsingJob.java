/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
import org.gbif.nameparser.util.UnicodeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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

  private static final Map<Rank, Rank> DIVISION2PHYLUM = ImmutableMap.of(
      Rank.SUPERDIVISION, Rank.SUPERPHYLUM,
      Rank.DIVISION, Rank.PHYLUM,
      Rank.SUBDIVISION, Rank.SUBPHYLUM,
      Rank.INFRADIVISION, Rank.INFRAPHYLUM
  );

  private static final Splitter WHITESPACE_SPLITTER = Splitter.on(" ").trimResults().omitEmptyStrings();
  private static final CharMatcher AUTHORTEAM_DELIMITER = CharMatcher.anyOf(",&");
  private static final Splitter AUTHORTEAM_SPLITTER = Splitter.on(AUTHORTEAM_DELIMITER).trimResults().omitEmptyStrings();
  private static final Splitter AUTHORTEAM_SEMI_SPLITTER = Splitter.on(";").trimResults().omitEmptyStrings();
  private static final Pattern AUTHOR_INITIAL_SWAP = Pattern.compile("^([^,]+) *, *([^,]+)$");
  private static final Pattern NORM_EX_HORT = Pattern.compile("\\b(?:hort(?:usa?)?|cv)[. ]ex ", CASE_INSENSITIVE);
  private static final String SPACE_AUTHOR = "\\p{Lu}\\p{Ll}+ \\p{Lu}+";
  private static final Pattern SPACE_AUTHORTEAM = Pattern.compile("^" + SPACE_AUTHOR +"(?: " + SPACE_AUTHOR + ")*$");
  private static final Pattern INITIALS = Pattern.compile("^\\p{Lu}{1,2}(?:[. ]\\p{Lu}{1,2}){0,2}\\.?$");

  // name parsing
  static final String NAME_LETTERS = "A-ZÏËÖÜÄÉÈČÁÀÆŒ";
  static final String name_letters = "a-zïëöüäåéèčáàæœ";
  static final String AUTHOR_LETTERS = NAME_LETTERS + "\\p{Lu}"; // upper case unicode letter, not numerical
  // (\W is alphanum)
  static final String author_letters = name_letters + "\\p{Ll}-?"; // lower case unicode letter, not numerical
  // common 3 char or longer name suffices
  private static final String AUTHOR_TOKEN_3 = "fil|filius|hort|jun|junior|sen|senior";
  // common name suffices (ms=manuscript, not yet published)
  private static final String AUTHOR_TOKEN = "(?:" +
      "(?<=\\bden )heede" +
      "|(?:\\p{Lu}|-[a-z])[\\p{Lu}\\p{Ll}'-]*" +
      "|" + AUTHOR_TOKEN_3 +
      "|al|f|j|jr|ms|sr|v|v[ao]n|zu[rm]?|bis|d[aeiou]?|de[nrmls]?|degli|e|l[ae]s?|s|ter|'?t|y" +
    ")\\.?";
  @VisibleForTesting
  static final String AUTHOR = AUTHOR_TOKEN + "(?:[ '-]?" + AUTHOR_TOKEN + ")*";
  // common author suffices that can be mistaken for epithets
  static final String AUTHOR_SUFFIX = "(?:bis|ter|d(?:[ae][rnl]?|egli)|van(?: de[nr]?)|zur?)";
  private static final Pattern AUTHOR_SUFFIX_P = Pattern.compile("^" + AUTHOR_SUFFIX + "$");
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
  static final String YEAR_LOOSE = YEAR + "[abcdh?]?(?:[/,-][0-9]{1,4})?";

  private static final String NOTHO = "notho";
  static final String RANK_MARKER = ("(?:"+NOTHO+"|agamo)?(?:(?<!f[ .])sp|t\\.infr|" +
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
  
  private static final String UNALLOWED_EPITHETS = "aff|and|cf|des|from|ms|of|the|where";
  private static final String UNALLOWED_EPITHET_ENDING =
      "bacilliform|coliform|coryneform|cytoform|chemoform|biovar|serovar|genomovar|agamovar|cultivar|genotype|serotype|subtype|ribotype|isolate";
  // allow for cf/aff markers before epithets
  static final String EPI_QUALIFIER = "(?:\\b(aff|cf|nr)[?. ])?";
  static final String EPITHET = "(?:[0-9]+-?|[a-z]-|[doml]'|(?:van|novae) [a-z])?"
            // avoid matching to rank markers
            + "(?!"+RANK_MARKER+"\\b)"
            + "[" + name_letters + "][" + name_letters + "+-]*(?<! d)[" + name_letters + "]"
            // avoid epithets and those ending with the unallowed endings, e.g. serovar and author suffices like filius
            + "(?<!(?:\\b(?:ex|l[ae]|v[ao]n|"+AUTHOR_TOKEN_3+")\\.?|\\b(?:"+UNALLOWED_EPITHETS+")|"+ UNALLOWED_EPITHET_ENDING +"))(?=\\b)";
  static final String MONOMIAL =
    "[" + NAME_LETTERS + "](?:\\.|[" + name_letters + "]+)(?:-[" + NAME_LETTERS + "]?[" + name_letters + "]+)?";
  // a pattern matching typical latin word endings. Helps identify name parts from authors
  private static final Pattern LATIN_ENDINGS;
  static {
      try (BufferedReader br = new BufferedReader(new InputStreamReader(
          ParsingJob.class.getResourceAsStream("/nameparser/latin-endings.txt"), StandardCharsets.UTF_8
      ))) {
        Set<String> endings = br.lines().collect(Collectors.toSet());
        LATIN_ENDINGS = Pattern.compile("(" + Joiner.on('|').skipNulls().join(endings) + ")$");
      } catch (IOException e) {
        throw new IllegalStateException("Failed to read latin-endings.txt from classpath resources", e);
      }
  }
  @VisibleForTesting
  protected static final String INFRAGENERIC =
    "(?:\\(([" + NAME_LETTERS + "][" + name_letters + "-]+)\\)" +
        "|(?: .+?[. ]| )((?:"+NOTHO+")?(?:" +
          StringUtils.join(RankUtils.RANK_MARKER_MAP_INFRAGENERIC.keySet(), "|") +
        "))[. ]([" + NAME_LETTERS + "][" + name_letters + "-]+)"
    + ")";

  static final Pattern SENIOR_EPITHET = Pattern.compile("^(" + MONOMIAL + "(?:" + INFRAGENERIC + ")?)(?:\\b| )senior\\b");
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
  private static final Pattern EXTINCT_PATTERN = Pattern.compile("[†‡✝]+\\s*");

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
  private static final String GTDB_STRAIN_BLOCK = "(?:(?:[A-Z]+(?:[a-z]{1,2}[A-Z]*)?)?[0-9]+(?:b)?[A-Z]*" +
      "|FULL|COMBO|bin)"; // specific tokens
  private static final String GTDB_OTU_MONOMIAL = "(?:(?:"+GTDB_STRAIN_BLOCK+"-?)*(?:"+GTDB_STRAIN_BLOCK+")(?:_[A-Z])?|[A-Z][a-z]{2,}_[A-Z])";
  private static final Pattern GTDB_MONOMIAL_PATTERN = Pattern.compile("^" + GTDB_OTU_MONOMIAL + "$");
  private static final Pattern GTDB_BINOMIAL_PATTERN = Pattern.compile("^(" + GTDB_OTU_MONOMIAL + ") +(sp[0-9]+)$");
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
  static final Pattern EXTRACT_SENSU = Pattern.compile("[;, ]?(?:\\b|^)" +
      "(" +
        "(?:(?:excl[. ](?:gen|sp|var)|mut.char|p.p)[. ])?" +
        "\\(?(?:" +
          "ss?[. ](?:(?:ampl|l|s|str)[. ]" +
          "|(?:ampl|lat|strict)(?:[uo]|issimo)?)" +
          "|(?:(?:ss[. ])?[aA]uctt?|[eE]mend|[fF]ide|[nN]on|not|[nN]ec|[sS]ec|[sS]ensu|[aA]ccording to)(?:[. (]|\\.?$).*" +
        ")\\)?" +
      ")");
  private static final String NOV_RANKS = "((?:[sS]ub)?(?:[fF]am|[gG]en|[sS]s?p(?:ec)?|[vV]ar|[fF](?:orma?)?))";
  private static final Pattern NOV_RANK_MARKER = Pattern.compile("\\b(" + NOV_RANKS + ")\\b");
  static final String MANUSCRIPT_STATUS = "(?:(?:comb[. ]?)?ined|ms|in press|unpublished)\\.?($|\\s)";
  static final Pattern MANUSCRIPT_STATUS_PATTERN = Pattern.compile(MANUSCRIPT_STATUS);
  static final Pattern EXTRACT_NOMSTATUS = Pattern.compile("[;, ]?"
      + "[(\\[]?"
      + "\\b("
        + "(?:comb|"+NOV_RANKS+")[. ]nov\\b[. ]?(?:ined[. ])?"
        + "|"+MANUSCRIPT_STATUS
        + "|orth[. ](?:var|error)"
        + "|nom(?:en)?[. ]"
          + "(?:utiq(?:ue)?[. ])?"
          + "(?:ambig|alter|alt|correct|cons|dubium|dub|herb|illeg|invalid|inval|negatum|neg|novum|nov|nudum|nud|oblitum|obl|praeoccup|prov|prot|transf|superfl|super|rejic|rej)\\b[. ]?"
          + "(?:prop[. ]|proposed\\b)?"
      + ")"
      + "[)\\]]?");
  private static final Pattern NORM_ANON = Pattern.compile("\\b(anon\\.?)(\\b|\\s|$)");
  private static final Pattern COMMA_AFTER_BASYEAR = Pattern.compile("("+YEAR+")\\s*\\)\\s*,");
  private static final Pattern NORM_APOSTROPHES = Pattern.compile("([\u0060\u00B4\u2018\u2019]+)");
  private static final Pattern NORM_QUOTES = Pattern.compile("([\"'`´]+)");
  private static final Pattern REPL_GENUS_QUOTE = Pattern.compile("^' *(" + MONOMIAL + ") *'");
  private static final Pattern REPL_ENCLOSING_QUOTE = Pattern.compile("^$");//Pattern.compile("^[',\\s]+|[',\\s]+$");
  private static final Pattern NORM_UPPERCASE_WORDS = Pattern.compile("\\b(\\p{Lu})(\\p{Lu}{2,})\\b");
  private static final Pattern NORM_LOWERCASE_BINOMIAL = Pattern.compile("^(" + EPITHET + ") (" + EPITHET + ")");
  private static final Pattern NORM_HYBRID_HOMOGLYPHS = Pattern.compile("[хᕁᕽ᙮ⅹ⤫⤬⨯ｘ\uD835\uDC31\uD835\uDC65\uD835\uDC99\uD835\uDCCD\uD835\uDD01\uD835\uDD35\uD835\uDD69\uD835\uDD9D\uD835\uDDD1\uD835\uDE05\uD835\uDE39\uD835\uDE6D\uD835\uDEA1]");
  private static final Pattern NORM_WHITESPACE = Pattern.compile("(?:\\\\[nr]|\\s)+");
  private static final Pattern REPL_UNDERSCORE = Pattern.compile("_+");
  private static final Pattern NORM_NO_SQUARE_BRACKETS = Pattern.compile("\\[(.*?)\\]");
  private static final Pattern NORM_BRACKETS_OPEN = Pattern.compile("\\s*([{(\\[])\\s*,?\\s*");
  private static final Pattern NORM_BRACKETS_CLOSE = Pattern.compile("\\s*,?\\s*([})\\]])\\s*");
  private static final Pattern NORM_BRACKETS_OPEN_STRONG = Pattern.compile("( ?[{\\[] ?)+");
  private static final Pattern NORM_BRACKETS_CLOSE_STRONG = Pattern.compile("( ?[}\\]] ?)+");
  private static final Pattern NORM_AND = Pattern.compile("\\b *(?<!-)(and|et|und|\\+|,&)(?!-) *\\b");
  private static final Pattern NORM_SUBGENUS = Pattern.compile("(" + MONOMIAL + ") (" + MONOMIAL + ") (" + EPITHET + ")");
  private static final Pattern NO_Q_MARKS = Pattern.compile("([" + author_letters + "])\\?+");
  private static final Pattern NORM_PUNCTUATIONS = Pattern.compile("\\s*([.,;:&(){}\\[\\]-])\\s*\\1*\\s*");
  private static final Pattern NORM_YEAR = Pattern.compile("[\"'\\[]+\\s*(" + YEAR_LOOSE + ")\\s*[\"'\\]]+");
  private static final Pattern NORM_IMPRINT_YEAR = Pattern.compile("(" + YEAR_LOOSE + ")\\s*" +
      "([(\\[,&]? *(?:not|imprint)? *\"?" + YEAR_LOOSE + "\"?[)\\]]?)");
  // √ó is an utf garbaged version of the hybrid cross found in IPNI. See http://dev.gbif.org/issues/browse/POR-3081
  private static final Pattern NORM_HYBRIDS_GENUS = Pattern.compile("^\\s*(?:[+×xX]|√ó)\\s*([" + NAME_LETTERS + "])");
  private static final Pattern NORM_HYBRIDS_EPITH = Pattern.compile("^\\s*(×?" + MONOMIAL + ")\\s+(?:×|√ó|[xX]\\s)\\s*(" + EPITHET + ")");
  private static final Pattern NORM_HYBRIDS_FORMULA = Pattern.compile("[. ]([×xX]|√ó) ");
  private static final Pattern NORM_TF_GENUS = Pattern.compile("^([" + NAME_LETTERS + "])\\(([" + name_letters + "-]+)\\)\\.? ");
  private static final Pattern REPL_FINAL_PUNCTUATIONS = Pattern.compile("[,;:]+$");
  private static final Pattern REPL_IN_REF = Pattern.compile("[, ]?\\b(in|IN|apud) (" + AUTHOR_TEAM + ")(.*?)$");
  private static final Pattern REPL_RANK_PREFIXES = Pattern.compile("^(sub)?(fossil|" +
      StringUtils.join(RankUtils.RANK_MARKER_MAP_SUPRAGENERIC.keySet(), "|") + ")\\.?\\s+", CASE_INSENSITIVE);
  private static final Pattern MANUSCRIPT_NAMES = Pattern.compile("\\b(indet|spp?)[. ](?:nov\\.)?[A-Z0-9][a-zA-Z0-9-]*(?:\\(.+?\\))?");
  private static final Pattern NO_LETTERS = Pattern.compile("^[^a-zA-Z]+$");
  private static final Pattern BAD_AUTHORSHIP = Pattern.compile("(?:" +
    "\\b(?:not\\s|un)(?:applicable|given|known|specified|certain)|missing|\\?)" +
    "(?:[, ]+(" + YEAR_LOOSE + "))?$", Pattern.CASE_INSENSITIVE);
  private static final Pattern PLACEHOLDER_GENUS = Pattern.compile("^(In|Dummy|Missing|Temp|Unknown|Unplaced|Unspecified) (?=[a-z]+)\\b");
  private static final String PLACEHOLDER_NAME = "(?:allocation|awaiting|" +
    "deleted?|dummy|incertae ?sedis|[iu]ndetermined|mixed|" +
    "not (?:assigned|stated)|" +
    "place ?holder|temp|tobedeleted|" +
    "un(?:accepted|allocated|assigned|certain|classed|classified|cultured|described|det(?:ermined)?|ident|known|named|placed|specified)" +
  ")"; // not assigned
  private static final Pattern REMOVE_PLACEHOLDER_INFRAGENERIC = Pattern.compile("\\b\\( ?"+PLACEHOLDER_NAME+" ?\\) ", CASE_INSENSITIVE);
  @VisibleForTesting
  static final Pattern PLACEHOLDER = Pattern.compile("^N\\.\\s*N\\.|\\b"+PLACEHOLDER_NAME+"\\b", CASE_INSENSITIVE);
  private static final Pattern INFORMAL_UNPARSABLE = Pattern.compile(" clade\\b", CASE_INSENSITIVE);
  private static final Pattern DOUBTFUL = Pattern.compile("^[" + AUTHOR_LETTERS + author_letters + HYBRID_MARKER + RankUtils.ALPHA_DELTA + "\":;&*+\\s,.()\\[\\]/'`´0-9-†]+$");
  private static final Pattern DOUBTFUL_NULL = Pattern.compile("\\bnull\\b", CASE_INSENSITIVE);
  private static final Pattern XML_ENTITY_STRIP = Pattern.compile("&\\s*([a-z]+)\\s*;");
  // matches badly formed amoersands which are important in names / authorships
  private static final Pattern AMPERSAND_ENTITY = Pattern.compile("& *amp +");

  private static final Pattern XML_TAGS = Pattern.compile("< */? *[a-zA-Z] *>");
  private static final Pattern STARTING_EPITHET = Pattern.compile("^\\s*(" + EPITHET + ")\\b");
  private static final Pattern FORM_SPECIALIS = Pattern.compile("\\bf\\. *sp(?:ec)?\\b");
  private static final Pattern SENSU_LATU = Pattern.compile("\\bs\\.l\\.\\b");
  private static final Pattern ABREV_AUTHOR_PREFIXES = Pattern.compile("\\b(v\\.(?:d\\.)?)("+AUTHOR+")");

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
        sb.append(r.name().toLowerCase(), 0, r.name().length() - 3);
      }
    }
    sb.append(")type\\b");
    TYPE_TO_VAR = Pattern.compile(sb.toString());
  }
  private static final Set<String> BLACKLIST_EPITHETS;
  static {
    try (BufferedReader br = new BufferedReader(new InputStreamReader(
      ParsingJob.class.getResourceAsStream("/nameparser/blacklist-epithets.txt"), StandardCharsets.UTF_8
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
  private static final Pattern REMOVE_INTER_RANKS = Pattern.compile("\\b((?:subsp|ssp|var)[ .].+)\\b("+RANK_MARKER+"\\b.{2,})");
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
                // #8 infraspecies qualifier
                " ?"+ EPI_QUALIFIER +
                // #9 infraspecies rank
                "[. ]?(" + RANK_MARKER + ")?" +
                // #10 infraspecies epitheton, avoid matching to bis and degli which is part of (Italian) author names
                "[. ](×?\"?(?!" + AUTHOR_SUFFIX + "\\b)" + EPITHET + "\"?)" +
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

  // Allowable phrase rank marker
  private static final String AUTHOR_WS = AUTHOR_TOKEN + "(?:[ '-]*" + AUTHOR_TOKEN + ")*";
  private static final String AUTHOR_TEAM_WS = AUTHOR_WS + "(?:\\s*[&,;]+\\s*" + AUTHOR_WS + ")*";
  private static final String PLACEHOLDER_RANK_MARKER = "sp|subsp|ssp|var|cv";
  private static final String ALL_LETTERS_NUMBERS = NAME_LETTERS + name_letters + "0-9";
  private static final String DESC_WORD = "[" + NAME_LETTERS + "][" + ALL_LETTERS_NUMBERS + ".\\-_]*";
  private static final String LOCATION_OR_DESC = "(?:\\d+\\.?|" + DESC_WORD + "(?:\\s+" + DESC_WORD +")*)";
  private static final String DESCRIPTIVE_PHRASE = "(?:'" + LOCATION_OR_DESC + "'|" + LOCATION_OR_DESC + ")";
  private static final String SINE_NOMINE = "s\\.n\\.,?";
  private static final String VOUCHER_NUMBER_OR_DATE = "(?:[A-Z]*[0-9]+|[0-9]+[\\s\\-/][0-9A-Za-z]+[\\s\\-/][0-9]+)";
  private static final String VOUCHER = AUTHOR_TEAM_WS + "(?:\\s+" + SINE_NOMINE + ")?\\s+" + VOUCHER_NUMBER_OR_DATE;
  private static final String NOTE = "\\[([^]]+)]";

  public static final Pattern PHRASE_NAME = Pattern.compile("^" +
      // This pattern has to be applied before normalisation, since otherwise phrases and vouchers get played with
      // Group 1 is normal scientific name - will either represent a Monomial or binomial with optional author and infrageneric name
      "(" + MONOMIAL + "(?:\\s*\\(" + MONOMIAL + "\\))?(?:\\s+" + EPITHET +")?(?:\\s+\\(?" + AUTHOR_TEAM_WS + "\\)?)?)"
      // Group 2 is the Rank marker.  For a phrase it needs to be sp. subsp. or var.
      + "\\s+(" + PLACEHOLDER_RANK_MARKER + ")\\.?"
      // Group 3 indicates the mandatory location/desc for the phrase name.
      + "\\s*(" + DESCRIPTIVE_PHRASE + ")"
      //Group 4 is the VOUCHER for the phrase it indicates the collector and a voucher id
      + "\\s*\\((" + VOUCHER + ")\\)"
      //Group 5 is the party proposing addition of the taxon
      + "\\s*(" + AUTHOR_TEAM + ")?"
      //Group 6 is any additional notes
      + "\\s*(" + NOTE + ")?$"
  );

  static Matcher matcherInterruptable(Pattern pattern, String text) throws InterruptedException {
    if (Thread.currentThread().isInterrupted()) {
      throw new InterruptedException("Interrupted!");
    }
    return pattern.matcher(new InterruptibleCharSequence(text));
  }

  static String replAllInterruptable(Pattern pattern, String text, String replacement) throws InterruptedException {
    if (Thread.currentThread().isInterrupted()) {
      throw new InterruptedException("Interrupted!");
    }
    return pattern.matcher(new InterruptibleCharSequence(text)).replaceAll(replacement);
  }

  static boolean findInterruptable(Pattern pattern, String text) throws InterruptedException {
    if (Thread.currentThread().isInterrupted()) {
      throw new InterruptedException("Interrupted!");
    }
    return pattern.matcher(new InterruptibleCharSequence(text)).find();
  }

  final Rank rank;
  final String scientificName;
  final String scientificNameCleaned; // preCleaned scientificName
  final ParserConfigs configs;
  final ParsedName pn;
  boolean ignoreAuthorship;
  String name; // current version of the scientificName being parsed/normalized
  ParsedName.State state; // current parsing state

  /**
   * @param scientificName the full scientific name to parse
   * @param rank the rank of the name if it is known externally. Helps identifying infrageneric names vs bracket authors
   */
  ParsingJob(String scientificName, Rank rank, NomCode code, ParserConfigs configs) {
    this.scientificName = Preconditions.checkNotNull(scientificName);
    this.configs = Preconditions.checkNotNull(configs);
    this.rank = Preconditions.checkNotNull(rank);
    pn =  new ParsedName();
    pn.setRank(rank);
    pn.setCode(code);
    // clean name, removing seriously wrong things
    scientificNameCleaned = preClean(scientificName, pn.getWarnings());
    name = scientificNameCleaned;
  }

  void unparsable(NameType type) throws UnparsableNameException {
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
  public ParsedName call() throws UnparsableNameException, InterruptedException {
    long start = 0;
    if (LOG.isDebugEnabled()) {
      start = System.currentTimeMillis();
    }

    // before further cleaning/parsing try if we have known OTU formats, i.e. BIN or SH numbers
    // test for special cases they do not need any further parsing
    if (!specialCases()) {
      // do the main incremental parsing
      parse();
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("Parsing time: {} for {}", (System.currentTimeMillis() - start), pn);
    }

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
  private boolean specialCases() throws UnparsableNameException, InterruptedException {
    // override exists?
    ParsedName over = configs.forName(name);
    if (over != null) {
      pn.copy(over);
      LOG.debug("Manual override found for name: {}", name);
      return true;
    }

    // BOLD/UNITE OTU names
    Matcher m = matcherInterruptable(OTU_PATTERN, name);
    if (m.find()) {
      pn.setUninomial(m.group(1).toUpperCase());
      pn.setType(NameType.OTU);
      pn.setRank(Rank.UNRANKED);
      pn.setState(ParsedName.State.COMPLETE);
      return true;
    }

    // GTDB OTU names
    // https://github.com/gbif/name-parser/issues/74
    m = matcherInterruptable(GTDB_MONOMIAL_PATTERN, name);
    if (m.find()) {
      pn.setUninomial(m.group());
      pn.setType(NameType.OTU);
      pn.setRank(Rank.UNRANKED);
      pn.setState(ParsedName.State.COMPLETE);
      return true;
    }
    m = matcherInterruptable(GTDB_BINOMIAL_PATTERN, name);
    if (m.find()) {
      pn.setGenus(m.group(1));
      pn.setSpecificEpithet(m.group(2));
      pn.setType(NameType.OTU);
      pn.setRank(Rank.SPECIES);
      pn.setState(ParsedName.State.COMPLETE);
      return true;
    }

    // BOLD style placeholders
    // https://github.com/gbif/name-parser/issues/45
    m = matcherInterruptable(BOLD_PLACEHOLDERS, name);
    if (m.find()) {
      pn.setUninomial(m.group(1));
      pn.setPhrase(m.group(2));
      pn.setState(ParsedName.State.COMPLETE);
      checkBlacklist(); // check blacklist
      pn.setType(NameType.PLACEHOLDER);
      determineCode();
      determineRank();
      return true;
    }
    // unparsable BOLD style placeholder: Iteaphila-group
    m = matcherInterruptable(UNPARSABLE_GROUP, name);
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
  
  void parse() throws UnparsableNameException, InterruptedException {
    // remove extinct markers
    Matcher m = matcherInterruptable(EXTINCT_PATTERN, name);
    if (m.find()) {
      pn.setExtinct(true);
      name = m.replaceFirst("");
    }

    // before any cleaning test for properly quoted candidate names
    m = matcherInterruptable(IS_CANDIDATUS_QUOTE_PATTERN, scientificName);
    if (m.find()) {
      pn.setCandidatus(true);
      name = m.replaceFirst(m.group(2));
    }
  
    // preparse nomenclatural references
    preparseNomRef();

    // normalize bacterial rank markers
    name = replAllInterruptable(TYPE_TO_VAR, name,"$1var");

    // TODO: parse manuscript names properly
    m = matcherInterruptable(INFRASPEC_UPPER, name);
    String infraspecEpithet = null;
    if (m.find()) {
      // we will replace is later again with infraspecific we memorized here!!!
      name = m.replaceFirst("vulgaris");
      infraspecEpithet = m.group(1);
      pn.setType(NameType.INFORMAL);
    }

    // remove placeholders from infragenerics and authors and set type
    removePlaceholderAuthor();
    m = matcherInterruptable(REMOVE_PLACEHOLDER_INFRAGENERIC, name);
    if (m.find()) {
      name = m.replaceFirst("");
      pn.setType(NameType.PLACEHOLDER);
    }

    // resolve parsable names with a placeholder genus only
    m = matcherInterruptable(PLACEHOLDER_GENUS, name);
    if (m.find()) {
      name = m.replaceFirst("? ");
      pn.setType(NameType.PLACEHOLDER);
    }

    // detect further unparsable names
    detectFurtherUnparsableNames();

    if (findInterruptable(IS_VIRUS_PATTERN, name) || findInterruptable(IS_VIRUS_PATTERN_CASE_SENSITIVE, name)) {
      unparsable(NameType.VIRUS);
    }

    // detect RNA/DNA gene/strain names and flag as informal
    if (findInterruptable(IS_GENE, name)) {
      pn.setType(NameType.INFORMAL);
    }

    // Extract phrase name phrase, voucher, nominating party and note
    // This is done before normalisation so that we can preserve case on the phrase
    m = matcherInterruptable(PHRASE_NAME, name);
    if (m.find()) {
      name = m.group(1);
      pn.setRank(RankUtils.inferRank(m.group(2)));
      pn.setPhrase(stripSurround(m.group(3), '\'', '\''));
      pn.setVoucher(m.group(4));
      pn.setNominatingParty(m.group(5));
      pn.setTaxonomicNote(stripSurround(m.group(6), '[', ']'));
      pn.setType(NameType.INFORMAL);
    }

    // normalise name
    name = normalize(name);

    if (StringUtils.isBlank(name)) {
      unparsable(NameType.NO_NAME);
    }

    // remove family in front of subfamily ranks
    m = matcherInterruptable(FAMILY_PREFIX, name);
    if (m.find()) {
      name = m.replaceFirst("$1");
    }

    // check for supraspecific ranks at the beginning of the name
    m = matcherInterruptable(SUPRA_RANK_PREFIX, name);
    if (m.find()) {
      pn.setRank(RankUtils.RANK_MARKER_MAP.get(m.group(1).replace(".", "")));
      name = m.replaceFirst("");
    }

    // parse cultivar names first BEFORE we strongly normalize
    // this will potentially remove quotes needed to find cultivar names
    // this will potentially remove quotes needed to find cultivar group names
    m = matcherInterruptable(CULTIVAR_GROUP, name);
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
    m = matcherInterruptable(CULTIVAR, name);
    if (m.find()) {
      pn.setCultivarEpithet(m.group(2));
      name = m.replaceFirst("$1");
      pn.setRank(Rank.CULTIVAR);
    }

    // name without any latin char letter at all?
    if (findInterruptable(NO_LETTERS, name)) {
      unparsable(NameType.NO_NAME);
    }

    if (findInterruptable(HYBRID_FORMULA_PATTERN, name)) {
      unparsable(NameType.HYBRID_FORMULA);
    }

    m = matcherInterruptable(IS_CANDIDATUS_PATTERN, name);
    if (m.find()) {
      pn.setCandidatus(true);
      name = m.replaceFirst("");
    }

    // extract nom.illeg. and other nomen status notes
    extractNomStatus();

    // manuscript (unpublished) names  without a full scientific name, e.g. Verticordia sp.1
    // http://splink.cria.org.br/documentos/appendix_j.pdf
    m = matcherInterruptable(MANUSCRIPT_NAMES, name);
    String epithet = null;
    if (m.find()) {
      pn.setType(NameType.INFORMAL);
      epithet = m.group(0);
      setRank(m.group(1).replace("indet", "sp"));
      name = m.replaceFirst("");
      pn.setManuscript(true);
    }

    // parse out species/strain names with numbers found in Genebank/EBI names, e.g. Advenella kashmirensis W13003
    m = matcherInterruptable(STRAIN, name);
    if (m.find()) {
      name = m.replaceFirst(m.group(1));
      pn.setType(NameType.INFORMAL);
      pn.setPhrase(m.group(2));
      LOG.debug("Strain: {}", m.group(2));
    }

    // extract sec reference
    extractSecReference();

    // check for indets
    m = matcherInterruptable(RANK_MARKER_AT_END, name);
    // f. is a marker for forms, but more often also found in authorships as "filius" - son of.
    // so ignore those
    if (m.find() && !findInterruptable(FILIUS_AT_END, name)) {
      // use as rank unless we already have a cultivar
      ignoreAuthorship = true;
      if (pn.getCultivarEpithet() == null) {
        setRank(m.group(2));
      }
      name = m.replaceAll("");
    }

    // extract bibliographic in references
    extractPublishedIn();

    // save abbreviated author prefixes, avoiding them to become rank markers, e.g. v.
    final Map<String, String> authorPrefixes = new HashMap<>();
    m = matcherInterruptable(ABREV_AUTHOR_PREFIXES, name);
    if (m.find()) {
      StringBuffer sb = new StringBuffer();
      do {
        authorPrefixes.put(m.group(2), m.group(1));
        m.appendReplacement(sb, m.group(2));
      } while (m.find());
      m.appendTail(sb);
      name = sb.toString();
    }

    // remove superflous epithets with rank markers
    m = matcherInterruptable(REMOVE_INTER_RANKS, name);
    if (m.find()) {
      logMatcher(m);
      pn.addWarning("Intermediate classification removed: " + m.group(1));
      name = m.replaceFirst("$2");
    }

    // remember current rank for later reuse
    final Rank preparsingRank = pn.getRank();

    // replace senior epithets as they are taken as authors below
    m = matcherInterruptable(SENIOR_EPITHET, name);
    boolean seniorSpecies = false;
    if (m.find()) {
      name = m.replaceFirst("$1 zenior");
      seniorSpecies = true;
    }

    String nameStrongly = normalizeStrong(name);

    if (StringUtils.isBlank(nameStrongly)) {
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
      m = matcherInterruptable(IS_VIRUS_PATTERN_POSTFAIL, nameStrongly);
      if (m.find()) {
        unparsable(NameType.VIRUS);
      }

      // cant parse it, fail!
      // Does it appear to be a genuine name starting with a monomial?
      if (findInterruptable(POTENTIAL_NAME_PATTERN, name)) {
        unparsable(NameType.SCIENTIFIC);
      } else {
        unparsable(NameType.NO_NAME);
      }
    }

    // move back senior?
    if (seniorSpecies) {
      pn.setSpecificEpithet("senior");
    }
    // apply saved author prefixes again
    if (!authorPrefixes.isEmpty()) {
      applyAuthorPrefix(authorPrefixes, pn.getBasionymAuthorship());
      applyAuthorPrefix(authorPrefixes, pn.getCombinationAuthorship());
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

    if (state != null) {
      pn.setState(state);
    }
    
    // determine name type
    determineNameType(name);

    // raise warnings for blacklisted epithets
    checkBlacklist();
    
    // flag names that match doubtful patterns
    applyDoubtfulFlag();

    // determine rank if not yet assigned or change according to code
    determineRank();

    // determine code if not yet assigned
    determineCode();
  }

  void applyAuthorPrefix(Map<String, String> prefixes, Authorship auth) {
    applyAuthorPrefix(prefixes, auth.getAuthors());
    applyAuthorPrefix(prefixes, auth.getExAuthors());
  }
  void applyAuthorPrefix(Map<String, String> prefixes, List<String> authors) {
    for (int idx=0; idx<authors.size(); idx++) {
      String a = authors.get(idx);
      if (prefixes.containsKey(a)) {
        authors.set(idx, prefixes.get(a) + a);
      }
    }
  }

  void extractPublishedIn() throws InterruptedException {
    Matcher m = matcherInterruptable(REPL_IN_REF, name);
    if (m.find()) {
      pn.setPublishedIn(normNote(concat(m.group(2), m.group(3))));
      name = m.replaceFirst(m.group(3));
    }
  }

  void detectFurtherUnparsableNames() throws UnparsableNameException, InterruptedException {
    if (findInterruptable(PLACEHOLDER, name)) {
      unparsable(NameType.PLACEHOLDER);
    }
    if (findInterruptable(INFORMAL_UNPARSABLE, name)) {
      unparsable(NameType.INFORMAL);
    }
  }

  void preparseNomRef() throws InterruptedException {
    Matcher m = matcherInterruptable(NOM_REFS, name);
    if (m.find()) {
      name = stripNomRef(m);
      state = ParsedName.State.PARTIAL;
    } else {
      m = matcherInterruptable(NOM_REF_VOLUME, name);
      if (m.find()) {
        name = stripNomRef(m);
        state = ParsedName.State.PARTIAL;
      }
    }
  }

  void removePlaceholderAuthor() throws InterruptedException {
    Matcher m = matcherInterruptable(BAD_AUTHORSHIP, name);
    // check match was more than just ?
    if (m.find() && m.group(0).length()>2) {
      logMatcher(m);
      // replace with year only?
      name = m.replaceFirst(" $1");
      pn.addWarning(Warnings.AUTHORSHIP_REMOVED);
    }
  }

  void extractSecReference() throws InterruptedException {
    Matcher m = matcherInterruptable(EXTRACT_SENSU, name);
    if (m.find()) {
      pn.setTaxonomicNote(lowerCaseFirstChar(normNote(m.group(1))));
      name = m.replaceFirst("");
    }
  }


  void extractNomStatus() throws InterruptedException {
    // extract nom.illeg. and other nomen status notes
    // includes manuscript notes, e.g. ined.
    Matcher m = matcherInterruptable(EXTRACT_NOMSTATUS, name);
    if (m.find()) {
      StringBuffer sb = new StringBuffer();
      do {
        String note = StringUtils.trimToNull(m.group(1));
        if (note != null) {
          pn.addNomenclaturalNote(note);
          m.appendReplacement(sb, " ");
          // if there was a rank given in the nom status populate the rank marker field
          Matcher rm = matcherInterruptable(NOV_RANK_MARKER, note);
          if (rm.find()) {
            setRank(rm.group(1), true);
          }
          // was this a manuscript note?
          Matcher man = matcherInterruptable(MANUSCRIPT_STATUS_PATTERN, note);
          if (man.find()) {
            pn.setManuscript(true);
          }
        }
      } while (m.find());
      m.appendTail(sb);
      name = sb.toString();
    }
  }

  private static String lowerCaseFirstChar(String x) {
    if (x != null && x.length() > 0) {
      return x.substring(0, 1).toLowerCase() + x.substring(1);
    }
    return x;
  }

  private static String concat(String... x) {
    if (x == null) return null;

    StringBuilder sb = new StringBuilder();
    for (String s : x) {
      if (s != null) {
        sb.append(s);
      }
    }
    return sb.toString();
  }

  private static String normNote(String note) {
    if (note.startsWith("(") && note.endsWith(")")) {
      note = note.substring(1, note.length()-1);
    }
    return StringUtils.trimToNull(
        note
        // punctuation to be followed by a space. Dots are special because of author initials
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
   * - normalized hybrid marker to be the ascii multiplication sign
   * - trims whitespace around hyphens
   * - pads whitespace around &
   * - adds whitespace after dots following a genus abbreviation, rank marker or author name
   * - keeps whitespace before opening and after closing brackets
   * - removes whitespace inside brackets
   * - removes whitespace before commas
   * - removes whitespace between hybrid marker and following name part in case it is NOT a hybrid formula
   * - trims the string and replaces multi whitespace with single space
   * - capitalizes all only uppercase words (authors are often found in upper case only)
   *
   * @param name To normalize
   *
   * @return The normalized name
   */
  String normalize(String name) throws InterruptedException {
    if (name == null) {
      return null;
    }
    if (Thread.currentThread().isInterrupted()) {
      throw new InterruptedException("Interrupted!");
    }

    // normalise usage of rank marker with 2 dots, i.e. forma specialis and sensu latu
    Matcher m = matcherInterruptable(FORM_SPECIALIS, name);
    if (m.find()) {
      name = m.replaceAll("fsp");
    }
    m = matcherInterruptable(SENSU_LATU, name);
    if (m.find()) {
      name = m.replaceAll("sl");
    }

    // cleanup years
    name = replAllInterruptable(NORM_YEAR, name,"$1");

    // remove imprint years. See ICZN §22A.2.3 http://www.iczn.org/iczn/index.jsp?nfv=&article=22
    m = matcherInterruptable(NORM_IMPRINT_YEAR, name);
    if (m.find()) {
      LOG.debug("Imprint year {} removed", m.group(2));
      name = m.replaceAll("$1");
    }

    // replace underscores
    name = replAllInterruptable(REPL_UNDERSCORE, name," ");

    // normalise punctuations removing any adjacent space
    m = matcherInterruptable(NORM_PUNCTUATIONS, name);
    if (m.find()) {
      name = m.replaceAll("$1");
    }
    // normalise different usages of ampersand, and, et &amp; to always use &
    name = replAllInterruptable(NORM_AND, name,"&");

    // remove commans after basionym brackets
    m = matcherInterruptable(COMMA_AFTER_BASYEAR, name);
    if (m.find()) {
      name = m.replaceFirst("$1)");
    }

    // no whitespace before and after brackets, keeping the bracket style
    m = matcherInterruptable(NORM_BRACKETS_OPEN, name);
    if (m.find()) {
      name = m.replaceAll("$1");
    }
    m = matcherInterruptable(NORM_BRACKETS_CLOSE, name);
    if (m.find()) {
      name = m.replaceAll("$1");
    }
    // normalize hybrid markers
    m = matcherInterruptable(NORM_HYBRID_HOMOGLYPHS, name);
    if (m.find()) {
      name = m.replaceAll(HYBRID_MARKER);
    }
    m = matcherInterruptable(NORM_HYBRIDS_GENUS, name);
    if (m.find()) {
      name = m.replaceFirst(HYBRID_MARKER+"$1");
    }
    m = matcherInterruptable(NORM_HYBRIDS_EPITH, name);
    if (m.find()) {
      name = m.replaceFirst("$1 "+HYBRID_MARKER+"$2");
    }
    m = matcherInterruptable(NORM_HYBRIDS_FORMULA, name);
    if (m.find()) {
      name = m.replaceAll(" "+HYBRID_MARKER+" ");
    }
    // capitalize Anonymous author
    m = matcherInterruptable(NORM_ANON, name);
    if (m.find()) {
      name = m.replaceFirst("Anon.");
    }
    // capitalize all entire upper case words
    m = matcherInterruptable(NORM_UPPERCASE_WORDS, name);
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
    m = matcherInterruptable(NORM_LOWERCASE_BINOMIAL, name);
    if (m.find()) {
      name = m.replaceFirst(StringUtils.capitalize(m.group(1)) + " " + m.group(2));
    }

    // finally whitespace and trimming
    name = replAllInterruptable(NORM_WHITESPACE, name," ");
    return StringUtils.trimToEmpty(name);
  }


  String normalizeHort(String name) throws InterruptedException {
    // normalize ex hort. (for gardeners, often used as ex names) spelled in lower case
    return replAllInterruptable(NORM_EX_HORT, name,"hort.ex ");
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
  String normalizeStrong(String name) throws InterruptedException {
    if (name == null) {
      return null;
    }
    // normalize ex hort.
    name = normalizeHort(name);

    // normalize all quotes to single "
    name = replAllInterruptable(NORM_QUOTES, name, "'");
    // remove quotes from genus
    name = matcherInterruptable(REPL_GENUS_QUOTE, name).replaceFirst("$1 ");
    // remove enclosing quotes
    Matcher m = matcherInterruptable(REPL_ENCLOSING_QUOTE, name);
    if (m.find()) {
      name = m.replaceAll("");
      pn.addWarning(Warnings.REPL_ENCLOSING_QUOTE);
    }

    // no question marks after letters (after years they should remain)
    name = noQMarks(name);

    // remove prefixes
    name = replAllInterruptable(REPL_RANK_PREFIXES, name, "");

    // remove brackets inside the genus, the kind taxon finder produces
    m = matcherInterruptable(NORM_TF_GENUS, name);
    if (m.find()) {
      name = m.replaceAll("$1$2 ");
    }

    // TODO: replace square brackets, keeping content (or better remove all within?)
    //name = NORM_NO_SQUARE_BRACKETS.matcher(name).replaceAll(" $1 ");

    // replace different kind of brackets with ()
    name = normBrackets(name);

    // add ? genus when name starts with an epithet
    m = matcherInterruptable(STARTING_EPITHET, name);
    if (m.find()) {
      name = m.replaceFirst("? $1");
      pn.addWarning(Warnings.MISSING_GENUS);
    }

    // add parenthesis around subgenus if missing
    m = matcherInterruptable(NORM_SUBGENUS, name);
    if (m.find()) {
      // make sure epithet is not a rank mismatch or author suffix
      if (parseRank(m.group(3)) == null && !AUTHOR_SUFFIX_P.matcher(m.group(3)).find()){
        name = m.replaceAll("$1($2)$3");
      }
    }

    // finally NORMALIZE PUNCTUATION AND WHITESPACE again
    return normWsPunct(name);
  }

  String normWsPunct(String name) throws InterruptedException {
    name = replAllInterruptable(NORM_PUNCTUATIONS, name,"$1");
    name = replAllInterruptable(NORM_WHITESPACE, name," ");
    name = replAllInterruptable(REPL_FINAL_PUNCTUATIONS, name,"");
    return StringUtils.trimToEmpty(name);
  }

  String normBrackets(String name) throws InterruptedException {
    name = replAllInterruptable(NORM_BRACKETS_OPEN_STRONG, name, "(");
    name = replAllInterruptable(NORM_BRACKETS_CLOSE_STRONG, name, ")");
    return name;
  }

  String noQMarks(String name) throws InterruptedException {
    Matcher m = matcherInterruptable(NO_Q_MARKS, name);
    if (m.find()) {
      name = m.replaceAll("$1");
      pn.setDoubtful(true);
      pn.addWarning(Warnings.QUESTION_MARKS_REMOVED);
    }
    return name;
  }

  String stripSurround(String part, char left, char right) {
    if (part == null || part.length() < 2)
      return part;
    if (part.charAt(0) == left && part.charAt(part.length() - 1) == right) {
      part = part.substring(0, part.length() - 1).substring(1).trim();
      part = part.isEmpty() ? null : part;
    }
    return part;
  }

  /**
   * basic careful cleaning, trying to preserve all parsable name parts
   */
  String preClean(String name) throws InterruptedException {
    return preClean(name, pn.getWarnings());
  }

  /**
   * basic careful cleaning, trying to preserve all parsable name parts
   */
  public static String preClean(String name, @Nullable Set<String> warnings) {
    // remove bad whitespace in html entities
    Matcher m = XML_ENTITY_STRIP.matcher(name);
    if (m.find()) {
      name = m.replaceAll("&$1;");
    }
    // unescape html entities
    int length = name.length();
    name = StringEscapeUtils.unescapeHtml4(name);
    if (warnings != null && length > name.length()) {
      warnings.add(Warnings.HTML_ENTITIES);
    }
    // finally remove still existing bad ampersands missing the closing ;
    m = AMPERSAND_ENTITY.matcher(name);
    if (m.find()) {
      name = m.replaceAll("&");
      if (warnings != null) {
        warnings.add(Warnings.HTML_ENTITIES);
      }
    }

    // replace xml tags
    m = XML_TAGS.matcher(name);
    if (m.find()) {
      name = m.replaceAll("");
      if (warnings != null) {
        warnings.add(Warnings.XML_TAGS);
      }
    }

    // translate some very similar characters that sometimes get misused instead of real letters
    String before = name;
    name = UnicodeUtils.replaceHomoglyphs(name, true, "⍺");
    name = StringUtils.replaceChars(name, "¡", "i");
    if (warnings != null && !name.equals(before)) {
      warnings.add(Warnings.HOMOGLYHPS);
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
        if (pn.isPhraseName()) {
          pn.setType(NameType.INFORMAL);
        } else if (pn.isIndetermined()) {
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
  
  private void applyDoubtfulFlag() throws InterruptedException {
    // all rules below do not apply to unparsable names
    Matcher m = matcherInterruptable(DOUBTFUL, scientificNameCleaned);
    if (!m.find()) {
      pn.setDoubtful(true);
      pn.addWarning(Warnings.UNUSUAL_CHARACTERS);

    } else if (pn.getType().isParsable()){
      m = matcherInterruptable(DOUBTFUL_NULL, scientificNameCleaned);
      if (m.find()) {
        pn.setDoubtful(true);
        pn.addWarning(Warnings.NULL_EPITHET);
      }
    }
  }
  
  private void determineRank() {
    if (pn.getRank().otherOrUnranked()) {
      pn.setRank(RankUtils.inferRank(pn));
    }
    // division is used in botany as a synonym for phylum
    if (DIVISION2PHYLUM.containsKey(pn.getRank()) && pn.getCode() != NomCode.ZOOLOGICAL) {
      pn.setRank(DIVISION2PHYLUM.get(pn.getRank()));
    }
  }
  
  private void determineCode() {
    // no code given?
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

      } else if (!pn.isManuscript() && (
                 (pn.hasBasionymAuthorship() && pn.getBasionymAuthorship().getYear() != null) || (pn.hasCombinationAuthorship() && pn.getCombinationAuthorship().getYear() != null))
      ) {
        // if years are given its a zoological name
        pn.setCode(NomCode.ZOOLOGICAL);

      } else if (!pn.isManuscript() && pn.hasBasionymAuthorship()) {
        if (!pn.hasCombinationAuthorship()) {
          // if only the basionym authorship is given its likely a zoological name!
          pn.setCode(NomCode.ZOOLOGICAL);

        } else {
          // if both the basionym and combination authorship is given its a botanical name!
          pn.setCode(NomCode.BOTANICAL);
        }
      } else if (pn.getNomenclaturalNote() != null && pn.getNomenclaturalNote().contains("illeg")) {
        pn.setCode(NomCode.BOTANICAL);
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
  private boolean parseNormalisedName(String name) throws InterruptedException {
    LOG.debug("Parse normed name string: {}", name);
    Matcher matcher = matcherInterruptable(NAME_PATTERN, name);
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
      setEpithetQualifier(NamePart.INFRASPECIFIC, matcher.group(8));
      setRank(matcher.group(9));
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
      pn.setEpithetQualifier(part, qualifier+".");
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
        // fall through
        case INFRASPECIFIC_NAME:
          if (!rank.isInfraspecific()) {
            break;
          }
        // fall through
        case INFRASUBSPECIFIC_NAME:
          if (!rank.isInfrasubspecific()) {
            break;
          }
        // fall through
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
        || pn.getRank().isSpeciesOrBelow()) { //  && pn.getRank().isRestrictedToCode() != NomCode.CULTIVARS
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
  private void lookForIrregularRankMarker() throws InterruptedException {
    if (pn.getRank().otherOrUnranked()) {
      if (pn.getInfraspecificEpithet() != null) {
        Matcher m = matcherInterruptable(RANK_MARKER_ONLY, pn.getInfraspecificEpithet());
        if (m.find()) {
          // we found a rank marker, make it one
          setRank(pn.getInfraspecificEpithet());
          pn.setInfraspecificEpithet(null);
        }
      }
      if (pn.getSpecificEpithet() != null) {
        Matcher m = matcherInterruptable(RANK_MARKER_ONLY, pn.getSpecificEpithet());
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

  static Authorship parseAuthorship(String ex, String authors, String year) throws InterruptedException {
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
  private static List<String> splitTeam(String team) throws InterruptedException {
    // treat semicolon differently. Single author name can contain a comma now!
    if (team.contains(";")) {
      List<String> authors = Lists.newArrayList();
      for (String a : AUTHORTEAM_SEMI_SPLITTER.split(team)) {
        Matcher m = matcherInterruptable(AUTHOR_INITIAL_SWAP, a);
        if (m.find()) {
          authors.add(normAuthor(m.group(2) + " " + m.group(1), true));
        } else {
          authors.add(normAuthor(a, false));
        }
      }
      return authors;

    } else if(AUTHORTEAM_DELIMITER.matchesAnyOf(team)) {
      return sanitizeAuthors(AUTHORTEAM_SPLITTER.splitToList(team));
  
    } else if (findInterruptable(SPACE_AUTHORTEAM, team)){
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
  private static List<String> sanitizeAuthors(List<String> tokens) throws InterruptedException {
    final List<String> authors = new ArrayList<>();
    final Iterator<String> iter = tokens.iterator();
    while (iter.hasNext()) {
      String author = iter.next();
      if (iter.hasNext() && author.length()>3 && !author.endsWith(".")) {
        String next = iter.next();
        if (findInterruptable(INITIALS, next)) {
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
  private static String normAuthor(String authors, boolean normPunctuation) throws InterruptedException {
    if (normPunctuation) {
      authors = replAllInterruptable(NORM_PUNCTUATIONS, authors, "$1");
    }
    return StringUtils.trimToNull(authors);
  }

  static void logMatcher(Matcher matcher) {
    if (LOG.isDebugEnabled()) {
      int i = -1;
      while (i < matcher.groupCount()) {
        i++;
        LOG.debug("  {}: >{}<", i, matcher.group(i));
      }
    }
  }

}
