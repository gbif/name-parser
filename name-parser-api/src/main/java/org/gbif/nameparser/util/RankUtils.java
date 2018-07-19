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
  public static final Map<String, Rank> RANK_MARKER_MAP_INFRAGENERIC = buildRankMarkerMap(
      Arrays.stream(Rank.values()).filter(r -> r.isGenusGroup() && r != GENUS),

      Maps.immutableEntry("suprasect", SUPERSECTION),
      Maps.immutableEntry("supraser", SUPERSERIES),
      Maps.immutableEntry("sect", SECTION),
      Maps.immutableEntry("section", SECTION),
      Maps.immutableEntry("ser", SERIES),
      Maps.immutableEntry("series", SERIES),
      Maps.immutableEntry("subg", SUBGENUS),
      Maps.immutableEntry("subgen", SUBGENUS),
      Maps.immutableEntry("subgenus", SUBGENUS),
      Maps.immutableEntry("subsect", SUBSECTION),
      Maps.immutableEntry("subsection", SUBSECTION),
      Maps.immutableEntry("subser", SUBSERIES),
      Maps.immutableEntry("subseries", SUBSERIES)
  );

  /**
   * Map of species rank markers.
   */
  public static final Map<String, Rank> RANK_MARKER_MAP_SPECIFIC = ImmutableMap.<String, Rank>builder()
      .put("sl", SPECIES_AGGREGATE) // sensu lat
      .put("agg", SPECIES_AGGREGATE)
      .put("aggr", SPECIES_AGGREGATE)
      .put("group", SPECIES_AGGREGATE)
      .put("sp", SPECIES)
      .put("spec", SPECIES)
      .put("species", SPECIES)
      .put("spp", SPECIES)
      .build();

  /**
   * Map of only infraspecific rank markers to their respective rank enum.
   */
  public static final Map<String, Rank> RANK_MARKER_MAP_INFRASPECIFIC = buildRankMarkerMap(
      Arrays.stream(Rank.values()).filter(Rank::isInfraspecific),

      Maps.immutableEntry("aberration", ABERRATION),
      Maps.immutableEntry("bv", BIOVAR),
      Maps.immutableEntry("conv", CONVARIETY),
      Maps.immutableEntry("ct", CHEMOFORM),
      Maps.immutableEntry("cv", CULTIVAR),
      Maps.immutableEntry("f", FORM),
      Maps.immutableEntry("fo", FORM),
      Maps.immutableEntry("form", FORM),
      Maps.immutableEntry("forma", FORM),
      Maps.immutableEntry("fsp", FORMA_SPECIALIS),
      Maps.immutableEntry("fspec", FORMA_SPECIALIS),
      Maps.immutableEntry("gx", GREX),
      Maps.immutableEntry("hort", CULTIVAR),
      Maps.immutableEntry("m", MORPH),
      Maps.immutableEntry("morpha", MORPH),
      Maps.immutableEntry("nat", NATIO),
      Maps.immutableEntry("proles", PROLES),
      Maps.immutableEntry("pv", PATHOVAR),
      Maps.immutableEntry("sf", SUBFORM),
      Maps.immutableEntry("ssp", SUBSPECIES),
      Maps.immutableEntry("st", STRAIN),
      Maps.immutableEntry("subf", SUBFORM),
      Maps.immutableEntry("subform", SUBFORM),
      Maps.immutableEntry("subsp", SUBSPECIES),
      Maps.immutableEntry("subv", SUBVARIETY),
      Maps.immutableEntry("subvar", SUBVARIETY),
      Maps.immutableEntry("sv", SUBVARIETY),
      Maps.immutableEntry("v", VARIETY),
      Maps.immutableEntry("var", VARIETY),
      Maps.immutableEntry("\\*+", INFRASPECIFIC_NAME)
  );

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
