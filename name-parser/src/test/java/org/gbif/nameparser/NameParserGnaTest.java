package org.gbif.nameparser;

import org.gbif.nameparser.api.NameParser;
import org.gbif.nameparser.api.ParsedName;
import org.gbif.nameparser.api.Rank;
import org.gbif.nameparser.api.UnparsableNameException;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import static org.gbif.nameparser.api.Rank.*;

/**
 * Tests imported from the gnparser project's test_data.md, see
 * https://raw.githubusercontent.com/gnames/gnparser/refs/heads/main/testdata/test_data.md
 *
 * Each test method corresponds to one ### group in that file. Cases with parsed=false
 * or shapes that cannot be expressed by the GBIF model (graft chimeras, hybrid formulae,
 * cultivars, viruses, candidatus, surrogates, etc.) are skipped with a comment.
 *
 * The structural assertions, rank and authorship breakdown (combination + basionym
 * authors with year) are extracted from the JSON details block in the source file and
 * compared against the GBIF parser using {@link NameAssertion}.
 *
 * Some expectations have been manually adjusted to match the GBIF parser expectations
 * as the GNA cases often are problems with OCR and text mining.
 */
@Ignore("GN tests are not yet fully implemented")
public class NameParserGnaTest {

  private final NameParser parser = new NameParserImpl();

  @Test
  public void uninomialsWithoutAuthorship() throws Exception {
      // group: Uninomials without authorship
      assertName("Pseudocercospora", "Pseudocercospora")
          .monomial("Pseudocercospora");
  }

  @Test
  public void uninomialsWithAuthorship() throws Exception {
      // group: Uninomials with authorship
      assertName("Tremoctopus violaceus Delle Chiaje, 1830", "Tremoctopus violaceus")
          .species("Tremoctopus", "violaceus")
          .combAuthors("1830", "Delle Chiaje");
      assertName("Protis hydrothermica ten Hove & Zibrowius, 1986", "Protis hydrothermica")
          .species("Protis", "hydrothermica")
          .combAuthors("1986", "ten Hove", "Zibrowius");
      assertName("Cladoniicola staurospora Diederich, van den Boom & Aptroot 2001", "Cladoniicola staurospora")
          .species("Cladoniicola", "staurospora")
          .combAuthors("2001", "Diederich", "van den Boom", "Aptroot");
      assertName("Stagonospora polyspora M.T. Lucas & Sousa da Câmara 1934", "Stagonospora polyspora")
          .species("Stagonospora", "polyspora")
          .combAuthors("1934", "M.T. Lucas", "Sousa da Câmara");
      assertName("Stagonospora polyspora M.T. Lucas et Sousa da Câmara 1934", "Stagonospora polyspora")
          .species("Stagonospora", "polyspora")
          .combAuthors("1934", "M.T. Lucas", "Sousa da Câmara");
      assertName("Pseudocercospora dendrobii U. Braun & Crous 2003", "Pseudocercospora dendrobii")
          .species("Pseudocercospora", "dendrobii")
          .combAuthors("2003", "U. Braun", "Crous");
      assertName("Abaxisotima acuminata (Wang, Yuwen & Xiangwei Liu 1996)", "Abaxisotima acuminata")
          .species("Abaxisotima", "acuminata")
          .basAuthors("1996", "Wang", "Yuwen", "Xiangwei Liu");
      assertName("Aboilomimus sichuanensis ornatus Liu, Xiang-wei, M. Zhou, W Bi & L. Tang, 2009", "Aboilomimus sichuanensis ornatus")
          .infraSpecies("Aboilomimus", "sichuanensis", INFRASPECIFIC_NAME, "ornatus")
          .combAuthors("2009", "Liu", "Xiang-wei", "M. Zhou", "W. Bi", "L. Tang");
      assertName("Pseudocercospora Speg.", "Pseudocercospora")
          .monomial("Pseudocercospora")
          .combAuthors(null, "Speg.");
      assertName("Döringina Ihering 1929 (synonym)", "Doeringina")
          .monomial("Doeringina")
          .combAuthors("1929", "Ihering");
      assertName("Pseudocercospora Speg., Francis Jack.-Drake.", "Pseudocercospora")
          .monomial("Pseudocercospora")
          .combAuthors(null, "Speg.", "Francis Jack.-Drake.");
      assertName("Aaaba de Laubenfels, 1936", "Aaaba")
          .monomial("Aaaba")
          .combAuthors("1936", "de Laubenfels");
      assertName("Abbottia F. von Mueller, 1875", "Abbottia")
          .monomial("Abbottia")
          .combAuthors("1875", "F.von Mueller");
      assertName("Abella von Heyden, 1826", "Abella")
          .monomial("Abella")
          .combAuthors("1826", "von Heyden");
      assertName("Micropleura v Linstow 1906", "Micropleura")
          .monomial("Micropleura")
          .combAuthors("1906", "v Linstow");
      assertName("Pseudocercospora Speg. 1910", "Pseudocercospora")
          .monomial("Pseudocercospora")
          .combAuthors("1910", "Speg.");
      assertName("Pseudocercospora Spegazzini, 1910", "Pseudocercospora")
          .monomial("Pseudocercospora")
          .combAuthors("1910", "Spegazzini");
      assertName("Rhynchonellidae d'Orbigny 1847", "Rhynchonellidae")
          .monomial("Rhynchonellidae")
          .combAuthors("1847", "d'Orbigny");
      assertName("Rhynchonellidae d‘Orbigny 1847", "Rhynchonellidae")
          .monomial("Rhynchonellidae")
          .combAuthors("1847", "d'Orbigny");
      assertName("Rhynchonellidae d’Orbigny 1847", "Rhynchonellidae")
          .monomial("Rhynchonellidae")
          .combAuthors("1847", "d'Orbigny");
      assertName("Ataladoris Iredale & O'Donoghue 1923", "Ataladoris")
          .monomial("Ataladoris")
          .combAuthors("1923", "Iredale", "O'Donoghue");
      assertName("Anteplana le Renard 1995", "Anteplana")
          .monomial("Anteplana")
          .combAuthors("1995", "le Renard");
      assertName("Candinia le Renard, Sabelli & Taviani 1996", "Candinia")
          .monomial("Candinia")
          .combAuthors("1996", "le Renard", "Sabelli", "Taviani");
      assertName("Polypodium le-sourdianum Fourn.", "Polypodium")
          .species("Polypodium", "le-sourdianum")
          .combAuthors(null, "le Sourdianum Fourn.");
  }

  @Test
  public void twoLetterGenusNamesLegacyGeneraNotAllowedAnymore() throws Exception {
      // group: Two-letter genus names (legacy genera, not allowed anymore)
      assertName("Ca Dyar 1914", "Ca")
          .monomial("Ca")
          .combAuthors("1914", "Dyar");
      assertName("Ea Distant 1911", "Ea")
          .monomial("Ea")
          .combAuthors("1911", "Distant");
      assertName("Do", "Do")
          .monomial("Do");
      assertName("Ge Nicéville 1895", "Ge")
          .monomial("Ge")
          .combAuthors("1895", "Nicéville");
      assertName("Ia Thomas 1902", "Ia")
          .monomial("Ia")
          .combAuthors("1902", "Thomas");
      assertName("Io Lea 1831", "Io")
          .monomial("Io")
          .combAuthors("1831", "Lea");
      assertName("Io Blanchard 1852", "Io")
          .monomial("Io")
          .combAuthors("1852", "Blanchard");
      assertName("Ix Bergroth 1916", "Ix")
          .monomial("Ix")
          .combAuthors("1916", "Bergroth");
      assertName("Lo Seale 1906", "Lo")
          .monomial("Lo")
          .combAuthors("1906", "Seale");
      assertName("Oa Girault 1929", "Oa")
          .monomial("Oa")
          .combAuthors("1929", "Girault");
      assertName("Oo", "Oo")
          .monomial("Oo");
      assertName("Nu", "Nu")
          .monomial("Nu");
      assertName("Ra Whitley 1931", "Ra")
          .monomial("Ra")
          .combAuthors("1931", "Whitley");
      assertName("Ty Bory de St. Vincent 1827", "Ty")
          .monomial("Ty")
          .combAuthors("1827", "Bory de St.Vincent");
      assertName("Ua Girault 1929", "Ua")
          .monomial("Ua")
          .combAuthors("1929", "Girault");
      assertName("Aa Baker 1940", "Aa")
          .monomial("Aa")
          .combAuthors("1940", "Baker");
      assertName("Ja Uéno 1955", "Ja")
          .monomial("Ja")
          .combAuthors("1955", "Uéno");
      assertName("Zu Walters & Fitch 1960", "Zu")
          .monomial("Zu")
          .combAuthors("1960", "Walters", "Fitch");
      assertName("La Bleszynski 1966", "La")
          .monomial("La")
          .combAuthors("1966", "Bleszynski");
      assertName("Qu Durkoop", "Qu")
          .monomial("Qu")
          .combAuthors(null, "Durkoop");
      assertName("As Slipinski 1982", "As")
          .monomial("As")
          .combAuthors("1982", "Slipinski");
      assertName("Ba Solem 1983", "Ba")
          .monomial("Ba")
          .combAuthors("1983", "Solem");
  }

  @Test
  public void combinationOfTwoUninomials() throws Exception {
      // group: Combination of two uninomials
      assertName("Agaricus tr. Hypholoma Fr.", "Hypholoma")
          .monomial("Hypholoma")
          .rank(TRIBE)
          .combAuthors(null, "Fr.");
      assertName("Agaricus tr Hypholoma Fr.", "Hypholoma")
          .monomial("Hypholoma")
          .rank(TRIBE)
          .combAuthors(null, "Fr.");
      assertName("Agaricus subtr. Oesypii Fr.", "Oesypii")
          .monomial("Oesypii")
          .rank(SUBTRIBE)
          .combAuthors(null, "Fr.");
      assertName("Agaricus subtr Oesypii Fr.", "Oesypii")
          .monomial("Oesypii")
          .rank(SUBTRIBE)
          .combAuthors(null, "Fr.");
      assertName("Poaceae subtrib. Scolochloinae Soreng", "Scolochloinae")
          .monomial("Scolochloinae")
          .rank(SUBTRIBE)
          .combAuthors(null, "Soreng");
      assertName("Zygophyllaceae subfam. Tribuloideae D.M.Porter", "Tribuloideae")
          .monomial("Tribuloideae")
          .rank(SUBFAMILY)
          .combAuthors(null, "D.M. Porter");
      assertName("Cordia (Adans.) Kuntze sect. Salimori", "Salimori")
          .monomial("Salimori");
      assertName("Cordia sect. Salimori (Adans.) Kuntz", "Salimori")
          .monomial("Salimori")
          .combAuthors(null, "Kuntz")
          .basAuthors(null, "Adans.");
      assertName("Poaceae supertrib. Arundinarodae L.Liu", "Arundinarodae")
          .monomial("Arundinarodae")
          .rank(SUPERTRIBE)
          .combAuthors(null, "L. Liu");
      assertName("Alchemilla subsect. Sericeae A.Plocek", "Sericeae")
          .monomial("Sericeae")
          .combAuthors(null, "A. Plocek");
      assertName("subgen. Psammophrynopsis Koch, 1953", "Psammophrynopsis")
          .monomial("Psammophrynopsis")
          .rank(SUBGENUS)
          .combAuthors("1953", "Koch");
      assertName("Hymenophyllum subgen. Hymenoglossum (Presl) R.M.Tryon & A.Tryon", "Hymenoglossum")
          .monomial("Hymenoglossum")
          .rank(SUBGENUS)
          .combAuthors(null, "R.M. Tryon", "A. Tryon")
          .basAuthors(null, "Presl");
      assertName("Pereskia subg. Maihuenia Philippi ex F.A.C.Weber, 1898", "Maihuenia")
          .monomial("Maihuenia")
          .rank(SUBGENUS)
          .combAuthors(null, "Philippi");
      assertName("Aconitum ser. Tangutica W.T. Wang", "Tangutica")
          .monomial("Tangutica")
          .combAuthors(null, "W.T.Wang");
      assertName("Calathus (Lindrothius) KURNAKOV 1961", "Lindrothius")
          .monomial("Lindrothius")
          .rank(SUBGENUS)
          .combAuthors("1961", "Kurnakov");
      assertName("Eucalyptus subser. Regulares Brooker", "Regulares")
          .monomial("Regulares")
          .combAuthors(null, "Brooker");
      assertName("Rosa div. Caninae Lindl.", "Caninae")
          .monomial("Caninae")
          .combAuthors(null, "Lindl.");
      assertName("Rosa div Caninae Lindl.", "Caninae")
          .monomial("Caninae")
          .combAuthors(null, "Lindl.");
      assertName("Aaleniella (Danocythere)", "Danocythere")
          .monomial("Danocythere")
          .rank(SUBGENUS);
  }

  @Test
  public void icnNamesThatLookLikeCombinedUninomialsForIczn() throws Exception {
      // group: ICN names that look like combined uninomials for ICZN
      assertName("Clathrotropis (Bentham) Harms in Dalla Torre & Harms, 1901", "Clathrotropis")
          .monomial("Clathrotropis")
          .combAuthors(null, "Harms")
          .basAuthors(null, "Bentham");
      assertName("Humiriastrum (Urban) Cuatrecasas, 1961", "Humiriastrum")
          .monomial("Humiriastrum")
          .combAuthors("1961", "Cuatrecasas")
          .basAuthors(null, "Urban");
      assertName("Pampocactus (Doweld) Doweld", "Pampocactus")
          .monomial("Pampocactus")
          .combAuthors(null, "Doweld")
          .basAuthors(null, "Doweld");
      assertName("Pampocactus (Doweld)", "Pampocactus")
          .monomial("Pampocactus")
          .basAuthors(null, "Doweld");
      assertName("Drepanolejeunea (Spruce) (Steph.)", "Drepanolejeunea")
          .monomial("Drepanolejeunea")
          .basAuthors(null, "Spruce");
      assertName("Glaphyropteris (Fée) C.Presl ex Fée", "Glaphyropteris")
          .monomial("Glaphyropteris")
          .combAuthors(null, "C. Presl")
          .basAuthors(null, "Fée");
  }

  @Test
  public void binomialsWithoutAuthorship() throws Exception {
      // group: Binomials without authorship
      assertName("Notopholia corrusca", "Notopholia corrusca")
          .species("Notopholia", "corrusca");
      assertName("Cyathicula scelobelonium", "Cyathicula scelobelonium")
          .species("Cyathicula", "scelobelonium");
      assertName("Pseudocercospora     dendrobii", "Pseudocercospora dendrobii")
          .species("Pseudocercospora", "dendrobii");
      assertName("Cucurbita pepo", "Cucurbita pepo")
          .species("Cucurbita", "pepo");
      assertName("Hirsutëlla male", "Hirsutëlla male")
          .species("Hirsutella", "male");
      assertName("Aëtosaurus ferratus", "Aëtosaurus ferratus")
          .species("Aetosaurus", "ferratus");
      assertName("Remera cvancarai", "Remera cvancarai")
          .species("Remera", "cvancarai");
  }

  @Test
  public void binomialsWithAuthorship() throws Exception {
      // group: Binomials with authorship
      assertName("Gazella farasani Thouless, al Bassri, 1991", "Gazella farasani")
          .species("Gazella", "farasani")
          .combAuthors("1991", "Thouless", "al Bassri");
      assertName("Anomalurus laticeps Aguilar-Amat i Banús, 1922", "Anomalurus laticeps")
          .species("Anomalurus", "laticeps")
          .combAuthors("1922", "Aguilar-Amat i Banús");
      assertName("Glis wagneri Đulić & Tortić, 1960", "Glis wagneri")
          .species("Glis", "wagneri")
          .combAuthors("1960", "Đulić", "Tortić");
      assertName("Mico rondoni Ferrari, Sena, M. P. C. Schneider, & e Silva Júnior, 2010", "Mico rondoni")
          .species("Mico", "rondoni")
          .combAuthors("2010", "Ferrari", "Sena", "M.P.C.Schneider", "e Silva Júnior");
      assertName("Trachypithecus caudalis (Đào Văn Tiến, 1977)", "Trachypithecus caudalis")
          .species("Trachypithecus", "caudalis")
          .basAuthors("1977", "Đào Văn Tiến");
      assertName("Cymatium raderi D’Attilio & Myers, 1984", "Cymatium raderi")
          .species("Cymatium", "raderi")
          .combAuthors("1984", "D'Attilio", "Myers");
      assertName("Melania testudinaria Von dem Busch, 1842", "Melania testudinaria")
          .species("Melania", "testudinaria")
          .combAuthors("1842", "Von dem Busch");
      assertName("Cryptopleura farlowiana (J.Agardh) ver Steeg & Jossly", "Cryptopleura farlowiana")
          .species("Cryptopleura", "farlowiana")
          .combAuthors(null, "ver Steeg", "Jossly")
          .basAuthors(null, "J.Agardh");
      assertName("Pyxilla caput avis J.-J.Brun", "Pyxilla caput avis")
          .infraSpecies("Pyxilla", "caput", INFRASPECIFIC_NAME, "avis")
          .combAuthors(null, "J.-J.Brun");
      assertName("Muscicapa randi Amadon & duPont, 1970", "Muscicapa randi")
          .species("Muscicapa", "randi")
          .combAuthors("1970", "Amadon", "duPont");
      assertName("Scytalopus alvarezlopezi Stiles, Laverde-R. & Cadena 2017", "Scytalopus alvarezlopezi")
          .species("Scytalopus", "alvarezlopezi")
          .combAuthors("2017", "Stiles", "Laverde-R.", "Cadena");
      assertName("Carabus (Tanaocarabus) hendrichsi Bolvar y Pieltain, Rotger & Coronado 1967", "Carabus hendrichsi")
          .species("Carabus", "hendrichsi")
          .combAuthors("1967", "Bolvar", "Pieltain", "Rotger", "Coronado");
      assertName("Nemcia epacridoides (Meissner)Crisp", "Nemcia epacridoides")
          .species("Nemcia", "epacridoides")
          .combAuthors(null, "Crisp")
          .basAuthors(null, "Meissner");
      assertName("Pseudocercospora dendrobii Goh & W.H. Hsieh 1990", "Pseudocercospora dendrobii")
          .species("Pseudocercospora", "dendrobii")
          .combAuthors("1990", "Goh", "W.H.Hsieh");
      assertName("Pseudocercospora dendrobii Goh and W.H. Hsieh 1990", "Pseudocercospora dendrobii")
          .species("Pseudocercospora", "dendrobii")
          .combAuthors("1990", "Goh", "W.H.Hsieh");
      assertName("Pseudocercospora dendrobii Goh et W.H. Hsieh 1990", "Pseudocercospora dendrobii")
          .species("Pseudocercospora", "dendrobii")
          .combAuthors("1990", "Goh", "W.H.Hsieh");
      assertName("Schottera nicaeënsis (J.V. Lamouroux ex Duby) Guiry & Hollenberg", "Schottera nicaeensis")
          .species("Schottera", "nicaeensis")
          .combAuthors(null, "Guiry", "Hollenberg")
          .basAuthors(null, "J.V.Lamouroux");
      assertName("Laevapex vazi dos Santos, 1989", "Laevapex vazi")
          .species("Laevapex", "vazi")
          .combAuthors("1989", "dos Santos");
      assertName("Periclimenaeus aurae dos Santos, Calado & Araújo, 2008", "Periclimenaeus aurae")
          .species("Periclimenaeus", "aurae")
          .combAuthors("2008", "dos Santos", "Calado", "Araújo");
      assertName("Nototriton matama Boza-Oviedo, Rovito, Chaves, García-Rodríguez, Artavia, Bolaños, and Wake, 2012", "Nototriton matama")
          .species("Nototriton", "matama")
          .combAuthors("2012", "Boza-Oviedo", "Rovito", "Chaves", "García-Rodríguez", "Artavia", "Bolaños", "Wake");
      assertName("Architectonica offlexa Iredale, 1931", "Architectonica offlexa")
          .species("Architectonica", "offlexa")
          .combAuthors("1931", "Iredale");
      assertName("Maracanda amoena Mc'Lach", "Maracanda amoena")
          .species("Maracanda", "amoena")
          .combAuthors(null, "Mc'Lach");
      assertName("Maracanda amoena Mc’Lach", "Maracanda amoena")
          .species("Maracanda", "amoena")
          .combAuthors(null, "Mc'Lach");
      assertName("Tridentella tangeroae Bruce, 198?", "Tridentella tangeroae")
          .species("Tridentella", "tangeroae")
          .combAuthors("198?", "Bruce");
      assertName("Calobota acanthoclada (Dinter) Boatwr. & B.-E.van Wyk", "Calobota acanthoclada")
          .species("Calobota", "acanthoclada")
          .combAuthors(null, "Boatwr.", "B.-E.van Wyk")
          .basAuthors(null, "Dinter");
      assertName("Zanthopsis bispinosa M'Coy, 1849", "Zanthopsis bispinosa")
          .species("Zanthopsis", "bispinosa")
          .combAuthors("1849", "M'Coy");
      assertName("Scilla rupestris v.d. Merwe", "Scilla rupestris")
          .species("Scilla", "rupestris")
          .combAuthors(null, "v.d.Merwe");
      assertName("Bembix bidentata v.d.L.", "Bembix bidentata")
          .species("Bembix", "bidentata")
          .combAuthors(null, "v.d.L.");
      assertName("Pompilus cinctellus v. d. L.", "Pompilus cinctellus")
          .species("Pompilus", "cinctellus")
          .combAuthors(null, "v.d.L.");
      assertName("Setaphis viridis v. d.G.", "Setaphis viridis")
          .species("Setaphis", "viridis")
          .combAuthors(null, "v.d.G.");
      assertName("Coleophora mendica Baldizzone & v. d.Wolf 2000", "Coleophora mendica")
          .species("Coleophora", "mendica")
          .combAuthors("2000", "Baldizzone", "v.d.Wolf");
      assertName("Psoronaias semigranosa von dem Busch in Philippi, 1845", "Psoronaias semigranosa")
          .species("Psoronaias", "semigranosa")
          .combAuthors(null, "von dem Busch");
      assertName("Phora sororcula v d Wulp 1871", "Phora sororcula")
          .species("Phora", "sororcula")
          .combAuthors("1871", "v d Wulp");
      assertName("Aeolothrips andalusiacus zur Strassen 1973", "Aeolothrips andalusiacus")
          .species("Aeolothrips", "andalusiacus")
          .combAuthors("1973", "zur Strassen");
      assertName("Orthosia kindermannii Fischer v. Roslerstamm, 1837", "Orthosia kindermannii")
          .species("Orthosia", "kindermannii")
          .combAuthors("1837", "Fischer v.Roslerstamm");
      assertName("Boreophilia nomensis (Casey, 1910)", "Boreophilia nomensis")
          .species("Boreophilia", "nomensis")
          .basAuthors("1910", "Casey");
      assertName("Nereidavus kulkovi Kul'kov in Kul'kov & Obut, 1973", "Nereidavus kulkovi")
          .species("Nereidavus", "kulkovi")
          .combAuthors(null, "Kul'kov");
      assertName("Xylaria potentillae A S. Xu", "Xylaria potentillae")
          .species("Xylaria", "potentillae")
          .combAuthors(null, "A S.Xu");
      assertName("Pseudocyrtopora el Hajjaji 1987", "Pseudocyrtopora")
          .monomial("Pseudocyrtopora")
          .combAuthors("1987", "el Hajjaji");
      assertName("Geositta poeciloptera (zu Wied-Neuwied, 1830)", "Geositta poeciloptera")
          .species("Geositta", "poeciloptera")
          .basAuthors("1830", "zu Wied-Neuwied");
      assertName("Abacetus laevicollis de Chaudoir, 1869", "Abacetus laevicollis")
          .species("Abacetus", "laevicollis")
          .combAuthors("1869", "de Chaudoir");
      assertName("Gastrosericus eremorum von Beaumont 1955", "Gastrosericus eremorum")
          .species("Gastrosericus", "eremorum")
          .combAuthors("1955", "von Beaumont");
      assertName("Agaricus squamula Berk. & M.A. Curtis 1860", "Agaricus squamula")
          .species("Agaricus", "squamula")
          .combAuthors("1860", "Berk.", "M.A.Curtis");
      assertName("Peltula coriacea Büdel, Henssen & Wessels 1986", "Peltula coriacea")
          .species("Peltula", "coriacea")
          .combAuthors("1986", "Büdel", "Henssen", "Wessels");
      assertName("Tuber liui A S. Xu 1999", "Tuber liui")
          .species("Tuber", "liui")
          .combAuthors("1999", "A S.Xu");
      assertName("Lecanora wetmorei Śliwa 2004", "Lecanora wetmorei")
          .species("Lecanora", "wetmorei")
          .combAuthors("2004", "Śliwa");
      assertName("Vachonobisium troglophilum Vitali-di Castri, 1963", "Vachonobisium troglophilum")
          .species("Vachonobisium", "troglophilum")
          .combAuthors("1963", "Vitali-di Castri");
      assertName("Hyalesthes angustula Horvßth, 1909", "Hyalesthes angustula")
          .species("Hyalesthes", "angustula")
          .combAuthors("1909", "Horvßth");
      assertName("Platypus bicaudatulus Schedl (1935h)", "Platypus bicaudatulus")
          .species("Platypus", "bicaudatulus")
          .combAuthors("1935", "Schedl");
      assertName("Platypus bicaudatulus Schedl (1935)", "Platypus bicaudatulus")
          .species("Platypus", "bicaudatulus")
          .combAuthors("1935", "Schedl");
      assertName("Platypus bicaudatulus Schedl 1935", "Platypus bicaudatulus")
          .species("Platypus", "bicaudatulus")
          .combAuthors("1935", "Schedl");
      assertName("Platypus bicaudatulus Schedl, 1935h", "Platypus bicaudatulus")
          .species("Platypus", "bicaudatulus")
          .combAuthors("1935", "Schedl");
      assertName("Rotalina cultrata d'Orb. 1840", "Rotalina cultrata")
          .species("Rotalina", "cultrata")
          .combAuthors("1840", "d'Orb.");
      assertName("Stylosanthes guianensis (Aubl.) Sw. var. robusta L.'t Mannetje", "Stylosanthes guianensis robusta")
          .infraSpecies("Stylosanthes", "guianensis", VARIETY, "robusta")
          .combAuthors(null, "L.'t Mannetje");
      assertName("Doxander vittatus entropi (Man in 't Veld & Visser, 1993)", "Doxander vittatus entropi")
          .infraSpecies("Doxander", "vittatus", INFRASPECIFIC_NAME, "entropi")
          .basAuthors(null, "Man");
      assertName("Elaeagnus triflora Roxb. var. brevilimbatus E.'t Hart", "Elaeagnus triflora brevilimbatus")
          .infraSpecies("Elaeagnus", "triflora", VARIETY, "brevilimbatus")
          .combAuthors(null, "E.'t Hart");
      assertName("Laevistrombus guidoi (Man in't Veld & De Turck, 1998)", "Laevistrombus guidoi")
          .species("Laevistrombus", "guidoi")
          .basAuthors("1998", "Man in't Veld", "De Turck");
      assertName("Strombus guidoi Man in't Veld & De Turck, 1998", "Strombus guidoi")
          .species("Strombus", "guidoi")
          .combAuthors("1998", "Man in't Veld", "De Turck");
      assertName("Strombus vittatus entropi Man in't Veld & Visser, 1993", "Strombus vittatus entropi")
          .infraSpecies("Strombus", "vittatus", INFRASPECIFIC_NAME, "entropi")
          .combAuthors("1993", "Man in't Veld", "Visser");
      assertName("Velutina haliotoides (Linnaeus, 1758),", "Velutina haliotoides")
          .species("Velutina", "haliotoides")
          .basAuthors("1758", "Linnaeus");
      assertName("Hennediella microphylla (R.Br.bis) Paris", "Hennediella microphylla")
          .species("Hennediella", "microphylla")
          .combAuthors(null, "Paris")
          .basAuthors(null, "R.Br.bis");
      assertName("Pseudocercosporella endophytica Crous & H. Sm. ter", "Pseudocercosporella endophytica")
          .species("Pseudocercosporella", "endophytica")
          .combAuthors(null, "Crous", "H.Sm.ter");
      assertName("Kudoa amazonica Velasco, Sindeaux Neto, Videira, de Cássia Silva do Nascimento, Gonçalves & Matos, 2019", "Kudoa amazonica")
          .species("Kudoa", "amazonica")
          .combAuthors("2019", "Velasco", "Sindeaux Neto", "Videira", "de Cássia Silva do Nascimento", "Gonçalves", "Matos");
      assertName("Branchinecta papillata Rogers, de los Rios & Zuniga, 2008", "Branchinecta papillata")
          .species("Branchinecta", "papillata")
          .combAuthors("2008", "Rogers", "de los Rios", "Zuniga");
      assertName("Gerrhonotus lazcanoi Banda-Leal, Manuel Nevárez-de los Reyes and Bryson, 2017", "Gerrhonotus lazcanoi")
          .species("Gerrhonotus", "lazcanoi")
          .combAuthors("2017", "Banda-Leal", "Manuel Nevárez-de los Reyes", "Bryson");
      assertName("Lynceus huentelauquensis  Sigvardt, Rogers, De los Ríos, Palero, and Olesen, 2019", "Lynceus huentelauquensis")
          .species("Lynceus", "huentelauquensis")
          .combAuthors("2019", "Sigvardt", "Rogers", "De los Ríos", "Palero", "Olesen");
      assertName("Echiophis brunneus (Castro-Aguirre & Suárez de los Cobos, 1983)", "Echiophis brunneus")
          .species("Echiophis", "brunneus")
          .basAuthors("1983", "Castro-Aguirre", "Suárez de los Cobos");
  }

  @Test
  public void binomialsWithAnAbbreviatedGenus() throws Exception {
      // group: Binomials with an abbreviated genus
      assertName("M. alpium", "M. alpium")
          .species("M.", "alpium");
      assertName("Mo. alpium (Osbeck, 1778)", "Mo. alpium")
          .species("Mo.", "alpium")
          .basAuthors("1778", "Osbeck");
  }

  @Test
  public void binomialsWithAbbreviatedSubgenus() throws Exception {
      // group: Binomials with abbreviated subgenus
      assertName("Phalaena (Tin.) guttella Fab.", "Phalaena guttella")
          .species("Phalaena", "guttella")
          .combAuthors(null, "Fab.");
      assertName("Gahrliepia (G.) tessellata Traub & Morrow 1955", "Gahrliepia tessellata")
          .species("Gahrliepia", "tessellata")
          .combAuthors("1955", "Traub", "Morrow");
      // skipped: Bosmina (Eubosmina) coregoni x B. (E.) longispina
      assertName("Simia (Cercop.) nasuus Kerr 1792", "Simia nasuus")
          .species("Simia", "nasuus")
          .combAuthors("1792", "Kerr");
  }

  @Test
  public void binomialsWithBasionymAndCombinationAuthors() throws Exception {
      // group: Binomials with basionym and combination authors
      assertName("Yarrowia lipolytica var. lipolytica (Wick., Kurtzman & E.A. Herrm.) Van der Walt & Arx 1981", "Yarrowia lipolytica lipolytica")
          .infraSpecies("Yarrowia", "lipolytica", VARIETY, "lipolytica")
          .combAuthors("1981", "Van der Walt", "Arx")
          .basAuthors(null, "Wick.", "Kurtzman", "E.A.Herrm.");
      assertName("Pseudocercospora dendrobii(H.C.     Burnett)U. Braun & Crous     2003", "Pseudocercospora dendrobii")
          .species("Pseudocercospora", "dendrobii")
          .combAuthors("2003", "U.Braun", "Crous")
          .basAuthors(null, "H.C.Burnett");
      assertName("Pseudocercospora dendrobii(H.C.     Burnett, 1873)U. Braun & Crous     2003", "Pseudocercospora dendrobii")
          .species("Pseudocercospora", "dendrobii")
          .combAuthors("2003", "U.Braun", "Crous")
          .basAuthors("1873", "H.C.Burnett");
      assertName("Pseudocercospora dendrobii(H.C.     Burnett 1873)U. Braun & Crous ,    2003", "Pseudocercospora dendrobii")
          .species("Pseudocercospora", "dendrobii")
          .combAuthors("2003", "U.Braun", "Crous")
          .basAuthors("1873", "H.C.Burnett");
      assertName("Sedella pumila (Benth.) Britton & Rose", "Sedella pumila")
          .species("Sedella", "pumila")
          .combAuthors(null, "Britton", "Rose")
          .basAuthors(null, "Benth.");
      assertName("Impatiens nomenyae Eb.Fisch. & Raheliv.", "Impatiens nomenyae")
          .species("Impatiens", "nomenyae")
          .combAuthors(null, "Eb.Fisch.", "Raheliv.");
      assertName("Armeria carpetana ssp. carpetana H. del Villar", "Armeria carpetana carpetana")
          .infraSpecies("Armeria", "carpetana", SUBSPECIES, "carpetana")
          .combAuthors(null, "H.del Villar");
  }

  @Test
  public void exceptionsWithBinomials() throws Exception {
      // group: Exceptions with Binomials
      assertName("Agra not Erwin, 2002", "Agra not")
          .species("Agra", "not")
          .combAuthors("2002", "Erwin");
      assertName("Navicula bacterium Frenguelli", "Navicula bacterium")
          .species("Navicula", "bacterium")
          .combAuthors(null, "Frenguelli");
      assertName("Bottaria nudum (Nyl.) Vain.", "Bottaria nudum")
          .species("Bottaria", "nudum")
          .combAuthors(null, "Vain.")
          .basAuthors(null, "Nyl.");
      assertName("Turkozelotes attavirus Chatzaki, 2019", "Turkozelotes attavirus")
          .species("Turkozelotes", "attavirus")
          .combAuthors("2019", "Chatzaki");
      assertName("Phalium (Semicassis) vector R. T. Abbott, 1993", "Phalium vector")
          .species("Phalium", "vector")
          .combAuthors("1993", "R.T.Abbott");
      assertName("Spirophora bacterium Lendenfeld, 1887", "Spirophora bacterium")
          .species("Spirophora", "bacterium")
          .combAuthors("1887", "Lendenfeld");
  }

  @Test
  public void binomialsWithMcAndMacAuthors() throws Exception {
      // group: Binomials with Mc and Mac authors
      assertName("Zygocera norfolkensis McKeown 1938", "Zygocera norfolkensis")
          .species("Zygocera", "norfolkensis")
          .combAuthors("1938", "McKeown");
      assertName("Zygocera norfolkensis MacKeown 1938", "Zygocera norfolkensis")
          .species("Zygocera", "norfolkensis")
          .combAuthors("1938", "MacKeown");
      assertName("Zygocera norfolkensis Mac'Keown 1938", "Zygocera norfolkensis")
          .species("Zygocera", "norfolkensis")
          .combAuthors("1938", "Mac'Keown");
      assertName("Zygocera norfolkensis Mc'Keown 1938", "Zygocera norfolkensis")
          .species("Zygocera", "norfolkensis")
          .combAuthors("1938", "Mc'Keown");
  }

  @Test
  public void infraspeciesWithoutRankIczn() throws Exception {
      // group: Infraspecies without rank (ICZN)
      assertName("Myotis fimbriatus taiwanensis Ärnbäck-Christie-Linde, 1908", "Myotis fimbriatus taiwanensis")
          .infraSpecies("Myotis", "fimbriatus", INFRASPECIFIC_NAME, "taiwanensis")
          .combAuthors("1908", "Ärnbäck-Christie-Linde");
      assertName("Peristernia nassatula forskali Tapparone-Canefri 1875", "Peristernia nassatula forskali")
          .infraSpecies("Peristernia", "nassatula", INFRASPECIFIC_NAME, "forskali")
          .combAuthors("1875", "Tapparone-Canefri");
      assertName("Cypraeovula (Luponia) amphithales perdentata", "Cypraeovula amphithales perdentata")
          .infraSpecies("Cypraeovula", "amphithales", INFRASPECIFIC_NAME, "perdentata");
      assertName("Triticum repens vulgäre", "Triticum repens vulgaere")
          .infraSpecies("Triticum", "repens", INFRASPECIFIC_NAME, "vulgaere");
      assertName("Hydnellum scrobiculatum zonatum (Batsch) K. A. Harrison 1961", "Hydnellum scrobiculatum zonatum")
          .infraSpecies("Hydnellum", "scrobiculatum", INFRASPECIFIC_NAME, "zonatum")
          .combAuthors("1961", "K.A.Harrison")
          .basAuthors(null, "Batsch");
      assertName("Hydnellum scrobiculatum zonatum (Banker) D. Hall & D.E. Stuntz 1972", "Hydnellum scrobiculatum zonatum")
          .infraSpecies("Hydnellum", "scrobiculatum", INFRASPECIFIC_NAME, "zonatum")
          .combAuthors("1972", "D.Hall", "D.E.Stuntz")
          .basAuthors(null, "Banker");
      assertName("Hydnellum (Hydnellum) scrobiculatum zonatum (Banker) D. Hall & D.E. Stuntz 1972", "Hydnellum scrobiculatum zonatum")
          .infraSpecies("Hydnellum", "scrobiculatum", INFRASPECIFIC_NAME, "zonatum")
          .combAuthors("1972", "D.Hall", "D.E.Stuntz")
          .basAuthors(null, "Banker");
      assertName("Hydnellum scrobiculatum zonatum", "Hydnellum scrobiculatum zonatum")
          .infraSpecies("Hydnellum", "scrobiculatum", INFRASPECIFIC_NAME, "zonatum");
      assertName("Mus musculus hortulanus", "Mus musculus hortulanus")
          .infraSpecies("Mus", "musculus", INFRASPECIFIC_NAME, "hortulanus");
      assertName("Ortygospiza atricollis mülleri", "Ortygospiza atricollis muelleri")
          .infraSpecies("Ortygospiza", "atricollis", INFRASPECIFIC_NAME, "muelleri");
      assertName("Cortinarius angulatus B gracilescens Fr. 1838", "Cortinarius angulatus gracilescens")
          .infraSpecies("Cortinarius", "angulatus", INFRASPECIFIC_NAME, "gracilescens")
          .combAuthors("1838", "Fr.");
      assertName("Caulerpa fastigiata confervoides P. L. Crouan & H. M. Crouan ex Weber-van Bosse", "Caulerpa fastigiata confervoides")
          .infraSpecies("Caulerpa", "fastigiata", INFRASPECIFIC_NAME, "confervoides")
          .combAuthors(null, "P.L.Crouan", "H.M.Crouan");
      assertName("Rhinanthus glacialis simplex(Sterneck) J.Dostál", "Rhinanthus glacialis simplex")
          .infraSpecies("Rhinanthus", "glacialis", INFRASPECIFIC_NAME, "simplex")
          .combAuthors(null, "J.Dostál")
          .basAuthors(null, "Sterneck");
  }

  @Test
  public void legacyIcznNamesWithRank() throws Exception {
      // group: Legacy ICZN names with rank
      assertName("Acipenser gueldenstaedti colchicus natio danubicus Movchan, 1967", "Acipenser gueldenstaedti colchicus danubicus")
          .infraSpecies("Acipenser", "gueldenstaedti", NATIO, "danubicus")
          .combAuthors("1967", "Movchan");
  }

  @Test
  public void infraspeciesWithRankIcn() throws Exception {
      // group: Infraspecies with rank (ICN)
      assertName("Cantharellus sinuosus var. multiplex(A.H.Sm.) Romagn., 1995", "Cantharellus sinuosus multiplex")
          .infraSpecies("Cantharellus", "sinuosus", VARIETY, "multiplex")
          .combAuthors("1995", "Romagn.")
          .basAuthors(null, "A.H.Sm.");
      assertName("Crematogaster impressa st. brazzai Santschi 1937", "Crematogaster impressa brazzai")
          .infraSpecies("Crematogaster", "impressa", INFRASPECIFIC_NAME, "brazzai")
          .combAuthors("1937", "Santschi");
      assertName("Cibotium st.-johnii Krajina", "Cibotium st-johnii")
          .species("Cibotium", "st-johnii")
          .combAuthors(null, "Krajina");
      assertName("Plantago major prol. lutulenta (Lamotte) Rouy", "Plantago major lutulenta")
          .infraSpecies("Plantago", "major", PROLES, "lutulenta")
          .combAuthors(null, "Rouy")
          .basAuthors(null, "Lamotte");
      assertName("Camponotus conspicuus st. zonatus", "Camponotus conspicuus zonatus")
          .infraSpecies("Camponotus", "conspicuus", INFRASPECIFIC_NAME, "zonatus");
      assertName("Fagus sylvatica subsp. orientalis (Lipsky) Greuter & Burdet", "Fagus sylvatica orientalis")
          .infraSpecies("Fagus", "sylvatica", SUBSPECIES, "orientalis")
          .combAuthors(null, "Greuter", "Burdet")
          .basAuthors(null, "Lipsky");
      assertName("Tillandsia utriculata subspec. utriculata", "Tillandsia utriculata utriculata")
          .infraSpecies("Tillandsia", "utriculata", SUBSPECIES, "utriculata");
      assertName("Prunus mexicana S. Watson var. reticulata (Sarg.) Sarg.", "Prunus mexicana reticulata")
          .infraSpecies("Prunus", "mexicana", VARIETY, "reticulata")
          .combAuthors(null, "Sarg.")
          .basAuthors(null, "Sarg.");
      assertName("Potamogeton iilinoensis var. ventanicola", "Potamogeton iilinoensis ventanicola")
          .infraSpecies("Potamogeton", "iilinoensis", VARIETY, "ventanicola");
      assertName("Potamogeton iilinoensis var. ventanicola (Hicken) Horn af Rantzien", "Potamogeton iilinoensis ventanicola")
          .infraSpecies("Potamogeton", "iilinoensis", VARIETY, "ventanicola")
          .combAuthors(null, "Horn af Rantzien")
          .basAuthors(null, "Hicken");
      assertName("Triticum repens var. vulgäre", "Triticum repens vulgaere")
          .infraSpecies("Triticum", "repens", VARIETY, "vulgaere");
      assertName("Aus bus Linn. var. bus", "Aus bus bus")
          .infraSpecies("Aus", "bus", VARIETY, "bus");
      assertName("Agalinis purpurea (L.) Briton var. borealis (Berg.) Peterson 1987", "Agalinis purpurea borealis")
          .infraSpecies("Agalinis", "purpurea", VARIETY, "borealis")
          .combAuthors("1987", "Peterson")
          .basAuthors(null, "Berg.");
      assertName("Callideriphus flavicollis morph. reductus Fuchs 1961", "Callideriphus flavicollis reductus")
          .infraSpecies("Callideriphus", "flavicollis", MORPH, "reductus")
          .combAuthors("1961", "Fuchs");
      assertName("Caulerpa cupressoides forma nuda", "Caulerpa cupressoides nuda")
          .infraSpecies("Caulerpa", "cupressoides", FORM, "nuda");
      assertName("Chlorocyperus glaber form. fasciculariforme (Lojac.) Soó", "Chlorocyperus glaber fasciculariforme")
          .infraSpecies("Chlorocyperus", "glaber", FORM, "fasciculariforme")
          .combAuthors(null, "Soó")
          .basAuthors(null, "Lojac.");
      assertName("Pteris longifolia fm. stipularis Linnaeus 1753", "Pteris longifolia stipularis")
          .infraSpecies("Pteris", "longifolia", FORM, "stipularis")
          .combAuthors("1753", "Linnaeus");
      assertName("Pteris longifolia fm stipularis Linnaeus 1753", "Pteris longifolia stipularis")
          .infraSpecies("Pteris", "longifolia", FORM, "stipularis")
          .combAuthors("1753", "Linnaeus");
      assertName("Sphaerotheca    fuliginea    f.     dahliae    Movss.     1967", "Sphaerotheca fuliginea dahliae")
          .infraSpecies("Sphaerotheca", "fuliginea", FORM, "dahliae")
          .combAuthors("1967", "Movss.");
      assertName("Allophylus amazonicus var amazonicus", "Allophylus amazonicus amazonicus")
          .infraSpecies("Allophylus", "amazonicus", VARIETY, "amazonicus");
      assertName("Yarrowia lipolytica variety lipolytic", "Yarrowia lipolytica lipolytic")
          .infraSpecies("Yarrowia", "lipolytica", VARIETY, "lipolytic");
      assertName("Prunus armeniaca convar. budae (Pénzes) Soó", "Prunus armeniaca budae")
          .infraSpecies("Prunus", "armeniaca", CONVARIETY, "budae")
          .combAuthors(null, "Soó")
          .basAuthors(null, "Pénzes");
      assertName("Polypodium pectinatum (L.) f. typica Rosenst.", "Polypodium pectinatum typica")
          .infraSpecies("Polypodium", "pectinatum", FORM, "typica")
          .combAuthors(null, "Rosenst.");
      assertName("Polypodium pectinatum L. f. typica Rosenst.", "Polypodium pectinatum typica")
          .infraSpecies("Polypodium", "pectinatum", FORM, "typica")
          .combAuthors(null, "Rosenst.");
      assertName("Rubus fruticosus agamosp. chloocladus (W.C.R. Watson) A. & D. Löve", "Rubus fruticosus chloocladus")
          .infraSpecies("Rubus", "fruticosus", INFRASPECIFIC_NAME, "chloocladus")
          .combAuthors(null, "A.", "D.Löve")
          .basAuthors(null, "W.C.R.Watson");
      assertName("Rubus fruticosus L. agamossp. discolor (Weihe & Nees) A. & D. Löve", "Rubus fruticosus discolor")
          .infraSpecies("Rubus", "fruticosus", SUBSPECIES, "discolor")
          .combAuthors(null, "A.", "D.Löve")
          .basAuthors(null, "Weihe", "Nees");
      assertName("Rubus fruticosus agamovar. graecensis (W.Maurer) A. & D. Löve", "Rubus fruticosus graecensis")
          .infraSpecies("Rubus", "fruticosus", VARIETY, "graecensis")
          .combAuthors(null, "A.", "D.Löve")
          .basAuthors(null, "W.Maurer");
      assertName("Polypodium pectinatum L.f. typica Rosenst.", "Polypodium pectinatum typica")
          .infraSpecies("Polypodium", "pectinatum", INFRASPECIFIC_NAME, "typica")
          .combAuthors(null, "Rosenst.");
      assertName("Polypodium lineare C.Chr. f. caudatoattenuatum Takeda", "Polypodium lineare caudatoattenuatum")
          .infraSpecies("Polypodium", "lineare", FORM, "caudatoattenuatum")
          .combAuthors(null, "Takeda");
      assertName("Rhododendron weyrichii Maxim. f. albiflorum T.Yamaz.", "Rhododendron weyrichii albiflorum")
          .infraSpecies("Rhododendron", "weyrichii", FORM, "albiflorum")
          .combAuthors(null, "T.Yamaz.");
      assertName("Armeria maaritima (Mill.) Willd. fma. originaria Bern.", "Armeria maaritima originaria")
          .infraSpecies("Armeria", "maaritima", FORM, "originaria")
          .combAuthors(null, "Bern.");
      assertName("Rhododendron weyrichii Maxim. albiflorum T.Yamaz. f. fakeepithet", "Rhododendron weyrichii albiflorum fakeepithet")
          .infraSpecies("Rhododendron", "weyrichii", FORM, "fakeepithet");
      assertName("Rhododendron weyrichii Maxim. albiflorum (T.Yamaz. f.) fakeepithet", "Rhododendron weyrichii albiflorum fakeepithet")
          .infraSpecies("Rhododendron", "weyrichii", INFRASPECIFIC_NAME, "fakeepithet");
      assertName("Cotoneaster (Pyracantha) rogersiana var.aurantiaca", "Cotoneaster rogersiana aurantiaca")
          .infraSpecies("Cotoneaster", "rogersiana", VARIETY, "aurantiaca");
      assertName("Poa annua fo varia", "Poa annua varia")
          .infraSpecies("Poa", "annua", FORM, "varia");
      assertName("Physarum globuliferum forma. flavum Leontyev & Dudka", "Physarum globuliferum flavum")
          .infraSpecies("Physarum", "globuliferum", FORM, "flavum")
          .combAuthors(null, "Leontyev", "Dudka");
      assertName("Homalanthus nutans (Mull.Arg.) Benth. & Hook. f. ex Drake", "Homalanthus nutans")
          .species("Homalanthus", "nutans")
          .combAuthors(null, "Benth.", "Hook.fil.")
          .basAuthors(null, "Mull.Arg.");
      assertName("Calicium furfuraceum * furfuraceum (L.) Pers. 1797", "Calicium furfuraceum furfuraceum")
          .infraSpecies("Calicium", "furfuraceum", INFRASPECIFIC_NAME, "furfuraceum")
          .combAuthors("1797", "Pers.")
          .basAuthors(null, "L.");
      assertName("Polyrhachis orsyllus nat musculus Forel 1901", "Polyrhachis orsyllus musculus")
          .infraSpecies("Polyrhachis", "orsyllus", INFRASPECIFIC_NAME, "musculus")
          .combAuthors("1901", "Forel");
      assertName("Acidalia remutaria ab. n. undularia", "Acidalia remutaria undularia")
          .infraSpecies("Acidalia", "remutaria", INFRASPECIFIC_NAME, "undularia");
      assertName("Acmaeops (Pseudodinoptera) bivittata ab. fusciceps Aurivillius, 1912", "Acmaeops bivittata fusciceps")
          .infraSpecies("Acmaeops", "bivittata", ABERRATION, "fusciceps")
          .combAuthors("1912", "Aurivillius");
  }

  @Test
  public void infraspeciesMultipleIcn() throws Exception {
      // group: Infraspecies multiple (ICN)
      assertName("Hydnellum scrobiculatum var. zonatum f. parvum (Banker) D. Hall & D.E. Stuntz 1972", "Hydnellum scrobiculatum zonatum parvum")
          .infraSpecies("Hydnellum", "scrobiculatum", FORM, "parvum")
          .combAuthors("1972", "D.Hall", "D.E.Stuntz")
          .basAuthors(null, "Banker");
      assertName("Senecio fuchsii C.C.Gmel. subsp. fuchsii var. expansus (Boiss. & Heldr.) Hayek", "Senecio fuchsii fuchsii expansus")
          .infraSpecies("Senecio", "fuchsii", VARIETY, "expansus")
          .combAuthors(null, "Hayek")
          .basAuthors(null, "Boiss.", "Heldr.");
      assertName("Senecio fuchsii C.C.Gmel. subsp. fuchsii var. fuchsii", "Senecio fuchsii fuchsii fuchsii")
          .infraSpecies("Senecio", "fuchsii", VARIETY, "fuchsii");
      assertName("Euastrum divergens var. rhodesiense f. coronulum A.M. Scott & Prescott", "Euastrum divergens rhodesiense coronulum")
          .infraSpecies("Euastrum", "divergens", FORM, "coronulum")
          .combAuthors(null, "A.M.Scott", "Prescott");
  }

  @Test
  public void infraspeciesWithGreekLettersIcn() throws Exception {
      // group: Infraspecies with greek letters (ICN)
      assertName("Aristotelia fruticosa var. δ. microphylla Hook.f.", "Aristotelia fruticosa microphylla")
          .infraSpecies("Aristotelia", "fruticosa", VARIETY, "microphylla")
          .combAuthors(null, "Hook.fil.");
      assertName("Hieracium unr. Verbasciformia Arv.-Touv.", "Verbasciformia")
          .monomial("Verbasciformia")
          .combAuthors(null, "Arv.-Touv.");
      assertName("Aristotelia fruticosa var. δ microphylla Hook.f.", "Aristotelia fruticosa microphylla")
          .infraSpecies("Aristotelia", "fruticosa", VARIETY, "microphylla")
          .combAuthors(null, "Hook.fil.");
      assertName("Aristotelia fruticosa var.δ.microphylla Hook.f.", "Aristotelia fruticosa microphylla")
          .infraSpecies("Aristotelia", "fruticosa", VARIETY, "microphylla")
          .combAuthors(null, "Hook.fil.");
      assertName("Aristotelia fruticosa var. δmicrophylla Hook.f.", "Aristotelia fruticosa")
          .species("Aristotelia", "fruticosa");
  }

  @Test
  public void namesWithTheDaggerChar() throws Exception {
      // group: Names with the dagger char '†'
      assertName("Henriksenopterix†", "Henriksenopterix")
          .monomial("Henriksenopterix");
      assertName("Henriksenopterix† paucistriata (Henriksen, 1922)", "Henriksenopterix paucistriata")
          .species("Henriksenopterix", "paucistriata")
          .basAuthors("1922", "Henriksen");
      assertName("Heteralocha acutirostris (Gould, 1837) Huia N E†", "Heteralocha acutirostris")
          .species("Heteralocha", "acutirostris")
          .combAuthors(null, "Huia N E")
          .basAuthors("1837", "Gould");
      assertName("Oncorhynchus nerka (Walbaum, 1792) Sockeye salmon F A †?", "Oncorhynchus nerka salmon")
          .infraSpecies("Oncorhynchus", "nerka", INFRASPECIFIC_NAME, "salmon")
          .combAuthors(null, "F A");
  }

  @Test
  public void hybridsWithNothoRanks() throws Exception {
      // group: Hybrids with notho- ranks
      assertName("Crataegus curvisepala nvar. naviculiformis T. Petauer", "Crataegus curvisepala naviculiformis")
          .infraSpecies("Crataegus", "curvisepala", VARIETY, "naviculiformis")
          .combAuthors(null, "T.Petauer");
      assertName("Aconitum W. Mucher nothosect. Acopellus", "Acopellus")
          .monomial("Acopellus");
      assertName("Aconitum W. Mucher nothoser. Acotoxicum", "Acotoxicum")
          .monomial("Acotoxicum");
      assertName("Abies masjoannis nothof. mesoides", "Abies masjoannis mesoides")
          .infraSpecies("Abies", "masjoannis", FORM, "mesoides");
      assertName("Aconitum berdaui nothosubsp. walasii (Mitka) Mitka", "Aconitum berdaui walasii")
          .infraSpecies("Aconitum", "berdaui", SUBSPECIES, "walasii")
          .combAuthors(null, "Mitka")
          .basAuthors(null, "Mitka");
      assertName("Aconitum tauricum nothossp. hayekianum (Gáyer) Grintescu", "Aconitum tauricum hayekianum")
          .infraSpecies("Aconitum", "tauricum", SUBSPECIES, "hayekianum")
          .combAuthors(null, "Grintescu")
          .basAuthors(null, "Gáyer");
      assertName("Aeonium holospathulatum nothovar. sanchezii (Bañares) Bañares", "Aeonium holospathulatum sanchezii")
          .infraSpecies("Aeonium", "holospathulatum", VARIETY, "sanchezii")
          .combAuthors(null, "Bañares")
          .basAuthors(null, "Bañares");
      assertName("Amaranthus ×ozanonii (Contré) Lambinon nothosubsp. ralletii", "Amaranthus ozanonii ralletii")
          .infraSpecies("Amaranthus", "ozanonii", SUBSPECIES, "ralletii");
      assertName("Aconitum ×teppneri Mucher ex Starm. nothosubsp. goetzii", "Aconitum teppneri goetzii")
          .infraSpecies("Aconitum", "teppneri", SUBSPECIES, "goetzii");
      assertName("Aeonium × proliferum Bañares nothovar. glabrifolium Bañares", "Aeonium proliferum glabrifolium")
          .infraSpecies("Aeonium", "proliferum", VARIETY, "glabrifolium")
          .combAuthors(null, "Bañares");
      assertName("Biscogniauxia nothofagi Whalley, Læssøe & Kile 1990", "Biscogniauxia nothofagi")
          .species("Biscogniauxia", "nothofagi")
          .combAuthors("1990", "Whalley", "Læssøe", "Kile");
  }

  @Test
  public void namedHybrids() throws Exception {
      // group: Named hybrids
      assertName("×Agropogon P. Fourn. 1934", "Agropogon")
          .monomial("Agropogon")
          .combAuthors("1934", "P.Fourn.");
      assertName("xAgropogon P. Fourn.", "Agropogon")
          .monomial("Agropogon")
          .combAuthors(null, "P.Fourn.");
      assertName("XAgropogon P.Fourn.", "Agropogon")
          .monomial("Agropogon")
          .combAuthors(null, "P.Fourn.");
      assertName("× Agropogon", "Agropogon")
          .monomial("Agropogon");
      assertName("x Agropogon", "Agropogon")
          .monomial("Agropogon");
      assertName("X Agropogon", "Agropogon")
          .monomial("Agropogon");
      assertName("X Cupressocyparis leylandii", "Cupressocyparis leylandii")
          .species("Cupressocyparis", "leylandii");
      assertName("×Heucherella tiarelloides", "Heucherella tiarelloides")
          .species("Heucherella", "tiarelloides");
      assertName("xHeucherella tiarelloides", "Heucherella tiarelloides")
          .species("Heucherella", "tiarelloides");
      assertName("x Heucherella tiarelloides", "Heucherella tiarelloides")
          .species("Heucherella", "tiarelloides");
      assertName("XAgroelymus Lapage sect. Agroelinelymus", "Agroelinelymus")
          .monomial("Agroelinelymus");
      assertName("×Agropogon littoralis (Sm.) C. E. Hubb. 1946", "Agropogon littoralis")
          .species("Agropogon", "littoralis")
          .combAuthors("1946", "C.E.Hubb.")
          .basAuthors(null, "Sm.");
      assertName("Asplenium X inexpectatum (E.L. Braun 1940) Morton (1956)", "Asplenium inexpectatum")
          .species("Asplenium", "inexpectatum")
          .combAuthors("1956", "Morton")
          .basAuthors("1940", "E.L.Braun");
      assertName("Androrchis × fallax (De Not.) W.Foelsche & Jakely", "Androrchis fallax")
          .species("Androrchis", "fallax")
          .combAuthors(null, "W.Foelsche", "Jakely")
          .basAuthors(null, "De Not.");
      assertName("Salix ×capreola Andersson (1867)", "Salix capreola")
          .species("Salix", "capreola")
          .combAuthors("1867", "Andersson");
      assertName("Polypodium  x vulgare nothosubsp. mantoniae (Rothm.) Schidlay", "Polypodium vulgare mantoniae")
          .infraSpecies("Polypodium", "vulgare", SUBSPECIES, "mantoniae")
          .combAuthors(null, "Schidlay")
          .basAuthors(null, "Rothm.");
      assertName("Salix x capreola Andersson", "Salix capreola")
          .species("Salix", "capreola")
          .combAuthors(null, "Andersson");
      assertName("x Abacopterella x altifrons T.E.Almeida & A.R.Field", "Abacopterella altifrons")
          .species("Abacopterella", "altifrons")
          .combAuthors(null, "T.E.Almeida", "A.R.Field");
  }

  @Test
  public void hybridFormulae() throws Exception {
      // group: Hybrid formulae
      // skipped: Stanhopea tigrina Bateman ex Lindl. x S. ecornuta Lem.
      // skipped: Arthopyrenia hyalospora X Hydnellum scrobiculatum
      // skipped: Arthopyrenia hyalospora (Banker) D. Hall X Hydnellum scrobiculatum D.E. Stuntz
      // skipped: Arthopyrenia hyalospora x
      // skipped: Arthopyrenia hyalospora × ?
      // skipped: Agrostis L. × Polypogon Desf.
      // skipped: Agrostis stolonifera L. × Polypogon monspeliensis (L.) Desf.
      // skipped: Coeloglossum viride (L.) Hartman x Dactylorhiza majalis (Rchb. f.) P.F. Hunt & Summerhayes ssp. praetermissa (Druce) D.M. Moore & Soó
      // skipped: Salix aurita L. × S. caprea L.
      // skipped: Asplenium rhizophyllum X A. ruta-muraria E.L. Braun 1939
      // skipped: Asplenium rhizophyllum DC. x ruta-muraria E.L. Braun 1939
      // skipped: Tilletia caries (Bjerk.) Tul. × T. foetida (Wallr.) Liro.
      // skipped: Brassica oleracea L. subsp. capitata (L.) DC. convar. fruticosa (Metzg.) Alef. × B. oleracea L. subsp. capitata (L.) var. costata DC.
      // skipped: Ambystoma laterale × A. texanum × A. tigrinum
      assertName("Pseudocercospora broussonetiae (Chupp & Linder) X.J. Liu & Y.L. Guo 1989", "Pseudocercospora broussonetiae")
          .species("Pseudocercospora", "broussonetiae")
          .combAuthors("1989", "X.J.Liu", "Y.L.Guo")
          .basAuthors(null, "Chupp", "Linder");
  }

  @Test
  public void graftChimeras() throws Exception {
      // group: Graft-chimeras
      // skipped: + Crataegomespilus
      // skipped: +Crataegomespilus
      // skipped: Cytisus purpureus + Laburnum anagyroides
      // skipped: Crataegus + Mespilus
  }

  @Test
  public void genusWithHyphenAllowedByIcn() throws Exception {
      // group: Genus with hyphen (allowed by ICN)
      assertName("Saxo-Fridericia R. H. Schomb.", "Saxo-fridericia")
          .monomial("Saxo-fridericia")
          .combAuthors(null, "R.H.Schomb.");
      assertName("Saxo-fridericia R. H. Schomb.", "Saxo-fridericia")
          .monomial("Saxo-fridericia")
          .combAuthors(null, "R.H.Schomb.");
      assertName("Uva-ursi cinerea (Howell) A. Heller", "Uva-ursi cinerea")
          .species("Uva-ursi", "cinerea")
          .combAuthors(null, "A.Heller")
          .basAuthors(null, "Howell");
      assertName("Uva-Ursi cinerea (Howell) A. Heller", "Uva-ursi cinerea")
          .species("Uva-ursi", "cinerea")
          .combAuthors(null, "A.Heller")
          .basAuthors(null, "Howell");
      assertName("Prunus-lauro-cerasus", "Prunus-lauro-cerasus")
          .monomial("Prunus-lauro-cerasus");
      assertName("Prunus-Lauro-Cerasus", "Prunus-lauro-cerasus")
          .monomial("Prunus-lauro-cerasus");
      assertName("Tsugo-piceo-picea × crassifolia (Flous) Campo-Duplan & Gaussen", "Tsugo-piceo-picea crassifolia")
          .species("Tsugo-piceo-picea", "crassifolia")
          .combAuthors(null, "Campo-Duplan", "Gaussen")
          .basAuthors(null, "Flous");
      // skipped: Tsugo-piceo-piceo-picea × crassifolia
      assertName("De-Filippii Gortani & Merla 1934", "De-filippii")
          .monomial("De-filippii")
          .combAuthors("1934", "Gortani", "Merla");
      assertName("Eu-Scalpellum Hoek, 1907", "Eu-scalpellum")
          .monomial("Eu-scalpellum")
          .combAuthors("1907", "Hoek");
      assertName("Eu-hookeria olfersiana (Hornsch.) Hampe", "Eu-hookeria olfersiana")
          .species("Eu-hookeria", "olfersiana")
          .combAuthors(null, "Hampe")
          .basAuthors(null, "Hornsch.");
      assertName("Le-monniera", "Le-monniera")
          .monomial("Le-monniera");
      assertName("Le-Monniera clitandrifolia (A. Chev.) Lecomte", "Le-monniera clitandrifolia")
          .species("Le-monniera", "clitandrifolia")
          .combAuthors(null, "Lecomte")
          .basAuthors(null, "A.Chev.");
      assertName("Ne-ourbania adendrobium (Rchb.f. ) Fawc. & Rendle", "Ne-ourbania adendrobium")
          .species("Ne-ourbania", "adendrobium")
          .combAuthors(null, "Fawc.", "Rendle")
          .basAuthors(null, "Rchb.fil.");
      // skipped: Ph-echinodermata
      assertName("Prunus-lauro-cerasus", "Prunus-lauro-cerasus")
          .monomial("Prunus-lauro-cerasus");
      assertName("Prunus-Lauro-Cerasus", "Prunus-lauro-cerasus")
          .monomial("Prunus-lauro-cerasus");
      assertName("Tsugo-piceo-picea × crassifolia (Flous) Campo-Duplan & Gaussen", "Tsugo-piceo-picea crassifolia")
          .species("Tsugo-piceo-picea", "crassifolia")
          .combAuthors(null, "Campo-Duplan", "Gaussen")
          .basAuthors(null, "Flous");
      // skipped: Tsugo-piceo-piceo-picea × crassifolia
  }

  @Test
  public void misspelledName() throws Exception {
      // group: Misspelled name
      assertName("Ambrysus-Stål, 1862", "Ambrysus-stål")
          .monomial("Ambrysus-stål");
  }

  @Test
  public void aBasionymAuthorInParenthesisBasionymIsAnIcnTerm() throws Exception {
      // group: A 'basionym' author in parenthesis (basionym is an ICN term)
      assertName("Zophosis persis (Chatanay, 1914)", "Zophosis persis")
          .species("Zophosis", "persis")
          .basAuthors("1914", "Chatanay");
      assertName("Zophosis persis (Chatanay 1914)", "Zophosis persis")
          .species("Zophosis", "persis")
          .basAuthors("1914", "Chatanay");
      assertName("Lobodon (Hombrot & Jacquinot, 1842), 2020", "Lobodon")
          .monomial("Lobodon")
          .basAuthors("1842", "Hombrot", "Jacquinot");
      assertName("Zophosis persis (Chatanay), 1914", "Zophosis persis")
          .species("Zophosis", "persis")
          .basAuthors("1914", "Chatanay");
      assertName("Zophosis quadrilineata (Oliv. )", "Zophosis quadrilineata")
          .species("Zophosis", "quadrilineata")
          .basAuthors(null, "Oliv.");
      assertName("Zophosis quadrilineata (Olivier 1795)", "Zophosis quadrilineata")
          .species("Zophosis", "quadrilineata")
          .basAuthors("1795", "Olivier");
  }

  @Test
  public void infragenericEpithetsIczn() throws Exception {
      // group: Infrageneric epithets (ICZN)
      assertName("Hegeter (Hegeter) tenuipunctatus Brullé, 1838", "Hegeter tenuipunctatus")
          .species("Hegeter", "tenuipunctatus")
          .combAuthors("1838", "Brullé");
      assertName("Hegeter (Hegeter) intercedens Lindberg H 1950", "Hegeter intercedens")
          .species("Hegeter", "intercedens")
          .combAuthors("1950", "Lindberg H");
      assertName("Cyprideis (Cyprideis) thessalonike amasyaensis", "Cyprideis thessalonike amasyaensis")
          .infraSpecies("Cyprideis", "thessalonike", INFRASPECIFIC_NAME, "amasyaensis");
      assertName("Acanthoderes (acanthoderes) satanas Aurivillius, 1923", "Acanthoderes satanas")
          .species("Acanthoderes", "satanas")
          .combAuthors("1923", "Aurivillius");
      assertName("Acanthoderes (Abramov) satanas Aurivillius", "Acanthoderes satanas")
          .species("Acanthoderes", "satanas")
          .combAuthors(null, "Aurivillius");
  }

  @Test
  public void namesWithMultipleDashesInSpecificEpithet() throws Exception {
      // group: Names with multiple dashes in specific epithet
      assertName("Athyrium boreo-occidentali-indobharaticola-birianum Fraser-Jenk.", "Athyrium boreo-occidentali-indobharaticola-birianum")
          .species("Athyrium", "boreo-occidentali-indobharaticola-birianum")
          .combAuthors(null, "Fraser-Jenk.");
      assertName("Puccinia band-i-amirii Durrieu, 1975", "Puccinia band-i-amirii")
          .species("Puccinia", "band-i-amirii")
          .combAuthors("1975", "Durrieu");
  }

  @Test
  public void genusWithQuestionMark() throws Exception {
      // group: Genus with question mark
      assertName("Ferganoconcha? oblonga", "Ferganoconcha oblonga")
          .species("Ferganoconcha", "oblonga");
  }

  @Test
  public void epithetsWithAPeriodCharacter() throws Exception {
      // group: Epithets with a period character
      assertName("Macromitrium st.-johnii E. B. Bartram", "Macromitrium st-johnii")
          .species("Macromitrium", "st-johnii")
          .combAuthors(null, "E.B.Bartram");
  }

  @Test
  public void epithetsStartingWithNon() throws Exception {
      // group: Epithets starting with non-
      assertName("Peperomia non-alata Trel.", "Peperomia non-alata")
          .species("Peperomia", "non-alata")
          .combAuthors(null, "Trel.");
      assertName("Hyacinthoides non-scripta (L.) Chouard ex Rothm.", "Hyacinthoides non-scripta")
          .species("Hyacinthoides", "non-scripta")
          .combAuthors(null, "Chouard")
          .basAuthors(null, "L.");
      assertName("Monocelis non-scripta Curini-Galletti, 2014", "Monocelis non-scripta")
          .species("Monocelis", "non-scripta")
          .combAuthors("2014", "Curini-Galletti");
  }

  @Test
  public void epithetsStartingWithAuthorsPrefixesDeDiLaVonEtc() throws Exception {
      // group: Epithets starting with authors' prefixes (de, di, la, von etc.)
      assertName("Aspicilia desertorum desertorum", "Aspicilia desertorum desertorum")
          .infraSpecies("Aspicilia", "desertorum", INFRASPECIFIC_NAME, "desertorum");
      assertName("Theope thestias discus", "Theope thestias discus")
          .infraSpecies("Theope", "thestias", INFRASPECIFIC_NAME, "discus");
      assertName("Ocydromus dalmatinus dalmatinus (Dejean, 1831)", "Ocydromus dalmatinus dalmatinus")
          .infraSpecies("Ocydromus", "dalmatinus", INFRASPECIFIC_NAME, "dalmatinus")
          .basAuthors("1831", "Dejean");
      assertName("Rhipidia gracilirama lassula", "Rhipidia gracilirama lassula")
          .infraSpecies("Rhipidia", "gracilirama", INFRASPECIFIC_NAME, "lassula");
  }

  @Test
  public void authorshipMissingOneParenthesis() throws Exception {
      // group: Authorship missing one parenthesis
      assertName("Ocydromus dalmatinus dalmatinus Dejean, 1831)", "Ocydromus dalmatinus dalmatinus")
          .infraSpecies("Ocydromus", "dalmatinus", INFRASPECIFIC_NAME, "dalmatinus")
          .combAuthors("1831", "Dejean");
      assertName("Ocydromus dalmatinus dalmatinus Dejean, 1831 )", "Ocydromus dalmatinus dalmatinus")
          .infraSpecies("Ocydromus", "dalmatinus", INFRASPECIFIC_NAME, "dalmatinus")
          .combAuthors("1831", "Dejean");
      assertName("Ocydromus dalmatinus dalmatinus ( Dejean, 1831 Mill.", "Ocydromus dalmatinus dalmatinus")
          .infraSpecies("Ocydromus", "dalmatinus", INFRASPECIFIC_NAME, "dalmatinus")
          .combAuthors(null, "Mill.")
          .basAuthors("1831", "Dejean");
      assertName("Ocydromus dalmatinus dalmatinus (Dejean, 1831 Mill.", "Ocydromus dalmatinus dalmatinus")
          .infraSpecies("Ocydromus", "dalmatinus", INFRASPECIFIC_NAME, "dalmatinus")
          .combAuthors(null, "Mill.")
          .basAuthors("1831", "Dejean");
  }

  @Test
  public void unknownAuthorship() throws Exception {
      // group: Unknown authorship
      assertName("Saccharomyces drosophilae anon.", "Saccharomyces drosophilae")
          .species("Saccharomyces", "drosophilae")
          .combAuthors(null, "anon.");
      assertName("Physalospora rubiginosa (Fr.) anon.", "Physalospora rubiginosa")
          .species("Physalospora", "rubiginosa")
          .combAuthors(null, "anon.")
          .basAuthors(null, "Fr.");
      assertName("Tragacantha leporina (?) Kuntze", "Tragacantha leporina")
          .species("Tragacantha", "leporina")
          .combAuthors(null, "Kuntze")
          .basAuthors(null, "anon.");
      assertName("Lachenalia tricolor var. nelsonii (auct.) Baker", "Lachenalia tricolor nelsonii")
          .infraSpecies("Lachenalia", "tricolor", VARIETY, "nelsonii")
          .combAuthors(null, "Baker")
          .basAuthors(null, "anon.");
      assertName("Lachenalia tricolor var. nelsonii (anon.) Baker", "Lachenalia tricolor nelsonii")
          .infraSpecies("Lachenalia", "tricolor", VARIETY, "nelsonii")
          .combAuthors(null, "Baker")
          .basAuthors(null, "anon.");
      assertName("Puya acris anon.", "Puya acris")
          .species("Puya", "acris")
          .combAuthors(null, "anon.");
  }

  @Test
  public void treatingApudWith() throws Exception {
      // group: Treating apud (with)
      assertName("Pseudocercospora dendrobii Goh apud W.H. Hsieh 1990", "Pseudocercospora dendrobii")
          .species("Pseudocercospora", "dendrobii")
          .combAuthors("1990", "Goh", "W.H.Hsieh");
  }

  @Test
  public void namesWithExAuthorsWeFollowIcznConvention() throws Exception {
      // group: Names with ex authors (we follow ICZN convention)
      assertName("Amathia tricornis Busk ms in Chimonides, 1987", "Amathia tricornis")
          .species("Amathia", "tricornis")
          .combAuthors(null, "Busk");
      assertName("Pisania billehousti Souverbie, in Souverbie and Montrouzier, 1864", "Pisania billehousti")
          .species("Pisania", "billehousti")
          .combAuthors(null, "Souverbie");
      assertName("Arthopyrenia hyalospora (Nyl. ex Banker) R.C. Harris", "Arthopyrenia hyalospora")
          .species("Arthopyrenia", "hyalospora")
          .combAuthors(null, "R.C.Harris")
          .basAuthors(null, "Nyl.");
      assertName("Arthopyrenia hyalospora (Nyl. ex. Banker) R.C. Harris", "Arthopyrenia hyalospora")
          .species("Arthopyrenia", "hyalospora")
          .combAuthors(null, "R.C.Harris")
          .basAuthors(null, "Nyl.");
      assertName("Arthopyrenia hyalospora Nyl. ex Banker", "Arthopyrenia hyalospora")
          .species("Arthopyrenia", "hyalospora")
          .combAuthors(null, "Nyl.");
      assertName("Arthopyrenia hyalospora Nyl. ex. Banker", "Arthopyrenia hyalospora")
          .species("Arthopyrenia", "hyalospora")
          .combAuthors(null, "Nyl.");
      assertName("Glomopsis lonicerae Peck ex C.J. Gould 1945", "Glomopsis lonicerae")
          .species("Glomopsis", "lonicerae")
          .combAuthors(null, "Peck");
      assertName("Glomopsis lonicerae Peck ex. C.J. Gould 1945", "Glomopsis lonicerae")
          .species("Glomopsis", "lonicerae")
          .combAuthors(null, "Peck");
      assertName("Acanthobasidium delicatum (Wakef.) Oberw. ex Jülich 1979", "Acanthobasidium delicatum")
          .species("Acanthobasidium", "delicatum")
          .combAuthors(null, "Oberw.")
          .basAuthors(null, "Wakef.");
      assertName("Acanthobasidium delicatum (Wakef.) Oberw. ex. Jülich 1979", "Acanthobasidium delicatum")
          .species("Acanthobasidium", "delicatum")
          .combAuthors(null, "Oberw.")
          .basAuthors(null, "Wakef.");
      assertName("Mycosphaerella eryngii (Fr. ex Duby) Johanson ex Oudem. 1897", "Mycosphaerella eryngii")
          .species("Mycosphaerella", "eryngii")
          .combAuthors(null, "Johanson")
          .basAuthors(null, "Fr.");
      assertName("Mycosphaerella eryngii (Fr. ex. Duby) Johanson ex. Oudem. 1897", "Mycosphaerella eryngii")
          .species("Mycosphaerella", "eryngii")
          .combAuthors(null, "Johanson")
          .basAuthors(null, "Fr.");
      assertName("Mycosphaerella eryngii (Fr. Duby) ex Oudem. 1897", "Mycosphaerella eryngii")
          .species("Mycosphaerella", "eryngii")
          .basAuthors(null, "Fr.Duby");
  }

  @Test
  public void emptySpaces() throws Exception {
      // group: Empty spaces
      assertName("Asplenium       X inexpectatum(E. L. Braun ex Friesner      )Morton", "Asplenium inexpectatum")
          .species("Asplenium", "inexpectatum")
          .combAuthors(null, "Morton")
          .basAuthors(null, "E.L.Braun");
  }

  @Test
  public void namesWithADash() throws Exception {
      // group: Names with a dash
      assertName("Drosophila obscura-x Burla, 1951", "Drosophila obscura-x")
          .species("Drosophila", "obscura-x")
          .combAuthors("1951", "Burla");
      assertName("Sanogasta x-signata (Keyserling,1891)", "Sanogasta x-signata")
          .species("Sanogasta", "x-signata")
          .basAuthors("1891", "Keyserling");
      assertName("Aedes w-albus (Theobald, 1905)", "Aedes w-albus")
          .species("Aedes", "w-albus")
          .basAuthors("1905", "Theobald");
      assertName("Abryna regis-petri Paiva, 1860", "Abryna regis-petri")
          .species("Abryna", "regis-petri")
          .combAuthors("1860", "Paiva");
      assertName("Solms-laubachia orbiculata Y.C. Lan & T.Y. Cheo", "Solms-laubachia orbiculata")
          .species("Solms-laubachia", "orbiculata")
          .combAuthors(null, "Y.C.Lan", "T.Y.Cheo");
  }

  @Test
  public void authorshipWithDegli() throws Exception {
      // group: Authorship with 'degli'
      assertName("Cestodiscus gemmifer F. S. Castracane degli Antelminelli", "Cestodiscus gemmifer")
          .species("Cestodiscus", "gemmifer")
          .combAuthors(null, "F.S.Castracane degli Antelminelli");
  }

  @Test
  public void authorshipWithFiliusSonOf() throws Exception {
      // group: Authorship with filius (son of)
      assertName("Oxytropis minjanensis Rech. f.", "Oxytropis minjanensis")
          .species("Oxytropis", "minjanensis")
          .combAuthors(null, "Rech.fil.");
      assertName("Platypus bicaudatulus Schedl f. 1935", "Platypus bicaudatulus")
          .species("Platypus", "bicaudatulus")
          .combAuthors("1935", "Schedl fil.");
      assertName("Platypus bicaudatulus Schedl filius 1935", "Platypus bicaudatulus")
          .species("Platypus", "bicaudatulus")
          .combAuthors("1935", "Schedl fil.");
      assertName("Fimbristylis ovata (Burm. f.) J. Kern", "Fimbristylis ovata")
          .species("Fimbristylis", "ovata")
          .combAuthors(null, "J.Kern")
          .basAuthors(null, "Burm.fil.");
      assertName("Carex chordorrhiza Ehrh. ex L. f.", "Carex chordorrhiza")
          .species("Carex", "chordorrhiza")
          .combAuthors(null, "Ehrh.");
      assertName("Amelanchier arborea var. arborea (Michx. f.) Fernald", "Amelanchier arborea arborea")
          .infraSpecies("Amelanchier", "arborea", VARIETY, "arborea")
          .combAuthors(null, "Fernald")
          .basAuthors(null, "Michx.fil.");
      assertName("Cerastium arvense var. fuegianum Hook. f.", "Cerastium arvense fuegianum")
          .infraSpecies("Cerastium", "arvense", VARIETY, "fuegianum")
          .combAuthors(null, "Hook.fil.");
      assertName("Cerastium arvense var. fuegianum Hook.f.", "Cerastium arvense fuegianum")
          .infraSpecies("Cerastium", "arvense", VARIETY, "fuegianum")
          .combAuthors(null, "Hook.fil.");
      assertName("Cerastium arvense ssp. velutinum var. velutinum (Raf.) Britton f.", "Cerastium arvense velutinum velutinum")
          .infraSpecies("Cerastium", "arvense", VARIETY, "velutinum")
          .combAuthors(null, "Britton fil.")
          .basAuthors(null, "Raf.");
      assertName("Jacquemontia spiciflora (Choisy) Hall. fil.", "Jacquemontia spiciflora")
          .species("Jacquemontia", "spiciflora")
          .combAuthors(null, "Hall.fil.")
          .basAuthors(null, "Choisy");
      // skipped: Littorina (Littorina) littorea fa major (Linnaeus, 1758)
      assertName("Amelanchier arborea f. hirsuta (Michx. f.) Fernald", "Amelanchier arborea hirsuta")
          .infraSpecies("Amelanchier", "arborea", FORM, "hirsuta")
          .combAuthors(null, "Fernald")
          .basAuthors(null, "Michx.fil.");
      assertName("Betula pendula fo. dalecarlica (L. f.) C.K. Schneid.", "Betula pendula dalecarlica")
          .infraSpecies("Betula", "pendula", FORM, "dalecarlica")
          .combAuthors(null, "C.K.Schneid.")
          .basAuthors(null, "L.fil.");
      assertName("Racomitrium canescens f. ericoides (F. Weber ex Brid.) Mönk.", "Racomitrium canescens ericoides")
          .infraSpecies("Racomitrium", "canescens", FORM, "ericoides")
          .combAuthors(null, "Mönk.")
          .basAuthors(null, "F.Weber");
      assertName("Racomitrium canescens forma ericoides (F. Weber ex Brid.) Mönk.", "Racomitrium canescens ericoides")
          .infraSpecies("Racomitrium", "canescens", FORM, "ericoides")
          .combAuthors(null, "Mönk.")
          .basAuthors(null, "F.Weber");
      assertName("Polypodium pectinatum L. f., Rosenst.", "Polypodium pectinatum")
          .species("Polypodium", "pectinatum")
          .combAuthors(null, "L.fil.", "Rosenst.");
      assertName("Polypodium pectinatum L. f.", "Polypodium pectinatum")
          .species("Polypodium", "pectinatum")
          .combAuthors(null, "L.fil.");
      assertName("Polypodium pectinatum (L. f.) typica Rosent", "Polypodium pectinatum typica")
          .infraSpecies("Polypodium", "pectinatum", INFRASPECIFIC_NAME, "typica")
          .combAuthors(null, "Rosent");
  }

  @Test
  public void namesWithEmendRectifiedByAuthorship() throws Exception {
      // group: Names with emend (rectified by) authorship
      assertName("Chlorobium phaeobacteroides Pfennig, 1968 emend. Imhoff, 2003", "Chlorobium phaeobacteroides")
          .species("Chlorobium", "phaeobacteroides")
          .combAuthors("1968", "Pfennig");
      assertName("Chlorobium phaeobacteroides Pfennig, 1968 emend Imhoff, 2003", "Chlorobium phaeobacteroides")
          .species("Chlorobium", "phaeobacteroides")
          .combAuthors("1968", "Pfennig");
  }

  @Test
  public void namesWithAnUnparsedTail() throws Exception {
      // group: Names with an unparsed "tail"
      assertName("Morea (Morea) Burt 2342343242 23424322342 23424234", "Morea")
          .monomial("Morea")
          .rank(SUBGENUS)
          .combAuthors(null, "Burt");
      assertName("Nautilus asterizans von", "Nautilus asterizans")
          .species("Nautilus", "asterizans");
      assertName("Dryopteris X separabilis Small (pro sp.)", "Dryopteris separabilis")
          .species("Dryopteris", "separabilis")
          .combAuthors(null, "Small");
      assertName("Eulima excellens Verkrüzen fide Paetel, 1887", "Eulima excellens")
          .species("Eulima", "excellens")
          .combAuthors(null, "Verkrüzen");
      assertName("Procamallanus (Spirocamallanus) soodi Lakshmi & Kumari, 2001 nec (Gupta & Masood, 1988)", "Procamallanus soodi")
          .species("Procamallanus", "soodi")
          .combAuthors("2001", "Lakshmi", "Kumari");
      assertName("Membranipora minuscula Canu, 1911 non Hincks, 1882", "Membranipora minuscula")
          .species("Membranipora", "minuscula")
          .combAuthors("1911", "Canu");
      assertName("Proboscina subechinata Canu & Bassler, 1920 non d'Orbigny, 1853", "Proboscina subechinata")
          .species("Proboscina", "subechinata")
          .combAuthors("1920", "Canu", "Bassler");
      assertName("Porina reussi Meneghini in De Amicis, 1885 vide Neviani (1900)", "Porina reussi")
          .species("Porina", "reussi")
          .combAuthors(null, "Meneghini");
  }

  @Test
  public void abbreviatedWordsAfterAName() throws Exception {
      // group: Abbreviated words after a name
      assertName("Graphis scripta L. a.b pulverulenta", "Graphis scripta")
          .species("Graphis", "scripta")
          .combAuthors(null, "L.");
      assertName("Cetraria iberica a.crespo & barreno", "Cetraria iberica")
          .species("Cetraria", "iberica");
      assertName("Lecanora achariana a.l.sm.", "Lecanora achariana")
          .species("Lecanora", "achariana");
      assertName("Arthrosporum populorum a.massal.", "Arthrosporum populorum")
          .species("Arthrosporum", "populorum");
      assertName("Eletica laeviceps ab.lateapicalis Pic", "Eletica laeviceps")
          .species("Eletica", "laeviceps");
  }

  @Test
  public void epithetsStartingWithNumericValueNotAllowedAnymore() throws Exception {
      // group: Epithets starting with numeric value (not allowed anymore)
      assertName("Acanthoderes 4-gibbus RILEY Charles Valentine, 1880", "Acanthoderes quadrigibbus")
          .species("Acanthoderes", "quadrigibbus")
          .combAuthors("1880", "Riley Charles Valentine");
      assertName("Acrosoma 12-spinosa Keyserling, 1892", "Acrosoma duodecimspinosa")
          .species("Acrosoma", "duodecimspinosa")
          .combAuthors("1892", "Keyserling");
      assertName("Canuleius 24-spinosus Redtenbacher, 1906", "Canuleius vigintiquatuorspinosus")
          .species("Canuleius", "vigintiquatuorspinosus")
          .combAuthors("1906", "Redtenbacher");
      assertName("Canuleius 777-spinosus Redtenbacher, 1906", "Canuleius")
          .monomial("Canuleius");
      assertName("Rhynchophorus 13punctatus Herbst, J.F.W., 1795", "Rhynchophorus tredecimpunctatus")
          .species("Rhynchophorus", "tredecimpunctatus")
          .combAuthors("1795", "Herbst", "J.F.W.");
      assertName("Rhynchophorus 13.punctatus Herbst, J.F.W., 1795", "Rhynchophorus tredecimpunctatus")
          .species("Rhynchophorus", "tredecimpunctatus")
          .combAuthors("1795", "Herbst", "J.F.W.");
  }

  @Test
  public void nonAsciiUtf8CharactersInAName() throws Exception {
      // group: Non-ASCII UTF-8 characters in a name
      assertName("Seleuca chûjôi Voss, 1957", "Seleuca chujoi")
          .species("Seleuca", "chujoi")
          .combAuthors("1957", "Voss");
      assertName("Pleurotus ëous (Berk.) Sacc. 1887", "Pleurotus eous")
          .species("Pleurotus", "eous")
          .combAuthors("1887", "Sacc.")
          .basAuthors(null, "Berk.");
      assertName("Sténométope laevissimus Bibron 1855", "Stenometope laevissimus")
          .species("Stenometope", "laevissimus")
          .combAuthors("1855", "Bibron");
      assertName("Choriozopella trägårdhi Lawrence, 1947", "Choriozopella traegaordhi")
          .species("Choriozopella", "traegaordhi")
          .combAuthors("1947", "Lawrence");
      assertName("Isoëtes asplundii H. P. Fuchs", "Isoetes asplundii")
          .species("Isoetes", "asplundii")
          .combAuthors(null, "H.P.Fuchs");
      assertName("Cerambyx thomæ GMELIN J. F., 1790", "Cerambyx thomae")
          .species("Cerambyx", "thomae")
          .combAuthors("1790", "Gmelin J.F.");
      assertName("Campethera cailliautii fülleborni", "Campethera cailliautii fuelleborni")
          .infraSpecies("Campethera", "cailliautii", INFRASPECIFIC_NAME, "fuelleborni");
      assertName("Östrupia Heiden ex Hustedt, 1935", "Oestrupia")
          .monomial("Oestrupia")
          .combAuthors(null, "Heiden");
  }

  @Test
  public void epithetsWithAnApostrophe() throws Exception {
      // group: Epithets with an apostrophe
      assertName("Solanum tuberosum f. wila-k'oyu Ochoa", "Solanum tuberosum wila-koyu")
          .infraSpecies("Solanum", "tuberosum", FORM, "wila-koyu")
          .combAuthors(null, "Ochoa");
      assertName("Junellia o'donelli Moldenke, 1946", "Junellia odonelli")
          .species("Junellia", "odonelli")
          .combAuthors("1946", "Moldenke");
      assertName("Trophon d'orbignyi Carcelles, 1946", "Trophon dorbignyi")
          .species("Trophon", "dorbignyi")
          .combAuthors("1946", "Carcelles");
      assertName("Phrynosoma m’callii", "Phrynosoma mcallii")
          .species("Phrynosoma", "mcallii");
      assertName("Arca m'coyi Tenison-Woods, 1878", "Arca mcoyi")
          .species("Arca", "mcoyi")
          .combAuthors("1878", "Tenison-Woods");
      assertName("Nucula m'andrewii Hanley, 1860", "Nucula mandrewii")
          .species("Nucula", "mandrewii")
          .combAuthors("1860", "Hanley");
      assertName("Eristalis l'herminierii Macquart", "Eristalis lherminierii")
          .species("Eristalis", "lherminierii")
          .combAuthors(null, "Macquart");
      assertName("Odynerus o'neili Cameron", "Odynerus oneili")
          .species("Odynerus", "oneili")
          .combAuthors(null, "Cameron");
      assertName("Serjania meridionalis Cambess. var. o'donelli F.A. Barkley", "Serjania meridionalis odonelli")
          .infraSpecies("Serjania", "meridionalis", VARIETY, "odonelli")
          .combAuthors(null, "F.A.Barkley");
  }

  @Test
  public void authorsWithAnApostrophe() throws Exception {
      // group: Authors with an apostrophe
      assertName("Galega officinalis (L.) L´Hèr. subsp. mackayana (O'Flannagan) Mc Inley var. petiolata (È. Neé) Brüch.", "Galega officinalis mackayana petiolata")
          .infraSpecies("Galega", "officinalis", VARIETY, "petiolata")
          .combAuthors(null, "Brüch.")
          .basAuthors(null, "È.Neé");
      assertName("Galega officinalis (L.) L`Hèr. subsp. mackayana (O'Flannagan) Mc Inley var. petiolata (È. Neé) Brüch.", "Galega officinalis mackayana petiolata")
          .infraSpecies("Galega", "officinalis", VARIETY, "petiolata")
          .combAuthors(null, "Brüch.")
          .basAuthors(null, "È.Neé");
      assertName("Galega officinalis (L.) L'Hèr. subsp. mackayana (O'Flannagan) Mc Inley var. petiolata (È. Neé) Brüch.", "Galega officinalis mackayana petiolata")
          .infraSpecies("Galega", "officinalis", VARIETY, "petiolata")
          .combAuthors(null, "Brüch.")
          .basAuthors(null, "È.Neé");
  }

  @Test
  public void digraphUnicodeCharacters() throws Exception {
      // group: Digraph unicode characters
      assertName("Crisia romanica Zágoršek Silye & Szabó 2008", "Crisia romanica")
          .species("Crisia", "romanica")
          .combAuthors("2008", "Zágoršek Silye", "Szabó");
      assertName("Æschopalæa grisella Pascoe, 1864", "Aeschopalaea grisella")
          .species("Aeschopalaea", "grisella")
          .combAuthors("1864", "Pascoe");
      assertName("Læptura laetifica Dow, 1913", "Laeptura laetifica")
          .species("Laeptura", "laetifica")
          .combAuthors("1913", "Dow");
      assertName("Leptura lætifica Dow, 1913", "Leptura laetifica")
          .species("Leptura", "laetifica")
          .combAuthors("1913", "Dow");
      assertName("Leptura leætifica Dow, 1913", "Leptura leaetifica")
          .species("Leptura", "leaetifica")
          .combAuthors("1913", "Dow");
      assertName("Leæptura laetifica Dow, 1913", "Leaeptura laetifica")
          .species("Leaeptura", "laetifica")
          .combAuthors("1913", "Dow");
      assertName("Leœptura laetifica Dow, 1913", "Leoeptura laetifica")
          .species("Leoeptura", "laetifica")
          .combAuthors("1913", "Dow");
      assertName("Ærenea cognata Lacordaire, 1872", "Aerenea cognata")
          .species("Aerenea", "cognata")
          .combAuthors("1872", "Lacordaire");
      assertName("Œdicnemus capensis", "Oedicnemus capensis")
          .species("Oedicnemus", "capensis");
      assertName("Œnanthe œnanthe", "Oenanthe oenanthe")
          .species("Oenanthe", "oenanthe");
      assertName("Hördeum vulgare cœrulescens", "Hoerdeum vulgare coerulescens")
          .infraSpecies("Hoerdeum", "vulgare", INFRASPECIFIC_NAME, "coerulescens");
      assertName("Hordeum vulgare cœrulescens Metzger", "Hordeum vulgare coerulescens")
          .infraSpecies("Hordeum", "vulgare", INFRASPECIFIC_NAME, "coerulescens")
          .combAuthors(null, "Metzger");
      assertName("Hordeum vulgare f. cœrulescens", "Hordeum vulgare coerulescens")
          .infraSpecies("Hordeum", "vulgare", FORM, "coerulescens");
  }

  @Test
  public void oldStyleS() throws Exception {
      // group: Old style s (ſ)
      assertName("Musca domeſtica Linnaeus 1758", "Musca domestica")
          .species("Musca", "domestica")
          .combAuthors("1758", "Linnaeus");
      assertName("Amphisbæna fuliginoſa Linnaeus 1758", "Amphisbaena fuliginosa")
          .species("Amphisbaena", "fuliginosa")
          .combAuthors("1758", "Linnaeus");
      assertName("Dreyfusia nüßlini", "Dreyfusia nuesslini")
          .species("Dreyfusia", "nuesslini");
  }

  @Test
  public void miscellaneousDiacritics() throws Exception {
      // group: Miscellaneous diacritics
      assertName("Pärdosa", "Paerdosa")
          .monomial("Paerdosa");
      assertName("Pårdosa", "Paordosa")
          .monomial("Paordosa");
      assertName("Pardøsa", "Pardoesa")
          .monomial("Pardoesa");
      assertName("Pardösa", "Pardoesa")
          .monomial("Pardoesa");
      assertName("Rühlella", "Ruehlella")
          .monomial("Ruehlella");
  }

  @Test
  public void openNomenclatureApproximateNames() throws Exception {
      // group: Open Nomenclature ('approximate' names)
      // skipped: Solygia ? distanti
      assertName("Buteo borealis ? ventralis", "Buteo borealis ventralis")
          .infraSpecies("Buteo", "borealis", INFRASPECIFIC_NAME, "ventralis");
      // skipped: Euxoa nr. idahoensis sp. 1clay
      // skipped: Acarinina aff. pentacamerata
      // skipped: Acarinina aff pentacamerata
      // skipped: Sphingomonas sp. 37
      // skipped: Thryothorus leucotis spp. bogotensis
      // skipped: Endoxyla sp. GM-, 2003
      // skipped: X Aegilotrichum sp.
      // skipped: Liopropoma sp.2 Not applicable
      // skipped: Lacanobia sp. nr. subjuncta Bold:Aab, 0925
      // skipped: Lacanobia nr. subjuncta Bold:Aab, 0925
      // skipped: Abturia cf. alabamensis (Morton )
      // skipped: Abturia cf alabamensis (Morton )
      // skipped: Calidris cf. cooperi
      assertName("Aesculus cf. × hybrida", "Aesculus hybrida")
          .species("Aesculus", "hybrida");
      assertName("Daphnia (Daphnia) x krausi Flossner 1993", "Daphnia krausi")
          .species("Daphnia", "krausi")
          .combAuthors("1993", "Flossner");
      // skipped: Barbus cf macrotaenia × toppini
      // skipped: Gemmula cf. cosmoi NP-2008
  }

  @Test
  public void surrogateNameStrings() throws Exception {
      // group: Surrogate Name-Strings
      // skipped: Coleoptera sp. BOLD:AAV0432
      assertName("Coleoptera Bold:AAV0432", "Coleoptera")
          .monomial("Coleoptera");
  }

  @Test
  public void virusLikeNormalNames() throws Exception {
      // group: Virus-like "normal" names
      assertName("Ceylonesmus vector Chamberlin, 1941", "Ceylonesmus vector")
          .species("Ceylonesmus", "vector")
          .combAuthors("1941", "Chamberlin");
  }

  @Test
  public void virusesPlasmidsPrionsEtc() throws Exception {
      // group: Viruses, plasmids, prions etc.
      // skipped: Arv1virus
      // skipped: Turtle herpesviruses
      // skipped: Cre expression vector
      // skipped: Cyanophage
      // skipped: Drosophila sturtevanti rhabdovirus
      // skipped: Hydra expression vector
      // skipped: Gateway destination plasmid
      // skipped: Abutilon mosaic virus [X15983] [X15984] Abutilon mosaic virus ICTV
      // skipped: Omphalotus sp. Ictv Garcia, 18224
      // skipped: Acute bee paralysis virus [AF150629] Acute bee paralysis virus
      // skipped: Adeno-associated virus - 3
      // skipped: ?M1-like Viruses Methanobrevibacter phage PG
      // skipped: Aeromonas phage 65
      // skipped: Bacillus phage SPß [AF020713] Bacillus phage SPb ICTV
      // skipped: Apple scar skin viroid
      // skipped: Australian grapevine viroid [X17101] Australian grapevine viroid ICTV
      // skipped: Agents of Spongiform Encephalopathies CWD prion Chronic wasting disease
      // skipped: Phi h-like viruses
      // skipped: Viroids
      // skipped: Fungal prions
      // skipped: Human rhinovirus A11
      // skipped: Kobuvirus korean black goat/South Korea/2010
      // skipped: Australian bat lyssavirus human/AUS/1998
      // skipped: Gossypium mustilinum symptomless alphasatellite
      // skipped: Okra leaf curl Mali alphasatellites-Cameroon
      // skipped: Bemisia betasatellite LW-2014
      // skipped: Tomato leaf curl Bangladesh betasatellites [India/Patna/Chilli/2008]
      // skipped: Intracisternal A-particles
      // skipped: Saccharomyces cerevisiae killer particle M1
      // skipped: Uranotaenia sapphirina NPV
      // skipped: Uranotaenia sapphirina Npv
      // skipped: Spodoptera exigua nuclear polyhedrosis virus SeMNPV
      // skipped: Spodoptera frugiperda MNPV
      // skipped: Rachiplusia ou MNPV (strain R1)
      // skipped: Orgyia pseudotsugata nuclear polyhedrosis virus OpMNPV
      // skipped: Mamestra configurata NPV-A
      // skipped: Helicoverpa armigera SNPV NNg1
      // skipped: Zamilon virophage
      // skipped: Sputnik virophage 3
      // skipped: Bacteriophage PH75
      // skipped: Escherichia coli bacteriophage
      // skipped: Betasatellites
      // skipped: Satellite Nucleic Acids (Subviral DNA-ssDNA)
  }

  @Test
  public void nameStringsWithRna() throws Exception {
      // group: Name-strings with RNA
      // skipped: ssRNA
      // skipped: Alpha proteobacterium RNA12
      // skipped: Ustilaginoidea virens RNA virus
      // skipped: Candida albicans RNA_CTR0-3
      assertName("Carabus satyrus satyrus KURNAKOV, 1962", "Carabus satyrus satyrus")
          .infraSpecies("Carabus", "satyrus", INFRASPECIFIC_NAME, "satyrus")
          .combAuthors("1962", "Kurnakov");
  }

  @Test
  public void epithetPrioniIsNotAPrion() throws Exception {
      // group: Epithet prioni is not a prion
      assertName("Fakus prioni", "Fakus prioni")
          .species("Fakus", "prioni");
  }

  @Test
  public void namesWithSatelliteAsASubstring() throws Exception {
      // group: Names with "satellite" as a substring
      assertName("Crassatellites fulvida", "Crassatellites fulvida")
          .species("Crassatellites", "fulvida");
  }

  @Test
  public void bacterialGenus() throws Exception {
      // group: Bacterial genus
      assertName("Salmonella werahensis (Castellani) Hauduroy and Ehringer in Hauduroy 1937", "Salmonella werahensis")
          .species("Salmonella", "werahensis")
          .combAuthors(null, "Hauduroy", "Ehringer")
          .basAuthors(null, "Castellani");
  }

  @Test
  public void bacteriaGenusHomonym() throws Exception {
      // group: Bacteria genus homonym
      assertName("Actinomyces cardiffensis", "Actinomyces cardiffensis")
          .species("Actinomyces", "cardiffensis");
  }

  @Test
  public void bacteriaWithPathovarRank() throws Exception {
      // group: Bacteria with pathovar rank
      assertName("Xanthomonas axonopodis pv. phaseoli", "Xanthomonas axonopodis phaseoli")
          .infraSpecies("Xanthomonas", "axonopodis", PATHOVAR, "phaseoli");
      assertName("Xanthomonas axonopodis pathovar. phaseoli", "Xanthomonas axonopodis phaseoli")
          .infraSpecies("Xanthomonas", "axonopodis", PATHOVAR, "phaseoli");
      assertName("Xanthomonas axonopodis pathovar.", "Xanthomonas axonopodis")
          .species("Xanthomonas", "axonopodis");
      assertName("Xanthomonas axonopodis pv.", "Xanthomonas axonopodis")
          .species("Xanthomonas", "axonopodis");
  }

  @Test
  public void strayExIsNotParsedAsSpecies() throws Exception {
      // group: "Stray" ex is not parsed as species
      assertName("Pelargonium cucullatum ssp. cucullatum (L.) L'Her. ex [Soland.]", "Pelargonium cucullatum cucullatum")
          .infraSpecies("Pelargonium", "cucullatum", SUBSPECIES, "cucullatum")
          .combAuthors(null, "L'Her.")
          .basAuthors(null, "L.");
      assertName("Acastella ex gr. rouaulti", "Acastella")
          .monomial("Acastella");
  }

  @Test
  public void authorshipInUpperCase() throws Exception {
      // group: Authorship in upper case
      assertName("Lecanora strobilinoides GIRALT & GÓMEZ-BOLEA", "Lecanora strobilinoides")
          .species("Lecanora", "strobilinoides")
          .combAuthors(null, "Giralt", "Gómez-Bolea");
  }

  @Test
  public void numbersAndLettersSeparatedWithAreNotParsedAsAuthors() throws Exception {
      // group: Numbers and letters separated with '-' are not parsed as authors
      // skipped: Astatotilapia cf. bloyeti OS-2017
  }

  @Test
  public void doubleParenthesis() throws Exception {
      // group: Double parenthesis
      assertName("Eichornia crassipes ( (Martius) ) Solms-Laub.", "Eichornia crassipes")
          .species("Eichornia", "crassipes")
          .combAuthors(null, "Solms-Laub.")
          .basAuthors(null, "Martius");
  }

  @Test
  public void numbersAtTheStartMiddleOfNames() throws Exception {
      // group: Numbers at the start/middle of names
      assertName("Nesomyrmex madecassus_01m", "Nesomyrmex")
          .monomial("Nesomyrmex");
      // skipped: Hypochrys0des
      // skipped: Hypochrys0des Leraut 1981
      assertName("Phyllodoce mucosa 0ersted, 1843", "Phyllodoce mucosa")
          .species("Phyllodoce", "mucosa");
      assertName("Attelabus 0l.", "Attelabus")
          .monomial("Attelabus");
      assertName("Acrobothrium 0lsson 1872", "Acrobothrium")
          .monomial("Acrobothrium");
      assertName("Staphylinus haemrrhoidalis 0l. nec Gmel", "Staphylinus haemrrhoidalis")
          .species("Staphylinus", "haemrrhoidalis");
      // skipped: Ea92virus
  }

  @Test
  public void yearWithoutAuthorship() throws Exception {
      // group: Year without authorship
      assertName("Acarospora cratericola 1929", "Acarospora cratericola")
          .species("Acarospora", "cratericola");
      assertName("Goggia gemmula 1996", "Goggia gemmula")
          .species("Goggia", "gemmula");
  }

  @Test
  public void yearRange() throws Exception {
      // group: Year range
      assertName("Eurodryas orientalis Herrich-Schäffer 1845-1847", "Eurodryas orientalis")
          .species("Eurodryas", "orientalis")
          .combAuthors("1845", "Herrich-Schäffer");
      assertName("Tridentella tangeroae Bruce, 1987-92", "Tridentella tangeroae")
          .species("Tridentella", "tangeroae")
          .combAuthors("1987", "Bruce");
      assertName("Macroplectra unicolor Moore, 1858/59", "Macroplectra unicolor")
          .species("Macroplectra", "unicolor")
          .combAuthors("1858", "Moore");
      assertName("Seryda basirei Druce, 1891/901", "Seryda basirei")
          .species("Seryda", "basirei")
          .combAuthors("1891", "Druce");
  }

  @Test
  public void yearWithPageNumber() throws Exception {
      // group: Year with page number
      assertName("Recilia truncatus Dash & Viraktamath, 1998: 29", "Recilia truncatus")
          .species("Recilia", "truncatus")
          .combAuthors("1998", "Dash", "Viraktamath")
          .partial(":29");
      assertName("Recilia truncatus Dash & Viraktamath, 1998:29", "Recilia truncatus")
          .species("Recilia", "truncatus")
          .combAuthors("1998", "Dash", "Viraktamath")
          .partial(":29");
  }

  @Test
  public void yearInSquareBrackets() throws Exception {
      // group: Year in square brackets
      assertName("Anthoscopus Cabanis [1851]", "Anthoscopus")
          .monomial("Anthoscopus")
          .combAuthors("1851", "Cabanis");
      assertName("Anthoscopus Cabanis [185?]", "Anthoscopus")
          .monomial("Anthoscopus")
          .combAuthors("185?", "Cabanis");
      assertName("Anthoscopus Cabanis [1851?]", "Anthoscopus")
          .monomial("Anthoscopus")
          .combAuthors("1851?", "Cabanis");
      assertName("Anthoscopus Cabanis [1851]", "Anthoscopus")
          .monomial("Anthoscopus")
          .combAuthors("1851", "Cabanis");
      assertName("Anthoscopus Cabanis [1851?]", "Anthoscopus")
          .monomial("Anthoscopus")
          .combAuthors("1851?", "Cabanis");
      assertName("Trismegistia monodii Ando, 1973 [1974]", "Trismegistia monodii")
          .species("Trismegistia", "monodii")
          .combAuthors("1973", "Ando");
      assertName("Zygaena witti Wiegel [1973]", "Zygaena witti")
          .species("Zygaena", "witti")
          .combAuthors("1973", "Wiegel");
      assertName("Deyeuxia coarctata Kunth, 1815 [1816]", "Deyeuxia coarctata")
          .species("Deyeuxia", "coarctata")
          .combAuthors("1815", "Kunth");
  }

  @Test
  public void utf80x3000CharacterIdeographicSpace() throws Exception {
      // group: UTF-8 0x3000 character (IDEOGRAPHIC_SPACE)
      assertName("Kinosternidae　Agassiz, 1857", "Kinosternidae")
          .monomial("Kinosternidae")
          .combAuthors("1857", "Agassiz");
  }

  @Test
  public void namesWithExAsSpEpithet() throws Exception {
      // group: Names with 'ex' as sp. epithet
      assertName("Acanthochiton exquisitus", "Acanthochiton exquisitus")
          .species("Acanthochiton", "exquisitus");
  }

  @Test
  public void namesWithSpanishYInsteadOf() throws Exception {
      // group: Names with Spanish 'y' instead of '&'
      assertName("Caloptenopsis crassiusculus (Martínez y Fernández-Castillo, 1896)", "Caloptenopsis crassiusculus")
          .species("Caloptenopsis", "crassiusculus")
          .basAuthors("1896", "Martínez", "Fernández-Castillo");
      assertName("Dicranum saxatile Lagasca y Segura, García & Clemente y Rubio, 1802", "Dicranum saxatile")
          .species("Dicranum", "saxatile")
          .combAuthors("1802", "Lagasca", "Segura", "García", "Clemente", "Rubio");
      assertName("Carabus (Tanaocarabus) hendrichsi Bolvar y Pieltain, Rotger & Coronado 1967", "Carabus hendrichsi")
          .species("Carabus", "Tanaocarabus", "hendrichsi")
          .combAuthors("1967", "Bolvar", "Pieltain", "Rotger", "Coronado");
  }

  @Test
  public void normalizeAtypicalDashes() throws Exception {
      // group: Normalize atypical dashes
      assertName("Passalus (Pertinax) gaboi Jiménez‑Ferbans & Reyes‑Castillo, 2022", "Passalus gaboi")
          .species("Passalus", "gaboi")
          .combAuthors("2022", "Jiménez-Ferbans", "Reyes-Castillo");
  }

  @Test
  public void discardApostrophesAtTheStartAndEndOfWords() throws Exception {
      // group: Discard apostrophes at the start and end of words
      assertName("Labeotropheus trewavasae 'albino", "Labeotropheus trewavasae")
          .species("Labeotropheus", "trewavasae");
      assertName("Labeotropheus trewavasae albino'", "Labeotropheus trewavasae")
          .species("Labeotropheus", "trewavasae");
      assertName("Phedimus takesimensis (Nakai) 't Hart", "Phedimus takesimensis")
          .species("Phedimus", "takesimensis")
          .combAuthors(null, "'t Hart")
          .basAuthors(null, "Nakai");
  }

  @Test
  public void discardApostropheWithDashRareNeedsFurtherInvestigation() throws Exception {
      // group: Discard apostrophe with dash (rare, needs further investigation)
      assertName("Solanum juzepczukii janck'o-ckaisalla", "Solanum juzepczukii jancko-ckaisalla")
          .infraSpecies("Solanum", "juzepczukii", INFRASPECIFIC_NAME, "jancko-ckaisalla");
  }

  @Test
  public void possibleCanonical() throws Exception {
      // group: Possible canonical
      assertName("Morea (Morea) burtius 2342343242 23424322342 23424234", "Morea burtius")
          .species("Morea", "burtius");
      assertName("Verpericola megasoma \"\"Dall\" Pils.", "Verpericola megasoma")
          .species("Verpericola", "megasoma");
      assertName("Verpericola megasoma \"Dall\" Pils.", "Verpericola megasoma")
          .species("Verpericola", "megasoma");
      assertName("Moraea spathulata ( (L. f. Klatt", "Moraea spathulata")
          .species("Moraea", "spathulata");
      assertName("Stewartia micrantha (Chun) Sealy, Bot. Mag. 176: t. 510. 1967.", "Stewartia micrantha")
          .species("Stewartia", "micrantha")
          .combAuthors(null, "Sealy", "Bot.Mag.")
          .basAuthors(null, "Chun");
      assertName("Pyrobaculum neutrophilum V24Sta", "Pyrobaculum neutrophilum")
          .species("Pyrobaculum", "neutrophilum");
      assertName("Rana aurora Baird and Girard, 1852; H.B. Shaffer et al., 2004", "Rana aurora")
          .species("Rana", "aurora")
          .combAuthors("1852", "Baird", "Girard");
      assertName("Agropyron pectiniforme var. karabaljikji ined.?", "Agropyron pectiniforme karabaljikji")
          .infraSpecies("Agropyron", "pectiniforme", VARIETY, "karabaljikji");
      assertName("Staphylococcus hyicus chromogenes Devriese et al. 1978 (Approved Lists 1980).", "Staphylococcus hyicus chromogenes")
          .infraSpecies("Staphylococcus", "hyicus", INFRASPECIFIC_NAME, "chromogenes")
          .combAuthors("1978", "Devriese et al.");
  }

  @Test
  public void treatingAlAsEtAl() throws Exception {
      // group: Treating `& al.` as `et al.`
      assertName("Adonis cyllenea Boiss. & al.", "Adonis cyllenea")
          .species("Adonis", "cyllenea")
          .combAuthors(null, "Boiss.et al.");
      assertName("Adonis cyllenea Boiss. & al", "Adonis cyllenea")
          .species("Adonis", "cyllenea")
          .combAuthors(null, "Boiss.et al.");
      assertName("Adonis cyllenea Boiss. & al. var. paryadrica Boiss.", "Adonis cyllenea paryadrica")
          .infraSpecies("Adonis", "cyllenea", VARIETY, "paryadrica")
          .combAuthors(null, "Boiss.");
      assertName("Adonis cyllenea Boiss. & al var. paryadrica Boiss.", "Adonis cyllenea paryadrica")
          .infraSpecies("Adonis", "cyllenea", VARIETY, "paryadrica")
          .combAuthors(null, "Boiss.");
      assertName("Adetus fuscoapicalis Souza f. et al. 2001", "Adetus fuscoapicalis")
          .species("Adetus", "fuscoapicalis")
          .combAuthors("2001", "Souza fil.et al.");
      assertName("Sterigmostemon rhodanthum Rech. f. et al. in Rech. f.", "Sterigmostemon rhodanthum")
          .species("Sterigmostemon", "rhodanthum")
          .combAuthors(null, "Rech.fil.et al.");
  }

  @Test
  public void authorsDoNotStartWithApostrophe() throws Exception {
      // group: Authors do not start with apostrophe
      assertName("Nereidavus kulkovi 'Kulkov", "Nereidavus kulkovi")
          .species("Nereidavus", "kulkovi");
  }

  @Test
  public void epithetsDoNotStartOrEndWithADash() throws Exception {
      // group: Epithets do not start or end with a dash
      assertName("Abryna -petri Paiva, 1860", "Abryna")
          .monomial("Abryna");
      assertName("Abryna petri- Paiva, 1860", "Abryna")
          .monomial("Abryna");
  }

  @Test
  public void namesThatContainOf() throws Exception {
      // group: Names that contain "of"
      assertName("Musca capraria Trustees of the British Museum (Natural History), 1939", "Musca capraria")
          .species("Musca", "capraria")
          .combAuthors(null, "Trustees");
      assertName("Nassellarid genera of uncertain affinities", "Nassellarid genera")
          .species("Nassellarid", "genera");
      assertName("Natica of nidus", "Natica")
          .monomial("Natica");
      assertName("Neritina chemmoi Reeve var of cornea Linn", "Neritina chemmoi")
          .species("Neritina", "chemmoi")
          .combAuthors(null, "Reeve");
  }

  @Test
  public void cultivars() throws Exception {
      // group: Cultivars
      assertName("Sarracenia flava 'Maxima'", "Sarracenia flava")
          .species("Sarracenia", "flava");
  }

  @Test
  public void openTaxonomyWithRanksUnfinished() throws Exception {
      // group: "Open taxonomy" with ranks unfinished
      assertName("Alyxia reinwardti var", "Alyxia reinwardti")
          .species("Alyxia", "reinwardti");
      assertName("Alyxia reinwardti var.", "Alyxia reinwardti")
          .species("Alyxia", "reinwardti");
      assertName("Alyxia reinwardti ssp", "Alyxia reinwardti")
          .species("Alyxia", "reinwardti");
      assertName("Alyxia reinwardti ssp.", "Alyxia reinwardti")
          .species("Alyxia", "reinwardti");
      // skipped: Alaria spp
      // skipped: Alaria spp.
      // skipped: Xenodon sp
      // skipped: Xenodon sp.
      // skipped: Formicidae cf.
      // skipped: Formicidae cf
      // skipped: Arctostaphylos preglauca cf.
      // skipped: Albinaria brevicollis cf. sica Fuchs & Kaufel 1936
      // skipped: Albinaria cf brevicollis sica Fuchs & Kaufel 1936
      // skipped: Albinaria brevicollis cf
      // skipped: Acastoides spp.
  }

  @Test
  public void ignoringSerovarSerotype() throws Exception {
      // group: Ignoring serovar/serotype
      assertName("Aggregatibacter actinomycetemcomitans serotype d str. SA508", "Aggregatibacter actinomycetemcomitans")
          .species("Aggregatibacter", "actinomycetemcomitans");
      // skipped: Bacterium sp. (serotype) aboney Dräger 1951
      assertName("Streptococcus pyogenes (serotype M18)", "Streptococcus pyogenes")
          .species("Streptococcus", "pyogenes");
      assertName("Actinobacillus pleuropneumoniae serovar 2 strain S1536", "Actinobacillus pleuropneumoniae")
          .species("Actinobacillus", "pleuropneumoniae");
      assertName("Leptospira interrogans serovar Fugis", "Leptospira interrogans")
          .species("Leptospira", "interrogans");
  }

  @Test
  public void ignoringSensuSec() throws Exception {
      // group: Ignoring sensu sec
      assertName("Senecio legionensis sensu Samp., non Lange", "Senecio legionensis")
          .species("Senecio", "legionensis");
      assertName("Pseudomonas methanica (Söhngen 1906) sensu. Dworkin and Foster 1956", "Pseudomonas methanica")
          .species("Pseudomonas", "methanica")
          .basAuthors("1906", "Söhngen");
      assertName("Abarema scutifera sensu auct., non (Blanco)Kosterm.", "Abarema scutifera")
          .species("Abarema", "scutifera");
      assertName("Puya acris Auct.", "Puya acris")
          .species("Puya", "acris");
      assertName("Puya acris Auct non L.", "Puya acris")
          .species("Puya", "acris");
      assertName("Galium tricorne Stokes, pro parte", "Galium tricorne")
          .species("Galium", "tricorne")
          .combAuthors(null, "Stokes");
      assertName("Galium tricorne Stokes,pro parte", "Galium tricorne")
          .species("Galium", "tricorne")
          .combAuthors(null, "Stokes");
      assertName("Senecio jacquinianus sec. Rchb.", "Senecio jacquinianus")
          .species("Senecio", "jacquinianus");
      assertName("Acantholimon ulicinum s.l. (Schultes) Boiss.", "Acantholimon ulicinum")
          .species("Acantholimon", "ulicinum");
      assertName("Acantholimon ulicinum s. l. (Schultes) Boiss.", "Acantholimon ulicinum")
          .species("Acantholimon", "ulicinum");
      assertName("Acantholimon ulicinum S. L. Schultes", "Acantholimon ulicinum")
          .species("Acantholimon", "ulicinum")
          .combAuthors(null, "S.L.Schultes");
      assertName("Amitostigma formosana (S.S.Ying) S.S.Ying", "Amitostigma formosana")
          .species("Amitostigma", "formosana")
          .combAuthors(null, "S.S.Ying")
          .basAuthors(null, "S.S.Ying");
      assertName("Amaurorhinus bewichianus (Wollaston,1860) (s.str.)", "Amaurorhinus bewichianus")
          .species("Amaurorhinus", "bewichianus")
          .basAuthors("1860", "Wollaston");
      assertName("Ammodramus caudacutus (s.s.) diversus", "Ammodramus caudacutus")
          .species("Ammodramus", "caudacutus");
      assertName("Arenaria serpyllifolia L. s.str.", "Arenaria serpyllifolia")
          .species("Arenaria", "serpyllifolia")
          .combAuthors(null, "L.");
      assertName("Asplenium trichomanes L. s.lat. - Asplen trich", "Asplenium trichomanes")
          .species("Asplenium", "trichomanes")
          .combAuthors(null, "L.");
      assertName("Asplenium anisophyllum Kunze, s.l.", "Asplenium anisophyllum")
          .species("Asplenium", "anisophyllum")
          .combAuthors(null, "Kunze");
      assertName("Abramis Cuvier 1816 sec. Dybowski 1862", "Abramis")
          .monomial("Abramis")
          .combAuthors("1816", "Cuvier");
      assertName("Abramis brama subsp. bergi Grib & Vernidub 1935 sec Eschmeyer 2004", "Abramis brama bergi")
          .infraSpecies("Abramis", "brama", SUBSPECIES, "bergi")
          .combAuthors("1935", "Grib", "Vernidub");
      assertName("Abarema clypearia (Jack) Kosterm., P. P.", "Abarema clypearia")
          .species("Abarema", "clypearia")
          .combAuthors(null, "Kosterm.")
          .basAuthors(null, "Jack");
      assertName("Abarema clypearia (Jack) Kosterm., p.p.", "Abarema clypearia")
          .species("Abarema", "clypearia")
          .combAuthors(null, "Kosterm.")
          .basAuthors(null, "Jack");
      assertName("Abarema clypearia (Jack) Kosterm., p. p.", "Abarema clypearia")
          .species("Abarema", "clypearia")
          .combAuthors(null, "Kosterm.")
          .basAuthors(null, "Jack");
      assertName("Indigofera phyllogramme var. aphylla R.Vig., p.p.B", "Indigofera phyllogramme aphylla")
          .infraSpecies("Indigofera", "phyllogramme", VARIETY, "aphylla")
          .combAuthors(null, "R.Vig.");
  }

  @Test
  public void ignoreTerminalAnnotations() throws Exception {
      // group: Ignore terminal annotations
      assertName("Abida secale margaridae I.M.Fake Ms", "Abida secale margaridae")
          .infraSpecies("Abida", "secale", INFRASPECIFIC_NAME, "margaridae")
          .combAuthors(null, "I.M.Fake");
      assertName("Abida secale margaridae I.M.Fake ms", "Abida secale margaridae")
          .infraSpecies("Abida", "secale", INFRASPECIFIC_NAME, "margaridae")
          .combAuthors(null, "I.M.Fake");
  }

  @Test
  public void unparseableHortAnnotations() throws Exception {
      // group: Unparseable hort. annotations
      assertName("Asplenium mayi ht.May; Gard.", "Asplenium mayi")
          .species("Asplenium", "mayi");
      assertName("Asplenium mayii ht.May; Gard.", "Asplenium mayii")
          .species("Asplenium", "mayii");
      assertName("Davallia decora ht.Bull.; Gard.Chr.", "Davallia decora")
          .species("Davallia", "decora");
      assertName("Gymnogramma alstoni ht.Birkenh.; Gard.", "Gymnogramma alstoni")
          .species("Gymnogramma", "alstoni");
      assertName("Gymnogramma sprengeriana ht.Wiener Ill.", "Gymnogramma sprengeriana")
          .species("Gymnogramma", "sprengeriana");
  }

  @Test
  public void removingNomenclaturalAnnotations() throws Exception {
      // group: Removing nomenclatural annotations
      assertName("Amphiprora pseudoduplex (Osada & Kobayasi, 1990) comb. nov.", "Amphiprora pseudoduplex")
          .species("Amphiprora", "pseudoduplex")
          .basAuthors("1990", "Osada", "Kobayasi");
      assertName("Methanosarcina barkeri str. fusaro", "Methanosarcina barkeri")
          .species("Methanosarcina", "barkeri");
      assertName("Arthopyrenia hyalospora (Nyl.) R.C. Harris comb. nov.", "Arthopyrenia hyalospora")
          .species("Arthopyrenia", "hyalospora")
          .combAuthors(null, "R.C.Harris")
          .basAuthors(null, "Nyl.");
      assertName("Acanthophis lancasteri WELLS & WELLINGTON (nomen nudum)", "Acanthophis lancasteri")
          .species("Acanthophis", "lancasteri")
          .combAuthors(null, "Wells", "Wellington");
      assertName("Acontias lineatus WAGLER 1830: 196 (nomen nudum)", "Acontias lineatus")
          .species("Acontias", "lineatus")
          .combAuthors("1830", "Wagler");
      assertName("Akeratidae Nomen Nudum", "Akeratidae")
          .monomial("Akeratidae");
      assertName("Aster exilis Ell., nomen dubium", "Aster exilis")
          .species("Aster", "exilis")
          .combAuthors(null, "Ell.");
      assertName("Abutilon avicennae Gaertn., nom. illeg.", "Abutilon avicennae")
          .species("Abutilon", "avicennae")
          .combAuthors(null, "Gaertn.");
      assertName("Achillea bonarota nom. in herb.", "Achillea bonarota")
          .species("Achillea", "bonarota");
      assertName("Aconitum napellus var. formosum (Rchb.) W. D. J. Koch (nom. ambig.)", "Aconitum napellus formosum")
          .infraSpecies("Aconitum", "napellus", VARIETY, "formosum")
          .combAuthors(null, "W.D.J.Koch")
          .basAuthors(null, "Rchb.");
      assertName("Aesculus canadensis Hort. ex Lavallée", "Aesculus canadensis")
          .species("Aesculus", "canadensis")
          .combAuthors(null, "Hort.");
      assertName("× Dialaeliopsis hort.", "Dialaeliopsis")
          .monomial("Dialaeliopsis");
  }

  @Test
  public void miscAnnotations() throws Exception {
      // group: Misc annotations
      assertName("Feldmannia species", "Feldmannia")
          .monomial("Feldmannia");
      assertName("Periglypta G. Paulay, MS", "Periglypta")
          .monomial("Periglypta")
          .combAuthors(null, "G.Paulay");
      assertName("Teredo not found", "Teredo")
          .monomial("Teredo");
      assertName("Velutina haliotoides (Linnaeus, 1758), sensu Fabricius, 1780", "Velutina haliotoides")
          .species("Velutina", "haliotoides")
          .basAuthors("1758", "Linnaeus");
      assertName("Acarospora cratericola cratericola Shenk 1974 group", "Acarospora cratericola cratericola")
          .infraSpecies("Acarospora", "cratericola", INFRASPECIFIC_NAME, "cratericola")
          .combAuthors("1974", "Shenk");
      assertName("Acarospora cratericola cratericola Shenk 1974 species group", "Acarospora cratericola cratericola")
          .infraSpecies("Acarospora", "cratericola", INFRASPECIFIC_NAME, "cratericola")
          .combAuthors("1974", "Shenk");
      assertName("Acarospora cratericola cratericola Shenk 1974 species complex", "Acarospora cratericola cratericola")
          .infraSpecies("Acarospora", "cratericola", INFRASPECIFIC_NAME, "cratericola")
          .combAuthors("1974", "Shenk");
      assertName("Parus caeruleus species complex", "Parus caeruleus")
          .species("Parus", "caeruleus");
      assertName("Crenarchaeote enrichment culture clone OREC-B1022", "Crenarchaeote")
          .monomial("Crenarchaeote");
      assertName("Diodora dorsata  CF", "Diodora dorsata")
          .species("Diodora", "dorsata");
      assertName("Dasysyrphus intrudens complex sp. BBDCQ003-10", "Dasysyrphus intrudens")
          .species("Dasysyrphus", "intrudens");
  }

  @Test
  public void horticulturalAnnotation() throws Exception {
      // group: Horticultural annotation
      assertName("Lachenalia tricolor var. nelsonii (ht.) Baker", "Lachenalia tricolor nelsonii")
          .infraSpecies("Lachenalia", "tricolor", VARIETY, "nelsonii");
      assertName("Lachenalia tricolor var. nelsonii (hort.) Baker", "Lachenalia tricolor nelsonii")
          .infraSpecies("Lachenalia", "tricolor", VARIETY, "nelsonii");
      assertName("Puya acris ht.", "Puya acris")
          .species("Puya", "acris");
      assertName("Puya acris hort.", "Puya acris")
          .species("Puya", "acris");
  }

  @Test
  public void namesWithMihi() throws Exception {
      // group: Names with "mihi"
      assertName("Characium obovatum mihi. var. longipes mihi", "Characium obovatum longipes")
          .infraSpecies("Characium", "obovatum", VARIETY, "longipes");
      assertName("Regulus modestus mihi. Gould 1837", "Regulus modestus")
          .species("Regulus", "modestus")
          .combAuthors("1837", "Gould");
  }

  @Test
  public void exceptionsWithMihi() throws Exception {
      // group: Exceptions with "mihi"
      assertName("Eucyclops serrulatus mihi Dussart, Graf & Husson, 1966", "Eucyclops serrulatus mihi")
          .infraSpecies("Eucyclops", "serrulatus", INFRASPECIFIC_NAME, "kihi")
          .combAuthors("1966", "Dussart", "Graf", "Husson");
  }

  @Test
  public void exceptionsFromRanksRankLineEpithets() throws Exception {
      // group: Exceptions from ranks (rank-line epithets)
      assertName("Selenops ab Logunov & Jäger, 2015", "Selenops ab")
          .species("Selenops", "ab")
          .combAuthors("2015", "Logunov", "Jäger");
      assertName("Helophorus (Lihelophorus) ser Zaitzev, 1908", "Helophorus ser")
          .species("Helophorus", "ser")
          .combAuthors("1908", "Zaitzev");
      assertName("Serina subser Gredler, 1898", "Serina subser")
          .species("Serina", "subser")
          .combAuthors("1898", "Gredler");
      assertName("Serina ser Gredler, 1898", "Serina ser")
          .species("Serina", "ser")
          .combAuthors("1898", "Gredler");
  }

  @Test
  public void exceptionsFromAuthorPrefixesPrefixLikeEpithets() throws Exception {
      // group: Exceptions from author prefixes (prefix-like epithets)
      assertName("Campylosphaera dela (M.N.Bramlette & F.R.Sullivan) W.W.Hay & H.Mohler", "Campylosphaera dela")
          .species("Campylosphaera", "dela")
          .combAuthors(null, "W.W.Hay", "H.Mohler")
          .basAuthors(null, "M.N.Bramlette", "F.R.Sullivan");
      assertName("Antaplaga dela Druce, 1904", "Antaplaga dela")
          .species("Antaplaga", "dela")
          .combAuthors("1904", "Druce");
      assertName("Baeolidia dela (Er. Marcus & Ev. Marcus, 1960)", "Baeolidia dela")
          .species("Baeolidia", "dela")
          .basAuthors("1960", "Er.Marcus", "Ev.Marcus");
      assertName("Dicentria dela Druce, 1894", "Dicentria dela")
          .species("Dicentria", "dela")
          .combAuthors("1894", "Druce");
      assertName("Eulaira dela Chamberlin & Ivie, 1933", "Eulaira dela")
          .species("Eulaira", "dela")
          .combAuthors("1933", "Chamberlin", "Ivie");
      assertName("Paralvinella dela Detinova, 1988", "Paralvinella dela")
          .species("Paralvinella", "dela")
          .combAuthors("1988", "Detinova");
      assertName("Scoparia dela Clarke, 1965", "Scoparia dela")
          .species("Scoparia", "dela")
          .combAuthors("1965", "Clarke");
      assertName("Tortolena dela Chamberlin & Ivie, 1941", "Tortolena dela")
          .species("Tortolena", "dela")
          .combAuthors("1941", "Chamberlin", "Ivie");
      assertName("Semiothisa da Dyar, 1916", "Semiothisa da")
          .species("Semiothisa", "da")
          .combAuthors("1916", "Dyar");
      assertName("Gnathopleustes den (J.L. Barnard, 1969)", "Gnathopleustes den")
          .species("Gnathopleustes", "den")
          .basAuthors("1969", "J.L.Barnard");
      assertName("Agnetina den Cao, T.K.T. & Bae, 2006", "Agnetina den")
          .species("Agnetina", "den")
          .combAuthors("2006", "Cao", "T.K.T.", "Bae");
      assertName("Desmoxytes des Srisonchai, Enghoff & Panha, 2016", "Desmoxytes des")
          .species("Desmoxytes", "des")
          .combAuthors("2016", "Srisonchai", "Enghoff", "Panha");
      assertName("Meteorus dos Zitani, 1998", "Meteorus dos")
          .species("Meteorus", "dos")
          .combAuthors("1998", "Zitani");
      assertName("Stenoecia dos Freyer, 1838", "Stenoecia dos")
          .species("Stenoecia", "dos")
          .combAuthors("1838", "Freyer");
      assertName("Sympycnus du Curran, 1929", "Sympycnus du")
          .species("Sympycnus", "du")
          .combAuthors("1929", "Curran");
      assertName("Bolitoglossa la Campbell, Smith, Streicher, Acevedo & Brodie, 2010", "Bolitoglossa la")
          .species("Bolitoglossa", "la")
          .combAuthors("2010", "Campbell", "Smith", "Streicher", "Acevedo", "Brodie");
      assertName("Leptonetela la Wang & Li, 2017", "Leptonetela la")
          .species("Leptonetela", "la")
          .combAuthors("2017", "Wang", "Li");
      assertName("Nocaracris van Ünal, 2016", "Nocaracris van")
          .species("Nocaracris", "van")
          .combAuthors("2016", "Ünal");
      assertName("Zodarion van Bosmans, 2009", "Zodarion van")
          .species("Zodarion", "van")
          .combAuthors("2009", "Bosmans");
      assertName("Malamatidia zu Jäger & Dankittipakul, 2010", "Malamatidia zu")
          .species("Malamatidia", "zu")
          .combAuthors("2010", "Jäger", "Dankittipakul");
  }

  @Test
  public void exceptionsFromAuthorSuffixesSuffixLikeEpithets() throws Exception {
      // group: Exceptions from author suffixes (suffix-like epithets)
      assertName("Ruteloryctes bis Dechambre, 2006", "Ruteloryctes bis")
          .species("Ruteloryctes", "bis")
          .combAuthors("2006", "Dechambre");
  }

  @Test
  public void icvcnBinomialNamesAndExceptions() throws Exception {
      // group: ICVCN binomial names and exceptions
      // skipped: Tokiviricetes
      // skipped: Usarudivirus nymphense
      // skipped: Ictavirus ictaluridallo1
      // skipped: Aghbyvirus ISAO8
      assertName("Mahavira", "Mahavira")
          .monomial("Mahavira");
  }

  @Test
  public void notParsedOcrErrorsToGetBetterPrecisionRecallRatio() throws Exception {
      // group: Not parsed OCR errors to get better precision/recall ratio
      // skipped: Mom.alpium (Osbeck, 1778)
  }

  @Test
  public void noParsingGeneraAbbreviatedTo3LettersTooRare() throws Exception {
      // group: No parsing -- Genera abbreviated to 3 letters (too rare)
      // skipped: Gen. et n. sp. Kaimatira Pumice Sand, Marton N ~1 Ma
      // skipped: Genn. et n. sp. Kaimatira Pumice Sand, Marton N ~1 Ma
  }

  @Test
  public void noParsingIncertaeSedis() throws Exception {
      // group: No parsing -- incertae sedis
      // skipped: Incertae sedis
      // skipped: </i>Hipponicidae<i> incertae sedis</i>
      // skipped: incertae sedis
      // skipped: Inc.   sed.
      // skipped: inc.sed.
      // skipped: inc.   sed.
      // skipped: Incertaesedis obscuricornis Fairmaire LMH 1893
      // skipped: Uropodoideaincertaesedis
  }

  @Test
  public void noParsingBacteriumCandidatus() throws Exception {
      // group: No parsing -- bacterium, Candidatus
      // skipped: Acidobacteria bacterium
      // skipped: Oscillatoriales cyanobacterium PCC 10608
      // skipped: Acidimicrobiales bacterium JGI 01_E13
      assertName("Acidobacterium ailaaui Myers & King, 2016", "Acidobacterium ailaaui")
          .species("Acidobacterium", "ailaaui")
          .combAuthors("2016", "Myers", "King");
      // skipped: Candidatus Amesbacteria bacterium GW2011_GWC1_46_24
      assertName("Candidatus", "Candidatus")
          .monomial("Candidatus");
      assertName("Candidatus Puniceispirillum Oh, Kwon, Kang, Kang, Lee, Kim & Cho, 2010", "Puniceispirillum")
          .monomial("Puniceispirillum")
          .combAuthors("2010", "Oh", "Kwon", "Kang", "Kang", "Lee", "Kim", "Cho");
      assertName("Candidatus Halobonum", "Halobonum")
          .monomial("Halobonum");
      // skipped: Candidatus Endomicrobium sp. MdDo-005
      // skipped: Candidatus Abawacabacteria bacterium
      assertName("Candidatus Accumulibacter phosphatis clade IIA str. UW-1", "Accumulibacter phosphatis")
          .species("Accumulibacter", "phosphatis");
      assertName("Candidatus Anammoxoglobus environmental samples", "Anammoxoglobus")
          .monomial("Anammoxoglobus");
  }

  @Test
  public void noParsingNotNoneUnidentifiedPhrases() throws Exception {
      // group: No parsing -- 'Not', 'None', 'Unidentified'  phrases
      // skipped: None recorded
      // skipped: NONE recorded
      // skipped: NoNe recorded
      // skipped: None
      // skipped: unidentified recorded
      // skipped: UniDentiFied recorded
      // skipped: not recorded
      // skipped: NOT recorded
      // skipped: Not recorded
      // skipped: Not assigned
      assertName("Notassigned", "Notassigned")
          .monomial("Notassigned");
      // skipped: Unnamed clade
      // skipped: Unamed clade
  }

  @Test
  public void noParsingGenusWithApostrophe() throws Exception {
      // group: No parsing -- genus with apostrophe
      // skipped: Abbott's moray eel
      // skipped: Chambers' twinpod
      // skipped: Columnea × Alladin's
      // skipped: Hawai'i silversword
  }

  @Test
  public void noParsingCamelcaseGenusWord() throws Exception {
      // group: No parsing -- CamelCase 'genus' word
      // skipped: PomaTomus
      // skipped: DizygopUwa stosei
      // skipped: Oxytox[idae] Lindermann
      // skipped: ScarabaeinGCsp.
  }

  @Test
  public void noParsingPhytoplasma() throws Exception {
      // group: No parsing -- phytoplasma
      // skipped: Alfalfa witches'-broom phytoplasma
      // skipped: Allium ampeloprasumphytoplasma
      // skipped: Alstroemeria sp. phytoplasma
  }

  @Test
  public void noParsingSymbiont() throws Exception {
      // group: No parsing symbiont
      // skipped: Alvinella pompejana symbiont
      // skipped: Acyrthosiphon kondoi endosymbiont
      // skipped: Burkholderia sp. (Gigaspora margarita endosymbiont)
      assertName("Dictyochloropsis symbiontica Tschermak-Woess", "Dictyochloropsis symbiontica")
          .species("Dictyochloropsis", "symbiontica")
          .combAuthors(null, "Tschermak-Woess");
      assertName("Dylakosoma symbionticum var. valens Skuja", "Dylakosoma symbionticum valens")
          .infraSpecies("Dylakosoma", "symbionticum", VARIETY, "valens")
          .combAuthors(null, "Skuja");
      // skipped: Wolbachia endosymbiont of Leptogenys gracilis
  }

  @Test
  public void namesWithSpecNovSpec() throws Exception {
      // group: Names with spec., nov spec
      assertName("Lampona spec Platnick, 2000", "Lampona spec")
          .species("Lampona", "spec")
          .combAuthors("2000", "Platnick");
      assertName("Gobiosoma spec (Ginsburg, 1939)", "Gobiosoma spec")
          .species("Gobiosoma", "spec")
          .basAuthors("1939", "Ginsburg");
      assertName("Globigerina spec", "Globigerina")
          .monomial("Globigerina");
      assertName("Eunotia genuflexa Norpel-Schempp nov spec", "Eunotia genuflexa")
          .species("Eunotia", "genuflexa")
          .combAuthors(null, "Norpel-Schempp");
      assertName("Ctenotus spec.", "Ctenotus")
          .monomial("Ctenotus");
      assertName("Byrsophlebidae spec. 2", "Byrsophlebidae")
          .monomial("Byrsophlebidae");
      assertName("Naviculadicta witkowskii LB & Metzeltin nov spec", "Naviculadicta witkowskii")
          .species("Naviculadicta", "witkowskii")
          .combAuthors(null, "LB", "Metzeltin");
  }

  @Test
  public void htmlTagsAndEntities() throws Exception {
      // group: HTML tags and entities
      assertName("Velutina haliotoides (Linnaeus, 1758) <i>sensu</i> Fabricius, 1780", "Velutina haliotoides")
          .species("Velutina", "haliotoides")
          .basAuthors("1758", "Linnaeus");
      assertName("Velutina haliotoides (Linnaeus, 1758), <i>sensu</i> Fabricius, 1780", "Velutina haliotoides")
          .species("Velutina", "haliotoides")
          .basAuthors("1758", "Linnaeus");
      assertName("<i>Velutina halioides</i> (Linnaeus, 1758)", "Velutina halioides")
          .species("Velutina", "halioides")
          .basAuthors("1758", "Linnaeus");
      assertName("Quadrella steyermarkii (Standl.) Iltis &amp; Cornejo", "Quadrella steyermarkii")
          .species("Quadrella", "steyermarkii")
          .combAuthors(null, "Iltis", "Cornejo")
          .basAuthors(null, "Standl.");
      assertName("Torymus bangalorensis (Mani &amp; Kurian, 1953)", "Torymus bangalorensis")
          .species("Torymus", "bangalorensis")
          .basAuthors("1953", "Mani", "Kurian");
  }

  @Test
  public void underscoresInsteadOfSpaces() throws Exception {
      // group: Underscores instead of spaces
      assertName("Oxalis_barrelieri", "Oxalis barrelieri")
          .species("Oxalis", "barrelieri");
      // skipped: Oxalis_barrelieri ined.?
      assertName("Pseudocercospora__dendrobii", "Pseudocercospora dendrobii")
          .species("Pseudocercospora", "dendrobii");
      // skipped: Oxalis_barrelieri
      assertName("Oxalis barrelieri XXZ_21243", "Oxalis barrelieri")
          .species("Oxalis", "barrelieri");
  }

  // -------------------- helpers --------------------

  NameAssertion assertName(String rawName, String expectedCanonicalWithoutAuthors) throws UnparsableNameException, InterruptedException {
    ParsedName n = parser.parse(rawName, null, Rank.UNRANKED, null);
    org.junit.Assert.assertEquals(expectedCanonicalWithoutAuthors, n.canonicalNameWithoutAuthorship());
    return new NameAssertion(n);
  }
}

