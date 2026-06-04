package org.gbif.nameparser.token;

import java.util.Set;

/**
 * Lower-case particles that combine with the following capitalised author surname
 * (e.g. "de Vriese", "Van Heurck", "von der Linde"). Multiple particles can chain.
 */
public final class AuthorParticles {

  private static final Set<String> PARTICLES = Set.of(
      "a", "ab", "af", "ap", "auf",
      "d", "da", "dal", "dalla", "dalle", "dallo",
      "das", "de", "degli", "dei", "del", "della", "delle", "delli", "dello",
      "der", "des", "di", "do", "dos", "du",
      "el", "in",
      "la", "las", "le", "les", "lo", "los",
      "of", "ofver",
      "te", "ten", "ter",
      "und", "v", "van", "vd", "ven", "vom", "von",
      "y",
      "zu", "zum", "zur"
  );

  private AuthorParticles() {}

  public static boolean isParticle(String word) {
    return PARTICLES.contains(word.toLowerCase());
  }

  /** Capitalised particles like "Van" (Dutch convention) — also valid as start-of-author. */
  public static boolean isCapitalisedParticle(String word) {
    return word.length() > 0 && isParticle(word) && Character.isUpperCase(word.charAt(0));
  }
}
