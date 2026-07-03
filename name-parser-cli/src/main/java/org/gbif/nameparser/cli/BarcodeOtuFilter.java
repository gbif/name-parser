package org.gbif.nameparser.cli;

import java.util.regex.Pattern;

/**
 * Recognises UNITE Species Hypothesis and BOLD BIN identifiers so the validator
 * can drop them before spending LLM budget. These are DNA-barcode OTU codes, not
 * scientific names, and would otherwise dominate the unparsable tail:
 * <ul>
 *   <li>UNITE SH — e.g. {@code SH1957732.10FU}</li>
 *   <li>BOLD BIN — e.g. {@code BOLD:AAA0001}</li>
 * </ul>
 *
 * <p>This is a cheap pre-filter on the raw input string; the validator additionally
 * skips any parse rejected with {@link org.gbif.nameparser.api.NameType#OTU}, so an
 * OTU code never reaches the model however the parser classifies it.
 */
public final class BarcodeOtuFilter {

  private static final Pattern UNITE_SH = Pattern.compile("(?i)^SH\\d{5,}(\\.\\d+)?FU\\b");
  private static final Pattern BOLD_BIN = Pattern.compile("(?i)^BOLD:[A-Z]{2,5}\\d+\\b");

  private BarcodeOtuFilter() {}

  /** True for a UNITE SH or BOLD BIN identifier that should be excluded from validation. */
  public static boolean isBarcodeOtu(String name) {
    if (name == null) return false;
    String s = name.strip();
    return UNITE_SH.matcher(s).find() || BOLD_BIN.matcher(s).find();
  }
}
