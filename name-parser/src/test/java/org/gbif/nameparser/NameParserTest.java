package org.gbif.nameparser;

import org.apache.commons.io.LineIterator;
import org.gbif.nameparser.api.*;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;

import static org.gbif.nameparser.api.NamePart.INFRASPECIFIC;
import static org.gbif.nameparser.api.NameType.*;
import static org.gbif.nameparser.api.Rank.*;
import static org.junit.Assert.*;


/**
 *
 */
public class NameParserTest {
  private static Logger LOG = LoggerFactory.getLogger(NameParserTest.class);
  static final NameParser parser = new NameParserGBIF();

  @Test
  public void parseSpecies() throws Exception {

    assertName("Zophosis persis (Chatanay 1914)", "Zophosis persis")
        .species("Zophosis", "persis")
        .basAuthors("1914", "Chatanay")
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
        .nothingElse();
  }

  @Test
  public void parseCapitalAuthors() throws Exception {
    assertName("Anniella nigra FISCHER 1885", "Anniella nigra")
        .species("Anniella", "nigra")
        .combAuthors("1885", "Fischer")
        .nothingElse();
  }

  @Test
  public void parseInfraSpecies() throws Exception {

    assertName("Abies alba ssp. alpina Mill.", "Abies alba subsp. alpina")
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
        .basAuthors("1939", "Ark");

    assertName("Agaricus compactus sarcocephalus (Fr.) Fr. ", "Agaricus compactus sarcocephalus")
        .infraSpecies("Agaricus", "compactus", INFRASPECIFIC_NAME, "sarcocephalus")
        .combAuthors(null, "Fr.")
        .basAuthors(null, "Fr.")
        .nothingElse();

    assertName("Baccharis microphylla Kunth var. rhomboidea Wedd. ex Sch. Bip. (nom. nud.)", "Baccharis microphylla var. rhomboidea")
        .infraSpecies("Baccharis", "microphylla", VARIETY, "rhomboidea")
        .combAuthors(null, "Sch.Bip.")
        .combExAuthors("Wedd.")
        .nomNote("nom. nud.")
        .nothingElse();

  }

  @Test
  public void testExAuthors() throws Exception {
    // In botany (99% of ex author use) the ex author comes first, see https://en.wikipedia.org/wiki/Author_citation_(botany)#Usage_of_the_term_.22ex.22
    assertName("Baccharis microphylla Kunth var. rhomboidea Wedd. ex Sch. Bip. (nom. nud.)", "Baccharis microphylla var. rhomboidea")
        .infraSpecies("Baccharis", "microphylla", VARIETY, "rhomboidea")
        .combAuthors(null, "Sch.Bip.")
        .combExAuthors("Wedd.")
        .nomNote("nom. nud.")
        .nothingElse();

    assertName("Abutilon bastardioides Baker f. ex Rose", "Abutilon bastardioides")
        .species("Abutilon", "bastardioides")
        .combAuthors(null, "Rose")
        .combExAuthors("Baker f.")
        .nothingElse();

    assertName("Abutilon bastardioides Baker f. ex Rose", "Abutilon bastardioides")
        .species("Abutilon", "bastardioides")
        .combAuthors(null, "Rose")
        .combExAuthors("Baker f.")
        .nothingElse();

    // "Abies brevifolia cv. ex Dallim."
    // "Abies brevifolia hort. ex Dallim."
    // "Acacia truncata (Burm. f.) hort. ex Hoffmanns."
    // "Anoectocalyx ex Cogn. 'Triana'"
    // "Anoectocalyx \"Triana\" ex Cogn.  "
    // "Aukuba ex Koehne 'Thunb'   "
    // "Abutilon ×hybridum cv. ex Voss"
  }

  @Test
  public void test4PartedNames() throws Exception {
    assertName("Bombus sichelii alticola latofasciatus", "Bombus sichelii latofasciatus")
        .infraSpecies("Bombus", "sichelii", INFRASUBSPECIFIC_NAME, "latofasciatus")
        .nothingElse();

    assertName("Poa pratensis kewensis primula (L.) Rouy, 1913", "Poa pratensis primula")
        .infraSpecies("Poa", "pratensis", INFRASUBSPECIFIC_NAME, "primula")
        .combAuthors("1913", "Rouy")
        .basAuthors(null, "L.")
        .nothingElse();

    assertName("Acipenser gueldenstaedti colchicus natio danubicus Movchan, 1967", "Acipenser gueldenstaedti natio danubicus")
        .infraSpecies("Acipenser", "gueldenstaedti", NATIO, "danubicus")
        .combAuthors("1967", "Movchan")
        .nothingElse();
  }

  @Test
  public void parseMonomial() throws Exception {

    assertName("Acripeza Guérin-Ménéville 1838", "Acripeza")
        .monomial("Acripeza")
        .combAuthors("1838", "Guérin-Ménéville")
        .nothingElse();

  }

  @Test
  public void parseInfraGeneric() throws Exception {
    assertName("Echinocereus sect. Triglochidiata Bravo", "Echinocereus sect. Triglochidiata")
        .infraGeneric("Echinocereus", SECTION, "Triglochidiata")
        .combAuthors(null, "Bravo")
        .nothingElse();

    assertName("Zignoella subgen. Trematostoma Sacc.", "Zignoella subgen. Trematostoma")
        .infraGeneric("Zignoella", SUBGENUS, "Trematostoma")
        .combAuthors(null, "Sacc.")
        .nothingElse();

    assertName("subgen. Trematostoma Sacc.", "Trematostoma")
        .monomial("Trematostoma", SUBGENUS)
        .combAuthors(null, "Sacc.")
        .nothingElse();

    assertName("Polygonum subgen. Bistorta (L.) Zernov", "Polygonum subgen. Bistorta")
        .infraGeneric("Polygonum", SUBGENUS, "Bistorta")
        .combAuthors(null, "Zernov")
        .basAuthors(null, "L.")
        .nothingElse();

    assertName("Arrhoges (Antarctohoges)", "Arrhoges")
        .monomial("Arrhoges")
        .basAuthors(null, "Antarctohoges")
        .nothingElse();

    assertName("Arrhoges (Antarctohoges)", SUBGENUS,"Arrhoges subgen. Antarctohoges")
        .infraGeneric("Arrhoges", SUBGENUS, "Antarctohoges")
        .nothingElse();

    assertName("Festuca subg. Schedonorus (P. Beauv. ) Peterm.","Festuca subgen. Schedonorus")
        .infraGeneric("Festuca", SUBGENUS, "Schedonorus")
        .combAuthors(null, "Peterm.")
        .basAuthors(null, "P.Beauv.")
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
        .nothingElse();
  }

  @Test
  public void testNotNames() throws Exception {
    assertName("Diatrypella favacea var. favacea (Fr.) Ces. & De Not.", "Diatrypella favacea var. favacea")
        .infraSpecies("Diatrypella", "favacea", VARIETY, "favacea")
        .combAuthors(null, "Ces.", "De Not.")
        .basAuthors(null, "Fr.")
        .nothingElse();

    assertName("Protoventuria rosae (De Not.) Berl. & Sacc.", "Protoventuria rosae")
        .species("Protoventuria", "rosae")
        .combAuthors(null, "Berl.", "Sacc.")
        .basAuthors(null, "De Not.")
        .nothingElse();

    assertName("Hormospora De Not.", "Hormospora")
        .monomial("Hormospora")
        .combAuthors(null, "De Not.")
        .nothingElse();
  }

  @Test
  public void unparsablePlaceholder() throws Exception {
    assertUnparsable("[unassigned] Cladobranchia", PLACEHOLDER);

    assertUnparsable("Biota incertae sedis", PLACEHOLDER);

    assertUnparsable("Mollusca not assigned", PLACEHOLDER);

    assertUnparsable("Unaccepted", PLACEHOLDER);
  }

  @Test
  public void parsePlaceholder() throws Exception {
    assertName("\"? gryphoidis", "? gryphoidis")
        .species("?", "gryphoidis")
        .type(PLACEHOLDER)
        .nothingElse();

    assertName("\"? gryphoidis (Bourguignat 1870) Schoepf. 1909", "? gryphoidis")
        .species("?", "gryphoidis")
        .basAuthors("1870", "Bourguignat")
        .combAuthors("1909", "Schoepf.")
        .type(PLACEHOLDER)
        .nothingElse();

    assertName("Missing penchinati Bourguignat, 1870", "? penchinati")
        .species("?", "penchinati")
        .combAuthors("1870", "Bourguignat")
        .type(PLACEHOLDER)
        .nothingElse();
  }

  @Test
  public void parseSanctioned() throws Exception {
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
        .nothingElse();

    assertName("Agaricus compactus sarcocephalus (Fr. : Fr.) Fr. ", "Agaricus compactus sarcocephalus")
        .infraSpecies("Agaricus", "compactus", INFRASPECIFIC_NAME, "sarcocephalus")
        .combAuthors(null, "Fr.")
        .basAuthors(null, "Fr.")
        .nothingElse();
  }

  @Test
  public void parseNothotaxa() throws Exception {
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
  public void testCandidatus() throws Exception {
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
  @Ignore
  public void testStrains() throws Exception {
    assertName("Endobugula sp. JYr4", "Endobugula sp. JYr4")
        .species("Endobugula", null)
        .strain("sp. JYr4")
        .nothingElse();

    // avoid author & year to be accepted as strain
    assertName("Anniella nigra FISCHER 1885", "Anniella nigra")
        .species("Anniella", "nigra")
        .combAuthors("1885", "Fischer")
        .nothingElse();
  }

  /**
   * Not sure what to do with these names.
   * They look broken.
   *
   * @see <a href="http://dev.gbif.org/issues/browse/GBIFCOM-11">GBIFCOM-11</a>
   */
  @Test
  @Ignore
  public void testExNames() throws Exception {
    // "Abies brevifolia cv. ex Dallim."
    // "Abies brevifolia hort. ex Dallim."
    // "Abutilon bastardioides Baker f. ex Rose"
    // "Acacia truncata (Burm. f.) hort. ex Hoffmanns."
    // "Anoectocalyx ex Cogn. 'Triana'"
    // "Anoectocalyx \"Triana\" ex Cogn.  "
    // "Aukuba ex Koehne 'Thunb'   "
    // "Crepinella subgen. Marchal ex Oliver  "
    // "Echinocereus sect. Triglochidiata ex Bravo"
    // "Hadrolaelia sect. Sophronitis ex Chiron & V.P.Castro"
    // "Abutilon ×hybridum cv. ex Voss"
  }
  
  @Test
  public void parseCultivars() throws Exception {
    // fix cultivar names
    assertName("Acer campestre L. cv. 'nanum'", "Acer campestre 'nanum'")
        .infraSpecies("Acer", "campestre", CULTIVAR, null)
        .cultivar("nanum")
        .combAuthors(null, "L.")
        .nothingElse();
  }

  @Test
  public void testCultivarPattern() throws Exception {
    testCultivar("'Kentish Belle'");
    testCultivar("'Nabob'");
    testCultivar("\"Dall\"");
    testCultivar(" cv. 'Belmonte'");
    testCultivar("Sorbus hupehensis C.K.Schneid. cv. 'November pink'");
    testCultivar("Symphoricarpos albus (L.) S.F.Blake cv. 'Turesson'");
    testCultivar("Symphoricarpos sp. cv. 'mother of pearl'");
  }

  private void testCultivar(String cultivar) {
    Matcher m = NameParserGBIF.CULTIVAR.matcher(cultivar);
    assertTrue (m.find());
  }

  @Test
  public void testEscaping() throws Exception {
    assertEquals("Caloplaca poliotera (Nyl.) J. Steiner",
        NameParserGBIF.normalize("Caloplaca poliotera (Nyl.) J. Steiner\r\n\r\n"));
    assertEquals("Caloplaca poliotera (Nyl.) J. Steiner",
        NameParserGBIF.normalize("Caloplaca poliotera (Nyl.) J. Steiner\\r\\n\\r\\r"));

    assertName("Caloplaca poliotera (Nyl.) J. Steiner\r\n\r\n", "Caloplaca poliotera")
        .species("Caloplaca", "poliotera")
        .basAuthors(null, "Nyl.")
        .combAuthors(null, "J.Steiner")
        .nothingElse();
  }

  @Test
  public void testHybridFormulas() throws Exception {
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

    assertName("Polypodium  x vulgare nothosubsp. mantoniae (Rothm.) Schidlay", "Polypodium vulgare nothosubsp. mantoniae")
        .infraSpecies("Polypodium", "vulgare", SUBSPECIES, "mantoniae")
        .basAuthors(null, "Rothm.")
        .combAuthors(null, "Schidlay")
        .notho(INFRASPECIFIC)
        .nothingElse();
  }

  private void assertHybridFormula(String name) {
    assertUnparsable(name, HYBRID_FORMULA);
  }

  @Test
  public void testOTU() throws Exception {
    assertName("BOLD:ACW2100", "BOLD:ACW2100")
        .monomial("BOLD:ACW2100", Rank.SPECIES)
        .type(OTU)
        .nothingElse();

    assertName("Festuca sp. BOLD:ACW2100", "BOLD:ACW2100")
        .monomial("BOLD:ACW2100", Rank.SPECIES)
        .type(OTU)
        .nothingElse();

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
  public void testHybridAlikeNames() throws Exception {
    assertName("Huaiyuanella Xing, Yan & Yin, 1984", "Huaiyuanella")
        .monomial("Huaiyuanella")
        .combAuthors("1984", "Xing", "Yan", "Yin")
        .nothingElse();

    assertName("Caveasphaera Xiao & Knoll, 2000", "Caveasphaera")
        .monomial("Caveasphaera")
        .combAuthors("2000", "Xiao", "Knoll")
        .nothingElse();
  }

  @Test
  @Ignore("Need to evaluate and implement these alpha/beta/gamme/theta names. Comes from cladistics?")
  public void testAlphaBetaThetaNames() {
    // 11383509 | VARIETY | Trianosperma ficifolia var. βrigida Cogn.
    // 11599666 |         | U. caerulea var. β
    // 12142976 | CLASS   | γ Proteobacteria
    // 12142978 | CLASS   | γ-proteobacteria
    // 16220269 |         | U. caerulea var. β
    // 1297218  | SPECIES | Bacteriophage Qβ
    // 307122   | SPECIES | Agaricus collinitus β mucosus (Bull.) Fr.
    // 313460   | SPECIES | Agaricus muscarius β regalis Fr. (1821)
    // 315162   | SPECIES | Agaricus personatus β saevus
    // 1774875  | VARIETY | Caesarea albiflora var. βramosa Cambess.
    // 3164679  | VARIETY | Cyclotus amethystinus var. α Guppy, 1868 (in part)
    // 3164681  | VARIETY | Cyclotus amethystinus var. β Guppy, 1868
    // 6531344  | SPECIES | Lycoperdon pyriforme β tessellatum Pers. (1801)
    // 7487686  | VARIETY | Nephilengys malabarensis var. β
    // 9665391  | VARIETY | Ranunculus purshii Hook. var. repens(-δ) Hook.
  }

  @Test
  public void testHybridNames() throws Exception {
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
  public void testApostropheAuthors() throws Exception {
    assertName("Cirsium creticum d'Urv.", "Cirsium creticum")
        .species("Cirsium", "creticum")
        .combAuthors(null, "d'Urv.")
        .nothingElse();

    // TODO: autonym authors are the species authors !!!
    assertName("Cirsium creticum d'Urv. subsp. creticum", "Cirsium creticum subsp. creticum")
        .infraSpecies("Cirsium", "creticum", SUBSPECIES, "creticum")
        //.combAuthors(null, "d'Urv.")
        .autonym()
        .nothingElse();
  }

  @Test
  public void testExtinctNames() throws Exception {
    assertName("†Titanoptera", "Titanoptera")
        .monomial("Titanoptera")
        .nothingElse();

    assertName("† Tuarangiida MacKinnon, 1982", "Tuarangiida")
        .monomial("Tuarangiida")
        .combAuthors("1982", "MacKinnon")
        .nothingElse();
  }

  @Test
  public void testTimeoutNameFile() throws Exception {
    Reader reader = resourceReader("timeout-names.txt");
    LineIterator iter = new LineIterator(reader);
    while (iter.hasNext()) {
      String line = iter.nextLine();
      if (line == null || line.startsWith("#") || line.trim().isEmpty()) {
        continue;
      }
      ParsedName n = parser.parse(line, null);
    }
  }

  /**
   * Expect empty unparsable results for nothing or whitespace
   */
  @Test
  public void testEmpty() throws Exception {
    assertEmptyName(null);
    assertEmptyName("");
    assertEmptyName(" ");
    assertEmptyName("\t");
    assertEmptyName("\n");
    assertEmptyName("\t\n");
    // TODO: should this be no name instead ???
    assertEmptyName("\"");
    assertEmptyName("'");
  }

  /**
   * Avoid NPEs and other exceptions for very short non names and other extremes found in occurrences.
   */
  @Test
  public void testAvoidNPE() throws Exception {
    assertNoName("\\");
    assertNoName(".");
    assertNoName("@");
    assertNoName("&nbsp;");
    assertNoName("X");
    assertNoName("a");
    assertNoName("von");
    assertNoName("143");
    assertNoName("321-432");
    assertNoName("-,.#");
    assertNoName(" .");
  }

  @Test
  public void testStringIndexOutOfBoundsException() throws Exception {
    parser.parse("Amblyomma americanum (Linnaeus, 1758)", null);
    parser.parse("Salix taiwanalpina var. chingshuishanensis (S.S.Ying) F.Y.Lu, C.H.Ou, Y.C.Chen, Y.S.Chi, K.C.Lu & Y.H.Tseng ", null);
    parser.parse("Salix taiwanalpina var. chingshuishanensis (S.S.Ying) F.Y.Lu, C.H.Ou, Y.C.Chen, Y.S.Chi, K.C.Lu & amp  Y.H.Tseng ", null);
    parser.parse("Salix morrisonicola var. takasagoalpina (Koidz.) F.Y.Lu, C.H.Ou, Y.C.Chen, Y.S.Chi, K.C.Lu & amp; Y.H.Tseng", null);
    parser.parse("Ficus ernanii Carauta, Pederneir., P.P.Souza, A.F.P.Machado, M.D.M.Vianna & amp; Romaniuc", null);
  }

  @Test
  public void testSensuParsing() throws Exception {
    assertEquals("sensu Baker f.", parser.parse("Trifolium repens sensu Baker f.", null).getSensu());
    assertEquals("sensu latu", parser.parse("Achillea millefolium sensu latu", null).getSensu());
    assertEquals("sec. Greuter, 2009", parser.parse("Achillea millefolium sec. Greuter 2009", null).getSensu());
  }

  @Test
  @Ignore
  public void testOccNameFile() throws Exception {
    Reader reader = resourceReader("parseNull.txt");
    LineIterator iter = new LineIterator(reader);

    int parseFails = 0;
    int parseAuthorless = 0;
    int lineNum = 0;
    long start = System.currentTimeMillis();

    while (iter.hasNext()) {
      lineNum++;
      String name = iter.nextLine();
      ParsedName n;
      try {
        n = parser.parse(name, null);
        if (!n.isAuthorsParsed()) {
          parseAuthorless++;
          LOG.warn("NO AUTHORS\t " + name);
        }
      } catch (UnparsableNameException e) {
        parseFails++;
        LOG.warn("FAIL\t " + name);
      }
    }
    long end = System.currentTimeMillis();
    LOG.info("\n\nNames tested: " + lineNum);
    LOG.info("Names parse fail: " + parseFails);
    LOG.info("Names parse no authors: " + parseAuthorless);
    LOG.info("Total time: " + (end - start));
    LOG.info("Average per name: " + (((double) end - start) / lineNum));

    int currFail = 2;
    if ((parseFails) > currFail) {
      fail("We are getting worse, not better. Currently failing: " + (parseFails) + ". Was passing:" + currFail);
    }
    int currNoAuthors = 109;
    if ((parseAuthorless) > currNoAuthors) {
      fail(
          "We are getting worse, not better. Currently without authors: " + (parseAuthorless) + ". Was:" + currNoAuthors);
    }
  }

  @Test
  public void testViralNames() throws Exception {
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

  }

  public boolean isViralName(String name) {
    try {
      ParsedName pn = parser.parse(name, null);
    } catch (UnparsableNameException e) {
      // swallow
      if (VIRUS == e.getType()) {
        return true;
      }
    }
    return false;
  }



  // **************
  // HELPER METHODS
  // **************


  private void assertEmptyName(String name) {
    assertUnparsableName(name, NO_NAME, null);
  }

  private void assertNoName(String name) {
    assertUnparsable(name, NO_NAME);
  }

  private void assertUnparsable(String name, NameType type) {
    assertUnparsableName(name, type, name);
  }

  private void assertUnparsableName(String name, NameType type, String expectedName) {
    try {
      parser.parse(name);
      fail("Expected "+name+" to be unparsable");

    } catch (UnparsableNameException ex) {
      assertEquals(type, ex.getType());
      assertEquals(expectedName, ex.getName());
    }
  }

  static NameAssertion assertName(String rawName, String expectedCanonicalWithoutAuthors) throws UnparsableNameException {
    return assertName(rawName, null, expectedCanonicalWithoutAuthors);
  }

  static NameAssertion assertName(String rawName, Rank rank, String expectedCanonicalWithoutAuthors) throws UnparsableNameException {
    ParsedName n = parser.parse(rawName, rank);
    assertEquals(expectedCanonicalWithoutAuthors, n.canonicalNameWithoutAuthorship());
    return new NameAssertion(n);
  }

  private BufferedReader resourceReader(String resourceFileName) throws UnsupportedEncodingException {
    return new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/" + resourceFileName), "UTF-8"));
  }

}