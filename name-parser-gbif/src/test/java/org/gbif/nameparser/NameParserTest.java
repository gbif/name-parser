package org.gbif.nameparser;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import org.gbif.api.exception.UnparsableException;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.NamePart;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.Rank;
import org.gbif.utils.file.FileUtils;
import org.gbif.utils.file.InputStreamUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.util.regex.Matcher;

import static org.junit.Assert.*;

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
  private GBIFNameParser parser = new GBIFNameParser(50);

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

  private String extractNomNote(String name) {
    String nomNote = null;
    Matcher matcher = GBIFNameParser.EXTRACT_NOMSTATUS.matcher(name);
    if (matcher.find()) {
      nomNote = (StringUtils.trimToNull(matcher.group(1)));
    }
    return nomNote;
  }

  @Test
  public void test4PartedNames() throws Exception {
    ParsedName n = parser.parse("Bombus sichelii alticola latofasciatus", null);
    assertEquals("Bombus sichelii latofasciatus", n.canonicalName());
    assertEquals("Bombus sichelii infrasubsp. latofasciatus", n.canonicalNameWithMarker());

    n = parser.parse("Bombus sichelii alticola latofasciatus Vogt, 1909", null);
    assertEquals("Bombus sichelii latofasciatus", n.canonicalName());
    assertEquals("Bombus sichelii infrasubsp. latofasciatus", n.canonicalNameWithMarker());
    assertEquals("Bombus sichelii infrasubsp. latofasciatus Vogt, 1909", n.canonicalNameComplete());

    n = parser.parse("Poa pratensis kewensis proles (L.) Rouy, 1913", null);
    assertEquals("Poa", n.getGenusOrAbove());
    assertEquals("pratensis", n.getSpecificEpithet());
    assertEquals("proles", n.getInfraSpecificEpithet());
    assertEquals("Rouy", n.getAuthorship());
    assertEquals("1913", n.getYear());
    assertEquals("L.", n.getBracketAuthorship());
    assertNull(n.getBracketYear());
    assertEquals(NameType.SCIENTIFIC, n.getType());
    assertEquals("Poa pratensis proles", n.canonicalName());
    assertEquals("Poa pratensis infrasubsp. proles", n.canonicalNameWithMarker());
    assertEquals("Poa pratensis infrasubsp. proles (L.) Rouy, 1913", n.canonicalNameComplete());
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
    assertTrue(this.testAuthorship("Wang, Yuwen & Xian-wei Liu"));
    assertTrue(this.testAuthorship("Liu, Xian-wei, Z. Zheng & G. Xi"));
  }

  @Test
  public void testNotNames() throws Exception {
    ParsedName pn = parser.parse("Diatrypella favacea var. favacea (Fr.) Ces. & De Not.", null);
    assertEquals("Diatrypella", pn.getGenusOrAbove());
    assertEquals("favacea", pn.getSpecificEpithet());
    assertEquals(Rank.VARIETY, pn.getRank());
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
    assertNull(pn.getNotho());
    assertEquals("Polygonum", pn.getGenusOrAbove());
    assertEquals("Bistorta", pn.getInfraGeneric());
    assertEquals(Rank.SUBGENUS, pn.getRank());
    assertEquals("Zernov", pn.getAuthorship());
    assertEquals("L.", pn.getBracketAuthorship());

    ParsedName n = parser.parse("Arrhoges (Antarctohoges)", null);
    assertEquals("Arrhoges", n.getGenusOrAbove());
    assertEquals("Antarctohoges", n.getBracketAuthorship());
    assertNull(n.getInfraGeneric());
    assertNull(n.getRank());
    assertNull(n.getNotho());

    n = parser.parse("Arrhoges (Antarctohoges)", Rank.SUBGENUS);
    assertEquals("Arrhoges", n.getGenusOrAbove());
    assertEquals("Antarctohoges", n.getInfraGeneric());
    assertEquals(Rank.SUBGENUS, n.getRank());
    assertNull(n.getNotho());

    pn = parser.parse("Festuca subg. Schedonorus (P. Beauv. ) Peterm.", null);
    assertEquals("Festuca", pn.getGenusOrAbove());
    assertEquals("Schedonorus", pn.getInfraGeneric());
    assertEquals(Rank.SUBGENUS, pn.getRank());
    assertEquals("Peterm.", pn.getAuthorship());
    assertEquals("P. Beauv.", pn.getBracketAuthorship());

    n = parser.parse("Catapodium subg.Agropyropsis  Trab.", null);
    assertEquals("Catapodium", n.getGenusOrAbove());
    assertEquals("Agropyropsis", n.getInfraGeneric());
    assertEquals(Rank.SUBGENUS, n.getRank());

    n = parser.parse(" Gnaphalium subg. Laphangium Hilliard & B. L. Burtt", null);
    assertEquals("Gnaphalium", n.getGenusOrAbove());
    assertEquals("Laphangium", n.getInfraGeneric());
    assertEquals(Rank.SUBGENUS, n.getRank());

    n = parser.parse("Woodsiaceae (Hooker) Herter", null);
    assertEquals("Woodsiaceae", n.getGenusOrAbove());
    assertNull(n.getInfraGeneric());
    assertEquals(Rank.FAMILY, n.getRank());
  }

  @Test
  public void testClean1() throws Exception {
    assertEquals("", GBIFNameParser.preClean(""));
    assertEquals("Hallo Spencer", GBIFNameParser.preClean("Hallo Spencer "));
    assertEquals("Hallo Spencer", GBIFNameParser.preClean("' 'Hallo Spencer"));
    assertEquals("Hallo Spencer 1982", GBIFNameParser.preClean("'\" Hallo  Spencer 1982'"));
  }

  private boolean testCultivar(String cultivar) {
    Matcher m = GBIFNameParser.CULTIVAR.matcher(cultivar);
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

    parseCandidatus("Candidatus Phytoplasma allocasuarinae", "Phytoplasma", "allocasuarinae", null);

    parseCandidatus("Ca. Phytoplasma allocasuarinae", "Phytoplasma", "allocasuarinae", null);

    parseCandidatus("Candidatus Phytoplasma", "Phytoplasma", null, null);
    parseCandidatus("Ca. Phytoplasma", "Phytoplasma", null, null);

    parseCandidatus("'Candidatus Nicolleia'", "Nicolleia", null, null);


    pn = parseCandidatus("\"Candidatus Riegeria\" Gruber-Vodicka et al., 2011", "Riegeria", null, null);
    assertEquals("Gruber-Vodicka et al.", pn.getAuthorship());
    assertEquals("2011", pn.getYear());

    pn = parseCandidatus("Candidatus Protochlamydia amoebophila Collingro et al., 2005", "Protochlamydia", "amoebophila", null);
    assertEquals("Collingro et al.", pn.getAuthorship());
    assertEquals("2005", pn.getYear());

    assertParsedParts("Centropogon candidatus Lammers", NameType.SCIENTIFIC, "Centropogon", "candidatus", null, Rank.SPECIES, "Lammers", null);
    assertParsedParts("Parathalassius candidatus Melander", NameType.SCIENTIFIC, "Parathalassius", "candidatus", null, Rank.SPECIES, "Melander", null);


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
  //@Ignore("needs a parser update, just added for test first approach")
  public void testCultivarNames() throws Exception {
    ParsedName pn = parser.parse("Verpericola megasoma \"Dall\" Pils.", null);


    pn = parser.parse("Abutilon 'Kentish Belle'", null);
    assertEquals("Abutilon", pn.getGenusOrAbove());
    assertEquals("Kentish Belle", pn.getCultivarEpithet());
    assertEquals(NameType.CULTIVAR, pn.getType());

    pn = parser.parse("Abutilon 'Nabob'", null);
    assertEquals("Abutilon", pn.getGenusOrAbove());
    assertEquals("Nabob", pn.getCultivarEpithet());
    assertEquals(NameType.CULTIVAR, pn.getType());

    pn = parser.parse("Verpericola megasoma \"Dall\" Pils.", null);
    assertEquals("Verpericola", pn.getGenusOrAbove());
    assertEquals("megasoma", pn.getSpecificEpithet());
    assertEquals("Dall", pn.getCultivarEpithet());
    assertEquals("Pils.", pn.getAuthorship());
    assertEquals(NameType.CULTIVAR, pn.getType());

    pn = parser.parse("Sorbus americana Marshall cv. 'Belmonte'", null);
    assertEquals("Sorbus", pn.getGenusOrAbove());
    assertEquals("americana", pn.getSpecificEpithet());
    assertEquals("Belmonte", pn.getCultivarEpithet());
    assertEquals(NameType.CULTIVAR, pn.getType());

    pn = parser.parse("Sorbus hupehensis C.K.Schneid. cv. 'November pink'", null);
    assertEquals("Sorbus", pn.getGenusOrAbove());
    assertEquals("hupehensis", pn.getSpecificEpithet());
    assertEquals("November pink", pn.getCultivarEpithet());
    assertEquals(NameType.CULTIVAR, pn.getType());

    pn = parser.parse("Symphoricarpos albus (L.) S.F.Blake cv. 'Turesson'", null);
    assertEquals("Symphoricarpos", pn.getGenusOrAbove());
    assertEquals("albus", pn.getSpecificEpithet());
    assertEquals("Turesson", pn.getCultivarEpithet());
    assertEquals(NameType.CULTIVAR, pn.getType());
    assertEquals(Rank.CULTIVAR, pn.getRank());

    pn = parser.parse("Symphoricarpos sp. cv. 'mother of pearl'", null);
    assertEquals("Symphoricarpos", pn.getGenusOrAbove());
    assertEquals("mother of pearl", pn.getCultivarEpithet());
    assertEquals(NameType.CULTIVAR, pn.getType());
    assertEquals(Rank.CULTIVAR, pn.getRank());

    pn = parser.parse("Primula Border Auricula Group", null);
    assertEquals("Primula", pn.getGenusOrAbove());
    assertEquals("Border Auricula", pn.getCultivarEpithet());
    assertEquals(NameType.CULTIVAR, pn.getType());
    assertEquals(Rank.CULTIVAR_GROUP, pn.getRank());

    pn = parser.parse("Rhododendron boothii Mishmiense Group", null);
    assertEquals("Rhododendron", pn.getGenusOrAbove());
    assertEquals("boothii", pn.getSpecificEpithet());
    assertEquals("Mishmiense", pn.getCultivarEpithet());
    assertEquals(NameType.CULTIVAR, pn.getType());
    assertEquals(Rank.CULTIVAR_GROUP, pn.getRank());

    pn = parser.parse("Paphiopedilum Sorel grex", null);
    assertEquals("Paphiopedilum", pn.getGenusOrAbove());
    assertNull(pn.getSpecificEpithet());
    assertEquals("Sorel", pn.getCultivarEpithet());
    assertEquals(NameType.CULTIVAR, pn.getType());
    assertEquals(Rank.GREX, pn.getRank());
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
    assertNull(pn.getNotho());
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

    assertEquals("Polypodium vulgare nothosubsp. mantoniae (Rothm.) Schidlay", parser.parse("Polypodium  x vulgare nothosubsp. mantoniae (Rothm.) Schidlay ", null).fullName());
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
    assertNull(n.getInfraSpecificEpithet());
    assertEquals("L.L.Daniel", n.getAuthorship());

    n = parser.parse(" × Pyrocrataegus willei L.L.Daniel", null);
    assertEquals(NamePart.GENERIC, n.getNotho());
    assertEquals("Pyrocrataegus", n.getGenusOrAbove());
    assertEquals("willei", n.getSpecificEpithet());
    assertNull(n.getInfraSpecificEpithet());
    assertEquals("L.L.Daniel", n.getAuthorship());

    n = parser.parse(" X Pyrocrataegus willei L.L.Daniel", null);
    assertEquals(NamePart.GENERIC, n.getNotho());
    assertEquals("Pyrocrataegus", n.getGenusOrAbove());
    assertEquals("willei", n.getSpecificEpithet());
    assertNull(n.getInfraSpecificEpithet());
    assertEquals("L.L.Daniel", n.getAuthorship());

    n = parser.parse("Pyrocrataegus ×willei L.L.Daniel", null);
    assertEquals(NamePart.SPECIFIC, n.getNotho());
    assertEquals("Pyrocrataegus", n.getGenusOrAbove());
    assertEquals("willei", n.getSpecificEpithet());
    assertNull(n.getInfraSpecificEpithet());
    assertEquals("L.L.Daniel", n.getAuthorship());

    n = parser.parse("Pyrocrataegus × willei L.L.Daniel", null);
    assertEquals(NamePart.SPECIFIC, n.getNotho());
    assertEquals("Pyrocrataegus", n.getGenusOrAbove());
    assertEquals("willei", n.getSpecificEpithet());
    assertNull(n.getInfraSpecificEpithet());
    assertEquals("L.L.Daniel", n.getAuthorship());

    n = parser.parse("Pyrocrataegus x willei L.L.Daniel", null);
    assertEquals(NamePart.SPECIFIC, n.getNotho());
    assertEquals("Pyrocrataegus", n.getGenusOrAbove());
    assertEquals("willei", n.getSpecificEpithet());
    assertNull(n.getInfraSpecificEpithet());
    assertEquals("L.L.Daniel", n.getAuthorship());

    n = parser.parse("Pyrocrataegus X willei L.L.Daniel", null);
    assertEquals(NamePart.SPECIFIC, n.getNotho());
    assertEquals("Pyrocrataegus", n.getGenusOrAbove());
    assertEquals("willei", n.getSpecificEpithet());
    assertNull(n.getInfraSpecificEpithet());
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
   * http://dev.gbif.org/issues/browse/POR-3069
   */
  @Test
  public void testNullNameParts() throws Exception {
    assertParsedParts("Austrorhynchus pectatus null pectatus", NameType.DOUBTFUL, "Austrorhynchus", "pectatus", "pectatus", null, null);
    assertParsedParts("Poa pratensis null proles (L.) Rouy, 1913", NameType.INFORMAL, "Poa", "pratensis", null, Rank.PROLES, "Rouy", "1913", "L.", null);

    // should the infrasubspecific epithet kewensis be removed from the parsed name?
    //assertParsedParts("Poa pratensis kewensis proles", NameType.INFORMAL, "Poa", "pratensis", "kewensis", Rank.PROLES, null);
    //assertParsedParts("Poa pratensis kewensis proles (L.) Rouy, 1913", NameType.INFORMAL, "Poa", "pratensis", null, Rank.PROLES, "Rouy", "1913", "L.", null);
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

  /**
   * Tests that were previously hosted in the scientific_name.txt file
   */
  @Test
  public void testNameFileJava() throws Exception {
    ParsedName pn = parser.parse("Pseudocercospora ", null);
    assertEquals("Pseudocercospora", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertNull(pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Pseudocercospora Speg. ", null);
    assertEquals("Pseudocercospora", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Speg.", pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertNull(pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Pseudocercospora Speg. 1910 ", null);
    assertEquals("Pseudocercospora", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Speg.", pn.getAuthorship());
    assertEquals("1910", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertNull(pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Pseudocercospora Spegazzini, 1910 ", null);
    assertEquals("Pseudocercospora", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Spegazzini", pn.getAuthorship());
    assertEquals("1910", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertNull(pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Tridentella tangeroae Bruce, 198? ", null);
    assertEquals("Tridentella", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("tangeroae", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Bruce", pn.getAuthorship());
    assertEquals("198?", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.DOUBTFUL, pn.getType());

    pn = parser.parse("Ca Dyar 1914 ", null);
    assertEquals("Ca", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Dyar", pn.getAuthorship());
    assertEquals("1914", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertNull(pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Ea Distant 1911 ", null);
    assertEquals("Ea", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Distant", pn.getAuthorship());
    assertEquals("1911", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertNull(pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Ge Nicéville 1895 ", null);
    assertEquals("Ge", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Nicéville", pn.getAuthorship());
    assertEquals("1895", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertNull(pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Ia Thomas 1902 ", null);
    assertEquals("Ia", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Thomas", pn.getAuthorship());
    assertEquals("1902", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertNull(pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Io Lea 1831 ", null);
    assertEquals("Io", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Lea", pn.getAuthorship());
    assertEquals("1831", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertNull(pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Io Blanchard 1852 ", null);
    assertEquals("Io", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Blanchard", pn.getAuthorship());
    assertEquals("1852", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertNull(pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Ix Bergroth 1916 ", null);
    assertEquals("Ix", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Bergroth", pn.getAuthorship());
    assertEquals("1916", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertNull(pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Lo Seale 1906 ", null);
    assertEquals("Lo", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Seale", pn.getAuthorship());
    assertEquals("1906", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertNull(pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Oa Girault 1929 ", null);
    assertEquals("Oa", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Girault", pn.getAuthorship());
    assertEquals("1929", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertNull(pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Ra Whitley 1931 ", null);
    assertEquals("Ra", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Whitley", pn.getAuthorship());
    assertEquals("1931", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertNull(pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Ty Bory de St. Vincent 1827 ", null);
    assertEquals("Ty", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Bory de St. Vincent", pn.getAuthorship());
    assertEquals("1827", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertNull(pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Ua Girault 1929 ", null);
    assertEquals("Ua", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Girault", pn.getAuthorship());
    assertEquals("1929", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertNull(pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Aa Baker 1940 ", null);
    assertEquals("Aa", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Baker", pn.getAuthorship());
    assertEquals("1940", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertNull(pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Ja Uéno 1955 ", null);
    assertEquals("Ja", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Uéno", pn.getAuthorship());
    assertEquals("1955", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertNull(pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Zu Walters & Fitch 1960 ", null);
    assertEquals("Zu", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Walters & Fitch", pn.getAuthorship());
    assertEquals("1960", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertNull(pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("La Bleszynski 1966 ", null);
    assertEquals("La", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Bleszynski", pn.getAuthorship());
    assertEquals("1966", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertNull(pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Qu Durkoop ", null);
    assertEquals("Qu", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Durkoop", pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertNull(pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("As Slipinski 1982 ", null);
    assertEquals("As", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Slipinski", pn.getAuthorship());
    assertEquals("1982", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertNull(pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Ba Solem 1983 ", null);
    assertEquals("Ba", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Solem", pn.getAuthorship());
    assertEquals("1983", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertNull(pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Pseudocercospora     dendrobii ", null);
    assertEquals("Pseudocercospora", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("dendrobii", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Platypus bicaudatulus Schedl 1935 ", null);
    assertEquals("Platypus", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("bicaudatulus", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Schedl", pn.getAuthorship());
    assertEquals("1935", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Platypus bicaudatulus Schedl, 1935h ", null);
    assertEquals("Platypus", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("bicaudatulus", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Schedl", pn.getAuthorship());
    assertEquals("1935h", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Donatia novae zelandiae Hook.f.", null);
    assertEquals("Donatia", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("novae zelandiae", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Hook.f.", pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Donatia novae-zelandiae Hook.f", null);
    assertEquals("Donatia", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("novae-zelandiae", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Hook.f", pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Denticula van heurckii var. angusta Hust.", null);
    assertEquals("Denticula", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("van heurckii", pn.getSpecificEpithet());
    assertEquals("angusta", pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Hust.", pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.VARIETY, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Denticula van heurckii f. ventricosa Hust.", null);
    assertEquals("Denticula", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("van heurckii", pn.getSpecificEpithet());
    assertEquals("ventricosa", pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Hust.", pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.FORM, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Pseudocercospora dendrobii U. Braun & Crous ", null);
    assertEquals("Pseudocercospora", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("dendrobii", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("U. Braun & Crous", pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Pseudocercospora dendrobii U. Braun & Crous ", null);
    assertEquals("Pseudocercospora", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("dendrobii", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("U. Braun & Crous", pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Pseudocercospora dendrobii U. Braun and Crous ", null);
    assertEquals("Pseudocercospora", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("dendrobii", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("U. Braun & Crous", pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Pseudocercospora dendrobii U. Braun & Crous 2003 ", null);
    assertEquals("Pseudocercospora", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("dendrobii", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("U. Braun & Crous", pn.getAuthorship());
    assertEquals("2003", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Hegeter (Hegeter) intercedens Lindberg 1950 ", null);
    assertEquals("Hegeter", pn.getGenusOrAbove());
    assertEquals("Hegeter", pn.getInfraGeneric());
    assertEquals("intercedens", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Lindberg", pn.getAuthorship());
    assertEquals("1950", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Ferganoconcha? oblonga ", null);
    assertEquals("Ferganoconcha", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("oblonga", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.DOUBTFUL, pn.getType());

    pn = parser.parse("Rühlella", null);
    assertEquals("Rühlella", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertNull(pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Sténométope laevissimus Bibron 1855", null);
    assertEquals("Sténométope", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("laevissimus", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Bibron", pn.getAuthorship());
    assertEquals("1855", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Choriozopella trägårdhi Lawrence, 1947", null);
    assertEquals("Choriozopella", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("trägårdhi", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Lawrence", pn.getAuthorship());
    assertEquals("1947", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Læptura laetifica Dow, 1913 ", null);
    assertEquals("Læptura", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("laetifica", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Dow", pn.getAuthorship());
    assertEquals("1913", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Leptura lætifica Dow, 1913 ", null);
    assertEquals("Leptura", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("lætifica", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Dow", pn.getAuthorship());
    assertEquals("1913", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Leptura leætifica Dow, 1913 ", null);
    assertEquals("Leptura", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("leætifica", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Dow", pn.getAuthorship());
    assertEquals("1913", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Leæptura laetifica Dow, 1913 ", null);
    assertEquals("Leæptura", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("laetifica", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Dow", pn.getAuthorship());
    assertEquals("1913", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Leœptura laetifica Dow, 1913 ", null);
    assertEquals("Leœptura", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("laetifica", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Dow", pn.getAuthorship());
    assertEquals("1913", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Ærenea cognata Lacordaire, 1872 ", null);
    assertEquals("Ærenea", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("cognata", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Lacordaire", pn.getAuthorship());
    assertEquals("1872", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Œdicnemus capensis ", null);
    assertEquals("Œdicnemus", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("capensis", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Œnanthe œnanthe ", null);
    assertEquals("Œnanthe", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("œnanthe", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Zophosis persis (Chatanay, 1914) ", null);
    assertEquals("Zophosis", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("persis", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals("Chatanay", pn.getBracketAuthorship());
    assertEquals("1914", pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Zophosis persis (Chatanay 1914) ", null);
    assertEquals("Zophosis", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("persis", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals("Chatanay", pn.getBracketAuthorship());
    assertEquals("1914", pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Pseudocercospora dendrobii(H.C.     Burnett)U. Braun & Crous     2003 ", null);
    assertEquals("Pseudocercospora", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("dendrobii", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("U. Braun & Crous", pn.getAuthorship());
    assertEquals("2003", pn.getYear());
    assertEquals("H.C. Burnett", pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Pseudocercospora dendrobii(H.C.     Burnett, 1873)U. Braun & Crous     2003 ", null);
    assertEquals("Pseudocercospora", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("dendrobii", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("U. Braun & Crous", pn.getAuthorship());
    assertEquals("2003", pn.getYear());
    assertEquals("H.C. Burnett", pn.getBracketAuthorship());
    assertEquals("1873", pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Pseudocercospora dendrobii(H.C.     Burnett 1873)U. Braun & Crous ,    2003 ", null);
    assertEquals("Pseudocercospora", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("dendrobii", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("U. Braun & Crous", pn.getAuthorship());
    assertEquals("2003", pn.getYear());
    assertEquals("H.C. Burnett", pn.getBracketAuthorship());
    assertEquals("1873", pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Hydnellum scrobiculatum zonatum (Banker) D. Hall & D.E. Stuntz 1972 ", null);
    assertEquals("Hydnellum", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("scrobiculatum", pn.getSpecificEpithet());
    assertEquals("zonatum", pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("D. Hall & D.E. Stuntz", pn.getAuthorship());
    assertEquals("1972", pn.getYear());
    assertEquals("Banker", pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.INFRASPECIFIC_NAME, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Hydnellum (Hydnellum) scrobiculatum zonatum (Banker) D. Hall & D.E. Stuntz 1972 ", null);
    assertEquals("Hydnellum", pn.getGenusOrAbove());
    assertEquals("Hydnellum", pn.getInfraGeneric());
    assertEquals("scrobiculatum", pn.getSpecificEpithet());
    assertEquals("zonatum", pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("D. Hall & D.E. Stuntz", pn.getAuthorship());
    assertEquals("1972", pn.getYear());
    assertEquals("Banker", pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.INFRASPECIFIC_NAME, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Hydnellum scrobiculatum zonatum ", null);
    assertEquals("Hydnellum", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("scrobiculatum", pn.getSpecificEpithet());
    assertEquals("zonatum", pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.INFRASPECIFIC_NAME, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Aus bus Linn. var. bus ", null);
    assertEquals("Aus", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("bus", pn.getSpecificEpithet());
    assertEquals("bus", pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.VARIETY, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Agalinis purpurea (L.) Briton var. borealis (Berg.) Peterson 1987 ", null);
    assertEquals("Agalinis", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("purpurea", pn.getSpecificEpithet());
    assertEquals("borealis", pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Peterson", pn.getAuthorship());
    assertEquals("1987", pn.getYear());
    assertEquals("Berg.", pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.VARIETY, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Callideriphus flavicollis morph. reductus Fuchs 1961 ", null);
    assertEquals("Callideriphus", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("flavicollis", pn.getSpecificEpithet());
    assertEquals("reductus", pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Fuchs", pn.getAuthorship());
    assertEquals("1961", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.MORPH, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Caulerpa cupressoides forma nuda ", null);
    assertEquals("Caulerpa", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("cupressoides", pn.getSpecificEpithet());
    assertEquals("nuda", pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.FORM, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Chlorocyperus glaber form. fasciculariforme (Lojac.) Soó ", null);
    assertEquals("Chlorocyperus", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("glaber", pn.getSpecificEpithet());
    assertEquals("fasciculariforme", pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Soó", pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals("Lojac.", pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.FORM, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Sphaerotheca    fuliginea    f.     dahliae    Movss.     1967 ", null);
    assertEquals("Sphaerotheca", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("fuliginea", pn.getSpecificEpithet());
    assertEquals("dahliae", pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Movss.", pn.getAuthorship());
    assertEquals("1967", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.FORM, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Polypodium vulgare nothosubsp. mantoniae (Rothm.) Schidlay ", null);
    assertEquals("Polypodium", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("vulgare", pn.getSpecificEpithet());
    assertEquals("mantoniae", pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Schidlay", pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals("Rothm.", pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SUBSPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertEquals(NamePart.INFRASPECIFIC, pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Hydnellum scrobiculatum var. zonatum f. parvum (Banker) D. Hall & D.E. Stuntz 1972 ", null);
    assertEquals("Hydnellum", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("scrobiculatum", pn.getSpecificEpithet());
    assertEquals("parvum", pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("D. Hall & D.E. Stuntz", pn.getAuthorship());
    assertEquals("1972", pn.getYear());
    assertEquals("Banker", pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.FORM, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Senecio fuchsii C.C.Gmel. subsp. fuchsii var. expansus (Boiss. & Heldr.) Hayek ", null);
    assertEquals("Senecio", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("fuchsii", pn.getSpecificEpithet());
    assertEquals("expansus", pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Hayek", pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals("Boiss. & Heldr.", pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.VARIETY, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Senecio fuchsii C.C.Gmel. subsp. fuchsii var. fuchsii ", null);
    assertEquals("Senecio", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("fuchsii", pn.getSpecificEpithet());
    assertEquals("fuchsii", pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.VARIETY, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Tragacantha leporina (?) Kuntze ", null);
    assertEquals("Tragacantha", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("leporina", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
//    assertEquals("Kuntze", pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.DOUBTFUL, pn.getType());

    pn = parser.parse("Lachenalia tricolor var. nelsonii (auct.) Baker ", null);
    assertEquals("Lachenalia", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("tricolor", pn.getSpecificEpithet());
    assertEquals("nelsonii", pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Baker", pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.VARIETY, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Lachenalia tricolor var. nelsonii (anon.) Baker ", null);
    assertEquals("Lachenalia", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("tricolor", pn.getSpecificEpithet());
    assertEquals("nelsonii", pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
//    assertEquals("Baker", pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.VARIETY, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Lachenalia tricolor var. nelsonii (ht.) Baker ", null);
    assertEquals("Lachenalia", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("tricolor", pn.getSpecificEpithet());
    assertEquals("nelsonii", pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
//    assertEquals("Baker", pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.VARIETY, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Lachenalia tricolor var. nelsonii (hort.) Baker ", null);
    assertEquals("Lachenalia", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("tricolor", pn.getSpecificEpithet());
    assertEquals("nelsonii", pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
//    assertEquals("Baker", pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.VARIETY, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Puya acris ht. ", null);
    assertEquals("Puya", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("acris", pn.getSpecificEpithet());
//    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
//    assertNull(pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Puya acris anon. ", null);
    assertEquals("Puya", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("acris", pn.getSpecificEpithet());
//    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Puya acris hort. ", null);
    assertEquals("Puya", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("acris", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
//    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.INFORMAL, pn.getType());

    pn = parser.parse("Puya acris auct.", null);
    assertEquals("Puya", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("acris", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Anabaena catenula (K?tzing) Bornet & Flahault", null);
    assertEquals("Anabaena", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("catenula", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Bornet & Flahault", pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals("K?tzing", pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.DOUBTFUL, pn.getType());

    pn = parser.parse("Fagus sylvatica subsp. orientalis (Lipsky) Greuter & Burdet ", null);
    assertEquals("Fagus", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("sylvatica", pn.getSpecificEpithet());
    assertEquals("orientalis", pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Greuter & Burdet", pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals("Lipsky", pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SUBSPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Stagonospora polyspora M.T. Lucas & Sousa da Câmara 1934 ", null);
    assertEquals("Stagonospora", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("polyspora", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("M.T. Lucas & Sousa da Câmara", pn.getAuthorship());
    assertEquals("1934", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Stagonospora polyspora M.T. Lucas et Sousa da Câmara 1934 ", null);
    assertEquals("Stagonospora", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("polyspora", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("M.T. Lucas & Sousa da Câmara", pn.getAuthorship());
    assertEquals("1934", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Cladoniicola staurospora Diederich, van den Boom & Aptroot 2001 ", null);
    assertEquals("Cladoniicola", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("staurospora", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Diederich, van den Boom & Aptroot", pn.getAuthorship());
    assertEquals("2001", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Yarrowia lipolytica var. lipolytica (Wick., Kurtzman & E.A. Herrm.) Van der Walt & Arx 1981 ", null);
    assertEquals("Yarrowia", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("lipolytica", pn.getSpecificEpithet());
    assertEquals("lipolytica", pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Van der Walt & Arx", pn.getAuthorship());
    assertEquals("1981", pn.getYear());
    assertEquals("Wick., Kurtzman & E.A. Herrm.", pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.VARIETY, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Physalospora rubiginosa (Fr.) anon. ", null);
    assertEquals("Physalospora", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("rubiginosa", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals("Fr.", pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Pleurotus ëous (Berk.) Sacc. 1887 ", null);
    assertEquals("Pleurotus", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("ëous", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Sacc.", pn.getAuthorship());
    assertEquals("1887", pn.getYear());
    assertEquals("Berk.", pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Lecanora wetmorei Śliwa 2004 ", null);
    assertEquals("Lecanora", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("wetmorei", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Śliwa", pn.getAuthorship());
    assertEquals("2004", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Calicium furfuraceum * furfuraceum (L.) Pers. 1797 ", null);
    assertEquals("Calicium", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("furfuraceum", pn.getSpecificEpithet());
    assertEquals("furfuraceum", pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Pers.", pn.getAuthorship());
    assertEquals("1797", pn.getYear());
    assertEquals("L.", pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.INFRASPECIFIC_NAME, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Exobasidium vaccinii ** andromedae (P. Karst.) P. Karst. 1882 ", null);
    assertEquals("Exobasidium", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("vaccinii", pn.getSpecificEpithet());
    assertEquals("andromedae", pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("P. Karst.", pn.getAuthorship());
    assertEquals("1882", pn.getYear());
    assertEquals("P. Karst.", pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.INFRASPECIFIC_NAME, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Urceolaria scruposa **** clausa Flot. 1849 ", null);
    assertEquals("Urceolaria", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("scruposa", pn.getSpecificEpithet());
    assertEquals("clausa", pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Flot.", pn.getAuthorship());
    assertEquals("1849", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.INFRASPECIFIC_NAME, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Cortinarius angulatus B gracilescens Fr. 1838 ", null);
    assertEquals("Cortinarius", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("angulatus", pn.getSpecificEpithet());
//    assertEquals("gracilescens", pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
//    assertEquals("Fr.", pn.getAuthorship());
//    assertEquals("1838", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
//    assertEquals(Rank.INFRASPECIFIC_NAME, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Cyathicula scelobelonium ", null);
    assertEquals("Cyathicula", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("scelobelonium", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Tuber liui A S. Xu 1999 ", null);
    assertEquals("Tuber", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("liui", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("A S. Xu", pn.getAuthorship());
    assertEquals("1999", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Xylaria potentillae A S. Xu ", null);
    assertEquals("Xylaria", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("potentillae", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("A S. Xu", pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Agaricus squamula Berk. & M.A. Curtis 1860 ", null);
    assertEquals("Agaricus", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("squamula", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Berk. & M.A. Curtis", pn.getAuthorship());
    assertEquals("1860", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Peltula coriacea Büdel, Henssen & Wessels 1986 ", null);
    assertEquals("Peltula", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("coriacea", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Büdel, Henssen & Wessels", pn.getAuthorship());
    assertEquals("1986", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Saccharomyces drosophilae anon. ", null);
    assertEquals("Saccharomyces", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("drosophilae", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Abacetus laevicollis de Chaudoir, 1869 ", null);
    assertEquals("Abacetus", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("laevicollis", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("de Chaudoir", pn.getAuthorship());
    assertEquals("1869", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Gastrosericus eremorum von Beaumont 1955 ", null);
    assertEquals("Gastrosericus", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("eremorum", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("von Beaumont", pn.getAuthorship());
    assertEquals("1955", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Cypraeovula (Luponia) amphithales perdentata ", null);
    assertEquals("Cypraeovula", pn.getGenusOrAbove());
    assertEquals("Luponia", pn.getInfraGeneric());
    assertEquals("amphithales", pn.getSpecificEpithet());
    assertEquals("perdentata", pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.INFRASPECIFIC_NAME, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Polyrhachis orsyllus nat musculus Forel, 1901 ", null);
    assertEquals("Polyrhachis", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("orsyllus", pn.getSpecificEpithet());
    assertEquals("musculus", pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Forel", pn.getAuthorship());
    assertEquals("1901", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.NATIO, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Latrodectus 13-guttatus Thorell, 1875 ", null);
    assertEquals("Latrodectus", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("13-guttatus", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Thorell", pn.getAuthorship());
    assertEquals("1875", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Latrodectus 3-guttatus Thorell 1875 ", null);
    assertEquals("Latrodectus", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("3-guttatus", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Thorell", pn.getAuthorship());
    assertEquals("1875", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Arthopyrenia hyalospora (Nyl.) R.C. Harris comb. nov. ", null);
    assertEquals("Arthopyrenia", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("hyalospora", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("R.C. Harris", pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals("Nyl.", pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertEquals("comb. nov.", pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Arthopyrenia hyalospora (Nyl. ex Banker) R.C. Harris ", null);
    assertEquals("Arthopyrenia", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("hyalospora", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("R.C. Harris", pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals("Nyl. ex Banker", pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Arthopyrenia hyalospora Nyl. ex Banker ", null);
    assertEquals("Arthopyrenia", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("hyalospora", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Nyl. ex Banker", pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Glomopsis lonicerae Peck ex C.J. Gould 1945 ", null);
    assertEquals("Glomopsis", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("lonicerae", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Peck ex C.J. Gould", pn.getAuthorship());
    assertEquals("1945", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Acanthobasidium delicatum (Wakef.) Oberw. ex Jülich 1979 ", null);
    assertEquals("Acanthobasidium", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("delicatum", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Oberw. ex Jülich", pn.getAuthorship());
    assertEquals("1979", pn.getYear());
    assertEquals("Wakef.", pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Mycosphaerella eryngii (Fr. ex Duby) Johanson ex Oudem. 1897 ", null);
    assertEquals("Mycosphaerella", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("eryngii", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Johanson ex Oudem.", pn.getAuthorship());
    assertEquals("1897", pn.getYear());
    assertEquals("Fr. ex Duby", pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Mycosphaerella eryngii (Fr. Duby) ex Oudem., 1897 ", null);
    assertEquals("Mycosphaerella", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("eryngii", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("ex Oudem.", pn.getAuthorship());
    assertEquals("1897", pn.getYear());
    assertEquals("Fr. Duby", pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Mycosphaerella eryngii (Fr.ex Duby) ex Oudem. 1897 ", null);
    assertEquals("Mycosphaerella", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("eryngii", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("ex Oudem.", pn.getAuthorship());
    assertEquals("1897", pn.getYear());
    assertEquals("Fr.ex Duby", pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Salmonella werahensis (Castellani) Hauduroy and Ehringer in Hauduroy 1937 ", null);
    assertEquals("Salmonella", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("werahensis", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Hauduroy & Ehringer in Hauduroy", pn.getAuthorship());
    assertEquals("1937", pn.getYear());
    assertEquals("Castellani", pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("×Agropogon P. Fourn. 1934 ", null);
    assertEquals("Agropogon", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("P. Fourn.", pn.getAuthorship());
    assertEquals("1934", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertNull(pn.getRank());
    assertNull(pn.getNomStatus());
    assertEquals(NamePart.GENERIC, pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("xAgropogon P. Fourn. ", null);
    assertEquals("Agropogon", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("P. Fourn.", pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertNull(pn.getRank());
    assertNull(pn.getNomStatus());
    assertEquals(NamePart.GENERIC, pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("XAgropogon P.Fourn. ", null);
    assertEquals("Agropogon", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("P.Fourn.", pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertNull(pn.getRank());
    assertNull(pn.getNomStatus());
    assertEquals(NamePart.GENERIC, pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("× Agropogon ", null);
    assertEquals("Agropogon", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertNull(pn.getRank());
    assertNull(pn.getNomStatus());
    assertEquals(NamePart.GENERIC, pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("x Agropogon ", null);
    assertEquals("Agropogon", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertNull(pn.getRank());
    assertNull(pn.getNomStatus());
    assertEquals(NamePart.GENERIC, pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("X Agropogon ", null);
    assertEquals("Agropogon", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertNull(pn.getRank());
    assertNull(pn.getNomStatus());
    assertEquals(NamePart.GENERIC, pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("X Cupressocyparis leylandii ", null);
    assertEquals("Cupressocyparis", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("leylandii", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertEquals(NamePart.GENERIC, pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("×Heucherella tiarelloides ", null);
    assertEquals("Heucherella", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("tiarelloides", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertEquals(NamePart.GENERIC, pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("xHeucherella tiarelloides ", null);
    assertEquals("Heucherella", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("tiarelloides", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertEquals(NamePart.GENERIC, pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("x Heucherella tiarelloides ", null);
    assertEquals("Heucherella", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("tiarelloides", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertEquals(NamePart.GENERIC, pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("×Agropogon littoralis (Sm.) C. E. Hubb. 1946 ", null);
    assertEquals("Agropogon", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("littoralis", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("C. E. Hubb.", pn.getAuthorship());
    assertEquals("1946", pn.getYear());
    assertEquals("Sm.", pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertEquals(NamePart.GENERIC, pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Asplenium X inexpectatum (E.L. Braun 1940) Morton 1956 ", null);
    assertEquals("Asplenium", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("inexpectatum", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Morton", pn.getAuthorship());
    assertEquals("1956", pn.getYear());
    assertEquals("E.L. Braun", pn.getBracketAuthorship());
    assertEquals("1940", pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertEquals(NamePart.SPECIFIC, pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Mentha ×smithiana R. A. Graham 1949 ", null);
    assertEquals("Mentha", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("smithiana", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("R. A. Graham", pn.getAuthorship());
    assertEquals("1949", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertEquals(NamePart.SPECIFIC, pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Salix ×capreola Andersson (1867) ", null);
    assertEquals("Salix", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("capreola", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals("Andersson", pn.getBracketAuthorship());
    assertEquals("1867", pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertEquals(NamePart.SPECIFIC, pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Salix x capreola Andersson ", null);
    assertEquals("Salix", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("capreola", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Andersson", pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertEquals(NamePart.SPECIFIC, pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Senecio jacquinianus sec. Rchb. ", null);
    assertEquals("Senecio", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("jacquinianus", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Senecio legionensis sensu Samp., non Lange", null);
    assertEquals("Senecio", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("legionensis", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Pseudomonas methanica (Söhngen 1906) sensu. Dworkin and Foster 1956", null);
    assertEquals("Pseudomonas", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("methanica", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals("Söhngen", pn.getBracketAuthorship());
    assertEquals("1906", pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

//    pn = parser.parse("   Asplenium         Xinexpectatum ( E.L.      Braun   1940 )     Morton 1956    ", null);
//    assertEquals("Asplenium", pn.getGenusOrAbove());
//    assertNull(pn.getInfraGeneric());
//    assertEquals("inexpectatum", pn.getSpecificEpithet());
//    assertNull(pn.getInfraSpecificEpithet());
//    assertNull(pn.getCultivarEpithet());
//    assertEquals("Morton", pn.getAuthorship());
//    assertEquals("1956", pn.getYear());
//    assertEquals("E.L. Braun", pn.getBracketAuthorship());
//    assertEquals("1940", pn.getBracketYear());
//    assertNull(pn.getRank());
//    assertNull(pn.getNomStatus());
//    assertEquals(NamePart.SPECIFIC, pn.getNotho());
//    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Eichornia crassipes ( (Martius) ) Solms-Laub. ", null);
    assertEquals("Eichornia", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("crassipes", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Solms-Laub.", pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals("Martius", pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Acarospora cratericola 1929 ", null);
    assertEquals("Acarospora", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("cratericola", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertEquals("1929", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Tridentella tangeroae Bruce, 1987-92", null);
    assertEquals("Tridentella", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("tangeroae", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Bruce", pn.getAuthorship());
    assertEquals("1987-92", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Anthoscopus Cabanis [1851] ", null);
    assertEquals("Anthoscopus", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Cabanis", pn.getAuthorship());
    assertEquals("1851", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertNull(pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.DOUBTFUL, pn.getType());

    pn = parser.parse("Anthoscopus Cabanis [185?] ", null);
    assertEquals("Anthoscopus", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Cabanis", pn.getAuthorship());
    assertEquals("185?", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertNull(pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.DOUBTFUL, pn.getType());

    pn = parser.parse("Anthoscopus Cabanis [1851?] ", null);
    assertEquals("Anthoscopus", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Cabanis", pn.getAuthorship());
    assertEquals("1851?", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertNull(pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.DOUBTFUL, pn.getType());

    pn = parser.parse("Anthoscopus Cabanis [1851] ", null);
    assertEquals("Anthoscopus", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Cabanis", pn.getAuthorship());
    assertEquals("1851", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertNull(pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.DOUBTFUL, pn.getType());

    pn = parser.parse("Anthoscopus Cabanis [1851?] ", null);
    assertEquals("Anthoscopus", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Cabanis", pn.getAuthorship());
    assertEquals("1851?", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertNull(pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.DOUBTFUL, pn.getType());

    pn = parser.parse("Zygaena witti Wiegel [1973] ", null);
    assertEquals("Zygaena", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("witti", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Wiegel", pn.getAuthorship());
    assertEquals("1973", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.DOUBTFUL, pn.getType());

    pn = parser.parse("Morea (Morea) Burt 2342343242 23424322342 23424234 ", null);
    assertEquals("Morea", pn.getGenusOrAbove());
    assertEquals("Morea", pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
//    assertEquals("Burt", pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.INFRAGENERIC_NAME, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
//    assertEquals(NameType.DOUBTFUL, pn.getType());

//    pn = parser.parse("Morea ssjjlajajaj324$33 234243242 ", null);
//    assertEquals("Morea", pn.getGenusOrAbove());
//    assertNull(pn.getInfraGeneric());
//    assertNull(pn.getSpecificEpithet());
//    assertNull(pn.getInfraSpecificEpithet());
//    assertNull(pn.getCultivarEpithet());
//    assertNull(pn.getAuthorship());
//    assertNull(pn.getYear());
//    assertNull(pn.getBracketAuthorship());
//    assertNull(pn.getBracketYear());
//    assertNull(pn.getRank());
//    assertNull(pn.getNomStatus());
//    assertNull(pn.getNotho());
//    assertEquals(NameType.DOUBTFUL, pn.getType());

//    pn = parser.parse("Morea (Morea) burtius 2342343242 23424322342 23424234 ", null);
//    assertEquals("Morea", pn.getGenusOrAbove());
//    assertEquals("Morea", pn.getInfraGeneric());
//    assertEquals("burtius", pn.getSpecificEpithet());
//    assertNull(pn.getInfraSpecificEpithet());
//    assertNull(pn.getCultivarEpithet());
//    assertNull(pn.getAuthorship());
//    assertNull(pn.getYear());
//    assertNull(pn.getBracketAuthorship());
//    assertNull(pn.getBracketYear());
//    assertNull(pn.getRank());
//    assertNull(pn.getNomStatus());
//    assertNull(pn.getNotho());
//    assertEquals(NameType.DOUBTFUL, pn.getType());

//   pn = parser.parse("Moraea spathulata ( (L. f. Klatt ", null);
//   assertEquals("Moraea", pn.getGenusOrAbove());
//   assertNull(pn.getInfraGeneric());
//   assertEquals("spathulata", pn.getSpecificEpithet());
//   assertNull(pn.getInfraSpecificEpithet());
//   assertNull(pn.getCultivarEpithet());
//   assertNull(pn.getAuthorship());
//   assertNull(pn.getYear());
//   assertNull(pn.getBracketAuthorship());
//   assertNull(pn.getBracketYear());
//   assertNull(pn.getRank());
//   assertNull(pn.getNomStatus());
//   assertNull(pn.getNotho());
//   assertEquals(NameType.DOUBTFUL, pn.getType());

    pn = parser.parse("Verpericola megasoma \"Dall\" Pils. ", null);
    assertEquals("Verpericola", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("megasoma", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertEquals("Dall", pn.getCultivarEpithet());
    assertEquals("Pils.", pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.CULTIVAR, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.CULTIVAR, pn.getType());

    pn = parser.parse("Abelia 'Edward Goucher'", null);
    assertEquals("Abelia", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertEquals("Edward Goucher", pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.CULTIVAR, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.CULTIVAR, pn.getType());

    pn = parser.parse("Geranium exili Standl. in R. Knuth", null);
    assertEquals("Geranium", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("exili", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Standl. in R. Knuth", pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Racosperma spirorbe subsp. solandri (Benth.)Pedley", null);
    assertEquals("Racosperma", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("spirorbe", pn.getSpecificEpithet());
    assertEquals("solandri", pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Pedley", pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals("Benth.", pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SUBSPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Ctenotus alacer Storr, 1970 [\"1969\"]", null);
    assertEquals("Ctenotus", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("alacer", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Storr", pn.getAuthorship());
    assertEquals("1970", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.DOUBTFUL, pn.getType());

    pn = parser.parse("Anomalopus truncatus (Peters, 1876 [\"1877\"])", null);
    assertEquals("Anomalopus", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("truncatus", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals("Peters", pn.getBracketAuthorship());
    assertEquals("1876", pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.DOUBTFUL, pn.getType());

    pn = parser.parse("Nostochopis H.C. Wood ex E. Bornet & C. Flahault 1887", null);
    assertEquals("Nostochopis", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("H.C. Wood ex E. Bornet & C. Flahault", pn.getAuthorship());
    assertEquals("1887", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertNull(pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Nostochopis H.C. Wood ex E. Bornet & C. Flahault 1887 (\"1886-1888\")", null);
    assertEquals("Nostochopis", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
//    assertEquals("H.C. Wood ex E. Bornet & C. Flahault", pn.getAuthorship());
//    assertEquals("1887", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertNull(pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.DOUBTFUL, pn.getType());

    pn = parser.parse("Protospongia W.S. Kent 1881 (\"1880-1882\")", null);
    assertEquals("Protospongia", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
//    assertEquals("W.S. Kent", pn.getAuthorship());
//    assertEquals("1881", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertNull(pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.DOUBTFUL, pn.getType());

    pn = parser.parse("Trismegistia monodii Ando, 1973 [1974] ", null);
    assertEquals("Trismegistia", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("monodii", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Ando", pn.getAuthorship());
    assertEquals("1973", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.DOUBTFUL, pn.getType());

    pn = parser.parse("Deyeuxia coarctata Kunth, 1815 [1816] ", null);
    assertEquals("Deyeuxia", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("coarctata", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Kunth", pn.getAuthorship());
    assertEquals("1815", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.DOUBTFUL, pn.getType());

    pn = parser.parse("Proasellus arnautovici (Remy 1932 1941)", null);
    assertEquals("Proasellus", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("arnautovici", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
//    assertEquals("Remy", pn.getBracketAuthorship());
//    assertEquals("1932", pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Lestodiplosis cryphali Kieffer 1894 1901", null);
    assertEquals("Lestodiplosis", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("cryphali", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
//    assertEquals("Kieffer", pn.getAuthorship());
//    assertEquals("1894", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Microrape simplex 1927 1930", null);
    assertEquals("Microrape", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("simplex", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
//    assertEquals("1927", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Matricaria chamomilla L. 1755 1763, non 1753", null);
    assertEquals("Matricaria", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("chamomilla", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
//    assertEquals("L.", pn.getAuthorship());
//    assertEquals("1755", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Hymenoscyphus lutisedus (P. Karst.) anon. ined.", null);
    assertEquals("Hymenoscyphus", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("lutisedus", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals("P. Karst.", pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertEquals("ined.", pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Menegazzia wilsonii (Räsänen) anon.", null);
    assertEquals("Menegazzia", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("wilsonii", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals("Räsänen", pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Sao hispanica R. & E. Richter nom. nud. in Sampelayo 1935", null);
    assertEquals("Sao", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("hispanica", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("R. & E. Richter in Sampelayo", pn.getAuthorship());
    assertEquals("1935", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertEquals("nom. nud.", pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Vaucheria longicaulis var. bengalensis Islam, nom. illeg.", null);
    assertEquals("Vaucheria", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("longicaulis", pn.getSpecificEpithet());
    assertEquals("bengalensis", pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Islam", pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.VARIETY, pn.getRank());
    assertEquals("nom. illeg.", pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Dorataspidae nom. correct", null);
    assertEquals("Dorataspidae", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.FAMILY, pn.getRank());
    assertEquals("nom. correct", pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Ethmosphaeridae nom. transf.", null);
    assertEquals("Ethmosphaeridae", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.FAMILY, pn.getRank());
    assertEquals("nom. transf.", pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Fucus vesiculosus forma volubilis (Goodenough & Woodward) H.T. Powell, nom. inval", null);
    assertEquals("Fucus", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("vesiculosus", pn.getSpecificEpithet());
    assertEquals("volubilis", pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("H.T. Powell", pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals("Goodenough & Woodward", pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.FORM, pn.getRank());
    assertEquals("nom. inval", pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Fucus ramosissimus Oeder, nom. ambig.", null);
    assertEquals("Fucus", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("ramosissimus", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Oeder", pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertEquals("nom. ambig.", pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Myrionema majus Foslie, nom. nov.", null);
    assertEquals("Myrionema", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("majus", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Foslie", pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertEquals("nom. nov.", pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Pithecellobium montanum var. subfalcatum (Zoll. & Moritzi)Miq., nom.rejic.", null);
    assertEquals("Pithecellobium", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("montanum", pn.getSpecificEpithet());
    assertEquals("subfalcatum", pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Miq.", pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals("Zoll. & Moritzi", pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.VARIETY, pn.getRank());
    assertEquals("nom. rejic.", pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Lithothamnion glaciale forma verrucosum (Foslie) Foslie, nom. superfl.", null);
    assertEquals("Lithothamnion", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("glaciale", pn.getSpecificEpithet());
    assertEquals("verrucosum", pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Foslie", pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals("Foslie", pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.FORM, pn.getRank());
    assertEquals("nom. superfl.", pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Anthoceros agrestis var. agrestis Paton nom. cons. prop.", null);
    assertEquals("Anthoceros", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("agrestis", pn.getSpecificEpithet());
    assertEquals("agrestis", pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Paton", pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.VARIETY, pn.getRank());
    assertEquals("nom. cons. prop.", pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Corydalis bulbosa (L.) DC., nom. utique rej.", null);
    assertEquals("Corydalis", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("bulbosa", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("DC.", pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals("L.", pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertEquals("nom. utique rej.", pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Lithobius chibenus Ishii & Tamura (1994)", null);
    assertEquals("Lithobius", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("chibenus", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals("Ishii & Tamura", pn.getBracketAuthorship());
    assertEquals("1994", pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Lithobius elongipes Chamberlin (1952)", null);
    assertEquals("Lithobius", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("elongipes", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals("Chamberlin", pn.getBracketAuthorship());
    assertEquals("1952", pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Rubus rhodanthus W.C.R.Watson (1933)", null);
    assertEquals("Rubus", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("rhodanthus", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals("W.C.R.Watson", pn.getBracketAuthorship());
    assertEquals("1933", pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Platypus bicaudatulus Schedl (1935h) ", null);
    assertEquals("Platypus", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("bicaudatulus", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals("Schedl", pn.getBracketAuthorship());
    assertEquals("1935h", pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Platypus bicaudatulus Schedl (1935) ", null);
    assertEquals("Platypus", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("bicaudatulus", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals("Schedl", pn.getBracketAuthorship());
    assertEquals("1935", pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Sorbus poteriifolia Hand.-Mazz (1933)", null);
    assertEquals("Sorbus", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("poteriifolia", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals("Hand.-Mazz", pn.getBracketAuthorship());
    assertEquals("1933", pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Zophosis persis (Chatanay), 1914 ", null);
    assertEquals("Zophosis", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("persis", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals("Chatanay", pn.getBracketAuthorship());
    assertEquals("1914", pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Malacocarpus schumannianus (Nicolai (1893)) Britton & Rose", null);
    assertEquals("Malacocarpus", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("schumannianus", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Britton & Rose", pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals("Nicolai", pn.getBracketAuthorship());
    assertEquals("1893", pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Agropyron x acutum auct. non (DC.) Roem. & Schult.", null);
    assertEquals("Agropyron", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("acutum", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertEquals(NamePart.SPECIFIC, pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Carex leporina auct. non L. 1753", null);
    assertEquals("Carex", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("leporina", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Salicornia annua auct. auct. Sm., ex descr. non Sm. 1796", null);
    assertEquals("Salicornia", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("annua", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Rubus gremli auct. non Focke", null);
    assertEquals("Rubus", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("gremli", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Rubus carpinifolius auct. auct. non Weihe 1824", null);
    assertEquals("Rubus", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("carpinifolius", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Leucobryum glaucum var. albidum auct. eur. non (P. Beauv. ) Cardot", null);
    assertEquals("Leucobryum", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("glaucum", pn.getSpecificEpithet());
    assertEquals("albidum", pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.VARIETY, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Corynoptera inexpectata auct.", null);
    assertEquals("Corynoptera", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("inexpectata", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Coccinella (Coccinella) divaricata auct.", null);
    assertEquals("Coccinella", pn.getGenusOrAbove());
    assertEquals("Coccinella", pn.getInfraGeneric());
    assertEquals("divaricata", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Rebutia haagei Frič, Schelle, Fric sec.Backeb. & F.M.Knuth", null);
    assertEquals("Rebutia", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("haagei", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Frič, Schelle, Fric", pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Trox haroldi Fisch., sec. Kraatz & Bedel", null);
    assertEquals("Trox", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("haroldi", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Fisch.", pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Trophon sarsi S. Wood, sec. Jeffreys", null);
    assertEquals("Trophon", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("sarsi", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("S. Wood", pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Frustulia aff pararhomboides sec. Metzeltin & Lange-Bertalot", null);
    assertEquals("Frustulia", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("pararhomboides", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.INFORMAL, pn.getType());

    pn = parser.parse("Anabaena affinis Lemmermann", null);
    assertEquals("Anabaena", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("affinis", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Lemmermann", pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Anabaena sp.", null);
    assertEquals("Anabaena", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.INFORMAL, pn.getType());

    pn = parser.parse("Anabaena spec", null);
    assertEquals("Anabaena", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.INFORMAL, pn.getType());

    pn = parser.parse("Anabaena specularia", null);
    assertEquals("Anabaena", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("specularia", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Rasbora cf. elegans", null);
    assertEquals("Rasbora", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("elegans", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.INFORMAL, pn.getType());

    pn = parser.parse("Rasbora aff. elegans", null);
    assertEquals("Rasbora", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("elegans", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.INFORMAL, pn.getType());

    pn = parser.parse("Cathormiocerus inflatiscapus Escalera, M.M. de la 1918", null);
    assertEquals("Cathormiocerus", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("inflatiscapus", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
//    assertEquals("Escalera, M.M. de la", pn.getAuthorship());
//    assertEquals("1918", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Hypnum rutabulum var. campestre Müll. Hal.", null);
    assertEquals("Hypnum", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("rutabulum", pn.getSpecificEpithet());
    assertEquals("campestre", pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Müll. Hal.", pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.VARIETY, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Leptophascum leptophyllum (Müll. Hal.) J. Guerra & Cano", null);
    assertEquals("Leptophascum", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("leptophyllum", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("J. Guerra & Cano", pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals("Müll. Hal.", pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Pompeja psorica Herrich-Schöffer", null);
    assertEquals("Pompeja", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("psorica", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Herrich-Schöffer", pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Gloveria sphingiformis Barnes & McDunnough, 1910", null);
    assertEquals("Gloveria", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("sphingiformis", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Barnes & McDunnough", pn.getAuthorship());
    assertEquals("1910", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Gastromega badia Saalmüller, 1877/78", null);
    assertEquals("Gastromega", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("badia", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Saalmüller", pn.getAuthorship());
    assertEquals("1877/78", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Hasora coulteri Wood-Mason & de Nicóville, 1886", null);
    assertEquals("Hasora", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("coulteri", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Wood-Mason & de Nicóville", pn.getAuthorship());
    assertEquals("1886", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Pithauria uma De Nicóville, 1888", null);
    assertEquals("Pithauria", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("uma", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("De Nicóville", pn.getAuthorship());
    assertEquals("1888", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Lepidostoma quila Bueno-Soria & Padilla-Ramirez, 1981", null);
    assertEquals("Lepidostoma", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("quila", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Bueno-Soria & Padilla-Ramirez", pn.getAuthorship());
    assertEquals("1981", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Dinarthrum inerme McLachlan, 1878", null);
    assertEquals("Dinarthrum", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("inerme", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("McLachlan", pn.getAuthorship());
    assertEquals("1878", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Triplectides tambina Mosely, 1953", null);
    assertEquals("Triplectides", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("tambina", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Mosely", pn.getAuthorship());
    assertEquals("1953", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Oxyothespis sudanensis Giglio-Tos, 1916", null);
    assertEquals("Oxyothespis", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("sudanensis", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Giglio-Tos", pn.getAuthorship());
    assertEquals("1916", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Parastagmatoptera theresopolitana (Giglio-Tos, 1914)", null);
    assertEquals("Parastagmatoptera", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("theresopolitana", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals("Giglio-Tos", pn.getBracketAuthorship());
    assertEquals("1914", pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Oxyothespis nilotica nilotica Giglio-Tos, 1916", null);
    assertEquals("Oxyothespis", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("nilotica", pn.getSpecificEpithet());
    assertEquals("nilotica", pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Giglio-Tos", pn.getAuthorship());
    assertEquals("1916", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.INFRASPECIFIC_NAME, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Photina (Cardioptera) burmeisteri (Westwood, 1889)", null);
    assertEquals("Photina", pn.getGenusOrAbove());
    assertEquals("Cardioptera", pn.getInfraGeneric());
    assertEquals("burmeisteri", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals("Westwood", pn.getBracketAuthorship());
    assertEquals("1889", pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Syngenes inquinatus (Gerstaecker)", null);
    assertEquals("Syngenes", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("inquinatus", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals("Gerstaecker", pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Myrmeleon libelloides var. nigriventris A. Costa", null);
    assertEquals("Myrmeleon", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("libelloides", pn.getSpecificEpithet());
    assertEquals("nigriventris", pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("A. Costa", pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.VARIETY, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Ascalaphus nigripes (van der Weele)", null);
    assertEquals("Ascalaphus", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("nigripes", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals("van der Weele", pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Ascalaphus guttulatus A. Costa", null);
    assertEquals("Ascalaphus", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("guttulatus", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("A. Costa", pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Dichochrysa medogana (C.-K. Yang et al., 1988)", null);
    assertEquals("Dichochrysa", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("medogana", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals("C.-K. Yang et al.", pn.getBracketAuthorship());
    assertEquals("1988", pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Dichochrysa vitticlypea (C.-K. Yang & X.-X. Wang, 1990)", null);
    assertEquals("Dichochrysa", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("vitticlypea", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals("C.-K. Yang & X.-X. Wang", pn.getBracketAuthorship());
    assertEquals("1990", pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Dichochrysa qingchengshana (C.-K. Yang et al., 1992)", null);
    assertEquals("Dichochrysa", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("qingchengshana", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals("C.-K. Yang et al.", pn.getBracketAuthorship());
    assertEquals("1992", pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Colomastix tridentata LeCroy, 1995", null);
    assertEquals("Colomastix", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("tridentata", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("LeCroy", pn.getAuthorship());
    assertEquals("1995", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Sunamphitoe pelagica (H. Milne Edwards, 1830)", null);
    assertEquals("Sunamphitoe", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("pelagica", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals("H. Milne Edwards", pn.getBracketAuthorship());
    assertEquals("1830", pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Brotogeris jugularis (Statius Muller, 1776)", null);
    assertEquals("Brotogeris", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("jugularis", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals("Statius Muller", pn.getBracketAuthorship());
    assertEquals("1776", pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Coracopsis nigra sibilans Milne-Edwards & OuStalet, 1885", null);
    assertEquals("Coracopsis", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("nigra", pn.getSpecificEpithet());
    assertEquals("sibilans", pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Milne-Edwards & OuStalet", pn.getAuthorship());
    assertEquals("1885", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.INFRASPECIFIC_NAME, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Trichoglossus haematodus deplanchii J. Verreaux & Des Murs, 1860", null);
    assertEquals("Trichoglossus", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("haematodus", pn.getSpecificEpithet());
    assertEquals("deplanchii", pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("J. Verreaux & Des Murs", pn.getAuthorship());
    assertEquals("1860", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.INFRASPECIFIC_NAME, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Nannopsittaca dachilleae O'Neill, Munn & Franke, 1991", null);
    assertEquals("Nannopsittaca", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("dachilleae", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("O'Neill, Munn & Franke", pn.getAuthorship());
    assertEquals("1991", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Ramphastos brevis Meyer de Schauensee, 1945", null);
    assertEquals("Ramphastos", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("brevis", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Meyer de Schauensee", pn.getAuthorship());
    assertEquals("1945", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Touit melanonota (Wied-Neuwied, 1820)", null);
    assertEquals("Touit", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("melanonota", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals("Wied-Neuwied", pn.getBracketAuthorship());
    assertEquals("1820", pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Trachyphonus darnaudii (Prevost & Des Murs, 1847)", null);
    assertEquals("Trachyphonus", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("darnaudii", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals("Prevost & Des Murs", pn.getBracketAuthorship());
    assertEquals("1847", pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Anolis porcatus aracelyae Perez-Beato, 1996", null);
    assertEquals("Anolis", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("porcatus", pn.getSpecificEpithet());
    assertEquals("aracelyae", pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Perez-Beato", pn.getAuthorship());
    assertEquals("1996", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.INFRASPECIFIC_NAME, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Anolis gundlachi Peters, 1877", null);
    assertEquals("Anolis", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("gundlachi", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Peters", pn.getAuthorship());
    assertEquals("1877", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Anolis marmoratus girafus Lazell, 1964", null);
    assertEquals("Anolis", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("marmoratus", pn.getSpecificEpithet());
    assertEquals("girafus", pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Lazell", pn.getAuthorship());
    assertEquals("1964", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.INFRASPECIFIC_NAME, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Chorististium maculatum (non Bloch 1790)", null);
    assertEquals("Chorististium", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("maculatum", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Pikea lunulata (non Guichenot 1864)", null);
    assertEquals("Pikea", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("lunulata", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Luzonichthys taeniatus Randall & McCosker, 1992", null);
    assertEquals("Luzonichthys", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("taeniatus", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Randall & McCosker", pn.getAuthorship());
    assertEquals("1992", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Puntius stoliczkae", null);
    assertEquals("Puntius", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("stoliczkae", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Puntius arulius subsp. tambraparniei", null);
    assertEquals("Puntius", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("arulius", pn.getSpecificEpithet());
    assertEquals("tambraparniei", pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SUBSPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Actinia stellula Hemprich and Ehrenberg 1834", null);
    assertEquals("Actinia", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("stellula", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Hemprich & Ehrenberg", pn.getAuthorship());
    assertEquals("1834", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Anemonia vagans (Less.) Milne Edw.", null);
    assertEquals("Anemonia", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("vagans", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Milne Edw.", pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals("Less.", pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Epiactis fecunda (Verrill, 1899b)", null);
    assertEquals("Epiactis", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("fecunda", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals("Verrill", pn.getBracketAuthorship());
    assertEquals("1899b", pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Leptodictyum (Schimp.) Warnst.", null);
    assertEquals("Leptodictyum", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Warnst.", pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals("Schimp.", pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertNull(pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Pseudocurimata Fernandez-Yepez, 1948", null);
    assertEquals("Pseudocurimata", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Fernandez-Yepez", pn.getAuthorship());
    assertEquals("1948", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertNull(pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Pseudophorellia da Costa Lima, 1934", null);
    assertEquals("Pseudophorellia", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("da Costa Lima", pn.getAuthorship());
    assertEquals("1934", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertNull(pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Hershkovitzia Guimarães & d'Andretta, 1957", null);
    assertEquals("Hershkovitzia", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Guimarães & d'Andretta", pn.getAuthorship());
    assertEquals("1957", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertNull(pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Plectocolea (Mitten) Mitten, 1873", null);
    assertEquals("Plectocolea", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Mitten", pn.getAuthorship());
    assertEquals("1873", pn.getYear());
    assertEquals("Mitten", pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertNull(pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Discoporella d'Orbigny, 1852", null);
    assertEquals("Discoporella", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("d'Orbigny", pn.getAuthorship());
    assertEquals("1852", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertNull(pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Acripeza Guérin-Ménéville, 1838", null);
    assertEquals("Acripeza", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Guérin-Ménéville", pn.getAuthorship());
    assertEquals("1838", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertNull(pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Subpeltonotus Swaraj Ghai, Kailash Chandra & Ramamurthy, 1988", null);
    assertEquals("Subpeltonotus", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Swaraj Ghai, Kailash Chandra & Ramamurthy", pn.getAuthorship());
    assertEquals("1988", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertNull(pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Boettcherimima De Souza Lopes, 1950", null);
    assertEquals("Boettcherimima", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("De Souza Lopes", pn.getAuthorship());
    assertEquals("1950", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertNull(pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Surnicou Des Murs, 1853", null);
    assertEquals("Surnicou", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Des Murs", pn.getAuthorship());
    assertEquals("1853", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertNull(pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Cristocypridea Hou MS., 1977", null);
    assertEquals("Cristocypridea", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Hou MS.", pn.getAuthorship());
    assertEquals("1977", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertNull(pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Lecythis coriacea DC.", null);
    assertEquals("Lecythis", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("coriacea", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("DC.", pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Anhuiphyllum Yu Xueguang, 1991", null);
    assertEquals("Anhuiphyllum", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Yu Xueguang", pn.getAuthorship());
    assertEquals("1991", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertNull(pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Zonosphaeridium minor Tian Chuanrong, 1983", null);
    assertEquals("Zonosphaeridium", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("minor", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Tian Chuanrong", pn.getAuthorship());
    assertEquals("1983", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Oscarella microlobata Muricy, Boury-Esnault, Bézac & Vacelet, 1996", null);
    assertEquals("Oscarella", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("microlobata", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Muricy, Boury-Esnault, Bézac & Vacelet", pn.getAuthorship());
    assertEquals("1996", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Neoarctus primigenius Grimaldi de Zio, D'Abbabbo Gallo & Morone de Lucia, 1992", null);
    assertEquals("Neoarctus", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("primigenius", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Grimaldi de Zio, D'Abbabbo Gallo & Morone de Lucia", pn.getAuthorship());
    assertEquals("1992", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Phaonia wenshuiensis Zhang, Zhao Bin & Wu, 1985", null);
    assertEquals("Phaonia", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("wenshuiensis", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Zhang, Zhao Bin & Wu", pn.getAuthorship());
    assertEquals("1985", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Heteronychia (Eupierretia) helanshanensis Han, Zhao-Gan & Ye, 1985", null);
    assertEquals("Heteronychia", pn.getGenusOrAbove());
    assertEquals("Eupierretia", pn.getInfraGeneric());
    assertEquals("helanshanensis", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Han, Zhao-Gan & Ye", pn.getAuthorship());
    assertEquals("1985", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Solanophila karisimbica ab. fulvicollis Mader, 1941", null);
    assertEquals("Solanophila", pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertEquals("karisimbica", pn.getSpecificEpithet());
    assertEquals("fulvicollis", pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Mader", pn.getAuthorship());
    assertEquals("1941", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.ABERRATION, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Tortrix (Heterognomon) aglossana Kennel, 1899", null);
    assertEquals("Tortrix", pn.getGenusOrAbove());
    assertEquals("Heterognomon", pn.getInfraGeneric());
    assertEquals("aglossana", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Kennel", pn.getAuthorship());
    assertEquals("1899", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Leptochilus (Neoleptochilus) beaumonti Giordani Soika, 1953", null);
    assertEquals("Leptochilus", pn.getGenusOrAbove());
    assertEquals("Neoleptochilus", pn.getInfraGeneric());
    assertEquals("beaumonti", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getCultivarEpithet());
    assertEquals("Giordani Soika", pn.getAuthorship());
    assertEquals("1953", pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getNomStatus());
    assertNull(pn.getNotho());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

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
    assertParsedParts("Cribbia pendula la Croix & P.J.Cribb", NameType.SCIENTIFIC, "Cribbia", "pendula", null, Rank.SPECIES, "la Croix & P.J.Cribb", null);
    assertParsedParts("Cribbia pendula le Croix & P.J.Cribb", NameType.SCIENTIFIC, "Cribbia", "pendula", null, Rank.SPECIES, "le Croix & P.J.Cribb", null);
    assertParsedParts("Cribbia pendula de la Croix & le P.J.Cribb", NameType.SCIENTIFIC, "Cribbia", "pendula", null, Rank.SPECIES, "de la Croix & le P.J.Cribb", null);
    assertParsedParts("Cribbia pendula Croix & de le P.J.Cribb", NameType.SCIENTIFIC, "Cribbia", "pendula", null, Rank.SPECIES, "Croix & de le P.J.Cribb", null);
  }

  /**
   * https://github.com/gbif/name-parser/issues/5
   */
  @Test
  public void testVulpes() throws Exception {
    assertParsedParts("Vulpes vulpes sp. silaceus Miller, 1907", NameType.DOUBTFUL, "Vulpes", "vulpes", "silaceus", Rank.SUBSPECIES, "Miller", "1907");
  }

  @Test
  public void testChineseAuthors() throws Exception {
    assertParsedParts("Abaxisotima acuminata (Wang & Liu, 1996)", NameType.SCIENTIFIC, "Abaxisotima", "acuminata", null, null, null, null, "Wang & Liu", "1996");
    assertParsedParts("Abaxisotima acuminata (Wang, Yuwen & Xian-wei Liu, 1996)", NameType.SCIENTIFIC, "Abaxisotima", "acuminata", null, null, null, null, "Wang, Yuwen & Xian-wei Liu", "1996");

    assertParsedParts("Abaxisotima bicolor (Liu, Zheng & Xi, 1991)", NameType.SCIENTIFIC, "Abaxisotima", "bicolor", null, null, null, null, "Liu, Zheng & Xi", "1991");
    assertParsedParts("Abaxisotima bicolor (Liu, Xian-wei, Z. Zheng & G. Xi, 1991)", NameType.SCIENTIFIC, "Abaxisotima", "bicolor", null, null, null, null, "Liu, Xian-wei, Z. Zheng & G. Xi", "1991");
  }

  @Test
  public void testMicrobialRanks2() throws Exception {
    assertParsedMicrobial("Puccinia graminis f. sp. avenae",
                          NameType.SCIENTIFIC, "Puccinia", "graminis", "avenae", Rank.FORMA_SPECIALIS);

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
    assertParsedParts("Panthera leo leo (Linnaeus, 1758)", NameType.SCIENTIFIC, "Panthera", "leo", "leo", Rank.INFRASPECIFIC_NAME, null, null, "Linnaeus", "1758");
    assertParsedParts("Abies alba subsp. alba L.", NameType.SCIENTIFIC, "Abies", "alba", "alba", Rank.SUBSPECIES, "L.");
    //TODO: improve nameparser to extract autonym authors, http://dev.gbif.org/issues/browse/GBIFCOM-10
    //    assertParsedParts("Abies alba L. subsp. alba", "Abies", "alba", "alba", "subsp.", "L.");
    // this is a wrong name! autonym authors are the species authors, so if both are given they must be the same!
    //    assertParsedParts("Abies alba L. subsp. alba Mill.", "Abies", "alba", "alba", "subsp.", "L.");
  }

  @Test
  public void testNameParserFull() throws Exception {
    assertParsedParts("Abies alba L.", NameType.SCIENTIFIC, "Abies", "alba", null, null, "L.");
    assertParsedParts("Abies alba var. kosovo", "Abies", "alba", "kosovo", Rank.VARIETY);
    assertParsedParts("Abies alba subsp. parafil", "Abies", "alba", "parafil", Rank.SUBSPECIES);
    assertParsedParts("Abies   alba L. ssp. parafil DC.", "Abies", "alba", "parafil", Rank.SUBSPECIES, "DC.");

    assertParsedParts("Nuculoidea behrens var.christoph Williams & Breger [1916]  ", NameType.DOUBTFUL, "Nuculoidea", "behrens",
        "christoph", Rank.VARIETY, "Williams & Breger", "1916");
    assertParsedParts(" Nuculoidea Williams et  Breger 1916  ", NameType.SCIENTIFIC, "Nuculoidea", null, null, null, "Williams & Breger", "1916");

    assertParsedParts("Nuculoidea behrens v.christoph Williams & Breger [1916]  ", NameType.DOUBTFUL, "Nuculoidea", "behrens", "christoph",
        Rank.VARIETY, "Williams & Breger", "1916");
    assertParsedParts("Nuculoidea behrens var.christoph Williams & Breger [1916]  ", NameType.DOUBTFUL, "Nuculoidea", "behrens",
        "christoph", Rank.VARIETY, "Williams & Breger", "1916");
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
    assertParsedParts("Caulerpa cupressoides forma nuda", "Caulerpa", "cupressoides", "nuda", Rank.FORM, null);
    assertParsedParts("Agalinis purpurea (L.) Briton var. borealis (Berg.) Peterson 1987", null, "Agalinis", "purpurea",
        "borealis", Rank.VARIETY, "Peterson", "1987", "Berg.", null);
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
        "nigriventris", Rank.VARIETY, "A. Costa", "1855");
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
        "fulvicollis", Rank.ABERRATION, "Mader", "1941");
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
    assertParsedInfrageneric("Maxillaria sect. Multiflorae Christenson", null, "Maxillaria", "Multiflorae", Rank.SECTION, "Christenson", null, null, null);

    assertParsedParts("Maxillaria allenii L.O.Williams in Woodson & Schery", "Maxillaria", "allenii", null, null,
        "L.O.Williams in Woodson & Schery");
    assertParsedParts("Masdevallia strumosa P.Ortiz & E.Calderón", "Masdevallia", "strumosa", null, null,
        "P.Ortiz & E.Calderón");
    assertParsedParts("Neobisium (Neobisium) carcinoides balcanicum Hadži 1937", NameType.SCIENTIFIC, "Neobisium", "Neobisium",
        "carcinoides", "balcanicum", null, "Hadži", "1937", null, null);
    assertParsedParts("Nomascus concolor subsp. lu Delacour, 1951", "Nomascus", "concolor", "lu", Rank.SUBSPECIES, "Delacour",
        "1951");
    assertParsedParts("Polygonum subgen. Bistorta (L.) Zernov", NameType.SCIENTIFIC, "Polygonum", "Bistorta",
        null, null, Rank.SUBGENUS, "Zernov", null, "L.", null);
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
    assertParsedParts(" Puntius arulius subsp. tambraparniei (non Silas 1954)", "Puntius", "arulius", "tambraparniei", Rank.SUBSPECIES, null);
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
    assertStrain("Methylocystis sp. M6", NameType.INFORMAL, "Methylocystis", null, null, Rank.SPECIES, "M6");
    assertStrain("Advenella kashmirensis W13003", NameType.INFORMAL, "Advenella", "kashmirensis", null, null, "W13003");
    assertStrain("Garra cf. dampaensis M23", NameType.INFORMAL, "Garra", "dampaensis", null, null, "M23");
    assertStrain("Sphingobium lucknowense F2", NameType.INFORMAL, "Sphingobium", "lucknowense", null, null, "F2");
    assertStrain("Pseudomonas syringae pv. atrofaciens LMG 5095T", NameType.INFORMAL, "Pseudomonas", "syringae", "atrofaciens", Rank.PATHOVAR, "LMG 5095T");
  }

  /**
   * Detect BIN and SH numbers
   */
  @Test
  public void testOTUNames() throws Exception {
    assertOTU("SH460441.07FU");
    assertOTU("sh502517.07fu");
    assertOTU("BOLD:AAA1244");
    assertOTU("BOLD:AAA0001");
    assertOTU("BOLDAAA0001");
  }

  private void assertOTU(String name) throws UnparsableException {
    try {
      parser.parse(name, null);
      fail("OTU parsing should throw UnparsableException ");
    } catch (UnparsableException e) {
      assertEquals(NameType.OTU, e.type);
    }

    ParsedName pn = parser.parseQuietly(name, null);
    assertEquals(name, pn.getScientificName());
    assertEquals(NameType.OTU, pn.getType());
    assertNull(pn.canonicalNameWithMarker());
    assertNull(pn.getRank());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertNull(pn.getBracketAuthorship());
    assertNull(pn.getBracketYear());
    assertNull(pn.getGenusOrAbove());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getStrain());
  }

  /**
   * Test names highlighted in the GNParser paper:
   * https://bmcbioinformatics.biomedcentral.com/articles/10.1186/s12859-017-1663-3
   */
  @Test
  public void testGnparserPaper() throws Exception {
    assertParsedParts("Myosorex muricauda (Miller, 1900).", NameType.SCIENTIFIC, "Myosorex", "muricauda", null, Rank.SPECIES, null, null, "Miller", "1900");
    assertParsedParts("Campylium gollanii C. M?ller ex Vohra 1970 [1972] ", NameType.DOUBTFUL, "Campylium", "gollanii", null, Rank.SPECIES, "C. M?ller ex Vohra", "1970", null, null);
    assertParsedParts("Hieracium nobile subsp. perclusum (Arv. -Touv. ) O. Bolòs & Vigo", NameType.SCIENTIFIC, "Hieracium", "nobile", "perclusum", Rank.SUBSPECIES, "O. Bolòs & Vigo", null, "Arv.-Touv.", null);
  }

  @Test
  public void testPathovars() throws Exception {
    assertParsedParts("Xanthomonas campestris pv. citri (ex Hasse 1915) Dye 1978", NameType.SCIENTIFIC, "Xanthomonas", "campestris", "citri", Rank.PATHOVAR, "Dye", "1978", "ex Hasse", "1915");
    assertParsedParts("Xanthomonas campestris pv. oryzae (Xco)", NameType.SCIENTIFIC, "Xanthomonas", "campestris", "oryzae", Rank.PATHOVAR, null, null, "Xco", null);
    assertParsedParts("Streptococcus dysgalactiae (ex Diernhofer 1932) Garvie et al. 1983", NameType.SCIENTIFIC, "Streptococcus", "dysgalactiae", null, null, "Garvie et al.", "1983", "ex Diernhofer", "1932");
  }

  @Test
  public void testImprintYear() throws Exception {
    assertParsedParts(" Pompeja psorica Herrich-Schöffer [1854]", NameType.DOUBTFUL, "Pompeja", "psorica", null, Rank.SPECIES, "Herrich-Schöffer", "1854", null, null);
    assertParsedParts(" Syngenes inquinatus (Gerstaecker, [1885])", NameType.DOUBTFUL, "Syngenes", "inquinatus", null, Rank.SPECIES, null, null, "Gerstaecker", "1885");
    assertParsedParts(" Myrmeleon libelloides var. nigriventris A. Costa, [1855]", NameType.DOUBTFUL, "Myrmeleon", "libelloides", "nigriventris", Rank.VARIETY, "A. Costa", "1855");
    assertParsedParts("Ascalaphus nigripes (van der Weele, [1909])", NameType.DOUBTFUL, "Ascalaphus", "nigripes", null, Rank.SPECIES, null, null, "van der Weele", "1909");
    assertParsedParts(" Ascalaphus guttulatus A. Costa, [1855]", NameType.DOUBTFUL, "Ascalaphus", "guttulatus", null, Rank.SPECIES, "A. Costa", "1855");
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
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
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
  public void testAraneae() throws Exception {
    ParsedName pn = parser.parse("Araneae", Rank.ORDER);
    assertEquals("Araneae", pn.getGenusOrAbove());
    assertNull(pn.getAuthorship());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getInfraSpecificEpithet());
    assertEquals(Rank.ORDER, pn.getRank());
  }

  @Test
  public void testInCitation() throws Exception {
    ParsedName pn = parser.parse("Codonellina Canu & Bassler in Bassler, 1934", Rank.GENUS);
    assertEquals("Codonellina", pn.getGenusOrAbove());
    assertEquals("Canu & Bassler in Bassler", pn.getAuthorship());
    assertEquals("1934", pn.getYear());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getInfraSpecificEpithet());
    assertEquals(Rank.GENUS, pn.getRank());
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
  public void testAggregates() throws Exception {
    ParsedName pn = parser.parse("Taraxacum erythrospermum agg.", Rank.SECTION);
    assertEquals("Taraxacum", pn.getGenusOrAbove());
    assertEquals("erythrospermum", pn.getSpecificEpithet());
    assertNull(pn.getInfraGeneric());
      assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals(Rank.SPECIES_AGGREGATE, pn.getRank());

    pn = parser.parse("Taraxacum erythrospermum s.l.", Rank.SPECIES);
    assertEquals("Taraxacum", pn.getGenusOrAbove());
    assertEquals("erythrospermum", pn.getSpecificEpithet());
    assertNull(pn.getInfraGeneric());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    //TODO: decide if we want this as an aggregate or in the senus field
    assertEquals("s.l.", pn.getSensu());
    //assertEquals(Rank.SPECIES_AGGREGATE, pn.getRank());
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

    cn = parser.parse("Callideriphus flavicollis morph. reductus Fuchs 1961", null);
    assertEquals(Rank.MORPH, cn.getRank());

    ParsedName pn = parser.parse("Euphrasia rostkoviana Hayne subvar. campestris (Jord.) Hartl", null);
    assertEquals(Rank.SUBVARIETY, pn.getRank());
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

  private ParsedName assertParsedInfrageneric(String name, Rank inputRank, String genus, String infrageneric, Rank rankMarker, String basAuthor, String basYear) throws UnparsableException {
    return assertParsedParts(name, inputRank, null, genus, infrageneric, null, null, rankMarker, null, null, null, basAuthor, basYear, null, null);
  }

  private ParsedName assertParsedInfrageneric(String name, Rank inputRank, String genus, String infrageneric, Rank rankMarker, String author, String year, String basAuthor, String basYear) throws UnparsableException {
    return assertParsedParts(name, inputRank, null, genus, infrageneric, null, null, rankMarker, null, author, year, basAuthor, basYear, null, null);
  }

  private ParsedName assertParsedParts(String name, String genus, String infrageneric, String epithet, String infraepithet,
                                       Rank rank, NamePart notho, String author, String year, String basAuthor, String basYear, String nomStatus) throws UnparsableException {
    return assertParsedParts(name, null, null, genus, infrageneric, epithet, infraepithet, rank, notho, author, year, basAuthor, basYear, nomStatus, null);
  }

  private ParsedName assertParsedParts(String name, Rank inputRank, NameType type, String genus, String infrageneric, String epithet, String infraepithet,
                                       Rank rank, NamePart notho, String author, String year, String basAuthor, String basYear, String nomStatus, String strain)
      throws UnparsableException {
    ParsedName pn = parser.parse(name, inputRank);
    if (type != null) {
      assertEquals(type, pn.getType());
    }
    assertEquals(genus, pn.getGenusOrAbove());
    assertEquals(infrageneric, pn.getInfraGeneric());
    assertEquals(epithet, pn.getSpecificEpithet());
    assertEquals(infraepithet, pn.getInfraSpecificEpithet());
    if (rank != null) {
      assertEquals(rank, pn.getRank());
    }
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

  private ParsedName assertParsedName(String name, NameType type, String genus, String epithet, String infraepithet, Rank rank) {
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

  private ParsedName assertParsedParts(String name, NameType type, String genus, String infrageneric, String epithet, String infraepithet, Rank rank, String author, String year, String basAuthor, String basYear) throws UnparsableException {
    return assertParsedParts(name, null, type, genus, infrageneric, epithet, infraepithet, rank, null, author, year, basAuthor, basYear, null, null);
  }

  private ParsedName assertParsedParts(String name, NameType type, String genus, String epithet, String infraepithet, Rank rank, String author, String year, String basAuthor, String basYear) throws UnparsableException {
    return assertParsedParts(name, null, type, genus, null, epithet, infraepithet, rank, null, author, year, basAuthor, basYear, null, null);
  }

  private ParsedName assertParsedParts(String name, String genus, String epithet, String infraepithet, Rank rank) throws UnparsableException {
    return assertParsedParts(name, null, genus, null, epithet, infraepithet, rank, null, null, null, null);
  }

  private ParsedName assertParsedParts(String name, String genus, String epithet, String infraepithet, Rank rank, String author) throws UnparsableException {
    return assertParsedParts(name, null, genus, null, epithet, infraepithet, rank, author, null, null, null);
  }

  private ParsedName assertParsedParts(String name, NameType type, String genus, String epithet, String infraepithet, Rank rank, String author) throws UnparsableException {
    return assertParsedParts(name, type, genus, null, epithet, infraepithet, rank, author, null, null, null);
  }

  private ParsedName assertParsedParts(String name, String genus, String epithet, String infraepithet, Rank rank, String author, String year) throws UnparsableException {
    return assertParsedParts(name, null, genus, null, epithet, infraepithet, rank, author, year, null, null);
  }

  private ParsedName assertParsedParts(String name, String genus, String epithet, String infraepithet, NamePart notho, Rank rank, String author, String year) throws UnparsableException {
    return assertParsedParts(name, null, null, genus, null, epithet, infraepithet, rank, notho, author, year, null, null, null, null);
  }

  private ParsedName assertParsedParts(String name, NameType type, String genus, String epithet, String infraepithet, Rank rank, String author, String year) throws UnparsableException {
    return assertParsedParts(name, type, genus, null, epithet, infraepithet, rank, author, year, null, null);
  }

  private ParsedName assertStrain(String name, NameType type, String genus, String epithet, String infraepithet, Rank rank, String strain) throws UnparsableException {
    return assertParsedParts(name, null, type, genus, null, epithet, infraepithet, rank, null, null, null, null, null, null, strain);
  }

  @Test
  public void testInfragenericRanks() throws Exception {
    assertParsedInfrageneric("Bodotria (Vertebrata)", Rank.SUBGENUS, "Bodotria", "Vertebrata", Rank.SUBGENUS, null, null);

    assertParsedInfrageneric("Bodotria (Goodsir)", null, "Bodotria", null, null, "Goodsir", null);
    assertParsedInfrageneric("Bodotria (Goodsir)", Rank.SUBGENUS, "Bodotria", "Goodsir", Rank.SUBGENUS, null, null);
    assertParsedInfrageneric("Bodotria (J.Goodsir)", Rank.SUBGENUS, "Bodotria", null, Rank.SUBGENUS, "J.Goodsir", null);

    assertParsedInfrageneric("Latrunculia (Biannulata)", Rank.SUBGENUS, "Latrunculia", "Biannulata", Rank.SUBGENUS, null, null);

    assertParsedParts("Saperda (Saperda) candida m. bipunctata Breuning, 1952", null, NameType.SCIENTIFIC, "Saperda", "Saperda", "candida",
        "bipunctata", Rank.MORPH, null, "Breuning", "1952", null, null, null, null);

    assertParsedParts("Carex section Acrocystis", null, NameType.SCIENTIFIC, "Carex", "Acrocystis", null, null, Rank.SECTION, null, null, null, null, null, null, null);

    assertParsedParts("Juncus subgenus Alpini", null, NameType.SCIENTIFIC, "Juncus", "Alpini", null, null, Rank.SUBGENUS, null, null, null, null, null, null, null);

    assertParsedParts("Solidago subsection Triplinervae", null, NameType.SCIENTIFIC, "Solidago", "Triplinervae", null, null, Rank.SUBSECTION, null, null, null, null, null, null, null);

    assertParsedParts("Eleocharis series Maculosae", null, NameType.SCIENTIFIC, "Eleocharis", "Maculosae", null, null, Rank.SERIES, null, null, null, null, null, null, null);

    assertParsedParts("Hylaeus (Alfkenylaeus) Snelling, 1985", Rank.SECTION, NameType.SCIENTIFIC, "Hylaeus", "Alfkenylaeus", null, null, Rank.SECTION, null, "Snelling", "1985", null, null, null, null);
  }

  @Test
  public void testInfragenericRanks2() throws Exception {
    assertParsedInfrageneric("Bodotria (Vertebrata)", null, "Bodotria", "Vertebrata", null, null, null);

    assertParsedInfrageneric("Bodotria (Goodsir)", null, "Bodotria", null, null, "Goodsir", null);
    assertParsedInfrageneric("Bodotria (J.Goodsir)", null, "Bodotria", null, null, "J.Goodsir", null);

    assertParsedInfrageneric("Latrunculia (Biannulata)", null, "Latrunculia", "Biannulata", null, null, null);

    assertParsedParts("Saperda (Saperda) candida m. bipunctata Breuning, 1952", null, NameType.SCIENTIFIC, "Saperda", "Saperda", "candida",
        "bipunctata", Rank.MORPH, null, "Breuning", "1952", null, null, null, null);

    assertParsedParts("Carex section Acrocystis", null, NameType.SCIENTIFIC, "Carex", "Acrocystis", null, null, Rank.SECTION, null, null, null, null, null, null, null);

    assertParsedParts("Juncus subgenus Alpini", null, NameType.SCIENTIFIC, "Juncus", "Alpini", null, null, Rank.SUBGENUS, null, null, null, null, null, null, null);

    assertParsedParts("Solidago subsection Triplinervae", null, NameType.SCIENTIFIC, "Solidago", "Triplinervae", null, null, Rank.SUBSECTION, null, null, null, null, null, null, null);

    assertParsedParts("Eleocharis series Maculosae", null, NameType.SCIENTIFIC, "Eleocharis", "Maculosae", null, null, Rank.SERIES, null, null, null, null, null, null, null);

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
    assertParsedParts("Celtis sinensis var. nervosa (Hemsl.) F.Y.Lu, C.H.Ou, Y.C.Chen, Y.S.Chi, K.C.Lu & amp; Y.H.Tseng", null, "Celtis", "sinensis", "nervosa", Rank.VARIETY, "F.Y.Lu, C.H.Ou, Y.C.Chen, Y.S.Chi, K.C.Lu & Y.H.Tseng", null, "Hemsl.", null);
    assertParsedParts("Salix taiwanalpina var. chingshuishanensis (S.S.Ying) F.Y.Lu, C.H.Ou, Y.C.Chen, Y.S.Chi, K.C.Lu & Y.H.Tseng ", null, "Salix", "taiwanalpina", "chingshuishanensis", Rank.VARIETY, "F.Y.Lu, C.H.Ou, Y.C.Chen, Y.S.Chi, K.C.Lu & Y.H.Tseng", null, "S.S.Ying", null);
    assertParsedParts("Salix taiwanalpina var. chingshuishanensis (S.S.Ying) F.Y.Lu, C.H.Ou, Y.C.Chen, Y.S.Chi, K.C.Lu & amp  Y.H.Tseng ", null, "Salix", "taiwanalpina", "chingshuishanensis", Rank.VARIETY, "F.Y.Lu, C.H.Ou, Y.C.Chen, Y.S.Chi, K.C.Lu & Y.H.Tseng", null, "S.S.Ying", null);
    assertParsedParts("Salix morrisonicola var. takasagoalpina (Koidz.) F.Y.Lu, C.H.Ou, Y.C.Chen, Y.S.Chi, K.C.Lu & amp; Y.H.Tseng", null, "Salix", "morrisonicola", "takasagoalpina", Rank.VARIETY, "F.Y.Lu, C.H.Ou, Y.C.Chen, Y.S.Chi, K.C.Lu & Y.H.Tseng", null, "Koidz.", null);
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
    assertEquals(Rank.VARIETY, pn.getRank());
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
    assertEquals(Rank.VARIETY, pn.getRank());

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
    assertEquals(Rank.SPECIES, pn.getRank());
    assertEquals("sp. nov. ined.", pn.getNomStatus());

    pn = parser.parse("Stebbinsoseris gen. nov.", null);
    assertEquals("Stebbinsoseris", pn.getGenusOrAbove());
    assertNull(pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals(Rank.GENUS, pn.getRank());
    assertEquals("gen. nov.", pn.getNomStatus());

    pn = parser.parse("Astelia alpina var. novae-hollandiae", null);
    assertEquals("Astelia", pn.getGenusOrAbove());
    assertEquals("alpina", pn.getSpecificEpithet());
    assertEquals("novae-hollandiae", pn.getInfraSpecificEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals(Rank.VARIETY, pn.getRank());
    assertNull(pn.getNomStatus());

    pn = parser.parse("Astelia alpina var. november ", null);
    assertEquals("Astelia", pn.getGenusOrAbove());
    assertEquals("alpina", pn.getSpecificEpithet());
    assertEquals("november", pn.getInfraSpecificEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals(Rank.VARIETY, pn.getRank());
    assertNull(pn.getNomStatus());

    pn = parser.parse("Astelia alpina subsp. november ", null);
    assertEquals("Astelia", pn.getGenusOrAbove());
    assertEquals("alpina", pn.getSpecificEpithet());
    assertEquals("november", pn.getInfraSpecificEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals(Rank.SUBSPECIES, pn.getRank());
    assertNull(pn.getNomStatus());

    pn = parser.parse("Astelia alpina november ", null);
    assertEquals("Astelia", pn.getGenusOrAbove());
    assertEquals("alpina", pn.getSpecificEpithet());
    assertEquals("november", pn.getInfraSpecificEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals(Rank.INFRASPECIFIC_NAME, pn.getRank());
    assertNull(pn.getNomStatus());

    pn = parser.parse("Myrionema majus Foslie, nom. nov.", null);
    assertEquals("Myrionema", pn.getGenusOrAbove());
    assertEquals("majus", pn.getSpecificEpithet());
    assertNull(pn.getInfraSpecificEpithet());
    assertEquals("Foslie", pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertEquals("nom. nov.", pn.getNomStatus());

  }

  @Test
  public void testNormalizeName() {
    assertEquals("Nuculoidea Williams et Breger, 1916", GBIFNameParser.normalize("Nuculoidea Williams et  Breger 1916  "));
    assertEquals("Nuculoidea behrens var. christoph Williams & Breger [1916]",
        GBIFNameParser.normalize("Nuculoidea behrens var.christoph Williams & Breger [1916]  "));
    assertEquals("Nuculoidea behrens var. christoph Williams & Breger [1916]",
        GBIFNameParser.normalize("Nuculoidea behrens var.christoph Williams & Breger [1916]  "));
    assertEquals(GBIFNameParser.normalize("Nuculoidea Williams & Breger, 1916  "),
        GBIFNameParser.normalize("Nuculoidea   Williams& Breger, 1916"));
    assertEquals("Asplenium ×inexpectatum (E.L. Braun, 1940) Morton (1956)",
        GBIFNameParser.normalize("Asplenium X inexpectatum (E.L. Braun 1940)Morton (1956) "));
    assertEquals("×Agropogon", GBIFNameParser.normalize(" × Agropogon"));
    assertEquals("Salix ×capreola Andersson", GBIFNameParser.normalize("Salix × capreola Andersson"));
    assertEquals("Leucanitis roda Herrich-Schäffer (1851), 1845",
        GBIFNameParser.normalize("Leucanitis roda Herrich-Schäffer (1851) 1845"));

    assertEquals("Huaiyuanella Xing, Yan & Yin, 1984", GBIFNameParser.normalize("Huaiyuanella Xing, Yan&Yin, 1984"));

  }

  @Test
  public void testScientificNameStaysVerbatim() throws UnparsableException {
    for (String name : Lists.newArrayList(
        "Huaiyuanella Xing, Yan&Yin, 1984",
        "Abies alba agg.",
        "Trechus (Trechus) mogul Belousov & Kabak, 2001",
        "Trechus (Trechus) merditanus Apfelbeck, 1906"
    )) {
      assertEquals(name, parser.parseQuietly(name).getScientificName());
      assertEquals(name, parser.parseQuietly(name, Rank.SPECIES).getScientificName());
      assertEquals(name, parser.parseQuietly(name, Rank.INFRASPECIFIC_NAME).getScientificName());
      assertEquals(name, parser.parse(name).getScientificName());
    }
  }

  @Test
  public void testNormalizeStrongName() {
    assertEquals("Nuculoidea Williams & Breger, 1916",
        GBIFNameParser.normalizeStrong("Nuculoidea Williams et  Breger 1916  "));
    assertEquals("Nuculoidea behrens var. christoph Williams & Breger, 1916",
        GBIFNameParser.normalizeStrong("Nuculoidea behrens var.christoph Williams & Breger [1916]  "));
    assertEquals("Nuculoidea Williams & Breger, 1916",
        GBIFNameParser.normalizeStrong(" 'Nuculoidea Williams & Breger, 1916'"));
    assertEquals("Malacocarpus schumannianus (Nicolai, 1893) Britton & Rose",
        GBIFNameParser.normalizeStrong("Malacocarpus schumannianus (Nicolai (1893)) Britton & Rose"));
    assertEquals("Photina (Cardioptera) burmeisteri (Westwood, 1889)",
        GBIFNameParser.normalizeStrong("Photina Cardioptera burmeisteri (Westwood 1889)"));
    assertEquals("Suaeda forsskahlei Schweinf.",
        GBIFNameParser.normalizeStrong("Suaeda forsskahlei Schweinf. ms."));
    assertEquals("Acacia bicolor Bojer", GBIFNameParser.normalizeStrong("Acacia bicolor Bojer ms."));
    assertEquals("Leucanitis roda (Herrich-Schäffer, 1851), 1845",
        GBIFNameParser.normalizeStrong("Leucanitis roda Herrich-Schäffer (1851) 1845"));
    assertEquals("Astelia alpina var. novae-hollandiae",
        GBIFNameParser.normalizeStrong("Astelia alpina var. novae-hollandiae"));
    // ampersand entities are handled as part of the regular html entity escaping:
    assertEquals("N. behrens Williams & amp; Breger, 1916",
        GBIFNameParser.normalizeStrong("  N.behrens Williams &amp;  Breger , 1916  "));
    assertEquals("N.behrens Williams & Breger , 1916",
        GBIFNameParser.preClean("  N.behrens Williams &amp;  Breger , 1916  "));
    assertEquals("N. behrens Williams & Breger, 1916",
        GBIFNameParser.normalizeStrong(GBIFNameParser.preClean("  N.behrens Williams &amp;  Breger , 1916  ")));
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
    assertParsedParts("\"Acetobacter aceti var. muciparum\" (sic) (Hoyer) Frateur, 1950", "Acetobacter", "aceti", "muciparum", Rank.VARIETY);
    assertParsedParts("\"Acetobacter melanogenum (sic) var. malto-saccharovorans\" Frateur, 1950", "Acetobacter", "melanogenum", "malto-saccharovorans", Rank.VARIETY);
    assertParsedParts("\"Acetobacter melanogenum (sic) var. maltovorans\" Frateur, 1950", "Acetobacter", "melanogenum", "maltovorans", Rank.VARIETY);
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
    assertParsedParts("Oreocharis aurea var. cordato-ovata (C.Y. Wu ex H.W. Li) K.Y. Pan, A.L. Weitzman, & L.E. Skog", "Oreocharis", "aurea", "cordato-ovata", Rank.VARIETY, null);
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
  public void testLegacyInfraspecificRanks() throws Exception {
    ParsedName pn = parser.parse("Potamon (Potamon) potamios setiger natio sendschirili Pretzmann, 1984", null);
    assertEquals("Potamon", pn.getGenusOrAbove());
    assertEquals("potamios", pn.getSpecificEpithet());
    assertEquals("sendschirili", pn.getInfraSpecificEpithet());
    assertEquals("Pretzmann", pn.getAuthorship());
    assertEquals("1984", pn.getYear());
    assertEquals(Rank.NATIO, pn.getRank());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Acipenser gueldenstaedti colchicus natio danubicus Movchan, 1967", null);
    assertEquals("Acipenser", pn.getGenusOrAbove());
    assertEquals("gueldenstaedti", pn.getSpecificEpithet());
    assertEquals("danubicus", pn.getInfraSpecificEpithet());
    assertEquals("Movchan", pn.getAuthorship());
    assertEquals("1967", pn.getYear());
    assertEquals(Rank.NATIO, pn.getRank());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Potamon (Potamon) potamios setiger natio sendschirili", null);
    assertEquals("Potamon", pn.getGenusOrAbove());
    assertEquals("Potamon", pn.getInfraGeneric());
    assertEquals("potamios", pn.getSpecificEpithet());
    assertEquals("sendschirili", pn.getInfraSpecificEpithet());
    assertNull(pn.getAuthorship());
    assertNull(pn.getYear());
    assertEquals(Rank.NATIO, pn.getRank());
    assertEquals(NameType.SCIENTIFIC, pn.getType());


    pn = parser.parse("Achillea millefolium prol. ceretanica Sennen", null);
    assertEquals("Achillea", pn.getGenusOrAbove());
    assertEquals("millefolium", pn.getSpecificEpithet());
    assertEquals("ceretanica", pn.getInfraSpecificEpithet());
    assertEquals("Sennen", pn.getAuthorship());
    assertEquals(Rank.PROLES, pn.getRank());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Luzula erecta prol. nigricans (Gaudin) Rouy", null);
    assertEquals("Luzula", pn.getGenusOrAbove());
    assertEquals("erecta", pn.getSpecificEpithet());
    assertEquals("nigricans", pn.getInfraSpecificEpithet());
    assertEquals("Rouy", pn.getAuthorship());
    assertEquals("Gaudin", pn.getBracketAuthorship());
    assertEquals(Rank.PROLES, pn.getRank());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Gymnadenia conopsea race alpina (Turcz. ex Rchb. f.) Rouy", null);
    assertEquals("Gymnadenia", pn.getGenusOrAbove());
    assertEquals("conopsea", pn.getSpecificEpithet());
    assertEquals("alpina", pn.getInfraSpecificEpithet());
    assertEquals("Rouy", pn.getAuthorship());
    assertEquals("Turcz. ex Rchb. f.", pn.getBracketAuthorship());
    assertEquals(Rank.RACE, pn.getRank());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Ajuga vulgaris race candolleana Rouy", null);
    assertEquals("Ajuga", pn.getGenusOrAbove());
    assertEquals("vulgaris", pn.getSpecificEpithet());
    assertEquals("candolleana", pn.getInfraSpecificEpithet());
    assertEquals("Rouy", pn.getAuthorship());
    assertEquals(Rank.RACE, pn.getRank());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Convallaria majalis convar. latifolia (Mill.) Ponert", null);
    assertEquals("Convallaria", pn.getGenusOrAbove());
    assertEquals("majalis", pn.getSpecificEpithet());
    assertEquals("latifolia", pn.getInfraSpecificEpithet());
    assertEquals("Ponert", pn.getAuthorship());
    assertEquals("Mill.", pn.getBracketAuthorship());
    assertEquals(Rank.CONVARIETY, pn.getRank());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Curvularia trifolii f.sp. gladioli Parmelee & Luttr.", null);
    assertEquals("Curvularia", pn.getGenusOrAbove());
    assertEquals("trifolii", pn.getSpecificEpithet());
    assertEquals("gladioli", pn.getInfraSpecificEpithet());
    assertEquals("Parmelee & Luttr.", pn.getAuthorship());
    assertEquals(Rank.FORMA_SPECIALIS, pn.getRank());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Fusarium oxysporum f.spec. tulipae W.C. Snyder & H.N. Hansen", null);
    assertEquals("Fusarium", pn.getGenusOrAbove());
    assertEquals("oxysporum", pn.getSpecificEpithet());
    assertEquals("tulipae", pn.getInfraSpecificEpithet());
    assertEquals("W.C. Snyder & H.N. Hansen", pn.getAuthorship());
    assertEquals(Rank.FORMA_SPECIALIS, pn.getRank());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Hieracium aphyllum grex singulare Zahn", null);
    assertEquals("Hieracium", pn.getGenusOrAbove());
    assertEquals("aphyllum", pn.getSpecificEpithet());
    assertEquals("singulare", pn.getInfraSpecificEpithet());
    assertEquals("Zahn", pn.getAuthorship());
    assertEquals(Rank.GREX, pn.getRank());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Corvus corax varius morpha leucophaeus", null);
    assertEquals("Corvus", pn.getGenusOrAbove());
    assertEquals("corax", pn.getSpecificEpithet());
    assertEquals("leucophaeus", pn.getInfraSpecificEpithet());
    assertEquals(Rank.MORPH, pn.getRank());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

    pn = parser.parse("Arvicola amphibius ab. pallasi Ognev 1913", null);
    assertEquals("Arvicola", pn.getGenusOrAbove());
    assertEquals("amphibius", pn.getSpecificEpithet());
    assertEquals("pallasi", pn.getInfraSpecificEpithet());
    assertEquals("Ognev", pn.getAuthorship());
    assertEquals("1913", pn.getYear());
    assertEquals(Rank.ABERRATION, pn.getRank());
    assertEquals(NameType.SCIENTIFIC, pn.getType());

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
  public void backboneBasionymPlaceholder() throws Exception {
    ParsedName n = parser.parse("? attenuata Hincks, 1866", null);
    assertEquals("?", n.getGenusOrAbove());
    assertNull(n.getInfraGeneric());
    assertEquals("attenuata", n.getSpecificEpithet());
    assertNull(n.getInfraSpecificEpithet());
    assertEquals("Hincks", n.getAuthorship());
    assertEquals("1866", n.getYear());
    assertEquals(NameType.PLACEHOLDER, n.getType());
  }

  /**
   * http://dev.gbif.org/issues/browse/POR-2987
   */
  @Test
  public void parseUnknownAuthor() throws Exception {
    ParsedName n = parser.parse("Severinia turcomaniae amplialata Unknown, 1921", Rank.SUBSPECIES);
    assertEquals("Severinia", n.getGenusOrAbove());
    assertNull(n.getInfraGeneric());
    assertEquals("turcomaniae", n.getSpecificEpithet());
    assertEquals("amplialata", n.getInfraSpecificEpithet());
    assertNull(n.getAuthorship());
    assertEquals("1921", n.getYear());
    assertEquals(NameType.PLACEHOLDER, n.getType());
  }

  /**
   * http://dev.gbif.org/issues/browse/POR-3000
   */
  @Test
  public void parseAdultOrLarva() throws Exception {
    ParsedName n = parser.parse("Elmis sp. Lv.", Rank.SPECIES);
    assertEquals("Elmis", n.getGenusOrAbove());
    assertNull(n.getInfraGeneric());
    assertNull(n.getSpecificEpithet());
    assertNull(n.getInfraSpecificEpithet());
    assertNull(n.getAuthorship());
    assertNull(n.getYear());
    assertEquals(NameType.INFORMAL, n.getType());
    assertEquals(Rank.SPECIES, n.getRank());
    assertEquals("Elmis spec.", n.canonicalNameComplete());

    n = parser.parse("Elmis sp. Lv", Rank.SPECIES);
    assertEquals("Elmis", n.getGenusOrAbove());
    assertNull(n.getInfraGeneric());
    assertNull(n.getSpecificEpithet());
    assertNull(n.getInfraSpecificEpithet());
    assertNull(n.getAuthorship());
    assertNull(n.getYear());
    assertEquals(NameType.INFORMAL, n.getType());
    assertEquals(Rank.SPECIES, n.getRank());
    assertEquals("Elmis spec.", n.canonicalNameComplete());

    n = parser.parse("Elmis sp. Ad.", null);
    assertEquals("Elmis", n.getGenusOrAbove());
    assertNull(n.getInfraGeneric());
    assertNull(n.getSpecificEpithet());
    assertNull(n.getInfraSpecificEpithet());
    assertNull(n.getAuthorship());
    assertNull(n.getYear());
    assertEquals(NameType.INFORMAL, n.getType());
    assertEquals(Rank.SPECIES, n.getRank());
    assertEquals("Elmis spec.", n.canonicalNameComplete());

    n = parser.parse("Elmis ssp. Ad", null);
    assertEquals("Elmis", n.getGenusOrAbove());
    assertNull(n.getInfraGeneric());
    assertNull(n.getSpecificEpithet());
    assertNull(n.getInfraSpecificEpithet());
    assertNull(n.getAuthorship());
    assertNull(n.getYear());
    assertEquals(NameType.INFORMAL, n.getType());
    assertEquals(Rank.SUBSPECIES, n.getRank());
    assertEquals("Elmis subsp.", n.canonicalNameComplete());
  }

  /**
   * http://dev.gbif.org/issues/browse/POR-3081
   */
  @Test
  public void parseProblematicSpecies() throws Exception {
    ParsedName n = parser.parse("Angiopteris d'urvilleana de Vriese", Rank.SPECIES);
    assertEquals("Angiopteris", n.getGenusOrAbove());
    assertNull(n.getInfraGeneric());
    assertEquals("d'urvilleana", n.getSpecificEpithet());
    assertNull(n.getInfraSpecificEpithet());
    assertEquals("de Vriese", n.getAuthorship());
    assertEquals(NameType.SCIENTIFIC, n.getType());

    n = parser.parse("Polana (Bulbusana) vana DeLong & Freytag 1972", Rank.SPECIES);
    assertEquals("Polana", n.getGenusOrAbove());
    assertEquals("Bulbusana", n.getInfraGeneric());
    assertEquals("vana", n.getSpecificEpithet());
    assertNull(n.getInfraSpecificEpithet());
    assertEquals("DeLong & Freytag", n.getAuthorship());
    assertEquals("1972", n.getYear());
    assertEquals(NameType.SCIENTIFIC, n.getType());

    n = parser.parse("Tabanus 4punctatus Fabricius, 1805", Rank.SPECIES);
    assertEquals("Tabanus", n.getGenusOrAbove());
    assertNull(n.getInfraGeneric());
    assertEquals("4punctatus", n.getSpecificEpithet());
    assertNull(n.getInfraSpecificEpithet());
    assertEquals("Fabricius", n.getAuthorship());
    assertEquals("1805", n.getYear());
    assertEquals(NameType.SCIENTIFIC, n.getType());

    n = parser.parse("Acer √ó hillieri Lancaster", Rank.SPECIES);
    assertEquals("Acer", n.getGenusOrAbove());
    assertNull(n.getInfraGeneric());
    assertEquals("hillieri", n.getSpecificEpithet());
    assertEquals(NamePart.SPECIFIC, n.getNotho());
    assertEquals("Acer ×hillieri Lancaster", n.canonicalNameComplete());
    assertEquals("Acer ×hillieri Lancaster", n.fullName());
    //assertEquals("Acer × hillieri Lancaster", n.getScientificName());
    assertNull(n.getInfraSpecificEpithet());
    assertEquals("Lancaster", n.getAuthorship());
    assertNull(n.getYear());
    // the garbage hybrid cross makes this doubtful
    assertEquals(NameType.DOUBTFUL, n.getType());

  }


  @Test
  public void testWhitespaceEpitheta() throws Exception {
    ParsedName n = parser.parse("Nupserha van rooni usambarica", null);
    assertEquals("Nupserha", n.getGenusOrAbove());
    assertNull(n.getInfraGeneric());
    assertEquals("van rooni", n.getSpecificEpithet());
    assertEquals("usambarica", n.getInfraSpecificEpithet());

    n = parser.parse("Sargassum flavicans van pervillei", null);
    assertEquals("Sargassum", n.getGenusOrAbove());
    assertNull(n.getInfraGeneric());
    assertEquals("flavicans", n.getSpecificEpithet());
    assertEquals("van pervillei", n.getInfraSpecificEpithet());

    n = parser.parse("Salix novae angliae lingulata ", null);
    assertEquals("Salix", n.getGenusOrAbove());
    assertNull(n.getInfraGeneric());
    assertEquals("novae angliae", n.getSpecificEpithet());
    assertEquals("lingulata", n.getInfraSpecificEpithet());

    n = parser.parse("Ilex collina van trompii", null);
    assertEquals("Ilex", n.getGenusOrAbove());
    assertNull(n.getInfraGeneric());
    assertEquals("collina", n.getSpecificEpithet());
    assertEquals("van trompii", n.getInfraSpecificEpithet());

    n = parser.parse("Gaultheria depressa novae zealandiae", null);
    assertEquals("Gaultheria", n.getGenusOrAbove());
    assertNull(n.getInfraGeneric());
    assertEquals("depressa", n.getSpecificEpithet());
    assertEquals("novae zealandiae", n.getInfraSpecificEpithet());

    n = parser.parse("Caraguata van volxemi gracilior", null);
    assertEquals("Caraguata", n.getGenusOrAbove());
    assertNull(n.getInfraGeneric());
    assertEquals("van volxemi", n.getSpecificEpithet());
    assertEquals("gracilior", n.getInfraSpecificEpithet());

    n = parser.parse("Ancistrocerus agilis novae guineae", null);
    assertEquals("Ancistrocerus", n.getGenusOrAbove());
    assertNull(n.getInfraGeneric());
    assertEquals("agilis", n.getSpecificEpithet());
    assertEquals("novae guineae", n.getInfraSpecificEpithet());

    // also test the authorless parsing
    n = parser.parse("Ancistrocerus agilis novae guineae", null);
    assertEquals("Ancistrocerus", n.getGenusOrAbove());
    assertNull(n.getInfraGeneric());
    assertEquals("agilis", n.getSpecificEpithet());
    assertEquals("novae guineae", n.getInfraSpecificEpithet());
  }

  @Test
  @Ignore
  public void manuallyTestProblematicName() throws Exception {
    System.out.println(parser.parse("Polypodium  x vulgare nothosubsp. antoniae (Rothm.) Schidlaym ", null));
  }

}



