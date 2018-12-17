package org.gbif.nameparser;

import org.gbif.nameparser.api.ParsedName;
import org.gbif.nameparser.api.Rank;
import org.gbif.nameparser.util.RankUtils;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class RankUtilsTest {
  private static final Map<String, Rank> NAMES = new HashMap<String, Rank>();

  static {
    NAMES.put("Asteraceae", Rank.FAMILY);
    NAMES.put("Magnoliophyta", Rank.PHYLUM);
    NAMES.put("Fabales", Rank.ORDER);
    NAMES.put("Hominidae", Rank.FAMILY);
    NAMES.put("Drosophilinae", Rank.SUBFAMILY);
    NAMES.put("Agaricomycetes", Rank.CLASS);
  }

  @Test
  public void testFamilyGroup() {
    assertTrue(RankUtils.RANK_MARKER_MAP_FAMILY_GROUP.values().contains(Rank.SUBTRIBE));
    assertTrue(RankUtils.RANK_MARKER_MAP_FAMILY_GROUP.values().contains(Rank.TRIBE));
    assertTrue(RankUtils.RANK_MARKER_MAP_FAMILY_GROUP.values().contains(Rank.INFRAFAMILY));
    assertTrue(RankUtils.RANK_MARKER_MAP_FAMILY_GROUP.values().contains(Rank.SUPERFAMILY));

    assertFalse(RankUtils.RANK_MARKER_MAP_FAMILY_GROUP.values().contains(Rank.ORDER));
    assertFalse(RankUtils.RANK_MARKER_MAP_FAMILY_GROUP.values().contains(Rank.SECTION));
    assertFalse(RankUtils.RANK_MARKER_MAP_FAMILY_GROUP.values().contains(Rank.GENUS));
  }

  @Test
  public void testInferRank() {
    for (Map.Entry<String, Rank> stringRankEntry : NAMES.entrySet()) {
      ParsedName pn = new ParsedName();
      pn.setUninomial(stringRankEntry.getKey());
      assertEquals(stringRankEntry.getValue(), RankUtils.inferRank(pn));
    }

    assertEquals(Rank.SPECIES, RankUtils.inferRank(build("Abies","Abies", "alba")));
    assertEquals(Rank.SPECIES, RankUtils.inferRank(build("Abies", null, "alba")));
    assertEquals(Rank.INFRAGENERIC_NAME, RankUtils.inferRank(build(null, "Abies", null)));
    assertEquals(Rank.INFRAGENERIC_NAME, RankUtils.inferRank(build("", "Abies", null)));
    assertEquals(Rank.SUBFAMILY, RankUtils.inferRank(build("Neurolaenodinae", null, null)));
    // should not be able to infer the correct family
    assertEquals(Rank.UNRANKED, RankUtils.inferRank(build("Compositae", null, null)));
  }

  private static ParsedName build(String genus, String infragen, String spec) {
    ParsedName pn = new ParsedName();
    pn.setUninomial(genus);
    pn.setGenus(genus);
    pn.setInfragenericEpithet(infragen);
    pn.setSpecificEpithet(spec);
    return pn;
  }
  @Test
  public void testInferRank2() {
    for (Rank r : Rank.values()) {
      if (r.getMarker() != null) {
        assertEquals(r.getMarker(), r, RankUtils.inferRank(r.getMarker()));
      }
    }
  }


  @Test
  public void testSuffixMap() {
   
    for (Map.Entry<String, Rank> stringRankEntry : NAMES.entrySet()) {
      Rank r = null;
      for (String suffix : RankUtils.SUFFICES_RANK_MAP.keySet()) {
        if (stringRankEntry.getKey().endsWith(suffix)) {
          r = RankUtils.SUFFICES_RANK_MAP.get(suffix);
          break;
        }
      }
      assertEquals(stringRankEntry.getValue(), r);
    }
  }


  @Test
  public void testRankMarkers() {
    for (Rank r : Rank.values()) {
      if (r.notOtherOrUnranked() && r != Rank.CULTIVAR_GROUP) {
        assertEquals(r, RankUtils.inferRank(r.getMarker()));
        assertEquals(r, RankUtils.inferRank("notho"+r.getMarker()));
      }
    }
  }
}
