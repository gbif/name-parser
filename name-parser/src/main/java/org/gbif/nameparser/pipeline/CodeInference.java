package org.gbif.nameparser.pipeline;

import org.gbif.nameparser.api.NomCode;
import org.gbif.nameparser.api.ParsedName;
import org.gbif.nameparser.api.Rank;
import org.gbif.nameparser.api.Warnings;

/**
 * Infers a name's nomenclatural {@link NomCode} from the shape of its authorship and
 * from the nomenclatural / taxonomic notes the strippers extracted.
 *
 * <p>All code-setting logic lives here so the rule cascade and its priorities sit in
 * one place. {@link Assemble} calls {@link #infer} once, only when the name has no code
 * yet, and {@link #applyRankCodeMismatch} unconditionally afterwards.
 */
public final class CodeInference {

  private CodeInference() {}

  /**
   * Core authorship-based inference: maps the shape of the parsed authorship onto a
   * code, or returns null when the authorship gives no signal. Signal priority,
   * highest first:
   * <ol>
   *   <li>sanctioning author → BOTANICAL</li>
   *   <li>{@code (BasAuthor) RecombAuthor, year} with an explicit infraspecific marker
   *       → BOTANICAL (botanical recombination; the year is the publication year)</li>
   *   <li>any year on an authored span → ZOOLOGICAL (year citation is mandatory in
   *       zoology, optional in botany)</li>
   *   <li>filius suffix without a year → BOTANICAL</li>
   *   <li>basionym + recombination authors without years → BOTANICAL;
   *       basionym-only without years → ZOOLOGICAL</li>
   * </ol>
   */
  static NomCode fromAuthorship(AuthorshipParser.AuthState s, Rank rank) {
    if (s.sanctioningAuthor != null) return NomCode.BOTANICAL;
    // "(BasAuthor) RecombAuthor, year" with an explicit infraspecific rank marker
    // (subsp./var./f.) is the botanical recombination form — the year is the
    // publication year of the recombination, not a zoological author-year citation.
    // Run before the generic "year → zoological" rule.
    if (s.basionymPresent && s.basionym.getYear() == null
        && s.combination.hasAuthors() && s.combination.getYear() != null
        && hasBotanicalRankMarker(rank)) {
      return NomCode.BOTANICAL;
    }
    // Any year (basionym or combination) paired with an actual author is a strong
    // zoological signal: year citation is mandatory in zoological nomenclature but
    // optional in botanical. A bare year with no authors at all is not enough to
    // pick a code.
    boolean basYear = s.basionymPresent && s.basionym.getYear() != null && s.basionym.hasAuthors();
    boolean combYear = s.combination.getYear() != null && s.combination.hasAuthors();
    if (basYear || combYear) {
      return NomCode.ZOOLOGICAL;
    }
    // The "f."/"fil."/"filius" suffix is the standard botanical convention for the son
    // of a same-named author. It does also occur in older zoological literature
    // ("Lacerta agilis Linnaeus f., 1789") but those cases always carry a year and
    // are caught by the year rule above. A filius without any year is therefore a
    // strong botanical hint.
    if (s.hasFilius) return NomCode.BOTANICAL;
    // No years: two-part citation (original author in parens + recombination author)
    // without years is the standard botanical pattern; one-part basionym-only is
    // zoological.
    if (s.basionymPresent) {
      return s.combination.hasAuthors() ? NomCode.BOTANICAL : NomCode.ZOOLOGICAL;
    }
    return null;
  }

  /**
   * Runs the full code-inference cascade onto a name whose code is not yet set. Called
   * by {@link Assemble} only when {@code ctx.name.getCode() == null}. The botanical
   * rules each re-guard on {@code getCode() == null}; the manuscript+marker rule may
   * override an already-inferred ZOOLOGICAL back to BOTANICAL.
   */
  static void infer(ParseContext ctx, AuthorshipParser.AuthState authState) {
    ParsedName n = ctx.name;
    // Code-restricted ranks pin the code regardless of authorship shape.
    Rank r = n.getRank();
    NomCode pinned = r == null ? null : r.isRestrictedToCode();
    if (pinned != null) {
      n.setCode(pinned);
    } else if (authState != null) {
      // For manuscript names with only a basionym citation and no year, the
      // basionym-only-zoological rule doesn't apply — manuscripts are by definition
      // unpublished and the code follows from the eventual publication, not the
      // cited authors. Skip inference in that narrow case.
      boolean skipInference = n.isManuscript()
          && authState.basionymPresent
          && authState.basionym.getYear() == null
          && !authState.combination.hasAuthors()
          && authState.combination.getYear() == null;
      if (!skipInference) {
        NomCode inferred = fromAuthorship(authState, n.getRank());
        if (inferred != null) {
          n.setCode(inferred);
        }
      }
    }
    // A "nom. …" nomenclatural note (nom.cons., nom.illeg., nom.nud., …) is the
    // botanical convention; combined with comb authorship and no year on the author
    // span, it tips the code to BOTANICAL. (Year on the authorship would already
    // have fired the ZOOLOGICAL rule above.) When the name also has an explicit
    // infraspecific marker or ex-authors, those richer signals take precedence and
    // the nom-note is skipped here.
    if (n.getCode() == null && n.getNomenclaturalNote() != null
        && n.getNomenclaturalNote().toLowerCase().startsWith("nom")
        && n.hasCombinationAuthorship()
        && n.getCombinationAuthorship().getYear() == null
        && !ctx.explicitInfraMarker
        && !n.getCombinationAuthorship().hasExAuthors()) {
      n.setCode(NomCode.BOTANICAL);
    }
    // An explicit "subsp." / "var." / "f." marker on a name with no authorship at
    // all but with a parenthesised "(non … YYYY)" homonym citation is a botanical
    // citation pattern (the homonym reference is the sole authorship surrogate).
    if (n.getCode() == null && ctx.explicitInfraMarker
        && hasBotanicalRankMarker(n.getRank())
        && n.getTaxonomicNote() != null
        && !n.hasCombinationAuthorship()
        && (n.getBasionymAuthorship() == null || !n.getBasionymAuthorship().exists())) {
      n.setCode(NomCode.BOTANICAL);
    }
    // Manuscript name with an explicit subsp./var./f. marker is the botanical
    // pattern — manuscripts in zoology don't use rank markers. Force BOTANICAL even
    // when there's some parenthesised pseudo-basionym left over by the parser.
    if (ctx.explicitInfraMarker
        && hasBotanicalRankMarker(n.getRank())
        && n.isManuscript()
        && (n.getCode() == null || n.getCode() == NomCode.ZOOLOGICAL)) {
      n.setCode(NomCode.BOTANICAL);
    }
    // Autonym + explicit rank marker is the botanical convention. Zoological
    // trinomials don't use rank markers and don't repeat the species epithet.
    if (n.getCode() == null && n.isAutonym()
        && ctx.explicitInfraMarker
        && hasBotanicalRankMarker(n.getRank())) {
      n.setCode(NomCode.BOTANICAL);
    }
    // "Author(s) in Editor, YYYY" with a real publication year and no other code
    // signal — the year-bearing in-citation form is the zoological convention.
    // Skip for manuscript names ("ms.", "ined.") and IPNI-style references that
    // end in a year in parens (those are botanical citation form).
    if (n.getCode() == null && ctx.inAuthorCitation
        && ctx.pendingYear != null
        && n.hasCombinationAuthorship()
        && !n.isManuscript()
        && !publishedInLooksBotanical(n.getPublishedIn())) {
      n.setCode(NomCode.ZOOLOGICAL);
    }
  }

  /**
   * Rank-restricted code mismatch with the caller-supplied code → override the code to
   * what the rank requires and warn about it. E.g. supersect. is only valid in botany;
   * if the caller asked for ZOOLOGICAL, surface a CODE_MISMATCH and pin BOTANICAL.
   */
  static void applyRankCodeMismatch(ParseContext ctx) {
    ParsedName n = ctx.name;
    Rank rmm = n.getRank();
    NomCode pinnedRank = rmm == null ? null : rmm.isRestrictedToCode();
    if (pinnedRank != null && n.getCode() != null && n.getCode() != pinnedRank
        && ctx.requestedCode != null && ctx.requestedCode != pinnedRank) {
      n.setCode(pinnedRank);
      n.addWarning(Warnings.CODE_MISMATCH);
    }
  }

  /**
   * Ranks whose canonical rendering uses a botanical-style marker (subsp./var./f.).
   * The same set serves as the "explicit infraspecific marker" test in
   * {@link #fromAuthorship} — zoological trinomials never carry a rank marker.
   */
  private static boolean hasBotanicalRankMarker(Rank r) {
    return r == Rank.SUBSPECIES || r == Rank.VARIETY || r == Rank.SUBVARIETY
        || r == Rank.FORM || r == Rank.SUBFORM;
  }

  /**
   * Heuristic for "this publishedIn looks like a botanical citation". IPNI-style refs
   * end in a parenthesised year ("(1817)") and the in-citation form for them is the
   * botanical convention ("Author in Editor, Title (Year)").
   */
  private static boolean publishedInLooksBotanical(String pub) {
    if (pub == null) return false;
    return pub.matches(".*\\(\\d{4}\\)\\s*\\.?\\s*$");
  }
}
