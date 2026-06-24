package org.gbif.nameparser.cli;

import org.gbif.nameparser.NameParserImpl;
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
 * {@code parse} subcommand — streams an input file through the parser and writes
 * one row per result. Both reading and writing stream end-to-end, so the command
 * scales to multi-million-row inputs without growing memory.
 *
 * <p>Two input shapes are auto-detected by inspecting the first non-blank,
 * non-comment line:
 * <ul>
 *   <li><b>ColDP Name file</b> (TSV or CSV) — header columns matched against
 *       {@link life.catalogue.coldp.ColdpTerm}. Recognised columns:
 *       {@code ID}, {@code scientificName}, {@code authorship}, {@code rank},
 *       {@code code}. Other columns are ignored.</li>
 *   <li><b>Plain text</b> — one name per line; everything before the first tab is
 *       taken as the name.</li>
 * </ul>
 *
 * <p>Output format is selectable with {@code --format} (default {@code jsonl}):
 * {@code jsonl}, {@code json}, {@code csv} (ColDP), {@code tsv} (ColDP).
 *
 * <p>Use {@code -} as the input or output path to stream from stdin / to stdout
 * — convenient for unix pipes:
 * <pre>
 *   cat names.txt | name-parser-cli parse --input=- --output=- --format=tsv | …
 * </pre>
 *
 * <p>Options:
 * <ul>
 *   <li>{@code --input=PATH}  source file (default: {@code data/col-names.tsv}; {@code -} = stdin)</li>
 *   <li>{@code --output=PATH} target file (default: {@code &lt;input&gt;.&lt;format-ext&gt;}; {@code -} = stdout)</li>
 *   <li>{@code --format=FMT}  output format ({@code jsonl}, {@code json}, {@code csv}, {@code tsv})</li>
 *   <li>{@code --quiet}       suppress progress output (status info still goes to stderr)</li>
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

    NameParser parser = new NameParserImpl();
    long start = System.nanoTime();
    Stats s = run(parser, inputArg, outputArg, format, quiet ? null : System.err);
    long elapsedMs = (System.nanoTime() - start) / 1_000_000L;

    PrintStream summary = System.err;
    summary.printf("Parsed %,d names (%,d ok, %,d unparsable) in %.1fs → %s%n",
        s.total, s.ok, s.unparsable, elapsedMs / 1000.0,
        STDIO.equals(outputArg) ? "stdout" : Paths.get(outputArg).toAbsolutePath());
    if (!quiet) {
      summary.println("Input format detected: " + s.inputFormat);
    }
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
      stats.inputFormat = reader.format().toString();
      NameInput row;
      while ((row = reader.next()) != null) {
        ParseResult result = new ParseResult();
        result.line = row.line();
        result.id = row.id();
        result.input = row.name();
        try {
          result.parsed = parser.parse(row.name(), row.authorship(), row.rank(), row.code());
          stats.ok++;
        } catch (UnparsableNameException e) {
          result.error = new ParseResult.Err(e.getType(), e.getCode(), e.getMessage());
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
    System.out.println("Stream a list of names through the parser and write the results.");
    System.out.println("Input format is auto-detected:");
    System.out.println("  - ColDP Name file (TSV or CSV) — recognised by header columns");
    System.out.println("    matching ColdpTerm names (ID, scientificName, authorship, rank, code).");
    System.out.println("  - Plain text — one name per line.");
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
    public String inputFormat;
  }
}
