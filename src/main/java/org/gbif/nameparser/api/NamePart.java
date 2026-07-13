package org.gbif.nameparser.api;

import org.apache.commons.lang3.StringUtils;

/**
 * Enumeration to indicate a part of a canonical scientific name.
 */
public enum NamePart {
  
  GENERIC,
  INFRAGENERIC,
  SPECIFIC,
  INFRASPECIFIC;
  
  /**
   * Case insensitive lookup of a NamePart by its name that does not throw an exception but returns null
   * for a not found NamePart.
   *
   * @param namePart case insensitive name of name part
   * @return the matching NamePart or null
   */
  public static NamePart fromString(String namePart) {
    if (!StringUtils.isBlank(namePart)) {
      try {
        return valueOf(namePart.toUpperCase().trim());
      } catch (IllegalArgumentException e) {
        // swallow
      }
    }
    return null;
  }
  
}
