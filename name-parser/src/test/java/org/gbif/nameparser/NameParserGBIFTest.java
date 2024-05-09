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

import com.google.common.collect.Iterables;
import org.apache.commons.io.LineIterator;
import org.gbif.nameparser.api.*;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import static org.gbif.nameparser.api.NamePart.INFRASPECIFIC;
import static org.gbif.nameparser.api.NamePart.SPECIFIC;
import static org.gbif.nameparser.api.NameType.*;
import static org.gbif.nameparser.api.NomCode.*;
import static org.gbif.nameparser.api.Rank.*;
import static org.junit.Assert.*;

/**
 *
 */
public class NameParserGBIFTest {
  private static Logger LOG = LoggerFactory.getLogger(NameParserGBIFTest.class);
  private static final boolean DEBUG = true;
  //private static final boolean DEBUG = ManagementFactory.getRuntimeMXBean().getInputArguments().toString().indexOf("-agentlib:jdwp") > 0;

  private final NameParser parser;

  public NameParserGBIFTest() {
    this.parser = new NameParserGBIF(DEBUG ? 99999999 : 1000);
  }

  @After
  public void teardown() throws Exception {
    parser.close();
  }

  /**
   * https://github.com/gbif/name-parser/issues/33
   * https://github.com/gbif/name-parser/issues/66
   * https://www.ncbi.nlm.nih.gov/books/NBK8808/#A431
   */
  @Test
  public void sic() throws Exception {
    assertName("Ameiva plei Rosicky, 1955", "Ameiva plei")
            .species("Ameiva", "plei")
            .combAuthors("1955", "Rosicky")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Ameiva plei (sic) Duméril & Bibron, 1839", "Ameiva plei")
            .species("Ameiva", "plei")
            .combAuthors("1839", "Duméril", "Bibron")
            .sic()
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("†Amnicola kushixiaensis [sic]", "Amnicola kushixiaensis")
            .species("Amnicola", "kushixiaensis")
            .extinct()
            .sic()
            .nothingElse();

    assertName("†Amnicola (Amnicola) dubrueilliana [sic]", "Amnicola dubrueilliana")
            .species("Amnicola", "Amnicola", "dubrueilliana")
            .extinct()
            .sic()
            .nothingElse();

    assertName("Anabathron (Scrobs) elongatus [sic]", "Anabathron elongatus")
            .species("Anabathron", "Scrobs", "elongatus")
            .sic()
            .nothingElse();

    assertName("Scaphander lignarius var. brittanica [sic]", "Scaphander lignarius var. brittanica")
            .infraSpecies("Scaphander", "lignarius", VARIETY, "brittanica")
            .sic()
            .nothingElse();

    assertName("†Tulotoma bifarcinata var. contiqua [sic]", "Tulotoma bifarcinata var. contiqua")
            .infraSpecies("Tulotoma", "bifarcinata", VARIETY, "contiqua")
            .extinct()
            .sic()
            .nothingElse();

    assertName("Scincus homolocephalus (sic) Wiegmann, 1828", "Scincus homolocephalus")
            .species("Scincus", "homolocephalus")
            .combAuthors("1828", "Wiegmann")
            .sic()
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Cochlostyla (Dryocochlias) satyrus palawanensis [sic]", "Cochlostyla satyrus palawanensis")
            .infraSpecies("Cochlostyla","satyrus", INFRASPECIFIC_NAME, "palawanensis")
            .infraGeneric("Dryocochlias")
            .sic()
            .nothingElse();

    assertName("Clionella sinuata borni [sic]", "Clionella sinuata borni")
            .infraSpecies("Clionella", "sinuata", INFRASPECIFIC_NAME, "borni")
            .sic()
            .nothingElse();

    assertName("†Melanopsis (Melanopsis) pterochyla pterochyla [sic]", "Melanopsis pterochyla pterochyla")
            .infraSpecies("Melanopsis", "pterochyla", INFRASPECIFIC_NAME, "pterochyla")
            .infraGeneric("Melanopsis")
            .extinct()
            .sic()
            .nothingElse();

    assertName("Melanella hollandri [sic] var. detrita Kucik", "Melanella hollandri var. detrita")
            .infraSpecies("Melanella", "hollandri", VARIETY, "detrita")
            .combAuthors(null, "Kucik")
            .sic()
            .nothingElse();

    assertName("Alnetoidia (Alnella) [sic] sudzhuchenica Sohi, 1998", "Alnetoidia sudzhuchenica")
            .species("Alnetoidia", "Alnella", "sudzhuchenica")
            .combAuthors("1998", "Sohi")
            .sic()
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Flavobacterium branchiophila (sic) Wakabayashi et al. 1989", "Flavobacterium branchiophila")
            .species("Flavobacterium", "branchiophila")
            .combAuthors("1989", "Wakabayashi", "al.")
            .sic()
            .code(ZOOLOGICAL)
            .nothingElse();

    // https://lpsn.dsmz.de/text/glossary#corrigendum
    assertName("Campylobacter lari corrig. Benjamin et al. 1984", "Campylobacter lari")
            .species("Campylobacter", "lari")
            .combAuthors("1984", "Benjamin", "al.")
            .corrig()
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Campylobacter laridis (sic) Benjamin et al. 1984", "Campylobacter laridis")
            .species("Campylobacter", "laridis")
            .combAuthors("1984", "Benjamin", "al.")
            .sic()
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Firmicutes corrig. Gibbons & Murray, 1978", "Firmicutes")
            .monomial("Firmicutes")
            .combAuthors("1978", "Gibbons", "Murray")
            .corrig()
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Barleeidae [sic]", "Barleeidae")
            .monomial("Barleeidae")
            .sic()
            .nothingElse();

    assertName("†Sainshandiidae [sic]", "Sainshandiidae")
            .monomial("Sainshandiidae")
            .extinct()
            .sic()
            .nothingElse();

    assertName("Turbo porphyrites [sic, porphyria]", "Turbo porphyrites")
            .species("Turbo", "porphyrites")
            .partial("(sic,porphyria)") // not ideal , but hey
            .nothingElse();

    assertAuthorship("[sic]")
            .sic()
            .nothingElse();

    assertAuthorship("[sic] Walter")
            .sic()
            .combAuthors(null, "Walter")
            .nothingElse();

    assertAuthorship("[sic!]")
            .sic()
            .nothingElse();
  }

  @Test
  public void squareGenera() throws Exception {
    assertName("[Acontia] chia Holland, 1894", "Acontia chia")
            .species("Acontia", "chia")
            .combAuthors("1894", "Holland")
            .code(ZOOLOGICAL)
            .doubtful()
            .warning(Warnings.DOUBTFUL_GENUS)
            .nothingElse();

    assertName("[Dexia]", "Dexia")
            .monomial("Dexia")
            .doubtful()
            .warning(Warnings.DOUBTFUL_GENUS)
            .nothingElse();

    assertName("[Diomea] orbicularis Walker, 1858", "Diomea orbicularis")
            .species("Diomea", "orbicularis")
            .combAuthors("1858", "Walker")
            .code(ZOOLOGICAL)
            .doubtful()
            .warning(Warnings.DOUBTFUL_GENUS)
            .nothingElse();
  }

  @Test
  public void tinfr() throws Exception {
    assertName("Hieracium vulgatum t.infr. arrectariicaule Sudre", "Hieracium vulgatum arrectariicaule")
            .infraSpecies("Hieracium", "vulgatum", INFRASPECIFIC_NAME, "arrectariicaule")
            .combAuthors(null, "Sudre")
            .nothingElse();
  }

  /**
   * https://github.com/gbif/name-parser/issues/82
   */
  @Test
  public void error() throws Exception {
    assertName("Agama annectans Blanford, 1870 [orth. error]", "Agama annectans")
            .species("Agama", "annectans")
            .combAuthors("1870", "Blanford")
            .nomNote("orth.error")
            .code(ZOOLOGICAL)
            .nothingElse();
  }

  /**
   * https://github.com/gbif/name-parser/issues/80
   */
  @Test
  public void CkYang() throws Exception {
    assertName("Nemopistha sinica C.-k. Yang, 1986", "Nemopistha sinica")
            .species("Nemopistha", "sinica")
            .combAuthors("1986", "C.-k.Yang")
            .code(ZOOLOGICAL)
            .nothingElse();
  }

  /**
   * https://github.com/gbif/name-parser/issues/75
   */
  @Test
  public void oosterveldii() throws Exception {
    assertName("Taraxacum piet-oosterveldii H. Øllg. in press", "Taraxacum piet-oosterveldii")
            .species("Taraxacum", "piet-oosterveldii")
            .combAuthors(null, "H.Øllg.")
            .nomNote("in press")
            .manuscript()
            .nothingElse();
  }

  /**
   * https://github.com/gbif/name-parser/issues/72
   */
  @Test
  public void zurStrassen() throws Exception {
    assertName("Jezzinothrips cretacicus zur Strassen, 1973", "Jezzinothrips cretacicus")
            .species("Jezzinothrips", "cretacicus")
            .combAuthors("1973", "zur Strassen")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Jezzinothrips cretacicus amazur Strassen, 1973", "Jezzinothrips cretacicus amazur")
            .infraSpecies("Jezzinothrips", "cretacicus", INFRASPECIFIC_NAME, "amazur")
            .combAuthors("1973", "Strassen")
            .code(ZOOLOGICAL)
            .nothingElse();
  }

  /**
   * https://github.com/gbif/name-parser/issues/70
   */
  @Test
  public void nomSuperfl() throws Exception {
    assertName("Agrostis compressa Willd., nom. superfl.", "Agrostis compressa")
            .species("Agrostis", "compressa")
            .combAuthors(null, "Willd.")
            .nomNote("nom.superfl.")
            .nothingElse();
  }

  /**
   * https://github.com/gbif/name-parser/issues/68
   */
  @Test
  public void cladeNames() throws Exception {
    assertUnparsable("Amauropeltoid clade", INFORMAL);
  }

  /**
   * https://github.com/gbif/name-parser/issues/67
   */
  @Test
  public void doubleHyphenEpithet() throws Exception {
    assertName("Grammitis friderici-et-pauli (Christ) Copel.", "Grammitis friderici-et-pauli")
            .species("Grammitis", "friderici-et-pauli")
            .combAuthors(null, "Copel.")
            .basAuthors(null, "Christ")
            .code(BOTANICAL)
            .nothingElse();
  }

  /**
   * https://github.com/gbif/name-parser/issues/62
   */
  @Test
  public void desLoges() throws Exception {
    assertName("Desbrochers des Loges, 1881", "Desbrochers")
            .monomial("Desbrochers")
            .combAuthors("1881", "des Loges")
            .code(ZOOLOGICAL)
            .nothingElse();
  }

  /**
   * https://github.com/gbif/name-parser/issues/60
   */
  @Test
  public void subg() throws Exception {
    assertName("Centaurea subg. Jacea", "Centaurea subgen. Jacea")
            .infraGeneric("Centaurea", SUBGENUS, "Jacea")
            .nothingElse();

    assertName("Centaurea L. subg. Jacea", "Centaurea subgen. Jacea")
            .infraGeneric("Centaurea", SUBGENUS, "Jacea")
            .nothingElse();

    // not a series: https://github.com/gbif/checklistbank/issues/200
    assertName("Mergus merganser Linnaeus, 1758", "Mergus merganser")
            .species("Mergus", "merganser")
            .combAuthors("1758", "Linnaeus")
            .code(ZOOLOGICAL)
            .nothingElse();  }

  /**
   * https://github.com/gbif/name-parser/issues/59
   */
  @Test
  public void vonDen() throws Exception {
    assertName("Gyalidea minuta van den Boom & Vezda", "Gyalidea minuta")
            .species("Gyalidea", "minuta")
            .combAuthors(null, "van den Boom", "Vezda")
            .nothingElse();
  }

  /**
   * https://github.com/gbif/name-parser/issues/58
   */
  @Test
  public void vdAuthors() throws Exception {
    assertName("Taraxacum dunense v. Soest", "Taraxacum dunense")
            .species("Taraxacum", "dunense")
            .combAuthors(null, "v.Soest")
            .nothingElse();

    assertName("Rubus planus v. d. Beek", "Rubus planus")
            .species("Rubus", "planus")
            .combAuthors(null, "v.d.Beek")
            .nothingElse();
  }

  /**
   * https://github.com/gbif/name-parser/issues/56
   */
  @Test
  public void rechf() throws Exception {
    assertName("Salix repens L. subsp. galeifolia Neumann ex Rech. f.", "Salix repens subsp. galeifolia")
            .infraSpecies("Salix", "repens", SUBSPECIES, "galeifolia")
            .combExAuthors("Neumann")
            .combAuthors(null, "Rech.f.")
            .code(BOTANICAL)
            .nothingElse();
  }

  /**
   * https://github.com/gbif/name-parser/issues/49
   */
  @Test
  public void wfoAuthors() throws Exception {
    assertName("Taraxacum vulgaris Backer ex K.Heyne", "Taraxacum vulgaris")
            .species("Taraxacum", "vulgaris")
            .combAuthors(null, "K.Heyne")
            .combExAuthors("Backer")
            .nothingElse();

    assertName("Taraxacum oosterveldii Petrasiak & Johansen, unpublished", "Taraxacum oosterveldii")
            .species("Taraxacum", "oosterveldii")
            .combAuthors(null, "Petrasiak", "Johansen")
            .nomNote("unpublished")
            .manuscript()
            .nothingElse();
  }

  /**
   * https://github.com/gbif/portal-feedback/issues/3535
   */
  @Test
  public void noHybrids() throws Exception {
    assertName("Lepidodens similis Zhang F & Pan Z-X in Zhang, F, Pan, Z-X, Wu, J, Ding, Y-H, Yu, D-Y & Wang, B-X, 2016", "Lepidodens similis")
            .species("Lepidodens", "similis")
            .combAuthors("2016", "Zhang F", "Pan Z-X")
            .publishedIn("Zhang, F, Pan, Z-X, Wu, J, Ding, Y-H, Yu, D-Y & Wang, B-X, 2016")
            .code(ZOOLOGICAL)
            .nothingElse();
  }

  /**
   * https://github.com/gbif/checklistbank/issues/87
   */
  @Test
  public void nomRefs() throws Exception {
    assertName("Passiflora plumosa Feuillet & Cremers, Proceedings of the Koninklijke Nederlandse Akademie van Wetenschappen, Series C: Biological and Medical Sciences 87(3): 381, f. 2. 1984. Fig. 2I, J", "Passiflora plumosa")
            .species("Passiflora", "plumosa")
            .combAuthors(null, "Feuillet", "Cremers")
            .partial(", Proceedings of the Koninklijke Nederlandse Akademie van Wetenschappen, Series C: Biological and Medical Sciences 87(3): 381, f. 2. 1984. Fig. 2I, J")
            .warning(Warnings.NOMENCLATURAL_REFERENCE)
            .nothingElse();

    assertName("Passiflora jussieui Feuillet, Journal of the Botanical Research Institute of Texas 4(2): 611, f. 1. 2010. Figs 2E, F, 3E, F", "Passiflora jussieui")
            .species("Passiflora", "jussieui")
            .combAuthors(null, "Feuillet")
            .partial(", Journal of the Botanical Research Institute of Texas 4(2): 611, f. 1. 2010. Figs 2E, F, 3E, F")
            .warning(Warnings.NOMENCLATURAL_REFERENCE)
            .nothingElse();

    assertName("Passiflora eglandulosa J.M. MacDougal. Annals of the Missouri Botanical Garden 75: 1658-1662. figs 1, 2B, and 3. 1988. Figs 36-37", "Passiflora eglandulosa")
            .species("Passiflora", "eglandulosa")
            .combAuthors(null, "J.M.MacDougal")
            .partial(". Annals of the Missouri Botanical Garden 75: 1658-1662. figs 1, 2B, and 3. 1988. Figs 36-37")
            .warning(Warnings.NOMENCLATURAL_REFERENCE)
            .nothingElse();

    assertName("Passiflora eglandulosa J.M. MacDougal. Lingua franca de Missouri Botanical Garden 75: 1658-1662. figs 1, 2B, and 3. 1988. Figs 36-37", "Passiflora eglandulosa")
            .species("Passiflora", "eglandulosa")
            .combAuthors(null, "J.M.MacDougal")
            .partial(". Lingua franca de Missouri Botanical Garden 75: 1658-1662. figs 1, 2B, and 3. 1988. Figs 36-37")
            .warning(Warnings.NOMENCLATURAL_REFERENCE)
            .nothingElse();
  }


  /**
   * https://github.com/gbif/checklistbank/issues/87
   */
  @Test
  public void blacklisted() throws Exception {
    assertName("Abies null Hood", "Abies null")
            .species("Abies", "null")
            .combAuthors(null, "Hood")
            .doubtful()
            .warning(Warnings.NULL_EPITHET)
            .nothingElse();

    assertName("Unidentified unidentified Hood", "Unidentified unidentified")
            .species("Unidentified", "unidentified")
            .combAuthors(null, "Hood")
            .doubtful()
            .warning(Warnings.BLACKLISTED_EPITHET)
            .nothingElse();

    assertName("Abies unidentified", "Abies unidentified")
            .species("Abies", "unidentified")
            .doubtful()
            .warning(Warnings.BLACKLISTED_EPITHET)
            .nothingElse();

    assertName("Passiflora possible Müller", "Passiflora possible")
            .species("Passiflora", "possible")
            .combAuthors(null, "Müller")
            .doubtful()
            .warning(Warnings.BLACKLISTED_EPITHET)
            .nothingElse();

    // undetected nom rel
    // make sure we blacklist the shit epithet, as we have received such data in the past
    assertName("Passiflora eglandulosa J.M. MacDougal. Lingua shit de Missouri Botanical Garden 75. figs 1, 2B, and 3. 1988. Figs 36-37", "Passiflora eglandulosa shit")
            .infraSpecies("Passiflora", "eglandulosa", INFRASPECIFIC_NAME, "shit")
            .combAuthors(null, "de Missouri Botanical Garden")
            .doubtful()
            .partial("75.figs 1,2B,&3.1988.Figs 36-37")
            .warning(Warnings.BLACKLISTED_EPITHET)
            .nothingElse();
  }

  @Test
  public void species() throws Exception {
    assertName("Diodia teres Walter", "Diodia teres")
            .species("Diodia", "teres")
            .combAuthors(null, "Walter")
            .nothingElse();

    assertName("Dysponetus bulbosus Hartmann-Schroder 1982", "Dysponetus bulbosus")
            .species("Dysponetus", "bulbosus")
            .combAuthors("1982", "Hartmann-Schroder")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Zophosis persis (Chatanay 1914)", "Zophosis persis")
            .species("Zophosis", "persis")
            .basAuthors("1914", "Chatanay")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Abies alba Mill.", "Abies alba")
            .species("Abies", "alba")
            .combAuthors(null, "Mill.")
            .nothingElse();

    assertName("Alstonia vieillardii Van Heurck & Müll.Arg.", "Alstonia vieillardii")
            .species("Alstonia", "vieillardii")
            .combAuthors(null, "Van Heurck", "Müll.Arg.")
            .nothingElse();

    assertName("Angiopteris d'urvilleana de Vriese", "Angiopteris d'urvilleana")
            .species("Angiopteris", "d'urvilleana")
            .combAuthors(null, "de Vriese")
            .nothingElse();

    assertName("Agrostis hyemalis (Walter) Britton, Sterns, & Poggenb.", "Agrostis hyemalis")
            .species("Agrostis", "hyemalis")
            .combAuthors(null, "Britton", "Sterns", "Poggenb.")
            .basAuthors(null, "Walter")
            .code(BOTANICAL)
            .nothingElse();
  }

  @Test
  public void speciesWithSubgenus() throws Exception {
    assertName("Passalus (Pertinax) gaboi Jiménez-Maxim & Reyes, 2022", "Passalus gaboi")
            .species("Passalus", "Pertinax", "gaboi")
            .combAuthors("2022", "Jiménez-Maxim", "Reyes")
            .code(ZOOLOGICAL)
            .nothingElse();
  }

  @Test
  public void specialEpithets() throws Exception {
    assertName("Gracillaria v-flava Haworth, 1828", "Gracillaria v-flava")
            .species("Gracillaria", "v-flava")
            .combAuthors("1828", "Haworth")
            .code(ZOOLOGICAL)
            .nothingElse();
  }

  @Test
  public void capitalAuthors() throws Exception {
    assertName("Anniella nigra FISCHER 1885", "Anniella nigra")
            .species("Anniella", "nigra")
            .combAuthors("1885", "Fischer")
            .code(ZOOLOGICAL)
            .nothingElse();
  }

  @Test
  public void capitalMonomial() throws Exception {
    // https://github.com/CatalogueOfLife/checklistbank/issues/1262
    assertName("XENACOELOMORPHA", "Xenacoelomorpha")
            .monomial("Xenacoelomorpha")
            .nothingElse();
  }

  @Test
  public void infraSpecies() throws Exception {
    assertName("Poa pratensis subsp. anceps (Gaudin) Dumort., 1824", Rank.SPECIES, "Poa pratensis subsp. anceps")
            .infraSpecies("Poa", "pratensis", Rank.SUBSPECIES, "anceps")
            .basAuthors(null, "Gaudin")
            .combAuthors("1824", "Dumort.")
            .code(BOTANICAL)
            .warning(Warnings.SUBSPECIES_ASSIGNED)
            .nothingElse();

    assertName("Abies alba ssp. alpina Mill.", "Abies alba alpina")
            .infraSpecies("Abies", "alba", SUBSPECIES, "alpina")
            .combAuthors(null, "Mill.")
            .nothingElse();

    assertName("Festuca ovina L. subvar. gracilis Hackel", "Festuca ovina subvar. gracilis")
            .infraSpecies("Festuca", "ovina", SUBVARIETY, "gracilis")
            .combAuthors(null, "Hackel")
            .nothingElse();

    assertName("Pseudomonas syringae pv. aceris (Ark, 1939) Young, Dye & Wilkie, 1978", "Pseudomonas syringae pv. aceris")
            .infraSpecies("Pseudomonas", "syringae", PATHOVAR, "aceris")
            .combAuthors("1978", "Young", "Dye", "Wilkie")
            .basAuthors("1939", "Ark")
            .code(BACTERIAL)
            .nothingElse();

    assertName("Agaricus compactus sarcocephalus (Fr.) Fr. ", "Agaricus compactus sarcocephalus")
            .infraSpecies("Agaricus", "compactus", INFRASPECIFIC_NAME, "sarcocephalus")
            .combAuthors(null, "Fr.")
            .basAuthors(null, "Fr.")
            .code(BOTANICAL)
            .nothingElse();

    assertName("Baccharis microphylla Kunth var. rhomboidea Wedd. ex Sch. Bip. (nom. nud.)", "Baccharis microphylla var. rhomboidea")
            .infraSpecies("Baccharis", "microphylla", VARIETY, "rhomboidea")
            .combAuthors(null, "Sch.Bip.")
            .combExAuthors("Wedd.")
            .nomNote("nom.nud.")
            .nothingElse();

    assertName("Achillea millefolium subsp. pallidotegula B. Boivin var. pallidotegula", "Achillea millefolium var. pallidotegula")
            .infraSpecies("Achillea", "millefolium", Rank.VARIETY, "pallidotegula")
            .warning("Intermediate classification removed: subsp.pallidotegula B.Boivin ")
            .nothingElse();

    assertName("Achillea millefolium var. pallidotegula", Rank.INFRASPECIFIC_NAME, "Achillea millefolium var. pallidotegula")
            .infraSpecies("Achillea", "millefolium", Rank.VARIETY, "pallidotegula")
            .nothingElse();

    assertName("Monograptus turriculatus mut. minor", MUTATIO, "Monograptus turriculatus mut. minor")
            .infraSpecies("Monograptus", "turriculatus", MUTATIO, "minor")
            .code(ZOOLOGICAL)
            .nothingElse();
  }

  @Test
  public void exAuthors() throws Exception {
    assertName("Acacia truncata (Burm. f.) hort. ex Hoffmanns.", "Acacia truncata")
            .species("Acacia", "truncata")
            .basAuthors(null, "Burm.f.")
            .combExAuthors("hort.")
            .combAuthors(null, "Hoffmanns.")
            .code(BOTANICAL)
            .nothingElse();

    // In botany (99% of ex author use) the ex author comes first, see https://en.wikipedia.org/wiki/Author_citation_(botany)#Usage_of_the_term_.22ex.22
    assertName("Gymnocalycium eurypleurumn Plesn¡k ex F.Ritter", "Gymnocalycium eurypleurumn")
            .species("Gymnocalycium", "eurypleurumn")
            .combAuthors(null, "F.Ritter")
            .combExAuthors("Plesnik")
            .warning(Warnings.HOMOGLYHPS) // the ¡ in Plesn¡k is not a regular i
            .nothingElse();

    assertName("Abutilon bastardioides Baker f. ex Rose", "Abutilon bastardioides")
            .species("Abutilon", "bastardioides")
            .combAuthors(null, "Rose")
            .combExAuthors("Baker f.")
            .nothingElse();

    assertName("Baccharis microphylla Kunth var. rhomboidea Wedd. ex Sch. Bip. (nom. nud.)", "Baccharis microphylla var. rhomboidea")
            .infraSpecies("Baccharis", "microphylla", VARIETY, "rhomboidea")
            .combAuthors(null, "Sch.Bip.")
            .combExAuthors("Wedd.")
            .nomNote("nom.nud.")
            .nothingElse();

    assertName("Abies brevifolia hort. ex Dallim.", "Abies brevifolia")
            .species("Abies", "brevifolia")
            .combExAuthors("hort.")
            .combAuthors(null, "Dallim.")
            .nothingElse();

    assertName("Abies brevifolia cv. ex Dallim.", "Abies brevifolia")
            .species("Abies", "brevifolia")
            .combExAuthors("hort.")
            .combAuthors(null, "Dallim.")
            .nothingElse();

    assertName("Abutilon ×hybridum cv. ex Voss", "Abutilon × hybridum")
            .species("Abutilon", "hybridum")
            .notho(SPECIFIC)
            .combExAuthors("hort.")
            .combAuthors(null, "Voss")
            .nothingElse();

    // "Abutilon bastardioides Baker f. ex Rose"
    // "Aukuba ex Koehne 'Thunb'   "
    // "Crepinella subgen. Marchal ex Oliver  "
    // "Echinocereus sect. Triglochidiata ex Bravo"
    // "Hadrolaelia sect. Sophronitis ex Chiron & V.P.Castro"
  }

  @Test
  public void fourPartedNames() throws Exception {
    assertName("Poa pratensis kewensis primula (L.) Rouy, 1913", "Poa pratensis primula")
            .infraSpecies("Poa", "pratensis", INFRASUBSPECIFIC_NAME, "primula")
            .combAuthors("1913", "Rouy")
            .basAuthors(null, "L.")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Bombus sichelii alticola latofasciatus", "Bombus sichelii latofasciatus")
            .infraSpecies("Bombus", "sichelii", INFRASUBSPECIFIC_NAME, "latofasciatus")
            .nothingElse();

    assertName("Acipenser gueldenstaedti colchicus natio danubicus Movchan, 1967", "Acipenser gueldenstaedti natio danubicus")
            .infraSpecies("Acipenser", "gueldenstaedti", NATIO, "danubicus")
            .combAuthors("1967", "Movchan")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Cymbella cistula var. sinus regis", "Cymbella cistula var. sinus")
            .infraSpecies("Cymbella", "cistula", VARIETY, "sinus")
            .partial("regis")
            .nothingElse();
  }

  @Test
  public void monomial() throws Exception {
    assertName("Animalia", "Animalia")
            .monomial("Animalia")
            .nothingElse();
    assertName("Polychaeta", "Polychaeta")
            .monomial("Polychaeta")
            .nothingElse();
    assertName("Chrysopetalidae", "Chrysopetalidae")
            .monomial("Chrysopetalidae")
            .nothingElse();
    assertName("Chrysopetalidae", null, ZOOLOGICAL, "Chrysopetalidae")
            .monomial("Chrysopetalidae", Rank.FAMILY)
            .code(ZOOLOGICAL)
            .nothingElse();
    assertName("Acripeza Guérin-Ménéville 1838", "Acripeza")
            .monomial("Acripeza")
            .combAuthors("1838", "Guérin-Ménéville")
            .code(NomCode.ZOOLOGICAL)
            .nothingElse();
    // https://github.com/gbif/name-parser/issues/98
    assertName("Salmonidae Jarocki or Schinz, 1822", "Salmonidae")
            .monomial("Salmonidae")
            .combAuthors("1822", "Jarocki or Schinz")
            .code(NomCode.ZOOLOGICAL)
            .nothingElse();
  }

  @Test
  public void inReferences() throws Exception {

    assertName("Amathia tricornis Busk ms in Chimonides, 1987", "Amathia tricornis")
            .species("Amathia", "tricornis")
            .combAuthors("1987", "Busk")
            .publishedIn("Chimonides, 1987")
            .nomNote("ms")
            .manuscript()
            .nothingElse();

    assertName("Xolisma turquini Small apud Britton & Wilson", "Xolisma turquini")
            .species("Xolisma", "turquini")
            .combAuthors(null, "Small")
            .publishedIn("Britton & Wilson")
            .nothingElse();

    assertName("Negundo aceroides var. violaceum G.Kirchn. in Petzold & G.Kirchn.", "Negundo aceroides var. violaceum")
            .infraSpecies("Negundo", "aceroides", Rank.VARIETY, "violaceum")
            .combAuthors(null, "G.Kirchn.")
            .publishedIn("Petzold & G.Kirchn.")
            .nothingElse();

    assertName("Abies denheyeri Eghbalian, Khanjani and Ueckermann in Eghbalian, Khanjani & Ueckermann, 2017", "Abies denheyeri")
            .species("Abies", "denheyeri")
            .combAuthors("2017", "Eghbalian", "Khanjani", "Ueckermann")
            .publishedIn("Eghbalian, Khanjani & Ueckermann, 2017")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Mica Budde-Lund in Voeltzkow, 1908", "Mica")
            .monomial("Mica")
            .combAuthors("1908", "Budde-Lund")
            .publishedIn("Voeltzkow, 1908")
            .code(NomCode.ZOOLOGICAL)
            .nothingElse();

  }

  @Test
  public void supraGenericIPNI() throws Exception {
    assertName("Poaceae subtrib. Scolochloinae Soreng", "Scolochloinae")
            .monomial("Scolochloinae", SUBTRIBE)
            .combAuthors(null, "Soreng")
            .nothingElse();

    assertName("subtrib. Scolochloinae Soreng", "Scolochloinae")
            .monomial("Scolochloinae", SUBTRIBE)
            .combAuthors(null, "Soreng")
            .nothingElse();
  }

  @Test
  public void infraGeneric() throws Exception {
    // default to botanical ranks for sections
    assertName("Pinus suprasect. Taeda", "Pinus supersect. Taeda")
            .infraGeneric("Pinus", SUPERSECTION_BOTANY, "Taeda")
            .code(BOTANICAL)
            .nothingElse();

    // with zoological code it is an impossible name! The zoological section is above family rank and cannot be enclosed in a genus
    assertName("Pinus suprasect. Taeda", ZOOLOGICAL, "Pinus supersect. Taeda")
            .infraGeneric("Pinus", SUPERSECTION_BOTANY, "Taeda")
            .code(BOTANICAL)
            .warning(Warnings.CODE_MISMATCH)
            .nothingElse();

    // result in the zoological rank equivalent if the code is given!
    assertName("sect. Taeda", ZOOLOGICAL, "Taeda")
            .monomial("Taeda", SECTION_ZOOLOGY)
            .code(ZOOLOGICAL)
            .nothingElse();

    // IPNI notho ranks: https://github.com/gbif/name-parser/issues/15
    assertName("Aeonium nothosect. Leugalonium", "Aeonium nothosect. Leugalonium")
            .infraGeneric("Aeonium", SECTION_BOTANY, "Leugalonium")
            .notho(NamePart.INFRAGENERIC)
            .code(BOTANICAL)
            .nothingElse();

    assertName("Narcissus nothoser. Dubizettae", "Narcissus nothoser. Dubizettae")
            .infraGeneric("Narcissus", SERIES, "Dubizettae")
            .notho(NamePart.INFRAGENERIC)
            .code(NomCode.BOTANICAL)
            .nothingElse();

    assertName("Serapias nothosubsect. Pladiopetalae", "Serapias nothosubsect. Pladiopetalae")
            .infraGeneric("Serapias", SUBSECTION_BOTANY, "Pladiopetalae")
            .notho(NamePart.INFRAGENERIC)
            .code(NomCode.BOTANICAL)
            .nothingElse();

    assertName("Rubus nothosubgen. Cylarubus", "Rubus nothosubgen. Cylarubus")
            .infraGeneric("Rubus", SUBGENUS, "Cylarubus")
            .notho(NamePart.INFRAGENERIC)
            .nothingElse();

    assertName("Arrhoges (Antarctohoges)", SUBGENUS, "Arrhoges subgen. Antarctohoges")
            .infraGeneric("Arrhoges", SUBGENUS, "Antarctohoges")
            .nothingElse();

    assertName("Polygonum", Rank.SUBGENUS, "subgen. Polygonum")
            .infraGeneric(null, Rank.SUBGENUS, "Polygonum")
            .nothingElse();

    assertName("subgen. Trematostoma Sacc.", "subgen. Trematostoma")
            .infraGeneric(null, SUBGENUS, "Trematostoma")
            .combAuthors(null, "Sacc.")
            .nothingElse();

    assertName("Echinocereus sect. Triglochidiata Bravo", "Echinocereus sect. Triglochidiata")
            .infraGeneric("Echinocereus", SECTION_BOTANY, "Triglochidiata")
            .combAuthors(null, "Bravo")
            .code(BOTANICAL)
            .nothingElse();

    assertName("Zignoella subgen. Trematostoma Sacc.", "Zignoella subgen. Trematostoma")
            .infraGeneric("Zignoella", SUBGENUS, "Trematostoma")
            .combAuthors(null, "Sacc.")
            .nothingElse();

    assertName("Polygonum subgen. Bistorta (L.) Zernov", "Polygonum subgen. Bistorta")
            .infraGeneric("Polygonum", SUBGENUS, "Bistorta")
            .combAuthors(null, "Zernov")
            .basAuthors(null, "L.")
            .code(BOTANICAL)
            .nothingElse();

    assertName("Arrhoges (Antarctohoges)", "Arrhoges")
            .monomial("Arrhoges")
            .basAuthors(null, "Antarctohoges")
            .code(NomCode.ZOOLOGICAL)
            .nothingElse();

    assertName("Festuca subg. Schedonorus (P. Beauv. ) Peterm.", "Festuca subgen. Schedonorus")
            .infraGeneric("Festuca", SUBGENUS, "Schedonorus")
            .combAuthors(null, "Peterm.")
            .basAuthors(null, "P.Beauv.")
            .code(NomCode.BOTANICAL)
            .nothingElse();

    assertName("Catapodium subg.Agropyropsis  Trab.", "Catapodium subgen. Agropyropsis")
            .infraGeneric("Catapodium", SUBGENUS, "Agropyropsis")
            .combAuthors(null, "Trab.")
            .nothingElse();

    assertName(" Gnaphalium subg. Laphangium Hilliard & B. L. Burtt", "Gnaphalium subgen. Laphangium")
            .infraGeneric("Gnaphalium", SUBGENUS, "Laphangium")
            .combAuthors(null, "Hilliard", "B.L.Burtt")
            .nothingElse();

    assertName("Woodsiaceae (Hooker) Herter", "Woodsiaceae")
            .monomial("Woodsiaceae", FAMILY)
            .combAuthors(null, "Herter")
            .basAuthors(null, "Hooker")
            .code(NomCode.BOTANICAL)
            .nothingElse();
  }

  @Test
  public void notNames() throws Exception {
    assertName("Diatrypella favacea var. favacea (Fr.) Ces. & De Not.", "Diatrypella favacea var. favacea")
            .infraSpecies("Diatrypella", "favacea", VARIETY, "favacea")
            .combAuthors(null, "Ces.", "De Not.")
            .basAuthors(null, "Fr.")
            .code(BOTANICAL)
            .nothingElse();

    assertName("Protoventuria rosae (De Not.) Berl. & Sacc.", "Protoventuria rosae")
            .species("Protoventuria", "rosae")
            .combAuthors(null, "Berl.", "Sacc.")
            .basAuthors(null, "De Not.")
            .code(BOTANICAL)
            .nothingElse();

    assertName("Hormospora De Not.", "Hormospora")
            .monomial("Hormospora")
            .combAuthors(null, "De Not.")
            .nothingElse();
  }

  /**
   * http://dev.gbif.org/issues/browse/POR-2459
   */
  @Test
  public void unparsablePlaceholder() throws Exception {
    assertUnparsable("Mollusca not assigned", PLACEHOLDER);
    assertUnparsable("[unassigned] Cladobranchia", PLACEHOLDER);
    assertUnparsable("Biota incertae sedis", PLACEHOLDER);
    assertUnparsable("Unaccepted", PLACEHOLDER);
    assertUnparsable("uncultured Verrucomicrobiales bacterium", PLACEHOLDER);
    assertUnparsable("uncultured Vibrio sp.", PLACEHOLDER);
    assertUnparsable("uncultured virus", PLACEHOLDER);
    // ITIS placeholders:
    assertUnparsable("Temp dummy name", PLACEHOLDER);
    // https://de.wikipedia.org/wiki/N._N.
    assertUnparsable("N.N.", PLACEHOLDER);
    assertUnparsable("N.N. (e.g., Breoghania)", PLACEHOLDER);
    assertUnparsable("N.N. (Chitinivorax)", PLACEHOLDER);
    assertUnparsable("N.n. (Chitinivorax)", PLACEHOLDER);

    // https://github.com/gbif/checklistbank/issues/48
    assertUnparsable("Gen.nov. sp.nov.", NO_NAME);
    assertUnparsable("Gen.nov.", NO_NAME);
  }

  @Test
  public void placeholder() throws Exception {
    assertName("denheyeri Eghbalian, Khanjani and Ueckermann in Eghbalian, Khanjani & Ueckermann, 2017", "? denheyeri")
            .species("?", "denheyeri")
            .combAuthors("2017", "Eghbalian", "Khanjani", "Ueckermann")
            .type(PLACEHOLDER)
            .publishedIn("Eghbalian, Khanjani & Ueckermann, 2017")
            .code(NomCode.ZOOLOGICAL)
            .warning(Warnings.MISSING_GENUS)
            .nothingElse();

    assertName("\"? gryphoidis", "? gryphoidis")
            .species("?", "gryphoidis")
            .type(PLACEHOLDER)
            .nothingElse();

    assertName("\"? gryphoidis (Bourguignat 1870) Schoepf. 1909", "? gryphoidis")
            .species("?", "gryphoidis")
            .basAuthors("1870", "Bourguignat")
            .combAuthors("1909", "Schoepf.")
            .type(PLACEHOLDER)
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Missing penchinati Bourguignat, 1870", "? penchinati")
            .species("?", "penchinati")
            .combAuthors("1870", "Bourguignat")
            .type(PLACEHOLDER)
            .code(ZOOLOGICAL)
            .nothingElse();
  }

  @Test
  public void sanctioned() throws Exception {
    // sanctioning authors not supported
    // https://github.com/GlobalNamesArchitecture/gnparser/issues/409
    assertName("Boletus versicolor L. : Fr.", "Boletus versicolor")
            .species("Boletus", "versicolor")
            .combAuthors(null, "L.")
            .sanctAuthor("Fr.")
            .nothingElse();

    assertName("Agaricus compactus sarcocephalus (Fr. : Fr.) Fr. ", "Agaricus compactus sarcocephalus")
            .infraSpecies("Agaricus", "compactus", INFRASPECIFIC_NAME, "sarcocephalus")
            .combAuthors(null, "Fr.")
            .basAuthors(null, "Fr.")
            .code(BOTANICAL)
            .nothingElse();

    assertName("Agaricus compactus sarcocephalus (Fr. : Fr.) Fr. ", "Agaricus compactus sarcocephalus")
            .infraSpecies("Agaricus", "compactus", INFRASPECIFIC_NAME, "sarcocephalus")
            .combAuthors(null, "Fr.")
            .basAuthors(null, "Fr.")
            .code(BOTANICAL)
            .nothingElse();
  }

  @Test
  public void nothotaxa() throws Exception {
    // https://github.com/GlobalNamesArchitecture/gnparser/issues/410
    assertName("Iris germanica nothovar. florentina", "Iris germanica nothovar. florentina")
            .infraSpecies("Iris", "germanica", VARIETY, "florentina")
            .notho(INFRASPECIFIC)
            .nothingElse();

    assertName("Abies alba var. ×alpina L.", "Abies alba nothovar. alpina")
            .infraSpecies("Abies", "alba", VARIETY, "alpina")
            .notho(INFRASPECIFIC)
            .combAuthors(null, "L.")
            .nothingElse();
  }

  @Test
  public void aggregates() throws Exception {
    // see https://github.com/gbif/checklistbank/issues/69
    assertName("Achillea millefolium agg. L.", "Achillea millefolium")
            .binomial("Achillea", null, "millefolium", Rank.SPECIES_AGGREGATE)
            .combAuthors(null, "L.")
            .nothingElse();

    assertName("Strumigenys koningsbergeri-group", "Strumigenys koningsbergeri")
            .binomial("Strumigenys", null, "koningsbergeri", Rank.SPECIES_AGGREGATE)
            .nothingElse();

    assertName("Selenophorus parumpunctatus species group", "Selenophorus parumpunctatus")
            .binomial("Selenophorus", null, "parumpunctatus", Rank.SPECIES_AGGREGATE)
            .nothingElse();

    assertName("Monomorium monomorium group", "Monomorium monomorium")
            .binomial("Monomorium", null, "monomorium", Rank.SPECIES_AGGREGATE)
            .nothingElse();
  }


  /**
   * BOLD NCBI have lots of these aggregates.
   * As it can be on any level we do not want them to be parsed properly with rank=SpeciesAggregate
   */
  @Test
  public void unparsablePlaceholders() throws Exception {
    assertUnparsable("Iteaphila-group", PLACEHOLDER);
    assertUnparsable("Bartonella group", PLACEHOLDER);
  }

  @Test
  public void rankExplicit() throws Exception {
    assertName("Achillea millefolium L.", Rank.SPECIES, "Achillea millefolium")
            .species("Achillea", "millefolium")
            .combAuthors(null, "L.")
            .nothingElse();

    assertName("Achillea millefolium L.", Rank.SPECIES_AGGREGATE, "Achillea millefolium")
            .binomial("Achillea", null, "millefolium", Rank.SPECIES_AGGREGATE)
            .combAuthors(null, "L.")
            .nothingElse();

    // higher ranks should be marked as doubtful
    for (Rank r : Rank.values()) {
      if (r.otherOrUnranked() || r.isSpeciesOrBelow() || r.getMajorRank()==DIVISION) continue;
      NameAssertion ass = assertName("Achillea millefolium L.", r, "Achillea millefolium")
              .binomial("Achillea", null, "millefolium", r)
              .combAuthors(null, "L.")
              .type(INFORMAL)
              .doubtful();
      if (r.isRestrictedToCode() != null) {
        ass.code(r.isRestrictedToCode());
      }
      ass.warning(Warnings.RANK_MISMATCH);
      ass.nothingElse();
    }
  }

  @Test
  public void candidatus() throws Exception {
    assertName("\"Candidatus Endowatersipora\" Anderson and Haygood, 2007", "\"Candidatus Endowatersipora\"")
            .monomial("Endowatersipora")
            .candidatus()
            .combAuthors("2007", "Anderson", "Haygood")
            .nothingElse();

    assertName("Candidatus Phytoplasma allocasuarinae", "\"Candidatus Phytoplasma allocasuarinae\"")
            .species("Phytoplasma", "allocasuarinae")
            .candidatus()
            .nothingElse();

    assertName("Ca. Phytoplasma allocasuarinae", "\"Candidatus Phytoplasma allocasuarinae\"")
            .species("Phytoplasma", "allocasuarinae")
            .candidatus()
            .nothingElse();

    assertName("Ca. Phytoplasma", "\"Candidatus Phytoplasma\"")
            .monomial("Phytoplasma")
            .candidatus()
            .nothingElse();

    assertName("'Candidatus Nicolleia'", "\"Candidatus Nicolleia\"")
            .monomial("Nicolleia")
            .candidatus()
            .nothingElse();

    assertName("\"Candidatus Riegeria\" Gruber-Vodicka et al., 2011", "\"Candidatus Riegeria\"")
            .monomial("Riegeria")
            .combAuthors("2011", "Gruber-Vodicka", "al.")
            .candidatus()
            .nothingElse();

    assertName("Candidatus Endobugula", "\"Candidatus Endobugula\"")
            .monomial("Endobugula")
            .candidatus()
            .nothingElse();

    // not candidate names
    assertName("Centropogon candidatus Lammers", "Centropogon candidatus")
            .species("Centropogon", "candidatus")
            .combAuthors(null, "Lammers")
            .nothingElse();
  }

  @Test
  public void oddFungiRanks() throws Exception {
    assertName("Cyphelium disseminatum ⍺ subsessile", "Cyphelium disseminatum subsessile")
            .infraSpecies("Cyphelium", "disseminatum", INFRASPECIFIC_NAME, "subsessile")
            .nothingElse();

    assertName("Capitularia fimbriata *** carpophora", "Capitularia fimbriata carpophora")
            .infraSpecies("Capitularia", "fimbriata", INFRASPECIFIC_NAME, "carpophora")
            .nothingElse();

    assertName("Cyphelium disseminatum c subsessile", "Cyphelium disseminatum subsessile")
            .infraSpecies("Cyphelium", "disseminatum", INFRASPECIFIC_NAME, "subsessile")
            .nothingElse();

    assertName("Cyphelium disseminatum g subsessile", "Cyphelium disseminatum subsessile")
            .infraSpecies("Cyphelium", "disseminatum", INFRASPECIFIC_NAME, "subsessile")
            .nothingElse();
  }

  @Test
  @Ignore
  public void oddFungiRanksUnsupported() throws Exception {
    assertName("Capitularia fimbriata ⍺ vulgaris 3 tubaeformis *** carpophora", "Capitularia fimbriata carpophora")
            .infraSpecies("Capitularia", "fimbriata", INFRASPECIFIC_NAME, "carpophora")
            .nothingElse();

    assertName("Capitularia pyxidata ß longipes H. carpophora Floerke", "Capitularia pyxidata carpophora")
            .infraSpecies("Capitularia", "pyxidata", INFRASPECIFIC_NAME, "carpophora")
            .nothingElse();
  }

  @Test
  @Ignore
  public void strains() throws Exception {
    assertName("Endobugula sp. JYr4", "Endobugula sp. JYr4")
            .species("Endobugula", null)
            .phrase("JYr4")
            .nothingElse();

    assertName("Lepidoptera sp. JGP0404", "Lepidoptera sp. JGP0404")
            .species("Lepidoptera", null)
            .phrase("JGP0404")
            .type(NameType.INFORMAL)
            .nothingElse();

    // avoid author & year to be accepted as strain
    assertName("Anniella nigra FISCHER 1885", "Anniella nigra")
            .species("Anniella", "nigra")
            .combAuthors("1885", "Fischer")
            .nothingElse();
  }

  @Test
  public void norwegianRadiolaria() throws Exception {
    assertName("Actinomma leptodermum longispinum Cortese & Bjørklund 1998", "Actinomma leptodermum longispinum")
            .infraSpecies("Actinomma", "leptodermum", INFRASPECIFIC_NAME, "longispinum")
            .combAuthors("1998", "Cortese", "Bjørklund")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Arachnosphaera dichotoma  Jørgensen, 1900", "Arachnosphaera dichotoma")
            .species("Arachnosphaera", "dichotoma")
            .combAuthors("1900", "Jørgensen")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Hexaconthium pachydermum forma legitime Cortese & Bjørklund 1998", "Hexaconthium pachydermum f. legitime")
            .infraSpecies("Hexaconthium", "pachydermum", FORM, "legitime")
            .combAuthors("1998", "Cortese", "Bjørklund")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Hexaconthium pachydermum form A Cortese & Bjørklund 1998", "Hexaconthium pachydermum f. A")
            .infraSpecies("Hexaconthium", "pachydermum", FORM, "A")
            .combAuthors("1998", "Cortese", "Bjørklund")
            .type(INFORMAL)
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Tripodiscium gephyristes  (Hülseman, 1963) BJ&KR-Atsdatabanken", "Tripodiscium gephyristes")
            .species("Tripodiscium", "gephyristes")
            .basAuthors("1963", "Hülseman")
            .combAuthors(null, "BJ", "KR-Atsdatabanken")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Protocystis xiphodon  (Haeckel, 1887), Borgert, 1901", "Protocystis xiphodon")
            .species("Protocystis", "xiphodon")
            .basAuthors("1887", "Haeckel")
            .combAuthors("1901", "Borgert")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Acrosphaera lappacea  (Haeckel, 1887) Takahashi, 1991", "Acrosphaera lappacea")
            .species("Acrosphaera", "lappacea")
            .basAuthors("1887", "Haeckel")
            .combAuthors("1991", "Takahashi")
            .code(ZOOLOGICAL)
            .nothingElse();
  }

  @Test
  public void cultivars() throws Exception {
    assertName("Abutilon 'Kentish Belle'", "Abutilon 'Kentish Belle'")
            .cultivar("Abutilon", "Kentish Belle")
            .nothingElse();


    assertName("Acer campestre L. cv. 'nanum'", "Acer campestre 'nanum'")
            .cultivar("Acer", "campestre", "nanum")
            .combAuthors(null, "L.")
            .nothingElse();

    assertName("Verpericola megasoma \"Dall\" Pils.", "Verpericola megasoma 'Dall'")
            .cultivar("Verpericola", "megasoma", "Dall")
            .combAuthors(null, "Pils.")
            .nothingElse();

    assertName("Abutilon 'Kentish Belle'", "Abutilon 'Kentish Belle'")
            .cultivar("Abutilon", "Kentish Belle")
            .nothingElse();

    assertName("Abutilon 'Nabob'", "Abutilon 'Nabob'")
            .cultivar("Abutilon", "Nabob")
            .nothingElse();

    assertName("Sorbus americana Marshall cv. 'Belmonte'", "Sorbus americana 'Belmonte'")
            .cultivar("Sorbus", "americana", "Belmonte")
            .combAuthors(null, "Marshall")
            .nothingElse();

    assertName("Sorbus hupehensis C.K.Schneid. cv. 'November pink'", "Sorbus hupehensis 'November pink'")
            .cultivar("Sorbus", "hupehensis", "November pink")
            .combAuthors(null, "C.K.Schneid.")
            .nothingElse();

    assertName("Symphoricarpos albus (L.) S.F.Blake cv. 'Turesson'", "Symphoricarpos albus 'Turesson'")
            .cultivar("Symphoricarpos", "albus", CULTIVAR, "Turesson")
            .basAuthors(null, "L.")
            .combAuthors(null, "S.F.Blake")
            .nothingElse();

    assertName("Symphoricarpos sp. cv. 'mother of pearl'", "Symphoricarpos 'mother of pearl'")
            .cultivar("Symphoricarpos", CULTIVAR, "mother of pearl")
            .nothingElse();

    assertName("Primula Border Auricula Group", "Primula Border Auricula Group")
            .cultivar("Primula", CULTIVAR_GROUP, "Border Auricula")
            .nothingElse();

    assertName("Rhododendron boothii Mishmiense Group", "Rhododendron boothii Mishmiense Group")
            .cultivar("Rhododendron", "boothii", CULTIVAR_GROUP, "Mishmiense")
            .nothingElse();

    assertName("Paphiopedilum Sorel grex", "Paphiopedilum Sorel gx")
            .cultivar("Paphiopedilum", GREX, "Sorel")
            .nothingElse();

    assertName("Cattleya Prince John gx", "Cattleya Prince John gx")
            .cultivar("Cattleya", GREX, "Prince John")
            .nothingElse();
  }

  @Test
  public void hybridFormulas() throws Exception {
    assertName("Polypodium  x vulgare nothosubsp. mantoniae (Rothm.) Schidlay", "Polypodium vulgare nothosubsp. mantoniae")
            .infraSpecies("Polypodium", "vulgare", SUBSPECIES, "mantoniae")
            .basAuthors(null, "Rothm.")
            .combAuthors(null, "Schidlay")
            .notho(INFRASPECIFIC)
            .code(NomCode.BOTANICAL)
            .nothingElse();

    assertHybridFormula("Asplenium rhizophyllum DC. x ruta-muraria E.L. Braun 1939");
    assertHybridFormula("Arthopyrenia hyalospora X Hydnellum scrobiculatum");
    assertHybridFormula("Arthopyrenia hyalospora (Banker) D. Hall X Hydnellum scrobiculatum D.E. Stuntz");
    assertHybridFormula("Arthopyrenia hyalospora × ? ");
    assertHybridFormula("Agrostis L. × Polypogon Desf. ");
    assertHybridFormula("Agrostis stolonifera L. × Polypogon monspeliensis (L.) Desf. ");
    assertHybridFormula("Asplenium rhizophyllum X A. ruta-muraria E.L. Braun 1939");
    assertHybridFormula("Asplenium rhizophyllum DC. x ruta-muraria E.L. Braun 1939");
    assertHybridFormula("Asplenium rhizophyllum x ruta-muraria");
    assertHybridFormula("Salix aurita L. × S. caprea L.");
    assertHybridFormula("Mentha aquatica L. × M. arvensis L. × M. spicata L.");
    assertHybridFormula("Polypodium vulgare subsp. prionodes (Asch.) Rothm. × subsp. vulgare");
    assertHybridFormula("Tilletia caries (Bjerk.) Tul. × T. foetida (Wallr.) Liro.");
    assertHybridFormula("Cirsium acaulon x arvense");
    assertHybridFormula("Juncus effusus × inflexus");
    assertHybridFormula("Symphytum caucasicum x uplandicum");
  }

  protected void assertHybridFormula(String name) throws InterruptedException {
    assertUnparsable(name, HYBRID_FORMULA);
  }

  @Test
  public void oTU() throws Exception {

    // https://github.com/gbif/name-parser/issues/74
    assertName("Desulfobacterota_B", "Desulfobacterota_B")
            .monomial("Desulfobacterota_B")
            .type(OTU)
            .nothingElse();

    assertName("UBA3054", "UBA3054")
            .monomial("UBA3054")
            .type(OTU)
            .nothingElse();

    assertName("F0040", "F0040")
            .monomial("F0040")
            .type(OTU)
            .nothingElse();

    assertName("AABM5-125-24", "AABM5-125-24")
            .monomial("AABM5-125-24")
            .type(OTU)
            .nothingElse();

    assertName("B130-G9", "B130-G9")
            .monomial("B130-G9")
            .type(OTU)
            .nothingElse();

    assertName("BMS3Abin14", "BMS3Abin14")
            .monomial("BMS3Abin14")
            .type(OTU)
            .nothingElse();

    assertName("4572-55", "4572-55")
            .monomial("4572-55")
            .type(OTU)
            .nothingElse();

    assertName("T1SED10-198M", "T1SED10-198M")
            .monomial("T1SED10-198M")
            .type(OTU)
            .nothingElse();

    assertName("BMS3Abin14", "BMS3Abin14")
            .monomial("BMS3Abin14")
            .type(OTU)
            .nothingElse();

    assertName("UBA11359_C", "UBA11359_C")
            .monomial("UBA11359_C")
            .type(OTU)
            .nothingElse();

    assertName("01-FULL-45-15b", "01-FULL-45-15b")
            .monomial("01-FULL-45-15b")
            .type(OTU)
            .nothingElse();

    assertName("E44-bin80", "E44-bin80")
            .monomial("E44-bin80")
            .type(OTU)
            .nothingElse();

    assertName("E2", "E2")
            .monomial("E2")
            .type(OTU)
            .nothingElse();

    assertName("9FT-COMBO-53-11", "9FT-COMBO-53-11")
            .monomial("9FT-COMBO-53-11")
            .type(OTU)
            .nothingElse();

    assertName("AqS3", "AqS3")
            .monomial("AqS3")
            .type(OTU)
            .nothingElse();

    assertName("Gp7-AA8", "Gp7-AA8")
            .monomial("Gp7-AA8")
            .type(OTU)
            .nothingElse();

    assertName("0-14-0-10-38-17 sp002774085", "0-14-0-10-38-17 sp002774085")
            .species("0-14-0-10-38-17", "sp002774085")
            .type(OTU)
            .nothingElse();

    assertName("01-FULL-45-15b sp001822655", "01-FULL-45-15b sp001822655")
            .species("01-FULL-45-15b", "sp001822655")
            .type(OTU)
            .nothingElse();

    assertName("18JY21-1 sp004344915", "18JY21-1 sp004344915")
            .species("18JY21-1", "sp004344915")
            .type(OTU)
            .nothingElse();

    // unparsable
    assertUnparsable("SH1508347.08FU", OTU);
    assertUnparsable("SH19186714.17FU", OTU);
    assertUnparsable("SH191814.08FU", OTU);
    assertUnparsable("SH191814.04FU", OTU);
    assertUnparsable("BOLD:ACW2100", OTU);
    assertUnparsable("BOLD:ACW2100", OTU);
    assertUnparsableName(" BOLD:ACW2100 ", UNRANKED, OTU, "BOLD:ACW2100");
    assertUnparsableName("Festuca sp. BOLD:ACW2100", UNRANKED, OTU, "BOLD:ACW2100");
    assertUnparsableName("sh460441.07fu", UNRANKED, OTU, "SH460441.07FU");

    // no OTU names
    assertName("Boldenaria", "Boldenaria")
            .monomial("Boldenaria")
            .nothingElse();

    assertName("Boldea", "Boldea")
            .monomial("Boldea")
            .nothingElse();

    assertName("Boldiaceae", "Boldiaceae")
            .monomial("Boldiaceae", Rank.FAMILY)
            .nothingElse();

    assertName("Boldea vulgaris", "Boldea vulgaris")
            .species("Boldea", "vulgaris")
            .nothingElse();
  }

  /**
   * http://dev.gbif.org/issues/browse/POR-2397
   * <p>
   * TODO: convert all test cases. Can the strain & manuscript name property be merged ???
   */
  @Test
  public void strainNames() throws Exception {
    assertName("Candidatus Liberibacter solanacearum", "\"Candidatus Liberibacter solanacearum\"")
            .species("Liberibacter", "solanacearum")
            .candidatus()
            .nothingElse();

    //assertName("Methylocystis sp. M6", "Methylocystis sp. M6")
    //    .species("Liberibacter", "solanacearum")
    //    .candidatus()
    //    .nothingElse();

    //assertStrain("", NameType.INFORMAL, "Methylocystis", null, null, Rank.SPECIES, "M6");
    //assertStrain("Advenella kashmirensis W13003", NameType.INFORMAL, "Advenella", "kashmirensis", null, null, "W13003");
    //assertStrain("Garra cf. dampaensis M23", NameType.INFORMAL, "Garra", "dampaensis", null, null, "M23");
    //assertStrain("Sphingobium lucknowense F2", NameType.INFORMAL, "Sphingobium", "lucknowense", null, null, "F2");
    //assertStrain("Pseudomonas syringae pv. atrofaciens LMG 5095T", NameType.INFORMAL, "Pseudomonas", "syringae", "atrofaciens", Rank.PATHOVAR, "LMG 5095T");
  }

  @Test
  public void hybridAlikeNames() throws Exception {
    assertName("Huaiyuanella Xing, Yan & Yin, 1984", "Huaiyuanella")
            .monomial("Huaiyuanella")
            .combAuthors("1984", "Xing", "Yan", "Yin")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Caveasphaera Xiao & Knoll, 2000", "Caveasphaera")
            .monomial("Caveasphaera")
            .combAuthors("2000", "Xiao", "Knoll")
            .code(ZOOLOGICAL)
            .nothingElse();
  }

  @Test
  public void alphaBetaThetaNames() throws Exception {
    assertName("Euchlanis dilatata β-larga", "Euchlanis dilatata β-larga")
            .infraSpecies("Euchlanis", "dilatata", INFRASPECIFIC_NAME, "β-larga")
            .nothingElse();

    assertName("Trianosperma ficifolia var. βrigida Cogn.", "Trianosperma ficifolia var. βrigida")
            .infraSpecies("Trianosperma", "ficifolia", VARIETY, "βrigida")
            .combAuthors(null, "Cogn.")
            .nothingElse();

    assertName("Agaricus collinitus β mucosus (Bull.) Fr.", "Agaricus collinitus mucosus")
            .infraSpecies("Agaricus", "collinitus", INFRASPECIFIC_NAME, "mucosus")
            .combAuthors(null, "Fr.")
            .basAuthors(null, "Bull.")
            .code(BOTANICAL)
            .nothingElse();

    // epithets being a single greek char not supported at this stage!
    //assertName("Cyclotus amethystinus var. β Guppy, 1868", "Cyclotus amethystinus var. β")
    //        .infraSpecies("Cyclotus", "amethystinus", VARIETY, "β")
    //        .combAuthors("1868", "Guppy")
    //        .nothingElse();
  }

  @Test
  public void hybridNames() throws Exception {
    assertName("+ Pyrocrataegus willei L.L.Daniel", "× Pyrocrataegus willei")
            .species("Pyrocrataegus", "willei")
            .combAuthors(null, "L.L.Daniel")
            .notho(NamePart.GENERIC)
            .nothingElse();

    assertName("×Pyrocrataegus willei L.L. Daniel", "× Pyrocrataegus willei")
            .species("Pyrocrataegus", "willei")
            .combAuthors(null, "L.L.Daniel")
            .notho(NamePart.GENERIC)
            .nothingElse();

    assertName(" × Pyrocrataegus willei  L. L. Daniel", "× Pyrocrataegus willei")
            .species("Pyrocrataegus", "willei")
            .combAuthors(null, "L.L.Daniel")
            .notho(NamePart.GENERIC)
            .nothingElse();

    assertName(" X Pyrocrataegus willei L. L. Daniel", "× Pyrocrataegus willei")
            .species("Pyrocrataegus", "willei")
            .combAuthors(null, "L.L.Daniel")
            .notho(NamePart.GENERIC)
            .nothingElse();

    assertName("Pyrocrataegus ×willei L. L. Daniel", "Pyrocrataegus × willei")
            .species("Pyrocrataegus", "willei")
            .combAuthors(null, "L.L.Daniel")
            .notho(NamePart.SPECIFIC)
            .nothingElse();

    assertName("Pyrocrataegus × willei L. L. Daniel", "Pyrocrataegus × willei")
            .species("Pyrocrataegus", "willei")
            .combAuthors(null, "L.L.Daniel")
            .notho(NamePart.SPECIFIC)
            .nothingElse();

    assertName("Pyrocrataegus x willei L. L. Daniel", "Pyrocrataegus × willei")
            .species("Pyrocrataegus", "willei")
            .combAuthors(null, "L.L.Daniel")
            .notho(NamePart.SPECIFIC)
            .nothingElse();

    assertName("Pyrocrataegus X willei L. L. Daniel", "Pyrocrataegus × willei")
            .species("Pyrocrataegus", "willei")
            .combAuthors(null, "L.L.Daniel")
            .notho(NamePart.SPECIFIC)
            .nothingElse();

    assertName("Pyrocrataegus willei ×libidi  L.L.Daniel", "Pyrocrataegus willei × libidi")
            .infraSpecies("Pyrocrataegus", "willei", INFRASPECIFIC_NAME, "libidi")
            .combAuthors(null, "L.L.Daniel")
            .notho(INFRASPECIFIC)
            .nothingElse();

    assertName("Pyrocrataegus willei nothosubsp. libidi  L.L.Daniel", "Pyrocrataegus willei nothosubsp. libidi")
            .infraSpecies("Pyrocrataegus", "willei", SUBSPECIES, "libidi")
            .combAuthors(null, "L.L.Daniel")
            .notho(INFRASPECIFIC)
            .nothingElse();

    assertName("+ Pyrocrataegus willei nothosubsp. libidi  L.L.Daniel", "Pyrocrataegus willei nothosubsp. libidi")
            .infraSpecies("Pyrocrataegus", "willei", SUBSPECIES, "libidi")
            .combAuthors(null, "L.L.Daniel")
            .notho(INFRASPECIFIC)
            .nothingElse();

    //TODO: impossible name. should this not be a generic hybrid as its the highest rank crossed?
    assertName("×Pyrocrataegus ×willei ×libidi L.L.Daniel", "Pyrocrataegus willei × libidi")
            .infraSpecies("Pyrocrataegus", "willei", INFRASPECIFIC_NAME, "libidi")
            .combAuthors(null, "L.L.Daniel")
            .notho(INFRASPECIFIC)
            .nothingElse();
  }

  @Test
  public void authorVariations() throws Exception {
    //Van den heede works only if given as separate authorship

    //assertName("Asplenium cyprium Viane & Van den heede", "Asplenium cyprium")
    //    .species("Asplenium", "cyprium")
    //    .combAuthors(null, "Viane", "Van den heede")
    //    .nothingElse();

    // bis and ter as author suffix
    // https://github.com/Sp2000/colplus-backend/issues/591
    assertName("Lagenophora queenslandica Jian Wang ter & A.R.Bean", "Lagenophora queenslandica")
            .species("Lagenophora", "queenslandica")
            .combAuthors(null, "Jian Wang ter", "A.R.Bean")
            .nothingElse();

    assertName("Abies arctica A.Murray bis", "Abies arctica")
            .species("Abies", "arctica")
            .combAuthors(null, "A.Murray bis")
            .nothingElse();

    assertName("Abies lowiana (Gordon) A.Murray bis", "Abies lowiana")
            .species("Abies", "lowiana")
            .basAuthors(null, "Gordon")
            .combAuthors(null, "A.Murray bis")
            .code(BOTANICAL)
            .nothingElse();

    assertName("Trappeindia Castellano, S.L. Mill., L. Singh bis & T.N. Lakh. 2012", "Trappeindia")
            .monomial("Trappeindia")
            .combAuthors("2012", "Castellano", "S.L.Mill.", "L.Singh bis", "T.N.Lakh.")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Trichosporon cutaneum (Beurm., Gougerot & Vaucher bis) M. Ota", "Trichosporon cutaneum")
            .species("Trichosporon", "cutaneum")
            .basAuthors(null, "Beurm.", "Gougerot", "Vaucher bis")
            .combAuthors(null, "M.Ota")
            .code(BOTANICAL)
            .nothingElse();

    // van der
    assertName("Megistocera tenuis (van der Wulp, 1885)", "Megistocera tenuis")
            .species("Megistocera", "tenuis")
            .basAuthors("1885", "van der Wulp")
            .code(ZOOLOGICAL)
            .nothingElse();

    // turkish chars
    assertName("Stachys marashica Ilçim, Çenet & Dadandi", "Stachys marashica")
            .species("Stachys", "marashica")
            .combAuthors(null, "Ilçim", "Çenet", "Dadandi")
            .nothingElse();

    assertName("Viola bocquetiana S. Yildirimli", "Viola bocquetiana")
            .species("Viola", "bocquetiana")
            .combAuthors(null, "S.Yildirimli")
            .nothingElse();

    assertName("Anatolidamnicola gloeri gloeri Şahin, Koca & Yildirim, 2012", "Anatolidamnicola gloeri gloeri")
            .infraSpecies("Anatolidamnicola", "gloeri", Rank.INFRASPECIFIC_NAME, "gloeri")
            .combAuthors("2012", "Şahin", "Koca", "Yildirim")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Modiola caroliniana L.f", "Modiola caroliniana")
            .species("Modiola", "caroliniana")
            .combAuthors(null, "L.f")
            .nothingElse();

    assertName("Modiola caroliniana (L.) G. Don filius", "Modiola caroliniana")
            .species("Modiola", "caroliniana")
            .basAuthors(null, "L.")
            .combAuthors(null, "G.Don filius")
            .code(NomCode.BOTANICAL)
            .nothingElse();

    assertName("Modiola caroliniana (L.) G. Don fil.", "Modiola caroliniana")
            .species("Modiola", "caroliniana")
            .basAuthors(null, "L.")
            .combAuthors(null, "G.Don fil.")
            .code(NomCode.BOTANICAL)
            .nothingElse();

    assertName("Cirsium creticum d'Urv.", "Cirsium creticum")
            .species("Cirsium", "creticum")
            .combAuthors(null, "d'Urv.")
            .nothingElse();

    // TODO: autonym authors are the species authors !!!
    assertName("Cirsium creticum d'Urv. subsp. creticum", "Cirsium creticum subsp. creticum")
            .infraSpecies("Cirsium", "creticum", SUBSPECIES, "creticum")
            //.combAuthors(null, "d'Urv.")
            .autonym()
            .code(NomCode.BOTANICAL)
            .nothingElse();

    assertName("Cirsium creticum Balsamo M Fregni E Tongiorgi P", "Cirsium creticum")
            .species("Cirsium", "creticum")
            .combAuthors(null, "M.Balsamo", "E.Fregni", "P.Tongiorgi")
            .nothingElse();

    assertName("Cirsium creticum Balsamo M Todaro MA", "Cirsium creticum")
            .species("Cirsium", "creticum")
            .combAuthors(null, "M.Balsamo", "M.A.Todaro")
            .nothingElse();

    assertName("Bolivina albatrossi Cushman Em. Sellier de Civrieux, 1976", "Bolivina albatrossi")
            .species("Bolivina", "albatrossi")
            .combAuthors("1976", "Cushman Em.Sellier de Civrieux")
            .code(ZOOLOGICAL)
            .nothingElse();

    // http://dev.gbif.org/issues/browse/POR-101
    assertName("Cribbia pendula la Croix & P.J.Cribb", "Cribbia pendula")
            .species("Cribbia", "pendula")
            .combAuthors(null, "la Croix", "P.J.Cribb")
            .nothingElse();

    assertName("Cribbia pendula le Croix & P.J.Cribb", "Cribbia pendula")
            .species("Cribbia", "pendula")
            .combAuthors(null, "le Croix", "P.J.Cribb")
            .nothingElse();

    assertName("Cribbia pendula de la Croix & le P.J.Cribb", "Cribbia pendula")
            .species("Cribbia", "pendula")
            .combAuthors(null, "de la Croix", "le P.J.Cribb")
            .nothingElse();

    assertName("Cribbia pendula Croix & de le P.J.Cribb", "Cribbia pendula")
            .species("Cribbia", "pendula")
            .combAuthors(null, "Croix", "de le P.J.Cribb")
            .nothingElse();

    assertName("Navicula ambigua f. craticularis Istv?nffi, 1898, 1897", "Navicula ambigua f. craticularis")
            .infraSpecies("Navicula", "ambigua", Rank.FORM, "craticularis")
            .combAuthors("1898", "Istvnffi")
            .doubtful()
            .code(ZOOLOGICAL)
            .warning(Warnings.QUESTION_MARKS_REMOVED)
            .nothingElse();

    assertName("Cestodiscus gemmifer F.S.Castracane degli Antelminelli", "Cestodiscus gemmifer")
            .species("Cestodiscus", "gemmifer")
            .combAuthors(null, "F.S.Castracane degli Antelminelli")
            .nothingElse();

    assertName("Hieracium scorzoneraefolium De la Soie", "Hieracium scorzoneraefolium")
            .species("Hieracium", "scorzoneraefolium")
            .combAuthors(null, "De la Soie")
            .nothingElse();

    assertName("Sepidium capricorne des Desbrochers des Loges, 1881", "Sepidium capricorne")
            .species("Sepidium", "capricorne")
            .combAuthors("1881", "des Desbrochers des Loges")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Sepidium capricorne Desbrochers des Loges, 1881", "Sepidium capricorne")
            .species("Sepidium", "capricorne")
            .combAuthors("1881", "Desbrochers des Loges")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Calycostylis aurantiaca Hort. ex Vilmorin", "Calycostylis aurantiaca")
            .species("Calycostylis", "aurantiaca")
            .combAuthors(null, "Vilmorin")
            .combExAuthors("hort.")
            .nothingElse();

    assertName("Pourretia magnispatha hortusa ex K. Koch", "Pourretia magnispatha")
            .species("Pourretia", "magnispatha")
            .combAuthors(null, "K.Koch")
            .combExAuthors("hort.")
            .nothingElse();

    assertName("Pitcairnia pruinosa hortus ex K. Koch", "Pitcairnia pruinosa")
            .species("Pitcairnia", "pruinosa")
            .combAuthors(null, "K.Koch")
            .combExAuthors("hort.")
            .nothingElse();

    assertName("Platycarpha glomerata (Thunberg) A.P.de Candolle", "Platycarpha glomerata")
            .species("Platycarpha", "glomerata")
            .basAuthors(null, "Thunberg")
            .combAuthors(null, "A.P.de Candolle")
            .code(NomCode.BOTANICAL)
            .nothingElse();

    assertName("Abies alba (Huguet del Villar) S. Rivas-Martínez, F. Fernández González & D. Sánchez-Mata", "Abies alba")
            .species("Abies", "alba")
            .basAuthors(null, "Huguet del Villar")
            .combAuthors(null, "S.Rivas-Martínez", "F.Fernández González", "D.Sánchez-Mata")
            .code(NomCode.BOTANICAL)
            .nothingElse();

    assertName("Sida kohautiana var. corchorifolia (H. da C. Monteiro Filho) H. da C. Monteiro Filho", "Sida kohautiana var. corchorifolia")
            .infraSpecies("Sida", "kohautiana", VARIETY, "corchorifolia")
            .basAuthors(null, "H.da C.Monteiro Filho")
            .combAuthors(null, "H.da C.Monteiro Filho")
            .code(NomCode.BOTANICAL)
            .nothingElse();
  }

  /**
   * https://github.com/gbif/name-parser/issues/49
   */
  @Test
  public void unparsableAuthors() throws Exception {
    assertAuthorship("Allemão")
            .combAuthors(null, "Allemão")
            .nothingElse();
    //assertAuthorship("ex DC.")
    //    .combAuthors(null, "DC.")
    //    .nothingElse();

    //TODO: https://github.com/gbif/name-parser/issues/49
  }

  @Test
  public void extinctNames() throws Exception {
    assertName("Sicyoniidae † Ortmann, 1898", "Sicyoniidae")
            .monomial("Sicyoniidae")
            .combAuthors("1898", "Ortmann")
            .extinct()
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("†Titanoptera", "Titanoptera")
            .monomial("Titanoptera")
            .extinct()
            .nothingElse();

    assertName("†††Titanoptera", "Titanoptera")
            .monomial("Titanoptera")
            .extinct()
            .nothingElse();

    assertName("† Tuarangiida MacKinnon, 1982", "Tuarangiida")
            .monomial("Tuarangiida")
            .combAuthors("1982", "MacKinnon")
            .extinct()
            .code(ZOOLOGICAL)
            .nothingElse();
  }

  /**
   * Simply test all names in names-with-authors.txt and make sure they parse without exception
   * and have an authorship!
   * This test does not verify if the parsed name was correct in all its pieces,
   * so only use this as a quick way to add names to tests.
   * <p>
   * Exceptional cases should better be tested in a test on its own!
   */
  @Test
  public void namesWithAuthorFile() throws Exception {
    for (String name : iterResource("names-with-authors.txt")) {
      ParsedName n = parser.parse(name, null);
      assertTrue(name, n.getState().isParsed());
      assertTrue(name, n.hasAuthorship());
      assertEquals(NameType.SCIENTIFIC, n.getType());
    }
  }

  /**
   * Test all names in doubtful.txt and make sure they parse without exception,
   * but have a doubtful flag set.
   * This test does not verify if the parsed name was correct in all its pieces,
   * so only use this as a quick way to add names to tests.
   * <p>
   * Exceptional cases should better be tested in a test on its own!
   */
  @Test
  public void doubtfulFile() throws Exception {
    for (String name : iterResource("doubtful.txt")) {
      ParsedName n = parser.parse(name, null);
      assertTrue(name, n.isDoubtful());
      assertTrue(name, n.getState().isParsed());
      assertTrue(name, n.getType().isParsable());
    }
  }

  /**
   * Test all names in unparsable.txt and makes sure they are not parsable.
   */
  @Test
  public void unparsableFile() throws Exception {
    for (String name : iterResource("unparsable.txt")) {
      try {
        parser.parse(name);
        fail("Expected " + name + " to be unparsable");
      } catch (UnparsableNameException ex) {
        assertEquals(name, ex.getName());
      }
    }
  }

  /**
   * Test all names in nonames.txt and makes sure they are NO_NAMEs.
   */
  @Test
  public void nonamesFile() throws Exception {
    for (String name : iterResource("nonames.txt")) {
      try {
        ParsedName pn = parser.parse(name);
        fail("Expected " + name + " to be unparsable");
      } catch (UnparsableNameException ex) {
        assertEquals("Bad name type for: "+name, NameType.NO_NAME, ex.getType());
        assertEquals(name, ex.getName());
      }
    }
  }

  /**
   * Test all hybrid formulas in hybrids.txt and makes sure they are HYBRID_FORMULAs.
   */
  @Test
  public void hybridsFile() throws Exception {
    for (String name : iterResource("hybrids.txt")) {
      try {
        ParsedName pn = parser.parse(name);
        fail("Expected " + name + " to be unparsable hybrid");
      } catch (UnparsableNameException ex) {
        assertEquals(NameType.HYBRID_FORMULA, ex.getType());
        assertEquals(name, ex.getName());
      }
    }
  }

  /**
   * Test all names in nonames.txt and makes sure they are NO_NAMEs.
   */
  @Test
  public void placeholderFile() throws Exception {
    for (String name : iterResource("placeholder.txt")) {
      try {
        ParsedName pn = parser.parse(name);
        fail("Expected " + name + " to be an unparsable placeholder");
      } catch (UnparsableNameException ex) {
        assertEquals(NameType.PLACEHOLDER, ex.getType());
        assertEquals(name, ex.getName());
      }
    }
  }

  @Test
  public void occNameFile() throws Exception {
    int currFail = 4;
    int fails = parseFile("occurrence-names.txt");
    if (fails > currFail) {
      fail("We are getting worse, not better. Currently failing: " + fails + ". Was passing:" + currFail);
    }
  }

  /**
   * Parse all verbatim GBIF checklist names to spot room for improvements
   */
  @Test
  @Ignore
  public void gbifFile() throws Exception {
    parseFile("gbif-verbatim-names.txt");
  }

  /**
   * @return number of failed names
   */
  private int parseFile(String resourceName) throws Exception {
    int parseFails = 0;
    int counter = 0;
    long start = System.currentTimeMillis();
    for (String name : iterResource(resourceName)) {
      counter++;
      if (counter % 100000 == 0) {
        long end = System.currentTimeMillis();
        LOG.info("{} names tested, {} failed", counter, parseFails);
        LOG.info("Total time {}ms, average per name {}", (end - start), (((double) end - start) / counter));
      }
      try {
        ParsedName pn = parser.parse(name);
        if (pn.getState() != ParsedName.State.COMPLETE) {
          LOG.debug("{} {}", pn.getState(), name);
        }
      } catch (UnparsableNameException ex) {
        if (ex.getType().isParsable() || ex.getType() == NO_NAME) {
          parseFails++;
          LOG.warn("{}: {}", ex.getType(), name);
        }
      }
    }
    long end = System.currentTimeMillis();
    LOG.info("{} names tested, {} failed", counter, parseFails);
    LOG.info("Total time {}ms, average per name {}", (end - start), (((double) end - start) / counter));
    return parseFails;
  }

  /**
   * Converts lines of a classpath resource that are not empty or are comments starting with #
   * into a simple string iterable
   */
  private Iterable<String> iterResource(String resource) throws UnsupportedEncodingException {
    LineIterator iter = new LineIterator(resourceReader(resource));
    return Iterables.filter(() -> iter,
            line -> line != null && !line.trim().isEmpty() && !line.startsWith("#")
    );
  }

  /**
   * Expect empty unparsable results for nothing or whitespace
   */
  @Test
  public void empty() throws Exception {
    assertNoName(null);
    assertNoName("");
    assertNoName(" ");
    assertNoName("\t");
    assertNoName("\n");
    assertNoName("\t\n");
    assertNoName("\"");
    assertNoName("'");
  }

  /**
   * Avoid nPEs and other exceptions for very short non names and other extremes found in occurrences.
   */
  @Test
  public void avoidNPE() throws Exception {
    assertNoName("\\");
    assertNoName(".");
    assertNoName("@");
    assertNoName("&nbsp;");
    assertNoName("X");
    assertNoName("a");
    assertNoName("-,.#");
    assertNoName(" .");
  }

  @Test
  public void informal() throws Exception {
    assertName("Trisulcus aff. nana  (Popofsky, 1913), Petrushevskaya, 1971", "Trisulcus aff. nana")
            .species("Trisulcus", "nana")
            .basAuthors("1913", "Popofsky")
            .combAuthors("1971", "Petrushevskaya")
            .type(INFORMAL)
            .qualifiers(SPECIFIC, "aff.")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Cerapachys mayeri cf. var. brachynodus", "Cerapachys mayeri cf. var. brachynodus")
            .infraSpecies("Cerapachys", "mayeri", VARIETY, "brachynodus")
            .type(INFORMAL)
            .qualifiers(INFRASPECIFIC, "cf.")
            .nothingElse();

    assertName("Solenopsis cf fugax", "Solenopsis cf. fugax")
            .species("Solenopsis", "fugax")
            .type(INFORMAL)
            .qualifiers(SPECIFIC, "cf.")
            .nothingElse();
  }

  @Test
  public void abbreviated() throws Exception {
    assertName("N. giraldo", "N. giraldo")
            .species("N.", "giraldo")
            .type(INFORMAL)
            .nothingElse();

    assertName("B.", "B.")
            .monomial("B.")
            .type(INFORMAL)
            .nothingElse();
  }

  @Test
  public void stringIndexOutOfBoundsException() throws Exception {
    parser.parse("Amblyomma americanum (Linnaeus, 1758)", null);
    parser.parse("Salix taiwanalpina var. chingshuishanensis (S.S.Ying) F.Y.Lu, C.H.Ou, Y.C.Chen, Y.S.Chi, K.C.Lu & Y.H.Tseng ", null);
    parser.parse("Salix taiwanalpina var. chingshuishanensis (S.S.Ying) F.Y.Lu, C.H.Ou, Y.C.Chen, Y.S.Chi, K.C.Lu & amp  Y.H.Tseng ", null);
    parser.parse("Salix morrisonicola var. takasagoalpina (Koidz.) F.Y.Lu, C.H.Ou, Y.C.Chen, Y.S.Chi, K.C.Lu & amp; Y.H.Tseng", null);
    parser.parse("Ficus ernanii Carauta, Pederneir., P.P.Souza, A.F.P.Machado, M.D.M.Vianna & amp; Romaniuc", null);
  }

  @Test
  public void nomNotes() throws Exception {

    assertName("Anthurium lanceum Engl., nom. illeg., non. A. lancea.", "Anthurium lanceum")
            .species("Anthurium", "lanceum")
            .combAuthors(null, "Engl.")
            .nomNote("nom.illeg.")
            .code(BOTANICAL)
            .sensu("non. A.lancea.")
            .nothingElse();

    //TODO: pro syn.
    assertName("Combretum Loefl. (1758), nom. cons. [= Grislea L. 1753].", "Combretum")
            .monomial("Combretum")
            .combAuthors("1758", "Loefl.")
            .nomNote("nom.cons.")
            .doubtful()
            .partial(")(= Grislea L.1753).")
            .code(NomCode.ZOOLOGICAL)
            .warning(Warnings.UNUSUAL_CHARACTERS)
            .nothingElse();

    assertName("Anthurium lanceum Engl. nom.illeg.", "Anthurium lanceum")
            .species("Anthurium", "lanceum")
            .combAuthors(null, "Engl.")
            .nomNote("nom.illeg.")
            .code(BOTANICAL)
            .nothingElse();

  }

  @Test
  public void flagBadAuthorship() throws Exception {
    assertName("Cynoglossus aurolineatus Not applicable", "Cynoglossus aurolineatus")
            .species("Cynoglossus", "aurolineatus")
            .warning(Warnings.AUTHORSHIP_REMOVED)
            .nothingElse();

    assertName("Asellus major Not given", "Asellus major")
            .species("Asellus", "major")
            .warning(Warnings.AUTHORSHIP_REMOVED)
            .nothingElse();

    assertName("Doradidae <Unspecified Agent>", "Doradidae")
            .monomial("Doradidae")
            .warning(Warnings.AUTHORSHIP_REMOVED, Warnings.UNUSUAL_CHARACTERS)
            .doubtful()
            .nothingElse();
  }

  @Test
  public void taxonomicNotes() throws Exception {
    // bacteria
    assertName("Achromobacter Yabuuchi and Yano, 1981 emend. Yabuuchi et al., 1998", "Achromobacter")
            .monomial("Achromobacter")
            .combAuthors("1981", "Yabuuchi", "Yano")
            .sensu("emend. Yabuuchi et al., 1998")
            .code(ZOOLOGICAL)
            .nothingElse();

    // FishBase https://github.com/CatalogueOfLife/backend/issues/1067
    assertName("Centropyge fisheri (non Snyder, 1904)", "Centropyge fisheri")
            .species("Centropyge", "fisheri")
            .sensu("non Snyder, 1904")
            .nothingElse();

    assertName("Centropyge fisheri non (Snyder, 1904)", "Centropyge fisheri")
            .species("Centropyge", "fisheri")
            .sensu("non (Snyder, 1904)")
            .nothingElse();

    assertName("Centropyge fisheri (not Snyder, 1904)", "Centropyge fisheri")
            .species("Centropyge", "fisheri")
            .sensu("not Snyder, 1904")
            .nothingElse();

    // https://github.com/CatalogueOfLife/data/issues/146#issuecomment-649095386
    assertName("Vittaria auct.", "Vittaria")
            .monomial("Vittaria")
            .sensu("auct.")
            .nothingElse();

    // from Dyntaxa
    assertName("Pycnophyes Auctt., non Zelinka, 1907", "Pycnophyes")
            .monomial("Pycnophyes")
            .sensu("auctt., non Zelinka, 1907")
            .nothingElse();

    assertName("Dyadobacter (Chelius & Triplett, 2000) emend. Reddy & Garcia-Pichel, 2005", "Dyadobacter")
            .monomial("Dyadobacter")
            .basAuthors("2000", "Chelius", "Triplett")
            .sensu("emend. Reddy & Garcia-Pichel, 2005")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Thalassiosira praeconvexa Burckle emend Gersonde & Schrader, 1984", "Thalassiosira praeconvexa")
            .species("Thalassiosira", "praeconvexa")
            .combAuthors(null, "Burckle")
            .sensu("emend Gersonde & Schrader, 1984")
            .nothingElse();

    assertName("Amphora gracilis f. exilis Gutwinski according to Hollerback & Krasavina, 1971", "Amphora gracilis f. exilis")
            .infraSpecies("Amphora", "gracilis", Rank.FORM, "exilis")
            .combAuthors(null, "Gutwinski")
            .sensu("according to Hollerback & Krasavina, 1971")
            .nothingElse();

    assertSensu("Trifolium repens sensu Baker f.", "sensu Baker f.");
    assertSensu("Achillea millefolium sensu latu", "sensu latu");
    assertSensu("Achillea millefolium s.str.", "s.str.");
    assertSensu("Achillea millefolium sec. Greuter 2009", "sec. Greuter 2009");
    assertSensu("Globularia cordifolia L. excl. var. (emend. Lam.)", "excl. var. (emend. Lam.)");

    assertName("Ramaria subbotrytis (Coker) Corner 1950 ss. auct. europ.", "Ramaria subbotrytis")
            .species("Ramaria", "subbotrytis")
            .basAuthors(null, "Coker")
            .combAuthors("1950", "Corner")
            .sensu("ss. auct. europ.")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Thelephora cuticularis Berk. ss. auct. europ.", "Thelephora cuticularis")
            .species("Thelephora", "cuticularis")
            .combAuthors(null, "Berk.")
            .sensu("ss. auct. europ.")
            .nothingElse();

    assertName("Handmannia austriaca f. elliptica Handmann fide Hustedt, 1922", "Handmannia austriaca f. elliptica")
            .infraSpecies("Handmannia", "austriaca", Rank.FORM, "elliptica")
            .combAuthors(null, "Handmann")
            .sensu("fide Hustedt, 1922")
            .nothingElse();
  }

  @Test
  public void nonNames() throws Exception {
    // the entire name ends up as a taxonomic note, consider this as unparsed...
    assertUnparsable("non  Ramaria fagetorum Maas Geesteranus 1976 nomen nudum = Ramaria subbotrytis sensu auct. europ.", Rank.SPECIES, NameType.NO_NAME);

    assertName("Hebeloma album Peck 1900 non ss. auct. europ.", "Hebeloma album")
            .species("Hebeloma", "album")
            .combAuthors("1900", "Peck")
            .sensu("non ss. auct. europ.")
            .code(NomCode.ZOOLOGICAL)
            .nothingElse();

    assertName("Nitocris (Nitocris) similis Breuning, 1956 (nec Gahan, 1893)", "Nitocris similis")
            .binomial("Nitocris", "Nitocris", "similis", Rank.SPECIES)
            .combAuthors("1956", "Breuning")
            .sensu("nec Gahan, 1893")
            .code(NomCode.ZOOLOGICAL)
            .nothingElse();

    assertName("Bartlingia Brongn. non Rchb. 1824 nec F.Muell. 1882", "Bartlingia")
            .monomial("Bartlingia")
            .combAuthors(null, "Brongn.")
            .sensu("non Rchb. 1824 nec F.Muell. 1882")
            .nothingElse();

    assertName("Lindera Thunb. non Adans. 1763", "Lindera")
            .monomial("Lindera")
            .combAuthors(null, "Thunb.")
            .sensu("non Adans. 1763")
            .nothingElse();

    assertName("Chorististium maculatum (non Bloch 1790)", "Chorististium maculatum")
            .species("Chorististium", "maculatum")
            .sensu("non Bloch 1790")
            .nothingElse();

    assertName("Puntius arulius subsp. tambraparniei (non Silas 1954)", "Puntius arulius subsp. tambraparniei")
            .infraSpecies("Puntius", "arulius", Rank.SUBSPECIES, "tambraparniei")
            .sensu("non Silas 1954")
            .code(NomCode.BOTANICAL)
            .nothingElse();

  }

  @Test
  public void misapplied() throws Exception {
    assertName("Ficus exasperata auct. non Vahl", "Ficus exasperata")
            .species("Ficus", "exasperata")
            .sensu("auct. non Vahl")
            .nothingElse();

    assertName("Mentha rotundifolia auct. non (L.) Huds. 1762", "Mentha rotundifolia")
            .species("Mentha", "rotundifolia")
            .sensu("auct. non (L.) Huds. 1762")
            .nothingElse();

  }

  private void assertSensu(String raw, String sensu) throws UnparsableNameException, InterruptedException {
    assertEquals(sensu, parser.parse(raw, null).getTaxonomicNote());
  }

  @Test
  public void viralNames() throws Exception {
    assertTrue(isViralName("Cactus virus 2"));
    assertTrue(isViralName("Vibrio phage 149 (type IV)"));
    assertTrue(isViralName("Cactus virus 2"));
    assertTrue(isViralName("Suid herpesvirus 3 Ictv"));
    assertTrue(isViralName("Tomato yellow leaf curl Mali virus Ictv"));
    assertTrue(isViralName("Not Sapovirus MC10"));
    assertTrue(isViralName("Diolcogaster facetosa bracovirus"));
    assertTrue(isViralName("Human papillomavirus"));
    assertTrue(isViralName("Sapovirus Hu/GI/Nsc, 150/PA/Bra/, 1993"));
    assertTrue(isViralName("Aspergillus mycovirus, 1816"));
    assertTrue(isViralName("Hantavirus sdp2 Yxl-, 2008"));
    assertTrue(isViralName("Norovirus Nizhny Novgorod /, 2461 / Rus /, 2007"));
    assertTrue(isViralName("Carrot carlavirus WM-, 2008"));
    assertTrue(isViralName("C2-like viruses"));
    assertTrue(isViralName("C1 bacteriophage"));
    assertTrue(isViralName("C-terminal Gfp fusion vector pUG23"));
    assertTrue(isViralName("C-terminal Gfp fusion vector"));
    assertTrue(isViralName("CMVd3 Flexi Vector pFN24K (HaloTag 7)"));
    assertTrue(isViralName("bacteriophage, 315.6"));
    assertTrue(isViralName("bacteriophages"));
    assertTrue(isViralName("\"T1-like viruses\""));
    // http://dev.gbif.org/issues/browse/PF-2574
    assertTrue(isViralName("Inachis io NPV"));
    assertTrue(isViralName("Hyloicus pinastri NPV"));
    assertTrue(isViralName("Dictyoploca japonica NPV"));
    assertTrue(isViralName("Apocheima pilosaria NPV"));
    assertTrue(isViralName("Lymantria xylina NPV"));
    assertTrue(isViralName("Feltia subterranea GV"));
    assertTrue(isViralName("Dionychopus amasis GV"));

    assertFalse(isViralName("Forcipomyia flavirustica Remm, 1968"));

    assertName("Crassatellites janus Hedley, 1906", "Crassatellites janus")
            .species("Crassatellites", "janus")
            .combAuthors("1906", "Hedley")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Ypsolophus satellitella", "Ypsolophus satellitella")
            .species("Ypsolophus", "satellitella")
            .nothingElse();

    assertName("Nephodia satellites", "Nephodia satellites")
            .species("Nephodia", "satellites")
            .nothingElse();

    Reader reader = resourceReader("viruses.txt");
    LineIterator iter = new LineIterator(reader);
    while (iter.hasNext()) {
      String line = iter.nextLine();
      if (line == null || line.startsWith("#") || line.trim().isEmpty()) {
        continue;
      }
      assertTrue(isViralName(line));
    }
  }

  @Test
  public void apostropheEpithets() throws Exception {
    assertName("Junellia o'donelli Moldenke, 1946", "Junellia o'donelli")
            .species("Junellia", "o'donelli")
            .combAuthors("1946", "Moldenke")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Trophon d'orbignyi Carcelles, 1946", "Trophon d'orbignyi")
            .species("Trophon", "d'orbignyi")
            .combAuthors("1946", "Carcelles")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Arca m'coyi Tenison-Woods, 1878", "Arca m'coyi")
            .species("Arca", "m'coyi")
            .combAuthors("1878", "Tenison-Woods")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Nucula m'andrewii Hanley, 1860", "Nucula m'andrewii")
            .species("Nucula", "m'andrewii")
            .combAuthors("1860", "Hanley")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Eristalis l'herminierii Macquart", "Eristalis l'herminierii")
            .species("Eristalis", "l'herminierii")
            .combAuthors(null, "Macquart")
            .nothingElse();

    assertName("Odynerus o'neili Cameron", "Odynerus o'neili")
            .species("Odynerus", "o'neili")
            .combAuthors(null, "Cameron")
            .nothingElse();

    assertName("Serjania meridionalis Cambess. var. o'donelli F.A. Barkley", "Serjania meridionalis var. o'donelli")
            .infraSpecies("Serjania", "meridionalis", Rank.VARIETY, "o'donelli")
            .combAuthors(null, "F.A.Barkley")
            .nothingElse();
  }

  /**
   * https://github.com/gbif/name-parser/issues/28
   */
  @Test
  public void initialsAfterSurname() throws Exception {
    assertName("Purana guttularis (Walker, F., 1858)", "Purana guttularis")
            .species("Purana", "guttularis")
            .basAuthors("1858", "F.Walker")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Physomerinus septemfoveolatus Schaufuss, L. W.", "Physomerinus septemfoveolatus")
            .species("Physomerinus", "septemfoveolatus")
            .combAuthors(null, "L.W.Schaufuss")
            .nothingElse();

    assertName("Physomerinus septemfoveolatus Schaufuss, L. W., 1877", "Physomerinus septemfoveolatus")
            .species("Physomerinus", "septemfoveolatus")
            .combAuthors("1877", "L.W.Schaufuss")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Euplectus cavicollis LeConte, J. L., 1878", "Euplectus cavicollis")
            .species("Euplectus", "cavicollis")
            .combAuthors("1878", "J.L.LeConte")
            .code(ZOOLOGICAL)
            .nothingElse();
  }

  /**
   * https://github.com/gbif/name-parser/issues/45
   */
  @Test
  public void boldPlaceholder() throws Exception {
    assertName("OdontellidaeGEN", GENUS, "Odontellidae GEN")
            .monomial("Odontellidae", GENUS)
            .phrase("GEN")
            .type(PLACEHOLDER)
            .nothingElse();

    assertName("EusiridaeNZD", ZOOLOGICAL,"Eusiridae NZD")
            .monomial("Eusiridae", FAMILY)
            .phrase("NZD")
            .type(PLACEHOLDER)
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Blattellinae_SB","Blattellinae SB")
            .monomial("Blattellinae")
            .phrase("SB")
            .type(PLACEHOLDER)
            .nothingElse();

    assertName("GenusANIC_3","Genus ANIC_3")
            .monomial("Genus")
            .phrase("ANIC_3")
            .type(PLACEHOLDER)
            //.warning(Warnings.BLACKLISTED_EPITHET)
            .nothingElse();
  }

  /**
   * http://dev.gbif.org/issues/browse/POR-3069
   */
  @Test
  public void nullNameParts() throws Exception {
    assertName("Austrorhynchus pectatus null pectatus", "Austrorhynchus pectatus pectatus")
            .infraSpecies("Austrorhynchus", "pectatus", Rank.INFRASPECIFIC_NAME, "pectatus")
            .doubtful()
            .warning(Warnings.NULL_EPITHET)
            .nothingElse();

    //assertName("Poa pratensis null proles (L.) Rouy, 1913", "Poa pratensis proles")
    //    .infraSpecies("Poa", "pratensis", Rank.PROLES, "proles")
    //    .basAuthors(null, "L.")
    //    .combAuthors("1913", "Rouy")
    //    .nothingElse();

    // should the infrasubspecific epithet kewensis be removed from the parsed name?
    //assertParsedParts("Poa pratensis kewensis proles", NameType.INFORMAL, "Poa", "pratensis", "kewensis", Rank.PROLES, null);
    //assertParsedParts("Poa pratensis kewensis proles (L.) Rouy, 1913", NameType.INFORMAL, "Poa", "pratensis", null, Rank.PROLES, "Rouy", "1913", "L.", null);
  }


  @Test
  @Ignore
  public void rNANames() throws Exception {
    assertName("Calathus (Lindrothius) KURNAKOV 1961", "Calathus (Lindrothius)")
            .infraGeneric("Calathus", Rank.INFRAGENERIC_NAME, "Lindrothius")
            .combAuthors("1961", "Kurnakov")
            .nothingElse();

    assertTrue(isViralName("Ustilaginoidea virens RNA virus"));
    assertTrue(isViralName("Rhizoctonia solani dsRNA virus 2"));

    assertName("Candida albicans RNA_CTR0-3", "Candida albicans RNA_CTR0-3")
            .species("Candida", "albicans")
            .nothingElse();


    //pn = parser.parse("Alpha proteobacterium RNA12", null);
    //assertEquals("Alpha", pn.getGenusOrAbove());
    //assertEquals("proteobacterium", pn.getSpecificEpithet());
    //assertEquals(NameType.INFORMAL, pn.getType());
    //assertNull(pn.getInfraSpecificEpithet());
    //assertNull(pn.getAuthorship());
//
    //pn = parser.parse("Armillaria ostoyae RNA1", null);
    //assertEquals("Armillaria", pn.getGenusOrAbove());
    //assertEquals("ostoyae", pn.getSpecificEpithet());
    //assertEquals(NameType.INFORMAL, pn.getType());
    //assertNull(pn.getInfraSpecificEpithet());
    //assertNull(pn.getAuthorship());
//
    //assertUnparsableType(NameType.DOUBTFUL, "siRNA");
  }

  @Test
  public void indetNames() throws Exception {
    assertName("Nitzschia sinuata var. (Grunow) Lange-Bert.", "Nitzschia sinuata var.")
            .infraSpecies("Nitzschia", "sinuata", Rank.VARIETY, null)
            .type(NameType.INFORMAL)
            .warning(Warnings.INDETERMINED)
            .nothingElse();

    assertName("Canis lupus subsp. Linnaeus, 1758", "Canis lupus ssp.")
            .infraSpecies("Canis", "lupus", Rank.SUBSPECIES, null)
            .type(NameType.INFORMAL)
            .warning(Warnings.INDETERMINED)
            .nothingElse();

//    assertName("Aphaenogaster (Ichnomyrmex) Schwammerdami var. spinipes", "Aphaenogaster var. spinipes")
//        .infraSpecies("Aphaenogaster", null, Rank.VARIETY, "spinipes")
//        .infraGeneric("Ichnomyrmex")
//        .type(NameType.INFORMAL)
//        .nothingElse();
//
//    assertName("Ocymyrmex Weitzaeckeri subsp. arnoldi", "Ocymyrmex subsp. arnoldi")
//        .infraSpecies("Ocymyrmex", null, Rank.SUBSPECIES, "arnoldi")
//        .type(NameType.INFORMAL)
//        .nothingElse();
//
//    assertName("Navicula var. fasciata", "Navicula var. fasciata")
//        .infraSpecies("Navicula", null, Rank.VARIETY, "fasciata")
//        .type(NameType.INFORMAL)
//        .nothingElse();

    assertName("Polygonum spec.", "Polygonum sp.")
            .species("Polygonum", null)
            .type(NameType.INFORMAL)
            .warning(Warnings.INDETERMINED)
            .nothingElse();

    assertName("Polygonum vulgaris ssp.", "Polygonum vulgaris ssp.")
            .infraSpecies("Polygonum", "vulgaris", Rank.SUBSPECIES, null)
            .type(NameType.INFORMAL)
            .warning(Warnings.INDETERMINED)
            .nothingElse();

    assertName("Mesocricetus sp.", "Mesocricetus sp.")
            .species("Mesocricetus", null)
            .type(NameType.INFORMAL)
            .warning(Warnings.INDETERMINED)
            .nothingElse();

    // dont treat these authorships as forms
    assertName("Dioscoreales Hooker f.", BOTANICAL, "Dioscoreales")
            .monomial("Dioscoreales", Rank.ORDER)
            .combAuthors(null, "Hooker f.")
            .code(BOTANICAL)
            .nothingElse();

    assertName("Melastoma vacillans Blume var.", "Melastoma vacillans var.")
            .infraSpecies("Melastoma", "vacillans", Rank.VARIETY, null)
            .type(NameType.INFORMAL)
            .warning(Warnings.INDETERMINED)
            .nothingElse();

    assertName("Lepidoptera Hooker", Rank.SPECIES, "Lepidoptera sp.")
            .species("Lepidoptera", null)
            .type(NameType.INFORMAL)
            .warning(Warnings.INDETERMINED)
            .nothingElse();

    assertName("Lepidoptera alba DC.", Rank.SUBSPECIES, "Lepidoptera alba ssp.")
            .infraSpecies("Lepidoptera", "alba", Rank.SUBSPECIES, null)
            .type(NameType.INFORMAL)
            .warning(Warnings.INDETERMINED)
            .nothingElse();
  }

  @Test
  public void rankMismatch() throws Exception {
    assertName("Polygonum", Rank.CULTIVAR, "Polygonum cv.")
            .cultivar("Polygonum", null)
            .type(INFORMAL)
            .warning(Warnings.INDETERMINED)
            .nothingElse();

    assertName("Polygonum", Rank.SUBSPECIES, "Polygonum subsp.")
            .indet("Polygonum", null, Rank.SUBSPECIES)
            .warning(Warnings.INDETERMINED)
            .nothingElse();

    assertName("Polygonum alba", Rank.GENUS, "Polygonum alba")
            .binomial("Polygonum", null, "alba", Rank.GENUS)
            .type(INFORMAL)
            .doubtful()
            .warning(Warnings.RANK_MISMATCH)
            .nothingElse();
  }

  /**
   * https://github.com/gbif/name-parser/issues/5
   */
  @Test
  public void vulpes() throws Exception {
    assertName("Vulpes vulpes sp. silaceus Miller, 1907", "Vulpes vulpes silaceus")
            .infraSpecies("Vulpes", "vulpes", Rank.SUBSPECIES, "silaceus")
            .combAuthors("1907", "Miller")
            .warning(Warnings.SUBSPECIES_ASSIGNED)
            .code(ZOOLOGICAL)
            .nothingElse();
  }

  @Test
  public void microbialRanks2() throws Exception {
    assertName("Puccinia graminis f. sp. avenae", "Puccinia graminis f.sp. avenae")
            .infraSpecies("Puccinia", "graminis", Rank.FORMA_SPECIALIS, "avenae")
            .code(NomCode.BACTERIAL)
            .nothingElse();
  }

  @Test
  public void chineseAuthors() throws Exception {
    assertName("Abaxisotima acuminata (Wang & Liu, 1996)", "Abaxisotima acuminata")
            .species("Abaxisotima", "acuminata")
            .basAuthors("1996", "Wang", "Liu")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Abaxisotima acuminata (Wang, Yuwen & Xian-wei Liu, 1996)", "Abaxisotima acuminata")
            .species("Abaxisotima", "acuminata")
            .basAuthors("1996", "Wang", "Yuwen", "Xian-wei Liu")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Abaxisotima bicolor (Liu, Xian-wei, Z. Zheng & G. Xi, 1991)", "Abaxisotima bicolor")
            .species("Abaxisotima", "bicolor")
            .basAuthors("1991", "Liu", "Xian-wei", "Z.Zheng", "G.Xi")
            .code(ZOOLOGICAL)
            .nothingElse();
  }

  @Test
  public void authorshipOnlyNotes() throws Exception {
    assertAuthorship("(auct.) Rolfe")
            .sensu("(auct. ) Rolfe")
            .nothingElse();

    assertAuthorship("auct., nom. subnud.")
            .sensu("auct. , nom. subnud.")
            .nothingElse();

    assertAuthorship("Fischer-Le Saux et al., 1999 emend. Akhurst et al., 2004")
            .combAuthors("1999", "Fischer-Le Saux", "al.")
            .sensu("emend. Akhurst et al., 2004")
            .nothingElse();

    assertAuthorship("Trautv. & Meyer sensu lato")
            .combAuthors(null, "Trautv.", "Meyer")
            .sensu("sensu lato")
            .nothingElse();

    assertAuthorship("Mill. non Parolly")
            .combAuthors(null, "Mill.")
            .sensu("non Parolly")
            .nothingElse();
  }

  @Test
  public void authorshipOnly() throws Exception {
    assertAuthorship("1771")
            .combAuthors("1771")
            .nothingElse();

    assertAuthorship("Pallas, 1771")
            .combAuthors("1771", "Pallas")
            .nothingElse();


    // https://github.com/CatalogueOfLife/data/issues/176
    assertAuthorship("Maas & He")
            .combAuthors(null, "Maas", "He")
            .nothingElse();

    assertAuthorship("Yang & Wu")
            .combAuthors(null, "Yang", "Wu")
            .nothingElse();

    assertAuthorship("Freytag & Ma")
            .combAuthors(null, "Freytag", "Ma")
            .nothingElse();

    assertAuthorship("(Ristorcelli & Van ty) Wedd. ex Sch. Bip. (nom. nud.)")
            .basAuthors(null, "Ristorcelli", "Van ty")
            .combAuthors(null, "Sch.Bip.")
            .combExAuthors("Wedd.")
            .nomNote("nom.nud.")
            .nothingElse();

    assertAuthorship("(Wang & Liu, 1996)")
            .basAuthors("1996", "Wang", "Liu")
            .nothingElse();

    assertAuthorship("(Wang, Yuwen & Xian-wei Liu, 1996)")
            .basAuthors("1996", "Wang", "Yuwen", "Xian-wei Liu")
            .nothingElse();

    assertAuthorship("(Liu, Xian-wei, Z. Zheng & G. Xi, 1991)")
            .basAuthors("1991", "Liu", "Xian-wei", "Z.Zheng", "G.Xi")
            .nothingElse();

    assertAuthorship("(Ristorcelli & Van ty, 1941)")
            .basAuthors("1941", "Ristorcelli", "Van ty")
            .nothingElse();


    assertAuthorship("FISCHER 1885")
            .combAuthors("1885", "Fischer")
            .nothingElse();

    assertAuthorship("(Walker, F., 1858)")
            .basAuthors("1858", "F.Walker")
            .nothingElse();

    assertAuthorship("Schaufuss, L. W.")
            .combAuthors(null, "L.W.Schaufuss")
            .nothingElse();

    assertAuthorship("Schaufuss, L. W., 1877")
            .combAuthors("1877", "L.W.Schaufuss")
            .nothingElse();

    assertAuthorship("LeConte, J. L., 1878")
            .combAuthors("1878", "J.L.LeConte")
            .nothingElse();

    assertAuthorship("Jian Wang ter & A.R.Bean")
            .combAuthors(null, "Jian Wang ter", "A.R.Bean")
            .nothingElse();

    assertAuthorship("A.Murray bis")
            .combAuthors(null, "A.Murray bis")
            .nothingElse();

    assertAuthorship("(Gordon) A.Murray bis")
            .basAuthors(null, "Gordon")
            .combAuthors(null, "A.Murray bis")
            .nothingElse();

    assertAuthorship("Castellano, S.L. Mill., L. Singh bis & T.N. Lakh. 2012")
            .combAuthors("2012", "Castellano", "S.L.Mill.", "L.Singh bis", "T.N.Lakh.")
            .nothingElse();

    assertAuthorship("(Beurm., Gougerot & Vaucher bis) M. Ota")
            .basAuthors(null, "Beurm.", "Gougerot", "Vaucher bis")
            .combAuthors(null, "M.Ota")
            .nothingElse();

    // van der
    assertAuthorship("(van der Wulp, 1885)")
            .basAuthors("1885", "van der Wulp")
            .nothingElse();

    // https://www.ipni.org/a/40285-1
    assertAuthorship("Viane & Van den heede")
            .combAuthors(null, "Viane", "Van den heede")
            .nothingElse();

    assertAuthorship("van den Brink")
            .combAuthors(null, "van den Brink")
            .nothingElse();

    assertAuthorship("Van de Kerckh.")
            .combAuthors(null, "Van de Kerckh.")
            .nothingElse();

    assertAuthorship("Van de Putte")
            .combAuthors(null, "Van de Putte")
            .nothingElse();

    assertAuthorship("Van Dersal")
            .combAuthors(null, "Van Dersal")
            .nothingElse();

    // turkish chars
    assertAuthorship("Ilçim, Çenet & Dadandi")
            .combAuthors(null, "Ilçim", "Çenet", "Dadandi")
            .nothingElse();

    assertAuthorship("S. Yildirimli")
            .combAuthors(null, "S.Yildirimli")
            .nothingElse();

    assertAuthorship("Şahin, Koca & Yildirim, 2012")
            .combAuthors("2012", "Şahin", "Koca", "Yildirim")
            .nothingElse();

    assertAuthorship("L.f")
            .combAuthors(null, "L.f")
            .nothingElse();

    assertAuthorship("(L.) G. Don filius")
            .basAuthors(null, "L.")
            .combAuthors(null, "G.Don filius")
            .nothingElse();

    assertAuthorship("(L.) G. Don fil.")
            .basAuthors(null, "L.")
            .combAuthors(null, "G.Don fil.")
            .nothingElse();

    assertAuthorship("d'Urv.")
            .combAuthors(null, "d'Urv.")
            .nothingElse();

    assertAuthorship("Balsamo M Fregni E Tongiorgi P")
            .combAuthors(null, "M.Balsamo", "E.Fregni", "P.Tongiorgi")
            .nothingElse();

    assertAuthorship("Balsamo M Todaro MA")
            .combAuthors(null, "M.Balsamo", "M.A.Todaro")
            .nothingElse();

    assertAuthorship("Cushman Em. Sellier de Civrieux, 1976")
            .combAuthors("1976", "Cushman Em.Sellier de Civrieux")
            .nothingElse();

    // http://dev.gbif.org/issues/browse/POR-101
    assertAuthorship("la Croix & P.J.Cribb")
            .combAuthors(null, "la Croix", "P.J.Cribb")
            .nothingElse();

    assertAuthorship("le Croix & P.J.Cribb")
            .combAuthors(null, "le Croix", "P.J.Cribb")
            .nothingElse();

    assertAuthorship("de la Croix & le P.J.Cribb")
            .combAuthors(null, "de la Croix", "le P.J.Cribb")
            .nothingElse();

    assertAuthorship("Istv?nffi, 1898")
            .combAuthors("1898", "Istvnffi")
            .doubtful()
            .warning(Warnings.QUESTION_MARKS_REMOVED)
            .nothingElse();

    assertAuthorship("F.S.Castracane degli Antelminelli")
            .combAuthors(null, "F.S.Castracane degli Antelminelli")
            .nothingElse();

    assertAuthorship("De la Soie")
            .combAuthors(null, "De la Soie")
            .nothingElse();

    assertAuthorship("Hort. ex Vilmorin")
            .combAuthors(null, "Vilmorin")
            .combExAuthors("hort.")
            .nothingElse();

    assertAuthorship("hortusa ex K. Koch")
            .combAuthors(null, "K.Koch")
            .combExAuthors("hort.")
            .nothingElse();

    assertAuthorship("hortus ex K. Koch")
            .combAuthors(null, "K.Koch")
            .combExAuthors("hort.")
            .nothingElse();

    assertAuthorship("(Thunberg) A.P.de Candolle")
            .basAuthors(null, "Thunberg")
            .combAuthors(null, "A.P.de Candolle")
            .nothingElse();

    assertAuthorship("(Huguet del Villar) S. Rivas-Martínez, F. Fernández González & D. Sánchez-Mata")
            .basAuthors(null, "Huguet del Villar")
            .combAuthors(null, "S.Rivas-Martínez", "F.Fernández González", "D.Sánchez-Mata")
            .nothingElse();

    assertAuthorship("(H. da C. Monteiro Filho) H. da C. Monteiro Filho")
            .basAuthors(null, "H.da C.Monteiro Filho")
            .combAuthors(null, "H.da C.Monteiro Filho")
            .nothingElse();
  }

  /**
   * http://dev.gbif.org/issues/browse/POR-2454
   */
  @Test
  public void fungusNames() throws Exception {
    assertName("Merulius lacrimans (Wulfen : Fr.) Schum.", "Merulius lacrimans")
            .species("Merulius", "lacrimans")
            .combAuthors(null, "Schum.")
            .basAuthors(null, "Wulfen")
            .code(BOTANICAL)
            .nothingElse();

    assertName("Merulius lacrimans (Wulfen) Schum. : Fr.", "Merulius lacrimans")
            .species("Merulius", "lacrimans")
            .combAuthors(null, "Schum.")
            .basAuthors(null, "Wulfen")
            .sanctAuthor("Fr.")
            .code(BOTANICAL)
            .nothingElse();

    //assertParsedParts("", null, "Merulius", "lacrimans", null, null, "Schum.", null, "Wulfen : Fr.", null);
    //assertParsedParts("Aecidium berberidis Pers. ex J.F. Gmel.", null, "Aecidium", "berberidis", null, null, "Pers. ex J.F. Gmel.", null, null, null);
    //assertParsedParts("Roestelia penicillata (O.F. Müll.) Fr.", null, "Roestelia", "penicillata", null, null, "Fr.", null, "O.F. Müll.", null);
//
    //assertParsedParts("Mycosphaerella eryngii (Fr. Duby) ex Oudem., 1897", null, "Mycosphaerella", "eryngii", null, null, "ex Oudem.", "1897", "Fr. Duby", null);
    //assertParsedParts("Mycosphaerella eryngii (Fr.ex Duby) ex Oudem. 1897", null, "Mycosphaerella", "eryngii", null, null, "ex Oudem.", "1897", "Fr.ex Duby", null);
    //assertParsedParts("Mycosphaerella eryngii (Fr. ex Duby) Johanson ex Oudem. 1897", null, "Mycosphaerella", "eryngii", null, null, "Johanson ex Oudem.", "1897", "Fr. ex Duby", null);
  }

  @Test
  public void yearVariations() throws Exception {
    assertName("Deudorix epijarbas turbo Fruhstorfer, [1912]", "Deudorix epijarbas turbo")
            .infraSpecies("Deudorix", "epijarbas", Rank.INFRASPECIFIC_NAME, "turbo")
            .combAuthors("1912", "Fruhstorfer")
            .code(NomCode.ZOOLOGICAL)
            .nothingElse();
  }

  /**
   * https://github.com/gbif/name-parser/issues/27
   */
  @Test
  public void hyphens() throws Exception {
    assertName("Minilimosina v-atrum (Villeneuve, 1917)", "Minilimosina v-atrum")
            .species("Minilimosina", "v-atrum")
            .basAuthors("1917", "Villeneuve")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Aelurillus v-insignitus", "Aelurillus v-insignitus")
            .species("Aelurillus", "v-insignitus")
            .nothingElse();

    assertName("Desmometopa m-nigrum", "Desmometopa m-nigrum")
            .species("Desmometopa", "m-nigrum")
            .nothingElse();

    assertName("Chloroclystis v-ata", "Chloroclystis v-ata")
            .species("Chloroclystis", "v-ata")
            .nothingElse();

    assertName("Cortinarius moenne-loccozii Bidaud", "Cortinarius moenne-loccozii")
            .species("Cortinarius", "moenne-loccozii")
            .combAuthors(null, "Bidaud")
            .nothingElse();

    assertName("Asarum sieboldii f. non-maculatum (Y.N.Lee) M.Kim", "Asarum sieboldii f. non-maculatum")
            .infraSpecies("Asarum", "sieboldii", FORM, "non-maculatum")
            .combAuthors(null, "M.Kim")
            .basAuthors(null, "Y.N.Lee")
            .code(BOTANICAL)
            .nothingElse();

    // atypical hyphens, https://github.com/CatalogueOfLife/backend/issues/1178
    assertName("Passalus (Pertinax) gaboi Jiménez‑Ferbans & Reyes‑Castillo, 2022", "Passalus gaboi")
            .species("Passalus", "Pertinax", "gaboi")
            .infraGeneric("Pertinax")
            .combAuthors("2022", "Jiménez-Ferbans", "Reyes-Castillo")
            .code(ZOOLOGICAL)
            .warning(Warnings.HOMOGLYHPS)
            .nothingElse();
  }

  @Test
  public void imprintYears() throws Exception {
    assertName("Ophidocampa tapacumae Ehrenberg, 1870, 1869", "Ophidocampa tapacumae")
            .species("Ophidocampa", "tapacumae")
            .combAuthors("1870", "Ehrenberg")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Brachyspira Hovind-Hougen, Birch-Andersen, Henrik-Nielsen, Orholm, Pedersen, Teglbjaerg & Thaysen, 1983, 1982", "Brachyspira")
            .monomial("Brachyspira")
            .combAuthors("1983", "Hovind-Hougen", "Birch-Andersen", "Henrik-Nielsen", "Orholm", "Pedersen", "Teglbjaerg", "Thaysen")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Gyrosigma angulatum var. gamma Griffith & Henfrey, 1860, 1856", "Gyrosigma angulatum var. gamma")
            .infraSpecies("Gyrosigma", "angulatum", Rank.VARIETY, "gamma")
            .combAuthors("1860", "Griffith", "Henfrey")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Ctenotus alacer Storr, 1970 [\"1969\"]", "Ctenotus alacer")
            .species("Ctenotus", "alacer")
            .combAuthors("1970", "Storr")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Ctenotus alacer Storr, 1970 (imprint 1969)", "Ctenotus alacer")
            .species("Ctenotus", "alacer")
            .combAuthors("1970", "Storr")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Ctenotus alacer Storr, 1887 (\"1886-1888\")", "Ctenotus alacer")
            .species("Ctenotus", "alacer")
            .combAuthors("1887", "Storr")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Melanargia halimede menetriesi Wagener, 1959 & 1961", "Melanargia halimede menetriesi")
            .infraSpecies("Melanargia", "halimede", Rank.INFRASPECIFIC_NAME, "menetriesi")
            .combAuthors("1959", "Wagener")
            .code(ZOOLOGICAL)
            .nothingElse();
  }

  @Test
  public void lowerCaseNames() throws Exception {
    assertName("abies alba Mill.", "Abies alba")
            .species("Abies", "alba")
            .combAuthors(null, "Mill.")
            .type(SCIENTIFIC)
            .nothingElse();
  }

  @Test
  public void manuscriptNames() throws Exception {
    assertName("Abrodictyum caespifrons (C. Chr.) comb. ined.", "Abrodictyum caespifrons")
            .species("Abrodictyum", "caespifrons")
            .basAuthors(null, "C.Chr.")
            .type(SCIENTIFIC)
            .nomNote("comb.ined.")
            .manuscript()
            .nothingElse();

    assertName("Acranthera virescens (Ridl.) ined.", "Acranthera virescens")
            .species("Acranthera", "virescens")
            .basAuthors(null, "Ridl.")
            .type(SCIENTIFIC)
            .nomNote("ined.")
            .manuscript()
            .nothingElse();

    // real authorship is Siliç
    assertName("Micromeria cristata subsp. kosaninii ( ilic) ined.", "Micromeria cristata subsp. kosaninii")
            .infraSpecies("Micromeria", "cristata", SUBSPECIES, "kosaninii")
            //.basAuthors(null, "ilic")
            .partial("(ilic)")
            .type(SCIENTIFIC)
            .nomNote("ined.")
            .manuscript()
            .code(BOTANICAL)
            .nothingElse();

    assertName("Lepidoptera sp. JGP0404", "Lepidoptera sp.JGP0404")
            .species("Lepidoptera", "sp.JGP0404")
            .type(INFORMAL)
            .manuscript()
            .nothingElse();

    assertName("Genoplesium vernalis D.L.Jones ms.", "Genoplesium vernalis")
            .species("Genoplesium", "vernalis")
            .combAuthors(null, "D.L.Jones")
            .type(SCIENTIFIC)
            .manuscript()
            .nomNote("ms.")
            .nothingElse();

    assertName("Verticordia sp.1", "Verticordia sp.1")
            .species("Verticordia", "sp.1")
            .type(INFORMAL)
            .manuscript()
            .nothingElse();

    assertName("Bryozoan indet. 1", "Bryozoan indet.1")
            .species("Bryozoan", "indet.1")
            .type(INFORMAL)
            .manuscript()
            .nothingElse();

    assertName("Bryozoan sp. E", "Bryozoan sp.E")
            .species("Bryozoan", "sp.E")
            .type(INFORMAL)
            .manuscript()
            .nothingElse();

  }

  @Test
  public void phraseNames() throws Exception {
    assertName("Prostanthera sp. Somersbey (B.J.Conn 4024)", "Prostanthera sp. Somersbey (B.J.Conn 4024)")
            .phraseName("Prostanthera", "Somersbey", SPECIES, "B.J.Conn 4024", null)
            .nothingElse();
    assertName("Pultenaea sp. 'Olinda' (Coveny 6616)", "Pultenaea sp. Olinda (Coveny 6616)")
            .phraseName("Pultenaea", "Olinda", SPECIES, "Coveny 6616", null)
            .nothingElse();
    assertName("Pterostylis sp. Sandheath (D.Murfet 3190) R.J.Bates", "Pterostylis sp. Sandheath (D.Murfet 3190)")
            .phraseName("Pterostylis", "Sandheath", SPECIES, "D.Murfet 3190", "R.J.Bates")
            .nothingElse();
    // Check to make sure base name is parsed before haring off into the wilderness
    assertName("Acacia mutabilis Maslin", "Acacia mutabilis")
            .species("Acacia", "mutabilis")
            .combAuthors(null, "Maslin")
            .nothingElse();
    assertName("Acacia mutabilis Maslin subsp. Young River (G.F. Craig 2052)", "Acacia mutabilis ssp. Young River (G.F. Craig 2052)")
            .phraseName("Acacia", "Young River", SUBSPECIES, "G.F. Craig 2052", null)
            .combAuthors(null, "Maslin")
            .nothingElse();
    assertName("Dampiera sp. Central Wheatbelt (L.W.Sage, F.Hort, C.A.Hollister LWS2321)", "Dampiera sp. Central Wheatbelt (L.W.Sage, F.Hort, C.A.Hollister LWS2321)")
            .phraseName("Dampiera", "Central Wheatbelt", SPECIES, "L.W.Sage, F.Hort, C.A.Hollister LWS2321", null)
            .nothingElse();
    assertName("Dampiera     sp    Central  Wheatbelt (L.W.Sage,   F.Hort,   C.A.Hollister   LWS2321)", "Dampiera sp. Central Wheatbelt (L.W.Sage, F.Hort, C.A.Hollister LWS2321)")
            .phraseName("Dampiera", "Central Wheatbelt", SPECIES, "L.W.Sage, F.Hort, C.A.Hollister LWS2321", null)
            .nothingElse();
    assertName("Toechima sp. East Alligator (J.Russell-Smith 8418) NT Herbarium", "Toechima sp. East Alligator (J.Russell-Smith 8418)")
            .phraseName("Toechima", "East Alligator", SPECIES, "J.Russell-Smith 8418", "NT Herbarium")
            .nothingElse();
    assertName("Acacia sp. Mount Hilditch (M.E. Trudgen 19134)", "Acacia sp. Mount Hilditch (M.E. Trudgen 19134)")
            .phraseName("Acacia", "Mount Hilditch", SPECIES, "M.E. Trudgen 19134", null)
            .nothingElse();
  }

  @Test
  public void unsupportedAuthors() throws Exception {
    assertName(" Anolis marmoratus girafus LAZELL 1964: 377", "Anolis marmoratus girafus")
            .infraSpecies("Anolis", "marmoratus", Rank.INFRASPECIFIC_NAME, "girafus")
            .combAuthors("1964", "Lazell")
            .partial(":377")
            .code(ZOOLOGICAL)
            .nothingElse();
  }

  /**
   * From https://www.gbif.org/species/search?dataset_key=da38f103-4410-43d1-b716-ea6b1b92bbac&origin=SOURCE&issue=PARTIALLY_PARSABLE&advanced=1
   */
  @Test
  public void seniorEpithet() throws Exception {
    assertName("Mesotrichia senior (Vachal)", "Mesotrichia senior")
            .species("Mesotrichia", "senior")
            .basAuthors(null, "Vachal")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Xylocopa (Koptortosoma) senior clitelligera Friese", "Xylocopa senior clitelligera")
            .infraSpecies("Xylocopa", "senior", Rank.INFRASPECIFIC_NAME, "clitelligera")
            .infraGeneric("Koptortosoma")
            .combAuthors(null, "Friese")
            .nothingElse();
  }

  // **************
  // HELPER METHODS
  // **************

  public boolean isViralName(String name) throws InterruptedException {
    try {
      parser.parse(name, null);
    } catch (UnparsableNameException e) {
      // swallow
      if (NameType.VIRUS == e.getType()) {
        return true;
      }
    }
    return false;
  }

  private void assertNoName(String name) throws InterruptedException {
    assertUnparsable(name, NO_NAME);
  }

  private void assertUnparsable(String name, NameType type) throws InterruptedException {
    assertUnparsableName(name, Rank.UNRANKED, type, name);
  }

  private void assertUnparsable(String name, Rank rank, NameType type) throws InterruptedException {
    assertUnparsableName(name, rank, type, name);
  }

  private void assertUnparsableName(String name, Rank rank, NameType type, String expectedName) throws InterruptedException {
    try {
      parser.parse(name, rank, null);
      fail("Expected " + name + " to be unparsable");

    } catch (UnparsableNameException ex) {
      assertEquals(type, ex.getType());
      assertEquals(expectedName, ex.getName());
    }
  }

  NameAssertion assertAuthorship(String rawAuthorship) throws UnparsableNameException, InterruptedException {
    ParsedAuthorship n = parser.parseAuthorship(rawAuthorship);
    NameAssertion na = new NameAssertion((ParsedName) n);
    na.type(null);
    return na;
  }

  NameAssertion assertName(String rawName, String expectedCanonicalWithoutAuthors) throws UnparsableNameException, InterruptedException {
    return assertName(rawName, null, null, expectedCanonicalWithoutAuthors);
  }

  NameAssertion assertName(String rawName, Rank rank, String expectedCanonicalWithoutAuthors) throws UnparsableNameException, InterruptedException {
    return assertName(rawName, rank, null, expectedCanonicalWithoutAuthors);
  }

  NameAssertion assertName(String rawName, NomCode code, String expectedCanonicalWithoutAuthors) throws UnparsableNameException, InterruptedException {
    return assertName(rawName, null, code, expectedCanonicalWithoutAuthors);
  }

  NameAssertion assertName(String rawName, Rank rank, NomCode code, String expectedCanonicalWithoutAuthors) throws UnparsableNameException, InterruptedException {
    ParsedName n = parser.parse(rawName, rank, code);
    assertEquals(expectedCanonicalWithoutAuthors, n.canonicalNameWithoutAuthorship());
    return new NameAssertion(n);
  }

  private BufferedReader resourceReader(String resourceFileName) throws UnsupportedEncodingException {
    return new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/" + resourceFileName), "UTF-8"));
  }

}