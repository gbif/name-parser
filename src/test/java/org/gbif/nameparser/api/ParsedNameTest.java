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
  public void testIndetPhraseName() throws Exception {
    // a phrase does not stand in for the missing epithet
    ParsedName pn = informal("Rhizobium", Rank.SPECIES, "sp.");
    assertTrue(pn.isIndetermined());
    pn.setPhrase("sp. RE1-2a");
    assertTrue(pn.isIndetermined());

    pn = informal("Grevillea", Rank.SUBSPECIES, "Busselton (G.J.Keighery s.n. 28/8/1985)");
    pn.setSpecificEpithet("brachystylis");
    assertTrue(pn.isIndetermined());

    // a strain designation of a determined species
    pn = informal("Escherichia", Rank.SPECIES, "MV4242");
    pn.setSpecificEpithet("coli");
    assertFalse(pn.isIndetermined());

    // a qualified epithet is still there
    pn = informal("Abies", Rank.SPECIES, null);
    pn.setSpecificEpithet("alba");
    pn.setEpithetQualifier(NamePart.SPECIFIC, "cf.");
    assertFalse(pn.isIndetermined());

    // a convariety has an infraspecific epithet, a grex a cultivar one
    pn = informal("Prunus", Rank.CONVARIETY, null);
    pn.setSpecificEpithet("armeniaca");
    assertTrue(pn.isIndetermined());
    pn.setInfraspecificEpithet("budae");
    assertFalse(pn.isIndetermined());
    pn = informal("Paphiopedilum", Rank.GREX, null);
    assertTrue(pn.isIndetermined());
    pn.setCultivarEpithet("Sorel");
    assertFalse(pn.isIndetermined());
  }

  @Test
  public void testAnchor() throws Exception {
    ParsedName pn = informal("Rhizobium", Rank.SPECIES, "sp. RMCC TR1811");
    ParsedName a = pn.anchor().orElseThrow();
    assertEquals("Rhizobium", a.getUninomial());
    assertNull(a.getGenus());
    assertEquals(Rank.GENUS, a.getRank());
    assertEquals(NameType.SCIENTIFIC, a.getType());
    assertNull(a.getPhrase());
    assertFalse(a.isIndetermined());

    // a subgenus
    pn = informal("Lasioglossum", Rank.SPECIES, "sp. 1");
    pn.setInfragenericEpithet("Dialictus");
    assertAnchor("Lasioglossum subgen. Dialictus", Rank.SUBGENUS, pn);
    pn.setCode(NomCode.ZOOLOGICAL);
    assertAnchor("Lasioglossum (Dialictus)", Rank.SUBGENUS, pn);
    // or the infrageneric rank the name was given
    pn = informal("Abies", Rank.SECTION_BOTANY, "sp. 1");
    pn.setInfragenericEpithet("Grandes");
    assertAnchor("Abies sect. Grandes", Rank.SECTION_BOTANY, pn);

    // a species, with the specific authorship of the infraspecific name
    pn = informal("Acacia", Rank.SUBSPECIES, "Young River (G.F.Craig 2052)");
    pn.setSpecificEpithet("mutabilis");
    assertAnchor("Acacia mutabilis", Rank.SPECIES, pn);
    CombinedAuthorship maslin = new CombinedAuthorship();
    maslin.setCombinationAuthorship(Authorship.authors("Maslin"));
    pn.setSpecificAuthorship(maslin);
    assertAnchor("Acacia mutabilis Maslin", Rank.SPECIES, pn);

    // a strain of a determined species
    pn = informal("Escherichia", Rank.SPECIES, "MV4242");
    pn.setSpecificEpithet("coli");
    assertAnchor("Escherichia coli", Rank.SPECIES, pn);

    // a qualified epithet is cut, and its authorship with it
    pn = informal("Abies", Rank.SPECIES, null);
    pn.setSpecificEpithet("alba");
    pn.setEpithetQualifier(NamePart.SPECIFIC, "cf.");
    pn.setCombinationAuthorship(Authorship.authors("Mill."));
    assertAnchor("Abies", Rank.GENUS, pn);
    // a genus only doubted anchors the name all the same, without its epithets
    pn.setEpithetQualifier(NamePart.GENERIC, "?");
    pn.getEpithetQualifier().remove(NamePart.SPECIFIC);
    assertAnchor("Abies", Rank.GENUS, pn);
    // a genus qualified otherwise leaves nothing
    pn.setEpithetQualifier(NamePart.GENERIC, "cf.");
    assertTrue(pn.anchor().isEmpty());
    // a doubted uninomial is its own anchor
    pn = informal(null, Rank.ORDER, null);
    pn.setUninomial("Monotremata");
    pn.setEpithetQualifier(NamePart.GENERIC, "?");
    assertAnchor("Monotremata", Rank.ORDER, pn);

    // an infraspecific epithet whose species is missing hangs off the genus
    pn = informal("Navicula", Rank.VARIETY, null);
    pn.setInfraspecificEpithet("fasciata");
    pn.setCombinationAuthorship(Authorship.authors("Grunow"));
    assertAnchor("Navicula", Rank.GENUS, pn);

    // an indetermined infraspecific name keeps the authorship of its species
    pn = informal("Tylotropidius", Rank.SUBSPECIES, null);
    pn.setSpecificEpithet("didymus");
    pn.setCombinationAuthorship(Authorship.yearAuthors("1907", "Karny"));
    assertAnchor("Tylotropidius didymus Karny, 1907", Rank.SPECIES, pn);

    // a higher taxon keeps its own rank
    pn = new ParsedName();
    pn.setType(NameType.INFORMAL);
    pn.setUninomial("Ichneumonidae");
    pn.setRank(Rank.FAMILY);
    pn.setPhrase("sp. 3");
    assertAnchor("Ichneumonidae", Rank.FAMILY, pn);

    // a complete name is its own anchor, at its own rank, with its authorship but without the details
    pn = new ParsedName();
    pn.setType(NameType.SCIENTIFIC);
    pn.setGenus("Taraxacum");
    pn.setSpecificEpithet("officinale");
    pn.setRank(Rank.SPECIES_AGGREGATE);
    assertAnchor("Taraxacum officinale", Rank.SPECIES_AGGREGATE, pn);
    pn.setGenus("Abies");
    pn.setSpecificEpithet("alba");
    pn.setInfraspecificEpithet("alpina");
    pn.setRank(Rank.SUBSPECIES);
    pn.setSpecificAuthorship(maslin);
    pn.setCombinationAuthorship(Authorship.authors("Smith"));
    pn.setTaxonomicNote("sensu lato");
    a = pn.anchor().orElseThrow();
    assertEquals("Abies alba subsp. alpina Smith", a.canonicalName());
    assertEquals(maslin, a.getSpecificAuthorship());
    assertNull(a.getTaxonomicNote());

    // an unparsable type has none
    pn.setType(NameType.IDENTIFIER);
    assertTrue(pn.anchor().isEmpty());
  }

  private static void assertAnchor(String canonical, Rank rank, ParsedName pn) {
    ParsedName a = pn.anchor().orElseThrow();
    assertEquals(canonical, a.canonicalName());
    assertEquals(rank, a.getRank());
    assertEquals(NameType.SCIENTIFIC, a.getType());
  }

  private static ParsedName informal(String genus, Rank rank, String phrase) {
    ParsedName pn = new ParsedName();
    pn.setType(NameType.INFORMAL);
    pn.setGenus(genus);
    pn.setRank(rank);
    pn.setPhrase(phrase);
    return pn;
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
