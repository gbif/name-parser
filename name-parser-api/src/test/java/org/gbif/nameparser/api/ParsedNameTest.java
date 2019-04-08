package org.gbif.nameparser.api;


import org.junit.Test;

import static org.junit.Assert.*;

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
    
    
    pn = new ParsedName();
    pn.setGenus("Abies");
    pn.setInfraspecificEpithet("alpina");
    assertFalse(pn.isIndetermined());
    
    pn.setSpecificEpithet("alba");
    assertFalse(pn.isIndetermined());
    
    
    pn = new ParsedName();
    pn.setUninomial("Trematostoma");
    assertFalse(pn.isIndetermined());
    pn.setRank(Rank.INFRAGENERIC_NAME);
    assertFalse(pn.isIndetermined());
    pn.setRank(Rank.SUBGENUS);
    assertFalse(pn.isIndetermined());
    
    pn.setGenus(pn.getUninomial());
    pn.setUninomial(null);
    assertTrue(pn.isIndetermined());
    
    pn.setInfragenericEpithet("Trematostoma");
    assertFalse(pn.isIndetermined());
  }
  
  @Test
  public void testIncomplete() throws Exception {
    ParsedName pn = new ParsedName();
    assertFalse(pn.isIncomplete());
    
    pn.setGenus("Abies");
    assertFalse(pn.isIncomplete());
    
    pn.setSpecificEpithet("vulgaris");
    assertFalse(pn.isIncomplete());
    
    pn.setGenus(null);
    assertTrue(pn.isIncomplete());
  }
}
