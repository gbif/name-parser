package org.gbif.nameparser.api;

import org.gbif.nameparser.util.NameFormatter;

import java.util.Optional;

import javax.annotation.Nullable;

/**
 * The outcome of a parse, one of three variants: a structurally {@link Parsed} name, an
 * {@link Informal} name (a real taxon anchor carrying a provisional, non-code designation — e.g.
 * {@code "Rhizobium sp. RMCC TR1811"} or {@code "Bartonella group"}), or an {@link Unparsable}
 * classification (virus, hybrid formula, placeholder, BOLD BIN, ...).
 * <p>
 * This mirrors the shape a {@code Result}-like value takes in the Rust implementation, so both
 * parsers expose one contract. Prefer it over catching exceptions: it composes in streams
 * ({@code flatMap(ParseResult::parsed)}) and pattern-matches exhaustively over the three variants.
 * <p>
 * {@link #type()} and {@link #code()} are available on <em>all</em> variants, so callers can classify
 * a name without pattern-matching or taking the exception path.
 */
public sealed interface ParseResult permits ParseResult.Parsed, ParseResult.Informal, ParseResult.Unparsable {

  /** The name type, available whether or not the name was parsable. */
  NameType type();

  /** The nomenclatural code when known, else null. Available on all three variants. */
  @Nullable
  NomCode code();

  /**
   * The parsed name if this is a {@link Parsed} result, else empty ({@link Informal} and
   * {@link Unparsable} carry no {@link ParsedName}). Stream-friendly: {@code flatMap(ParseResult::parsed)}.
   */
  Optional<ParsedName> parsed();

  /** @return true if this carries a structured {@link ParsedName} (i.e. is a {@link Parsed} result). */
  default boolean isParsable() {
    return parsed().isPresent();
  }

  /**
   * The canonical string form of this result, available on every variant.
   * <p>
   * The three variants do not yield the same <em>kind</em> of string: a {@link Parsed} result renders
   * a genuinely normalised scientific name with authorship (its {@link ParsedName#canonicalName()}), an {@link Informal}
   * result renders its rebuilt supraspecific-taxon-plus-designation form (e.g. {@code "Rhizobium sp. RMCC TR1811"}),
   * but an {@link Unparsable} result has no structured form and simply echoes the verbatim input. Callers
   * must therefore not treat the {@link Unparsable} value as a cleaned or normalised name.
   *
   * @see NameFormatter#canonical(ParseResult)
   */
  default String canonicalName() {
    return NameFormatter.canonical(this);
  }

  /**
   * @return the parsed name (only a {@link Parsed} result carries one)
   * @throws UnparsableNameException if this is an {@link Informal} or {@link Unparsable} result
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
   * An informal / semistructured name: a real taxon {@link #taxon() anchor} (a genus or higher
   * uninomial) carrying a provisional, non-code designation instead of a determined species epithet —
   * e.g. a molecular provisional species ({@code "Rhizobium sp. RMCC TR1811"}), a numbered placeholder
   * ({@code "Allium sp. 1"}), or an informal group ({@code "Bartonella group"}).
   * <p>
   * Distinct from {@link Parsed}, which keeps every name with a species epithet (including cf./aff. and
   * infraspecific-indeterminate names, so their {@code specificAuthorship} is preserved), and from
   * {@link Unparsable}, which is not a name at all. Carries no {@link ParsedName} — its structure is the
   * flat {@code taxon} + {@code taxonRank} + {@code rank} + {@code phrase}.
   *
   * @param taxon     the supraspecific taxon it hangs off ({@code "Rhizobium"}, {@code "Ichneumonidae"}) —
   *                  the parser's best guess, NOT validated against a taxonomic backbone
   * @param taxonRank that taxon's rank (GENUS, FAMILY, ...)
   * @param rank      the rank the informal name purports to be (SPECIES for {@code "sp."}, UNRANKED for a group)
   * @param phrase    the distinguishing designator ({@code "RMCC TR1811"}, {@code "1"}, {@code "group"}); null for a bare {@code "Genus sp."}
   * @param code      the nomenclatural code when known, else null
   */
  record Informal(String taxon, Rank taxonRank, Rank rank, @Nullable String phrase, @Nullable NomCode code)
      implements ParseResult {
    public Informal {
      if (taxon == null) {
        throw new NullPointerException("taxon required");
      }
      if (taxonRank == null || rank == null) {
        throw new NullPointerException("taxonRank and rank required");
      }
    }

    @Override
    public NameType type() {
      return NameType.INFORMAL;
    }

    @Override
    public Optional<ParsedName> parsed() {
      return Optional.empty();
    }

    @Override
    public ParsedName orElseThrow() throws UnparsableNameException {
      throw new UnparsableNameException(NameType.INFORMAL, code, taxon);
    }

    /**
     * Rebuilds the {@code type = INFORMAL} {@link ParsedName} this informal name is a flattened view
     * of — the {@link #taxon()} in the genus slot (or the uninomial slot for a non-genus anchor),
     * plus its {@link #rank()}, {@link #phrase()} and {@link #code()}. The inverse of the parser's
     * flattening, for callers (the {@link org.gbif.nameparser.util.NameFormatter}, downstream models)
     * that need the structured form back. Note {@link #parsed()} still returns empty — this is a
     * lossy, best-guess anchor, not a validated {@link Parsed} result.
     */
    public ParsedName toParsedName() {
      ParsedName pn = new ParsedName();
      pn.setType(NameType.INFORMAL);
      pn.setRank(rank);
      pn.setCode(code);
      pn.setPhrase(phrase);
      if (taxonRank == Rank.GENUS) {
        pn.setGenus(taxon);
      } else {
        pn.setUninomial(taxon);
      }
      return pn;
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
