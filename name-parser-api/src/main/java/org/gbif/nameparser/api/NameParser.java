package org.gbif.nameparser.api;

import javax.annotation.Nullable;

/**
 * The basic Name parser contract.
 */
public interface NameParser {
  
  /**
   * @deprecated provide authorship, rank and code parameters
   */
  @Deprecated
  default ParsedName parse(String scientificName) throws UnparsableNameException {
    return parse(scientificName, Rank.UNRANKED);
  }

  /**
   * @deprecated provide authorship and code parameters
   */
  @Deprecated
  default ParsedName parse(String scientificName, Rank rank) throws UnparsableNameException {
    return parse(scientificName, null, rank, null);
  }
  
  /**
   * Fully parse the supplied name trying to extract authorships, a conceptual sec reference, remarks or notes
   * on the nomenclatural status.
   * <p>
   * For strings which are no scientific names and scientific names that cannot be expressed by the ParsedName class
   * the parser will throw an UnparsableException with a given NameType and the original, unparsed name. This is the
   * case for all virus names, BOLD BIN numbers and proper hybrid formulas, so make sure you catch and process this exception.
   *
   * @param scientificName the full scientific name to parse. May already contain an authorship
   * @param authorship     the full scientific name authorship to parse. Might be included in the scientificName already
   * @param rank           the rank of the name if it is known externally. Helps identifying infrageneric names vs bracket authors
   * @param code           the nomenclatural code the name falls into. Null if unknown
   *
   * @throws UnparsableNameException
  */
  ParsedName parse(String scientificName, @Nullable String authorship, @Nullable Rank rank, @Nullable NomCode code) throws UnparsableNameException;

}
