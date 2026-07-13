package org.gbif.nameparser.api;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class NameTypeTest {

  @Test
  public void testIsParsable() throws Exception {
    assertTrue(NameType.SCIENTIFIC.isParsable());
    assertTrue(NameType.INFORMAL.isParsable());
    // NameType.VIRUS no longer exists
    assertFalse(NameType.OTHER.isParsable());
    assertFalse(NameType.FORMULA.isParsable());
  }

  @Test
  public void unparsableCarriesCode() {
    UnparsableNameException e =
        new UnparsableNameException(NameType.OTHER, NomCode.VIRUS, "Tobacco mosaic virus");
    assertEquals(NameType.OTHER, e.getType());
    assertEquals(NomCode.VIRUS, e.getCode());
    assertNull(new UnparsableNameException(NameType.OTHER, "x").getCode());
  }

}