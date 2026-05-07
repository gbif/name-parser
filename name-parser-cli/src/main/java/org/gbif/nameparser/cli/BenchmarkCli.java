package org.gbif.nameparser.cli;

import org.gbif.nameparser.NameParserImpl;
import org.gbif.nameparser.api.NameParser;
import org.gbif.nameparser.api.NameType;
import org.gbif.nameparser.api.UnparsableNameException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumMap;
import java.util.Map;

/**
 * {@code benchmark} subcommand — measures the parser's throughput against an input
 * file containing one name per line. Reports count, total / average / min / p50 / p95
 * / max parsing times to stdout, plus a breakdown by {@link NameType}.
 *
 * <p>Lines starting with {@code #} and blank names are skipped. By default <b>every</b>
 * input row is timed — JIT warmup is opt-in via {@code --warmup}, in which case the
 * benchmark first parses 100 names from the input untimed to let HotSpot warm up, then a
 * timed pass that parses and reports on every name.
 *
 * <p>Timings are accumulated in primitive {@code long} arrays so the run stays cheap
 * and never keeps name strings in memory — this CLI is purely about measuring
 * throughput.
 *
 * <p>Options:
 * <ul>
 *   <li>{@code --input=PATH}  source file (default: {@code data/benchmark-data.txt})</li>
 *   <li>{@code --warmup}      parse the first 100 names untimed first to warm
 *       up the JIT before the timed pass</li>
 *   <li>{@code -h --help}     print usage and exit</li>
 * </ul>
 */
public final class BenchmarkCli {
  static final Path DEFAULT_INPUT = Paths.get("data/benchmark-data.txt");
  /** Number of names parsed during the optional --warmup pre-pass. */
  private static final int WARMUP_NAMES = 100;

  private final NameParser parser;

  public BenchmarkCli(NameParser parser) {
    this.parser = parser;
  }

  public static void main(String[] argv) throws Exception {
    Args a = Args.parse(argv);
    if (a.wantsHelp()) {
      printUsage();
      return;
    }
    Path input = a.path("input", DEFAULT_INPUT);
    boolean warmup = a.flag("warmup");
    if (!Files.exists(input)) {
      System.err.println("Input not found: " + input.toAbsolutePath());
      System.exit(2);
    }

    BenchmarkCli bench = new BenchmarkCli(new NameParserImpl());
    if (warmup) {
      System.err.println("Warming up the JIT — parsing the first " + WARMUP_NAMES + " names without timing…");
      bench.warmup(input);
    }
    Result r = bench.run(input);
    r.report(System.out);
  }

  /**
   * Untimed parse pass over the entire input, used to warm up the JIT before
   * timing. Output and exceptions are discarded — this is purely about touching
   * the parser code paths. Stops after {@link #WARMUP_NAMES} names so the warmup
   * cost stays bounded regardless of input size.
   */
  public void warmup(Path tsv) throws IOException {
    int n = 0;
    try (BufferedReader r = Files.newBufferedReader(tsv, StandardCharsets.UTF_8)) {
      String line;
      while (n < WARMUP_NAMES && (line = r.readLine()) != null) {
        if (line.isEmpty() || line.startsWith("#")) continue;
        String name = line.trim();
        if (name.isEmpty()) continue;
        try {
          parser.parse(name, null, null, null);
        } catch (UnparsableNameException ignored) {
          // counted in the timed pass
        }
        n++;
      }
    }
  }

  /**
   * Timed parse pass — every non-blank, non-comment line in the input is parsed
   * and its elapsed time recorded.
   */
  public Result run(Path tsv) throws IOException {
    long[] timings = new long[1024];
    int n = 0;
    int parseFailures = 0;
    Map<NameType, long[]> byType = new EnumMap<>(NameType.class);

    try (BufferedReader r = Files.newBufferedReader(tsv, StandardCharsets.UTF_8)) {
      String line;
      while ((line = r.readLine()) != null) {
        if (line.isEmpty() || line.startsWith("#")) continue;
        String name = line.trim();
        if (name.isEmpty()) continue;

        long t0 = System.nanoTime();
        boolean ok = true;
        NameType type;
        try {
          type = parser.parse(name, null, null, null).getType();
        } catch (UnparsableNameException e) {
          ok = false;
          type = e.getType();
        }
        long elapsed = System.nanoTime() - t0;

        if (n == timings.length) {
          long[] grown = new long[timings.length * 2];
          System.arraycopy(timings, 0, grown, 0, n);
          timings = grown;
        }
        timings[n++] = elapsed;
        if (!ok) parseFailures++;
        NameType key = type != null ? type : NameType.OTHER;
        byType.computeIfAbsent(key, k -> new long[1])[0]++;
      }
    }
    long[] trimmed = new long[n];
    System.arraycopy(timings, 0, trimmed, 0, n);
    return new Result(trimmed, parseFailures, byType);
  }

  private static void printUsage() {
    System.out.println("Usage: name-parser-cli benchmark [options]");
    System.out.println();
    System.out.println("Measure parser throughput on a name-per-line input file.");
    System.out.println("By default every input row is timed; --warmup parses the first");
    System.out.println("100 names untimed first so HotSpot is warm before the timed pass.");
    System.out.println();
    System.out.println("Options:");
    System.out.println("  --input=PATH    source file (default: data/benchmark-data.txt)");
    System.out.println("  --warmup        do a JIT warmup pass before timing");
    System.out.println("  -h --help       print this message and exit");
  }

  public static final class Result {
    private final long[] timings;
    private final int parseFailures;
    private final Map<NameType, long[]> byType;

    Result(long[] timings, int parseFailures, Map<NameType, long[]> byType) {
      this.timings = timings;
      this.parseFailures = parseFailures;
      this.byType = byType;
    }

    public int count() { return timings.length; }
    public int failures() { return parseFailures; }

    public void report(PrintStream out) {
      if (timings.length == 0) {
        out.println("No timings collected.");
        return;
      }
      long[] sorted = timings.clone();
      java.util.Arrays.sort(sorted);
      long min = sorted[0];
      long max = sorted[sorted.length - 1];
      long sum = 0;
      for (long v : sorted) sum += v;
      double avg = (double) sum / sorted.length;
      long p50 = percentile(sorted, 50);
      long p95 = percentile(sorted, 95);

      out.printf("Parsed names: %d (%d unparsable)%n", count(), failures());
      out.printf("Total:   %s%n", fmt(sum));
      out.printf("Average: %s%n", fmt(avg));
      out.printf("Min:     %s%n", fmt(min));
      out.printf("p50:     %s%n", fmt(p50));
      out.printf("p95:     %s%n", fmt(p95));
      out.printf("Max:     %s%n", fmt(max));

      out.println();
      out.println("Breakdown by name type:");
      byType.entrySet().stream()
          .sorted((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0]))
          .forEach(e -> out.printf("  %-20s %d%n", e.getKey(), e.getValue()[0]));
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
