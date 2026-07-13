package org.gbif.nameparser.util;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class LineReader implements Iterable<String>, AutoCloseable {
  private final BufferedReader br;
  private final boolean skipBlank;
  private final boolean skipComments;
  private int row = 0;
  private int currRow;

  /**
   * @param steam UTF8 character stream
   */
  public LineReader(InputStream steam) {
    this(new BufferedReader(new InputStreamReader(steam, StandardCharsets.UTF_8)));
  }

  public LineReader(BufferedReader br) {
    this(br, true, true);
  }

  public LineReader(BufferedReader br, boolean skipBlank, boolean skipComments) {
    this.br = br;
    this.skipBlank = skipBlank;
    this.skipComments = skipComments;
  }

  public int getRow() {
    return currRow;
  }

  @Override
  public Iterator<String> iterator() {
    return new LineIterator();
  }

  @Override
  public void close() {
    try {
      br.close();
    } catch (IOException e) {
      // close quietly
    }
  }

  class LineIterator implements Iterator<String> {
    private String next;

    public LineIterator() {
      fetch();
    }

    @Override
    public boolean hasNext() {
      return next != null;
    }

    @Override
    public String next() {
      String val = next;
      currRow = row;
      fetch();
      return val;
    }

    private void fetch() {
      try {
        while (row == 0 || next != null) {
          next = br.readLine();
          row++;
          if (skipBlank && StringUtils.isBlank(next)) {
            continue;
          }
          if (skipComments && next.startsWith("#")) {
            continue;
          }
          break;
        }
      } catch (IOException e) {
        throw new RuntimeException("Error reading row "+row, e);
      }
    }
  }
}
