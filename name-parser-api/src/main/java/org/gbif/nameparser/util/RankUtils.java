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

import static org.gbif.nameparser.api.Rank.*;


/**
 *
 */
public class RankUtils {
  /**
   * Matches all dots ("."), underscores ("_") and dashes ("-").
   */
  private static final Pattern NORMALIZE_RANK_MARKER = Pattern.compile("(?:[._ -]+|\\b(?:notho|agamo))");
  private static final List<Rank> LINNEAN_RANKS_REVERSE;
  static {
    var rev = new ArrayList<>(Rank.LINNEAN_RANKS);
    Collections.reverse(rev);
    LINNEAN_RANKS_REVERSE = List.copyOf(rev);
  }
  private static final Pattern NORMALIZE_RANK_MARKER_PATTERN = Pattern.compile("\\.");

  /**
   * @return the normalised rank marker or null if there is none
   */
  public static String rankMarker(Rank rank) {
    return rank == null || rank.getMarker() == null ? null :
           NORMALIZE_RANK_MARKER_PATTERN.matcher(rank.getMarker())
                                        .replaceAll("")
                                        .toLowerCase();
  }

  @SafeVarargs
  private static Map<String, Rank> buildRankMarkerMap(Stream<Rank> ranks, Map.Entry<String, Rank>... additions) {
    Map<String, Rank> map = new HashMap<>();
    ranks.forEach(r -> {
      if (r.getMarker() != null) {
        map.put(rankMarker(r), r);
      }
    });
    for (Map.Entry<String, Rank> add : additions) {
      map.put(add.getKey(), add.getValue());
    }
    return Map.copyOf(map);
  }
  
  public static final List<Rank> INFRASUBSPECIFIC_MICROBIAL_RANKS;
  
  static {
    List<Rank> microbialRanks = new ArrayList<>();
    for (Rank r : Rank.values()) {
      if (r.getCode() == NomCode.BACTERIAL && r.isInfraspecific()) {
        microbialRanks.add(r);
      }
    }
    INFRASUBSPECIFIC_MICROBIAL_RANKS = List.copyOf(microbialRanks);
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
      Map.entry("ib", SUPRAGENERIC_NAME),
      Map.entry("supersubtrib", SUPRAGENERIC_NAME),
      Map.entry("trib", TRIBE)
  );
  
  /**
   * Map of only infrageneric, normalised rank markers to their respective rank enum.
   * Warning! For section and series ranks only the botanical rank is given.
   * You can access the zoological version from the botanical rank via its Rank.sss() method.
   */
  public static final Map<String, Rank> RANK_MARKER_MAP_INFRAGENERIC = buildRankMarkerMap(
      Arrays.stream(Rank.values()).filter(r -> r.isGenusGroup() && r != GENUS),
      
      Map.entry("suprasect", SUPERSECTION_BOTANY),
      Map.entry("supraser", SUPERSERIES_BOTANY),
      Map.entry("sect", SECTION_BOTANY),
      Map.entry("section", SECTION_BOTANY),
      Map.entry("ser", SERIES_BOTANY),
      Map.entry("series", SERIES_BOTANY),
      Map.entry("subg", SUBGENUS),
      Map.entry("subgen", SUBGENUS),
      Map.entry("subgenus", SUBGENUS),
      Map.entry("subsect", SUBSECTION_BOTANY),
      Map.entry("subsection", SUBSECTION_BOTANY),
      Map.entry("subser", SUBSERIES_BOTANY),
      Map.entry("subseries", SUBSERIES_BOTANY)
  );
  
  /**
   * Map of species rank markers.
   */
  public static final Map<String, Rank> RANK_MARKER_MAP_SPECIFIC = Map.of(
      "sl", SPECIES_AGGREGATE, // sensu lat
      "agg", SPECIES_AGGREGATE,
      "aggr", SPECIES_AGGREGATE,
      "group", SPECIES_AGGREGATE,
      "sp", SPECIES,
      "spec", SPECIES,
      "species", SPECIES,
      "spp", SPECIES);

  public static final String ALPHA_DELTA = "ɑα⍺βɣγδẟ";

  /**
   * Map of only infraspecific rank markers to their respective rank enum.
   */
  public static final Map<String, Rank> RANK_MARKER_MAP_INFRASPECIFIC = buildRankMarkerMap(
      Arrays.stream(Rank.values()).filter(Rank::isInfraspecific),
      
      Map.entry("aberration", ABERRATION),
      Map.entry("bv", BIOVAR),
      Map.entry("conv", CONVARIETY),
      Map.entry("ct", CHEMOFORM),
      Map.entry("cv", CULTIVAR),
      Map.entry("f", FORM),
      Map.entry("form", FORM),
      Map.entry("forma", FORM),
      Map.entry("fsp", FORMA_SPECIALIS),
      Map.entry("fspec", FORMA_SPECIALIS),
      Map.entry("gx", GREX),
      Map.entry("hort", CULTIVAR),
      Map.entry("m", MORPH),
      Map.entry("morpha", MORPH),
      Map.entry("nat", NATIO),
      Map.entry("proles", PROLES),
      Map.entry("pv", PATHOVAR),
      Map.entry("sf", SUBFORM),
      Map.entry("ssp", SUBSPECIES),
      Map.entry("st", STRAIN),
      Map.entry("subf", SUBFORM),
      Map.entry("subform", SUBFORM),
      Map.entry("subsp", SUBSPECIES),
      Map.entry("subv", SUBVARIETY),
      Map.entry("subvar", SUBVARIETY),
      Map.entry("sv", SUBVARIETY),
      Map.entry("tinfr", INFRASPECIFIC_NAME),
      Map.entry("v", VARIETY),
      Map.entry("var", VARIETY),
      Map.entry("nvar", VARIETY),
      Map.entry("\\*+", INFRASPECIFIC_NAME),
      Map.entry("["+ALPHA_DELTA+"]", INFRASPECIFIC_NAME)
  );

  static class FluentHashMap<K, V> extends java.util.HashMap<K, V> {
    public FluentHashMap<K, V> with(Map<K, V> map) {
      putAll(map);
      return this;
    }
  }
  
  /**
   * Map of rank markers to their respective rank enum.
   * Note that the section ranks point to botany only here
   * */
  public static final Map<String, Rank> RANK_MARKER_MAP = Map.copyOf(
      new FluentHashMap<String, Rank>()
          .with(buildRankMarkerMap(Arrays.stream(Rank.values()), Map.entry("subser", SUBSERIES_BOTANY)))
          .with(RANK_MARKER_MAP_SUPRAGENERIC)
          .with(RANK_MARKER_MAP_INFRAGENERIC)
          .with(RANK_MARKER_MAP_SPECIFIC)
          .with(RANK_MARKER_MAP_INFRASPECIFIC)
  );

  /**
   * @return an unmodifiable view of a linked hash map of the given entries in that very order.
   */
  @SafeVarargs
  public static <K,V> Map<K,V> linkedHashMap(Map.Entry<K, V>... entries) {
    var map = new LinkedHashMap<K,V>();
    if (entries != null) {
      for (var e : entries) {
        map.put(e.getKey(), e.getValue());
      }
    }
    return Collections.unmodifiableMap(map);
  }

  /**
   * An immutable map of name suffices to corresponding ranks for each code.
   * To minimize wrong matches this map is sorted by suffix length with the first suffices being the longest and
   * therefore most accurate matches.
   * See https://code.iczn.org/formation-and-treatment-of-names/article-29-family-group-names/#art-29-2
   * and https://en.wikipedia.org/wiki/Taxonomic_rank#Terminations_of_names
   */
  public static final Map<NomCode, Map<String, Rank>> SUFFICES_RANK_MAP =
      // ImmutableMap keeps insertion order
      Map.of(
          NomCode.BACTERIAL, linkedHashMap(
              Map.entry("oideae", SUBFAMILY),
              Map.entry("aceae", FAMILY),
              Map.entry("ineae", SUBORDER),
              Map.entry("ales", ORDER),
              Map.entry("idae", SUBCLASS),
              Map.entry("inae", SUBTRIBE),
              Map.entry("eae", TRIBE),
              Map.entry("ia", CLASS)
          ),
          NomCode.BOTANICAL, linkedHashMap(
              Map.entry("mycetidae", SUBCLASS),
              Map.entry("phycidae", SUBCLASS),
              Map.entry("mycotina", SUBPHYLUM),
              Map.entry("phytina", SUBPHYLUM),
              Map.entry("mycetes", CLASS),
              Map.entry("phyceae", CLASS),
              Map.entry("mycota", PHYLUM),
              Map.entry("opsida", CLASS),
              Map.entry("oideae", SUBFAMILY),
              Map.entry("phyta", PHYLUM),
              Map.entry("ineae", SUBORDER),
              Map.entry("aceae", FAMILY),
              Map.entry("idae", SUBCLASS),
              Map.entry("anae", SUPERORDER),
              Map.entry("acea", SUPERFAMILY),
              Map.entry("aria", INFRAORDER),
              Map.entry("ales", ORDER),
              Map.entry("inae", SUBTRIBE),
              Map.entry("eae", TRIBE)
          ),
          NomCode.ZOOLOGICAL, linkedHashMap(
              Map.entry("oidea", SUPERFAMILY),
              Map.entry("oidae", EPIFAMILY),
              Map.entry("idae", FAMILY),
              Map.entry("inae", SUBFAMILY),
              Map.entry("ini", TRIBE),
              Map.entry("ina", SUBTRIBE)
          ),
          NomCode.VIRUS, linkedHashMap(
              Map.entry("viricetidae", SUBCLASS),
              Map.entry("viricotina", SUBPHYLUM),
              Map.entry("viricetes", CLASS),
              Map.entry("viricota", PHYLUM),
              Map.entry("virineae", SUBORDER),
              Map.entry("virites", SUBKINGDOM),
              Map.entry("virales", ORDER),
              Map.entry("viridae", FAMILY),
              Map.entry("virinae", SUBFAMILY),
              Map.entry("viriae", KINGDOM),
              Map.entry("viria", REALM),
              Map.entry("vira", SUBREALM)
          )
      );
  
  static final Map<String, Rank> GLOBAL_SUFFICES_RANK_MAP;
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
    GLOBAL_SUFFICES_RANK_MAP = Collections.unmodifiableMap(suffices);
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
  /**
   * Checks if there is a different rank existing in a given nomenclatural rank which is better suited
   * in case the inout rank is ambiguous, i.e. a rank like section which exists in several codes but in different placement
   * and we therefore also have several enum instances for it.
   *
   * In all cases but the sections currently this will return the original inout rank.
   * Ony for sections it verifies that the given rank is the correct one for the given code.
   */
  public static Rank bestCodeCompliantRank(Rank rank, NomCode code) {
    switch (rank) {
      case SUPERSECTION_BOTANY:
        return selectRank(rank, code, SUPERSECTION_ZOOLOGY, NomCode.ZOOLOGICAL);
      case SECTION_BOTANY:
        return selectRank(rank, code, SECTION_ZOOLOGY, NomCode.ZOOLOGICAL);
      case SUBSECTION_BOTANY:
        return selectRank(rank, code, SUBSECTION_ZOOLOGY, NomCode.ZOOLOGICAL);

      case SUPERSECTION_ZOOLOGY:
        return selectRank(rank, code, SUPERSECTION_BOTANY, NomCode.BOTANICAL, NomCode.BACTERIAL, NomCode.CULTIVARS);
      case SECTION_ZOOLOGY:
        return selectRank(rank, code, SECTION_BOTANY, NomCode.BOTANICAL, NomCode.BACTERIAL, NomCode.CULTIVARS);
      case SUBSECTION_ZOOLOGY:
        return selectRank(rank, code, SUBSECTION_BOTANY, NomCode.BOTANICAL, NomCode.BACTERIAL, NomCode.CULTIVARS);

      case SUPERSERIES_BOTANY:
        return selectRank(rank, code, SUPERSERIES_ZOOLOGY, NomCode.ZOOLOGICAL);
      case SERIES_BOTANY:
        return selectRank(rank, code, SERIES_ZOOLOGY, NomCode.ZOOLOGICAL);
      case SUBSERIES_BOTANY:
        return selectRank(rank, code, SUBSERIES_ZOOLOGY, NomCode.ZOOLOGICAL);

      case SUPERSERIES_ZOOLOGY:
        return selectRank(rank, code, SUPERSERIES_BOTANY, NomCode.BOTANICAL, NomCode.BACTERIAL, NomCode.CULTIVARS);
      case SERIES_ZOOLOGY:
        return selectRank(rank, code, SERIES_BOTANY, NomCode.BOTANICAL, NomCode.BACTERIAL, NomCode.CULTIVARS);
      case SUBSERIES_ZOOLOGY:
        return selectRank(rank, code, SUBSERIES_BOTANY, NomCode.BOTANICAL, NomCode.BACTERIAL, NomCode.CULTIVARS);
    }
    return rank;
  }

  public static Rank otherAmbiguousRank(Rank rank) {
    switch (rank) {
      case SUPERSECTION_BOTANY:
        return SUPERSECTION_ZOOLOGY;
      case SECTION_BOTANY:
        return SECTION_ZOOLOGY;
      case SUBSECTION_BOTANY:
        return SUBSECTION_ZOOLOGY;

      case SUPERSECTION_ZOOLOGY:
        return SUPERSECTION_BOTANY;
      case SECTION_ZOOLOGY:
        return SECTION_BOTANY;
      case SUBSECTION_ZOOLOGY:
        return SUBSECTION_BOTANY;

      case SUPERSERIES_BOTANY:
        return SUPERSERIES_ZOOLOGY;
      case SERIES_BOTANY:
        return SERIES_ZOOLOGY;
      case SUBSERIES_BOTANY:
        return SUBSERIES_ZOOLOGY;

      case SUPERSERIES_ZOOLOGY:
        return SUPERSERIES_BOTANY;
      case SERIES_ZOOLOGY:
        return SERIES_BOTANY;
      case SUBSERIES_ZOOLOGY:
        return SUBSERIES_BOTANY;
    }
    return rank;
  }

  private static Rank selectRank(Rank original, NomCode code, Rank other, NomCode... otherCodes) {
    for (NomCode c : otherCodes) {
      if (c == code) {
        return other;
      }
    }
    return original;
  }

  private static final Map<Rank, Rank> SIMILAR_RANKS = Map.of(
      SUPERSECTION_BOTANY, SUPERSECTION_ZOOLOGY,
      SECTION_BOTANY, SECTION_ZOOLOGY,
      SUBSECTION_BOTANY, SUBSECTION_ZOOLOGY,
      // reverse
      SUPERSECTION_ZOOLOGY, SUPERSECTION_BOTANY,
      SECTION_ZOOLOGY, SECTION_BOTANY,
      SUBSECTION_ZOOLOGY, SUBSECTION_BOTANY
  );
}
