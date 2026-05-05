package org.gbif.nameparser.pipeline;

import org.gbif.nameparser.api.NomCode;
import org.gbif.nameparser.api.ParsedName;
import org.gbif.nameparser.api.Rank;
import org.gbif.nameparser.api.UnparsableNameException;
import org.gbif.nameparser.token.Tokenizer;

import java.util.List;

/**
 * Orchestrates the staged parsing pipeline. Each stage mutates the shared
 * {@link ParseContext}.
 */
public final class Pipeline {

  private Pipeline() {}

  public static ParsedName run(String scientificName, String authorship, Rank rank, NomCode code)
      throws UnparsableNameException {
    if (scientificName == null) {
      throw new UnparsableNameException(org.gbif.nameparser.api.NameType.NO_NAME, null);
    }
    String trimmed = scientificName.trim();
    if (trimmed.isEmpty()) {
      throw new UnparsableNameException(org.gbif.nameparser.api.NameType.NO_NAME, scientificName);
    }
    ParseContext ctx = new ParseContext(trimmed, authorship, rank, code);
    Preflight.run(scientificName, ctx.working);
    StripAndStash.run(ctx);
    if (!hasLetter(ctx.working)) {
      throw new UnparsableNameException(org.gbif.nameparser.api.NameType.NO_NAME, scientificName);
    }
    ctx.tokens = Tokenizer.tokenize(ctx.working);

    int boundary = AuthorshipSplit.findBoundary(ctx.tokens);
    NameTokens.classify(ctx, boundary);

    AuthorshipParser.AuthState authState = null;
    if (boundary < ctx.tokens.size()) {
      authState = AuthorshipParser.parse(ctx.tokens, boundary);
      if (authState.combination.exists()) {
        ctx.name.setCombinationAuthorship(authState.combination);
      }
      if (authState.basionym.exists()) {
        ctx.name.setBasionymAuthorship(authState.basionym);
      }
      if (authState.unparsedFrom >= 0) {
        ctx.name.setState(ParsedName.State.PARTIAL);
        ctx.name.setUnparsed(authState.unparsedText);
      }
    }
    if (authorship != null && !authorship.isBlank()) {
      // Authorship parsed separately by re-tokenising the auxiliary string.
      List<org.gbif.nameparser.token.Token> aux = Tokenizer.tokenize(authorship);
      AuthorshipParser.AuthState extra = AuthorshipParser.parse(aux, 0);
      if (extra.combination.exists()) {
        ctx.name.setCombinationAuthorship(extra.combination);
      }
      if (extra.basionym.exists()) {
        ctx.name.setBasionymAuthorship(extra.basionym);
      }
      if (extra.sanctioningAuthor != null) {
        ctx.name.setSanctioningAuthor(extra.sanctioningAuthor);
      }
    }
    if (authState != null && authState.sanctioningAuthor != null) {
      ctx.name.setSanctioningAuthor(authState.sanctioningAuthor);
    }

    Assemble.finish(ctx, authState);

    // Apply year extracted from a "in <Reference>, <year>" tail AFTER code inference
    // so we don't mistake the publishedIn year for a zoological-convention year on
    // the combination authorship.
    if (ctx.pendingYear != null
        && ctx.name.hasCombinationAuthorship()
        && ctx.name.getCombinationAuthorship().getYear() == null) {
      ctx.name.getCombinationAuthorship().setYear(ctx.pendingYear);
    }

    return ctx.name;
  }

  private static boolean hasLetter(String s) {
    for (int i = 0; i < s.length(); ) {
      int cp = s.codePointAt(i);
      if (Character.isLetter(cp)) return true;
      i += Character.charCount(cp);
    }
    return false;
  }
}
