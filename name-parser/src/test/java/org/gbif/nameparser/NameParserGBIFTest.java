package org.gbif.nameparser;

import java.lang.management.ManagementFactory;

/**
 *
 */
public class NameParserGBIFTest extends NameParserTest {
  private static final boolean DEBUG = ManagementFactory.getRuntimeMXBean()
      .getInputArguments().toString().indexOf("-agentlib:jdwp") > 0;

  public NameParserGBIFTest() {
    super(new NameParserGBIF(DEBUG ? 99999999 : 1000));
  }

}