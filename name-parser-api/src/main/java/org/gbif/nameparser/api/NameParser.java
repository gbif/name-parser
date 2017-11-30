package org.gbif.nameparser.api;

/**
 * The basic Name parser contract.
 * Implementations for the GBIF and GNA parser are provided.
 */
public interface NameParser {

  ParsedName parse(String scientificName) throws UnparsableNameException;

  /**
   * Fully parse the supplied name also trying to extract authorships, a conceptual sec reference, remarks or notes
   * on the nomenclatural status. In some cases the authorship parsing proves impossible and this nameparser will
   * return null.
   *
   * For strings which are no scientific names and scientific names that cannot be expressed by the ParsedName class
   * the parser will throw an UnparsableException with a given NameType and the original, unparsed name. This is the
   * case for all virus names and proper hybrid formulas, so make sure you catch and process this exception.
   *
   * @param scientificName the full scientific name to parse
   * @param rank the rank of the name if it is known externally. Helps identifying infrageneric names vs bracket authors
   *
   * @throws UnparsableNameException
   */
  ParsedName parse(String scientificName, Rank rank) throws UnparsableNameException;

}
