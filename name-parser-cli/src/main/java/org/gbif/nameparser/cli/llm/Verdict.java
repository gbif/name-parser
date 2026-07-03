package org.gbif.nameparser.cli.llm;

import java.util.List;

/**
 * One LLM verdict on a single parser result. Populated by Gson from the model's
 * structured-output JSON, so the field names are the schema property names.
 *
 * <p>{@code index} ties the verdict back to its position in the batch that was
 * sent — the model is asked to echo it so results can be reconciled even if the
 * model reorders or merges its reasoning.
 */
public final class Verdict {
  /** 0-based position within the batch this verdict belongs to. */
  public int index;
  /** {@code ok} | {@code suspect} | {@code wrong}. */
  public String verdict;
  /** {@code low} | {@code med} | {@code high}. */
  public String confidence;
  /** Per-field problems the model identified; empty/{@code null} when {@code verdict == ok}. */
  public List<FieldIssue> fields;
  /** Free-text explanation, one or two sentences. */
  public String note;

  public boolean isOk() {
    return "ok".equalsIgnoreCase(verdict);
  }

  /** A single field the model believes the parser got wrong. */
  public static final class FieldIssue {
    /** ParsedName field name, e.g. {@code rank}, {@code code}, {@code combinationAuthorship.year}. */
    public String name;
    /** What the parser produced for that field. */
    public String parsed;
    /** What the model believes it should be. */
    public String expected;
    /** Why. */
    public String reason;
  }
}
