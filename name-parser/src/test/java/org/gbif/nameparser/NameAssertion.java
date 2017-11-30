package org.gbif.nameparser;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.gbif.nameparser.api.NamePart;
import org.gbif.nameparser.api.ParsedName;
import org.gbif.nameparser.api.Rank;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * Convenience class to assert equality of parts of a ParsedName for test assertions.
 */
public class NameAssertion {
  private final ParsedName n;
  private Set<NP> tested = Sets.newHashSet();

  private enum NP {EPITHETS, STRAIN, CULTIVAR, CANDIDATE, NOTHO,
    AUTH, EXAUTH, BAS, EXBAS, SANCT, RANK,
    SENSU, NOMNOTE, REMARK, DOUBTFUL
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
            assertNull(n.getInfragenericEpithet());
            assertNull(n.getSpecificEpithet());
            assertNull(n.getInfraspecificEpithet());
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
          case NOTHO:
            assertNull(n.getNotho());
            break;
          case AUTH:
            assertNull(n.getAuthorship().getYear());
            assertTrue(n.getAuthorship().getAuthors().isEmpty());
            break;
          case EXAUTH:
            assertTrue(n.getAuthorship().getExAuthors().isEmpty());
            break;
          case BAS:
            assertNull(n.getBasionymAuthorship().getYear());
            assertTrue(n.getBasionymAuthorship().getAuthors().isEmpty());
            break;
          case EXBAS:
            assertTrue(n.getBasionymAuthorship().getExAuthors().isEmpty());
            break;
          case SANCT:
            assertNull(n.getSanctioningAuthor());
            break;
          case RANK:
            assertEquals(Rank.UNRANKED, n.getRank());
            break;
          case SENSU:
            assertNull(n.getSensu());
            break;
          case NOMNOTE:
            assertNull(n.getNomenclaturalNotes());
            break;
          case REMARK:
            assertNull(n.getRemarks());
            break;
          case DOUBTFUL:
            assertFalse(n.isDoubtful());
            break;
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
    assertNull(n.getStrain());
    return add(NP.EPITHETS, NP.RANK, NP.STRAIN, NP.CULTIVAR);
  }

  NameAssertion infraGeneric(String genus, Rank rank, String infraGeneric) {
    assertNull(n.getUninomial());
    assertEquals(genus, n.getGenus());
    assertEquals(infraGeneric, n.getInfragenericEpithet());
    assertNull(n.getSpecificEpithet());
    assertNull(n.getInfraspecificEpithet());
    assertEquals(rank, n.getRank());
    assertNull(n.getCultivarEpithet());
    assertNull(n.getStrain());
    return add(NP.EPITHETS, NP.RANK, NP.STRAIN, NP.CULTIVAR);
  }

  NameAssertion species(String genus, String epithet) {
    assertNull(n.getUninomial());
    assertEquals(genus, n.getGenus());
    assertNull(n.getInfragenericEpithet());
    assertEquals(epithet, n.getSpecificEpithet());
    assertNull(n.getInfraspecificEpithet());
    assertEquals(Rank.SPECIES, n.getRank());
    return add(NP.EPITHETS, NP.RANK);
  }

  NameAssertion infraSpecies(String genus, String epithet, Rank rank, String infraEpithet) {
    assertNull(n.getUninomial());
    assertEquals(genus, n.getGenus());
    assertNull(n.getInfragenericEpithet());
    assertEquals(epithet, n.getSpecificEpithet());
    assertEquals(infraEpithet, n.getInfraspecificEpithet());
    assertEquals(rank, n.getRank());
    return add(NP.EPITHETS, NP.RANK);
  }

  NameAssertion combAuthors(String year, String... authors) {
    assertEquals(year, n.getAuthorship().getYear());
    assertEquals(Lists.newArrayList(authors), n.getAuthorship().getAuthors());
    return add(NP.AUTH);
  }

  NameAssertion notho(NamePart notho) {
    assertEquals(notho, n.getNotho());
    return add(NP.NOTHO);
  }

  NameAssertion cultivar(String epithet) {
    assertEquals(epithet, n.getCultivarEpithet());
    return add(NP.CULTIVAR);
  }

  NameAssertion candidatus() {
    assertTrue(n.isCandidatus());
    return add(NP.CANDIDATE);
  }

  NameAssertion strain(String strain) {
    assertEquals(strain, n.getStrain());
    return add(NP.STRAIN);
  }

  NameAssertion sensu(String sensu) {
    assertEquals(sensu, n.getSensu());
    return add(NP.SENSU);
  }

  NameAssertion nomNote(String nomNote) {
    assertEquals(nomNote, n.getNomenclaturalNotes());
    return add(NP.NOMNOTE);
  }

  NameAssertion doubtful() {
    assertTrue(n.isDoubtful());
    return add(NP.DOUBTFUL);
  }

  NameAssertion sanctAuthor(String author) {
    assertEquals(author, n.getSanctioningAuthor());
    return add(NP.SANCT);
  }

  NameAssertion combExAuthors(String... authors) {
    assertEquals(Lists.newArrayList(authors), n.getAuthorship().getExAuthors());
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

}
