package org.gbif.nameparser.cli;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BarcodeOtuFilterTest {

  @Test
  public void excludesUniteSh() {
    assertTrue(BarcodeOtuFilter.isBarcodeOtu("SH1957732.10FU"));
    assertTrue(BarcodeOtuFilter.isBarcodeOtu("SH0864666.10FU"));
    assertTrue(BarcodeOtuFilter.isBarcodeOtu("  SH1958183.10FU  "));
    assertTrue(BarcodeOtuFilter.isBarcodeOtu("sh1958183.10fu"));
  }

  @Test
  public void excludesBoldBin() {
    assertTrue(BarcodeOtuFilter.isBarcodeOtu("BOLD:AAA0001"));
    assertTrue(BarcodeOtuFilter.isBarcodeOtu("BOLD:ACR2714"));
    assertTrue(BarcodeOtuFilter.isBarcodeOtu("bold:aab5053"));
  }

  @Test
  public void keepsRealNames() {
    assertFalse(BarcodeOtuFilter.isBarcodeOtu("Abies alba Mill."));
    assertFalse(BarcodeOtuFilter.isBarcodeOtu("Vulpes vulpes silaceus Miller, 1907"));
    // superficially similar but not an OTU code
    assertFalse(BarcodeOtuFilter.isBarcodeOtu("Shorea"));
    assertFalse(BarcodeOtuFilter.isBarcodeOtu("Boldenaria"));
    assertFalse(BarcodeOtuFilter.isBarcodeOtu(null));
    assertFalse(BarcodeOtuFilter.isBarcodeOtu(""));
  }
}
