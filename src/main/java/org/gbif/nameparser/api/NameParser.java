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
   * @return a {@link ParseResult.Parsed} name, or an {@link ParseResult.Unparsable} classification
   *         for strings that are no scientific names or cannot be expressed by the {@link ParsedName}
   *         class (all virus names, BOLD BIN numbers and proper hybrid formulas)
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
