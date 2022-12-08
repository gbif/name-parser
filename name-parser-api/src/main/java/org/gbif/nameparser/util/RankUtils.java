/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.nameparser.util;

import org.gbif.nameparser.api.LinneanName;
import org.gbif.nameparser.api.NomCode;
import org.gbif.nameparser.api.Rank;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import static org.gbif.nameparser.api.Rank.*;


/**
 *
 */
public class RankUtils {
  /**
   * Matches all dots ("."), underscores ("_") and dashes ("-").
   */
  private static final Pattern NORMALIZE_RANK_MARKER = Pattern.compile("(?:[._ -]+|\\b(?:notho|agamo))");
  private static List<Rank> LINNEAN_RANKS_REVERSE = Lists.reverse(Rank.LINNEAN_RANKS);

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
      if (r.isRestrictedToCode() == NomCode.BACTERIAL && r.isInfraspecific()) {
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

  public static final String ALPHA_DELTA = "ɑα⍺βɣγδẟ";

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
      Maps.immutableEntry("proles", PROLE),
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
      Maps.immutableEntry("tinfr", INFRASPECIFIC_NAME),
      Maps.immutableEntry("v", VARIETY),
      Maps.immutableEntry("var", VARIETY),
      Maps.immutableEntry("nvar", VARIETY),
      Maps.immutableEntry("\\*+", INFRASPECIFIC_NAME),
      Maps.immutableEntry("["+ALPHA_DELTA+"]", INFRASPECIFIC_NAME)
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
   * An immutable map of name suffices to corresponding ranks for each code.
   * To minimize wrong matches this map is sorted by suffix length with the first suffices being the longest and
   * therefore most accurate matches.
   * See https://code.iczn.org/formation-and-treatment-of-names/article-29-family-group-names/#art-29-2
   * and https://en.wikipedia.org/wiki/Taxonomic_rank#Terminations_of_names
   */
  public static final Map<NomCode, Map<String, Rank>> SUFFICES_RANK_MAP =
      // ImmutableMap keeps insertion order
      ImmutableMap.of(
          NomCode.BACTERIAL, new ImmutableMap.Builder<String, Rank>()
              .put("oideae", SUBFAMILY)
              .put("aceae", FAMILY)
              .put("ineae", SUBORDER)
              .put("ales", ORDER)
              .put("idae", SUBCLASS)
              .put("inae", SUBTRIBE)
              .put("eae", TRIBE)
              .put("ia", CLASS)
              .build(),
          NomCode.BOTANICAL, new ImmutableMap.Builder<String, Rank>()
              .put("mycetidae", SUBCLASS)
              .put("phycidae", SUBCLASS)
              .put("mycotina", SUBPHYLUM)
              .put("phytina", SUBPHYLUM)
              .put("mycetes", CLASS)
              .put("phyceae", CLASS)
              .put("mycota", PHYLUM)
              .put("opsida", CLASS)
              .put("oideae", SUBFAMILY)
              .put("phyta", PHYLUM)
              .put("ineae", SUBORDER)
              .put("aceae", FAMILY)
              .put("idae", SUBCLASS)
              .put("anae", SUPERORDER)
              .put("acea", SUPERFAMILY)
              .put("aria", INFRAORDER)
              .put("ales", ORDER)
              .put("inae", SUBTRIBE)
              .put("eae", TRIBE)
              .build(),
          NomCode.ZOOLOGICAL, new ImmutableMap.Builder<String, Rank>()
              .put("oidea", SUPERFAMILY)
              .put("oidae", EPIFAMILY)
              .put("idae", FAMILY)
              .put("inae", SUBFAMILY)
              .put("ini", TRIBE)
              .put("ina", SUBTRIBE)
              .build(),
          NomCode.VIRUS, new ImmutableMap.Builder<String, Rank>()
              .put("viria", REALM)
              .put("vira", SUBREALM)
              .put("viriae", KINGDOM)
              .put("virites", SUBKINGDOM)
              .put("viricota", PHYLUM)
              .put("viricotina", SUBPHYLUM)
              .put("viricetes", CLASS)
              .put("viricetidae", SUBCLASS)
              .put("virales", ORDER)
              .put("virineae", SUBORDER)
              .put("viridae", FAMILY)
              .put("virinae", SUBFAMILY)
              .build()
      );
  
  private static final Map<String, Rank> GLOBAL_SUFFICES_RANK_MAP;
  static {
    Set<String> nonUnique = new HashSet<>();
    Map<String, Rank> suffices = new TreeMap<>(new Comparator<String>() {
      @Override
      public int compare(String s1, String s2) {
        if (s1.length() > s2.length()) {
          return -1;
        } else if (s1.length() < s2.length()) {
          return 1;
        } else {
          return s1.compareTo(s2);
        }
      }
    });
    SUFFICES_RANK_MAP.values().forEach(map -> {
      for (Map.Entry<String, Rank> e : map.entrySet()) {
        if (e.getKey().length() > 4) {
          if (!nonUnique.contains(e.getKey())) {
            if (e.getValue() != suffices.getOrDefault(e.getKey(), e.getValue())) {
              nonUnique.add(e.getKey());
              suffices.remove(e.getKey());
            } else {
              suffices.put(e.getKey(), e.getValue());
            }
          }
        }
      }
    });
    GLOBAL_SUFFICES_RANK_MAP = ImmutableMap.copyOf(suffices);
  }
  
  
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
   * As a final resort for higher monomials the suffices are inspected with the help of the supplied nomenclatural code (see LinneanName instance).
   * If no code is given certain suffices are ambiguous (e.g. -idae and -inae) and cannot be inferred!
   *
   * @return the inferred rank or UNRANKED if it cant be found.
   */
  public static Rank inferRank(LinneanName pn) {
    // detect rank based on parsed name
    if (pn.getInfraspecificEpithet() != null) {
      // some infraspecific name
      return INFRASPECIFIC_NAME;
      
    } else if (pn.getSpecificEpithet() != null) {
      // a species
      return SPECIES;
      
    } else if (pn.getInfragenericEpithet() != null) {
      // some infrageneric name
      return INFRAGENERIC_NAME;
      
    } else if (pn.getUninomial() != null) {
      // a suprageneric name, check suffices
      Map<String, Rank> suffices;
      if (pn.getCode() == null) {
        suffices = GLOBAL_SUFFICES_RANK_MAP;
      } else {
        suffices = SUFFICES_RANK_MAP.getOrDefault(pn.getCode(), Collections.emptyMap());
      }
      for (Map.Entry<String, Rank> e : suffices.entrySet()) {
        if (pn.getUninomial().endsWith(e.getKey())) {
          return e.getValue();
        }
      }
    }
    // default if we cant find anything else
    return UNRANKED;
  }


  /**
   * @return a list of all ranks above or equal the given minimum rank.
   */
  public static List<Rank> minRanks(Rank rank) {
    return Arrays.stream(Rank.values()).filter(
        r -> r.ordinal() <= rank.ordinal()
    ).collect(Collectors.toList());
  }

  /**
   * @return a list of all ranks below or equal the given maximum rank.
   */
  public static List<Rank> maxRanks(Rank rank) {
    return Arrays.stream(Rank.values()).filter(
        r -> r.ordinal() >= rank.ordinal()
    ).collect(Collectors.toList());
  }

  /**
   * Returns true if r1 is a higher rank than r2 and none of the 2 ranks are uncomparable or ambiguous between codes.
   */
  public static boolean higherThanCodeAgnostic(Rank r1, Rank r2) {
    return (!r1.isUncomparable() && !r2.isUncomparable() && r1.higherThan(r2) && !r1.isAmbiguous() && !r2.isAmbiguous());
  }

  /**
   * The ranks between the given minimum and maximum
   * @param inclusive if true also include the given min and max ranks
   */
  public static Set<Rank> between(Rank min, Rank max, boolean inclusive) {
    Set<Rank> ranks = new HashSet<>(RankUtils.minRanks(min));
    ranks.retainAll(RankUtils.maxRanks(max));
    if (!inclusive) {
      ranks.remove(min);
      ranks.remove(max);
    }
    return ranks;
  }

  public static Rank nextLowerLinneanRank(Rank rank) {
    for (Rank r : Rank.LINNEAN_RANKS) {
      if (r.ordinal() > rank.ordinal()) {
        return r;
      }
    }
    return null;
  }

  public static Rank nextHigherLinneanRank(Rank rank) {
    for (Rank r : LINNEAN_RANKS_REVERSE) {
      if (r.ordinal() < rank.ordinal()) {
        return r;
      }
    }
    return null;
  }

  public static Rank lowestRank(Collection<Rank> ranks) {
    if (ranks != null && !ranks.isEmpty()) {
      LinkedList<Rank> rs = new LinkedList<>(ranks);
      Collections.sort(rs);
      return rs.getLast();
    }
    return null;
  }

}
