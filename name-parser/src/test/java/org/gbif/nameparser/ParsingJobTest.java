package org.gbif.nameparser;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.gbif.nameparser.api.Authorship;
import org.junit.Ignore;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class ParsingJobTest {

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
  public void testAuthorship() throws Exception {
    assertAuthorTeamPattern("Britton, Sterns, & Poggenb.", null, "Britton", "Sterns", "Poggenb.");
    assertAuthorTeamPattern("Van Heurck & Müll. Arg.", null, "Van Heurck", "Müll.Arg.");
    assertAuthorTeamPattern("Gruber-Vodicka et al.", null, "Gruber-Vodicka", "al.");
    assertAuthorTeamPattern("L.");
    assertAuthorTeamPattern("Lin.");
    assertAuthorTeamPattern("Linné");
    assertAuthorTeamPattern("DC.");
    assertAuthorTeamPattern("de Chaudoir");
    assertAuthorTeamPattern("Hilaire");
    assertAuthorTeamPattern("St. Hilaire",null,"St.Hilaire");
    assertAuthorTeamPattern("Geoffroy St. Hilaire", null, "Geoffroy St.Hilaire");
    assertAuthorTeamPattern("Acev.-Rodr.");
    assertAuthorTeamPattern("Steyerm., Aristeg. & Wurdack",null,"Steyerm.","Aristeg.", "Wurdack");
    assertAuthorTeamPattern("Du Puy & Labat", null,"Du Puy","Labat");
    assertAuthorTeamPattern("Baum.-Bod.");
    assertAuthorTeamPattern("Engl. & v. Brehmer",null,"Engl.", "v.Brehmer");
    assertAuthorTeamPattern("F. v. Muell.", null, "F.v.Muell.");
    assertAuthorTeamPattern("W.J.de Wilde & Duyfjes", null, "W.J.de Wilde", "Duyfjes");
    assertAuthorTeamPattern("C.E.M.Bicudo");
    assertAuthorTeamPattern("Alves-da-Silva");
    assertAuthorTeamPattern("Alves-da-Silva & C.E.M.Bicudo", null, "Alves-da-Silva", "C.E.M.Bicudo");
    assertAuthorTeamPattern("Kingdon-Ward");
    assertAuthorTeamPattern("Merr. & L.M.Perry", null, "Merr.", "L.M.Perry");
    assertAuthorTeamPattern("Calat., Nav.-Ros. & Hafellner", null, "Calat.", "Nav.-Ros.", "Hafellner");
    assertAuthorTeamPattern("Barboza du Bocage");
    assertAuthorTeamPattern("Arv.-Touv. ex Dörfl.", "Arv.-Touv.", "Dörfl.");
    assertAuthorTeamPattern("Payri & P.W.Gabrielson", null, "Payri", "P.W.Gabrielson");
    assertAuthorTeamPattern("N'Yeurt, Payri & P.W.Gabrielson", null, "N'Yeurt", "Payri", "P.W.Gabrielson");
    assertAuthorTeamPattern("VanLand.");
    assertAuthorTeamPattern("MacLeish");
    assertAuthorTeamPattern("Monterosato ms.");
    assertAuthorTeamPattern("Arn. ms., Grunow", null, "Arn.ms.", "Grunow");
    assertAuthorTeamPattern("Mosely in Mosely & Kimmins", null, "Mosely");
    assertAuthorTeamPattern("Choi,J.H.; Im,W.T.; Yoo,J.S.; Lee,S.M.; Moon,D.S.; Kim,H.J.; Rhee,S.K.; Roh,D.H.", null,
        "J.H.Choi", "W.T.Im", "J.S.Yoo", "S.M.Lee", "D.S.Moon", "H.J.Kim", "S.K.Rhee", "D.H.Roh");
    assertAuthorTeamPattern("da Costa Lima", null, "da Costa Lima");
    assertAuthorTeamPattern("Krapov., W.C.Greg. & C.E.Simpson", null, "Krapov.", "W.C.Greg.", "C.E.Simpson");
    assertAuthorTeamPattern("de Jussieu", null, "de Jussieu");
    assertAuthorTeamPattern("Griseb. ex. Wedd.", "Griseb.", "Wedd.");
    assertAuthorTeamPattern("van-der Land", null, "van-der Land");
    assertAuthorTeamPattern("van der Land", null, "van der Land");
    assertAuthorTeamPattern("van Helmsick", null, "van Helmsick");
    assertAuthorTeamPattern("Xing, Yan & Yin", null, "Xing", "Yan", "Yin");
    assertAuthorTeamPattern("Xiao & Knoll", null, "Xiao", "Knoll");
    assertAuthorTeamPattern("Wang, Yuwen & Xian-wei Liu", null, "Wang", "Yuwen", "Xian-wei Liu");
    assertAuthorTeamPattern("Liu, Xian-wei, Z. Zheng & G. Xi", null, "Liu", "Xian-wei", "Z.Zheng", "G.Xi");
    assertAuthorTeamPattern("Clayton, D.H.; Price, R.D.; Page, R.D.M.", null, "D.H.Clayton", "R.D.Price", "R.D.M.Page");
    assertAuthorTeamPattern("Michiel de Ruyter", null, "Michiel de Ruyter");
    assertAuthorTeamPattern("DeFilipps", null, "DeFilipps");
    assertAuthorTeamPattern("Henk 't Hart", null, "Henk 't Hart");
    assertAuthorTeamPattern("P.E.Berry & Reg.B.Miller", null, "P.E.Berry", "Reg.B.Miller");
    assertAuthorTeamPattern("'t Hart", null, "'t Hart");
    assertAuthorTeamPattern("Abdallah & Sa'ad", null, "Abdallah", "Sa'ad");
    assertAuthorTeamPattern("Bollmann, M.Y.Cortés, Kleijne, J.B.Østerg. & Jer.R.Young", null, "Bollmann", "M.Y.Cortés", "Kleijne", "J.B.Østerg.", "Jer.R.Young");
    assertAuthorTeamPattern("Branco, M.T.P.Azevedo, Sant'Anna & Komárek", null, "Branco", "M.T.P.Azevedo", "Sant'Anna", "Komárek");
    assertAuthorTeamPattern("Janick Hendrik van Kinsbergen", null, "Janick Hendrik van Kinsbergen");
    assertAuthorTeamPattern("Jan Hendrik van Kinsbergen", null, "Jan Hendrik van Kinsbergen");
    //assertAuthorTeamPattern("A.F.Peters, E.C.Yang, A.F.Peters, E.C.Yang, F.C.Küpper & Prud'Homme van Reine", null, "A.F.Peters", "E.C.Yang", "A.F.Peters", "E.C.Yang", "F.C.Küpper", "Prud'Homme van Reine");
  }

  @Test
  public void testEscaping() throws Exception {
    assertEquals("Caloplaca poliotera (Nyl.) J. Steiner",
        ParsingJob.normalize("Caloplaca poliotera (Nyl.) J. Steiner\r\n\r\n"));
    assertEquals("Caloplaca poliotera (Nyl.) J. Steiner",
        ParsingJob.normalize("Caloplaca poliotera (Nyl.) J. Steiner\\r\\n\\r\\r"));
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
    Matcher m = ParsingJob.CULTIVAR.matcher(cultivar);
    assertTrue (m.find());
  }

  private void assertAuthorTeamPattern(String authorship) {
    assertAuthorTeamPattern(authorship, null, authorship);
  }

  private void assertAuthorTeamPattern(String authorship, String exAuthor, String ... authors) {
    Matcher m = ParsingJob.AUTHORSHIP_PATTERN.matcher(authorship);
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
  }


  @Test
  public void testNomenclaturalNotesPattern() throws Exception {
    assertNomNote("nom. illeg.",  "Vaucheria longicaulis var. bengalensis Islam, nom. illeg.");
    assertNomNote("nom. correct",  "Dorataspidae nom. correct");
    assertNomNote("nom. transf.",  "Ethmosphaeridae nom. transf.");
    assertNomNote("nom. ambig.",  "Fucus ramosissimus Oeder, nom. ambig.");
    assertNomNote("nom. nov.",  "Myrionema majus Foslie, nom. nov.");
    assertNomNote("nom. utique rej.",  "Corydalis bulbosa (L.) DC., nom. utique rej.");
    assertNomNote("nom. cons. prop.",  "Anthoceros agrestis var. agrestis Paton nom. cons. prop.");
    assertNomNote("nom. superfl.", "Lithothamnion glaciale forma verrucosum (Foslie) Foslie, nom. superfl.");
    assertNomNote("nom.rejic.","Pithecellobium montanum var. subfalcatum (Zoll. & Moritzi)Miq., nom.rejic.");
    assertNomNote("nom. inval","Fucus vesiculosus forma volubilis (Goodenough & Woodward) H.T. Powell, nom. inval");
    assertNomNote("nom. nud.",  "Sao hispanica R. & E. Richter nom. nud. in Sampelayo 1935");
    assertNomNote("nom.illeg.",  "Hallo (nom.illeg.)");
    assertNomNote("nom. super.",  "Calamagrostis cinnoides W. Bart. nom. super.");
    assertNomNote("nom. nud.",  "Iridaea undulosa var. papillosa Bory de Saint-Vincent, nom. nud.");
    assertNomNote("nom. inval","Sargassum angustifolium forma filiforme V. Krishnamurthy & H. Joshi, nom. inval");
    assertNomNote("nomen nudum",  "Solanum bifidum Vell. ex Dunal, nomen nudum");
    assertNomNote("nomen invalid.","Schoenoplectus ×scheuchzeri (Bruegger) Palla ex Janchen, nomen invalid.");
    assertNomNote("nom. nud.","Cryptomys \"Kasama\" Kawalika et al., 2001, nom. nud. (Kasama, Zambia) .");
    assertNomNote("nom. super.",  "Calamagrostis cinnoides W. Bart. nom. super.");
    assertNomNote("nom. dub.",  "Pandanus odorifer (Forssk.) Kuntze, nom. dub.");
    assertNomNote("nom. rejic.",  "non Clarisia Abat, 1792, nom. rejic.");
    assertNomNote("nom. cons","Yersinia pestis (Lehmann and Neumann, 1896) van Loghem, 1944 (Approved Lists, 1980) , nom. cons");
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

  private void assertNomNote(String expectedNote, String name) {
    String nomNote = null;
    Matcher matcher = ParsingJob.EXTRACT_NOMSTATUS.matcher(name);
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
    assertEquals("Anniella nigra Fischer, 1885", ParsingJob.normalize("Anniella nigra FISCHER 1885"));
    assertEquals("Nuculoidea Williams et Breger, 1916", ParsingJob.normalize("Nuculoidea Williams et  Breger 1916  "));
    assertEquals("Nuculoidea behrens var. christoph Williams & Breger [1916]",
        ParsingJob.normalize("Nuculoidea behrens var.christoph Williams & Breger [1916]  "));
    assertEquals("Nuculoidea behrens var. christoph Williams & Breger [1916]",
        ParsingJob.normalize("Nuculoidea behrens var.christoph Williams & Breger [1916]  "));
    assertEquals(ParsingJob.normalize("Nuculoidea Williams & Breger, 1916  "),
        ParsingJob.normalize("Nuculoidea   Williams& Breger, 1916"));
    assertEquals("Asplenium ×inexpectatum (E. L. Braun, 1940) Morton (1956)",
        ParsingJob.normalize("Asplenium X inexpectatum (E. L. Braun 1940)Morton (1956) "));
    assertEquals("×Agropogon", ParsingJob.normalize(" × Agropogon"));
    assertEquals("Salix ×capreola Andersson", ParsingJob.normalize("Salix × capreola Andersson"));
    assertEquals("Leucanitis roda Herrich-Schäffer (1851), 1845",
        ParsingJob.normalize("Leucanitis roda Herrich-Schäffer (1851) 1845"));

    assertEquals("Huaiyuanella Xing, Yan & Yin, 1984", ParsingJob.normalize("Huaiyuanella Xing, Yan&Yin, 1984"));
  }


  @Test
  public void testNormalizeStrongName() {
    assertEquals("Alstonia vieillardii Van Heurck & Müll. Arg.",
        ParsingJob.normalizeStrong("Alstonia vieillardii Van Heurck & Müll.Arg."));
    assertEquals("Nuculoidea Williams & Breger, 1916",
        ParsingJob.normalizeStrong("Nuculoidea Williams et  Breger 1916  "));
    assertEquals("Nuculoidea behrens var. christoph Williams & Breger, 1916",
        ParsingJob.normalizeStrong("Nuculoidea behrens var.christoph Williams & Breger [1916]  "));
    assertEquals("Nuculoidea Williams & Breger, 1916",
        ParsingJob.normalizeStrong(" 'Nuculoidea Williams & Breger, 1916'"));
    assertEquals("Photina (Cardioptera) burmeisteri (Westwood, 1889)",
        ParsingJob.normalizeStrong("Photina Cardioptera burmeisteri (Westwood 1889)"));
    assertEquals("Suaeda forsskahlei Schweinf.",
        ParsingJob.normalizeStrong("Suaeda forsskahlei Schweinf. ms."));
    assertEquals("Acacia bicolor Bojer", ParsingJob.normalizeStrong("Acacia bicolor Bojer ms."));
    assertEquals("Astelia alpina var. novae-hollandiae",
        ParsingJob.normalizeStrong("Astelia alpina var. novae-hollandiae"));
    // ampersand entities are handled as part of the regular html entity escaping:
    assertEquals("N. behrens Williams & amp; Breger, 1916",
        ParsingJob.normalizeStrong("  N.behrens Williams &amp;  Breger , 1916  "));
    assertEquals("N.behrens Williams & Breger , 1916",
        ParsingJob.preClean("  N.behrens Williams &amp;  Breger , 1916  "));
    assertEquals("N. behrens Williams & Breger, 1916",
        ParsingJob.normalizeStrong(ParsingJob.preClean("  N.behrens Williams &amp;  Breger , 1916  ")));
  }

  @Test
  @Ignore("needs to be verified")
  public void testNormalizeStrongNameIgnored() {
    assertEquals("Malacocarpus schumannianus (Nicolai, 1893) Britton & Rose",
        ParsingJob.normalizeStrong("Malacocarpus schumannianus (Nicolai (1893)) Britton & Rose"));
    assertEquals("Leucanitis roda (Herrich-Schäffer, 1851), 1845",
        ParsingJob.normalizeStrong("Leucanitis roda Herrich-Schäffer (1851) 1845"));
  }
}