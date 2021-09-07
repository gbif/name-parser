package org.gbif.nameparser;

import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.gbif.nameparser.api.*;

import static org.gbif.nameparser.api.NomCode.BACTERIAL;
import static org.gbif.nameparser.api.NomCode.BOTANICAL;
import static org.junit.Assert.*;

/**
 * Convenience class to assert equality of parts of a ParsedName for test assertions.
 */
public class NameAssertion {
  private final ParsedName n;
  private Set<NP> tested = Sets.newHashSet();
  
  private enum NP {
    TYPE,
    EPITHETS,
    INFRAGEN,
    STRAIN,
    CULTIVAR,
    EXTINCT,
    CANDIDATE,
    NOTHO,
    AUTH,
    EXAUTH,
    BAS,
    EXBAS,
    SANCT,
    RANK,
    TAXNOTE,
    NOMNOTE,
    PUBLISHEDIN,
    DOUBTFUL,
    STATE,
    CODE,
    REMAINS,
    WARNING,
    MANUSCRIPT,
    PHRASE,
    QUALIFIERS
  }
  
  public NameAssertion(ParsedName n) {
    this.n = n;
  }
  
  void nothingElse() {
    for (NP p : NP.values()) {
      if (!tested.contains(p)) {
        switch (p) {
          case EPITHETS:
            assertNull(n.getGenus());
            assertNull(n.getSpecificEpithet());
            assertNull(n.getInfraspecificEpithet());
            break;
          case INFRAGEN:
            assertNull(n.getInfragenericEpithet());
            break;
          case STRAIN:
            assertNull(n.getStrain());
            break;
          case CULTIVAR:
            assertNull(n.getCultivarEpithet());
            break;
          case CANDIDATE:
            assertFalse(n.isCandidatus());
            break;
          case EXTINCT:
            assertFalse(n.isExtinct());
            break;
          case NOTHO:
            assertNull(n.getNotho());
            break;
          case AUTH:
            if (n.getCombinationAuthorship() != null) {
              assertNull(n.getCombinationAuthorship().getYear());
              assertFalse(n.getCombinationAuthorship().hasAuthors());
            }
            break;
          case EXAUTH:
            if (n.getCombinationAuthorship() != null) {
              assertFalse(n.getCombinationAuthorship().hasExAuthors());
            }
            break;
          case BAS:
            if (n.getBasionymAuthorship() != null) {
              assertNull(n.getBasionymAuthorship().getYear());
              assertFalse(n.getBasionymAuthorship().hasAuthors());
            }
            break;
          case EXBAS:
            if (n.getBasionymAuthorship() != null) {
              assertFalse(n.getBasionymAuthorship().hasExAuthors());
            }
            break;
          case SANCT:
            assertNull(n.getSanctioningAuthor());
            break;
          case RANK:
            assertEquals(Rank.UNRANKED, n.getRank());
            break;
          case TAXNOTE:
            assertNull(n.getTaxonomicNote());
            break;
          case NOMNOTE:
            assertNull(n.getNomenclaturalNote());
            break;
          case PUBLISHEDIN:
            assertNull(n.getPublishedIn());
            break;
          case DOUBTFUL:
            assertFalse(n.isDoubtful());
            break;
          case STATE:
            assertEquals(ParsedName.State.COMPLETE, n.getState());
            break;
          case TYPE:
            assertEquals(NameType.SCIENTIFIC, n.getType());
            break;
          case CODE:
            assertNull(n.getCode());
            break;
          case REMAINS:
            assertNull(n.getUnparsed());
            break;
          case WARNING:
            String warn = Joiner.on("; ").join(n.getWarnings());
            assertTrue("contains warnings: "+warn, n.getWarnings().isEmpty());
            break;
          case MANUSCRIPT:
            assertFalse(n.isManuscript());
            break;
          case PHRASE:
            assertFalse(n.isPhraseName());
            assertNull(n.getVoucher());
            assertNull(n.getNominatingParty());
            break;
          case QUALIFIERS:
            assertTrue(n.getEpithetQualifier() == null || n.getEpithetQualifier().isEmpty());
            
        }
      }
    }
  }
  
  private NameAssertion add(NP... props) {
    for (NP p : props) {
      tested.add(p);
    }
    return this;
  }
  
  NameAssertion monomial(String monomial) {
    return monomial(monomial, Rank.UNRANKED);
  }
  
  NameAssertion monomial(String monomial, Rank rank) {
    assertEquals(monomial, n.getUninomial());
    assertEquals(rank, n.getRank());
    assertNull(n.getGenus());
    assertNull(n.getInfragenericEpithet());
    assertNull(n.getSpecificEpithet());
    assertNull(n.getInfraspecificEpithet());
    assertNull(n.getCultivarEpithet());
    return add(NP.EPITHETS, NP.RANK, NP.CULTIVAR);
  }
  
  NameAssertion infraGeneric(String genus, Rank rank, String infraGeneric) {
    assertNull(n.getUninomial());
    assertEquals(genus, n.getGenus());
    assertEquals(infraGeneric, n.getInfragenericEpithet());
    assertNull(n.getSpecificEpithet());
    assertNull(n.getInfraspecificEpithet());
    assertEquals(rank, n.getRank());
    assertNull(n.getCultivarEpithet());
    return add(NP.EPITHETS, NP.INFRAGEN, NP.RANK, NP.CULTIVAR);
  }
  
  NameAssertion infraGeneric(String infraGeneric) {
    assertEquals(infraGeneric, n.getInfragenericEpithet());
    return add(NP.INFRAGEN);
  }
  
  NameAssertion species(String genus, String epithet) {
    return binomial(genus, null, epithet, Rank.SPECIES);
  }
  
  NameAssertion binomial(String genus, String infraGeneric, String epithet, Rank rank) {
    assertNull(n.getUninomial());
    assertEquals(genus, n.getGenus());
    assertEquals(infraGeneric, n.getInfragenericEpithet());
    assertEquals(epithet, n.getSpecificEpithet());
    assertNull(n.getInfraspecificEpithet());
    assertEquals(rank, n.getRank());
    return add(NP.EPITHETS, NP.INFRAGEN, NP.RANK);
  }
  
  NameAssertion infraSpecies(String genus, String epithet, Rank rank, String infraEpithet) {
    assertNull(n.getUninomial());
    assertEquals(genus, n.getGenus());
    assertEquals(epithet, n.getSpecificEpithet());
    assertEquals(infraEpithet, n.getInfraspecificEpithet());
    assertEquals(rank, n.getRank());
    return add(NP.EPITHETS, NP.RANK);
  }
  
  NameAssertion indet(String genus, String epithet, Rank rank) {
    assertNull(n.getUninomial());
    assertEquals(genus, n.getGenus());
    assertEquals(epithet, n.getSpecificEpithet());
    assertNull(n.getInfraspecificEpithet());
    assertEquals(rank, n.getRank());
    assertEquals(NameType.INFORMAL, n.getType());
    assertTrue(n.isIndetermined());
    return add(NP.EPITHETS, NP.RANK, NP.TYPE);
  }
  
  NameAssertion combAuthors(String year, String... authors) {
    assertEquals(year, n.getCombinationAuthorship().getYear());
    assertEquals(Lists.newArrayList(authors), n.getCombinationAuthorship().getAuthors());
    return add(NP.AUTH);
  }
  
  NameAssertion autonym() {
    assertTrue(n.isAutonym());
    return this;
  }
  
  NameAssertion manuscript() {
    assertTrue(n.isManuscript());
    return add(NP.MANUSCRIPT);
  }
  
  NameAssertion type(NameType type) {
    assertEquals(type, n.getType());
    return add(NP.TYPE);
  }
  
  NameAssertion notho(NamePart notho) {
    assertEquals(notho, n.getNotho());
    return add(NP.NOTHO);
  }
  
  NameAssertion warning(String... warnings) {
    assertEquals(Sets.newHashSet(warnings), n.getWarnings());
    return add(NP.WARNING);
  }

  NameAssertion partial(String unparsed) {
    assertEquals(ParsedName.State.PARTIAL, n.getState());
    assertEquals(unparsed, n.getUnparsed());
    return add(NP.REMAINS, NP.STATE);
  }
  
  NameAssertion cultivar(String genus, String cultivar) {
    return cultivar(genus, null, Rank.CULTIVAR, cultivar);
  }
  
  NameAssertion cultivar(String genus, Rank rank, String cultivar) {
    return cultivar(genus, null, rank, cultivar);
  }
  
  NameAssertion cultivar(String genus, String species, String cultivar) {
    return cultivar(genus, species, Rank.CULTIVAR, cultivar);
  }
  
  NameAssertion cultivar(String genus, String species, Rank rank, String cultivar) {
    assertNull(n.getUninomial());
    assertEquals(genus, n.getGenus());
    assertEquals(species, n.getSpecificEpithet());
    assertNull(n.getInfragenericEpithet());
    assertNull(n.getInfraspecificEpithet());
    assertEquals(cultivar, n.getCultivarEpithet());
    assertEquals(rank, n.getRank());
    assertEquals(NomCode.CULTIVARS, n.getCode());
    return add(NP.EPITHETS, NP.RANK, NP.CULTIVAR, NP.CODE);
  }

  NameAssertion phrase(String genus, String phrase, Rank rank, String voucher, String nominatingParty) {
    assertNull(n.getUninomial());
    assertEquals(genus, n.getGenus());
    assertNull(n.getCultivarEpithet());
    assertEquals(phrase, n.getStrain());
    assertEquals(voucher, n.getVoucher());
    assertEquals(nominatingParty, n.getNominatingParty());
    assertTrue(n.isPhraseName());
    assertEquals(rank, n.getRank());
    assertEquals(NameType.INFORMAL, n.getType());
    return add(NP.EPITHETS, NP.RANK, NP.CULTIVAR, NP.TYPE, NP.STRAIN, NP.PHRASE);
  }

  NameAssertion code(NomCode code) {
    assertEquals(code, n.getCode());
    return add(NP.CODE);
  }

  NameAssertion extinct() {
    assertTrue(n.isExtinct());
    return add(NP.EXTINCT);
  }

  NameAssertion candidatus() {
    assertTrue(n.isCandidatus());
    assertEquals(BACTERIAL, n.getCode());
    return add(NP.CANDIDATE, NP.CODE);
  }
  
  NameAssertion strain(String strain) {
    assertEquals(strain, n.getStrain());
    return add(NP.STRAIN);
  }
  
  NameAssertion sensu(String sensu) {
    assertEquals(sensu, n.getTaxonomicNote());
    return add(NP.TAXNOTE);
  }

  NameAssertion publishedIn(String publishedIn) {
    assertEquals(publishedIn, n.getPublishedIn());
    return add(NP.PUBLISHEDIN);
  }

  NameAssertion nomNote(String nomNote) {
    assertEquals(nomNote, n.getNomenclaturalNote());
    return add(NP.NOMNOTE);
  }
  
  NameAssertion doubtful() {
    assertTrue(n.isDoubtful());
    return add(NP.DOUBTFUL);
  }
  
  NameAssertion rank(Rank rank) {
    assertEquals(rank, n.getRank());
    return add(NP.RANK);
  }
  
  NameAssertion state(ParsedName.State state) {
    assertEquals(state, n.getState());
    return add(NP.STATE);
  }
  
  NameAssertion sanctAuthor(String author) {
    assertEquals(author, n.getSanctioningAuthor());
    assertEquals(BOTANICAL, n.getCode());
    return add(NP.SANCT, NP.CODE);
  }
  
  NameAssertion combExAuthors(String... authors) {
    assertEquals(Lists.newArrayList(authors), n.getCombinationAuthorship().getExAuthors());
    return add(NP.EXAUTH);
  }
  
  NameAssertion basAuthors(String year, String... authors) {
    assertEquals(year, n.getBasionymAuthorship().getYear());
    assertEquals(Lists.newArrayList(authors), n.getBasionymAuthorship().getAuthors());
    return add(NP.BAS);
  }
  
  NameAssertion basExAuthors(String year, String... authors) {
    assertEquals(Lists.newArrayList(authors), n.getBasionymAuthorship().getExAuthors());
    return add(NP.EXBAS);
  }
  
  /**
   * @param partQuals list of part/value pairs
   */
  NameAssertion qualifiers(Object... partQuals) {
    for (int idx = 0; idx < partQuals.length; idx+=2) {
      NamePart part = (NamePart) partQuals[idx];
      String qualifier = (String) partQuals[idx+1];
      assertEquals(qualifier, n.getEpithetQualifier().get(part));
    }
    assertEquals(partQuals.length/2, n.getEpithetQualifier().size());
    return add(NP.QUALIFIERS);
  }
  
}
