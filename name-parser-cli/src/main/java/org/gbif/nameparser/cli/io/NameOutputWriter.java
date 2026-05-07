package org.gbif.nameparser.cli.io;

import org.gbif.nameparser.cli.ParseResult;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Writes one parsed row at a time. Implementations must NOT buffer rows in memory —
 * all writers in this package stream to disk (or to any other {@link Writer}).
 */
public interface NameOutputWriter extends AutoCloseable {
  void write(ParseResult row) throws IOException;

  @Override
  void close() throws IOException;

  static NameOutputWriter open(Path file, OutputFormat format) throws IOException {
    return open(Files.newBufferedWriter(file, StandardCharsets.UTF_8), format);
  }

  static NameOutputWriter open(OutputStream out, OutputFormat format) throws IOException {
    return open(new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8)), format);
  }

  static NameOutputWriter open(Writer w, OutputFormat format) throws IOException {
    return switch (format) {
      case JSONL -> new JsonlWriter(w);
      case JSON  -> new JsonArrayWriter(w);
      case CSV   -> new ColdpWriter(new PrintWriter(w), ',');
      case TSV   -> new ColdpWriter(new PrintWriter(w), '\t');
    };
  }
}
