/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.nameparser;

import org.gbif.nameparser.api.NomCode;
import org.gbif.nameparser.api.Rank;
import org.gbif.nameparser.api.UnparsableNameException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Standalone benchmark for {@link NameParserGBIF}.
 *
 * Reads a TSV input file with up to three columns: scientific name, rank, nomenclatural code.
 * Rank and code are optional and matched case-insensitively against the {@link Rank} and
 * {@link NomCode} enum constant names. Lines starting with '#' or matching the literal header
 * "name\trank\tcode" are skipped. Empty lines and blank names are ignored.
 *
 * Reports count, average, min, max, p50 and p95 parsing times to stdout, and writes a per-name
 * log sorted by descending parse time to a configurable log file.
 *
 * Usage:
 *   java org.gbif.nameparser.Benchmark &lt;input.tsv&gt; [output.log]
 */
public class Benchmark {
  private final NameParserGBIF parser;

  public Benchmark(NameParserGBIF parser) {
    this.parser = parser;
  }

  public static void main(String[] args) throws Exception {
    if (args.length < 1) {
      System.err.println("Usage: Benchmark <input.tsv> [output.log]");
      System.exit(1);
    }
    Path input = Paths.get(args[0]);
    Path log = args.length > 1 ? Paths.get(args[1]) : Paths.get("benchmark.log");

    try (NameParserGBIF parser = new NameParserGBIF(5_000)) {
      Result r = new Benchmark(parser).run(input);
      r.report(System.out);
      r.writeSortedLog(log);
      System.out.println("Wrote per-name log to " + log.toAbsolutePath());
    }
  }

  public Result run(Path tsv) throws IOException {
    List<Timing> timings = new ArrayList<>();
    int parseFailures = 0;
    long warmup = 50;
    long lineNo = 0;
    try (BufferedReader r = Files.newBufferedReader(tsv, StandardCharsets.UTF_8)) {
      String line;
      while ((line = r.readLine()) != null) {
        lineNo++;
        if (line.isEmpty() || line.startsWith("#")) continue;
        String name = line.trim();
        if (name.isEmpty()) continue;

        long t0 = System.nanoTime();
        boolean ok = true;
        try {
          parser.parse(name);
        } catch (UnparsableNameException e) {
          ok = false;
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new IOException("Interrupted", e);
        }
        long elapsed = System.nanoTime() - t0;

        if (warmup > 0) {
          warmup--;
        } else {
          timings.add(new Timing(name, elapsed, ok));
          if (!ok) parseFailures++;
        }
      }
    }
    return new Result(timings, parseFailures);
  }

  private static String col(String[] cols, int i) {
    return i < cols.length ? cols[i].trim() : "";
  }

  private static Rank parseRank(String s) {
    if (s == null || s.isEmpty()) return Rank.UNRANKED;
    try {
      return Rank.valueOf(s.toUpperCase());
    } catch (IllegalArgumentException e) {
      return Rank.UNRANKED;
    }
  }

  private static NomCode parseCode(String s) {
    if (s == null || s.isEmpty()) return null;
    try {
      return NomCode.valueOf(s.toUpperCase());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  static final class Timing {
    final String name;
    final long nanos;
    final boolean parsed;

    Timing(String name, long nanos, boolean parsed) {
      this.name = name;
      this.nanos = nanos;
      this.parsed = parsed;
    }
  }

  public static final class Result {
    private final List<Timing> timings;
    private final int parseFailures;

    Result(List<Timing> timings, int parseFailures) {
      this.timings = timings;
      this.parseFailures = parseFailures;
    }

    public int count() { return timings.size(); }
    public int failures() { return parseFailures; }

    public void report(PrintStream out) {
      if (timings.isEmpty()) {
        out.println("No timings collected.");
        return;
      }
      long[] sorted = timings.stream().mapToLong(t -> t.nanos).sorted().toArray();
      long min = sorted[0];
      long max = sorted[sorted.length - 1];
      long sum = 0;
      for (long v : sorted) sum += v;
      double avg = (double) sum / sorted.length;
      long p50 = percentile(sorted, 50);
      long p95 = percentile(sorted, 95);

      out.printf("Parsed names: %d (%d failed)%n", count(), failures());
      out.printf("Average: %s%n", fmt(avg));
      out.printf("Min:     %s%n", fmt(min));
      out.printf("p50:     %s%n", fmt(p50));
      out.printf("p95:     %s%n", fmt(p95));
      out.printf("Max:     %s%n", fmt(max));
    }

    public void writeSortedLog(Path out) throws IOException {
      List<Timing> sorted = new ArrayList<>(timings);
      sorted.sort(Comparator.comparingLong((Timing t) -> t.nanos).reversed());
      try (BufferedWriter w = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
        w.write("# nanos\tmillis\tparsed\trank\tcode\tname\n");
        for (Timing t : sorted) {
          w.write(t.nanos + "\t");
          w.write(String.format("%.3f", t.nanos / 1_000_000.0));
          w.write("\t");
          w.write(t.parsed ? "Y" : "N");
          w.write("\t");
          w.write(t.name);
          w.write("\n");
        }
      }
    }

    private static long percentile(long[] sorted, int p) {
      if (sorted.length == 0) return 0;
      int idx = (int) Math.ceil(p / 100.0 * sorted.length) - 1;
      if (idx < 0) idx = 0;
      if (idx >= sorted.length) idx = sorted.length - 1;
      return sorted[idx];
    }

    private static String fmt(double nanos) {
      if (nanos >= 1_000_000) return String.format("%.2f ms", nanos / 1_000_000.0);
      if (nanos >= 1_000)     return String.format("%.2f µs", nanos / 1_000.0);
      return String.format("%.0f ns", nanos);
    }
  }
}
