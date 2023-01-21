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

import org.gbif.nameparser.api.NomCode;
import org.gbif.nameparser.api.ParsedName;
import org.gbif.nameparser.api.Rank;

import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 *
 */
public class RankUtilsTest {


  @Test
  public void testNextLowerLinneanRank() throws Exception {
    assertEquals(Rank.SPECIES, RankUtils.nextLowerLinneanRank(Rank.GENUS));
    assertEquals(Rank.GENUS, RankUtils.nextLowerLinneanRank(Rank.SUBFAMILY));
    assertEquals(Rank.SPECIES, RankUtils.nextLowerLinneanRank(Rank.SUBGENUS));
    assertEquals(Rank.PHYLUM, RankUtils.nextLowerLinneanRank(Rank.KINGDOM));
    assertEquals(Rank.KINGDOM, RankUtils.nextLowerLinneanRank(Rank.DOMAIN));
    assertEquals(Rank.SPECIES, RankUtils.nextLowerLinneanRank(Rank.INFRAGENERIC_NAME));
    assertEquals(null, RankUtils.nextLowerLinneanRank(Rank.INFRASUBSPECIFIC_NAME));
    assertEquals(null, RankUtils.nextLowerLinneanRank(Rank.VARIETY));
  }

  @Test
  public void testNextHigherLinneanRank() throws Exception {
    assertEquals(Rank.FAMILY, RankUtils.nextHigherLinneanRank(Rank.GENUS));
    assertEquals(Rank.FAMILY, RankUtils.nextHigherLinneanRank(Rank.SUBFAMILY));
    assertEquals(Rank.GENUS, RankUtils.nextHigherLinneanRank(Rank.SUBGENUS));
    assertEquals(null, RankUtils.nextHigherLinneanRank(Rank.KINGDOM));
    assertEquals(null, RankUtils.nextHigherLinneanRank(Rank.DOMAIN));
    assertEquals(Rank.SPECIES, RankUtils.nextHigherLinneanRank(Rank.VARIETY));
  }

  @Test
  public void minRank() throws Exception {
    List<Rank> ranks = RankUtils.minRanks(Rank.GENUS);
    assertTrue(ranks.contains(Rank.GENUS));
    assertTrue(ranks.contains(Rank.FAMILY));
    assertTrue(ranks.contains(Rank.KINGDOM));
    assertTrue(ranks.contains(Rank.INFRACOHORT));
    assertFalse(ranks.contains(Rank.SUBGENUS));
    assertFalse(ranks.contains(Rank.SPECIES));
    assertEquals(63, ranks.size());
  }

  @Test
  public void maxRank() throws Exception {
    List<Rank> ranks = RankUtils.maxRanks(Rank.GENUS);
    assertTrue(ranks.contains(Rank.GENUS));
    assertFalse(ranks.contains(Rank.FAMILY));
    assertFalse(ranks.contains(Rank.KINGDOM));
    assertFalse(ranks.contains(Rank.INFRACOHORT));
    assertTrue(ranks.contains(Rank.SUBGENUS));
    assertTrue(ranks.contains(Rank.SPECIES));
    assertTrue(ranks.contains(Rank.SUBSPECIES));
    assertTrue(ranks.contains(Rank.NATIO));
    assertEquals(43, ranks.size());
  }

  @Test
  public void between() throws Exception {
    Set<Rank> ranks = RankUtils.between(Rank.GENUS, Rank.FAMILY, true);
    assertTrue(ranks.contains(Rank.GENUS));
    assertTrue(ranks.contains(Rank.FAMILY));
    assertTrue(ranks.contains(Rank.SUBFAMILY));
    assertFalse(ranks.contains(Rank.SUPERFAMILY));
    assertEquals(10, ranks.size());

    ranks = RankUtils.between(Rank.GENUS, Rank.FAMILY, false);
    assertFalse(ranks.contains(Rank.GENUS));
    assertFalse(ranks.contains(Rank.FAMILY));
    assertTrue(ranks.contains(Rank.SUBFAMILY));
    assertFalse(ranks.contains(Rank.SUPERFAMILY));
    assertEquals(8, ranks.size());
  }

  @Test
  public void testFamilyGroup() {
    assertTrue(RankUtils.RANK_MARKER_MAP_FAMILY_GROUP.containsValue(Rank.SUBTRIBE));
    assertTrue(RankUtils.RANK_MARKER_MAP_FAMILY_GROUP.containsValue(Rank.TRIBE));
    assertTrue(RankUtils.RANK_MARKER_MAP_FAMILY_GROUP.containsValue(Rank.INFRAFAMILY));
    assertTrue(RankUtils.RANK_MARKER_MAP_FAMILY_GROUP.containsValue(Rank.SUPERFAMILY));

    assertFalse(RankUtils.RANK_MARKER_MAP_FAMILY_GROUP.containsValue(Rank.ORDER));
    assertFalse(RankUtils.RANK_MARKER_MAP_FAMILY_GROUP.containsValue(Rank.SECTION));
    assertFalse(RankUtils.RANK_MARKER_MAP_FAMILY_GROUP.containsValue(Rank.GENUS));
  }
  
  private void assertInferred(String uninomial, NomCode code, Rank rank) {
    ParsedName pn = new ParsedName();
    pn.setUninomial(uninomial);
    pn.setCode(code);
    assertEquals(rank, RankUtils.inferRank(pn));
  }
  
  @Test
  public void testInferRank() {
    assertInferred("Asteraceae", NomCode.BOTANICAL, Rank.FAMILY);
    assertInferred("Asteraceae", null, Rank.FAMILY);
    assertInferred("Asteraceae", NomCode.ZOOLOGICAL, Rank.UNRANKED);
    assertInferred("Magnoliophyta", NomCode.BOTANICAL, Rank.PHYLUM);
    assertInferred("Fabales", NomCode.BOTANICAL, Rank.ORDER);
    assertInferred("Hominidae", NomCode.ZOOLOGICAL, Rank.FAMILY);
    assertInferred("Drosophilinae", NomCode.ZOOLOGICAL, Rank.SUBFAMILY);
    assertInferred("Agaricomycetes", NomCode.BOTANICAL, Rank.CLASS);
    assertInferred("Agaricomycetes", null, Rank.CLASS);
    assertInferred("Woodsioideae", null, Rank.SUBFAMILY);
    assertInferred("Antrophyoideae", null, Rank.SUBFAMILY);
    assertInferred("Protowoodsioideae", null, Rank.SUBFAMILY);
  
    assertInferred("Negarnaviricota", NomCode.VIRUS, Rank.PHYLUM);
    assertInferred("Negarnaviricota", null, Rank.PHYLUM);
    assertInferred("Picornavirales", null, Rank.ORDER);
    assertInferred("Riboviria", null, Rank.REALM);
  
    assertEquals(Rank.SPECIES, RankUtils.inferRank(build("Abies","Abies", "alba")));
    assertEquals(Rank.SPECIES, RankUtils.inferRank(build("Abies", null, "alba")));
    assertEquals(Rank.INFRAGENERIC_NAME, RankUtils.inferRank(build(null, "Abies", null)));
    assertEquals(Rank.INFRAGENERIC_NAME, RankUtils.inferRank(build("", "Abies", null)));
    assertEquals(Rank.SUBFAMILY, RankUtils.inferRank(build("Neurolaenodinae", null, null, NomCode.ZOOLOGICAL)));
    // should not be able to infer the correct family
    assertEquals(Rank.UNRANKED, RankUtils.inferRank(build("Compositae", null, null, NomCode.BOTANICAL)));
  }

  private static ParsedName build(String genus, String infragen, String spec) {
    return build(genus, infragen, spec, null);
  }

  private static ParsedName build(String genus, String infragen, String spec, NomCode code) {
    ParsedName pn = new ParsedName();
    pn.setUninomial(genus);
    pn.setGenus(genus);
    pn.setInfragenericEpithet(infragen);
    pn.setSpecificEpithet(spec);
    pn.setCode(code);
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
  public void testRankMarkers() {
    assertEquals(Rank.SUBSPECIES, RankUtils.inferRank("agamossp."));
    assertEquals(Rank.SUBSPECIES, RankUtils.inferRank("nothossp."));
    for (Rank r : Rank.values()) {
      if (r.notOtherOrUnranked() && r != Rank.CULTIVAR_GROUP) {
        assertEquals(r, RankUtils.inferRank(r.getMarker()));
        assertEquals(r, RankUtils.inferRank("notho"+r.getMarker()));
      }
    }
  }
}
