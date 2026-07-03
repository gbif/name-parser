package org.gbif.nameparser.cli;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ReservoirTest {

  @Test
  public void keepsAllWhenUnderCapacity() {
    Reservoir<Integer> r = new Reservoir<>(10, 17);
    for (int i = 0; i < 5; i++) r.offer(i);
    assertEquals(5, r.items().size());
    assertEquals(5, r.seen());
  }

  @Test
  public void capsAtCapacity() {
    Reservoir<Integer> r = new Reservoir<>(10, 17);
    for (int i = 0; i < 1000; i++) r.offer(i);
    assertEquals(10, r.items().size());
    assertEquals(1000, r.seen());
  }

  @Test
  public void sameSeedIsReproducible() {
    assertEquals(sample(17), sample(17));
  }

  @Test
  public void differentSeedDiffers() {
    // With a 1000-item stream into a 10-slot reservoir, two seeds practically never agree.
    assertTrue(!sample(17).equals(sample(999)));
  }

  private static List<Integer> sample(long seed) {
    Reservoir<Integer> r = new Reservoir<>(10, seed);
    for (int i = 0; i < 1000; i++) r.offer(i);
    return new ArrayList<>(r.items());
  }
}
