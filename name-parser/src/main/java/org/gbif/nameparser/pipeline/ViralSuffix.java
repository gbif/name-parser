package org.gbif.nameparser.pipeline;

import java.util.regex.Pattern;

/**
 * Recognises the standardized ICTV viral rank suffixes on a single word (genus,
 * monomial, or higher-taxon name). Per MSL41 every virus genus ends in one of these,
 * so the suffix alone is a reliable virus signal.
 *
 * <p>Only the <b>singular</b> canonical suffixes are matched, so Linnaean look-alikes
 * such as the mollusk genus {@code Crassatellites} ({@code -satellites}) are not
 * misread as viral.
 */
final class ViralSuffix {
  private ViralSuffix() {}

  private static final Pattern GENUS = Pattern.compile(
      "(?:virus|viroid|satellite|viriform)$", Pattern.CASE_INSENSITIVE);

  // Longer, unambiguous higher-taxon suffixes (7+ chars): safe for any word length.
  private static final Pattern HIGHER_LONG = Pattern.compile(
      "(?:viridae|viroidae|satellitidae"
      + "|virinae|viroinae|satellitinae"
      + "|virales|virineae"
      + "|viricetes|viricetidae|viricotina|viricota"
      + "|virites)$",
      Pattern.CASE_INSENSITIVE);

  // Short realm/kingdom suffixes (4–5 chars): require at least 5 characters before
  // the suffix so short names like "Mahavira" (only 4 chars before -vira) are excluded.
  // Real ICTV realm/kingdom names (e.g. "Orthornaviria", "Riboviria") have long stems.
  private static final Pattern HIGHER_SHORT = Pattern.compile(
      ".{5,}(?:virae|viria|vira)$",
      Pattern.CASE_INSENSITIVE);

  static boolean isViral(String word) {
    if (word == null) return false;
    return GENUS.matcher(word).find()
        || HIGHER_LONG.matcher(word).find()
        || HIGHER_SHORT.matcher(word).matches();
  }
}
