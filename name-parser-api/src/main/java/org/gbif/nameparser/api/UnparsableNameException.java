package org.gbif.nameparser.api;

/**
 * An exception thrown to indicate a string that cannot be parsed into a ParsedName in a meaningful way.
 * This explicitly includes hybrid formulas, and carries a NomCode for code-known unparsables such as viruses.
 */
public class UnparsableNameException extends Exception {
  private final NameType type;
  private final NomCode code;
  private final String name;

  public UnparsableNameException(NameType type, String name, String message) {
    this(type, (NomCode) null, name, message);
  }

  public UnparsableNameException(NameType type, String name) {
    this(type, (NomCode) null, name);
  }

  public UnparsableNameException(NameType type, NomCode code, String name) {
    super("Unparsable " + type + " name: " + name);
    this.type = type;
    this.code = code;
    this.name = name;
  }

  public UnparsableNameException(NameType type, NomCode code, String name, String message) {
    super(message);
    this.type = type;
    this.code = code;
    this.name = name;
  }

  public NameType getType() {
    return type;
  }

  /** The nomenclatural code when known despite the name being unparsable (e.g. VIRUS), else null. */
  public NomCode getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public static class UnparsableAuthorshipException extends UnparsableNameException {

    public UnparsableAuthorshipException(String authorship) {
      super(NameType.OTHER, authorship, "Unparsable authorship: " + authorship);
    }
  }
}
