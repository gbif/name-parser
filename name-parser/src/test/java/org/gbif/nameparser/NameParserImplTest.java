package org.gbif.nameparser;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.io.LineIterator;
import org.gbif.nameparser.api.*;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

import static org.gbif.nameparser.api.NamePart.INFRASPECIFIC;
import static org.gbif.nameparser.api.NamePart.SPECIFIC;
import static org.gbif.nameparser.api.NameType.*;
import static org.gbif.nameparser.api.NomCode.*;
import static org.gbif.nameparser.api.Rank.*;
import static org.junit.Assert.*;


public class NameParserImplTest {
  private static Logger LOG = LoggerFactory.getLogger(NameParserImplTest.class);
  private static final boolean DEBUG = true;

  private final NameParser parser;

  public NameParserImplTest() {
    this.parser = new NameParserImpl();
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

    // parenthesised corrig., like (sic) above
    assertName("Campylobacter lari (corrig.) Benjamin et al. 1984", "Campylobacter lari")
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

    assertName("Barleeidae [sic!]", "Barleeidae")
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
            .sic()
            .nothingElse();
  }

  @Test
  public void digitEpithetsLeadingNumeral() throws Exception {
    assertName("Coccinella 11-punctata Linnaeus, 1758", "Coccinella 11-punctata")
        .species("Coccinella", "11-punctata").combAuthors("1758", "Linnaeus").code(NomCode.ZOOLOGICAL).nothingElse();
    assertName("Coccinella 2-pustulata", "Coccinella 2-pustulata")
        .species("Coccinella", "2-pustulata").nothingElse();
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
            .nomNote("orth. error")
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
            .infraSpecies("Jezzinothrips", "cretacicus", SUBSPECIES, "amazur")
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
            .nomNote("nom. superfl.")
            .code(BOTANICAL)
            .nothingElse();
  }

  /**
   * https://github.com/gbif/name-parser/issues/68
   */
  @Test
  public void cladeNames() throws Exception {
    // keyword clade to throw unparsable
    assertUnparsable("Amauropeltoid clade", INFORMAL);
    assertUnparsable("Cyanobacteriota/Melainabacteria clade", Rank.UNRANKED, INFORMAL);
    // no clades
    assertName("Endococcus cladiae Zhurb. & Pino-Bodas", "Endococcus cladiae")
        .species("Endococcus", "cladiae")
        .combAuthors(null, "Zhurb.", "Pino-Bodas")
        .nothingElse();

    assertName("Clada tricostata clada (Clada) Pascoe, 1887", "Clada tricostata clada")
        .infraSpecies("Clada", "tricostata", SUBSPECIES, "clada")
        .combAuthors("1887", "Pascoe")
        .basAuthors(null, "Clada")
        .code(ZOOLOGICAL)
        .nothingElse();
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

  @Test
  public void nomCons() throws Exception {
    assertName("Polygala vulgaris L., 1753 [nom. et typ. cons.]", "Polygala vulgaris")
            .species("Polygala", "vulgaris")
            .combAuthors("1753", "L.")
            .nomNote("nom. & typ. cons.")
            .code(ZOOLOGICAL)
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
   * The "f." / "fil." / "filius" suffix is the regulated botanical convention for the
   * son of a same-named author, but it does also appear in older zoological literature
   * to distinguish father and son authorities. Those zoological cases always carry a
   * year, so the year-on-author-span signal correctly classifies them as ZOOLOGICAL.
   */
  @Test
  public void filiusZoological() throws Exception {
    assertName("Lacerta agilis Linnaeus f., 1789", "Lacerta agilis")
            .species("Lacerta", "agilis")
            .combAuthors("1789", "Linnaeus f.")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Testudo graeca Linnaeus f., 1789", "Testudo graeca")
            .species("Testudo", "graeca")
            .combAuthors("1789", "Linnaeus f.")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Raja batis Forster f., 1781", "Raja batis")
            .species("Raja", "batis")
            .combAuthors("1781", "Forster f.")
            .code(ZOOLOGICAL)
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

    // unpublished = manuscript name
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
            .combAuthors("2016", "F.Zhang", "Z-X.Pan")
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
            .publishedIn("Proceedings of the Koninklijke Nederlandse Akademie van Wetenschappen, Series C: Biological and Medical Sciences 87(3): 381, f. 2. 1984. Fig. 2I, J")
            .warning(Warnings.NOMENCLATURAL_REFERENCE)
            .nothingElse();

    assertName("Passiflora jussieui Feuillet, Journal of the Botanical Research Institute of Texas 4(2): 611, f. 1. 2010. Figs 2E, F, 3E, F", "Passiflora jussieui")
            .species("Passiflora", "jussieui")
            .combAuthors(null, "Feuillet")
            .publishedIn("Journal of the Botanical Research Institute of Texas 4(2): 611, f. 1. 2010. Figs 2E, F, 3E, F")
            .warning(Warnings.NOMENCLATURAL_REFERENCE)
            .nothingElse();

    assertName("Passiflora eglandulosa J.M. MacDougal. Annals of the Missouri Botanical Garden 75: 1658-1662. figs 1, 2B, and 3. 1988. Figs 36-37", "Passiflora eglandulosa")
            .species("Passiflora", "eglandulosa")
            .combAuthors(null, "J.M.MacDougal")
            .publishedIn("Annals of the Missouri Botanical Garden 75: 1658-1662. figs 1, 2B, and 3. 1988. Figs 36-37")
            .warning(Warnings.NOMENCLATURAL_REFERENCE)
            .nothingElse();

    assertName("Passiflora eglandulosa J.M. MacDougal. Lingua franca de Missouri Botanical Garden 75: 1658-1662. figs 1, 2B, and 3. 1988. Figs 36-37", "Passiflora eglandulosa")
            .species("Passiflora", "eglandulosa")
            .combAuthors(null, "J.M.MacDougal")
            .publishedIn("Lingua franca de Missouri Botanical Garden 75: 1658-1662. figs 1, 2B, and 3. 1988. Figs 36-37")
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

    // a literal "Null" genus is a data artefact too — flag doubtful like a null epithet
    assertName("Null bactus", "Null bactus")
            .species("Null", "bactus")
            .doubtful()
            .warning(Warnings.NULL_EPITHET)
            .nothingElse();

    assertName("Null", "Null")
            .monomial("Null")
            .doubtful()
            .warning(Warnings.NULL_EPITHET)
            .nothingElse();

    assertUnparsable("Unidentified unidentified Hood", PLACEHOLDER);

    assertUnparsable("Abies unidentified", PLACEHOLDER);

    assertName("Passiflora possible Müller", "Passiflora possible")
            .species("Passiflora", "possible")
            .combAuthors(null, "Müller")
            .doubtful()
            .warning(Warnings.BLACKLISTED_EPITHET)
            .nothingElse();
  }

  @Test
  public void authorWithPublication() throws Exception {
    // in publications often give the year the name was published. We propagate that to the authorship instance
    // and for botanical names we simply don't show it in the NameFormatter

    // these are difficult to find the cutoff between author and the publishedIn reference
    assertName("Passiflora eglandulosa J.M. MacDougal. Annals of the Missouri Botanical Garden 75. figs 1, 2B, and 3. 1988. Figs 36-37", "Passiflora eglandulosa")
        .species("Passiflora", "eglandulosa")
        .combAuthors("1988", "J.M.MacDougal")
        .publishedIn("Annals of the Missouri Botanical Garden 75. figs 1, 2B, and 3. 1988. Figs 36-37")
        .nothingElse();

    // IPNI botanical style, reference after a comma behind the (abbreviated?) author
    assertName("Samyda arborea Rich., Actes Soc. Hist. Nat. Paris 1: 109 (1792).", "Samyda arborea")
        .species("Samyda", "arborea")
        .combAuthors("1792", "Rich.")
        .publishedIn("Actes Soc. Hist. Nat. Paris 1: 109 (1792)")
        .nothingElse();

    assertName("Casearia arborea Urb., Symb. Antill. (Urban). 4(3): 421 (1910).", "Casearia arborea")
        .species("Casearia", "arborea")
        .combAuthors("1910", "Urb.")
        .publishedIn("Symb. Antill. (Urban). 4(3): 421 (1910)")
        .nothingElse();

    assertName("Abuta candicans Rich. in DC., Syst. Nat. 1: 543 (1817).", "Abuta candicans")
        .species("Abuta", "candicans")
        .combAuthors("1817", "Rich.")
        .publishedIn("DC., Syst. Nat. 1: 543 (1817)")
        .nothingElse();

    assertName("Antacanthus Rich. ex DC., Prodr. 4: 484 (1830), pro syn.", "Antacanthus")
        .monomial("Antacanthus")
        .combAuthors("1830", "DC.")
        .combExAuthors("Rich.")
        .publishedIn("Prodr. 4: 484 (1830)")
        .nomNote("pro syn.")
        .nothingElse();

    assertName("Aegiphila pyramidata Rich. ex Moldenke, Phytologia 1: 204, in obs., pro syn. (1937).", "Aegiphila pyramidata")
        .species("Aegiphila", "pyramidata")
        .combAuthors("1937", "Moldenke")
        .combExAuthors("Rich.")
        .publishedIn("Phytologia 1: 204, (1937)")
        .nomNote("in obs., pro syn.")
        .nothingElse();

    assertName("Amplexoididae Wang, Guang-Xu in Wang, He, Tang & Percival, 2018","Amplexoididae")
        .monomial("Amplexoididae")
        .combAuthors("2018", "Wang", "Guang-Xu")
        .publishedIn("Wang, He, Tang & Percival, 2018")
        .code(ZOOLOGICAL)
        .nothingElse();

    assertName("Roelofinae St Laurent & Kawahara in St Laurent, Mielke, Herbin, Dexter & Kawahara 2020", "Roelofinae")
        .monomial("Roelofinae")
        .combAuthors("2020", "St Laurent", "Kawahara")
        .publishedIn("St Laurent, Mielke, Herbin, Dexter & Kawahara 2020")
        .code(ZOOLOGICAL)
        .nothingElse();

    assertName("Charlottea Whalen & Carter in Carter, Whalen & Guex, 1998", "Charlottea")
        .monomial("Charlottea")
        .combAuthors("1998", "Whalen", "Carter")
        .publishedIn("Carter, Whalen & Guex, 1998")
        .code(ZOOLOGICAL)
        .nothingElse();
  }


  @Test
  public void zoobank() throws Exception {
    assertName("Euplexauridae McFadden, van Ofwegen & Quattrini, 2022", "Euplexauridae")
        .monomial("Euplexauridae")
        .combAuthors("2022", "McFadden", "van Ofwegen", "Quattrini")
        .code(ZOOLOGICAL)
        .nothingElse();

    assertName("Culexlineatascinciina Hoser, 2015", "Culexlineatascinciina")
        .monomial("Culexlineatascinciina")
        .combAuthors("2015", "Hoser")
        .code(ZOOLOGICAL)
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
    // bad rank given
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
            .nomNote("nom. nud.")
            .nothingElse();

    assertName("Achillea millefolium subsp. pallidotegula B. Boivin var. pallidotegula", "Achillea millefolium var. pallidotegula")
            .infraSpecies("Achillea", "millefolium", Rank.VARIETY, "pallidotegula")
            .warning(Warnings.QUADRINOMIAL, Warnings.REMOVED_PREFIX + "subsp. pallidotegula B.Boivin")
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
            .nomNote("nom. nud.")
            .nothingElse();

    // hort. = from hortulanorum (“of gardens”), the name was used in cultivation (nurseries, gardens, horticultural trade)
    //         often without valid scientific publication sometimes applied loosely or incorrectly
    // ex Dallim. = William Dallimore later validly published the name
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
            .infraGeneric("Narcissus", SERIES_BOTANY, "Dubizettae")
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
  /**
   * Autonym authorship is the species author and follows the nomenclatural codes:
   * ICN Art. 22.1/26.1 (botany) cite it after the species epithet, ICZN (zoology) at the
   * very end of the trinomen. The autonym's final epithet never carries an author itself.
   * https://www.iapt-taxon.org/icbn/frameset/0026Ch3Sec3a022.htm (ICN Art. 26)
   * https://code.iczn.org/types-in-the-species-group/article-72-general-provisions/ (ICZN Art. 72)
   */
  @Test
  public void autonymAuthorship() throws Exception {
    // botanical: species author after the species epithet, none after the autonym
    ParsedName acer = parser.parse("Acer rubrum L. var. rubrum", null, null, NomCode.BOTANICAL);
    assertEquals("rubrum", acer.getSpecificEpithet());
    assertEquals("rubrum", acer.getInfraspecificEpithet());
    assertTrue(acer.isAutonym());
    assertEquals("L.", org.gbif.nameparser.util.NameFormatter.authorshipComplete(acer));
    assertEquals("Acer rubrum L. var. rubrum",
        org.gbif.nameparser.util.NameFormatter.canonical(acer));

    // botanical recombination with basionym + combination author before the marker
    ParsedName trim = parser.parse("Trimezia spathata (Klatt) Baker subsp. spathata", null, null, null);
    assertTrue(trim.isAutonym());
    assertEquals("spathata", trim.getSpecificEpithet());
    assertEquals("spathata", trim.getInfraspecificEpithet());
    assertNull(trim.getInfragenericEpithet());
    assertEquals(NomCode.BOTANICAL, trim.getCode());
    assertEquals("(Klatt) Baker", org.gbif.nameparser.util.NameFormatter.authorshipComplete(trim));
    assertEquals("Trimezia spathata (Klatt) Baker subsp. spathata",
        org.gbif.nameparser.util.NameFormatter.canonical(trim));

    // zoological autonym: author at the very end, no rank marker
    ParsedName vul = parser.parse("Vulpes vulpes vulpes Linnaeus, 1758", null, null, NomCode.ZOOLOGICAL);
    assertTrue(vul.isAutonym());
    assertEquals("Vulpes vulpes vulpes Linnaeus, 1758",
        org.gbif.nameparser.util.NameFormatter.canonical(vul));

    // botanical autonym with no author renders cleanly without one
    ParsedName bare = parser.parse("Acer rubrum var. rubrum", null, null, NomCode.BOTANICAL);
    assertTrue(bare.isAutonym());
    assertEquals("Acer rubrum var. rubrum",
        org.gbif.nameparser.util.NameFormatter.canonical(bare));
  }

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
    assertUnparsable("Gen.nov. sp.nov.", NameType.OTHER);
    assertUnparsable("Gen.nov.", NameType.OTHER);

    assertName("Aster indet.", "Aster sp.").species("Aster", null).type(NameType.INFORMAL).warning(Warnings.INDETERMINED).nothingElse();
    assertUnparsable("Asteraceae incertae sedis", PLACEHOLDER);
    assertUnparsable("unassigned Abies", PLACEHOLDER);
    assertUnparsable("Unident-Boraginaceae", PLACEHOLDER);
    assertUnparsable("Unident", PLACEHOLDER);
    assertUnparsable("IncertaeSedis justi", PLACEHOLDER);
    // IPNI underscore-joined placeholder, https://github.com/CatalogueOfLife (name-parser v4 item 3)
    assertUnparsable("Incertae_sedis", PLACEHOLDER);
    assertUnparsable("Incertae_sedis", Rank.FAMILY, PLACEHOLDER);
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

    assertName("Agaricus ericetorum Pers. : Fr.", "Agaricus ericetorum")
        .species("Agaricus", "ericetorum")
        .combAuthors(null, "Pers.")
        .sanctAuthor("Fr.")
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
   * but treat them as a informal names.
   */
  @Test
  public void unparsablePlaceholders() throws Exception {
    assertUnparsable("Iteaphila-group", INFORMAL);
    assertUnparsable("Bartonella group", INFORMAL);
  }

  /**
   * "-lineage" labels are informal phylogenetic group names that, like the "-group" /
   * "-complex" aggregates, can refer to any rank and so are treated as INFORMAL.
   * Unlike those, the stem is often an OTU-/strain-like code with digits or a lowercase
   * start (NC12A-lineage, he2-lineage).
   */
  @Test
  public void lineageInformal() throws Exception {
    assertUnparsable("Vermistella-lineage", INFORMAL);
    assertUnparsable("Flamella-lineage", INFORMAL);
    assertUnparsable("Pessonella-lineage", INFORMAL);
    assertUnparsable("NC12A-lineage", INFORMAL);
    assertUnparsable("he2-lineage", INFORMAL);
  }

  /**
   * Regression tests graduated from TODO-names.txt: names that the parser now handles
   * correctly. The still-unsupported entries from that file are documented (with their
   * desired parse) in {@link #todoNamesUnsupported()} below.
   */
  @Test
  public void todoNames() throws Exception {
    assertName("Pseudoleptomesochrella incerta (Chap. and Delam. -deb., 1956)", "Pseudoleptomesochrella incerta")
            .species("Pseudoleptomesochrella", "incerta")
            .basAuthors("1956", "Chap.", "Delam.-deb.")
            .code(ZOOLOGICAL)
            .nothingElse();

    // homoglyph "?" in the author abbreviation cannot be recovered, so it is stripped
    assertName("Rosa intermedia Cr?p.", "Rosa intermedia")
            .species("Rosa", "intermedia")
            .combAuthors(null, "Crp.")
            .doubtful()
            .warning(Warnings.QUESTION_MARKS_REMOVED)
            .nothingElse();

    assertName("Rosa alpestris D?s?gl.", "Rosa alpestris")
            .species("Rosa", "alpestris")
            .combAuthors(null, "Dsgl")
            .doubtful()
            .warning(Warnings.QUESTION_MARKS_REMOVED)
            .nothingElse();

    assertName("Digitaria sanguinea Weber, orth. var.", "Digitaria sanguinea")
            .species("Digitaria", "sanguinea")
            .combAuthors(null, "Weber")
            .nomNote("orth. var.")
            .nothingElse();

    assertName("Quercus aquifolia Kotschy ex A.DC., nom. subnud.", "Quercus aquifolia")
            .species("Quercus", "aquifolia")
            .combExAuthors("Kotschy")
            .combAuthors(null, "A.DC.")
            .nomNote("nom. subnud.")
            .nothingElse();

    assertName("Spermacoce lanceolata Frank ex C.Presl, pro syn.", "Spermacoce lanceolata")
            .species("Spermacoce", "lanceolata")
            .combExAuthors("Frank")
            .combAuthors(null, "C.Presl")
            .nomNote("pro syn.")
            .nothingElse();

    assertName("Cavendishia polyantha H?rold, pro syn.", "Cavendishia polyantha")
            .species("Cavendishia", "polyantha")
            .combAuthors(null, "Hrold")
            .nomNote("pro syn.")
            .doubtful()
            .warning(Warnings.QUESTION_MARKS_REMOVED)
            .nothingElse();

    assertName("Leucopogon veillonii (Virot) comb. ined.", "Leucopogon veillonii")
            .species("Leucopogon", "veillonii")
            .basAuthors(null, "Virot")
            .nomNote("comb. ined.")
            .manuscript()
            .nothingElse();

    assertName("Vernoniastrum musofense var. miamensis (S. Moore) comb. ined.", "Vernoniastrum musofense var. miamensis")
            .infraSpecies("Vernoniastrum", "musofense", VARIETY, "miamensis")
            .basAuthors(null, "S.Moore")
            .nomNote("comb. ined.")
            .manuscript()
            .code(BOTANICAL)
            .nothingElse();

    assertName("Spermacoce tenuis Sess? & Moc., orth. var.", "Spermacoce tenuis")
            .species("Spermacoce", "tenuis")
            .combAuthors(null, "Sess", "Moc.")
            .nomNote("orth. var.")
            .nothingElse();

    assertName("Quercus serra Liebm., non Unger (1845), fossil name.", "Quercus serra")
            .species("Quercus", "serra")
            .combAuthors(null, "Liebm.")
            .sensu("non Unger (1845), fossil name.")
            .nothingElse();

    assertName("Ceratellopsis acuminata sensu Bourdot & Galzin (1927); fide Checklist of Basidiomycota of Great Britain and Ireland (2005)", "Ceratellopsis acuminata")
            .species("Ceratellopsis", "acuminata")
            .sensu("sensu Bourdot & Galzin (1927); fide Checklist of Basidiomycota of Great Britain and Ireland (2005)")
            .nothingElse();

    assertName("Agaricus rosellus sensu Withering [Bot. Arr. Brit. Pl. 1: 237 (1787)]; fide Checklist of Basidiomycota of Great Britai", "Agaricus rosellus")
            .species("Agaricus", "rosellus")
            .sensu("sensu Withering [Bot. Arr. Brit. Pl. 1: 237 (1787)]; fide Checklist of Basidiomycota of Great Britai")
            .nothingElse();

    assertName("Roridomyces albororidus (Maas Geest. & de Meijer) anon., 2010", "Roridomyces albororidus")
            .species("Roridomyces", "albororidus")
            .basAuthors(null, "Maas Geest.", "de Meijer")
            .combAuthors("2010", "anon.")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Collybia mephitica sensu Rea (1922); fide Checklist of Basidiomycota of Great Britain and Ireland (2005)", "Collybia mephitica")
            .species("Collybia", "mephitica")
            .sensu("sensu Rea (1922); fide Checklist of Basidiomycota of Great Britain and Ireland (2005)")
            .nothingElse();

    assertName("Russula amoena sensu A.A. Pearson [Naturalist (1948)]; fide Checklist of Basidiomycota of Great Britain and Ireland", "Russula amoena")
            .species("Russula", "amoena")
            .sensu("sensu A.A. Pearson [Naturalist (1948)]; fide Checklist of Basidiomycota of Great Britain and Ireland")
            .nothingElse();

    assertName("Puccinia veronicarum sensu Grove (1913) p.p.; fide Checklist of Basidiomycota of Great Britain and Ireland (2005)", "Puccinia veronicarum")
            .species("Puccinia", "veronicarum")
            .sensu("sensu Grove (1913) p.p.; fide Checklist of Basidiomycota of Great Britain and Ireland (2005)")
            .nothingElse();

    assertName("Uroleptopsis viridis (Perejaslawzewa, 1886) ?", "Uroleptopsis viridis")
            .species("Uroleptopsis", "viridis")
            .basAuthors("1886", "Perejaslawzewa")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Apatura iris junonina (Lambillion) & Cabeau, 1910", "Apatura iris junonina")
            .infraSpecies("Apatura", "iris", SUBSPECIES, "junonina")
            .basAuthors(null, "Lambillion")
            .combAuthors("1910", "Cabeau")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Scleropogon kelloggi (Wilcox, 137)", "Scleropogon kelloggi")
            .species("Scleropogon", "kelloggi")
            .basAuthors("137", "Wilcox")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Ospriocerus arizonensis (Bromley, 193k7)", "Ospriocerus arizonensis")
            .species("Ospriocerus", "arizonensis")
            .basAuthors("193", "Bromley")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Lepidanthrax coquilletti Evenhuis and Hall, 0000", "Lepidanthrax coquilletti")
            .species("Lepidanthrax", "coquilletti")
            .combAuthors("0000", "Evenhuis", "Hall")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Scopula cajanderi (Herz, 1903), 1903-01-01", "Scopula cajanderi")
            .species("Scopula", "cajanderi")
            .basAuthors("1903", "Herz")
            .combAuthors("1903")
            .code(ZOOLOGICAL)
            .warning(Warnings.YEAR_INTERPRETED)
            .nothingElse();

    assertName("Gnathamitermes tubiformans (Buckley, 1862), 1862-01-01", "Gnathamitermes tubiformans")
            .species("Gnathamitermes", "tubiformans")
            .basAuthors("1862", "Buckley")
            .combAuthors("1862")
            .code(ZOOLOGICAL)
            .warning(Warnings.YEAR_INTERPRETED)
            .nothingElse();

    assertName("Cyclanthera explodens var. intermedia Cogn. in Kuntze ex Kuntze", "Cyclanthera explodens var. intermedia")
            .infraSpecies("Cyclanthera", "explodens", VARIETY, "intermedia")
            .combAuthors(null, "Cogn.")
            .publishedIn("Kuntze ex Kuntze")
            .nothingElse();

    assertName("Pseudostenophylax clavatus Tian & Li in Tian, Li, Yang & Sun, in Chen, editor, 1993", "Pseudostenophylax clavatus")
            .species("Pseudostenophylax", "clavatus")
            .combAuthors("1993", "Tian", "Li")
            .publishedIn("Tian, Li, Yang & Sun, in Chen, editor, 1993")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Kanimia nitida (DC:) Baker", "Kanimia nitida")
            .species("Kanimia", "nitida")
            .basAuthors(null, "DC")
            .combAuthors(null, "Baker")
            .code(BOTANICAL)
            .nothingElse();
  }

  /**
   * Names from TODO-names.txt that the parser still gets wrong. Each assertion encodes the
   * desired parse; the inline comment describes the current bug. The method is @Ignore'd so
   * the build stays green — remove the annotation once these are fixed (and graduate the
   * fixed cases into {@link #todoNames()}). nothingElse() is intentionally omitted: only the
   * specific corrected fields are pinned, since the full target state of some fields (note
   * disposition, publishedIn cleanup) is still open.
   */
  @Ignore("desired parse for still-unsupported TODO-names.txt entries — not yet implemented")
  @Test
  public void todoNamesUnsupported() throws Exception {
    // "ex <Author>" with nothing before "ex": currently "ex" becomes an infraspecific epithet.
    assertName("Pitcairnia cinnagarina ex D. Dietr.", "Pitcairnia cinnagarina")
            .species("Pitcairnia", "cinnagarina")
            .combAuthors(null, "D.Dietr.");

    assertName("Grielum obtusifolium ex Harv.", "Grielum obtusifolium")
            .species("Grielum", "obtusifolium")
            .combAuthors(null, "Harv.");

    // Latin nomenclatural notes are currently absorbed as a second author.
    assertName("Quercus ovalis Gand., opus utique oppr.", "Quercus ovalis")
            .species("Quercus", "ovalis")
            .combAuthors(null, "Gand.")
            .nomNote("opus utique oppr.");

    assertName("Quercus meridionalis Gand., opus utique oppr.", "Quercus meridionalis")
            .species("Quercus", "meridionalis")
            .combAuthors(null, "Gand.")
            .nomNote("opus utique oppr.");

    assertName("Basanacantha spinosa var. typica K.Schum., not validly publ.", "Basanacantha spinosa var. typica")
            .infraSpecies("Basanacantha", "spinosa", VARIETY, "typica")
            .combAuthors(null, "K.Schum.")
            .nomNote("not validly publ.");

    assertName("Fraxinus humilior Garsault, opus utique oppr.", "Fraxinus humilior")
            .species("Fraxinus", "humilior")
            .combAuthors(null, "Garsault")
            .nomNote("opus utique oppr.");

    // "Later homonym of a fossil name." should be stripped, not appended as a comb author.
    assertName("Diospyros oblongifolia (Thwaites) Kosterm., Later homonym of a fossil name.", "Diospyros oblongifolia")
            .species("Diospyros", "oblongifolia")
            .basAuthors(null, "Thwaites")
            .combAuthors(null, "Kosterm.")
            .code(BOTANICAL);

    assertName("Euclea rufescens E.Mey., not validly publ.", "Euclea rufescens")
            .species("Euclea", "rufescens")
            .combAuthors(null, "E.Mey.")
            .nomNote("not validly publ.");

    assertName("Lecythis subbiflora Ruiz & Pav., no type indicated.", "Lecythis subbiflora")
            .species("Lecythis", "subbiflora")
            .combAuthors(null, "Ruiz", "Pav.")
            .nomNote("no type indicated.");

    // Trailing editorial remark should be stripped, not absorbed as an author.
    assertName("Menestoria tocoyenae DC., provisionally listed as a synonym.", "Menestoria tocoyenae")
            .species("Menestoria", "tocoyenae")
            .combAuthors(null, "DC.");

    assertName("Begonia hatacoa var. viridifolia Golding & Rekha Morris, without type.", "Begonia hatacoa var. viridifolia")
            .infraSpecies("Begonia", "hatacoa", VARIETY, "viridifolia")
            .combAuthors(null, "Golding", "Rekha Morris")
            .nomNote("without type.");

    // Unclosed "(" should still yield a basionym, not a plain combination author.
    assertName("Spilogona acuticornis (Malloch, 1920", "Spilogona acuticornis")
            .species("Spilogona", "acuticornis")
            .basAuthors("1920", "Malloch")
            .code(ZOOLOGICAL);

    assertName("Cerodontha lonicerae (Robineau-desvoidy, 1851", "Cerodontha lonicerae")
            .species("Cerodontha", "lonicerae")
            .basAuthors("1851", "Robineau-desvoidy")
            .code(ZOOLOGICAL);

    // Trailing edition letter must not become a forename initial ("A.Monod" / "D.Nunomura").
    assertName("Caecognathia regalis (Monod, 1926A)", "Caecognathia regalis")
            .species("Caecognathia", "regalis")
            .basAuthors("1926", "Monod")
            .code(ZOOLOGICAL);

    assertName("Caecognathia saikaiensis (Nunomura, 1992D)", "Caecognathia saikaiensis")
            .species("Caecognathia", "saikaiensis")
            .basAuthors("1992", "Nunomura")
            .code(ZOOLOGICAL);

    // "ap. Syr." (apud) is a publication pointer and must not be glued onto "Maxim.".
    assertName("Geranium sanguineum var. majus Maxim. ap. Syr. & Petunn. in Syr.", "Geranium sanguineum var. majus")
            .infraSpecies("Geranium", "sanguineum", VARIETY, "majus")
            .combAuthors(null, "Maxim.", "Petunn.")
            .publishedIn("Syr.");

    // Should be a basionym with year 1902; the stray ")" and date suffix must be cleaned off.
    assertName("Fidicina aldegondae (Kuhlgatz in Kuhlgatz and Melichar, 1902), 1902-01-01", "Fidicina aldegondae")
            .species("Fidicina", "aldegondae")
            .basAuthors("1902", "Kuhlgatz")
            .code(ZOOLOGICAL);

    // "ins Econ. Taxon. Bot. …" is the publication ref, not part of the "Anand Kumar" author.
    assertName("Primula chamaejasme (Wulfen) K.K. Khanna & Anand Kumar ins Econ. Taxon. Bot., 22(1): 237 (1998), isonym", "Primula chamaejasme")
            .species("Primula", "chamaejasme")
            .basAuthors(null, "Wulfen")
            .combAuthors(null, "K.K.Khanna", "Anand Kumar")
            .nomNote("isonym")
            .code(BOTANICAL);

    // "Fl. Brit. W. I. 147. 1859" is the publication ref, not a second author with year 147.
    assertName("Ilex montana var. lanceolata (Macfad.) Griseb., Fl. Brit. W. I. 147. 1859", "Ilex montana var. lanceolata")
            .infraSpecies("Ilex", "montana", VARIETY, "lanceolata")
            .basAuthors(null, "Macfad.")
            .combAuthors(null, "Griseb.")
            .code(BOTANICAL);

    // basionym Grunow (publ. in Cleve & Müller), combination D.G. Mann (in Round et al., 1990).
    assertName("Tryblionella marginulata (Grunow in Cleve & M?ller) D.G. Mann in Round et al., 1990", "Tryblionella marginulata")
            .species("Tryblionella", "marginulata")
            .basAuthors(null, "Grunow")
            .combAuthors(null, "D.G.Mann");

    // The trailing "?" must not survive inside the year ("1978?").
    assertName("Psilopteryx psorosa subsp. retezatica Botosaneanu & ?Schneider, 1978?", "Psilopteryx psorosa retezatica")
            .infraSpecies("Psilopteryx", "psorosa", SUBSPECIES, "retezatica")
            .combAuthors("1978", "Botosaneanu", "Schneider")
            .code(ZOOLOGICAL)
            .doubtful();

    // "(auct.)" should become the taxonomic note, not be left unparsed (state PARTIAL).
    assertName("Osmanthus ilicifolius f. variegatus (auct.) Rehder", "Osmanthus ilicifolius f. variegatus")
            .infraSpecies("Osmanthus", "ilicifolius", FORM, "variegatus")
            .combAuthors(null, "Rehder")
            .sensu("auct.");

    // "?" is a homoglyph for the hybrid sign "×" → nothospecies, not an INFORMAL "?" placeholder.
    assertName("Magnolia ?soulangeana Hamel (pro sp.)", "Magnolia × soulangeana")
            .species("Magnolia", "soulangeana")
            .notho(SPECIFIC)
            .combAuthors(null, "Hamel")
            .nomNote("pro sp.");
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

    assertName("Candidatus Liberibacter solanacearum", "\"Candidatus Liberibacter solanacearum\"")
        .species("Liberibacter", "solanacearum")
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
  @Ignore("very odd names - rare and no priority")
  public void oddFungiRanksUnsupported() throws Exception {
    assertName("Capitularia fimbriata ⍺ vulgaris 3 tubaeformis *** carpophora", "Capitularia fimbriata carpophora")
            .infraSpecies("Capitularia", "fimbriata", INFRASPECIFIC_NAME, "carpophora")
            .nothingElse();

    assertName("Capitularia pyxidata ß longipes H. carpophora Floerke", "Capitularia pyxidata carpophora")
            .infraSpecies("Capitularia", "pyxidata", INFRASPECIFIC_NAME, "carpophora")
            .nothingElse();
  }

  @Test
  public void strains() throws Exception {
    assertName("Endobugula sp. JYr4", "Endobugula sp. JYr4")
            .species("Endobugula", null)
            .phrase("JYr4")
            .type(NameType.INFORMAL)
            .nothingElse();

    assertPhraseName("Lepidoptera sp. JGP0404", "Lepidoptera sp. JGP0404", SPECIES, "JGP0404")
            .species("Lepidoptera", null)
            .phrase("JGP0404")
            .nothingElse();

    // avoid author & year to be accepted as strain
    assertName("Anniella nigra FISCHER 1885", "Anniella nigra")
            .species("Anniella", "nigra")
            .combAuthors("1885", "Fischer")
            .code(ZOOLOGICAL)
            .nothingElse();
  }


  @Test
  public void norwegianRadiolaria() throws Exception {
    assertName("Actinomma leptodermum longispinum Cortese & Bjørklund 1998", "Actinomma leptodermum longispinum")
            .infraSpecies("Actinomma", "leptodermum", SUBSPECIES, "longispinum")
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


  /**
   * cv. Abbreviation of cultivar
   * A formal category under the cultivated plant code (ICNCP), not the botanical code
   * Must follow strict formatting rules:
   *  - not italicized
   *  - capitalized
   *  - usually in quotes
   *
   * Correct style: Abies brevifolia 'Short Needle'
   * (or historically: Abies brevifolia cv. 'Short Needle')
   */
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
    assertUnparsable(name, FORMULA);
  }

  @Test
  public void oTU() throws Exception {

    // https://github.com/gbif/name-parser/issues/74
    assertName("Desulfobacterota_B", "Desulfobacterota B")
            .phraseName("Desulfobacterota", "B")
            .nothingElse();
    // unparsable identifiers
    assertUnparsable("UBA3054", NameType.OTHER);
    assertUnparsable("F0040", NameType.OTHER);
    assertUnparsable("AABM5-125-24", NameType.OTHER);
    assertUnparsable("B130-G9", NameType.OTHER);
    assertUnparsable("BMS3Abin14", NameType.OTHER);
    assertUnparsable("4572-55", NameType.OTHER);
    assertUnparsable("T1SED10-198M", NameType.OTHER);
    assertUnparsable("BMS3Abin14", NameType.OTHER);
    assertUnparsable("UBA11359_C", NameType.OTHER);
    assertUnparsable("01-FULL-45-15b", NameType.OTHER);
    assertUnparsable("E44-bin80", NameType.OTHER);
    assertUnparsable("E2", NameType.OTHER);
    assertUnparsable("9FT-COMBO-53-11", NameType.OTHER);
    assertUnparsable("AqS3", NameType.OTHER);
    assertUnparsable("Gp7-AA8", NameType.OTHER);
    assertUnparsable("0-14-0-10-38-17 sp002774085", NameType.OTHER);
    assertUnparsable("01-FULL-45-15b sp001822655", NameType.OTHER);
    assertUnparsable("18JY21-1 sp004344915", NameType.OTHER);
    assertUnparsable("SH1508347.08FU", NameType.OTHER);
    assertUnparsable("SH19186714.17FU", NameType.OTHER);
    assertUnparsable("SH191814.08FU", NameType.OTHER);
    assertUnparsable("SH191814.04FU", NameType.OTHER);
    assertUnparsable("BOLD:ACW2100", NameType.OTHER);
    assertUnparsable("BOLD:ACW2100", NameType.OTHER);
    assertUnparsableName(" BOLD:ACW2100 ", UNRANKED, NameType.OTHER, "BOLD:ACW2100");
    assertUnparsableName("Festuca sp. BOLD:ACW2100", UNRANKED, NameType.OTHER, "BOLD:ACW2100");
    assertUnparsableName("sh460441.07fu", UNRANKED, NameType.OTHER, "SH460441.07FU");

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

  // https://github.com/CatalogueOfLife/data/issues/1079
  @Test
  public void caseSensitive() throws Exception {
    assertName("CHIONE elevata", "Chione elevata")
            .species("Chione", "elevata")
            .nothingElse();

    assertName("CHIONE ELEVATA", "Chione elevata")
            .species("Chione", "elevata")
            .nothingElse();

    assertName("CHIONE ELEVATA VULGARIS", "Chione elevata vulgaris")
            .infraSpecies("Chione", "elevata", INFRASPECIFIC_NAME, "vulgaris")
            .nothingElse();

    assertName("CHIONE ELEVÄTA", "Chione")
            .monomial("Chione")
            .combAuthors(null, "Eleväta")
            .nothingElse();

    assertName("CHIONE ELEV.", "Chione")
            .monomial("Chione")
            .combAuthors(null, "Elev.")
            .nothingElse();

    assertName("chione elevata", "Chione elevata")
            .species("Chione", "elevata")
            .nothingElse();

    assertName("chione elevata vulgaris", "Chione elevata vulgaris")
            .infraSpecies("Chione", "elevata", INFRASPECIFIC_NAME, "vulgaris")
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

    assertName("Cyclotus amethystinus var. β Guppy, 1868", "Cyclotus amethystinus var. β")
            .infraSpecies("Cyclotus", "amethystinus", VARIETY, "β")
            .combAuthors("1868", "Guppy")
            .code(ZOOLOGICAL)
            .nothingElse();
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

    // cedilla inside an epithet must not split the word (gbif/name-parser#104)
    assertName("Euphrasia mendonçae", "Euphrasia mendonçae")
            .species("Euphrasia", "mendonçae")
            .nothingElse();

    assertName("Viola bocquetiana S. Yildirimli", "Viola bocquetiana")
            .species("Viola", "bocquetiana")
            .combAuthors(null, "S.Yildirimli")
            .nothingElse();

    assertName("Anatolidamnicola gloeri gloeri Şahin, Koca & Yildirim, 2012", "Anatolidamnicola gloeri gloeri")
            .infraSpecies("Anatolidamnicola", "gloeri", Rank.SUBSPECIES, "gloeri")
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

    // Autonym authors are the species authors (ICN Art. 22.1/26.1): the autonym's final
    // epithet carries no author, but the species author "d'Urv." is captured and rendered
    // after the species epithet.
    assertName("Cirsium creticum d'Urv. subsp. creticum", "Cirsium creticum subsp. creticum")
            .infraSpecies("Cirsium", "creticum", SUBSPECIES, "creticum")
            .combAuthors(null, "d'Urv.")
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
            .imprintYear("1897")
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

    assertName("Stenosigma humerale Giordani Soika, 1990", "Stenosigma humerale")
            .species("Stenosigma", "humerale")
            .combAuthors("1990", "Giordani Soika")
            .code(ZOOLOGICAL)
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
   * Test all names in other.txt and makes sure they are not parsable.
   */
  @Test
  public void otherFile() throws Exception {
    for (String name : iterResource("other.txt")) {
      try {
        parser.parse(name);
        fail("Expected " + name + " to be unparsable");
      } catch (UnparsableNameException ex) {
        assertEquals("Bad name type for: "+name, NameType.OTHER, ex.getType());
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
        assertEquals(NameType.FORMULA, ex.getType());
        assertEquals(name, ex.getName());
      }
    }
  }

  /**
   * Test all names in other.txt and makes sure they are NO_NAMEs.
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
    // https://github.com/gbif/portal-feedback/issues/5326#issuecomment-2107283007
    assertName("Foa fo", "Foa fo")
            .species("Foa", "fo")
            .type(SCIENTIFIC)
            .nothingElse();

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
            .warning(Warnings.ABBREVIATED_GENUS)
            .nothingElse();

    assertName("B.", "B.")
            .monomial("B.")
            .type(INFORMAL)
            .warning(Warnings.ABBREVIATED_GENUS)
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
            .nomNote("nom. illeg.")
            .code(BOTANICAL)
            .sensu("non. A.lancea.")
            .nothingElse();

    //TODO: pro syn.
    assertName("Combretum Loefl. (1758), nom. cons. [= Grislea L. 1753].", "Combretum")
            .monomial("Combretum")
            .combAuthors("1758", "Loefl.")
            .nomNote("nom. cons.")
            .doubtful()
            .partial("[= Grislea L. 1753].")
            .code(NomCode.ZOOLOGICAL)
            .nothingElse();

    assertName("Anthurium lanceum Engl. nom.illeg.", "Anthurium lanceum")
            .species("Anthurium", "lanceum")
            .combAuthors(null, "Engl.")
            .nomNote("nom. illeg.")
            .code(BOTANICAL)
            .nothingElse();

  }


  @Test
  public void testAuthorteam() throws Exception {
    assertAuthorship("Jarocki or Schinz",  "Jarocki or Schinz");
    assertAuthorship("van der Wulp",  "van der Wulp");
    assertAuthorship("Balsamo M Fregni E Tongiorgi MA", "M.Balsamo", "E.Fregni", "M.A.Tongiorgi");
    assertAuthorship("Walker, F.",  "F.Walker");
    assertAuthorship("Walker, F",  "F.Walker");
    assertAuthorship("Walker F",  "F.Walker");
    assertAuthorship("YJ Wang & ZQ Liu", "YJ Wang", "ZQ Liu");
    assertAuthorship("Y.-j. Wang & Z.-q. Liu", "Y.-j.Wang", "Z.-q.Liu");
    assertAuthorship("Petzold & G.Kirchn.",  "Petzold", "G.Kirchn.");
    assertAuthorship("Britton, Sterns, & Poggenb.",  "Britton", "Sterns", "Poggenb.");
    assertAuthorship("Van Heurck & Müll. Arg.",  "Van Heurck", "Müll.Arg.");
    assertAuthorship("Gruber-Vodicka",  "Gruber-Vodicka");
    assertAuthorship("Gruber-Vodicka et al.",  "Gruber-Vodicka", "al.");
    assertSingleAuthor("L.");
    assertSingleAuthor("Lin.");
    assertSingleAuthor("Linné");
    assertSingleAuthor("DC.");
    assertSingleAuthor("de Chaudoir");
    assertSingleAuthor("Hilaire");
    assertAuthorship("St. Hilaire","St.Hilaire");
    assertAuthorship("Geoffroy St. Hilaire",  "Geoffroy St.Hilaire");
    assertSingleAuthor("Acev.-Rodr.");
    assertAuthorship("Steyerm., Aristeg. & Wurdack","Steyerm.","Aristeg.", "Wurdack");
    assertAuthorship("Du Puy & Labat", "Du Puy","Labat");
    assertSingleAuthor("Baum.-Bod.");
    assertAuthorship("Engl. & v. Brehmer","Engl.", "v.Brehmer");
    assertAuthorship("F. v. Muell.",  "F.v.Muell.");
    assertAuthorship("W.J.de Wilde & Duyfjes",  "W.J.de Wilde", "Duyfjes");
    assertSingleAuthor("C.E.M.Bicudo");
    assertSingleAuthor("Alves-da-Silva");
    assertAuthorship("Alves-da-Silva & C.E.M.Bicudo",  "Alves-da-Silva", "C.E.M.Bicudo");
    assertSingleAuthor("Kingdon-Ward");
    assertAuthorship("Merr. & L.M.Perry",  "Merr.", "L.M.Perry");
    assertAuthorship("Calat., Nav.-Ros. & Hafellner",  "Calat.", "Nav.-Ros.", "Hafellner");
    assertSingleAuthor("Barboza du Bocage");
    assertAuthorship("Payri & P.W.Gabrielson",  "Payri", "P.W.Gabrielson");
    assertAuthorship("N'Yeurt, Payri & P.W.Gabrielson",  "N'Yeurt", "Payri", "P.W.Gabrielson");
    assertSingleAuthor("VanLand.");
    assertSingleAuthor("MacLeish");
    assertSingleAuthor("Monterosato ms.");
    assertAuthorship("Arn. ms., Grunow",  "Arn.ms.", "Grunow");
    assertAuthorship("Choi,J.H.; Im,W.T.; Yoo,J.S.; Lee,S.M.; Moon,D.S.; Kim,H.J.; Rhee,S.K.; Roh,D.H.",
        "J.H.Choi", "W.T.Im", "J.S.Yoo", "S.M.Lee", "D.S.Moon", "H.J.Kim", "S.K.Rhee", "D.H.Roh");
    assertAuthorship("da Costa Lima",  "da Costa Lima");
    assertAuthorship("Krapov., W.C.Greg. & C.E.Simpson",  "Krapov.", "W.C.Greg.", "C.E.Simpson");
    assertAuthorship("de Jussieu",  "de Jussieu");
    assertAuthorship("van-der Land",  "van-der Land");
    assertAuthorship("van der Land",  "van der Land");
    assertAuthorship("van Helmsick",  "van Helmsick");
    assertAuthorship("Xing, Yan & Yin",  "Xing", "Yan", "Yin");
    assertAuthorship("Xiao & Knoll",  "Xiao", "Knoll");
    assertAuthorship("Wang, Yuwen & Xian-wei Liu",  "Wang", "Yuwen", "Xian-wei Liu");
    assertAuthorship("Liu, Xian-wei, Z. Zheng & G. Xi",  "Liu", "Xian-wei", "Z.Zheng", "G.Xi");
    assertAuthorship("Clayton, D.H.; Price, R.D.; Page, R.D.M.",  "D.H.Clayton", "R.D.Price", "R.D.M.Page");
    assertAuthorship("Michiel de Ruyter",  "Michiel de Ruyter");
    assertAuthorship("DeFilipps",  "DeFilipps");
    assertAuthorship("Henk 't Hart",  "Henk 't Hart");
    assertAuthorship("P.E.Berry & Reg.B.Miller",  "P.E.Berry", "Reg.B.Miller");
    assertAuthorship("'t Hart",  "'t Hart");
    assertAuthorship("Abdallah & Sa'ad",  "Abdallah", "Sa'ad");
    assertSingleAuthor("Linnaeus filius");
    assertAuthorship("Bollmann, M.Y.Cortés, Kleijne, J.B.Østerg. & Jer.R.Young",  "Bollmann", "M.Y.Cortés", "Kleijne", "J.B.Østerg.", "Jer.R.Young");
    assertAuthorship("Branco, M.T.P.Azevedo, Sant'Anna & Komárek",  "Branco", "M.T.P.Azevedo", "Sant'Anna", "Komárek");
    assertSingleAuthor("Janick Hendrik van Kinsbergen");
    assertSingleAuthor("Jan Hendrik van Kinsbergen");
    assertSingleAuthor("Sainte-Claire Deville");
    assertSingleAuthor("Semenov-Tian-Shanskij");
    assertAuthorship("Semenov-Tian-Shanskij, Sainte-Claire Deville, Janick Hendrik van Kinsbergen",  "Semenov-Tian-Shanskij", "Sainte-Claire Deville", "Janick Hendrik van Kinsbergen");
    assertSingleAuthor("Scotto la Massese");
    assertSingleAuthor("An der Lan");
    assertAuthorship("Bor & s'Jacob",  "Bor", "s'Jacob");
    assertSingleAuthor("Brunner von Wattenwyl v.W.");
    // spanish "et"
    assertAuthorship("Martinez y Saez", "Martinez", "Saez");
    // not two separate names — a compound surname (family name), common in Portuguese-speaking cultures like Portugal and Brazil.
    assertSingleAuthor("Da Silva e Castro");
    assertAuthorship("LafuenteRoca & Carbonell",  "LafuenteRoca", "Carbonell");
    assertAuthorship("Mas-ComaBargues & Esteban",  "Mas-ComaBargues", "Esteban");
    assertSingleAuthor("Hondt d");
    assertSingleAuthor("Abou-El-Naga");
    assertAuthorship("Yong Wang bis, Y. Song, K. Geng & K.D. Hyde", "Yong Wang bis", "Y.Song", "K.Geng", "K.D.Hyde");
    assertAuthorship("Sh. Kumar, R. Singh ter, Gond & Saini", "Sh.Kumar", "R.Singh ter", "Gond", "Saini");
    assertSingleAuthor("R.Singh bis");
    assertAuthorship("zur Strassen", "zur Strassen");
    // Malformed input with stray "(" at the end — preserved verbatim as ex-authorship form.
    assertExAuthorship("Wedd. ex Sch. Bip. (", "Wedd.", "Sch.Bip.");
    assertExAuthorship("Plesn¡k ex F.Ritter", "Plesnik", "F.Ritter");
    assertAuthorship("Britton, Sterns, & Poggenb.", "Britton", "Sterns", "Poggenb.");
    assertAuthorship("Van Heurck & Müll. Arg.", "Van Heurck", "Müll.Arg.");
    assertAuthorship("Gruber-Vodicka", "Gruber-Vodicka");
    assertAuthorship("Gruber-Vodicka et al.", "Gruber-Vodicka", "al.");
    assertSingleAuthor("L.");
    assertSingleAuthor("Lin.");
    assertSingleAuthor("Linné");
    assertSingleAuthor("DC.");
    assertSingleAuthor("de Chaudoir");
    assertSingleAuthor("Hilaire");
    assertSingleAuthor("G.Don fil.");
    assertAuthorship("St. Hilaire","St.Hilaire");
    assertAuthorship("Geoffroy St. Hilaire", "Geoffroy St.Hilaire");
    assertSingleAuthor("Acev.-Rodr.");
    assertAuthorship("Steyerm., Aristeg. & Wurdack","Steyerm.","Aristeg.", "Wurdack");
    assertAuthorship("Du Puy & Labat", "Du Puy","Labat");
    assertSingleAuthor("Baum.-Bod.");
    assertAuthorship("Engl. & v. Brehmer","Engl.", "v.Brehmer");
    assertAuthorship("F. v. Muell.", "F.v.Muell.");
    assertAuthorship("W.J.de Wilde & Duyfjes", "W.J.de Wilde", "Duyfjes");
    assertSingleAuthor("C.E.M.Bicudo");
    assertSingleAuthor("Alves-da-Silva");
    assertAuthorship("Alves-da-Silva & C.E.M.Bicudo", "Alves-da-Silva", "C.E.M.Bicudo");
    assertSingleAuthor("Kingdon-Ward");
    assertAuthorship("Merr. & L.M.Perry", "Merr.", "L.M.Perry");
    assertAuthorship("Calat., Nav.-Ros. & Hafellner", "Calat.", "Nav.-Ros.", "Hafellner");
    assertExAuthorship("Arv.-Touv. ex Dörfl.", "Arv.-Touv.", "Dörfl.");
    assertAuthorship("Payri & P.W.Gabrielson", "Payri", "P.W.Gabrielson");
    assertAuthorship("N'Yeurt, Payri & P.W.Gabrielson", "N'Yeurt", "Payri", "P.W.Gabrielson");
    assertSingleAuthor("VanLand.");
    assertSingleAuthor("MacLeish");
    assertSingleAuthor("Monterosato ms.");
    assertAuthorship("Arn. ms., Grunow", "Arn.ms.", "Grunow");
    assertExAuthorship("Griseb. ex. Wedd.", "Griseb.", "Wedd.");
    assertAuthorship("Castellano, S.L.Mill., L.Singh bis & T.N.Lakh.", "Castellano", "S.L.Mill.", "L.Singh bis", "T.N.Lakh.");
    assertAuthorship("Blüthgen i.l.", "Blüthgen i.l.");
    assertAuthorship("Y.-j. Wang", "Y.-j.Wang");
    assertSingleAuthor("Z.-q.Liu");
    assertSingleAuthor("Van den heede");
    assertSingleAuthor("zur Strassen");
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

  /**
   * The longest known real, valid name (~860 chars): Homo naledi with its 47-author
   * describing team plus an equally long {@code sec.} concept reference. It must parse
   * cleanly — not get rejected by the 1000-char DoS cap — and, because it exceeds 250
   * chars, carry the LONG_NAME warning. A genuinely over-long input is still rejected.
   */
  @Test
  public void longName() throws Exception {
    String authors = "Berger, Hawks, de Ruiter, Churchill, Schmid, Delezene, Kivell, "
        + "Garvin, Williams, DeSilva, Skinner, Musiba, Cameron, Holliday, Harcourt-Smith, "
        + "Ackermann, Bastir, Bogin, Bolter, Brophy, Cofran, Congdon, Deane, Dembo, Drapeau, "
        + "Elliott, Feuerriegel, Garcia-Martinez, Green, Gurtov, Irish, Kruger, Laird, Marchi, "
        + "Meyer, Nalla, Negash, Orr, Radovcic, Schroeder, Scott, Throckmorton, Tocheri, "
        + "VanSickle, Walker, Wei & Zipfel";
    String homoNaledi = "Homo naledi " + authors + ", 2015 sec. " + authors;
    assertTrue(homoNaledi.length() > 250 && homoNaledi.length() < 1000);

    assertName(homoNaledi, "Homo naledi")
            .species("Homo", "naledi")
            .combAuthors("2015",
                "Berger", "Hawks", "de Ruiter", "Churchill", "Schmid", "Delezene", "Kivell",
                "Garvin", "Williams", "DeSilva", "Skinner", "Musiba", "Cameron", "Holliday",
                "Harcourt-Smith", "Ackermann", "Bastir", "Bogin", "Bolter", "Brophy", "Cofran",
                "Congdon", "Deane", "Dembo", "Drapeau", "Elliott", "Feuerriegel", "Garcia-Martinez",
                "Green", "Gurtov", "Irish", "Kruger", "Laird", "Marchi", "Meyer", "Nalla", "Negash",
                "Orr", "Radovcic", "Schroeder", "Scott", "Throckmorton", "Tocheri", "VanSickle",
                "Walker", "Wei", "Zipfel")
            .sensu("sec. " + authors)
            .warning(Warnings.LONG_NAME)
            .code(ZOOLOGICAL)
            .nothingElse();

    // A long-but-valid authorship (~420 chars) supplied on its own via parseAuthorship
    // still parses fine — the cap only rejects beyond 1000 chars.
    ParsedAuthorship pa = parser.parseAuthorship(authors + ", 2015", null);
    assertEquals("2015", pa.getCombinationAuthorship().getYear());
    assertEquals(47, pa.getCombinationAuthorship().getAuthors().size());
    assertEquals("Berger", pa.getCombinationAuthorship().getAuthors().get(0));
    assertEquals("Zipfel", pa.getCombinationAuthorship().getAuthors().get(46));

    // Beyond the 1000-char cap the input is rejected rather than parsed (DoS guard).
    StringBuilder tooLong = new StringBuilder("Homo naledi ");
    while (tooLong.length() <= 1000) {
      tooLong.append("Berger, ");
    }
    tooLong.append("2015");
    assertUnparsable(tooLong.toString(), NameType.OTHER);

    // The same cap guards the separately supplied authorship argument.
    StringBuilder longAuthorship = new StringBuilder();
    while (longAuthorship.length() <= 1000) {
      longAuthorship.append("Berger, ");
    }
    longAuthorship.append("2015");
    try {
      parser.parseAuthorship(longAuthorship.toString(), null);
      fail("expected over-long authorship to be rejected");
    } catch (UnparsableNameException e) {
      assertEquals(NameType.OTHER, e.getType());
    }
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

    assertSensu("Vespa emarginata Linnaeus, 1758: Fabricius, 1793", "Fabricius, 1793");
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

    // authorship-level sensu cases (sensu.txt)
    assertAuthorship("Miller sensu Busch, 1930", "Miller")
            .sensu("sensu Busch, 1930");

    // "(Author, year) sensu …": basionym authorship, sensu trails as the taxonomic note
    assertAuthorship("(Mereschkowsky, 1878) sensu Jankowski, 1992")
            .basAuthors("1878", "Mereschkowsky")
            .sensu("sensu Jankowski, 1992");

    assertName("Latrodectus marikitates sensu Whittaker", "Latrodectus marikitates")
            .species("Latrodectus", "marikitates")
            .sensu("sensu Whittaker")
            .nothingElse();

    // pure taxonomic note supplied as the authorship, no author preceding it (sensu.txt)
    assertAuthorship("sensu Turcz., p.p.")
            .combAuthors(null)
            .sensu("sensu Turcz., p.p.");
  }

  @Test
  public void nonNames() throws Exception {
    // the entire name ends up as a taxonomic note, consider this as unparsed...
    assertUnparsable("non  Ramaria fagetorum Maas Geesteranus 1976 nomen nudum = Ramaria subbotrytis sensu auct. europ.", Rank.SPECIES, NameType.OTHER);

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

    assertName("Mentha rotundifolia auct. nec Zeller, 1877", "Mentha rotundifolia")
        .species("Mentha", "rotundifolia")
        .sensu("auct. nec Zeller, 1877")
        .nothingElse();

    assertAuthorship("auct. nec Zeller, 1877")
        .sensu("auct. nec Zeller, 1877");

    assertName("Latrodectus marikitates auct. nec Whittaker", "Latrodectus marikitates")
        .species("Latrodectus", "marikitates")
        .sensu("auct. nec Whittaker")
        .nothingElse();
  }

  /**
   * Unicode apostrophe / quote variants are normalised to ASCII (' and ") in the parsed output,
   * on both the scientific name and the separately supplied authorship.
   */
  @Test
  public void quoteNormalisation() throws Exception {
    assertName("Abies alba O’Brien", "Abies alba")
            .species("Abies", "alba")
            .combAuthors(null, "O'Brien")
            .nothingElse();

    assertAuthorship("O’Brien", "O'Brien");   // U+2019 right single quotation mark
    assertAuthorship("OʼBrien", "O'Brien");   // U+02BC modifier letter apostrophe
    assertAuthorship("L´Hér.", "L'Hér.");     // U+00B4 acute accent used as apostrophe

    // In zoological nomenclature, names written like:
    //
    //'Prosthète' Hesse, 1861
    //
    //often indicate that the word is not available as a scientific name.
    // The quotation marks signal that it was published but is not recognized as a valid nomenclatural act.
    //
     // 'Prosthète' Hesse, 1861 is not a valid scientific genus name.
    //
    // It was a French vernacular (common-language) name introduced by the French zoologist Eugène Hesse in 1861 for an isopod crustacean.
    // The name was later ruled to be a vernacular term rather than an available zoological name under the ICZN.
    // The Official Index of Zoological Names explicitly lists 'Prosthète' Hesse, 1861 as "a vernacular name."
    assertName("'Prosthète' Hesse 1861", "'Prosthète'")
        .monomial("'Prosthète'")
        .doubtful() // because the quotes indicate it is not a valid scientific name
        .combAuthors("1861", "Hesse")
        .code(ZOOLOGICAL)
        .nothingElse();

    // the curly-quoted input parses identically to the ASCII-quoted one (‘Prosthète’ Hesse 1861)
    assertEquals(parser.parse("'Prosthète' Hesse 1861", null, null, null),
                 parser.parse("‘Prosthète’ Hesse 1861", null, null, null));
    assertEquals(parser.parse("\"Prosthète\" Hesse 1861", null, null, null),
                 parser.parse("“Prosthète” Hesse 1861", null, null, null));
  }

  @Test
  public void strayCharInEpithet() throws Exception {
    // a stray "!" inside an epithet (OCR/typo artefact for "pulchra") is kept as part of the
    // epithet, not split off into the authorship
    assertName("Lamprostiba pu!chra Pace, 2014", "Lamprostiba pu!chra")
            .species("Lamprostiba", "pu!chra")
            .combAuthors("2014", "Pace")
            .code(ZOOLOGICAL)
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

    // ICTV binomials that now parse as proper scientific names with code=VIRUS
    assertName("Lausannevirus", "Lausannevirus").monomial("Lausannevirus").code(NomCode.VIRUS).nothingElse();
    assertName("Clecrusatellite", "Clecrusatellite").monomial("Clecrusatellite").code(NomCode.VIRUS).nothingElse();
    assertName("Marseillevirus marseillevirus", "Marseillevirus marseillevirus")
        .species("Marseillevirus", "marseillevirus").code(NomCode.VIRUS).nothingElse();

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
  public void virusBinomialsParse() throws Exception {
    assertName("Tobamovirus tabaci", "Tobamovirus tabaci")
        .species("Tobamovirus", "tabaci").code(NomCode.VIRUS).nothingElse();
    assertName("Orthoebolavirus zairense", "Orthoebolavirus zairense")
        .species("Orthoebolavirus", "zairense").code(NomCode.VIRUS).nothingElse();
    assertName("Lausannevirus", "Lausannevirus")
        .monomial("Lausannevirus").code(NomCode.VIRUS).nothingElse();
    assertName("Coronaviridae", "Coronaviridae")
        .monomial("Coronaviridae", Rank.FAMILY).code(NomCode.VIRUS).nothingElse();
    // legacy vernacular → unparsable OTHER + code VIRUS
    assertUnparsable("Tobacco mosaic virus", NameType.OTHER, NomCode.VIRUS);
    assertUnparsable("Human papillomavirus", NameType.OTHER, NomCode.VIRUS);
    assertUnparsable("Acara virus", NameType.OTHER, NomCode.VIRUS);
  }

  @Test
  public void botanicalCodeFromSeparateRecombinationAuthorship() throws Exception {
    // A separately supplied botanical recombination authorship "(Basionym) Combination" (no year)
    // must infer BOTANICAL, just like the same authorship embedded in the name string does.
    assertName("Cerastium ligusticum subsp. granulatum", "(Huter et al.) P. D. Sell & Whitehead",
               Rank.SUBSPECIES, null, "Cerastium ligusticum subsp. granulatum")
        .infraSpecies("Cerastium", "ligusticum", Rank.SUBSPECIES, "granulatum")
        .basAuthors(null, "Huter", "al.")
        .combAuthors(null, "P.D.Sell", "Whitehead")
        .code(NomCode.BOTANICAL)
        .nothingElse();
  }

  @Test
  public void virusFalsePositiveAnimals() throws Exception {
    assertName("Aspilota vector", "Belokobylskij, 2007", Rank.SPECIES, NomCode.ZOOLOGICAL, "Aspilota vector")
        .species("Aspilota", "vector").combAuthors("2007", "Belokobylskij").code(NomCode.ZOOLOGICAL).nothingElse();
    assertName("Euragallia prion", "Euragallia prion")
        .species("Euragallia", "prion").nothingElse();
    assertName("Cryptops (Cryptops) vector", "Chamberlin, 1939", Rank.SPECIES, NomCode.ZOOLOGICAL, "Cryptops vector")
        .species("Cryptops", "Cryptops", "vector").combAuthors("1939", "Chamberlin").code(NomCode.ZOOLOGICAL).nothingElse();
    assertName("Prion", "Prion").monomial("Prion").nothingElse();
    assertName("Exochus virus", "Gauld & Sithole, 2002", Rank.SPECIES, NomCode.ZOOLOGICAL, "Exochus virus")
        .species("Exochus", "virus").combAuthors("2002", "Gauld", "Sithole").code(NomCode.ZOOLOGICAL).nothingElse();
    assertUnparsable("Acara virus", NameType.OTHER, NomCode.VIRUS);
  }

  @Test
  public void virusCallerCodeOverride() throws Exception {
    // caller asserts a non-virus code → bucket-A name parses under that code
    assertName("Tobamovirus tabaci", NomCode.ZOOLOGICAL, "Tobamovirus tabaci")
        .species("Tobamovirus", "tabaci").code(NomCode.ZOOLOGICAL);
    // caller forces VIRUS on a legacy bare-virus binomial → unparsable OTHER + VIRUS
    assertUnparsable("Acara virus", NameType.OTHER, NomCode.VIRUS);
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
   * The parser accepts name and authorship separately,
   * but the authorship can also be provided as part of the name - which happens a lot.
   * Make sure we can handle both cases.
   */
  @Test
  public void redundantAuthorship() throws Exception {
    assertName("Euplectus cavicollis LeConte, J. L., 1878", "LeConte, J. L., 1878", "Euplectus cavicollis")
        .species("Euplectus", "cavicollis")
        .combAuthors("1878", "J.L.LeConte")
        .code(ZOOLOGICAL)
        .nothingElse();

    assertName("Abies alba Mill.", (String)null, "Abies alba")
        .species("Abies", "alba")
        .combAuthors(null, "Mill.")
        .nothingElse();

    assertName("Abies alba", "Mill.", "Abies alba")
        .species("Abies", "alba")
        .combAuthors(null, "Mill.")
        .nothingElse();

    assertName("Abies alba Mill.", "Mill.", "Abies alba")
        .species("Abies", "alba")
        .combAuthors(null, "Mill.")
        .nothingElse();

    assertName("Abies alba  Mill", "Mill.", "Abies alba")
        .species("Abies", "alba")
        .combAuthors(null, "Mill.")
        .nothingElse();

    assertName("Puma concolor (Linnaeus, 1771)", "(Linnaeus, 1771)", "Puma concolor")
        .species("Puma", "concolor")
        .basAuthors("1771", "Linnaeus")
        .code(ZOOLOGICAL)
        .nothingElse();

    assertName("Puma concolor", "(Linnaeus, 1771)", "Puma concolor")
        .species("Puma", "concolor")
        .basAuthors("1771", "Linnaeus")
        .code(ZOOLOGICAL)
        .nothingElse();

    assertName("Puma concolor ( Linnaeus, 1771 )", "(Linnaeus 1771)", "Puma concolor")
        .species("Puma", "concolor")
        .basAuthors("1771", "Linnaeus")
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
            .type(INFORMAL)
            .nothingElse();

    assertName("EusiridaeNZD", ZOOLOGICAL,"Eusiridae NZD")
            .monomial("Eusiridae", FAMILY)
            .phrase("NZD")
            .type(INFORMAL)
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Blattellinae_SB","Blattellinae SB")
            .monomial("Blattellinae")
            .phrase("SB")
            .type(INFORMAL)
            .nothingElse();

    assertName("GenusANIC_3","Genus ANIC_3")
            .monomial("Genus")
            .phrase("ANIC_3")
            .type(INFORMAL)
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
  public void rNANames() throws Exception {
    assertName("Calathus (Lindrothius) KURNAKOV 1961", "Calathus (Lindrothius)")
            .infraGeneric("Calathus", Rank.INFRAGENERIC_NAME, "Lindrothius")
            .combAuthors("1961", "Kurnakov")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertTrue(isViralName("Ustilaginoidea virens RNA virus"));
    assertTrue(isViralName("Rhizoctonia solani dsRNA virus 2"));

    assertName("Candida albicans RNA_CTR0-3", "Candida albicans RNA_CTR0-3")
            .species("Candida", "albicans")
            .phrase("RNA_CTR0-3")
            .type(INFORMAL)
            .nothingElse();

    assertName("Alpha proteobacterium RNA12", "Alpha proteobacterium RNA12")
            .species("Alpha", "proteobacterium")
            .phrase("RNA12")
            .type(INFORMAL)
            .nothingElse();

    assertName("Armillaria ostoyae RNA1", "Armillaria ostoyae RNA1")
            .species("Armillaria", "ostoyae")
            .phrase("RNA1")
            .type(INFORMAL)
            .nothingElse();
  }

  @Test
  public void indetNames() throws Exception {
    assertName("Trametes spec.", "Trametes sp.")
            .species("Trametes", null)
            .type(NameType.INFORMAL)
            .warning(Warnings.INDETERMINED)
            .nothingElse();

    assertName("Trametes indet.", "Trametes sp.")
            .species("Trametes", null)
            .type(NameType.INFORMAL)
            .warning(Warnings.INDETERMINED)
            .nothingElse();

    assertName("Camillina indet", "Camillina sp.")
            .species("Camillina", null)
            .type(NameType.INFORMAL)
            .warning(Warnings.INDETERMINED)
            .nothingElse();

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
    // interpret as indetermined names if rank is below genus
    assertName("Polygonum", Rank.CULTIVAR, "Polygonum cv.")
            .cultivar("Polygonum", null)
            .type(INFORMAL)
            .warning(Warnings.INDETERMINED)
            .nothingElse();

    assertName("Polygonum", Rank.SUBSPECIES, "Polygonum subsp.")
            .indet("Polygonum", null, Rank.SUBSPECIES)
            .warning(Warnings.INDETERMINED)
            .nothingElse();

    // conflict
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
  public void zooSubspecies() throws Exception {
    // zoological trinomials are by default subspecies, not INFRASPECIFIC_NAME !!!
    assertName("Vulpes vulpes silaceus Miller, 1907", ZOOLOGICAL, "Vulpes vulpes silaceus")
        .infraSpecies("Vulpes", "vulpes", Rank.SUBSPECIES, "silaceus")
        .combAuthors("1907", "Miller")
        .code(ZOOLOGICAL)
        .nothingElse();

    // inferred code
    assertName("Vulpes vulpes silaceus Miller, 1907", "Vulpes vulpes silaceus")
        .infraSpecies("Vulpes", "vulpes", Rank.SUBSPECIES, "silaceus")
        .combAuthors("1907", "Miller")
        .code(ZOOLOGICAL)
        .nothingElse();

    // sp likely misspelled ssp for subspecies
    assertName("Vulpes vulpes sp. silaceus Miller, 1907", "Vulpes vulpes silaceus")
            .infraSpecies("Vulpes", "vulpes", Rank.SUBSPECIES, "silaceus")
            .combAuthors("1907", "Miller")
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
  public void etal() throws Exception {
    assertAuthorship("Hernández-García et. al., 2023")
            .combAuthors("2023", "Hernández-García", "al.")
            .nothingElse();

    assertAuthorship("Fischer-Le Saux et al., 1999 emend. Akhurst et al., 2004")
            .combAuthors("1999", "Fischer-Le Saux", "al.")
            .sensu("emend. Akhurst et al., 2004")
            .nothingElse();
  }


  @Test
  public void authorshipOnlyNotes() throws Exception {
    // "(auct.) Author": the parens mark a note, not a basionym → author + taxonomic note
    assertAuthorship("(auct.) Rolfe")
            .combAuthors(null, "Rolfe")
            .sensu("auct.")
            .nothingElse();

    assertAuthorship("(auct.) auct.")
            .sensu("auct.")
            .nothingElse();

    // taxonomic note + nomenclatural note are split into their own fields
    assertAuthorship("auct., nom. subnud.")
            .sensu("auct.")
            .nomNote("nom. subnud.")
            .nothingElse();

    // a parenthesised "(sensu …)" is the taxonomic note; the trailing name is the author
    assertAuthorship("(sensu Mereschkowsky, 1878) Jankowski, 1992")
            .combAuthors("1992", "Jankowski")
            .sensu("sensu Mereschkowsky, 1878")
            .nothingElse();

    // a leading parenthesised homonym citation makes the whole string a taxonomic note
    assertAuthorship("(non Scacchi, 1836) sensu Zibrowius, 1968")
            .sensu("(non Scacchi, 1836) sensu Zibrowius, 1968")
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

    assertExAuthorship("(Ristorcelli & Van ty) Wedd. ex Sch. Bip. (nom. nud.)", "Wedd.")
            .basAuthors(null, "Ristorcelli", "Van ty")
            .combAuthors(null, "Sch.Bip.")
            .combExAuthors("Wedd.")
            .nomNote("nom. nud.")
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

    assertExAuthorship("Hort. ex Vilmorin", "hort.")
            .combAuthors(null, "Vilmorin")
            .combExAuthors("hort.")
            .nothingElse();

    assertExAuthorship("hortusa ex K. Koch", "hort.")
            .combAuthors(null, "K.Koch")
            .combExAuthors("hort.")
            .nothingElse();

    assertExAuthorship("hortus ex K. Koch", "hort.")
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


  @Test
  public void testPhraseNames() throws Exception {
    assertPhraseName("Pultenaea sp. 'Olinda' (Coveny 6616)", "Pultenaea sp. 'Olinda' (Coveny 6616)", SPECIES, "'Olinda' (Coveny 6616)");
    assertPhraseName("Marsilea sp. Neutral Junction (D.E.Albrecht 9192)", "Marsilea sp. Neutral Junction (D.E.Albrecht 9192)", SPECIES, "Neutral Junction (D.E.Albrecht 9192)");
    assertPhraseName("Dampiera sp. Central Wheatbelt (L.W.Sage, F.Hort, C.A.Hollister LWS2321)", "Dampiera sp. Central Wheatbelt (L.W.Sage, F.Hort, C.A.Hollister LWS2321)", SPECIES, "Central Wheatbelt (L.W.Sage, F.Hort, C.A.Hollister LWS2321)");
    assertPhraseName("Baeckea ssp. 2 (LJM 2019)", "Baeckea subsp. 2 (LJM 2019)", SUBSPECIES, "2 (LJM 2019)");
    assertPhraseName("Baeckea var 2 (LJM 2019)", "Baeckea var. 2 (LJM 2019)", VARIETY, "2 (LJM 2019)");
    assertPhraseName("Baeckea sp. Bunney Road (S.Patrick 4059)", "Baeckea sp. Bunney Road (S.Patrick 4059)", SPECIES, "Bunney Road (S.Patrick 4059)");
    assertPhraseName("Prostanthera sp. Bundjalung Nat. Pk. (B.J.Conn 3471)", "Prostanthera sp. Bundjalung Nat. Pk. (B.J.Conn 3471)", SPECIES, "Bundjalung Nat. Pk. (B.J.Conn 3471)");
    assertPhraseName("Toechima sp. East Alligator (J.Russell-Smith 8418) NT Herbarium", "Toechima sp. East Alligator (J.Russell-Smith 8418) NT Herbarium", SPECIES, "East Alligator (J.Russell-Smith 8418) NT Herbarium");
    assertPhraseName("Goodenia sp. Bachsten Creek (M.D. Barrett 685) WA Herbarium", "Goodenia sp. Bachsten Creek (M.D. Barrett 685) WA Herbarium", SPECIES, "Bachsten Creek (M.D. Barrett 685) WA Herbarium");
    assertPhraseName("Baeckea sp. Beringbooding (AR Main 11/9/1957)", "Baeckea sp. Beringbooding (AR Main 11/9/1957)", SPECIES, "Beringbooding (AR Main 11/9/1957)");
    assertPhraseName("Sida sp. Walhallow Station (C.Edgood 28/Oct/94)", "Sida sp. Walhallow Station (C.Edgood 28/Oct/94)", SPECIES, "Walhallow Station (C.Edgood 28/Oct/94)");
    assertPhraseName("Elaeocarpus sp. Rocky Creek (Hunter s.n., 16 Sep 1993)", "Elaeocarpus sp. Rocky Creek (Hunter s.n., 16 Sep 1993)", SPECIES, "Rocky Creek (Hunter s.n., 16 Sep 1993)");
    assertPhraseName("Sida sp. B (C.Dunlop 1739)", "Sida sp. B (C.Dunlop 1739)", SPECIES, "B (C.Dunlop 1739)");
    assertPhraseName("Grevillea brachystylis subsp. Busselton (G.J.Keighery s.n. 28/8/1985)", "Grevillea brachystylis ssp. Busselton (G.J.Keighery s.n. 28/8/1985)", SUBSPECIES, "Busselton (G.J.Keighery s.n. 28/8/1985)");
    assertPhraseName("Baeckea sp. Calingiri (F.Hort 1710)", "Baeckea sp. Calingiri (F.Hort 1710)", SPECIES, "Calingiri (F.Hort 1710)");
    assertPhraseName("Baeckea sp. East Yuna (R Spjut & C Edson 7077)", "Baeckea sp. East Yuna (R Spjut & C Edson 7077)", SPECIES, "East Yuna (R Spjut & C Edson 7077)");
    assertPhraseName("Acacia sp. Goodlands (BR Maslin 7761) [aff. resinosa]", "Acacia sp. Goodlands (BR Maslin 7761) [aff. resinosa]", SPECIES, "Goodlands (BR Maslin 7761) [aff. resinosa]");
    assertPhraseName("Acacia sp. Manmanning (BR Maslin 7711) [aff. multispicata]", "Acacia sp. Manmanning (BR Maslin 7711) [aff. multispicata]", SPECIES, "Manmanning (BR Maslin 7711) [aff. multispicata]");
    var na = assertPhraseName("Atrichornis (Rahcinta) sp Glory (BR Maslin 7711)", "Atrichornis sp. Glory (BR Maslin 7711)", SPECIES, "Glory (BR Maslin 7711)");
    na.infraGeneric("Rahcinta");
    assertPhraseName("Acacia mutabilis subsp. Young River (G.F.Craig 2052)", "Acacia mutabilis ssp. Young River (G.F.Craig 2052)", SUBSPECIES, "Young River (G.F.Craig 2052)");
    assertPhraseName("Acacia mutabilis Maslin subsp. Young River (G.F.Craig 2052)", "Acacia mutabilis ssp. Young River (G.F.Craig 2052)", SUBSPECIES, "Young River (G.F.Craig 2052)")
        .combAuthors(null, "Maslin");
    assertPhraseName("Acacia sp. \"Morning Glory\"", "Acacia sp. \"Morning Glory\"", SPECIES, "\"Morning Glory\"");
  }

  private NameAssertion assertPhraseName(String sciname, String canonicalName, Rank rank, String phrase) throws UnparsableNameException {
    ParsedName n = parser.parse(sciname, null, null, null);
    var na =  new NameAssertion(n);
    na.phrase(phrase);
    if (rank != null) {
      na.rank(rank);
    }
    assertEquals(canonicalName, n.canonicalName());
    na.type(INFORMAL);
    return na;
  }




  @Test
  public void testNomenclaturalNotesPattern() throws Exception {
    // author only
    var pa = parser.parseAuthorship("nom. illeg.", null);
    var na =  new NameAssertion(pa);
    na.type(null);
    na.nomNote("nom. illeg.");
    na.nothingElse();

    assertNomNote("nom. illeg.",  "Vaucheria longicaulis var. bengalensis Islam, nom. illeg.");
    assertNomNote("nom. correct",  "Dorataspidae nom. correct");
    assertNomNote("nom. transf.",  "Ethmosphaeridae nom. transf.");
    assertNomNote("nom. ambig.",  "Fucus ramosissimus Oeder, nom. ambig.");
    assertNomNote("nom. nov.",  "Myrionema majus Foslie, nom. nov.");
    assertNomNote("nom. utique rej.",  "Corydalis bulbosa (L.) DC., nom. utique rej.");
    assertNomNote("nom. cons. prop.",  "Anthoceros agrestis var. agrestis Paton nom. cons. prop.");
    assertNomNote("nom. superfl.", "Lithothamnion glaciale forma verrucosum (Foslie) Foslie, nom. superfl.");
    assertNomNote("nom. rejic.","Pithecellobium montanum var. subfalcatum (Zoll. & Moritzi)Miq., nom.rejic.");
    assertNomNote("nom. inval.","Fucus vesiculosus forma volubilis (Goodenough & Woodward) H.T. Powell, nom. inval");
    assertNomNote("nom. nud.",  "Sao hispanica R. & E. Richter nom. nud. in Sampelayo 1935");
    assertNomNote("nom. illeg.",  "Hallo (nom.illeg.)");
    assertNomNote("nom. super.",  "Calamagrostis cinnoides W. Bart. nom. super.");
    assertNomNote("nom. nud.",  "Iridaea undulosa var. papillosa Bory de Saint-Vincent, nom. nud.");
    assertNomNote("nom. inval.","Sargassum angustifolium forma filiforme V. Krishnamurthy & H. Joshi, nom. inval");
    assertNomNote("nomen nudum",  "Solanum bifidum Vell. ex Dunal, nomen nudum");
    assertNomNote("nomen invalid","Schoenoplectus ×scheuchzeri (Bruegger) Palla ex Janchen, nomen invalid.");
    assertNomNote("nom. nud.","Cryptomys \"Kasama\" Kawalika et al., 2001, nom. nud. (Kasama, Zambia) .");
    assertNomNote("nom. super.",  "Calamagrostis cinnoides W. Bart. nom. super.");
    assertNomNote("nom. dub.",  "Pandanus odorifer (Forssk.) Kuntze, nom. dub.");
    assertNomNote("nom. rejic.",  "non Clarisia Abat, 1792, nom. rejic.");
    assertNomNote("nom. cons.","Yersinia pestis (Lehmann and Neumann, 1896) van Loghem, 1944 (Approved Lists, 1980) , nom. cons");
    assertNomNote("nom. rejic.","\"Pseudomonas denitrificans\" (Christensen, 1903) Bergey et al., 1923, nom. rejic.");
    assertNomNote("nom. nov.",  "Tipula rubiginosa Loew, 1863, nom. nov.");
    assertNomNote("nom. prov.",  "Amanita pruittii A.H.Sm. ex Tulloss & J.Lindgr., nom. prov.");
    assertNomNote("nom. cons.",  "Ramonda Rich., nom. cons.");
    assertNomNote("nom. cons.","Kluyver and van Niel, 1936 emend. Barker, 1956 (Approved Lists, 1980) , nom. cons., emend. Mah and Kuhn, 1984");
    assertNomNote("nom. superfl.",  "Coccocypselum tontanea (Aubl.) Kunth, nom. superfl.");
    assertNomNote("nom. ambig.",  "Lespedeza bicolor var. intermedia Maxim. , nom. ambig.");
    assertNomNote("nom. praeoccup.",  "Erebia aethiops uralensis Goltz, 1930 nom. praeoccup.");
    assertNomNote("comb. nov. ined.",  "Ipomopsis tridactyla (Rydb.) Wilken, comb. nov. ined.");
    assertNomNote("sp. nov. ined.",  "Orobanche riparia Collins, sp. nov. ined.");
    assertNomNote("gen. nov.",  "Anchimolgidae gen. nov. New Caledonia-Rjh-, 2004");
    assertNomNote("gen. nov. ined.",  "Stebbinsoseris gen. nov. ined.");
    assertNomNote("var. nov.",  "Euphorbia rossiana var. nov. Steinmann, 1199");
  }

  private NameAssertion assertNomNote(String note, String sciname) throws UnparsableNameException {
    ParsedName n = parser.parse(sciname, null, null, null);
    var na =  new NameAssertion(n);
    na.nomNote(note);
    return na;
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
    // The bracketed [1912] is the imprint year; the author span has no nominal
    // publication year, so code can't be inferred from authorship and the trinomial
    // stays INFRASPECIFIC_NAME.
    assertName("Deudorix epijarbas turbo Fruhstorfer, [1912]", "Deudorix epijarbas turbo")
            .infraSpecies("Deudorix", "epijarbas", Rank.INFRASPECIFIC_NAME, "turbo")
            .combAuthors(null, "Fruhstorfer")
            .imprintYear("1912")
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

    assertName("Asarum sieboldii f. non-maculatum (Y.N.Lee) M. Kim", "Asarum sieboldii f. non-maculatum")
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
            .imprintYear("1869")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Brachyspira Hovind-Hougen, Birch-Andersen, Henrik-Nielsen, Orholm, Pedersen, Teglbjaerg & Thaysen, 1983, 1982", "Brachyspira")
            .monomial("Brachyspira")
            .combAuthors("1983", "Hovind-Hougen", "Birch-Andersen", "Henrik-Nielsen", "Orholm", "Pedersen", "Teglbjaerg", "Thaysen")
            .imprintYear("1982")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Gyrosigma angulatum var. gamma Griffith & Henfrey, 1860, 1856", "Gyrosigma angulatum var. gamma")
            .infraSpecies("Gyrosigma", "angulatum", Rank.VARIETY, "gamma")
            .combAuthors("1860", "Griffith", "Henfrey")
            .imprintYear("1856")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Ctenotus alacer Storr, 1970 [\"1969\"]", "Ctenotus alacer")
            .species("Ctenotus", "alacer")
            .combAuthors("1970", "Storr")
            .imprintYear("1969")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Ctenotus alacer Storr, 1970 (imprint 1969)", "Ctenotus alacer")
            .species("Ctenotus", "alacer")
            .combAuthors("1970", "Storr")
            .imprintYear("1969")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Ctenotus alacer Storr, 1887 (\"1886-1888\")", "Ctenotus alacer")
            .species("Ctenotus", "alacer")
            .combAuthors("1887", "Storr")
            .imprintYear("1886-1888")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Melanargia halimede menetriesi Wagener, 1959 & 1961", "Melanargia halimede menetriesi")
            .infraSpecies("Melanargia", "halimede", Rank.SUBSPECIES, "menetriesi")
            .combAuthors("1959", "Wagener")
            .imprintYear("1961")
            .code(ZOOLOGICAL)
            .nothingElse();
  }

  /**
   * The four equivalent imprint-year forms from the ICZN Article 22 example:
   * <a href="https://code.iczn.org/date-of-publication/article-22-citation-of-date/">code.iczn.org/date-of-publication/article-22-citation-of-date</a>.
   * Anomalopus truncatus carries an imprint year inside the basionym brackets.
   */
  @Test
  public void icznImprint() throws Exception {
    assertName("Ctenotus alacer Storr, 1970 (\"1969\")", "Ctenotus alacer")
            .species("Ctenotus", "alacer")
            .combAuthors("1970", "Storr")
            .imprintYear("1969")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Ctenotus alacer Storr, 1970 [\"1969\"]", "Ctenotus alacer")
            .species("Ctenotus", "alacer")
            .combAuthors("1970", "Storr")
            .imprintYear("1969")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Ctenotus alacer Storr, 1970 (imprint 1969)", "Ctenotus alacer")
            .species("Ctenotus", "alacer")
            .combAuthors("1970", "Storr")
            .imprintYear("1969")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Ctenotus alacer Storr, 1970 (not 1969)", "Ctenotus alacer")
            .species("Ctenotus", "alacer")
            .combAuthors("1970", "Storr")
            .imprintYear("1969")
            .code(ZOOLOGICAL)
            .nothingElse();

    assertName("Anomalopus truncatus (Peters, 1876 [\"1877\"])", "Anomalopus truncatus")
            .species("Anomalopus", "truncatus")
            .basAuthors("1876", "Peters")
            .imprintYear("1877")
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
            .nomNote("comb. ined.")
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

    assertName("Genoplesium vernalis D.L.Jones ms.", "Genoplesium vernalis")
            .species("Genoplesium", "vernalis")
            .combAuthors(null, "D.L.Jones")
            .type(SCIENTIFIC)
            .manuscript()
            .nomNote("ms.")
            .nothingElse();

    assertPhraseName("Verticordia sp.1", "Verticordia sp. 1", SPECIES, "1")
            .species("Verticordia", null)
            .nothingElse();

    // Spelled-out "species N" placeholder keeps the verbatim marker word in the phrase and
    // renders it as-is ("Allium species 1"), not collapsed to the synthetic "sp." marker.
    assertPhraseName("Allium species 1", "Allium species 1", SPECIES, "species 1")
            .species("Allium", null)
            .nothingElse();

    assertPhraseName("Bryozoan sp. E", "Bryozoan sp. E", SPECIES, "E")
            .species("Bryozoan", null)
            .nothingElse();
  }

  @Test
  public void phraseNames() throws Exception {
    assertName("Prostanthera sp. Somersbey (B.J.Conn 4024)", "Prostanthera sp. Somersbey (B.J.Conn 4024)")
            .phraseIndetName("Prostanthera", "Somersbey (B.J.Conn 4024)", SPECIES)
            .nothingElse();
    assertName("Pultenaea sp. 'Olinda' (Coveny 6616)", "Pultenaea sp. 'Olinda' (Coveny 6616)")
            .phraseIndetName("Pultenaea", "'Olinda' (Coveny 6616)", SPECIES)
            .nothingElse();
    assertName("Pterostylis sp. Sandheath (D.Murfet 3190) R.J.Bates", "Pterostylis sp. Sandheath (D.Murfet 3190)")
            .phraseIndetName("Pterostylis", "Sandheath (D.Murfet 3190) R.J.Bates", SPECIES)
            .nothingElse();
    // Check to make sure base name is parsed before haring off into the wilderness
    assertName("Acacia mutabilis Maslin", "Acacia mutabilis")
            .species("Acacia", "mutabilis")
            .combAuthors(null, "Maslin")
            .nothingElse();
    assertName("Acacia mutabilis Maslin subsp. Young River (G.F. Craig 2052)", "Acacia mutabilis ssp. Young River (G.F. Craig 2052)")
            .phraseIndetName("Acacia", "Young River (G.F. Craig 2052)", SUBSPECIES)
            .combAuthors(null, "Maslin")
            .nothingElse();
    assertName("Dampiera sp. Central Wheatbelt (L.W.Sage, F.Hort, C.A.Hollister LWS2321)", "Dampiera sp. Central Wheatbelt (L.W.Sage, F.Hort, C.A.Hollister LWS2321)")
            .phraseIndetName("Dampiera", "Central Wheatbelt (L.W.Sage, F.Hort, C.A.Hollister LWS2321)", SPECIES)
            .nothingElse();
    assertName("Dampiera     sp    Central  Wheatbelt (L.W.Sage,   F.Hort,   C.A.Hollister   LWS2321)", "Dampiera sp. Central Wheatbelt (L.W.Sage, F.Hort, C.A.Hollister LWS2321)")
        .phraseIndetName("Dampiera", "Central Wheatbelt (L.W.Sage, F.Hort, C.A.Hollister LWS2321)", SPECIES)
            .nothingElse();
    assertName("Toechima sp. East Alligator (J.Russell-Smith 8418) NT Herbarium", "Toechima sp. East Alligator (J.Russell-Smith 8418) NT Herbarium")
            .phraseIndetName("Toechima", "East Alligator (J.Russell-Smith 8418) NT Herbarium", SPECIES)
            .nothingElse();
    assertName("Acacia sp. Mount Hilditch (M.E. Trudgen 19134)", "Acacia sp. Mount Hilditch (M.E. Trudgen 19134)")
            .phraseIndetName("Acacia", "Mount Hilditch (M.E. Trudgen 19134)", SPECIES)
            .nothingElse();
  }


  @Test
  public void allCapsAuthorsWithPage() throws Exception {
    assertName(" Anolis marmoratus girafus LAZELL 1964: 377", "Anolis marmoratus girafus")
            .infraSpecies("Anolis", "marmoratus", Rank.SUBSPECIES, "girafus")
            .combAuthors("1964", "Lazell")
            .publishedInPage("377")
            .code(ZOOLOGICAL)
            .nothingElse();
  }

  @Test
  public void testCultivarPattern() throws Exception {
    assertName("Abutilon 'Kentish Belle'", "Abutilon 'Kentish Belle'")
        .cultivar("Abutilon", "Kentish Belle");
    assertName("Abutilon 'Nabob'", "Abutilon 'Nabob'")
        .cultivar("Abutilon", "Nabob");
    assertName("Abutilon \"Dall\"", "Abutilon 'Dall'")
        .cultivar("Abutilon", "Dall");
    assertName("Arachis pintoi cv. 'Belmonte'", "Arachis pintoi 'Belmonte'")
        .cultivar("Arachis", "pintoi", "Belmonte");
    assertName("Sorbus hupehensis C.K.Schneid. cv. 'November pink'", "Sorbus hupehensis 'November pink'")
        .cultivar("Sorbus", "hupehensis", "November pink")
        .combAuthors(null, "C.K.Schneid.");
    assertName("Symphoricarpos albus (L.) S.F.Blake cv. 'Turesson'", "Symphoricarpos albus 'Turesson'")
        .cultivar("Symphoricarpos", "albus", "Turesson")
        .basAuthors(null, "L.")
        .combAuthors(null, "S.F.Blake");
    assertName("Symphoricarpos sp. cv. 'mother of pearl'", "Symphoricarpos 'mother of pearl'")
        .cultivar("Symphoricarpos", "mother of pearl");
  }

  private NameAssertion assertCultivar(String note) throws UnparsableNameException {
    ParsedName n = parser.parse("Abies alba "+note, null, null, null);
    var na =  new NameAssertion(n);
    na.nomNote(note);
    return na;
  }


  @Test
  public void testNomStatusRemarks() throws Exception {
    // parser expects a dot or a space as done by the string normalizer
    assertName("Aster megaformis sp.nov.", "Aster megaformis")
        .species("Aster", "megaformis")
        .nomNote("sp. nov.");
    assertName("Aster vulgaris Spec nov", "Aster vulgaris")
        .species("Aster", "vulgaris")
        .nomNote("Spec nov.");
    assertName("Asteraceae Fam.nov.", "Asteraceae")
        .monomial("Asteraceae", FAMILY)
        .nomNote("Fam. nov.");
    assertName("Aster Gen.nov.", "Aster")
        .monomial("Aster", GENUS)
        .nomNote("Gen. nov.");
    // our test only catches the first match, real parsing both!
    assertName("Perugia gruela Gen. nov. sp. nov", "Perugia gruela")
        .species("Perugia", "gruela")
        .nomNote("Gen. nov. sp. nov.");
    assertName("Abies keralia spec. nov.", "Abies keralia")
        .species("Abies", "keralia")
        .nomNote("spec. nov.");
    assertName("Abies sp. nov.", "Abies sp.")
        .species("Abies", null)
        .type(INFORMAL)
        .nomNote("sp. nov.");
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

  /**
   * PR2 names with underscores
   * https://www.dev.checklistbank.org/dataset/303326
   */
  @Test
  public void pr2() throws Exception {
    assertUnparsable("Basal_Cryptophyceae-1", NameType.OTHER);
  }

  @Test
  public void digitEpithetsTrailing() throws Exception {
    assertName("Simplexvirus humanalpha1", "Simplexvirus humanalpha1")
        .species("Simplexvirus", "humanalpha1").code(NomCode.VIRUS).nothingElse();
    assertName("Simplexvirus humanalpha2", "Simplexvirus humanalpha2")
        .species("Simplexvirus", "humanalpha2").code(NomCode.VIRUS).nothingElse();
    assertName("Lentivirus humimdef1", "Lentivirus humimdef1")
        .species("Lentivirus", "humimdef1").code(NomCode.VIRUS).nothingElse();
  }

  // **************
  // HELPER METHODS
  // **************

  public boolean isViralName(String name) throws InterruptedException {
    try {
      var pn = parser.parse(name, null);
      return pn.getCode() == NomCode.VIRUS;
    } catch (UnparsableNameException e) {
      return e.getType() == NameType.OTHER && e.getCode() == NomCode.VIRUS;
    }
  }

  private void assertNoName(String name) throws InterruptedException {
    assertUnparsable(name, NameType.OTHER);
  }

  private void assertUnparsable(String name, NameType type) throws InterruptedException {
    assertUnparsableName(name, Rank.UNRANKED, type, name);
  }

  private void assertUnparsable(String name, Rank rank, NameType type) {
    assertUnparsableName(name, rank, type, name);
  }

  private void assertUnparsable(String name, NameType type, NomCode code) {
    try {
      parser.parse(name, null, Rank.UNRANKED, null);
      fail("Expected " + name + " to be unparsable");
    } catch (UnparsableNameException ex) {
      assertEquals(type, ex.getType());
      assertEquals(code, ex.getCode());
    }
  }

  private void assertUnparsableName(String name, Rank rank, NameType type, String expectedName) {
    try {
      var pn = parser.parse(name, null, rank, null);
      fail("Expected " + name + " to be unparsable");

    } catch (UnparsableNameException ex) {
      assertEquals(type, ex.getType());
      assertEquals(expectedName, ex.getName());
    }
  }

  NameAssertion assertSingleAuthor(String rawAuthorship) throws UnparsableNameException {
    return assertExAuthorship(rawAuthorship, null, rawAuthorship);
  }
  NameAssertion assertAuthorship(String rawAuthorship, String... expectedAuthors) throws UnparsableNameException {
    return assertExAuthorship(rawAuthorship, null, expectedAuthors);
  }
  NameAssertion assertExAuthorship(String rawAuthorship, String exAuthor, String... expectedAuthors) throws UnparsableNameException {
    var pa = parser.parseAuthorship(rawAuthorship, null);
    var na = new NameAssertion(pa);
    na.type(null);
    Authorship auth = pa.getCombinationAuthorship();
    if (exAuthor == null) {
      assertFalse(auth.hasExAuthors());
    } else {
      na.combExAuthors(exAuthor);
      assertEquals(exAuthor, auth.getExAuthors().get(0));
      assertEquals(1, auth.getExAuthors().size());
    }
    if (expectedAuthors.length > 0) {
      assertEquals(Lists.newArrayList(expectedAuthors), auth.getAuthors());
    }
    return na;
  }

  NameAssertion assertName(String rawName, String expectedCanonicalWithoutAuthors) throws UnparsableNameException {
    return assertName(rawName, null, null, null, expectedCanonicalWithoutAuthors);
  }

  NameAssertion assertName(String rawName, String rawAuthorship, String expectedCanonicalWithoutAuthors) throws UnparsableNameException {
    return assertName(rawName, rawAuthorship, null, null, expectedCanonicalWithoutAuthors);
  }

  NameAssertion assertName(String rawName, Rank rank, String expectedCanonicalWithoutAuthors) throws UnparsableNameException {
    return assertName(rawName, rank, null, expectedCanonicalWithoutAuthors);
  }

  NameAssertion assertName(String rawName, String rawAuthorship, Rank rank, String expectedCanonicalWithoutAuthors) throws UnparsableNameException {
    return assertName(rawName, rawAuthorship, rank, null, expectedCanonicalWithoutAuthors);
  }

  NameAssertion assertName(String rawName, NomCode code, String expectedCanonicalWithoutAuthors) throws UnparsableNameException {
    return assertName(rawName, null, code, expectedCanonicalWithoutAuthors);
  }

  NameAssertion assertName(String rawName, Rank rank, NomCode code, String expectedCanonicalWithoutAuthors) throws UnparsableNameException {
    ParsedName n = parser.parse(rawName, null, rank, code);
    assertEquals(expectedCanonicalWithoutAuthors, n.canonicalNameWithoutAuthorship());
    return new NameAssertion(n);
  }

  NameAssertion assertName(String rawName, String rawAuthorship, Rank rank, NomCode code, String expectedCanonicalWithoutAuthors) throws UnparsableNameException {
    ParsedName n = parser.parse(rawName, rawAuthorship, rank, code);
    assertEquals(expectedCanonicalWithoutAuthors, n.canonicalNameWithoutAuthorship());
    return new NameAssertion(n);
  }

  private BufferedReader resourceReader(String resourceFileName) throws UnsupportedEncodingException {
    return new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/" + resourceFileName), "UTF-8"));
  }

}