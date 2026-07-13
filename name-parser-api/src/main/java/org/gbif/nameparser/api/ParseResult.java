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
 * The outcome of a parse: either a structurally {@link Parsed} name or an {@link Unparsable}
 * classification (virus, hybrid formula, placeholder, BOLD BIN, ...).
 * <p>
 * This mirrors the shape a {@code Result<ParsedName, Unparsable>} takes in the Rust implementation,
 * so both parsers expose one contract. Prefer it over catching exceptions: it composes in streams
 * ({@code flatMap(ParseResult::parsed)}) and pattern-matches exhaustively over the two variants.
 * <p>
 * {@link #type()} and {@link #code()} are available on <em>both</em> variants, so callers can
 * classify an unparsable name without pattern-matching or taking the exception path.
 */
public sealed interface ParseResult permits ParseResult.Parsed, ParseResult.Unparsable {

  /** The name type, available whether or not the name was parsable. */
  NameType type();

  /** The nomenclatural code when known, else null. Available on both variants. */
  @Nullable
  NomCode code();

  /** The parsed name if parsable, else empty. Stream-friendly: {@code flatMap(ParseResult::parsed)}. */
  Optional<ParsedName> parsed();

  /** @return true if this carries a structured {@link ParsedName}. */
  default boolean isParsable() {
    return parsed().isPresent();
  }

  /**
   * @return the parsed name
   * @throws UnparsableNameException if this is an {@link Unparsable} result
   */
  ParsedName orElseThrow() throws UnparsableNameException;

  /**
   * A successfully parsed name. Its {@link ParsedName#getState() state} may be
   * {@link ParsedName.State#COMPLETE} or {@link ParsedName.State#PARTIAL}.
   */
  record Parsed(ParsedName name) implements ParseResult {
    public Parsed {
      if (name == null) {
        throw new NullPointerException("name required");
      }
    }

    @Override
    public NameType type() {
      return name.getType();
    }

    @Override
    public NomCode code() {
      return name.getCode();
    }

    @Override
    public Optional<ParsedName> parsed() {
      return Optional.of(name);
    }

    @Override
    public ParsedName orElseThrow() {
      return name;
    }
  }

  /**
   * A name that cannot be expressed as a {@link ParsedName}: a virus, hybrid formula, placeholder,
   * BOLD BIN or other non-name text. Carries the classifying {@link NameType} and, when known, the
   * {@link NomCode} (e.g. VIRUS names).
   */
  record Unparsable(NameType type, @Nullable NomCode code, String name) implements ParseResult {
    public Unparsable {
      if (type == null) {
        throw new NullPointerException("type required");
      }
      if (type.isParsable()) {
        throw new IllegalArgumentException(type + " is a parsable type");
      }
    }

    public Unparsable(NameType type, String name) {
      this(type, null, name);
    }

    @Override
    public Optional<ParsedName> parsed() {
      return Optional.empty();
    }

    @Override
    public ParsedName orElseThrow() throws UnparsableNameException {
      throw new UnparsableNameException(this);
    }
  }
}
