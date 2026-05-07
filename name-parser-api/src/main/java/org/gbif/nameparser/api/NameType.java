package org.gbif.nameparser.api;

/**
 * A short classification of scientific name strings used in Checklist Bank.
 */
public enum NameType {
  
  /**
   * A parsable scientific latin name that might contain authorship but is not any of the other name types below (virus, hybrid, cultivar, etc).
   */
  SCIENTIFIC,
  
  /**
   * An unparsable virus name.
   */
  VIRUS,
  
  /**
   * An unparsable hybrid or graft-chimera <b>formula</b> (not a hybrid name).
   */
  FORMULA,

  /**
   * A variation of a scientific name that either adds additional notes or has some shortcomings to be classified as
   * regular scientific names. Frequent reasons are:
   * - informal addition like "cf."
   * - indetermined like "Abies spec."
   * - abbreviated genus "A. alba Mill"
   * - manuscript names lacking latin species names, e.g. Verticordia sp.1
   * - phrase name, structured semi-scientific name which at start with a uni- or binonmial followed by a phrase, e.g.
   *   <em>Dryandra sp. 1 (A.S.George 16647) WA Herbarium</em>,
   *   <em>Desulfobacterota_B</em>
   *   <em>Pultenaea sp. 'Olinda' (Coveny 6616)</em> or
   *   <em>Acacia mutabilis Maslin subsp. Young River (G.F. Craig 2052)</em>
   *   The 1, Olinda or Young River is the phrase, similar to a cultivar name, that identifies the taxon.
   *   The (A.S.George 16647), (Coveny 6616) or (G.F. Craig 2052) is the voucher, the name or initials of the person
   *   vouching for the specimen and the unique collector number assigned to the voucher.
   *   The WA Herbarium is the nominating party, the party that wants to have a placeholder name for this specimen
   *   https://florabase.dpaw.wa.gov.au/help/names#phrase
   *
   *   Informal names are semi parsable and start at least with a genus or uninomial.
   *   The remainder is parsed into the ParsedName.phrase field.
   */
  INFORMAL,

  /**
   * An unparsable placeholder name like "incertae sedis" or "unknown genus".
   */
  PLACEHOLDER,
  
  /**
   * Any other unparsable name including identifiers, numerical values, abbreviations or text extracts.
   * This is where former OTU names like BOLD:AAB5053, SH0864666.10FU or UBA3054 fall into these days.
   */
  OTHER;
  
  /**
   * @return true if the GBIF name parser can parse such a name into a ParsedName instance
   */
  public boolean isParsable() {
    return this == SCIENTIFIC || this == INFORMAL;
  }
  
}
