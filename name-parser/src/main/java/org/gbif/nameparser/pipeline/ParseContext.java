package org.gbif.nameparser.pipeline;

import org.gbif.nameparser.api.NameType;
import org.gbif.nameparser.api.NomCode;
import org.gbif.nameparser.api.ParsedName;
import org.gbif.nameparser.api.Rank;
import org.gbif.nameparser.token.Token;

import java.util.List;

/**
 * Mutable per-parse state shared across pipeline stages.
 */
public final class ParseContext {
  public final String original;
  public final String authorshipInput;
  public final Rank requestedRank;
  public final NomCode requestedCode;

  public String working;
  public List<Token> tokens;
  public final ParsedName name = new ParsedName();
  /** Set by StripAndStash when a parenthesised "[sic, …]" comment was removed. */
  public String pendingUnparsed;
  /** Set by StripAndStash when an aggregate marker was stripped from the input. */
  public boolean aggregate;
  /**
   * Set by {@link Preflight} when the input is a clean uni/binomial whose genus (or
   * monomial) carries an ICTV viral rank suffix. {@link Assemble} turns this into
   * {@link NomCode#VIRUS} when the caller supplied no code.
   */
  public boolean viralShape;
  /** Year extracted from a stripped publishedIn tail (e.g. "in Author, 1987"). */
  public String pendingYear;
  /**
   * True when {@link #pendingYear} came from a stripped publishedIn reference rather
   * than from the author span itself. Such a year is the publication year of the work
   * — code-neutral — and is propagated onto the combination authorship for output but
   * must NOT be used as a signal for code inference. (A name's nomenclatural code is
   * settled by other authorship cues; a publication year can attach to zoological,
   * botanical, or bacteriological names alike.)
   */
  public boolean pendingYearFromPublication;
  /**
   * Quote char ("'" or '"') a leading monomial was wrapped in (e.g. "'Prosthète' Hesse, 1861").
   * Such quotes mark a name that is not an available scientific name; the quotes are stripped
   * for parsing and re-wrapped around the parsed uninomial in {@link Assemble} so the output
   * keeps them, and the name is flagged doubtful.
   */
  public String quotedMonomial;
  /**
   * Token index range [{@code midAuthorFrom}, {@code midAuthorTo}) of an author span that
   * sits between the species epithet and a following infraspecific rank marker
   * ("Cirsium creticum d'Urv. subsp. creticum", "Trimezia spathata (Klatt) Baker subsp.
   * spathata"). Recorded by {@link NameTokens}. For an autonym this is the <em>species</em>
   * author (ICN Art. 22.1/26.1) — the autonym's final epithet bears no author of its own —
   * so {@link Pipeline} parses this span and applies it as the name's authorship. For
   * non-autonym infraspecific names the model holds the terminal (infraspecific) author
   * instead, so this span is left dropped.
   */
  public int midAuthorFrom = -1;
  public int midAuthorTo = -1;

  /**
   * Imprint year stripped from the input before authorship parsing (e.g. the "1969" in
   * {@code "Storr, 1970 [\"1969\"]"} / {@code "(imprint 1969)"}). Applied onto the name's
   * combination authorship once it exists — imprint years now live on {@link org.gbif.nameparser.api.Authorship}.
   */
  public String pendingImprintYear;

  /** Records a stripped imprint year on a first-writer-wins basis. */
  public void setPendingImprintYear(String year) {
    if (pendingImprintYear == null && year != null) {
      pendingImprintYear = year;
    }
  }

  /**
   * Species author extracted from a below-species name where it sits before the terminal
   * epithet (e.g. the "L." in "Acer campestre L. cv. 'Elsrijk' Broerse"). Parsed and set as
   * {@link ParsedName#setSpecificAuthorship} by {@link Pipeline}.
   */
  public String pendingSpecificAuthor;
  /**
   * Genus author of an infrageneric name where it sits before the rank marker (e.g. the
   * "(Adans.) Kuntze" in "Cordia (Adans.) Kuntze sect. Salimori"). Parsed and set as
   * {@link ParsedName#setGenericAuthorship} by {@link Pipeline}.
   */
  public String pendingGenericAuthor;

  /**
   * Records an unparsed remainder on a first-writer-wins basis. {@link #pendingUnparsed}
   * is a single slot written by several strip steps; routing every write through here
   * makes precedence deterministic (the earliest strip step in {@code StripAndStash.run}
   * wins) instead of depending on whether an individual writer remembered to null-check.
   * Empty/blank remainders are ignored.
   */
  public void setPendingUnparsed(String remainder) {
    if (pendingUnparsed == null && remainder != null && !remainder.isBlank()) {
      pendingUnparsed = remainder;
    }
  }

  /**
   * Records a publication-derived year on a first-writer-wins basis and marks it
   * code-neutral (see {@link #pendingYearFromPublication}). Every current writer of
   * {@link #pendingYear} sets it from a stripped publishedIn reference, so the two
   * fields are always set together — bundling them here keeps them in lock-step.
   */
  public void setPendingPublicationYear(String year) {
    if (pendingYear == null && year != null) {
      pendingYear = year;
      pendingYearFromPublication = true;
    }
  }

  public ParseContext(String scientificName, String authorship, Rank rank, NomCode code) {
    this.original = scientificName;
    this.authorshipInput = authorship;
    this.requestedRank = rank;
    this.requestedCode = code;
    this.working = scientificName;
    this.name.setRank(rank);
    this.name.setCode(code);
    this.name.setType(NameType.SCIENTIFIC);
    this.name.setState(ParsedName.State.COMPLETE);
  }
}
