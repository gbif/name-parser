package org.gbif.nameparser.cli.io;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class CsvTest {

  @Test
  public void splitTab() {
    assertArrayEquals(new String[]{"a", "b", "c"}, Csv.split("a\tb\tc", '\t'));
    assertArrayEquals(new String[]{"a", "", "c"}, Csv.split("a\t\tc", '\t'));
    assertArrayEquals(new String[]{""}, Csv.split("", '\t'));
  }

  @Test
  public void splitCsvWithQuotes() {
    assertArrayEquals(new String[]{"a", "b,c", "d"},
        Csv.split("a,\"b,c\",d", ','));
    assertArrayEquals(new String[]{"foo \"bar\""},
        Csv.split("\"foo \"\"bar\"\"\"", ','));
  }

  @Test
  public void quoteCsv() {
    assertEquals("plain", Csv.quoteCsv("plain"));
    assertEquals("\"a,b\"", Csv.quoteCsv("a,b"));
    assertEquals("\"a\"\"b\"", Csv.quoteCsv("a\"b"));
    assertEquals("\"line\nbreak\"", Csv.quoteCsv("line\nbreak"));
    assertEquals("", Csv.quoteCsv(null));
  }

  @Test
  public void sanitiseTsv() {
    assertEquals("plain", Csv.sanitiseTsv("plain"));
    assertEquals("a b c", Csv.sanitiseTsv("a\tb\tc"));
    assertEquals("a b c", Csv.sanitiseTsv("a\nb\rc"));
    assertEquals("", Csv.sanitiseTsv(null));
  }

  @Test
  public void detectDelimiter() {
    assertEquals('\t', Csv.detectDelimiter("a\tb\tc"));
    assertEquals(',', Csv.detectDelimiter("a,b,c"));
    assertEquals(',', Csv.detectDelimiter("just-one-name"));
    assertEquals('\t', Csv.detectDelimiter("a\tb,c"));
  }
}
