package org.gbif.nameparser.cli.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Streaming reader for parser input. Single-pass iterator — call {@link #next()}
 * until it returns {@code null}, then {@link #close()}.
 *
 * <p>The dev branch only supports plain text input (one name per line). If a line
 * contains a tab, only the substring before the first tab is used as the name —
 * which keeps the bundled {@code col-names.tsv} usable verbatim, just with the
 * authorship column ignored.
 */
public interface NameInputReader extends AutoCloseable {
  /** Returns the next row or {@code null} once the input is exhausted. */
  NameInput next() throws IOException;

  @Override
  void close() throws IOException;

  static NameInputReader open(Path file) throws IOException {
    return new PlainTextReader(Files.newBufferedReader(file, StandardCharsets.UTF_8));
  }

  static NameInputReader open(InputStream in) {
    return new PlainTextReader(new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)));
  }
}
