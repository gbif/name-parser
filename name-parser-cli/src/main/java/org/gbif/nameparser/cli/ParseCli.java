package org.gbif.nameparser.cli;

import org.gbif.nameparser.NameParserGBIF;
import org.gbif.nameparser.api.NameParser;
import org.gbif.nameparser.api.UnparsableNameException;
import org.gbif.nameparser.cli.io.NameInput;
import org.gbif.nameparser.cli.io.NameInputReader;
import org.gbif.nameparser.cli.io.NameOutputWriter;
import org.gbif.nameparser.cli.io.OutputFormat;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * {@code parse} subcommand — streams a plain-text input file (one name per line)
 * through {@link NameParserGBIF} and writes one row per result. Both reading and
 * writing stream end-to-end, so the command scales to multi-million-row inputs
 * without growing memory.
 *
 * <p>Input is plain text only on this branch — if a row contains tabs, only the
 * substring before the first tab is used as the name (so a bare TSV like
 * {@code col-names.tsv} can be fed in directly with the extra columns ignored).
 *
 * <p>Output format is selectable with {@code --format} (default {@code jsonl}):
 * <ul>
 *   <li>{@code jsonl} — one JSON object per line (consumed by {@code compare}).</li>
 *   <li>{@code json}  — single document with a JSON array of all rows.</li>
 *   <li>{@code csv} / {@code tsv} — flat ColDP Name file with header row.</li>
 * </ul>
 *
 * <p>Use {@code -} as the input or output path to stream from stdin / to stdout —
 * convenient for unix pipes:
 * <pre>
 *   cat names.txt | name-parser-cli parse --input=- --output=- --format=tsv | …
 * </pre>
 *
 * <p>Options:
 * <ul>
 *   <li>{@code --input=PATH}  source file (default: {@code data/col-names.tsv}; {@code -} = stdin)</li>
 *   <li>{@code --output=PATH} target file (default: {@code &lt;input&gt;.&lt;format-ext&gt;}; {@code -} = stdout)</li>
 *   <li>{@code --format=FMT}  output format ({@code jsonl}, {@code json}, {@code csv}, {@code tsv})</li>
 *   <li>{@code --quiet}       suppress progress output</li>
 *   <li>{@code -h --help}     print usage and exit</li>
 * </ul>
 *
 * <p>Progress and the final summary go to <b>stderr</b> so stdout stays a clean
 * data stream when piping.
 */
public final class ParseCli {
  static final Path DEFAULT_INPUT = Paths.get("data/col-names.tsv");
  static final String STDIO = "-";
  private static final long PROGRESS_EVERY = 100_000;

  private ParseCli() {}

  public static void main(String[] argv) throws Exception {
    Args a = Args.parse(argv);
    if (a.wantsHelp()) {
      printUsage();
      return;
    }
    String inputArg = a.string("input", DEFAULT_INPUT.toString());
    OutputFormat format = OutputFormat.fromString(a.string("format", "jsonl"));
    String outputArg = a.string("output",
        STDIO.equals(inputArg)
            ? STDIO
            : Paths.get(inputArg).getFileName().toString() + "." + format.extension());
    boolean quiet = a.flag("quiet");

    long start = System.nanoTime();
    Stats s;
    try (NameParser parser = new NameParserGBIF()) {
      s = run(parser, inputArg, outputArg, format, quiet ? null : System.err);
    }
    long elapsedMs = (System.nanoTime() - start) / 1_000_000L;

    System.err.printf("Parsed %,d names (%,d ok, %,d unparsable) in %.1fs → %s%n",
        s.total, s.ok, s.unparsable, elapsedMs / 1000.0,
        STDIO.equals(outputArg) ? "stdout" : Paths.get(outputArg).toAbsolutePath());
  }

  /** Convenience overload kept for embedding callers. */
  public static Stats run(NameParser parser, Path input, Path output, OutputFormat format,
                          PrintStream progress) throws IOException {
    return run(parser, input.toString(), output.toString(), format, progress);
  }

  /**
   * Streaming parse: read {@code inputArg} row-by-row, parse each name, write each
   * result to {@code outputArg}. Both arguments accept the literal {@code "-"} to
   * mean stdin / stdout. Returns running counters.
   */
  public static Stats run(NameParser parser, String inputArg, String outputArg,
                          OutputFormat format, PrintStream progress) throws IOException {
    Stats stats = new Stats();
    try (NameInputReader reader = openReader(inputArg);
         NameOutputWriter writer = openWriter(outputArg, format)) {
      NameInput row;
      while ((row = reader.next()) != null) {
        ParseResult result = new ParseResult();
        result.line = row.line();
        result.input = row.name();
        try {
          // dev's NameParser.parse takes (name, rank, code) — no separate authorship arg.
          result.parsed = parser.parse(row.name(), row.rank(), row.code());
          stats.ok++;
        } catch (UnparsableNameException e) {
          result.error = new ParseResult.Err(e.getType(), e.getMessage());
          stats.unparsable++;
        } catch (InterruptedException e) {
          // The parser uses a thread-pool with a per-call timeout; an
          // InterruptedException surfaces individual parse timeouts rather than
          // a request to abort. Mark the row, then continue with the next name.
          result.error = new ParseResult.Err(null, "parse timeout");
          stats.unparsable++;
        } catch (RuntimeException e) {
          result.error = new ParseResult.Err(null,
              e.getClass().getSimpleName() + ": " + e.getMessage());
          stats.unparsable++;
        }
        writer.write(result);
        stats.total++;

        if (progress != null && stats.total % PROGRESS_EVERY == 0) {
          progress.printf("  %,d parsed (line %,d)%n", stats.total, row.line());
        }
      }
    }
    return stats;
  }

  private static NameInputReader openReader(String inputArg) throws IOException {
    if (STDIO.equals(inputArg)) return NameInputReader.open(System.in);
    return NameInputReader.open(Paths.get(inputArg));
  }

  private static NameOutputWriter openWriter(String outputArg, OutputFormat format)
      throws IOException {
    if (STDIO.equals(outputArg)) {
      // Wrap System.out so the writer can call close() (and thus flush trailing
      // markers like the JSON array's "]") without actually closing the JVM's
      // stdout descriptor and breaking the shell pipe.
      return NameOutputWriter.open(nonClosingStdout(), format);
    }
    return NameOutputWriter.open(Paths.get(outputArg), format);
  }

  private static OutputStream nonClosingStdout() {
    return new FilterOutputStream(System.out) {
      @Override
      public void close() throws IOException {
        flush();
      }
    };
  }

  private static void printUsage() {
    System.out.println("Usage: name-parser-cli parse [options]");
    System.out.println();
    System.out.println("Stream a plain-text list of names through the parser and write the results.");
    System.out.println("If a row contains tabs only the first column (before the tab) is used as");
    System.out.println("the name — the bundled col-names.tsv is usable verbatim, additional ColDP");
    System.out.println("columns are silently ignored.");
    System.out.println();
    System.out.println("Use '-' as the input or output path to stream from stdin / to stdout:");
    System.out.println("  cat names.txt | name-parser-cli parse --input=- --output=- --format=tsv");
    System.out.println();
    System.out.println("Options:");
    System.out.println("  --input=PATH    source file (default: data/col-names.tsv; '-' = stdin)");
    System.out.println("  --output=PATH   target file (default: <input>.<format-ext>; '-' = stdout)");
    System.out.println("  --format=FMT    output format: jsonl (default), json, csv, tsv");
    System.out.println("                  csv / tsv produce a flat ColDP Name file with header");
    System.out.println("  --quiet         suppress progress output");
    System.out.println("  -h --help       print this message and exit");
    System.out.println();
    System.out.println("Progress and the run summary are written to stderr so stdout stays a");
    System.out.println("clean data stream when piping.");
  }

  public static final class Stats {
    public long total;
    public long ok;
    public long unparsable;
  }
}
