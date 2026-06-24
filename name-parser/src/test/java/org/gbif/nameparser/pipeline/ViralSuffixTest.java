package org.gbif.nameparser.pipeline;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ViralSuffixTest {
  @Test
  public void viralGenusAndHigherSuffixes() {
    assertTrue(ViralSuffix.isViral("Tobamovirus"));
    assertTrue(ViralSuffix.isViral("Orthoebolavirus"));
    assertTrue(ViralSuffix.isViral("Lausannevirus"));
    assertTrue(ViralSuffix.isViral("Pospiviroid"));
    assertTrue(ViralSuffix.isViral("Colecusatellite"));
    assertTrue(ViralSuffix.isViral("Coronaviridae"));
    assertTrue(ViralSuffix.isViral("Nidovirales"));
    assertTrue(ViralSuffix.isViral("Pisuviricota"));
  }

  @Test
  public void nonViralLookAlikes() {
    assertFalse(ViralSuffix.isViral("Crassatellites")); // mollusk, plural -satellites
    assertFalse(ViralSuffix.isViral("Aspilota"));
    assertFalse(ViralSuffix.isViral("Adomaviruses"));   // plural -viruses
    assertFalse(ViralSuffix.isViral(null));
  }
}
