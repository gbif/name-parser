package org.gbif.nameparser;

import org.apache.commons.lang3.StringUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.util.regex.Matcher;

import static org.junit.Assert.assertEquals;


/**
 *
 */
public class NameParserPatternTest {

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
    Matcher matcher = NameParserGBIF.EXTRACT_NOMSTATUS.matcher(name);
    if (matcher.find()) {
      nomNote = (StringUtils.trimToNull(matcher.group(1)));
    }
    assertEquals(expectedNote, nomNote);
  }

  @Test
  public void testClean1() throws Exception {
    assertEquals("", NameParserGBIF.preClean(""));
    assertEquals("Hallo Spencer", NameParserGBIF.preClean("Hallo Spencer "));
    assertEquals("Hallo Spencer", NameParserGBIF.preClean("' 'Hallo Spencer"));
    assertEquals("Hallo Spencer 1982", NameParserGBIF.preClean("'\" Hallo  Spencer 1982'"));
  }


  @Test
  public void testNormalizeName() {
    assertEquals("Nuculoidea Williams et Breger, 1916", NameParserGBIF.normalize("Nuculoidea Williams et  Breger 1916  "));
    assertEquals("Nuculoidea behrens var. christoph Williams & Breger [1916]",
        NameParserGBIF.normalize("Nuculoidea behrens var.christoph Williams & Breger [1916]  "));
    assertEquals("Nuculoidea behrens var. christoph Williams & Breger [1916]",
        NameParserGBIF.normalize("Nuculoidea behrens var.christoph Williams & Breger [1916]  "));
    assertEquals(NameParserGBIF.normalize("Nuculoidea Williams & Breger, 1916  "),
        NameParserGBIF.normalize("Nuculoidea   Williams& Breger, 1916"));
    assertEquals("Asplenium ×inexpectatum (E.L. Braun, 1940) Morton (1956)",
        NameParserGBIF.normalize("Asplenium X inexpectatum (E.L. Braun 1940)Morton (1956) "));
    assertEquals("×Agropogon", NameParserGBIF.normalize(" × Agropogon"));
    assertEquals("Salix ×capreola Andersson", NameParserGBIF.normalize("Salix × capreola Andersson"));
    assertEquals("Leucanitis roda Herrich-Schäffer (1851), 1845",
        NameParserGBIF.normalize("Leucanitis roda Herrich-Schäffer (1851) 1845"));

    assertEquals("Huaiyuanella Xing, Yan & Yin, 1984", NameParserGBIF.normalize("Huaiyuanella Xing, Yan&Yin, 1984"));
  }


  @Test
  public void testNormalizeStrongName() {
    assertEquals("Nuculoidea Williams & Breger, 1916",
        NameParserGBIF.normalizeStrong("Nuculoidea Williams et  Breger 1916  "));
    assertEquals("Nuculoidea behrens var. christoph Williams & Breger, 1916",
        NameParserGBIF.normalizeStrong("Nuculoidea behrens var.christoph Williams & Breger [1916]  "));
    assertEquals("Nuculoidea Williams & Breger, 1916",
        NameParserGBIF.normalizeStrong(" 'Nuculoidea Williams & Breger, 1916'"));
    assertEquals("Photina (Cardioptera) burmeisteri (Westwood, 1889)",
        NameParserGBIF.normalizeStrong("Photina Cardioptera burmeisteri (Westwood 1889)"));
    assertEquals("Suaeda forsskahlei Schweinf.",
        NameParserGBIF.normalizeStrong("Suaeda forsskahlei Schweinf. ms."));
    assertEquals("Acacia bicolor Bojer", NameParserGBIF.normalizeStrong("Acacia bicolor Bojer ms."));
    assertEquals("Astelia alpina var. novae-hollandiae",
        NameParserGBIF.normalizeStrong("Astelia alpina var. novae-hollandiae"));
    // ampersand entities are handled as part of the regular html entity escaping:
    assertEquals("N. behrens Williams & amp; Breger, 1916",
        NameParserGBIF.normalizeStrong("  N.behrens Williams &amp;  Breger , 1916  "));
    assertEquals("N.behrens Williams & Breger , 1916",
        NameParserGBIF.preClean("  N.behrens Williams &amp;  Breger , 1916  "));
    assertEquals("N. behrens Williams & Breger, 1916",
        NameParserGBIF.normalizeStrong(NameParserGBIF.preClean("  N.behrens Williams &amp;  Breger , 1916  ")));
  }

  @Test
  @Ignore("needs to be verified")
  public void testNormalizeStrongNameIgnored() {
    assertEquals("Malacocarpus schumannianus (Nicolai, 1893) Britton & Rose",
        NameParserGBIF.normalizeStrong("Malacocarpus schumannianus (Nicolai (1893)) Britton & Rose"));
    assertEquals("Leucanitis roda (Herrich-Schäffer, 1851), 1845",
        NameParserGBIF.normalizeStrong("Leucanitis roda Herrich-Schäffer (1851) 1845"));
  }

}
