package org.gbif.nameparser.cli.io;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal CSV / TSV helpers for single-line records.
 *
 * <p>Field rules follow RFC 4180:
 * <ul>
 *   <li>Fields are separated by the configured delimiter.</li>
 *   <li>A field may be enclosed in double quotes; a literal double quote inside a
 *       quoted field is escaped by doubling it ({@code ""}).</li>
 *   <li>Embedded newlines are NOT supported (records are assumed to be single-line).</li>
 *   <li>For TSV ({@code '\t'} delimiter) values are written without quoting; any
 *       embedded tab, newline or carriage return is replaced with a single space.</li>
 * </ul>
 *
 * <p>This is intentionally small — for our throughput-oriented CLI we don't need a
 * full CSV library and the ColDP files we produce/consume don't carry multi-line
 * values.
 */
public final class Csv {
  private Csv() {}

  /** Split a single-line record into fields. */
  public static String[] split(String line, char delimiter) {
    List<String> out = new ArrayList<>();
    StringBuilder cur = new StringBuilder();
    boolean inQuotes = false;
    int n = line.length();
    for (int i = 0; i < n; i++) {
      char c = line.charAt(i);
      if (inQuotes) {
        if (c == '"') {
          if (i + 1 < n && line.charAt(i + 1) == '"') {
            cur.append('"');
            i++;
          } else {
            inQuotes = false;
          }
        } else {
          cur.append(c);
        }
      } else {
        if (c == '"' && cur.length() == 0) {
          inQuotes = true;
        } else if (c == delimiter) {
          out.add(cur.toString());
          cur.setLength(0);
        } else {
          cur.append(c);
        }
      }
    }
    out.add(cur.toString());
    return out.toArray(new String[0]);
  }

  /** Quote a single field for CSV writing. Returns the input unchanged if no quoting is needed. */
  public static String quoteCsv(String s) {
    if (s == null) return "";
    boolean needsQuote = false;
    int n = s.length();
    for (int i = 0; i < n; i++) {
      char c = s.charAt(i);
      if (c == ',' || c == '"' || c == '\n' || c == '\r') {
        needsQuote = true;
        break;
      }
    }
    if (!needsQuote) return s;
    StringBuilder b = new StringBuilder(n + 4);
    b.append('"');
    for (int i = 0; i < n; i++) {
      char c = s.charAt(i);
      if (c == '"') b.append('"');
      b.append(c);
    }
    b.append('"');
    return b.toString();
  }

  /** Sanitise a TSV field — replaces tabs, newlines and carriage returns with single spaces. */
  public static String sanitiseTsv(String s) {
    if (s == null) return "";
    int n = s.length();
    StringBuilder b = null;
    for (int i = 0; i < n; i++) {
      char c = s.charAt(i);
      if (c == '\t' || c == '\n' || c == '\r') {
        if (b == null) {
          b = new StringBuilder(n);
          b.append(s, 0, i);
        }
        b.append(' ');
      } else if (b != null) {
        b.append(c);
      }
    }
    return b == null ? s : b.toString();
  }

  /** Heuristic — sniff the most likely delimiter for a header row. */
  public static char detectDelimiter(String header) {
    int tabs = 0, commas = 0;
    int n = header.length();
    for (int i = 0; i < n; i++) {
      char c = header.charAt(i);
      if (c == '\t') tabs++;
      else if (c == ',') commas++;
    }
    return tabs >= commas && tabs > 0 ? '\t' : ',';
  }
}
