package org.gbif.nameparser.pipeline;

import org.gbif.nameparser.api.NameType;
import org.gbif.nameparser.api.UnparsableNameException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Heuristic gate that throws {@link UnparsableNameException} for inputs that
 * are not scientific names: viruses, hybrid formulas, OTU/specimen codes, and
 * placeholders. Runs before any other stage so unparsable inputs short-circuit.
 */
public final class Preflight {

  // ---------- VIRUS ----------
  // Match anything ending in -virus / -viruses / -viroid / -viroids / -phage(s) /
  // -satellite, plus standalone viral keywords. Word characters can precede the
  // suffix (so "Sapovirus", "papillomavirus", "C2-like viruses" all match).
  private static final Pattern VIRUS = Pattern.compile(
      "(?:viru(?:s|ses)\\b" +
          "|viroid(?:s)?\\b" +
          "|phages?" +
          "|virion(?:s)?\\b" +
          "|\\bsatellite\\b" +
          "|(?:alpha|beta|delta|circular)[\\s_-]*satellites?\\b" +
          "|\\b(?:Clecru|Milvet|Subclov)satellite\\b" +
          "|bacteriophages?\\b" +
          "|\\b[MSC]?NPV\\b|\\bGV\\b|\\bICTV\\b" +
          "|(?:fusion\\s+)?vector\\b" +
          "|\\bprions?\\b" +
          "|\\bparticles?\\b" +
          "|\\breplicons?\\b" +
          "|\\bRNA\\b)",
      Pattern.CASE_INSENSITIVE);

  // "Genus species [(Subgenus)] [Author], YYYY" — zoological author-year pattern.
  // Allows VIRUS-matching epithets (vector, virus, phage) to be parsed as real
  // species when an explicit Title-cased author + 4-digit year follows. The author
  // must start with an uppercase letter and look like a Latin surname (≥3 chars,
  // with optional initials / particle / hyphen / dot prefix) — strain-code-like
  // trailing tokens ("WM-, 2008") don't qualify.
  // Every quantifier here is possessive and the author span is expressed as two non-overlapping
  // character-class runs (an upper/punctuation run, then a required lowercase surname letter, then
  // the rest), so the whole pattern is linear-time — no backtracking. The earlier form used an
  // overlapping separator/token loop "(?:[\s,&.-][\p{L}.-']+)*" that was a catastrophic-backtracking
  // (ReDoS) hazard on inputs with a long dotted/hyphenated tail and no trailing year; the parser has
  // no execution timeout, so that must stay linear. The required lowercase letter keeps all-caps
  // strain codes ("WM-, 2008") from qualifying as a Latin surname, exactly as before.
  private static final Pattern ZOOLOGICAL_BINOMIAL = Pattern.compile(
      "^\\p{Lu}\\p{Ll}++"                                 // Genus
      + "\\s++(?:\\(\\p{Lu}\\p{Ll}++\\)\\s++)?"           // optional (Subgenus)
      + "\\p{Ll}[\\p{Ll}\\-]*+\\s++"                      // species (lowercase, ≥1 char)
      + "(?:\\([^)]*+\\)\\s*+)?"                          // optional (basionym) span
      + "\\p{Lu}[\\p{Lu}.,&'\\-\\s]*+"                    // author: upper start + non-lowercase run
      + "\\p{Ll}[\\p{L}.,&'\\-\\s]*+"                     // ...a real surname lowercase, then the rest
      + "\\b(1[6-9]\\d\\d|20\\d\\d)\\b",                  // 4-digit year (16xx–20xx)
      Pattern.UNICODE_CHARACTER_CLASS);

  private static final Pattern CLEAN_BINOMIAL = Pattern.compile(
      "^\\p{Lu}\\p{Ll}+(?:\\s+\\(\\p{Lu}\\p{Ll}+\\))?\\s+\\p{Ll}[\\p{Ll}\\d\\-]*$",
      Pattern.UNICODE_CHARACTER_CLASS);
  private static final Pattern CLEAN_MONOMIAL = Pattern.compile(
      "^\\p{Lu}[\\p{Ll}\\-]+$", Pattern.UNICODE_CHARACTER_CLASS);
  private static final Pattern SOFT_WORD = Pattern.compile(
      "(?:vector|prions?|particles?|replicons?|rna)$", Pattern.CASE_INSENSITIVE);
  // A soft virus-word appearing as the leading Title-cased GENUS token — "Prion vittatus",
  // "Prion Lacépède, 1799". These are real animal genera (Prion = petrels), not viruses. The
  // reject only fires on them when a genuinely viral token (HARD_VIRUS below) is also present.
  // Case-sensitive and anchored so a lowercase epithet ("Euragallia prion") or a viral genus with
  // a suffix ("Rnavirus …", where "Rna" is not followed by a word boundary) never matches.
  private static final Pattern SOFT_GENUS = Pattern.compile(
      "^(?:Vector|Prions?|Particles?|Replicons?|Rna)\\b");
  // The genuinely viral triggers (VIRUS minus the ambiguous English SOFT_WORDs). When only a soft
  // word matched, there is no real viral signal and a leading soft-genus is let through to parse.
  private static final Pattern HARD_VIRUS = Pattern.compile(
      "(?:viru(?:s|ses)\\b" +
          "|viroid(?:s)?\\b" +
          "|phages?" +
          "|virion(?:s)?\\b" +
          "|\\bsatellite\\b" +
          "|(?:alpha|beta|delta|circular)[\\s_-]*satellites?\\b" +
          "|\\b(?:Clecru|Milvet|Subclov)satellite\\b" +
          "|bacteriophages?\\b" +
          "|\\b[MSC]?NPV\\b|\\bGV\\b|\\bICTV\\b)",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern HARD_WORD = Pattern.compile(
      "(?:virus|viroid|phages?|virion|satellite)$", Pattern.CASE_INSENSITIVE);
  private static final Pattern AUTH_ZOO = Pattern.compile(
      "^\\(?\\p{Lu}\\p{Ll}{2,}.*\\b(?:1[6-9]\\d\\d|20\\d\\d)\\b",
      Pattern.UNICODE_CHARACTER_CLASS);

  // ---------- HYBRID FORMULA ----------
  // Hybrid-formula detection is done structurally in looksLikeHybridFormula() below
  // rather than with a single regex — it needs to inspect the spans on either side of
  // the cross to avoid false positives on single-genus notho markers.

  // ---------- NO_NAME ----------
  // Pure alphanumeric codes / OTU identifiers.
  private static final Pattern OTU_BOLD =
      Pattern.compile("^BOLD[:_-][A-Z0-9]+$", Pattern.CASE_INSENSITIVE);
  private static final Pattern OTU_SH =
      Pattern.compile("^SH\\d{6,}\\.[0-9A-Z.]+$", Pattern.CASE_INSENSITIVE);
  private static final Pattern OTU_GTDB_UBA =
      Pattern.compile("^(?:UBA|GTDB|GCA|GCF)[\\d_-]+$", Pattern.CASE_INSENSITIVE);
  // anything starting with a digit, or containing only mixed letters+digits (no Latin epithet shape)
  private static final Pattern PURE_ALPHANUM = Pattern.compile(
      "^(?=.*\\d)[\\p{L}\\d_.\\-]+$", Pattern.UNICODE_CHARACTER_CLASS);
  // Patterns like "Basal_Cryptophyceae-1" — single underscore-separated parts with a digit suffix.
  private static final Pattern PR2_LIKE =
      Pattern.compile("^[\\p{L}]+_[\\p{L}\\-]+(?:-\\d+)?$", Pattern.UNICODE_CHARACTER_CLASS);
  private static final Pattern GEN_NOV =
      Pattern.compile("^Gen\\.?\\s*nov\\.?(?:\\s+(?:sp|species)\\.?\\s*nov\\.?)?\\s*$",
          Pattern.CASE_INSENSITIVE);
  // GTDB/SILVA specimen codes: anything ending with "sp" + 8+ digits after whitespace.
  private static final Pattern OTU_SPECIMEN_SUFFIX =
      Pattern.compile("\\bsp\\d{8,}$", Pattern.CASE_INSENSITIVE);

  // ---------- PLACEHOLDER ----------
  private static final Pattern PLACEHOLDER_KEYWORDS = Pattern.compile(
      "(?:\\(delete\\)|\\b(?:" +
          "incertae[\\s_]*sedis|inc\\.\\s*sed\\.?|incertaesedis" +
          "|not\\s+assigned|unassigned" +
          "|unknown|unaccepted|unidentified|undetermined|undet|indet\\.?|indeterminate" +
          "|uncultured" +
          "|undescribed(?:\\s+(?:species|genus|family))?" +
          "|temp\\s+dummy(?:\\s+name)?" +
          ")\\b)",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern PLACEHOLDER_PREFIX = Pattern.compile(
      "^(?:Unident|Undescribed|IncertaeSedis|Undet)(?:[-\\s]|$)", Pattern.CASE_INSENSITIVE);
  private static final Pattern QUESTION_PREFIX = Pattern.compile("^\\?\\s+\\p{Ll}");
  private static final Pattern NN_PLACEHOLDER = Pattern.compile(
      "^N\\.\\s*[Nn]\\.?(?:\\s*\\(.*\\))?\\s*$");
  // "Genus indet." / "Genus indet" patterns are INFORMAL, not PLACEHOLDER
  private static final Pattern INDET_SPECIES = Pattern.compile(
      "^[\\p{Lu}][\\p{L}\\-]+(?:\\s+[\\p{Lu}][\\p{L}]+)?\\s+(?:indet|undet)\\.?\\s*$",
      Pattern.UNICODE_CHARACTER_CLASS);

  // "clade" as a standalone word: a phylogenetic clade label, not a Linnean name.
  private static final Pattern CLADE_KEYWORD = Pattern.compile(
      "\\bclade\\b", Pattern.CASE_INSENSITIVE);

  // Monomial aggregate forms: "Iteaphila-group" / "Bartonella group" — informal
  // taxonomic group labels that can refer to any rank, so we reject them as INFORMAL.
  private static final Pattern MONOMIAL_AGGREGATE = Pattern.compile(
      "^[\\p{Lu}][\\p{L}]+(?:-group|\\s+group|-complex|\\s+complex)$",
      Pattern.UNICODE_CHARACTER_CLASS);

  // "-lineage" / " lineage" labels ("Vermistella-lineage", "NC12A-lineage", "he2-lineage"):
  // informal phylogenetic lineage names that, like the -group / -complex aggregates, can
  // refer to any rank. Unlike those, the stem is often an OTU-/strain-like code with digits
  // or a lowercase start, so the stem accepts any letter case and embedded digits.
  private static final Pattern LINEAGE_LABEL = Pattern.compile(
      "^[\\p{L}][\\p{L}\\d]*(?:-lineage|\\s+lineage)$",
      Pattern.UNICODE_CHARACTER_CLASS);

  // ---------- Precompiled in-method literals ----------
  private static final Pattern HTML_ENTITY_NAMED = Pattern.compile("&[a-zA-Z]+;");
  private static final Pattern HTML_ENTITY_NUMERIC = Pattern.compile("&#\\d+;");
  private static final Pattern DELETE_MARKER =
      Pattern.compile("(?:.*\\s)?delete(?:\\s.*|,.*|\\s*)");
  private static final Pattern NON_HOMONYM =
      Pattern.compile("(?i)non\\s+\\p{Lu}\\p{L}+(?:\\s.*)?");
  private static final Pattern QUESTION_ONLY = Pattern.compile("^\\?\\s+\\p{Ll}+\\s*$");
  private static final Pattern LATIN_WORD =
      Pattern.compile("[\\p{L}][\\p{L}.\\-]+", Pattern.UNICODE_CHARACTER_CLASS);
  private static final Pattern LATIN_WORD_MIN2 =
      Pattern.compile("[\\p{L}]{2,}", Pattern.UNICODE_CHARACTER_CLASS);
  private static final Pattern AUTHOR_ABBREV =
      Pattern.compile("\\b[\\p{Lu}][\\p{L}]*\\.", Pattern.UNICODE_CHARACTER_CLASS);

  private Preflight() {}

  /**
   * If the input matches a non-scientific category, throws {@link UnparsableNameException}
   * with the appropriate {@link NameType}. Otherwise returns silently.
   */
  static void run(String original, ParseContext ctx) throws UnparsableNameException {
    String s = ctx.working.trim();
    if (s.isEmpty()) {
      throw new UnparsableNameException(NameType.OTHER, original);
    }

    // Inputs that are too short or that are just an HTML entity stub (no real
    // name content) — bail out before any regex work touches them.
    // A single bare letter ("X" / "a") is not a name. An abbreviated genus ("B.")
    // is allowed because the dot marks it as a stand-in for a longer name.
    if (countLetters(s) == 1) {
      String t = s.trim();
      boolean isAbbrev = t.length() == 2 && t.charAt(1) == '.' && Character.isLetter(t.charAt(0));
      if (!isAbbrev) {
        throw new UnparsableNameException(NameType.OTHER, original);
      }
    }
    if (HTML_ENTITY_NAMED.matcher(s).matches() || HTML_ENTITY_NUMERIC.matcher(s).matches()) {
      throw new UnparsableNameException(NameType.OTHER, original);
    }

    // NO_NAME markers (text deletion / discard tags). A leading "non " followed by a
    // proper Latin name is a homonym citation, not a deletion marker — leave those to
    // the regular parser path. Only reject "non" when it precedes a single short word
    // or punctuation (the typical checklist-cleanup leftover).
    String lower = s.toLowerCase();
    if (lower.contains("tobedeleted")
        || lower.contains("(delete)")
        || s.startsWith("@")
        || DELETE_MARKER.matcher(lower).matches()
        || lower.contains("[delete]")
        || lower.contains("[none]")) {
      throw new UnparsableNameException(NameType.OTHER, original);
    }
    if (lower.startsWith("non ")
        && (!NON_HOMONYM.matcher(s).matches() || s.contains("="))) {
      throw new UnparsableNameException(NameType.OTHER, original);
    }

    // Placeholder keywords first — some placeholder strings contain "virus" (e.g.
    // "uncultured virus") and the explicit keyword wins over the virus marker.
    if ((PLACEHOLDER_KEYWORDS.matcher(s).find()
        || NN_PLACEHOLDER.matcher(s).matches()
        || PLACEHOLDER_PREFIX.matcher(s).find()
        || s.startsWith("[unassigned]")
        || s.equalsIgnoreCase("Unaccepted"))
        && !INDET_SPECIES.matcher(s).matches()) {
      throw new UnparsableNameException(NameType.PLACEHOLDER, original);
    }

    // Virus — check before the leading-question-mark placeholder so that "? circular
    // satellites" reads as a virus rather than an unstructured placeholder.
    // Clean ICTV binomials/monomials with a viral genus suffix are let through to parse;
    // legacy vernacular virus names become OTHER + NomCode.VIRUS.
    applyVirusGate(s, ctx, original);

    // Monomial-aggregate forms ("Iteaphila-group", "Bartonella group", "Foo-complex"):
    // a single uninomial followed by an aggregate marker is an informal taxonomic
    // grouping label that the parser model can't represent.
    if (MONOMIAL_AGGREGATE.matcher(s).matches()) {
      throw new UnparsableNameException(NameType.INFORMAL, original);
    }

    // "-lineage" / " lineage" labels — informal phylogenetic lineage names (any rank).
    // Checked before the OTU/code rejections below so digit/lowercase stems like
    // "NC12A-lineage" and "he2-lineage" are flagged INFORMAL rather than OTHER.
    if (LINEAGE_LABEL.matcher(s).matches()) {
      throw new UnparsableNameException(NameType.INFORMAL, original);
    }

    // Leading "? <epithet>" — placeholder for missing genus. Only fully unparsable
    // when there's nothing else on the line; with authorship/year following, the
    // missing-genus form is reconstructed downstream (see StripAndStash).
    if (QUESTION_PREFIX.matcher(s).find()
        && !INDET_SPECIES.matcher(s).matches()
        && QUESTION_ONLY.matcher(s).matches()) {
      throw new UnparsableNameException(NameType.PLACEHOLDER, original);
    }

    // Phylogenetic clade label — not a Linnean name.
    if (CLADE_KEYWORD.matcher(s).find()) {
      throw new UnparsableNameException(NameType.INFORMAL, original);
    }

    // Pure code-like NO_NAME — use the normalised (trimmed) form as the exception name.
    if (OTU_BOLD.matcher(s).matches()
        || OTU_GTDB_UBA.matcher(s).matches()
        || GEN_NOV.matcher(s).matches()
        || s.startsWith("@")) {
      throw new UnparsableNameException(NameType.OTHER, s);
    }
    // SH identifiers are canonical in uppercase.
    if (OTU_SH.matcher(s).matches()) {
      throw new UnparsableNameException(NameType.OTHER, s.toUpperCase());
    }
    // pure alphanumeric mash with digit (no spaces) and no obvious Latin epithet
    if (!s.contains(" ") && PURE_ALPHANUM.matcher(s).matches() && hasDigit(s)
        && !isPlausibleSingleWordName(s)) {
      throw new UnparsableNameException(NameType.OTHER, s);
    }
    // PR2-style underscored name with hyphenated digit suffix → NO_NAME
    if (PR2_LIKE.matcher(s).matches() && s.contains("-") && hasDigit(s)) {
      throw new UnparsableNameException(NameType.OTHER, s);
    }
    // GTDB/SILVA specimen codes ending with "sp" + 8+ digits (e.g. "18JY21-1 sp004344915").
    if (s.contains(" ") && OTU_SPECIMEN_SUFFIX.matcher(s).find()) {
      throw new UnparsableNameException(NameType.OTHER, s);
    }
    // Multi-word input whose last token is a known OTU code (e.g. "Festuca sp. BOLD:ACW2100").
    if (s.contains(" ")) {
      String last = s.substring(s.lastIndexOf(' ') + 1);
      if (OTU_BOLD.matcher(last).matches()) {
        throw new UnparsableNameException(NameType.OTHER, last);
      }
      if (OTU_SH.matcher(last).matches()) {
        throw new UnparsableNameException(NameType.OTHER, last.toUpperCase());
      }
    }

    // Hybrid formula — only when the cross sits between two distinct name spans.
    if (looksLikeHybridFormula(s)) {
      throw new UnparsableNameException(NameType.FORMULA, original);
    }
  }

  private static void applyVirusGate(String s, ParseContext ctx, String original)
      throws UnparsableNameException {
    boolean clean = CLEAN_BINOMIAL.matcher(s).matches() || CLEAN_MONOMIAL.matcher(s).matches();
    org.gbif.nameparser.api.NomCode req = ctx.requestedCode;

    // Bucket A: clean uni/binomial whose genus/monomial carries a viral rank suffix.
    if (clean && ViralSuffix.isViral(firstWord(s))) {
      if (req == null || req == org.gbif.nameparser.api.NomCode.VIRUS) {
        ctx.viralShape = true;
      }
      return; // parse; a non-virus caller code is kept and wins over inference
    }
    if (!VIRUS.matcher(s).find()) {
      return; // no viral trigger at all
    }
    // Inline "Genus [(Subgenus)] epithet Author, YYYY" overrides a stray viral token.
    if (ZOOLOGICAL_BINOMIAL.matcher(s).find()) {
      return;
    }
    if (req == org.gbif.nameparser.api.NomCode.VIRUS) {
      if (clean) { ctx.viralShape = true; return; }
      throw new UnparsableNameException(NameType.OTHER, org.gbif.nameparser.api.NomCode.VIRUS, original);
    }
    // Soft virus-word as the leading GENUS with no genuinely viral token present → a real animal
    // genus (e.g. "Prion vittatus", "Prion Lacépède, 1799"), not a virus. Covers both the clean
    // binomial and the authored-monomial forms, which the last-word SOFT_WORD rescue below misses.
    if (SOFT_GENUS.matcher(s).find() && !HARD_VIRUS.matcher(s).find()) {
      return;
    }
    if (clean && req != null) {
      return; // caller asserts a non-virus code for a clean binomial
    }
    if (clean && SOFT_WORD.matcher(lastWord(s)).find()) {
      return; // bucket B soft
    }
    if (clean && HARD_WORD.matcher(lastWord(s)).find()
        && ctx.authorshipInput != null
        && AUTH_ZOO.matcher(ctx.authorshipInput.trim()).find()) {
      return; // bucket B hard, rescued by a separately supplied zoological author+year
    }
    throw new UnparsableNameException(NameType.OTHER, org.gbif.nameparser.api.NomCode.VIRUS, original);
  }

  private static String firstWord(String s) {
    int sp = s.indexOf(' ');
    return sp < 0 ? s : s.substring(0, sp);
  }

  private static String lastWord(String s) {
    int sp = s.lastIndexOf(' ');
    return sp < 0 ? s : s.substring(sp + 1);
  }

  private static boolean looksLikeHybridFormula(String s) {
    // A hybrid formula has the cross between two NAME spans where the left side contains
    // a *binomial* (Genus + epithet) or an authored monomial. A single "× epithet" (notho
    // marker) on either side does NOT count.
    int n = s.length();
    for (int i = 0; i < n; i++) {
      char c = s.charAt(i);
      boolean cross = c == '×';
      boolean asciiX = !cross && (c == 'x' || c == 'X')
          && i > 0 && i + 1 < n
          && s.charAt(i - 1) == ' ' && s.charAt(i + 1) == ' ';
      boolean plus = !cross && !asciiX && c == '+'
          && i > 0 && i + 1 < n
          && s.charAt(i - 1) == ' ' && s.charAt(i + 1) == ' ';
      if (!cross && !asciiX && !plus) continue;
      if (i == 0 || s.charAt(i - 1) != ' ') continue;
      // Right of cross must be whitespace-separated; otherwise ×x is a notho marker
      // glued to an epithet (e.g. "var. ×alpina").
      if (i + 1 >= s.length() || s.charAt(i + 1) != ' ') continue;
      String left = s.substring(0, i).trim();
      String right = s.substring(i + 1).trim();
      // Accept "?" as a valid right side (the second taxon is unspecified).
      boolean rightOk = containsLatinWord(right) || right.startsWith("?");
      if (!rightOk) continue;
      if (countLatinWords(left) >= 2 || hasAuthorAbbrev(left)) {
        return true;
      }
      // Graft-chimera formula: single genus on each side (e.g. "Crataegus + Mespilus")
      if (plus && countLatinWords(left) == 1
          && !left.isEmpty() && Character.isUpperCase(left.codePointAt(0))
          && !right.isEmpty() && Character.isUpperCase(right.trim().codePointAt(0))) {
        return true;
      }
    }
    return false;
  }

  private static int countLatinWords(String s) {
    Matcher m = LATIN_WORD.matcher(s);
    int count = 0;
    while (m.find()) count++;
    return count;
  }

  private static boolean containsLatinWord(String s) {
    return LATIN_WORD_MIN2.matcher(s).find();
  }

  private static boolean hasAuthorAbbrev(String s) {
    return AUTHOR_ABBREV.matcher(s).find();
  }

  private static int countLetters(String s) {
    int c = 0;
    for (int i = 0; i < s.length(); ) {
      int cp = s.codePointAt(i);
      if (Character.isLetter(cp)) c++;
      i += Character.charCount(cp);
    }
    return c;
  }

  private static boolean hasDigit(String s) {
    for (int i = 0; i < s.length(); i++) if (Character.isDigit(s.charAt(i))) return true;
    return false;
  }

  /** Conservative check: a single-token name with at least one digit is plausibly NOT scientific. */
  private static boolean isPlausibleSingleWordName(String s) {
    // A monomial with only letters and an internal hyphen (e.g. "Foo-bar") is plausible.
    // If it contains a digit at all, treat as code.
    return !hasDigit(s);
  }
}
