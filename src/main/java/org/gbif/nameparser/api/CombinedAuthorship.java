package org.gbif.nameparser.api;

import java.util.Objects;

public class CombinedAuthorship implements CombinedAuthorshipIF {

  /**
   * Authorship with years of the name, but excluding any basionym authorship.
   * For binomials the combination authors.
   */
  private Authorship combinationAuthorship = new Authorship();

  /**
   * Basionym authorship with years of the name
   */
  private Authorship basionymAuthorship = new Authorship();

  /**
   * The sanctioning author for sanctioned fungal names.
   * Fr. or Pers.
   */
  private String sanctioningAuthor;


  @Override
  public boolean hasCombinationAuthorship() {
    return combinationAuthorship != null && !combinationAuthorship.isEmpty();
  }

  @Override
  public Authorship getCombinationAuthorship() {
    return combinationAuthorship;
  }

  public void setCombinationAuthorship(Authorship combinationAuthorship) {
    this.combinationAuthorship = combinationAuthorship;
  }

  @Override
  public boolean hasBasionymAuthorship() {
    return basionymAuthorship != null && !basionymAuthorship.isEmpty();
  }

  @Override
  public Authorship getBasionymAuthorship() {
    return basionymAuthorship;
  }

  public void setBasionymAuthorship(Authorship basionymAuthorship) {
    this.basionymAuthorship = basionymAuthorship;
  }

  @Override
  public String getSanctioningAuthor() {
    return sanctioningAuthor;
  }

  public void setSanctioningAuthor(String sanctioningAuthor) {
    this.sanctioningAuthor = sanctioningAuthor;
  }

  /**
   * @return true if any kind of authorship exists
   */
  @Override
  public boolean hasAuthorship() {
    return (combinationAuthorship != null && combinationAuthorship.exists()) || (basionymAuthorship != null && basionymAuthorship.exists());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof CombinedAuthorship that)) return false;
    return Objects.equals(combinationAuthorship, that.combinationAuthorship)
        && Objects.equals(basionymAuthorship, that.basionymAuthorship)
        && Objects.equals(sanctioningAuthor, that.sanctioningAuthor);
  }

  @Override
  public int hashCode() {
    return Objects.hash(combinationAuthorship, basionymAuthorship, sanctioningAuthor);
  }
}
