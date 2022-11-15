/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.nameparser.api;

import javax.annotation.Nullable;

/**
 * The basic Name parser contract.
 * Implementations for the GBIF and GNA parser are provided.
 */
public interface NameParser extends AutoCloseable {
  
  /**
   * @deprecated provide rank and code parameters
   */
  @Deprecated
  ParsedName parse(String scientificName) throws UnparsableNameException, InterruptedException;

  default ParsedAuthorship parseAuthorship(String authorship) throws UnparsableNameException, InterruptedException {
    return parse("Abies alba " + authorship, Rank.SPECIES, null);
  }

  /**
   * @deprecated provide rank and code parameters
   */
  @Deprecated
  ParsedName parse(String scientificName, Rank rank) throws UnparsableNameException, InterruptedException;
  
  /**
   * Fully parse the supplied name also trying to extract authorships, a conceptual sec reference, remarks or notes
   * on the nomenclatural status. In some cases the authorship parsing proves impossible and this nameparser will
   * return null.
   * <p>
   * For strings which are no scientific names and scientific names that cannot be expressed by the ParsedName class
   * the parser will throw an UnparsableException with a given NameType and the original, unparsed name. This is the
   * case for all virus names and proper hybrid formulas, so make sure you catch and process this exception.
   *
   * @param scientificName the full scientific name to parse
   * @param rank           the rank of the name if it is known externally. Helps identifying infrageneric names vs bracket authors
   * @param code           the nomenclatural code the name falls into. Null if unknown
   *
   * @throws UnparsableNameException
  */
  ParsedName parse(String scientificName, Rank rank, @Nullable NomCode code) throws UnparsableNameException, InterruptedException;
  
}
