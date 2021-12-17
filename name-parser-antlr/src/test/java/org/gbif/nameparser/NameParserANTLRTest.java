/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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