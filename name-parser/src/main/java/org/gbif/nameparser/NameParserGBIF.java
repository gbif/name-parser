package org.gbif.nameparser;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.gbif.nameparser.api.*;
import org.gbif.nameparser.util.RankUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static org.gbif.nameparser.NormalisedNameParser.*;

/**
 * The default GBIF name parser build on regular expressions.
 * In order to avoid long running regex matches it runs the core parsing in a background threadpool
 * which is shared across all instances of the parser.
 *
 * Make sure to reuse the instance as much as possible and don't forget to close it for the threads to shutdown properly.
 */
public class NameParserGBIF implements NameParser {

  private static Logger LOG = LoggerFactory.getLogger(NameParserGBIF.class);
  private final NormalisedNameParser nnParser;
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
  private static final Pattern CULTIVAR_GROUP = Pattern.compile("(?<!^)\\b[\"']?((?:[" + NAME_LETTERS + "][" + name_letters + "]{2,}[- ]?){1,3})[\"']? (Group|Hybrids|Sort|[Gg]rex)\\b");

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
  private static final Pattern IS_OTU_PATTERN = Pattern.compile("^(BOLD:?[0-9A-Z]+$|SH[0-9]+\\.[0-9]+FU)", CASE_INSENSITIVE);
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
  private static final Pattern REPLACE_QUOTES = Pattern.compile("(^\\s*[\"',]+)|([\"',]+\\s*$)");

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
  // TODO: this next regex gets real slow with long list of authors - needs fixing !!!
  private static final Pattern NORM_ORIG_AUTH =
    Pattern.compile("(?<=[ \\(])(" + AUTHORSHIP + ") ?\\( ?(" + YEAR_LOOSE + ")\\)");
  private static final Pattern NORM_ORIG_AUTH2 = Pattern.compile("\\((" + AUTHORSHIP + ")\\) ?,? ?(" + YEAR_LOOSE + ")");
  private static final Pattern NORM_IMPRINT_YEAR =
    Pattern.compile("(" + YEAR_LOOSE + ")\\s*(?:\\(\"?[\\s0-9-_,?]+\"?\\)|\\[\"?[0-9 -,]+\"?\\]|\"[0-9 -,]+\")");
  // √ó is an utf garbaged version of the hybrid cross found in IPNI. See http://dev.gbif.org/issues/browse/POR-3081
  private static final Pattern NORM_HYBRIDS_GENUS = Pattern.compile("^\\s*(?:[+×xX]|√ó)\\s*([" + NAME_LETTERS + "])");
  private static final Pattern NORM_HYBRIDS_EPITH = Pattern.compile("^\\s*(×?" + MONOMIAL + ")\\s+(?:×|√ó|[xX]\\s)\\s*(" + EPHITHET + ")");
  private static final Pattern NORM_HYBRIDS_FORM = Pattern.compile(" ([×xX]|√ó) ");
  private static final Pattern NORM_INDET = Pattern.compile("((^| )(undet|indet|aff|cf)[#!?\\.]?)+(?![a-z])");
  private static final Pattern NORM_DOTS = Pattern.compile("(^\\s*[" + NAME_LETTERS + "]|" + RANK_MARKER_ALL + "|"+AUTHOR_CAP+"*)\\.");
  private static final Pattern NORM_TF_GENUS =
    Pattern.compile("^([" + NAME_LETTERS + "])\\(([" + name_letters + "-]+)\\)\\.? ");
  private static final Pattern NORM_IN_COMMA = Pattern.compile(", in ", CASE_INSENSITIVE);
  private static final Pattern NORM_IN_BIB = Pattern.compile("( in .+$| ?: ?[0-9]+)", CASE_INSENSITIVE);
  private static final Pattern NORM_PREFIXES = Pattern.compile("^(sub)?(fossil|" +
      StringUtils.join(RankUtils.RANK_MARKER_MAP_SUPRAGENERIC.keySet(), "|") + ")\\.?\\s+", CASE_INSENSITIVE);
  private static final Pattern NORM_SUFFIXES =
    Pattern.compile("[,;:]? (sp|anon|spp|hort|ms|&|[a-zA-Z][0-9])?\\.? *$", CASE_INSENSITIVE);
  // removed not|indetermin[a-z]+
  private static final Pattern NO_LETTERS = Pattern.compile("^[^a-zA-Z]+$");
  private static final String PLACEHOLDER_AUTHOR = "(?:unknown|unspecified|uncertain|\\?)";
  private static final Pattern REMOVE_PLACEHOLDER_AUTHOR = Pattern.compile("\\b"+PLACEHOLDER_AUTHOR+"[, ] ?(" + YEAR_LOOSE + ")$", CASE_INSENSITIVE);
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

  /**
   * The default name parser without an explicit monomials list using the default timeout of 1s for parsing.
   */
  public NameParserGBIF() {
    this.nnParser= new NormalisedNameParser(1000);  // max default parsing time is one second;
  }

  /**
   * The default name parser without an explicit monomials list using the given timeout in milliseconds for parsing.
   */
  public NameParserGBIF(long timeout) {
    this.nnParser= new NormalisedNameParser(timeout / 2);
  }

  static ParsedName unparsable(NameType type, String name) throws UnparsableNameException {
    throw new UnparsableNameException(type, name);
  }

  public ParsedName parse(String scientificName) throws UnparsableNameException {
    return parse(scientificName, null);
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
   * @param scientificName the full scientific name to parse
   * @param rank the rank of the name if it is known externally. Helps identifying infrageneric names vs bracket authors
   *
   * @throws UnparsableNameException
   */
  public ParsedName parse(final String scientificName, Rank rank) throws UnparsableNameException {
    if (Strings.isNullOrEmpty(scientificName)) {
      unparsable(NameType.NO_NAME, null);
    }
    long start = 0;
    if (LOG.isDebugEnabled()) {
      start = System.currentTimeMillis();
    }

    ParsedName pn = new ParsedName();

    // clean name, removing seriously wrong things
    String name = preClean(scientificName);

    // before any cleaning try if we have known OTU formats, i.e. BIN or SH numbers
    if (IS_OTU_PATTERN.matcher(name).find()) {
      unparsable(NameType.OTU, scientificName);
    }

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
      if (cgroup.equalsIgnoreCase("grex")) {
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
          NormalisedNameParser.setRank(pn, rm.group(1));
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
        NormalisedNameParser.setRank(pn, m.group(2));
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
    boolean parsed = nnParser.parseNormalisedName(pn, name, rank);
    if (!parsed) {
      // try again with stronger normalisation, maybe that helps...
      LOG.debug("Can't parse, use dirty normalizer");
      final String deDirtedName = cleanStrong(name);
      parsed = nnParser.parseNormalisedName(pn, deDirtedName, rank);
      if (!parsed) {
        LOG.debug("Still can't parse, try to ignore authors");
        // try to parse canonical alone ignoring authorship as last resort
        parsed = nnParser.parseNormalisedNameIgnoreAuthors(pn, deDirtedName, rank);
        pn.setAuthorsParsed(false);
        if (!parsed) {
          // we just cant parse this one
          // try to spot a virus name once we know its not a scientific name
          m = IS_VIRUS_PATTERN_POSTFAIL.matcher(name);
          if (m.find()) {
            unparsable(NameType.VIRUS, scientificName);
          }

          unparsable(NameType.NO_NAME, scientificName);
        }
      }
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

    LOG.debug("Parsing time: {}", (System.currentTimeMillis() - start));
    // build canonical name
    return pn;
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

    // normalise usage of dots after abbreviated genus and rank marker
    m = NORM_DOTS.matcher(name);
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
}
