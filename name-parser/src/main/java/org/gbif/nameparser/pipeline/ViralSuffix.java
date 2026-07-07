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

  // Higher-taxon suffixes: longer ones (7+ chars) are unambiguous; the short realm
  // suffix -viria and kingdom suffix -virae are included unguarded because ICTV MSL41
  // has zero subrealm taxa (the formerly guarded -vira is omitted entirely to avoid
  // false positives such as the hummingbird genus Elvira or the word Mahavira).
  private static final Pattern HIGHER = Pattern.compile(
      "(?:viridae|viroidae|satellitidae"
      + "|virinae|viroinae|satellitinae"
      + "|virales|virineae"
      + "|viricetes|viricetidae|viricotina|viricota"
      + "|virites|viria|virae)$",
      Pattern.CASE_INSENSITIVE);

  static boolean isViral(String word) {
    if (word == null) return false;
    return GENUS.matcher(word).find()
        || HIGHER.matcher(word).find();
  }
}
