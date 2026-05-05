package org.gbif.nameparser.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Closer {
  private static Logger LOG = LoggerFactory.getLogger(Closer.class);
  
  private Closer(){}
  
  public static void closeQuitely(AutoCloseable ... closeable) {
    if (closeable != null) {
      for (AutoCloseable ac : closeable) {
        try {
          ac.close();
        } catch (Exception e) {
          LOG.error("Error closing {}", ac, e);
        }
      }
    }
  }
}
