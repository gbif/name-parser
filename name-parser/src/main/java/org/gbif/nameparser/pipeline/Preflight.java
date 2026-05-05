package org.gbif.nameparser.pipeline;

import org.gbif.nameparser.api.NameType;
import org.gbif.nameparser.api.UnparsableNameException;

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

  // ---------- HYBRID FORMULA ----------
  // "Genus epithet ... × Genus epithet ..." — at least one × OR a lone " x " between two name-like spans.
  // To avoid false positives on single-genus hybrids ("× Foo bar" / "Foo × bar"), we require something
  // that looks like a second name (uppercase or epithet) on either side of the cross.
  private static final Pattern HYBRID_FORMULA = Pattern.compile(
      "^[\\s\\S]*?\\b(?:[\\p{Lu}][\\p{L}.\\-]+(?:\\s+[\\p{Ll}][\\p{L}.\\-]+)+|" +
          "[\\p{Lu}][\\p{L}.\\-]+)\\s*[×x]\\s+" +
          "(?:[\\p{Lu}][\\p{L}.\\-]+|[\\p{Ll}][\\p{L}.\\-]{2,})" +
          "(?:\\s+[\\p{Ll}][\\p{L}.\\-]+|\\s+[\\p{Lu}]\\.?[\\p{L}.\\-]*)+",
      Pattern.UNICODE_CHARACTER_CLASS);

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
          "incertae\\s*sedis|inc\\.\\s*sed\\.?|incertaesedis" +
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

  private Preflight() {}

  /**
   * If the input matches a non-scientific category, throws {@link UnparsableNameException}
   * with the appropriate {@link NameType}. Otherwise returns silently.
   */
  static void run(String original, String working) throws UnparsableNameException {
    String s = working.trim();
    if (s.isEmpty()) {
      throw new UnparsableNameException(NameType.NO_NAME, original);
    }

    // Inputs that are too short or that are just an HTML entity stub (no real
    // name content) — bail out before any regex work touches them.
    // A single bare letter ("X" / "a") is not a name. An abbreviated genus ("B.")
    // is allowed because the dot marks it as a stand-in for a longer name.
    if (countLetters(s) == 1) {
      String t = s.trim();
      boolean isAbbrev = t.length() == 2 && t.charAt(1) == '.' && Character.isLetter(t.charAt(0));
      if (!isAbbrev) {
        throw new UnparsableNameException(NameType.NO_NAME, original);
      }
    }
    if (s.matches("&[a-zA-Z]+;") || s.matches("&#\\d+;")) {
      throw new UnparsableNameException(NameType.NO_NAME, original);
    }

    // NO_NAME markers (text deletion / discard tags).
    String lower = s.toLowerCase();
    if (lower.contains("tobedeleted")
        || lower.contains("(delete)")
        || s.startsWith("@")) {
      throw new UnparsableNameException(NameType.NO_NAME, original);
    }

    // Placeholder first — some placeholder strings contain "virus" (e.g. "uncultured virus").
    if (PLACEHOLDER_KEYWORDS.matcher(s).find()
        || NN_PLACEHOLDER.matcher(s).matches()
        || PLACEHOLDER_PREFIX.matcher(s).find()
        || QUESTION_PREFIX.matcher(s).find()
        || s.startsWith("[unassigned]")
        || s.equalsIgnoreCase("Unaccepted")) {
      throw new UnparsableNameException(NameType.PLACEHOLDER, original);
    }

    // Virus
    if (VIRUS.matcher(s).find()) {
      throw new UnparsableNameException(NameType.VIRUS, original);
    }

    // Pure code-like NO_NAME — use the normalised (trimmed) form as the exception name.
    if (OTU_BOLD.matcher(s).matches()
        || OTU_GTDB_UBA.matcher(s).matches()
        || GEN_NOV.matcher(s).matches()
        || s.startsWith("@")) {
      throw new UnparsableNameException(NameType.NO_NAME, s);
    }
    // SH identifiers are canonical in uppercase.
    if (OTU_SH.matcher(s).matches()) {
      throw new UnparsableNameException(NameType.NO_NAME, s.toUpperCase());
    }
    // pure alphanumeric mash with digit (no spaces) and no obvious Latin epithet
    if (!s.contains(" ") && PURE_ALPHANUM.matcher(s).matches() && hasDigit(s)
        && !isPlausibleSingleWordName(s)) {
      throw new UnparsableNameException(NameType.NO_NAME, s);
    }
    // PR2-style underscored name with hyphenated digit suffix → NO_NAME
    if (PR2_LIKE.matcher(s).matches() && s.contains("-") && hasDigit(s)) {
      throw new UnparsableNameException(NameType.NO_NAME, s);
    }
    // GTDB/SILVA specimen codes ending with "sp" + 8+ digits (e.g. "18JY21-1 sp004344915").
    if (s.contains(" ") && OTU_SPECIMEN_SUFFIX.matcher(s).find()) {
      throw new UnparsableNameException(NameType.NO_NAME, s);
    }
    // Multi-word input whose last token is a known OTU code (e.g. "Festuca sp. BOLD:ACW2100").
    if (s.contains(" ")) {
      String last = s.substring(s.lastIndexOf(' ') + 1);
      if (OTU_BOLD.matcher(last).matches()) {
        throw new UnparsableNameException(NameType.NO_NAME, last);
      }
      if (OTU_SH.matcher(last).matches()) {
        throw new UnparsableNameException(NameType.NO_NAME, last.toUpperCase());
      }
    }

    // Hybrid formula — only when the cross sits between two distinct name spans.
    if (looksLikeHybridFormula(s)) {
      throw new UnparsableNameException(NameType.HYBRID_FORMULA, original);
    }
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
      if (!cross && !asciiX) continue;
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
    }
    return false;
  }

  private static int countLatinWords(String s) {
    java.util.regex.Matcher m = java.util.regex.Pattern
        .compile("[\\p{L}][\\p{L}.\\-]+", Pattern.UNICODE_CHARACTER_CLASS).matcher(s);
    int count = 0;
    while (m.find()) count++;
    return count;
  }

  private static boolean containsLatinWord(String s) {
    return java.util.regex.Pattern
        .compile("[\\p{L}]{2,}", Pattern.UNICODE_CHARACTER_CLASS).matcher(s).find();
  }

  private static boolean hasAuthorAbbrev(String s) {
    return java.util.regex.Pattern
        .compile("\\b[\\p{Lu}][\\p{L}]*\\.", Pattern.UNICODE_CHARACTER_CLASS).matcher(s).find();
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
