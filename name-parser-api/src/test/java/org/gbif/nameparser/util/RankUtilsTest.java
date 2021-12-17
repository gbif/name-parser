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

import static org.junit.Assert.*;

/**
 *
 */
public class RankUtilsTest {
  
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
