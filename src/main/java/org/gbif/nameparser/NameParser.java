package org.gbif.nameparser;

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.Rank;
import org.gbif.utils.file.FileUtils;
import org.gbif.utils.rs.RsGbifOrg;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

public class NameParser {

  private static Logger LOG = LoggerFactory.getLogger(NameParser.class);
  private TreeSet<String> MONOMIALS = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
  private static char[] QUOTES = new char[4];

  static {
    QUOTES[0] = '"';
    QUOTES[1] = '\'';
    QUOTES[2] = '"';
    QUOTES[3] = '\'';
  }

  // name parsing
  protected static final String NAME_LETTERS = "A-ZÏËÖÜÄÉÈČÁÀÆŒ";
  protected static final String name_letters = "a-zïëöüäåéèčáàæœ";
  protected static final String AUTHOR_LETTERS = NAME_LETTERS + "\\p{Lu}"; // upper case unicode letter, not numerical
  // (\W is alphanum)
  protected static final String author_letters = name_letters + "\\p{Ll}"; // lower case unicode letter, not numerical
  // (\W is alphanum)
  protected static final String all_letters_numbers = name_letters + NAME_LETTERS + "0-9";
  protected static final String AUTHOR_PREFIXES =
    "(?:[vV](?:an)(?:[ -](?:den|der) )? ?|von[ -](?:den |der |dem )?|(?:del|Des|De|de|di|Di|da|N)[`' _]|le |d'|D'|de la |Mac|Mc|Le|St\\.? ?|Ou|O')";
  protected static final String AUTHOR = "(?:" +
                                         // author initials
                                         "(?:" + "(?:[" + AUTHOR_LETTERS + "]{1,3}\\.?[ -]?){0,3}" +
                                         // or full first name
                                         "|[" + AUTHOR_LETTERS + "][" + author_letters + "?]{3,}" + " )?" +
                                         // common prefixes
                                         AUTHOR_PREFIXES + "?" +
                                         // only allow v. in front of Capital Authornames - if included in AUTHOR_PREFIXES parseIgnoreAuthors fails
                                         "(?:v\\. )?" +
                                         // regular author name
                                         "[" + AUTHOR_LETTERS + "]+[" + author_letters + "?]*\\.?" +
                                         // potential double names, e.g. Solms-Laub.
                                         "(?:(?:[- ](?:de|da|du)?[- ]?)[" + AUTHOR_LETTERS + "]+[" + author_letters
                                         + "?]*\\.?)?" +
                                         // common name suffices (ms=manuscript, not yet published)
                                         "(?: ?(?:f|fil|j|jr|jun|junior|sr|sen|senior|ms)\\.?)?" +
                                         // at last there might be 2 well known sanction authors for fungus, see POR-2454
                                         "(?: *: *(?:Pers|Fr)\\.?)?" +
                                         ")";
  protected static final String AUTHOR_TEAM =
    AUTHOR + "?(?:(?: ?ex\\.? | & | et | in |, ?|; ?|\\.)(?:" + AUTHOR + "|al\\.?))*";
  protected static final Pattern AUTHOR_TEAM_PATTERN = Pattern.compile("^" + AUTHOR_TEAM + "$");
  protected static final String YEAR = "[12][0-9][0-9][0-9?][abcdh?]?(?:[/-][0-9]{1,4})?";
  // protected static final String YEAR_RANGE = YEAR+"(?: ?-? ?"+YEAR+")?";

  private static final String RANK_MARKER_ALL =
    "(notho)? *(" + StringUtils.join(Rank.RANK_MARKER_MAP.keySet(), "|") + ")\\.?";
  private static final Pattern RANK_MARKER = Pattern.compile("^" + RANK_MARKER_ALL + "$");
  private static final Pattern RANK_MARKER_AT_END = Pattern.compile(" " + RANK_MARKER_ALL + "$");
  protected static final String RANK_MARKER_SPECIES =
    "(?:notho)?(?:" + StringUtils.join(Rank.RANK_MARKER_MAP_INFRASPECIFIC.keySet(), "|") + "|agg)\\.?";

  protected static final String EPHITHET_PREFIXES = "van|novae";
  protected static final String EPHITHET =
    "(?:[0-9]+-)?" + "(?:(?:" + EPHITHET_PREFIXES + ") [a-z])?" + "[" + name_letters + "+-]{1,}(?<! d)[" + name_letters
    + "](?<!\\bex)";
  protected static final String MONOMIAL =
    "[" + NAME_LETTERS + "](?:\\.|[" + name_letters + "]+)(?:-[" + NAME_LETTERS + "]?[" + name_letters + "]+)?";
  protected static final String INFRAGENERIC =
    "(?:" + "\\( ?([" + NAME_LETTERS + "][" + name_letters + "-]+) ?\\)" + "|" + "(" + StringUtils
      .join(Rank.RANK_MARKER_MAP_INFRAGENERIC.keySet(), "|") + ")\\.? ?([" + NAME_LETTERS + "][" + name_letters + "-]+)"
    + ")";
  // this is only used to detect whether we have a hybrid formula. If not, all markers are normalised
  public static final String HYBRID_MARKER = "×";
  public static final Pattern HYBRID_FORMULA_PATTERN = Pattern.compile(" " + HYBRID_MARKER + " ");
  protected static final Pattern CULTIVAR =
    Pattern.compile("(?: cv\\.? ?)?[\"'] ?((?:[" + NAME_LETTERS + "]?[" + name_letters + "]+[- ]?){1,3}) ?[\"']");

  private static final Pattern STRAIN = Pattern.compile("([a-z]\\.?) +([A-Z]+ *[0-9]+T?)$");
  // this is only used to detect whether we have a virus name
  public static final Pattern IS_VIRUS_PATTERN =
    Pattern.compile("(\\b(bacterio)?phage(s)?\\b|virus(es)?\\b|\\bictv$)", CASE_INSENSITIVE);
  private static final Pattern IS_VIRUS_PATTERN_POSTFAIL = Pattern.compile("(\\b(vector)\\b)", CASE_INSENSITIVE);
  // spots a Candidatus bacterial name
  private static final String CANDIDATUS = "(Candidatus\\s|Ca\\.)\\s*";
  private static final Pattern IS_CANDIDATUS_PATTERN = Pattern.compile(CANDIDATUS, CASE_INSENSITIVE);
  private static final Pattern IS_CANDIDATUS_QUOTE_PATTERN = Pattern.compile("\"" + CANDIDATUS + "(.+)\"", CASE_INSENSITIVE);
  // name normalising
  private static final String SENSU =
    "(s\\.(?:l\\.|str\\.)|sensu\\s+(?:latu|strictu?)|(sec|sensu|auct|non)((\\.|\\s)(.*))?)";
  private static final Pattern EXTRACT_SENSU = Pattern.compile(",?\\s+(" + SENSU + "$|\\(" + SENSU + "\\))");
  protected static final Pattern EXTRACT_NOMSTATUS = Pattern.compile(
    "(?:, ?| )\\(?((?:comb|gen|sp|var)?[\\. ] ?nov\\.?(?: ?ined\\.?)?|ined\\.|nom(?:\\s+|\\.\\s*|en\\s+)(?:utiq(?:ue\\s+|\\.\\s*))?(?:ambig|alter|alt|correct|cons|dubium|dub|herb|illeg|invalid|inval|negatum|neg|novum|nov|nudum|nud|oblitum|obl|praeoccup|prov|prot|transf|superfl|super|rejic|rej)\\.?(?:\\s+(?:prop|proposed)\\.?)?)\\)?");
  private static final Pattern EXTRACT_REMARKS = Pattern.compile("\\s+(anon\\.?)(\\s.+)?$");
  private static final Pattern EXTRACT_YEAR = Pattern.compile("(" + YEAR + "\\s*\\)?)");
  private static final Pattern COMMA_BEFORE_YEAR = Pattern.compile("(,+|[^0-9\\(\\[\"])\\s*(\\d{3})");
  private static final Pattern REPLACE_QUOTES = Pattern.compile("(^\\s*[\"',]+)|([\"',]+\\s*$)");
  private static final Pattern NORM_QUOTES = Pattern.compile("([\"'`´]+)");
  private static final Pattern NORM_UPPERCASE_WORDS = Pattern.compile("\\b(\\p{Lu})(\\p{Lu}{2,})\\b");
  private static final Pattern NORM_WHITESPACE = Pattern.compile("\\s+");
  private static final Pattern NORM_NO_SQUARE_BRACKETS = Pattern.compile("\\[(.*?)\\]");
  private static final Pattern NORM_BRACKETS_OPEN = Pattern.compile("([{(\\[])\\s*,?");
  private static final Pattern NORM_BRACKETS_CLOSE = Pattern.compile(",?\\s*([})\\]])");
  private static final Pattern NORM_BRACKETS_OPEN_STRONG = Pattern.compile("( ?[{(\\[] ?)+");
  private static final Pattern NORM_BRACKETS_CLOSE_STRONG = Pattern.compile("( ?[})\\]] ?)+");
  private static final Pattern NORM_AND = Pattern.compile(" (and|et|und|&amp;) ");
  private static final Pattern NORM_ET_AL = Pattern.compile("& al\\.?");
  private static final Pattern NORM_AMPERSAND_WS = Pattern.compile("&");
  private static final Pattern NORM_HYPHENS = Pattern.compile("\\s*-\\s*");
  private static final Pattern NORM_SUBGENUS =
    Pattern.compile("(" + MONOMIAL + ") (" + MONOMIAL + ") ([" + name_letters + "+-]{5,})");
  private static final Pattern NO_Q_MARKS = Pattern.compile("([" + author_letters + "])\\?+");
  private static final Pattern NORM_COMMAS = Pattern.compile("\\s*,+");
  private static final String AUTHOR_STRONG = "[" + AUTHOR_LETTERS + "]+[" + author_letters + "]{2,}\\.?";
  // TODO: this next regex gets real slow with long list of authors - needs fixing !!!
  private static final Pattern NORM_ORIG_AUTH =
    Pattern.compile("(?<=[ \\(])(" + AUTHOR_TEAM + ") ?\\( ?(" + YEAR + ")\\)");
  private static final Pattern NORM_ORIG_AUTH2 = Pattern.compile("\\((" + AUTHOR_TEAM + ")\\) ?,? ?(" + YEAR + ")");
  private static final Pattern NORM_IMPRINT_YEAR =
    Pattern.compile("(" + YEAR + ")\\s*(?:\\(\"?[\\s0-9-_,?]+\"?\\)|\\[\"?[0-9 -,]+\"?\\]|\"[0-9 -,]+\")");
  private static final Pattern NORM_HYBRIDS_GENUS = Pattern.compile("^\\s*[+×xX]\\s*([" + NAME_LETTERS + "])");
  private static final Pattern NORM_HYBRIDS_EPITH =
    Pattern.compile("^\\s*(×?" + MONOMIAL + ")\\s+(?:×|[xX]\\s)\\s*(" + EPHITHET + ")");
  private static final Pattern NORM_HYBRIDS_FORM = Pattern.compile(" [×xX] ");
  private static final Pattern NORM_INDET = Pattern.compile("((^| )(undet|indet|aff|cf)[#!?\\.]?)+(?![a-z])");
  private static final Pattern NORM_DOTS = Pattern.compile("(^\\s*[" + NAME_LETTERS + "]|" + RANK_MARKER_ALL + ")\\.");
  private static final Pattern NORM_TF_GENUS =
    Pattern.compile("^([" + NAME_LETTERS + "])\\(([" + name_letters + "-]+)\\)\\.? ");
  private static final Pattern NORM_IN_BIB = Pattern.compile("( in .+$| ?: ?[0-9]+)", CASE_INSENSITIVE);
  private static final Pattern NORM_PREFIXES = Pattern
    .compile("^(sub)?(fossil|" + StringUtils.join(Rank.RANK_MARKER_MAP_SUPRAGENERIC.keySet(), "|") + ")\\.?\\s+",
      CASE_INSENSITIVE);
  private static final Pattern NORM_SUFFIXES =
    Pattern.compile("[,;:]? (sp|anon|spp|hort|ms|[a-zA-Z][0-9])?\\.? *$", CASE_INSENSITIVE);
  // removed not|indetermin[a-z]+
  private static final Pattern NO_LETTERS = Pattern.compile("^[^a-zA-Z]+$");
  private static final Pattern BLACKLISTED = Pattern.compile(
    "\\b(unnamed|mixed|unassigned|unallocated|unplaced|undetermined|unclassified|uncultured|unknown|unspecified|uncertain|incertae sedis|not assigned|awaiting allocation)\\b",
    CASE_INSENSITIVE);
  private static final Pattern DOUBTFUL =
    Pattern.compile("^[" + AUTHOR_LETTERS + author_letters + HYBRID_MARKER + "&*+ ,.()/'`´0-9-]+$");
  private static final String[] badNameParts = {"author", "unknown", "unassigned", "not_stated"};
  private static final Pattern XML_TAGS = Pattern.compile("< */? *[a-zA-Z] *>");
  private static final Pattern FIRST_WORD = Pattern.compile("^([×xX]\\s+)?([×x][A-Z])?([a-zA-Z])([a-zA-Z]+) ");
  private static final String WEIRD_CHARS = "[§$%/#+!;:_|\"=*]";
  private static final Pattern NORM_WEIRD_CHARS = Pattern.compile(WEIRD_CHARS);

  private static final Pattern COMB_BAS_AUTHOR_SWAP = Pattern.compile(
    // #1 comb authorteam
    "( " + AUTHOR_TEAM + ")" +
    // #2 comb year
    "(?:( ?,? ?" + YEAR + "))?" +
    // #3 basionym authors
    " ?\\(( ?" + AUTHOR_TEAM + ")" +
    // #4 basionym year
    "( ?,? ?" + YEAR + ")?" + "\\)");

  // main name matcher
  public static final Pattern CANON_NAME_IGNORE_AUTHORS = Pattern.compile("^" +
                                                                          // #1 genus/monomial
                                                                          "(×?" + MONOMIAL + ")" +
                                                                          // #2 or #4 subgenus/section with #3 infrageneric rank marker
                                                                          "(?:(?<!ceae) " + INFRAGENERIC + ")?" +
                                                                          // catch author name prefixes just to ignore them so they dont become wrong epithets
                                                                          "(?: " + AUTHOR_PREFIXES + ")?" +
                                                                          // #5 species
                                                                          "(?: (×?" + EPHITHET + "))?" +
                                                                          // catch author name prefixes just to ignore them so they dont become wrong epithets
                                                                          "(?: " + AUTHOR_PREFIXES + ")?" + "(?:" +
                                                                          // either directly a infraspecific epitheton or a author but then mandate rank marker
                                                                          "(?:" +
                                                                          // anything in between
                                                                          ".*" +
                                                                          // #6 infraspecies rank
                                                                          "( " + RANK_MARKER_SPECIES + "[ .])" +
                                                                          // #7 infraspecies epitheton
                                                                          "(×?" + EPHITHET + ")" + ")" + "|" +
                                                                          // #8 infraspecies epitheton
                                                                          " (×?" + EPHITHET + ")" + ")?");

  public static final Pattern NAME_PATTERN = Pattern.compile("^" +
                                                             // #1 genus/monomial
                                                             "(×?" + MONOMIAL + ")" +
                                                             // #2 or #4 subgenus/section with #3 infrageneric rank marker
                                                             "(?:(?<!ceae) " + INFRAGENERIC + ")?" +
                                                             // #5 species
                                                             "(?: (×?" + EPHITHET + "))?" + "(?:" +

                                                             "(?:" +
                                                             // #6 strip out intermediate, irrelevant authors or infraspecific ranks in case of quadrinomials
                                                             "( .*?)?" +
                                                             // #7 infraspecies rank
                                                             "( " + RANK_MARKER_SPECIES + ")" + ")?" +

                                                             // #8 infraspecies epitheton
                                                             "(?: (×?\"?" + EPHITHET + "\"?))" + ")?" +
                                                             // #9 entire authorship incl basionyms and year
                                                             "(,?" + "(?: ?\\(" +
                                                             // #10 basionym authors
                                                             "(" + AUTHOR_TEAM + ")?" +
                                                             // #11 basionym year
                                                             ",?( ?" + YEAR + ")?" + "\\))?" +
                                                             // #12 authors
                                                             "( " + AUTHOR_TEAM + ")?" +
                                                             // #13 year with or without brackets
                                                             "(?: ?\\(?,? ?(" + YEAR + ")\\)?)?" + ")" + "$");

  private static String cutoffBadSuffices(String name) {
    boolean done = false;
    String lowercase = name.toLowerCase();
    int cuttoff = lowercase.length();
    while (!done) {
      done = true;
      for (String bad : badNameParts) {
        if (lowercase.endsWith(" " + bad)) {
          int remove = bad.length() + 1;
          cuttoff -= remove;
          lowercase = lowercase.substring(0, cuttoff);
          done = false;
        }
      }
    }
    if (cuttoff != name.length()) {
      name = name.substring(0, cuttoff);
    }
    return name;
  }

  /**
   * A very optimistic cleaning intended for names potentially very very dirty
   *
   * @param name To normalize
   *
   * @return The normalized name
   */
  protected static String cleanStrong(String name) {
    if (name != null) {
      // test for known bad suffixes like in Palythoa texaensis author unknown
      name = cutoffBadSuffices(name);

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
      Matcher m = FIRST_WORD.matcher(name);
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
   * - unescapes unicode chars \\uhhhh, \\nnn, \xhh
   * - pads whitespace around &
   * - adds whitespace after dots following a genus abbreviation or rank marker
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
  public static String normalize(String name) {
    if (name == null) {
      return null;
    }
    name = org.gbif.utils.text.StringUtils.unescapeUnicodeChars(name);

    // normalise usage of dots after abbreviated genus and rank marker
    Matcher m = NORM_DOTS.matcher(name);
    if (m.find()) {
      name = m.replaceAll("$1. ");
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
  protected static String normalizeStrong(String name) {
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
    m = EXTRACT_YEAR.matcher(name);
    if (m.find() && name.length() < 80) {
      m = NORM_ORIG_AUTH.matcher(name);
      if (m.find()) {
        name = m.replaceAll("($1 $2)");
      }
      m = NORM_ORIG_AUTH2.matcher(name);
      if (m.find()) {
        name = m.replaceAll("($1 $2)");
      }
    }

    // replace square brackets, keeping content (or better remove all within?)
    name = NORM_NO_SQUARE_BRACKETS.matcher(name).replaceAll(" $1 ");
    // replace different kind of brackets with ()
    name = NORM_BRACKETS_OPEN_STRONG.matcher(name).replaceAll(" (");
    name = NORM_BRACKETS_CLOSE_STRONG.matcher(name).replaceAll(") ");
    // normalise different usages of ampersand, and, et &amp;
    name = NORM_AND.matcher(name).replaceAll(" & ");
    // but keep "et al." instead of "& al."
    name = NORM_ET_AL.matcher(name).replaceAll(" et al.");

    // // add commans between authors in space delimited list
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
  protected static String preClean(String name) {
    // unescape unicode
    name = org.gbif.utils.text.StringUtils.unescapeUnicodeChars(name);
    // TODO: unescape html entities
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
    return StringUtils.trimToEmpty(name);
  }

  private static void setCanonicalInfraSpecies(ParsedName pn, String epi) {
    if (epi == null || epi.equalsIgnoreCase("sec") || epi.equalsIgnoreCase("sensu")) {
      return;
    }
    pn.setInfraSpecificEpithet(StringUtils.trimToNull(epi));
  }

  public void addMonomials(Set<String> monomials) {
    MONOMIALS.addAll(monomials);
  }

  /**
   * 2 letter epitheta can also be author prefixes - check that programmatically, not in regex
   */
  private void checkEpithetVsAuthorPrefx(ParsedName cn) {
    if (cn.getRankMarker() == null) {
      if (cn.getInfraSpecificEpithet() != null) {
        // might be subspecies without rank marker
        // or short authorship prefix in epithet. test
        String extendedAuthor = cn.getInfraSpecificEpithet() + " " + cn.getAuthorship();
        Matcher m = AUTHOR_TEAM_PATTERN.matcher(extendedAuthor);
        if (m.find()) {
          // matches author. Prefer that
          LOG.debug("use infraspecific epithet as author prefix");
          cn.setInfraSpecificEpithet(null);
          cn.setAuthorship(extendedAuthor);
        }
      } else {
        // might be monomial with the author prefix erroneously taken as the species epithet
        String extendedAuthor = cn.getSpecificEpithet() + " " + cn.getAuthorship();
        Matcher m = AUTHOR_TEAM_PATTERN.matcher(extendedAuthor);
        if (m.find()) {
          // matches author. Prefer that
          LOG.debug("use specific epithet as author prefix");
          cn.setSpecificEpithet(null);
          cn.setAuthorship(extendedAuthor);
        }
      }
    }
  }

  public Set<String> getMonomials() {
    return MONOMIALS;
  }

  /**
   * if no rank marker is set yet inspect epitheta for wrongly placed rank markers and modify parsed name accordingly.
   * This is sometimes the case for informal names like: Coccyzus americanus ssp.
   *
   * @param cn the already parsed name
   */
  private void lookForIrregularRankMarker(ParsedName cn) {
    if (cn.getRankMarker() == null) {
      if (cn.getInfraSpecificEpithet() != null) {
        Matcher m = RANK_MARKER.matcher(cn.getInfraSpecificEpithet());
        if (m.find()) {
          // we found a rank marker, make it one
          cn.setRankMarker(cn.getInfraSpecificEpithet());
          cn.setInfraSpecificEpithet(null);
        }
      } else if (cn.getSpecificEpithet() != null) {
        Matcher m = RANK_MARKER.matcher(cn.getSpecificEpithet());
        if (m.find()) {
          // we found a rank marker, make it one
          cn.setRankMarker(cn.getSpecificEpithet());
          cn.setSpecificEpithet(null);
        }
      }
    }
  }

  /**
   * Fully parse the supplied name also trying to extract authorships, a conceptual sec reference, remarks or notes
   * on the nomenclatural status. In some cases the authorship parsing proves impossible and this nameparser will
   * return null.
   *
   * For strings which are no scientific names and scientific names that cannot be expressed by the ParsedName class
   * the parser will throw an UnparsableException with a given NameType and the original, unparsed name. This is the
   * case for all virus names and proper hybrid formulas, so make sure you catch and process this exception.
   */
  public ParsedName parse(final String scientificName) throws UnparsableException {
    if (Strings.isNullOrEmpty(scientificName)) {
      throw new UnparsableException(NameType.BLACKLISTED, scientificName);
    }
    long start = 0;
    if (LOG.isDebugEnabled()) {
      start = System.currentTimeMillis();
    }

    ParsedName pn = new ParsedName();
    pn.setScientificName(scientificName);

    String name = scientificName;

    // before any cleaning test for properly quoted candidate names
    Matcher m = IS_CANDIDATUS_QUOTE_PATTERN.matcher(scientificName);
    if (m.find()) {
      pn.setType(NameType.CANDIDATUS);
      name = m.replaceFirst(m.group(2));
    }

    // clean name, removing seriously wrong things
    name = preClean(name);

    // parse out species/strain names with numbers found in Genebank/EBI names, e.g. Advenella kashmirensis W13003
    m = STRAIN.matcher(name);
    if (m.find()) {
      name = m.replaceFirst(m.group(1));
      pn.setType(NameType.INFORMAL);
      pn.setStrain(m.group(2));
      LOG.debug("Strain: {}", m.group(2));
    }

    // normalise name
    name = normalize(name);
    if (Strings.isNullOrEmpty(name)) {
      throw new UnparsableException(NameType.BLACKLISTED, scientificName);
    }

    // parse cultivar names first BEFORE we strongly normalize
    // this will potentially remove quotes needed to find cultivar names
    m = CULTIVAR.matcher(name);
    if (m.find()) {
      pn.setCultivarEpithet(m.group(1));
      name = m.replaceFirst(" ");
      pn.setType(NameType.CULTIVAR);
      pn.setRank(Rank.CULTIVAR);
      LOG.debug("Cultivar: {}", pn.getCultivarEpithet());
    }

    // detect unparsable names
    m = BLACKLISTED.matcher(name);
    if (m.find()) {
      throw new UnparsableException(NameType.BLACKLISTED, scientificName);
    }
    // name without any latin char letter at all?
    m = NO_LETTERS.matcher(name);
    if (m.find()) {
      throw new UnparsableException(NameType.BLACKLISTED, scientificName);
    }
    m = HYBRID_FORMULA_PATTERN.matcher(name);
    if (m.find()) {
      throw new UnparsableException(NameType.HYBRID, scientificName);
    }
    m = IS_VIRUS_PATTERN.matcher(name);
    if (m.find()) {
      throw new UnparsableException(NameType.VIRUS, scientificName);
    }

    m = IS_CANDIDATUS_PATTERN.matcher(name);
    if (m.find()) {
      pn.setType(NameType.CANDIDATUS);
      name = m.replaceFirst("");
    }

    // extract nom.illeg. and other nomen status notes
    m = EXTRACT_NOMSTATUS.matcher(name);
    if (m.find()) {
      pn.setNomStatus(StringUtils.trimToNull(m.group(1)));
      name = m.replaceFirst("");
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

    // check for indets
    m = RANK_MARKER_AT_END.matcher(name);
    // f. is a marker for forms, but more often also found in authorships as "filius" - son of.
    // so ignore those
    if (m.find() && !(name.endsWith(" f.") || name.endsWith(" f"))) {
      pn.setType(NameType.INFORMAL);
      pn.setRankMarker(m.group(2));
      name = m.replaceAll(" ");
    }
    m = NORM_INDET.matcher(name);
    if (m.find()) {
      pn.setType(NameType.INFORMAL);
      name = m.replaceAll(" ");
    }

    name = normalizeStrong(name);
    if (Strings.isNullOrEmpty(name)) {
      throw new UnparsableException(NameType.DOUBTFUL, scientificName);
    }

    // remember current rank for later reuse
    Rank origRank = pn.getRank();

    // try regular parsing
    int passNo = 1;
    boolean parsed = parseNormalisedName(pn, name);
    if (!parsed) {
      // try again with stronger normalisation, maybe that helps...
      LOG.debug("Can't parse, use dirty normalizer");
      final String deDirtedName = cleanStrong(name);
      parsed = parseNormalisedName(pn, deDirtedName);
      passNo++;
      if (!parsed) {
        LOG.debug("Still can't parse, try to ignore authors");
        // try to parse canonical alone ignoring authorship as last resort
        parsed = parseNormalisedNameIgnoreAuthors(pn, deDirtedName);
        passNo++;
        pn.setAuthorsParsed(false);
        if (!parsed) {
          // we just cant parse this one
          // try to spot a virus name once we know its not a scientific name
          m = IS_VIRUS_PATTERN_POSTFAIL.matcher(name);
          if (m.find()) {
            throw new UnparsableException(NameType.VIRUS, scientificName);
          }

          throw new UnparsableException(NameType.DOUBTFUL, scientificName);
        }
      }
    }
    if (origRank != null) {
      pn.setRank(origRank);
    }

    // primarily make sure again we haven't parsed a non name
    postAssertParsing(pn, scientificName, name);

    // determine name type if not yet assigned
    if (pn.getType() == null) {
      // a doubtful name?
      m = DOUBTFUL.matcher(scientificName);
      if (!m.find()) {
        pn.setType(NameType.DOUBTFUL);
      } else {
        // a wellformed, code compliant name?
        if (scientificName.equals(pn.canonicalNameComplete())) {
          pn.setType(NameType.WELLFORMED);
        } else {
          pn.setType(NameType.SCINAME);
        }
      }
    }

    LOG.debug("Parsing time: {}", (System.currentTimeMillis() - start));

    return pn;
  }

  private void postAssertParsing(ParsedName pn, final String rawName, final String normedName)
    throws UnparsableException {
    // if we only match a monomial in the 3rd pass its suspicious
    if (pn.getGenusOrAbove() != null && !pn.isBinomial()) {
      // a monomial match, but it was a lower case name - doubtful at least!
      if (Character.isLowerCase(normedName.charAt(0))) {
        throw new UnparsableException(NameType.DOUBTFUL, rawName);
      }
    }
  }

  private boolean parseNormalisedName(ParsedName cn, String scientificName) {
    LOG.debug("Parse normed name string: {}", scientificName);
    Matcher matcher = NAME_PATTERN.matcher(scientificName);
    if (matcher.find() && matcher.group(0).equals(scientificName)) {
      if (LOG.isDebugEnabled()) {
        logMatcher(matcher);
      }
      cn.setGenusOrAbove(StringUtils.trimToNull(matcher.group(1)));
      boolean bracketSubrankFound = false;
      if (matcher.group(2) != null) {
        bracketSubrankFound = true;
        cn.setInfraGeneric(StringUtils.trimToNull(matcher.group(2)));
      } else if (matcher.group(4) != null) {
        String rank = StringUtils.trimToNull(matcher.group(3));
        if (!rank.endsWith(".")) {
          rank = rank + ".";
        }
        cn.setRankMarker(rank);
        cn.setInfraGeneric(StringUtils.trimToNull(matcher.group(4)));
      }
      cn.setSpecificEpithet(StringUtils.trimToNull(matcher.group(5)));
      // #6 is filling authors or ranks in the middle not stored in ParsedName
      if (matcher.group(7) != null && matcher.group(7).length() > 1) {
        cn.setRankMarker(StringUtils.trimToNull(matcher.group(7)));
      }
      cn.setInfraSpecificEpithet(StringUtils.trimToNull(matcher.group(8)));

      // #9 is entire authorship, not stored in ParsedName
      cn.setBracketAuthorship(StringUtils.trimToNull(matcher.group(10)));
      if (bracketSubrankFound && cn.getBracketAuthorship() == null && cn.getSpecificEpithet() == null && !MONOMIALS
        .contains(cn.getInfraGeneric())) {
        // rather an author than a infrageneric rank. Swap
        cn.setBracketAuthorship(cn.getInfraGeneric());
        cn.setInfraGeneric(null);
        LOG.debug("swapped subrank with bracket author: {}", cn.getBracketAuthorship());
      }
      if (matcher.group(11) != null && matcher.group(11).length() > 2) {
        String yearAsString = matcher.group(11).trim();
        cn.setBracketYear(yearAsString);
      }
      cn.setAuthorship(StringUtils.trimToNull(matcher.group(12)));
      if (matcher.group(13) != null && matcher.group(13).length() > 2) {
        String yearAsString = matcher.group(13).trim();
        cn.setYear(yearAsString);
      }

      // make sure (infra)specific epithet is not a rank marker!
      lookForIrregularRankMarker(cn);
      // 2 letter epitheta can also be author prefixes - check that programmatically, not in regex
      checkEpithetVsAuthorPrefx(cn);
      return true;
    }
    return false;
  }

  private void logMatcher(Matcher matcher) {
    int i = -1;
    while (i < matcher.groupCount()) {
      i++;
      LOG.debug("  {}: >{}<", i, matcher.group(i));
    }
  }

  private boolean parseNormalisedNameIgnoreAuthors(ParsedName cn, String scientificName) {
    LOG.debug("Parse normed name string ignoring authors: {}", scientificName);

    // match for canonical
    Matcher matcher = CANON_NAME_IGNORE_AUTHORS.matcher(scientificName);
    boolean matchFound = matcher.find();
    if (matchFound) {
      if (LOG.isDebugEnabled()) {
        logMatcher(matcher);
      }
      cn.setGenusOrAbove(StringUtils.trimToNull(matcher.group(1)));
      if (matcher.group(2) != null) {
        // subrank in paranthesis. Not an author?
        cn.setInfraGeneric(StringUtils.trimToNull(matcher.group(2)));
        if (!MONOMIALS.contains(cn.getInfraGeneric())) {
          // rather an author...
          cn.setInfraGeneric(null);
        }
      } else if (matcher.group(4) != null) {
        // infrageneric with rank indicator given
        String rank = StringUtils.trimToNull(matcher.group(3));
        cn.setRankMarker(rank);
        cn.setInfraGeneric(StringUtils.trimToNull(matcher.group(4)));
      }
      cn.setSpecificEpithet(StringUtils.trimToNull(matcher.group(5)));
      if (matcher.group(6) != null && matcher.group(6).length() > 1) {
        cn.setRankMarker(StringUtils.trimToNull(matcher.group(6)));
      }
      if (matcher.group(7) != null && matcher.group(7).length() >= 2) {
        setCanonicalInfraSpecies(cn, matcher.group(7));
      } else {
        setCanonicalInfraSpecies(cn, matcher.group(8));
      }

      // make sure (infra)specific epithet is not a rank marker!
      lookForIrregularRankMarker(cn);

      return true;
    }
    return false;
  }

  /**
   * parses the name without authorship and returns the ParsedName.canonicalName() string
   */
  public String parseToCanonical(String scientificName) {
    if (Strings.isNullOrEmpty(scientificName)) {
      return null;
    }
    try {
      ParsedName pn = parse(scientificName);
      if (pn != null) {
        return pn.canonicalName();
      }
    } catch (UnparsableException e) {
      LOG.warn("Unparsable name " + scientificName + " >>> " + e.getMessage());
    }
    return null;
  }

  /**
   * Read generic and suprageneric names from rs.gbif.org dictionaries and feed them into nameparser for monomial
   * references.
   * Used to better disambiguate subgenera/genera and authors
   */
  public void readMonomialsRsGbifOrg() {
    MONOMIALS.clear();
    // add suprageneric names
    InputStream in;
    Set<String> names;
    try {
      in = RsGbifOrg.authorityUrl(RsGbifOrg.FILENAME_SUPRAGENERIC).openStream();
      names = FileUtils.streamToSet(in);
      addMonomials(names);
      LOG.debug("Loaded " + names.size() + " suprageneric names from rs.gbif.org into NameParser");
    } catch (IOException e) {
      LOG.warn(
        "Couldn't read suprageneric names dictionary from rs.gbif.org to feed into NameParser: " + e.getMessage());
    } catch (Exception e) {
      LOG.warn("Error supplying NameParser with suprageneric names from rs.gbif.org", e);
    }
    // add genera
    try {
      in = RsGbifOrg.authorityUrl(RsGbifOrg.FILENAME_GENERA).openStream();
      names = FileUtils.streamToSet(in);
      addMonomials(names);
      LOG.debug("Loaded " + names.size() + " generic names from rs.gbif.org into NameParser");
    } catch (IOException e) {
      LOG.warn("Couldn't read generic names dictionary from rs.gbif.org to feed into NameParser: " + e.getMessage());
    } catch (Exception e) {
      LOG.warn("Error supplying NameParser with generic names from rs.gbif.org", e);
    }
  }

  /**
   * Provide a set of case insensitive words that indicate a true monomial to detect a taxonomic subrank instead of an
   * author.
   * For example in "Chordata Vertebrata"
   */
  public void setMonomials(Set<String> monomials) {
    MONOMIALS.clear();
    MONOMIALS.addAll(monomials);
  }

}
