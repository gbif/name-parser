package org.gbif.nameparser.cli.io;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.gbif.nameparser.cli.ParseResult;

import java.io.IOException;
import java.io.Writer;

/**
 * Writes a single JSON document holding an array of parsed rows. Streams the rows
 * out one at a time — no in-memory accumulation — by writing the opening bracket
 * eagerly, comma-separating subsequent rows, and closing the bracket on
 * {@link #close()}.
 */
final class JsonArrayWriter implements NameOutputWriter {
  private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
  private final Writer out;
  private boolean started;
  private boolean closed;

  JsonArrayWriter(Writer out) throws IOException {
    this.out = out;
    this.out.write('[');
  }

  @Override
  public void write(ParseResult row) throws IOException {
    if (started) {
      out.write(',');
    }
    out.write('\n');
    out.write(GSON.toJson(row));
    started = true;
  }

  @Override
  public void close() throws IOException {
    if (closed) return;
    closed = true;
    if (started) out.write('\n');
    out.write(']');
    out.write('\n');
    out.close();
  }
}
