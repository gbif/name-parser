package org.gbif.nameparser.api;


import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;

public class ParsedNameTest {

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
    pn.setRank(Rank.SPECIES);
    assertTrue(pn.isIndetermined());

    pn.setRank(null);
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
