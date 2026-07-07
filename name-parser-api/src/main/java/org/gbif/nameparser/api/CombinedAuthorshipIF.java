package org.gbif.nameparser.api;

/**
 * A name's combined authorship — its basionym and combination {@link Authorship}s plus any
 * sanctioning author — as consumed by {@link org.gbif.nameparser.util.NameFormatter}.
 *
 * <p>Implemented by {@link CombinedAuthorship} (and therefore by {@link ParsedAuthorship} and
 * {@link ParsedName}); other models can implement it to be rendered by the formatter directly.
 */
public interface CombinedAuthorshipIF {

  /**
   * @return the combination authorship — for binomials the combination authors.
   */
  Authorship getCombinationAuthorship();

  /**
   * @return the basionym authorship.
   */
  Authorship getBasionymAuthorship();

  /**
   * @return the sanctioning author for sanctioned fungal names (Fr. / Pers.), or null.
   */
  String getSanctioningAuthor();

  default boolean hasCombinationAuthorship() {
    Authorship a = getCombinationAuthorship();
    return a != null && !a.isEmpty();
  }

  default boolean hasBasionymAuthorship() {
    Authorship a = getBasionymAuthorship();
    return a != null && !a.isEmpty();
  }

  /**
   * @return true if any kind of authorship exists
   */
  default boolean hasAuthorship() {
    Authorship c = getCombinationAuthorship();
    Authorship b = getBasionymAuthorship();
    return (c != null && c.exists()) || (b != null && b.exists());
  }
}
