package org.gbif.nameparser.api;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NameTypeTest {
  
  @Test
  public void testIsParsable() throws Exception {
    assertTrue(NameType.SCIENTIFIC.isParsable());
    assertTrue(NameType.INFORMAL.isParsable());
    assertFalse(NameType.VIRUS.isParsable());
    assertFalse(NameType.NO_NAME.isParsable());
    assertFalse(NameType.HYBRID_FORMULA.isParsable());
    assertFalse(NameType.OTU.isParsable());
  }
  
}