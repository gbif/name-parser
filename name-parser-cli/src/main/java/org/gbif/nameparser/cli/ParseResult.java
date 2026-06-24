package org.gbif.nameparser.cli;

import org.gbif.nameparser.api.NameType;
import org.gbif.nameparser.api.NomCode;
import org.gbif.nameparser.api.ParsedName;
import org.gbif.nameparser.api.UnparsableNameException;

/**
 * One row of parser output: the raw input, its source line number, and either the
 * resulting {@link ParsedName} (on success) or a small {@link Err} record extracted
 * from the {@link UnparsableNameException} (on failure).
 *
 * <p>Designed for streaming JSONL serialization — Gson default reflection produces
 * the desired flat shape:
 * <pre>{"line":42,"input":"Felis catus","parsed":{...}}</pre>
 * or
 * <pre>{"line":99,"input":"Iridoviridae","error":{"type":"OTHER","code":"VIRUS","message":"..."}}</pre>
 */
public final class ParseResult {
  public long line;
  /** Optional record identifier — populated from the {@code ID} column of ColDP input. */
  public String id;
  public String input;
  public ParsedName parsed;
  public Err error;

  public static final class Err {
    public NameType type;
    /** Nomenclatural code associated with the unparsable name, e.g. {@code VIRUS}. May be null. */
    public NomCode code;
    public String message;

    public Err() {}

    public Err(NameType type, String message) {
      this(type, (NomCode) null, message);
    }

    public Err(NameType type, NomCode code, String message) {
      this.type = type;
      this.code = code;
      this.message = message;
    }
  }
}
