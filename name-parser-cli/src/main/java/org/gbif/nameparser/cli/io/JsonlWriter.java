package org.gbif.nameparser.cli.io;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.gbif.nameparser.cli.ParseResult;

import java.io.IOException;
import java.io.Writer;

/**
 * Writes one JSON object per line — the format consumed by {@code compare}.
 */
final class JsonlWriter implements NameOutputWriter {
  private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
  private final Writer out;

  JsonlWriter(Writer out) {
    this.out = out;
  }

  @Override
  public void write(ParseResult row) throws IOException {
    out.write(GSON.toJson(row));
    out.write('\n');
  }

  @Override
  public void close() throws IOException {
    out.close();
  }
}
