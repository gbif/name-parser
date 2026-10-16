package org.gbif.nameparser.api;

import org.gbif.nameparser.util.NameFormatter;

import java.util.Optional;

import javax.annotation.Nullable;

/**
 * The outcome of a parse, one of two variants: a structurally {@link Parsed} name or an {@link Unparsable}
 * classification (virus, hybrid formula, placeholder, BOLD BIN, ...).
 * <p>
 * Informal names — provisional, open-nomenclature and indetermined names such as
 * {@code "Rhizobium sp. RMCC TR1811"}, {@code "Abies cf. alba"} or {@code "Bartonella group"} — are
 * {@link Parsed} names of type {@link NameType#INFORMAL}; {@link ParsedName#anchor()} gives the real
 * taxon they hang off and {@link ParsedName#getPhrase()} their provisional designation.
 * <p>
 * This mirrors the shape a {@code Result}-like value takes in the Rust implementation, so both
 * parsers expose one contract. Prefer it over catching exceptions: it composes in streams
 * ({@code flatMap(ParseResult::parsed)}) and pattern-matches exhaustively over the two variants.
 * <p>
 * {@link #type()} and {@link #code()} are available on <em>all</em> variants, so callers can classify
 * a name without pattern-matching or taking the exception path.
 */
public sealed interface ParseResult permits ParseResult.Parsed, ParseResult.Unparsable {

  /** The name type, available whether or not the name was parsable. */
  NameType type();

  /** The nomenclatural code when known, else null. Available on both variants. */
  @Nullable
  NomCode code();

  /**
   * The parsed name if this is a {@link Parsed} result, else empty ({@link Unparsable} carries no
   * {@link ParsedName}). Stream-friendly: {@code flatMap(ParseResult::parsed)}.
   */
  Optional<ParsedName> parsed();

  /** @return true if this carries a structured {@link ParsedName} (i.e. is a {@link Parsed} result). */
  default boolean isParsable() {
    return parsed().isPresent();
  }

  /**
   * The canonical string form of this result, available on every variant.
   * <p>
   * The two variants do not yield the same <em>kind</em> of string: a {@link Parsed} result renders
   * a genuinely normalised name with authorship (its {@link ParsedName#canonicalName()}), but an
   * {@link Unparsable} result has no structured form and simply echoes the verbatim input. Callers
   * must therefore not treat the {@link Unparsable} value as a cleaned or normalised name.
   *
   * @see NameFormatter#canonical(ParseResult)
   */
  default String canonicalName() {
    return NameFormatter.canonical(this);
  }

  /**
   * The complete canonical string form of this result, available on every variant, including all
   * details such as non code compliant, informal remarks.
   * <p>
   * As with {@link #canonicalName()}, an {@link Unparsable} result simply echoes the verbatim input and
   * must not be treated as a cleaned or normalised name.
   *
   * @see NameFormatter#canonicalComplete(ParseResult)
   */
  default String canonicalNameComplete() {
    return NameFormatter.canonicalComplete(this);
  }

  /**
   * @return the parsed name (only a {@link Parsed} result carries one)
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
