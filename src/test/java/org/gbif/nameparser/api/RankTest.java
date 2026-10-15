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

      if (r.lowerOrEqualsTo(Rank.SUPERKINGDOM) && r.higherOrEqualsTo(Rank.INFRAKINGDOM)) {
        assertEquals(Rank.KINGDOM, r.getMajorRank());
        assertTrue(r.isSuprageneric());
        assertFalse(r.isFamilyGroup());
        assertFalse(r.isGenusGroup());
      }
      if (r.lowerOrEqualsTo(Rank.SUPERPHYLUM) && r.higherOrEqualsTo(Rank.NANOPHYLUM)) {
        assertEquals(Rank.PHYLUM, r.getMajorRank());
        assertTrue(r.isSuprageneric());
        assertFalse(r.isFamilyGroup());
        assertFalse(r.isGenusGroup());
      }
      if (r.lowerOrEqualsTo(Rank.GIGACLASS) && r.higherOrEqualsTo(Rank.PARVCLASS)) {
        assertEquals(Rank.CLASS, r.getMajorRank());
        assertTrue(r.isSuprageneric());
        assertFalse(r.isFamilyGroup());
        assertFalse(r.isGenusGroup());
      }
      if (r.lowerOrEqualsTo(Rank.GIGAORDER) && r.higherOrEqualsTo(Rank.PARVORDER)) {
        assertEquals(Rank.ORDER, r.getMajorRank());
        assertTrue(r.isSuprageneric());
        assertFalse(r.isFamilyGroup());
        assertFalse(r.isGenusGroup());
      }
      if (r.lowerOrEqualsTo(Rank.GIGAFAMILY) && r.higherOrEqualsTo(Rank.INFRAFAMILY)) {
        assertEquals(Rank.FAMILY, r.getMajorRank());
        assertTrue(r.isSuprageneric());
        assertTrue(r.isFamilyGroup());
        assertFalse(r.isGenusGroup());
      }
      if (r.lowerOrEqualsTo(Rank.SUPERTRIBE) && r.higherOrEqualsTo(Rank.INFRATRIBE)) {
        assertEquals(Rank.TRIBE, r.getMajorRank());
        assertTrue(r.isSuprageneric());
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
    assertEquals(14, counter);
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
    assertTrue(Rank.SPECIES.higherThan(Rank.INFRASPECIFIC_NAME));
    assertTrue(Rank.SPECIES.higherThan(Rank.INFRASUBSPECIFIC_NAME));
    assertTrue(Rank.GENUS.higherThan(Rank.INFRAGENERIC_NAME));
    assertTrue(Rank.GENUS.higherThan(Rank.INFRASPECIFIC_NAME));

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
      if (r.getCode() != null) continue;
      if (r.name().startsWith("SUPER") || r.name().startsWith("SUB") || r.name().startsWith("INFRA")) continue;
      counter++;
      System.out.println(r);
    }
    assertEquals(8, counter);
  }

  @Test
  public void groups() {
    for (Rank r : Rank.values()) {
      RankGroup g = r.getGroup();
      assertEquals(r.name(), r.isSuprafamilial(), g == RankGroup.SUPRAFAMILIAL);
      assertEquals(r.name(), r.isFamilyGroup(), g == RankGroup.FAMILY);
      assertEquals(r.name(), r.isGenusGroup(), g == RankGroup.GENUS);
      assertEquals(r.name(), r == Rank.SPECIES_AGGREGATE || r == Rank.SPECIES, g == RankGroup.SPECIES);
      assertEquals(r.name(), r.isInfraspecific(), g == RankGroup.INFRASPECIFIC);
      assertEquals(r.name(), r == Rank.SUPRAGENERIC_NAME || r.otherOrUnranked(), g == null);
    }
    assertTrue(Rank.FALANX.isSuprafamilial());
    assertTrue(Rank.SUBSERIES_ZOOLOGY.isSuprafamilial());
    assertFalse(Rank.SUPERFAMILY.isSuprafamilial());
    assertFalse(Rank.SUPRAGENERIC_NAME.isSuprafamilial());
    assertFalse(Rank.UNRANKED.isSuprafamilial());
    assertEquals(RankGroup.FAMILY, Rank.SUPERFAMILY.getGroup());
    assertEquals(RankGroup.GENUS, Rank.SUPERGENUS.getGroup());
    assertEquals(RankGroup.GENUS, Rank.SECTION_BOTANY.getGroup());
    assertEquals(RankGroup.SPECIES, Rank.SPECIES_AGGREGATE.getGroup());
    assertEquals(RankGroup.INFRASPECIFIC, Rank.SUBSPECIES.getGroup());
  }

  @Test
  public void groupOrder() {
    RankGroup prev = null;
    for (Rank r : Rank.values()) {
      RankGroup g = r.getGroup();
      if (g != null) {
        if (prev != null) {
          assertTrue(r.name(), prev.compareTo(g) <= 0);
        }
        prev = g;
      }
    }
  }

  @Test
  public void sameGroup() {
    assertTrue(Rank.ORDER.sameGroup(Rank.CLASS));
    assertTrue(Rank.KINGDOM.sameGroup(Rank.DOMAIN));
    assertTrue(Rank.FAMILY.sameGroup(Rank.TRIBE));
    assertTrue(Rank.SUBGENUS.sameGroup(Rank.INFRAGENERIC_NAME));
    assertTrue(Rank.SUBSPECIES.sameGroup(Rank.VARIETY));

    assertFalse(Rank.ORDER.sameGroup(Rank.FAMILY));
    assertFalse(Rank.SUPERFAMILY.sameGroup(Rank.ORDER));
    assertFalse(Rank.SUPERGENUS.sameGroup(Rank.ORDER));
    assertFalse(Rank.GENUS.sameGroup(Rank.SPECIES_AGGREGATE));
    assertFalse(Rank.SPECIES.sameGroup(Rank.SUBSPECIES));
    // no group, not even with themselves
    assertFalse(Rank.SUPRAGENERIC_NAME.sameGroup(Rank.FAMILY));
    assertFalse(Rank.SUPRAGENERIC_NAME.sameGroup(Rank.SUPRAGENERIC_NAME));
    assertFalse(Rank.UNRANKED.sameGroup(Rank.UNRANKED));
    assertFalse(Rank.OTHER.sameGroup(Rank.OTHER));
    assertFalse(Rank.SPECIES.sameGroup(null));
  }

  @Test
  public void concrete() {
    for (Rank r : Rank.values()) {
      assertEquals(r.name(), !r.isUncomparable(), r.isConcrete());
    }
    assertTrue(Rank.SPECIES.isConcrete());
    assertFalse(Rank.INFRAGENERIC_NAME.isConcrete());
    assertFalse(Rank.UNRANKED.isConcrete());
  }

  @Test
  public void covers() {
    for (Rank r : Rank.values()) {
      assertTrue(r.name(), r.covers(r));
      assertFalse(r.name(), r.covers(null));
      if (r.isConcrete() || r.otherOrUnranked()) {
        for (Rank r2 : Rank.values()) {
          assertEquals(r + " covers " + r2, r == r2, r.covers(r2));
        }
      }
    }
    assertTrue(Rank.SUPRAGENERIC_NAME.covers(Rank.KINGDOM));
    assertTrue(Rank.SUPRAGENERIC_NAME.covers(Rank.FALANX));
    assertTrue(Rank.SUPRAGENERIC_NAME.covers(Rank.SUPERFAMILY));
    assertTrue(Rank.SUPRAGENERIC_NAME.covers(Rank.INFRATRIBE));
    assertFalse(Rank.SUPRAGENERIC_NAME.covers(Rank.SUPERGENUS));
    assertFalse(Rank.SUPRAGENERIC_NAME.covers(Rank.GENUS));
    assertFalse(Rank.SUPRAGENERIC_NAME.covers(Rank.UNRANKED));

    assertTrue(Rank.INFRAGENERIC_NAME.covers(Rank.SUBGENUS));
    assertTrue(Rank.INFRAGENERIC_NAME.covers(Rank.SECTION_BOTANY));
    assertFalse(Rank.INFRAGENERIC_NAME.covers(Rank.SUPERGENUS));
    assertFalse(Rank.INFRAGENERIC_NAME.covers(Rank.GENUS));
    assertFalse(Rank.INFRAGENERIC_NAME.covers(Rank.SPECIES_AGGREGATE));

    assertTrue(Rank.INFRASPECIFIC_NAME.covers(Rank.SUBSPECIES));
    assertTrue(Rank.INFRASPECIFIC_NAME.covers(Rank.STRAIN));
    assertTrue(Rank.INFRASPECIFIC_NAME.covers(Rank.INFRASUBSPECIFIC_NAME));
    assertFalse(Rank.INFRASPECIFIC_NAME.covers(Rank.SPECIES));

    assertTrue(Rank.INFRASUBSPECIFIC_NAME.covers(Rank.VARIETY));
    assertFalse(Rank.INFRASUBSPECIFIC_NAME.covers(Rank.SUBSPECIES));
    assertFalse(Rank.INFRASUBSPECIFIC_NAME.covers(Rank.INFRASPECIFIC_NAME));

    assertFalse(Rank.SUPRAGENERIC_NAME.covers(Rank.INFRAGENERIC_NAME));
    assertFalse(Rank.UNRANKED.covers(Rank.SPECIES));
    assertFalse(Rank.OTHER.covers(Rank.UNRANKED));
  }
}
