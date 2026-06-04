package org.gbif.nameparser.cli.io;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class InputDetectorTest {

  @Test
  public void coldpTsv() {
    assertEquals(InputFormat.COLDP_TSV,
        InputDetector.classify("ID\tscientificName\tauthorship\trank"));
  }

  @Test
  public void coldpCsv() {
    assertEquals(InputFormat.COLDP_CSV,
        InputDetector.classify("ID,scientificName,authorship,rank,code"));
  }

  @Test
  public void coldpTsvSubsetOfColumns() {
    // Only one ColDP-recognised header is enough.
    assertEquals(InputFormat.COLDP_TSV,
        InputDetector.classify("foo\tscientificName\tbar"));
  }

  @Test
  public void plainSingleColumn() {
    assertEquals(InputFormat.PLAIN, InputDetector.classify("Felis catus"));
  }

  @Test
  public void plainTwoColumnsWithoutColdpHeaders() {
    assertEquals(InputFormat.PLAIN,
        InputDetector.classify("Felis catus\tLinnaeus, 1758"));
  }
}
