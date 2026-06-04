package org.gbif.nameparser.cli.io;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Reader for the simplest input shape: one name per line. Blank lines, lines
 * starting with {@code #} and the literal {@code scientificName} TSV header are
 * skipped. If a line contains a tab, only the substring before the first tab is
 * returned (preserves compatibility with the bundled {@code col-names.tsv}).
 *
 * <p>The constructor accepts an optional already-consumed first line and its line
 * number — used by {@link NameInputReader#open(BufferedReader)} when the format
 * was detected by peeking at the first record.
 */
final class PlainTextReader implements NameInputReader {
  private final BufferedReader reader;
  private String pushedBack;
  private long pushedBackLine;
  private long lineNumber;

  /**
   * @param reader            stream positioned just after {@code firstLine} (if
   *                          provided); the reader becomes the owner.
   * @param firstLine         optional line that has already been consumed from
   *                          the stream by the caller. {@code null} means start
   *                          fresh from {@code reader}.
   * @param firstLineNumber   1-based line number of {@code firstLine} (ignored if
   *                          {@code firstLine == null}).
   */
  PlainTextReader(BufferedReader reader, String firstLine, long firstLineNumber) {
    this.reader = reader;
    this.pushedBack = firstLine;
    this.pushedBackLine = firstLineNumber;
    this.lineNumber = firstLineNumber - (firstLine == null ? 0 : 1);
  }

  @Override
  public NameInput next() throws IOException {
    String raw;
    while ((raw = readNext()) != null) {
      if (raw.isEmpty() || raw.startsWith("#")) continue;
      String name = firstColumn(raw).trim();
      if (name.isEmpty() || "scientificName".equals(name)) continue;
      return new NameInput(lineNumber, null, name, null, null, null);
    }
    return null;
  }

  private String readNext() throws IOException {
    if (pushedBack != null) {
      String x = pushedBack;
      pushedBack = null;
      lineNumber = pushedBackLine;
      return x;
    }
    String line = reader.readLine();
    if (line != null) lineNumber++;
    return line;
  }

  @Override
  public InputFormat format() {
    return InputFormat.PLAIN;
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
