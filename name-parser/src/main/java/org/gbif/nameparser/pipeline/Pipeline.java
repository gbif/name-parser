package org.gbif.nameparser.pipeline;

import org.gbif.nameparser.api.NameType;
import org.gbif.nameparser.api.NomCode;
import org.gbif.nameparser.api.ParsedName;
import org.gbif.nameparser.api.Rank;
import org.gbif.nameparser.api.UnparsableNameException;
import org.gbif.nameparser.token.Token;
import org.gbif.nameparser.token.Tokenizer;
import org.gbif.nameparser.util.UnicodeUtils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Orchestrates the staged parsing pipeline. Each stage mutates the shared
 * {@link ParseContext}.
 */
public final class Pipeline {

  private Pipeline() {}

  public static ParsedName run(String scientificName, String authorship, Rank rank, NomCode code)
      throws UnparsableNameException {
    if (scientificName == null) {
      throw new UnparsableNameException(NameType.OTHER, null);
    }
    String trimmed = scientificName.trim();
    if (trimmed.isEmpty()) {
      throw new UnparsableNameException(NameType.OTHER, scientificName);
    }
    // Normalise the many unicode apostrophe / quote variants to ASCII (' and ") up front so
    // every parsed field (genus, epithets, authorship, unparsed) and both the name and the
    // separately supplied authorship come out with consistent ASCII punctuation. The raw
    // scientificName is kept for faithful echo in any UnparsableNameException thrown below.
    trimmed = UnicodeUtils.normalizeQuotes(trimmed);
    authorship = UnicodeUtils.normalizeQuotes(authorship);
    ParseContext ctx = new ParseContext(trimmed, authorship, rank, code);
    splitGluedPhraseName(ctx);
    Preflight.run(scientificName, ctx.working);
    StripAndStash.run(ctx);
    if (!hasLetter(ctx.working)) {
      throw new UnparsableNameException(NameType.OTHER, scientificName);
    }
    ctx.tokens = Tokenizer.tokenize(ctx.working);

    int boundary = AuthorshipSplit.findBoundary(ctx.tokens, ctx);
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
      if (authState.imprintYear != null && ctx.name.getImprintYear() == null) {
        ctx.name.setImprintYear(authState.imprintYear);
      }
    }
    AuthorshipParser.AuthState extraState = null;
    if (authorship != null && !authorship.isBlank()) {
      // Run the same annotation strippers (sic / corrig / extinct dagger / brackets etc.)
      // on the auxiliary authorship string so its tokens are clean before parsing.
      String authClean = StripAndStash.stripAuthorshipMarkers(authorship, ctx.name);
      // Authorship parsed separately by re-tokenising the auxiliary string.
      List<Token> aux = Tokenizer.tokenize(authClean);
      extraState = AuthorshipParser.parse(aux, 0);
      if (extraState.combination.exists()) {
        ctx.name.setCombinationAuthorship(extraState.combination);
      }
      if (extraState.basionym.exists()) {
        ctx.name.setBasionymAuthorship(extraState.basionym);
      }
      if (extraState.sanctioningAuthor != null) {
        ctx.name.setSanctioningAuthor(extraState.sanctioningAuthor);
      }
      if (extraState.imprintYear != null && ctx.name.getImprintYear() == null) {
        ctx.name.setImprintYear(extraState.imprintYear);
      }
    }
    if (authState != null && authState.sanctioningAuthor != null) {
      ctx.name.setSanctioningAuthor(authState.sanctioningAuthor);
    }

    // Code inference uses the main scientific name's authState by default. When the
    // main name had no authorship of its own, fall back to the auxiliary authorship
    // state only when it has a basionym citation (parens) with a year — that's the
    // "(Author, YYYY)" zoological pattern. Year-only or plain "Author, year" supplied
    // as separate authorship is parsed for authors but doesn't tip the code on its own.
    AuthorshipParser.AuthState codeState = authState;
    if ((codeState == null || (!codeState.combination.exists() && !codeState.basionymPresent))
        && extraState != null
        && extraState.basionymPresent && extraState.basionym.getYear() != null) {
      codeState = extraState;
    }

    // Year that came directly off the author span (e.g. "Linnaeus, 1771") is applied
    // BEFORE code inference because it IS the zoological author-year citation we want
    // to detect. A year extracted from a stripped publishedIn reference is just the
    // publication year — code-neutral — so it's applied AFTER inference instead, so
    // the same year on a botanical or bacterial name doesn't get misread as a
    // zoological author-year.
    if (ctx.pendingYear != null
        && !ctx.pendingYearFromPublication
        && ctx.name.hasCombinationAuthorship()
        && ctx.name.getCombinationAuthorship().getYear() == null) {
      ctx.name.getCombinationAuthorship().setYear(ctx.pendingYear);
    }

    Assemble.finish(ctx, codeState);

    if (ctx.pendingYear != null
        && ctx.pendingYearFromPublication
        && ctx.name.hasCombinationAuthorship()
        && ctx.name.getCombinationAuthorship().getYear() == null) {
      ctx.name.getCombinationAuthorship().setYear(ctx.pendingYear);
    }

    return ctx.name;
  }

  // Pattern: Latin-style prefix glued to an all-caps / alphanumeric phrase suffix
  // ("OdontellidaeGEN", "GenusANIC_3"). Underscored prefixes ("Blattellinae_SB") are
  // handled later in Assemble; we don't match those here.
  private static final Pattern GLUED_PHRASE = Pattern.compile(
      "^([\\p{Lu}][\\p{Ll}]+)([\\p{Lu}]{2,}[\\p{Lu}\\d_]*)$",
      Pattern.UNICODE_CHARACTER_CLASS);

  /**
   * BOLD/specimen-style phrase names with no whitespace between the Latin prefix and
   * the phrase suffix ("OdontellidaeGEN", "GenusANIC_3"). Splits the working string so
   * Preflight doesn't reject the alphanumeric form and the rest of the pipeline can
   * treat the prefix as a normal uninomial.
   */
  private static void splitGluedPhraseName(ParseContext ctx) {
    if (ctx.working == null || ctx.working.indexOf(' ') >= 0) return;
    Matcher m = GLUED_PHRASE.matcher(ctx.working);
    if (!m.matches()) return;
    ctx.name.setPhrase(m.group(2));
    ctx.name.setType(NameType.INFORMAL);
    ctx.working = m.group(1);
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
