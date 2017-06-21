package org.gbif.nameparser;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.gbif.api.vocabulary.Rank;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.regex.Pattern;

import static org.gbif.api.vocabulary.Rank.*;

/**
 * TODO: copied from GBIFNameParser. Move to a shared module
 */
public class GnaRankUtils {
  /**
   * Matches all dots ("."), underscores ("_") and dashes ("-").
   */
  private static final Pattern NORMALIZE_RANK_MARKER = Pattern.compile("(?:[._ -]+|\\bnotho)");

  /**
   * Map of only suprageneric rank markers to their respective rank enum.
   */
  protected static final Map<String, Rank> RANK_MARKER_MAP_SUPRAGENERIC;
  static {
    Map<String, Rank> ranks = Maps.newHashMap();
    for (Rank r : Rank.values()) {
      if (r.isSuprageneric() && r.getMarker() != null) {
        ranks.put(r.getMarker().replaceAll("\\.", ""), r);
      }
    }
    ranks.put("fam", FAMILY);
    ranks.put("gen", GENUS);
    ranks.put("ib", SUPRAGENERIC_NAME);
    ranks.put("sect", SECTION);
    ranks.put("supersubtrib", SUPRAGENERIC_NAME);
    ranks.put("trib", TRIBE);

    RANK_MARKER_MAP_SUPRAGENERIC = ImmutableMap.copyOf(ranks);
  }

  /**
   * Map of only infrageneric rank markers to their respective rank enum.
   */
  protected static final Map<String, Rank> RANK_MARKER_MAP_INFRAGENERIC;
  static {
    Map<String, Rank> ranks = Maps.newHashMap();
    for (Rank r : Rank.values()) {
      if (r.isInfrageneric() && !r.isSpeciesOrBelow() && r.getMarker() != null) {
        ranks.put(r.getMarker().replaceAll("\\.", ""), r);
      }
    }
    ranks.put("sl", SPECIES_AGGREGATE); // sensu latu
    ranks.put("aggr", SPECIES_AGGREGATE);
    ranks.put("sect", SECTION);
    ranks.put("section", SECTION);
    ranks.put("ser", SERIES);
    ranks.put("series", SERIES);
    ranks.put("sp", SPECIES);
    ranks.put("spec", SPECIES);
    ranks.put("species", SPECIES);
    ranks.put("spp", SPECIES);
    ranks.put("subg", SUBGENUS);
    ranks.put("subgen", SUBGENUS);
    ranks.put("subgenus", SUBGENUS);
    ranks.put("subsect", SUBSECTION);
    ranks.put("subsection", SUBSECTION);
    ranks.put("subser", SUBSERIES);
    ranks.put("subseries", SUBSERIES);
    RANK_MARKER_MAP_INFRAGENERIC = ImmutableMap.copyOf(ranks);
  }

  /**
   * Map of only infraspecific rank markers to their respective rank enum.
   */
  protected static final Map<String, Rank> RANK_MARKER_MAP_INFRASPECIFIC;
  static {
    Map<String, Rank> ranks = Maps.newHashMap();
    for (Rank r : Rank.values()) {
      if (r.isInfraspecific() && r.getMarker() != null) {
        ranks.put(r.getMarker().replaceAll("\\.", ""), r);
      }
    }
    ranks.put("aberration", ABERRATION);
    ranks.put("bv", BIOVAR);
    ranks.put("conv", CONVARIETY);
    ranks.put("ct", CHEMOFORM);
    ranks.put("cv", CULTIVAR);
    ranks.put("f", FORM);
    ranks.put("fo", FORM);
    ranks.put("form", FORM);
    ranks.put("forma", FORM);
    ranks.put("fsp", FORMA_SPECIALIS);
    ranks.put("fspec", FORMA_SPECIALIS);
    ranks.put("gx", GREX);
    ranks.put("hort", CULTIVAR);
    ranks.put("m", MORPH);
    ranks.put("morpha", MORPH);
    ranks.put("nat", NATIO);
    ranks.put("proles", PROLES);
    ranks.put("pv", PATHOVAR);
    ranks.put("sf", SUBFORM);
    ranks.put("ssp", SUBSPECIES);
    ranks.put("st", STRAIN);
    ranks.put("subf", SUBFORM);
    ranks.put("subform", SUBFORM);
    ranks.put("subsp", SUBSPECIES);
    ranks.put("subv", SUBVARIETY);
    ranks.put("subvar", SUBVARIETY);
    ranks.put("sv", SUBVARIETY);
    ranks.put("v", VARIETY);
    ranks.put("var", VARIETY);
    ranks.put("\\*+", INFRASPECIFIC_NAME);
    RANK_MARKER_MAP_INFRASPECIFIC = ImmutableMap.copyOf(ranks);
  }

  /**
   * Map of rank markers to their respective rank enum.
   */
  protected static final Map<String, Rank> RANK_MARKER_MAP;
  static {
    Map<String, Rank> ranks = Maps.newHashMap();
    for (Rank r : Rank.values()) {
      if (r.getMarker() != null) {
        ranks.put(r.getMarker().replaceAll("\\.", ""), r);
      }
    }
    ranks.putAll(RANK_MARKER_MAP_SUPRAGENERIC);
    ranks.putAll(RANK_MARKER_MAP_INFRAGENERIC);
    ranks.putAll(RANK_MARKER_MAP_INFRASPECIFIC);
    ranks.put("subser", SUBSERIES);
    RANK_MARKER_MAP = ImmutableMap.copyOf(ranks);
  }

  /**
   * Tries its best to infer a rank from a given rank marker such as subsp.
   *
   * @return the inferred rank or null
   */
  public static Rank inferRank(@Nullable String rankMarker) {
    if (rankMarker != null) {
      return RANK_MARKER_MAP.get(NORMALIZE_RANK_MARKER.matcher(rankMarker.toLowerCase()).replaceAll(""));
    }
    return null;
  }



}
