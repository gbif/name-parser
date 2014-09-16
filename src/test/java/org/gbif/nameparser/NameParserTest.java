package org.gbif.nameparser;

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.NamePart;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.Rank;
import org.gbif.utils.file.FileUtils;
import org.gbif.utils.file.InputStreamUtils;

import java.io.IOException;
import java.io.Reader;
import java.util.Set;
import java.util.regex.Matcher;

import com.google.common.base.Strings;
import junit.framework.TestCase;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.gbif.nameparser.NameParser.normalize;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test various scientific and non scientific names to parse.
 * TODO: Add tests for the following names found unparsed in CLB:
 * (?) Physignathus cochinchinensis Guérin, 1829
 * (Acmaeops)
 * (Acmaeops) rufula Gardiner, 1970
 * (Amsel, 1935)
 * (Antopocerus)
 * (Arocephalus) languidus (Flor, 1861)
 * (Athysanella) salsa (Ball & Beamer, 1940)
 * (Erdianus) nigricans (Kirschbaum, 1868)
 * ? Callidium
 * ? Callidium (Phymatodes)
 * ? Callidium (Phymatodes) semicircularis Bland, 1862
 * ? Compsa
 * ? Compsa flavofasciata Martins & Galileo, 2002
 */
public class NameParserTest {

  private static Logger LOG = LoggerFactory.getLogger(NameParserTest.class);
  private static final int CURRENTLY_FAIL = 25;
  private static final int CURRENTLY_FAIL_EXCL_AUTHORS = 19;
  private static final String NAMES_TEST_FILE = "scientific_names.txt";
  private static final String MONOMIALS_FILE = "monomials.txt";
  private static final int SLOW = 100; // in milliseconds
  private static final InputStreamUtils streamUtils = new InputStreamUtils();
  private static NameParser parser;

  @BeforeClass
  public static void setup() {
    parser = new NameParser();
    Set<String> mon;
    try {
      mon = FileUtils.streamToSet(streamUtils.classpathStream(MONOMIALS_FILE));
      parser.setMonomials(mon);
      LOG.info("Parser setup. Read {} known monomials", mon.size());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void assertHybridFormula(String name) {
    try {
      parser.parse(name);
    } catch (UnparsableException e) {
      assertEquals(NameType.HYBRID, e.type);
    }
  }

  private void assertHybridName(ParsedName pn) {
    assertTrue(NameType.HYBRID != pn.getType());
    assertTrue(pn.canonicalNameWithMarker().length() > 8);
  }

  private void assertUnparsableType(NameType type, String name) {
    try {
      parser.parse(name);
      fail("Name should be unparsable: " + name);
    } catch (UnparsableException e) {
      assertEquals(type, e.type);
    }
  }

  private void assertBlacklisted(String name) {
    assertUnparsableType(NameType.BLACKLISTED, name);
  }

  private void assertUnparsableDoubtful(String name) {
    try {
      parser.parse(name);
      fail("Name should be unparsable: " + name);
    } catch (UnparsableException e) {
      assertEquals(NameType.DOUBTFUL, e.type);
    }
  }

  private void allowUnparsable(String name) {
    try {
      parser.parse(name);
    } catch (UnparsableException e) {
    }
  }

  // 0scientificName
  // 1genusOrAbove
  // 2infraGeneric
  // 3specificEpithet
  // 4infraSpecificEpithet
  // 5 cultivarEpithet
  // 6authorship
  // 7year
  // 8bracketAuthorship
  // 9bracketYear
  // 10rank
  // 11nomcode
  // 12nothotype
  // 13nametype
  private ParsedName buildParsedNameFromTabRow(String[] cols) {
    ParsedName pn = new ParsedName(NameType.fromString(cols[13]), StringUtils.trimToNull(cols[1]),
      StringUtils.trimToNull(cols[2]), StringUtils.trimToNull(cols[3]), StringUtils.trimToNull(cols[4]),
      NamePart.fromString(cols[12]), StringUtils.trimToNull(cols[10]), StringUtils.trimToNull(cols[6]),
      StringUtils.trimToNull(cols[7]), StringUtils.trimToNull(cols[8]), StringUtils.trimToNull(cols[9]),
      StringUtils.trimToNull(cols[5]), null, null, null, null);
    return pn;
  }

  private String extractNomNote(String name) {
    String nomNote = null;
    Matcher matcher = NameParser.EXTRACT_NOMSTATUS.matcher(name);
    if (matcher.find()) {
      nomNote = (StringUtils.trimToNull(matcher.group(1)));
    }
    return nomNote;
  }

  @Test
  @Ignore
  public void test4PartedNames() throws Exception {
    ParsedName n = parser.parse("Bombus sichelii alticola latofasciatus");
    assertEquals("Bombus sichelii infrasubsp. latofasciatus", n.canonicalNameWithMarker());
    assertEquals("Bombus sichelii latofasciatus", n.canonicalName());

    n = parser.parse("Bombus sichelii alticola latofasciatus Vogt, 1909");
    assertEquals("Bombus sichelii infrasubsp. latofasciatus", n.canonicalNameWithMarker());
    assertEquals("Bombus sichelii latofasciatus", n.canonicalName());
  }

  private boolean testAuthorship(String author) {
    Matcher m = NameParser.AUTHOR_TEAM_PATTERN.matcher(author);
    if (m.find()) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Avoid NPEs and other exceptions for very short non names and other extremes found in occurrences.
   */
  @Test
  public void testAvoidNPE() throws Exception {
    allowUnparsable(null);
    allowUnparsable(" ");
    allowUnparsable("");
    allowUnparsable("\"");
    allowUnparsable("\\");
    allowUnparsable(".");
    allowUnparsable("a");
    allowUnparsable("'");
    allowUnparsable("von");
    allowUnparsable("X");
    allowUnparsable("@");
    allowUnparsable("&nbsp;");
    allowUnparsable("\"? gryphoidis");
  }

  @Test
  public void testAuthorshipPattern() throws Exception {
    assertTrue(this.testAuthorship("L."));
    assertTrue(this.testAuthorship("Lin."));
    assertTrue(this.testAuthorship("Linné"));
    assertTrue(this.testAuthorship("DC."));
    assertTrue(this.testAuthorship("de Chaudoir"));
    assertTrue(this.testAuthorship("Hilaire"));
    assertTrue(this.testAuthorship("St. Hilaire"));
    assertTrue(this.testAuthorship("Geoffroy St. Hilaire"));
    assertTrue(this.testAuthorship("Acev.-Rodr."));
    assertTrue(this.testAuthorship("Steyerm., Aristeg. & Wurdack"));
    assertTrue(this.testAuthorship("Du Puy & Labat"));
    assertTrue(this.testAuthorship("Baum.-Bod."));
    assertTrue(this.testAuthorship("Engl. & v. Brehmer"));
    assertTrue(this.testAuthorship("F. v. Muell."));
    assertTrue(this.testAuthorship("W.J.de Wilde & Duyfjes"));
    assertTrue(this.testAuthorship("C.E.M.Bicudo"));
    assertTrue(this.testAuthorship("Alves-da-Silva"));
    assertTrue(this.testAuthorship("Alves-da-Silva & C.E.M.Bicudo"));
    assertTrue(this.testAuthorship("Kingdon-Ward"));
    assertTrue(this.testAuthorship("Merr. & L.M.Perry"));
    assertTrue(this.testAuthorship("Calat., Nav.-Ros. & Hafellner"));
    assertTrue(this.testAuthorship("Barboza du Bocage"));
    assertTrue(this.testAuthorship("Arv.-Touv. ex Dörfl."));
    assertTrue(this.testAuthorship("Payri & P.W.Gabrielson"));
    assertTrue(this.testAuthorship("N'Yeurt, Payri & P.W.Gabrielson"));
    assertTrue(this.testAuthorship("VanLand."));
    assertTrue(this.testAuthorship("MacLeish"));
    assertTrue(this.testAuthorship("Monterosato ms."));
    assertTrue(this.testAuthorship("Arn. ms., Grunow"));
    assertTrue(this.testAuthorship("Mosely in Mosely & Kimmins"));
    assertTrue(this.testAuthorship("Choi,J.H.; Im,W.T.; Yoo,J.S.; Lee,S.M.; Moon,D.S.; Kim,H.J.; Rhee,S.K.; Roh,D.H."));
    assertTrue(this.testAuthorship("da Costa Lima"));
    assertTrue(this.testAuthorship("Krapov., W.C.Greg. & C.E.Simpson"));
    assertTrue(this.testAuthorship("de Jussieu"));
    assertTrue(this.testAuthorship("Griseb. ex. Wedd."));
    assertTrue(this.testAuthorship("van-der Land"));
    assertTrue(this.testAuthorship("van der Land"));
    assertTrue(this.testAuthorship("van Helmsick"));
    assertTrue(this.testAuthorship("Xing, Yan & Yin"));
    assertTrue(this.testAuthorship("Xiao & Knoll"));
  }

  @Test
  public void testNotNames() throws Exception {
    ParsedName pn = parser.parse("Diatrypella favacea var. favacea (Fr.) Ces. & De Not.");
    assertEquals("Diatrypella", pn.getGenusOrAbove());
    assertEquals("favacea", pn.getSpecificEpithet());
    assertEquals("var.", pn.getRankMarker());
    assertEquals("favacea", pn.getInfraSpecificEpithet());
    assertEquals("Ces. & De Not.", pn.getAuthorship());
    assertEquals("Fr.", pn.getBracketAuthorship());

    pn = parser.parse("Protoventuria rosae (De Not.) Berl. & Sacc.");
    assertEquals("Protoventuria", pn.getGenusOrAbove());
    assertEquals("rosae", pn.getSpecificEpithet());
    assertEquals("Berl. & Sacc.", pn.getAuthorship());
    assertEquals("De Not.", pn.getBracketAuthorship());

    pn = parser.parse("Hormospora De Not.");
    assertEquals("Hormospora", pn.getGenusOrAbove());
    assertEquals("De Not.", pn.getAuthorship());
  }

  @Test
  public void testCanonNameParserSubgenera() throws Exception {
    ParsedName pn = parser.parse("Polygonum subgen. Bistorta (L.) Zernov");
    assertTrue(pn.getNotho() == null);
    assertEquals("Polygonum", pn.getGenusOrAbove());
    assertEquals("Bistorta", pn.getInfraGeneric());
    assertEquals("subgen.", pn.getRankMarker());
    assertEquals("Zernov", pn.getAuthorship());
    assertEquals("L.", pn.getBracketAuthorship());

    ParsedName n = parser.parse("Arrhoges (Antarctohoges)");
    assertEquals("Arrhoges", n.getGenusOrAbove());
    assertEquals("Antarctohoges", n.getInfraGeneric());
    assertTrue(n.getRankMarker() == null);
    // assertEquals(NomenclaturalCode.Zoological, n.code);
    assertTrue(n.getNotho() == null);

    pn = parser.parse("Festuca subg. Schedonorus (P. Beauv. ) Peterm.");
    assertEquals("Festuca", pn.getGenusOrAbove());
    assertEquals("Schedonorus", pn.getInfraGeneric());
    assertEquals("subgen.", pn.getRankMarker());
    assertEquals("Peterm.", pn.getAuthorship());
    assertEquals("P. Beauv.", pn.getBracketAuthorship());

    n = parser.parse("Catapodium subg.Agropyropsis  Trab.");
    assertEquals("Catapodium", n.getGenusOrAbove());
    assertEquals("Agropyropsis", n.getInfraGeneric());
    assertEquals("subgen.", n.getRankMarker());

    n = parser.parse(" Gnaphalium subg. Laphangium Hilliard & B. L. Burtt");
    assertEquals("Gnaphalium", n.getGenusOrAbove());
    assertEquals("Laphangium", n.getInfraGeneric());
    assertEquals("subgen.", n.getRankMarker());

    n = parser.parse("Woodsiaceae (Hooker) Herter");
    assertEquals("Woodsiaceae", n.getGenusOrAbove());
    assertTrue(n.getInfraGeneric() == null);
    assertTrue(n.getRankMarker() == null);

  }

  @Test
  public void testClean1() throws Exception {
    assertEquals("", NameParser.preClean(""));
    assertEquals("Hallo Spencer", NameParser.preClean("Hallo Spencer "));
    assertEquals("Hallo Spencer", NameParser.preClean("' 'Hallo Spencer"));
    assertEquals("Hallo Spencer 1982", NameParser.preClean("'\" Hallo  Spencer 1982'"));
  }

  private boolean testCultivar(String cultivar) {
    Matcher m = NameParser.CULTIVAR.matcher(cultivar);
    if (m.find()) {
      return true;
    } else {
      return false;
    }
  }

  private ParsedName parseCandidatus(String name, String expectedGenus, String expectedSpecies, String expectedInfraSpecies)
    throws UnparsableException {
    ParsedName pn = parser.parse(name);
    assertEquals(NameType.CANDIDATUS, pn.getType());

    assertEquals(Strings.nullToEmpty(expectedGenus), Strings.nullToEmpty(pn.getGenusOrAbove()));
    assertEquals(Strings.nullToEmpty(expectedSpecies), Strings.nullToEmpty(pn.getSpecificEpithet()));
    assertEquals(Strings.nullToEmpty(expectedInfraSpecies), Strings.nullToEmpty(pn.getInfraSpecificEpithet()));

    return pn;
  }

  @Test
  public void testCandidatus() throws Exception {
    ParsedName pn = parseCandidatus("\"Candidatus Endowatersipora\" Anderson and Haygood, 2007", "Endowatersipora", null, null);
    assertEquals("Anderson & Haygood", pn.getAuthorship());
    assertEquals("2007", pn.getYear());

    pn = parseCandidatus("Candidatus Phytoplasma allocasuarinae", "Phytoplasma", "allocasuarinae",null);

    pn = parseCandidatus("Ca. Phytoplasma allocasuarinae", "Phytoplasma", "allocasuarinae",null);

    pn = parseCandidatus("Candidatus Phytoplasma", "Phytoplasma", null, null);
    pn = parseCandidatus("Ca. Phytoplasma", "Phytoplasma", null, null);

    pn = parseCandidatus("'Candidatus Nicolleia'", "Nicolleia", null, null);


    pn = parseCandidatus("\"Candidatus Riegeria\" Gruber-Vodicka et al., 2011", "Riegeria", null, null);
    assertEquals("Gruber-Vodicka et al.", pn.getAuthorship());
    assertEquals("2011", pn.getYear());

    //TODO: parse strains
    //pn = parseCandidatus("Candidatus Midichloria sp. ixholo1", "Midichloria", "ixholo1",null);
    //pn = parseCandidatus("Candidatus Endobugula sp. JYr4", "Endobugula", "JYr4",null);

  }

  /**
   * @see <a href="http://dev.gbif.org/issues/browse/GBIFCOM-11">GBIFCOM-11</a>
   */
  @Test
  public void testExNames() throws Exception {
    ParsedName pn = assertExAuthorName(parser.parse("Abies brevifolia cv. ex Dallim."));
    pn = assertExAuthorName(parser.parse("Abies brevifolia hort. ex Dallim."));
    pn = assertExAuthorName(parser.parse("Abutilon bastardioides Baker f. ex Rose"));
    pn = assertExAuthorName(parser.parse("Acacia truncata (Burm. f.) hort. ex Hoffmanns."));
    pn = assertExAuthorName(parser.parse("Anoectocalyx ex Cogn. 'Triana'"));
    pn = assertExAuthorName(parser.parse("Anoectocalyx \"Triana\" ex Cogn.  "));
    pn = assertExAuthorName(parser.parse("Aukuba ex Koehne 'Thunb'   "));
    pn = assertExAuthorName(parser.parse("Crepinella subgen. Marchal ex Oliver  "));
    pn = assertExAuthorName(parser.parse("Echinocereus sect. Triglochidiata ex Bravo"));
    pn = assertExAuthorName(parser.parse("Hadrolaelia sect. Sophronitis ex Chiron & V.P.Castro"));

    pn = assertExAuthorName(parser.parse("Abutilon ×hybridum cv. ex Voss"));
  }

  private ParsedName assertExAuthorName(ParsedName pn) {
    assertFalse("ex".equalsIgnoreCase(pn.getSpecificEpithet()));
    assertFalse("ex".equalsIgnoreCase(pn.getInfraSpecificEpithet()));
    assertFalse("ex".equalsIgnoreCase(pn.getInfraGeneric()));
    assertNotNull(pn.getGenusOrAbove());
    return pn;
  }

  @Test
  public void testCultivarNames() throws Exception {
    ParsedName pn = parser.parse("Abutilon 'Kentish Belle'");
    assertEquals("Abutilon", pn.getGenusOrAbove());
    assertEquals(NameType.CULTIVAR, pn.getType());

    pn = parser.parse("Abutilon 'Nabob'");
    assertEquals("Abutilon", pn.getGenusOrAbove());
    assertEquals(NameType.CULTIVAR, pn.getType());

    pn = parser.parse("Verpericola megasoma \"Dall\" Pils.");
    assertEquals("Verpericola", pn.getGenusOrAbove());
    assertEquals("megasoma", pn.getSpecificEpithet());
    assertEquals(NameType.CULTIVAR, pn.getType());

    pn = parser.parse("Sorbus americana Marshall cv. 'Belmonte'");
    assertEquals("Sorbus", pn.getGenusOrAbove());
    assertEquals("americana", pn.getSpecificEpithet());
    assertEquals(NameType.CULTIVAR, pn.getType());

    pn = parser.parse("Sorbus hupehensis C.K.Schneid. cv. 'November pink'");
    assertEquals("Sorbus", pn.getGenusOrAbove());
    assertEquals("hupehensis", pn.getSpecificEpithet());
    assertEquals(NameType.CULTIVAR, pn.getType());

    pn = parser.parse("Symphoricarpos albus (L.) S.F.Blake cv. 'Turesson'");
    assertEquals("Symphoricarpos", pn.getGenusOrAbove());
    assertEquals("albus", pn.getSpecificEpithet());
    assertEquals(NameType.CULTIVAR, pn.getType());
    assertEquals(Rank.CULTIVAR, pn.getRank());

    pn = parser.parse("Symphoricarpos sp. cv. 'mother of pearl'");
    assertEquals("Symphoricarpos", pn.getGenusOrAbove());
    assertEquals(NameType.CULTIVAR, pn.getType());
    assertEquals(Rank.CULTIVAR, pn.getRank());

  }

  @Test
  public void testCultivarPattern() throws Exception {
    assertTrue(this.testCultivar("'Kentish Belle'"));
    assertTrue(this.testCultivar("'Nabob'"));
    assertTrue(this.testCultivar("\"Dall\""));
    assertTrue(this.testCultivar(" cv. 'Belmonte'"));
    assertTrue(this.testCultivar("Sorbus hupehensis C.K.Schneid. cv. 'November pink'"));
    assertTrue(this.testCultivar("Symphoricarpos albus (L.) S.F.Blake cv. 'Turesson'"));
    assertTrue(this.testCultivar("Symphoricarpos sp. cv. 'mother of pearl'"));

  }

  @Test
  public void testEscaping() throws Exception {
    assertEquals("Caloplaca poliotera (Nyl.) J. Steiner",
      parser.normalize("Caloplaca poliotera (Nyl.) J. Steiner\\r\\n\\r\\n"));

    ParsedName pn = parser.parse("Caloplaca poliotera (Nyl.) J. Steiner\\r\\n\\r\\n.");
    assertTrue(pn.getNotho() == null);
    assertEquals("Caloplaca", pn.getGenusOrAbove());
    assertEquals("poliotera", pn.getSpecificEpithet());
    assertEquals("Nyl.", pn.getBracketAuthorship());
    assertEquals("J. Steiner", pn.getAuthorship());
  }

  @Test
  public void testHybridFormulas() throws Exception {
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

    assertEquals("Polypodium vulgare nothosubsp. mantoniae (Rothm.) Schidlay",
      parser.parse("Polypodium  x vulgare nothosubsp. mantoniae (Rothm.) Schidlay ").fullName());
    assertHybridName(parser.parse("Arthopyrenia hyalospora x "));
  }

  @Test
  public void testHybridAlikeNames() throws Exception {
    ParsedName n = parser.parse("Huaiyuanella Xing, Yan & Yin, 1984");
    assertNull(n.getNotho());
    assertEquals("Huaiyuanella", n.getGenusOrAbove());
    assertNull(n.getSpecificEpithet());
    assertEquals("Xing, Yan & Yin", n.getAuthorship());
    assertEquals("1984", n.getYear());

    n = parser.parse("Caveasphaera Xiao & Knoll, 2000");
    assertNull(n.getNotho());
    assertEquals("Caveasphaera", n.getGenusOrAbove());
    assertNull(n.getSpecificEpithet());
    assertEquals("2000", n.getYear());

  }

  @Test
  public void testHybridNames() throws Exception {

    ParsedName n = parser.parse("+ Pyrocrataegus willei L.L.Daniel");
    assertEquals(NamePart.GENERIC, n.getNotho());
    assertEquals("Pyrocrataegus", n.getGenusOrAbove());
    assertEquals("willei", n.getSpecificEpithet());
    assertEquals("L.L.Daniel", n.getAuthorship());

    n = parser.parse("×Pyrocrataegus willei L.L.Daniel");
    assertEquals(NamePart.GENERIC, n.getNotho());
    assertEquals("Pyrocrataegus", n.getGenusOrAbove());
    assertEquals("willei", n.getSpecificEpithet());
    assertTrue(n.getInfraSpecificEpithet() == null);
    assertEquals("L.L.Daniel", n.getAuthorship());

    n = parser.parse(" × Pyrocrataegus willei L.L.Daniel");
    assertEquals(NamePart.GENERIC, n.getNotho());
    assertEquals("Pyrocrataegus", n.getGenusOrAbove());
    assertEquals("willei", n.getSpecificEpithet());
    assertTrue(n.getInfraSpecificEpithet() == null);
    assertEquals("L.L.Daniel", n.getAuthorship());

    n = parser.parse(" X Pyrocrataegus willei L.L.Daniel");
    assertEquals(NamePart.GENERIC, n.getNotho());
    assertEquals("Pyrocrataegus", n.getGenusOrAbove());
    assertEquals("willei", n.getSpecificEpithet());
    assertTrue(n.getInfraSpecificEpithet() == null);
    assertEquals("L.L.Daniel", n.getAuthorship());

    n = parser.parse("Pyrocrataegus ×willei L.L.Daniel");
    assertEquals(NamePart.SPECIFIC, n.getNotho());
    assertEquals("Pyrocrataegus", n.getGenusOrAbove());
    assertEquals("willei", n.getSpecificEpithet());
    assertTrue(n.getInfraSpecificEpithet() == null);
    assertEquals("L.L.Daniel", n.getAuthorship());

    n = parser.parse("Pyrocrataegus × willei L.L.Daniel");
    assertEquals(NamePart.SPECIFIC, n.getNotho());
    assertEquals("Pyrocrataegus", n.getGenusOrAbove());
    assertEquals("willei", n.getSpecificEpithet());
    assertTrue(n.getInfraSpecificEpithet() == null);
    assertEquals("L.L.Daniel", n.getAuthorship());

    n = parser.parse("Pyrocrataegus x willei L.L.Daniel");
    assertEquals(NamePart.SPECIFIC, n.getNotho());
    assertEquals("Pyrocrataegus", n.getGenusOrAbove());
    assertEquals("willei", n.getSpecificEpithet());
    assertTrue(n.getInfraSpecificEpithet() == null);
    assertEquals("L.L.Daniel", n.getAuthorship());

    n = parser.parse("Pyrocrataegus X willei L.L.Daniel");
    assertEquals(NamePart.SPECIFIC, n.getNotho());
    assertEquals("Pyrocrataegus", n.getGenusOrAbove());
    assertEquals("willei", n.getSpecificEpithet());
    assertTrue(n.getInfraSpecificEpithet() == null);
    assertEquals("L.L.Daniel", n.getAuthorship());

    n = parser.parse("Pyrocrataegus willei ×libidi L.L.Daniel");
    assertEquals(NamePart.INFRASPECIFIC, n.getNotho());
    assertEquals("Pyrocrataegus", n.getGenusOrAbove());
    assertEquals("willei", n.getSpecificEpithet());
    assertEquals("libidi", n.getInfraSpecificEpithet());
    assertEquals("L.L.Daniel", n.getAuthorship());

    n = parser.parse("×Pyrocrataegus ×willei ×libidi L.L.Daniel");
    assertEquals(NamePart.INFRASPECIFIC, n.getNotho());
    assertEquals("Pyrocrataegus", n.getGenusOrAbove());
    assertEquals("willei", n.getSpecificEpithet());
    assertEquals("libidi", n.getInfraSpecificEpithet());
    assertEquals("L.L.Daniel", n.getAuthorship());

    n = parser.parse("+ Pyrocrataegus willei ×libidi L.L.Daniel");
    assertEquals(NamePart.INFRASPECIFIC, n.getNotho());
    assertEquals("Pyrocrataegus", n.getGenusOrAbove());
    assertEquals("willei", n.getSpecificEpithet());
    assertEquals("libidi", n.getInfraSpecificEpithet());
    assertEquals("L.L.Daniel", n.getAuthorship());

    n = parser.parse("Pyrocrataegus willei nothosubsp. libidi L.L.Daniel");
    assertEquals(NamePart.INFRASPECIFIC, n.getNotho());
    assertEquals("Pyrocrataegus", n.getGenusOrAbove());
    assertEquals("willei", n.getSpecificEpithet());
    assertEquals("libidi", n.getInfraSpecificEpithet());
    assertEquals("L.L.Daniel", n.getAuthorship());

    n = parser.parse("+ Pyrocrataegus willei nothosubsp. libidi L.L.Daniel");
    assertEquals(NamePart.INFRASPECIFIC, n.getNotho());
    assertEquals("Pyrocrataegus", n.getGenusOrAbove());
    assertEquals("willei", n.getSpecificEpithet());
    assertEquals("libidi", n.getInfraSpecificEpithet());
    assertEquals("L.L.Daniel", n.getAuthorship());
  }

  @Test
  public void testIndetParsering() throws Exception {
    ParsedName pn = parser.parse("Polygonum spec.");
    assertEquals("Polygonum", pn.getGenusOrAbove());
    assertNull(pn.getSpecificEpithet());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertEquals(NameType.INFORMAL, pn.getType());

    pn = parser.parse("Polygonum vulgaris ssp.");
    assertEquals("Polygonum", pn.getGenusOrAbove());
    assertEquals("vulgaris", pn.getSpecificEpithet());
    assertEquals(Rank.SUBSPECIES, pn.getRank());
    assertEquals(NameType.INFORMAL, pn.getType());

    pn = parser.parse("Mesocricetus sp.");
    assertEquals("Mesocricetus", pn.getGenusOrAbove());
    assertNull(pn.getSpecificEpithet());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertEquals(NameType.INFORMAL, pn.getType());

    // but dont treat these bacterial names as indets
    pn = parser.parse("Bartonella sp. RN, 10623LA");
    assertEquals("Bartonella", pn.getGenusOrAbove());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertTrue(NameType.INFORMAL != pn.getType());

    // and dont treat these authorships as forms
    pn = parser.parse("Dioscoreales Hooker f.");
    assertEquals("Dioscoreales", pn.getGenusOrAbove());
    assertEquals("Hooker f.", pn.getAuthorship());
    assertEquals(Rank.ORDER, pn.getRank());
    assertTrue(NameType.WELLFORMED == pn.getType());

    pn = parser.parse("Melastoma vacillans Blume var.");
  }

  @Test
  @Ignore
  public void testMonomialRsGbifOrg() throws Exception {
    NameParser np = new NameParser();
    np.readMonomialsRsGbifOrg();
  }

  @Test
  public void testNameFile() throws Exception {
    LOG.info("\n\nSTARTING FULL PARSER\n");
    Reader reader = FileUtils.getInputStreamReader(streamUtils.classpathStream(NAMES_TEST_FILE));
    LineIterator iter = new LineIterator(reader);

    int parseFails = 0;
    int parseNoAuthors = 0;
    int parseErrors = 0;
    int cnt = 0;
    int lineNum = 0;
    long start = System.currentTimeMillis();

    while (iter.hasNext()) {
      lineNum++;
      String line = iter.nextLine();
      if (lineNum == 1 || line == null || line.startsWith("#") || line.trim().isEmpty()) {
        continue;
      }
      cnt++;
      String[] cols = FileUtils.TAB_DELIMITED.split(line);
      String name = cols[0];
      if (cols.length < 14) {
        LOG.warn("Too short line in names test file:");
        LOG.warn(line);
      }
      ParsedName expected = buildParsedNameFromTabRow(cols);
      ParsedName n;
      try {
        n = parser.parse(name);
        if (!n.isAuthorsParsed()) {
          parseNoAuthors++;
        }
        // copy scientific name, we dont wanna compare it, it might be slightly different
        expected.setScientificName(n.getScientificName());
        if (!n.equals(expected)) {
          parseErrors++;
          LOG.warn("WRONG\t " + name + "\tEXPECTED: " + expected + "\tPARSED: " + n);
        }
      } catch (Exception e) {
        parseFails++;
        LOG.warn("FAIL\t " + name + "\tEXPECTED: " + expected);
      }
    }
    long end = System.currentTimeMillis();
    int successfulParses = cnt - parseFails - parseErrors;
    LOG.info("\n\nNames tested: " + cnt);
    LOG.info("Names success: " + successfulParses);
    LOG.info("Names parse fail: " + parseFails);
    LOG.info("Names parse errors: " + parseErrors);
    LOG.info("Names ignoring authors: " + parseNoAuthors);
    LOG.info("Total time: " + (end - start));
    LOG.info("Average per name: " + (((double) end - start) / cnt));

    if ((parseFails + parseErrors) > CURRENTLY_FAIL) {
      TestCase.fail(
        "We are getting worse, not better. Currently failing: " + (parseFails + parseErrors) + ". We used to fail :"
        + CURRENTLY_FAIL);
    } else if (parseFails + parseErrors < CURRENTLY_FAIL) {
      LOG.info("We are getting better! Used to fail : " + CURRENTLY_FAIL);
    }
    if ((parseFails + parseNoAuthors) > CURRENTLY_FAIL_EXCL_AUTHORS) {
      TestCase.fail(
        "We are getting worse, not better. Currently failing ignroing authors: " + (parseFails + parseNoAuthors)
        + ". Was passing:" + CURRENTLY_FAIL_EXCL_AUTHORS);
    } else if (parseFails + parseNoAuthors < CURRENTLY_FAIL_EXCL_AUTHORS) {
      LOG.info("We are getting better! Used to fail without authors: " + CURRENTLY_FAIL_EXCL_AUTHORS);
    }

  }

  @Test
  public void testNameParserCanonical() throws Exception {

    assertEquals("Abelia grandifolia", parser.parseToCanonical("Abelia grandifolia Villarreal, 2000 Villarreal, 2000"));
    assertEquals("Abelia macrotera",
      parser.parseToCanonical("Abelia macrotera (Graebn. & Buchw.) Rehder (Graebn. et Buchw.) Rehder"));
    assertEquals("Abelia mexicana", parser.parseToCanonical("Abelia mexicana Villarreal, 2000 Villarreal, 2000"));
    assertEquals("Abelia serrata", parser.parseToCanonical("Abelia serrata Abelia serrata Siebold & Zucc."));
    assertEquals("Abelia spathulata colorata", parser.parseToCanonical(
      "Abelia spathulata colorata (Hara & Kurosawa) Hara & Kurosawa (Hara et Kurosawa) Hara et Kurosawa"));
    assertEquals("Abelia tetrasepala",
      parser.parseToCanonical("Abelia tetrasepala (Koidz.) H.Hara & S.Kuros. (Koidz.) H.Hara et S.Kuros."));
    assertEquals("Abelia tetrasepala",
      parser.parseToCanonical("Abelia tetrasepala (Koidz.) Hara & Kurosawa (Koidz.) Hara et Kurosawa"));
    assertEquals("Abelmoschus esculentus",
      parser.parseToCanonical("Abelmoschus esculentus Abelmoschus esculentus (L.) Moench"));
    assertEquals("Abelmoschus moschatus",
      parser.parseToCanonical("Abelmoschus moschatus Abelmoschus moschatus Medik."));
    assertEquals("Abelmoschus moschatus tuberosus",
      parser.parseToCanonical("Abelmoschus moschatus subsp. tuberosus (Span.) Borss.Waalk."));
    assertEquals("Abia fasciata", parser.parseToCanonical("Abia fasciata (Linnaeus, 1758) (Linnaeus, 1758)"));
    assertEquals("Abia fasciata", parser.parseToCanonical("Abia fasciata Linnaeus, 1758 Linnaeus, 1758"));
    assertEquals("Abida secale", parser.parseToCanonical("Abida secale (Draparnaud, 1801) (Draparnaud) , 1801"));
    assertEquals("Abida secale brauniopsis", parser.parseToCanonical("Abida secale brauniopsis Bofill i Poch, Artur"));
    assertEquals("Abida secale inis", parser.parseToCanonical("Abida secale inis Bofill i Poch, Artur"));
    assertEquals("Abida secale meridionalis",
      parser.parseToCanonical("Abida secale meridionalis Martínez Ortí, AlbertoGómez, BenjamínFaci, Guill"));
    assertEquals("Abies alba", parser.parseToCanonical("Abies alba Abies alba Mill."));
    assertEquals("Abies alba", parser.parseToCanonical("Abies alba Abies alba Miller"));
    assertEquals("Abies alba", parser.parseToCanonical("Abies alba Mill. (species) : Mill."));
    assertEquals("Abies amabilis",
      parser.parseToCanonical("Abies amabilis Abies amabilis (Dougl. ex Loud.) Dougl. ex Forbes"));
    assertEquals("Abies amabilis", parser.parseToCanonical("Abies amabilis Abies amabilis Douglas ex Forbes"));
    assertEquals("Abies arizonica", parser.parseToCanonical("Abies arizonica Abies arizonica Merriam"));
    assertEquals("Abies balsamea", parser.parseToCanonical("Abies balsamea (L.) Mill. (species) : (L.) Mill."));
    assertEquals("Abies balsamea", parser.parseToCanonical("Abies balsamea (L.) Mill. / Ledeb."));
    assertEquals("Abies balsamea", parser.parseToCanonical("Abies balsamea Abies balsamea (L.) Mill."));
    assertEquals("Abelia grandiflora", parser.parseToCanonical("Abelia grandiflora Rehd (species) : Rehd"));
  }

  @Test
  public void testAutonyms() throws Exception {
    assertParsedParts("Panthera leo leo (Linnaeus, 1758)", NameType.SCINAME, "Panthera", "leo", "leo", null, null, null, "Linnaeus",
      "1758");
    assertParsedParts("Abies alba subsp. alba L.", NameType.SCINAME, "Abies", "alba", "alba", "subsp.", "L.");
    //TODO: improve nameparser to extract autonym authors, http://dev.gbif.org/issues/browse/GBIFCOM-10
    //    assertParsedParts("Abies alba L. subsp. alba", "Abies", "alba", "alba", "subsp.", "L.");
    // this is a wrong name! autonym authors are the species authors, so if both are given they must be the same!
    //    assertParsedParts("Abies alba L. subsp. alba Mill.", "Abies", "alba", "alba", "subsp.", "L.");
  }

  @Test
  public void testNameParserFull() throws Exception {
    assertParsedParts("Abies alba L.", NameType.WELLFORMED, "Abies", "alba", null, null, "L.");
    assertParsedParts("Abies alba var. kosovo", "Abies", "alba", "kosovo", "var.");
    assertParsedParts("Abies alba subsp. parafil", "Abies", "alba", "parafil", "subsp.");
    assertParsedParts("Abies   alba L. ssp. parafil DC.", "Abies", "alba", "parafil", "subsp.", "DC.");

    assertParsedParts("Nuculoidea behrens var.christoph Williams & Breger [1916]  ", NameType.DOUBTFUL, "Nuculoidea", "behrens",
      "christoph", "var.", "Williams & Breger", "1916");
    assertParsedParts(" Nuculoidea Williams et  Breger 1916  ", NameType.SCINAME, "Nuculoidea", null, null, null, "Williams & Breger", "1916");

    assertParsedParts("Nuculoidea behrens v.christoph Williams & Breger [1916]  ", NameType.DOUBTFUL, "Nuculoidea", "behrens", "christoph",
      "var.", "Williams & Breger", "1916");
    assertParsedParts("Nuculoidea behrens var.christoph Williams & Breger [1916]  ", NameType.DOUBTFUL, "Nuculoidea", "behrens",
      "christoph", "var.", "Williams & Breger", "1916");
    assertParsedParts(" Megacardita hornii  calafia", "Megacardita", "hornii", "calafia", null);
    assertParsedParts(" Megacardita hornii  calafia", "Megacardita", "hornii", "calafia", null);
    assertParsedParts(" A. anthophora acervorum", "A.", "anthophora", "acervorum", null);
    assertParsedParts(" x Festulolium nilssonii Cugnac & A. Camus", NameType.SCINAME, "Festulolium", null, "nilssonii", null, null,
      NamePart.GENERIC, "Cugnac & A. Camus", null, null, null, null, null);
    assertParsedParts("x Festulolium nilssonii Cugnac & A. Camus", NameType.SCINAME, "Festulolium", null, "nilssonii", null, null,
      NamePart.GENERIC, "Cugnac & A. Camus", null, null, null, null, null);
    assertParsedParts("Festulolium x nilssonii Cugnac & A. Camus", NameType.SCINAME, "Festulolium", null, "nilssonii", null, null,
      NamePart.SPECIFIC, "Cugnac & A. Camus", null, null, null, null, null);

    assertParsedParts("Ges Klaus 1895", "Ges", null, null, null, "Klaus", "1895");
    assertParsedParts("Ge Nicéville 1895", "Ge", null, null, null, "Nicéville", "1895");
    assertParsedParts("Œdicnemus capensis", "Œdicnemus", "capensis", null, null, null);
    assertParsedParts("Zophosis persis (Chatanay, 1914)", null, "Zophosis", "persis", null, null, null, null, "Chatanay",
      "1914");
    assertParsedParts("Caulerpa cupressoides forma nuda", "Caulerpa", "cupressoides", "nuda", "f.", null);
    assertParsedParts("Agalinis purpurea (L.) Briton var. borealis (Berg.) Peterson 1987", null, "Agalinis", "purpurea",
      "borealis", "var.", "Peterson", "1987", "Berg.", null);
    assertParsedParts("Agaricus squamula Berk. & M. A. Curtis 1860", null, "Agaricus", "squamula", null, null,
      "Berk. & M. A. Curtis", "1860", null, null);
    assertParsedParts("Agaricus squamula Berk. & M.A. Curtis 1860", null, "Agaricus", "squamula", null, null,
      "Berk. & M.A. Curtis", "1860", null, null);
    assertParsedParts("Cladoniicola staurospora Diederich, van den Boom & Aptroot 2001", "Cladoniicola", "staurospora",
      null, null, "Diederich, van den Boom & Aptroot", "2001");
    assertParsedParts(" Pompeja psorica Herrich-Schöffer [1854]", "Pompeja", "psorica", null, null, "Herrich-Schöffer",
      "1854");
    assertParsedParts(" Gloveria sphingiformis Barnes & McDunnough 1910", "Gloveria", "sphingiformis", null, null,
      "Barnes & McDunnough", "1910");
    assertParsedParts("Gastromega badia Saalmüller 1877/78", "Gastromega", "badia", null, null, "Saalmüller", "1877/78");
    assertParsedParts("Hasora coulteri Wood-Mason & de Nicóville 1886", "Hasora", "coulteri", null, null,
      "Wood-Mason & de Nicóville", "1886");
    assertParsedParts("Pithauria uma De Nicóville 1888", "Pithauria", "uma", null, null, "De Nicóville", "1888");
    assertParsedParts(" Lepidostoma quila Bueno-Soria & Padilla-Ramirez 1981", "Lepidostoma", "quila", null, null,
      "Bueno-Soria & Padilla-Ramirez", "1981");
    assertParsedParts(" Dinarthrum inerme McLachlan 1878", "Dinarthrum", "inerme", null, null, "McLachlan", "1878");

    assertParsedParts(" Triplectides tambina Mosely in Mosely & Kimmins 1953", "Triplectides", "tambina", null, null,
      "Mosely in Mosely & Kimmins", "1953");
    assertParsedParts(" Oxyothespis sudanensis Giglio-Tos 1916", "Oxyothespis", "sudanensis", null, null, "Giglio-Tos",
      "1916");
    assertParsedParts(" Parastagmatoptera theresopolitana (Giglio-Tos 1914)", null, "Parastagmatoptera", "theresopolitana",
      null, null, null, null, "Giglio-Tos", "1914");
    assertParsedParts(" Oxyothespis nilotica nilotica Giglio-Tos 1916", "Oxyothespis", "nilotica", "nilotica", null,
      "Giglio-Tos", "1916");
    assertParsedParts(" Photina Cardioptera burmeisteri (Westwood 1889)", null, "Photina", "burmeisteri", null, null, null,
      null, "Westwood", "1889");
    assertParsedParts(" Syngenes inquinatus (Gerstaecker, [1885])", null, "Syngenes", "inquinatus", null, null, null, null,
      "Gerstaecker", "1885");
    assertParsedParts(" Myrmeleon libelloides var. nigriventris A. Costa, [1855]", "Myrmeleon", "libelloides",
      "nigriventris", "var.", "A. Costa", "1855");
    assertParsedParts("Ascalaphus nigripes (van der Weele, [1909])", null, "Ascalaphus", "nigripes", null, null, null, null,
      "van der Weele", "1909");
    assertParsedParts(" Ascalaphus guttulatus A. Costa, [1855]", "Ascalaphus", "guttulatus", null, null, "A. Costa",
      "1855");
    assertParsedParts("Dichochrysa medogana (C.-K. Yang et al in Huang et al. 1988)", null, "Dichochrysa", "medogana", null,
      null, null, null, "C.-K. Yang et al. in Huang et al.", "1988");
    assertParsedParts(" Dichochrysa vitticlypea (C.-K. Yang & X.-X. Wang 1990)", null, "Dichochrysa", "vitticlypea", null,
      null, null, null, "C.-K. Yang & X.-X. Wang", "1990");
    assertParsedParts(" Dichochrysa qingchengshana (C.-K. Yang et al. 1992)", null, "Dichochrysa", "qingchengshana", null,
      null, null, null, "C.-K. Yang et al.", "1992");
    assertParsedParts(" Colomastix tridentata LeCroy 1995", "Colomastix", "tridentata", null, null, "LeCroy", "1995");
    assertParsedParts(" Sunamphitoe pelagica (H. Milne Edwards 1830)", null, "Sunamphitoe", "pelagica", null, null, null,
      null, "H. Milne Edwards", "1830");

    // TO BE CONTINUED
    assertParsedParts(" Brotogeris jugularis (Statius Muller 1776)", null, "Brotogeris", "jugularis", null, null, null, null,
      "Statius Muller", "1776");
    assertParsedParts(" Coracopsis nigra sibilans Milne-Edwards & OuStalet 1885", "Coracopsis", "nigra", "sibilans",
      null, "Milne-Edwards & OuStalet", "1885");
    assertParsedParts(" Trichoglossus haematodus deplanchii J. Verreaux & Des Murs 1860", "Trichoglossus", "haematodus",
      "deplanchii", null, "J. Verreaux & Des Murs", "1860");
    assertParsedParts(" Nannopsittaca dachilleae O'Neill, Munn & Franke 1991", "Nannopsittaca", "dachilleae", null,
      null, "O'Neill, Munn & Franke", "1991");
    assertParsedParts(" Ramphastos brevis Meyer de Schauensee 1945", "Ramphastos", "brevis", null, null,
      "Meyer de Schauensee", "1945");
    assertParsedParts(" Touit melanonota (Wied-Neuwied 1820)", null, "Touit", "melanonota", null, null, null, null,
      "Wied-Neuwied", "1820");
    assertParsedParts(" Trachyphonus darnaudii (Prevost & Des Murs 1847)", null, "Trachyphonus", "darnaudii", null, null,
      null, null, "Prevost & Des Murs", "1847");
    assertParsedParts(" Anolis porcatus aracelyae PEREZ-BEATO 1996", "Anolis", "porcatus", "aracelyae", null,
      "Perez-Beato", "1996");
    assertParsedParts(" Luzonichthys taeniatus Randall & McCosker 1992", "Luzonichthys", "taeniatus", null, null,
      "Randall & McCosker", "1992");
    assertParsedParts("Actinia stellula Hemprich and Ehrenberg in Ehrenberg 1834", "Actinia", "stellula", null, null,
      "Hemprich & Ehrenberg in Ehrenberg", "1834");
    assertParsedParts("Anemonia vagans (Less.) Milne Edw.", null, "Anemonia", "vagans", null, null, "Milne Edw.", null,
      "Less.", null);
    assertParsedParts("Epiactis fecunda (Verrill 1899b)", null, "Epiactis", "fecunda", null, null, null, null, "Verrill",
      "1899b");
    assertParsedParts(" Pseudocurimata Fernandez-Yepez 1948", "Pseudocurimata", null, null, null, "Fernandez-Yepez",
      "1948");
    assertParsedParts(" Hershkovitzia Guimarães & d'Andretta 1957", "Hershkovitzia", null, null, null,
      "Guimarães & d'Andretta", "1957");
    assertParsedParts(" Plectocolea (Mitten) Mitten in B.C. Seemann 1873", null, "Plectocolea", null, null, null,
      "Mitten in B.C. Seemann", "1873", "Mitten", null);
    assertParsedParts(" Discoporella d'Orbigny 1852", "Discoporella", null, null, null, "d'Orbigny", "1852");
    assertParsedParts(" Acripeza Guérin-Ménéville 1838", "Acripeza", null, null, null, "Guérin-Ménéville", "1838");
    assertParsedParts(" Subpeltonotus Swaraj Ghai, Kailash Chandra & Ramamurthy 1988", "Subpeltonotus", null, null,
      null, "Swaraj Ghai, Kailash Chandra & Ramamurthy", "1988");
    assertParsedParts(" Boettcherimima De Souza Lopes 1950", "Boettcherimima", null, null, null, "De Souza Lopes",
      "1950");
    assertParsedParts(" Surnicou Des Murs 1853", "Surnicou", null, null, null, "Des Murs", "1853");
    assertParsedParts(" Cristocypridea Hou MS. 1977", "Cristocypridea", null, null, null, "Hou MS.", "1977");
    assertParsedParts("Lecythis coriacea DC.", "Lecythis", "coriacea", null, null, "DC.");
    assertParsedParts(" Anhuiphyllum Yu Xueguang 1991", "Anhuiphyllum", null, null, null, "Yu Xueguang", "1991");
    assertParsedParts(" Zonosphaeridium minor Tian Chuanrong 1983", "Zonosphaeridium", "minor", null, null,
      "Tian Chuanrong", "1983");
    assertParsedParts(" Oscarella microlobata Muricy, Boury-Esnault, Bézac & Vacelet 1996", "Oscarella", "microlobata",
      null, null, "Muricy, Boury-Esnault, Bézac & Vacelet", "1996");
    assertParsedParts(" Neoarctus primigenius Grimaldi de Zio, D'Abbabbo Gallo & Morone de Lucia 1992", "Neoarctus",
      "primigenius", null, null, "Grimaldi de Zio, D'Abbabbo Gallo & Morone de Lucia", "1992");
    assertParsedParts(" Phaonia wenshuiensis Zhang, Zhao Bin & Wu 1985", "Phaonia", "wenshuiensis", null, null,
      "Zhang, Zhao Bin & Wu", "1985");
    assertParsedParts(" Heteronychia helanshanensis Han, Zhao-Gan & Ye 1985", "Heteronychia", "helanshanensis", null,
      null, "Han, Zhao-Gan & Ye", "1985");
    assertParsedParts(" Solanophila karisimbica ab. fulvicollis Mader 1941", "Solanophila", "karisimbica",
      "fulvicollis", "ab.", "Mader", "1941");
    assertParsedParts(" Tortrix Heterognomon aglossana Kennel 1899", "Tortrix", "aglossana", null, null, "Kennel",
      "1899");
    assertParsedParts(" Leptochilus (Neoleptochilus) beaumonti Giordani Soika 1953", "Leptochilus", "beaumonti", null,
      null, "Giordani Soika", "1953");
    assertParsedParts(" Lutzomyia (Helcocyrtomyia) rispaili Torres-Espejo, Caceres & le Pont 1995", "Lutzomyia",
      "rispaili", null, null, "Torres-Espejo, Caceres & le Pont", "1995");
    assertParsedParts("Gastropacha minima De Lajonquiére 1979", "Gastropacha", "minima", null, null, "De Lajonquiére",
      "1979");
    assertParsedParts("Lithobius elongipes Chamberlin (1952)", null, "Lithobius", "elongipes", null, null, null, null,
      "Chamberlin", "1952");
    assertParsedParts("Maxillaria sect. Multiflorae Christenson", "Maxillaria", null, null, "sect.", "Christenson");
    assertParsedParts("Maxillaria allenii L.O.Williams in Woodson & Schery", "Maxillaria", "allenii", null, null,
      "L.O.Williams in Woodson & Schery");
    assertParsedParts("Masdevallia strumosa P.Ortiz & E.Calderón", "Masdevallia", "strumosa", null, null,
      "P.Ortiz & E.Calderón");
    assertParsedParts("Neobisium (Neobisium) carcinoides balcanicum Hadži 1937", "Neobisium", "carcinoides",
      "balcanicum", null, "Hadži", "1937");
    assertParsedParts("Nomascus concolor subsp. lu Delacour, 1951", "Nomascus", "concolor", "lu", "subsp.", "Delacour",
      "1951");
    assertParsedParts("Polygonum subgen. Bistorta (L.) Zernov", null, "Polygonum", null, null, "subgen.", "Zernov", null,
      "L.", null);
    assertParsedParts("Stagonospora polyspora M.T. Lucas & Sousa da Câmara, 1934", "Stagonospora", "polyspora", null,
      null, "M.T. Lucas & Sousa da Câmara", "1934");

    assertParsedParts("Euphorbiaceae de Jussieu, 1789", "Euphorbiaceae", null, null, null, "de Jussieu", "1789");
    assertParsedParts("Leucanitis roda Herrich-Schäffer (1851) 1845", null, "Leucanitis", "roda", null, null, null, "1845",
      "Herrich-Schäffer", "1851");

    ParsedName pn = parser.parse("Loranthus incanus Schumach. & Thonn. subsp. sessilis Sprague");
    assertEquals("Loranthus", pn.getGenusOrAbove());
    assertEquals("incanus", pn.getSpecificEpithet());
    assertEquals("sessilis", pn.getInfraSpecificEpithet());
    assertEquals("Sprague", pn.getAuthorship());

    pn = parser.parse("Mascagnia brevifolia  var. paniculata Nied.,");
    assertEquals("Mascagnia", pn.getGenusOrAbove());
    assertEquals("brevifolia", pn.getSpecificEpithet());
    assertEquals("paniculata", pn.getInfraSpecificEpithet());
    assertEquals("Nied.", pn.getAuthorship());
    assertEquals(Rank.VARIETY, pn.getRank());

    pn = parser.parse("Leveillula jaczewskii U. Braun (ined.)");
    assertEquals("Leveillula", pn.getGenusOrAbove());
    assertEquals("jaczewskii", pn.getSpecificEpithet());
    assertEquals("U. Braun", pn.getAuthorship());
    assertEquals(Rank.SPECIES, pn.getRank());

    pn = parser.parse("Heteropterys leschenaultiana fo. ovata Nied.");
    assertEquals("Heteropterys", pn.getGenusOrAbove());
    assertEquals("leschenaultiana", pn.getSpecificEpithet());
    assertEquals("ovata", pn.getInfraSpecificEpithet());
    assertEquals("Nied.", pn.getAuthorship());
    assertEquals(Rank.FORM, pn.getRank());

    pn = parser.parse("Cymbella mendosa f. apiculata (Hust.) VanLand.");
    assertEquals("Cymbella", pn.getGenusOrAbove());
    assertEquals("mendosa", pn.getSpecificEpithet());
    assertEquals("apiculata", pn.getInfraSpecificEpithet());
    assertEquals("VanLand.", pn.getAuthorship());
    assertEquals(Rank.FORM, pn.getRank());

    pn = parser.parse("Lasioglossum channelense McGinley, 1986");
    assertEquals("Lasioglossum", pn.getGenusOrAbove());
    assertEquals("channelense", pn.getSpecificEpithet());
    assertEquals("McGinley", pn.getAuthorship());
    assertEquals("1986", pn.getYear());
    assertEquals(Rank.SPECIES, pn.getRank());

    pn = parser.parse("Liolaemus hermannunezi PINCHEIRA-DONOSO, SCOLARO & SCHULTE 2007");
    assertEquals("Liolaemus", pn.getGenusOrAbove());
    assertEquals("hermannunezi", pn.getSpecificEpithet());
    assertEquals("Pincheira-Donoso, Scolaro & Schulte", pn.getAuthorship());
    assertEquals("2007", pn.getYear());
    assertEquals(Rank.SPECIES, pn.getRank());

    pn = parser.parse("Liolaemus hermannunezi Pincheira-Donoso, Scolaro & Schulte, 2007");
    assertEquals("Liolaemus", pn.getGenusOrAbove());
    assertEquals("hermannunezi", pn.getSpecificEpithet());
    assertEquals("Pincheira-Donoso, Scolaro & Schulte", pn.getAuthorship());
    assertEquals("2007", pn.getYear());
    assertEquals(Rank.SPECIES, pn.getRank());

    pn = parser.parse("Pseudoeryx relictualis SCHARGEL, RIVAS-FUENMAYOR, BARROS & P.FAUR 2007");
    assertEquals("Pseudoeryx", pn.getGenusOrAbove());
    assertEquals("relictualis", pn.getSpecificEpithet());
    assertEquals("Schargel, Rivas-Fuenmayor, Barros & P.Faur", pn.getAuthorship());
    assertEquals("2007", pn.getYear());
    assertEquals(Rank.SPECIES, pn.getRank());

    pn = parser.parse("Cyrtodactylus phongnhakebangensis ZIEGLER, RÖSLER, HERRMANN & THANH 2003");
    assertEquals("Cyrtodactylus", pn.getGenusOrAbove());
    assertEquals("phongnhakebangensis", pn.getSpecificEpithet());
    assertEquals("Ziegler, Rösler, Herrmann & Thanh", pn.getAuthorship());
    assertEquals("2003", pn.getYear());
    assertEquals(Rank.SPECIES, pn.getRank());

    pn = parser.parse(
      "Cnemidophorus mumbuca Colli, Caldwell, Costa, Gainsbury, Garda, Mesquita, Filho, Soares, Silva, Valdujo, Vieira, Vitt, Wer");
    assertEquals("Cnemidophorus", pn.getGenusOrAbove());
    assertEquals("mumbuca", pn.getSpecificEpithet());
    assertEquals("Colli, Caldwell, Costa, Gainsbury, Garda, Mesquita, Filho, Soares, Silva, Valdujo, Vieira, Vitt, Wer",
      pn.getAuthorship());
    assertEquals(Rank.SPECIES, pn.getRank());

    pn = parser.parse(
      "Cnemidophorus mumbuca COLLI, CALDWELL, COSTA, GAINSBURY, GARDA, MESQUITA, FILHO, SOARES, SILVA, VALDUJO, VIEIRA, VITT, WER");
    assertEquals("Cnemidophorus", pn.getGenusOrAbove());
    assertEquals("mumbuca", pn.getSpecificEpithet());
    assertEquals("Colli, Caldwell, Costa, Gainsbury, Garda, Mesquita, Filho, Soares, Silva, Valdujo, Vieira, Vitt, Wer",
      pn.getAuthorship());
    assertEquals(Rank.SPECIES, pn.getRank());

    // TODO: fix this, da Costa Lima should be the author, no epithet
    // assertParsedParts(" Pseudophorellia da Costa Lima 1934", "Pseudophorellia", null, null, null,
    // "da Costa Lima","1934");

  }

  @Test
  public void testUnsupportedAuthors() throws Exception {
    // NOT YET COMPLETELY PARSING THE AUTHOR
    assertParsedParts(" Anolis marmoratus girafus LAZELL 1964: 377", "Anolis", "marmoratus", "girafus", null);
    assertParsedParts(" Chorististium maculatum (non Bloch 1790)", "Chorististium", "maculatum", null, null);
    assertParsedParts(" Pikea lunulata (non Guichenot 1864)", "Pikea", "lunulata", null, null);
    assertParsedParts(" Puntius stoliczkae (non Day 1871)", "Puntius", "stoliczkae", null, null);
    assertParsedParts(" Puntius arulius subsp. tambraparniei (non Silas 1954)", "Puntius", "arulius", "tambraparniei",
      "subsp.", null);
  }

  /**
   * http://dev.gbif.org/issues/browse/POR-2454
   */
  @Test
  public void testFungusNames() throws Exception {
    assertParsedParts("Merulius lacrimans (Wulfen : Fr.) Schum.", null, "Merulius", "lacrimans", null, null, "Schum.", null, "Wulfen : Fr.", null);
    assertParsedParts("Aecidium berberidis Pers. ex J.F. Gmel.", null, "Aecidium", "berberidis", null, null, "Pers. ex J.F. Gmel.", null);
    assertParsedParts("Roestelia penicillata (O.F. Müll.) Fr.", null, "Roestelia", "penicillata", null, null, "Fr.", null, "O.F. Müll.", null);

    assertParsedParts("Mycosphaerella eryngii (Fr. Duby) ex Oudem., 1897", null, "Mycosphaerella", "eryngii", null, null, "ex Oudem.", "1897", "Fr. Duby", null);
    assertParsedParts("Mycosphaerella eryngii (Fr.ex Duby) ex Oudem. 1897", null, "Mycosphaerella", "eryngii", null, null, "ex Oudem.", "1897", "Fr.ex Duby", null);
    assertParsedParts("Mycosphaerella eryngii (Fr. ex Duby) Johanson ex Oudem. 1897", null, "Mycosphaerella", "eryngii", null, null, "Johanson ex Oudem.", "1897", "Fr. ex Duby", null);
  }

  /**
   * http://dev.gbif.org/issues/browse/POR-2397
   */
  @Test
  public void testStrainNames() throws Exception {
    assertStrain("Candidatus Liberibacter solanacearum", NameType.CANDIDATUS, "Liberibacter", "solanacearum", null, null, null);
    assertStrain("Methylocystis sp. M6", NameType.INFORMAL, "Methylocystis", null, null, "sp.", "M6");
    assertStrain("Advenella kashmirensis W13003", NameType.INFORMAL, "Advenella", "kashmirensis", null, null, "W13003");
    assertStrain("Garra cf. dampaensis M23", NameType.INFORMAL, "Garra", "dampaensis", null, null, "M23");
    assertStrain("Sphingobium lucknowense F2", NameType.INFORMAL, "Sphingobium", "lucknowense", null, null, "F2");
    assertStrain("Pseudomonas syringae pv. atrofaciens LMG 5095T", NameType.INFORMAL, "Pseudomonas", "syringae", "atrofaciens", "pv.", "LMG 5095T");
  }

  @Test
  public void testPathovars() throws Exception {
    assertParsedParts("Xanthomonas campestris pv. citri (ex Hasse 1915) Dye 1978", NameType.SCINAME, "Xanthomonas", "campestris", "citri", "pv.", "Dye", "1978", "ex Hasse", "1915");
    assertParsedParts("Xanthomonas campestris pv. oryzae (Xco)", NameType.WELLFORMED, "Xanthomonas", "campestris", "oryzae", "pv.", null, null, "Xco", null);
    assertParsedParts("Streptococcus dysgalactiae (ex Diernhofer 1932) Garvie et al. 1983", NameType.SCINAME, "Streptococcus", "dysgalactiae", null, null, "Garvie et al.", "1983", "ex Diernhofer", "1932");
  }

  @Test
  public void testImprintYear() throws Exception {
    assertParsedParts(" Pompeja psorica Herrich-Schöffer [1854]", NameType.DOUBTFUL, "Pompeja", "psorica", null, null, "Herrich-Schöffer", "1854", null, null);
    assertParsedParts(" Syngenes inquinatus (Gerstaecker, [1885])", NameType.DOUBTFUL, "Syngenes", "inquinatus", null, null, null, null, "Gerstaecker", "1885");
    assertParsedParts(" Myrmeleon libelloides var. nigriventris A. Costa, [1855]", NameType.DOUBTFUL, "Myrmeleon", "libelloides", "nigriventris", "var.", "A. Costa", "1855");
    assertParsedParts("Ascalaphus nigripes (van der Weele, [1909])", NameType.DOUBTFUL, "Ascalaphus", "nigripes", null, null, null, null, "van der Weele", "1909");
    assertParsedParts(" Ascalaphus guttulatus A. Costa, [1855]", NameType.DOUBTFUL, "Ascalaphus", "guttulatus", null, null, "A. Costa", "1855");
  }

  @Test
  public void testNameParserIssue() throws Exception {
    // issue42
    ParsedName pn = parser.parse("Abacetus laevicollis de Chaudoir, 1869");
    assertEquals("Abacetus laevicollis", pn.canonicalNameWithMarker());
    assertEquals("de Chaudoir", pn.getAuthorship());
    assertEquals("1869", pn.getYear());
    assertTrue(1869 == pn.getYearInt());
    // issue50
    pn = parser.parse("Deinococcus-Thermus");
    assertEquals("Deinococcus-Thermus", pn.canonicalNameWithMarker());
    assertTrue(pn.getAuthorship() == null);
    assertTrue(pn.getYear() == null);
    // issue51
    pn = parser.parse("Alectis alexandrinus (Geoffroy St. Hilaire, 1817)");
    assertEquals("Alectis alexandrinus", pn.canonicalNameWithMarker());
    assertEquals("Geoffroy St. Hilaire", pn.getBracketAuthorship());
    assertEquals("1817", pn.getBracketYear());
    assertTrue(1817 == pn.getBracketYearInt());
    // issue60
    pn = parser.parse("Euphorbiaceae de Jussieu, 1789");
    assertEquals("Euphorbiaceae", pn.canonicalName());
    assertEquals("Euphorbiaceae", pn.getGenusOrAbove());
    assertEquals("de Jussieu", pn.getAuthorship());
    assertEquals("1789", pn.getYear());
  }

  @Test
  public void testNameParserRankMarker() throws Exception {
    assertEquals(Rank.SUBSPECIES, parser.parse("Coccyzuz americanus ssp.").getRank());
    assertEquals(Rank.SUBSPECIES, parser.parse("Coccyzuz ssp").getRank());
    assertEquals(Rank.SPECIES, parser.parse("Asteraceae spec.").getRank());

    ParsedName cn = parser.parse("Asteraceae spec.");
    assertEquals(Rank.SPECIES, cn.getRank());
    assertEquals("sp.", cn.getRankMarker());

    cn = parser.parse("Callideriphus flavicollis morph. reductus Fuchs 1961");
    assertEquals(Rank.INFRASUBSPECIFIC_NAME, cn.getRank());
    assertEquals("morph.", cn.getRankMarker());

    ParsedName pn = parser.parse("Euphrasia rostkoviana Hayne subvar. campestris (Jord.) Hartl");
    assertEquals(Rank.INFRASUBSPECIFIC_NAME, cn.getRank());
    assertEquals("subvar.", pn.getRankMarker());
    assertEquals("Euphrasia", pn.getGenusOrAbove());
    assertEquals("rostkoviana", pn.getSpecificEpithet());
    assertEquals("campestris", pn.getInfraSpecificEpithet());
    assertEquals("Hartl", pn.getAuthorship());
    assertEquals("Jord.", pn.getBracketAuthorship());

  }

  /**
   * test true bad occurrence names
   */
  @Test
  public void testNameParserSingleDirty() throws Exception {
    assertEquals("Verrucaria foveolata", parser.parseToCanonical("Verrucaria foveolata /Flörke) A. Massal."));
    assertEquals("Limatula gwyni", parser.parse("LImatula gwyni	(Sykes, 1903)").canonicalNameWithMarker());
    assertEquals("×Fatshedera lizei", parser.parse("× fatshedera lizei").canonicalNameWithMarker());
    assertEquals("Fatshedera lizei", parser.parse("fatshedera lizei ").canonicalNameWithMarker());
  }

  private ParsedName assertParsedParts(String name, String genus, String subgenus, String epithet, String infraepithet,
    String rank, NamePart notho, String author, String year, String basAuthor, String basYear, String nomStatus) throws UnparsableException {
    return assertParsedParts(name, null, genus, subgenus, epithet, infraepithet, rank, notho, author, year, basAuthor, basYear, nomStatus, null);
  }

  private ParsedName assertParsedParts(String name, NameType type, String genus, String subgenus, String epithet, String infraepithet,
    String rank, NamePart notho, String author, String year, String basAuthor, String basYear, String nomStatus, String strain)
    throws UnparsableException {
    ParsedName pn = parser.parse(name);
    if (type != null) {
      assertEquals(type, pn.getType());
    }
    assertEquals(genus, pn.getGenusOrAbove());
    assertEquals(epithet, pn.getSpecificEpithet());
    assertEquals(infraepithet, pn.getInfraSpecificEpithet());
    assertEquals(rank, pn.getRankMarker());
    assertEquals(notho, pn.getNotho());
    assertEquals(author, pn.getAuthorship());
    assertEquals(year, pn.getYear());
    assertEquals(basAuthor, pn.getBracketAuthorship());
    assertEquals(basYear, pn.getBracketYear());
    assertEquals(nomStatus, pn.getNomStatus());
    assertEquals(strain, pn.getStrain());

    return pn;
  }

  private ParsedName assertParsedName(String name, NameType type, String genus, String epithet, String infraepithet, String rank) {
    ParsedName pn = null;
    try {
      pn = assertParsedParts(name, type, genus, null, epithet, infraepithet, rank, null, null, null, null, null, null, null);
      assertEquals("Wrong name type", type, pn.getType());
    } catch (UnparsableException e) {
      assertEquals("Wrong name type", type, e.type);
    }
    return pn;
  }

  private void assertParsedParts(String name, NameType type, String genus, String epithet, String infraepithet, String rank, String author, String year, String basAuthor, String basYear) throws UnparsableException {
    assertParsedParts(name, type, genus, null, epithet, infraepithet, rank, null, author, year, basAuthor, basYear, null, null);
  }

  private void assertParsedParts(String name, String genus, String epithet, String infraepithet, String rank)
    throws UnparsableException {
    assertParsedParts(name, null, genus, epithet, infraepithet, rank, null, null, null, null);
  }

  private void assertParsedParts(String name, String genus, String epithet, String infraepithet, String rank, String author) throws UnparsableException {
    assertParsedParts(name, null, genus, epithet, infraepithet, rank, author, null, null, null);
  }

  private void assertParsedParts(String name, NameType type, String genus, String epithet, String infraepithet, String rank, String author) throws UnparsableException {
    assertParsedParts(name, type, genus, epithet, infraepithet, rank, author, null, null, null);
  }

  private void assertParsedParts(String name, String genus, String epithet, String infraepithet, String rank, String author, String year) throws UnparsableException {
    assertParsedParts(name, null, genus, epithet, infraepithet, rank, author, year, null, null);
  }

  private void assertParsedParts(String name, NameType type, String genus, String epithet, String infraepithet, String rank, String author, String year) throws UnparsableException {
    assertParsedParts(name, type, genus, epithet, infraepithet, rank, author, year, null, null);
  }

  private void assertStrain(String name, NameType type, String genus, String epithet, String infraepithet, String rank, String strain) throws UnparsableException {
    assertParsedParts(name, type, genus, null, epithet, infraepithet, rank, null, null, null, null, null, null, strain);
  }

  @Test
  public void testInfragenericRanks() throws Exception {
    assertEquals("Bodotria subgen. Vertebrata", parser.parse("Bodotria (Vertebrata)").canonicalNameWithMarker());
    assertEquals("Bodotria", parser.parse("Bodotria (Goodsir)").canonicalNameWithMarker());
    assertEquals("Latrunculia subgen. Biannulata", parser.parse("Latrunculia (Biannulata)").canonicalNameWithMarker());
    assertEquals("Chordata subgen. Cephalochordata",
      parser.parse("Chordata (Cephalochordata)").canonicalNameWithMarker());

    assertParsedParts("Saperda (Saperda) candida m. bipunctata Breuning, 1952", NameType.SCINAME, "Saperda", "Saperda", "candida",
      "bipunctata", "m.", null, "Breuning", "1952", null, null, null, null);

    assertParsedParts("Carex section Acrocystis", NameType.SCINAME, "Carex", "Acrocystis", null, null, "sect.", null, null, null, null,
      null, null, null);

    assertParsedParts("Juncus subgenus Alpini", NameType.SCINAME, "Juncus", "Alpini", null, null, "subgen.", null, null, null, null, null,
      null, null);

    assertParsedParts("Solidago subsection Triplinervae", NameType.SCINAME, "Solidago", "Triplinervae", null, null, "subsect.", null,
      null, null, null, null, null, null);

    assertParsedParts("Eleocharis series Maculosae", NameType.SCINAME, "Eleocharis", "Maculosae", null, null, "ser.", null, null, null,
      null, null, null, null);

  }


  @Test
  public void testBlacklisted() throws Exception {
    assertBlacklisted("unknown Plantanus");
    assertBlacklisted("Macrodasyida incertae sedis");
    assertBlacklisted(" uncertain Plantanus");
    assertBlacklisted("Unknown Cyanobacteria");
    assertBlacklisted("Unknown methanomicrobium (strain EBac)");
    assertBlacklisted("Demospongiae incertae sedis");
    assertBlacklisted("Unallocated Demospongiae");
    assertBlacklisted("Asteracea (awaiting allocation)");
    assertBlacklisted("143");
    assertBlacklisted("321-432");
    assertBlacklisted("-,.#");
    assertBlacklisted("");
    assertBlacklisted(" ");
    assertBlacklisted(" .");
  }

  @Test
  public void testUnparsables() throws Exception {
    assertUnparsableDoubtful("pilosa 1986 ?");
    assertUnparsableDoubtful("H4N2 subtype");
    assertUnparsableDoubtful("endosymbiont 'C3 71' of Calyptogena sp. JS, 2002");
  }

  @Test
  public void testNameType() throws Exception {
    assertUnparsableType(NameType.HYBRID, "Asplenium rhizophyllum DC. x ruta-muraria E.L. Braun 1939");
    assertUnparsableType(NameType.HYBRID, "Agrostis L. × Polypogon Desf. ");

    assertUnparsableType(NameType.VIRUS, "Cactus virus 2");

    assertEquals(NameType.WELLFORMED, parser.parse("Neobisium carcinoides subsp. balcanicum Hadži, 1937").getType());
    assertEquals(NameType.WELLFORMED, parser.parse("Festulolium nilssonii Cugnac & A. Camus").getType());
    assertEquals(NameType.WELLFORMED, parser.parse("Coccyzuz americanus").getType());
    assertEquals(NameType.WELLFORMED, parser.parse("Tavila indeterminata Walker, 1869").getType());
    assertEquals(NameType.WELLFORMED, parser.parse("Phyllodonta indeterminata Schaus, 1901").getType());
    assertEquals(NameType.WELLFORMED, parser.parse("×Festulolium nilssonii Cugnac & A. Camus").getType());

    // not using the multiplication sign, but an simple x
    assertEquals(NameType.SCINAME, parser.parse("x Festulolium nilssonii Cugnac & A. Camus").getType());
    assertEquals(NameType.SCINAME, parser.parse("xFestulolium nilssonii Cugnac & A. Camus").getType());
    assertEquals(NameType.SCINAME, parser.parse("nuculoidea behrens subsp. behrens var.christoph").getType());
    assertEquals(NameType.SCINAME, parser.parse("Neobisium (Neobisium) carcinoides balcanicum Hadži 1937").getType());
    assertEquals(NameType.SCINAME, parser.parse("Neobisium carcinoides ssp. balcanicum Hadži, 1937").getType());
    assertEquals(NameType.SCINAME,
      parser.parse("Valsa hypodermia sensu Berkeley & Broome (Not. Brit. Fungi no., 862)").getType());
    assertEquals(NameType.SCINAME, parser.parse("Solanum aculeatissimum auct. not Jacq.").getType());

    assertEquals(NameType.INFORMAL, parser.parse("Coccyzuz cf americanus").getType());
    assertEquals(NameType.INFORMAL, parser.parse("Coccyzuz americanus ssp.").getType());
    assertEquals(NameType.INFORMAL, parser.parse("Asteraceae spec").getType());

    assertEquals(NameType.DOUBTFUL, parser.parse("Callitrichia pilosa (Jocqu¿ & Scharff, 1986)").getType());
    assertEquals(NameType.DOUBTFUL, parser.parse("Sinopoda exspectata J¿ger & Ono, 2001").getType());
    assertEquals(NameType.DOUBTFUL, parser.parse("Callitrichia pilosa 1986 ?").getType());
    assertEquals(NameType.DOUBTFUL,
      parser.parse("Scaphytopius acutus delongi Young, 1952c:, 248 [n.sp. , not nom. nov.]").getType());

  }

  @Test
  public void testLowerCaseMonomials() throws Exception {
    assertUnparsableDoubtful("tree");
    assertUnparsableDoubtful(" tree");
    assertUnparsableDoubtful("tim");
    assertUnparsableDoubtful("abies");
    assertUnparsableDoubtful("abies (Thats it)");
  }

  @Test
  public void testSensuParsing() throws Exception {
    assertEquals("sensu Baker f.", parser.parse("Trifolium repens sensu Baker f.").getSensu());
    assertEquals("sensu latu", parser.parse("Achillea millefolium sensu latu").getSensu());
    assertEquals("sec. Greuter, 2009", parser.parse("Achillea millefolium sec. Greuter 2009").getSensu());
  }

  @Test
  public void testNomenclaturalNotesPattern() throws Exception {
    assertEquals("nom. illeg.", extractNomNote("Vaucheria longicaulis var. bengalensis Islam, nom. illeg."));
    assertEquals("nom. correct", extractNomNote("Dorataspidae nom. correct"));
    assertEquals("nom. transf.", extractNomNote("Ethmosphaeridae nom. transf."));
    assertEquals("nom. ambig.", extractNomNote("Fucus ramosissimus Oeder, nom. ambig."));
    assertEquals("nom. nov.", extractNomNote("Myrionema majus Foslie, nom. nov."));
    assertEquals("nom. utique rej.", extractNomNote("Corydalis bulbosa (L.) DC., nom. utique rej."));
    assertEquals("nom. cons. prop.", extractNomNote("Anthoceros agrestis var. agrestis Paton nom. cons. prop."));
    assertEquals("nom. superfl.",
      extractNomNote("Lithothamnion glaciale forma verrucosum (Foslie) Foslie, nom. superfl."));
    assertEquals("nom.rejic.",
      extractNomNote("Pithecellobium montanum var. subfalcatum (Zoll. & Moritzi)Miq., nom.rejic."));
    assertEquals("nom. inval",
      extractNomNote("Fucus vesiculosus forma volubilis (Goodenough & Woodward) H.T. Powell, nom. inval"));
    assertEquals("nom. nud.", extractNomNote("Sao hispanica R. & E. Richter nom. nud. in Sampelayo 1935"));
    assertEquals("nom.illeg.", extractNomNote("Hallo (nom.illeg.)"));
    assertEquals("nom. super.", extractNomNote("Calamagrostis cinnoides W. Bart. nom. super."));
    assertEquals("nom. nud.", extractNomNote("Iridaea undulosa var. papillosa Bory de Saint-Vincent, nom. nud."));
    assertEquals("nom. inval",
      extractNomNote("Sargassum angustifolium forma filiforme V. Krishnamurthy & H. Joshi, nom. inval"));
    assertEquals("nomen nudum", extractNomNote("Solanum bifidum Vell. ex Dunal, nomen nudum"));
    assertEquals("nomen invalid.",
      extractNomNote("Schoenoplectus ×scheuchzeri (Bruegger) Palla ex Janchen, nomen invalid."));
    assertEquals("nom. nud.",
      extractNomNote("Cryptomys \"Kasama\" Kawalika et al., 2001, nom. nud. (Kasama, Zambia) ."));
    assertEquals("nom. super.", extractNomNote("Calamagrostis cinnoides W. Bart. nom. super."));
    assertEquals("nom. dub.", extractNomNote("Pandanus odorifer (Forssk.) Kuntze, nom. dub."));
    assertEquals("nom. rejic.", extractNomNote("non Clarisia Abat, 1792, nom. rejic."));
    assertEquals("nom. cons", extractNomNote(
      "Yersinia pestis (Lehmann and Neumann, 1896) van Loghem, 1944 (Approved Lists, 1980) , nom. cons"));
    assertEquals("nom. rejic.",
      extractNomNote("\"Pseudomonas denitrificans\" (Christensen, 1903) Bergey et al., 1923, nom. rejic."));
    assertEquals("nom. nov.", extractNomNote("Tipula rubiginosa Loew, 1863, nom. nov."));
    assertEquals("nom. prov.", extractNomNote("Amanita pruittii A.H.Sm. ex Tulloss & J.Lindgr., nom. prov."));
    assertEquals("nom. cons.", extractNomNote("Ramonda Rich., nom. cons."));
    assertEquals("nom. cons.", extractNomNote(
      "Kluyver and van Niel, 1936 emend. Barker, 1956 (Approved Lists, 1980) , nom. cons., emend. Mah and Kuhn, 1984"));
    assertEquals("nom. superfl.", extractNomNote("Coccocypselum tontanea (Aubl.) Kunth, nom. superfl."));
    assertEquals("nom. ambig.", extractNomNote("Lespedeza bicolor var. intermedia Maxim. , nom. ambig."));
    assertEquals("nom. praeoccup.", extractNomNote("Erebia aethiops uralensis Goltz, 1930 nom. praeoccup."));

    assertEquals("comb. nov. ined.", extractNomNote("Ipomopsis tridactyla (Rydb.) Wilken, comb. nov. ined."));
    assertEquals("sp. nov. ined.", extractNomNote("Orobanche riparia Collins, sp. nov. ined."));
    assertEquals("gen. nov.", extractNomNote("Anchimolgidae gen. nov. New Caledonia-Rjh-, 2004"));
    assertEquals("gen. nov. ined.", extractNomNote("Stebbinsoseris gen. nov. ined."));
    assertEquals("var. nov.", extractNomNote("Euphorbia rossiana var. nov. Steinmann, 1199"));

  }

  @Test
  public void testNormalizeName() {
    assertEquals("Nuculoidea Williams et Breger, 1916", NameParser.normalize("Nuculoidea Williams et  Breger 1916  "));
    assertEquals("Nuculoidea behrens var. christoph Williams & Breger [1916]",
      NameParser.normalize("Nuculoidea behrens var.christoph Williams & Breger [1916]  "));
    assertEquals("Nuculoidea behrens var. christoph Williams & Breger [1916]",
      NameParser.normalize("Nuculoidea behrens var.christoph Williams & Breger [1916]  "));
    assertEquals(NameParser.normalize("Nuculoidea Williams & Breger, 1916  "),
      NameParser.normalize("Nuculoidea   Williams& Breger, 1916"));
    assertEquals("Asplenium ×inexpectatum (E.L. Braun, 1940) Morton (1956)",
      NameParser.normalize("Asplenium X inexpectatum (E.L. Braun 1940)Morton (1956) "));
    assertEquals("×Agropogon", NameParser.normalize(" × Agropogon"));
    assertEquals("Salix ×capreola Andersson", NameParser.normalize("Salix × capreola Andersson"));
    assertEquals("Leucanitis roda Herrich-Schäffer (1851), 1845",
      NameParser.normalize("Leucanitis roda Herrich-Schäffer (1851) 1845"));

    assertEquals("Huaiyuanella Xing, Yan & Yin, 1984", NameParser.normalize("Huaiyuanella Xing, Yan&Yin, 1984"));

  }

  @Test
  public void testNormalizeStrongName() {
    assertEquals("Nuculoidea Williams & Breger, 1916",
      NameParser.normalize(NameParser.normalizeStrong("Nuculoidea Williams et  Breger 1916  ")));
    assertEquals("Nuculoidea behrens var. christoph Williams & Breger, 1916",
      NameParser.normalize(NameParser.normalizeStrong("Nuculoidea behrens var.christoph Williams & Breger [1916]  ")));
    assertEquals("N. behrens Williams & Breger, 1916",
      NameParser.normalize(NameParser.normalizeStrong("  N.behrens Williams &amp;  Breger , 1916  ")));
    assertEquals("Nuculoidea Williams & Breger, 1916",
      NameParser.normalize(NameParser.normalizeStrong(" 'Nuculoidea Williams & Breger, 1916'")));
    assertEquals("Malacocarpus schumannianus (Nicolai, 1893) Britton & Rose",
      NameParser.normalize(NameParser.normalizeStrong("Malacocarpus schumannianus (Nicolai (1893)) Britton & Rose")));
    assertEquals("Photina (Cardioptera) burmeisteri (Westwood, 1889)",
      NameParser.normalize(NameParser.normalizeStrong("Photina Cardioptera burmeisteri (Westwood 1889)")));
    assertEquals("Suaeda forsskahlei Schweinf.",
      NameParser.normalize(NameParser.normalizeStrong("Suaeda forsskahlei Schweinf. ms.")));
    assertEquals("Acacia bicolor Bojer", NameParser.normalize(NameParser.normalizeStrong("Acacia bicolor Bojer ms.")));

    assertEquals("Leucanitis roda (Herrich-Schäffer, 1851), 1845",
      NameParser.normalize(NameParser.normalizeStrong("Leucanitis roda Herrich-Schäffer (1851) 1845")));
  }

  @Test
  public void testOccNameFile() throws Exception {
    Reader reader = FileUtils.getInputStreamReader(streamUtils.classpathStream("parseNull.txt"));
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
        n = parser.parse(name);
        if (!n.isAuthorsParsed()) {
          parseAuthorless++;
          LOG.warn("NO AUTHORS\t " + name);
        }
      } catch (UnparsableException e) {
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
  public void testPeterDesmetNPE() throws Exception {
    ParsedName n = null;
    boolean parsed = false;
    try {
      n = parser.parse("(1-SFS) Aster lowrieanus Porter");
      parsed = true;
    } catch (UnparsableException e) {
      // nothing, expected to fail
    }
    try {
      n = parser.parse("(1-SFS)%20Aster%20lowrieanus%20Porter");
      parsed = true;
    } catch (UnparsableException e) {
      // nothing, expected to fail
    }
  }

  /**
   * These names have been parsing extremely slowely before - make sure this doesnt happen again
   */
  @Test
  public void testSlowNames() throws Exception {
    long start = System.currentTimeMillis();
    ParsedName pn = parser.parse("\"Acetobacter aceti var. muciparum\" (sic) (Hoyer) Frateur, 1950");
    assertEquals("Acetobacter", pn.getGenusOrAbove());
    assertEquals("aceti", pn.getSpecificEpithet());
    assertEquals("muciparum", pn.getInfraSpecificEpithet());
    assertEquals("var.", pn.getRankMarker());
    assertTrue(System.currentTimeMillis() - start < SLOW);

    start = System.currentTimeMillis();
    pn = parser.parse("\"Acetobacter melanogenum (sic) var. malto-saccharovorans\" Frateur, 1950");
    assertEquals("Acetobacter", pn.getGenusOrAbove());
    assertEquals("melanogenum", pn.getSpecificEpithet());
    assertTrue(System.currentTimeMillis() - start < SLOW);

    start = System.currentTimeMillis();
    pn = parser.parse("\"Acetobacter melanogenum (sic) var. maltovorans\" Frateur, 1950");
    assertEquals("Acetobacter", pn.getGenusOrAbove());
    assertEquals("melanogenum", pn.getSpecificEpithet());
    assertTrue(System.currentTimeMillis() - start < SLOW);

    start = System.currentTimeMillis();
    pn = parser.parse("'Abelmoschus esculentus' bunchy top phytoplasma");
    assertEquals("Abelmoschus", pn.getGenusOrAbove());
    assertEquals("esculentus", pn.getSpecificEpithet());
    assertTrue(System.currentTimeMillis() - start < SLOW);

    assertNotSlow("'38/89' designation is probably a typo");
    assertNotSlow("Argyropelecus d'Urvillei Valenciennes, 1849");
    assertNotSlow("Batillipes africanus Morone De Lucia, D'Addabbo Gallo and Grimaldi de Zio, 1988");
    assertNotSlow("Blainville's beaked whale gammaherpesvirus");
    assertNotSlow("Abrotanellinae H.Rob., G.D.Carr, R.M.King & A.M.Powell");
    assertNotSlow("Acidomyces B.J. Baker, M.A. Lutz, S.C. Dawson, P.L. Bond & Banfield");
    assertNotSlow("Acidomyces richmondensis B.J. Baker, M.A. Lutz, S.C. Dawson, P.L. Bond & Banfield, 2004");
    assertNotSlow("Acrodictys liputii L. Cai, K.Q. Zhang, McKenzie, W.H. Ho & K.D. Hyde");
    assertNotSlow("×Attabignya minarum M.J.Balick, A.B.Anderson & J.T.de Medeiros-Costa");
    assertNotSlow(
      "Paenibacillus donghaensis Choi,J.H.; Im,W.T.; Yoo,J.S.; Lee,S.M.; Moon,D.S.; Kim,H.J.; Rhee,S.K.; Roh,D.H.");

    // TODO: fix nameparser so that these names are faster!
    //assertNotSlow("Yamatocallis obscura (Ghosh, M.R., A.K. Ghosh & D.N. Raychaudhuri, 1971");
    //assertNotSlow("Xanthotrogus tadzhikorum Nikolajev, 2008");
    //assertNotSlow("Xylothamia G.L. Nesom, Y.B. Suh, D.R. Morgan & B.B. Simpson, 1990");
    //assertNotSlow("Virginianthus E.M. Friis, H. Eklund, K.R. Pedersen & P.R. Crane, 1994");

  }

  /**
   * @return true if name could be parsed
   */
  private boolean assertNotSlow(String name) {
    long start = System.currentTimeMillis();
    boolean parsed = false;
    try {
      ParsedName pn = parser.parse(name);
      parsed = true;
    } catch (UnparsableException e) {
      // TODO: Handle exception
    }
    assertTrue(System.currentTimeMillis() - start < SLOW);
    return parsed;
  }

  @Test
  public void testSpacelessAuthors() throws Exception {

    ParsedName pn = parser.parse("Abelmoschus moschatus Medik. subsp. tuberosus (Span.) Borss.Waalk.");
    assertEquals("Abelmoschus", pn.getGenusOrAbove());
    assertEquals("moschatus", pn.getSpecificEpithet());
    assertEquals("tuberosus", pn.getInfraSpecificEpithet());
    assertEquals("Borss.Waalk.", pn.getAuthorship());
    assertEquals("Span.", pn.getBracketAuthorship());
    assertEquals(NameType.SCINAME, pn.getType());

    pn = parser.parse("Acalypha hochstetteriana Müll.Arg.");
    assertEquals("Acalypha", pn.getGenusOrAbove());
    assertEquals("hochstetteriana", pn.getSpecificEpithet());
    assertEquals("Müll.Arg.", pn.getAuthorship());
    assertEquals(NameType.WELLFORMED, pn.getType());

    pn = parser.parse("Caloplaca variabilis (Pers.) Müll.Arg.");
    assertEquals("Caloplaca", pn.getGenusOrAbove());
    assertEquals("variabilis", pn.getSpecificEpithet());
    assertEquals("Müll.Arg.", pn.getAuthorship());
    assertEquals("Pers.", pn.getBracketAuthorship());
    assertEquals(NameType.WELLFORMED, pn.getType());

    pn = parser.parse("Tridax imbricatus Sch.Bip.");
    assertEquals("Tridax", pn.getGenusOrAbove());
    assertEquals("imbricatus", pn.getSpecificEpithet());
    assertEquals("Sch.Bip.", pn.getAuthorship());
    assertEquals(NameType.WELLFORMED, pn.getType());

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

    assertFalse(isViralName("Forcipomyia flavirustica Remm, 1968"));

  }

  @Test
  public void testBacteriaNames() throws Exception {
    assertUnparsableDoubtful("HMWdDNA-degrading marine bacterium 10");
    assertUnparsableDoubtful("SAR11 cluster bacterium enrichment culture clone Pshtc-, 019");
    assertUnparsableDoubtful("SR1 bacterium canine oral taxon, 369");
    assertUnparsableDoubtful("bacterium 'glyphosate'");
    assertUnparsableDoubtful("bacterium 'hafez'");
    assertUnparsableDoubtful("bacterium A1");
  }

  public boolean isViralName(String name) {
    try {
      ParsedName pn = parser.parse(name);
    } catch (UnparsableException e) {
      // swallow
      if (NameType.VIRUS == e.type) {
        return true;
      }
    }
    return false;
  }

  @Test
  public void testWhitespaceEpitheta() throws Exception {
    ParsedName n = parser.parse("Nupserha van rooni usambarica");
    assertEquals("Nupserha", n.getGenusOrAbove());
    assertTrue(n.getInfraGeneric() == null);
    assertEquals("van rooni", n.getSpecificEpithet());
    assertEquals("usambarica", n.getInfraSpecificEpithet());

    n = parser.parse("Sargassum flavicans van pervillei");
    assertEquals("Sargassum", n.getGenusOrAbove());
    assertTrue(n.getInfraGeneric() == null);
    assertEquals("flavicans", n.getSpecificEpithet());
    assertEquals("van pervillei", n.getInfraSpecificEpithet());

    n = parser.parse("Salix novae angliae lingulata ");
    assertEquals("Salix", n.getGenusOrAbove());
    assertTrue(n.getInfraGeneric() == null);
    assertEquals("novae angliae", n.getSpecificEpithet());
    assertEquals("lingulata", n.getInfraSpecificEpithet());

    n = parser.parse("Ilex collina van trompii");
    assertEquals("Ilex", n.getGenusOrAbove());
    assertTrue(n.getInfraGeneric() == null);
    assertEquals("collina", n.getSpecificEpithet());
    assertEquals("van trompii", n.getInfraSpecificEpithet());

    n = parser.parse("Gaultheria depressa novae zealandiae");
    assertEquals("Gaultheria", n.getGenusOrAbove());
    assertTrue(n.getInfraGeneric() == null);
    assertEquals("depressa", n.getSpecificEpithet());
    assertEquals("novae zealandiae", n.getInfraSpecificEpithet());

    n = parser.parse("Caraguata van volxemi gracilior");
    assertEquals("Caraguata", n.getGenusOrAbove());
    assertTrue(n.getInfraGeneric() == null);
    assertEquals("van volxemi", n.getSpecificEpithet());
    assertEquals("gracilior", n.getInfraSpecificEpithet());

    n = parser.parse("Ancistrocerus agilis novae guineae");
    assertEquals("Ancistrocerus", n.getGenusOrAbove());
    assertTrue(n.getInfraGeneric() == null);
    assertEquals("agilis", n.getSpecificEpithet());
    assertEquals("novae guineae", n.getInfraSpecificEpithet());

    // also test the authorless parsing
    n = parser.parse("Ancistrocerus agilis novae guineae");
    assertEquals("Ancistrocerus", n.getGenusOrAbove());
    assertTrue(n.getInfraGeneric() == null);
    assertEquals("agilis", n.getSpecificEpithet());
    assertEquals("novae guineae", n.getInfraSpecificEpithet());
  }


  @Test
 // @Ignore("This test is left only to manually try to parse and test new names. Please leave it in the code")
  public void testNewProblemNames() {
    // if no args supplied, then use some default examples
    String[] names = new String[] {"Candidatus Liberibacter solanacearum","Advenella kashmirensis W13003","Garra cf. dampaensis M23","Sphingobium lucknowense F2","Pseudomonas syringae pv. atrofaciens LMG 5095"};

    for (String name : names) {
      LOG.debug("\n\nIN   : " + name);
      ParsedName pn = null;
      try {
        pn = parser.parse(name);
      } catch (UnparsableException e) {
        LOG.error("UnparsableException", e);
      }
      LOG.debug("NORM : " + normalize(name));

      if (pn != null) {
        LOG.info("FULL : " + pn);
      } else {
        LOG.info("FULL : CANNOT PARSE");
      }
    }
  }
}



