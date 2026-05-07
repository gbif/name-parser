package org.gbif.nameparser.cli;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * {@code compare} subcommand — streams two JSONL files in lockstep, walks each
 * row's parsed result tree and reports both aggregate metrics (rows compared / rows
 * differing, status transitions, top differing fields) and a per-row dump of every
 * differing leaf value.
 *
 * <p>Both inputs are expected to come from the same source file (matching line
 * numbers, same row order). If the {@code line} fields disagree it is counted as a
 * mismatch but comparison continues. Streaming: only small running counters are
 * kept in memory regardless of input size.
 *
 * <p>Whitespace inside parsed string values is significant by default. Use
 * {@code --ignore-whitespace} to strip all whitespace from string leaves before
 * comparing (useful when an authorship rendering tweak only changes spacing).
 *
 * <p>Options:
 * <ul>
 *   <li>{@code --a=PATH}, {@code --b=PATH} — JSONL files to compare (or pass them positionally)</li>
 *   <li>{@code --output=PATH} — write per-row diffs here instead of stdout</li>
 *   <li>{@code --ignore-whitespace} — strip whitespace from string leaves before comparing</li>
 *   <li>{@code --max-diffs=N} — cap per-row diff dump at N rows (default: 100)</li>
 *   <li>{@code -h --help} — print usage and exit</li>
 * </ul>
 */
public final class CompareCli {
  private CompareCli() {}

  public static void main(String[] argv) throws Exception {
    Args a = Args.parse(argv);
    if (a.wantsHelp()) {
      printUsage();
      return;
    }
    Path fa = a.path("a", a.positionalPath(0));
    Path fb = a.path("b", a.positionalPath(1));
    Path diffsOut = a.path("output", a.positionalPath(2));
    boolean ignoreWs = a.flag("ignore-whitespace");
    int maxDiffs = a.integer("max-diffs", 100);

    if (fa == null || fb == null) {
      System.err.println("Two JSONL files are required.");
      System.err.println();
      printUsage();
      System.exit(2);
    }

    PrintStream summary = System.out;
    if (diffsOut == null) {
      Report r = compare(fa, fb, ignoreWs, maxDiffs, new PrintWriter(System.out, true));
      r.printSummary(summary);
    } else {
      try (BufferedWriter w = Files.newBufferedWriter(diffsOut, StandardCharsets.UTF_8);
           PrintWriter pw = new PrintWriter(w)) {
        Report r = compare(fa, fb, ignoreWs, maxDiffs, pw);
        r.printSummary(summary);
        summary.println("Per-row diffs written to " + diffsOut.toAbsolutePath());
      }
    }
  }

  /**
   * Stream both files, compare row-by-row, write per-row diffs to {@code diffSink}
   * and return the running counters.
   */
  public static Report compare(Path a, Path b, boolean ignoreWhitespace, int maxDiffs,
                               PrintWriter diffSink) throws IOException {
    Report report = new Report();
    report.fileA = a.toString();
    report.fileB = b.toString();
    report.ignoreWhitespace = ignoreWhitespace;

    try (BufferedReader ra = Files.newBufferedReader(a, StandardCharsets.UTF_8);
         BufferedReader rb = Files.newBufferedReader(b, StandardCharsets.UTF_8)) {
      while (true) {
        String la = ra.readLine();
        String lb = rb.readLine();
        if (la == null && lb == null) break;
        if (la == null) {
          long extra = 1;
          while (rb.readLine() != null) extra++;
          report.extraRowsB = extra;
          break;
        }
        if (lb == null) {
          long extra = 1;
          while (ra.readLine() != null) extra++;
          report.extraRowsA = extra;
          break;
        }
        report.rowsCompared++;
        JsonObject oa = JsonParser.parseString(la).getAsJsonObject();
        JsonObject ob = JsonParser.parseString(lb).getAsJsonObject();

        long lineA = oa.has("line") ? oa.get("line").getAsLong() : -1;
        long lineB = ob.has("line") ? ob.get("line").getAsLong() : -1;
        if (lineA != lineB) {
          report.lineNumberMismatches++;
        }

        Status sa = statusOf(oa);
        Status sb = statusOf(ob);
        if (sa != sb) {
          String key = sa + "→" + sb;
          report.statusTransitions.merge(key, 1L, Long::sum);
        }

        List<Diff> diffs = new ArrayList<>();
        diffElement("", oa, ob, ignoreWhitespace, diffs);
        if (!diffs.isEmpty()) {
          report.rowsDiffered++;
          for (Diff d : diffs) {
            report.fieldDiffCounts.merge(d.path, 1L, Long::sum);
          }
          if (report.rowsDiffered <= maxDiffs) {
            String input = oa.has("input") ? oa.get("input").getAsString()
                : (ob.has("input") ? ob.get("input").getAsString() : "?");
            diffSink.printf("Line %d \"%s\" (status %s vs %s):%n", lineA, input, sa, sb);
            for (Diff d : diffs) {
              diffSink.printf("  %-44s  %s  →  %s%n", d.path, abbreviate(d.left), abbreviate(d.right));
            }
          } else if (report.rowsDiffered == maxDiffs + 1) {
            diffSink.printf("… further per-row diffs suppressed (--max-diffs=%d)%n", maxDiffs);
          }
        }
      }
    }
    diffSink.flush();
    return report;
  }

  private static Status statusOf(JsonObject row) {
    if (row.has("error") && !row.get("error").isJsonNull()) return Status.ERROR;
    if (row.has("parsed") && !row.get("parsed").isJsonNull()) return Status.PARSED;
    return Status.EMPTY;
  }

  private enum Status { PARSED, ERROR, EMPTY }

  private static void diffElement(String path, JsonElement a, JsonElement b,
                                  boolean ignoreWhitespace, List<Diff> out) {
    if (jsonEquals(a, b, ignoreWhitespace)) return;
    if (a.isJsonObject() && b.isJsonObject()) {
      JsonObject oa = a.getAsJsonObject();
      JsonObject ob = b.getAsJsonObject();
      TreeSet<String> keys = new TreeSet<>();
      for (String k : oa.keySet()) keys.add(k);
      for (String k : ob.keySet()) keys.add(k);
      for (String k : keys) {
        JsonElement va = oa.has(k) ? oa.get(k) : JsonNull.INSTANCE;
        JsonElement vb = ob.has(k) ? ob.get(k) : JsonNull.INSTANCE;
        diffElement(path.isEmpty() ? k : path + "." + k, va, vb, ignoreWhitespace, out);
      }
    } else if (a.isJsonArray() && b.isJsonArray()) {
      JsonArray aa = a.getAsJsonArray();
      JsonArray ab = b.getAsJsonArray();
      int n = Math.max(aa.size(), ab.size());
      for (int i = 0; i < n; i++) {
        JsonElement va = i < aa.size() ? aa.get(i) : JsonNull.INSTANCE;
        JsonElement vb = i < ab.size() ? ab.get(i) : JsonNull.INSTANCE;
        diffElement(path + "[" + i + "]", va, vb, ignoreWhitespace, out);
      }
    } else {
      out.add(new Diff(path, render(a), render(b)));
    }
  }

  private static boolean jsonEquals(JsonElement a, JsonElement b, boolean ignoreWhitespace) {
    if (a == b) return true;
    if (a.isJsonNull() || b.isJsonNull()) return a.isJsonNull() && b.isJsonNull();
    if (a.isJsonPrimitive() && b.isJsonPrimitive()) {
      JsonPrimitive pa = a.getAsJsonPrimitive();
      JsonPrimitive pb = b.getAsJsonPrimitive();
      if (ignoreWhitespace && pa.isString() && pb.isString()) {
        return stripWs(pa.getAsString()).equals(stripWs(pb.getAsString()));
      }
      return pa.equals(pb);
    }
    if (a.isJsonArray() && b.isJsonArray()) {
      JsonArray aa = a.getAsJsonArray();
      JsonArray ab = b.getAsJsonArray();
      if (aa.size() != ab.size()) return false;
      for (int i = 0; i < aa.size(); i++) {
        if (!jsonEquals(aa.get(i), ab.get(i), ignoreWhitespace)) return false;
      }
      return true;
    }
    if (a.isJsonObject() && b.isJsonObject()) {
      JsonObject oa = a.getAsJsonObject();
      JsonObject ob = b.getAsJsonObject();
      if (!oa.keySet().equals(ob.keySet())) return false;
      for (String k : oa.keySet()) {
        if (!jsonEquals(oa.get(k), ob.get(k), ignoreWhitespace)) return false;
      }
      return true;
    }
    return false;
  }

  private static String stripWs(String s) {
    int n = s.length();
    StringBuilder b = new StringBuilder(n);
    for (int i = 0; i < n; i++) {
      char c = s.charAt(i);
      if (!Character.isWhitespace(c)) b.append(c);
    }
    return b.toString();
  }

  private static String render(JsonElement e) {
    if (e == null || e.isJsonNull()) return "null";
    if (e.isJsonPrimitive()) {
      JsonPrimitive p = e.getAsJsonPrimitive();
      if (p.isString()) return "\"" + p.getAsString() + "\"";
      return p.getAsString();
    }
    return e.toString();
  }

  private static String abbreviate(String s) {
    int max = 80;
    if (s.length() <= max) return s;
    return s.substring(0, max - 1) + "…";
  }

  private static void printUsage() {
    System.out.println("Usage: name-parser-cli compare [options] <a.jsonl> <b.jsonl> [diffs.txt]");
    System.out.println();
    System.out.println("Stream two JSONL files produced by 'parse' in lockstep and report");
    System.out.println("aggregate metrics plus per-row diffs.");
    System.out.println();
    System.out.println("Options:");
    System.out.println("  --a=PATH              first JSONL file (alt. to first positional arg)");
    System.out.println("  --b=PATH              second JSONL file (alt. to second positional arg)");
    System.out.println("  --output=PATH         write per-row diffs here (default: stdout)");
    System.out.println("  --ignore-whitespace   strip whitespace from string leaves before compare");
    System.out.println("  --max-diffs=N         cap per-row diff dump at N rows (default: 100)");
    System.out.println("  -h --help             print this message and exit");
  }

  private static final class Diff {
    final String path;
    final String left;
    final String right;
    Diff(String path, String left, String right) {
      this.path = path;
      this.left = left;
      this.right = right;
    }
  }

  public static final class Report {
    public String fileA;
    public String fileB;
    public boolean ignoreWhitespace;
    public long rowsCompared;
    public long rowsDiffered;
    public long lineNumberMismatches;
    public long extraRowsA;
    public long extraRowsB;
    public final Map<String, Long> statusTransitions = new LinkedHashMap<>();
    public final Map<String, Long> fieldDiffCounts = new LinkedHashMap<>();

    public void printSummary(PrintStream out) {
      out.println("=== JSONL comparison summary ===");
      out.println("A: " + fileA);
      out.println("B: " + fileB);
      out.println("ignore-whitespace: " + ignoreWhitespace);
      out.printf("Rows compared:      %,d%n", rowsCompared);
      out.printf("Rows identical:     %,d%n", rowsCompared - rowsDiffered);
      out.printf("Rows differing:     %,d (%.2f%%)%n",
          rowsDiffered,
          rowsCompared == 0 ? 0.0 : 100.0 * rowsDiffered / rowsCompared);
      if (lineNumberMismatches > 0) {
        out.printf("Line-number mismatches: %,d%n", lineNumberMismatches);
      }
      if (extraRowsA > 0) out.printf("Extra rows in A: %,d%n", extraRowsA);
      if (extraRowsB > 0) out.printf("Extra rows in B: %,d%n", extraRowsB);

      if (!statusTransitions.isEmpty()) {
        out.println();
        out.println("Status transitions (parsed/error/empty):");
        statusTransitions.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .forEach(e -> out.printf("  %-20s %,d%n", e.getKey(), e.getValue()));
      }

      if (!fieldDiffCounts.isEmpty()) {
        out.println();
        out.println("Top differing fields:");
        fieldDiffCounts.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(40)
            .forEach(e -> out.printf("  %-44s %,d%n", e.getKey(), e.getValue()));
      }
    }
  }
}
