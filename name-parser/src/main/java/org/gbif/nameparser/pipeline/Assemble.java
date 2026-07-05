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
    if (ctx.aggregate && n.getSpecificEpithet() != null && n.getInfraspecificEpithet() == null) {
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

    // Indeterminate infraspecific names ("Nitzschia sinuata var. (Grunow) Lange-Bert.",
    // "Canis lupus subsp. Linnaeus, 1758") keep the authorship trailing the rank marker —
    // it belongs to the (unnamed) infraspecific taxon and is not a parsing artefact.
    if (n.getCode() == null) {
      // All code-setting heuristics live in CodeInference (called only when the name
      // has no code yet).
      CodeInference.infer(ctx, authState);
    }

    if (ctx.viralShape && n.getCode() == null) {
      n.setCode(NomCode.VIRUS);
    }

    // Rank-restricted code mismatch with the caller-supplied code → override the code
    // to what the rank requires and warn (e.g. supersect. is botany-only).
    CodeInference.applyRankCodeMismatch(ctx);

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
    // Viral code is inferred from a highly reliable suffix, so it is safe to drive
    // suffix-based rank inference (e.g. "Coronaviridae" -> FAMILY).
    // Never apply code-specific suffix maps derived from authorship-inferred code — that
    // would silently assign ranks to names whose code we merely guessed.
    if (n.getRank() == Rank.UNRANKED && n.getUninomial() != null) {
      NomCode codeForInference = ctx.requestedCode != null ? ctx.requestedCode
          : (ctx.viralShape ? n.getCode() : null);
      Rank r = codeForInference != null
          ? rankFromSuffix(n.getUninomial(), codeForInference)
          : rankFromGlobalSuffix(n.getUninomial());
      if (r != null) n.setRank(r);
    }

    // Flag the literal "null" epithet and any blacklisted epithet as doubtful.
    flagBlacklistedEpithets(n);

    // Flag implausible authorship years ("Wilcox, 137", "Hall, 0000", "Bromley, 193k7"→193).
    flagUnlikelyYears(n);

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

    // Re-wrap a quoted leading monomial ("'Prosthète'") so the output keeps the quotes that
    // mark it as an unavailable name; the quotes were stripped for parsing in StripAndStash.
    if (ctx.quotedMonomial != null) {
      String q = ctx.quotedMonomial;
      if (n.getUninomial() != null) {
        n.setUninomial(q + n.getUninomial() + q);
      } else if (n.getGenus() != null) {
        n.setGenus(q + n.getGenus() + q);
      }
    }
  }

  /** Plausible authorship years fall in this inclusive range; anything else is flagged. */
  private static final int MIN_YEAR = 1500;
  private static final int MAX_YEAR = 2100;
  private static final java.util.regex.Pattern YEAR_4DIGIT =
      java.util.regex.Pattern.compile("\\d{4}");

  /**
   * A parsed authorship year that isn't a clean 4-digit number in a plausible range is a
   * data-quality artefact ("Wilcox, 137", "Hall, 0000", the "193" truncated from "193k7").
   * Flag the name doubtful and warn. An intentionally uncertain year ("198?") is left alone.
   */
  private static void flagUnlikelyYears(ParsedName n) {
    if (isUnlikelyYear(n.getCombinationAuthorship() == null ? null : n.getCombinationAuthorship().getYear())
        || isUnlikelyYear(n.getBasionymAuthorship() == null ? null : n.getBasionymAuthorship().getYear())) {
      n.setDoubtful(true);
      n.addWarning(Warnings.UNLIKELY_YEAR);
    }
  }

  private static boolean isUnlikelyYear(String year) {
    if (year == null || year.endsWith("?")) return false;
    if (!YEAR_4DIGIT.matcher(year).matches()) return true;
    int v = Integer.parseInt(year);
    return v < MIN_YEAR || v > MAX_YEAR;
  }

  private static void flagBlacklistedEpithets(ParsedName n) {
    // A literal "null" is a data artefact in any name part — uninomial or genus just as much
    // as an epithet ("Null bactus", "Abies null Hood"). Flag the name doubtful in all cases.
    String[] nameParts = {n.getUninomial(), n.getGenus(), n.getSpecificEpithet(), n.getInfraspecificEpithet()};
    for (String part : nameParts) {
      if (part != null && "null".equalsIgnoreCase(part)) {
        n.setDoubtful(true);
        n.addWarning(Warnings.NULL_EPITHET);
      }
    }
    String[] epithets = {n.getSpecificEpithet(), n.getInfraspecificEpithet()};
    for (String ep : epithets) {
      if (ep == null) continue;
      if (BlacklistedEpithets.contains(ep)) {
        n.setDoubtful(true);
        n.addWarning(Warnings.BLACKLISTED_EPITHET);
      }
    }
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
