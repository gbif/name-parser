package org.gbif.nameparser.api;


import org.junit.Test;

import java.util.Collections;
import java.util.List;

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
  public void testListEpithet() throws Exception {
    ParsedName pn = new ParsedName();
    pn.setUninomial("Abies");
    assertEquals(Collections.EMPTY_LIST, pn.listEpithets());
    
    pn.setInfraspecificEpithet("abieta");
    assertEquals(List.of("abieta"), pn.listEpithets());

    pn.setSpecificEpithet("vulgaris");
    assertEquals(List.of("vulgaris", "abieta"), pn.listEpithets());
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
  public void testEqualsIncludesAuthorship() {
    ParsedName a = new ParsedName();
    a.setGenus("Abies");
    a.setSpecificEpithet("alba");

    ParsedName b = new ParsedName();
    b.setGenus("Abies");
    b.setSpecificEpithet("alba");
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());

    // names that differ only in authorship must not be equal
    b.getCombinationAuthorship().setAuthors(List.of("Mill."));
    assertNotEquals(a, b);
  }

  @Test
  public void testCopyDoesNotShareCollections() {
    ParsedName a = new ParsedName();
    a.addWarning("w1");
    a.setEpithetQualifier(NamePart.SPECIFIC, "cf.");

    ParsedName b = new ParsedName();
    b.copy(a);
    // mutating the copy must not leak back into the source
    b.addWarning("w2");
    b.setEpithetQualifier(NamePart.GENERIC, "aff.");

    assertEquals(1, a.getWarnings().size());
    assertFalse(a.hasEpithetQualifier(NamePart.GENERIC));
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
