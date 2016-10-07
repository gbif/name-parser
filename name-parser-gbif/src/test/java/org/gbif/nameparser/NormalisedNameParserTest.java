package org.gbif.nameparser;

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.nameparser.NormalisedNameParser;

import java.util.regex.Pattern;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NormalisedNameParserTest {

  @Test
  public void testEpithetPattern() throws Exception {
    Pattern epi = Pattern.compile("^"+ NormalisedNameParser.EPHITHET+"$");
    assertTrue(epi.matcher("alba").find());
    assertTrue(epi.matcher("biovas").find());
    assertTrue(epi.matcher("serovat").find());
    assertTrue(epi.matcher("novo-zelandia").find());
    assertTrue(epi.matcher("elevar").find());
    assertTrue(epi.matcher("zelandia").find());
    assertTrue(epi.matcher("zelandia").find());
    assertTrue(epi.matcher("zelandia").find());
    assertTrue(epi.matcher("zelandia").find());

    assertFalse(epi.matcher("").find());
    assertFalse(epi.matcher("a").find());
    assertFalse(epi.matcher("serovar").find());
    assertFalse(epi.matcher("genotype").find());
    assertFalse(epi.matcher("agamovar").find());
    assertFalse(epi.matcher("cultivar").find());
    assertFalse(epi.matcher("serotype").find());
    assertFalse(epi.matcher("cytoform").find());
    assertFalse(epi.matcher("chemoform").find());
  }

  @Test
  public void timeoutLongNames() throws Exception {
    final int timeout = 50;
    NormalisedNameParser parser = new NormalisedNameParser(timeout);

    String name = "Equicapillimyces hongkongensis S.S.Y. Wong, A.H.Y. Ngan, Riggs, J.L.L. Teng, G.K.Y. Choi, R.W.S. Poon, J.J.Y. Hui, F.J. Low, Luk &";

    ParsedName n = new ParsedName();
    final long pStart = System.currentTimeMillis();
    assertFalse("No timeout happening for long running parsing", parser.parseNormalisedName(n, name, null));
    final long duration = System.currentTimeMillis() - pStart;

    // the duration is the timeout PLUS some initialization overhead thats why we add 50ms to it
    assertTrue("No timeout happening for long running parsing", duration < 50 + timeout);
  }

}