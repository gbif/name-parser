package org.gbif.nameparser.util;

import com.google.common.collect.*;
import org.gbif.nameparser.api.NomCode;
import org.gbif.nameparser.api.ParsedName;
import org.gbif.nameparser.api.Rank;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.gbif.nameparser.api.Rank.*;


/**
 *
 */
public class RankUtils {
  /**
   * Matches all dots ("."), underscores ("_") and dashes ("-").
   */
  private static final Pattern NORMALIZE_RANK_MARKER = Pattern.compile("(?:[._ -]+|\\bnotho)");

  private static Map<String, Rank> buildRankMarkerMap(Stream<Rank> ranks, Map.Entry<String, Rank>... additions) {
    Map<String, Rank> map = Maps.newHashMap();
    ranks.forEach(r -> {
      if (r.getMarker() != null) {
        map.put(r.getMarker().replaceAll("\\.", ""), r);
      }
    });
    for (Map.Entry<String, Rank> add : additions) {
      map.put(add.getKey(), add.getValue());
    }
    return ImmutableMap.copyOf(map);
  }

  public static final List<Rank> INFRASUBSPECIFIC_MICROBIAL_RANKS;
  static {
    List<Rank> microbialRanks = Lists.newArrayList();
    for (Rank r : Rank.values()) {
      if (r.isRestrictedToCode()== NomCode.BACTERIAL && r.isInfraspecific()) {
        microbialRanks.add(r);
      }
    }
    INFRASUBSPECIFIC_MICROBIAL_RANKS = ImmutableList.copyOf(microbialRanks);
  }

  /**
   * Map of only suprageneric rank markers to their respective rank enum.
   */
  public static final Map<String, Rank> RANK_MARKER_MAP_FAMILY_GROUP = buildRankMarkerMap(
      Arrays.stream(Rank.values())
          .filter(Rank::isFamilyGroup)
  );

  /**
   * Map of only suprageneric rank markers to their respective rank enum.
   */
  public static final Map<String, Rank> RANK_MARKER_MAP_SUPRAGENERIC = buildRankMarkerMap(
      Arrays.stream(Rank.values()).filter(Rank::isSuprageneric),
      Maps.immutableEntry("ib", SUPRAGENERIC_NAME),
      Maps.immutableEntry("supersubtrib", SUPRAGENERIC_NAME),
      Maps.immutableEntry("trib", TRIBE)
  );

  /**
   * Map of only infrageneric rank markers to their respective rank enum.
   */
  public static final Map<String, Rank> RANK_MARKER_MAP_INFRAGENERIC;
  static {
    Map<String, Rank> ranks = Maps.newHashMap();
    for (Rank r : Rank.values()) {
      if (r.isInfrageneric() && !r.isSpeciesOrBelow() && r.getMarker() != null) {
        ranks.put(r.getMarker().replaceAll("\\.", ""), r);
      }
    }
    ranks.put("sect", SECTION);
    ranks.put("section", SECTION);
    ranks.put("ser", SERIES);
    ranks.put("series", SERIES);
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
   * Map of species rank markers.
   */
  public static final Map<String, Rank> RANK_MARKER_MAP_SPECIFIC;
  static {
    Map<String, Rank> ranks = Maps.newHashMap();
    ranks.put("sl", SPECIES_AGGREGATE); // sensu latu
    ranks.put("agg", SPECIES_AGGREGATE);
    ranks.put("aggr", SPECIES_AGGREGATE);
    ranks.put("sp", SPECIES);
    ranks.put("spec", SPECIES);
    ranks.put("species", SPECIES);
    ranks.put("spp", SPECIES);
    RANK_MARKER_MAP_SPECIFIC = ImmutableMap.copyOf(ranks);
  }

  /**
   * Map of only infraspecific rank markers to their respective rank enum.
   */
  public static final Map<String, Rank> RANK_MARKER_MAP_INFRASPECIFIC;
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

  static class FluentHashMap<K, V> extends java.util.HashMap<K, V> {
    public FluentHashMap<K, V> with(Map<K, V> map) {
      putAll(map);
      return this;
    }
  }

  /**
   * Map of rank markers to their respective rank enum.
   */
  public static final Map<String, Rank> RANK_MARKER_MAP = ImmutableMap.copyOf(
      new FluentHashMap<String, Rank>()
        .with(buildRankMarkerMap(Arrays.stream(Rank.values()), Maps.immutableEntry("subser", SUBSERIES)))
        .with(RANK_MARKER_MAP_SUPRAGENERIC)
        .with(RANK_MARKER_MAP_INFRAGENERIC)
        .with(RANK_MARKER_MAP_SPECIFIC)
        .with(RANK_MARKER_MAP_INFRASPECIFIC)
  );

  /**
   * An immutable map of name suffices to corresponding ranks across all kingdoms.
   * To minimize wrong matches this map is sorted by suffix length with the first suffices being the longest and
   * therefore most accurate matches.
   * See http://www.nhm.ac.uk/hosted-sites/iczn/code/index.jsp?nfv=true&article=29
   */
  public static final SortedMap<String, Rank> SUFFICES_RANK_MAP =
      new ImmutableSortedMap.Builder<String, Rank>(Ordering.natural())
          .put("mycetidae", SUBCLASS)
          .put("phycidae", SUBCLASS)
          .put("mycotina", SUBPHYLUM)
          .put("phytina", SUBPHYLUM)
          .put("phyceae", CLASS)
          .put("mycetes", CLASS)
          .put("mycota", PHYLUM)
          .put("opsida", CLASS)
          .put("oideae", SUBFAMILY)
          .put("aceae", FAMILY)
          .put("phyta", PHYLUM)
          .put("oidea", SUPERFAMILY)
          .put("ineae", SUBORDER)
          .put("anae", SUPERORDER)
          .put("ales", ORDER)
          .put("acea", SUPERFAMILY)
          .put("idae", FAMILY)
          .put("inae", SUBFAMILY)
          .put("eae", TRIBE)
          .put("ini", TRIBE)
          .put("ina", SUBTRIBE)
          .build();

  /**
   * Tries its best to infer a rank from a given rank marker such as subsp.
   *
   * @return the inferred rank or null
   */
  public static Rank inferRank(String rankMarker) {
    if (rankMarker != null) {
      return RANK_MARKER_MAP.get(NORMALIZE_RANK_MARKER.matcher(rankMarker.toLowerCase()).replaceAll(""));
    }
    return null;
  }

  /**
   * Tries its best to infer a rank from an atomised name by just looking at the name parts ignoring any existing rank on the instance.
   * As a final resort for higher monomials the suffices are inspected, but no attempt is made to disambiguate
   * the 2 known homonym suffices -idae and -inae, but instead the far more widespread zoological versions are
   * interpreted.
   * @return the inferred rank or UNRANKED if it cant be found.
   */

  public static Rank inferRank(ParsedName pn) {
    // default if we cant find anything else
    Rank rank = UNRANKED;
    // detect rank based on parsed name
    if (pn.getInfraspecificEpithet() != null) {
      // some infraspecific name
      rank = INFRASPECIFIC_NAME;
    } else if (pn.getSpecificEpithet() != null) {
      // a species
      rank = SPECIES;
    } else if (pn.getInfragenericEpithet() != null) {
      // some infrageneric name
      rank = INFRAGENERIC_NAME;
    } else if (pn.getUninomial() != null) {
      // a suprageneric name, check suffices
      for (String suffix : SUFFICES_RANK_MAP.keySet()) {
        if (pn.getUninomial().endsWith(suffix)) {
          rank = SUFFICES_RANK_MAP.get(suffix);
          break;
        }
      }
    }
    return rank;
  }

}
