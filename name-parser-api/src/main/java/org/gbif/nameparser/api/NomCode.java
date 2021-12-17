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
package org.gbif.nameparser.api;

import java.net.URI;

/**
 * Enumeration representing the different nomenclatoral codes found in biology for scientific names.
 * <p/>
 * Nomenclature codes or codes of nomenclature are the various rulebooks that govern biological taxonomic
 * nomenclature, each in their own broad field of organisms.
 * To an end-user who only deals with names of species, with some awareness that species are assignable to
 * families, it may not be noticeable that there is more than one code, but beyond this basic level these are rather
 * different in the way they work.
 *
 * @see <a href="http://en.wikipedia.org/wiki/Nomenclature_codes">Nomenclature codes (Wikipedia)</a>
 */
public enum NomCode {
  
  BACTERIAL('P', "ICNP",
      "International Code of Nomenclature of Prokaryotes",
      "https://doi.org/10.1099/ijsem.0.000778"),
  BOTANICAL('B', "ICN",
      "International Code of Nomenclature for algae, fungi, and plants",
      "https://www.iapt-taxon.org/nomen/main.php"),
  CULTIVARS('C', "ICNCP",
      "International Code of Nomenclature for Cultivated Plants",
      "https://www.ishs.org/scripta-horticulturae/international-code-nomenclature-cultivated-plants-ninth-edition"),
  PHYTOSOCIOLOGICAL('S', "ICPN", // S for Syntaxonomy
      "International Code of Phytosociological Nomenclature",
      "https://doi.org/10.2307/3236580"),
  VIRUS('V', "ICVCN",
      "International Code of Virus Classifications and Nomenclature",
      "https://talk.ictvonline.org/information/w/ictv-information/383/ictv-code"),
  ZOOLOGICAL('Z', "ICZN",
      "International Code of Zoological Nomenclature",
      "http://www.iczn.org/code");
  
  private final String title;
  private final Character abbrev;
  private final String acronym;
  private final URI link;
  
  NomCode(Character abbrev, String acronym, String title, String link) {
    this.abbrev = abbrev;
    this.acronym = acronym;
    this.link = URI.create(link);
    this.title = title;
  }

  public Character getAbbrev() {
    return abbrev;
  }

  public String getAcronym() {
    return acronym;
  }
  
  public URI getLink() {
    return link;
  }
  
  public String getTitle() {
    return title;
  }
  
}
