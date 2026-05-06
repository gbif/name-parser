package org.gbif.nameparser.pipeline;

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
    // Monomial + caller-supplied rank SPECIES → indeterminate species placeholder
    // (e.g. "Lepidoptera Hooker" parsed as genus-only, but caller says it's a species)
    if (requested == Rank.SPECIES
        && n.getUninomial() != null && n.getSpecificEpithet() == null
        && n.getType() != org.gbif.nameparser.api.NameType.INFORMAL) {
      n.setGenus(n.getUninomial());
      n.setUninomial(null);
      n.setRank(Rank.SPECIES);
      n.setType(org.gbif.nameparser.api.NameType.INFORMAL);
      n.addWarning(Warnings.INDETERMINED);
      n.setCombinationAuthorship(null);
      n.setBasionymAuthorship(null);
    }
    // A monomial with an underscore is either:
    //   - "Genus_species" (underscore as space): genus + specific epithet (when after-part starts lowercase)
    //   - GTDB-style phrase name (e.g. "Desulfobacterota_B"): uninomial + phrase (when after-part starts uppercase)
    if (n.getUninomial() != null && n.getUninomial().indexOf('_') >= 0
        && n.getType() == org.gbif.nameparser.api.NameType.SCIENTIFIC) {
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
        n.setType(org.gbif.nameparser.api.NameType.INFORMAL);
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

    // Indeterminate infraspecific names (e.g. "Nitzschia sinuata var.") carry no
    // valid authorship — any trailing author-like tokens are artefacts of parsing.
    if (n.getType() == org.gbif.nameparser.api.NameType.INFORMAL
        && n.getRank() != null && n.getRank().isInfraspecific()
        && n.getInfraspecificEpithet() == null) {
      n.setCombinationAuthorship(null);
      n.setBasionymAuthorship(null);
      // Skip code inference for indet infraspecific names
    } else if (n.getCode() == null) {
      // Code-restricted ranks pin the code regardless of authorship shape.
      Rank r = n.getRank();
      NomCode pinned = r == null ? null : r.isRestrictedToCode();
      if (pinned != null) {
        n.setCode(pinned);
      } else if (r == Rank.INFRASUBSPECIFIC_NAME || r == Rank.NATIO) {
        n.setCode(NomCode.ZOOLOGICAL);
      } else if (authState != null) {
        NomCode inferred = AuthorshipParser.inferCode(authState);
        if (inferred != null) {
          n.setCode(inferred);
        }
      }
      // A nom-note plus a "non …" / "auct. non" tax-note (homonym citation) is a
      // strong botanical signal.
      if (n.getCode() == null && n.getNomenclaturalNote() != null && n.getTaxonomicNote() != null
          && n.hasCombinationAuthorship()
          && (n.getCombinationAuthorship() == null || n.getCombinationAuthorship().getYear() == null)) {
        String tn = n.getTaxonomicNote().toLowerCase();
        if (tn.startsWith("non") || tn.startsWith("auct")) {
          n.setCode(NomCode.BOTANICAL);
        }
      }
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
