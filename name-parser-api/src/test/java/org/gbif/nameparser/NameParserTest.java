package org.gbif.nameparser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;

import com.google.common.collect.Iterables;
import org.apache.commons.io.LineIterator;
import org.gbif.nameparser.api.*;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.gbif.nameparser.api.NamePart.INFRASPECIFIC;
import static org.gbif.nameparser.api.NamePart.SPECIFIC;
import static org.gbif.nameparser.api.NameType.*;
import static org.gbif.nameparser.api.NomCode.BOTANICAL;
import static org.gbif.nameparser.api.NomCode.ZOOLOGICAL;
import static org.gbif.nameparser.api.Rank.*;
import static org.junit.Assert.*;

/**
 *
 */
public abstract class NameParserTest {
  private static Logger LOG = LoggerFactory.getLogger(NameParserTest.class);
  private final NameParser parser;

  protected NameParserTest(NameParser parser) {
    this.parser = parser;
  }

  @Test
  public void species() throws Exception {
    assertName("Diodia teres Walter", "Diodia teres")
        .species("Diodia", "teres")
        .combAuthors(null, "Walter")
        .nothingElse();

    assertName("Dysponetus bulbosus Hartmann-Schroder 1982", "Dysponetus bulbosus")
        .species("Dysponetus", "bulbosus")
        .combAuthors("1982", "Hartmann-Schroder")
        .nothingElse();

    assertName("Zophosis persis (Chatanay 1914)", "Zophosis persis")
        .species("Zophosis", "persis")
        .basAuthors("1914", "Chatanay")
        .nothingElse();

    assertName("Abies alba Mill.", "Abies alba")
        .species("Abies", "alba")
        .combAuthors(null, "Mill.")
        .nothingElse();

    assertName("Alstonia vieillardii Van Heurck & Müll.Arg.", "Alstonia vieillardii")
        .species("Alstonia", "vieillardii")
        .combAuthors(null, "Van Heurck", "Müll.Arg.")
        .nothingElse();

    assertName("Angiopteris d'urvilleana de Vriese", "Angiopteris d'urvilleana")
        .species("Angiopteris", "d'urvilleana")
        .combAuthors(null, "de Vriese")
        .nothingElse();

    assertName("Agrostis hyemalis (Walter) Britton, Sterns, & Poggenb.", "Agrostis hyemalis")
        .species("Agrostis", "hyemalis")
        .combAuthors(null, "Britton", "Sterns", "Poggenb.")
        .basAuthors(null, "Walter")
        .nothingElse();
  }

  @Test
  public void capitalAuthors() throws Exception {
    assertName("Anniella nigra FISCHER 1885", "Anniella nigra")
        .species("Anniella", "nigra")
        .combAuthors("1885", "Fischer")
        .nothingElse();
  }

  @Test
  public void infraSpecies() throws Exception {
    assertName("Poa pratensis subsp. anceps (Gaudin) Dumort., 1824", Rank.SPECIES, "Poa pratensis subsp. anceps")
        .infraSpecies("Poa", "pratensis", Rank.SUBSPECIES, "anceps")
        .basAuthors(null, "Gaudin")
        .combAuthors("1824", "Dumort.")
        .nothingElse();

    assertName("Abies alba ssp. alpina Mill.", "Abies alba subsp. alpina")
        .infraSpecies("Abies", "alba", SUBSPECIES, "alpina")
        .combAuthors(null, "Mill.")
        .nothingElse();

    assertName("Festuca ovina L. subvar. gracilis Hackel", "Festuca ovina subvar. gracilis")
        .infraSpecies("Festuca", "ovina", SUBVARIETY, "gracilis")
        .combAuthors(null, "Hackel")
        .nothingElse();

    assertName("Pseudomonas syringae pv. aceris (Ark, 1939) Young, Dye & Wilkie, 1978", "Pseudomonas syringae pv. aceris")
        .infraSpecies("Pseudomonas", "syringae", PATHOVAR, "aceris")
        .combAuthors("1978", "Young", "Dye", "Wilkie")
        .basAuthors("1939", "Ark");

    assertName("Agaricus compactus sarcocephalus (Fr.) Fr. ", "Agaricus compactus sarcocephalus")
        .infraSpecies("Agaricus", "compactus", INFRASPECIFIC_NAME, "sarcocephalus")
        .combAuthors(null, "Fr.")
        .basAuthors(null, "Fr.")
        .nothingElse();

    assertName("Baccharis microphylla Kunth var. rhomboidea Wedd. ex Sch. Bip. (nom. nud.)", "Baccharis microphylla var. rhomboidea")
        .infraSpecies("Baccharis", "microphylla", VARIETY, "rhomboidea")
        .combAuthors(null, "Sch.Bip.")
        .combExAuthors("Wedd.")
        .nomNote("nom.nud.")
        .nothingElse();

    assertName("Achillea millefolium subsp. pallidotegula B. Boivin var. pallidotegula", "Achillea millefolium var. pallidotegula")
        .infraSpecies("Achillea", "millefolium", Rank.VARIETY, "pallidotegula")
        .nothingElse();

    assertName("Achillea millefolium var. pallidotegula", Rank.INFRASPECIFIC_NAME, "Achillea millefolium var. pallidotegula")
        .infraSpecies("Achillea", "millefolium", Rank.VARIETY, "pallidotegula")
        .nothingElse();
  }

  @Test
  public void exAuthors() throws Exception {
    assertName("Acacia truncata (Burm. f.) hort. ex Hoffmanns.", "Acacia truncata")
        .species("Acacia", "truncata")
        .basAuthors(null, "Burm.f.")
        .combExAuthors("hort.")
        .combAuthors(null, "Hoffmanns.")
        .nothingElse();

    // In botany (99% of ex author use) the ex author comes first, see https://en.wikipedia.org/wiki/Author_citation_(botany)#Usage_of_the_term_.22ex.22
    assertName("Gymnocalycium eurypleurumn Plesn¡k ex F.Ritter", "Gymnocalycium eurypleurumn")
        .species("Gymnocalycium", "eurypleurumn")
        .combAuthors(null, "F.Ritter")
        .combExAuthors("Plesnik")
        .doubtful()
        .nothingElse();

    assertName("Abutilon bastardioides Baker f. ex Rose", "Abutilon bastardioides")
        .species("Abutilon", "bastardioides")
        .combAuthors(null, "Rose")
        .combExAuthors("Baker f.")
        .nothingElse();

    assertName("Baccharis microphylla Kunth var. rhomboidea Wedd. ex Sch. Bip. (nom. nud.)", "Baccharis microphylla var. rhomboidea")
        .infraSpecies("Baccharis", "microphylla", VARIETY, "rhomboidea")
        .combAuthors(null, "Sch.Bip.")
        .combExAuthors("Wedd.")
        .nomNote("nom.nud.")
        .nothingElse();

    assertName("Abies brevifolia hort. ex Dallim.", "Abies brevifolia")
        .species("Abies", "brevifolia")
        .combExAuthors("hort.")
        .combAuthors(null, "Dallim.")
        .nothingElse();

    assertName("Abies brevifolia cv. ex Dallim.", "Abies brevifolia")
        .species("Abies", "brevifolia")
        .combExAuthors("hort.")
        .combAuthors(null, "Dallim.")
        .nothingElse();

    assertName("Abutilon ×hybridum cv. ex Voss", "Abutilon × hybridum")
        .species("Abutilon", "hybridum")
        .notho(SPECIFIC)
        .combExAuthors("hort.")
        .combAuthors(null, "Voss")
        .nothingElse();

    // "Abutilon bastardioides Baker f. ex Rose"
    // "Aukuba ex Koehne 'Thunb'   "
    // "Crepinella subgen. Marchal ex Oliver  "
    // "Echinocereus sect. Triglochidiata ex Bravo"
    // "Hadrolaelia sect. Sophronitis ex Chiron & V.P.Castro"
  }

  @Test
  public void fourPartedNames() throws Exception {
    assertName("Poa pratensis kewensis primula (L.) Rouy, 1913", "Poa pratensis primula")
        .infraSpecies("Poa", "pratensis", INFRASUBSPECIFIC_NAME, "primula")
        .combAuthors("1913", "Rouy")
        .basAuthors(null, "L.")
        .nothingElse();

    assertName("Bombus sichelii alticola latofasciatus", "Bombus sichelii latofasciatus")
        .infraSpecies("Bombus", "sichelii", INFRASUBSPECIFIC_NAME, "latofasciatus")
        .nothingElse();

    assertName("Acipenser gueldenstaedti colchicus natio danubicus Movchan, 1967", "Acipenser gueldenstaedti natio danubicus")
        .infraSpecies("Acipenser", "gueldenstaedti", NATIO, "danubicus")
        .combAuthors("1967", "Movchan")
        .code(ZOOLOGICAL)
        .nothingElse();

    assertName("Cymbella cistula var. sinus regis", "Cymbella cistula var. sinus")
        .infraSpecies("Cymbella", "cistula", VARIETY, "sinus")
        .partial("regis")
        .nothingElse();
  }

  @Test
  public void monomial() throws Exception {
    assertName("Animalia", "Animalia")
        .monomial("Animalia")
        .nothingElse();
    assertName("Polychaeta", "Polychaeta")
        .monomial("Polychaeta")
        .nothingElse();
    assertName("Chrysopetalidae", "Chrysopetalidae")
        .monomial("Chrysopetalidae", Rank.FAMILY)
        .nothingElse();
    assertName("Acripeza Guérin-Ménéville 1838", "Acripeza")
        .monomial("Acripeza")
        .combAuthors("1838", "Guérin-Ménéville")
        .nothingElse();
  }

  @Test
  public void inReferences() throws Exception {
    assertName("Xolisma turquini Small apud Britton & Wilson", "Xolisma turquini")
        .species("Xolisma", "turquini")
        .combAuthors(null, "Small")
        .remarks("apud Britton & Wilson")
        .nothingElse();

    assertName("Negundo aceroides var. violaceum G.Kirchn. in Petzold & G.Kirchn.", "Negundo aceroides var. violaceum")
        .infraSpecies("Negundo", "aceroides", Rank.VARIETY, "violaceum")
        .combAuthors(null, "G.Kirchn.")
        .remarks("in Petzold & G.Kirchn.")
        .nothingElse();

    assertName("Abies denheyeri Eghbalian, Khanjani and Ueckermann in Eghbalian, Khanjani & Ueckermann, 2017", "Abies denheyeri")
        .species("Abies", "denheyeri")
        .combAuthors("2017", "Eghbalian", "Khanjani", "Ueckermann")
        .remarks("in Eghbalian, Khanjani & Ueckermann")
        .nothingElse();

    assertName("Mica Budde-Lund in Voeltzkow, 1908", "Mica")
        .monomial("Mica")
        .combAuthors("1908", "Budde-Lund")
        .remarks("in Voeltzkow")
        .nothingElse();

  }

  @Test
  public void supraGenericIPNI() throws Exception {
    assertName("Poaceae subtrib. Scolochloinae Soreng", null, "Scolochloinae")
        .monomial("Scolochloinae", SUBTRIBE)
        .combAuthors(null, "Soreng")
        .nothingElse();

    assertName("subtrib. Scolochloinae Soreng", null, "Scolochloinae")
        .monomial("Scolochloinae", SUBTRIBE)
        .combAuthors(null, "Soreng")
        .nothingElse();
  }

  @Test
  public void infraGeneric() throws Exception {
    // IPNI notho ranks: https://github.com/gbif/name-parser/issues/15
    assertName("Pinus suprasect. Taeda", null, "Pinus supersect. Taeda")
        .infraGeneric("Pinus", SUPERSECTION, "Taeda")
        .code(NomCode.BOTANICAL)
        .nothingElse();

    assertName("Aeonium nothosect. Leugalonium", null, "Aeonium nothosect. Leugalonium")
        .infraGeneric("Aeonium", SECTION, "Leugalonium")
        .notho(NamePart.INFRAGENERIC)
        .code(NomCode.BOTANICAL)
        .nothingElse();

    assertName("Narcissus nothoser. Dubizettae", null, "Narcissus nothoser. Dubizettae")
        .infraGeneric("Narcissus", SERIES, "Dubizettae")
        .notho(NamePart.INFRAGENERIC)
        .code(NomCode.BOTANICAL)
        .nothingElse();

    assertName("Serapias nothosubsect. Pladiopetalae", null, "Serapias nothosubsect. Pladiopetalae")
        .infraGeneric("Serapias", SUBSECTION, "Pladiopetalae")
        .notho(NamePart.INFRAGENERIC)
        .code(NomCode.BOTANICAL)
        .nothingElse();

    assertName("Rubus nothosubgen. Cylarubus", null, "Rubus nothosubgen. Cylarubus")
        .infraGeneric("Rubus", SUBGENUS, "Cylarubus")
        .notho(NamePart.INFRAGENERIC)
        .nothingElse();

    assertName("Arrhoges (Antarctohoges)", SUBGENUS, "Arrhoges subgen. Antarctohoges")
        .infraGeneric("Arrhoges", SUBGENUS, "Antarctohoges")
        .nothingElse();

    assertName("Polygonum", Rank.SUBGENUS, "subgen. Polygonum")
        .infraGeneric(null, Rank.SUBGENUS, "Polygonum")
        .nothingElse();

    assertName("subgen. Trematostoma Sacc.", "subgen. Trematostoma")
        .infraGeneric(null, SUBGENUS, "Trematostoma")
        .combAuthors(null, "Sacc.")
        .nothingElse();

    assertName("Echinocereus sect. Triglochidiata Bravo", "Echinocereus sect. Triglochidiata")
        .infraGeneric("Echinocereus", SECTION, "Triglochidiata")
        .combAuthors(null, "Bravo")
        .code(BOTANICAL)
        .nothingElse();

    assertName("Zignoella subgen. Trematostoma Sacc.", "Zignoella subgen. Trematostoma")
        .infraGeneric("Zignoella", SUBGENUS, "Trematostoma")
        .combAuthors(null, "Sacc.")
        .nothingElse();

    assertName("Polygonum subgen. Bistorta (L.) Zernov", "Polygonum subgen. Bistorta")
        .infraGeneric("Polygonum", SUBGENUS, "Bistorta")
        .combAuthors(null, "Zernov")
        .basAuthors(null, "L.")
        .nothingElse();

    assertName("Arrhoges (Antarctohoges)", "Arrhoges")
        .monomial("Arrhoges")
        .basAuthors(null, "Antarctohoges")
        .nothingElse();

    assertName("Festuca subg. Schedonorus (P. Beauv. ) Peterm.","Festuca subgen. Schedonorus")
        .infraGeneric("Festuca", SUBGENUS, "Schedonorus")
        .combAuthors(null, "Peterm.")
        .basAuthors(null, "P.Beauv.")
        .nothingElse();

    assertName("Catapodium subg.Agropyropsis  Trab.", "Catapodium subgen. Agropyropsis")
        .infraGeneric("Catapodium", SUBGENUS, "Agropyropsis")
        .combAuthors(null, "Trab.")
        .nothingElse();

    assertName(" Gnaphalium subg. Laphangium Hilliard & B. L. Burtt", "Gnaphalium subgen. Laphangium")
        .infraGeneric("Gnaphalium", SUBGENUS, "Laphangium")
        .combAuthors(null, "Hilliard", "B.L.Burtt")
        .nothingElse();

    assertName("Woodsiaceae (Hooker) Herter", "Woodsiaceae")
        .monomial("Woodsiaceae", FAMILY)
        .combAuthors(null, "Herter")
        .basAuthors(null, "Hooker")
        .nothingElse();
  }

  @Test
  public void notNames() throws Exception {
    assertName("Diatrypella favacea var. favacea (Fr.) Ces. & De Not.", "Diatrypella favacea var. favacea")
        .infraSpecies("Diatrypella", "favacea", VARIETY, "favacea")
        .combAuthors(null, "Ces.", "De Not.")
        .basAuthors(null, "Fr.")
        .nothingElse();

    assertName("Protoventuria rosae (De Not.) Berl. & Sacc.", "Protoventuria rosae")
        .species("Protoventuria", "rosae")
        .combAuthors(null, "Berl.", "Sacc.")
        .basAuthors(null, "De Not.")
        .nothingElse();

    assertName("Hormospora De Not.", "Hormospora")
        .monomial("Hormospora")
        .combAuthors(null, "De Not.")
        .nothingElse();
  }

  /**
   * http://dev.gbif.org/issues/browse/POR-2459
   */
  @Test
  public void unparsablePlaceholder() throws Exception {
    assertUnparsable("[unassigned] Cladobranchia", PLACEHOLDER);
    assertUnparsable("Biota incertae sedis", PLACEHOLDER);
    assertUnparsable("Mollusca not assigned", PLACEHOLDER);
    assertUnparsable("Unaccepted", PLACEHOLDER);
    assertUnparsable("uncultured Verrucomicrobiales bacterium", PLACEHOLDER);
    assertUnparsable("uncultured Vibrio sp.", PLACEHOLDER);
    assertUnparsable("uncultured virus", PLACEHOLDER);
    // ITIS placeholders:
    assertUnparsable("Temp dummy name", PLACEHOLDER);
    // https://github.com/gbif/checklistbank/issues/48
    assertUnparsable("Gen.nov. sp.nov.", NO_NAME);
    assertUnparsable("Gen.nov.", NO_NAME);
  }

  @Test
  public void placeholder() throws Exception {
    assertName("denheyeri Eghbalian, Khanjani and Ueckermann in Eghbalian, Khanjani & Ueckermann, 2017", "? denheyeri")
        .species("?", "denheyeri")
        .combAuthors("2017", "Eghbalian", "Khanjani", "Ueckermann")
        .type(PLACEHOLDER)
        .remarks("in Eghbalian, Khanjani & Ueckermann")
        .nothingElse();

    assertName("\"? gryphoidis", "? gryphoidis")
        .species("?", "gryphoidis")
        .type(PLACEHOLDER)
        .nothingElse();

    assertName("\"? gryphoidis (Bourguignat 1870) Schoepf. 1909", "? gryphoidis")
        .species("?", "gryphoidis")
        .basAuthors("1870", "Bourguignat")
        .combAuthors("1909", "Schoepf.")
        .type(PLACEHOLDER)
        .nothingElse();

    assertName("Missing penchinati Bourguignat, 1870", "? penchinati")
        .species("?", "penchinati")
        .combAuthors("1870", "Bourguignat")
        .type(PLACEHOLDER)
        .nothingElse();
  }

  @Test
  public void sanctioned() throws Exception {
    // sanctioning authors not supported
    // https://github.com/GlobalNamesArchitecture/gnparser/issues/409
    assertName("Boletus versicolor L. : Fr.", "Boletus versicolor")
        .species("Boletus", "versicolor")
        .combAuthors(null, "L.")
        .sanctAuthor("Fr.")
        .nothingElse();

    assertName("Agaricus compactus sarcocephalus (Fr. : Fr.) Fr. ", "Agaricus compactus sarcocephalus")
        .infraSpecies("Agaricus", "compactus", INFRASPECIFIC_NAME, "sarcocephalus")
        .combAuthors(null, "Fr.")
        .basAuthors(null, "Fr.")
        .nothingElse();

    assertName("Agaricus compactus sarcocephalus (Fr. : Fr.) Fr. ", "Agaricus compactus sarcocephalus")
        .infraSpecies("Agaricus", "compactus", INFRASPECIFIC_NAME, "sarcocephalus")
        .combAuthors(null, "Fr.")
        .basAuthors(null, "Fr.")
        .nothingElse();
  }

  @Test
  public void nothotaxa() throws Exception {
    // https://github.com/GlobalNamesArchitecture/gnparser/issues/410
    assertName("Iris germanica nothovar. florentina", "Iris germanica nothovar. florentina")
        .infraSpecies("Iris", "germanica", VARIETY, "florentina")
        .notho(INFRASPECIFIC)
        .nothingElse();

    assertName("Abies alba var. ×alpina L.", "Abies alba nothovar. alpina")
        .infraSpecies("Abies", "alba", VARIETY, "alpina")
        .notho(INFRASPECIFIC)
        .combAuthors(null, "L.")
        .nothingElse();
  }

  @Test
  public void aggregates() throws Exception {
    assertName("Achillea millefolium agg. L.", "Achillea millefolium")
        .binomial("Achillea", null, "millefolium", Rank.SPECIES_AGGREGATE)
        .combAuthors(null, "L.")
        .nothingElse();
  }

  @Test
  public void rankExplicit() throws Exception {
    assertName("Achillea millefolium L.", Rank.SPECIES, "Achillea millefolium")
        .species("Achillea", "millefolium")
        .combAuthors(null, "L.")
        .nothingElse();

    assertName("Achillea millefolium L.", Rank.SPECIES_AGGREGATE, "Achillea millefolium")
        .binomial("Achillea", null, "millefolium", Rank.SPECIES_AGGREGATE)
        .combAuthors(null, "L.")
        .nothingElse();

    // higher ranks should be marked as doubtful
    for (Rank r : Rank.values()) {
      if (r.otherOrUnranked() || r.isSpeciesOrBelow()) continue;
      NameAssertion ass = assertName("Achillea millefolium L.", r, "Achillea millefolium")
          .binomial("Achillea", null, "millefolium", r)
          .combAuthors(null, "L.")
          .type(INFORMAL)
          .doubtful();
      if (r.isRestrictedToCode() != null) {
        ass.code(r.isRestrictedToCode());
      }
      ass.nothingElse();
    }
  }

  @Test
  public void candidatus() throws Exception {
    assertName("\"Candidatus Endowatersipora\" Anderson and Haygood, 2007", "\"Candidatus Endowatersipora\"")
        .monomial("Endowatersipora")
        .candidatus()
        .combAuthors("2007", "Anderson", "Haygood")
        .nothingElse();

    assertName("Candidatus Phytoplasma allocasuarinae", "\"Candidatus Phytoplasma allocasuarinae\"")
        .species("Phytoplasma", "allocasuarinae")
        .candidatus()
        .nothingElse();

    assertName("Ca. Phytoplasma allocasuarinae", "\"Candidatus Phytoplasma allocasuarinae\"")
        .species("Phytoplasma", "allocasuarinae")
        .candidatus()
        .nothingElse();

    assertName("Ca. Phytoplasma", "\"Candidatus Phytoplasma\"")
        .monomial("Phytoplasma")
        .candidatus()
        .nothingElse();

    assertName("'Candidatus Nicolleia'", "\"Candidatus Nicolleia\"")
        .monomial("Nicolleia")
        .candidatus()
        .nothingElse();

    assertName("\"Candidatus Riegeria\" Gruber-Vodicka et al., 2011", "\"Candidatus Riegeria\"")
        .monomial("Riegeria")
        .combAuthors("2011", "Gruber-Vodicka", "al.")
        .candidatus()
        .nothingElse();

    assertName("Candidatus Endobugula", "\"Candidatus Endobugula\"")
        .monomial("Endobugula")
        .candidatus()
        .nothingElse();

    // not candidate names
    assertName("Centropogon candidatus Lammers", "Centropogon candidatus")
        .species("Centropogon", "candidatus")
        .combAuthors(null, "Lammers")
        .nothingElse();
  }

  @Test
  @Ignore
  public void strains() throws Exception {
    assertName("Endobugula sp. JYr4", "Endobugula sp. JYr4")
        .species("Endobugula", null)
        .strain("sp. JYr4")
        .nothingElse();

    // avoid author & year to be accepted as strain
    assertName("Anniella nigra FISCHER 1885", "Anniella nigra")
        .species("Anniella", "nigra")
        .combAuthors("1885", "Fischer")
        .nothingElse();
  }

  @Test
  public void norwegianRadiolaria() throws Exception {
    assertName("Actinomma leptodermum longispinum Cortese & Bjørklund 1998", "Actinomma leptodermum longispinum")
        .infraSpecies("Actinomma", "leptodermum", INFRASPECIFIC_NAME, "longispinum")
        .combAuthors("1998", "Cortese", "Bjørklund")
        .nothingElse();

    assertName("Arachnosphaera dichotoma  Jørgensen, 1900", "Arachnosphaera dichotoma")
        .species("Arachnosphaera", "dichotoma")
        .combAuthors("1900", "Jørgensen")
        .nothingElse();

    assertName("Hexaconthium pachydermum forma legitime Cortese & Bjørklund 1998","Hexaconthium pachydermum f. legitime")
        .infraSpecies("Hexaconthium", "pachydermum", FORM, "legitime")
        .combAuthors("1998", "Cortese", "Bjørklund")
        .nothingElse();

    assertName("Hexaconthium pachydermum form A Cortese & Bjørklund 1998","Hexaconthium pachydermum f. A")
        .infraSpecies("Hexaconthium", "pachydermum", FORM, "A")
        .combAuthors("1998", "Cortese", "Bjørklund")
        .type(INFORMAL)
        .nothingElse();

    assertName("Trisulcus aff. nana  (Popofsky, 1913), Petrushevskaya, 1971", "Trisulcus nana")
        .species("Trisulcus", "nana")
        .basAuthors("1913", "Popofsky")
        .combAuthors("1971", "Petrushevskaya")
        .type(INFORMAL)
        .remarks("aff.")
        .nothingElse();

    assertName("Tripodiscium gephyristes  (Hülseman, 1963) BJ&KR-Atsdatabanken", "Tripodiscium gephyristes")
        .species("Tripodiscium", "gephyristes")
        .basAuthors("1963", "Hülseman")
        .combAuthors(null, "BJ", "KR-Atsdatabanken")
        .nothingElse();

    assertName("Protocystis xiphodon  (Haeckel, 1887), Borgert, 1901", "Protocystis xiphodon")
        .species("Protocystis", "xiphodon")
        .basAuthors("1887", "Haeckel")
        .combAuthors("1901", "Borgert")
        .nothingElse();

    assertName("Acrosphaera lappacea  (Haeckel, 1887) Takahashi, 1991", "Acrosphaera lappacea")
        .species("Acrosphaera", "lappacea")
        .basAuthors("1887", "Haeckel")
        .combAuthors("1991", "Takahashi")
        .nothingElse();
  }

  @Test
  public void cultivars() throws Exception {
    assertName("Abutilon 'Kentish Belle'", "Abutilon 'Kentish Belle'")
        .cultivar("Abutilon", "Kentish Belle")
        .nothingElse();



    assertName("Acer campestre L. cv. 'nanum'", "Acer campestre 'nanum'")
        .cultivar("Acer", "campestre", "nanum")
        .combAuthors(null, "L.")
        .nothingElse();

    assertName("Verpericola megasoma \"Dall\" Pils.", "Verpericola megasoma 'Dall'")
        .cultivar("Verpericola", "megasoma", "Dall")
        .combAuthors(null, "Pils.")
        .nothingElse();

    assertName("Abutilon 'Kentish Belle'", "Abutilon 'Kentish Belle'")
        .cultivar("Abutilon", "Kentish Belle")
        .nothingElse();

    assertName("Abutilon 'Nabob'", "Abutilon 'Nabob'")
        .cultivar("Abutilon", "Nabob")
        .nothingElse();

    assertName("Sorbus americana Marshall cv. 'Belmonte'", "Sorbus americana 'Belmonte'")
        .cultivar("Sorbus", "americana", "Belmonte")
        .combAuthors(null, "Marshall")
        .nothingElse();

    assertName("Sorbus hupehensis C.K.Schneid. cv. 'November pink'", "Sorbus hupehensis 'November pink'")
        .cultivar("Sorbus", "hupehensis", "November pink")
        .combAuthors(null, "C.K.Schneid.")
        .nothingElse();

    assertName("Symphoricarpos albus (L.) S.F.Blake cv. 'Turesson'", "Symphoricarpos albus 'Turesson'")
        .cultivar("Symphoricarpos", "albus", CULTIVAR, "Turesson")
        .basAuthors(null, "L.")
        .combAuthors(null, "S.F.Blake")
        .nothingElse();

    assertName("Symphoricarpos sp. cv. 'mother of pearl'", "Symphoricarpos 'mother of pearl'")
        .cultivar("Symphoricarpos", CULTIVAR, "mother of pearl")
        .nothingElse();

    assertName("Primula Border Auricula Group", "Primula Border Auricula Group")
        .cultivar("Primula", CULTIVAR_GROUP, "Border Auricula")
        .nothingElse();

    assertName("Rhododendron boothii Mishmiense Group", "Rhododendron boothii Mishmiense Group")
        .cultivar("Rhododendron", "boothii", CULTIVAR_GROUP, "Mishmiense")
        .nothingElse();

    assertName("Paphiopedilum Sorel grex", "Paphiopedilum Sorel gx")
        .cultivar("Paphiopedilum", GREX, "Sorel")
        .nothingElse();

    assertName("Cattleya Prince John gx", "Cattleya Prince John gx")
        .cultivar("Cattleya", GREX, "Prince John")
        .nothingElse();
  }

  @Test
  public void hybridFormulas() throws Exception {
    assertName("Polypodium  x vulgare nothosubsp. mantoniae (Rothm.) Schidlay", "Polypodium vulgare nothosubsp. mantoniae")
        .infraSpecies("Polypodium", "vulgare", SUBSPECIES, "mantoniae")
        .basAuthors(null, "Rothm.")
        .combAuthors(null, "Schidlay")
        .notho(INFRASPECIFIC)
        .nothingElse();

    assertHybridFormula("Asplenium rhizophyllum DC. x ruta-muraria E.L. Braun 1939");
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
  }

  private void assertHybridFormula(String name) {
    assertUnparsable(name, HYBRID_FORMULA);
  }

  @Test
  public void oTU() throws Exception {
    assertName("BOLD:ACW2100", "BOLD:ACW2100")
        .monomial("BOLD:ACW2100", Rank.SPECIES)
        .type(OTU)
        .nothingElse();

    assertName("SH460441.07FU", "SH460441.07FU")
        .monomial("SH460441.07FU", Rank.SPECIES)
        .type(OTU)
        .nothingElse();

    assertName("sh460441.07fu", "SH460441.07FU")
        .monomial("SH460441.07FU", Rank.SPECIES)
        .type(OTU)
        .nothingElse();

    assertName("Festuca sp. BOLD:ACW2100", "BOLD:ACW2100")
        .monomial("BOLD:ACW2100", Rank.SPECIES)
        .type(OTU)
        .nothingElse();

    // no OTU names
    assertName("Boldenaria", "Boldenaria")
        .monomial("Boldenaria")
        .nothingElse();

    assertName("Boldea", "Boldea")
        .monomial("Boldea")
        .nothingElse();

    assertName("Boldiaceae", "Boldiaceae")
        .monomial("Boldiaceae", Rank.FAMILY)
        .nothingElse();

    assertName("Boldea vulgaris", "Boldea vulgaris")
        .species("Boldea", "vulgaris")
        .nothingElse();
  }

  /**
   * http://dev.gbif.org/issues/browse/POR-2397
   *
   * TODO: convert all test cases. Can the strain & manuscript name property be merged ???
   */
  @Test
  public void strainNames() throws Exception {
    assertName("Candidatus Liberibacter solanacearum", "\"Candidatus Liberibacter solanacearum\"")
        .species("Liberibacter", "solanacearum")
        .candidatus()
        .nothingElse();

    //assertName("Methylocystis sp. M6", "Methylocystis sp. M6")
    //    .species("Liberibacter", "solanacearum")
    //    .candidatus()
    //    .nothingElse();

    //assertStrain("", NameType.INFORMAL, "Methylocystis", null, null, Rank.SPECIES, "M6");
    //assertStrain("Advenella kashmirensis W13003", NameType.INFORMAL, "Advenella", "kashmirensis", null, null, "W13003");
    //assertStrain("Garra cf. dampaensis M23", NameType.INFORMAL, "Garra", "dampaensis", null, null, "M23");
    //assertStrain("Sphingobium lucknowense F2", NameType.INFORMAL, "Sphingobium", "lucknowense", null, null, "F2");
    //assertStrain("Pseudomonas syringae pv. atrofaciens LMG 5095T", NameType.INFORMAL, "Pseudomonas", "syringae", "atrofaciens", Rank.PATHOVAR, "LMG 5095T");
  }

  @Test
  public void hybridAlikeNames() throws Exception {
    assertName("Huaiyuanella Xing, Yan & Yin, 1984", "Huaiyuanella")
        .monomial("Huaiyuanella")
        .combAuthors("1984", "Xing", "Yan", "Yin")
        .nothingElse();

    assertName("Caveasphaera Xiao & Knoll, 2000", "Caveasphaera")
        .monomial("Caveasphaera")
        .combAuthors("2000", "Xiao", "Knoll")
        .nothingElse();
  }

  @Test
  @Ignore("Need to evaluate and implement these alpha/beta/gamme/theta names. Comes from cladistics?")
  public void alphaBetaThetaNames() {
    // 11383509 | VARIETY | Trianosperma ficifolia var. βrigida Cogn.
    // 11599666 |         | U. caerulea var. β
    // 12142976 | CLASS   | γ Proteobacteria
    // 12142978 | CLASS   | γ-proteobacteria
    // 16220269 |         | U. caerulea var. β
    // 1297218  | SPECIES | Bacteriophage Qβ
    // 307122   | SPECIES | Agaricus collinitus β mucosus (Bull.) Fr.
    // 313460   | SPECIES | Agaricus muscarius β regalis Fr. (1821)
    // 315162   | SPECIES | Agaricus personatus β saevus
    // 1774875  | VARIETY | Caesarea albiflora var. βramosa Cambess.
    // 3164679  | VARIETY | Cyclotus amethystinus var. α Guppy, 1868 (in part)
    // 3164681  | VARIETY | Cyclotus amethystinus var. β Guppy, 1868
    // 6531344  | SPECIES | Lycoperdon pyriforme β tessellatum Pers. (1801)
    // 7487686  | VARIETY | Nephilengys malabarensis var. β
    // 9665391  | VARIETY | Ranunculus purshii Hook. var. repens(-δ) Hook.
  }

  @Test
  public void hybridNames() throws Exception {
    assertName("+ Pyrocrataegus willei L.L.Daniel", "× Pyrocrataegus willei")
        .species("Pyrocrataegus", "willei")
        .combAuthors(null, "L.L.Daniel")
        .notho(NamePart.GENERIC)
        .nothingElse();

    assertName("×Pyrocrataegus willei L.L. Daniel", "× Pyrocrataegus willei")
        .species("Pyrocrataegus", "willei")
        .combAuthors(null, "L.L.Daniel")
        .notho(NamePart.GENERIC)
        .nothingElse();

    assertName(" × Pyrocrataegus willei  L. L. Daniel", "× Pyrocrataegus willei")
        .species("Pyrocrataegus", "willei")
        .combAuthors(null, "L.L.Daniel")
        .notho(NamePart.GENERIC)
        .nothingElse();

    assertName(" X Pyrocrataegus willei L. L. Daniel", "× Pyrocrataegus willei")
        .species("Pyrocrataegus", "willei")
        .combAuthors(null, "L.L.Daniel")
        .notho(NamePart.GENERIC)
        .nothingElse();

    assertName("Pyrocrataegus ×willei L. L. Daniel", "Pyrocrataegus × willei")
        .species("Pyrocrataegus", "willei")
        .combAuthors(null, "L.L.Daniel")
        .notho(NamePart.SPECIFIC)
        .nothingElse();

    assertName("Pyrocrataegus × willei L. L. Daniel", "Pyrocrataegus × willei")
        .species("Pyrocrataegus", "willei")
        .combAuthors(null, "L.L.Daniel")
        .notho(NamePart.SPECIFIC)
        .nothingElse();

    assertName("Pyrocrataegus x willei L. L. Daniel", "Pyrocrataegus × willei")
        .species("Pyrocrataegus", "willei")
        .combAuthors(null, "L.L.Daniel")
        .notho(NamePart.SPECIFIC)
        .nothingElse();

    assertName("Pyrocrataegus X willei L. L. Daniel", "Pyrocrataegus × willei")
        .species("Pyrocrataegus", "willei")
        .combAuthors(null, "L.L.Daniel")
        .notho(NamePart.SPECIFIC)
        .nothingElse();

    assertName("Pyrocrataegus willei ×libidi  L.L.Daniel", "Pyrocrataegus willei × libidi")
        .infraSpecies("Pyrocrataegus", "willei", INFRASPECIFIC_NAME, "libidi")
        .combAuthors(null, "L.L.Daniel")
        .notho(INFRASPECIFIC)
        .nothingElse();

    assertName("Pyrocrataegus willei nothosubsp. libidi  L.L.Daniel", "Pyrocrataegus willei nothosubsp. libidi")
        .infraSpecies("Pyrocrataegus", "willei", SUBSPECIES, "libidi")
        .combAuthors(null, "L.L.Daniel")
        .notho(INFRASPECIFIC)
        .nothingElse();

    assertName("+ Pyrocrataegus willei nothosubsp. libidi  L.L.Daniel", "Pyrocrataegus willei nothosubsp. libidi")
        .infraSpecies("Pyrocrataegus", "willei", SUBSPECIES, "libidi")
        .combAuthors(null, "L.L.Daniel")
        .notho(INFRASPECIFIC)
        .nothingElse();

    //TODO: impossible name. should this not be a generic hybrid as its the highest rank crossed?
    assertName("×Pyrocrataegus ×willei ×libidi L.L.Daniel", "Pyrocrataegus willei × libidi")
        .infraSpecies("Pyrocrataegus", "willei", INFRASPECIFIC_NAME, "libidi")
        .combAuthors(null, "L.L.Daniel")
        .notho(INFRASPECIFIC)
        .nothingElse();

  }

  @Test
  public void authorVariations() throws Exception {
    assertName("Modiola caroliniana L.f", "Modiola caroliniana")
        .species("Modiola", "caroliniana")
        .combAuthors(null, "L.f")
        .nothingElse();

    assertName("Modiola caroliniana (L.) G. Don filius", "Modiola caroliniana")
        .species("Modiola", "caroliniana")
        .basAuthors(null, "L.")
        .combAuthors(null, "G.Don filius")
        .nothingElse();

    assertName("Modiola caroliniana (L.) G. Don fil.", "Modiola caroliniana")
        .species("Modiola", "caroliniana")
        .basAuthors(null, "L.")
        .combAuthors(null, "G.Don fil.")
        .nothingElse();

    assertName("Cirsium creticum d'Urv.", "Cirsium creticum")
        .species("Cirsium", "creticum")
        .combAuthors(null, "d'Urv.")
        .nothingElse();

    // TODO: autonym authors are the species authors !!!
    assertName("Cirsium creticum d'Urv. subsp. creticum", "Cirsium creticum subsp. creticum")
        .infraSpecies("Cirsium", "creticum", SUBSPECIES, "creticum")
        //.combAuthors(null, "d'Urv.")
        .autonym()
        .nothingElse();

    assertName("Cirsium creticum Balsamo M Fregni E Tongiorgi P", "Cirsium creticum")
        .species("Cirsium", "creticum")
        .combAuthors(null, "M.Balsamo", "E.Fregni", "P.Tongiorgi")
        .nothingElse();

    assertName("Cirsium creticum Balsamo M Todaro MA", "Cirsium creticum")
        .species("Cirsium", "creticum")
        .combAuthors(null, "M.Balsamo", "M.A.Todaro")
        .nothingElse();

    assertName("Bolivina albatrossi Cushman Em. Sellier de Civrieux, 1976", "Bolivina albatrossi")
        .species("Bolivina", "albatrossi")
        .combAuthors("1976", "Cushman Em.Sellier de Civrieux")
        .nothingElse();

    // http://dev.gbif.org/issues/browse/POR-101
    assertName("Cribbia pendula la Croix & P.J.Cribb", "Cribbia pendula")
        .species("Cribbia", "pendula")
        .combAuthors(null, "la Croix", "P.J.Cribb")
        .nothingElse();

    assertName("Cribbia pendula le Croix & P.J.Cribb", "Cribbia pendula")
        .species("Cribbia", "pendula")
        .combAuthors(null, "le Croix", "P.J.Cribb")
        .nothingElse();

    assertName("Cribbia pendula de la Croix & le P.J.Cribb", "Cribbia pendula")
        .species("Cribbia", "pendula")
        .combAuthors(null, "de la Croix", "le P.J.Cribb")
        .nothingElse();

    assertName("Cribbia pendula Croix & de le P.J.Cribb", "Cribbia pendula")
        .species("Cribbia", "pendula")
        .combAuthors(null, "Croix", "de le P.J.Cribb")
        .nothingElse();

    assertName("Navicula ambigua f. craticularis Istv?nffi, 1898, 1897", "Navicula ambigua f. craticularis")
        .infraSpecies("Navicula", "ambigua", Rank.FORM, "craticularis")
        .combAuthors("1898", "Istvnffi")
        .doubtful()
        .nothingElse();

    assertName("Cestodiscus gemmifer F.S.Castracane degli Antelminelli", "Cestodiscus gemmifer")
        .species("Cestodiscus", "gemmifer")
        .combAuthors(null, "F.S.Castracane degli Antelminelli")
        .nothingElse();

    assertName("Hieracium scorzoneraefolium De la Soie", "Hieracium scorzoneraefolium")
        .species("Hieracium", "scorzoneraefolium")
        .combAuthors(null, "De la Soie")
        .nothingElse();

    assertName("Calycostylis aurantiaca Hort. ex Vilmorin", "Calycostylis aurantiaca")
        .species("Calycostylis", "aurantiaca")
        .combAuthors(null, "Vilmorin")
        .combExAuthors("hort.")
        .nothingElse();

    assertName("Pourretia magnispatha hortusa ex K. Koch", "Pourretia magnispatha")
        .species("Pourretia", "magnispatha")
        .combAuthors(null, "K.Koch")
        .combExAuthors("hort.")
        .nothingElse();

    assertName("Pitcairnia pruinosa hortus ex K. Koch", "Pitcairnia pruinosa")
        .species("Pitcairnia", "pruinosa")
        .combAuthors(null, "K.Koch")
        .combExAuthors("hort.")
        .nothingElse();

    assertName("Platycarpha glomerata (Thunberg) A.P.de Candolle", "Platycarpha glomerata")
        .species("Platycarpha", "glomerata")
        .basAuthors(null, "Thunberg")
        .combAuthors(null, "A.P.de Candolle")
        .nothingElse();

  }

  @Test
  public void extinctNames() throws Exception {
    assertName("†Titanoptera", "Titanoptera")
        .monomial("Titanoptera")
        .nothingElse();

    assertName("† Tuarangiida MacKinnon, 1982", "Tuarangiida")
        .monomial("Tuarangiida")
        .combAuthors("1982", "MacKinnon")
        .nothingElse();
  }

  /**
   * Simply test all names in names-with-authors.txt and make sure they parse without exception
   * and have an authorship!
   * This test does not verify if the parsed name was correct in all its pieces,
   * so only use this as a quick way to add names to tests.
   *
   * Exceptional cases should better be tested in a test on its own!
   */
  @Test
  public void namesWithAuthorFile() throws Exception {
    for (String name : iterResource("names-with-authors.txt")) {
      ParsedName n = parser.parse(name, null);
      assertTrue(name, n.getState().isParsed());
      assertTrue(name, n.hasAuthorship());
    }
  }

  /**
   * Test all names in doubtful.txt and make sure they parse without exception,
   * but have a doubtful flag set.
   * This test does not verify if the parsed name was correct in all its pieces,
   * so only use this as a quick way to add names to tests.
   *
   * Exceptional cases should better be tested in a test on its own!
   */
  @Test
  public void doubtfulFile() throws Exception {
    for (String name : iterResource("doubtful.txt")) {
      ParsedName n = parser.parse(name, null);
      assertTrue(name, n.isDoubtful());
      assertTrue(name, n.getState().isParsed());
      assertTrue(name, n.getType().isParsable());
    }
  }

  /**
   * Test all names in unparsable.txt and makes sure they are not parsable.
   */
  @Test
  public void unparsableFile() throws Exception {
    for (String name : iterResource("unparsable.txt")) {
      try {
        parser.parse(name);
        fail("Expected "+name+" to be unparsable");
      } catch (UnparsableNameException ex) {
        assertEquals(name, ex.getName());
      }
    }
  }

  /**
   * Test all names in nonames.txt and makes sure they are NO_NAMEs.
   */
  @Test
  public void nonamesFile() throws Exception {
    for (String name : iterResource("nonames.txt")) {
      try {
        ParsedName pn = parser.parse(name);
        fail("Expected "+name+" to be unparsable");
      } catch (UnparsableNameException ex) {
        assertEquals(NameType.NO_NAME, ex.getType());
        assertEquals(name, ex.getName());
      }
    }
  }

  /**
   * Test all hybrid formulas in hybrids.txt and makes sure they are HYBRID_FORMULAs.
   */
  @Test
  public void hybridsFile() throws Exception {
    for (String name : iterResource("hybrids.txt")) {
      try {
        ParsedName pn = parser.parse(name);
        fail("Expected "+name+" to be unparsable hybrid");
      } catch (UnparsableNameException ex) {
        assertEquals(NameType.HYBRID_FORMULA, ex.getType());
        assertEquals(name, ex.getName());
      }
    }
  }

  /**
   * Test all names in nonames.txt and makes sure they are NO_NAMEs.
   */
  @Test
  public void placeholderFile() throws Exception {
    for (String name : iterResource("placeholder.txt")) {
      try {
        ParsedName pn = parser.parse(name);
        fail("Expected "+name+" to be an unparsable placeholder");
      } catch (UnparsableNameException ex) {
        assertEquals(NameType.PLACEHOLDER, ex.getType());
        assertEquals(name, ex.getName());
      }
    }
  }

  @Test
  public void occNameFile() throws Exception {
    int currFail = 4;
    int fails = parseFile("occurrence-names.txt");
    if (fails > currFail) {
      fail("We are getting worse, not better. Currently failing: " + fails + ". Was passing:" + currFail);
    }
  }

  /**
   * Parse all verbatim GBIF checklist names to spot room for improvements
   */
  @Test
  @Ignore
  public void gbifFile() throws Exception {
    parseFile("gbif-verbatim-names.txt");
  }

  /**
   * @return number of failed names
   */
  private int parseFile(String resourceName) throws Exception {
    int parseFails = 0;
    int counter = 0;
    long start = System.currentTimeMillis();
    for (String name : iterResource(resourceName)) {
      counter++;
      if (counter % 100000 == 0) {
        long end = System.currentTimeMillis();
        LOG.info("{} names tested, {} failed", counter, parseFails);
        LOG.info("Total time {}ms, average per name {}", (end - start), (((double) end - start) / counter));
      }
      try {
        ParsedName pn = parser.parse(name);
        if (pn.getState() != ParsedName.State.COMPLETE) {
          LOG.debug("{} {}", pn.getState(), name);
        }
      } catch (UnparsableNameException ex) {
        if (ex.getType().isParsable() || ex.getType() == NO_NAME) {
          parseFails++;
          LOG.warn("{}: {}", ex.getType(), name);
        }
      }
    }
    long end = System.currentTimeMillis();
    LOG.info("{} names tested, {} failed", counter, parseFails);
    LOG.info("Total time {}ms, average per name {}", (end - start), (((double) end - start) / counter));
    return parseFails;
  }

  /**
   * Converts lines of a classpath resource that are not empty or are comments starting with #
   * into a simple string iterable
   */
  private Iterable<String> iterResource(String resource) throws UnsupportedEncodingException {
    LineIterator iter = new LineIterator(resourceReader(resource));
    return Iterables.filter(() -> iter,
        line -> line != null && !line.trim().isEmpty() && !line.startsWith("#")
    );
  }

  /**
   * Expect empty unparsable results for nothing or whitespace
   */
  @Test
  public void empty() throws Exception {
    assertNoName(null);
    assertNoName("");
    assertNoName(" ");
    assertNoName("\t");
    assertNoName("\n");
    assertNoName("\t\n");
    assertNoName("\"");
    assertNoName("'");
  }

  /**
   * Avoid nPEs and other exceptions for very short non names and other extremes found in occurrences.
   */
  @Test
  public void avoidNPE() throws Exception {
    assertNoName("\\");
    assertNoName(".");
    assertNoName("@");
    assertNoName("&nbsp;");
    assertNoName("X");
    assertNoName("a");
    assertNoName("143");
    assertNoName("321-432");
    assertNoName("-,.#");
    assertNoName(" .");
  }

  @Test
  public void informal() throws Exception {
    assertName("Trisulcus aff. nana  Petrushevskaya, 1971", "Trisulcus nana")
        .species("Trisulcus", "nana")
        .combAuthors("1971", "Petrushevskaya")
        .type(INFORMAL)
        .remarks("aff.")
        .nothingElse();
  }

  @Test
  public void abbreviated() throws Exception {
    assertName("N. giraldo", "N. giraldo")
        .species("N.", "giraldo")
        .type(INFORMAL)
        .nothingElse();

    assertName("B.", "B.")
        .monomial("B.")
        .type(INFORMAL)
        .nothingElse();
  }

  @Test
  public void stringIndexOutOfBoundsException() throws Exception {
    parser.parse("Amblyomma americanum (Linnaeus, 1758)", null);
    parser.parse("Salix taiwanalpina var. chingshuishanensis (S.S.Ying) F.Y.Lu, C.H.Ou, Y.C.Chen, Y.S.Chi, K.C.Lu & Y.H.Tseng ", null);
    parser.parse("Salix taiwanalpina var. chingshuishanensis (S.S.Ying) F.Y.Lu, C.H.Ou, Y.C.Chen, Y.S.Chi, K.C.Lu & amp  Y.H.Tseng ", null);
    parser.parse("Salix morrisonicola var. takasagoalpina (Koidz.) F.Y.Lu, C.H.Ou, Y.C.Chen, Y.S.Chi, K.C.Lu & amp; Y.H.Tseng", null);
    parser.parse("Ficus ernanii Carauta, Pederneir., P.P.Souza, A.F.P.Machado, M.D.M.Vianna & amp; Romaniuc", null);
  }

  @Test
  public void nomNotes() throws Exception {
    //TODO: pro syn.
    assertName("Combretum Loefl. (1758), nom. cons. [= Grislea L. 1753].", "Combretum")
        .monomial("Combretum")
        .combAuthors("1758", "Loefl.")
        .nomNote("nom.cons.")
        .doubtful()
        .partial(")(= Grislea L.1753).")
        .nothingElse();
  }

  @Test
  public void taxonomicNotes() throws Exception {
    assertName("Dyadobacter (Chelius & Triplett, 2000) emend. Reddy & Garcia-Pichel, 2005", "Dyadobacter")
        .monomial("Dyadobacter")
        .basAuthors("2000", "Chelius", "Triplett")
        .sensu("emend. Reddy & Garcia-Pichel, 2005")
        .nothingElse();

    assertName("Thalassiosira praeconvexa Burckle emend Gersonde & Schrader, 1984", "Thalassiosira praeconvexa")
        .species("Thalassiosira", "praeconvexa")
        .combAuthors(null, "Burckle")
        .sensu("emend Gersonde & Schrader, 1984")
        .nothingElse();

    assertName("Amphora gracilis f. exilis Gutwinski according to Hollerback & Krasavina, 1971", "Amphora gracilis f. exilis")
        .infraSpecies("Amphora", "gracilis", Rank.FORM, "exilis")
        .combAuthors(null, "Gutwinski")
        .sensu("according to Hollerback & Krasavina, 1971")
        .nothingElse();

    assertSensu("Trifolium repens sensu Baker f.", "sensu Baker f.");
    assertSensu("Achillea millefolium sensu latu", "sensu latu");
    assertSensu("Achillea millefolium s.str.", "s.str.");
    assertSensu("Achillea millefolium sec. Greuter 2009", "sec. Greuter 2009");
    assertSensu("Globularia cordifolia L. excl. var. (emend. Lam.)", "excl. var. (emend. Lam.)");

    assertName("Ramaria subbotrytis (Coker) Corner 1950 ss. auct. europ.", "Ramaria subbotrytis")
        .species("Ramaria", "subbotrytis")
        .basAuthors(null, "Coker")
        .combAuthors("1950", "Corner")
        .sensu("ss. auct. europ.")
        .nothingElse();

    assertName("Thelephora cuticularis Berk. ss. auct. europ.", "Thelephora cuticularis")
        .species("Thelephora", "cuticularis")
        .combAuthors(null, "Berk.")
        .sensu("ss. auct. europ.")
        .nothingElse();

    assertName("Handmannia austriaca f. elliptica Handmann fide Hustedt, 1922", "Handmannia austriaca f. elliptica")
        .infraSpecies("Handmannia", "austriaca", Rank.FORM, "elliptica")
        .combAuthors(null, "Handmann")
        .sensu("fide Hustedt, 1922")
        .nothingElse();
  }

  @Test
  public void nonNames() throws Exception {
    // the entire name ends up as a taxonomic note, consider this as unparsed...
    assertUnparsable("non  Ramaria fagetorum Maas Geesteranus 1976 nomen nudum = Ramaria subbotrytis sensu auct. europ.", Rank.SPECIES, NameType.NO_NAME);

    assertName("Hebeloma album Peck 1900 non ss. auct. europ.", "Hebeloma album")
        .species("Hebeloma", "album")
        .combAuthors("1900", "Peck")
        .sensu("non ss. auct. europ.")
        .nothingElse();

    assertName("Nitocris (Nitocris) similis Breuning, 1956 (nec Gahan, 1893)", "Nitocris similis")
        .binomial("Nitocris", "Nitocris", "similis", Rank.SPECIES)
        .combAuthors("1956", "Breuning")
        .sensu("nec Gahan, 1893")
        .nothingElse();

    assertName("Bartlingia Brongn. non Rchb. 1824 nec F.Muell. 1882", "Bartlingia")
        .monomial("Bartlingia")
        .combAuthors(null, "Brongn.")
        .sensu("non Rchb. 1824 nec F.Muell. 1882")
        .nothingElse();

    assertName("Lindera Thunb. non Adans. 1763", "Lindera")
        .monomial("Lindera")
        .combAuthors(null, "Thunb.")
        .sensu("non Adans. 1763")
        .nothingElse();

    assertName("Chorististium maculatum (non Bloch 1790)", "Chorististium maculatum")
        .species("Chorististium", "maculatum")
        .sensu("non Bloch 1790")
        .nothingElse();

    assertName("Puntius arulius subsp. tambraparniei (non Silas 1954)", "Puntius arulius subsp. tambraparniei")
        .infraSpecies("Puntius", "arulius", Rank.SUBSPECIES, "tambraparniei")
        .sensu("non Silas 1954")
        .nothingElse();

  }

  @Test
  public void misapplied() throws Exception {
    assertName("Ficus exasperata auct. non Vahl", "Ficus exasperata")
        .species("Ficus", "exasperata")
        .sensu("auct. non Vahl")
        .nothingElse();

    assertName("Mentha rotundifolia auct. non (L.) Huds. 1762", "Mentha rotundifolia")
        .species("Mentha", "rotundifolia")
        .sensu("auct. non (L.) Huds. 1762")
        .nothingElse();

  }

  private void assertSensu(String raw, String sensu) throws UnparsableNameException {
    assertEquals(sensu, parser.parse(raw, null).getTaxonomicNote());
  }

  @Test
  public void viralNames() throws Exception {
    assertTrue(isViralName("Cactus virus 2"));
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

    assertName("Crassatellites janus Hedley, 1906", "Crassatellites janus")
        .species("Crassatellites", "janus")
        .combAuthors("1906", "Hedley")
        .nothingElse();

    assertName("Ypsolophus satellitella", "Ypsolophus satellitella")
        .species("Ypsolophus", "satellitella")
        .nothingElse();

    assertName("Nephodia satellites", "Nephodia satellites")
        .species("Nephodia", "satellites")
        .nothingElse();

    Reader reader = resourceReader("viruses.txt");
    LineIterator iter = new LineIterator(reader);
    while (iter.hasNext()) {
      String line = iter.nextLine();
      if (line == null || line.startsWith("#") || line.trim().isEmpty()) {
        continue;
      }
      assertTrue(isViralName(line));
    }
  }

  @Test
  public void apostropheEpithets() throws Exception {
    assertName("Junellia o'donelli Moldenke, 1946", "Junellia o'donelli")
        .species("Junellia", "o'donelli")
        .combAuthors("1946", "Moldenke")
        .nothingElse();

    assertName("Trophon d'orbignyi Carcelles, 1946", "Trophon d'orbignyi")
        .species("Trophon", "d'orbignyi")
        .combAuthors("1946", "Carcelles")
        .nothingElse();

    assertName("Arca m'coyi Tenison-Woods, 1878", "Arca m'coyi")
        .species("Arca", "m'coyi")
        .combAuthors("1878", "Tenison-Woods")
        .nothingElse();

    assertName("Nucula m'andrewii Hanley, 1860", "Nucula m'andrewii")
        .species("Nucula", "m'andrewii")
        .combAuthors("1860", "Hanley")
        .nothingElse();

    assertName("Eristalis l'herminierii Macquart", "Eristalis l'herminierii")
        .species("Eristalis", "l'herminierii")
        .combAuthors(null, "Macquart")
        .nothingElse();

    assertName("Odynerus o'neili Cameron", "Odynerus o'neili")
        .species("Odynerus", "o'neili")
        .combAuthors(null, "Cameron")
        .nothingElse();

    assertName("Serjania meridionalis Cambess. var. o'donelli F.A. Barkley", "Serjania meridionalis var. o'donelli")
        .infraSpecies("Serjania", "meridionalis", Rank.VARIETY, "o'donelli")
        .combAuthors(null, "F.A.Barkley")
        .nothingElse();
  }

  /**
   * http://dev.gbif.org/issues/browse/POR-3069
   */
  @Test
  public void nullNameParts() throws Exception {
    assertName("Austrorhynchus pectatus null pectatus", "Austrorhynchus pectatus pectatus")
        .infraSpecies("Austrorhynchus", "pectatus", Rank.INFRASPECIFIC_NAME, "pectatus")
        .doubtful()
        .nothingElse();

    //assertName("Poa pratensis null proles (L.) Rouy, 1913", "Poa pratensis proles")
    //    .infraSpecies("Poa", "pratensis", Rank.PROLES, "proles")
    //    .basAuthors(null, "L.")
    //    .combAuthors("1913", "Rouy")
    //    .nothingElse();

    // should the infrasubspecific epithet kewensis be removed from the parsed name?
    //assertParsedParts("Poa pratensis kewensis proles", NameType.INFORMAL, "Poa", "pratensis", "kewensis", Rank.PROLES, null);
    //assertParsedParts("Poa pratensis kewensis proles (L.) Rouy, 1913", NameType.INFORMAL, "Poa", "pratensis", null, Rank.PROLES, "Rouy", "1913", "L.", null);
  }



  @Test
  @Ignore
  public void rNANames() throws Exception {
    assertName("Calathus (Lindrothius) KURNAKOV 1961", "Calathus (Lindrothius)")
        .infraGeneric("Calathus", Rank.INFRAGENERIC_NAME, "Lindrothius")
        .combAuthors("1961", "Kurnakov")
        .nothingElse();

    assertTrue(isViralName("Ustilaginoidea virens RNA virus"));
    assertTrue(isViralName("Rhizoctonia solani dsRNA virus 2"));

    assertName("Candida albicans RNA_CTR0-3", "Candida albicans RNA_CTR0-3")
        .species("Candida", "albicans")
        .nothingElse();


    //pn = parser.parse("Alpha proteobacterium RNA12", null);
    //assertEquals("Alpha", pn.getGenusOrAbove());
    //assertEquals("proteobacterium", pn.getSpecificEpithet());
    //assertEquals(NameType.INFORMAL, pn.getType());
    //assertNull(pn.getInfraSpecificEpithet());
    //assertNull(pn.getAuthorship());
//
    //pn = parser.parse("Armillaria ostoyae RNA1", null);
    //assertEquals("Armillaria", pn.getGenusOrAbove());
    //assertEquals("ostoyae", pn.getSpecificEpithet());
    //assertEquals(NameType.INFORMAL, pn.getType());
    //assertNull(pn.getInfraSpecificEpithet());
    //assertNull(pn.getAuthorship());
//
    //assertUnparsableType(NameType.DOUBTFUL, "siRNA");
  }

  @Test
  public void indetNames() throws Exception {
    assertName("Nitzschia sinuata var. (Grunow) Lange-Bert.", "Nitzschia sinuata var.")
        .infraSpecies("Nitzschia", "sinuata", Rank.VARIETY, null)
        .type(NameType.INFORMAL)
        .nothingElse();

    assertName("Canis lupus subsp. Linnaeus, 1758", "Canis lupus subsp.")
        .infraSpecies("Canis", "lupus", Rank.SUBSPECIES, null)
        .type(NameType.INFORMAL)
        .nothingElse();

//    assertName("Aphaenogaster (Ichnomyrmex) Schwammerdami var. spinipes", "Aphaenogaster var. spinipes")
//        .infraSpecies("Aphaenogaster", null, Rank.VARIETY, "spinipes")
//        .infraGeneric("Ichnomyrmex")
//        .type(NameType.INFORMAL)
//        .nothingElse();
//
//    assertName("Ocymyrmex Weitzaeckeri subsp. arnoldi", "Ocymyrmex subsp. arnoldi")
//        .infraSpecies("Ocymyrmex", null, Rank.SUBSPECIES, "arnoldi")
//        .type(NameType.INFORMAL)
//        .nothingElse();
//
//    assertName("Navicula var. fasciata", "Navicula var. fasciata")
//        .infraSpecies("Navicula", null, Rank.VARIETY, "fasciata")
//        .type(NameType.INFORMAL)
//        .nothingElse();

    assertName("Polygonum spec.", "Polygonum spec.")
        .species("Polygonum", null)
        .type(NameType.INFORMAL)
        .nothingElse();

    assertName("Polygonum vulgaris ssp.", "Polygonum vulgaris subsp.")
        .infraSpecies("Polygonum", "vulgaris", Rank.SUBSPECIES, null)
        .type(NameType.INFORMAL)
        .nothingElse();

    assertName("Mesocricetus sp.", "Mesocricetus spec.")
        .species("Mesocricetus", null)
        .type(NameType.INFORMAL)
        .nothingElse();

   // dont treat these authorships as forms
    assertName("Dioscoreales Hooker f.", "Dioscoreales")
        .monomial("Dioscoreales", Rank.ORDER)
        .combAuthors(null, "Hooker f.")
        .nothingElse();

    assertName("Melastoma vacillans Blume var.", "Melastoma vacillans var.")
        .infraSpecies("Melastoma", "vacillans", Rank.VARIETY, null)
        .type(NameType.INFORMAL)
        .nothingElse();

    assertName("Lepidoptera Hooker", Rank.SPECIES, "Lepidoptera spec.")
        .species("Lepidoptera", null)
        .type(NameType.INFORMAL)
        .nothingElse();

    assertName("Lepidoptera alba DC.", Rank.SUBSPECIES, "Lepidoptera alba subsp.")
        .infraSpecies("Lepidoptera", "alba", Rank.SUBSPECIES, null)
        .type(NameType.INFORMAL)
        .nothingElse();
  }

  @Test
  public void rankMismatch() throws Exception {
    assertName("Polygonum", Rank.CULTIVAR, "Polygonum cv.")
        .cultivar("Polygonum", null)
        .type(INFORMAL)
        .nothingElse();

    assertName("Polygonum", Rank.SUBSPECIES, "Polygonum subsp.")
        .indet("Polygonum", null, Rank.SUBSPECIES)
        .nothingElse();

    assertName("Polygonum alba", Rank.GENUS, "Polygonum alba")
        .binomial("Polygonum", null, "alba", Rank.GENUS)
        .type(INFORMAL)
        .doubtful()
        .nothingElse();
  }

  /**
   * https://github.com/gbif/name-parser/issues/5
   */
  @Test
  public void vulpes() throws Exception {
    assertName("Vulpes vulpes sp. silaceus Miller, 1907", "Vulpes vulpes subsp. silaceus")
        .infraSpecies("Vulpes", "vulpes", Rank.SUBSPECIES, "silaceus")
        .combAuthors("1907", "Miller")
        .nothingElse();
  }

  @Test
  public void microbialRanks2() throws Exception {
    assertName("Puccinia graminis f. sp. avenae", "Puccinia graminis f.sp. avenae")
        .infraSpecies("Puccinia", "graminis", Rank.FORMA_SPECIALIS, "avenae")
        .code(NomCode.BACTERIAL)
        .nothingElse();
  }

  @Test
  public void chineseAuthors() throws Exception {
    assertName("Abaxisotima acuminata (Wang & Liu, 1996)", "Abaxisotima acuminata")
        .species("Abaxisotima", "acuminata")
        .basAuthors("1996", "Wang", "Liu")
        .nothingElse();

    assertName("Abaxisotima acuminata (Wang, Yuwen & Xian-wei Liu, 1996)", "Abaxisotima acuminata")
        .species("Abaxisotima", "acuminata")
        .basAuthors("1996", "Wang", "Yuwen", "Xian-wei Liu")
        .nothingElse();

    assertName("Abaxisotima bicolor (Liu, Xian-wei, Z. Zheng & G. Xi, 1991)", "Abaxisotima bicolor")
        .species("Abaxisotima", "bicolor")
        .basAuthors("1991", "Liu", "Xian-wei", "Z.Zheng", "G.Xi")
        .nothingElse();
  }

  /**
   * http://dev.gbif.org/issues/browse/POR-2454
   */
  @Test
  public void fungusNames() throws Exception {
    assertName("Merulius lacrimans (Wulfen : Fr.) Schum.", "Merulius lacrimans")
        .species("Merulius", "lacrimans")
        .combAuthors(null, "Schum.")
        .basAuthors(null, "Wulfen")
        .nothingElse();

    assertName("Merulius lacrimans (Wulfen) Schum. : Fr.", "Merulius lacrimans")
        .species("Merulius", "lacrimans")
        .combAuthors(null, "Schum.")
        .basAuthors(null, "Wulfen")
        .sanctAuthor("Fr.")
        .nothingElse();

    //assertParsedParts("", null, "Merulius", "lacrimans", null, null, "Schum.", null, "Wulfen : Fr.", null);
    //assertParsedParts("Aecidium berberidis Pers. ex J.F. Gmel.", null, "Aecidium", "berberidis", null, null, "Pers. ex J.F. Gmel.", null, null, null);
    //assertParsedParts("Roestelia penicillata (O.F. Müll.) Fr.", null, "Roestelia", "penicillata", null, null, "Fr.", null, "O.F. Müll.", null);
//
    //assertParsedParts("Mycosphaerella eryngii (Fr. Duby) ex Oudem., 1897", null, "Mycosphaerella", "eryngii", null, null, "ex Oudem.", "1897", "Fr. Duby", null);
    //assertParsedParts("Mycosphaerella eryngii (Fr.ex Duby) ex Oudem. 1897", null, "Mycosphaerella", "eryngii", null, null, "ex Oudem.", "1897", "Fr.ex Duby", null);
    //assertParsedParts("Mycosphaerella eryngii (Fr. ex Duby) Johanson ex Oudem. 1897", null, "Mycosphaerella", "eryngii", null, null, "Johanson ex Oudem.", "1897", "Fr. ex Duby", null);
  }

  @Test
  public void yearVariations() throws Exception {
    assertName("Deudorix epijarbas turbo Fruhstorfer, [1912]", "Deudorix epijarbas turbo")
          .infraSpecies("Deudorix", "epijarbas", Rank.INFRASPECIFIC_NAME, "turbo")
          .combAuthors("1912", "Fruhstorfer")
          .nothingElse();
  }

  @Test
  public void imprintYears() throws Exception {
    assertName("Ophidocampa tapacumae Ehrenberg, 1870, 1869", "Ophidocampa tapacumae")
        .species("Ophidocampa", "tapacumae")
        .combAuthors("1870", "Ehrenberg")
        .nothingElse();

    assertName("Brachyspira Hovind-Hougen, Birch-Andersen, Henrik-Nielsen, Orholm, Pedersen, Teglbjaerg & Thaysen, 1983, 1982", "Brachyspira")
        .monomial("Brachyspira")
        .combAuthors("1983", "Hovind-Hougen", "Birch-Andersen", "Henrik-Nielsen", "Orholm", "Pedersen", "Teglbjaerg", "Thaysen")
        .nothingElse();

    assertName("Gyrosigma angulatum var. gamma Griffith & Henfrey, 1860, 1856", "Gyrosigma angulatum var. gamma")
        .infraSpecies("Gyrosigma", "angulatum", Rank.VARIETY, "gamma")
        .combAuthors("1860", "Griffith", "Henfrey")
        .nothingElse();

    assertName("Ctenotus alacer Storr, 1970 [\"1969\"]", "Ctenotus alacer")
        .species("Ctenotus", "alacer")
        .combAuthors("1970", "Storr")
        .nothingElse();

    assertName("Ctenotus alacer Storr, 1970 (imprint 1969)", "Ctenotus alacer")
        .species("Ctenotus", "alacer")
        .combAuthors("1970", "Storr")
        .nothingElse();

    assertName("Ctenotus alacer Storr, 1887 (\"1886-1888\")", "Ctenotus alacer")
        .species("Ctenotus", "alacer")
        .combAuthors("1887", "Storr")
        .nothingElse();

    assertName("Melanargia halimede menetriesi Wagener, 1959 & 1961", "Melanargia halimede menetriesi")
        .infraSpecies("Melanargia", "halimede", Rank.INFRASPECIFIC_NAME, "menetriesi")
        .combAuthors("1959", "Wagener")
        .nothingElse();
  }

  @Test
  public void lowerCaseNames() throws Exception {
    assertName("abies alba Mill.", "Abies alba")
        .species("Abies", "alba")
        .combAuthors(null, "Mill.")
        .type(SCIENTIFIC)
        .nothingElse();
  }

  @Test
  public void manuscriptNames() throws Exception {
    assertName("Lepidoptera sp. JGP0404", "Lepidoptera spec.")
        .species("Lepidoptera", null)
        .type(INFORMAL)
        .remarks("sp.JGP0404")
        .nothingElse();

    assertName("Genoplesium vernalis D.L.Jones ms.", "Genoplesium vernalis")
        .species("Genoplesium", "vernalis")
        .combAuthors(null, "D.L.Jones")
        .type(INFORMAL)
        .nothingElse();

    assertName("Verticordia sp.1", "Verticordia spec.")
        .species("Verticordia", null)
        .type(INFORMAL)
        .remarks("sp.1")
        .nothingElse();

    assertName("Bryozoan indet. 1", "Bryozoan spec.")
        .species("Bryozoan", null)
        .type(INFORMAL)
        .remarks("indet.1")
        .nothingElse();

    assertName("Bryozoan sp. E", "Bryozoan spec.")
        .species("Bryozoan", null)
        .type(INFORMAL)
        .remarks("sp.E")
        .nothingElse();

    assertName("Prostanthera sp. Somersbey (B.J.Conn 4024)", "Prostanthera spec.")
        .species("Prostanthera", null)
        .type(INFORMAL)
        .remarks("sp.Somersbey(B.J.Conn 4024)")
        .nothingElse();
  }

  @Test
  public void unsupportedAuthors() throws Exception {
    assertName(" Anolis marmoratus girafus LAZELL 1964: 377", "Anolis marmoratus girafus")
        .infraSpecies("Anolis", "marmoratus", Rank.INFRASPECIFIC_NAME, "girafus")
        .combAuthors("1964", "Lazell")
        .partial(":377")
        .nothingElse();
  }

  // **************
  // HELPER METHODS
  // **************

  public boolean isViralName(String name) {
    try {
      parser.parse(name, null);
    } catch (UnparsableNameException e) {
      // swallow
      if (NameType.VIRUS == e.getType()) {
        return true;
      }
    }
    return false;
  }

  private void assertNoName(String name) {
    assertUnparsable(name, NO_NAME);
  }

  private void assertUnparsable(String name, NameType type) {
    assertUnparsableName(name, Rank.UNRANKED, type, name);
  }

  private void assertUnparsable(String name, Rank rank, NameType type) {
    assertUnparsableName(name, rank, type, name);
  }

  private void assertUnparsableName(String name, Rank rank, NameType type, String expectedName) {
    try {
      parser.parse(name, rank);
      fail("Expected "+name+" to be unparsable");

    } catch (UnparsableNameException ex) {
      assertEquals(type, ex.getType());
      assertEquals(expectedName, ex.getName());
    }
  }

  NameAssertion assertName(String rawName, String expectedCanonicalWithoutAuthors) throws UnparsableNameException {
    return assertName(rawName, null, expectedCanonicalWithoutAuthors);
  }

  NameAssertion assertName(String rawName, Rank rank, String expectedCanonicalWithoutAuthors) throws UnparsableNameException {
    ParsedName n = parser.parse(rawName, rank);
    assertEquals(expectedCanonicalWithoutAuthors, n.canonicalNameWithoutAuthorship());
    return new NameAssertion(n);
  }

  private BufferedReader resourceReader(String resourceFileName) throws UnsupportedEncodingException {
    return new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/" + resourceFileName), "UTF-8"));
  }

}