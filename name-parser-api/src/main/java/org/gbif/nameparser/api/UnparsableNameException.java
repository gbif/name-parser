package org.gbif.nameparser.api;

/**
 *
 */
public class UnparsableNameException extends Exception {
  private final NameType type;
  private final String name;

  public UnparsableNameException(NameType type, String name) {
    super("Unparsable "+type+" name: " + name);
    this.type = type;
    this.name = name;
  }

  public NameType getType() {
    return type;
  }

  public String getName() {
    return name;
  }

}
