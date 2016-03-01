package org.gbif.nameparser;

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.NamePart;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.Rank;
import org.gbif.utils.file.FileUtils;
import org.gbif.utils.file.InputStreamUtils;

import java.io.Reader;
import java.util.List;
import java.util.regex.Matcher;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import junit.framework.TestCase;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  private static final InputStreamUtils streamUtils = new InputStreamUtils();
  private NameParser parser = new NameParser(50);

  private void assertHybridFormula(String name) {
    try {
      parser.parse(name, null);
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
      parser.parse(name, null);
      fail("Name should be an unparsable " + type + ": " + name);
    } catch (UnparsableException e) {
      assertEquals(type, e.type);
    }
  }

  private void assertNoName(String name) {
    assertUnparsableType(NameType.NO_NAME, name);
  }

  private void assertUnparsableDoubtful(String name) {
    try {
      parser.parse(name, null);
      fail("Name should be unparsable: " + name);
    } catch (UnparsableException e) {
      assertEquals(NameType.DOUBTFUL, e.type);
    }
  }

  private void allowUnparsable(String name) {
    try {
      parser.parse(name, null);
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
    ParsedName pn = null;
    try {
      pn = new ParsedName(NameType.fromString(cols[13]), StringUtils.trimToNull(cols[1]),
          StringUtils.trimToNull(cols[2]), StringUtils.trimToNull(cols[3]), StringUtils.trimToNull(cols[4]),
          NamePart.fromString(cols[12]), StringUtils.trimToNull(cols[10]), StringUtils.trimToNull(cols[6]),
          StringUtils.trimToNull(cols[7]), StringUtils.trimToNull(cols[8]), StringUtils.trimToNull(cols[9]),
          StringUtils.trimToNull(cols[5]), null, null, null, null);
    } catch (ArrayIndexOutOfBoundsException e) {
      LOG.error("scientific_names.txt file bogus, too little columns:\n{}", Joiner.on("|").useForNull("").join(cols));
      throw e;
    }
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
    ParsedName n = parser.parse("Bombus sichelii alticola latofasciatus", null);
    assertEquals("Bombus sichelii infrasubsp. latofasciatus", n.canonicalNameWithMarker());
    assertEquals("Bombus sichelii latofasciatus", n.canonicalName());

    n = parser.parse("Bombus sichelii alticola latofasciatus Vogt, 1909", null);
    assertEquals("Bombus sichelii infrasubsp. latofasciatus", n.canonicalNameWithMarker());
    assertEquals("Bombus sichelii latofasciatus", n.canonicalName());
  }

  private boolean testAuthorship(String author) {
    Matcher m = NormalisedNameParser.AUTHOR_TEAM_PATTERN.matcher(author);
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
    ParsedName pn = parser.parse("Diatrypella favacea var. favacea (Fr.) Ces. & De Not.", null);
    assertEquals("Diatrypella", pn.getGenusOrAbove());
    assertEquals("favacea", pn.getSpecificEpithet());
    assertEquals("var.", pn.getRankMarker());
    assertEquals("favacea", pn.getInfraSpecificEpithet());
    assertEquals("Ces. & De Not.", pn.getAuthorship());
    assertEquals("Fr.", pn.getBracketAuthorship());

    pn = parser.parse("Protoventuria rosae (De Not.) Berl. & Sacc.", null);
    assertEquals("Protoventuria", pn.getGenusOrAbove());
    assertEquals("rosae", pn.getSpecificEpithet());
    assertEquals("Berl. & Sacc.", pn.getAuthorship());
    assertEquals("De Not.", pn.getBracketAuthorship());

    pn = parser.parse("Hormospora De Not.", null);
    assertEquals("Hormospora", pn.getGenusOrAbove());
    assertEquals("De Not.", pn.getAuthorship());
  }

  @Test
  public void testCanonNameParserSubgenera() throws Exception {
    ParsedName pn = parser.parse("Polygonum subgen. Bistorta (L.) Zernov", null);
    assertTrue(pn.getNotho() == null);
    assertEquals("Polygonum", pn.getGenusOrAbove());
    assertEquals("Bistorta", pn.getInfraGeneric());
    assertEquals("subgen.", pn.getRankMarker());
    assertEquals("Zernov", pn.getAuthorship());
    assertEquals("L.", pn.getBracketAuthorship());

    ParsedName n = parser.parse("Arrhoges (Antarctohoges)", null);
    assertEquals("Arrhoges", n.getGenusOrAbove());
    assertEquals("Antarctohoges", n.getBracketAuthorship());
    assertNull(n.getInfraGeneric());
    assertNull(n.getRankMarker());
    assertNull(n.getNotho());

    n = parser.parse("Arrhoges (Antarctohoges)", Rank.SUBGENUS);
    assertEquals("Arrhoges", n.getGenusOrAbove());
    assertEquals("Antarctohoges", n.getInfraGeneric());
    assertTrue(n.getRankMarker() == Rank.SUBGENUS.getMarker());
    assertTrue(n.getNotho() == null);

    pn = parser.parse("Festuca subg. Schedonorus (P. Beauv. ) Peterm.", null);
    assertEquals("Festuca", pn.getGenusOrAbove());
    assertEquals("Schedonorus", pn.getInfraGeneric());
    assertEquals("subgen.", pn.getRankMarker());
    assertEquals("Peterm.", pn.getAuthorship());
    assertEquals("P. Beauv.", pn.getBracketAuthorship());

    n = parser.parse("Catapodium subg.Agropyropsis  Trab.", null);
    assertEquals("Catapodium", n.getGenusOrAbove());
    assertEquals("Agropyropsis", n.getInfraGeneric());
    assertEquals("subgen.", n.getRankMarker());

    n = parser.parse(" Gnaphalium subg. Laphangium Hilliard & B. L. Burtt", null);
    assertEquals("Gnaphalium", n.getGenusOrAbove());
    assertEquals("Laphangium", n.getInfraGeneric());
    assertEquals("subgen.", n.getRankMarker());

    n = parser.parse("Woodsiaceae (Hooker) Herter", null);
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
    ParsedName pn = parser.parse(name, null);
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

    pn = parseCandidatus("Candidatus Phytoplasma allocasuarinae", "Phytoplasma", "allocasuarinae", null);

    pn = parseCandidatus("Ca. Phytoplasma allocasuarinae", "Phytoplasma", "allocasuarinae", null);

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
    ParsedName pn = assertExAuthorName(parser.parse("Abies brevifolia cv. ex Dallim.", null));
    pn = assertExAuthorName(parser.parse("Abies brevifolia hort. ex Dallim.", null));
    pn = assertExAuthorName(parser.parse("Abutilon bastardioides Baker f. ex Rose", null));
    pn = assertExAuthorName(parser.parse("Acacia truncata (Burm. f.) hort. ex Hoffmanns.", null));
    pn = assertExAuthorName(parser.parse("Anoectocalyx ex Cogn. 'Triana'", null));
    pn = assertExAuthorName(parser.parse("Anoectocalyx \"Triana\" ex Cogn.  ", null));
    pn = assertExAuthorName(parser.parse("Aukuba ex Koehne 'Thunb'   ", null));
    pn = assertExAuthorName(parser.parse("Crepinella subgen. Marchal ex Oliver  ", null));
    pn = assertExAuthorName(parser.parse("Echinocereus sect. Triglochidiata ex Bravo", null));
    pn = assertExAuthorName(parser.parse("Hadrolaelia sect. Sophronitis ex Chiron & V.P.Castro", null));

    pn = assertExAuthorName(parser.parse("Abutilon ×hybridum cv. ex Voss", null));
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
    ParsedName pn = parser.parse("Abutilon 'Kentish Belle'", null);
    assertEquals("Abutilon", pn.getGenusOrAbove());
    assertEquals(NameType.CULTIVAR, pn.getType());

    pn = parser.parse("Abutilon 'Nabob'", null);
    assertEquals("Abutilon", pn.getGenusOrAbove());
    assertEquals(NameType.CULTIVAR, pn.getType());

    pn = parser.parse("Verpericola megasoma \"Dall\" Pils.", null);
    assertEquals("Verpericola", pn.getGenusOrAbove());
    assertEquals("megasoma", pn.getSpecificEpithet());
    assertEquals(NameType.CULTIVAR, pn.getType());

    pn = parser.parse("Sorbus americana Marshall cv. 'Belmonte'", null);
    assertEquals("Sorbus", pn.getGenusOrAbove());
    assertEquals("americana", pn.getSpecificEpithet());
    assertEquals(NameType.CULTIVAR, pn.getType());

    pn = parser.parse("Sorbus hupehensis C.K.Schneid. cv. 'November pink'", null);
    assertEquals("Sorbus", pn.getGenusOrAbove());
    assertEquals("hupehensis", pn.getSpecificEpithet());
    assertEquals(NameType.CULTIVAR, pn.getType());

    pn = parser.parse("Symphoricarpos albus (L.) S.F.Blake cv. 'Turesson'", null);
    assertEquals("Symphoricarpos", pn.getGenusOrAbove());
    assertEquals("albus", pn.getSpecificEpithet());
    assertEquals(NameType.CULTIVAR, pn.getType());
    assertEquals(Rank.CULTIVAR, pn.getRank());

    pn = parser.parse("Symphoricarpos sp. cv. 'mother of pearl'", null);
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

    ParsedName pn = parser.parse("Caloplaca poliotera (Nyl.) J. Steiner\\r\\n\\r\\n.", null);
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
        parser.parse("Polypodium  x vulgare nothosubsp. mantoniae (Rothm.) Schidlay ", null).fullName());
    assertHybridName(parser.parse("Arthopyrenia hyalospora x ", null));
  }

  @Test
  public void testHybridAlikeNames() throws Exception {
    ParsedName n = parser.parse("Huaiyuanella Xing, Yan & Yin, 1984", null);
    assertNull(n.getNotho());
    assertEquals("Huaiyuanella", n.getGenusOrAbove());
    assertNull(n.getSpecificEpithet());
    assertEquals("Xing, Yan & Yin", n.getAuthorship());
    assertEquals("1984", n.getYear());

    n = parser.parse("Caveasphaera Xiao & Knoll, 2000", null);
    assertNull(n.getNotho());
    assertEquals("Caveasphaera", n.getGenusOrAbove());
    assertNull(n.getSpecificEpithet());
    assertEquals("2000", n.getYear());

  }

  @Test
  public void testHybridNames() throws Exception {

    ParsedName n = parser.parse("+ Pyrocrataegus willei L.L.Daniel", null);
    assertEquals(NamePart.GENERIC, n.getNotho());
    assertEquals("Pyrocrataegus", n.getGenusOrAbove());
    assertEquals("willei", n.getSpecificEpithet());
    assertEquals("L.L.Daniel", n.getAuthorship());

    n = parser.parse("×Pyrocrataegus willei L.L.Daniel", null);
    assertEquals(NamePart.GENERIC, n.getNotho());
    assertEquals("Pyrocrataegus", n.getGenusOrAbove());
    assertEquals("willei", n.getSpecificEpithet());
    assertTrue(n.getInfraSpecificEpithet() == null);
    assertEquals("L.L.Daniel", n.getAuthorship());

    n = parser.parse(" × Pyrocrataegus willei L.L.Daniel", null);
    assertEquals(NamePart.GENERIC, n.getNotho());
    assertEquals("Pyrocrataegus", n.getGenusOrAbove());
    assertEquals("willei", n.getSpecificEpithet());
    assertTrue(n.getInfraSpecificEpithet() == null);
    assertEquals("L.L.Daniel", n.getAuthorship());

    n = parser.parse(" X Pyrocrataegus willei L.L.Daniel", null);
    assertEquals(NamePart.GENERIC, n.getNotho());
    assertEquals("Pyrocrataegus", n.getGenusOrAbove());
    assertEquals("willei", n.getSpecificEpithet());
    assertTrue(n.getInfraSpecificEpithet() == null);
    assertEquals("L.L.Daniel", n.getAuthorship());

    n = parser.parse("Pyrocrataegus ×willei L.L.Daniel", null);
    assertEquals(NamePart.SPECIFIC, n.getNotho());
    assertEquals("Pyrocrataegus", n.getGenusOrAbove());
    assertEquals("willei", n.getSpecificEpithet());
    assertTrue(n.getInfraSpecificEpithet() == null);
    assertEquals("L.L.Daniel", n.getAuthorship());

    n = parser.parse("Pyrocrataegus × willei L.L.Daniel", null);
    assertEquals(NamePart.SPECIFIC, n.getNotho());
    assertEquals("Pyrocrataegus", n.getGenusOrAbove());
    assertEquals("willei", n.getSpecificEpithet());
    assertTrue(n.getInfraSpecificEpithet() == null);
    assertEquals("L.L.Daniel", n.getAuthorship());

    n = parser.parse("Pyrocrataegus x willei L.L.Daniel", null);
    assertEquals(NamePart.SPECIFIC, n.getNotho());
    assertEquals("Pyrocrataegus", n.getGenusOrAbove());
    assertEquals("willei", n.getSpecificEpithet());
    assertTrue(n.getInfraSpecificEpithet() == null);
    assertEquals("L.L.Daniel", n.getAuthorship());

    n = parser.parse("Pyrocrataegus X willei L.L.Daniel", null);
    assertEquals(NamePart.SPECIFIC, n.getNotho());
    assertEquals("Pyrocrataegus", n.getGenusOrAbove());
    assertEquals("willei", n.getSpecificEpithet());
    assertTrue(n.getInfraSpecificEpithet() == null);
    assertEquals("L.L.Daniel", n.getAuthorship());

    n = parser.parse("Pyrocrataegus willei ×libidi L.L.Daniel", null);
    assertEquals(NamePart.INFRASPECIFIC, n.getNotho());
    assertEquals("Pyrocrataegus", n.getGenusOrAbove());
    assertEquals("willei", n.getSpecificEpithet());
    assertEquals("libidi", n.getInfraSpecificEpithet());
    assertEquals("L.L.Daniel", n.getAuthorship());

    n = parser.parse("×Pyrocrataegus ×willei ×libidi L.L.Daniel", null);
    assertEquals(NamePart.INFRASPECIFIC, n.getNotho());
    assertEquals("Pyrocrataegus", n.getGenusOrAbove());
    assertEquals("willei", n.getSpecificEpithet());
    assertEquals("libidi", n.getInfraSpecificEpithet());
    assertEquals("L.L.Daniel", n.getAuthorship());

    n = parser.parse("+ Pyrocrataegus willei ×libidi L.L.Daniel", null);
    assertEquals(NamePart.INFRASPECIFIC, n.getNotho());
    assertEquals("Pyrocrataegus", n.getGenusOrAbove());
    assertEquals("willei", n.getSpecificEpithet());
    assertEquals("libidi", n.getInfraSpecificEpithet());
    assertEquals("L.L.Daniel", n.getAuthorship());

    n = parser.parse("Pyrocrataegus willei nothosubsp. libidi L.L.Daniel", null);
    assertEquals(NamePart.INFRASPECIFIC, n.getNotho());
    assertEquals("Pyrocrataegus", n.getGenusOrAbove());
    assertEquals("willei", n.getSpecificEpithet());
    assertEquals("libidi", n.getInfraSpecificEpithet());
    assertEquals("L.L.Daniel", n.getAuthorship());

    n = parser.parse("+ Pyrocrataegus willei nothosubsp. libidi L.L.Daniel", null);
    assertEquals(NamePart.INFRASPECIFIC, n.getNotho());
    assertEquals("Pyrocrataegus", n.getGenusOrAbove());
    assertEquals("willei", n.getSpecificEpithet());
    assertEquals("libidi", n.getInfraSpecificEpithet());
    assertEquals("L.L.Daniel", n.getAuthorship());
  }

  @Test
  public void testApostropheAuthors() throws Exception {
    ParsedName pn = parser.parse("Cirsium creticum d'Urv.", null);
    assertEquals("Cirsium", pn.getGenusOrAbove());
    assertEquals("creticum", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertEquals("d'Urv.", pn.getAuthorship());

    pn = parser.parse("Cirsium creticum d'Urv. subsp. creticum", null);
    assertEquals("Cirsium", pn.getGenusOrAbove());
    assertEquals("creticum", pn.getSpecificEpithet());
    assertEquals("creticum", pn.getInfraSpecificEpithet());
    assertEquals(Rank.SUBSPECIES, pn.getRank());
    assertNull(pn.getAuthorship());
  }

  @Test
  public void testExtinctNames() throws Exception {
    ParsedName pn = parser.parse("†Titanoptera", null);
    assertEquals("Titanoptera", pn.getGenusOrAbove());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getRank());
    assertNull(pn.getAuthorship());

    pn = parser.parse("† Tuarangiida MacKinnon, 1982", null);
    assertEquals("Tuarangiida", pn.getGenusOrAbove());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getRank());
    assertEquals("MacKinnon", pn.getAuthorship());
    assertEquals("1982", pn.getYear());
  }

  @Test
  public void testApostropheEpithets() throws Exception {
    ParsedName pn = parser.parse("Junellia o'donelli Moldenke, 1946", null);
    assertEquals("Junellia", pn.getGenusOrAbove());
    assertEquals("o'donelli", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertEquals("Moldenke", pn.getAuthorship());
    assertEquals("1946", pn.getYear());

    pn = parser.parse("Trophon d'orbignyi Carcelles, 1946", null);
    assertEquals("Trophon", pn.getGenusOrAbove());
    assertEquals("d'orbignyi", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertEquals("Carcelles", pn.getAuthorship());
    assertEquals("1946", pn.getYear());

    pn = parser.parse("Arca m'coyi Tenison-Woods, 1878", null);
    assertEquals("Arca", pn.getGenusOrAbove());
    assertEquals("m'coyi", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertEquals("Tenison-Woods", pn.getAuthorship());
    assertEquals("1878", pn.getYear());

    pn = parser.parse("Nucula m'andrewii Hanley, 1860", null);
    assertEquals("Nucula", pn.getGenusOrAbove());
    assertEquals("m'andrewii", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertEquals("Hanley", pn.getAuthorship());
    assertEquals("1860", pn.getYear());

    pn = parser.parse("Eristalis l'herminierii Macquart", null);
    assertEquals("Eristalis", pn.getGenusOrAbove());
    assertEquals("l'herminierii", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertEquals("Macquart", pn.getAuthorship());

    pn = parser.parse("Odynerus o'neili Cameron", null);
    assertEquals("Odynerus", pn.getGenusOrAbove());
    assertEquals("o'neili", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertEquals("Cameron", pn.getAuthorship());

    pn = parser.parse("Serjania meridionalis Cambess. var. o'donelli F.A. Barkley", null);
    assertEquals("Serjania", pn.getGenusOrAbove());
    assertEquals("meridionalis", pn.getSpecificEpithet());
    assertEquals("o'donelli", pn.getInfraSpecificEpithet());
    assertEquals(Rank.VARIETY, pn.getRank());
    assertEquals("F.A. Barkley", pn.getAuthorship());
  }

  @Test
  public void testVirusNames() throws Exception {
    ParsedName pn = parser.parse("Crassatellites janus Hedley, 1906", null);
    assertEquals("Crassatellites", pn.getGenusOrAbove());
    assertEquals("janus", pn.getSpecificEpithet());
    assertEquals("Hedley", pn.getAuthorship());
    assertEquals("1906", pn.getYear());

    pn = parser.parse("Ypsolophus satellitella", null);
    assertEquals("Ypsolophus", pn.getGenusOrAbove());
    assertEquals("satellitella", pn.getSpecificEpithet());

    pn = parser.parse("Nephodia satellites", null);
    assertEquals("Nephodia", pn.getGenusOrAbove());
    assertEquals("satellites", pn.getSpecificEpithet());

    Reader reader = FileUtils.getInputStreamReader(streamUtils.classpathStream("viruses.txt"));
    LineIterator iter = new LineIterator(reader);
    while (iter.hasNext()) {
      String line = iter.nextLine();
      if (line == null || line.startsWith("#") || line.trim().isEmpty()) {
        continue;
      }
      assertUnparsableType(NameType.VIRUS, line);
    }
  }

  /**
   * http://dev.gbif.org/issues/browse/POR-2459
   */
  @Test
  public void testEbiUncultured() throws Exception {
    assertUnparsableType(NameType.PLACEHOLDER, "uncultured Verrucomicrobiales bacterium");
    assertUnparsableType(NameType.PLACEHOLDER, "uncultured Verrucomicrobium sp.");
    assertUnparsableType(NameType.PLACEHOLDER, "uncultured Verrucomicrobia bacterium");
    assertUnparsableType(NameType.PLACEHOLDER, "uncultured Vibrio sp.");
    assertUnparsableType(NameType.PLACEHOLDER, "uncultured Wilcoxina");
    assertUnparsableType(NameType.PLACEHOLDER, "uncultured zygomycete");
    assertUnparsableType(NameType.PLACEHOLDER, "uncultured Yonghaparkia sp");
    assertUnparsableType(NameType.PLACEHOLDER, "uncultured virus");
    // other placeholders e.g from ITIS:
    assertUnparsableType(NameType.PLACEHOLDER, "Temp dummy name");

  }

  @Test
  public void testRNANames() throws Exception {
    ParsedName pn = parser.parse("Calathus (Lindrothius) KURNAKOV 1961", null);
    assertEquals("Calathus", pn.getGenusOrAbove());
    assertEquals("Lindrothius", pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertEquals("Kurnakov", pn.getAuthorship());
    assertEquals("1961", pn.getYear());

    assertUnparsableType(NameType.VIRUS, "Cactus virus 2");
    assertUnparsableType(NameType.VIRUS, "Ustilaginoidea virens RNA virus");
    assertUnparsableType(NameType.VIRUS, "Rhizoctonia solani dsRNA virus 2");

    pn = parser.parse("Candida albicans RNA_CTR0-3", null);
    assertEquals("Candida", pn.getGenusOrAbove());
    assertEquals("albicans", pn.getSpecificEpithet());
    assertEquals(NameType.INFORMAL, pn.getType());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getAuthorship());

    pn = parser.parse("Alpha proteobacterium RNA12", null);
    assertEquals("Alpha", pn.getGenusOrAbove());
    assertEquals("proteobacterium", pn.getSpecificEpithet());
    assertEquals(NameType.INFORMAL, pn.getType());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getAuthorship());

    pn = parser.parse("Armillaria ostoyae RNA1", null);
    assertEquals("Armillaria", pn.getGenusOrAbove());
    assertEquals("ostoyae", pn.getSpecificEpithet());
    assertEquals(NameType.INFORMAL, pn.getType());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getAuthorship());

    assertUnparsableType(NameType.DOUBTFUL, "siRNA");
  }

  @Test
  public void testIndetParsering() throws Exception {
    ParsedName pn = parser.parse("Polygonum spec.", null);
    assertEquals("Polygonum", pn.getGenusOrAbove());
    assertNull(pn.getSpecificEpithet());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertEquals(NameType.INFORMAL, pn.getType());

    pn = parser.parse("Polygonum vulgaris ssp.", null);
    assertEquals("Polygonum", pn.getGenusOrAbove());
    assertEquals("vulgaris", pn.getSpecificEpithet());
    assertEquals(Rank.SUBSPECIES, pn.getRank());
    assertEquals(NameType.INFORMAL, pn.getType());

    pn = parser.parse("Mesocricetus sp.", null);
    assertEquals("Mesocricetus", pn.getGenusOrAbove());
    assertNull(pn.getSpecificEpithet());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertEquals(NameType.INFORMAL, pn.getType());

    // also treat these bacterial names as indets
    pn = parser.parse("Bartonella sp. RN, 10623LA", null);
    assertEquals("Bartonella", pn.getGenusOrAbove());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertEquals(NameType.INFORMAL, pn.getType());

    // and dont treat these authorships as forms
    pn = parser.parse("Dioscoreales Hooker f.", null);
    assertEquals("Dioscoreales", pn.getGenusOrAbove());
    assertEquals("Hooker f.", pn.getAuthorship());
    assertEquals(Rank.ORDER, pn.getRank());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Lepidoptera sp. JGP0404", null);
    assertEquals("Lepidoptera", pn.getGenusOrAbove());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getAuthorship());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertEquals(NameType.INFORMAL, pn.getType());

    pn = parser.parse("Melastoma vacillans Blume var.", null);

    pn = parser.parse("Lepidoptera Hooker", Rank.SPECIES);
    assertEquals("Lepidoptera", pn.getGenusOrAbove());
    assertNull(pn.getSpecificEpithet());
    assertEquals("Hooker", pn.getAuthorship());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertEquals(NameType.INFORMAL, pn.getType());

    pn = parser.parse("Lepidoptera alba DC.", Rank.SUBSPECIES);
    assertEquals("Lepidoptera", pn.getGenusOrAbove());
    assertEquals("alba", pn.getSpecificEpithet());
    assertEquals("DC.", pn.getAuthorship());
    assertEquals(Rank.SUBSPECIES, pn.getRank());
    assertEquals(NameType.INFORMAL, pn.getType());

    pn = parser.parse("Lepidoptera alba DC.", Rank.SUBSPECIES);
    assertEquals("Lepidoptera", pn.getGenusOrAbove());
    assertEquals("alba", pn.getSpecificEpithet());
    assertEquals("DC.", pn.getAuthorship());
    assertEquals(Rank.SUBSPECIES, pn.getRank());
    assertEquals(NameType.INFORMAL, pn.getType());
  }


  @Test
  public void testRankMismatch() throws Exception {
    ParsedName pn = parser.parse("Polygonum", Rank.SUBGENUS);
    assertEquals("Polygonum", pn.getGenusOrAbove());
    assertNull(pn.getSpecificEpithet());
    assertEquals(Rank.SUBGENUS, pn.getRank());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Polygonum", Rank.SUBSPECIES);
    assertEquals("Polygonum", pn.getGenusOrAbove());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getAuthorship());
    assertEquals(Rank.SUBSPECIES, pn.getRank());
    assertEquals(NameType.INFORMAL, pn.getType());

    pn = parser.parse("Polygonum alba", Rank.GENUS);
    assertEquals("Polygonum", pn.getGenusOrAbove());
    assertEquals("alba", pn.getSpecificEpithet());
    assertNull(pn.getAuthorship());
    assertEquals(Rank.GENUS, pn.getRank());
    assertEquals(NameType.DOUBTFUL, pn.getType());

    pn = parser.parse("Polygonum", Rank.CULTIVAR);
    assertEquals("Polygonum", pn.getGenusOrAbove());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getAuthorship());
    assertEquals(Rank.CULTIVAR, pn.getRank());
    assertEquals(NameType.INFORMAL, pn.getType());
  }

  @Test
  public void testTimeoutNameFile() throws Exception {
    Reader reader = FileUtils.getInputStreamReader(streamUtils.classpathStream("timeout-names.txt"));
    LineIterator iter = new LineIterator(reader);
    while (iter.hasNext()) {
      String line = iter.nextLine();
      if (line == null || line.startsWith("#") || line.trim().isEmpty()) {
        continue;
      }
      ParsedName n = parser.parse(line, null);
    }
  }

  @Test
  public void testNameFile() throws Exception {
    final int CURRENTLY_FAIL = 24;
    final int CURRENTLY_FAIL_EXCL_AUTHORS = 19;

    LOG.info("\n\nSTARTING FULL PARSER\n");
    Reader reader = FileUtils.getInputStreamReader(streamUtils.classpathStream("scientific_names.txt"));
    LineIterator iter = new LineIterator(reader);

    int parseFails = 0;
    int parseNoAuthors = 0;
    int parseErrors = 0;
    int cnt = 0;
    int lineNum = 0;
    long start = System.currentTimeMillis();

    Splitter tabSplit = Splitter.on('\t');
    while (iter.hasNext()) {
      lineNum++;
      String line = iter.nextLine();
      if (lineNum == 1 || line == null || line.startsWith("#") || line.trim().isEmpty()) {
        continue;
      }
      cnt++;

      List<String> cols = tabSplit.splitToList(line);
      String name = cols.get(0);
      if (cols.size() < 14) {
        LOG.warn("Too short line in names test file:");
        LOG.warn(line);
      }
      ParsedName expected = buildParsedNameFromTabRow(cols.toArray(new String[cols.size()]));
      ParsedName n;
      try {
        n = parser.parse(name, null);
        if (!n.isAuthorsParsed()) {
          parseNoAuthors++;
        }
        // copy scientific name, we dont wanna compare it, it might be slightly different
        expected.setScientificName(n.getScientificName());
        // remove SCIENTIFIC nametype as we only like to compare other values
        if (NameType.SCIENTIFIC == n.getType()) {
          n.setType(null);
        }
        if (!n.equals(expected)) {
          parseErrors++;
          LOG.warn("WRONG\t " + name + "\n  EXPECTED: " + expected + "\n  PARSED  : " + n);
        }
      } catch (Exception e) {
        parseFails++;
        LOG.warn("FAIL\t " + name + "\nEXPECTED: " + expected);
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

    assertEquals("Abelia grandifolia", parser.parseToCanonical("Abelia grandifolia Villarreal, 2000 Villarreal, 2000", null));
    assertEquals("Abelia macrotera",
        parser.parseToCanonical("Abelia macrotera (Graebn. & Buchw.) Rehder (Graebn. et Buchw.) Rehder", null));
    assertEquals("Abelia mexicana", parser.parseToCanonical("Abelia mexicana Villarreal, 2000 Villarreal, 2000", null));
    assertEquals("Abelia serrata", parser.parseToCanonical("Abelia serrata Abelia serrata Siebold & Zucc.", null));
    assertEquals("Abelia spathulata colorata", parser.parseToCanonical(
        "Abelia spathulata colorata (Hara & Kurosawa) Hara & Kurosawa (Hara et Kurosawa) Hara et Kurosawa", null));
    assertEquals("Abelia tetrasepala",
        parser.parseToCanonical("Abelia tetrasepala (Koidz.) H.Hara & S.Kuros. (Koidz.) H.Hara et S.Kuros.", null));
    assertEquals("Abelia tetrasepala",
        parser.parseToCanonical("Abelia tetrasepala (Koidz.) Hara & Kurosawa (Koidz.) Hara et Kurosawa", null));
    assertEquals("Abelmoschus esculentus",
        parser.parseToCanonical("Abelmoschus esculentus Abelmoschus esculentus (L.) Moench", null));
    assertEquals("Abelmoschus moschatus",
        parser.parseToCanonical("Abelmoschus moschatus Abelmoschus moschatus Medik.", null));
    assertEquals("Abelmoschus moschatus tuberosus",
        parser.parseToCanonical("Abelmoschus moschatus subsp. tuberosus (Span.) Borss.Waalk.", null));
    assertEquals("Abia fasciata", parser.parseToCanonical("Abia fasciata (Linnaeus, 1758) (Linnaeus, 1758)", null));
    assertEquals("Abia fasciata", parser.parseToCanonical("Abia fasciata Linnaeus, 1758 Linnaeus, 1758", null));
    assertEquals("Abida secale", parser.parseToCanonical("Abida secale (Draparnaud, 1801) (Draparnaud) , 1801", null));
    assertEquals("Abida secale brauniopsis", parser.parseToCanonical("Abida secale brauniopsis Bofill i Poch, Artur", null));
    assertEquals("Abida secale inis", parser.parseToCanonical("Abida secale inis Bofill i Poch, Artur", null));
    assertEquals("Abida secale meridionalis",
        parser.parseToCanonical("Abida secale meridionalis Martínez Ortí, AlbertoGómez, BenjamínFaci, Guill", null));
    assertEquals("Abies alba", parser.parseToCanonical("Abies alba Abies alba Mill.", null));
    assertEquals("Abies alba", parser.parseToCanonical("Abies alba Abies alba Miller", null));
    assertEquals("Abies alba", parser.parseToCanonical("Abies alba Mill. (species) : Mill.", null));
    assertEquals("Abies amabilis",
        parser.parseToCanonical("Abies amabilis Abies amabilis (Dougl. ex Loud.) Dougl. ex Forbes", null));
    assertEquals("Abies amabilis", parser.parseToCanonical("Abies amabilis Abies amabilis Douglas ex Forbes", null));
    assertEquals("Abies arizonica", parser.parseToCanonical("Abies arizonica Abies arizonica Merriam", null));
    assertEquals("Abies balsamea", parser.parseToCanonical("Abies balsamea (L.) Mill. (species) : (L.) Mill.", null));
    assertEquals("Abies balsamea", parser.parseToCanonical("Abies balsamea (L.) Mill. / Ledeb.", null));
    assertEquals("Abies balsamea", parser.parseToCanonical("Abies balsamea Abies balsamea (L.) Mill.", null));
    assertEquals("Abelia grandiflora", parser.parseToCanonical("Abelia grandiflora Rehd (species) : Rehd", null));
  }

  /**
   * http://dev.gbif.org/issues/browse/POR-101
   */
  @Test
  public void testAuthorTroubles() throws Exception {
    assertParsedParts("Cribbia pendula la Croix & P.J.Cribb", NameType.SCIENTIFIC, "Cribbia", "pendula", null, null, "la Croix & P.J.Cribb", null);
    assertParsedParts("Cribbia pendula le Croix & P.J.Cribb", NameType.SCIENTIFIC, "Cribbia", "pendula", null, null, "le Croix & P.J.Cribb", null);
    assertParsedParts("Cribbia pendula de la Croix & le P.J.Cribb", NameType.SCIENTIFIC, "Cribbia", "pendula", null, null, "de la Croix & le P.J.Cribb", null);
    assertParsedParts("Cribbia pendula Croix & de le P.J.Cribb", NameType.SCIENTIFIC, "Cribbia", "pendula", null, null, "Croix & de le P.J.Cribb", null);
  }

  /**
   * http://dev.gbif.org/issues/browse/POR-159
   */
  @Test
  public void testMicrobialRanks() throws Exception {
    assertParsedMicrobial("Salmonella enterica serovar Typhimurium",
        NameType.SCIENTIFIC, "Salmonella", "enterica", "Typhimurium", Rank.SEROVAR);
    assertParsedMicrobial("Salmonella enterica serovar Dublin",
        NameType.SCIENTIFIC, "Salmonella", "enterica", "Dublin", Rank.SEROVAR);

    assertParsedMicrobial("Yersinia pestis biovar Orientalis str. IP674",
        NameType.INFORMAL, "Yersinia", "pestis", "Orientalis", Rank.BIOVAR, null, null, null, null, "IP674");
    assertParsedMicrobial("Rhizobium leguminosarum biovar viciae",
        NameType.SCIENTIFIC, "Rhizobium", "leguminosarum", "viciae", Rank.BIOVAR);

    assertParsedMicrobial("Thymus vulgaris ct. thymol",
        NameType.SCIENTIFIC, "Thymus", "vulgaris", "thymol", Rank.CHEMOFORM);

    assertParsedMicrobial("Staphyloccocus aureus phagovar 42D",
        NameType.SCIENTIFIC, "Staphyloccocus", "aureus", "42D", Rank.PHAGOVAR);

    assertParsedMicrobial("Pseudomonas syringae pv. lachrymans",
        NameType.SCIENTIFIC, "Pseudomonas", "syringae", "lachrymans", Rank.PATHOVAR);

    assertParsedMicrobial("Pseudomonas syringae pv. aceris (Ark, 1939) Young, Dye & Wilkie, 1978",
        NameType.SCIENTIFIC, "Pseudomonas", "syringae", "aceris", Rank.PATHOVAR, "Young, Dye & Wilkie", "1978", "Ark", "1939", null);

//    assertParsedMicrobial("Acinetobacter junii morphovar I",
//        NameType.SCIENTIFIC, "Acinetobacter", "junii", null, Rank.MORPHOVAR, "I", null, null, null, null);

    assertParsedMicrobial("Puccinia graminis f. sp. avenae",
        NameType.SCIENTIFIC, "Puccinia", "graminis", "avenae", Rank.FORMA_SPECIALIS);

    assertParsedMicrobial("Puccinia graminis f.sp. avenae",
        NameType.SCIENTIFIC, "Puccinia", "graminis", "avenae", Rank.FORMA_SPECIALIS);

    assertParsedMicrobial("Bacillus thuringiensis serovar huazhongensis",
        NameType.SCIENTIFIC, "Bacillus", "thuringiensis", "huazhongensis", Rank.SEROVAR);

    // informal indet name
    assertParsedMicrobial("Bacillus thuringiensis serovar",
        NameType.INFORMAL, "Bacillus", "thuringiensis", null, Rank.SEROVAR);

    // informal indet name
    assertParsedMicrobial("Bacillus serovar",
        NameType.INFORMAL, "Bacillus", null, null, Rank.SEROVAR);

    assertParsedMicrobial("Listeria monocytogenes serovar 4b",
        NameType.SCIENTIFIC, "Listeria", "monocytogenes", "4b", Rank.SEROVAR);

    assertParsedMicrobial("Listeria monocytogenes serotype 4b",
        NameType.SCIENTIFIC, "Listeria", "monocytogenes", "4b", Rank.SEROVAR);

    // make sure real names get properly parsed still!
    assertParsedSpecies("Calathus bolivar Negre", NameType.SCIENTIFIC, "Calathus", "bolivar", "Negre", null);
    assertParsedSpecies("Eublemma debivar Berio, 1945", NameType.SCIENTIFIC, "Eublemma", "debivar", "Berio", 1945);
    assertParsedSpecies("Ceryx evar Pagenstecher, 1886", NameType.SCIENTIFIC, "Ceryx", "evar", "Pagenstecher", 1886);

    assertParsedSpecies("Klyxum equisetiform (Luttschwager, 1922)", NameType.SCIENTIFIC, "Klyxum", "equisetiform", null, null, "Luttschwager", 1922);
    assertParsedSpecies("Dendronthema grandiform", NameType.SCIENTIFIC, "Dendronthema", "grandiform", null, null);
    assertParsedSpecies("Bryconamericus subtilisform Román-Valencia, 2003", NameType.SCIENTIFIC, "Bryconamericus", "subtilisform", "Román-Valencia", 2003);
    assertParsedSpecies("Crisia bucinaform", NameType.SCIENTIFIC, "Crisia", "bucinaform", null, null);

    assertParsedSpecies("Melitoxestis centrotype Janse, 1958", NameType.SCIENTIFIC, "Melitoxestis", "centrotype", "Janse", 1958);
    assertParsedSpecies("Sanys coenotype Hampson, 1926", NameType.SCIENTIFIC, "Sanys", "coenotype", "Hampson", 1926);
    assertParsedSpecies("Ethmia phricotype Bradley, 1965", NameType.SCIENTIFIC, "Ethmia", "phricotype", "Bradley", 1965);
    assertParsedSpecies("Pseudoceros maximus-type A Lang, 1884", NameType.SCIENTIFIC, "Pseudoceros", "maximus-type", "A Lang", 1884);
    assertParsedSpecies("Egnasia microtype Hampson, 1926", NameType.SCIENTIFIC, "Egnasia", "microtype", "Hampson", 1926);
    assertParsedSpecies("Oenochroa zalotype Turner, 1935", NameType.SCIENTIFIC, "Oenochroa", "zalotype", "Turner", 1935);

    assertParsedSpecies("Leptura vibex Horn, 1885", NameType.SCIENTIFIC, "Leptura", "vibex", "Horn", 1885);
    assertParsedSpecies("Hylesia tapabex Dyar, 1913", NameType.SCIENTIFIC, "Hylesia", "tapabex", "Dyar", 1913);
    assertParsedSpecies("Myristica mediovibex W.J.de Wilde", NameType.SCIENTIFIC, "Myristica", "mediovibex", "W.J.de Wilde", null);
    assertParsedSpecies("Peperomia obex Trel.", NameType.SCIENTIFIC, "Peperomia", "obex", "Trel.", null);
    assertParsedInfraspecies("Capra ibex graicus Matschie, 1912", NameType.SCIENTIFIC, "Capra", "ibex", "graicus", "Matschie", 1912);
    assertParsedSpecies("Neogastromyzon crassiobex Tan, 2006", NameType.SCIENTIFIC, "Neogastromyzon", "crassiobex", "Tan", 2006);

  }

  @Test
  @Ignore("strains not yet well parsed, see http://dev.gbif.org/issues/browse/POR-2699")
  public void testBacteriaStrains() throws Exception {
    assertParsedMicrobial("Listeria monocytogenes serotype 4b str. F, 2365", NameType.INFORMAL, "Listeria",
        "monocytogenes", "4b", Rank.SEROVAR, "F, 2365");
  }

  @Test
  public void testAutonyms() throws Exception {
    assertParsedParts("Panthera leo leo (Linnaeus, 1758)", NameType.SCIENTIFIC, "Panthera", "leo", "leo", null, null, null, "Linnaeus",
        "1758");
    assertParsedParts("Abies alba subsp. alba L.", NameType.SCIENTIFIC, "Abies", "alba", "alba", "subsp.", "L.");
    //TODO: improve nameparser to extract autonym authors, http://dev.gbif.org/issues/browse/GBIFCOM-10
    //    assertParsedParts("Abies alba L. subsp. alba", "Abies", "alba", "alba", "subsp.", "L.");
    // this is a wrong name! autonym authors are the species authors, so if both are given they must be the same!
    //    assertParsedParts("Abies alba L. subsp. alba Mill.", "Abies", "alba", "alba", "subsp.", "L.");
  }

  @Test
  public void testNameParserFull() throws Exception {
    assertParsedParts("Abies alba L.", NameType.SCIENTIFIC, "Abies", "alba", null, null, "L.");
    assertParsedParts("Abies alba var. kosovo", "Abies", "alba", "kosovo", "var.");
    assertParsedParts("Abies alba subsp. parafil", "Abies", "alba", "parafil", "subsp.");
    assertParsedParts("Abies   alba L. ssp. parafil DC.", "Abies", "alba", "parafil", "subsp.", "DC.");

    assertParsedParts("Nuculoidea behrens var.christoph Williams & Breger [1916]  ", NameType.DOUBTFUL, "Nuculoidea", "behrens",
        "christoph", "var.", "Williams & Breger", "1916");
    assertParsedParts(" Nuculoidea Williams et  Breger 1916  ", NameType.SCIENTIFIC, "Nuculoidea", null, null, null, "Williams & Breger", "1916");

    assertParsedParts("Nuculoidea behrens v.christoph Williams & Breger [1916]  ", NameType.DOUBTFUL, "Nuculoidea", "behrens", "christoph",
        "var.", "Williams & Breger", "1916");
    assertParsedParts("Nuculoidea behrens var.christoph Williams & Breger [1916]  ", NameType.DOUBTFUL, "Nuculoidea", "behrens",
        "christoph", "var.", "Williams & Breger", "1916");
    assertParsedParts(" Megacardita hornii  calafia", "Megacardita", "hornii", "calafia", null);
    assertParsedParts(" Megacardita hornii  calafia", "Megacardita", "hornii", "calafia", null);
    assertParsedParts(" A. anthophora acervorum", "A.", "anthophora", "acervorum", null);
    assertParsedParts(" x Festulolium nilssonii Cugnac & A. Camus", null, NameType.SCIENTIFIC, "Festulolium", null, "nilssonii", null, null,
        NamePart.GENERIC, "Cugnac & A. Camus", null, null, null, null, null);
    assertParsedParts("x Festulolium nilssonii Cugnac & A. Camus", null, NameType.SCIENTIFIC, "Festulolium", null, "nilssonii", null, null,
        NamePart.GENERIC, "Cugnac & A. Camus", null, null, null, null, null);
    assertParsedParts("Festulolium x nilssonii Cugnac & A. Camus", null, NameType.SCIENTIFIC, "Festulolium", null, "nilssonii", null, null,
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
    assertParsedParts(" Photina Cardioptera burmeisteri (Westwood 1889)", null, "Photina", "Cardioptera", "burmeisteri", null, null, null,
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
    assertParsedParts(" Tortrix Heterognomon aglossana Kennel 1899", NameType.SCIENTIFIC, "Tortrix", "Heterognomon", "aglossana", null, null, "Kennel",
        "1899", null, null);
    assertParsedParts(" Leptochilus (Neoleptochilus) beaumonti Giordani Soika 1953", NameType.SCIENTIFIC, "Leptochilus", "Neoleptochilus", "beaumonti", null,
        null, "Giordani Soika", "1953", null, null);
    assertParsedParts(" Lutzomyia (Helcocyrtomyia) rispaili Torres-Espejo, Caceres & le Pont 1995", NameType.SCIENTIFIC, "Lutzomyia", "Helcocyrtomyia",
        "rispaili", null, null, "Torres-Espejo, Caceres & le Pont", "1995", null, null);
    assertParsedParts("Gastropacha minima De Lajonquiére 1979", "Gastropacha", "minima", null, null, "De Lajonquiére",
        "1979");
    assertParsedParts("Lithobius elongipes Chamberlin (1952)", null, "Lithobius", "elongipes", null, null, null, null,
        "Chamberlin", "1952");
    assertParsedInfrageneric("Maxillaria sect. Multiflorae Christenson", null, "Maxillaria", "Multiflorae", "sect.", "Christenson", null, null, null);

    assertParsedParts("Maxillaria allenii L.O.Williams in Woodson & Schery", "Maxillaria", "allenii", null, null,
        "L.O.Williams in Woodson & Schery");
    assertParsedParts("Masdevallia strumosa P.Ortiz & E.Calderón", "Masdevallia", "strumosa", null, null,
        "P.Ortiz & E.Calderón");
    assertParsedParts("Neobisium (Neobisium) carcinoides balcanicum Hadži 1937", NameType.SCIENTIFIC, "Neobisium", "Neobisium",
        "carcinoides", "balcanicum", null, "Hadži", "1937", null, null);
    assertParsedParts("Nomascus concolor subsp. lu Delacour, 1951", "Nomascus", "concolor", "lu", "subsp.", "Delacour",
        "1951");
    assertParsedParts("Polygonum subgen. Bistorta (L.) Zernov", NameType.SCIENTIFIC, "Polygonum", "Bistorta",
        null, null, "subgen.", "Zernov", null, "L.", null);
    assertParsedParts("Stagonospora polyspora M.T. Lucas & Sousa da Câmara, 1934", "Stagonospora", "polyspora", null,
        null, "M.T. Lucas & Sousa da Câmara", "1934");

    assertParsedParts("Euphorbiaceae de Jussieu, 1789", "Euphorbiaceae", null, null, null, "de Jussieu", "1789");
    assertParsedParts("Leucanitis roda Herrich-Schäffer (1851) 1845", null, "Leucanitis", "roda", null, null, null, "1845",
        "Herrich-Schäffer", "1851");

    ParsedName pn = parser.parse("Loranthus incanus Schumach. & Thonn. subsp. sessilis Sprague", null);
    assertEquals("Loranthus", pn.getGenusOrAbove());
    assertEquals("incanus", pn.getSpecificEpithet());
    assertEquals("sessilis", pn.getInfraSpecificEpithet());
    assertEquals("Sprague", pn.getAuthorship());

    pn = parser.parse("Mascagnia brevifolia  var. paniculata Nied.,", null);
    assertEquals("Mascagnia", pn.getGenusOrAbove());
    assertEquals("brevifolia", pn.getSpecificEpithet());
    assertEquals("paniculata", pn.getInfraSpecificEpithet());
    assertEquals("Nied.", pn.getAuthorship());
    assertEquals(Rank.VARIETY, pn.getRank());

    pn = parser.parse("Leveillula jaczewskii U. Braun (ined.)", null);
    assertEquals("Leveillula", pn.getGenusOrAbove());
    assertEquals("jaczewskii", pn.getSpecificEpithet());
    assertEquals("U. Braun", pn.getAuthorship());
    assertEquals(Rank.SPECIES, pn.getRank());

    pn = parser.parse("Heteropterys leschenaultiana fo. ovata Nied.", null);
    assertEquals("Heteropterys", pn.getGenusOrAbove());
    assertEquals("leschenaultiana", pn.getSpecificEpithet());
    assertEquals("ovata", pn.getInfraSpecificEpithet());
    assertEquals("Nied.", pn.getAuthorship());
    assertEquals(Rank.FORM, pn.getRank());

    pn = parser.parse("Cymbella mendosa f. apiculata (Hust.) VanLand.", null);
    assertEquals("Cymbella", pn.getGenusOrAbove());
    assertEquals("mendosa", pn.getSpecificEpithet());
    assertEquals("apiculata", pn.getInfraSpecificEpithet());
    assertEquals("VanLand.", pn.getAuthorship());
    assertEquals(Rank.FORM, pn.getRank());

    pn = parser.parse("Lasioglossum channelense McGinley, 1986", null);
    assertEquals("Lasioglossum", pn.getGenusOrAbove());
    assertEquals("channelense", pn.getSpecificEpithet());
    assertEquals("McGinley", pn.getAuthorship());
    assertEquals("1986", pn.getYear());
    assertEquals(Rank.SPECIES, pn.getRank());

    pn = parser.parse("Liolaemus hermannunezi PINCHEIRA-DONOSO, SCOLARO & SCHULTE 2007", null);
    assertEquals("Liolaemus", pn.getGenusOrAbove());
    assertEquals("hermannunezi", pn.getSpecificEpithet());
    assertEquals("Pincheira-Donoso, Scolaro & Schulte", pn.getAuthorship());
    assertEquals("2007", pn.getYear());
    assertEquals(Rank.SPECIES, pn.getRank());

    pn = parser.parse("Liolaemus hermannunezi Pincheira-Donoso, Scolaro & Schulte, 2007", null);
    assertEquals("Liolaemus", pn.getGenusOrAbove());
    assertEquals("hermannunezi", pn.getSpecificEpithet());
    assertEquals("Pincheira-Donoso, Scolaro & Schulte", pn.getAuthorship());
    assertEquals("2007", pn.getYear());
    assertEquals(Rank.SPECIES, pn.getRank());

    pn = parser.parse("Pseudoeryx relictualis SCHARGEL, RIVAS-FUENMAYOR, BARROS & P.FAUR 2007", null);
    assertEquals("Pseudoeryx", pn.getGenusOrAbove());
    assertEquals("relictualis", pn.getSpecificEpithet());
    assertEquals("Schargel, Rivas-Fuenmayor, Barros & P.Faur", pn.getAuthorship());
    assertEquals("2007", pn.getYear());
    assertEquals(Rank.SPECIES, pn.getRank());

    pn = parser.parse("Cyrtodactylus phongnhakebangensis ZIEGLER, RÖSLER, HERRMANN & THANH 2003", null);
    assertEquals("Cyrtodactylus", pn.getGenusOrAbove());
    assertEquals("phongnhakebangensis", pn.getSpecificEpithet());
    assertEquals("Ziegler, Rösler, Herrmann & Thanh", pn.getAuthorship());
    assertEquals("2003", pn.getYear());
    assertEquals(Rank.SPECIES, pn.getRank());

    pn = parser.parse(
        "Cnemidophorus mumbuca Colli, Caldwell, Costa, Gainsbury, Garda, Mesquita, Filho, Soares, Silva, Valdujo, Vieira, Vitt, Wer", null);
    assertEquals("Cnemidophorus", pn.getGenusOrAbove());
    assertEquals("mumbuca", pn.getSpecificEpithet());
    assertEquals("Colli, Caldwell, Costa, Gainsbury, Garda, Mesquita, Filho, Soares, Silva, Valdujo, Vieira, Vitt, Wer",
        pn.getAuthorship());
    assertEquals(Rank.SPECIES, pn.getRank());

    pn = parser.parse(
        "Cnemidophorus mumbuca COLLI, CALDWELL, COSTA, GAINSBURY, GARDA, MESQUITA, FILHO, SOARES, SILVA, VALDUJO, VIEIRA, VITT, WER", null);
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
    assertParsedParts("Aecidium berberidis Pers. ex J.F. Gmel.", null, "Aecidium", "berberidis", null, null, "Pers. ex J.F. Gmel.", null, null, null);
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
    assertParsedParts("Xanthomonas campestris pv. citri (ex Hasse 1915) Dye 1978", NameType.SCIENTIFIC, "Xanthomonas", "campestris", "citri", "pv.", "Dye", "1978", "ex Hasse", "1915");
    assertParsedParts("Xanthomonas campestris pv. oryzae (Xco)", NameType.SCIENTIFIC, "Xanthomonas", "campestris", "oryzae", "pv.", null, null, "Xco", null);
    assertParsedParts("Streptococcus dysgalactiae (ex Diernhofer 1932) Garvie et al. 1983", NameType.SCIENTIFIC, "Streptococcus", "dysgalactiae", null, null, "Garvie et al.", "1983", "ex Diernhofer", "1932");
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
    ParsedName pn = parser.parse("Abacetus laevicollis de Chaudoir, 1869", null);
    assertEquals("Abacetus laevicollis", pn.canonicalNameWithMarker());
    assertEquals("de Chaudoir", pn.getAuthorship());
    assertEquals("1869", pn.getYear());
    assertTrue(1869 == pn.getYearInt());
    // issue50
    pn = parser.parse("Deinococcus-Thermus", null);
    assertEquals("Deinococcus-Thermus", pn.canonicalNameWithMarker());
    assertTrue(pn.getAuthorship() == null);
    assertTrue(pn.getYear() == null);
    // issue51
    pn = parser.parse("Alectis alexandrinus (Geoffroy St. Hilaire, 1817)", null);
    assertEquals("Alectis alexandrinus", pn.canonicalNameWithMarker());
    assertEquals("Geoffroy St. Hilaire", pn.getBracketAuthorship());
    assertEquals("1817", pn.getBracketYear());
    assertTrue(1817 == pn.getBracketYearInt());
    // issue60
    pn = parser.parse("Euphorbiaceae de Jussieu, 1789", null);
    assertEquals("Euphorbiaceae", pn.canonicalName());
    assertEquals("Euphorbiaceae", pn.getGenusOrAbove());
    assertEquals("de Jussieu", pn.getAuthorship());
    assertEquals("1789", pn.getYear());
  }

  @Test
  @Ignore
  public void testElmisSp() throws Exception {
    ParsedName pn = parser.parse("Elmis sp. Lv.", null);
    assertEquals("Elmis", pn.getGenusOrAbove());
    assertEquals("Lv.", pn.getAuthorship());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getInfraSpecificEpithet());
  }

  @Test
  public void testAraneae() throws Exception {
    ParsedName pn = parser.parse("Araneae", Rank.ORDER);
    assertEquals("Araneae", pn.getGenusOrAbove());
    assertNull(pn.getAuthorship());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getInfraSpecificEpithet());
    assertEquals(Rank.ORDER, pn.getRank());
  }

  @Test
  public void testInfraspecificName() throws Exception {
    ParsedName pn = parser.parse("Melitaea didyma embriki Bryk, 1940", Rank.INFRASPECIFIC_NAME);
    assertEquals("Melitaea", pn.getGenusOrAbove());
    assertEquals("didyma", pn.getSpecificEpithet());
    assertEquals("embriki", pn.getInfraSpecificEpithet());
    assertEquals("Bryk", pn.getAuthorship());
    assertEquals("1940", pn.getYear());
    assertNull(pn.getInfraGeneric());
    assertEquals(Rank.INFRASPECIFIC_NAME, pn.getRank());

    pn = parser.parse("Melitaea didyma elevar Fruhstorfer, 1917", Rank.INFRASPECIFIC_NAME);
    assertEquals("Melitaea", pn.getGenusOrAbove());
    assertEquals("didyma", pn.getSpecificEpithet());
    assertEquals("elevar", pn.getInfraSpecificEpithet());
    assertEquals("Fruhstorfer", pn.getAuthorship());
    assertEquals("1917", pn.getYear());
    assertNull(pn.getInfraGeneric());
    assertEquals(Rank.INFRASPECIFIC_NAME, pn.getRank());
  }

  @Test
  public void testFormAlikes() throws Exception {
    ParsedName pn = parser.parse("Abacosa pallida Alef.", null);
    assertEquals("Abacosa", pn.getGenusOrAbove());
    assertEquals("pallida", pn.getSpecificEpithet());
    assertEquals("Alef.", pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getInfraGeneric());
    assertEquals(Rank.SPECIES, pn.getRank());
  }

  @Test
  public void testNameParserRankMarker() throws Exception {
    assertEquals(Rank.SUBSPECIES, parser.parse("Coccyzuz americanus ssp.", null).getRank());
    assertEquals(Rank.SUBSPECIES, parser.parse("Coccyzuz ssp", null).getRank());
    assertEquals(Rank.SPECIES, parser.parse("Asteraceae spec.", null).getRank());

    ParsedName cn = parser.parse("Asteraceae spec.", null);
    assertEquals(Rank.SPECIES, cn.getRank());
    assertEquals("sp.", cn.getRankMarker());

    cn = parser.parse("Callideriphus flavicollis morph. reductus Fuchs 1961", null);
    assertEquals(Rank.INFRASUBSPECIFIC_NAME, cn.getRank());
    assertEquals("morph.", cn.getRankMarker());

    ParsedName pn = parser.parse("Euphrasia rostkoviana Hayne subvar. campestris (Jord.) Hartl", null);
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
    assertEquals("Verrucaria foveolata", parser.parseToCanonical("Verrucaria foveolata /Flörke) A. Massal.", null));
    assertEquals("Limatula gwyni", parser.parse("LImatula gwyni	(Sykes, 1903)", null).canonicalNameWithMarker());
    assertEquals("×Fatshedera lizei", parser.parse("× fatshedera lizei", null).canonicalNameWithMarker());
    assertEquals("Fatshedera lizei", parser.parse("fatshedera lizei ", null).canonicalNameWithMarker());
  }

  private ParsedName assertParsedInfrageneric(String name, Rank rank, String genus, String infrageneric, String rankMarker, String basAuthor, String basYear) throws UnparsableException {
    return assertParsedParts(name, rank, null, genus, infrageneric, null, null, rankMarker, null, null, null, basAuthor, basYear, null, null);
  }

  private ParsedName assertParsedInfrageneric(String name, Rank rank, String genus, String infrageneric, String rankMarker, String author, String year, String basAuthor, String basYear) throws UnparsableException {
    return assertParsedParts(name, rank, null, genus, infrageneric, null, null, rankMarker, null, author, year, basAuthor, basYear, null, null);
  }

  private ParsedName assertParsedParts(String name, String genus, String infrageneric, String epithet, String infraepithet,
                                       String rank, NamePart notho, String author, String year, String basAuthor, String basYear, String nomStatus) throws UnparsableException {
    return assertParsedParts(name, null, null, genus, infrageneric, epithet, infraepithet, rank, notho, author, year, basAuthor, basYear, nomStatus, null);
  }

  private ParsedName assertParsedParts(String name, Rank rank, NameType type, String genus, String infrageneric, String epithet, String infraepithet,
                                       String rankMarker, NamePart notho, String author, String year, String basAuthor, String basYear, String nomStatus, String strain)
      throws UnparsableException {
    ParsedName pn = parser.parse(name, rank);
    if (type != null) {
      assertEquals(type, pn.getType());
    }
    assertEquals(genus, pn.getGenusOrAbove());
    assertEquals(infrageneric, pn.getInfraGeneric());
    assertEquals(epithet, pn.getSpecificEpithet());
    assertEquals(infraepithet, pn.getInfraSpecificEpithet());
    assertEquals(rankMarker, pn.getRankMarker());
    assertEquals(notho, pn.getNotho());
    assertEquals(author, pn.getAuthorship());
    assertEquals(year, pn.getYear());
    assertEquals(basAuthor, pn.getBracketAuthorship());
    assertEquals(basYear, pn.getBracketYear());
    assertEquals(nomStatus, pn.getNomStatus());
    assertEquals(strain, pn.getStrain());

    return pn;
  }

  private ParsedName assertParsedMicrobial(String name, NameType type, String genus, String epithet, String infraepithet,
                                           Rank rank, String strain) throws UnparsableException {
    return assertParsedMicrobial(name, type, genus, epithet, infraepithet, rank, null, null, null, null, strain);
  }

  private ParsedName assertParsedMicrobial(String name, NameType type, String genus, String epithet, String infraepithet,
                                           Rank rank) throws UnparsableException {
    return assertParsedMicrobial(name, type, genus, epithet, infraepithet, rank, null, null, null, null, null);
  }

  private ParsedName assertParsedMicrobial(String name, NameType type, String genus, String epithet, String infraepithet,
                                           Rank rank, String author, String year, String basAuthor, String basYear, String strain)
      throws UnparsableException {
    ParsedName pn = parser.parse(name, null);
    assertEquals(genus, pn.getGenusOrAbove());
    assertEquals(epithet, pn.getSpecificEpithet());
    assertEquals(infraepithet, pn.getInfraSpecificEpithet());
    assertEquals(rank, pn.getRank());
    assertEquals(author, pn.getAuthorship());
    assertEquals(year, pn.getYear());
    assertEquals(basAuthor, pn.getBracketAuthorship());
    assertEquals(basYear, pn.getBracketYear());
    assertEquals(strain, pn.getStrain());
    if (type != null) {
      assertEquals(type, pn.getType());
    }
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    return pn;
  }

  private ParsedName assertParsedName(String name, NameType type, String genus, String epithet, String infraepithet, String rank) {
    ParsedName pn = null;
    try {
      pn = assertParsedParts(name, null, type, genus, null, epithet, infraepithet, rank, null, null, null, null, null, null, null);
      assertEquals("Wrong name type", type, pn.getType());
    } catch (UnparsableException e) {
      assertEquals("Wrong name type", type, e.type);
    }
    return pn;
  }

  private ParsedName assertParsedSpecies(String name, NameType type, String genus, String epithet, String author, Integer year) {
    ParsedName pn = null;
    try {
      pn = assertParsedParts(name, null, type, genus, null, epithet, null, null, null, author, year == null ? null : year.toString(), null, null, null, null);
      assertEquals("Wrong name type", type, pn.getType());
    } catch (UnparsableException e) {
      assertEquals("Wrong name type", type, e.type);
    }
    return pn;
  }

  private ParsedName assertParsedInfraspecies(String name, NameType type, String genus, String epithet, String infraepithet, String author, Integer year) {
    ParsedName pn = null;
    try {
      pn = assertParsedParts(name, null, type, genus, null, epithet, infraepithet, null, null, author, year == null ? null : year.toString(), null, null, null, null);
      assertEquals("Wrong name type", type, pn.getType());
    } catch (UnparsableException e) {
      assertEquals("Wrong name type", type, e.type);
    }
    return pn;
  }

  private ParsedName assertParsedSpecies(String name, NameType type, String genus, String epithet, String author, Integer year, String basAuthor, Integer basYear) {
    ParsedName pn = null;
    try {
      pn = assertParsedParts(name, null, type, genus, null, epithet, null, null, null, author, year == null ? null : year.toString(), basAuthor, basYear == null ? null : basYear.toString(), null, null);
      assertEquals("Wrong name type", type, pn.getType());
    } catch (UnparsableException e) {
      assertEquals("Wrong name type", type, e.type);
    }
    return pn;
  }

  private void assertParsedParts(String name, NameType type, String genus, String infrageneric, String epithet, String infraepithet, String rank, String author, String year, String basAuthor, String basYear) throws UnparsableException {
    assertParsedParts(name, null, type, genus, infrageneric, epithet, infraepithet, rank, null, author, year, basAuthor, basYear, null, null);
  }

  private void assertParsedParts(String name, NameType type, String genus, String epithet, String infraepithet, String rank, String author, String year, String basAuthor, String basYear) throws UnparsableException {
    assertParsedParts(name, null, type, genus, null, epithet, infraepithet, rank, null, author, year, basAuthor, basYear, null, null);
  }

  private void assertParsedParts(String name, String genus, String epithet, String infraepithet, String rank)
      throws UnparsableException {
    assertParsedParts(name, null, genus, null, epithet, infraepithet, rank, null, null, null, null);
  }

  private void assertParsedParts(String name, String genus, String epithet, String infraepithet, String rank, String author) throws UnparsableException {
    assertParsedParts(name, null, genus, null, epithet, infraepithet, rank, author, null, null, null);
  }

  private void assertParsedParts(String name, NameType type, String genus, String epithet, String infraepithet, String rank, String author) throws UnparsableException {
    assertParsedParts(name, type, genus, null, epithet, infraepithet, rank, author, null, null, null);
  }

  private void assertParsedParts(String name, String genus, String epithet, String infraepithet, String rank, String author, String year) throws UnparsableException {
    assertParsedParts(name, null, genus, null, epithet, infraepithet, rank, author, year, null, null);
  }

  private void assertParsedParts(String name, String genus, String epithet, String infraepithet, NamePart notho, String rank, String author, String year) throws UnparsableException {
    assertParsedParts(name, null, null, genus, null, epithet, infraepithet, rank, notho, author, year, null, null, null, null);
  }

  private void assertParsedParts(String name, NameType type, String genus, String epithet, String infraepithet, String rank, String author, String year) throws UnparsableException {
    assertParsedParts(name, type, genus, null, epithet, infraepithet, rank, author, year, null, null);
  }

  private void assertStrain(String name, NameType type, String genus, String epithet, String infraepithet, String rank, String strain) throws UnparsableException {
    assertParsedParts(name, null, type, genus, null, epithet, infraepithet, rank, null, null, null, null, null, null, strain);
  }

  @Test
  public void testInfragenericRanks() throws Exception {
    assertParsedInfrageneric("Bodotria (Vertebrata)", Rank.SUBGENUS, "Bodotria", "Vertebrata", "subgen.", null, null);

    assertParsedInfrageneric("Bodotria (Goodsir)", null, "Bodotria", null, null, "Goodsir", null);
    assertParsedInfrageneric("Bodotria (Goodsir)", Rank.SUBGENUS, "Bodotria", "Goodsir", "subgen.", null, null);
    assertParsedInfrageneric("Bodotria (J.Goodsir)", Rank.SUBGENUS, "Bodotria", null, "subgen.", "J.Goodsir", null);

    assertParsedInfrageneric("Latrunculia (Biannulata)", Rank.SUBGENUS, "Latrunculia", "Biannulata", "subgen.", null, null);

    assertParsedParts("Saperda (Saperda) candida m. bipunctata Breuning, 1952", null, NameType.SCIENTIFIC, "Saperda", "Saperda", "candida",
        "bipunctata", "m.", null, "Breuning", "1952", null, null, null, null);

    assertParsedParts("Carex section Acrocystis", null, NameType.SCIENTIFIC, "Carex", "Acrocystis", null, null, "sect.", null, null, null, null, null, null, null);

    assertParsedParts("Juncus subgenus Alpini", null, NameType.SCIENTIFIC, "Juncus", "Alpini", null, null, "subgen.", null, null, null, null, null, null, null);

    assertParsedParts("Solidago subsection Triplinervae", null, NameType.SCIENTIFIC, "Solidago", "Triplinervae", null, null, "subsect.", null, null, null, null, null, null, null);

    assertParsedParts("Eleocharis series Maculosae", null, NameType.SCIENTIFIC, "Eleocharis", "Maculosae", null, null, "ser.", null, null, null, null, null, null, null);

    assertParsedParts("Hylaeus (Alfkenylaeus) Snelling, 1985", Rank.SECTION, NameType.SCIENTIFIC, "Hylaeus", "Alfkenylaeus", null, null, "sect.", null, "Snelling", "1985", null, null, null, null);
  }

  @Test
  public void testInfragenericRanks2() throws Exception {
    assertParsedInfrageneric("Bodotria (Vertebrata)", null, "Bodotria", "Vertebrata", null, null, null);

    assertParsedInfrageneric("Bodotria (Goodsir)", null, "Bodotria", null, null, "Goodsir", null);
    assertParsedInfrageneric("Bodotria (J.Goodsir)", null, "Bodotria", null, null, "J.Goodsir", null);

    assertParsedInfrageneric("Latrunculia (Biannulata)", null, "Latrunculia", "Biannulata", null, null, null);

    assertParsedParts("Saperda (Saperda) candida m. bipunctata Breuning, 1952", null, NameType.SCIENTIFIC, "Saperda", "Saperda", "candida",
        "bipunctata", "m.", null, "Breuning", "1952", null, null, null, null);

    assertParsedParts("Carex section Acrocystis", null, NameType.SCIENTIFIC, "Carex", "Acrocystis", null, null, "sect.", null, null, null, null, null, null, null);

    assertParsedParts("Juncus subgenus Alpini", null, NameType.SCIENTIFIC, "Juncus", "Alpini", null, null, "subgen.", null, null, null, null, null, null, null);

    assertParsedParts("Solidago subsection Triplinervae", null, NameType.SCIENTIFIC, "Solidago", "Triplinervae", null, null, "subsect.", null, null, null, null, null, null, null);

    assertParsedParts("Eleocharis series Maculosae", null, NameType.SCIENTIFIC, "Eleocharis", "Maculosae", null, null, "ser.", null, null, null, null, null, null, null);

    assertParsedParts("Hylaeus (Alfkenylaeus) Snelling, 1985", null, NameType.SCIENTIFIC, "Hylaeus", "Alfkenylaeus", null, null, null, null, "Snelling", "1985", null, null, null, null);
  }

  @Test
  public void testBlacklisted() throws Exception {
    assertNoName("143");
    assertNoName("321-432");
    assertNoName("-,.#");
    assertNoName("");
    assertNoName(" ");
    assertNoName(" .");
  }

  @Test
  public void testUnparsables() throws Exception {
    assertUnparsableDoubtful("pilosa 1986 ?");
    assertUnparsableDoubtful("H4N2 subtype");
    assertUnparsableDoubtful("endosymbiont 'C3 71' of Calyptogena sp. JS, 2002");
  }

  /**
   * http://dev.gbif.org/issues/browse/POR-2672
   */
  @Test
  public void testBadAmpersands() throws Exception {
    assertParsedParts("Celtis sinensis var. nervosa (Hemsl.) F.Y.Lu, C.H.Ou, Y.C.Chen, Y.S.Chi, K.C.Lu & amp; Y.H.Tseng", null, "Celtis", "sinensis", "nervosa", "var.", "F.Y.Lu, C.H.Ou, Y.C.Chen, Y.S.Chi, K.C.Lu & Y.H.Tseng", null, "Hemsl.", null);
    assertParsedParts("Salix taiwanalpina var. chingshuishanensis (S.S.Ying) F.Y.Lu, C.H.Ou, Y.C.Chen, Y.S.Chi, K.C.Lu & Y.H.Tseng ", null, "Salix", "taiwanalpina", "chingshuishanensis", "var.", "F.Y.Lu, C.H.Ou, Y.C.Chen, Y.S.Chi, K.C.Lu & Y.H.Tseng", null, "S.S.Ying", null);
    assertParsedParts("Salix taiwanalpina var. chingshuishanensis (S.S.Ying) F.Y.Lu, C.H.Ou, Y.C.Chen, Y.S.Chi, K.C.Lu & amp  Y.H.Tseng ", null, "Salix", "taiwanalpina", "chingshuishanensis", "var.", "F.Y.Lu, C.H.Ou, Y.C.Chen, Y.S.Chi, K.C.Lu & Y.H.Tseng", null, "S.S.Ying", null);
    assertParsedParts("Salix morrisonicola var. takasagoalpina (Koidz.) F.Y.Lu, C.H.Ou, Y.C.Chen, Y.S.Chi, K.C.Lu & amp; Y.H.Tseng", null, "Salix", "morrisonicola", "takasagoalpina", "var.", "F.Y.Lu, C.H.Ou, Y.C.Chen, Y.S.Chi, K.C.Lu & Y.H.Tseng", null, "Koidz.", null);
    assertParsedParts("Ficus ernanii Carauta, Pederneir., P.P.Souza, A.F.P.Machado, M.D.M.Vianna & amp; Romaniuc", "Ficus", "ernanii", null, null, "Carauta, Pederneir., P.P.Souza, A.F.P.Machado, M.D.M.Vianna & Romaniuc");
  }

  @Test
  public void testCommonPlaceholders() throws Exception {
    assertUnparsableType(NameType.PLACEHOLDER, "Salix taiwanalpina unassigned");
    assertUnparsableType(NameType.PLACEHOLDER, "Salix taiwanalpina alpina UNKNOWN ");
    assertUnparsableType(NameType.PLACEHOLDER, "unknown Plantanus");
    assertUnparsableType(NameType.PLACEHOLDER, "Asteracea (awaiting allocation)");
    assertUnparsableType(NameType.PLACEHOLDER, "Macrodasyida incertae sedis");
    assertUnparsableType(NameType.PLACEHOLDER, " uncertain Plantanus");
    assertUnparsableType(NameType.PLACEHOLDER, "Unknown Cyanobacteria");
    assertUnparsableType(NameType.PLACEHOLDER, "Unknown methanomicrobium (strain EBac)");
    assertUnparsableType(NameType.PLACEHOLDER, "Demospongiae incertae sedis");
    assertUnparsableType(NameType.PLACEHOLDER, "Unallocated Demospongiae");
  }

  @Test
  public void testMissingSpeciesEpithet() throws Exception {
    ParsedName pn = parser.parse("Navicula var. fasciata", null);
    assertEquals("Navicula", pn.getGenusOrAbove());
    assertEquals("fasciata", pn.getInfraSpecificEpithet());
    assertEquals("var.", pn.getRankMarker());
    assertNull(pn.getSpecificEpithet());
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
  public void testNameType() throws Exception {
    assertUnparsableType(NameType.HYBRID, "Asplenium rhizophyllum DC. x ruta-muraria E.L. Braun 1939");
    assertUnparsableType(NameType.HYBRID, "Agrostis L. × Polypogon Desf. ");

    assertUnparsableType(NameType.VIRUS, "Cactus virus 2");

    assertEquals(NameType.SCIENTIFIC, parser.parse("Neobisium carcinoides subsp. balcanicum Hadži, 1937", null).getType());
    assertEquals(NameType.SCIENTIFIC, parser.parse("Festulolium nilssonii Cugnac & A. Camus", null).getType());
    assertEquals(NameType.SCIENTIFIC, parser.parse("Coccyzuz americanus", null).getType());
    assertEquals(NameType.SCIENTIFIC, parser.parse("Tavila indeterminata Walker, 1869", null).getType());
    assertEquals(NameType.SCIENTIFIC, parser.parse("Phyllodonta indeterminata Schaus, 1901", null).getType());
    assertEquals(NameType.SCIENTIFIC, parser.parse("×Festulolium nilssonii Cugnac & A. Camus", null).getType());

    // not using the multiplication sign, but an simple x
    assertEquals(NameType.SCIENTIFIC, parser.parse("x Festulolium nilssonii Cugnac & A. Camus", null).getType());
    assertEquals(NameType.SCIENTIFIC, parser.parse("xFestulolium nilssonii Cugnac & A. Camus", null).getType());
    assertEquals(NameType.SCIENTIFIC, parser.parse("nuculoidea behrens subsp. behrens var.christoph", null).getType());
    assertEquals(NameType.SCIENTIFIC, parser.parse("Neobisium (Neobisium) carcinoides balcanicum Hadži 1937", null).getType());
    assertEquals(NameType.SCIENTIFIC, parser.parse("Neobisium carcinoides ssp. balcanicum Hadži, 1937", null).getType());
    assertEquals(NameType.SCIENTIFIC,
        parser.parse("Valsa hypodermia sensu Berkeley & Broome (Not. Brit. Fungi no., 862)", null).getType());
    assertEquals(NameType.SCIENTIFIC, parser.parse("Solanum aculeatissimum auct. not Jacq.", null).getType());

    assertEquals(NameType.INFORMAL, parser.parse("Coccyzuz cf americanus", null).getType());
    assertEquals(NameType.INFORMAL, parser.parse("Coccyzuz americanus ssp.", null).getType());
    assertEquals(NameType.INFORMAL, parser.parse("Asteraceae spec", null).getType());

    assertEquals(NameType.DOUBTFUL, parser.parse("Callitrichia pilosa (Jocqu¿ & Scharff, 1986)", null).getType());
    assertEquals(NameType.DOUBTFUL, parser.parse("Sinopoda exspectata J¿ger & Ono, 2001", null).getType());
    assertEquals(NameType.DOUBTFUL, parser.parse("Callitrichia pilosa 1986 ?", null).getType());
    assertEquals(NameType.DOUBTFUL,
        parser.parse("Scaphytopius acutus delongi Young, 1952c:, 248 [n.sp. , not nom. nov.]", null).getType());

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
    assertEquals("sensu Baker f.", parser.parse("Trifolium repens sensu Baker f.", null).getSensu());
    assertEquals("sensu latu", parser.parse("Achillea millefolium sensu latu", null).getSensu());
    assertEquals("sec. Greuter, 2009", parser.parse("Achillea millefolium sec. Greuter 2009", null).getSensu());
  }

  @Test
  public void testExtinctPrefix() throws Exception {
    ParsedName pn = parser.parse("†Lachnus bonneti", null);
    assertEquals("Lachnus", pn.getGenusOrAbove());
    assertEquals("bonneti", pn.getSpecificEpithet());
    assertNull(pn.getAuthorship());
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

  /**
   * http://dev.gbif.org/issues/browse/POR-2508
   */
  @Test
  public void testNovNames() throws Exception {

    ParsedName pn = parser.parse("Euphorbia rossiana var. nov. Steinmann, 1899", null);
    assertEquals("Euphorbia", pn.getGenusOrAbove());
    assertEquals("rossiana", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertEquals("Steinmann", pn.getAuthorship());
    assertEquals("1899", pn.getYear());
    assertEquals("var. nov.", pn.getNomStatus());
    assertEquals("var.", pn.getRankMarker());

    pn = parser.parse("Ipomopsis tridactyla (Rydb.) Wilken, comb. nov. ined.", null);
    assertEquals("Ipomopsis", pn.getGenusOrAbove());
    assertEquals("tridactyla", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertEquals("Rydb.", pn.getBracketAuthorship());
    assertEquals("Wilken", pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals("comb. nov. ined.", pn.getNomStatus());

    pn = parser.parse("Orobanche riparia Collins, sp. nov. ined.", null);
    assertEquals("Orobanche", pn.getGenusOrAbove());
    assertEquals("riparia", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertEquals("Collins", pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals("sp.", pn.getRankMarker());
    assertEquals("sp. nov. ined.", pn.getNomStatus());

    pn = parser.parse("Stebbinsoseris gen. nov.", null);
    assertEquals("Stebbinsoseris", pn.getGenusOrAbove());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals("gen.", pn.getRankMarker());
    assertEquals("gen. nov.", pn.getNomStatus());

    pn = parser.parse("Astelia alpina var. novae-hollandiae", null);
    assertEquals("Astelia", pn.getGenusOrAbove());
    assertEquals("alpina", pn.getSpecificEpithet());
    assertEquals("novae-hollandiae", pn.getInfraSpecificEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals("var.", pn.getRankMarker());
    assertNull(pn.getNomStatus());

    pn = parser.parse("Astelia alpina var. november ", null);
    assertEquals("Astelia", pn.getGenusOrAbove());
    assertEquals("alpina", pn.getSpecificEpithet());
    assertEquals("november", pn.getInfraSpecificEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals("var.", pn.getRankMarker());
    assertNull(pn.getNomStatus());

    pn = parser.parse("Astelia alpina subsp. november ", null);
    assertEquals("Astelia", pn.getGenusOrAbove());
    assertEquals("alpina", pn.getSpecificEpithet());
    assertEquals("november", pn.getInfraSpecificEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals("subsp.", pn.getRankMarker());
    assertNull(pn.getNomStatus());

    pn = parser.parse("Astelia alpina november ", null);
    assertEquals("Astelia", pn.getGenusOrAbove());
    assertEquals("alpina", pn.getSpecificEpithet());
    assertEquals("november", pn.getInfraSpecificEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getRankMarker());
    assertNull(pn.getNomStatus());

    pn = parser.parse("Myrionema majus Foslie, nom. nov.", null);
    assertEquals("Myrionema", pn.getGenusOrAbove());
    assertEquals("majus", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertEquals("Foslie", pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getRankMarker());
    assertEquals("nom. nov.", pn.getNomStatus());

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
        NameParser.normalizeStrong("Nuculoidea Williams et  Breger 1916  "));
    assertEquals("Nuculoidea behrens var. christoph Williams & Breger, 1916",
        NameParser.normalizeStrong("Nuculoidea behrens var.christoph Williams & Breger [1916]  "));
    assertEquals("Nuculoidea Williams & Breger, 1916",
        NameParser.normalizeStrong(" 'Nuculoidea Williams & Breger, 1916'"));
    assertEquals("Malacocarpus schumannianus (Nicolai, 1893) Britton & Rose",
        NameParser.normalizeStrong("Malacocarpus schumannianus (Nicolai (1893)) Britton & Rose"));
    assertEquals("Photina (Cardioptera) burmeisteri (Westwood, 1889)",
        NameParser.normalizeStrong("Photina Cardioptera burmeisteri (Westwood 1889)"));
    assertEquals("Suaeda forsskahlei Schweinf.",
        NameParser.normalizeStrong("Suaeda forsskahlei Schweinf. ms."));
    assertEquals("Acacia bicolor Bojer", NameParser.normalizeStrong("Acacia bicolor Bojer ms."));
    assertEquals("Leucanitis roda (Herrich-Schäffer, 1851), 1845",
        NameParser.normalizeStrong("Leucanitis roda Herrich-Schäffer (1851) 1845"));
    assertEquals("Astelia alpina var. novae-hollandiae",
        NameParser.normalizeStrong("Astelia alpina var. novae-hollandiae"));
    // ampersand entities are handled as part of the regular html entity escaping:
    assertEquals("N. behrens Williams & amp; Breger, 1916",
        NameParser.normalizeStrong("  N.behrens Williams &amp;  Breger , 1916  "));
    assertEquals("N.behrens Williams & Breger , 1916",
        NameParser.preClean("  N.behrens Williams &amp;  Breger , 1916  "));
    assertEquals("N. behrens Williams & Breger, 1916",
        NameParser.normalizeStrong(NameParser.preClean("  N.behrens Williams &amp;  Breger , 1916  ")));
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
        n = parser.parse(name, null);
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
      n = parser.parse("(1-SFS) Aster lowrieanus Porter", null);
      parsed = true;
    } catch (UnparsableException e) {
      // nothing, expected to fail
    }
    try {
      n = parser.parse("(1-SFS)%20Aster%20lowrieanus%20Porter", null);
      parsed = true;
    } catch (UnparsableException e) {
      // nothing, expected to fail
    }
  }

  /**
   * These names have been parsing extremely slowely before.
   * Verify all these names parse correctly before reaching a parsing timeout.
   */
  @Test
  public void testSlowNames() throws Exception {
    assertParsedParts("\"Acetobacter aceti var. muciparum\" (sic) (Hoyer) Frateur, 1950", "Acetobacter", "aceti", "muciparum", "var.");
    assertParsedParts("\"Acetobacter melanogenum (sic) var. malto-saccharovorans\" Frateur, 1950", "Acetobacter", "melanogenum", "malto-saccharovorans", "var.");
    assertParsedParts("\"Acetobacter melanogenum (sic) var. maltovorans\" Frateur, 1950", "Acetobacter", "melanogenum", "maltovorans", "var.");
    assertParsedParts("'Abelmoschus esculentus' bunchy top phytoplasma", "Abelmoschus", "esculentus", null, null);
    assertParsedParts("Argyropelecus d'Urvillei Valenciennes, 1849", "Argyropelecus", null, null, null, "d'Urvillei Valenciennes", "1849");
    assertParsedParts("Batillipes africanus Morone De Lucia, D'Addabbo Gallo and Grimaldi de Zio, 1988", "Batillipes", "africanus", null, null, "Morone De Lucia, D'Addabbo Gallo & Grimaldi de Zio", "1988");
    assertParsedParts("Abrotanellinae H.Rob., G.D.Carr, R.M.King & A.M.Powell", "Abrotanellinae", null, null, null, "H.Rob., G.D.Carr, R.M.King & A.M.Powell");
    assertParsedParts("Acidomyces B.J. Baker, M.A. Lutz, S.C. Dawson, P.L. Bond & Banfield", "Acidomyces", null, null, null, "B.J. Baker, M.A. Lutz, S.C. Dawson, P.L. Bond & Banfield");
    assertParsedParts("Acidomyces richmondensis B.J. Baker, M.A. Lutz, S.C. Dawson, P.L. Bond & Banfield, 2004", "Acidomyces", "richmondensis", null, null, "B.J. Baker, M.A. Lutz, S.C. Dawson, P.L. Bond & Banfield", "2004");
    assertParsedParts("Acrodictys liputii L. Cai, K.Q. Zhang, McKenzie, W.H. Ho & K.D. Hyde", "Acrodictys", "liputii", null, null, "L. Cai, K.Q. Zhang, McKenzie, W.H. Ho & K.D. Hyde");
    assertParsedParts("×Attabignya minarum M.J.Balick, A.B.Anderson & J.T.de Medeiros-Costa", "Attabignya", "minarum", null, NamePart.GENERIC, null, "M.J.Balick, A.B.Anderson & J.T.de Medeiros-Costa", null);
    assertParsedParts("Paenibacillus donghaensis Choi,J.H.; Im,W.T.; Yoo,J.S.; Lee,S.M.; Moon,D.S.; Kim,H.J.; Rhee,S.K.; Roh,D.H.", "Paenibacillus", "donghaensis", null, null, "Choi, J.H.; Im, W.T.; Yoo, J.S.; Lee, S.M.; Moon, D.S.; Kim, H.J.; Rhee, S.K.; Roh, D.H.");
    assertParsedParts("Yamatocallis obscura (Ghosh, M.R., A.K. Ghosh & D.N. Raychaudhuri, 1971", "Yamatocallis", "obscura", null, null, null);
    assertParsedParts("Xanthotrogus tadzhikorum Nikolajev, 2008", "Xanthotrogus", "tadzhikorum", null, null, "Nikolajev", "2008");
    assertParsedParts("Xylothamia G.L. Nesom, Y.B. Suh, D.R. Morgan & B.B. Simpson, 1990", "Xylothamia", null, null, null, "G.L. Nesom, Y.B. Suh, D.R. Morgan & B.B. Simpson", "1990");
    assertParsedParts("Virginianthus E.M. Friis, H. Eklund, K.R. Pedersen & P.R. Crane, 1994", "Virginianthus", null, null, null, "E.M. Friis, H. Eklund, K.R. Pedersen & P.R. Crane", "1994");
    assertParsedParts("Equicapillimyces hongkongensis S.S.Y. Wong, A.H.Y. Ngan, Riggs, J.L.L. Teng, G.K.Y. Choi, R.W.S. Poon, J.J.Y. Hui, F.J. Low, Luk", "Equicapillimyces", "hongkongensis", null, null, "S.S.Y. Wong, A.H.Y. Ngan, Riggs, J.L.L. Teng, G.K.Y. Choi, R.W.S. Poon, J.J.Y. Hui, F.J. Low, Luk");

    // these timeout so no author is parsed! Make sure canonical parsing works!
    assertParsedParts("Oreocharis aurea var. cordato-ovata (C.Y. Wu ex H.W. Li) K.Y. Pan, A.L. Weitzman, & L.E. Skog", "Oreocharis", "aurea", "cordato-ovata", "var.", null);
    assertParsedParts("Candida mesorugosa G.M. Chaves, G.R. Terçarioli, A.C.B. Padovan, R. Rosas, R.C. Ferreira, A.S.A. Melo & A.L. Colombo 20", "Candida", "mesorugosa", null, null);
    assertParsedParts("Torulopsis deparaffina H.T. Gao, C.J. Mu, R.H. Li, L.G. Wei, W.Y. Tan, Yue Y. Li, Zhong Q. Li, X.Z. Zhang & J.E. Wang 1979", "Torulopsis", "deparaffina", null, null);
    assertParsedParts("Ophiocordyceps mrciensis (Aung, J.C. Kang, Z.Q. Liang, Soytong & K.D. Hyde) G.H. Sung, J.M. Sung, Hywel-Jones & Spatafora 200", "Ophiocordyceps", "mrciensis", null, null);
    assertParsedParts("Paecilomyces hepiali Q.T. Chen & R.Q. Dai ex R.Q. Dai, X.M. Li, A.J. Shao, Shu F. Lin, J.L. Lan, Wei H. Chen & C.Y. Shen", "Paecilomyces", "hepiali", null, null);
    assertParsedParts("Fusarium mexicanum Otero-Colina, Rodr.-Alvar., Fern.-Pavía, M. Maymon, R.C. Ploetz, T. Aoki, O'Donnell & S. Freeman 201", "Fusarium", "mexicanum", null, null);
    assertParsedParts("Cyrtodactylus bintangrendah Grismer, Wood Jr,  Quah, Anuar, Muin, Sumontha, Ahmad, Bauer, Wangkulangkul, Grismer9 & Pauwels, 201", "Cyrtodactylus", "bintangrendah", null, null);

    // unparsables
    assertUnparsableType(NameType.DOUBTFUL, "'38/89' designation is probably a typo");
    assertUnparsableType(NameType.VIRUS, "Blainville's beaked whale gammaherpesvirus");
  }

  @Test
  public void testSpacelessAuthors() throws Exception {

    ParsedName pn = parser.parse("Abelmoschus moschatus Medik. subsp. tuberosus (Span.) Borss.Waalk.", null);
    assertEquals("Abelmoschus", pn.getGenusOrAbove());
    assertEquals("moschatus", pn.getSpecificEpithet());
    assertEquals("tuberosus", pn.getInfraSpecificEpithet());
    assertEquals("Borss.Waalk.", pn.getAuthorship());
    assertEquals("Span.", pn.getBracketAuthorship());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Acalypha hochstetteriana Müll.Arg.", null);
    assertEquals("Acalypha", pn.getGenusOrAbove());
    assertEquals("hochstetteriana", pn.getSpecificEpithet());
    assertEquals("Müll.Arg.", pn.getAuthorship());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Caloplaca variabilis (Pers.) Müll.Arg.", null);
    assertEquals("Caloplaca", pn.getGenusOrAbove());
    assertEquals("variabilis", pn.getSpecificEpithet());
    assertEquals("Müll.Arg.", pn.getAuthorship());
    assertEquals("Pers.", pn.getBracketAuthorship());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Tridax imbricatus Sch.Bip.", null);
    assertEquals("Tridax", pn.getGenusOrAbove());
    assertEquals("imbricatus", pn.getSpecificEpithet());
    assertEquals("Sch.Bip.", pn.getAuthorship());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

  }


  /**
   * http://dev.gbif.org/issues/browse/POR-2988
   */
  @Test
  public void testDoubleEtAl() throws Exception {
    ParsedName pn = parser.parse("Hymenopus coronatoides Wang & et al., 1994", null);
    assertEquals("Hymenopus", pn.getGenusOrAbove());
    assertEquals("coronatoides", pn.getSpecificEpithet());
    assertEquals("Wang et al.", pn.getAuthorship());
    assertEquals("1994", pn.getYear());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Hymenopus coronatoides Wang & & et & al., 1994", null);
    assertEquals("Hymenopus", pn.getGenusOrAbove());
    assertEquals("coronatoides", pn.getSpecificEpithet());
    assertEquals("Wang et al.", pn.getAuthorship());
    assertEquals("1994", pn.getYear());
    assertEquals(NameType.SCIENTIFIC, pn.getType());
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
      ParsedName pn = parser.parse(name, null);
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
    ParsedName n = parser.parse("Nupserha van rooni usambarica", null);
    assertEquals("Nupserha", n.getGenusOrAbove());
    assertTrue(n.getInfraGeneric() == null);
    assertEquals("van rooni", n.getSpecificEpithet());
    assertEquals("usambarica", n.getInfraSpecificEpithet());

    n = parser.parse("Sargassum flavicans van pervillei", null);
    assertEquals("Sargassum", n.getGenusOrAbove());
    assertTrue(n.getInfraGeneric() == null);
    assertEquals("flavicans", n.getSpecificEpithet());
    assertEquals("van pervillei", n.getInfraSpecificEpithet());

    n = parser.parse("Salix novae angliae lingulata ", null);
    assertEquals("Salix", n.getGenusOrAbove());
    assertTrue(n.getInfraGeneric() == null);
    assertEquals("novae angliae", n.getSpecificEpithet());
    assertEquals("lingulata", n.getInfraSpecificEpithet());

    n = parser.parse("Ilex collina van trompii", null);
    assertEquals("Ilex", n.getGenusOrAbove());
    assertTrue(n.getInfraGeneric() == null);
    assertEquals("collina", n.getSpecificEpithet());
    assertEquals("van trompii", n.getInfraSpecificEpithet());

    n = parser.parse("Gaultheria depressa novae zealandiae", null);
    assertEquals("Gaultheria", n.getGenusOrAbove());
    assertTrue(n.getInfraGeneric() == null);
    assertEquals("depressa", n.getSpecificEpithet());
    assertEquals("novae zealandiae", n.getInfraSpecificEpithet());

    n = parser.parse("Caraguata van volxemi gracilior", null);
    assertEquals("Caraguata", n.getGenusOrAbove());
    assertTrue(n.getInfraGeneric() == null);
    assertEquals("van volxemi", n.getSpecificEpithet());
    assertEquals("gracilior", n.getInfraSpecificEpithet());

    n = parser.parse("Ancistrocerus agilis novae guineae", null);
    assertEquals("Ancistrocerus", n.getGenusOrAbove());
    assertTrue(n.getInfraGeneric() == null);
    assertEquals("agilis", n.getSpecificEpithet());
    assertEquals("novae guineae", n.getInfraSpecificEpithet());

    // also test the authorless parsing
    n = parser.parse("Ancistrocerus agilis novae guineae", null);
    assertEquals("Ancistrocerus", n.getGenusOrAbove());
    assertTrue(n.getInfraGeneric() == null);
    assertEquals("agilis", n.getSpecificEpithet());
    assertEquals("novae guineae", n.getInfraSpecificEpithet());
  }

  @Test
  @Ignore
  public void manuallyTestProblematicName() throws Exception {
    for (String n : new String[]{
        "Severinia turcomaniae amplialata Unknown, 1921",
        "Tipula (Unplaced) fumipennis Alexander, 1912",
    }) {
      System.out.println(parser.parse(n, null));
    }

  }

}



