package org.gbif.nameparser.cli.io;

/**
 * Detected shape of the parser input file.
 */
public enum InputFormat {
  /** Plain text file — one name per line. */
  PLAIN,
  /** Tab-separated ColDP Name file (first row is the header). */
  COLDP_TSV,
  /** Comma-separated ColDP Name file (first row is the header). */
  COLDP_CSV
}
