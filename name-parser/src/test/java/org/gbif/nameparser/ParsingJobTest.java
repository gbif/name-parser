/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
  final static ParsingJob JOB = new ParsingJob("Abies", Rank.UNRANKED, null, new ParserConfigs());
  static final Pattern AUTHOR_PATTERN = Pattern.compile("^" + ParsingJob.AUTHOR + "$");
  static final Pattern AUTHORSHIP_PATTERN = Pattern.compile("^" + ParsingJob.AUTHORSHIP + "$");

  @Test
  public void infragenericPattern() throws Exception {
    Pattern IG = Pattern.compile(ParsingJob.INFRAGENERIC);

    assertTrue(IG.matcher(" ser.Jacea").find());
    assertTrue(IG.matcher(" L.subg.Jacea").find());
    assertFalse(IG.matcher("Mergus merganser Linnaeus,1758").find());
  }

  @Test
  public void testFamilyPrefixPattern() throws Exception {
    assertFalse(ParsingJob.FAMILY_PREFIX.matcher("Poaceae").find());
    assertTrue(ParsingJob.FAMILY_PREFIX.matcher("Poaceae subtrib. Cxyz").find());
  }

  @Test
  public void testPlaceholders() throws Exception {
    assertTrue(ParsingJob.PLACEHOLDER.matcher("Aster indet.").find());
    assertTrue(ParsingJob.PLACEHOLDER.matcher("Asteraceae incertae sedis").find());
    assertTrue(ParsingJob.PLACEHOLDER.matcher("unassigned Abies").find());
    assertTrue(ParsingJob.PLACEHOLDER.matcher("Unident-Boraginaceae").find());
    assertTrue(ParsingJob.PLACEHOLDER.matcher("Unident").find());
    assertTrue(ParsingJob.PLACEHOLDER.matcher("IncertaeSedis justi").find());
  }

  @Test
  public void testEpithetPattern() throws Exception {
    Pattern epi = Pattern.compile("^"+ ParsingJob.EPITHET +"$");
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
    assertAuthorTeamPattern("Jarocki or Schinz",  "Jarocki or Schinz");
    assertAuthorTeamPattern("van der Wulp",  "van der Wulp");
    assertAuthorTeamPattern("Balsamo M Fregni E Tongiorgi MA", "M.Balsamo", "E.Fregni", "M.A.Tongiorgi");
    assertAuthorTeamPattern("Walker, F.",  "F.Walker");
    assertAuthorTeamPattern("Walker, F",  "F.Walker");
    assertAuthorTeamPattern("Walker F",  "F.Walker");
    assertAuthorTeamPattern("YJ Wang & ZQ Liu", "YJ Wang", "ZQ Liu");
    assertAuthorTeamPattern("Y.-j. Wang & Z.-q. Liu", "Y.-j.Wang", "Z.-q.Liu");
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
    assertAuthorTeamPattern("Yong Wang bis, Y. Song, K. Geng & K.D. Hyde", "Yong Wang bis", "Y.Song", "K.Geng", "K.D.Hyde");
    assertAuthorTeamPattern("Sh. Kumar, R. Singh ter, Gond & Saini", "Sh.Kumar", "R.Singh ter", "Gond", "Saini");
    assertAuthorTeamPattern("R.Singh bis");
  }

  @Test
  public void testAuthorship() throws Exception {
    assertAuthorshipPattern("zur Strassen", null, "zur Strassen");
    assertAuthorshipPatternFails("Wedd. ex Sch. Bip. (");
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
    assertAuthorshipPattern("G.Don fil.");
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
    assertAuthorshipPattern("Castellano, S.L.Mill., L.Singh bis & T.N.Lakh.", null, "Castellano", "S.L.Mill.", "L.Singh bis", "T.N.Lakh.");
    assertAuthorshipPattern("Blüthgen i.l.", null, "Blüthgen i.l.");
    assertAuthorshipPattern("Vecht i.sch.", null, "Vecht i.sch.");
  }
  
  @Test
  public void testAuthor() throws InterruptedException {
    assertAuthorPattern("Y.-j. Wang");
    assertAuthorPattern("Z.-q.Liu");
    assertAuthorPattern("Van den heede");
    assertAuthorPattern("zur Strassen");
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

  @Test
  public void testPotentialName() throws Exception {
    noPotentialName("sp.nov.");
    isPotentialName("Abies");
    isPotentialName("Aba");
    isPotentialName("Abies keralia spec. nov.");
    isPotentialName("Aba");

    noPotentialName("APSE-2");
    noPotentialName("34212");
    noPotentialName("Ab123452");
    noPotentialName("Abies43");
    noPotentialName("ARV-138");
    noPotentialName("ATCC 4321");
  }

  @Test
  public void testPhraseNames() throws Exception {
    checkPattern(ParsingJob.PHRASE_NAME, "Pultenaea sp. 'Olinda' (Coveny 6616)", "Pultenaea", "sp", "'Olinda'", "Coveny 6616", null, null);
    checkPattern(ParsingJob.PHRASE_NAME, "Marsilea sp. Neutral Junction (D.E.Albrecht 9192)", "Marsilea", "sp", "Neutral Junction", "D.E.Albrecht 9192", null, null);
    checkPattern(ParsingJob.PHRASE_NAME, "Dampiera sp. Central Wheatbelt (L.W.Sage, F.Hort, C.A.Hollister LWS2321)", "Dampiera", "sp", "Central Wheatbelt", "L.W.Sage, F.Hort, C.A.Hollister LWS2321", null, null);
    checkPattern(ParsingJob.PHRASE_NAME, "Baeckea ssp. 2 (LJM 2019)", "Baeckea", "ssp", "2", "LJM 2019", null, null);
    checkPattern(ParsingJob.PHRASE_NAME, "Baeckea var 2 (LJM 2019)","Baeckea", "var", "2", "LJM 2019", null, null);
    checkPattern(ParsingJob.PHRASE_NAME, "Baeckea sp. Bunney Road (S.Patrick 4059)");
    checkPattern(ParsingJob.PHRASE_NAME, "Prostanthera sp. Bundjalung Nat. Pk. (B.J.Conn 3471)");
    checkPattern(ParsingJob.PHRASE_NAME, "Toechima sp. East Alligator (J.Russell-Smith 8418) NT Herbarium", "Toechima", "sp", "East Alligator", "J.Russell-Smith 8418", "NT Herbarium", null);
    checkPattern(ParsingJob.PHRASE_NAME, "Goodenia sp. Bachsten Creek (M.D. Barrett 685) WA Herbarium");
    checkPattern(ParsingJob.PHRASE_NAME, "Baeckea sp. Beringbooding (AR Main 11/9/1957)", "Baeckea", "sp", "Beringbooding", "AR Main 11/9/1957", null, null);
    checkPattern(ParsingJob.PHRASE_NAME, "Sida sp. Walhallow Station (C.Edgood 28/Oct/94)", "Sida", "sp", "Walhallow Station", "C.Edgood 28/Oct/94", null, null);
    checkPattern(ParsingJob.PHRASE_NAME, "Elaeocarpus sp. Rocky Creek (Hunter s.n., 16 Sep 1993)", "Elaeocarpus", "sp", "Rocky Creek", "Hunter s.n., 16 Sep 1993", null);
    checkPattern(ParsingJob.PHRASE_NAME, "Sida sp. B (C.Dunlop 1739)", "Sida", "sp", "B", "C.Dunlop 1739", null, null);
    checkPattern(ParsingJob.PHRASE_NAME, "Grevillea brachystylis subsp. Busselton (G.J.Keighery s.n. 28/8/1985)", "Grevillea brachystylis", "subsp", "Busselton", "G.J.Keighery s.n. 28/8/1985", null, null);
    checkPattern(ParsingJob.PHRASE_NAME, "Baeckea sp. Calingiri (F.Hort 1710)");
    checkPattern(ParsingJob.PHRASE_NAME, "Baeckea sp. East Yuna (R Spjut & C Edson 7077)");
    checkPattern(ParsingJob.PHRASE_NAME, "Acacia sp. Goodlands (BR Maslin 7761) [aff. resinosa]", "Acacia", "sp", "Goodlands", "BR Maslin 7761", null, "[aff. resinosa]");
    checkPattern(ParsingJob.PHRASE_NAME, "Acacia sp. Manmanning (BR Maslin 7711) [aff. multispicata]");
    checkPattern(ParsingJob.PHRASE_NAME, "Atrichornis (Rahcinta) sp Glory (BR Maslin 7711)", "Atrichornis (Rahcinta)", "sp", "Glory", "BR Maslin 7711", null);
    checkPattern(ParsingJob.PHRASE_NAME, "Acacia mutabilis subsp. Young River (G.F.Craig 2052)", "Acacia mutabilis", "subsp", "Young River", "G.F.Craig 2052", null);
    checkPattern(ParsingJob.PHRASE_NAME, "Acacia mutabilis Maslin subsp. Young River (G.F.Craig 2052)", "Acacia mutabilis Maslin", "subsp", "Young River", "G.F.Craig 2052", null);
    assertFalse(ParsingJob.PHRASE_NAME.matcher( "Elaeocarpus sp. Rocky Creek").matches()); // Looks like an author
    assertFalse(ParsingJob.PHRASE_NAME.matcher("Acacia sp. \"Morning Glory\"").matches()); // Looks like a cultivar
  }

  private static void checkPattern(Pattern pattern, String test, String... parts) {
    Matcher matcher = pattern.matcher(test);
    assertTrue(matcher.matches());
    for (int i = 0; i < parts.length; i++)
      assertEquals(parts[i], matcher.group(i + 1));
  }


  private static void checkPatternFails(Pattern pattern, String test) throws InterruptedException {
    assertFalse(pattern.matcher(JOB.normalize(test)).matches());
  }


  private static void nomStatusRemark(String remarks) throws InterruptedException {
    nomStatusRemark(remarks, remarks);
  }
  private static void nomStatusRemark(String remarks, String match) throws InterruptedException {
    Matcher m = ParsingJob.EXTRACT_NOMSTATUS.matcher(JOB.normalize(remarks));
    assertTrue (m.find());
    assertEquals(match, m.group().trim());
  }

  private static void isPotentialName(String name) {
    assertTrue(ParsingJob.POTENTIAL_NAME_PATTERN.matcher(name).find());
  }
  private static void noPotentialName(String name) {
    assertFalse(ParsingJob.POTENTIAL_NAME_PATTERN.matcher(name).find());
  }

  private static void testCultivar(String cultivar) {
    Matcher m = ParsingJob.CULTIVAR.matcher(cultivar);
    assertTrue (m.find());
  }

  private static void assertAuthorshipPattern(String authorship) throws InterruptedException {
    assertAuthorshipPattern(authorship, null, authorship);
  }

  private static void assertAuthorshipPattern(String authorship, String exAuthor, String ... authors) throws InterruptedException {
    try {
      String normed = JOB.normalize(JOB.preClean(authorship));
      Matcher m = AUTHORSHIP_PATTERN.matcher(normed);
      assertTrue(authorship, m.find());
      if (ParsingJob.LOG.isDebugEnabled()) {
        ParsingJob.logMatcher(m);
      }
      Authorship auth = ParsingJob.parseAuthorship(m.group(1), m.group(2), m.group(3));
      if (exAuthor == null) {
        assertFalse(auth.hasExAuthors());
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

  private static void assertAuthorshipPatternFails(String authorship) throws InterruptedException {
    String normed = JOB.normalize(authorship);
    Matcher m = AUTHORSHIP_PATTERN.matcher(normed);
    if (m.find()) {
      if (ParsingJob.LOG.isDebugEnabled()) {
        ParsingJob.logMatcher(m);
      }
      fail(authorship);
    }
  }


  private void assertAuthorTeamPattern(String authorship) throws InterruptedException {
    assertAuthorTeamPattern(authorship, authorship);
  }

  private void assertAuthorTeamPattern(String authorship, String ... authors) throws InterruptedException {
    String normed = JOB.normalize(authorship);
    Matcher m = ParsingJob.AUTHOR_TEAM_PATTERN.matcher(normed);
    assertTrue(authorship, m.find());
    if (ParsingJob.LOG.isDebugEnabled()) {
      ParsingJob.logMatcher(m);
    }
    Authorship auth = ParsingJob.parseAuthorship(null, m.group(0), null);
    assertEquals(Lists.newArrayList(authors), auth.getAuthors());
  }
  
  private void assertAuthorPattern(String author) throws InterruptedException {
    String normed = JOB.normalize(author);
    Matcher m = AUTHOR_PATTERN.matcher(normed);
    assertTrue(author, m.find());
    if (ParsingJob.LOG.isDebugEnabled()) {
      ParsingJob.logMatcher(m);
    }
  }

  @Test
  public void testTaxonomicNotesPattern() throws Exception {
    assertTaxNote("non(Snyder,1904)", "Centropyge fisheri non (Snyder, 1904)");
    assertTaxNote("(non Snyder,1904)", "Centropyge fisheri (non Snyder, 1904)");
    assertTaxNote("auct.amer.", "Ramaria subbotrytis (Coker) Corner 1950 auct. amer.");
    assertTaxNote("s.l.", "Ramaria subbotrytis s.l.");
    assertTaxNote("s.ampl.", "Ramaria subbotrytis (Coker) Corner 1950 s. ampl.");
    assertTaxNote("ss.auct.europ.", "Ramaria subbotrytis (Coker) Corner 1950 ss. auct. europ.");
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

  private void assertNomNote(String expectedNote, String name) throws InterruptedException {
    String note = null;
    Matcher matcher = ParsingJob.EXTRACT_NOMSTATUS.matcher(JOB.normalize(name));
    if (matcher.find()) {
      note = StringUtils.trimToNull(matcher.group(1));
    }
    assertEquals(expectedNote, note);
  }

  private void assertTaxNote(String expectedNote, String name) throws InterruptedException {
    String note = null;
    Matcher matcher = ParsingJob.EXTRACT_SENSU.matcher(JOB.normalize(name));
    if (matcher.find()) {
      note = StringUtils.trimToNull(matcher.group(1));
    }
    assertEquals(expectedNote, note);
  }

  @Test
  public void testClean1() throws Exception {
    assertEquals("", JOB.preClean(""));
    assertEquals("Hallo Spencer", JOB.preClean("Hallo Spencer "));
    assertEquals("Hallo Spencer", JOB.preClean("' 'Hallo Spencer"));
    assertEquals("Hallo Spencer 1982", JOB.preClean("'\" Hallo  Spencer 1982'"));
  }

  @Test
  public void testNormalizeName() throws InterruptedException {
    assertNormalize("Anniella nigra FISCHER 1885", "Anniella nigra Fischer 1885");
    assertNormalize("Nuculoidea Williams et  Breger 1916  ","Nuculoidea Williams&Breger 1916");
    assertNormalize("Nuculoidea behrens var.christoph Williams & Breger [1916]  ", "Nuculoidea behrens var.christoph Williams&Breger 1916");
    assertNormalize("Nuculoidea behrens var.christoph Williams & Breger [1916]  ","Nuculoidea behrens var.christoph Williams&Breger 1916");
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

  private void assertNormalize(String raw, String expected) throws InterruptedException {
    assertEquals(expected, JOB.normalize(JOB.preClean(raw)));
  }

  @Test
  public void testNormalizeStrongName() throws InterruptedException {
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
    assertNormalizeStrong("Bathylychnops chilensis Parin_NV, Belyanina_TN & Evseenko 2009", "Bathylychnops chilensis Parin_NV,Belyanina_TN&Evseenko 2009");
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

  private void assertNormalizeStrong(String raw, String expected) throws InterruptedException {
    assertEquals(expected, JOB.normalizeStrong(JOB.normalize(JOB.preClean(raw))));
  }

}