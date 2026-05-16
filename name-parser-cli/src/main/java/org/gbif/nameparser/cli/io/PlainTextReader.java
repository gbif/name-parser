package org.gbif.nameparser.cli.io;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Reads one name per line from a {@link BufferedReader}. Blank lines, lines starting
 * with {@code #} and the literal {@code scientificName} TSV header are skipped. If a
 * line contains a tab, only the substring before the first tab is returned, so a
 * bare TSV (like {@code col-names.tsv}) can be fed in directly with the extra
 * columns silently ignored.
 */
final class PlainTextReader implements NameInputReader {
  private final BufferedReader reader;
  private long lineNumber = 0;

  PlainTextReader(BufferedReader reader) {
    this.reader = reader;
  }

  @Override
  public NameInput next() throws IOException {
    String raw;
    while ((raw = reader.readLine()) != null) {
      lineNumber++;
      if (raw.isEmpty() || raw.startsWith("#")) continue;
      String name = firstColumn(raw).trim();
      if (name.isEmpty() || "scientificName".equals(name)) continue;
      return new NameInput(lineNumber, null, name, null, null, null);
    }
    return null;
  }

  @Override
  public void close() throws IOException {
    reader.close();
  }

  private static String firstColumn(String line) {
    int t = line.indexOf('\t');
    return t < 0 ? line : line.substring(0, t);
  }
}
