package org.gbif.nameparser.api;


import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;

public class ParsedNameTest {
  
  @Test
  public void isConsistent() throws Exception {
    ParsedName n = new ParsedName();
    assertTrue(n.isConsistent());

    n.setUninomial("Asteraceae");
    n.setRank(Rank.FAMILY);
    assertTrue(n.isConsistent());
    for (Rank r : Rank.values()) {
      if (r.isSuprageneric()) {
        n.setRank(r);
        assertTrue(n.isConsistent());
      }
    }

    n.setRank(Rank.GENUS);
    assertTrue(n.isConsistent());

    n.setUninomial("Abies");
    assertTrue(n.isConsistent());

    n.getCombinationAuthorship().getAuthors().add("Mill.");
    assertTrue(n.isConsistent());

    n.setRank(Rank.SPECIES);
    assertFalse(n.isConsistent());

    n.setInfragenericEpithet("Pinoideae");
    assertFalse(n.isConsistent());

    n.setRank(Rank.SUBGENUS);
    // should we not also check if scientificName property makes sense???
    assertTrue(n.isConsistent());

    n.setGenus("Abies");
    assertTrue(n.isConsistent());

    n.setSpecificEpithet("alba");
    assertFalse(n.isConsistent());

    n.setRank(Rank.SPECIES);
    assertFalse(n.isConsistent());

    n.setInfragenericEpithet(null);
    assertTrue(n.isConsistent());

    n.setRank(Rank.VARIETY);
    assertFalse(n.isConsistent());

    n.setInfraspecificEpithet("alpina");
    assertTrue(n.isConsistent());

    n.setRank(Rank.SPECIES);
    assertFalse(n.isConsistent());

    n.setRank(Rank.UNRANKED);
    assertTrue(n.isConsistent());

    n.setSpecificEpithet(null);
    assertFalse(n.isConsistent());
  }

  @Test
  public void unrankedDefault() throws Exception {
    ParsedName n = new ParsedName();
    assertEquals(Rank.UNRANKED, n.getRank());

    for (Rank r : Rank.values()) {
      n.setRank(r);
      assertEquals(r, n.getRank());
    }

    n.setRank(null);
    assertEquals(Rank.UNRANKED, n.getRank());
  }

  @Test
  public void testTerminalEpithet() throws Exception {
    ParsedName pn = new ParsedName();
    pn.setUninomial("Abies");
    assertNull(pn.getTerminalEpithet());

    pn.setInfraspecificEpithet("abieta");
    assertEquals("abieta", pn.getTerminalEpithet());

    pn.setSpecificEpithet("vulgaris");
    assertEquals("abieta", pn.getTerminalEpithet());

    pn.setInfraspecificEpithet(null);
    assertEquals("vulgaris", pn.getTerminalEpithet());

    // indet.
    pn.setGenus(null);
    assertEquals("vulgaris", pn.getTerminalEpithet());
  }

  @Test
  public void testIndet() throws Exception {
    ParsedName pn = new ParsedName();
    pn.setGenus("Abies");
    assertFalse(pn.isIndetermined());

    pn.setRank(Rank.SPECIES);
    assertTrue(pn.isIndetermined());

    pn.setSpecificEpithet("vulgaris");
    assertFalse(pn.isIndetermined());

    pn.setRank(Rank.SUBSPECIES);
    assertTrue(pn.isIndetermined());

    pn.setInfraspecificEpithet("kingkong");
    assertFalse(pn.isIndetermined());

    for (Rank r : Rank.values()) {
      if (r.isInfraspecific()) {
        assertFalse(r.toString(), pn.isIndetermined());
      }
    }

    pn.setInfraspecificEpithet(null);
    for (Rank r : Rank.values()) {
      if (r.isInfraspecific()) {
        assertTrue(r.toString(), pn.isIndetermined());
      }
    }

    pn.setRank(Rank.SUBGENUS);
    pn.setSpecificEpithet(null);
    assertTrue(pn.isIndetermined());

    pn.setInfragenericEpithet("Mysubgenus");
    assertFalse(pn.isIndetermined());
  }

}
