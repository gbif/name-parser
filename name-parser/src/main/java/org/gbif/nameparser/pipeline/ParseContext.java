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
  /** Year extracted from a stripped publishedIn tail (e.g. "in Author, 1987"). */
  public String pendingYear;

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
