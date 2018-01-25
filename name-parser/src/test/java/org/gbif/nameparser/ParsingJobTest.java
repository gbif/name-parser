package org.gbif.nameparser;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.gbif.nameparser.api.Authorship;
import org.gbif.nameparser.api.Rank;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class ParsingJobTest {
  final static ParsingJob JOB = new ParsingJob("Abies", Rank.UNRANKED);
  static final Pattern AUTHORSHIP_PATTERN = Pattern.compile("^" + ParsingJob.AUTHORSHIP + "$");

  @Test
  public void testEpithetPattern() throws Exception {
    Pattern epi = Pattern.compile("^"+ ParsingJob.EPHITHET+"$");
    assertTrue(epi.matcher("alba").find());
    assertTrue(epi.matcher("biovas").find());
    assertTrue(epi.matcher("serovat").find());
    assertTrue(epi.matcher("novo-zelandia").find());
    assertTrue(epi.matcher("elevar").find());
    assertTrue(epi.matcher("zelandia").find());
    assertTrue(epi.matcher("zelandia").find());
    assertTrue(epi.matcher("zelandia").find());
    assertTrue(epi.matcher("zelandia").find());

    assertFalse(epi.matcher("").find());
    assertFalse(epi.matcher("a").find());
    assertFalse(epi.matcher("serovar").find());
    assertFalse(epi.matcher("genotype").find());
    assertFalse(epi.matcher("agamovar").find());
    assertFalse(epi.matcher("cultivar").find());
    assertFalse(epi.matcher("serotype").find());
    assertFalse(epi.matcher("cytoform").find());
    assertFalse(epi.matcher("chemoform").find());
  }

  @Test
  public void testAuthorteam() throws Exception {
    assertAuthorTeamPattern("Petzold & G.Kirchn.",  "Petzold", "G.Kirchn.");
    assertAuthorTeamPattern("Britton, Sterns, & Poggenb.",  "Britton", "Sterns", "Poggenb.");
    assertAuthorTeamPattern("Van Heurck & Müll. Arg.",  "Van Heurck", "Müll.Arg.");
    assertAuthorTeamPattern("Gruber-Vodicka",  "Gruber-Vodicka");
    assertAuthorTeamPattern("Gruber-Vodicka et al.",  "Gruber-Vodicka", "al.");
    assertAuthorTeamPattern("L.");
    assertAuthorTeamPattern("Lin.");
    assertAuthorTeamPattern("Linné");
    assertAuthorTeamPattern("DC.");
    assertAuthorTeamPattern("de Chaudoir");
    assertAuthorTeamPattern("Hilaire");
    assertAuthorTeamPattern("St. Hilaire","St.Hilaire");
    assertAuthorTeamPattern("Geoffroy St. Hilaire",  "Geoffroy St.Hilaire");
    assertAuthorTeamPattern("Acev.-Rodr.");
    assertAuthorTeamPattern("Steyerm., Aristeg. & Wurdack","Steyerm.","Aristeg.", "Wurdack");
    assertAuthorTeamPattern("Du Puy & Labat", "Du Puy","Labat");
    assertAuthorTeamPattern("Baum.-Bod.");
    assertAuthorTeamPattern("Engl. & v. Brehmer","Engl.", "v.Brehmer");
    assertAuthorTeamPattern("F. v. Muell.",  "F.v.Muell.");
    assertAuthorTeamPattern("W.J.de Wilde & Duyfjes",  "W.J.de Wilde", "Duyfjes");
    assertAuthorTeamPattern("C.E.M.Bicudo");
    assertAuthorTeamPattern("Alves-da-Silva");
    assertAuthorTeamPattern("Alves-da-Silva & C.E.M.Bicudo",  "Alves-da-Silva", "C.E.M.Bicudo");
    assertAuthorTeamPattern("Kingdon-Ward");
    assertAuthorTeamPattern("Merr. & L.M.Perry",  "Merr.", "L.M.Perry");
    assertAuthorTeamPattern("Calat., Nav.-Ros. & Hafellner",  "Calat.", "Nav.-Ros.", "Hafellner");
    assertAuthorTeamPattern("Barboza du Bocage");
    assertAuthorTeamPattern("Payri & P.W.Gabrielson",  "Payri", "P.W.Gabrielson");
    assertAuthorTeamPattern("N'Yeurt, Payri & P.W.Gabrielson",  "N'Yeurt", "Payri", "P.W.Gabrielson");
    assertAuthorTeamPattern("VanLand.");
    assertAuthorTeamPattern("MacLeish");
    assertAuthorTeamPattern("Monterosato ms.");
    assertAuthorTeamPattern("Arn. ms., Grunow",  "Arn.ms.", "Grunow");
    assertAuthorTeamPattern("Choi,J.H.; Im,W.T.; Yoo,J.S.; Lee,S.M.; Moon,D.S.; Kim,H.J.; Rhee,S.K.; Roh,D.H.",
        "J.H.Choi", "W.T.Im", "J.S.Yoo", "S.M.Lee", "D.S.Moon", "H.J.Kim", "S.K.Rhee", "D.H.Roh");
    assertAuthorTeamPattern("da Costa Lima",  "da Costa Lima");
    assertAuthorTeamPattern("Krapov., W.C.Greg. & C.E.Simpson",  "Krapov.", "W.C.Greg.", "C.E.Simpson");
    assertAuthorTeamPattern("de Jussieu",  "de Jussieu");
    assertAuthorTeamPattern("van-der Land",  "van-der Land");
    assertAuthorTeamPattern("van der Land",  "van der Land");
    assertAuthorTeamPattern("van Helmsick",  "van Helmsick");
    assertAuthorTeamPattern("Xing, Yan & Yin",  "Xing", "Yan", "Yin");
    assertAuthorTeamPattern("Xiao & Knoll",  "Xiao", "Knoll");
    assertAuthorTeamPattern("Wang, Yuwen & Xian-wei Liu",  "Wang", "Yuwen", "Xian-wei Liu");
    assertAuthorTeamPattern("Liu, Xian-wei, Z. Zheng & G. Xi",  "Liu", "Xian-wei", "Z.Zheng", "G.Xi");
    assertAuthorTeamPattern("Clayton, D.H.; Price, R.D.; Page, R.D.M.",  "D.H.Clayton", "R.D.Price", "R.D.M.Page");
    assertAuthorTeamPattern("Michiel de Ruyter",  "Michiel de Ruyter");
    assertAuthorTeamPattern("DeFilipps",  "DeFilipps");
    assertAuthorTeamPattern("Henk 't Hart",  "Henk 't Hart");
    assertAuthorTeamPattern("P.E.Berry & Reg.B.Miller",  "P.E.Berry", "Reg.B.Miller");
    assertAuthorTeamPattern("'t Hart",  "'t Hart");
    assertAuthorTeamPattern("Abdallah & Sa'ad",  "Abdallah", "Sa'ad");
    assertAuthorTeamPattern("Linnaeus filius");
    assertAuthorTeamPattern("Bollmann, M.Y.Cortés, Kleijne, J.B.Østerg. & Jer.R.Young",  "Bollmann", "M.Y.Cortés", "Kleijne", "J.B.Østerg.", "Jer.R.Young");
    assertAuthorTeamPattern("Branco, M.T.P.Azevedo, Sant'Anna & Komárek",  "Branco", "M.T.P.Azevedo", "Sant'Anna", "Komárek");
    assertAuthorTeamPattern("Janick Hendrik van Kinsbergen");
    assertAuthorTeamPattern("Jan Hendrik van Kinsbergen");
    assertAuthorTeamPattern("Sainte-Claire Deville");
    assertAuthorTeamPattern("Semenov-Tian-Shanskij");
    assertAuthorTeamPattern("Semenov-Tian-Shanskij, Sainte-Claire Deville, Janick Hendrik van Kinsbergen",  "Semenov-Tian-Shanskij", "Sainte-Claire Deville", "Janick Hendrik van Kinsbergen");
    assertAuthorTeamPattern("Scotto la Massese");
    assertAuthorTeamPattern("An der Lan");
    assertAuthorTeamPattern("Bor & s'Jacob",  "Bor", "s'Jacob");
    assertAuthorTeamPattern("Brunner von Wattenwyl v.W.");
    assertAuthorTeamPattern("Martinez y Saez");
    assertAuthorTeamPattern("Da Silva e Castro");
    assertAuthorTeamPattern("LafuenteRoca & Carbonell",  "LafuenteRoca", "Carbonell");
    assertAuthorTeamPattern("Mas-ComaBargues & Esteban",  "Mas-ComaBargues", "Esteban");
    assertAuthorTeamPattern("Hondt d");
    assertAuthorTeamPattern("Abou-El-Naga");
  }

  @Test
  public void testAuthorship() throws Exception {
    assertAuthorshipPattern("Plesn¡k ex F.Ritter", "Plesnik", "F.Ritter");
    assertAuthorshipPattern("Britton, Sterns, & Poggenb.", null, "Britton", "Sterns", "Poggenb.");
    assertAuthorshipPattern("Van Heurck & Müll. Arg.", null, "Van Heurck", "Müll.Arg.");
    assertAuthorshipPattern("Gruber-Vodicka", null, "Gruber-Vodicka");
    assertAuthorshipPattern("Gruber-Vodicka et al.", null, "Gruber-Vodicka", "al.");
    assertAuthorshipPattern("L.");
    assertAuthorshipPattern("Lin.");
    assertAuthorshipPattern("Linné");
    assertAuthorshipPattern("DC.");
    assertAuthorshipPattern("de Chaudoir");
    assertAuthorshipPattern("Hilaire");
    assertAuthorshipPattern("St. Hilaire",null,"St.Hilaire");
    assertAuthorshipPattern("Geoffroy St. Hilaire", null, "Geoffroy St.Hilaire");
    assertAuthorshipPattern("Acev.-Rodr.");
    assertAuthorshipPattern("Steyerm., Aristeg. & Wurdack",null,"Steyerm.","Aristeg.", "Wurdack");
    assertAuthorshipPattern("Du Puy & Labat", null,"Du Puy","Labat");
    assertAuthorshipPattern("Baum.-Bod.");
    assertAuthorshipPattern("Engl. & v. Brehmer",null,"Engl.", "v.Brehmer");
    assertAuthorshipPattern("F. v. Muell.", null, "F.v.Muell.");
    assertAuthorshipPattern("W.J.de Wilde & Duyfjes", null, "W.J.de Wilde", "Duyfjes");
    assertAuthorshipPattern("C.E.M.Bicudo");
    assertAuthorshipPattern("Alves-da-Silva");
    assertAuthorshipPattern("Alves-da-Silva & C.E.M.Bicudo", null, "Alves-da-Silva", "C.E.M.Bicudo");
    assertAuthorshipPattern("Kingdon-Ward");
    assertAuthorshipPattern("Merr. & L.M.Perry", null, "Merr.", "L.M.Perry");
    assertAuthorshipPattern("Calat., Nav.-Ros. & Hafellner", null, "Calat.", "Nav.-Ros.", "Hafellner");
    assertAuthorshipPattern("Arv.-Touv. ex Dörfl.", "Arv.-Touv.", "Dörfl.");
    assertAuthorshipPattern("Payri & P.W.Gabrielson", null, "Payri", "P.W.Gabrielson");
    assertAuthorshipPattern("N'Yeurt, Payri & P.W.Gabrielson", null, "N'Yeurt", "Payri", "P.W.Gabrielson");
    assertAuthorshipPattern("VanLand.");
    assertAuthorshipPattern("MacLeish");
    assertAuthorshipPattern("Monterosato ms.");
    assertAuthorshipPattern("Arn. ms., Grunow", null, "Arn.ms.", "Grunow");
    assertAuthorshipPattern("Griseb. ex. Wedd.", "Griseb.", "Wedd.");

    assertAuthorshipPatternFails("Wedd. ex Sch. Bip. (");
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

  @Test
  public void testNomStatusRemarks() throws Exception {
    // parser expects a dot or a space as done by the string normalizer
    nomStatusRemark("sp.nov.");
    nomStatusRemark("Spec nov");
    nomStatusRemark("Fam.nov.");
    nomStatusRemark("Gen.nov.");
    // our test only catches the first match, real parsing both!
    nomStatusRemark("Gen. nov. sp. nov", "Gen.nov.");
    nomStatusRemark("Abies keralia spec. nov.", "spec.nov.");
    nomStatusRemark("Abies sp. nov.", "sp.nov.");
  }

  private void nomStatusRemark(String remarks) {
    nomStatusRemark(remarks, remarks);
  }
  private void nomStatusRemark(String remarks, String match) {
    Matcher m = ParsingJob.EXTRACT_NOMSTATUS.matcher(JOB.normalize(remarks));
    assertTrue (m.find());
    assertEquals(match, m.group().trim());
  }

  private void testCultivar(String cultivar) {
    Matcher m = ParsingJob.CULTIVAR.matcher(cultivar);
    assertTrue (m.find());
  }

  private void assertAuthorshipPattern(String authorship) {
    assertAuthorshipPattern(authorship, null, authorship);
  }

  private void assertAuthorshipPattern(String authorship, String exAuthor, String ... authors) {
    try {
      String normed = JOB.normalize(authorship);
      Matcher m = AUTHORSHIP_PATTERN.matcher(normed);
      assertTrue(authorship, m.find());
      if (ParsingJob.LOG.isDebugEnabled()) {
        ParsingJob.logMatcher(m);
      }
      Authorship auth = ParsingJob.parseAuthorship(m.group(1), m.group(2), m.group(3));
      if (exAuthor == null) {
        assertTrue(auth.getExAuthors().isEmpty());
      } else {
        assertEquals(exAuthor, auth.getExAuthors().get(0));
        assertEquals(1, auth.getExAuthors().size());
      }
      assertEquals(Lists.newArrayList(authors), auth.getAuthors());

    } catch (AssertionError | RuntimeException e) {
      System.err.println("Authorship error: " + authorship);
      throw e;
    }
  }

  private void assertAuthorshipPatternFails(String authorship) {
    String normed = JOB.normalize(authorship);
    Matcher m = AUTHORSHIP_PATTERN.matcher(normed);
    assertFalse(authorship, m.find());
  }


  private void assertAuthorTeamPattern(String authorship) {
    assertAuthorTeamPattern(authorship, authorship);
  }

  private void assertAuthorTeamPattern(String authorship, String ... authors) {
    String normed = JOB.normalize(authorship);
    Matcher m = ParsingJob.AUTHOR_TEAM_PATTERN.matcher(normed);
    assertTrue(authorship, m.find());
    if (ParsingJob.LOG.isDebugEnabled()) {
      ParsingJob.logMatcher(m);
    }
    Authorship auth = ParsingJob.parseAuthorship(null, m.group(0), null);
    assertEquals(Lists.newArrayList(authors), auth.getAuthors());
  }

  @Test
  public void testNomenclaturalNotesPattern() throws Exception {
    assertNomNote("nom.illeg.",  "Vaucheria longicaulis var. bengalensis Islam, nom. illeg.");
    assertNomNote("nom.correct",  "Dorataspidae nom. correct");
    assertNomNote("nom.transf.",  "Ethmosphaeridae nom. transf.");
    assertNomNote("nom.ambig.",  "Fucus ramosissimus Oeder, nom. ambig.");
    assertNomNote("nom.nov.",  "Myrionema majus Foslie, nom. nov.");
    assertNomNote("nom.utique rej.",  "Corydalis bulbosa (L.) DC., nom. utique rej.");
    assertNomNote("nom.cons.prop.",  "Anthoceros agrestis var. agrestis Paton nom. cons. prop.");
    assertNomNote("nom.superfl.", "Lithothamnion glaciale forma verrucosum (Foslie) Foslie, nom. superfl.");
    assertNomNote("nom.rejic.","Pithecellobium montanum var. subfalcatum (Zoll. & Moritzi)Miq., nom.rejic.");
    assertNomNote("nom.inval","Fucus vesiculosus forma volubilis (Goodenough & Woodward) H.T. Powell, nom. inval");
    assertNomNote("nom.nud.",  "Sao hispanica R. & E. Richter nom. nud. in Sampelayo 1935");
    assertNomNote("nom.illeg.",  "Hallo (nom.illeg.)");
    assertNomNote("nom.super.",  "Calamagrostis cinnoides W. Bart. nom. super.");
    assertNomNote("nom.nud.",  "Iridaea undulosa var. papillosa Bory de Saint-Vincent, nom. nud.");
    assertNomNote("nom.inval","Sargassum angustifolium forma filiforme V. Krishnamurthy & H. Joshi, nom. inval");
    assertNomNote("nomen nudum",  "Solanum bifidum Vell. ex Dunal, nomen nudum");
    assertNomNote("nomen invalid.","Schoenoplectus ×scheuchzeri (Bruegger) Palla ex Janchen, nomen invalid.");
    assertNomNote("nom.nud.","Cryptomys \"Kasama\" Kawalika et al., 2001, nom. nud. (Kasama, Zambia) .");
    assertNomNote("nom.super.",  "Calamagrostis cinnoides W. Bart. nom. super.");
    assertNomNote("nom.dub.",  "Pandanus odorifer (Forssk.) Kuntze, nom. dub.");
    assertNomNote("nom.rejic.",  "non Clarisia Abat, 1792, nom. rejic.");
    assertNomNote("nom.cons","Yersinia pestis (Lehmann and Neumann, 1896) van Loghem, 1944 (Approved Lists, 1980) , nom. cons");
    assertNomNote("nom.rejic.","\"Pseudomonas denitrificans\" (Christensen, 1903) Bergey et al., 1923, nom. rejic.");
    assertNomNote("nom.nov.",  "Tipula rubiginosa Loew, 1863, nom. nov.");
    assertNomNote("nom.prov.",  "Amanita pruittii A.H.Sm. ex Tulloss & J.Lindgr., nom. prov.");
    assertNomNote("nom.cons.",  "Ramonda Rich., nom. cons.");
    assertNomNote("nom.cons.","Kluyver and van Niel, 1936 emend. Barker, 1956 (Approved Lists, 1980) , nom. cons., emend. Mah and Kuhn, 1984");
    assertNomNote("nom.superfl.",  "Coccocypselum tontanea (Aubl.) Kunth, nom. superfl.");
    assertNomNote("nom.ambig.",  "Lespedeza bicolor var. intermedia Maxim. , nom. ambig.");
    assertNomNote("nom.praeoccup.",  "Erebia aethiops uralensis Goltz, 1930 nom. praeoccup.");
    assertNomNote("comb.nov.ined.",  "Ipomopsis tridactyla (Rydb.) Wilken, comb. nov. ined.");
    assertNomNote("sp.nov.ined.",  "Orobanche riparia Collins, sp. nov. ined.");
    assertNomNote("gen.nov.",  "Anchimolgidae gen. nov. New Caledonia-Rjh-, 2004");
    assertNomNote("gen.nov.ined.",  "Stebbinsoseris gen. nov. ined.");
    assertNomNote("var.nov.",  "Euphorbia rossiana var. nov. Steinmann, 1199");
  }

  private void assertNomNote(String expectedNote, String name) {
    String nomNote = null;
    Matcher matcher = ParsingJob.EXTRACT_NOMSTATUS.matcher(JOB.normalize(name));
    if (matcher.find()) {
      nomNote = (StringUtils.trimToNull(matcher.group(1)));
    }
    assertEquals(expectedNote, nomNote);
  }

  @Test
  public void testClean1() throws Exception {
    assertEquals("", ParsingJob.preClean(""));
    assertEquals("Hallo Spencer", ParsingJob.preClean("Hallo Spencer "));
    assertEquals("Hallo Spencer", ParsingJob.preClean("' 'Hallo Spencer"));
    assertEquals("Hallo Spencer 1982", ParsingJob.preClean("'\" Hallo  Spencer 1982'"));
  }

  @Test
  public void testNormalizeName() {
    assertNormalize("Anniella nigra FISCHER 1885", "Anniella nigra Fischer 1885");
    assertNormalize("Nuculoidea Williams et  Breger 1916  ","Nuculoidea Williams&Breger 1916");
    assertNormalize("Nuculoidea behrens var.christoph Williams & Breger [1916]  ", "Nuculoidea behrens var.christoph Williams&Breger[1916]");
    assertNormalize("Nuculoidea behrens var.christoph Williams & Breger [1916]  ","Nuculoidea behrens var.christoph Williams&Breger[1916]");
    assertNormalize("Nuculoidea   Williams& Breger, 1916  ", "Nuculoidea Williams&Breger,1916");
    assertNormalize("Asplenium X inexpectatum (E. L. Braun 1940)Morton (1956) ", "Asplenium ×inexpectatum(E.L.Braun 1940)Morton(1956)");
    assertNormalize(" × Agropogon", "×Agropogon");
    assertNormalize("Salix × capreola Andersson","Salix ×capreola Andersson");
    assertNormalize("Leucanitis roda Herrich-Schäffer (1851) 1845", "Leucanitis roda Herrich-Schäffer(1851)1845");
    assertNormalize("Huaiyuanella Xing, Yan&Yin, 1984", "Huaiyuanella Xing,Yan&Yin,1984");
    assertNormalize("Caloplaca poliotera (Nyl.) J. Steiner\r\n\r\n", "Caloplaca poliotera(Nyl.)J.Steiner");
    assertNormalize("Caloplaca poliotera (Nyl.) J. Steiner\\r\\n\\r\\r", "Caloplaca poliotera(Nyl.)J.Steiner");
    assertNormalize("Choi,J.H.; Im,W.T.; Yoo,J.S.; Lee,S.M.; Moon,D.S.; Kim,H.J.; Rhee,S.K.", "Choi,J.H.;Im,W.T.;Yoo,J.S.;Lee,S.M.;Moon,D.S.;Kim,H.J.;Rhee,S.K.");

    // imprint years
    assertNormalize("Ctenotus alacer Storr, 1970 [\"1969\"]", "Ctenotus alacer Storr,1970");
    assertNormalize("C. Flahault 1887 (\"1886-1888\")", "C.Flahault 1887");
    assertNormalize("Ehrenberg, 1870, 1869", "Ehrenberg,1870");
    assertNormalize("Ctenotus alacer Storr, 1970 (\"1969\")", "Ctenotus alacer Storr,1970");
    assertNormalize("Ctenotus alacer Storr, 1970 [\"1969\"]", "Ctenotus alacer Storr,1970");
    assertNormalize("Ctenotus alacer Storr, 1970 (imprint 1969)", "Ctenotus alacer Storr,1970");
    assertNormalize("Ctenotus alacer Storr, 1970 (not 1969)", "Ctenotus alacer Storr,1970");
  }

  private void assertNormalize(String raw, String expected) {
    assertEquals(expected, JOB.normalize(ParsingJob.preClean(raw)));
  }

  @Test
  public void testNormalizeStrongName() {
    assertNormalizeStrong("Alstonia vieillardii Van Heurck & Müll.Arg.", "Alstonia vieillardii Van Heurck&Müll.Arg.");
    assertNormalizeStrong("Nuculoidea Williams et  Breger 1916  ","Nuculoidea Williams&Breger 1916");
    //assertNormalizeStrong("Nuculoidea behrens var.christoph Williams & Breger [1916]  ", "Nuculoidea behrens var.christoph Williams&Breger,1916");
    assertNormalizeStrong(" 'Nuculoidea Williams & Breger, 1916'", "Nuculoidea Williams&Breger,1916");
    assertNormalizeStrong("Photina Cardioptera burmeisteri (Westwood 1889)", "Photina(Cardioptera)burmeisteri(Westwood 1889)");
    assertNormalizeStrong("Suaeda forsskahlei Schweinf. ms.", "Suaeda forsskahlei Schweinf.ms.");
    assertNormalizeStrong("Acacia bicolor Bojer ms.", "Acacia bicolor Bojer ms.");
    assertNormalizeStrong("Astelia alpina var. novae-hollandiae", "Astelia alpina var.novae-hollandiae");
    assertNormalizeStrong("  N.behrens Williams &amp;  Breger , 1916  ", "N.behrens Williams&Breger,1916");
    assertNormalizeStrong("Melanoides kinshassaensis D+P", "Melanoides kinshassaensis D&P");
    assertNormalizeStrong("Bathylychnops chilensis Parin_NV, Belyanina_TN & Evseenko 2009", "Bathylychnops chilensis Parin NV,Belyanina TN&Evseenko 2009");
    assertNormalizeStrong("denheyeri Eghbalian, Khanjani and Ueckermann in Eghbalian, Khanjani & Ueckermann, 2017", "? denheyeri Eghbalian,Khanjani&Ueckermann in Eghbalian,Khanjani&Ueckermann,2017");
    // http://zoobank.org/References/C37149C7-FC3B-4267-9CD0-03E0E0059459
    // http://www.tandfonline.com/doi/full/10.1080/14772019.2016.1246112
    assertNormalizeStrong("‘Perca’ lactarioides Lin, Nolf, Steurbaut & Girone, 2016", "Perca lactarioides Lin,Nolf,Steurbaut&Girone,2016");
    assertNormalizeStrong(" ‘Liopropoma’ sculpta sp. nov.", "Liopropoma sculpta sp.nov.");
    assertNormalizeStrong("fordycei Boersma, McCurry & Pyenson, 2017", "? fordycei Boersma,McCurry&Pyenson,2017");

    //TODO: very expected results!
    //assertNormalizeStrong("Malacocarpus schumannianus (Nicolai (1893)) Britton & Rose", "Malacocarpus schumannianus (Nicolai, 1893) Britton & Rose");
    //assertNormalizeStrong("Leucanitis roda Herrich-Schäffer (1851) 1845", "Leucanitis roda (Herrich-Schäffer, 1851), 1845");
  }

  private void assertNormalizeStrong(String raw, String expected) {
    assertEquals(expected, JOB.normalizeStrong(JOB.normalize(ParsingJob.preClean(raw))));
  }

}