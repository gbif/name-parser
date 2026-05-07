package org.gbif.nameparser.cli.io;

import life.catalogue.coldp.ColdpTerm;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Sniffs an input file's first non-blank, non-comment line and decides whether it
 * looks like a ColDP Name file (TSV or CSV) or a plain-text list with one name
 * per line.
 *
 * <p>Detection is conservative: a row is treated as a ColDP header only if at
 * least one column header resolves to a {@link ColdpTerm} via
 * {@link ColdpTerm#find(String, boolean)}. If the first column happens to be an
 * actual scientific name we fall through to {@link InputFormat#PLAIN}.
 */
public final class InputDetector {
  private InputDetector() {}

  /** Read the first usable header line from a file and classify it. */
  public static InputFormat detect(Path file) throws IOException {
    try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
      String line;
      while ((line = r.readLine()) != null) {
        if (line.isEmpty() || line.startsWith("#")) continue;
        return classify(line);
      }
      return InputFormat.PLAIN;
    }
  }

  /** Classify a single header line. Visible for testing and for stream input. */
  public static InputFormat classify(String headerLine) {
    char delim = Csv.detectDelimiter(headerLine);
    String[] cols = Csv.split(headerLine, delim);
    if (cols.length < 1) return InputFormat.PLAIN;
    boolean anyColdp = false;
    for (String c : cols) {
      if (c == null || c.isBlank()) continue;
      if (ColdpTerm.find(c.trim(), false) != null) {
        anyColdp = true;
        break;
      }
    }
    if (!anyColdp) return InputFormat.PLAIN;
    return delim == '\t' ? InputFormat.COLDP_TSV : InputFormat.COLDP_CSV;
  }
}
