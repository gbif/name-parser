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

/**
 * A short classification of scientific name strings used in Checklist Bank.
 */
public enum NameType {
  
  /**
   * A scientific latin name that might contain authorship but is not any of the other name types below (virus, hybrid, cultivar, etc).
   */
  SCIENTIFIC,
  
  /**
   * A virus name.
   */
  VIRUS,
  
  /**
   * A hybrid <b>formula</b> (not a hybrid name).
   */
  HYBRID_FORMULA,

  /**
   * A variation of a scientific name that either adds additional notes or has some shortcomings to be classified as
   * regular scientific names. Frequent reasons are:
   * - informal addition like "cf."
   * - indetermined like "Abies spec."
   * - abbreviated genus "A. alba Mill"
   * - manuscript names lacking latin species names, e.g. Verticordia sp.1
   * - phrase name, Phrase names are a structured semi-informal name with the form
   *   <em>Dryandra sp. 1 (A.S.George 16647) WA Herbarium</em>,
   *   <em>Pultenaea sp. 'Olinda' (Coveny 6616)</em> or
   *   <em>Acacia mutabilis Maslin subsp. Young River (G.F. Craig 2052)</em>
   *   The 1, Olinda or Young River is the phrase, similar to a cultivar name, that identifies the taxon.
   *   The (A.S.George 16647), (Coveny 6616) or (G.F. Craig 2052) is the voucher, the name or initials of the person
   *   vouching for the specimen and the unique collector number assigned to the voucher.
   *   The WA Herbarium is the nominating party, the party that wants to have a placeholder name for this specimen
   *   https://florabase.dpaw.wa.gov.au/help/names#phrase
   */
  INFORMAL,
  
  /**
   * Operational Taxonomic Unit.
   * An OTU is a pragmatic definition to group individuals by similarity, equivalent to but not necessarily in line
   * with classical Linnaean taxonomy or modern Evolutionary taxonomy.
   * <p>
   * A OTU usually refers to clusters of organisms, grouped by DNA sequence similarity of a specific taxonomic marker gene.
   * In other words, OTUs are pragmatic proxies for "species" at different taxonomic levels.
   * <p>
   * Sequences can be clustered according to their similarity to one another,
   * and operational taxonomic units are defined based on the similarity threshold (usually 97% similarity) set by the researcher.
   * Typically, OTU's are based on similar 16S rRNA sequences.
   */
  OTU,

  /**
   * A placeholder name like "incertae sedis" or "unknown genus".
   */
  PLACEHOLDER,
  
  /**
   * Surely not a scientific name of any kind.
   */
  NO_NAME;
  
  /**
   * @return true if the GBIF name parser can parse such a name into a ParsedName instance
   */
  public boolean isParsable() {
    return this == SCIENTIFIC || this == INFORMAL;
  }
  
}
