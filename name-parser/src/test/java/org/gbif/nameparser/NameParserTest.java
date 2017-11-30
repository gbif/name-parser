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

    assertName("Alstonia vieillardii Van Heurck & Müll. Arg.", "Alstonia vieillardii")
        .species("Alstonia", "vieillardii")
        .combAuthors(null, "Van Heurck", "Müll.Arg.")
        .nothingElse();

    assertName("Angiopteris d'urvilleana de Vriese", "Angiopteris d'urvilleana")
        .species("Angiopteris", "d'urvilleana")
        .combAuthors(null, "de Vriese")
        .nothingElse();
  }

  @Test
  public void parseInfraSpecies() throws Exception {

    assertName("Abies alba ssp. alpina Mill.", "Abies alba subsp. alpina")
        .infraSpecies("Abies", "alba", Rank.SUBSPECIES, "alpina")
        .combAuthors(null, "Mill.")
        .nothingElse();

    assertName("Festuca ovina L. subvar. gracilis Hackel", "Festuca ovina subvar. gracilis")
        .infraSpecies("Festuca", "ovina", Rank.SUBVARIETY, "gracilis")
        .combAuthors(null, "Hackel")
        .nothingElse();

    assertName("Pseudomonas syringae pv. aceris (Ark, 1939) Young, Dye & Wilkie, 1978", "Pseudomonas syringae pv. aceris")
        .infraSpecies("Pseudomonas", "syringae", Rank.PATHOVAR, "aceris")
        .combAuthors("1978", "Young", "Dye", "Wilkie")
        .basAuthors("1939", "Ark");

    assertName("Agaricus compactus sarcocephalus (Fr.) Fr. ", "Agaricus compactus sarcocephalus")
        .infraSpecies("Agaricus", "compactus", Rank.INFRASPECIFIC_NAME, "sarcocephalus")
        .combAuthors(null, "Fr.")
        .basAuthors(null, "Fr.")
        .nothingElse();

    assertName("Baccharis microphylla Kunth var. rhomboidea Wedd. ex Sch. Bip. (nom. nud.)", "Baccharis microphylla var. rhomboidea")
        .infraSpecies("Baccharis", "microphylla", Rank.VARIETY, "rhomboidea")
        .combAuthors(null, "Sch.Bip.")
        .combExAuthors("Wedd.")
        .nomNote("nom. nud.")
        .nothingElse();

  }

  @Test
  public void testExAuthors() throws Exception {
    // In botany (99% of ex author use) the ex author comes first, see https://en.wikipedia.org/wiki/Author_citation_(botany)#Usage_of_the_term_.22ex.22
    assertName("Baccharis microphylla Kunth var. rhomboidea Wedd. ex Sch. Bip. (nom. nud.)", "Baccharis microphylla var. rhomboidea")
        .infraSpecies("Baccharis", "microphylla", Rank.VARIETY, "rhomboidea")
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
        .infraSpecies("Bombus", "sichelii", Rank.INFRASUBSPECIFIC_NAME, "latofasciatus")
        .nothingElse();

    assertName("Poa pratensis kewensis primula (L.) Rouy, 1913", "Poa pratensis primula")
        .infraSpecies("Poa", "pratensis", Rank.INFRASUBSPECIFIC_NAME, "primula")
        .combAuthors("1913", "Rouy")
        .basAuthors(null, "L.")
        .nothingElse();

    assertName("Acipenser gueldenstaedti colchicus natio danubicus Movchan, 1967", "Acipenser gueldenstaedti natio danubicus")
        .infraSpecies("Acipenser", "gueldenstaedti", Rank.NATIO, "danubicus")
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
        .infraGeneric("Echinocereus", Rank.SECTION, "Triglochidiata")
        .combAuthors(null, "Bravo")
        .nothingElse();

    assertName("Zignoella subgen. Trematostoma Sacc.", "Zignoella subgen. Trematostoma")
        .infraGeneric("Zignoella", Rank.SUBGENUS, "Trematostoma")
        .combAuthors(null, "Sacc.")
        .nothingElse();

    assertName("subgen. Trematostoma Sacc.", "Trematostoma")
        .monomial("Trematostoma", Rank.SUBGENUS)
        .combAuthors(null, "Sacc.")
        .nothingElse();

    assertName("Polygonum subgen. Bistorta (L.) Zernov", "Polygonum subgen. Bistorta")
        .infraGeneric("Polygonum", Rank.SUBGENUS, "Bistorta")
        .combAuthors(null, "Zernov")
        .basAuthors(null, "L.")
        .nothingElse();

    assertName("Arrhoges (Antarctohoges)", "Arrhoges")
        .monomial("Arrhoges")
        .basAuthors(null, "Antarctohoges")
        .nothingElse();

    assertName("Arrhoges (Antarctohoges)", Rank.SUBGENUS,"Arrhoges subgen. Antarctohoges")
        .infraGeneric("Arrhoges", Rank.SUBGENUS, "Antarctohoges")
        .nothingElse();

    assertName("Festuca subg. Schedonorus (P. Beauv. ) Peterm.","Festuca subgen. Schedonorus")
        .infraGeneric("Festuca", Rank.SUBGENUS, "Schedonorus")
        .combAuthors(null, "Peterm.")
        .basAuthors(null, "P.Beauv.")
        .nothingElse();

    assertName("Catapodium subg.Agropyropsis  Trab.", "Catapodium subgen. Agropyropsis")
        .infraGeneric("Catapodium", Rank.SUBGENUS, "Agropyropsis")
        .combAuthors(null, "Trab.")
        .nothingElse();

    assertName(" Gnaphalium subg. Laphangium Hilliard & B. L. Burtt", "Gnaphalium subgen. Laphangium")
        .infraGeneric("Gnaphalium", Rank.SUBGENUS, "Laphangium")
        .combAuthors(null, "Hilliard", "B.L.Burtt")
        .nothingElse();

    assertName("Woodsiaceae (Hooker) Herter", "Woodsiaceae")
        .monomial("Woodsiaceae", Rank.FAMILY)
        .combAuthors(null, "Herter")
        .basAuthors(null, "Hooker")
        .nothingElse();
  }

  @Test
  public void testNotNames() throws Exception {
    assertName("Diatrypella favacea var. favacea (Fr.) Ces. & De Not.", "Diatrypella favacea var. favacea")
        .infraSpecies("Diatrypella", "favacea", Rank.VARIETY, "favacea")
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
  public void parsePlaceholder() throws Exception {
    assertName("\"? gryphoidis", "? gryphoidis", NameType.PLACEHOLDER)
        .species("?", "gryphoidis")
        .doubtful()
        .nothingElse();

    assertUnparsable("[unassigned] Cladobranchia", NameType.PLACEHOLDER);

    assertUnparsable("Biota incertae sedis", NameType.PLACEHOLDER);

    assertUnparsable("Mollusca not assigned", NameType.PLACEHOLDER);
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
        .infraSpecies("Agaricus", "compactus", Rank.INFRASPECIFIC_NAME, "sarcocephalus")
        .combAuthors(null, "Fr.")
        .basAuthors(null, "Fr.")
        .nothingElse();

    assertName("Agaricus compactus sarcocephalus (Fr. : Fr.) Fr. ", "Agaricus compactus sarcocephalus")
        .infraSpecies("Agaricus", "compactus", Rank.INFRASPECIFIC_NAME, "sarcocephalus")
        .combAuthors(null, "Fr.")
        .basAuthors(null, "Fr.")
        .nothingElse();
  }

  @Test
  public void parseNothotaxa() throws Exception {
    // https://github.com/GlobalNamesArchitecture/gnparser/issues/410
    assertName("Iris germanica nothovar. florentina", "Iris germanica nothovar. florentina")
        .infraSpecies("Iris", "germanica", Rank.VARIETY, "florentina")
        .notho(NamePart.INFRASPECIFIC)
        .nothingElse();

    assertName("Abies alba var. ×alpina L.", "Abies alba nothovar. alpina")
        .infraSpecies("Abies", "alba", Rank.VARIETY, "alpina")
        .notho(NamePart.INFRASPECIFIC)
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
    assertName("Acer campestre L. cv. 'nanum'", "Acer campestre 'nanum'", NameType.SCIENTIFIC)
        .infraSpecies("Acer", "campestre", Rank.CULTIVAR, null)
        .cultivar("nanum")
        .combAuthors(null, "L.")
        .nothingElse();
  }

  @Test
  public void parseHybridFormulas() throws Exception {
    assertUnparsable("Asplenium rhizophyllum DC. x ruta-muraria E.L. Braun 1939", NameType.HYBRID_FORMULA);
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
      if (NameType.VIRUS == e.getType()) {
        return true;
      }
    }
    return false;
  }



  // **************
  // HELPER METHODS
  // **************


  private void assertEmptyName(String name) {
    assertUnparsableName(name, NameType.NO_NAME, null);
  }

  private void assertNoName(String name) {
    assertUnparsable(name, NameType.NO_NAME);
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
    return assertName(rawName, null, expectedCanonicalWithoutAuthors, NameType.SCIENTIFIC);
  }

  static NameAssertion assertName(String rawName, Rank rank, String expectedCanonicalWithoutAuthors) throws UnparsableNameException {
    return assertName(rawName, rank, expectedCanonicalWithoutAuthors, NameType.SCIENTIFIC);
  }

  static NameAssertion assertName(String rawName, String expectedCanonicalWithoutAuthors, NameType type) throws UnparsableNameException {
    return assertName(rawName, null, expectedCanonicalWithoutAuthors, type);
  }

  static NameAssertion assertName(String rawName, Rank rank, String expectedCanonicalWithoutAuthors, NameType type) throws UnparsableNameException {
    ParsedName n = parser.parse(rawName, rank);
    assertEquals(expectedCanonicalWithoutAuthors, n.canonicalNameWithoutAuthorship());
    assertEquals(type, n.getType());
    return new NameAssertion(n);
  }

  private BufferedReader resourceReader(String resourceFileName) throws UnsupportedEncodingException {
    return new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/" + resourceFileName), "UTF-8"));
  }

}