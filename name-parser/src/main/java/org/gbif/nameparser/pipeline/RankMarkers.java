package org.gbif.nameparser.pipeline;

import org.gbif.nameparser.api.Rank;

import java.util.HashMap;
import java.util.Map;

/**
 * Recognised infraspecific and infrageneric rank markers (case-insensitive,
 * trailing dot optional). Tier 1 keeps the high-coverage subset; rarer markers
 * (cv., grex, microbial bv./ct./sv., agamosp.) come in later tiers.
 */
public final class RankMarkers {

  private static final Map<String, Rank> INFRASPECIFIC = new HashMap<>();
  private static final Map<String, Rank> INFRAGENERIC = new HashMap<>();

  static {
    // infraspecific
    INFRASPECIFIC.put("subsp", Rank.SUBSPECIES);
    INFRASPECIFIC.put("ssp", Rank.SUBSPECIES);
    INFRASPECIFIC.put("var", Rank.VARIETY);
    INFRASPECIFIC.put("subvar", Rank.SUBVARIETY);
    INFRASPECIFIC.put("subv", Rank.SUBVARIETY);
    INFRASPECIFIC.put("f", Rank.FORM);
    INFRASPECIFIC.put("forma", Rank.FORM);
    INFRASPECIFIC.put("form", Rank.FORM);
    INFRASPECIFIC.put("fo", Rank.FORM);
    INFRASPECIFIC.put("subf", Rank.SUBFORM);
    INFRASPECIFIC.put("subforma", Rank.SUBFORM);
    INFRASPECIFIC.put("pv", Rank.PATHOVAR);
    INFRASPECIFIC.put("pathovar", Rank.PATHOVAR);
    INFRASPECIFIC.put("bv", Rank.BIOVAR);
    INFRASPECIFIC.put("biovar", Rank.BIOVAR);
    INFRASPECIFIC.put("ct", Rank.CHEMOFORM);
    INFRASPECIFIC.put("chemoform", Rank.CHEMOFORM);
    INFRASPECIFIC.put("sv", Rank.SEROVAR);
    INFRASPECIFIC.put("serovar", Rank.SEROVAR);
    INFRASPECIFIC.put("morph", Rank.MORPH);
    INFRASPECIFIC.put("morphovar", Rank.MORPHOVAR);
    INFRASPECIFIC.put("phagovar", Rank.PHAGOVAR);
    INFRASPECIFIC.put("nat", Rank.NATIO);
    INFRASPECIFIC.put("natio", Rank.NATIO);
    INFRASPECIFIC.put("mut", Rank.MUTATIO);
    INFRASPECIFIC.put("mutatio", Rank.MUTATIO);
    INFRASPECIFIC.put("agamosp", Rank.SPECIES);
    INFRASPECIFIC.put("agamossp", Rank.SUBSPECIES);
    INFRASPECIFIC.put("agamovar", Rank.VARIETY);
    INFRASPECIFIC.put("conv", Rank.CONVARIETY);
    INFRASPECIFIC.put("convar", Rank.CONVARIETY);
    INFRASPECIFIC.put("subspec", Rank.SUBSPECIES);
    INFRASPECIFIC.put("variety", Rank.VARIETY);
    INFRASPECIFIC.put("fm", Rank.FORM);
    INFRASPECIFIC.put("fma", Rank.FORM);
    INFRASPECIFIC.put("prol", Rank.PROLES);
    INFRASPECIFIC.put("proles", Rank.PROLES);
    INFRASPECIFIC.put("ab", Rank.ABERRATION);
    INFRASPECIFIC.put("aberration", Rank.ABERRATION);
    INFRASPECIFIC.put("strain", Rank.STRAIN);
    INFRASPECIFIC.put("str", Rank.STRAIN);
    // "st." used in some old fungal works as a generic infraspecific marker; map to
    // INFRASPECIFIC_NAME.
    INFRASPECIFIC.put("st", Rank.INFRASPECIFIC_NAME);
    // "*" between two lowercase epithets is an old infraspecific separator.
    INFRASPECIFIC.put("*", Rank.INFRASPECIFIC_NAME);

    // infrageneric
    INFRAGENERIC.put("subg", Rank.SUBGENUS);
    INFRAGENERIC.put("subgen", Rank.SUBGENUS);
    INFRAGENERIC.put("sect", Rank.SECTION_BOTANY);
    INFRAGENERIC.put("subsect", Rank.SUBSECTION_BOTANY);
    INFRAGENERIC.put("supersect", Rank.SUPERSECTION_BOTANY);
    // IPNI also writes "supersect." as "suprasect." — same rank.
    INFRAGENERIC.put("suprasect", Rank.SUPERSECTION_BOTANY);
    INFRAGENERIC.put("ser", Rank.SERIES_BOTANY);
    INFRAGENERIC.put("subser", Rank.SUBSERIES_BOTANY);
  }

  /** Returns the matched rank, recognising "notho-" prefix variants for infrageneric ranks. */
  public static Rank matchInfragenericAllowNotho(String word, boolean[] notho) {
    String w = word.toLowerCase();
    if (w.startsWith("notho")) {
      Rank r = INFRAGENERIC.get(w.substring(5));
      if (r != null) {
        notho[0] = true;
        return r;
      }
    }
    notho[0] = false;
    return INFRAGENERIC.get(w);
  }

  /** Returns the matched rank, recognising "notho-" prefix variants for infraspecific ranks. */
  public static Rank matchInfraspecificAllowNotho(String word, boolean[] notho) {
    String w = word.toLowerCase();
    if (w.startsWith("notho")) {
      Rank r = INFRASPECIFIC.get(w.substring(5));
      if (r != null) {
        notho[0] = true;
        return r;
      }
    }
    notho[0] = false;
    return INFRASPECIFIC.get(w);
  }

  private RankMarkers() {}

  /** Returns the matched infraspecific rank, or null. */
  public static Rank matchInfraspecific(String word) {
    return INFRASPECIFIC.get(word.toLowerCase());
  }

  public static Rank matchInfrageneric(String word) {
    return INFRAGENERIC.get(word.toLowerCase());
  }
}
