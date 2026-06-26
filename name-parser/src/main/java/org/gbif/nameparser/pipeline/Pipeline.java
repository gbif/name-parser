package org.gbif.nameparser.pipeline;

import org.gbif.nameparser.api.NameType;
import org.gbif.nameparser.api.NomCode;
import org.gbif.nameparser.api.ParsedName;
import org.gbif.nameparser.api.Rank;
import org.gbif.nameparser.api.UnparsableNameException;
import org.gbif.nameparser.api.Warnings;
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

  /**
   * Hard upper bound on the input length. Beyond this the input is rejected as unparsable
   * rather than parsed: real scientific names — even with very large authorships — stay
   * well under this (the longest known valid name is ~860 chars), and the regex-heavy
   * pipeline has no execution timeout, so an unbounded input is a denial-of-service risk
   * (deep regex recursion can overflow the stack on the caller's thread).
   */
  static final int MAX_LENGTH = 1000;

  /**
   * Inputs longer than this still parse but carry a {@link Warnings#LONG_NAME} flag so
   * callers can spot the unusual (but legitimate) very-long names.
   */
  static final int LONG_NAME_LENGTH = 250;

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
    if (trimmed.length() > MAX_LENGTH) {
      throw new UnparsableNameException(NameType.OTHER, scientificName);
    }
    // The separately supplied authorship is tokenised and run through the same
    // regex-heavy authorship parser, so it carries the same DoS exposure — cap it too.
    if (authorship != null && authorship.trim().length() > MAX_LENGTH) {
      throw new UnparsableNameException(NameType.OTHER, scientificName);
    }
    // Normalise the many unicode apostrophe / quote variants to ASCII (' and ") up front so
    // every parsed field (genus, epithets, authorship, unparsed) and both the name and the
    // separately supplied authorship come out with consistent ASCII punctuation. The raw
    // scientificName is kept for faithful echo in any UnparsableNameException thrown below.
    trimmed = UnicodeUtils.normalizeQuotes(trimmed);
    authorship = UnicodeUtils.normalizeQuotes(authorship);
    ParseContext ctx = new ParseContext(trimmed, authorship, rank, code);
    if (trimmed.length() > LONG_NAME_LENGTH) {
      ctx.name.addWarning(Warnings.LONG_NAME);
    }
    splitGluedPhraseName(ctx);
    Preflight.run(scientificName, ctx);
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
      applyAuthorship(ctx.name, authState);
      // Unparsed remainder is specific to the embedded path (the separately supplied
      // authorship string has no leftover name material to park).
      if (authState.unparsedFrom >= 0) {
        ctx.name.setState(ParsedName.State.PARTIAL);
        ctx.name.setUnparsed(authState.unparsedText);
      }
    }
    // Autonym species author: a "(Bas) Comb" or plain author span recorded mid-name by
    // NameTokens, sitting between the species epithet and the infraspecific marker. The
    // autonym's final epithet carries no author of its own (ICN Art. 22.1/26.1), so this
    // span IS the species author and becomes the name's authorship. Only applied when the
    // name is an autonym and no trailing authorship was already parsed.
    AuthorshipParser.AuthState autonymState = null;
    if (ctx.midAuthorFrom >= 0
        && ctx.name.isAutonym()
        && !ctx.name.hasAuthorship()) {
      List<Token> span = ctx.tokens.subList(ctx.midAuthorFrom, ctx.midAuthorTo);
      autonymState = AuthorshipParser.parse(span, 0);
      applyAuthorship(ctx.name, autonymState);
    }

    AuthorshipParser.AuthState extraState = null;
    if (authorship != null && !authorship.isBlank()) {
      // Run the same annotation strippers (sic / corrig / extinct dagger / brackets etc.)
      // on the auxiliary authorship string so its tokens are clean before parsing.
      String authClean = StripAndStash.stripAuthorshipMarkers(authorship, ctx.name);
      // Authorship parsed separately by re-tokenising the auxiliary string.
      List<Token> aux = Tokenizer.tokenize(authClean);
      extraState = AuthorshipParser.parse(aux, 0);
      applyAuthorship(ctx.name, extraState);
      // A sanctioning author from the separately supplied authorship is applied here;
      // the embedded path applies its own sanctioning author further below.
      if (extraState.sanctioningAuthor != null) {
        ctx.name.setSanctioningAuthor(extraState.sanctioningAuthor);
      }
    }
    if (authState != null && authState.sanctioningAuthor != null) {
      ctx.name.setSanctioningAuthor(authState.sanctioningAuthor);
    }

    // Code inference uses the main scientific name's authState by default. When the
    // main name had no authorship of its own, fall back to the auxiliary authorship
    // state when it carries a basionym citation (parens) that tips the code:
    //  - "(Author, YYYY)" — the zoological recombination pattern (year inside the parens);
    //  - "(Basionym) Combination" — the botanical recombination pattern (a combination
    //    author follows the parenthesised basionym, no year needed).
    // A bare "(Author)" or plain "Author, year" supplied as separate authorship is parsed
    // for authors but doesn't tip the code on its own.
    AuthorshipParser.AuthState codeState = authState;
    if ((codeState == null || (!codeState.combination.exists() && !codeState.basionymPresent))
        && extraState != null
        && extraState.basionymPresent
        && (extraState.basionym.getYear() != null || extraState.combination.exists())) {
      codeState = extraState;
    }
    // An autonym's species author (captured mid-name) drives code inference when the name
    // had no other authorship — e.g. "Trimezia spathata (Klatt) Baker subsp. spathata"
    // infers BOTANICAL from its basionym+combination authors.
    if ((codeState == null || (!codeState.combination.exists() && !codeState.basionymPresent))
        && autonymState != null) {
      codeState = autonymState;
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

  /**
   * Applies the combination + basionym authorship and any imprint year from a parsed
   * {@link AuthorshipParser.AuthState} onto the name. Shared by the embedded-authorship
   * and the separately-supplied-authorship paths. The sanctioning author and the
   * unparsed remainder are applied by the callers, since those differ between the two
   * paths.
   */
  private static void applyAuthorship(ParsedName name, AuthorshipParser.AuthState st) {
    if (st.combination.exists()) {
      name.setCombinationAuthorship(st.combination);
    }
    if (st.basionym.exists()) {
      name.setBasionymAuthorship(st.basionym);
    }
    if (st.imprintYear != null && name.getImprintYear() == null) {
      name.setImprintYear(st.imprintYear);
    }
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
