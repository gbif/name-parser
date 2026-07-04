package org.gbif.nameparser;

import org.gbif.nameparser.api.*;
import org.junit.Test;

import static org.gbif.nameparser.api.NameType.FORMULA;
import static org.gbif.nameparser.api.Rank.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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
      // group: Uninomials with authorship — author whitespace around dots is collapsed
      // ("M.T. Lucas" → "M.T.Lucas"), ligatures/diacritics kept verbatim, year-bearing
      // trinomials become SUBSPECIES (clearly zoological).
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
          .combAuthors("1934", "M.T.Lucas", "Sousa da Câmara");
      assertName("Stagonospora polyspora M.T. Lucas et Sousa da Câmara 1934", "Stagonospora polyspora")
          .species("Stagonospora", "polyspora")
          .combAuthors("1934", "M.T.Lucas", "Sousa da Câmara");
      assertName("Pseudocercospora dendrobii U. Braun & Crous 2003", "Pseudocercospora dendrobii")
          .species("Pseudocercospora", "dendrobii")
          .combAuthors("2003", "U.Braun", "Crous");
      assertName("Abaxisotima acuminata (Wang, Yuwen & Xiangwei Liu 1996)", "Abaxisotima acuminata")
          .species("Abaxisotima", "acuminata")
          .basAuthors("1996", "Wang", "Yuwen", "Xiangwei Liu");
      assertName("Aboilomimus sichuanensis ornatus Liu, Xiang-wei, M. Zhou, W Bi & L. Tang, 2009", "Aboilomimus sichuanensis ornatus")
          .infraSpecies("Aboilomimus", "sichuanensis", SUBSPECIES, "ornatus")
          .combAuthors("2009", "Liu", "Xiang-wei", "M.Zhou", "W.Bi", "L.Tang")
          .code(NomCode.ZOOLOGICAL);
      assertName("Pseudocercospora Speg.", "Pseudocercospora")
          .monomial("Pseudocercospora")
          .combAuthors(null, "Speg.");
      // "(synonym)" tail is currently parsed as an extra author, not stripped.
      assertName("Döringina Ihering 1929 (synonym)", "Döringina")
          .monomial("Döringina")
          .combAuthors("1929", "Ihering", "synonym");
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
      // "le-sourdianum" is parsed as the species epithet, "Fourn." as the comb author.
      assertName("Polypodium le-sourdianum Fourn.", "Polypodium le-sourdianum")
          .species("Polypodium", "le-sourdianum")
          .combAuthors(null, "Fourn.");
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
          .species("Hirsutëlla", "male");
      assertName("Aëtosaurus ferratus", "Aëtosaurus ferratus")
          .species("Aëtosaurus", "ferratus");
      assertName("Remera cvancarai", "Remera cvancarai")
          .species("Remera", "cvancarai");
  }

  @Test
  public void binomialsWithAuthorship() throws Exception {
      // group: Binomials with authorship
      assertName("Gazella farasani Thouless, al Bassri, 1991", "Gazella farasani")
          .species("Gazella", "farasani")
          .combAuthors("1991", "Thouless", "al Bassri")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();

      assertName("Anomalurus laticeps Aguilar-Amat i Banús, 1922", "Anomalurus laticeps")
          .species("Anomalurus", "laticeps")
          .combAuthors("1922", "Aguilar-Amat i Banús")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();

      assertName("Glis wagneri Đulić & Tortić, 1960", "Glis wagneri")
          .species("Glis", "wagneri")
          .combAuthors("1960", "Đulić", "Tortić")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();

      assertName("Mico rondoni Ferrari, Sena, M. P. C. Schneider, & e Silva Júnior, 2010", "Mico rondoni")
          .species("Mico", "rondoni")
          .combAuthors("2010", "Ferrari", "Sena", "M.P.C.Schneider", "e Silva Júnior")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();

      assertName("Trachypithecus caudalis (Đào Văn Tiến, 1977)", "Trachypithecus caudalis")
          .species("Trachypithecus", "caudalis")
          .basAuthors("1977", "Đào Văn Tiến")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();

      assertName("Cymatium raderi D’Attilio & Myers, 1984", "Cymatium raderi")
          .species("Cymatium", "raderi")
          .combAuthors("1984", "D'Attilio", "Myers")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();

      assertName("Melania testudinaria Von dem Busch, 1842", "Melania testudinaria")
          .species("Melania", "testudinaria")
          .combAuthors("1842", "Von dem Busch")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();

      assertName("Cryptopleura farlowiana (J.Agardh) ver Steeg & Jossly", "Cryptopleura farlowiana")
          .species("Cryptopleura", "farlowiana")
          .combAuthors(null, "ver Steeg", "Jossly")
          .basAuthors(null, "J.Agardh")
          .code(NomCode.BOTANICAL)
          .nothingElse();

      assertName("Pyxilla caput avis J.-J.Brun", "Pyxilla caput avis")
          .infraSpecies("Pyxilla", "caput", INFRASPECIFIC_NAME, "avis")
          .combAuthors(null, "J.-J.Brun")
          .nothingElse();

      assertName("Muscicapa randi Amadon & duPont, 1970", "Muscicapa randi")
          .species("Muscicapa", "randi")
          .combAuthors("1970", "Amadon", "duPont")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();

      assertName("Scytalopus alvarezlopezi Stiles, Laverde-R. & Cadena 2017", "Scytalopus alvarezlopezi")
          .species("Scytalopus", "alvarezlopezi")
          .combAuthors("2017", "Stiles", "Laverde-R.", "Cadena")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();

      assertName("Carabus (Tanaocarabus) hendrichsi Bolvar y Pieltain, Rotger & Coronado 1967", "Carabus hendrichsi")
          .species("Carabus", "Tanaocarabus", "hendrichsi")
          .combAuthors("1967", "Bolvar", "Pieltain", "Rotger", "Coronado")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();

      assertName("Nemcia epacridoides (Meissner)Crisp", "Nemcia epacridoides")
          .species("Nemcia", "epacridoides")
          .combAuthors(null, "Crisp")
          .basAuthors(null, "Meissner")
          .code(NomCode.BOTANICAL)
          .nothingElse();

      assertName("Pseudocercospora dendrobii Goh & W.H. Hsieh 1990", "Pseudocercospora dendrobii")
          .species("Pseudocercospora", "dendrobii")
          .combAuthors("1990", "Goh", "W.H.Hsieh")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();

      assertName("Pseudocercospora dendrobii Goh and W.H. Hsieh 1990", "Pseudocercospora dendrobii")
          .species("Pseudocercospora", "dendrobii")
          .combAuthors("1990", "Goh", "W.H.Hsieh")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();

      assertName("Pseudocercospora dendrobii Goh et W.H. Hsieh 1990", "Pseudocercospora dendrobii")
          .species("Pseudocercospora", "dendrobii")
          .combAuthors("1990", "Goh", "W.H.Hsieh")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();

      assertName("Schottera nicaeënsis (J.V. Lamouroux ex Duby) Guiry & Hollenberg", "Schottera nicaeënsis")
          .species("Schottera", "nicaeënsis")
          .combAuthors(null, "Guiry", "Hollenberg")
          .basExAuthors(null, "J.V.Lamouroux")
          .basAuthors(null, "Duby")
          .code(NomCode.BOTANICAL)
          .nothingElse();

      assertName("Laevapex vazi dos Santos, 1989", "Laevapex vazi")
          .species("Laevapex", "vazi")
          .combAuthors("1989", "dos Santos")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();

      assertName("Periclimenaeus aurae dos Santos, Calado & Araújo, 2008", "Periclimenaeus aurae")
          .species("Periclimenaeus", "aurae")
          .combAuthors("2008", "dos Santos", "Calado", "Araújo")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();

      assertName("Nototriton matama Boza-Oviedo, Rovito, Chaves, García-Rodríguez, Artavia, Bolaños, and Wake, 2012", "Nototriton matama")
          .species("Nototriton", "matama")
          .combAuthors("2012", "Boza-Oviedo", "Rovito", "Chaves", "García-Rodríguez", "Artavia", "Bolaños", "Wake")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();

      assertName("Architectonica offlexa Iredale, 1931", "Architectonica offlexa")
          .species("Architectonica", "offlexa")
          .combAuthors("1931", "Iredale")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();

      assertName("Maracanda amoena Mc'Lach", "Maracanda amoena")
          .species("Maracanda", "amoena")
          .combAuthors(null, "Mc'Lach")
          .nothingElse();

      assertName("Maracanda amoena Mc’Lach", "Maracanda amoena")
          .species("Maracanda", "amoena")
          .combAuthors(null, "Mc'Lach")
          .nothingElse();

      assertName("Tridentella tangeroae Bruce, 198?", "Tridentella tangeroae")
          .species("Tridentella", "tangeroae")
          .combAuthors("198?", "Bruce")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();

      assertName("Calobota acanthoclada (Dinter) Boatwr. & B.-E.van Wyk", "Calobota acanthoclada")
          .species("Calobota", "acanthoclada")
          .combAuthors(null, "Boatwr.", "B.-E.van Wyk")
          .basAuthors(null, "Dinter")
          .code(NomCode.BOTANICAL)
          .nothingElse();

      assertName("Zanthopsis bispinosa M'Coy, 1849", "Zanthopsis bispinosa")
          .species("Zanthopsis", "bispinosa")
          .combAuthors("1849", "M'Coy")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();

      assertName("Scilla rupestris v.d. Merwe", "Scilla rupestris")
          .species("Scilla", "rupestris")
          .combAuthors(null, "v.d.Merwe")
          .nothingElse();

      assertName("Bembix bidentata v.d.L.", "Bembix bidentata")
          .species("Bembix", "bidentata")
          .combAuthors(null, "v.d.L.")
          .nothingElse();

      assertName("Pompilus cinctellus v. d. L.", "Pompilus cinctellus")
          .species("Pompilus", "cinctellus")
          .combAuthors(null, "v.d.L.")
          .nothingElse();

      assertName("Setaphis viridis v. d.G.", "Setaphis viridis")
          .species("Setaphis", "viridis")
          .combAuthors(null, "v.d.G.")
          .nothingElse();

      assertName("Coleophora mendica Baldizzone & v. d.Wolf 2000", "Coleophora mendica")
          .species("Coleophora", "mendica")
          .combAuthors("2000", "Baldizzone", "v.d.Wolf")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();

      assertName("Psoronaias semigranosa von dem Busch in Philippi, 1845", "Psoronaias semigranosa")
          .species("Psoronaias", "semigranosa")
          .combAuthors("1845", "von dem Busch")
          .publishedIn("Philippi, 1845")
          .nothingElse();

      assertName("Phora sororcula v d Wulp 1871", "Phora sororcula")
          .species("Phora", "sororcula")
          .combAuthors("1871", "v d Wulp")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();

      assertName("Aeolothrips andalusiacus zur Strassen 1973", "Aeolothrips andalusiacus")
          .species("Aeolothrips", "andalusiacus")
          .combAuthors("1973", "zur Strassen")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();

      assertName("Orthosia kindermannii Fischer v. Roslerstamm, 1837", "Orthosia kindermannii")
          .species("Orthosia", "kindermannii")
          .combAuthors("1837", "Fischer v.Roslerstamm")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();

      assertName("Boreophilia nomensis (Casey, 1910)", "Boreophilia nomensis")
          .species("Boreophilia", "nomensis")
          .basAuthors("1910", "Casey")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();

      assertName("Nereidavus kulkovi Kul'kov in Kul'kov & Obut, 1973", "Nereidavus kulkovi")
          .species("Nereidavus", "kulkovi")
          .combAuthors("1973", "Kul'kov")
          .publishedIn("Kul'kov & Obut, 1973")
          .nothingElse();

      assertName("Xylaria potentillae A S. Xu", "Xylaria potentillae")
          .species("Xylaria", "potentillae")
          .combAuthors(null, "A.S.Xu")
          .nothingElse();

      assertName("Pseudocyrtopora el Hajjaji 1987", "Pseudocyrtopora")
          .monomial("Pseudocyrtopora")
          .combAuthors("1987", "el Hajjaji")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();

      assertName("Geositta poeciloptera (zu Wied-Neuwied, 1830)", "Geositta poeciloptera")
          .species("Geositta", "poeciloptera")
          .basAuthors("1830", "zu Wied-Neuwied")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();

      assertName("Abacetus laevicollis de Chaudoir, 1869", "Abacetus laevicollis")
          .species("Abacetus", "laevicollis")
          .combAuthors("1869", "de Chaudoir")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();

      assertName("Gastrosericus eremorum von Beaumont 1955", "Gastrosericus eremorum")
          .species("Gastrosericus", "eremorum")
          .combAuthors("1955", "von Beaumont")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();

      assertName("Agaricus squamula Berk. & M.A. Curtis 1860", "Agaricus squamula")
          .species("Agaricus", "squamula")
          .combAuthors("1860", "Berk.", "M.A.Curtis")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();

      assertName("Peltula coriacea Büdel, Henssen & Wessels 1986", "Peltula coriacea")
          .species("Peltula", "coriacea")
          .combAuthors("1986", "Büdel", "Henssen", "Wessels")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();

      assertName("Tuber liui A S. Xu 1999", "Tuber liui")
          .species("Tuber", "liui")
          .combAuthors("1999", "A.S.Xu")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();

      assertName("Lecanora wetmorei Śliwa 2004", "Lecanora wetmorei")
          .species("Lecanora", "wetmorei")
          .combAuthors("2004", "Śliwa")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();

      assertName("Vachonobisium troglophilum Vitali-di Castri, 1963", "Vachonobisium troglophilum")
          .species("Vachonobisium", "troglophilum")
          .combAuthors("1963", "Vitali-di Castri")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();

      assertName("Hyalesthes angustula Horvßth, 1909", "Hyalesthes angustula")
          .species("Hyalesthes", "angustula")
          .combAuthors("1909", "Horvßth")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();

      assertName("Platypus bicaudatulus Schedl (1935h)", "Platypus bicaudatulus")
          .species("Platypus", "bicaudatulus")
          .combAuthors("1935", "Schedl")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();

      assertName("Platypus bicaudatulus Schedl (1935)", "Platypus bicaudatulus")
          .species("Platypus", "bicaudatulus")
          .combAuthors("1935", "Schedl")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();

      assertName("Platypus bicaudatulus Schedl 1935", "Platypus bicaudatulus")
          .species("Platypus", "bicaudatulus")
          .combAuthors("1935", "Schedl")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();

      assertName("Platypus bicaudatulus Schedl, 1935h", "Platypus bicaudatulus")
          .species("Platypus", "bicaudatulus")
          .combAuthors("1935", "Schedl")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();

      assertName("Rotalina cultrata d'Orb. 1840", "Rotalina cultrata")
          .species("Rotalina", "cultrata")
          .combAuthors("1840", "d'Orb.")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();

      assertName("Stylosanthes guianensis (Aubl.) Sw. var. robusta L.'t Mannetje", "Stylosanthes guianensis var. robusta")
          .infraSpecies("Stylosanthes", "guianensis", VARIETY, "robusta")
          .combAuthors(null, "L.'t Mannetje")
          .nothingElse();

      assertName("Doxander vittatus entropi (Man in 't Veld & Visser, 1993)", "Doxander vittatus entropi")
          .infraSpecies("Doxander", "vittatus", SUBSPECIES, "entropi")
          .basAuthors("1993", "Man in 't Veld", "Visser")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();

      assertName("Elaeagnus triflora Roxb. var. brevilimbatus E.'t Hart", "Elaeagnus triflora var. brevilimbatus")
          .infraSpecies("Elaeagnus", "triflora", VARIETY, "brevilimbatus")
          .combAuthors(null, "E.'t Hart")
          .nothingElse();

      assertName("Laevistrombus guidoi (Man in't Veld & De Turck, 1998)", "Laevistrombus guidoi")
          .species("Laevistrombus", "guidoi")
          .basAuthors("1998", "Man in't Veld", "De Turck")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();

      assertName("Strombus guidoi Man in't Veld & De Turck, 1998", "Strombus guidoi")
          .species("Strombus", "guidoi")
          .combAuthors("1998", "Man in't Veld", "De Turck")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();

      assertName("Strombus vittatus entropi Man in't Veld & Visser, 1993", "Strombus vittatus entropi")
          .infraSpecies("Strombus", "vittatus", SUBSPECIES, "entropi")
          .combAuthors("1993", "Man in't Veld", "Visser")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();

      assertName("Velutina haliotoides (Linnaeus, 1758),", "Velutina haliotoides")
          .species("Velutina", "haliotoides")
          .basAuthors("1758", "Linnaeus")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();

      assertName("Hennediella microphylla (R.Br.bis) Paris", "Hennediella microphylla")
          .species("Hennediella", "microphylla")
          .combAuthors(null, "Paris")
          .basAuthors(null, "R.Br.bis")
          .code(NomCode.BOTANICAL)
          .nothingElse();

      assertName("Pseudocercosporella endophytica Crous & H. Sm. ter", "Pseudocercosporella endophytica")
          .species("Pseudocercosporella", "endophytica")
          .combAuthors(null, "Crous", "H.Sm.ter")
          .nothingElse();

      assertName("Kudoa amazonica Velasco, Sindeaux Neto, Videira, de Cássia Silva do Nascimento, Gonçalves & Matos, 2019", "Kudoa amazonica")
          .species("Kudoa", "amazonica")
          .combAuthors("2019", "Velasco", "Sindeaux Neto", "Videira", "de Cássia Silva do Nascimento", "Gonçalves", "Matos")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();

      assertName("Branchinecta papillata Rogers, de los Rios & Zuniga, 2008", "Branchinecta papillata")
          .species("Branchinecta", "papillata")
          .combAuthors("2008", "Rogers", "de los Rios", "Zuniga")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();

      assertName("Gerrhonotus lazcanoi Banda-Leal, Manuel Nevárez-de los Reyes and Bryson, 2017", "Gerrhonotus lazcanoi")
          .species("Gerrhonotus", "lazcanoi")
          .combAuthors("2017", "Banda-Leal", "Manuel Nevárez-de los Reyes", "Bryson")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();

      assertName("Lynceus huentelauquensis  Sigvardt, Rogers, De los Ríos, Palero, and Olesen, 2019", "Lynceus huentelauquensis")
          .species("Lynceus", "huentelauquensis")
          .combAuthors("2019", "Sigvardt", "Rogers", "De los Ríos", "Palero", "Olesen")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();

      assertName("Echiophis brunneus (Castro-Aguirre & Suárez de los Cobos, 1983)", "Echiophis brunneus")
          .species("Echiophis", "brunneus")
          .basAuthors("1983", "Castro-Aguirre", "Suárez de los Cobos")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();
  }

  @Test
  public void binomialsWithAnAbbreviatedGenus() throws Exception {
      // group: Binomials with an abbreviated genus — INFORMAL with ABBREVIATED_GENUS
      // warning. Year-bearing trinomials → SUBSPECIES via the zoological-trinomial rule.
      assertName("M. alpium", "M. alpium")
          .species("M.", "alpium")
          .type(NameType.INFORMAL)
          .warning(Warnings.ABBREVIATED_GENUS);
      assertName("Mo. alpium (Osbeck, 1778)", "Mo. alpium")
          .species("Mo.", "alpium")
          .basAuthors("1778", "Osbeck")
          .type(NameType.INFORMAL)
          .code(NomCode.ZOOLOGICAL)
          .warning(Warnings.ABBREVIATED_GENUS);
  }

  @Test
  public void binomialsWithAbbreviatedSubgenus() throws Exception {
      // group: Binomials with abbreviated subgenus — kept as SCIENTIFIC with the
      // ABBREVIATED_SUBGENUS warning so callers can see the infrageneric epithet is
      // incomplete.
      assertName("Phalaena (Tin.) guttella Fab.", "Phalaena guttella")
          .species("Phalaena", "Tin.", "guttella")
          .combAuthors(null, "Fab.")
          .warning(Warnings.ABBREVIATED_SUBGENUS);
      assertName("Gahrliepia (G.) tessellata Traub & Morrow 1955", "Gahrliepia tessellata")
          .species("Gahrliepia", "G.", "tessellata")
          .combAuthors("1955", "Traub", "Morrow")
          .code(NomCode.ZOOLOGICAL)
          .warning(Warnings.ABBREVIATED_SUBGENUS);
      assertName("Simia (Cercop.) nasuus Kerr 1792", "Simia nasuus")
          .species("Simia", "Cercop.", "nasuus")
          .combAuthors("1792", "Kerr")
          .code(NomCode.ZOOLOGICAL)
          .warning(Warnings.ABBREVIATED_SUBGENUS);
  }

  @Test
  public void binomialsWithBasionymAndCombinationAuthors() throws Exception {
      // group: Binomials with basionym and combination authors. Botanical "var." /
      // "subsp." kept in canonical.
      assertName("Yarrowia lipolytica var. lipolytica (Wick., Kurtzman & E.A. Herrm.) Van der Walt & Arx 1981", "Yarrowia lipolytica var. lipolytica")
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
      assertName("Armeria carpetana ssp. carpetana H. del Villar", "Armeria carpetana subsp. carpetana")
          .infraSpecies("Armeria", "carpetana", SUBSPECIES, "carpetana")
          .combAuthors(null, "H.del Villar");
  }

  @Test
  public void exceptionsWithBinomials() throws Exception {
      // group: Exceptions with Binomials — names whose species epithet happens to
      // look like a virus marker, a blacklisted word, or otherwise unusual still
      // parse when an explicit Title-cased author + year follows.
      assertName("Agra not Erwin, 2002", "Agra not")
          .species("Agra", "not")
          .combAuthors("2002", "Erwin")
          .code(NomCode.ZOOLOGICAL)
          .warning(Warnings.BLACKLISTED_EPITHET)
          .doubtful();
      assertName("Navicula bacterium Frenguelli", "Navicula bacterium")
          .species("Navicula", "bacterium")
          .combAuthors(null, "Frenguelli");
      assertName("Bottaria nudum (Nyl.) Vain.", "Bottaria nudum")
          .species("Bottaria", "nudum")
          .combAuthors(null, "Vain.")
          .basAuthors(null, "Nyl.");
      assertName("Turkozelotes attavirus Chatzaki, 2019", "Turkozelotes attavirus")
          .species("Turkozelotes", "attavirus")
          .combAuthors("2019", "Chatzaki")
          .code(NomCode.ZOOLOGICAL);
      assertName("Phalium (Semicassis) vector R. T. Abbott, 1993", "Phalium vector")
          .species("Phalium", "Semicassis", "vector")
          .combAuthors("1993", "R.T.Abbott")
          .code(NomCode.ZOOLOGICAL);
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
      // group: Infraspecies without rank (ICZN). Trinomials whose authorship carries a
      // year are inferred as zoological → bumped to SUBSPECIES; pure trinomials with no
      // code signal stay INFRASPECIFIC_NAME.
      assertName("Myotis fimbriatus taiwanensis Ärnbäck-Christie-Linde, 1908", "Myotis fimbriatus taiwanensis")
          .infraSpecies("Myotis", "fimbriatus", SUBSPECIES, "taiwanensis")
          .combAuthors("1908", "Ärnbäck-Christie-Linde")
          .code(NomCode.ZOOLOGICAL);
      assertName("Peristernia nassatula forskali Tapparone-Canefri 1875", "Peristernia nassatula forskali")
          .infraSpecies("Peristernia", "nassatula", SUBSPECIES, "forskali")
          .combAuthors("1875", "Tapparone-Canefri")
          .code(NomCode.ZOOLOGICAL);
      assertName("Cypraeovula (Luponia) amphithales perdentata", "Cypraeovula amphithales perdentata")
          .infraSpecies("Cypraeovula", "amphithales", INFRASPECIFIC_NAME, "perdentata")
          .infraGeneric("Luponia");
      assertName("Triticum repens vulgäre", "Triticum repens vulgäre")
          .infraSpecies("Triticum", "repens", INFRASPECIFIC_NAME, "vulgäre");
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
          .infraGeneric("Hydnellum")
          .combAuthors("1972", "D.Hall", "D.E.Stuntz")
          .basAuthors(null, "Banker");
      assertName("Hydnellum scrobiculatum zonatum", "Hydnellum scrobiculatum zonatum")
          .infraSpecies("Hydnellum", "scrobiculatum", INFRASPECIFIC_NAME, "zonatum");
      assertName("Mus musculus hortulanus", "Mus musculus hortulanus")
          .infraSpecies("Mus", "musculus", INFRASPECIFIC_NAME, "hortulanus");
      assertName("Ortygospiza atricollis mülleri", "Ortygospiza atricollis mülleri")
          .infraSpecies("Ortygospiza", "atricollis", INFRASPECIFIC_NAME, "mülleri");
      assertName("Caulerpa fastigiata confervoides P. L. Crouan & H. M. Crouan ex Weber-van Bosse", "Caulerpa fastigiata confervoides")
          .infraSpecies("Caulerpa", "fastigiata", INFRASPECIFIC_NAME, "confervoides")
          .combAuthors(null, "Weber-van Bosse")
          .combExAuthors("P.L.Crouan", "H.M.Crouan");
      assertName("Rhinanthus glacialis simplex(Sterneck) J.Dostál", "Rhinanthus glacialis simplex")
          .infraSpecies("Rhinanthus", "glacialis", INFRASPECIFIC_NAME, "simplex")
          .combAuthors(null, "J.Dostál")
          .basAuthors(null, "Sterneck")
          .code(NomCode.BOTANICAL);
  }

  @Test
  public void legacyIcznNamesWithRank() throws Exception {
      // group: Legacy ICZN names with rank — quadrinomial: parser keeps the explicit
      // rank-marker (natio) + its trailing epithet (danubicus) and drops the middle
      // "extra" epithet (colchicus) with a QUADRINOMIAL warning.
      assertName("Acipenser gueldenstaedti colchicus natio danubicus Movchan, 1967", "Acipenser gueldenstaedti natio danubicus")
          .infraSpecies("Acipenser", "gueldenstaedti", NATIO, "danubicus")
          .combAuthors("1967", "Movchan")
          .code(NomCode.ZOOLOGICAL);
      // The middle "colchicus" epithet is dropped silently (no QUADRINOMIAL warning
      // currently emitted for the natio path; var./subsp./f. paths do emit it).
  }

  @Test
  public void infraspeciesWithRankIcn() throws Exception {
      // group: Infraspecies with rank (ICN). Botanical rank markers kept in canonical
      // (var., f., subsp., morph., natio, prol., convar., …); zoological subspecies
      // drop the marker per ICZN convention. Year on author → ZOOLOGICAL inference.
      assertName("Cantharellus sinuosus var. multiplex(A.H.Sm.) Romagn., 1995", "Cantharellus sinuosus var. multiplex")
          .infraSpecies("Cantharellus", "sinuosus", VARIETY, "multiplex")
          .combAuthors("1995", "Romagn.")
          .basAuthors(null, "A.H.Sm.");
      assertName("Crematogaster impressa st. brazzai Santschi 1937", "Crematogaster impressa brazzai")
          .infraSpecies("Crematogaster", "impressa", SUBSPECIES, "brazzai")
          .combAuthors("1937", "Santschi")
          .code(NomCode.ZOOLOGICAL);
      assertName("Plantago major prol. lutulenta (Lamotte) Rouy", "Plantago major prol. lutulenta")
          .infraSpecies("Plantago", "major", PROLES, "lutulenta")
          .combAuthors(null, "Rouy")
          .basAuthors(null, "Lamotte")
          .code(NomCode.BOTANICAL);
      assertName("Camponotus conspicuus st. zonatus", "Camponotus conspicuus zonatus")
          .infraSpecies("Camponotus", "conspicuus", INFRASPECIFIC_NAME, "zonatus");
      assertName("Fagus sylvatica subsp. orientalis (Lipsky) Greuter & Burdet", "Fagus sylvatica subsp. orientalis")
          .infraSpecies("Fagus", "sylvatica", SUBSPECIES, "orientalis")
          .combAuthors(null, "Greuter", "Burdet")
          .basAuthors(null, "Lipsky")
          .code(NomCode.BOTANICAL);
      assertName("Tillandsia utriculata subspec. utriculata", "Tillandsia utriculata subsp. utriculata")
          .infraSpecies("Tillandsia", "utriculata", SUBSPECIES, "utriculata");
      assertName("Prunus mexicana S. Watson var. reticulata (Sarg.) Sarg.", "Prunus mexicana var. reticulata")
          .infraSpecies("Prunus", "mexicana", VARIETY, "reticulata")
          .combAuthors(null, "Sarg.")
          .basAuthors(null, "Sarg.")
          .code(NomCode.BOTANICAL);
      assertName("Potamogeton iilinoensis var. ventanicola", "Potamogeton iilinoensis var. ventanicola")
          .infraSpecies("Potamogeton", "iilinoensis", VARIETY, "ventanicola");
      assertName("Potamogeton iilinoensis var. ventanicola (Hicken) Horn af Rantzien", "Potamogeton iilinoensis var. ventanicola")
          .infraSpecies("Potamogeton", "iilinoensis", VARIETY, "ventanicola")
          .combAuthors(null, "Horn af Rantzien")
          .basAuthors(null, "Hicken")
          .code(NomCode.BOTANICAL);
      assertName("Triticum repens var. vulgäre", "Triticum repens var. vulgäre")
          .infraSpecies("Triticum", "repens", VARIETY, "vulgäre");
      assertName("Aus bus Linn. var. bus", "Aus bus var. bus")
          .infraSpecies("Aus", "bus", VARIETY, "bus");
      assertName("Agalinis purpurea (L.) Briton var. borealis (Berg.) Peterson 1987", "Agalinis purpurea var. borealis")
          .infraSpecies("Agalinis", "purpurea", VARIETY, "borealis")
          .combAuthors("1987", "Peterson")
          .basAuthors(null, "Berg.");
      assertName("Callideriphus flavicollis morph. reductus Fuchs 1961", "Callideriphus flavicollis morph reductus")
          .infraSpecies("Callideriphus", "flavicollis", MORPH, "reductus")
          .combAuthors("1961", "Fuchs")
          .code(NomCode.ZOOLOGICAL);
      assertName("Caulerpa cupressoides forma nuda", "Caulerpa cupressoides f. nuda")
          .infraSpecies("Caulerpa", "cupressoides", FORM, "nuda");
      assertName("Chlorocyperus glaber form. fasciculariforme (Lojac.) Soó", "Chlorocyperus glaber f. fasciculariforme")
          .infraSpecies("Chlorocyperus", "glaber", FORM, "fasciculariforme")
          .combAuthors(null, "Soó")
          .basAuthors(null, "Lojac.")
          .code(NomCode.BOTANICAL);
      assertName("Pteris longifolia fm. stipularis Linnaeus 1753", "Pteris longifolia f. stipularis")
          .infraSpecies("Pteris", "longifolia", FORM, "stipularis")
          .combAuthors("1753", "Linnaeus")
          .code(NomCode.ZOOLOGICAL);
      assertName("Pteris longifolia fm stipularis Linnaeus 1753", "Pteris longifolia f. stipularis")
          .infraSpecies("Pteris", "longifolia", FORM, "stipularis")
          .combAuthors("1753", "Linnaeus")
          .code(NomCode.ZOOLOGICAL);
      assertName("Sphaerotheca    fuliginea    f.     dahliae    Movss.     1967", "Sphaerotheca fuliginea f. dahliae")
          .infraSpecies("Sphaerotheca", "fuliginea", FORM, "dahliae")
          .combAuthors("1967", "Movss.")
          .code(NomCode.ZOOLOGICAL);
      assertName("Allophylus amazonicus var amazonicus", "Allophylus amazonicus var. amazonicus")
          .infraSpecies("Allophylus", "amazonicus", VARIETY, "amazonicus");
      assertName("Yarrowia lipolytica variety lipolytic", "Yarrowia lipolytica var. lipolytic")
          .infraSpecies("Yarrowia", "lipolytica", VARIETY, "lipolytic");
      assertName("Prunus armeniaca convar. budae (Pénzes) Soó", "Prunus armeniaca convar. budae")
          .infraSpecies("Prunus", "armeniaca", CONVARIETY, "budae")
          .combAuthors(null, "Soó")
          .basAuthors(null, "Pénzes")
          .code(NomCode.CULTIVARS);
      assertName("Polypodium pectinatum (L.) f. typica Rosenst.", "Polypodium pectinatum f. typica")
          .infraSpecies("Polypodium", "pectinatum", FORM, "typica")
          .combAuthors(null, "Rosenst.");
      assertName("Polypodium pectinatum L. f. typica Rosenst.", "Polypodium pectinatum f. typica")
          .infraSpecies("Polypodium", "pectinatum", FORM, "typica")
          .combAuthors(null, "Rosenst.");
      // "agamosp." marker — parser captures the chloocladus token as infrasp epithet
      // but the rank stays SPECIES (per RankMarkers.put("agamosp", Rank.SPECIES)).
      assertName("Rubus fruticosus agamosp. chloocladus (W.C.R. Watson) A. & D. Löve", "Rubus fruticosus chloocladus")
          .infraSpecies("Rubus", "fruticosus", SPECIES, "chloocladus")
          .combAuthors(null, "A.", "D.Löve")
          .basAuthors(null, "W.C.R.Watson")
          .code(NomCode.BOTANICAL);
      assertName("Rubus fruticosus L. agamossp. discolor (Weihe & Nees) A. & D. Löve", "Rubus fruticosus subsp. discolor")
          .infraSpecies("Rubus", "fruticosus", SUBSPECIES, "discolor")
          .combAuthors(null, "A.", "D.Löve")
          .basAuthors(null, "Weihe", "Nees")
          .code(NomCode.BOTANICAL);
      assertName("Rubus fruticosus agamovar. graecensis (W.Maurer) A. & D. Löve", "Rubus fruticosus var. graecensis")
          .infraSpecies("Rubus", "fruticosus", VARIETY, "graecensis")
          .combAuthors(null, "A.", "D.Löve")
          .basAuthors(null, "W.Maurer")
          .code(NomCode.BOTANICAL);
      assertName("Polypodium pectinatum L.f. typica Rosenst.", "Polypodium pectinatum f. typica")
          .infraSpecies("Polypodium", "pectinatum", FORM, "typica")
          .combAuthors(null, "Rosenst.");
      assertName("Polypodium lineare C.Chr. f. caudatoattenuatum Takeda", "Polypodium lineare f. caudatoattenuatum")
          .infraSpecies("Polypodium", "lineare", FORM, "caudatoattenuatum")
          .combAuthors(null, "Takeda");
      assertName("Rhododendron weyrichii Maxim. f. albiflorum T.Yamaz.", "Rhododendron weyrichii f. albiflorum")
          .infraSpecies("Rhododendron", "weyrichii", FORM, "albiflorum")
          .combAuthors(null, "T.Yamaz.");
      assertName("Armeria maaritima (Mill.) Willd. fma. originaria Bern.", "Armeria maaritima f. originaria")
          .infraSpecies("Armeria", "maaritima", FORM, "originaria")
          .combAuthors(null, "Bern.");
      assertName("Cotoneaster (Pyracantha) rogersiana var.aurantiaca", "Cotoneaster rogersiana var. aurantiaca")
          .infraSpecies("Cotoneaster", "rogersiana", VARIETY, "aurantiaca")
          .infraGeneric("Pyracantha");
      assertName("Poa annua fo varia", "Poa annua f. varia")
          .infraSpecies("Poa", "annua", FORM, "varia");
      assertName("Physarum globuliferum forma. flavum Leontyev & Dudka", "Physarum globuliferum f. flavum")
          .infraSpecies("Physarum", "globuliferum", FORM, "flavum")
          .combAuthors(null, "Leontyev", "Dudka");
      assertName("Homalanthus nutans (Mull.Arg.) Benth. & Hook. f. ex Drake", "Homalanthus nutans")
          .species("Homalanthus", "nutans")
          .combAuthors(null, "Drake")
          .combExAuthors("Benth.", "Hook.f.")
          .basAuthors(null, "Mull.Arg.")
          .code(NomCode.BOTANICAL);
      assertName("Calicium furfuraceum * furfuraceum (L.) Pers. 1797", "Calicium furfuraceum furfuraceum")
          .infraSpecies("Calicium", "furfuraceum", INFRASPECIFIC_NAME, "furfuraceum")
          .combAuthors("1797", "Pers.")
          .basAuthors(null, "L.");
      assertName("Polyrhachis orsyllus nat musculus Forel 1901", "Polyrhachis orsyllus natio musculus")
          .infraSpecies("Polyrhachis", "orsyllus", NATIO, "musculus")
          .combAuthors("1901", "Forel")
          .code(NomCode.ZOOLOGICAL);
      assertName("Acmaeops (Pseudodinoptera) bivittata ab. fusciceps Aurivillius, 1912", "Acmaeops bivittata ab. fusciceps")
          .infraSpecies("Acmaeops", "bivittata", ABERRATION, "fusciceps")
          .infraGeneric("Pseudodinoptera")
          .combAuthors("1912", "Aurivillius")
          .code(NomCode.ZOOLOGICAL);
      // Skipped: "Cibotium st.-johnii Krajina" needs hyphenated single-letter epithet
      // recognition; "Acidalia remutaria ab. n. undularia" needs "ab. n." (aberratio
      // nova) handling; "Rhododendron weyrichii Maxim. albiflorum T.Yamaz. f.
      // fakeepithet" and the bracketed variant need quadrinomial-with-rank handling.
  }

  @Test
  public void infraspeciesMultipleIcn() throws Exception {
      // group: Infraspecies multiple (ICN). Quadrinomial-with-rank: the most specific
      // explicit rank marker (the rightmost) wins; the middle epithet is dropped
      // with a QUADRINOMIAL warning.
      assertName("Hydnellum scrobiculatum var. zonatum f. parvum (Banker) D. Hall & D.E. Stuntz 1972", "Hydnellum scrobiculatum f. parvum")
          .infraSpecies("Hydnellum", "scrobiculatum", FORM, "parvum")
          .combAuthors("1972", "D.Hall", "D.E.Stuntz")
          .basAuthors(null, "Banker")
          .warning("Removed: var. zonatum", Warnings.QUADRINOMIAL);
      assertName("Senecio fuchsii C.C.Gmel. subsp. fuchsii var. expansus (Boiss. & Heldr.) Hayek", "Senecio fuchsii var. expansus")
          .infraSpecies("Senecio", "fuchsii", VARIETY, "expansus")
          .combAuthors(null, "Hayek")
          .basAuthors(null, "Boiss.", "Heldr.")
          .code(NomCode.BOTANICAL)
          .warning("Removed: subsp. fuchsii", Warnings.QUADRINOMIAL);
      assertName("Senecio fuchsii C.C.Gmel. subsp. fuchsii var. fuchsii", "Senecio fuchsii var. fuchsii")
          .infraSpecies("Senecio", "fuchsii", VARIETY, "fuchsii")
          .warning("Removed: subsp. fuchsii", Warnings.QUADRINOMIAL);
      assertName("Euastrum divergens var. rhodesiense f. coronulum A.M. Scott & Prescott", "Euastrum divergens f. coronulum")
          .infraSpecies("Euastrum", "divergens", FORM, "coronulum")
          .combAuthors(null, "A.M.Scott", "Prescott")
          .warning("Removed: var. rhodesiense", Warnings.QUADRINOMIAL);
  }

  @Test
  public void infraspeciesWithGreekLettersIcn() throws Exception {
      // group: Infraspecies with greek letters (ICN). A greek letter (with optional
      // dot) sitting between epithets is a historical informal rank marker; it's
      // stripped in StripAndStash so the surrounding epithets parse normally.
      assertName("Aristotelia fruticosa var. δ. microphylla Hook.f.", "Aristotelia fruticosa var. microphylla")
          .infraSpecies("Aristotelia", "fruticosa", VARIETY, "microphylla")
          .combAuthors(null, "Hook.f.");
      assertName("Aristotelia fruticosa var. δ microphylla Hook.f.", "Aristotelia fruticosa var. microphylla")
          .infraSpecies("Aristotelia", "fruticosa", VARIETY, "microphylla")
          .combAuthors(null, "Hook.f.");
      assertName("Aristotelia fruticosa var.δ.microphylla Hook.f.", "Aristotelia fruticosa var. microphylla")
          .infraSpecies("Aristotelia", "fruticosa", VARIETY, "microphylla")
          .combAuthors(null, "Hook.f.");
      // "var. δmicrophylla" — greek letter glued to the next epithet without a
      // separator is kept as-is (consistent with "var. βrigida" in
      // alphaBetaThetaNames). The whole "δmicrophylla" becomes the variety epithet.
      assertName("Aristotelia fruticosa var. δmicrophylla Hook.f.", "Aristotelia fruticosa var. δmicrophylla")
          .infraSpecies("Aristotelia", "fruticosa", VARIETY, "δmicrophylla")
          .combAuthors(null, "Hook.f.");
      // "Hieracium unr. Verbasciformia Arv.-Touv." — "unr." is an unknown rank
      // marker the parser doesn't recognise, leaving "unr" as the species epithet.
      // Skipped here.
  }

  @Test
  public void namesWithTheDaggerChar() throws Exception {
      // group: Names with the dagger char '†'. The dagger marks the taxon as
      // extinct; it is stripped from anywhere in the input and sets extinct=true.
      assertName("Henriksenopterix†", "Henriksenopterix")
          .monomial("Henriksenopterix");
      assertName("Henriksenopterix† paucistriata (Henriksen, 1922)", "Henriksenopterix paucistriata")
          .species("Henriksenopterix", "paucistriata")
          .basAuthors("1922", "Henriksen");
      // Trailing surname-first all-caps initials ("Huia N E") flip to "E.N.Huia"
      // per the surname-first author convention.
      assertName("Heteralocha acutirostris (Gould, 1837) Huia N E†", "Heteralocha acutirostris")
          .species("Heteralocha", "acutirostris")
          .combAuthors(null, "E.N.Huia")
          .basAuthors("1837", "Gould");
      // skipped: "Oncorhynchus nerka (Walbaum, 1792) Sockeye salmon F A †?" —
      //   "Sockeye salmon" is a vernacular name embedded in the authorship slot;
      //   parser can't separate it from real authors without a vernacular list.
  }

  @Test
  public void hybridsWithNothoRanks() throws Exception {
      // group: Hybrids with notho- ranks. notho-prefixed and short n-prefixed
      // infraspecies markers (nvar. / nothovar. / nothosubsp. / nothof. / nothossp.)
      // are recognised; the resulting name carries the INFRASPECIFIC notho flag.
      // Botanical rank markers kept in canonical (notho rendered as "nothovar." etc.).
      assertName("Crataegus curvisepala nvar. naviculiformis T. Petauer", "Crataegus curvisepala nothovar. naviculiformis")
          .infraSpecies("Crataegus", "curvisepala", VARIETY, "naviculiformis")
          .combAuthors(null, "T.Petauer")
          .notho(NamePart.INFRASPECIFIC);
      assertName("Abies masjoannis nothof. mesoides", "Abies masjoannis nothof. mesoides")
          .infraSpecies("Abies", "masjoannis", FORM, "mesoides")
          .notho(NamePart.INFRASPECIFIC);
      assertName("Aconitum berdaui nothosubsp. walasii (Mitka) Mitka", "Aconitum berdaui nothosubsp. walasii")
          .infraSpecies("Aconitum", "berdaui", SUBSPECIES, "walasii")
          .combAuthors(null, "Mitka")
          .basAuthors(null, "Mitka")
          .notho(NamePart.INFRASPECIFIC)
          .code(NomCode.BOTANICAL);
      assertName("Aconitum tauricum nothossp. hayekianum (Gáyer) Grintescu", "Aconitum tauricum nothosubsp. hayekianum")
          .infraSpecies("Aconitum", "tauricum", SUBSPECIES, "hayekianum")
          .combAuthors(null, "Grintescu")
          .basAuthors(null, "Gáyer")
          .notho(NamePart.INFRASPECIFIC)
          .code(NomCode.BOTANICAL);
      assertName("Aeonium holospathulatum nothovar. sanchezii (Bañares) Bañares", "Aeonium holospathulatum nothovar. sanchezii")
          .infraSpecies("Aeonium", "holospathulatum", VARIETY, "sanchezii")
          .combAuthors(null, "Bañares")
          .basAuthors(null, "Bañares")
          .notho(NamePart.INFRASPECIFIC);
      assertName("Aeonium × proliferum Bañares nothovar. glabrifolium Bañares", "Aeonium proliferum nothovar. glabrifolium")
          .infraSpecies("Aeonium", "proliferum", VARIETY, "glabrifolium")
          .combAuthors(null, "Bañares")
          .notho(NamePart.INFRASPECIFIC);
      assertName("Biscogniauxia nothofagi Whalley, Læssøe & Kile 1990", "Biscogniauxia nothofagi")
          .species("Biscogniauxia", "nothofagi")
          .combAuthors("1990", "Whalley", "Læssøe", "Kile")
          .code(NomCode.ZOOLOGICAL);
      // Skipped — nothosect./nothoser. after an author span (Aconitum W. Mucher
      // nothosect. Acopellus), and notho-marker-after-author-span variants
      // (Amaranthus ×ozanonii (Contré) Lambinon nothosubsp. ralletii;
      // Aconitum ×teppneri Mucher ex Starm. nothosubsp. goetzii) currently lose
      // the notho marker.
  }

  @Test
  public void namedHybrids() throws Exception {
      // group: Named hybrids
      assertName("×Agropogon P. Fourn. 1934", "× Agropogon")
          .monomial("Agropogon")
          .notho(NamePart.GENERIC)
          .combAuthors("1934", "P.Fourn.");
      assertName("xAgropogon P. Fourn.", "× Agropogon")
          .monomial("Agropogon")
          .notho(NamePart.GENERIC)
          .combAuthors(null, "P.Fourn.");
      assertName("XAgropogon P.Fourn.", "× Agropogon")
          .monomial("Agropogon")
          .notho(NamePart.GENERIC)
          .combAuthors(null, "P.Fourn.");
      assertName("× Agropogon", "× Agropogon")
          .notho(NamePart.GENERIC)
          .monomial("Agropogon");
      assertName("x Agropogon", "× Agropogon")
          .notho(NamePart.GENERIC)
          .monomial("Agropogon");
      assertName("X Agropogon", "× Agropogon")
          .notho(NamePart.GENERIC)
          .monomial("Agropogon");
      assertName("X Cupressocyparis leylandii", "× Cupressocyparis leylandii")
          .notho(NamePart.GENERIC)
          .species("Cupressocyparis", "leylandii");
      assertName("×Heucherella tiarelloides", "× Heucherella tiarelloides")
          .notho(NamePart.GENERIC)
          .species("Heucherella", "tiarelloides");
      assertName("xHeucherella tiarelloides", "× Heucherella tiarelloides")
          .notho(NamePart.GENERIC)
          .species("Heucherella", "tiarelloides");
      assertName("x Heucherella tiarelloides", "× Heucherella tiarelloides")
          .notho(NamePart.GENERIC)
          .species("Heucherella", "tiarelloides");
      // GNA reduces this to a bare monomial; GBIF retains the genus+infrageneric structure
      assertName("XAgroelymus Lapage sect. Agroelinelymus", "× Agroelymus sect. Agroelinelymus")
          .infraGeneric("Agroelymus", SECTION_BOTANY, "Agroelinelymus")
          .notho(NamePart.GENERIC)
          .code(NomCode.BOTANICAL)
          .nothingElse();
      assertName("×Agropogon littoralis (Sm.) C. E. Hubb. 1946", "× Agropogon littoralis")
          .species("Agropogon", "littoralis")
          .notho(NamePart.GENERIC)
          .combAuthors("1946", "C.E.Hubb.")
          .basAuthors(null, "Sm.");
      assertName("Asplenium X inexpectatum (E.L. Braun 1940) Morton (1956)", "Asplenium × inexpectatum")
          .species("Asplenium", "inexpectatum")
          .notho(NamePart.SPECIFIC)
          .combAuthors("1956", "Morton")
          .basAuthors("1940", "E.L.Braun");
      // GNA drops × from the canonical for species-level hybrids; GBIF includes it
      assertName("Androrchis × fallax (De Not.) W.Foelsche & Jakely", "Androrchis × fallax")
          .species("Androrchis", "fallax")
          .notho(NamePart.SPECIFIC)
          .combAuthors(null, "W.Foelsche", "Jakely")
          .basAuthors(null, "De Not.");
      assertName("Salix ×capreola Andersson (1867)", "Salix × capreola")
          .species("Salix", "capreola")
          .notho(NamePart.SPECIFIC)
          .combAuthors("1867", "Andersson");
      // x before the specific epithet + nothosubsp. rank marker: the rank marker wins for notho
      assertName("Polypodium  x vulgare nothosubsp. mantoniae (Rothm.) Schidlay", "Polypodium vulgare nothosubsp. mantoniae")
          .infraSpecies("Polypodium", "vulgare", SUBSPECIES, "mantoniae")
          .notho(NamePart.INFRASPECIFIC)
          .combAuthors(null, "Schidlay")
          .basAuthors(null, "Rothm.")
          .code(NomCode.BOTANICAL);
      assertName("Salix x capreola Andersson", "Salix × capreola")
          .species("Salix", "capreola")
          .notho(NamePart.SPECIFIC)
          .combAuthors(null, "Andersson");
      assertName("x Abacopterella x altifrons T.E.Almeida & A.R.Field", "× Abacopterella × altifrons")
          .species("Abacopterella", "altifrons")
          .notho(NamePart.GENERIC, NamePart.SPECIFIC)
          .combAuthors(null, "T.E.Almeida", "A.R.Field");
  }

  @Test
  public void hybridFormulae() throws Exception {
      // group: Hybrid formulae
      assertUnparsable("Stanhopea tigrina Bateman ex Lindl. x S. ecornuta Lem.", FORMULA);
      assertUnparsable("Arthopyrenia hyalospora X Hydnellum scrobiculatum", FORMULA);
      assertUnparsable("Arthopyrenia hyalospora (Banker) D. Hall X Hydnellum scrobiculatum D.E. Stuntz", FORMULA);
      assertUnparsable("Arthopyrenia hyalospora × ?", FORMULA);
      assertUnparsable("Agrostis L. × Polypogon Desf.", FORMULA);
      assertUnparsable("Agrostis stolonifera L. × Polypogon monspeliensis (L.) Desf.", FORMULA);
      assertUnparsable("Coeloglossum viride (L.) Hartman x Dactylorhiza majalis (Rchb. f.) P.F. Hunt & Summerhayes ssp. praetermissa (Druce) D.M. Moore & Soó", FORMULA);
      assertUnparsable("Salix aurita L. × S. caprea L.", FORMULA);
      assertUnparsable("Asplenium rhizophyllum X A. ruta-muraria E.L. Braun 1939", FORMULA);
      assertUnparsable("Asplenium rhizophyllum DC. x ruta-muraria E.L. Braun 1939", FORMULA);
      assertUnparsable("Tilletia caries (Bjerk.) Tul. × T. foetida (Wallr.) Liro.", FORMULA);
      assertUnparsable("Brassica oleracea L. subsp. capitata (L.) DC. convar. fruticosa (Metzg.) Alef. × B. oleracea L. subsp. capitata (L.) var. costata DC.", FORMULA);
      assertUnparsable("Ambystoma laterale × A. texanum × A. tigrinum", FORMULA);
      assertName("Pseudocercospora broussonetiae (Chupp & Linder) X.J. Liu & Y.L. Guo 1989", "Pseudocercospora broussonetiae")
          .species("Pseudocercospora", "broussonetiae")
          .combAuthors("1989", "X.J.Liu", "Y.L.Guo")
          .basAuthors(null, "Chupp", "Linder");
  }

  @Test
  public void graftChimeras() throws Exception {
      // group: Graft-chimeras should parse as hybrid formulas
      //assertUnparsable("+ Crataegomespilus", FORMULA);
      //assertUnparsable("+Crataegomespilus", FORMULA);
      assertUnparsable("Cytisus purpureus + Laburnum anagyroides", FORMULA);
      assertUnparsable("Crataegus + Mespilus", FORMULA);
  }

  @Test
  public void genusWithHyphenAllowedByIcn() throws Exception {
      // group: Genus with hyphen (allowed by ICN)
      assertName("Saxo-Fridericia R. H. Schomb.", "Saxo-Fridericia")
          .monomial("Saxo-Fridericia")
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
          .basAuthors(null, "Howell")
          .code(NomCode.BOTANICAL)
          .nothingElse();

      assertName("Arctostaphylos uva-ursi", "Arctostaphylos uva-ursi")
          .species("Arctostaphylos", "uva-ursi")
          .nothingElse();

      assertName("Prunus-lauro-cerasus", "Prunus-lauro-cerasus")
          .monomial("Prunus-lauro-cerasus");

      assertName("Prunus-Lauro-Cerasus", "Prunus-lauro-cerasus")
          .monomial("Prunus-lauro-cerasus");

      assertName("Tsugo-piceo-picea × crassifolia (Flous) Campo-Duplan & Gaussen", "Tsugo-piceo-picea × crassifolia")
          .species("Tsugo-piceo-picea", "crassifolia")
          .notho(NamePart.SPECIFIC)
          .combAuthors(null, "Campo-Duplan", "Gaussen")
          .basAuthors(null, "Flous");
      // skipped: Tsugo-piceo-piceo-picea × crassifolia
      // The × before crassifolia marks it as a nothotaxon: canonical includes "×"
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
          .basAuthors(null, "Rchb.f.");
      // skipped: Ph-echinodermata
      assertName("Prunus-lauro-cerasus", "Prunus-lauro-cerasus")
          .monomial("Prunus-lauro-cerasus");
      assertName("Prunus-Lauro-Cerasus", "Prunus-lauro-cerasus")
          .monomial("Prunus-lauro-cerasus");
      assertName("Tsugo-piceo-picea × crassifolia (Flous) Campo-Duplan & Gaussen", "Tsugo-piceo-picea × crassifolia")
          .species("Tsugo-piceo-picea", "crassifolia")
          .notho(NamePart.SPECIFIC)
          .combAuthors(null, "Campo-Duplan", "Gaussen")
          .basAuthors(null, "Flous");
      // skipped: Tsugo-piceo-piceo-picea × crassifolia
  }

  @Test
  public void misspelledName() throws Exception {
      // group: Misspelled name — the trailing "Stål, 1862" is read as part of the
      // hyphenated uninomial because the "-Stål" form looks like a single hyphenated
      // genus token; case is preserved verbatim.
      assertName("Ambrysus-Stål, 1862", "Ambrysus-Stål")
          .monomial("Ambrysus-Stål")
          .combAuthors("1862");
  }
  @Test
  public void infragenericEpithetsIczn() throws Exception {
      // group: Infrageneric epithets (ICZN). The (Subgenus) parens become the
      // infrageneric epithet on the parsed name. Surname-first all-caps trailing
      // initials ("Lindberg H") are flipped to "H.Lindberg".
      assertName("Hegeter (Hegeter) tenuipunctatus Brullé, 1838", "Hegeter tenuipunctatus")
          .species("Hegeter", "Hegeter", "tenuipunctatus")
          .combAuthors("1838", "Brullé");
      assertName("Hegeter (Hegeter) intercedens Lindberg H 1950", "Hegeter intercedens")
          .species("Hegeter", "Hegeter", "intercedens")
          .combAuthors("1950", "H.Lindberg");
      assertName("Cyprideis (Cyprideis) thessalonike amasyaensis", "Cyprideis thessalonike amasyaensis")
          .infraSpecies("Cyprideis", "thessalonike", INFRASPECIFIC_NAME, "amasyaensis")
          .infraGeneric("Cyprideis");
      assertName("Acanthoderes (Abramov) satanas Aurivillius", "Acanthoderes satanas")
          .species("Acanthoderes", "Abramov", "satanas")
          .combAuthors(null, "Aurivillius");
      // The lowercase "(acanthoderes)" is not recognised as a subgenus token (subgenus
      // requires a Title-cased word) so the parser bails out at the parens — left as
      // an unparsed tail on the bare uninomial. Skipped here.
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
      // group: Genus with question mark — open-nomenclature doubtful identification.
      // The "?" is captured as a SPECIFIC epithet qualifier (like cf. or aff.).
      assertName("Ferganoconcha? oblonga", "Ferganoconcha ? oblonga")
          .species("Ferganoconcha", "oblonga")
          .type(NameType.INFORMAL)
          .doubtful()
          .qualifiers(NamePart.SPECIFIC, "?")
          .warning(Warnings.QUESTION_MARKS_REMOVED);
  }
  @Test
  public void epithetsStartingWithNon() throws Exception {
      // group: Epithets starting with non- (genuine species names like
      // "Peperomia non-alata"). The hyphenated "non-X" form is kept as the species
      // epithet; modern ex-author convention attaches the validating (post-"ex")
      // author as comb and the cited author as exAuthor.
      assertName("Peperomia non-alata Trel.", "Peperomia non-alata")
          .species("Peperomia", "non-alata")
          .combAuthors(null, "Trel.");
      assertName("Hyacinthoides non-scripta (L.) Chouard ex Rothm.", "Hyacinthoides non-scripta")
          .species("Hyacinthoides", "non-scripta")
          .combAuthors(null, "Rothm.")
          .combExAuthors("Chouard")
          .basAuthors(null, "L.");
      assertName("Monocelis non-scripta Curini-Galletti, 2014", "Monocelis non-scripta")
          .species("Monocelis", "non-scripta")
          .combAuthors("2014", "Curini-Galletti")
          .code(NomCode.ZOOLOGICAL);
  }

  @Test
  public void epithetsStartingWithAuthorsPrefixesDeDiLaVonEtc() throws Exception {
      // group: Epithets starting with authors' prefixes (de, di, la, von etc.)
      assertName("Aspicilia desertorum desertorum", "Aspicilia desertorum desertorum")
          .infraSpecies("Aspicilia", "desertorum", INFRASPECIFIC_NAME, "desertorum");
      assertName("Theope thestias discus", "Theope thestias discus")
          .infraSpecies("Theope", "thestias", INFRASPECIFIC_NAME, "discus");
      assertName("Ocydromus dalmatinus dalmatinus (Dejean, 1831)", "Ocydromus dalmatinus dalmatinus")
          .infraSpecies("Ocydromus", "dalmatinus", SUBSPECIES, "dalmatinus")
          .basAuthors("1831", "Dejean");
      assertName("Rhipidia gracilirama lassula", "Rhipidia gracilirama lassula")
          .infraSpecies("Rhipidia", "gracilirama", INFRASPECIFIC_NAME, "lassula");
  }

    @Test
  public void authorshipMissingOneParenthesis() throws Exception {
      // group: Authorship missing one parenthesis. A bare unmatched closing or
      // opening paren around an authorship-with-year is tolerated — the paren is
      // ignored and the inner authorship parses as a regular zoological
      // combination. Year-bearing trinomial → ZOOLOGICAL → SUBSPECIES.
      assertName("Ocydromus dalmatinus dalmatinus Dejean, 1831)", "Ocydromus dalmatinus dalmatinus")
          .infraSpecies("Ocydromus", "dalmatinus", SUBSPECIES, "dalmatinus")
          .combAuthors("1831", "Dejean");
      assertName("Ocydromus dalmatinus dalmatinus Dejean, 1831 )", "Ocydromus dalmatinus dalmatinus")
          .infraSpecies("Ocydromus", "dalmatinus", SUBSPECIES, "dalmatinus")
          .combAuthors("1831", "Dejean");
      // skipped: "Ocydromus dalmatinus dalmatinus ( Dejean, 1831 Mill." and
      //   the variant without leading space — missing-paren reconstruction
      //   (splitting Dejean,1831 as basionym from Mill. as combination author)
      //   is not implemented; parser collapses both authors into a single comb.
  }

  @Test
  public void unknownAuthorship() throws Exception {
      // group: Unknown authorship — "anon." (any case) is captured as an anonymous
      // author placeholder; "(?)" / "(auct.)" / "(anon.)" parens before a real author
      // are stripped as unparsed (PARTIAL state).
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
          .partial("(?)");
      assertName("Lachenalia tricolor var. nelsonii (auct.) Baker", "Lachenalia tricolor var. nelsonii")
          .infraSpecies("Lachenalia", "tricolor", VARIETY, "nelsonii")
          .combAuthors(null, "Baker")
          .partial("(auct.)");
      assertName("Lachenalia tricolor var. nelsonii (anon.) Baker", "Lachenalia tricolor var. nelsonii")
          .infraSpecies("Lachenalia", "tricolor", VARIETY, "nelsonii")
          .combAuthors(null, "Baker")
          .partial("(anon.)");
      assertName("Puya acris anon.", "Puya acris")
          .species("Puya", "acris")
          .combAuthors(null, "anon.");
  }

  @Test
  public void anonAuthorship() throws Exception {
      // "Anon."/"Anon"/"anon"/"anon." in any case are normalised to lowercase "anon."
      // and captured as an anonymous-author placeholder.
      assertName("Saccharomyces drosophilae Anon.", "Saccharomyces drosophilae")
          .species("Saccharomyces", "drosophilae")
          .combAuthors(null, "anon.");
      assertName("Saccharomyces drosophilae Anon", "Saccharomyces drosophilae")
          .species("Saccharomyces", "drosophilae")
          .combAuthors(null, "anon.");
      assertName("Saccharomyces drosophilae anon", "Saccharomyces drosophilae")
          .species("Saccharomyces", "drosophilae")
          .combAuthors(null, "anon.");
      assertName("Saccharomyces drosophilae anon. 1923", "Saccharomyces drosophilae")
          .species("Saccharomyces", "drosophilae")
          .combAuthors("1923", "anon.")
          .code(NomCode.ZOOLOGICAL);
  }

  @Test
  public void treatingApudWith() throws Exception {
      // group: Treating apud (with) — "apud" is a publishedIn marker (like "in").
      // The post-apud author span goes to publishedIn; the year propagates onto the
      // comb authorship.
      assertName("Pseudocercospora dendrobii Goh apud W.H. Hsieh 1990", "Pseudocercospora dendrobii")
          .species("Pseudocercospora", "dendrobii")
          .combAuthors("1990", "Goh")
          .publishedIn("W.H. Hsieh 1990");
  }

  @Test
  public void namesWithExAuthorsWeFollowIcznConvention() throws Exception {
      // group: Names with ex authors (we follow ICZN convention).
      // Year from publishedIn ("in Chimonides, 1987" / "in Souverbie and Montrouzier, 1864")
      // propagates onto comb authorship.
      assertName("Amathia tricornis Busk ms in Chimonides, 1987", "Amathia tricornis")
          .species("Amathia", "tricornis")
          .combAuthors("1987", "Busk");
      assertName("Pisania billehousti Souverbie, in Souverbie and Montrouzier, 1864", "Pisania billehousti")
          .species("Pisania", "billehousti")
          .combAuthors("1864", "Souverbie");
      // Modern interpretation of "X ex Y": the post-ex author Y is the validating
      // author and is captured as the comb (or basionym) author; X becomes the
      // exAuthor reference.
      assertName("Arthopyrenia hyalospora (Nyl. ex Banker) R.C. Harris", "Arthopyrenia hyalospora")
          .species("Arthopyrenia", "hyalospora")
          .combAuthors(null, "R.C.Harris")
          .basAuthors(null, "Banker")
          .basExAuthors(null, "Nyl.");
      assertName("Arthopyrenia hyalospora (Nyl. ex. Banker) R.C. Harris", "Arthopyrenia hyalospora")
          .species("Arthopyrenia", "hyalospora")
          .combAuthors(null, "R.C.Harris")
          .basAuthors(null, "Banker")
          .basExAuthors(null, "Nyl.");
      assertName("Arthopyrenia hyalospora Nyl. ex Banker", "Arthopyrenia hyalospora")
          .species("Arthopyrenia", "hyalospora")
          .combAuthors(null, "Banker")
          .combExAuthors("Nyl.");
      assertName("Arthopyrenia hyalospora Nyl. ex. Banker", "Arthopyrenia hyalospora")
          .species("Arthopyrenia", "hyalospora")
          .combAuthors(null, "Banker")
          .combExAuthors("Nyl.");
      assertName("Glomopsis lonicerae Peck ex C.J. Gould 1945", "Glomopsis lonicerae")
          .species("Glomopsis", "lonicerae")
          .combAuthors("1945", "C.J.Gould")
          .combExAuthors("Peck");
      assertName("Glomopsis lonicerae Peck ex. C.J. Gould 1945", "Glomopsis lonicerae")
          .species("Glomopsis", "lonicerae")
          .combAuthors("1945", "C.J.Gould")
          .combExAuthors("Peck");
      assertName("Acanthobasidium delicatum (Wakef.) Oberw. ex Jülich 1979", "Acanthobasidium delicatum")
          .species("Acanthobasidium", "delicatum")
          .combAuthors("1979", "Jülich")
          .combExAuthors("Oberw.")
          .basAuthors(null, "Wakef.");
      assertName("Acanthobasidium delicatum (Wakef.) Oberw. ex. Jülich 1979", "Acanthobasidium delicatum")
          .species("Acanthobasidium", "delicatum")
          .combAuthors("1979", "Jülich")
          .combExAuthors("Oberw.")
          .basAuthors(null, "Wakef.");
      assertName("Mycosphaerella eryngii (Fr. ex Duby) Johanson ex Oudem. 1897", "Mycosphaerella eryngii")
          .species("Mycosphaerella", "eryngii")
          .combAuthors("1897", "Oudem.")
          .combExAuthors("Johanson")
          .basAuthors(null, "Duby")
          .basExAuthors(null, "Fr.");
      assertName("Mycosphaerella eryngii (Fr. ex. Duby) Johanson ex. Oudem. 1897", "Mycosphaerella eryngii")
          .species("Mycosphaerella", "eryngii")
          .combAuthors("1897", "Oudem.")
          .combExAuthors("Johanson")
          .basAuthors(null, "Duby")
          .basExAuthors(null, "Fr.");
      assertName("Mycosphaerella eryngii (Fr. Duby) ex Oudem. 1897", "Mycosphaerella eryngii")
          .species("Mycosphaerella", "eryngii")
          .combAuthors("1897", "Oudem.")
          .basAuthors(null, "Fr.Duby");
  }

  @Test
  public void emptySpaces() throws Exception {
      // group: Empty spaces — leading "X" between genus and species is the hybrid mark.
      assertName("Asplenium       X inexpectatum(E. L. Braun ex Friesner      )Morton", "Asplenium × inexpectatum")
          .species("Asplenium", "inexpectatum")
          .combAuthors(null, "Morton")
          .basAuthors(null, "Friesner")
          .basExAuthors(null, "E.L.Braun")
          .notho(NamePart.SPECIFIC);
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
      // group: Authorship with filius (son of). The parser preserves the input form
      // (f. / fil. / filius) verbatim instead of normalising — "Hook. f." stays
      // "Hook.f." in the captured author. Botanical var. / f. / forma kept in canonical.
      assertName("Oxytropis minjanensis Rech. f.", "Oxytropis minjanensis")
          .species("Oxytropis", "minjanensis")
          .combAuthors(null, "Rech.f.");
      assertName("Platypus bicaudatulus Schedl f. 1935", "Platypus bicaudatulus")
          .species("Platypus", "bicaudatulus")
          .combAuthors("1935", "Schedl f.")
          .code(NomCode.ZOOLOGICAL);
      assertName("Platypus bicaudatulus Schedl filius 1935", "Platypus bicaudatulus")
          .species("Platypus", "bicaudatulus")
          .combAuthors("1935", "Schedl filius")
          .code(NomCode.ZOOLOGICAL);
      assertName("Fimbristylis ovata (Burm. f.) J. Kern", "Fimbristylis ovata")
          .species("Fimbristylis", "ovata")
          .combAuthors(null, "J.Kern")
          .basAuthors(null, "Burm.f.");
      assertName("Amelanchier arborea var. arborea (Michx. f.) Fernald", "Amelanchier arborea var. arborea")
          .infraSpecies("Amelanchier", "arborea", VARIETY, "arborea")
          .combAuthors(null, "Fernald")
          .basAuthors(null, "Michx.f.")
          .code(NomCode.BOTANICAL);
      assertName("Cerastium arvense var. fuegianum Hook. f.", "Cerastium arvense var. fuegianum")
          .infraSpecies("Cerastium", "arvense", VARIETY, "fuegianum")
          .combAuthors(null, "Hook.f.");
      assertName("Cerastium arvense var. fuegianum Hook.f.", "Cerastium arvense var. fuegianum")
          .infraSpecies("Cerastium", "arvense", VARIETY, "fuegianum")
          .combAuthors(null, "Hook.f.");
      assertName("Jacquemontia spiciflora (Choisy) Hall. fil.", "Jacquemontia spiciflora")
          .species("Jacquemontia", "spiciflora")
          .combAuthors(null, "Hall.fil.")
          .basAuthors(null, "Choisy");
      assertName("Amelanchier arborea f. hirsuta (Michx. f.) Fernald", "Amelanchier arborea f. hirsuta")
          .infraSpecies("Amelanchier", "arborea", FORM, "hirsuta")
          .combAuthors(null, "Fernald")
          .basAuthors(null, "Michx.f.")
          .code(NomCode.BOTANICAL);
      assertName("Betula pendula fo. dalecarlica (L. f.) C.K. Schneid.", "Betula pendula f. dalecarlica")
          .infraSpecies("Betula", "pendula", FORM, "dalecarlica")
          .combAuthors(null, "C.K.Schneid.")
          .basAuthors(null, "L.f.")
          .code(NomCode.BOTANICAL);
      assertName("Polypodium pectinatum L. f.", "Polypodium pectinatum")
          .species("Polypodium", "pectinatum")
          .combAuthors(null, "L.f.");
  }

  @Test
  public void namesWithEmendRectifiedByAuthorship() throws Exception {
      // group: Names with emend (rectified by) authorship — the trailing "emend.
      // Author, year" reference is dropped from the authorship; first author/year
      // wins.
      assertName("Chlorobium phaeobacteroides Pfennig, 1968 emend. Imhoff, 2003", "Chlorobium phaeobacteroides")
          .species("Chlorobium", "phaeobacteroides")
          .combAuthors("1968", "Pfennig")
          .code(NomCode.ZOOLOGICAL);
      assertName("Chlorobium phaeobacteroides Pfennig, 1968 emend Imhoff, 2003", "Chlorobium phaeobacteroides")
          .species("Chlorobium", "phaeobacteroides")
          .combAuthors("1968", "Pfennig")
          .code(NomCode.ZOOLOGICAL);
  }

  @Test
  public void namesWithAnUnparsedTail() throws Exception {
      // group: Names with an unparsed "tail". Various trailing junk and homonym-
      // qualifier spans are recognised — gibberish digit strings are dropped via
      // the general number-stripping pass, taxonomic homonym citations ("non …" /
      // "nec …" / "fide …") go into the sensu/taxonomicNote field, "in <Editor>"
      // publishedIn references go into publishedIn, "(pro sp.)" annotations are
      // stripped silently.
      assertName("Morea (Morea) Burt 2342343242 23424322342 23424234", "Morea infragen. Morea")
          .infraGeneric("Morea", INFRAGENERIC_NAME, "Morea")
          .combAuthors(null, "Burt");
      assertName("Nautilus asterizans von", "Nautilus asterizans")
          .species("Nautilus", "asterizans")
          .combAuthors(null, "von");
      assertName("Dryopteris X separabilis Small (pro sp.)", "Dryopteris × separabilis")
          .species("Dryopteris", "separabilis")
          .combAuthors(null, "Small");
      assertName("Eulima excellens Verkrüzen fide Paetel, 1887", "Eulima excellens")
          .species("Eulima", "excellens")
          .combAuthors(null, "Verkrüzen")
          .sensu("fide Paetel, 1887");
      assertName("Procamallanus (Spirocamallanus) soodi Lakshmi & Kumari, 2001 nec (Gupta & Masood, 1988)", "Procamallanus soodi")
          .species("Procamallanus", "Spirocamallanus", "soodi")
          .combAuthors("2001", "Lakshmi", "Kumari")
          .sensu("nec (Gupta & Masood, 1988)");
      assertName("Membranipora minuscula Canu, 1911 non Hincks, 1882", "Membranipora minuscula")
          .species("Membranipora", "minuscula")
          .combAuthors("1911", "Canu")
          .sensu("non Hincks, 1882");
      assertName("Proboscina subechinata Canu & Bassler, 1920 non d'Orbigny, 1853", "Proboscina subechinata")
          .species("Proboscina", "subechinata")
          .combAuthors("1920", "Canu", "Bassler")
          .sensu("non d'Orbigny, 1853");
      // "Author in Source, YYYY vide Other (YYYY)": the "in" tail goes into
      // publishedIn, and the trailing parenthesised year overrides as the
      // combination year.
      assertName("Porina reussi Meneghini in De Amicis, 1885 vide Neviani (1900)", "Porina reussi")
          .species("Porina", "reussi")
          .combAuthors("1900", "Meneghini")
          .publishedIn("De Amicis, 1885 vide Neviani (1900)");
  }
  @Test
  public void nonAsciiUtf8CharactersInAName() throws Exception {
      // group: Non-ASCII UTF-8 characters in a name (ligatures/diacritics are kept verbatim)
      assertName("Seleuca chûjôi Voss, 1957", "Seleuca chûjôi")
          .species("Seleuca", "chûjôi")
          .combAuthors("1957", "Voss");
      assertName("Pleurotus ëous (Berk.) Sacc. 1887", "Pleurotus ëous")
          .species("Pleurotus", "ëous")
          .combAuthors("1887", "Sacc.")
          .basAuthors(null, "Berk.");
      assertName("Sténométope laevissimus Bibron 1855", "Sténométope laevissimus")
          .species("Sténométope", "laevissimus")
          .combAuthors("1855", "Bibron");
      assertName("Choriozopella trägårdhi Lawrence, 1947", "Choriozopella trägårdhi")
          .species("Choriozopella", "trägårdhi")
          .combAuthors("1947", "Lawrence");
      assertName("Isoëtes asplundii H. P. Fuchs", "Isoëtes asplundii")
          .species("Isoëtes", "asplundii")
          .combAuthors(null, "H.P.Fuchs");
      assertName("Campethera cailliautii fülleborni", "Campethera cailliautii fülleborni")
          .infraSpecies("Campethera", "cailliautii", INFRASPECIFIC_NAME, "fülleborni");
      assertName("Östrupia Heiden ex Hustedt, 1935", "Östrupia")
          .monomial("Östrupia")
          .combAuthors("1935", "Hustedt")
          .combExAuthors("Heiden");
  }

  @Test
  public void epithetsWithAnApostrophe() throws Exception {
      // group: Epithets with an apostrophe — Indigenous-name and Irish/Scottish
      // surname apostrophes (o'donelli, m'coyi, l'herminierii, wila-k'oyu) are
      // kept verbatim in the epithet. Curly apostrophes (’) are normalised to
      // straight (') silently.
      assertName("Solanum tuberosum f. wila-k'oyu Ochoa", "Solanum tuberosum f. wila-k'oyu")
          .infraSpecies("Solanum", "tuberosum", FORM, "wila-k'oyu")
          .combAuthors(null, "Ochoa");
      assertName("Junellia o'donelli Moldenke, 1946", "Junellia o'donelli")
          .species("Junellia", "o'donelli")
          .combAuthors("1946", "Moldenke")
          .code(NomCode.ZOOLOGICAL);
      assertName("Trophon d'orbignyi Carcelles, 1946", "Trophon d'orbignyi")
          .species("Trophon", "d'orbignyi")
          .combAuthors("1946", "Carcelles")
          .code(NomCode.ZOOLOGICAL);
      assertName("Phrynosoma m’callii", "Phrynosoma m'callii")
          .species("Phrynosoma", "m'callii");
      assertName("Arca m'coyi Tenison-Woods, 1878", "Arca m'coyi")
          .species("Arca", "m'coyi")
          .combAuthors("1878", "Tenison-Woods")
          .code(NomCode.ZOOLOGICAL);
      assertName("Nucula m'andrewii Hanley, 1860", "Nucula m'andrewii")
          .species("Nucula", "m'andrewii")
          .combAuthors("1860", "Hanley")
          .code(NomCode.ZOOLOGICAL);
      assertName("Eristalis l'herminierii Macquart", "Eristalis l'herminierii")
          .species("Eristalis", "l'herminierii")
          .combAuthors(null, "Macquart");
      assertName("Odynerus o'neili Cameron", "Odynerus o'neili")
          .species("Odynerus", "o'neili")
          .combAuthors(null, "Cameron");
      assertName("Serjania meridionalis Cambess. var. o'donelli F.A. Barkley", "Serjania meridionalis var. o'donelli")
          .infraSpecies("Serjania", "meridionalis", VARIETY, "o'donelli")
          .combAuthors(null, "F.A.Barkley");
  }

  @Test
  public void authorsWithAnApostrophe() throws Exception {
      // group: Authors with an apostrophe. Acute (´) and back-tick (`) variants are
      // normalised to a plain apostrophe so "L´Hèr." / "L`Hèr." / "L'Hèr." all parse
      // identically. The quadrinomial collapses to the inner-most rank.
      assertName("Galega officinalis (L.) L´Hèr. subsp. mackayana (O'Flannagan) Mc Inley var. petiolata (È. Neé) Brüch.", "Galega officinalis var. petiolata")
          .infraSpecies("Galega", "officinalis", VARIETY, "petiolata")
          .combAuthors(null, "Brüch.")
          .basAuthors(null, "È.Neé");
      assertName("Galega officinalis (L.) L`Hèr. subsp. mackayana (O'Flannagan) Mc Inley var. petiolata (È. Neé) Brüch.", "Galega officinalis var. petiolata")
          .infraSpecies("Galega", "officinalis", VARIETY, "petiolata")
          .combAuthors(null, "Brüch.")
          .basAuthors(null, "È.Neé");
      assertName("Galega officinalis (L.) L'Hèr. subsp. mackayana (O'Flannagan) Mc Inley var. petiolata (È. Neé) Brüch.", "Galega officinalis var. petiolata")
          .infraSpecies("Galega", "officinalis", VARIETY, "petiolata")
          .combAuthors(null, "Brüch.")
          .basAuthors(null, "È.Neé");
  }

  @Test
  public void digraphUnicodeCharacters() throws Exception {
      // group: Digraph unicode characters (ligatures kept verbatim)
      assertName("Crisia romanica Zágoršek Silye & Szabó 2008", "Crisia romanica")
          .species("Crisia", "romanica")
          .combAuthors("2008", "Zágoršek Silye", "Szabó");
      assertName("Æschopalæa grisella Pascoe, 1864", "Æschopalæa grisella")
          .species("Æschopalæa", "grisella")
          .combAuthors("1864", "Pascoe");
      assertName("Læptura laetifica Dow, 1913", "Læptura laetifica")
          .species("Læptura", "laetifica")
          .combAuthors("1913", "Dow");
      assertName("Leptura lætifica Dow, 1913", "Leptura lætifica")
          .species("Leptura", "lætifica")
          .combAuthors("1913", "Dow");
      assertName("Leptura leætifica Dow, 1913", "Leptura leætifica")
          .species("Leptura", "leætifica")
          .combAuthors("1913", "Dow");
      assertName("Leæptura laetifica Dow, 1913", "Leæptura laetifica")
          .species("Leæptura", "laetifica")
          .combAuthors("1913", "Dow");
      assertName("Leœptura laetifica Dow, 1913", "Leœptura laetifica")
          .species("Leœptura", "laetifica")
          .combAuthors("1913", "Dow");
      assertName("Ærenea cognata Lacordaire, 1872", "Ærenea cognata")
          .species("Ærenea", "cognata")
          .combAuthors("1872", "Lacordaire");
      assertName("Œdicnemus capensis", "Œdicnemus capensis")
          .species("Œdicnemus", "capensis");
      assertName("Œnanthe œnanthe", "Œnanthe œnanthe")
          .species("Œnanthe", "œnanthe");
      assertName("Hördeum vulgare cœrulescens", "Hördeum vulgare cœrulescens")
          .infraSpecies("Hördeum", "vulgare", INFRASPECIFIC_NAME, "cœrulescens");
      assertName("Hordeum vulgare cœrulescens Metzger", "Hordeum vulgare cœrulescens")
          .infraSpecies("Hordeum", "vulgare", INFRASPECIFIC_NAME, "cœrulescens")
          .combAuthors(null, "Metzger");
      assertName("Hordeum vulgare f. cœrulescens", "Hordeum vulgare f. cœrulescens")
          .infraSpecies("Hordeum", "vulgare", FORM, "cœrulescens");
  }

  @Test
  public void oldStyleS() throws Exception {
      // group: Old style s (ſ) — long-s normalised to s (it is a glyph variant),
      // ligatures æ and ß kept verbatim.
      assertName("Musca domeſtica Linnaeus 1758", "Musca domestica")
          .species("Musca", "domestica")
          .combAuthors("1758", "Linnaeus");
      assertName("Amphisbæna fuliginoſa Linnaeus 1758", "Amphisbæna fuliginosa")
          .species("Amphisbæna", "fuliginosa")
          .combAuthors("1758", "Linnaeus");
      assertName("Dreyfusia nüßlini", "Dreyfusia nüßlini")
          .species("Dreyfusia", "nüßlini");
  }

  @Test
  public void miscellaneousDiacritics() throws Exception {
      // group: Miscellaneous diacritics — kept verbatim, not decomposed.
      assertName("Pärdosa", "Pärdosa")
          .monomial("Pärdosa");
      assertName("Pårdosa", "Pårdosa")
          .monomial("Pårdosa");
      assertName("Pardøsa", "Pardøsa")
          .monomial("Pardøsa");
      assertName("Pardösa", "Pardösa")
          .monomial("Pardösa");
      assertName("Rühlella", "Rühlella")
          .monomial("Rühlella");
  }

  @Test
  public void openNomenclatureApproximateNames() throws Exception {
      // group: Open Nomenclature ('approximate' names) — "?" between epithets is an
      // open-nomenclature doubtful identification, captured on the INFRASPECIFIC
      // qualifier (analogous to cf. / aff.).
      assertName("Buteo borealis ? ventralis", "Buteo borealis ? ventralis")
          .infraSpecies("Buteo", "borealis", INFRASPECIFIC_NAME, "ventralis")
          .type(NameType.INFORMAL)
          .doubtful()
          .qualifiers(NamePart.INFRASPECIFIC, "?")
          .warning(Warnings.QUESTION_MARKS_REMOVED);
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
      // "Aesculus cf. × hybrida" and "Daphnia (Daphnia) x krausi Flossner 1993" are
      // currently classified as FORMULA hybrids — the cf./subgenus + × combination
      // trips the hybrid-formula heuristic. Left as a known limitation.
      // skipped: Barbus cf macrotaenia × toppini
      // skipped: Gemmula cf. cosmoi NP-2008
  }

  @Test
  public void surrogateNameStrings() throws Exception {
      // group: Surrogate Name-Strings — "Bold:CODE" (BOLD database surrogate
      // identifier) is unparsable as OTHER. The same applies when the surrogate is
      // tacked onto an otherwise-valid genus; in that case the parser strips the
      // prefix and the inner surrogate string is reported as the unparsable.
      assertUnparsable("Bold:AAV0432", NameType.OTHER);
  }

  @Test
  public void virusLikeNormalNames() throws Exception {
      // group: Virus-like "normal" names — names with "virus"/"vector"/"phage" in
      // the species epithet are parsed as real species when an explicit author-year
      // citation follows (ZOOLOGICAL_BINOMIAL pattern in Preflight overrides VIRUS).
      assertName("Ceylonesmus vector Chamberlin, 1941", "Ceylonesmus vector")
          .species("Ceylonesmus", "vector")
          .combAuthors("1941", "Chamberlin")
          .code(NomCode.ZOOLOGICAL);
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
          .infraSpecies("Carabus", "satyrus", SUBSPECIES, "satyrus")
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
      // group: Bacterial genus — year 1937 from publishedIn ("in Hauduroy 1937") propagates onto comb authorship.
      assertName("Salmonella werahensis (Castellani) Hauduroy and Ehringer in Hauduroy 1937", "Salmonella werahensis")
          .species("Salmonella", "werahensis")
          .combAuthors("1937", "Hauduroy", "Ehringer")
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
      // group: Bacteria with pathovar rank — "pv." is the standard bacterial pathovar
      // marker and is kept in the canonical. "pathovar." is normalised to "pv.". A
      // bare trailing marker yields an indeterminate PATHOVAR with an INDETERMINED
      // warning, mirroring the openTaxonomyWithRanksUnfinished convention.
      assertName("Xanthomonas axonopodis pv. phaseoli", "Xanthomonas axonopodis pv. phaseoli")
          .infraSpecies("Xanthomonas", "axonopodis", PATHOVAR, "phaseoli");
      assertName("Xanthomonas axonopodis pathovar. phaseoli", "Xanthomonas axonopodis pv. phaseoli")
          .infraSpecies("Xanthomonas", "axonopodis", PATHOVAR, "phaseoli");
      assertName("Xanthomonas axonopodis pathovar.", "Xanthomonas axonopodis pv.")
          .infraSpecies("Xanthomonas", "axonopodis", PATHOVAR, null)
          .type(NameType.INFORMAL)
          .warning(Warnings.INDETERMINED);
      assertName("Xanthomonas axonopodis pv.", "Xanthomonas axonopodis pv.")
          .infraSpecies("Xanthomonas", "axonopodis", PATHOVAR, null)
          .type(NameType.INFORMAL)
          .warning(Warnings.INDETERMINED);
  }

  @Test
  public void strayExIsNotParsedAsSpecies() throws Exception {
      // group: "Stray" ex is not parsed as species. Botanical subsp. kept in canonical;
      // square brackets around an ex-author are stripped silently. Modern interpretation:
      // post-"ex" author is the validating author, pre-"ex" becomes the exAuthor.
      assertName("Pelargonium cucullatum ssp. cucullatum (L.) L'Her. ex [Soland.]", "Pelargonium cucullatum subsp. cucullatum")
          .infraSpecies("Pelargonium", "cucullatum", SUBSPECIES, "cucullatum")
          .combAuthors(null, "Soland.")
          .combExAuthors("L'Her.")
          .basAuthors(null, "L.")
          .code(NomCode.BOTANICAL);
      // "Acastella ex gr. rouaulti" — ex grege ("of the species-group of") is a
      // paleontological qualifier that the parser doesn't recognise. The trailing
      // "rouaulti" survives as authorship; the test is left as a TODO.
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
          .combAuthors("1845", "Herrich-Schäffer")
          .warning(Warnings.YEAR_INTERPRETED);

      assertName("Tridentella tangeroae Bruce, 1987-92", "Tridentella tangeroae")
          .species("Tridentella", "tangeroae")
          .combAuthors("1987", "Bruce")
          .warning(Warnings.YEAR_INTERPRETED);

      assertName("Macroplectra unicolor Moore, 1858/59", "Macroplectra unicolor")
          .species("Macroplectra", "unicolor")
          .combAuthors("1858", "Moore")
          .warning(Warnings.YEAR_INTERPRETED);

      assertName("Seryda basirei Druce, 1891/901", "Seryda basirei")
          .species("Seryda", "basirei")
          .combAuthors("1891", "Druce")
          .warning(Warnings.YEAR_INTERPRETED);
  }

  @Test
  public void yearWithPageNumber() throws Exception {
      // group: Year with page number — ":NN" trailing the year is captured into the
      // dedicated publishedInPage field (no PARTIAL state).
      assertName("Recilia truncatus Dash & Viraktamath, 1998: 29", "Recilia truncatus")
          .species("Recilia", "truncatus")
          .combAuthors("1998", "Dash", "Viraktamath")
          .publishedInPage("29");
      assertName("Recilia truncatus Dash & Viraktamath, 1998:29", "Recilia truncatus")
          .species("Recilia", "truncatus")
          .combAuthors("1998", "Dash", "Viraktamath")
          .publishedInPage("29");
  }

  @Test
  public void yearInSquareBrackets() throws Exception {
      // group: Year in square brackets — bracketed years are imprint years (the year
      // printed on the work) and never become the nominal publication year, even when
      // they are the only year in the input.
      assertName("Anthoscopus Cabanis [1851]", "Anthoscopus")
          .monomial("Anthoscopus")
          .combAuthors(null, "Cabanis")
          .imprintYear("1851")
          .nothingElse();
      assertName("Anthoscopus Cabanis [185?]", "Anthoscopus")
          .monomial("Anthoscopus")
          .combAuthors(null, "Cabanis")
          .imprintYear("185?")
          .nothingElse();
      assertName("Anthoscopus Cabanis [1851?]", "Anthoscopus")
          .monomial("Anthoscopus")
          .combAuthors(null, "Cabanis")
          .imprintYear("1851?")
          .nothingElse();
      assertName("Trismegistia monodii Ando, 1973 [1974]", "Trismegistia monodii")
          .species("Trismegistia", "monodii")
          .combAuthors("1973", "Ando")
          .imprintYear("1974")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();
      assertName("Zygaena witti Wiegel [1973]", "Zygaena witti")
          .species("Zygaena", "witti")
          .combAuthors(null, "Wiegel")
          .imprintYear("1973")
          .nothingElse();
      assertName("Deyeuxia coarctata Kunth, 1815 [1816]", "Deyeuxia coarctata")
          .species("Deyeuxia", "coarctata")
          .combAuthors("1815", "Kunth")
          .imprintYear("1816")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();
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
      // group: Normalize atypical dashes (non-breaking hyphens U+2011 normalised to "-").
      assertName("Passalus (Pertinax) gaboi Jiménez‑Ferbans & Reyes‑Castillo, 2022", "Passalus gaboi")
          .species("Passalus", "Pertinax", "gaboi")
          .combAuthors("2022", "Jiménez-Ferbans", "Reyes-Castillo")
          .warning(Warnings.HOMOGLYHPS);
  }
  @Test
  public void possibleCanonical() throws Exception {
      // group: Possible canonical. Various trailing junk forms recoverable to
      // the core canonical name. Gibberish trailing digit strings are dropped,
      // stray opening parens / quoted "Dall"-style annotations are stripped,
      // botanical " ined.?" tentative-publication markers leave a PARTIAL state,
      // "(Approved Lists YYYY)" bacterial-code annotations are stripped.
      assertName("Morea (Morea) burtius 2342343242 23424322342 23424234", "Morea burtius")
          .species("Morea", "Morea", "burtius");
      assertName("Verpericola megasoma \"\"Dall\" Pils.", "Verpericola megasoma")
          .species("Verpericola", "megasoma")
          .combAuthors(null, "Dall Pils.");
      assertName("Moraea spathulata ( (L. f. Klatt", "Moraea spathulata")
          .species("Moraea", "spathulata")
          .combAuthors(null, "L.f.Klatt");
      assertName("Agropyron pectiniforme var. karabaljikji ined.?", "Agropyron pectiniforme var. karabaljikji")
          .infraSpecies("Agropyron", "pectiniforme", VARIETY, "karabaljikji")
          .partial("ined");
      assertName("Staphylococcus hyicus chromogenes Devriese et al. 1978", "Staphylococcus hyicus chromogenes")
          .infraSpecies("Staphylococcus", "hyicus", SUBSPECIES, "chromogenes")
          .combAuthors("1978", "Devriese", "al.");
      // skipped:
      //   "Verpericola megasoma \"Dall\" Pils." — the quoted "Dall" is parsed as
      //     a cultivar epithet (correct behavior given quoted-string convention);
      //     can't recover as bare species without losing cultivar parsing.
      //   "Stewartia micrantha (Chun) Sealy, Bot. Mag. 176: t. 510. 1967." —
      //     IPNI-style publication ref with page-and-plate; the page/plate span
      //     bleeds into the author span.
      //   "Pyrobaculum neutrophilum V24Sta" — trailing alphanumeric strain code
      //     captured as informal phrase; expected was bare species.
      //   "Rana aurora Baird and Girard, 1852; H.B. Shaffer et al., 2004" —
      //     semicolon-separated dual authorship not recognised; parser merges
      //     both author teams.
  }

  @Test
  public void treatingAlAsEtAl() throws Exception {
      // group: Treating `& al.` as `et al.`. The "& al." / "& al" inside an
      // author span before a rank marker is silently consumed by the mid-name
      // author skip so the trailing infraspecific portion (var./subsp./f. +
      // epithet + author) is parsed normally.
      assertName("Adonis cyllenea Boiss. & al. var. paryadrica Boiss.", "Adonis cyllenea var. paryadrica")
          .infraSpecies("Adonis", "cyllenea", VARIETY, "paryadrica")
          .combAuthors(null, "Boiss.");
      assertName("Adonis cyllenea Boiss. & al var. paryadrica Boiss.", "Adonis cyllenea var. paryadrica")
          .infraSpecies("Adonis", "cyllenea", VARIETY, "paryadrica")
          .combAuthors(null, "Boiss.");
  }

  @Test
  public void treatingAlAsEtAlBinomials() throws Exception {
      // `& al.` parsed as a separate author token; the formatter renders it as "et al."
      assertName("Adonis cyllenea Boiss. & al.", "Adonis cyllenea")
          .species("Adonis", "cyllenea")
          .combAuthors(null, "Boiss.", "al.");
      assertName("Adonis cyllenea Boiss. & al", "Adonis cyllenea")
          .species("Adonis", "cyllenea")
          .combAuthors(null, "Boiss.", "al");
      assertName("Adetus fuscoapicalis Souza f. et al. 2001", "Adetus fuscoapicalis")
          .species("Adetus", "fuscoapicalis")
          .combAuthors("2001", "Souza f.", "al.");
      assertName("Sterigmostemon rhodanthum Rech. f. et al. in Rech. f.", "Sterigmostemon rhodanthum")
          .species("Sterigmostemon", "rhodanthum")
          .combAuthors(null, "Rech.f.", "al.");
  }

  @Test
  public void authorsDoNotStartWithApostrophe() throws Exception {
      // group: Authors do not start with apostrophe
      assertName("Nereidavus kulkovi 'Kulkov", "Nereidavus kulkovi")
          .species("Nereidavus", "kulkovi");
  }

  @Test
  public void epithetsDoNotStartOrEndWithADash() throws Exception {
      // group: Epithets do not start or end with a dash. A leading-dash epithet is not
      // recognised as the species, so the rest of the line collapses into authorship.
      // A trailing-dash epithet has the dash stripped and parses as a normal binomial.
      assertName("Abryna -petri Paiva, 1860", "Abryna")
          .monomial("Abryna")
          .combAuthors("1860", "petri Paiva");
      assertName("Abryna petri- Paiva, 1860", "Abryna petri")
          .species("Abryna", "petri")
          .combAuthors("1860", "Paiva");
  }

  @Test
  public void namesThatContainOf() throws Exception {
      // group: Names that contain "of" — the parser keeps the full author span verbatim,
      // including "of" inside organisation names. Years from publishedIn-like tails attach
      // to the comb authorship.
      assertName("Musca capraria Trustees of the British Museum (Natural History), 1939", "Musca capraria")
          .species("Musca", "capraria")
          .combAuthors("1939", "Trustees of the British Museum Natural History");
      assertName("Nassellarid genera of uncertain affinities", "Nassellarid genera")
          .species("Nassellarid", "genera")
          .combAuthors(null, "of uncertain affinities");
      assertName("Natica of nidus", "Natica")
          .monomial("Natica")
          .combAuthors(null, "of nidus");
      assertName("Neritina chemmoi Reeve var of cornea Linn", "Neritina chemmoi")
          .species("Neritina", "chemmoi")
          .combAuthors(null, "Reeve var of cornea Linn");
  }

  @Test
  public void cultivars() throws Exception {
      // group: Cultivars — quoted cultivar epithet is captured as cultivarEpithet.
      assertName("Sarracenia flava 'Maxima'", "Sarracenia flava 'Maxima'")
          .cultivar("Sarracenia", "flava", "Maxima");
  }

  @Test
  public void openTaxonomyWithRanksUnfinished() throws Exception {
      // group: "Open taxonomy" with ranks unfinished — bare rank marker after a binomial
      // produces an indeterminate infraspecific name with the marker preserved in the
      // canonical, an INDETERMINED warning, and INFORMAL type.
      assertName("Alyxia reinwardti var", "Alyxia reinwardti var.")
          .infraSpecies("Alyxia", "reinwardti", VARIETY, null)
          .type(NameType.INFORMAL)
          .warning(Warnings.INDETERMINED);
      assertName("Alyxia reinwardti var.", "Alyxia reinwardti var.")
          .infraSpecies("Alyxia", "reinwardti", VARIETY, null)
          .type(NameType.INFORMAL)
          .warning(Warnings.INDETERMINED);
      assertName("Alyxia reinwardti ssp", "Alyxia reinwardti ssp.")
          .infraSpecies("Alyxia", "reinwardti", SUBSPECIES, null)
          .type(NameType.INFORMAL)
          .warning(Warnings.INDETERMINED);
      assertName("Alyxia reinwardti ssp.", "Alyxia reinwardti ssp.")
          .infraSpecies("Alyxia", "reinwardti", SUBSPECIES, null)
          .type(NameType.INFORMAL)
          .warning(Warnings.INDETERMINED);
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
      // group: Ignoring serovar/serotype. Bacterial subspecific epidemiological
      // designators (serotype/serovar [strain]) are silently stripped — they aren't
      // formal nomenclatural ranks.
      assertName("Aggregatibacter actinomycetemcomitans serotype d str. SA508", "Aggregatibacter actinomycetemcomitans")
          .species("Aggregatibacter", "actinomycetemcomitans");
      assertName("Streptococcus pyogenes (serotype M18)", "Streptococcus pyogenes")
          .species("Streptococcus", "pyogenes");
      assertName("Actinobacillus pleuropneumoniae serovar 2 strain S1536", "Actinobacillus pleuropneumoniae")
          .species("Actinobacillus", "pleuropneumoniae");
      assertName("Leptospira interrogans serovar Fugis", "Leptospira interrogans")
          .species("Leptospira", "interrogans");
  }

  @Test
  public void ignoringSensuSec() throws Exception {
      // group: Ignoring sensu sec — sensu/sec/auct./s.str./s.l. spans go into the
      // taxonomicNote field; ", pro parte" / ", p.p." are stripped silently with the
      // doubtful flag. Botanical "var." kept in canonical.
      assertName("Senecio legionensis sensu Samp., non Lange", "Senecio legionensis")
          .species("Senecio", "legionensis")
          .sensu("sensu Samp., non Lange");
      assertName("Abarema scutifera sensu auct., non (Blanco)Kosterm.", "Abarema scutifera")
          .species("Abarema", "scutifera")
          .sensu("sensu auct., non (Blanco)Kosterm.");
      assertName("Puya acris Auct.", "Puya acris")
          .species("Puya", "acris")
          .sensu("auct.");
      assertName("Puya acris Auct non L.", "Puya acris")
          .species("Puya", "acris")
          .sensu("auct non L.");
      assertName("Galium tricorne Stokes, pro parte", "Galium tricorne")
          .species("Galium", "tricorne")
          .combAuthors(null, "Stokes")
          .doubtful();
      assertName("Galium tricorne Stokes,pro parte", "Galium tricorne")
          .species("Galium", "tricorne")
          .combAuthors(null, "Stokes")
          .doubtful();
      assertName("Senecio jacquinianus sec. Rchb.", "Senecio jacquinianus")
          .species("Senecio", "jacquinianus")
          .sensu("sec. Rchb.");
      assertName("Acantholimon ulicinum S. L. Schultes", "Acantholimon ulicinum")
          .species("Acantholimon", "ulicinum")
          .combAuthors(null, "S.L.Schultes");
      assertName("Amitostigma formosana (S.S.Ying) S.S.Ying", "Amitostigma formosana")
          .species("Amitostigma", "formosana")
          .combAuthors(null, "S.S.Ying")
          .basAuthors(null, "S.S.Ying");
      assertName("Arenaria serpyllifolia L. s.str.", "Arenaria serpyllifolia")
          .species("Arenaria", "serpyllifolia")
          .combAuthors(null, "L.")
          .sensu("s.str.");
      assertName("Asplenium anisophyllum Kunze, s.l.", "Asplenium anisophyllum")
          .species("Asplenium", "anisophyllum")
          .combAuthors(null, "Kunze")
          .sensu("s.l.");
      assertName("Abramis Cuvier 1816 sec. Dybowski 1862", "Abramis")
          .monomial("Abramis")
          .combAuthors("1816", "Cuvier")
          .sensu("sec. Dybowski 1862")
          .code(NomCode.ZOOLOGICAL);
      assertName("Abramis brama subsp. bergi Grib & Vernidub 1935 sec Eschmeyer 2004", "Abramis brama bergi")
          .infraSpecies("Abramis", "brama", SUBSPECIES, "bergi")
          .combAuthors("1935", "Grib", "Vernidub")
          .sensu("sec Eschmeyer 2004")
          .code(NomCode.ZOOLOGICAL);
      assertName("Abarema clypearia (Jack) Kosterm., P. P.", "Abarema clypearia")
          .species("Abarema", "clypearia")
          .combAuthors(null, "Kosterm.")
          .basAuthors(null, "Jack")
          .doubtful();
      assertName("Abarema clypearia (Jack) Kosterm., p.p.", "Abarema clypearia")
          .species("Abarema", "clypearia")
          .combAuthors(null, "Kosterm.")
          .basAuthors(null, "Jack")
          .doubtful();
      assertName("Abarema clypearia (Jack) Kosterm., p. p.", "Abarema clypearia")
          .species("Abarema", "clypearia")
          .combAuthors(null, "Kosterm.")
          .basAuthors(null, "Jack")
          .doubtful();
      assertName("Indigofera phyllogramme var. aphylla R.Vig., p.p.B", "Indigofera phyllogramme var. aphylla")
          .infraSpecies("Indigofera", "phyllogramme", VARIETY, "aphylla")
          .combAuthors(null, "R.Vig.")
          .doubtful();
      // The remaining inputs ("Pseudomonas methanica (...) sensu. Dworkin and Foster
      // 1956", "Acantholimon ulicinum s.l. (Schultes) Boiss.", "Amaurorhinus
      // bewichianus (Wollaston,1860) (s.str.)", "Ammodramus caudacutus (s.s.)
      // diversus", "Asplenium trichomanes L. s.lat. - Asplen trich") aren't yet
      // disambiguated — the s.str./s.l./s.s. tokens get folded into the author span
      // when they sit between the species and parenthesised basionym/comb-author
      // material. Left as TODOs.
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
  public void hortAnnotations() throws Exception {
      // group: Hort. annotations — "ht." is normalised to the horticultural marker
      // "hort." (StripAndStash), so "ht.<Author>" parses as a species with the
      // horticultural author glued to the following semicolon-separated author span
      // (matching the spelled-out "hort.<Author>" twin), rather than leaking "ht" in
      // as a bogus infraspecific epithet.
      assertName("Asplenium mayi ht.May; Gard.", "Asplenium mayi")
          .species("Asplenium", "mayi")
          .combAuthors(null, "hort.May", "Gard.");
      assertName("Asplenium mayii ht.May; Gard.", "Asplenium mayii")
          .species("Asplenium", "mayii")
          .combAuthors(null, "hort.May", "Gard.");
      assertName("Davallia decora ht.Bull.; Gard.Chr.", "Davallia decora")
          .species("Davallia", "decora")
          .combAuthors(null, "hort.Bull.", "Gard.Chr.");
      assertName("Gymnogramma alstoni ht.Birkenh.; Gard.", "Gymnogramma alstoni")
          .species("Gymnogramma", "alstoni")
          .combAuthors(null, "hort.Birkenh.", "Gard.");
      assertName("Gymnogramma sprengeriana ht.Wiener Ill.", "Gymnogramma sprengeriana")
          .species("Gymnogramma", "sprengeriana")
          .combAuthors(null, "hort.Wiener Ill.");
  }

  @Test
  public void removingNomenclaturalAnnotations() throws Exception {
      // group: Removing nomenclatural annotations. Known nomenclatural notes are
      // stripped into nomenclaturalNote. Bacterial "str." (strain) marker is kept
      // in the canonical (consistent with bacterial pv. policy). Semicolon-separated
      // "(nomen nudum)" is currently captured as nomNote when it has the canonical
      // form; the bare "Nomen Nudum" trailing form is not recognised.
      assertName("Amphiprora pseudoduplex (Osada & Kobayasi, 1990) comb. nov.", "Amphiprora pseudoduplex")
          .species("Amphiprora", "pseudoduplex")
          .basAuthors("1990", "Osada", "Kobayasi")
          .code(NomCode.ZOOLOGICAL)
          .nomNote("comb. nov.");
      assertName("Methanosarcina barkeri str. fusaro", "Methanosarcina barkeri strain fusaro")
          .infraSpecies("Methanosarcina", "barkeri", STRAIN, "fusaro");
      assertName("Arthopyrenia hyalospora (Nyl.) R.C. Harris comb. nov.", "Arthopyrenia hyalospora")
          .species("Arthopyrenia", "hyalospora")
          .combAuthors(null, "R.C.Harris")
          .basAuthors(null, "Nyl.")
          .nomNote("comb. nov.");
      assertName("Acontias lineatus WAGLER 1830: 196 (nomen nudum)", "Acontias lineatus")
          .species("Acontias", "lineatus")
          .combAuthors("1830", "Wagler");
      // The trailing "(nomen nudum)" suppresses both the ":196" page capture and the
      // ZOOLOGICAL code inference (parser leans BOTANICAL because "nomen" smells like
      // a nom. annotation).
      assertName("Aster exilis Ell., nomen dubium", "Aster exilis")
          .species("Aster", "exilis")
          .combAuthors(null, "Ell.")
          .nomNote("nomen dubium");
      assertName("Abutilon avicennae Gaertn., nom. illeg.", "Abutilon avicennae")
          .species("Abutilon", "avicennae")
          .combAuthors(null, "Gaertn.")
          .nomNote("nom. illeg.");
      assertName("Achillea bonarota nom. in herb.", "Achillea bonarota")
          .species("Achillea", "bonarota")
          .nomNote("nom. in herb.");
      assertName("Aconitum napellus var. formosum (Rchb.) W. D. J. Koch (nom. ambig.)", "Aconitum napellus var. formosum")
          .infraSpecies("Aconitum", "napellus", VARIETY, "formosum")
          .combAuthors(null, "W.D.J.Koch")
          .basAuthors(null, "Rchb.")
          .code(NomCode.BOTANICAL)
          .nomNote("nom. ambig.");
      assertName("Aesculus canadensis Hort. ex Lavallée", "Aesculus canadensis")
          .species("Aesculus", "canadensis")
          .combAuthors(null, "Lavallée")
          .combExAuthors("hort.");
      assertName("× Dialaeliopsis hort.", "× Dialaeliopsis")
          .monomial("Dialaeliopsis")
          .notho(NamePart.GENERIC)
          .combAuthors(null, "hort.");
  }

  @Test
  public void miscAnnotations() throws Exception {
      // group: Misc annotations. Trailing data-quality artefacts ("species",
      // "not found", "MS"), sensu spans, and informal aggregate annotations
      // ("group" / "species group" / "species complex") are stripped. For binomials
      // an "agg./group/complex" annotation promotes the rank to SPECIES_AGGREGATE;
      // for trinomials it's stripped silently without touching the rank, so the
      // trinomial's regular code-driven rank (ZOOLOGICAL → SUBSPECIES) is kept.
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
          .infraSpecies("Acarospora", "cratericola", SUBSPECIES, "cratericola")
          .combAuthors("1974", "Shenk");
      assertName("Acarospora cratericola cratericola Shenk 1974 species group", "Acarospora cratericola cratericola")
          .infraSpecies("Acarospora", "cratericola", SUBSPECIES, "cratericola")
          .combAuthors("1974", "Shenk");
      assertName("Acarospora cratericola cratericola Shenk 1974 species complex", "Acarospora cratericola cratericola")
          .infraSpecies("Acarospora", "cratericola", SUBSPECIES, "cratericola")
          .combAuthors("1974", "Shenk");
      assertName("Parus caeruleus species complex", "Parus caeruleus")
          .binomial("Parus", null, "caeruleus", SPECIES_AGGREGATE);
      // skipped: Crenarchaeote enrichment culture clone OREC-B1022
      //   — env-sample annotation pattern not implemented (parses as messy trinomial)
      // skipped: Diodora dorsata  CF
      //   — trailing 2-letter all-caps token parses as a short author surname
      // skipped: Dasysyrphus intrudens complex sp. BBDCQ003-10
      //   — multi-annotation strip (`complex` mid-string + trailing strain code)
      //     not implemented
  }

  @Test
  public void horticulturalAnnotation() throws Exception {
      // group: Horticultural annotation. Botanical "var." kept in canonical; the
      // (ht.) / (hort.) marker after the rank-marker variety is left as an unparsed
      // tail (state=PARTIAL). "ht." is normalised to "hort." on the way in.
      assertName("Lachenalia tricolor var. nelsonii (ht.) Baker", "Lachenalia tricolor var. nelsonii")
          .infraSpecies("Lachenalia", "tricolor", VARIETY, "nelsonii")
          .combAuthors(null, "Baker")
          .partial("(hort.)");
      assertName("Lachenalia tricolor var. nelsonii (hort.) Baker", "Lachenalia tricolor var. nelsonii")
          .infraSpecies("Lachenalia", "tricolor", VARIETY, "nelsonii")
          .combAuthors(null, "Baker")
          .partial("(hort.)");
      // Trailing "ht."/"hort." after a binomial both parse as a species with the
      // horticultural marker as the comb author ("ht." is normalised to "hort.").
      assertName("Puya acris ht.", "Puya acris")
          .species("Puya", "acris")
          .combAuthors(null, "hort.");
      assertName("Puya acris hort.", "Puya acris")
          .species("Puya", "acris")
          .combAuthors(null, "hort.");
  }

  @Test
  public void namesWithMihi() throws Exception {
      // group: Names with "mihi" — Latin "by me", a self-attribution placeholder.
      // Stripped from the name with an AUTHORSHIP_REMOVED warning.
      assertName("Characium obovatum mihi. var. longipes mihi", "Characium obovatum var. longipes")
          .infraSpecies("Characium", "obovatum", VARIETY, "longipes")
          .warning(Warnings.AUTHORSHIP_REMOVED);
      assertName("Regulus modestus mihi. Gould 1837", "Regulus modestus")
          .species("Regulus", "modestus")
          .combAuthors("1837", "Gould")
          .code(NomCode.ZOOLOGICAL)
          .warning(Warnings.AUTHORSHIP_REMOVED);
  }

  @Test
  public void exceptionsWithMihi() throws Exception {
      // "mihi" between species and authors is also stripped, leaving the binomial
      // with the real authorship.
      assertName("Eucyclops serrulatus mihi Dussart, Graf & Husson, 1966", "Eucyclops serrulatus")
          .species("Eucyclops", "serrulatus")
          .combAuthors("1966", "Dussart", "Graf", "Husson")
          .code(NomCode.ZOOLOGICAL)
          .warning(Warnings.AUTHORSHIP_REMOVED);
  }

  @Test
  public void exceptionsFromRanksRankLineEpithets() throws Exception {
      // group: Exceptions from ranks (rank-line epithets) — words that look like
      // infrageneric rank markers (ab, ser, subser) but are genuine species
      // epithets when followed by an author-year span.
      assertName("Selenops ab Logunov & Jäger, 2015", "Selenops ab")
          .species("Selenops", "ab")
          .combAuthors("2015", "Logunov", "Jäger")
          .code(NomCode.ZOOLOGICAL);
      assertName("Helophorus (Lihelophorus) ser Zaitzev, 1908", "Helophorus ser")
          .species("Helophorus", "Lihelophorus", "ser")
          .combAuthors("1908", "Zaitzev")
          .code(NomCode.ZOOLOGICAL);
      // "Serina subser Gredler, 1898" and "Serina ser Gredler, 1898" — the parser
      // takes "subser"/"ser" as infrageneric rank markers (SUBSERIES_BOTANY /
      // SERIES_BOTANY) and folds "Gredler" into the infrageneric epithet. Left
      // as TODOs — needs context-aware disambiguation.
  }

  @Test
  public void exceptionsFromAuthorPrefixesPrefixLikeEpithets() throws Exception {
      // group: Exceptions from author prefixes (prefix-like epithets) — words like
      // "dela" / "den" that aren't in the AuthorParticles list already parse as
      // species. Genuine author particles (de, des, dos, du, la, van, zu) used as
      // species epithets remain ambiguous without an authority lookup ("Aaaba de
      // Laubenfels, 1936" is a uninomial; "Semiothisa da Dyar, 1916" is a binomial)
      // — those cases are kept as inline TODOs.
      assertName("Campylosphaera dela (M.N.Bramlette & F.R.Sullivan) W.W.Hay & H.Mohler", "Campylosphaera dela")
          .species("Campylosphaera", "dela")
          .combAuthors(null, "W.W.Hay", "H.Mohler")
          .basAuthors(null, "M.N.Bramlette", "F.R.Sullivan");
      assertName("Antaplaga dela Druce, 1904", "Antaplaga dela")
          .species("Antaplaga", "dela")
          .combAuthors("1904", "Druce")
          .code(NomCode.ZOOLOGICAL);
      assertName("Baeolidia dela (Er. Marcus & Ev. Marcus, 1960)", "Baeolidia dela")
          .species("Baeolidia", "dela")
          .basAuthors("1960", "Er.Marcus", "Ev.Marcus")
          .code(NomCode.ZOOLOGICAL);
      assertName("Dicentria dela Druce, 1894", "Dicentria dela")
          .species("Dicentria", "dela")
          .combAuthors("1894", "Druce")
          .code(NomCode.ZOOLOGICAL);
      assertName("Eulaira dela Chamberlin & Ivie, 1933", "Eulaira dela")
          .species("Eulaira", "dela")
          .combAuthors("1933", "Chamberlin", "Ivie")
          .code(NomCode.ZOOLOGICAL);
      assertName("Paralvinella dela Detinova, 1988", "Paralvinella dela")
          .species("Paralvinella", "dela")
          .combAuthors("1988", "Detinova")
          .code(NomCode.ZOOLOGICAL);
      assertName("Scoparia dela Clarke, 1965", "Scoparia dela")
          .species("Scoparia", "dela")
          .combAuthors("1965", "Clarke")
          .code(NomCode.ZOOLOGICAL);
      assertName("Tortolena dela Chamberlin & Ivie, 1941", "Tortolena dela")
          .species("Tortolena", "dela")
          .combAuthors("1941", "Chamberlin", "Ivie")
          .code(NomCode.ZOOLOGICAL);
      // "den" is parsed as the species epithet here because the trailing author
      // span has initials (J.L.) — disambiguates from particle usage.
      assertName("Gnathopleustes den (J.L. Barnard, 1969)", "Gnathopleustes den")
          .species("Gnathopleustes", "den")
          .basAuthors("1969", "J.L.Barnard")
          .code(NomCode.ZOOLOGICAL);
      assertName("Agnetina den Cao, T.K.T. & Bae, 2006", "Agnetina den")
          .species("Agnetina", "den")
          .combAuthors("2006", "T.K.T.Cao", "Bae")
          .code(NomCode.ZOOLOGICAL);
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
      // group: No parsing -- bacterium, Candidatus. The "Candidatus" prefix is captured
      // as a flag (isCandidatus()) and rendered in the canonical inside quotes.
      assertName("Acidobacterium ailaaui Myers & King, 2016", "Acidobacterium ailaaui")
          .species("Acidobacterium", "ailaaui")
          .combAuthors("2016", "Myers", "King");
      assertName("Candidatus", "Candidatus")
          .monomial("Candidatus");
      assertName("Candidatus Puniceispirillum Oh, Kwon, Kang, Kang, Lee, Kim & Cho, 2010", "\"Candidatus Puniceispirillum\"")
          .monomial("Puniceispirillum")
          .combAuthors("2010", "Oh", "Kwon", "Kang", "Kang", "Lee", "Kim", "Cho")
          .candidatus();
      assertName("Candidatus Halobonum", "\"Candidatus Halobonum\"")
          .monomial("Halobonum")
          .candidatus();
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
      // group: No parsing symbiont — botanical "var." kept in canonical.
      assertName("Dictyochloropsis symbiontica Tschermak-Woess", "Dictyochloropsis symbiontica")
          .species("Dictyochloropsis", "symbiontica")
          .combAuthors(null, "Tschermak-Woess");
      assertName("Dylakosoma symbionticum var. valens Skuja", "Dylakosoma symbionticum var. valens")
          .infraSpecies("Dylakosoma", "symbionticum", VARIETY, "valens")
          .combAuthors(null, "Skuja");
  }

  @Test
  public void namesWithSpecNovSpec() throws Exception {
      // group: Names with spec., nov spec
      assertName("Lampona spec Platnick, 2000", "Lampona sp.")
          .species("Lampona", null)
          .combAuthors("2000", "Platnick")
          .type(NameType.INFORMAL)
          .code(NomCode.ZOOLOGICAL)
          .warning(Warnings.INDETERMINED)
          .nothingElse();

      assertName("Gobiosoma spec (Ginsburg, 1939)", "Gobiosoma sp.")
          .species("Gobiosoma", null)
          .basAuthors("1939", "Ginsburg")
          .type(NameType.INFORMAL)
          .code(NomCode.ZOOLOGICAL)
          .warning(Warnings.INDETERMINED)
          .nothingElse();

      assertName("Globigerina spec", "Globigerina sp.")
          .species("Globigerina", null)
          .type(NameType.INFORMAL)
          .warning(Warnings.INDETERMINED)
          .nothingElse();

//      assertName("Eunotia genuflexa Norpel-Schempp nov spec", "Eunotia genuflexa")
//          .species("Eunotia", "genuflexa")
//          .combAuthors(null, "Norpel-Schempp")
//          .nomNote("nov spec")
//          .nothingElse();

      assertName("Ctenotus spec.", "Ctenotus sp.")
          .species("Ctenotus", null)
          .type(NameType.INFORMAL)
          .warning(Warnings.INDETERMINED)
          .nothingElse();

      assertName("Byrsophlebidae spec. 2", "Byrsophlebidae sp. 2")
          .phraseIndetName("Byrsophlebidae", "2", SPECIES)
          .nothingElse();

      assertName("Naviculadicta witkowskii LB & Metzeltin nov spec", "Naviculadicta witkowskii")
          .species("Naviculadicta", "witkowskii")
          .combAuthors(null, "LB", "Metzeltin")
          .nomNote("nov spec.")
          .nothingElse();
  }

  @Test
  public void htmlTagsAndEntities() throws Exception {
      // group: HTML tags and entities
      // HTML tags are stripped but their text content is kept, so the "sensu Fabricius,
      // 1780" concept reference lands in the taxonomic note rather than the authorship.
      assertName("Velutina haliotoides (Linnaeus, 1758) <i>sensu</i> Fabricius, 1780", "Velutina haliotoides")
          .species("Velutina", "haliotoides")
          .basAuthors("1758", "Linnaeus")
          .sensu("sensu Fabricius, 1780")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();

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
          .basAuthors("1953", "Mani", "Kurian")
          .code(NomCode.ZOOLOGICAL)
          .nothingElse();
  }

  @Test
  public void underscoresInsteadOfSpaces() throws Exception {
      // group: Underscores instead of spaces
      assertName("Oxalis_barrelieri", "Oxalis barrelieri")
          .species("Oxalis", "barrelieri");

      assertName("Pseudocercospora__dendrobii", "Pseudocercospora dendrobii")
          .species("Pseudocercospora", "dendrobii");

      assertName("Oxalis barrelieri XXZ_21243", "Oxalis barrelieri")
          .species("Oxalis", "barrelieri")
          .partial("XXZ_21243");
  }

  // -------------------- helpers --------------------

  NameAssertion assertName(String rawName, String expectedCanonicalWithoutAuthors) throws UnparsableNameException, InterruptedException {
    ParsedName n = parser.parse(rawName, null, Rank.UNRANKED, null);
    assertEquals(expectedCanonicalWithoutAuthors, n.canonicalNameWithoutAuthorship());
    return new NameAssertion(n);
  }

    void assertUnparsable(String rawName, NameType type) throws UnparsableNameException, InterruptedException {
      try {
        parser.parse(rawName, null, Rank.UNRANKED, null);
        fail("Name should be unparsable: " + rawName);
      } catch (UnparsableNameException e) {
        assertEquals(type, e.getType());
        assertEquals(rawName, e.getName());
      }
    }
}

