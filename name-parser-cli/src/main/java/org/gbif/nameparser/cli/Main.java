package org.gbif.nameparser.cli;

import java.util.Arrays;

/**
 * Entry point for the {@code name-parser-cli} executable jar. Dispatches to one of
 * the bundled subcommands:
 * <ul>
 *   <li>{@code parse}     — parse a list of names into JSONL ({@link ParseCli})</li>
 *   <li>{@code compare}   — diff two JSONL files row-by-row ({@link CompareCli})</li>
 *   <li>{@code benchmark} — measure parser throughput ({@link BenchmarkCli})</li>
 * </ul>
 * Run {@code java -jar name-parser-cli.jar &lt;command&gt; --help} for per-command options.
 */
public final class Main {
  private Main() {}

  public static void main(String[] args) throws Exception {
    if (args.length == 0 || "-h".equals(args[0]) || "--help".equals(args[0])) {
      printUsage();
      System.exit(args.length == 0 ? 2 : 0);
    }
    String cmd = args[0];
    String[] rest = Arrays.copyOfRange(args, 1, args.length);
    switch (cmd) {
      case "parse":
        ParseCli.main(rest);
        break;
      case "compare":
        CompareCli.main(rest);
        break;
      case "benchmark":
        BenchmarkCli.main(rest);
        break;
      default:
        System.err.println("Unknown command: " + cmd);
        printUsage();
        System.exit(2);
    }
  }

  private static void printUsage() {
    System.out.println("Usage: name-parser-cli <command> [options]");
    System.out.println();
    System.out.println("Commands:");
    System.out.println("  parse      Parse a list of names into JSONL");
    System.out.println("  compare    Compare two JSONL outputs row-by-row");
    System.out.println("  benchmark  Measure parser throughput");
    System.out.println();
    System.out.println("Run 'name-parser-cli <command> --help' for per-command options.");
  }
}
