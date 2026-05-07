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
   * True when {@link NameTokens} consumed an explicit infraspecific rank marker
   * (subsp./var./f./…). The marker itself is the botanical convention — zoological
   * trinomials simply stack epithets — so its presence is a soft botanical signal
   * used by code inference when no other authorship cue is available.
   */
  public boolean explicitInfraMarker;
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
