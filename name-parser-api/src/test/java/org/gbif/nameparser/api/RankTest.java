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
package org.gbif.nameparser.api;

import org.gbif.nameparser.util.RankUtils;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
public class RankTest {

  @Test
  public void testMarkerVsPlurals() {
    for (Rank r : Rank.values()) {
      if (r.getPlural() != null) {
        assertFalse(r + " plural swapped", r.getPlural().contains("."));
        if (r != Rank.LUSUS) {
          assertTrue(r + " plural swapped", r.getPlural().length() >= r.getMarker().length());
        }
      }
    }
  }

  @Test
  public void mayor() {
    for (Rank r : Rank.values()) {
      if (r.lowerOrEqualsTo(Rank.SUPERPHYLUM) && r.higherOrEqualsTo(Rank.NANOPHYLUM)) {
        assertEquals(Rank.PHYLUM, r.getMajorRank());
        assertFalse(r.isFamilyGroup());
        assertFalse(r.isGenusGroup());
      }
      if (r.lowerOrEqualsTo(Rank.GIGACLASS) && r.higherOrEqualsTo(Rank.PARVCLASS)) {
        assertEquals(Rank.CLASS, r.getMajorRank());
        assertFalse(r.isFamilyGroup());
        assertFalse(r.isGenusGroup());
      }
      if (r.lowerOrEqualsTo(Rank.GIGAORDER) && r.higherOrEqualsTo(Rank.PARVORDER)) {
        assertEquals(Rank.ORDER, r.getMajorRank());
        assertFalse(r.isFamilyGroup());
        assertFalse(r.isGenusGroup());
      }
      if (r.lowerOrEqualsTo(Rank.GIGAFAMILY) && r.higherOrEqualsTo(Rank.INFRAFAMILY)) {
        assertEquals(Rank.FAMILY, r.getMajorRank());
        assertTrue(r.isFamilyGroup());
        assertFalse(r.isGenusGroup());
      }
      if (r.lowerOrEqualsTo(Rank.SUPERTRIBE) && r.higherOrEqualsTo(Rank.INFRATRIBE)) {
        assertEquals(Rank.TRIBE, r.getMajorRank());
        assertTrue(r.isFamilyGroup());
        assertFalse(r.isGenusGroup());
      }
      if (r.lowerOrEqualsTo(Rank.SUPERGENUS) && r.higherOrEqualsTo(Rank.INFRAGENUS)) {
        assertEquals(Rank.GENUS, r.getMajorRank());
        assertFalse(r.isFamilyGroup());
        assertTrue(r.isGenusGroup());
      }
    }
  }

  @Test
  public void testIsInfraspecific() {
    assertFalse(Rank.SUPERFAMILY.isInfraspecific());
    assertFalse(Rank.KINGDOM.isInfraspecific());
    assertFalse(Rank.INFRAGENERIC_NAME.isInfraspecific());
    assertFalse(Rank.GENUS.isInfraspecific());
    assertFalse(Rank.SPECIES.isInfraspecific());
    assertTrue(Rank.SUBFORM.isInfraspecific());
    assertTrue(Rank.STRAIN.isInfraspecific());
    assertTrue(Rank.CULTIVAR.isInfraspecific());
    assertTrue(Rank.VARIETY.isInfraspecific());
    assertTrue(Rank.PATHOVAR.isInfraspecific());
    for (Rank r : Rank.values()) {
      if (r.isRestrictedToCode() == NomCode.BACTERIAL)
        assertTrue(r.isInfraspecific());
    }
  }
  
  @Test
  public void testGenusGroup() {
    for (Rank r : Rank.values()) {
      if (r == Rank.GENUS || r == Rank.SUPERGENUS || (r.isInfrageneric() && !r.isSpeciesOrBelow())) {
        assertTrue(r.name(), r.isGenusGroup());
      } else {
        assertFalse(r.name(), r.isGenusGroup());
      }
    }
  }

  @Test
  public void ambiguous() {
    int counter = 0;
    for (Rank r : Rank.values()) {
      if (r.hasAmbiguousMarker()) {
        counter++;
        Rank other = RankUtils.otherAmbiguousRank(r);
        assertNotEquals(other, r);
        assertNotNull(r.getCode());
        assertNotNull(other.getCode());
        assertNotEquals(other.getCode(), r.getCode());
      }
    }
    assertEquals(6, counter);
  }
  
  @Test
  @Ignore
  public void printPostgresEnum() {
    System.out.print("CREATE TYPE rank AS ENUM (");
    boolean first = true;
    for (Rank r : Rank.values()) {
      if (!first) {
        System.out.print(",");
      } else {
        first = false;
      }
      System.out.print("\n  '" + r.name().toLowerCase() + "'");
    }
    System.out.println(");");
  }
  
  @Test
  public void testIsLinnean() {
    assertTrue(Rank.KINGDOM.isLinnean());
    assertTrue(Rank.PHYLUM.isLinnean());
    assertTrue(Rank.CLASS.isLinnean());
    assertTrue(Rank.ORDER.isLinnean());
    assertTrue(Rank.FAMILY.isLinnean());
    assertTrue(Rank.GENUS.isLinnean());
    assertTrue(Rank.SPECIES.isLinnean());
    assertFalse(Rank.SUBSECTION_BOTANY.isLinnean());
    assertFalse(Rank.SUBGENUS.isLinnean());
    assertFalse(Rank.SUPERFAMILY.isLinnean());
    assertFalse(Rank.INFRAGENERIC_NAME.isLinnean());
    assertFalse(Rank.PATHOVAR.isLinnean());
    assertFalse(Rank.CHEMOFORM.isLinnean());
  }
  
  @Test
  public void testIsDwC() {
    assertTrue(Rank.DWC_RANKS.contains(Rank.KINGDOM));
    assertTrue(Rank.DWC_RANKS.contains(Rank.PHYLUM));
    assertTrue(Rank.DWC_RANKS.contains(Rank.CLASS));
    assertTrue(Rank.DWC_RANKS.contains(Rank.ORDER));
    assertTrue(Rank.DWC_RANKS.contains(Rank.FAMILY));
    assertTrue(Rank.DWC_RANKS.contains(Rank.GENUS));
    assertTrue(Rank.DWC_RANKS.contains(Rank.SUBGENUS));
    assertTrue(Rank.DWC_RANKS.contains(Rank.SPECIES));
  }

  @Test
  public void majorRanks() {
    for (Rank r : Rank.values()) {
      assertNotNull(r.getMajorRank());
    }
    assertEquals(Rank.FAMILY, Rank.SUPERFAMILY.getMajorRank());
    assertEquals(Rank.FAMILY, Rank.EPIFAMILY.getMajorRank());
    assertEquals(Rank.ORDER, Rank.MAGNORDER.getMajorRank());
    assertEquals(Rank.ORDER, Rank.PARVORDER.getMajorRank());
    assertEquals(Rank.INFRASPECIFIC_NAME, Rank.PATHOVAR.getMajorRank());
    assertEquals(Rank.INFRASPECIFIC_NAME, Rank.VARIETY.getMajorRank());
    assertEquals(Rank.INFRASPECIFIC_NAME, Rank.SUBFORM.getMajorRank());
    assertEquals(Rank.GENUS, Rank.INFRAGENERIC_NAME.getMajorRank());
    assertEquals(Rank.SUPRAGENERIC_NAME, Rank.SUPRAGENERIC_NAME.getMajorRank());

    assertEquals(Rank.PHYLUM, Rank.SUPERPHYLUM.getMajorRank());
    assertEquals(Rank.PHYLUM, Rank.PHYLUM.getMajorRank());
    assertEquals(Rank.PHYLUM, Rank.SUBPHYLUM.getMajorRank());
    assertEquals(Rank.PHYLUM, Rank.INFRAPHYLUM.getMajorRank());
    assertEquals(Rank.PHYLUM, Rank.PARVPHYLUM.getMajorRank());
    assertEquals(Rank.PHYLUM, Rank.MICROPHYLUM.getMajorRank());
    assertEquals(Rank.PHYLUM, Rank.NANOPHYLUM.getMajorRank());
  }

  @Test
  public void testIsSpeciesOrBelow() {
    assertFalse(Rank.SUPERFAMILY.isSpeciesOrBelow());
    assertFalse(Rank.KINGDOM.isSpeciesOrBelow());
    assertFalse(Rank.INFRAGENERIC_NAME.isSpeciesOrBelow());
    assertFalse(Rank.GENUS.isSpeciesOrBelow());
    assertTrue(Rank.SPECIES.isSpeciesOrBelow());
    assertTrue(Rank.SUBFORM.isSpeciesOrBelow());
    assertTrue(Rank.STRAIN.isSpeciesOrBelow());
    assertTrue(Rank.CULTIVAR.isSpeciesOrBelow());
    assertTrue(Rank.VARIETY.isSpeciesOrBelow());
    assertTrue(Rank.PATHOVAR.isSpeciesOrBelow());
    assertTrue(Rank.NATIO.isSpeciesOrBelow());
    assertTrue(Rank.GREX.isSpeciesOrBelow());
  }
  
  @Test
  public void testIsLegacy() {
    assertTrue(Rank.NATIO.isLegacy());
    assertTrue(Rank.PROLES.isLegacy());
  }
  
  @Test
  public void testIsInfrageneric() {
    assertFalse(Rank.SUPERFAMILY.isInfrageneric());
    assertFalse(Rank.FAMILY.isInfrageneric());
    assertFalse(Rank.SUPRAGENERIC_NAME.isInfrageneric());
    assertFalse(Rank.SUBFAMILY.isInfrageneric());
    assertFalse(Rank.TRIBE.isInfrageneric());
    assertFalse(Rank.GENUS.isInfrageneric());
    
    assertTrue(Rank.SUBGENUS.isInfrageneric());
    assertTrue(Rank.INFRAGENERIC_NAME.isInfrageneric());
    assertTrue(Rank.SPECIES.isInfrageneric());
    assertTrue(Rank.SUBFORM.isInfrageneric());
    assertTrue(Rank.STRAIN.isInfrageneric());
    assertTrue(Rank.CULTIVAR.isInfrageneric());
    assertTrue(Rank.VARIETY.isInfrageneric());
  }
  
  @Test
  public void testIsSuprageneric() {
    assertTrue(Rank.SUPERFAMILY.isSuprageneric());
    assertTrue(Rank.KINGDOM.isSuprageneric());
    assertTrue(Rank.PHYLUM.isSuprageneric());
    assertTrue(Rank.SUPERFAMILY.isSuprageneric());
    assertTrue(Rank.SUPRAGENERIC_NAME.isSuprageneric());
    assertTrue(Rank.TRIBE.isSuprageneric());
    assertTrue(Rank.SUBFAMILY.isSuprageneric());
    assertFalse(Rank.INFRAGENERIC_NAME.isSuprageneric());
    assertFalse(Rank.GENUS.isSuprageneric());
    assertFalse(Rank.SPECIES.isSuprageneric());
    assertFalse(Rank.SUBFORM.isSuprageneric());
    assertFalse(Rank.STRAIN.isSuprageneric());
    assertFalse(Rank.CULTIVAR.isSuprageneric());
    assertFalse(Rank.VARIETY.isSuprageneric());
  }
  
  @Test
  public void testIsUncomparable() {
    assertFalse(Rank.KINGDOM.isUncomparable());
    assertFalse(Rank.PHYLUM.isUncomparable());
    assertFalse(Rank.CLASS.isUncomparable());
    assertFalse(Rank.ORDER.isUncomparable());
    assertFalse(Rank.FAMILY.isUncomparable());
    assertFalse(Rank.GENUS.isUncomparable());
    assertFalse(Rank.SPECIES.isUncomparable());
    assertFalse(Rank.SUBSECTION_BOTANY.isUncomparable());
    assertFalse(Rank.SUBGENUS.isUncomparable());
    assertFalse(Rank.SUPERFAMILY.isUncomparable());
    assertTrue(Rank.INFRAGENERIC_NAME.isUncomparable());
  }
  
  @Test
  public void testHigher() {
    assertFalse(Rank.SUPERFAMILY.higherThan(Rank.KINGDOM));
    assertFalse(Rank.SUPERFAMILY.higherThan(Rank.KINGDOM));
    assertFalse(Rank.SPECIES.higherThan(Rank.SUBGENUS));
    assertFalse(Rank.SPECIES.higherThan(Rank.SPECIES));
    assertFalse(Rank.SPECIES.higherThan(Rank.OTHER));
    assertFalse(Rank.SPECIES.higherThan(Rank.UNRANKED));

    assertTrue(Rank.INFRASPECIFIC_NAME.higherThan(Rank.VARIETY));
    assertTrue(Rank.SUPERFAMILY.higherThan(Rank.FAMILY));
    assertTrue(Rank.SPECIES.higherThan(Rank.VARIETY));
    assertTrue(Rank.GENUS.higherThan(Rank.INFRAGENERIC_NAME));
    
    int expectedHigher = Rank.DWC_RANKS.size() - 1;
    for (Rank r : Rank.DWC_RANKS) {
      int higherCount = 0;
      for (Rank r2 : Rank.DWC_RANKS) {
        if (r.higherThan(r2)) {
          higherCount++;
        }
      }
      assertEquals(expectedHigher, higherCount);
      expectedHigher--;
    }
    
    // questionable
    assertFalse(Rank.UNRANKED.higherThan(Rank.VARIETY));
    assertFalse(Rank.UNRANKED.lowerThan(Rank.VARIETY));
  }
  
  @Test
  public void testHigherEquals() {
    assertFalse(Rank.SUPERFAMILY.higherOrEqualsTo(Rank.KINGDOM));
    assertFalse(Rank.SUPERFAMILY.higherOrEqualsTo(Rank.KINGDOM));
    assertFalse(Rank.SPECIES.higherOrEqualsTo(Rank.SUBGENUS));
  
    assertTrue(Rank.SPECIES.higherOrEqualsTo(Rank.SPECIES));
    assertTrue(Rank.INFRASPECIFIC_NAME.higherOrEqualsTo(Rank.VARIETY));
    assertTrue(Rank.SUPERFAMILY.higherOrEqualsTo(Rank.FAMILY));
    assertTrue(Rank.SPECIES.higherOrEqualsTo(Rank.VARIETY));
    assertTrue(Rank.GENUS.higherOrEqualsTo(Rank.INFRAGENERIC_NAME));
    
    int expectedHigher = Rank.DWC_RANKS.size();
    for (Rank r : Rank.DWC_RANKS) {
      int higherCount = 0;
      for (Rank r2 : Rank.DWC_RANKS) {
        if (r.higherOrEqualsTo(r2)) {
          higherCount++;
        }
      }
      assertEquals(expectedHigher, higherCount);
      expectedHigher--;
    }
  }
  
  @Test
  public void testNotUnranked() {
    assertFalse(Rank.UNRANKED.notOtherOrUnranked());
    assertFalse(Rank.OTHER.notOtherOrUnranked());
    
    assertTrue(Rank.INFRAGENERIC_NAME.notOtherOrUnranked());
    assertTrue(Rank.SPECIES.notOtherOrUnranked());
    assertTrue(Rank.GENUS.notOtherOrUnranked());
    assertTrue(Rank.SUBGENUS.notOtherOrUnranked());
    assertTrue(Rank.DOMAIN.notOtherOrUnranked());
    assertTrue(Rank.INFRASUBSPECIFIC_NAME.notOtherOrUnranked());
    assertTrue(Rank.STRAIN.notOtherOrUnranked());
  }

  @Test
  public void testCodeSpecific() {
    int counter = 0;
    for (Rank r : Rank.values()) {
      if (r.isLinnean() || r.isUncomparable()) continue;
      if (r.isRestrictedToCode() != null) continue;
      if (r.name().startsWith("SUPER") || r.name().startsWith("SUB") || r.name().startsWith("INFRA")) continue;
      counter++;
      System.out.println(r);
    }
    assertEquals(7, counter);
  }
}