package org.gbif.nameparser;

import org.gbif.api.model.checklistbank.ParsedName;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class NormalisedNameParserTest {

  @Test
  @Ignore
  public void testMonomialRsGbifOrg() throws Exception {
    NormalisedNameParser np = new NormalisedNameParser(100);
    np.readMonomialsRsGbifOrg();
  }

  @Test
  public void timeoutLongNames() throws Exception {
    NormalisedNameParser parser = new NormalisedNameParser(50);

    String name = "Equicapillimyces hongkongensis S.S.Y. Wong, A.H.Y. Ngan, Riggs, J.L.L. Teng, G.K.Y. Choi, R.W.S. Poon, J.J.Y. Hui, F.J. Low, Luk &";

    ParsedName n = new ParsedName();
    final long pStart = System.currentTimeMillis();
    Assert.assertFalse("No timeout happening for long running parsing", parser.parseNormalisedName(n, name));
    final long duration = System.currentTimeMillis() - pStart;

    Assert.assertTrue("No timeout happening for long running parsing", duration < 75);
  }

}