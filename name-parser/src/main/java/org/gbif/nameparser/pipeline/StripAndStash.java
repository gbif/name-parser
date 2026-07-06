package org.gbif.nameparser.pipeline;

import org.gbif.nameparser.api.NameType;
import org.gbif.nameparser.api.NomCode;
import org.gbif.nameparser.api.ParsedName;
import org.gbif.nameparser.api.Rank;
import org.gbif.nameparser.api.Warnings;
import org.gbif.nameparser.token.AuthorParticles;
import org.gbif.nameparser.util.RankUtils;
import org.gbif.nameparser.util.UnicodeUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pre-tokenisation stripper. Removes annotations from the working string and
 * stashes them onto the {@link ParseContext}/{@link ParsedName}.
 *
 * <p>Order matters — markers are stripped from the most specific to the most
 * general so that, for instance, "[sic, porphyria]" doesn't leak through the
 * plain "[sic]" path. {@link #run(ParseContext)} is the explicit, ordered list of
 * strip steps; each step is a self-contained method that takes the working string,
 * mutates the {@link ParseContext} as needed, and returns the (possibly shortened)
 * working string. Steps are deliberately ordered — reordering them changes behaviour.
 */
public final class StripAndStash {

  // [sic], (sic), [sic!] (with no comma inside)
  private static final Pattern SIC =
      Pattern.compile("\\s*[\\(\\[]\\s*sic\\s*!?\\s*[\\)\\]]");
  // [sic, ...] / (sic, ...) — keep the inner text in the unparsed remainder.
  private static final Pattern SIC_WITH_COMMENT =
      Pattern.compile("\\s*[\\(\\[]\\s*sic\\s*,([^)\\]]+)[\\)\\]]");
  // bracketed "(corrig.)" / "[corrig.]" (like SIC) or a bare " corrig." token
  private static final Pattern CORRIG =
      Pattern.compile("\\s*[\\(\\[]\\s*corrig\\.?\\s*[\\)\\]]|(?<=\\s)corrig\\.?(?=\\s|$)");
  // A standalone manuscript marker supplied as the whole authorship ("ined." / "ms." / "msc." /
  // "unpublished"). A marker that follows an author ("Monterosato ms.") is glued as a suffix instead.
  private static final Pattern STANDALONE_MS =
      Pattern.compile("(?i)^(?:ined|ms|msc|unpublished)\\.?$");

  // ---- Nomenclatural notes ----
  // Anchors on a nom/comb/orth/spec keyword and captures from there to end of string.
  // Stops before " non " / " nec " (those are taxonomic-note tails).
  private static final Pattern NOM_NOTE = Pattern.compile(
      "\\s+(" +
          // Possessive (*+) on the word run: [\s.&]* and [a-z.]* both accept '.', so a
          // greedy star could re-partition a dotted run exponentially. The run only ever
          // matches lowercase abbreviation words and never needs to give a completed
          // iteration back (the trailing lookahead needs whitespace/uppercase/comma, none
          // of which this run consumes), so forbidding backtracking is behaviour-neutral.
          "(?i:nom|comb|orth|nomen)\\b\\.?(?:(?!\\s+in\\s+\\p{Lu})[\\s.&]*[a-z][a-z.]*)*+" +
          "|(?i:sp|spec|gen|fam|var|form)\\b\\.?\\s*(?i:nov)\\b\\.?(?:\\s+ined\\b\\.?)?(?:\\s+(?i:sp|spec|gen|fam|var|form)\\b\\.?\\s*(?i:nov)\\b\\.?(?:\\s+ined\\b\\.?)?)*" +
          "|(?i:nov)\\b\\.?\\s+(?i:sp|spec|gen|fam|var|form)\\b\\.?" +
          "|(?:in\\s+obs\\b\\.?,?\\s*)?pro\\s+syn\\b\\.?" +
          ")\\s*(?=$|,\\s*non(?:n\\.?)?\\b|,\\s*nec\\b|,\\s*emend\\b|,\\s*sensu\\b|,\\s*auctt?\\b|,\\s*fide\\b|\\s+in\\s+\\p{Lu}|\\s+\\(.*\\)\\s*\\.?\\s*$|\\s+\\p{Lu})",
      Pattern.UNICODE_CHARACTER_CLASS);

  // Bracketed / parenthesised nomenclatural annotation at end:
  // "[nom. et typ. cons.]" / "(nom. nud.)" / "[orth. error]"
  private static final Pattern BRACKETED_NOM_NOTE = Pattern.compile(
      "\\s*[\\[\\(]\\s*((?:nom|comb|orth|typ)\\b[^\\]\\)]*)[\\]\\)]\\s*$",
      Pattern.CASE_INSENSITIVE);

  // ---- Taxonomic notes ----
  // Once an anchor (auct., sensu, sec., s.l., s.str., emend., fide, according to, …)
  // is hit, the note runs to end of string.
  private static final Pattern TAX_NOTE = Pattern.compile(
      "\\s+,?\\s*(auctt?\\b\\.?(?:[,.]?\\s.*)?" +
          "|sensu(?:\\s.*)?" +
          "|sec\\.?(?:\\s.*)?" +
          "|nec\\b(?:\\s.*)?" +
          "|nonn?\\.?\\s+\\(?\\p{Lu}.*" +
          "|emend\\b\\.?\\s+\\(?\\p{Lu}.*" +
          "|fide\\b\\.?\\s+\\(?\\p{Lu}.*" +
          "|according\\s+to\\s+\\p{Lu}.*" +
          "|excl\\.\\s+.*" +
          "|ss\\b\\.?\\s+.*" +
          // Case-sensitive (lower-case "s") so trailing uppercase author initials ("Author S.L.",
          // "Mill. S.L.") are not mistaken for a sensu-lato marker — matching the sibling
          // SENSU_LATO_REMAINDER / SENSU_STRICTO_SS patterns.
          "|(?-i:s\\.\\s*l\\.?|s\\.\\s*str\\.?|s\\.\\s*lat\\.?|s\\.\\s*ampl\\.?)" +
          ")$",
      Pattern.CASE_INSENSITIVE);

  // Abbreviated sensu-lato / sensu-stricto marker followed by trailing junk that is not
  // part of the name, e.g. "Asplenium trichomanes L. s.lat. - Asplen trich". The marker
  // becomes the taxonomic note and the trailing remainder is parked as unparsed.
  // Case-sensitive: the marker is lower-case "s", so uppercase author initials ("S. L.
  // Schultes", "S.L. Mill.") are not mistaken for a sensu-lato marker.
  private static final Pattern SENSU_LATO_REMAINDER = Pattern.compile(
      "\\s+(s\\.\\s*l\\.?|s\\.\\s*lat\\.?|s\\.\\s*str\\.?|s\\.\\s*ampl\\.?)\\s+(\\S.*?)\\s*$");

  // "s.s." / "s. s." = sensu stricto, at end of string or before trailing junk. Case-sensitive
  // (lower-case) so uppercase author initials ("S.S.Ying") are never taken as the marker.
  private static final Pattern SENSU_STRICTO_SS = Pattern.compile(
      "\\s+s\\.\\s*s\\.?(\\s+\\S.*?)?\\s*$");

  // Parenthesised "(nec ..., YYYY)" / "(non ..., YYYY)" / "(not ..., YYYY)" at end —
  // homonym citation, captured as taxonomic note.
  private static final Pattern PAREN_TAX_NOTE = Pattern.compile(
      "\\s*\\(\\s*((?:nec|non|not)\\s+[^)]+)\\)\\s*\\.?\\s*$",
      Pattern.CASE_INSENSITIVE);

  // Parenthesised taxonomic note inside an authorship: "(auct.)", "(sensu Author, year)",
  // "(sec ...)". The parentheses here mark a note, not a basionym, so the inner text is
  // captured as the taxonomic note and the parens dropped, leaving any real author beside it.
  private static final Pattern PAREN_NOTE = Pattern.compile(
      "\\(\\s*((?:auctt?|sensu|sec)\\b[^)]*)\\)\\s*", Pattern.CASE_INSENSITIVE);

  // Leading parenthesised homonym citation "(non/nec/not ...)" — the whole authorship is a
  // misapplied/taxonomic note, never a basionym.
  private static final Pattern LEADING_HOMONYM_PAREN = Pattern.compile(
      "^\\(\\s*(?:non|nec|not)\\b", Pattern.CASE_INSENSITIVE);

  // Trailing synonymy reference in square brackets: "[= Grislea L. 1753]".
  private static final Pattern SYNONYM_BRACKET = Pattern.compile(
      "\\s*\\[\\s*=\\s*[^\\]]+\\]\\s*\\.?\\s*$");

  // Trailing square-bracket comment introduced by a taxonomic-concept keyword, e.g.
  // "Eunoa [auctt. misspelling for Eunoe]" — the whole bracket content becomes the
  // taxonomic note. Handled separately from the "[= synonym]" and "[sic]" brackets.
  private static final Pattern BRACKETED_TAX_NOTE = Pattern.compile(
      "\\s*\\[\\s*((?:auctt?|sensu|sec|non|nec|misspelling|misapplied|misident)\\b[^\\]]*)\\]\\s*\\.?\\s*$",
      Pattern.CASE_INSENSITIVE);

  // Informal letter-based species subdivision from old floras: a species (optionally
  // followed by its abbreviated author) then a lowercase letter marker ("a.", "b.",
  // "a.b.") then an epithet — "Graphis scripta L. a.b pulverulenta". The marker is
  // replaced by a synthetic rank marker so downstream parsing maps it to Rank.OTHER.
  private static final Pattern LETTER_SUBDIVISION_MARKER = Pattern.compile(
      "^(\\p{Lu}[\\p{Ll}-]+\\s+[\\p{Ll}][\\p{Ll}-]+(?:\\s+\\p{Lu}[\\p{Ll}]*\\.?)*)"
          + "\\s+((?:[a-z]\\.){1,3}[a-z]?)"
          + "\\s+([\\p{Ll}][\\p{Ll}-]{2,})\\s*$",
      Pattern.UNICODE_CHARACTER_CLASS);

  // ---- Aggregate markers (suffix forms) ----
  private static final Pattern AGGREGATE = Pattern.compile(
      "(?:\\s+(?:agg\\.?|aggregate|species\\s+group|species\\s+complex|group|complex)" +
          "|\\s*-\\s*group|\\s*-\\s*aggregate)\\s*$",
      Pattern.CASE_INSENSITIVE);

  // ---- Published-in / nomenclatural reference ----
  private static final Pattern IN_PRESS = Pattern.compile(
      "\\s+in\\s+press\\b\\.?", Pattern.CASE_INSENSITIVE);
  // " in <Author>" / " apud <Author>" tail — e.g. "Busk in Chimonides, 1987",
  // "Small apud Britton & Wilson".
  private static final Pattern IN_AUTHOR = Pattern.compile(
      "\\s+(?:in|apud)\\s+([\\p{Lu}][^\\s].*)$");

  // Trailing page reference: " : 377" / ": 12-18" — pulled into publishedInPage.
  private static final Pattern PUBLISHED_PAGE = Pattern.compile(
      "\\s*:\\s*(\\d+(?:[-\\u2013]\\d+)?)\\s*$");

  // ---------- Precompiled in-method Pattern.compile literals ----------
  private static final Pattern YEAR_4DIGIT = Pattern.compile("\\b\\d{4}\\b");
  private static final Pattern QUOTED_MONOMIAL = Pattern.compile(
      "^(['\"])\\s*([\\p{Lu}][\\p{L}-]+)\\s*\\1(\\s+.+)?$",
      Pattern.UNICODE_CHARACTER_CLASS);
  private static final Pattern TRAILING_STRAIN_CODE = Pattern.compile(
      "^([\\p{Lu}][\\p{Ll}]+\\s+[\\p{Ll}]+)\\s+"
          + "([dr]?RNA[a-zA-Z0-9_\\-]*|[\\p{Lu}][\\p{L}\\d]*\\d[\\p{L}\\d_\\-]*)"
          + "\\s*$",
      Pattern.UNICODE_CHARACTER_CLASS);
  // A quoted strain designation introduced by one or more "str"/"strain" markers, at end of
  // input: "… str .'Aph K2'", "… str. 'Aph K2'", "… strain str .'Aph K2'",
  // "… str .'Heaney 1986/Camb140 1/1'". The marker's abbreviation dot may be spaced ("str .")
  // and the quote may be glued to it (".'Aph"). group(2) is the designation text. The designation
  // is kept as the phrase (not mangled into authorship or reinterpreted as a cultivar epithet).
  private static final Pattern STRAIN_DESIGNATION = Pattern.compile(
      "\\s+(?:str|strain)\\b\\s*\\.?"
          + "(?:\\s+(?:str|strain)\\b\\s*\\.?)*"
          + "\\s*(['\"])(.+?)\\1\\s*$",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern IMPRINT_YEAR_QUOTED = Pattern.compile(
      "\\s*[\\[\\(]\\s*\"(\\d{4}(?:[-\\u2013]\\d{4})?)\"\\s*[\\]\\)]\\s*\\.?\\s*$");
  private static final Pattern IMPRINT_YEAR_KEYWORD = Pattern.compile(
      "\\s*\\(\\s*(?:imprint|not)\\s+(\\d{4}(?:[-\\u2013]\\d{4})?)\\s*\\)\\s*\\.?\\s*$",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern IMPRINT_YEAR_ALT = Pattern.compile(
      "\\s+&\\s+(\\d{4})\\s*\\.?\\s*$");
  private static final Pattern TRAILING_OTU_CODE = Pattern.compile(
      "\\s+([A-Z0-9]{3,}_\\d{3,})$");
  private static final Pattern SEROVAR_PAREN = Pattern.compile(
      "\\s*\\(\\s*(?:serotype|serovar)\\s+[^)]+\\)\\s*\\.?\\s*$",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern SEROVAR_BARE = Pattern.compile(
      "\\s+(?:serotype|serovar)\\s+\\S+(?:\\s+(?:str\\.?|strain)\\s+\\S+)?\\s*\\.?\\s*$",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern ANGLE_BRACKET_AUTHORSHIP = Pattern.compile(
      "\\s+<\\s*(\\p{Lu}[^>]*\\s[^>]*)>\\s*$",
      Pattern.UNICODE_CHARACTER_CLASS);
  private static final Pattern CANDIDATUS_PREFIX = Pattern.compile(
      "^[\"']?(?:Candidatus|Ca\\.)\\s+", Pattern.CASE_INSENSITIVE);
  private static final Pattern CULTIVAR_GROUP_GREX = Pattern.compile(
      "\\s+([\\p{Lu}][\\p{L}]+(?:\\s+[\\p{Lu}][\\p{L}]+)*)\\s+(Group|grex|gx)\\s*$",
      Pattern.UNICODE_CHARACTER_CLASS);
  private static final Pattern QUOTED_CULTIVAR_END = Pattern.compile(
      "\\s+(cv\\.?\\s+)?(['\"])([^'\"]+)\\2\\s*$");
  private static final Pattern QUOTED_CULTIVAR_MID = Pattern.compile(
      "\\s+(?:cv\\.?\\s+)?(['\"])([^'\"]+)\\1(\\s+[\\p{Lu}].*)$",
      Pattern.UNICODE_CHARACTER_CLASS);
  // "Cordia (Adans.) Kuntze sect. Salimori" — authorship placed BEFORE the infrageneric
  // rank marker. group(1)=genus, group(2)=author span (optional parenthesised basionym +
  // combination author words), group(3)=marker + sectional epithet.
  private static final Pattern INFRAGEN_AUTHOR_BEFORE_MARKER = Pattern.compile(
      "^(\\p{Lu}[\\p{Ll}]+)\\s+"
          + "((?:\\(\\s*[^()]*\\)\\s*)?\\p{Lu}[\\p{L}.'\\-]*(?:\\s+\\p{Lu}[\\p{L}.'\\-]*)*)"
          + "\\s+((?:subg|subgen|subgenus|sect|subsect|supersect|ser|subser|superser|divisio|div)"
          + "\\.?\\s+\\p{Lu}[\\p{Ll}]+)$",
      Pattern.UNICODE_CHARACTER_CLASS);
  private static final Pattern QUOTED_CULTIVAR_OPEN = Pattern.compile(
      "\\s+(cv\\.?\\s+)?(['\"])(\\p{Ll}[\\p{Ll} ]*)\\s*$",
      Pattern.UNICODE_CHARACTER_CLASS);
  private static final Pattern DOUBTFUL_GENUS_BRACKET = Pattern.compile(
      "^\\[\\s*([\\p{Lu}][\\p{L}\\-]+)\\s*\\](\\s|$)",
      Pattern.UNICODE_CHARACTER_CLASS);
  private static final Pattern NOM_NOTE_RANK_HINT = Pattern.compile(
      "^(gen|fam|var|form|sp|spec)\\b\\.?",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern AUTHORSHIP_PLACEHOLDER = Pattern.compile(
      "\\s+Not\\s+(?:applicable|given|known|recorded|found)\\s*$",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern PRO_PARTE = Pattern.compile(
      "\\s*,\\s*(?:pro\\s+parte|p\\.\\s*p\\.[A-Z]?)\\s*$",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern PRO_SP_ANNOTATION = Pattern.compile(
      "\\s+\\(\\s*pro\\s+(?:sp|spec|syn|hyb)\\b\\.?\\s*\\)\\s*\\.?\\s*$",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern APPROVED_LISTS = Pattern.compile(
      "\\s*\\(\\s*Approved\\s+Lists\\s+\\d{4}\\s*\\)\\s*\\.?\\s*$",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern COLON_CONCEPT_REFERENCE = Pattern.compile(
      "\\s*:\\s+(\\p{Lu}[^:]*,\\s*\\d{3,4})\\s*\\.?\\s*$",
      Pattern.UNICODE_CHARACTER_CLASS);
  private static final Pattern IN_AUTHOR_YEAR = Pattern.compile(
      ",?\\s*(\\d{3,4})\\s*\\.?\\s*$");
  private static final Pattern IN_AUTHOR_PAREN_YEAR = Pattern.compile("\\((\\d{4})\\)");
  private static final Pattern IPNI_CITATION = Pattern.compile(
      "(?<=\\s)[\\p{Lu}][\\p{L}.]+,\\s+(.+\\(\\d{4}\\))\\.?\\s*$",
      Pattern.UNICODE_CHARACTER_CLASS);
  private static final Pattern IPNI_EMBEDDED_NOM_NOTE = Pattern.compile(
      "\\s+((?:in\\s+obs\\b\\.?,?\\s*)?pro\\s+syn\\b\\.?|nom\\b\\.?(?:\\s+[a-zA-Z][a-zA-Z.]*)*"
          + "|comb\\b\\.?(?:\\s+[a-zA-Z][a-zA-Z.]*)*"
          + "|orth\\b\\.?(?:\\s+[a-zA-Z][a-zA-Z.]*)*)\\s*(?=\\(\\d{4}\\))",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern IPNI_YEAR = Pattern.compile("\\((\\d{4})\\)\\s*\\.?\\s*$");
  private static final Pattern PERIOD_SEPARATED_REFERENCE = Pattern.compile(
      "\\s+[\\p{Lu}][\\p{L}]{2,}\\.\\s+"
          // Filler words up to the FIRST connector. A negative lookahead keeps the
          // connectors out of the filler and a possessive quantifier (*+) forbids
          // backtracking, so this can't blow up on a long connector-free run. The
          // captured span still runs from the first ref word to end-of-string, so the
          // strip is identical to the old greedy form. (Literal connectors were dropped
          // from the filler — they were already covered by [\p{Ll}][\p{L}]+.)
          // The title must OPEN with a real word (uppercase + ≥2 lowercase, e.g. "Annals",
          // "Journal", "Lingua") — not initials like "Z.W." — so an author list carrying a
          // "de"/"et" name particle ("Yin, Z.W. de Beer & Wingf.") isn't mistaken for a ref.
          + "([\\p{Lu}][\\p{Ll}]{2,}[\\p{L}.]*(?:\\s+(?!(?:of|in|de|et|the|und|für)\\b)(?:[\\p{Lu}][\\p{L}.]+|[\\p{Ll}][\\p{L}]+))*+"
          + "\\s+(?:of|in|de|et|the|und|für)\\s+.*)$",
      Pattern.UNICODE_CHARACTER_CLASS);
  private static final Pattern PERIOD_REF_YEAR = Pattern.compile("\\b(\\d{4})\\b");
  private static final Pattern COMMA_PREFIXED_REFERENCE = Pattern.compile(
      "\\s+[\\p{Lu}][\\p{L}.]+,\\s+"
          // Filler = capitalised words plus the non-boundary lowercase words (on/and/for);
          // the boundary connectors (of/in/de/et/the/und/für) are excluded from the filler
          // and the quantifier is possessive (*+), so the first connector is an unambiguous
          // stop with no backtracking. Captured span (first ref word → end) is unchanged.
          + "([\\p{Lu}][\\p{Ll}]{2,}[\\p{L}.]*(?:\\s+(?:[\\p{Lu}][\\p{L}.]+|on|and|for))*+"
          + "\\s+(?:of|in|de|et|the|und|für)\\s+.*)$",
      Pattern.UNICODE_CHARACTER_CLASS);
  private static final Pattern MANUSCRIPT_MARKER = Pattern.compile(
      "\\s*,?\\s+(ined\\.?|ms\\.?|msc\\.?|unpublished)\\s*$",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern PHRASE_GENUS_SUBGENUS = Pattern.compile(
      "^([\\p{Lu}][\\p{Ll}]+)\\s+\\(([\\p{Lu}][\\p{Ll}]+)\\)$",
      Pattern.UNICODE_CHARACTER_CLASS);
  private static final Pattern AUTHOR_START = Pattern.compile(
      "^([\\p{Lu}][\\p{Ll}]+(?:\\s+[\\p{Ll}]+)?)\\s+([\\p{Lu}][\\p{L}.]+.*)$",
      Pattern.UNICODE_CHARACTER_CLASS);

  // ---------- Precompiled in-method matches/replaceAll/replaceFirst literals ----------
  private static final Pattern WHITESPACE = Pattern.compile("\\s+");
  private static final Pattern MULTI_SPACE = Pattern.compile("\\s{2,}");
  private static final Pattern ET_WORD = Pattern.compile("\\bet\\b");
  private static final Pattern AND_WORD = Pattern.compile("\\band\\b");
  private static final Pattern SPACE_AROUND_DOT = Pattern.compile("\\s*\\.\\s*");
  private static final Pattern DOT_BEFORE_ALNUM = Pattern.compile("\\.(?=[\\p{L}\\d])");
  private static final Pattern SPACE_AROUND_AMP = Pattern.compile("\\s*&\\s*");
  private static final Pattern LETTER_QMARK_LETTER = Pattern.compile(".*\\p{L}\\?\\p{L}.*");
  private static final Pattern QMARK_BETWEEN_LETTERS = Pattern.compile("(\\p{L})\\?(\\p{L})");
  // Uncertainty markers used for the doubtful/uncertain-authorship flagging.
  private static final Pattern TRAILING_QMARK = Pattern.compile("\\s\\?\\s*$");
  // A "?" glued to a word only reads as uncertain authorship when it sits at the end of the
  // string or right before an author separator (& or comma) — NOT when a lowercase epithet
  // follows ("Ferganoconcha? oblonga"), which is an open-nomenclature identification qualifier.
  private static final Pattern UNCERTAIN_AUTHOR_QMARK =
      Pattern.compile("\\p{L}\\?(?=\\s*(?:$|[&,]))", Pattern.UNICODE_CHARACTER_CLASS);
  private static final Pattern UNCERTAIN_AUTHOR_OR =
      Pattern.compile("\\p{Lu}\\p{L}*\\s+or\\s+\\p{Lu}", Pattern.UNICODE_CHARACTER_CLASS);
  private static final Pattern UNCERTAIN_AUTHOR_SLASH =
      Pattern.compile("\\p{L}\\s*/\\s*\\p{L}", Pattern.UNICODE_CHARACTER_CLASS);
  private static final Pattern HORT_EX = Pattern.compile("\\bHort\\.(?=\\s+ex\\s+)");
  private static final Pattern HORTUS_EX = Pattern.compile("\\bhortus[a]?\\b(?=\\s+ex\\s+)");
  private static final Pattern LEADING_AUCT = Pattern.compile("^(Auct)");
  private static final Pattern LEADING_AUCTT = Pattern.compile("^(Auctt)");
  private static final Pattern MANUSCRIPT_KEYWORD =
      Pattern.compile("(?i).*\\b(?:ined|ms|msc|unpublished)\\b.*");
  private static final Pattern INITIAL_DOT_SPACE =
      Pattern.compile("\\b(\\p{Lu})\\.\\s+([\\p{Ll}][\\p{Ll}]{3,})");
  private static final Pattern GREEK_MARKER_TEST =
      Pattern.compile(".*[\\p{Ll}.]\\s*[\\u03B1-\\u03C9\\u237A](?:\\s+|\\.\\s*)\\p{Ll}.*");
  private static final Pattern STAR_MARKER_TEST =
      Pattern.compile(".*\\p{Ll}\\s+\\*+\\s+\\p{Ll}.*");
  private static final Pattern GREEK_MARKER =
      Pattern.compile("([\\p{Ll}.])\\s*[\\u03B1-\\u03C9\\u237A](?:\\s+|\\.\\s*)(?=[\\p{Ll}])");
  private static final Pattern STAR_MARKER =
      Pattern.compile("(?<=\\p{Ll})\\s+\\*+\\s+(?=\\p{Ll})");
  private static final Pattern NULL_EPITHET_TEST =
      Pattern.compile(".*[a-z]\\s+null\\s+[a-z]+.*");
  private static final Pattern NULL_EPITHET_MID =
      Pattern.compile("(?<=[a-z])\\s+null\\s+(?=[a-z])");
  private static final Pattern DOUBLE_UNDERSCORE = Pattern.compile("_{2,}");
  private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");
  private static final Pattern DAGGER = Pattern.compile("[†✝]");
  private static final Pattern TINFR_MARKER = Pattern.compile("\\b[tT]\\.?\\s*infr\\.?\\s+");
  private static final Pattern CV_EX = Pattern.compile("\\bcv\\.(?=\\s+ex\\s+)");
  private static final Pattern HT_MARKER = Pattern.compile("\\bht\\.");
  private static final Pattern TRAILING_CV = Pattern.compile("\\s+cv\\.?\\s*$");
  private static final Pattern CV_MARKER = Pattern.compile("\\s+cv\\.?(?=\\s|$)");
  private static final Pattern RANK_MARKER_SUFFIX =
      Pattern.compile(".*\\b(?:sp|spec|subsp|ssp|var|form|f)\\.?$");
  private static final Pattern SP_NOV_PREFIX =
      Pattern.compile("(?i)^(?:sp|spec)\\b\\.?\\s+nov.*");
  private static final Pattern SINGLE_TITLE_WORD = Pattern.compile("^[\\p{Lu}][\\p{Ll}]+$");
  private static final Pattern MISSING_GENUS_EPITHET =
      Pattern.compile("^[a-z][a-z\\-]+\\s+\\p{Lu}.*");
  private static final Pattern MISSING_GENUS_NOTE_KEYWORD =
      Pattern.compile("^(?:non|nec|not|sensu|sec|auct|auctt|fide|emend|ss|s|cf|aff|hort)\\b.*");
  private static final Pattern MIHI_TEST = Pattern.compile("(?i).*\\bmihi\\b.*");
  private static final Pattern MIHI = Pattern.compile("(?i)\\s+mihi\\.?(?=\\s|$)");
  private static final Pattern ANON_UPPER = Pattern.compile("(?<=\\s)Anon\\b\\.?");
  private static final Pattern ANON_LOWER = Pattern.compile("(?<=\\s)anon\\b(?!\\.)");
  private static final Pattern SEROVAR_TEST =
      Pattern.compile("(?i).*\\b(?:serotype|serovar)\\b.*");
  private static final Pattern PAGE_RANGE_TEST = Pattern.compile(".*\\b\\d{3,}-\\d{3,}\\b.*");
  private static final Pattern GENUS_SUBGENUS_TEST =
      Pattern.compile("^[\\p{Lu}][\\p{Ll}]+\\s+\\([\\p{Lu}][\\p{Ll}]+\\)$");
  private static final Pattern TRAILING_SPECIES_WORD_TEST =
      Pattern.compile("^[\\p{Lu}][\\p{Ll}]+\\s+species\\s*\\.?$");
  private static final Pattern TRAILING_SPECIES_WORD = Pattern.compile("\\s+species\\s*\\.?$");
  private static final Pattern DIGITS_ONLY = Pattern.compile("\\d+");

  private StripAndStash() {}

  /** True if a 4-digit year appears anywhere in s[0, end). */
  private static boolean hasEarlierYear(String s, int end) {
    Matcher m = YEAR_4DIGIT.matcher(s.substring(0, end));
    return m.find();
  }

  /**
   * Canonical form for nomenclatural notes: collapse whitespace, ensure a single space
   * follows every period, normalise "et" / "and" between letters to "&" with spaces.
   * Abbreviated notes ("nom. nud.", "Spec nov") get a closing dot if missing; spelled-out
   * "nomen …" forms have any trailing dot stripped (the dot there is sentence punctuation).
   */
  private static String normaliseNomNote(String raw) {
    String s = WHITESPACE.matcher(raw).replaceAll(" ").trim();
    // Normalise "et" / "and" connectives to "&" (with surrounding spaces).
    s = AND_WORD.matcher(ET_WORD.matcher(s).replaceAll("&")).replaceAll("&");
    // Collapse weird spacing around dots, then re-insert a single space after each
    // interior period that is followed by a word character: "nom.illeg." → "nom. illeg.".
    // Periods followed by punctuation (",", ")") keep the abbreviation glued.
    s = SPACE_AROUND_DOT.matcher(s).replaceAll(".");
    s = DOT_BEFORE_ALNUM.matcher(s).replaceAll(". ");
    // Add space around "&" if missing
    s = SPACE_AROUND_AMP.matcher(s).replaceAll(" & ");
    s = MULTI_SPACE.matcher(s).replaceAll(" ").trim();
    // Spelled-out "nomen …" forms drop any trailing dot (the dot in the input is
    // sentence punctuation, not part of the abbreviation).
    if (s.regionMatches(true, 0, "nomen", 0, 5)) {
      while (s.endsWith(".")) s = s.substring(0, s.length() - 1);
      return s.trim();
    }
    // Final word that is a complete English/Latin word doesn't get a trailing dot
    // appended ("orth. error", "nom. correct"). Everything else (abbreviated suffixes
    // like "nud", "cons", "nov", "inval") gets a closing dot if missing.
    if (!s.isEmpty() && !s.endsWith(".")) {
      int lastSpace = s.lastIndexOf(' ');
      String lastWord = lastSpace < 0 ? s : s.substring(lastSpace + 1);
      if (!FULL_WORD_NOM_NOTES.contains(lastWord.toLowerCase())) {
        s = s + ".";
      }
    }
    return s;
  }

  // Suffix words that are complete English/Latin forms — when one of these closes a
  // nomenclatural note without a trailing dot, leave it that way.
  private static final java.util.Set<String> FULL_WORD_NOM_NOTES = java.util.Set.of(
      "correct", "error");

  /**
   * Strips inline annotations from an externally-supplied authorship string and applies
   * any flags they imply directly to the {@link ParsedName}.
   * Returns the cleaned authorship ready for tokenisation.
   */
  static String stripAuthorshipMarkers(String authorship, ParsedName name) {
    String s = authorship.trim();
    if (s.isEmpty()) return s;
    // A standalone manuscript marker as the whole authorship is a manuscript flag, not an author.
    if (STANDALONE_MS.matcher(s).matches()) {
      name.setManuscript(true);
      return "";
    }
    Matcher m = SIC_WITH_COMMENT.matcher(s);
    if (m.find()) {
      name.setOriginalSpelling(Boolean.TRUE);
      s = m.replaceFirst("");
    }
    m = SIC.matcher(s);
    if (m.find()) {
      name.setOriginalSpelling(Boolean.TRUE);
      s = m.replaceFirst("");
    }
    m = CORRIG.matcher(" " + s);
    if (m.find()) {
      name.setOriginalSpelling(Boolean.FALSE);
      // prepend a space so a leading "corrig." (e.g. a standalone authorship) also matches
      s = WHITESPACE.matcher(CORRIG.matcher(" " + s).replaceAll("")).replaceAll(" ").trim();
    }
    // "?" inside a word — transcription artefact for a missing letter ("Istv?nffi").
    // Strip the ? and glue the surrounding word parts; flag doubtful + warning.
    if (s.indexOf('?') >= 0 && LETTER_QMARK_LETTER.matcher(s).matches()) {
      s = QMARK_BETWEEN_LETTERS.matcher(s).replaceAll("$1$2");
      name.setDoubtful(true);
      name.addWarning(Warnings.QUESTION_MARKS_REMOVED);
    }
    // Win-1252 → UTF-8 artefacts inside an aux authorship ("Plesn¡k" should read
    // as "Plesnik"), repaired with the same shared mapping as the main pipeline.
    s = repairWin1252Artefacts(name, s);
    // "Hort." / "hortus(a)" horticultural placeholder is by convention written
    // lower-case "hort.".
    s = HORT_EX.matcher(s).replaceAll("hort.");
    s = HORTUS_EX.matcher(s).replaceAll("hort.");
    // A leading parenthesised homonym citation "(non/nec/not ...)" makes the whole authorship a
    // misapplied/taxonomic note rather than a basionym — capture it verbatim, no author left.
    if (LEADING_HOMONYM_PAREN.matcher(s).find()) {
      String norm = WHITESPACE.matcher(s).replaceAll(" ").trim();
      String existing = name.getTaxonomicNote();
      name.setTaxonomicNote(existing == null ? norm : existing + " " + norm);
      return "";
    }
    // Parenthesised taxonomic note "(auct.)" / "(sensu ...)" / "(sec ...)" — the parens mark a
    // note, not a basionym. Capture the inner text as the taxonomic note, drop the parens, and
    // keep any real author beside them: "(auct.) Rolfe" → author Rolfe + note "auct.";
    // "(sensu X, 1878) Y, 1992" → author Y, 1992 + note "sensu X, 1878".
    m = PAREN_NOTE.matcher(s);
    if (m.find()) {
      String norm = WHITESPACE.matcher(m.group(1).trim()).replaceAll(" ");
      norm = LEADING_AUCTT.matcher(LEADING_AUCT.matcher(norm).replaceAll("auct")).replaceAll("auctt");
      String existing = name.getTaxonomicNote();
      name.setTaxonomicNote(existing == null ? norm : existing + " " + norm);
      s = (s.substring(0, m.start()) + s.substring(m.end())).trim();
    }
    // Bracketed nom-notes "(nom. nud.)" / "[nom. cons.]" in the auxiliary authorship —
    // extract into nomenclaturalNote and drop from the string before tokenisation.
    m = BRACKETED_NOM_NOTE.matcher(s);
    if (m.find()) {
      String raw = m.group(1).trim();
      String existing = name.getNomenclaturalNote();
      name.setNomenclaturalNote(existing == null ? normaliseNomNote(raw) : existing + " " + normaliseNomNote(raw));
      s = s.substring(0, m.start()).trim();
    }
    // Bare nomenclatural notes ("nom. illeg.", "comb. nov.", "sp. nov.", …) in the auxiliary
    // authorship — same extraction as run(), so a separately supplied authorship behaves like
    // the equivalent tail on a full name. Match against a space-padded copy so a note anchored
    // at the very START of the string ("nom. illeg.") is caught too — NOM_NOTE requires a
    // leading whitespace. The leading padding space ends up in "before" and is removed by trim.
    String paddedNom = " " + s;
    m = NOM_NOTE.matcher(paddedNom);
    if (m.find()) {
      String raw = m.group(1).trim();
      if (!raw.isEmpty()) {
        String norm = normaliseNomNote(raw);
        String existing = name.getNomenclaturalNote();
        name.setNomenclaturalNote(existing == null ? norm : existing + " " + norm);
        String before = paddedNom.substring(0, m.start());
        String after = paddedNom.substring(m.end());
        s = (before + (after.isEmpty() ? "" : " " + after)).trim();
        while (s.endsWith(",")) s = s.substring(0, s.length() - 1).trim();
        if (MANUSCRIPT_KEYWORD.matcher(raw).matches()) {
          name.setManuscript(true);
        }
      }
    }
    // Strip taxonomic-note tails (sensu, emend., auct., etc.) from the auxiliary authorship
    // string. The same patterns are applied to the main working string in run(); apply them
    // here too so a separately-supplied "Author, year emend. Other, year" doesn't leak the
    // second year into the parsed authorship.
    // Match against a space-padded copy so a note anchored at the very START of the string
    // ("sensu Turcz., p.p.") is caught too — TAX_NOTE requires a leading whitespace. The note
    // always runs to end of string, so group(1) is a suffix of s and the author part is
    // whatever precedes it (empty when the whole string is the note).
    m = TAX_NOTE.matcher(" " + s);
    if (m.find()) {
      String raw = m.group(1).trim();
      if (!raw.isEmpty()) {
        String norm = INITIAL_DOT_SPACE.matcher(raw).replaceAll("$1.$2");
        norm = LEADING_AUCTT.matcher(LEADING_AUCT.matcher(norm).replaceAll("auct")).replaceAll("auctt");
        String existing = name.getTaxonomicNote();
        name.setTaxonomicNote(existing == null ? norm
            : existing.equals(norm) ? existing : existing + " " + norm);
        s = s.substring(0, s.length() - m.group(1).length()).trim();
        while (s.endsWith(",")) s = s.substring(0, s.length() - 1).trim();
      }
    }
    return s.trim();
  }

  /**
   * Ordered list of strip/stash steps applied to the working string before tokenisation.
   * Each step is named for what it removes; the order is load-bearing (see class doc).
   */
  /**
   * Flags open-nomenclature uncertainty before any of it is normalised away. A trailing
   * standalone "?" ("… (Author, 1886) ?") is dropped but marks the name doubtful. A "?"
   * glued to an author word ("Sess?", "Smith?") or alternative authors joined by "or" / "/"
   * ("Jarocki or Schinz", "Smith/Jones") mark the authorship uncertain. Internal "letter?letter"
   * transcription artefacts ("Istv?nffi") are left to {@link #repairQuestionMarkInWord}.
   */
  private static String flagUncertainAuthorship(ParseContext ctx, String s) {
    if (TRAILING_QMARK.matcher(s).find()) {
      ctx.name.setDoubtful(true);
      ctx.name.addWarning(Warnings.QUESTION_MARKS_REMOVED);
      s = TRAILING_QMARK.matcher(s).replaceAll("").trim();
    }
    if (UNCERTAIN_AUTHOR_QMARK.matcher(s).find()
        || UNCERTAIN_AUTHOR_OR.matcher(s).find()
        || UNCERTAIN_AUTHOR_SLASH.matcher(s).find()) {
      ctx.name.setDoubtful(true);
      ctx.name.addWarning(Warnings.UNCERTAIN_AUTHORSHIP);
    }
    return s;
  }

  /**
   * Splits off an authorship placed before an infrageneric rank marker as the genus author.
   * "Cordia (Adans.) Kuntze sect. Salimori" leaves "Cordia sect. Salimori" for the pipeline
   * (the sectional epithet is not read as an author) and stashes "(Adans.) Kuntze" as the
   * generic authorship, applied by {@link Pipeline} — the section itself is unauthored.
   */
  private static String extractGenericAuthor(ParseContext ctx, String s) {
    Matcher m = INFRAGEN_AUTHOR_BEFORE_MARKER.matcher(s);
    if (m.matches()) {
      ctx.pendingGenericAuthor = m.group(2).trim();
      s = m.group(1) + " " + m.group(3);
    }
    return s;
  }

  static void run(ParseContext ctx) {
    String s = ctx.working;
    s = flagUncertainAuthorship(ctx, s);
    s = extractGenericAuthor(ctx, s);
    s = stripQuotedMonomial(ctx, s);
    s = applyMissingGenusPlaceholder(ctx, s);
    s = stripInfraRankLetters(ctx, s);
    s = normaliseLetterSubdivisionMarker(ctx, s);
    s = repairQuestionMarkInWord(ctx, s);
    s = stripStrainDesignation(ctx, s);
    s = stashTrailingStrainCode(ctx, s);
    s = stripImprintYears(ctx, s);
    s = stripNullBetweenEpithets(ctx, s);
    s = normaliseHyphens(ctx, s);
    s = replaceHomoglyphs(ctx, s);
    s = repairWin1252Artefacts(ctx.name, s);
    s = normaliseDoubleUnderscores(ctx, s);
    s = stashTrailingOtuCode(ctx, s);
    s = stripSerovarSerotype(ctx, s);
    s = stripAngleBracketAuthorship(ctx, s);
    s = stripHtml(ctx, s);
    s = stripCandidatus(ctx, s);
    s = normaliseHortExPlaceholder(ctx, s);
    s = stripCultivarGroupGrex(ctx, s);
    s = stripQuotedCultivar(ctx, s);
    s = stripExtinctDagger(ctx, s);
    s = stripTinfrMarker(ctx, s);
    s = stripDoubtfulGenusBrackets(ctx, s);
    s = stripSicAndCorrig(ctx, s);
    s = stashSynonymBracket(ctx, s);
    s = stripBracketedNomNote(ctx, s);
    s = stripNomNote(ctx, s);
    s = stripAuthorshipPlaceholders(ctx, s);
    s = stripTrailingSpeciesWord(ctx, s);
    s = stripProParte(ctx, s);
    s = stripProSpAnnotation(ctx, s);
    s = stripApprovedLists(ctx, s);
    s = stripMihi(ctx, s);
    s = normaliseAnon(ctx, s);
    s = stripColonConceptReference(ctx, s);
    s = stripBracketedTaxNote(ctx, s);
    s = stripParenTaxNote(ctx, s);
    s = stripSensuLatoRemainder(ctx, s);
    s = stripSensuStrictoSS(ctx, s);
    s = stripTaxNote(ctx, s);
    s = stripAggregateSuffix(ctx, s);
    s = stripPublishedPage(ctx, s);
    s = stripInPress(ctx, s);
    s = stripInAuthorCitation(ctx, s);
    s = stripIpniCitation(ctx, s);
    s = stripPeriodSeparatedReference(ctx, s);
    s = stripCommaPrefixedReference(ctx, s);
    s = stripManuscriptMarker(ctx, s);
    s = stripSupraRankPrefix(ctx, s);
    s = stripLeadingInfragenericMarker(ctx, s);
    s = stashPhraseName(ctx, s);
    ctx.working = s;
  }

  private static String stripStrainDesignation(ParseContext ctx, String s) {
    // A quoted strain designation after a "str"/"strain" marker ("Aphanizomenon flos-aquae
    // str .'Aph K2'") is kept intact as the phrase of an informal STRAIN name. Without this the
    // messy "str ." spacing and the glued ".'Aph" quote let the designation leak into the
    // authorship parser (mangled to "K.2.'Aph & '") or, with clean spacing, get reinterpreted as
    // a cultivar epithet — losing the requested STRAIN rank. The remaining "Genus species str."
    // is left for NameTokens, which resolves the trailing "str" marker to Rank.STRAIN; the stashed
    // phrase then makes it a phrase name.
    Matcher m = STRAIN_DESIGNATION.matcher(s);
    if (m.find()) {
      String prefix = s.substring(0, m.start()).trim();
      // Only when a plausible name precedes the marker (a capitalised genus), never on junk.
      if (!prefix.isEmpty() && Character.isUpperCase(prefix.codePointAt(0))) {
        ctx.name.setPhrase(m.group(2).trim());
        ctx.name.setType(NameType.INFORMAL);
        return prefix + " str.";
      }
    }
    return s;
  }

  private static String firstWord(String s) {
    int sp = 0;
    while (sp < s.length() && !Character.isWhitespace(s.charAt(sp))) sp++;
    return s.substring(0, sp);
  }

  private static String stripQuotedMonomial(ParseContext ctx, String s) {
    // A leading monomial wrapped in quotes ("'Prosthète' Hesse, 1861" / "\"Foo\" Bar, 2000")
    // marks a word that is not an available scientific name. Strip the quotes for parsing,
    // remember the quote char so Assemble can re-wrap the parsed uninomial, and flag doubtful.
    Matcher qm = QUOTED_MONOMIAL.matcher(s);
    if (qm.find()) {
      ctx.quotedMonomial = qm.group(1);
      ctx.name.setDoubtful(true);
      s = (qm.group(2) + (qm.group(3) == null ? "" : qm.group(3))).trim();
    }
    return s;
  }

  private static String applyMissingGenusPlaceholder(ParseContext ctx, String s) {
    // Missing-genus placeholder forms — the user-facing genus is replaced by "?":
    //   "denheyeri Eghbalian, …, 2017"            → "? denheyeri Eghbalian, …, 2017"
    //   "Missing penchinati Bourguignat, 1870"    → "? penchinati Bourguignat, 1870"
    //   "\"? gryphoidis"                          → "? gryphoidis"
    //   "\"? gryphoidis (Bourguignat 1870) …"     → "? gryphoidis (Bourguignat 1870) …"
    // Emit PLACEHOLDER type + MISSING_GENUS warning; rest of the pipeline parses normally.
    String missing = null;
    boolean emitWarning = false;
    if (s.startsWith("\"? ") || s.startsWith("\"?\t")) {
      missing = "? " + s.substring(3).trim();
    } else if (s.startsWith("Missing ") && s.length() > 8 && Character.isLowerCase(s.charAt(8))) {
      missing = "? " + s.substring(8);
    } else if (s.length() > 1 && Character.isLowerCase(s.codePointAt(0))
        && MISSING_GENUS_EPITHET.matcher(s).matches()
        && !MISSING_GENUS_NOTE_KEYWORD.matcher(s).matches()
        && !AuthorParticles.isParticle(firstWord(s))) {
      // Lowercase-starting epithet followed by a capitalised author/year — assume
      // the genus is missing and prepend the placeholder. This is the only form
      // that emits MISSING_GENUS (the others have an explicit "?" or "Missing"
      // marker that the user wrote on purpose). Skip when the first word is a
      // known taxonomic-note keyword that's NOT a real epithet, or a surname particle
      // ("van Berg", "del Rosario Author") — a particle-led input is an author name,
      // not an epithet whose genus went missing.
      missing = "? " + s;
      emitWarning = true;
    }
    if (missing != null) {
      ctx.name.setType(NameType.PLACEHOLDER);
      if (emitWarning) ctx.name.addWarning(Warnings.MISSING_GENUS);
      s = missing;
    }
    return s;
  }

  private static String stripInfraRankLetters(ParseContext ctx, String s) {
    // Strip Greek-like single-letter rank markers (⍺, β, …) and informal "***"
    // markers sitting between two lowercase epithets — these are fungal rank
    // markers and must not be converted to ASCII letters / taken as authorship
    // by downstream passes. The greek letter must be followed by a separator (space
    // or dot) so we don't strip an inline glyph in epithets like "βrigida".
    // Forms covered: " δ ", " δ. ", ".δ.", ".δ ".
    if (s.indexOf('⍺') >= 0
        || GREEK_MARKER_TEST.matcher(s).matches()
        || STAR_MARKER_TEST.matcher(s).matches()) {
      s = GREEK_MARKER.matcher(s).replaceAll("$1 ");
      s = STAR_MARKER.matcher(s).replaceAll(" ");
    }
    return s;
  }

  private static String normaliseLetterSubdivisionMarker(ParseContext ctx, String s) {
    // Old floras subdivide a species informally with letters ("a.", "b.", "a.b.").
    // Rewrite such a marker to the synthetic RankMarkers.LETTER_SUBDIVISION token so
    // the normal rank-marker path treats the trailing epithet as an infraspecific of
    // rank OTHER. Any abbreviated author before the marker is left in place and dropped
    // by the mid-name-author logic, exactly as it would be before a "var." marker.
    Matcher m = LETTER_SUBDIVISION_MARKER.matcher(s);
    if (m.find()) {
      // A single-letter marker that is itself a real rank marker ("f." = forma) must be
      // left for the normal machinery; only genuine subdivision letters are rewritten.
      String[] segments = m.group(2).split("[^a-z]+");
      boolean realMarker = segments.length == 1
          && RankMarkers.matchInfraspecific(segments[0]) != null;
      if (!realMarker) {
        s = m.group(1) + " " + RankMarkers.LETTER_SUBDIVISION + " " + m.group(3);
      }
    }
    return s;
  }

  private static String repairQuestionMarkInWord(ParseContext ctx, String s) {
    // "?" inside a word — transcription artefact for a missing letter ("Istv?nffi").
    // Strip the ? and glue the surrounding word parts; flag doubtful + warning.
    if (s.indexOf('?') >= 0 && LETTER_QMARK_LETTER.matcher(s).matches()) {
      s = QMARK_BETWEEN_LETTERS.matcher(s).replaceAll("$1$2");
      ctx.name.setDoubtful(true);
      ctx.name.addWarning(Warnings.QUESTION_MARKS_REMOVED);
    }
    return s;
  }

  private static String stashTrailingStrainCode(ParseContext ctx, String s) {
    // Trailing strain-code suffix on a binomial ("Candida albicans RNA_CTR0-3",
    // "Armillaria ostoyae RNA1", "Alpha proteobacterium RNA12") — capture the code
    // as an informal phrase and reduce the working string to "Genus species". The
    // code is recognised by an uppercase prefix that contains at least one digit OR
    // starts with "RNA"/"DNA" with letters/digits/underscores/hyphens following.
    Matcher pm = TRAILING_STRAIN_CODE.matcher(s);
    if (pm.find()) {
      String code = pm.group(2);
      // Don't consume a single trailing year ("Genus species 1842") — those are
      // numeric-only and handled by authorship parsing.
      if (!DIGITS_ONLY.matcher(code).matches()) {
        ctx.name.setPhrase(code);
        ctx.name.setType(NameType.INFORMAL);
        s = pm.group(1);
      }
    }
    return s;
  }

  private static String stripImprintYears(ParseContext ctx, String s) {
    // Imprint year annotations at end. By definition the imprint year is a SECONDARY
    // year cited alongside the publication year. Only strip when there's already
    // another 4-digit year elsewhere in the string — otherwise a single year-in-brackets
    // / year-in-parens is the publication year itself.
    //   "Storr, 1970 [\"1969\"]"     → imprintYear=1969
    //   "Storr, 1970 (imprint 1969)" → imprintYear=1969
    //   "Storr, 1887 (\"1886-1888\")" → imprintYear="1886-1888"
    //   "Wagener, 1959 & 1961"       → imprintYear=1961
    //   "Storr, 1970 (imprint 1969)" with explicit "imprint" keyword always strips.
    {
      // Quoted year in brackets / parens always strips ("…1970 [\"1969\"]" /
      // "…1887 (\"1886-1888\")"); unquoted bracketed year strips only with another
      // 4-digit year present elsewhere.
      Matcher m1 = IMPRINT_YEAR_QUOTED.matcher(s);
      if (m1.find()) {
        ctx.setPendingImprintYear(m1.group(1));
        s = s.substring(0, m1.start()).trim();
      }
    }
    {
      // "(imprint YYYY)" / "(not YYYY)" — explicit ICZN forms (Article 22), always strip.
      Matcher m1 = IMPRINT_YEAR_KEYWORD.matcher(s);
      if (m1.find()) {
        ctx.setPendingImprintYear(m1.group(1));
        s = s.substring(0, m1.start()).trim();
      }
    }
    {
      // " & YYYY" trailing alternate year — only strips when there's another year
      // earlier in the string.
      Matcher m1 = IMPRINT_YEAR_ALT.matcher(s);
      if (m1.find() && hasEarlierYear(s, m1.start())) {
        ctx.setPendingImprintYear(m1.group(1));
        s = s.substring(0, m1.start()).trim();
      }
    }
    return s;
  }

  private static String stripNullBetweenEpithets(ParseContext ctx, String s) {
    // Bare "null" between two lowercase epithets ("Austrorhynchus pectatus null pectatus")
    // is a data-quality artefact — drop the token and flag doubtful + NULL_EPITHET. Don't
    // touch "Abies null Hood" (a single "null" epithet followed by an author span); that
    // case is kept and flagged downstream by Assemble.flagBlacklistedEpithets.
    if (NULL_EPITHET_TEST.matcher(s).matches()) {
      s = NULL_EPITHET_MID.matcher(s).replaceAll(" ");
      ctx.name.setDoubtful(true);
      ctx.name.addWarning(Warnings.NULL_EPITHET);
    }
    return s;
  }

  private static String normaliseHyphens(ParseContext ctx, String s) {
    // Normalise unicode hyphens to their ASCII counterpart so that downstream
    // tokenisation and canonical output use a consistent character. Flag the
    // strip with HOMOGLYHPS — these are visually-identical hyphen variants.
    String beforeHyphen = s;
    s = s.replace('‐', '-')
         .replace('‑', '-')
         .replace('‒', '-')
         .replace('–', '-')
         .replace('—', '-');
    if (!s.equals(beforeHyphen)) {
      ctx.name.addWarning(Warnings.HOMOGLYHPS);
    }
    return s;
  }

  private static String replaceHomoglyphs(ParseContext ctx, String s) {
    // Apostrophe / quote variants (curly, prime, modifier-letter, fullwidth, …) are already
    // normalised to ASCII in Pipeline.run before tokenisation, so nothing to do here.
    // Replace known homoglyphs (Latin look-alikes from other scripts) with their
    // canonical Latin counterpart. Emit a HOMOGLYHPS warning when anything actually
    // changed. Hyphen homoglyphs are intentionally excluded — those are normalised
    // above already.
    if (UnicodeUtils.containsHomoglyphs(s)) {
      String repl = UnicodeUtils.replaceHomoglyphs(s, false);
      if (!repl.equals(s)) {
        ctx.name.addWarning(Warnings.HOMOGLYHPS);
        s = repl;
      }
    }
    return s;
  }

  private static String repairWin1252Artefacts(ParsedName name, String s) {
    // Win-1252 → UTF-8 transcription artefacts that the homoglyph table doesn't cover
    // (e.g. "Plesn¡k" should read as "Plesnik"). Map a small set of high-bit punctuation
    // characters to their Latin look-alikes when they sit between letters. Shared by the
    // main strip pipeline and the separately-supplied-authorship path so the two never
    // repair different character sets.
    if (s.indexOf('¡') >= 0 || s.indexOf('¢') >= 0 || s.indexOf('£') >= 0
        || s.indexOf('‚') >= 0 || s.indexOf('„') >= 0 || s.indexOf('‰') >= 0) {
      String before = s;
      s = s.replace('¡', 'i')
           .replace('¢', 'c')
           .replace('£', 'L')
           .replace('‚', 'e')
           .replace('„', 'a')
           .replace('‰', 'e');
      if (!s.equals(before)) {
        name.addWarning(Warnings.HOMOGLYHPS);
      }
    }
    return s;
  }

  private static String normaliseDoubleUnderscores(ParseContext ctx, String s) {
    // Normalize double (or more) underscores between letters to a single space
    // (e.g. "Pseudocercospora__dendrobii" → "Pseudocercospora dendrobii").
    if (s.indexOf("__") >= 0) {
      s = DOUBLE_UNDERSCORE.matcher(s).replaceAll(" ").trim();
    }
    return s;
  }

  private static String stashTrailingOtuCode(ParseContext ctx, String s) {
    // Strip trailing OTU-code identifiers (e.g. "Oxalis barrelieri XXZ_21243") — store
    // as pendingUnparsed so the name portion is still parsed normally.
    if (s.contains(" ") && ctx.pendingUnparsed == null) {
      Matcher otuM = TRAILING_OTU_CODE.matcher(s);
      if (otuM.find()) {
        ctx.setPendingUnparsed(otuM.group(1));
        s = s.substring(0, otuM.start()).trim();
      }
    }
    return s;
  }

  private static String stripSerovarSerotype(ParseContext ctx, String s) {
    // Bacterial serovar/serotype/strain annotations on a binomial — these are
    // sub-species level epidemiological designators, not formal taxonomic ranks.
    // Strip silently so the underlying binomial parses cleanly.
    //   "Aggregatibacter actinomycetemcomitans serotype d str. SA508"
    //   "Streptococcus pyogenes (serotype M18)"
    //   "Actinobacillus pleuropneumoniae serovar 2 strain S1536"
    //   "Leptospira interrogans serovar Fugis"
    if (SEROVAR_TEST.matcher(s).matches()) {
      // Parenthesised "(serotype X)" / "(serovar X)" suffix
      Matcher pm = SEROVAR_PAREN.matcher(s);
      if (pm.find()) {
        s = s.substring(0, pm.start()).trim();
      }
      // Bare " serovar/serotype X [strain/str. Y]" suffix
      Matcher pm2 = SEROVAR_BARE.matcher(s);
      if (pm2.find()) {
        s = s.substring(0, pm2.start()).trim();
      }
    }
    return s;
  }

  private static String stripAngleBracketAuthorship(ParseContext ctx, String s) {
    // Angle-bracketed authorship placeholder ("Doradidae <Unspecified Agent>") — the
    // bracketed text starts with a capital and contains spaces (i.e. it isn't an
    // HTML tag). Flag the strip with AUTHORSHIP_REMOVED + UNUSUAL_CHARACTERS warnings
    // and mark the name doubtful so callers know the authorship couldn't be parsed.
    if (s.indexOf('<') >= 0) {
      Matcher br = ANGLE_BRACKET_AUTHORSHIP.matcher(s);
      if (br.find()) {
        ctx.name.addWarning(Warnings.AUTHORSHIP_REMOVED);
        ctx.name.addWarning(Warnings.UNUSUAL_CHARACTERS);
        ctx.name.setDoubtful(true);
        s = s.substring(0, br.start()).trim();
      }
    }
    return s;
  }

  private static String stripHtml(ParseContext ctx, String s) {
    // Strip HTML tags and decode HTML entities (e.g. "<i>sensu</i> Author" or "&amp;").
    if (s.indexOf('<') >= 0 || s.indexOf('&') >= 0) {
      // Strip HTML tags but keep their text content, so a tagged connector like
      // "<i>sensu</i> Fabricius, 1780" becomes "sensu Fabricius, 1780" and is picked up
      // as a taxonomic note by the normal note handling downstream.
      String beforeTags = s;
      s = HTML_TAG.matcher(s).replaceAll("");
      if (!s.equals(beforeTags)) {
        ctx.name.addWarning(Warnings.XML_TAGS);
      }
      // Decode basic HTML entities
      String beforeEntities = s;
      s = s.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&nbsp;", " ");
      if (!s.equals(beforeEntities)) {
        ctx.name.addWarning(Warnings.HTML_ENTITIES);
      }
      // Clean up any extra whitespace introduced by tag removal
      s = MULTI_SPACE.matcher(s).replaceAll(" ").trim();
    }
    return s;
  }

  private static String stripCandidatus(ParseContext ctx, String s) {
    // Candidatus prefix — quoted "Candidatus …" or bare "Candidatus …" / "Ca. …".
    Matcher cm = CANDIDATUS_PREFIX.matcher(s);
    if (cm.find()) {
      ctx.name.setCandidatus(true);
      ctx.name.setCode(NomCode.BACTERIAL);
      s = s.substring(cm.end());
      if (s.endsWith("\"") || s.endsWith("'")) s = s.substring(0, s.length() - 1);
    }
    return s;
  }

  private static String normaliseHortExPlaceholder(ParseContext ctx, String s) {
    // "cv. ex Author" / "Hort. ex Author" / "hortus(a) ex Author" — all variants of
    // the horticultural placeholder for the unknown gardener-author. Normalise to
    // canonical lower-case "hort.".
    s = CV_EX.matcher(s).replaceAll("hort.");
    s = HORT_EX.matcher(s).replaceAll("hort.");
    s = HORTUS_EX.matcher(s).replaceAll("hort.");
    // "ht." is an occasional abbreviation of the horticultural marker "hort." used
    // directly on the author span ("Gymnogramma alstoni ht.Birkenh.; Gard."). Normalise
    // it so it parses like its spelled-out twin instead of leaking "ht" in as a bogus
    // infraspecific epithet. A lowercase standalone "ht." is not a real epithet or author.
    s = HT_MARKER.matcher(s).replaceAll("hort.");
    return s;
  }

  private static String stripCultivarGroupGrex(ParseContext ctx, String s) {
    // Cultivar Group / grex names: "Genus [species] CapWord(s) (Group|grex|gx)" at end.
    // Capture the capitalised epithet sequence as the cultivarEpithet and pin the rank
    // accordingly. Trailing word is stripped from the working string.
    Matcher gm = CULTIVAR_GROUP_GREX.matcher(s);
    if (gm.find()) {
      ctx.name.setCultivarEpithet(gm.group(1).trim());
      ctx.name.setCode(NomCode.CULTIVARS);
      ctx.name.setRank("Group".equals(gm.group(2)) ? Rank.CULTIVAR_GROUP : Rank.GREX);
      s = s.substring(0, gm.start()).trim();
    }
    return s;
  }

  private static String stripQuotedCultivar(ParseContext ctx, String s) {
    // Quoted cultivar epithet: " 'Name'" / " \"Name\"" → cultivarEpithet (quoted).
    // Two positions: at end of input, OR in the middle followed by an author span.
    // Skip when the quote is immediately preceded by a rank marker (sp./ssp./var./...)
    // without an explicit cv. — that is a phrase-name form, not a cultivar.
    Matcher cm = QUOTED_CULTIVAR_END.matcher(s);
    boolean cmFound = cm.find();
    boolean hasCvMarker = cmFound && cm.group(1) != null;
    String preceding = cmFound ? s.substring(0, cm.start()).trim() : null;
    boolean isRankMarkerPrefix = !hasCvMarker && preceding != null
        && RANK_MARKER_SUFFIX.matcher(preceding).matches();
    if (cmFound && !isRankMarkerPrefix) {
      ctx.name.setCultivarEpithet(cm.group(3).trim());
      ctx.name.setCode(NomCode.CULTIVARS);
      ctx.name.setRank(Rank.CULTIVAR);
      s = s.substring(0, cm.start()).trim();
      // strip a trailing " cv." marker if it survived
      s = TRAILING_CV.matcher(s).replaceAll("").trim();
    } else {
      // Mid-string quoted epithet followed by an author span
      // ("Verpericola megasoma \"Dall\" Pils.").
      Matcher cmMid = QUOTED_CULTIVAR_MID.matcher(s);
      if (cmMid.find()) {
        ctx.name.setCultivarEpithet(cmMid.group(2).trim());
        ctx.name.setCode(NomCode.CULTIVARS);
        ctx.name.setRank(Rank.CULTIVAR);
        // The cultivar's own authorship is the cultivar author (group 3). Split off any
        // species author that precedes the cultivar epithet and stash it as the specific
        // authorship ("Acer campestre L. cv. 'Elsrijk' Broerse" → combination author
        // "Broerse", specific author "L."), leaving the binomial + cultivar author.
        String prefix = s.substring(0, cmMid.start()).trim();
        int authorStart = findAuthorStart(prefix);
        String namePart;
        if (authorStart > 0) {
          namePart = prefix.substring(0, authorStart).trim();
          ctx.pendingSpecificAuthor = prefix.substring(authorStart).trim();
        } else {
          namePart = prefix;
        }
        s = (namePart + cmMid.group(3)).trim();
        s = CV_MARKER.matcher(s).replaceAll("").trim();
      } else {
        // Unclosed trailing cultivar quote: " 'albino" / " \"albino" (opening quote,
        // no closing one) — common in aquarium/horticultural trade lists. Treat it like
        // the closed form. The content is restricted to lowercase letters and spaces so
        // this never swallows an apostrophe-particle author ("… 't Veld & Visser, 1993)")
        // nor a capitalised bogus apostrophe author ("Nereidavus kulkovi 'Kulkov"), both
        // of which start upper-case or carry punctuation. Same rank-marker guard as above.
        Matcher cmOpen = QUOTED_CULTIVAR_OPEN.matcher(s);
        boolean openFound = cmOpen.find();
        boolean openHasCv = openFound && cmOpen.group(1) != null;
        String openPreceding = openFound ? s.substring(0, cmOpen.start()).trim() : null;
        boolean openRankPrefix = !openHasCv && openPreceding != null
            && RANK_MARKER_SUFFIX.matcher(openPreceding).matches();
        if (openFound && !openRankPrefix) {
          ctx.name.setCultivarEpithet(cmOpen.group(3).trim());
          ctx.name.setCode(NomCode.CULTIVARS);
          ctx.name.setRank(Rank.CULTIVAR);
          s = s.substring(0, cmOpen.start()).trim();
          s = TRAILING_CV.matcher(s).replaceAll("").trim();
        }
      }
    }
    return s;
  }

  private static String stripExtinctDagger(ParseContext ctx, String s) {
    // Extinct dagger(s) anywhere — strip all occurrences
    if (s.indexOf('†') >= 0 || s.indexOf('✝') >= 0) {
      ctx.name.setExtinct(true);
      s = WHITESPACE.matcher(DAGGER.matcher(s).replaceAll(" ")).replaceAll(" ").trim();
    }
    return s;
  }

  private static String stripTinfrMarker(ParseContext ctx, String s) {
    // "t.infr." infraspecific abbreviation (Hieracium "the infrasubspecific epithet"
    // notation). Strip the marker so the trailing epithet is parsed normally; the
    // resulting binomial+infra structure already maps to INFRASPECIFIC_NAME rank.
    if (s.indexOf("infr") >= 0) {
      s = TINFR_MARKER.matcher(s).replaceAll("");
    }
    return s;
  }

  private static String stripDoubtfulGenusBrackets(ParseContext ctx, String s) {
    // Doubtful genus in square brackets at the start: "[Acontia] chia ..." or just "[Dexia]".
    // Strip the brackets, mark the name doubtful and emit the DOUBTFUL_GENUS warning.
    Matcher dg = DOUBTFUL_GENUS_BRACKET.matcher(s);
    if (dg.find()) {
      ctx.name.setDoubtful(true);
      ctx.name.addWarning(Warnings.DOUBTFUL_GENUS);
      s = (dg.group(1) + dg.group(2) + s.substring(dg.end())).trim();
    }
    return s;
  }

  private static String stripSicAndCorrig(ParseContext ctx, String s) {
    Matcher m = null;
    // [sic, comment] first
    m = SIC_WITH_COMMENT.matcher(s);
    if (m.find()) {
      ctx.name.setOriginalSpelling(Boolean.TRUE);
      String inner = m.group(1).trim();
      // Park the parenthetical comment as unparsed; canonical drops it.
      ctx.setPendingUnparsed("(sic," + WHITESPACE.matcher(inner).replaceAll("") + ")");
      s = m.replaceFirst("");
    }
    m = SIC.matcher(s);
    if (m.find()) {
      ctx.name.setOriginalSpelling(Boolean.TRUE);
      s = m.replaceFirst("");
    }
    m = CORRIG.matcher(" " + s);
    if (m.find()) {
      ctx.name.setOriginalSpelling(Boolean.FALSE);
      // remove the "corrig." marker (bare or bracketed); prepend a space so a leading marker matches too
      s = WHITESPACE.matcher(CORRIG.matcher(" " + s).replaceAll("")).replaceAll(" ").trim();
    }
    return s;
  }

  private static String stashSynonymBracket(ParseContext ctx, String s) {
    // Trailing synonymy reference in square brackets: "[= Grislea L. 1753]" — park as
    // unparsed first so subsequent nom-note / tax-note checks see a clean tail.
    Matcher m = SYNONYM_BRACKET.matcher(s);
    if (m.find()) {
      String tail = s.substring(m.start()).trim();
      ctx.setPendingUnparsed(tail);
      ctx.name.setDoubtful(true);
      s = s.substring(0, m.start()).trim();
      while (s.endsWith(",")) s = s.substring(0, s.length() - 1).trim();
    }
    return s;
  }

  private static String stripBracketedNomNote(ParseContext ctx, String s) {
    // Bracketed nom notes at end — e.g. "[nom. et typ. cons.]" / "[orth. error]" / "(nom. nud.)"
    Matcher m = BRACKETED_NOM_NOTE.matcher(s);
    if (m.find()) {
      String raw = m.group(1).trim();
      ctx.name.setNomenclaturalNote(normaliseNomNote(raw));
      s = s.substring(0, m.start()).trim();
      if (s.endsWith(",")) s = s.substring(0, s.length() - 1).trim();
    }
    return s;
  }

  private static String stripNomNote(ParseContext ctx, String s) {
    // Nomenclatural notes — anchored after a nom/comb/orth keyword. May appear in the
    // middle of the string when followed by a comma + non/nec/end, so splice rather
    // than truncate.
    Matcher m = NOM_NOTE.matcher(s);
    if (m.find()) {
      String raw = m.group(1).trim();
      String norm = normaliseNomNote(raw);
      String existing = ctx.name.getNomenclaturalNote();
      ctx.name.setNomenclaturalNote(existing == null ? norm : existing + " " + norm);
      String before = s.substring(0, m.start()).trim();
      String after = s.substring(m.end()).trim();
      s = (before + (after.isEmpty() ? "" : " " + after)).trim();
      while (s.endsWith(",")) s = s.substring(0, s.length() - 1).trim();
      // "sp. nov." or "spec. nov." on a bare monomial — keep the species indet marker
      // in the working string so the regular indet/INFORMAL handling fires later.
      if (SP_NOV_PREFIX.matcher(raw).matches()
          && SINGLE_TITLE_WORD.matcher(before).matches()) {
        s = before + " sp.";
      }
      // Captured notes containing the manuscript markers "ined." / "ms." set the
      // manuscript flag (the standalone manuscript-marker block doesn't see them
      // anymore because NOM_NOTE already consumed the keyword).
      if (MANUSCRIPT_KEYWORD.matcher(raw).matches()) {
        ctx.name.setManuscript(true);
      }
      // Rank hint from the captured nom-note prefix: "Gen. nov." → GENUS, "Fam. nov."
      // → FAMILY, etc. Only fires when the parsed name doesn't already carry a rank
      // and the note really starts with one of these markers.
      if (ctx.name.getRank() == null || ctx.name.getRank() == Rank.UNRANKED) {
        Matcher rm = NOM_NOTE_RANK_HINT.matcher(raw);
        if (rm.find()) {
          switch (rm.group(1).toLowerCase()) {
            case "gen": ctx.name.setRank(Rank.GENUS); break;
            case "fam": ctx.name.setRank(Rank.FAMILY); break;
            case "var": ctx.name.setRank(Rank.VARIETY); break;
            case "form": ctx.name.setRank(Rank.FORM); break;
            // sp/spec stays default (SPECIES will be assigned by Assemble for binomials)
          }
        }
      }
    }
    return s;
  }

  private static String stripAuthorshipPlaceholders(ParseContext ctx, String s) {
    // Authorship placeholders: "Not applicable", "Not given", "Not known" etc.
    // Stripped silently with an AUTHORSHIP_REMOVED warning so the bare name still parses.
    // (Angle-bracket placeholders like "<Unspecified Agent>" are handled earlier by
    // stripAngleBracketAuthorship / stripHtml, so no <...> reaches this step.)
    Matcher pm = AUTHORSHIP_PLACEHOLDER.matcher(s);
    if (pm.find()) {
      ctx.name.addWarning(Warnings.AUTHORSHIP_REMOVED);
      s = s.substring(0, pm.start()).trim();
    }
    return s;
  }

  private static String stripTrailingSpeciesWord(ParseContext ctx, String s) {
    // Trailing " species" on a bare uninomial — drop the word and produce a monomial
    // (no rank, no INFORMAL marker). Only fires when the rest is a single Title-cased
    // word so we don't mangle real binomials like "Genus species" + author.
    if (TRAILING_SPECIES_WORD_TEST.matcher(s).matches()) {
      s = TRAILING_SPECIES_WORD.matcher(s).replaceFirst("").trim();
    }
    return s;
  }

  private static String stripProParte(ParseContext ctx, String s) {
    // ", pro parte" / ", p.p." — botanical/zoological "in part" qualifier on a
    // taxonomic-concept author. Stripped silently with the doubtful flag.
    Matcher pm = PRO_PARTE.matcher(s);
    if (pm.find()) {
      ctx.name.setDoubtful(true);
      s = s.substring(0, pm.start()).trim();
    }
    return s;
  }

  private static String stripProSpAnnotation(ParseContext ctx, String s) {
    // " (pro sp.)" — botanical "given as a species" annotation following a hybrid
    // name. Strip silently so the inner name parses cleanly.
    Matcher pm = PRO_SP_ANNOTATION.matcher(s);
    if (pm.find()) {
      s = s.substring(0, pm.start()).trim();
    }
    return s;
  }

  private static String stripApprovedLists(ParseContext ctx, String s) {
    // " (Approved Lists YYYY)" — bacterial code annotation marking the name's
    // inclusion in the Approved Lists of Bacterial Names. Strip silently.
    Matcher pm = APPROVED_LISTS.matcher(s);
    if (pm.find()) {
      s = s.substring(0, pm.start()).trim();
    }
    return s;
  }

  private static String stripMihi(ParseContext ctx, String s) {
    // "mihi" / "Mihi" — Latin "by me", a self-attribution placeholder used by some
    // authors. It is not a real authorship and is stripped wherever it appears with
    // an AUTHORSHIP_REMOVED warning. Common patterns:
    //   "Genus species mihi"             → strip trailing
    //   "Genus species mihi. Author …"   → strip middle (between species and author)
    //   "Genus species mihi var. epithet mihi" → strip both occurrences
    if (MIHI_TEST.matcher(s).matches()) {
      String before = s;
      s = MIHI.matcher(s).replaceAll("").trim();
      if (!s.equals(before)) {
        ctx.name.addWarning(Warnings.AUTHORSHIP_REMOVED);
      }
    }
    return s;
  }

  private static String normaliseAnon(ParseContext ctx, String s) {
    // "Anon."/"Anon"/"anon" — anonymous-author placeholder. Normalise to lowercase
    // "anon." so the downstream parser captures it as a real (anonymous) authorship.
    s = ANON_UPPER.matcher(s).replaceAll("anon.");
    s = ANON_LOWER.matcher(s).replaceAll("anon.");
    return s;
  }

  private static String stripColonConceptReference(ParseContext ctx, String s) {
    // ": <Author>, YYYY" trailing concept reference — botanical taxonomic-concept
    // citation form ("Vespa emarginata Linnaeus, 1758: Fabricius, 1793"). The
    // Linnaeus year is the original publication; Fabricius is the sensu author. The
    // explicit ", YYYY" requirement keeps the simpler ": SanctAuthor" sanctioning-
    // author form (e.g. "Boletus versicolor L. : Fr.") out of this strip.
    Matcher pm = COLON_CONCEPT_REFERENCE.matcher(s);
    if (pm.find()) {
      String note = pm.group(1).trim();
      String existing = ctx.name.getTaxonomicNote();
      ctx.name.setTaxonomicNote(existing == null ? note : existing + " " + note);
      s = s.substring(0, pm.start()).trim();
    }
    return s;
  }

  private static String stripBracketedTaxNote(ParseContext ctx, String s) {
    // Trailing "[auctt. misspelling for Eunoe]" style bracket introduced by a
    // taxonomic-concept keyword → the whole bracket content becomes the taxonomic note.
    Matcher m = BRACKETED_TAX_NOTE.matcher(s);
    if (m.find()) {
      String note = WHITESPACE.matcher(m.group(1).trim()).replaceAll(" ");
      note = LEADING_AUCTT.matcher(LEADING_AUCT.matcher(note).replaceAll("auct")).replaceAll("auctt");
      String existing = ctx.name.getTaxonomicNote();
      ctx.name.setTaxonomicNote(existing == null ? note : existing + " " + note);
      s = s.substring(0, m.start()).trim();
    }
    return s;
  }

  private static String stripSensuLatoRemainder(ParseContext ctx, String s) {
    // "s.lat." / "s.str." etc. mid-string, followed by trailing junk → note + unparsed.
    Matcher m = SENSU_LATO_REMAINDER.matcher(s);
    if (m.find()) {
      String note = WHITESPACE.matcher(m.group(1)).replaceAll("").toLowerCase();
      String remainder = m.group(2).trim();
      String existing = ctx.name.getTaxonomicNote();
      ctx.name.setTaxonomicNote(existing == null ? note : existing + " " + note);
      ctx.setPendingUnparsed(remainder);
      s = s.substring(0, m.start()).trim();
    }
    return s;
  }

  private static String stripSensuStrictoSS(ParseContext ctx, String s) {
    // "s.s." (sensu stricto) at end, optionally before trailing junk → note + unparsed.
    Matcher m = SENSU_STRICTO_SS.matcher(s);
    if (m.find()) {
      String existing = ctx.name.getTaxonomicNote();
      ctx.name.setTaxonomicNote(existing == null ? "s.s." : existing + " s.s.");
      if (m.group(1) != null) {
        ctx.setPendingUnparsed(m.group(1).trim());
      }
      s = s.substring(0, m.start()).trim();
    }
    return s;
  }

  private static String stripParenTaxNote(ParseContext ctx, String s) {
    // Parenthesised "(nec/non/not …, YYYY)" homonym citation at end → taxonomic note.
    Matcher m = PAREN_TAX_NOTE.matcher(s);
    if (m.find()) {
      String note = m.group(1).trim();
      String existing = ctx.name.getTaxonomicNote();
      ctx.name.setTaxonomicNote(existing == null ? note : existing + " " + note);
      s = s.substring(0, m.start()).trim();
    }
    return s;
  }

  private static String stripTaxNote(ParseContext ctx, String s) {
    // Taxonomic notes (anchor at end). Apply the dotted-initial-no-space convention to
    // abbreviated authors only ("F. Schmidt" → "F.Schmidt"); keep spacing for everything
    // else, so abbreviated taxonomic keywords like "ss. auct. europ." render verbatim.
    Matcher m = TAX_NOTE.matcher(s);
    if (m.find()) {
      String raw = m.group(1).trim();
      if (!raw.isEmpty()) {
        // Only collapse when the left side of the dot is a single capital letter — a
        // genuine author initial. "ss." or "auct." (multi-letter abbreviations) must
        // keep the trailing space.
        String norm = INITIAL_DOT_SPACE.matcher(raw).replaceAll("$1.$2");
        // Lowercase a leading "Auct." / "Auctt." — the keyword is by convention
        // rendered in lower case regardless of how it appeared in the input.
        norm = LEADING_AUCTT.matcher(LEADING_AUCT.matcher(norm).replaceAll("auct")).replaceAll("auctt");
        ctx.name.setTaxonomicNote(norm);
        s = s.substring(0, m.start()).trim();
        while (s.endsWith(",")) s = s.substring(0, s.length() - 1).trim();
      }
    }
    return s;
  }

  private static String stripAggregateSuffix(ParseContext ctx, String s) {
    // Aggregate suffix → rank promoted to SPECIES_AGGREGATE.
    Matcher m = AGGREGATE.matcher(s);
    if (m.find()) {
      ctx.aggregate = true;
      s = s.substring(0, m.start()).trim();
    }
    return s;
  }

  private static String stripPublishedPage(ParseContext ctx, String s) {
    // Trailing page reference (": 377") — pulled into publishedInPage and stripped.
    // Run before IN_AUTHOR so a "Smith, 1900: 12 in Editor" tail handles both.
    Matcher m = PUBLISHED_PAGE.matcher(s);
    if (m.find()) {
      ctx.name.setPublishedInPage(m.group(1));
      s = s.substring(0, m.start()).trim();
    }
    return s;
  }

  private static String stripInPress(ParseContext ctx, String s) {
    // " in press" → manuscript + nomenclaturalNote.
    Matcher m = IN_PRESS.matcher(s);
    if (m.find()) {
      ctx.name.setManuscript(true);
      String existing = ctx.name.getNomenclaturalNote();
      ctx.name.setNomenclaturalNote(existing == null ? "in press" : existing + " in press");
      s = m.replaceFirst("");
    }
    return s;
  }

  private static String stripInAuthorCitation(ParseContext ctx, String s) {
    // " in <Author>" trailing tail — runs first so an "Author in Source, Title (Year)"
    // tail doesn't get partially consumed by the IPNI / period-separator patterns that
    // follow. Years pulled from a publishedIn reference are publication years and are
    // marked code-neutral (pendingYearFromPublication=true) so they don't influence
    // code inference; the same year may attach to a zoological, botanical, or
    // bacteriological name.
    Matcher m = IN_AUTHOR.matcher(s);
    if (m.find()) {
      String ref = m.group(1).trim();
      // Trailing period after a closing paren is sentence punctuation — strip it.
      // After other characters (an author abbreviation like "G.Kirchn.") keep the dot.
      if (ref.endsWith(").")) {
        ref = ref.substring(0, ref.length() - 1).trim();
      }
      if (ref.length() >= 2) {
        String existing = ctx.name.getPublishedIn();
        ctx.name.setPublishedIn(existing == null ? ref : existing + " " + ref);
        // A parenthesised year takes precedence over a bare trailing year when both are
        // present, so try it first (setPendingPublicationYear is first-writer-wins).
        Matcher pyear = IN_AUTHOR_PAREN_YEAR.matcher(ref);
        if (pyear.find()) {
          ctx.setPendingPublicationYear(pyear.group(1));
        }
        Matcher ym = IN_AUTHOR_YEAR.matcher(ref);
        if (ym.find()) {
          ctx.setPendingPublicationYear(ym.group(1));
        }
        s = s.substring(0, m.start()).trim();
      }
    }
    return s;
  }

  private static String stripIpniCitation(ParseContext ctx, String s) {
    // IPNI-style citation: "Author., Title (Year)." — comma after the author then
    // a publication title ending with the year in parentheses. Pull the reference
    // (and year) out so the leading author span is what's parsed downstream.
    Matcher pm = IPNI_CITATION.matcher(s);
    if (pm.find()) {
      String ref = pm.group(1).trim();
      // Embedded nomNote ("in obs., pro syn.") that sits before the year parens —
      // pull it out into the nomenclaturalNote and drop from the ref text.
      Matcher nm = IPNI_EMBEDDED_NOM_NOTE.matcher(ref);
      if (nm.find()) {
        String note = nm.group(1).trim();
        String existing = ctx.name.getNomenclaturalNote();
        ctx.name.setNomenclaturalNote(existing == null ? note : existing + " " + note);
        ref = MULTI_SPACE.matcher(ref.substring(0, nm.start()) + " " + ref.substring(nm.end()))
            .replaceAll(" ").trim();
      }
      ctx.name.setPublishedIn(ref);
      Matcher ym = IPNI_YEAR.matcher(ref);
      if (ym.find()) {
        ctx.setPendingPublicationYear(ym.group(1));
      }
      s = s.substring(0, pm.start(1)).trim();
      if (s.endsWith(",")) s = s.substring(0, s.length() - 1).trim();
    }
    return s;
  }

  private static String stripPeriodSeparatedReference(ParseContext ctx, String s) {
    // "Surname. <Reference Title> ... <year> ..." — citation where the publication
    // reference is concatenated after the author with a period. We recognise the
    // reference by an English/Latin preposition ("Annals of the …", "Journal of
    // the …") inside it, which is rare inside author names. The leading surname must
    // be at least three letters so we don't truncate at an initial.
    Matcher pm = PERIOD_SEPARATED_REFERENCE.matcher(s);
    if (pm.find()) {
      String ref = pm.group(1).trim();
      if (ref.endsWith(".")) ref = ref.substring(0, ref.length() - 1);
      ctx.name.setPublishedIn(ref);
      // Refs that include a numeric pagination range ("1658-1662") are full
      // bibliographic citations: the year is ambiguous with the pagination context
      // so we don't propagate it onto the combination authorship, and we flag the
      // strip with NOMENCLATURAL_REFERENCE. Refs without a range are author-year
      // style with a clean trailing year — propagate the year, no warning.
      boolean hasPageRange = PAGE_RANGE_TEST.matcher(ref).matches();
      if (hasPageRange) {
        ctx.name.addWarning(org.gbif.nameparser.api.Warnings.NOMENCLATURAL_REFERENCE);
      } else {
        Matcher ym = PERIOD_REF_YEAR.matcher(ref);
        if (ym.find()) {
          ctx.setPendingPublicationYear(ym.group(1));
        }
      }
      s = s.substring(0, pm.start(1)).trim();
      if (s.endsWith(".")) s = s.substring(0, s.length() - 1).trim();
    }
    return s;
  }

  private static String stripCommaPrefixedReference(ParseContext ctx, String s) {
    // "Author(s), <Reference Title> …" — comma-prefixed publication reference (no period
    // after the author span). The title must contain a recognisable connector ("of",
    // "in", "the", "und", "für") so we don't accidentally swallow a comma-separated
    // co-author. The capture extends to end of input and includes pages / years / figs.
    Matcher pm = COMMA_PREFIXED_REFERENCE.matcher(s);
    if (pm.find()) {
      String ref = pm.group(1).trim();
      ctx.name.setPublishedIn(ref);
      // Don't propagate the title's year onto the combination authorship — for a
      // comma-prefixed reference the year is the publication year of the article
      // (different from any zoological/botanical author-year citation).
      s = s.substring(0, pm.start(1)).trim();
      if (s.endsWith(",")) s = s.substring(0, s.length() - 1).trim();
      ctx.name.addWarning(org.gbif.nameparser.api.Warnings.NOMENCLATURAL_REFERENCE);
    }
    return s;
  }

  private static String stripManuscriptMarker(ParseContext ctx, String s) {
    // Manuscript marker "ined." / "ms." / "msc." / "unpublished" at end → manuscript flag.
    // Runs AFTER in-author so a trailing "Busk ms in Chimonides, 1987" cleanly strips both.
    Matcher mm = MANUSCRIPT_MARKER.matcher(s);
    if (mm.find()) {
      ctx.name.setManuscript(true);
      // Preserve trailing dot from input ("ined." stays, "ms" stays).
      String tag = mm.group(1).toLowerCase();
      String existing = ctx.name.getNomenclaturalNote();
      ctx.name.setNomenclaturalNote(existing == null ? tag : existing + " " + tag);
      s = s.substring(0, mm.start()).trim();
    }
    return s;
  }

  private static String stripSupraRankPrefix(ParseContext ctx, String s) {
    // "<Family> <suprageneric-rank-marker> <Name> [Author …]" — strip the family prefix
    // and rank marker so the inner uninomial is what's parsed. Also accept a leading
    // rank marker with no family prefix ("subtrib. Scolochloinae Soreng").
    Matcher pm = SUPRA_RANK_PREFIX.matcher(s);
    if (pm.find()) {
      Rank r = SUPRA_RANK_MARKERS.get(pm.group(1).toLowerCase());
      if (r != null) {
        ctx.name.setRank(r);
        s = s.substring(pm.end()).trim();
      }
    }
    return s;
  }

  private static String stripLeadingInfragenericMarker(ParseContext ctx, String s) {
    // Leading infrageneric rank marker without a genus prefix ("subgen. Trematostoma Sacc.",
    // "sect. Taeda"). Strip the marker and pin the rank on the parsed name. Caller-supplied
    // ZOOLOGICAL code switches botanical-flavoured section/series ranks to the zoological
    // variant ("sect." → SECTION_ZOOLOGY).
    Matcher pm = LEADING_INFRAGEN_MARKER.matcher(s);
    if (pm.find()) {
      Rank r = RankUtils.RANK_MARKER_MAP_INFRAGENERIC.get(pm.group(1).toLowerCase());
      if (r != null) {
        if (ctx.requestedCode == NomCode.ZOOLOGICAL) {
          Rank zool = BOT_TO_ZOOL.get(r);
          if (zool != null) r = zool;
        }
        ctx.name.setRank(r);
        if (r.getCode() != null && ctx.name.getCode() == null) {
          ctx.name.setCode(r.getCode());
        }
        s = s.substring(pm.end()).trim();
      }
    }
    return s;
  }

  private static String stashPhraseName(ParseContext ctx, String s) {
    // Phrase-name forms like "Prostanthera sp. Somersbey (B.J.Conn 4024)" — when the
    // string has the shape "<Genus[ species]>[ author]? <rank-marker> <Phrase>" where
    // the phrase contains parens, a quoted token, or mixed letters+digits, set the
    // phrase aside and rewrite working to "Genus[ species] marker. [Author]" so
    // NameTokens sees an indet name and (if present) the species author at the end.
    Matcher pm = PHRASE_NAME.matcher(s);
    if (pm.find()) {
      String prefix = WHITESPACE.matcher(pm.group(1).trim()).replaceAll(" ");
      String marker = pm.group(2);
      String phrase = WHITESPACE.matcher(pm.group(3).trim()).replaceAll(" ");
      Rank rank = PHRASE_RANK_MARKERS.get(marker.toLowerCase());
      if (rank != null) {
        ctx.name.setPhrase(phrase);
        int authorStart = findAuthorStart(prefix);
        // When the prefix is just a genus (no species), pin the rank explicitly
        // and drop the marker from the working string — leaving "Genus <marker>."
        // would have NameTokens treat the marker as a species epithet.
        boolean prefixIsGenusOnly = !prefix.trim().contains(" ");
        // Genus + (Subgenus) has no species: subgenus parens already suggest
        // INFRAGENERIC_NAME, but the sp. marker is the stronger signal and pins
        // SPECIES. Set rank explicitly here as well.
        boolean prefixIsGenusPlusSubgenus = GENUS_SUBGENUS_TEST.matcher(prefix.trim()).matches();
        if (authorStart > 0) {
          // Place the marker BEFORE the author so the author span trails as the
          // species author for AuthorshipParser to pick up.
          s = prefix.substring(0, authorStart).trim() + " " + marker + "."
              + " " + prefix.substring(authorStart).trim();
        } else if (prefixIsGenusOnly && rank != Rank.SPECIES) {
          ctx.name.setRank(rank);
          s = prefix;
        } else if (prefixIsGenusPlusSubgenus) {
          ctx.name.setRank(rank);
          // Extract subgenus directly from the prefix; drop the parens so the simpler
          // "Genus" working string is left for AuthorshipSplit (which now defaults a
          // no-trailing "(Subgenus)" to basionym authorship).
          Matcher gm = PHRASE_GENUS_SUBGENUS.matcher(prefix.trim());
          if (gm.matches()) {
            ctx.name.setInfragenericEpithet(gm.group(2));
            s = gm.group(1);
          } else {
            s = prefix;
          }
        } else {
          s = prefix + " " + marker + ".";
        }
      }
    }
    return s;
  }

  /**
   * Find the start of the author span within a "Genus species[ author]" prefix string.
   * Returns the index of the author span, or -1 when the prefix is just Genus[ species].
   */
  private static int findAuthorStart(String prefix) {
    // Pattern: <Genus>(\s+<species>)?(\s+<Author...>)
    Matcher m = AUTHOR_START.matcher(prefix);
    if (m.matches()) {
      return m.start(2);
    }
    return -1;
  }

  // Recognised infraspecific markers that introduce a phrase name. SUBSPECIES is most
  // common ("subsp.", "ssp."), SPECIES uses "sp.", "spec.".
  private static final java.util.Map<String, Rank> PHRASE_RANK_MARKERS = java.util.Map.of(
      "sp", Rank.SPECIES,
      "spec", Rank.SPECIES,
      "subsp", Rank.SUBSPECIES,
      "ssp", Rank.SUBSPECIES,
      "var", Rank.VARIETY,
      "form", Rank.FORM,
      "f", Rank.FORM);

  /**
   * Phrase-name pattern: "<Genus[ (Subgenus)][ species]>[ author]? <marker>(.) <phrase>"
   * where the phrase either contains parens (the most reliable signal), starts with a
   * digit (Baeckea ssp. 2 (LJM 2019)), or is fully quoted. Captures the Latin prefix
   * (including any author tokens before the marker), the rank marker, and the phrase
   * text.
   */
  private static final Pattern PHRASE_NAME = Pattern.compile(
      "^([\\p{Lu}][\\p{Ll}]+(?:\\s+\\([\\p{Lu}][\\p{Ll}]+\\))?(?:\\s+[\\p{Ll}]+)?(?:\\s+[\\p{Lu}][\\p{L}.]+)*)"
          + "\\s+(sp|spec|subsp|ssp|var|form|f)\\.?"
          + "\\s+("
          // phrase with parens, may start with capital, digit, or quote.
          // [^(]*+ locks the FIRST '(' possessively instead of a greedy [^$]* that
          // backtracks over every split point when no balanced parens follow. The
          // captured phrase (group 3) is anchored start..end so its text is unchanged.
          + "[\\p{Lu}A-Z\\d'\"][^(]*+\\(.+\\)[^$]*?"
          // OR fully quoted phrase without parens
          + "|['\"][^'\"]+['\"]"
          + ")\\s*$",
      Pattern.UNICODE_CHARACTER_CLASS);

  // Suprageneric rank markers that may appear after a family prefix or at the start
  // of the input. Trailing dot optional, case-insensitive.
  private static final java.util.Map<String, Rank> SUPRA_RANK_MARKERS = java.util.Map.ofEntries(
      java.util.Map.entry("subfam", Rank.SUBFAMILY),
      java.util.Map.entry("subfamily", Rank.SUBFAMILY),
      java.util.Map.entry("trib", Rank.TRIBE),
      java.util.Map.entry("tribe", Rank.TRIBE),
      java.util.Map.entry("subtrib", Rank.SUBTRIBE),
      java.util.Map.entry("subtribe", Rank.SUBTRIBE),
      java.util.Map.entry("supertrib", Rank.SUPERTRIBE),
      java.util.Map.entry("infratrib", Rank.INFRATRIBE));

  /** Botanical-flavoured infrageneric ranks that have a zoological counterpart at the
   * same nominal level. Used when a leading rank marker is parsed with a caller-supplied
   * ZOOLOGICAL code. */
  private static final java.util.Map<Rank, Rank> BOT_TO_ZOOL = java.util.Map.of(
      Rank.SECTION_BOTANY, Rank.SECTION_ZOOLOGY,
      Rank.SUBSECTION_BOTANY, Rank.SUBSECTION_ZOOLOGY,
      Rank.SUPERSECTION_BOTANY, Rank.SUPERSECTION_ZOOLOGY,
      Rank.SERIES_BOTANY, Rank.SERIES_ZOOLOGY,
      Rank.SUBSERIES_BOTANY, Rank.SUBSERIES_ZOOLOGY,
      Rank.SUPERSERIES_BOTANY, Rank.SUPERSERIES_ZOOLOGY);

  /** Leading infrageneric rank marker at the very start of the input ("subgen. X" /
   * "sect. X"). Captures the marker word (no trailing dot). */
  private static final Pattern LEADING_INFRAGEN_MARKER = Pattern.compile(
      "^(subg|subgen|subgenus|sect|section|subsect|subsection|supersect|suprasect"
          + "|ser|series|subser|subseries)\\.?\\s+(?=[\\p{Lu}])",
      Pattern.UNICODE_CHARACTER_CLASS | Pattern.CASE_INSENSITIVE);

  // Match a family-shaped prefix (capitalised word ending in -aceae / -idae / -inae /
  // -oideae, or any capitalised word ≥4 chars), optional space, then a recognised
  // suprageneric rank marker (with optional dot), then space before the inner name.
  // Group 1 = the rank marker text (no trailing dot).
  private static final Pattern SUPRA_RANK_PREFIX = Pattern.compile(
      "^(?:[\\p{Lu}][\\p{L}]{2,}\\s+)?"
          + "(subfam(?:ily)?|subtrib(?:e)?|supertrib|infratrib|trib(?:e)?)"
          + "\\.?\\s+(?=[\\p{Lu}])",
      Pattern.UNICODE_CHARACTER_CLASS | Pattern.CASE_INSENSITIVE);
}
