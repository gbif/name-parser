package org.gbif.nameparser.pipeline;

import org.gbif.nameparser.api.NomCode;
import org.gbif.nameparser.api.ParsedName;
import org.gbif.nameparser.api.Rank;

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
    // A monomial with an underscore is a phrase-name-style informal taxon code
    // (e.g. GTDB names like "Desulfobacterota_B"): split into monomial + phrase.
    if (n.getUninomial() != null && n.getUninomial().indexOf('_') >= 0
        && n.getType() == org.gbif.nameparser.api.NameType.SCIENTIFIC) {
      String uni = n.getUninomial();
      int idx = uni.indexOf('_');
      n.setUninomial(uni.substring(0, idx));
      n.setPhrase(uni.substring(idx + 1));
      n.setType(org.gbif.nameparser.api.NameType.INFORMAL);
      n.setRank(Rank.UNRANKED);
    }
    if (ctx.pendingUnparsed != null && n.getUnparsed() == null) {
      n.setState(ParsedName.State.PARTIAL);
      n.setUnparsed(ctx.pendingUnparsed);
    }

    if (n.getCode() == null) {
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

    // Family-suffix rank inference: use the caller-supplied code when available;
    // fall back to unambiguous cross-code suffixes (-aceae → botanical family,
    // -idae → zoological family) so bare monomials like "Boldiaceae" get a rank.
    if (n.getRank() == Rank.UNRANKED && n.getUninomial() != null) {
      Rank r = ctx.requestedCode != null
          ? familyRank(n.getUninomial(), ctx.requestedCode)
          : familyRankUnambiguous(n.getUninomial());
      if (r != null) n.setRank(r);
    }
  }

  private static boolean isBotanicalInfraspecific(Rank r) {
    if (r == null) return false;
    switch (r) {
      case SUBSPECIES:
      case VARIETY:
      case SUBVARIETY:
      case FORM:
      case SUBFORM:
      case INFRASPECIFIC_NAME:
        return true;
      default:
        return false;
    }
  }

  private static Rank familyRankUnambiguous(String name) {
    String s = name.toLowerCase();
    // Only the exclusively-botanical suffixes are safe without a code.
    // Zoological -idae/-oidea need an explicit ZOOLOGICAL code to avoid false positives.
    if (s.endsWith("aceae")) return Rank.FAMILY;
    if (s.endsWith("oideae")) return Rank.SUBFAMILY;
    return null;
  }

  private static Rank familyRank(String name, NomCode code) {
    String s = name.toLowerCase();
    switch (code) {
      case ZOOLOGICAL:
        if (s.endsWith("oidea")) return Rank.SUPERFAMILY;
        if (s.endsWith("idae")) return Rank.FAMILY;
        if (s.endsWith("inae")) return Rank.SUBFAMILY;
        if (s.endsWith("ini")) return Rank.TRIBE;
        if (s.endsWith("ina")) return Rank.SUBTRIBE;
        return null;
      case BOTANICAL:
        if (s.endsWith("aceae")) return Rank.FAMILY;
        if (s.endsWith("oideae")) return Rank.SUBFAMILY;
        if (s.endsWith("eae")) return Rank.TRIBE;
        if (s.endsWith("inae")) return Rank.SUBTRIBE;
        return null;
      default:
        return null;
    }
  }
}
