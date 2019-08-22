package org.gbif.nameparser;

import org.gbif.api.exception.UnparsableException;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.NamePart;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.Rank;
import org.gbif.nameparser.api.Warnings;
import org.junit.Test;

import java.lang.management.ManagementFactory;

import static org.gbif.nameparser.api.NameType.PLACEHOLDER;
import static org.gbif.nameparser.api.NomCode.ZOOLOGICAL;
import static org.gbif.nameparser.api.Rank.FAMILY;
import static org.gbif.nameparser.api.Rank.GENUS;
import static org.junit.Assert.*;


/**
 *
 */
public class NameParserGbifV1Test {
  private static final boolean DEBUG = ManagementFactory.getRuntimeMXBean()
      .getInputArguments().toString().indexOf("-agentlib:jdwp") > 0;

  NameParserGbifV1 parser = new NameParserGbifV1(DEBUG ? 99999999 : 1000);

  @Test
  public void convertNameType() throws Exception {
    org.gbif.nameparser.api.ParsedName pn = new org.gbif.nameparser.api.ParsedName();
    for (org.gbif.nameparser.api.NameType t : org.gbif.nameparser.api.NameType.values()) {
      pn.setType(t);
      assertNotNull(NameParserGbifV1.gbifNameType(pn));
    }
    pn.setCandidatus(true);
    assertEquals(NameType.CANDIDATUS, NameParserGbifV1.gbifNameType(pn));

    pn.setCandidatus(false);
    pn.setCultivarEpithet("Bella");
    assertEquals(NameType.CULTIVAR, NameParserGbifV1.gbifNameType(pn));

    pn.setDoubtful(true);
    assertEquals(NameType.DOUBTFUL, NameParserGbifV1.gbifNameType(pn));
  
    pn.addWarning(Warnings.BLACKLISTED_EPITHET);
    assertEquals(NameType.BLACKLISTED, NameParserGbifV1.gbifNameType(pn));
  }

  @Test
  public void convertNamePart() throws Exception {
    for (org.gbif.nameparser.api.NamePart t : org.gbif.nameparser.api.NamePart.values()) {
      System.out.println(t.name());
      assertNotNull(NameParserGbifV1.toGbif(t));
    }
  }

  @Test
  public void convertRank() throws Exception {
    for (org.gbif.nameparser.api.Rank t : org.gbif.nameparser.api.Rank.values()) {
      assertNotNull(NameParserGbifV1.toGbif(t));
    }
  }

  @Test
  public void convertRankReverse() throws Exception {
    for (org.gbif.api.vocabulary.Rank t : org.gbif.api.vocabulary.Rank.values()) {
      assertNotNull(NameParserGbifV1.fromGbif(t));
    }
  }

  @Test
  public void parseQuietly() throws Exception {
    ParsedName pn = parser.parseQuietly("Navicula var. fasciata", Rank.VARIETY);
    assertNull(pn.getSpecificEpithet());
    assertEquals(NameType.INFORMAL, pn.getType());

    assertEquals("Abies alba", parser.parseQuietly("Abies alba Mill.").canonicalName());
    pn = parser.parseQuietly("Abies alba x Pinus graecus L.");
    assertEquals("Abies alba x Pinus graecus L.", pn.getScientificName());
    assertEquals(NameType.HYBRID, pn.getType());
    assertNull(pn.getGenusOrAbove());

    assertFalse(parser.parseQuietly("Protoscenium simplex  (Cleve, 1899), Jørgensen, 1905 ", Rank.SPECIES).isParsedPartially());
    assertFalse(parser.parseQuietly("Plagiacanthidae", Rank.SPECIES).isParsedPartially());

    pn = parser.parseQuietly("Plagiacanthidae WhatTheHeck @%&/ (please dont parse me[ he?5789])", Rank.SPECIES);
    assertTrue(pn.isParsed());
    assertTrue(pn.isParsedPartially());
    assertEquals("Plagiacanthidae", pn.getGenusOrAbove());
  }

  @Test
  public void assertScientificName() throws Exception {
    ParsedName pn = parser.parseQuietly("Abies sp.");
    assertEquals("Abies sp.", pn.getScientificName());
    assertEquals("Abies spec.", pn.canonicalName());
    assertEquals("Abies", pn.getGenusOrAbove());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getSpecificEpithet());

    pn = parser.parseQuietly("×Abies Mill.");
    assertEquals("×Abies Mill.", pn.getScientificName());
    assertEquals("Abies", pn.canonicalName());
    assertEquals("Abies", pn.getGenusOrAbove());
    assertNull(pn.getRank());
    assertNull(pn.getSpecificEpithet());
    assertEquals(NamePart.GENERIC, pn.getNotho());

    pn = parser.parseQuietly("? hostilis Gravenhorst, 1829");
    assertEquals("? hostilis Gravenhorst, 1829", pn.getScientificName());
    assertEquals("? hostilis", pn.canonicalName());
    assertEquals("?", pn.getGenusOrAbove());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertEquals("hostilis", pn.getSpecificEpithet());

    pn = parser.parseQuietly("unassigned Asteraceae");
    assertEquals("unassigned Asteraceae", pn.getScientificName());
    assertNull(pn.canonicalName());
    assertNull(pn.getGenusOrAbove());
    assertNull(pn.getRank());
    assertNull(pn.getSpecificEpithet());
    
    // https://github.com/gbif/name-parser/issues/45
    pn = parser.parseQuietly("OdontellidaeGEN");
    assertEquals(NameType.PLACEHOLDER, pn.getType());
    assertEquals("OdontellidaeGEN", pn.getScientificName());
    assertEquals("OdontellidaeGEN", pn.canonicalName());
    assertEquals("OdontellidaeGEN", pn.getGenusOrAbove());
    assertNull(pn.getRank());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getAuthorship());
  }
  
  @Test
  public void unparsable() throws Exception {
    String[] unparsables = new String[]{"BOLD:AAX3687", "Potatoe virus", "Pinus alba × Abies picea Mill."};
    for (String n : unparsables) {
      try {
        ParsedName pn = parser.parse(n);
        fail(n+ " should be unparsable");
      } catch (UnparsableException e) {
        // expected
      }
    }
  }

  @Test
  public void parseToCanonical() throws Exception {
    assertEquals("Abies alba", parser.parseToCanonical("Abies alba Mill."));
    assertNull(parser.parseToCanonical("BOLD:AAX3687", Rank.SPECIES));
  }

  @Test
  public void parseToCanonicalOrScientificName() throws Exception {
    assertEquals("BOLD:AAX3687", parser.parseToCanonicalOrScientificName("BOLD:AAX3687", Rank.SPECIES));
    assertEquals("Abies alba", parser.parseToCanonicalOrScientificName("Abies alba"));
    assertEquals("Abies alba x Pinus graecus L.", parser.parseToCanonicalOrScientificName("Abies alba x Pinus graecus L."));
  }

}