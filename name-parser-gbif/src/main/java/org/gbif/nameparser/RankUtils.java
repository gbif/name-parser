package org.gbif.nameparser;

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.NomenclaturalCode;
import org.gbif.api.vocabulary.Rank;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;

import static org.gbif.api.vocabulary.Rank.ABERRATION;
import static org.gbif.api.vocabulary.Rank.BIOVAR;
import static org.gbif.api.vocabulary.Rank.CHEMOFORM;
import static org.gbif.api.vocabulary.Rank.CLASS;
import static org.gbif.api.vocabulary.Rank.CONVARIETY;
import static org.gbif.api.vocabulary.Rank.CULTIVAR;
import static org.gbif.api.vocabulary.Rank.FAMILY;
import static org.gbif.api.vocabulary.Rank.FORM;
import static org.gbif.api.vocabulary.Rank.FORMA_SPECIALIS;
import static org.gbif.api.vocabulary.Rank.GENUS;
import static org.gbif.api.vocabulary.Rank.GREX;
import static org.gbif.api.vocabulary.Rank.INFRAGENERIC_NAME;
import static org.gbif.api.vocabulary.Rank.INFRASPECIFIC_NAME;
import static org.gbif.api.vocabulary.Rank.MORPH;
import static org.gbif.api.vocabulary.Rank.NATIO;
import static org.gbif.api.vocabulary.Rank.ORDER;
import static org.gbif.api.vocabulary.Rank.PATHOVAR;
import static org.gbif.api.vocabulary.Rank.PHYLUM;
import static org.gbif.api.vocabulary.Rank.PROLES;
import static org.gbif.api.vocabulary.Rank.SECTION;
import static org.gbif.api.vocabulary.Rank.SERIES;
import static org.gbif.api.vocabulary.Rank.SPECIES;
import static org.gbif.api.vocabulary.Rank.SPECIES_AGGREGATE;
import static org.gbif.api.vocabulary.Rank.STRAIN;
import static org.gbif.api.vocabulary.Rank.SUBCLASS;
import static org.gbif.api.vocabulary.Rank.SUBFAMILY;
import static org.gbif.api.vocabulary.Rank.SUBFORM;
import static org.gbif.api.vocabulary.Rank.SUBGENUS;
import static org.gbif.api.vocabulary.Rank.SUBORDER;
import static org.gbif.api.vocabulary.Rank.SUBPHYLUM;
import static org.gbif.api.vocabulary.Rank.SUBSECTION;
import static org.gbif.api.vocabulary.Rank.SUBSERIES;
import static org.gbif.api.vocabulary.Rank.SUBSPECIES;
import static org.gbif.api.vocabulary.Rank.SUBTRIBE;
import static org.gbif.api.vocabulary.Rank.SUBVARIETY;
import static org.gbif.api.vocabulary.Rank.SUPERFAMILY;
import static org.gbif.api.vocabulary.Rank.SUPERORDER;
import static org.gbif.api.vocabulary.Rank.SUPRAGENERIC_NAME;
import static org.gbif.api.vocabulary.Rank.TRIBE;
import static org.gbif.api.vocabulary.Rank.UNRANKED;
import static org.gbif.api.vocabulary.Rank.VARIETY;

/**
 *
 */
public class RankUtils {
  /**
   * Matches all dots ("."), underscores ("_") and dashes ("-").
   */
  private static final Pattern NORMALIZE_RANK_MARKER = Pattern.compile("(?:[._ -]+|\\bnotho)");

  protected static final List<Rank> INFRASUBSPECIFIC_MICROBIAL_RANKS;
  static {
    List<Rank> microbialRanks = Lists.newArrayList();
    for (Rank r : Rank.values()) {
      if (r.isRestrictedToCode()== NomenclaturalCode.BACTERIAL && r.isInfraspecific()) {
        microbialRanks.add(r);
      }
    }
    INFRASUBSPECIFIC_MICROBIAL_RANKS = ImmutableList.copyOf(microbialRanks);
  }

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
   * An immutable map of name suffices to corresponding ranks across all kingdoms.
   * To minimize wrong matches this map is sorted by suffix length with the first suffices being the longest and
   * therefore most accurate matches.
   * See http://www.nhm.ac.uk/hosted-sites/iczn/code/index.jsp?nfv=true&article=29
   */
  protected static final SortedMap<String, Rank> SUFFICES_RANK_MAP =
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
  public static Rank inferRank(@Nullable String rankMarker) {
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
   * @return the inferred rank or null if it cant be found.
   */

  public static Rank inferRank(ParsedName pn) {
    Rank inferred = inferRank(pn.getGenusOrAbove(), pn.getInfraGeneric(), pn.getSpecificEpithet(), null, pn.getInfraSpecificEpithet());
    if (inferred != null && inferred.notOtherOrUnknown()) {
      return inferred;
    }
    return null;
  }

  public static Rank inferRank(
      @Nullable String genusOrAbove,
      @Nullable String infraGeneric,
      @Nullable String specificEpithet,
      @Nullable String rankMarker,
      @Nullable String infraSpecificEpithet
  ) {
    // first try rank marker
    Rank markerRank = inferRank(rankMarker);
    if (markerRank != null) {
      return markerRank;
    }

    // default if we cant find anything else
    Rank rank = UNRANKED;
    // detect rank based on parsed name
    if (infraSpecificEpithet != null) {
      // some infraspecific name
      rank = INFRASPECIFIC_NAME;
    } else if (specificEpithet != null) {
      // a species
      rank = SPECIES;
    } else if (infraGeneric != null) {
      // some infrageneric name
      rank = INFRAGENERIC_NAME;
    } else if (genusOrAbove != null) {
      // a suprageneric name, check suffices
      for (String suffix : SUFFICES_RANK_MAP.keySet()) {
        if (genusOrAbove.endsWith(suffix)) {
          rank = SUFFICES_RANK_MAP.get(suffix);
          break;
        }
      }
    }
    return rank;
  }

}
