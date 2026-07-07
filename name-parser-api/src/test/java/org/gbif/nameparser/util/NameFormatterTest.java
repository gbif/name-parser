package org.gbif.nameparser.util;

import org.gbif.nameparser.api.*;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 *
 */
public class NameFormatterTest {
  ParsedName pn;
  
  @Before
  public void init() {
    pn = new ParsedName();
  }
  
  
  @Test
  public void testFullAuthorship() throws Exception {
    assertNull(pn.authorshipComplete());

    pn.setCombinationAuthorship(Authorship.authors("L."));
    assertEquals("L.", pn.authorshipComplete());

    pn.setBasionymAuthorship(Authorship.authors("Bassier"));
    assertEquals("(Bassier) L.", pn.authorshipComplete());
    assertEquals("(Bassier) L.", pn.authorshipComplete());
    
    pn.getCombinationAuthorship().getAuthors().add("Rohe");
    assertEquals("(Bassier) L. & Rohe", pn.authorshipComplete());
    assertEquals("(Bassier) L. & Rohe", pn.authorshipComplete());
    
    pn.setSanctioningAuthor("Fr.");
    assertEquals("(Bassier) L. & Rohe : Fr.", pn.authorshipComplete());
  }
  
  @Test
  public void testFullAuthorshipSanctioning() throws Exception {
    pn.setCombinationAuthorship(Authorship.authors("L."));
    pn.setSanctioningAuthor("Pers.");
    assertEquals("L. : Pers.", pn.authorshipComplete());
  }

  /** Imprint year (now on the Authorship) renders in canonicalComplete but not canonical. */
  @Test
  public void imprintYearRendering() throws Exception {
    // imprint next to the combination year: "Storr, 1970 [1969]"
    pn.setGenus("Ctenotus");
    pn.setSpecificEpithet("alacer");
    pn.setRank(Rank.SPECIES);
    pn.setCode(NomCode.ZOOLOGICAL);
    Authorship comb = Authorship.authors("Storr");
    comb.setYear("1970");
    comb.setImprintYear("1969");
    pn.setCombinationAuthorship(comb);
    assertEquals("Ctenotus alacer Storr, 1970 [1969]", NameFormatter.canonicalComplete(pn));
    // the imprint year is part of the authorship, so it shows wherever the authorship shows
    assertEquals("Ctenotus alacer Storr, 1970 [1969]", NameFormatter.canonical(pn));
    assertEquals("Ctenotus alacer", NameFormatter.canonicalWithoutAuthorship(pn));

    // imprint inside the basionym brackets: "(Peters, 1876 [1877])"
    ParsedName bn = new ParsedName();
    bn.setGenus("Anomalopus");
    bn.setSpecificEpithet("truncatus");
    bn.setRank(Rank.SPECIES);
    bn.setCode(NomCode.ZOOLOGICAL);
    Authorship bas = Authorship.authors("Peters");
    bas.setYear("1876");
    bas.setImprintYear("1877");
    bn.setBasionymAuthorship(bas);
    assertEquals("Anomalopus truncatus (Peters, 1876 [1877])", NameFormatter.canonicalComplete(bn));
    assertEquals("Anomalopus truncatus (Peters, 1876 [1877])", NameFormatter.canonical(bn));
  }

  /** The genus author of an infrageneric name renders in canonicalComplete but not canonical. */
  @Test
  public void genericAuthorshipRendering() throws Exception {
    pn.setGenus("Cordia");
    pn.setInfragenericEpithet("Salimori");
    pn.setRank(Rank.SECTION_BOTANY);
    pn.setCode(NomCode.BOTANICAL);
    CombinedAuthorship generic = new CombinedAuthorship();
    generic.setBasionymAuthorship(Authorship.authors("Adans."));
    generic.setCombinationAuthorship(Authorship.authors("Kuntze"));
    pn.setGenericAuthorship(generic);
    assertEquals("Cordia (Adans.) Kuntze sect. Salimori", NameFormatter.canonicalComplete(pn));
    assertEquals("Cordia sect. Salimori", NameFormatter.canonical(pn));
    assertEquals("Cordia sect. Salimori", NameFormatter.canonicalWithoutAuthorship(pn));
  }

  /** The cultivar author follows the cultivar epithet; the species author precedes it (complete only). */
  @Test
  public void cultivarAuthorshipRendering() throws Exception {
    pn.setGenus("Acer");
    pn.setSpecificEpithet("campestre");
    pn.setCultivarEpithet("Elsrijk");
    pn.setRank(Rank.CULTIVAR);
    pn.setCode(NomCode.CULTIVARS);
    pn.setCombinationAuthorship(Authorship.authors("Broerse"));
    CombinedAuthorship specific = new CombinedAuthorship();
    specific.setCombinationAuthorship(Authorship.authors("L."));
    pn.setSpecificAuthorship(specific);
    assertEquals("Acer campestre L. 'Elsrijk' Broerse", NameFormatter.canonicalComplete(pn));
    assertEquals("Acer campestre 'Elsrijk' Broerse", NameFormatter.canonical(pn));
  }

  /** The species author of a below-species name renders in canonicalComplete but not canonical. */
  @Test
  public void specificAuthorshipRendering() throws Exception {
    pn.setGenus("Acer");
    pn.setSpecificEpithet("campestre");
    pn.setInfraspecificEpithet("hebecarpum");
    pn.setRank(Rank.SUBSPECIES);
    pn.setCode(NomCode.BOTANICAL);
    pn.setCombinationAuthorship(Authorship.authors("Bar"));
    CombinedAuthorship specific = new CombinedAuthorship();
    specific.setCombinationAuthorship(Authorship.authors("L."));
    pn.setSpecificAuthorship(specific);
    assertEquals("Acer campestre L. subsp. hebecarpum Bar", NameFormatter.canonicalComplete(pn));
    assertEquals("Acer campestre subsp. hebecarpum Bar", NameFormatter.canonical(pn));
  }
  
  @Test
  public void testCandidatus() throws Exception {
    pn.setUninomial("Endowatersipora");
    pn.setCandidatus(true);
    assertEquals("\"Candidatus Endowatersipora\"", pn.canonicalName());
    
    pn.setUninomial(null);
    pn.setGenus("Protochlamydia");
    pn.setSpecificEpithet("amoebophila");
    pn.setCandidatus(true);
    pn.setCombinationAuthorship(Authorship.yearAuthors("2005", "Collingro"));
    assertEquals("\"Candidatus Protochlamydia amoebophila\" Collingro, 2005", pn.canonicalName());
  }
  
  @Test
  public void inedNames() throws Exception {
    pn.setGenus("Acranthera");
    pn.setSpecificEpithet("virescens");
    pn.setBasionymAuthorship(Authorship.authors("Ridl."));
    pn.setCode(NomCode.BOTANICAL);
    pn.setManuscript(true);
    pn.setNomenclaturalNote("ined.");
    assertEquals("Acranthera virescens (Ridl.)", pn.canonicalName());
    assertEquals("Acranthera virescens (Ridl.), ined.", pn.canonicalNameComplete());
    assertEquals("<i>Acranthera</i> <i>virescens</i> (Ridl.), ined.", NameFormatter.canonicalCompleteHtml(pn));
    
    assertEquals("Acranthera virescens (Ridl.)", pn.canonicalName());
    assertEquals("Acranthera virescens (Ridl.), ined.", pn.canonicalNameComplete());
    assertEquals("<i>Acranthera</i> <i>virescens</i> (Ridl.), ined.", NameFormatter.canonicalCompleteHtml(pn));
  }

  @Test
  public void testCultivar() throws Exception {
    pn.setUninomial("Symphoricarpos");
    pn.setCode(NomCode.CULTIVARS);
    assertEquals("Symphoricarpos", pn.canonicalName());
    
    pn.setUninomial(null);
    pn.setGenus("Symphoricarpos");
    pn.setCultivarEpithet("mother of pearl");
    assertEquals("Symphoricarpos 'mother of pearl'", pn.canonicalName());
    
    pn.setRank(Rank.CULTIVAR);
    assertEquals("Symphoricarpos 'mother of pearl'", pn.canonicalName());
    
    pn.setGenus("Primula");
    pn.setCultivarEpithet("Border Auricula");
    assertEquals("Primula 'Border Auricula'", pn.canonicalName());
    
    pn.setRank(Rank.CULTIVAR_GROUP);
    assertEquals("Primula Border Auricula Group", pn.canonicalName());
    
    pn.setRank(Rank.GREX);
    assertEquals("Primula Border Auricula gx", pn.canonicalName());
    
    pn.setSpecificEpithet("boothii");
    assertEquals("Primula boothii Border Auricula gx", pn.canonicalName());
    
    pn.setRank(Rank.CULTIVAR_GROUP);
    assertEquals("Primula boothii Border Auricula Group", pn.canonicalName());
  }
  
  @Test
  @Ignore("currently expected to be handled externally")
  public void testCanonicalAscii() throws Exception {
    pn.setGenus("Abies");
    pn.setSpecificEpithet("vülgårîs");
    pn.setInfraspecificEpithet("æbiéñtø");
    
    assertName("Abies vulgaris aebiento", "Abies vülgårîs aebiéñtø");
  }
  
  @Test
  public void testUnparsableCanonical() throws Exception {
    pn.setType(NameType.PLACEHOLDER);
    pn.setState(ParsedName.State.NONE);
    assertNameNull();
  }
  
  @Test
  public void testUninomials() throws Exception {
    pn.setUninomial("Abies");
    assertEquals("Abies", pn.canonicalName());
    
    pn.setCombinationAuthorship(Authorship.yearAuthors("1877"));
    assertEquals("Abies 1877", pn.canonicalName());
    
    pn.getCombinationAuthorship().setAuthors(List.of("Mill."));
    assertEquals("Abies Mill., 1877", pn.canonicalName());
    
    pn.setNotho(NamePart.GENERIC);
    assertEquals("× Abies Mill., 1877", pn.canonicalName());
  }
  
  @Test
  public void testEtAl() throws Exception {
    pn.setGenus("Abies");
    pn.setSpecificEpithet("arnoldi");
    pn.setRank(Rank.SPECIES);
    assertEquals("Abies arnoldi", pn.canonicalNameComplete());

    pn.setCombinationAuthorship(Authorship.authors("Peter", "Fränzl", "Jung"));
    assertEquals("Abies arnoldi Peter, Fränzl & Jung", pn.canonicalNameComplete());
  
    pn.getCombinationAuthorship().getAuthors().add("al");
    assertEquals("Abies arnoldi Peter, Fränzl, Jung et al.", pn.canonicalNameComplete());
  
    pn.getCombinationAuthorship().getAuthors().add("al.");
    assertEquals("Abies arnoldi Peter, Fränzl, Jung, al et al.", pn.canonicalNameComplete());
  }
  
  @Test
  public void testIndet() throws Exception {
    pn.setGenus("Abies");
    pn.setSpecificEpithet("arnoldi");
    pn.setRank(Rank.SUBSPECIES);
    assertEquals("Abies arnoldi ssp.", pn.canonicalName());
  
    pn = new ParsedName();
    pn.setGenus("Ocymyrmex");
    pn.setInfragenericEpithet("Weitzaeckeri");
    pn.setInfraspecificEpithet("arnoldi");
    pn.setRank(Rank.SUBSPECIES);
    assertEquals("Ocymyrmex subsp. arnoldi", pn.canonicalName());
    pn = new ParsedName();
    
    pn.setNomenclaturalNote("sp.nov.");
    for (Rank r : Rank.values()) {
      pn.setRank(r);
      assertNull(pn.canonicalName());
      assertEquals("sp.nov.", pn.canonicalNameComplete());
    }
    
    pn.setGenus("Abies");
    pn.setRank(Rank.UNRANKED);
    assertEquals("Abies", pn.canonicalName());
    
    pn.setRank(Rank.SPECIES);
    assertEquals("Abies sp.", pn.canonicalName());
    
    pn.setSpecificEpithet("alba");
    assertEquals("Abies alba", pn.canonicalName());
  }
  
  @Test
  public void testAuthorship() throws Exception {
    pn.setBasionymAuthorship(Authorship.yearAuthors("1999", "Carl."));
    assertEquals("(Carl., 1999)", NameFormatter.authorshipComplete(pn));

    pn.setCombinationAuthorship(Authorship.yearAuthors("1887", "Mill."));
    assertEquals("(Carl., 1999) Mill., 1887", NameFormatter.authorshipComplete(pn));

    pn.setCombinationAuthorship(Authorship.yearAuthors("1887", "Mill.", "Döring", "Banki", "Robertson"));
    assertEquals("(Carl., 1999) Mill., Döring, Banki & Robertson, 1887", NameFormatter.authorshipComplete(pn));

    pn.setCode(NomCode.BACTERIAL);
    assertEquals("(Carl. 1999) Mill. et al. 1887", NameFormatter.authorshipComplete(pn));

    pn.setCombinationAuthorship(Authorship.yearAuthors("1887", "Mill.", "Döring"));
    assertEquals("(Carl. 1999) Mill. & Döring 1887", NameFormatter.authorshipComplete(pn));

    pn.setCombinationAuthorship(Authorship.yearAuthors("1887", "Mill."));
    assertEquals("(Carl. 1999) Mill. 1887", NameFormatter.authorshipComplete(pn));

    // Botanical names usually omit the year, but the ICN doesn't forbid it (some Fungi
    // groups cite it) — so when a year is present it is still rendered.
    pn.setCode(NomCode.BOTANICAL);
    assertEquals("(Carl., 1999) Mill., 1887", NameFormatter.authorshipComplete(pn));
  }
  
  @Test
  public void testInfraspecOnly() throws Exception {
    pn.setInfraspecificEpithet("vulgaris");
    assertEquals("vulgaris", NameFormatter.canonical(pn));
    assertEquals("vulgaris", NameFormatter.canonicalWithoutAuthorship(pn));
    
    pn.setSpecificEpithet("carrera");
    assertEquals("carrera vulgaris", NameFormatter.canonical(pn));
    assertEquals("carrera vulgaris", NameFormatter.canonicalWithoutAuthorship(pn));
    
    pn.setRank(Rank.SUBSPECIES);
    pn.setSpecificEpithet(null);
    pn.setCode(NomCode.BOTANICAL);
    assertEquals("subsp. vulgaris", NameFormatter.canonical(pn));
    assertEquals("subsp. vulgaris", NameFormatter.canonicalWithoutAuthorship(pn));
    
    pn.setSpecificEpithet("carrera");
    assertEquals("carrera subsp. vulgaris", NameFormatter.canonical(pn));
    assertEquals("carrera subsp. vulgaris", NameFormatter.canonicalWithoutAuthorship(pn));

    pn.setCombinationAuthorship(Authorship.yearAuthors("1887", "Mill."));
    assertEquals("carrera subsp. vulgaris Mill., 1887", NameFormatter.canonical(pn));
  }
  
  @Test
  public void testCanonicalNames() throws Exception {
    pn.setGenus("Abies");
    assertEquals("Abies", pn.canonicalName());
    
    pn.setSpecificEpithet("alba");
    assertEquals("Abies alba", pn.canonicalName());
    
    pn = new ParsedName();
    pn.setGenus("Abies");
    pn.setSpecificEpithet("alba");
    pn.setRank(Rank.VARIETY);
    pn.setCombinationAuthorship(Authorship.yearAuthors("1887", "Mill."));
    pn.setBasionymAuthorship(Authorship.authors("Carl."));
    pn.setNotho(NamePart.GENERIC);
    pn.setInfraspecificEpithet("alpina");
    pn.setTaxonomicNote("Döring");
    pn.setNomenclaturalNote("nom. illeg.");
    
    assertEquals("Abies alba alpina", NameFormatter.canonicalMinimal(pn));
    assertEquals("× Abies alba var. alpina", NameFormatter.canonicalWithoutAuthorship(pn));
    assertEquals("× Abies alba var. alpina (Carl.) Mill., 1887", NameFormatter.canonical(pn));
    assertEquals("× Abies alba var. alpina (Carl.) Mill., 1887 Döring, nom. illeg.", NameFormatter.canonicalComplete(pn));
  }
  
  @Test
  public void epithetQualifiers() throws Exception {
    pn.setGenus("Abies");
    pn.setSpecificEpithet("alba");
    pn.setEpithetQualifier(NamePart.SPECIFIC, "aff.");
    assertEquals("Abies alba", NameFormatter.canonicalMinimal(pn));
    assertEquals("Abies aff. alba", pn.canonicalName());
    assertEquals("Abies aff. alba", pn.canonicalNameComplete());
  
    pn.getEpithetQualifier().clear();
    pn.setEpithetQualifier(NamePart.INFRASPECIFIC, "cf.");
    pn.setInfraspecificEpithet("alpina");
    pn.setRank(Rank.VARIETY);
    pn.setCombinationAuthorship(Authorship.yearAuthors("1887", "Mill."));
    pn.setBasionymAuthorship(Authorship.authors("Carl."));
    pn.setNotho(NamePart.GENERIC);
    pn.setNomenclaturalNote("nom. illeg.");
    
    assertEquals("Abies alba alpina", NameFormatter.canonicalMinimal(pn));
    assertEquals("× Abies alba cf. var. alpina (Carl.) Mill., 1887", NameFormatter.canonical(pn));
    assertEquals("× Abies alba cf. var. alpina (Carl.) Mill., 1887, nom. illeg.", NameFormatter.canonicalComplete(pn));
  }
  
  @Test
  public void testCanonicalName() throws Exception {
    assertNull(pn.canonicalName());
    
    pn.setUninomial("Asteraceae");
    pn.setRank(Rank.FAMILY);
    assertEquals("Asteraceae", pn.canonicalName());
    
    pn.setUninomial("Abies");
    pn.setRank(Rank.GENUS);
    pn.setCombinationAuthorship(Authorship.authors("Mill."));
    assertEquals("Abies Mill.", pn.canonicalName());
    
    pn.setRank(Rank.UNRANKED);
    assertEquals("Abies Mill.", pn.canonicalName());
    
    pn.setUninomial(null);
    pn.setInfragenericEpithet("Pinoideae");
    assertEquals("Pinoideae Mill.", pn.canonicalName());
    
    pn.setGenus("Abies");
    pn.setRank(Rank.INFRAGENERIC_NAME);
    assertEquals("Abies infragen. Pinoideae Mill.", pn.canonicalName());
    
    pn.setRank(Rank.SUBGENUS);
    assertEquals("Abies subgen. Pinoideae Mill.", pn.canonicalName());
    
    pn.setCode(NomCode.ZOOLOGICAL);
    assertEquals("Abies (Pinoideae) Mill.", pn.canonicalName());
    
    pn.setInfragenericEpithet(null);
    pn.setSpecificEpithet("alba");
    assertEquals("Abies alba Mill.", pn.canonicalName());
    
    pn.setRank(Rank.SPECIES);
    assertEquals("Abies alba Mill.", pn.canonicalName());
    
    pn.setInfraspecificEpithet("alpina");
    assertEquals("Abies alba alpina Mill.", pn.canonicalName());
    
    pn.setRank(Rank.SUBSPECIES);
    assertEquals("Abies alba alpina Mill.", pn.canonicalName());
    
    pn.setRank(Rank.VARIETY);
    assertEquals("Abies alba var. alpina Mill.", pn.canonicalName());
    
    pn.setCode(NomCode.BOTANICAL);
    pn.setRank(Rank.SUBSPECIES);
    assertEquals("Abies alba subsp. alpina Mill.", pn.canonicalName());
    
    pn.setRank(Rank.VARIETY);
    assertEquals("Abies alba var. alpina Mill.", pn.canonicalName());
    
    pn.setRank(Rank.INFRASPECIFIC_NAME);
    assertEquals("Abies alba alpina Mill.", pn.canonicalName());
    
    pn.setNotho(NamePart.INFRASPECIFIC);
    assertEquals("Abies alba × alpina Mill.", pn.canonicalName());
    
    pn.setNotho(NamePart.GENERIC);
    assertEquals("× Abies alba alpina Mill.", pn.canonicalName());
  }
  
  @Test
  public void testUnparsable() throws Exception {
    for (NameType t : NameType.values()) {
      if (!t.isParsable()) {
        pn.setType(t);
        for (Rank r : Rank.values()) {
          pn.setRank(r);
          assertName(null, null);
        }
      }
    }
  }
  
  /**
   * https://github.com/Sp2000/colplus-backend/issues/478
   */
  @Test
  public void bacterialInfraspec() throws Exception {
    pn.setGenus("Spirulina");
    pn.setSpecificEpithet("subsalsa");
    pn.setInfraspecificEpithet("subsalsa");
    pn.setRank(Rank.INFRASPECIFIC_NAME);
    assertName(
        "Spirulina subsalsa subsalsa",
        "Spirulina subsalsa subsalsa"
    );
    pn.setCode(NomCode.BACTERIAL);
    assertName(
        "Spirulina subsalsa subsalsa",
        "Spirulina subsalsa subsalsa"
    );
  }
  
  /**
   * http://dev.gbif.org/issues/browse/POR-2624
   */
  @Test
  public void testSubgenus() throws Exception {
    // Brachyhypopomus (Odontohypopomus) Sullivan, Zuanon & Cox Fernandes, 2013
    pn.setGenus("Brachyhypopomus");
    pn.setInfragenericEpithet("Odontohypopomus");
    pn.setCombinationAuthorship(authorship("2013", "Sullivan", "Zuanon", "Cox Fernandes"));
    assertName(
        "Odontohypopomus",
        "Brachyhypopomus Odontohypopomus Sullivan, Zuanon & Cox Fernandes, 2013"
    );
    
    pn.setRank(Rank.INFRAGENERIC_NAME);
    assertName(
        "Odontohypopomus",
        "Brachyhypopomus infragen. Odontohypopomus Sullivan, Zuanon & Cox Fernandes, 2013"
    );
    
    // with given rank marker it is shown instead of brackets
    pn.setRank(Rank.SUBGENUS);
    assertName(
        "Odontohypopomus",
        "Brachyhypopomus subgen. Odontohypopomus Sullivan, Zuanon & Cox Fernandes, 2013"
    );
    
    // but not for zoological names
    pn.setCode(NomCode.ZOOLOGICAL);
    assertName(
        "Odontohypopomus",
        "Brachyhypopomus (Odontohypopomus) Sullivan, Zuanon & Cox Fernandes, 2013"
    );
    
    // Achillea sect. Ptarmica (Mill.) W.D.J.Koch
    pn = new ParsedName();
    pn.setCode(NomCode.BOTANICAL);
    pn.setGenus("Achillea");
    pn.setInfragenericEpithet("Ptarmica");
    pn.setCombinationAuthorship(Authorship.authors("W.D.J.Koch"));
    pn.setBasionymAuthorship(Authorship.authors("Mill."));
    assertName(
        "Ptarmica",
        "Achillea Ptarmica (Mill.) W.D.J.Koch"
    );
    
    pn.setRank(Rank.SECTION_BOTANY);
    assertName(
        "Ptarmica",
        "Achillea sect. Ptarmica (Mill.) W.D.J.Koch"
    );
    
    pn.setGenus(null);
    assertName(
        "Ptarmica",
        "sect. Ptarmica (Mill.) W.D.J.Koch"
    );
    
    pn.setRank(Rank.SUBGENUS);
    pn.setCode(NomCode.ZOOLOGICAL);
    assertName(
        "Ptarmica",
        "subgen. Ptarmica (Mill.) W.D.J.Koch"
    );
    
  }
  
  @Test
  public void testBuildName() throws Exception {
    pn.setUninomial("Pseudomonas");
    assertName("Pseudomonas");
    assertHtml("<i>Pseudomonas</i>");
    
    pn.setUninomial(null);
    pn.setGenus("Pseudomonas");
    pn.setSpecificEpithet("syringae");
    assertName("Pseudomonas syringae");
    assertHtml("<i>Pseudomonas</i> <i>syringae</i>");

    pn.setCombinationAuthorship(Authorship.authors("Van Hall"));
    assertName("Pseudomonas syringae", "Pseudomonas syringae Van Hall");
    assertHtml("<i>Pseudomonas</i> <i>syringae</i> Van Hall");
    
    pn.getCombinationAuthorship().setYear("1904");
    assertName("Pseudomonas syringae", "Pseudomonas syringae Van Hall, 1904");
    assertHtml("<i>Pseudomonas</i> <i>syringae</i> Van Hall, 1904");

    pn.setBasionymAuthorship(Authorship.authors("Carl."));
    assertName("Pseudomonas syringae", "Pseudomonas syringae (Carl.) Van Hall, 1904");
    assertHtml("<i>Pseudomonas</i> <i>syringae</i> (Carl.) Van Hall, 1904");
    
    pn.setRank(Rank.PATHOVAR);
    pn.setInfraspecificEpithet("aceris");
    pn.getBasionymAuthorship().getAuthors().clear();
    assertName("Pseudomonas syringae aceris", "Pseudomonas syringae pv. aceris Van Hall, 1904");
    assertHtml("<i>Pseudomonas</i> <i>syringae</i> pv. <i>aceris</i> Van Hall, 1904");
    
    pn.setPhrase("CFBP 2339");
    assertName("Pseudomonas syringae aceris", "Pseudomonas syringae pv. aceris Van Hall, 1904 CFBP 2339");
    assertHtml("<i>Pseudomonas</i> <i>syringae</i> pv. <i>aceris</i> Van Hall, 1904 CFBP 2339");
    
    pn.getCombinationAuthorship().setYear(null);
    pn.getCombinationAuthorship().getAuthors().clear();
    assertName("Pseudomonas syringae aceris", "Pseudomonas syringae pv. aceris CFBP 2339");
    
    pn.setTaxonomicNote("tax note");
    assertHtml("<i>Pseudomonas</i> <i>syringae</i> pv. <i>aceris</i> CFBP 2339 tax note");
    
    pn = new ParsedName();
    pn.setGenus("Abax");
    pn.setSpecificEpithet("carinatus");
    pn.setInfraspecificEpithet("carinatus");
    pn.setBasionymAuthorship(Authorship.yearAuthors("1812", "Duftschmid"));
    // Autonym authorship is the species author and is no longer suppressed (ICN/ICZN).
    // With no botanical code set this zoological-style autonym cites the author at the end.
    pn.setRank(Rank.UNRANKED);
    assertName("Abax carinatus carinatus", "Abax carinatus carinatus (Duftschmid, 1812)");

    pn.setRank(null);
    assertName("Abax carinatus carinatus", "Abax carinatus carinatus (Duftschmid, 1812)");

    pn.setInfraspecificEpithet("urinatus");
    assertName("Abax carinatus urinatus", "Abax carinatus urinatus (Duftschmid, 1812)");
    
    pn.setRank(null);
    assertName("Abax carinatus urinatus", "Abax carinatus urinatus (Duftschmid, 1812)");
    
    pn.setRank(Rank.SUBSPECIES);
    assertName("Abax carinatus urinatus", "Abax carinatus subsp. urinatus (Duftschmid, 1812)");

    // Botanical: subsp. rank marker shown; the basionym year is kept now that botanical
    // names render the year when one is present (ICN doesn't forbid it).
    pn.setCode(NomCode.BOTANICAL);
    assertName("Abax carinatus urinatus", "Abax carinatus subsp. urinatus (Duftschmid, 1812)");
    
    
    pn = new ParsedName();
    pn.setGenus("Polypodium");
    pn.setSpecificEpithet("vulgare");
    pn.setInfraspecificEpithet("mantoniae");
    pn.setBasionymAuthorship(Authorship.authors("Rothm."));
    pn.setCombinationAuthorship(Authorship.authors("Schidlay"));
    pn.setRank(Rank.SUBSPECIES);
    pn.setNotho(NamePart.INFRASPECIFIC);
    assertName(
        "Polypodium vulgare mantoniae",
        "Polypodium vulgare nothosubsp. mantoniae (Rothm.) Schidlay",
        "Polypodium vulgare nothosubsp. mantoniae (Rothm.) Schidlay");
  }


  @Test
  public void testHybridFormula() throws Exception {
    // Ophrys × varvarae Faller & Kreutz — genus and epithet italicised,
    // the hybrid marker and authorship are not.
    pn.setGenus("Ophrys");
    pn.setSpecificEpithet("varvarae");
    pn.setNotho(NamePart.SPECIFIC);
    pn.setCombinationAuthorship(Authorship.authors("Faller", "Kreutz"));

    assertEquals("Ophrys × varvarae Faller & Kreutz", NameFormatter.canonicalComplete(pn));
    assertEquals("<i>Ophrys</i> × <i>varvarae</i> Faller & Kreutz", NameFormatter.canonicalCompleteHtml(pn));
  }

  @Test
  public void testPhraseName() throws Exception {
    pn.setGenus("Acacia");
    pn.setRank(Rank.SPECIES);
    pn.setPhrase("Bigge Island (A.A. Mitchell 3436)");
    pn.setType(NameType.INFORMAL);

    assertEquals("Acacia sp. Bigge Island (A.A. Mitchell 3436)", NameFormatter.canonical(pn));
    assertEquals("Acacia", NameFormatter.canonicalMinimal(pn));
    assertEquals("Acacia sp. Bigge Island (A.A. Mitchell 3436)", NameFormatter.canonicalWithoutAuthorship(pn));
    assertEquals("<i>Acacia</i> sp. Bigge Island (A.A. Mitchell 3436)", NameFormatter.canonicalCompleteHtml(pn));
  }

  private Authorship authorship(String year, String... authors) {
    Authorship a = new Authorship();
    a.setYear(year);
    if (authors != null) {
      a.setAuthors(new ArrayList<>(Arrays.asList(authors)));
    }
    return a;
  }
  
  private void assertNameNull() {
    assertName(null, null, null);
  }
  
  /**
   * assert all build name methods return the same string
   */
  private void assertName(String name) {
    assertName(name, name, name);
  }
  
  private void assertHtml(String html) {
    assertEquals("wrong html name", html, NameFormatter.canonicalCompleteHtml(pn));
  }
  
  /**
   * assert a minimal trinomen and a canonical & complete name being the same string
   */
  private void assertName(String trinomen, String canonical) {
    assertName(trinomen, canonical, canonical);
  }
  
  private void assertName(String trinomen, String canonical, String complete) {
    assertEquals("wrong trinomen", trinomen, NameFormatter.canonicalMinimal(pn));
    assertEquals("wrong canonical", canonical, NameFormatter.canonical(pn));
    assertEquals("wrong canonicalComplete", complete, NameFormatter.canonicalComplete(pn));
  }
}