package org.gbif.nameparser.api;

/**
 * A short classification of scientific name strings used in Checklist Bank.
 */
public enum NameType {
  
  /**
   * A parsable scientific latin name that might contain authorship but is not any of the other name types below (hybrid, cultivar, etc).
   */
  SCIENTIFIC,

  /**
   * An unparsable hybrid or graft-chimera <b>formula</b> (not a named hybrid).
   */
  FORMULA,

  /**
   * A variation of a scientific name that adds informal notes or falls short of a regular scientific
   * name. Semi parseable. Frequent reasons are:
   * <ul>
   *   <li>an open-nomenclature qualifier like "cf." or "aff." on a determined binomial</li>
   *   <li>an indetermined name like "Abies spec." or "Bacterium sp. RE1-2a"</li>
   *   <li>an abbreviated genus "A. alba Mill"</li>
   *   <li>a manuscript / phrase name: a uni- or binomial followed by a free-text phrase that
   *       identifies an as-yet-undescribed taxon, e.g. <em>Verticordia sp.1</em>,
   *       <em>Pultenaea sp. 'Olinda' (Coveny 6616)</em>,
   *       <em>Dryandra sp. 1 (A.S.George 16647) WA Herbarium</em> or
   *       <em>Acacia mutabilis Maslin subsp. Young River (G.F. Craig 2052)</em></li>
   * </ul>
   *
   * The whole verbatim designation from the rank marker to the end of the name — <em>including</em>
   * that marker — is captured in {@link ParsedName#getPhrase()}: e.g. "sp.1",
   * "sp. 'Olinda' (Coveny 6616)" or "sp. RE1-2a", not merely the "1" / "Olinda" / "RE1-2a" token, so
   * it round-trips as written. A bare "Genus sp." keeps the marker itself ("sp.") as the phrase.
   * Within such a phrase the (Coveny 6616) part is the voucher — the collector's name or initials
   * and the unique specimen number they assigned — and "WA Herbarium" the nominating party.
   * See https://florabase.dpaw.wa.gov.au/help/names#phrase
   *
   * <p>Informal names are semi parsable and start at least with a genus or uninomial. A supraspecific
   * anchor carrying such a designation with <em>no</em> species epithet is surfaced as the dedicated
   * {@link ParseResult.Informal} result variant (a flat taxon + rank + phrase); a name with a species
   * epithet (a binomial core) stays a {@link ParseResult.Parsed} whose {@code type} is INFORMAL.
   */
  INFORMAL,

  /**
   * An unparsable placeholder name like "incertae sedis" or "unknown genus".
   */
  PLACEHOLDER,

  /**
   * An anchorless, scheme-prefixed <b>machine identifier</b> rather than a name: a UNITE species
   * hypothesis (SH0864666.10FU), a BOLD BIN (BOLD:AAB5053), an OTU/ASV/ESV/zOTU operational unit, a
   * GTDB/assembly accession (UBA3054), or a standalone culture-collection accession (DSM 10, ATCC
   * 11775). Unparsable like {@link #OTHER}, but a more specific classification — these used to fall
   * into OTHER.
   */
  IDENTIFIER,

  /**
   * Any other unparsable name including numerical values, abbreviations or free text extracts.
   */
  OTHER;
  
  /**
   * @return true if the GBIF name parser can parse such a name into a ParsedName instance
   */
  public boolean isParsable() {
    return this == SCIENTIFIC || this == INFORMAL;
  }
  
}
