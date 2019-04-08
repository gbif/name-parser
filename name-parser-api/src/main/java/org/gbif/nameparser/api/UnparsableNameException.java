package org.gbif.nameparser.api;

/**
 * An exception thrown to indicate a string that cannot be parsed into a ParsedName in a meaningful way.
 * This explicitly includes virus names and hybrid formulas which have a structure
 * that cannot be accomodated by the ParsedName class.
 */
public class UnparsableNameException extends Exception {
  private final NameType type;
  private final String name;
  
  public UnparsableNameException(NameType type, String name) {
    super("Unparsable " + type + " name: " + name);
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
