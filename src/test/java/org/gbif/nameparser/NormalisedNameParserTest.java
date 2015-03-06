package org.gbif.nameparser;

import org.gbif.api.model.checklistbank.ParsedName;

import java.util.regex.Pattern;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NormalisedNameParserTest {

  @Test
  @Ignore
  public void testMonomialRsGbifOrg() throws Exception {
    NormalisedNameParser np = new NormalisedNameParser(100);
    np.readMonomialsRsGbifOrg();
  }

  @Test
  public void testEpithetPattern() throws Exception {
    Pattern epi = Pattern.compile("^"+NormalisedNameParser.EPHITHET+"$");
    assertTrue(epi.matcher("alba").find());
    assertTrue(epi.matcher("biovas").find());
    assertTrue(epi.matcher("serovat").find());
    assertTrue(epi.matcher("novo-zelandia").find());
    assertTrue(epi.matcher("novae zelandia").find());

    assertFalse(epi.matcher("").find());
    assertFalse(epi.matcher("a").find());
    assertFalse(epi.matcher("serovar").find());
    assertFalse(epi.matcher("genotype").find());
    assertFalse(epi.matcher("agamovar").find());
    assertFalse(epi.matcher("cultivar").find());
    assertFalse(epi.matcher("serotype").find());
    assertFalse(epi.matcher("form").find());
    assertFalse(epi.matcher("nvar").find());
    assertFalse(epi.matcher("cytoform").find());
    assertFalse(epi.matcher("chemoform").find());
    assertFalse(epi.matcher("form").find());
  }

  @Test
  public void timeoutLongNames() throws Exception {
    NormalisedNameParser parser = new NormalisedNameParser(50);

    String name = "Equicapillimyces hongkongensis S.S.Y. Wong, A.H.Y. Ngan, Riggs, J.L.L. Teng, G.K.Y. Choi, R.W.S. Poon, J.J.Y. Hui, F.J. Low, Luk &";

    ParsedName n = new ParsedName();
    final long pStart = System.currentTimeMillis();
    assertFalse("No timeout happening for long running parsing", parser.parseNormalisedName(n, name));
    final long duration = System.currentTimeMillis() - pStart;

    assertTrue("No timeout happening for long running parsing", duration < 75);
  }

}