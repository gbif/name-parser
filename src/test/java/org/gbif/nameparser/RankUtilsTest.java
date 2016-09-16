package org.gbif.nameparser;

import org.gbif.api.vocabulary.Rank;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

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
  public void testInferRank() {
    for (Map.Entry<String, Rank> stringRankEntry : NAMES.entrySet()) {
      assertEquals(stringRankEntry.getValue(), RankUtils.inferRank(stringRankEntry.getKey(), null, null, null, null));
    }

    assertEquals(Rank.SPECIES, RankUtils.inferRank("Abies", "Abies", "alba", null, null));
    assertEquals(Rank.SPECIES, RankUtils.inferRank("Abies", null, "alba", null, null));
    assertEquals(Rank.INFRAGENERIC_NAME, RankUtils.inferRank(null, "Abies", null, null, null));
    assertEquals(Rank.INFRAGENERIC_NAME, RankUtils.inferRank("", "Abies", null, null, null));
    assertEquals(Rank.VARIETY, RankUtils.inferRank(null, "Abies", "alba", "var.", "alpina"));
    assertEquals(Rank.PATHOVAR, RankUtils.inferRank(null, "Pseudomonas", "syringae ", "pv.", "aceris"));
    assertEquals(Rank.SUBSPECIES, RankUtils.inferRank(null, "Abies", "alba", "ssp.", null));
    assertEquals(Rank.SPECIES, RankUtils.inferRank(null, "Abies", null, "spec.", null));
    assertEquals(Rank.SUPRAGENERIC_NAME, RankUtils.inferRank("Neurolaenodinae", null, null, "ib.", null));
    assertEquals(Rank.SUPRAGENERIC_NAME, RankUtils.inferRank("Neurolaenodinae", null, null, "supersubtrib.", null));
    assertEquals(Rank.BIOVAR, RankUtils.inferRank(null, "Pseudomonas", "syringae ", "bv.", "aceris"));
    assertEquals(Rank.BIOVAR, RankUtils.inferRank(null, "Thymus", "vulgaris", "biovar", "geraniol"));
    assertEquals(Rank.CHEMOFORM, RankUtils.inferRank(null, "Thymus", "vulgaris", "ct.", "geraniol"));
    assertEquals(Rank.CHEMOFORM, RankUtils.inferRank(null, "Thymus", "vulgaris", "chemoform", "geraniol"));
    assertEquals(Rank.CHEMOVAR, RankUtils.inferRank(null, "Thymus", "vulgaris", "chemovar", "geraniol"));
    assertEquals(Rank.SEROVAR, RankUtils.inferRank(null, "Thymus", "vulgaris", "serovar", "geraniol"));

    // should not be able to infer the correct family
    assertEquals(Rank.UNRANKED, RankUtils.inferRank("Compositae", null, null, null, null));
  }

  @Test
  public void testInferRank2() {
    for (Rank r : Rank.values()) {
      if (r.getMarker() != null) {
        assertEquals(r.getMarker(), r, RankUtils.inferRank(r.getMarker()));
        assertEquals(r.getMarker(), r, RankUtils.inferRank("Gagga", null, null, r.getMarker(), null));
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
      if (r.notOtherOrUnknown() && r != Rank.CULTIVAR_GROUP) {
        assertEquals(r, RankUtils.inferRank(r.getMarker()));
        assertEquals(r, RankUtils.inferRank("notho"+r.getMarker()));
      }
    }
  }
}
