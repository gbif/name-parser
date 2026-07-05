package org.gbif.nameparser.api;

public class CombinedAuthorship {

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


  public boolean hasCombinationAuthorship() {
    return combinationAuthorship != null && !combinationAuthorship.isEmpty();
  }

  public Authorship getCombinationAuthorship() {
    return combinationAuthorship;
  }

  public void setCombinationAuthorship(Authorship combinationAuthorship) {
    this.combinationAuthorship = combinationAuthorship;
  }

  public boolean hasBasionymAuthorship() {
    return basionymAuthorship != null && !basionymAuthorship.isEmpty();
  }

  public Authorship getBasionymAuthorship() {
    return basionymAuthorship;
  }

  public void setBasionymAuthorship(Authorship basionymAuthorship) {
    this.basionymAuthorship = basionymAuthorship;
  }

  public String getSanctioningAuthor() {
    return sanctioningAuthor;
  }

  public void setSanctioningAuthor(String sanctioningAuthor) {
    this.sanctioningAuthor = sanctioningAuthor;
  }

  /**
   * @return true if any kind of authorship exists
   */
  public boolean hasAuthorship() {
    return (combinationAuthorship != null && combinationAuthorship.exists()) || (basionymAuthorship != null && basionymAuthorship.exists());
  }
}
