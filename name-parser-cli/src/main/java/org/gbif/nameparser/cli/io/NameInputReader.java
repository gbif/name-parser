package org.gbif.nameparser.cli.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Streaming reader for parser input. Implementations are single-pass iterators —
 * call {@link #next()} until it returns {@code null}, then {@link #close()}.
 *
 * <p>Use {@link #open(Path)}, {@link #open(InputStream)} or
 * {@link #open(BufferedReader)} (the most general) to pick the right concrete
 * reader. All three peek the first non-blank, non-comment line and decide
 * whether the input looks like a ColDP Name file or plain text — see
 * {@link InputDetector#classify(String)}.
 */
public interface NameInputReader extends AutoCloseable {
  /** Returns the next row or {@code null} once the input is exhausted. */
  NameInput next() throws IOException;

  /** Detected/declared format of the open file. */
  InputFormat format();

  @Override
  void close() throws IOException;

  static NameInputReader open(Path file) throws IOException {
    BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8);
    try {
      return open(br);
    } catch (IOException e) {
      br.close();
      throw e;
    }
  }

  static NameInputReader open(InputStream in) throws IOException {
    return open(new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)));
  }

  /**
   * Build a reader from any {@link BufferedReader} — used for stdin and other
   * non-file streams. The first non-blank, non-comment line is consumed to
   * detect the format; for plain text it becomes the first data row, for ColDP
   * it is the column header.
   */
  static NameInputReader open(BufferedReader reader) throws IOException {
    long lineNumber = 0;
    String line;
    while ((line = reader.readLine()) != null) {
      lineNumber++;
      if (line.isEmpty() || line.startsWith("#")) continue;
      InputFormat fmt = InputDetector.classify(line);
      switch (fmt) {
        case PLAIN:
          return new PlainTextReader(reader, line, lineNumber);
        case COLDP_TSV:
          return new ColdpReader(reader, '\t', line, lineNumber);
        case COLDP_CSV:
          return new ColdpReader(reader, ',', line, lineNumber);
        default:
          throw new IllegalStateException("Unhandled format " + fmt);
      }
    }
    // Empty input — return a reader that immediately yields end-of-stream.
    return new PlainTextReader(reader, null, 0);
  }
}
