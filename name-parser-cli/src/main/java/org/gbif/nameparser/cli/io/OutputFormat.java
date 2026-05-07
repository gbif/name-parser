package org.gbif.nameparser.cli.io;

/**
 * Output formats for the {@code parse} command.
 */
public enum OutputFormat {
  /** JSON Lines — one self-contained JSON object per row. (Default.) */
  JSONL,
  /** A single JSON document containing an array of all rows. */
  JSON,
  /** Comma-separated ColDP Name file with header. */
  CSV,
  /** Tab-separated ColDP Name file with header. */
  TSV;

  public static OutputFormat fromString(String s) {
    if (s == null) return JSONL;
    String n = s.trim().toLowerCase();
    return switch (n) {
      case "jsonl", "json-lines", "ndjson" -> JSONL;
      case "json" -> JSON;
      case "csv"  -> CSV;
      case "tsv"  -> TSV;
      default -> throw new IllegalArgumentException("Unknown output format: " + s);
    };
  }

  /** Default file extension (without the leading dot). */
  public String extension() {
    return switch (this) {
      case JSONL -> "jsonl";
      case JSON  -> "json";
      case CSV   -> "csv";
      case TSV   -> "tsv";
    };
  }
}
