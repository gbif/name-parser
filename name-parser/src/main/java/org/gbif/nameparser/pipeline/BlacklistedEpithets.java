package org.gbif.nameparser.pipeline;

import org.gbif.nameparser.api.Warnings;
import org.gbif.nameparser.util.LineReader;

import java.util.HashSet;
import java.util.Set;

/**
 * Loads {@code nameparser/blacklist-epithets.txt} once and exposes a case-sensitive
 * membership check. Blacklisted epithets cause the parser to flag a name as doubtful
 * with a {@link Warnings#BLACKLISTED_EPITHET} warning.
 */
final class BlacklistedEpithets {

  private static final Set<String> EPITHETS = load();

  private BlacklistedEpithets() {}

  static boolean contains(String epithet) {
    return epithet != null && EPITHETS.contains(epithet.toLowerCase());
  }

  private static Set<String> load() {
    Set<String> out = new HashSet<>();
    try (LineReader lr = new LineReader(BlacklistedEpithets.class
        .getResourceAsStream("/nameparser/blacklist-epithets.txt"))) {
      for (String line : lr) {
        out.add(line.trim().toLowerCase());
      }
    }
    return out;
  }
}
