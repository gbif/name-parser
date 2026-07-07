package org.gbif.nameparser.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Seeded reservoir sampler (Algorithm R). Keeps a uniform random sample of at most
 * {@code capacity} items from a stream of unknown length in a single pass and
 * bounded memory — used to pick which parses to send to the LLM without holding all
 * ~6.3M names. A fixed seed makes each run reproducible.
 */
public final class Reservoir<T> {
  private final int capacity;
  private final Random rng;
  private final List<T> items;
  private long seen;

  public Reservoir(int capacity, long seed) {
    this.capacity = Math.max(0, capacity);
    this.rng = new Random(seed);
    this.items = new ArrayList<>(Math.min(capacity, 1024));
  }

  /** Offer one item; it may or may not be retained. */
  public void offer(T item) {
    seen++;
    if (capacity == 0) return;
    if (items.size() < capacity) {
      items.add(item);
    } else {
      long j = (long) (rng.nextDouble() * seen); // uniform in [0, seen)
      if (j < capacity) {
        items.set((int) j, item);
      }
    }
  }

  /** The retained sample (order is not meaningful). */
  public List<T> items() {
    return items;
  }

  /** Total number of items offered. */
  public long seen() {
    return seen;
  }
}
