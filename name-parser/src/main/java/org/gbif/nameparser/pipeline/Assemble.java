package org.gbif.nameparser.pipeline;

import org.gbif.nameparser.api.NameType;
import org.gbif.nameparser.api.NomCode;
import org.gbif.nameparser.api.ParsedName;
import org.gbif.nameparser.api.Rank;
import org.gbif.nameparser.api.Warnings;
import org.gbif.nameparser.util.RankUtils;

import java.util.Map;

/**
 * Final stage: invariants on the produced {@link ParsedName} (rank defaulted,
 * code inferred where possible).
 */
public final class Assemble {

  private Assemble() {}

  static void finish(ParseContext ctx, AuthorshipParser.AuthState authState) {
    ParsedName n = ctx.name;
    if (ctx.aggregate && n.getSpecificEpithet() != null) {
      n.setRank(Rank.SPECIES_AGGREGATE);
    }
    // Caller-supplied rank wins when explicit (and not just UNRANKED) and the parsed
    // structure is compatible.
    Rank requested = ctx.requestedRank;
    if (requested != null && requested != Rank.UNRANKED && requested != n.getRank()) {
      if (requested == Rank.SPECIES_AGGREGATE && n.getSpecificEpithet() != null) {
        n.setRank(Rank.SPECIES_AGGREGATE);
      }
    }
    if (n.getRank() == null) {
      n.setRank(Rank.UNRANKED);
    }
    // Monomial + caller-supplied rank at species level or below → indeterminate
    // placeholder (e.g. "Polygonum" + CULTIVAR → genus "Polygonum" with cv. marker;
    // "Lepidoptera Hooker" + SPECIES → genus parsed as genus-only, but caller says
    // it's a species).
    if (requested != null && requested != Rank.UNRANKED
        && (requested == Rank.SPECIES
            || requested.isInfraspecific() || requested == Rank.CULTIVAR
            || requested == Rank.CULTIVAR_GROUP || requested == Rank.GREX)
        && n.getUninomial() != null && n.getSpecificEpithet() == null
        && n.getType() != NameType.INFORMAL) {
      n.setGenus(n.getUninomial());
      n.setUninomial(null);
      n.setRank(requested);
      n.setType(NameType.INFORMAL);
      n.addWarning(Warnings.INDETERMINED);
      n.setCombinationAuthorship(null);
      n.setBasionymAuthorship(null);
    }
    // Monomial whose rank is strictly infrageneric (SUBGENUS, SECTION_BOTANY, …) →
    // move the uninomial into infragenericEpithet. Triggers both for caller-supplied
    // ranks ("Polygonum" + SUBGENUS) and for a leading rank marker stripped by
    // StripAndStash ("subgen. Trematostoma" → rank=SUBGENUS, uninomial=Trematostoma).
    {
      Rank r = requested != null && requested.isInfragenericStrictly() ? requested : n.getRank();
      if (r != null && r.isInfragenericStrictly()
          && n.getUninomial() != null && n.getGenus() == null
          && n.getInfragenericEpithet() == null) {
        n.setInfragenericEpithet(n.getUninomial());
        n.setUninomial(null);
        n.setRank(r);
      }
    }
    // Binomial (or richer) + caller-supplied higher-rank → keep the parsed structure
    // but pin the rank to what the caller asked, flag the mismatch as informal +
    // doubtful with a RANK_MISMATCH warning ("Polygonum alba" + GENUS).
    if (requested != null && requested != Rank.UNRANKED
        && n.getSpecificEpithet() != null
        && requested.higherThan(Rank.SPECIES)
        && requested != n.getRank()) {
      n.setRank(requested);
      n.setType(NameType.INFORMAL);
      n.setDoubtful(true);
      n.addWarning(Warnings.RANK_MISMATCH);
    }
    // A monomial with an underscore is either:
    //   - "Genus_species" (underscore as space): genus + specific epithet (when after-part starts lowercase)
    //   - GTDB-style phrase name (e.g. "Desulfobacterota_B"): uninomial + phrase (when after-part starts uppercase)
    if (n.getUninomial() != null && n.getUninomial().indexOf('_') >= 0
        && n.getType() == NameType.SCIENTIFIC) {
      String uni = n.getUninomial();
      int idx = uni.indexOf('_');
      String before = uni.substring(0, idx);
      String after = uni.substring(idx + 1);
      if (!after.isEmpty() && Character.isLowerCase(after.codePointAt(0))) {
        // "Oxalis_barrelieri" → genus + specific epithet
        n.setUninomial(null);
        n.setGenus(before);
        n.setSpecificEpithet(after);
        n.setRank(Rank.SPECIES);
      } else {
        // "Desulfobacterota_B" → GTDB-style phrase name
        n.setUninomial(before);
        n.setPhrase(after);
        n.setType(NameType.INFORMAL);
        n.setRank(Rank.UNRANKED);
      }
    }
    if (ctx.pendingUnparsed != null && n.getUnparsed() == null) {
      n.setState(ParsedName.State.PARTIAL);
      n.setUnparsed(ctx.pendingUnparsed);
    }

    if (authState != null && authState.yearRange) {
      n.addWarning(Warnings.YEAR_INTERPRETED);
    }

    // Indeterminate infraspecific names without a phrase ("Nitzschia sinuata var.")
    // carry no valid authorship — any trailing author-like tokens are artefacts of
    // parsing. When there IS a phrase ("Acacia mutabilis Maslin subsp. <phrase>")
    // the species author is meaningful and stays.
    if (n.getType() == NameType.INFORMAL
        && n.getRank() != null && n.getRank().isInfraspecific()
        && n.getInfraspecificEpithet() == null
        && (n.getPhrase() == null || n.getPhrase().isEmpty())) {
      n.setCombinationAuthorship(null);
      n.setBasionymAuthorship(null);
      if (n.getCode() == null) {
        NomCode pinned = n.getRank().isRestrictedToCode();
        if (pinned != null) n.setCode(pinned);
      }
    } else if (n.getCode() == null) {
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
          NomCode inferred = AuthorshipParser.inferCode(authState, n.getRank());
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

    // Rank-restricted code mismatch with the caller-supplied code → override the
    // code to what the rank requires and warn about it. E.g. supersect. is only
    // valid in botany; if the caller asked for ZOOLOGICAL, surface a CODE_MISMATCH
    // and pin BOTANICAL.
    {
      Rank rmm = n.getRank();
      NomCode pinnedRank = rmm == null ? null : rmm.isRestrictedToCode();
      if (pinnedRank != null && n.getCode() != null && n.getCode() != pinnedRank
          && ctx.requestedCode != null && ctx.requestedCode != pinnedRank) {
        n.setCode(pinnedRank);
        n.addWarning(Warnings.CODE_MISMATCH);
      }
    }

    // Zoological trinomials default to SUBSPECIES, not the generic INFRASPECIFIC_NAME:
    // ICZN doesn't use rank markers for subspecies, so a bare "Genus species infra"
    // with the zoological code (caller-supplied or inferred) is by convention a
    // subspecies.
    if (n.getRank() == Rank.INFRASPECIFIC_NAME && n.getCode() == NomCode.ZOOLOGICAL
        && n.getInfraspecificEpithet() != null) {
      n.setRank(Rank.SUBSPECIES);
    }

    // Suffix-based rank inference for monomials: use the explicitly-requested code when
    // provided, otherwise fall back to globally unambiguous suffixes only (-aceae, -oideae).
    // Never apply code-specific suffix maps derived from authorship-inferred code — that
    // would silently assign ranks to names whose code we merely guessed.
    if (n.getRank() == Rank.UNRANKED && n.getUninomial() != null) {
      NomCode codeForInference = ctx.requestedCode;
      Rank r = codeForInference != null
          ? rankFromSuffix(n.getUninomial(), codeForInference)
          : rankFromGlobalSuffix(n.getUninomial());
      if (r != null) n.setRank(r);
    }

    // Flag the literal "null" epithet and any blacklisted epithet as doubtful.
    flagBlacklistedEpithets(n);

    // A cultivar epithet pins the name as a valid scientific identification — clear the
    // INFORMAL flag and INDETERMINED warning that an "sp." indet marker may have left
    // behind ("Symphoricarpos sp. cv. 'mother of pearl'" is a complete cultivar name).
    if (n.getCultivarEpithet() != null) {
      if (n.getType() == NameType.INFORMAL) {
        n.setType(NameType.SCIENTIFIC);
      }
      n.getWarnings().remove(Warnings.INDETERMINED);
    }

    // A phrase epithet without a cultivar always denotes an INFORMAL name — promote a
    // monomial uninomial to a genus so callers see "Baeckea ssp. <phrase>" as a phrase
    // name on a genus, not a uninomial scientific name. Skip promotion for suprageneric
    // phrase forms (e.g. GTDB "Desulfobacterota_B") where the uninomial really is at
    // family/order level.
    if (n.getPhrase() != null && !n.getPhrase().isEmpty() && n.getCultivarEpithet() == null) {
      Rank r = n.getRank();
      boolean belowGenus = r != null && (r.isInfrageneric()
          || r == Rank.SPECIES || r.isInfraspecific());
      if (belowGenus && n.getUninomial() != null && n.getGenus() == null) {
        n.setGenus(n.getUninomial());
        n.setUninomial(null);
      }
      n.setType(NameType.INFORMAL);
    }
  }

  private static void flagBlacklistedEpithets(ParsedName n) {
    String[] epithets = {n.getSpecificEpithet(), n.getInfraspecificEpithet()};
    for (String ep : epithets) {
      if (ep == null) continue;
      if ("null".equalsIgnoreCase(ep)) {
        n.setDoubtful(true);
        n.addWarning(Warnings.NULL_EPITHET);
      } else if (BlacklistedEpithets.contains(ep)) {
        n.setDoubtful(true);
        n.addWarning(Warnings.BLACKLISTED_EPITHET);
      }
    }
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

  /** Ranks whose canonical rendering uses a botanical-style marker (subsp./var./f.). */
  private static boolean hasBotanicalRankMarker(Rank r) {
    return r == Rank.SUBSPECIES || r == Rank.VARIETY || r == Rank.SUBVARIETY
        || r == Rank.FORM || r == Rank.SUBFORM;
  }

  private static Rank rankFromSuffix(String name, NomCode code) {
    Map<String, Rank> suffixes = RankUtils.SUFFICES_RANK_MAP.get(code);
    if (suffixes == null) return null;
    String s = name.toLowerCase();
    for (Map.Entry<String, Rank> e : suffixes.entrySet()) {
      if (s.endsWith(e.getKey())) {
        return e.getValue();
      }
    }
    return null;
  }

  private static Rank rankFromGlobalSuffix(String name) {
    String s = name.toLowerCase();
    // Only the unambiguous cross-code suffixes are safe when no code is known.
    if (s.endsWith("aceae")) return Rank.FAMILY;
    if (s.endsWith("oideae")) return Rank.SUBFAMILY;
    return null;
  }
}
