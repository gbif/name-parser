package org.gbif.nameparser;

import org.gbif.nameparser.api.UnparsableNameException;
import org.junit.Test;

/**
 *
 */
public class NameParserANTLRTest {

  @Test
  public void parse() throws UnparsableNameException {
    NameParserANTLR parser = new NameParserANTLR();
    System.out.println(parser.parse("Abies alba Mill."));
    System.out.println(parser.parse("Abies alba L."));
    System.out.println(parser.parse("BOLD:AAA2176"));
    System.out.println(parser.parse("SH495646.07FU"));
    System.out.println(parser.parse("Bryocyclops campaneri Rocha C.E.F. & Bjornberg M.H.G.C., 1987"));
    System.out.println(parser.parse("Equine rhinitis A virus"));
    System.out.println(parser.parse("Isosphaera pallida (ex Woronichin, 1927) Giovannoni et al., 1995"));
  }

}