package org.gbif.nameparser.api;

import java.util.Optional;

import javax.annotation.Nullable;

/**
 * The basic name parser contract.
 * <p>
 * Parsing never throws for an unparsable name — it returns a {@link ParseResult}, one of three
 * variants: a {@link ParseResult.Parsed} name, an {@link ParseResult.Informal} name (a taxon anchor
 * carrying a provisional, non-code designation, e.g. {@code "Rhizobium sp. RMCC TR1811"}), or an
 * {@link ParseResult.Unparsable} classification (virus, hybrid formula, placeholder, ...). Call
 * {@link ParseResult#orElseThrow()} at the call sites that want a fail-fast style.
 */
public interface NameParser {

  /**
   * Fully parse the supplied name, extracting authorship, a conceptual sec reference, remarks and
   * notes on the nomenclatural status.
   *
   * @param scientificName the full scientific name to parse. May already contain an authorship
   * @param authorship     the full scientific name authorship to parse. Might be included in the scientificName already
   * @param rank           the rank of the name if it is known externally. Helps identifying infrageneric names vs bracket authors
   * @param code           the nomenclatural code the name falls into. Null if unknown
   *
   * @return one of the three {@link ParseResult} variants: a {@link ParseResult.Parsed} name, an
   *         {@link ParseResult.Informal} name (a taxon anchor with a provisional, non-code designation),
   *         or an {@link ParseResult.Unparsable} classification for strings that are no scientific names
   *         or cannot be expressed by the {@link ParsedName} class (all virus names, BOLD BIN numbers
   *         and proper hybrid formulas)
   */
  ParseResult parse(String scientificName, @Nullable String authorship, @Nullable Rank rank, @Nullable NomCode code);

  default ParseResult parse(String scientificName) {
    return parse(scientificName, null, Rank.UNRANKED, null);
  }

  default ParseResult parse(String scientificName, Rank rank) {
    return parse(scientificName, null, rank, null);
  }

  /**
   * Parses only the authorship part of a scientific name.
   *
   * @return the parsed authorship, or empty if it could not be parsed
   */
  default Optional<ParsedAuthorship> parseAuthorship(String authorship, @Nullable NomCode code) {
    return parse("Abies alba", authorship, Rank.SPECIES, code)
        .parsed()
        .map(ParsedAuthorship::new);
  }
}
