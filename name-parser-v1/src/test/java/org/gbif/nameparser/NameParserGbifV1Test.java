package org.gbif.nameparser;

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.Rank;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 *
 */
public class NameParserGbifV1Test {
  NameParserGbifV1 parser = new NameParserGbifV1();

  @Test
  public void convertNameType() throws Exception {
    for (org.gbif.nameparser.api.NameType t : org.gbif.nameparser.api.NameType.values()) {
      assertNotNull(NameParserGbifV1.toGbif(t));
    }
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
    assertEquals("Abies alba", parser.parseQuietly("Abies alba Mill.").canonicalName());
    ParsedName pn = parser.parseQuietly("Abies alba x Pinus graecus L.");
    assertEquals("Abies alba x Pinus graecus L.", pn.getScientificName());
    assertEquals(NameType.HYBRID, pn.getType());
    assertNull(pn.getGenusOrAbove());

    assertTrue(parser.parseQuietly("Protoscenium simplex  (Cleve, 1899), Jørgensen, 1905 ", Rank.SPECIES).isAuthorsParsed());
    assertTrue(parser.parseQuietly("Plagiacanthidae", Rank.SPECIES).isAuthorsParsed());
  }

  @Test
  public void parseToCanonical() throws Exception {
    assertEquals("Abies alba", parser.parseToCanonical("Abies alba Mill."));
  }

  @Test
  public void parseToCanonicalOrScientificName() throws Exception {
    assertEquals("Abies alba", parser.parseToCanonicalOrScientificName("Abies alba"));
    assertEquals("Abies alba x Pinus graecus L.", parser.parseToCanonicalOrScientificName("Abies alba x Pinus graecus L."));
  }

}