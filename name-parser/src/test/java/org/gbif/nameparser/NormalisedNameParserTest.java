package org.gbif.nameparser;

import com.google.common.collect.Lists;
import org.gbif.nameparser.api.Authorship;
import org.gbif.nameparser.api.ParsedName;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NormalisedNameParserTest {

  @Test
  public void testEpithetPattern() throws Exception {
    Pattern epi = Pattern.compile("^"+ NormalisedNameParser.EPHITHET+"$");
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
  }

  private void assertAuthorTeamPattern(String authorship) {
    assertAuthorTeamPattern(authorship, null, authorship);
  }

  private void assertAuthorTeamPattern(String authorship, String exAuthor, String ... authors) {
    Matcher m = NormalisedNameParser.AUTHORSHIP_PATTERN.matcher(authorship);
    assertTrue(m.find());
    if (NormalisedNameParser.LOG.isDebugEnabled()) {
      NormalisedNameParser.logMatcher(m);
    }
    Authorship auth = NormalisedNameParser.parseAuthorship(m.group(1), m.group(2), m.group(3));
    if (exAuthor == null) {
      assertTrue(auth.getExAuthors().isEmpty());
    } else {
      assertEquals(exAuthor, auth.getExAuthors().get(0));
      assertEquals(1, auth.getExAuthors().size());
    }
    assertEquals(Lists.newArrayList(authors), auth.getAuthors());
  }

  @Test
  public void timeoutLongNames() throws Exception {
    final int timeout = 50;
    NormalisedNameParser parser = new NormalisedNameParser(timeout);

    String name = "Equicapillimyces hongkongensis S.S.Y. Wong, A.H.Y. Ngan, Riggs, J.L.L. Teng, G.K.Y. Choi, R.W.S. Poon, J.J.Y. Hui, F.J. Low, Luk &";

    ParsedName n = new ParsedName();
    final long pStart = System.currentTimeMillis();
    assertFalse("No timeout happening for long running parsing", parser.parseNormalisedName(n, name, null));
    final long duration = System.currentTimeMillis() - pStart;

    // the duration is the timeout PLUS some initialization overhead thats why we add 50ms to it
    assertTrue("No timeout happening for long running parsing", duration < 50 + timeout);
  }

}