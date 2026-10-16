package org.gbif.nameparser.api;

import org.gbif.nameparser.util.NameFormatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Authorship of the name (recombination) or basionym
 * including authors, ex authors, in authors and the year.
 * <p>
 * The parsed authorship for basionyms does not include brackets.
 * A sanctioned fungal name or basionym carries its sanctioning author, e.g. {@code "(Wulfen : Fr.) Schum."}.
 */
public class Authorship {

  /**
   * list of authors.
   * Null until set, so an empty authorship allocates no list.
   */
  private List<String> authors;
  
  /**
   * list of ex-authors excluding ex itself
   * Author A ex Author B
   *   A = proposed the name (not validly published) → the ex-author
   *   B = validly published the name → the publishing/validating author
   * Null until set, as few names have ex-authors.
   */
  private List<String> exAuthors;

  /**
   * list of in-authors excluding in itself, the authors of the work that contains the publication of the name
   * Author A in Author B
   *   A = the author(s) of the name
   *   B = the author(s) of the work that A contributed the name to, e.g. "Natterer in Kuhl, 1817" or
   *       "Boucher ex DC. in Lam. &amp; DC."
   * A name is cited by A or by B depending on the code and on the source, so both refer to the same name.
   * The in-authors stay part of the {@link ParsedAuthorship#getPublishedIn() publishedIn} citation as well,
   * this is only an additional structured copy. A citation that is no author team, e.g. "Mill. in Gard. Dict. ed. 8",
   * has no in-authors.
   * Null until set, as almost no name has in-authors.
   */
  private List<String> inAuthors;

  /**
   * The year the combination or basionym was first published, usually the same as the publishedIn reference.
   * It is used for sorting names and ought to be populated even for botanical names which do not use it in the complete authorship string.
   */
  private String year;

  /**
   * The imprint or printing date — the year actually printed on the work, when it differs from the
   * actual publication year. It is cited after the actual year, which stays in {@link #getYear() year}
   * (e.g. {@code "Storr, 1970 [\"1969\"]"} or {@code "Storr, 1970 [1969]"} → year=1970, imprintYear="1969").
   * A bracketed year on its own is no imprint year: it is the actual year established from external
   * evidence (ICZN Recommendation 22A.2.3), so {@code "Cabanis [1851]"} → year=1851, imprintYear=null,
   * {@link #isBracketedYear() bracketedYear}=true.
   *
   * <p>See ICZN Article 22 for the citation conventions:
   * <a href="https://code.iczn.org/date-of-publication/article-22-citation-of-date/">code.iczn.org/date-of-publication/article-22-citation-of-date</a>.
   */
  private String imprintYear;

  /**
   * The {@link #getYear() year} is not given in the work itself but established from external evidence,
   * and is therefore cited in square brackets (ICZN Recommendation 22A.2.3): {@code "Westwood, [1851]"}
   * → year=1851, bracketedYear=true. Rendered by {@link NameFormatter} in square brackets again.
   * See <a href="https://code.iczn.org/date-of-publication/article-22-citation-of-date/">ICZN Article 22</a>.
   */
  private boolean bracketedYear;

  /**
   * The name or nomenclatural act was published anonymously. Any {@link #getAuthors() authors} are then attributed
   * from external evidence, not given in the work itself.
   * <p>
   * Rendered by {@link NameFormatter} per code: without authors as "Anon." (zoology, ICZN Recommendation 51D) or
   * "anon." (botany), with attributed authors in square brackets as ICZN Recommendation 51D asks for
   * (e.g. {@code "[Denis & Schiffermüller], 1775"}).
   * See <a href="https://code.iczn.org/authorship/article-51-citation-of-names-of-authors/">ICZN Article 51</a>.
   */
  private boolean anonymous;

  /**
   * The sanctioning author of a sanctioned fungal name (ICN Art. 15), Fries ("Fr.") or Persoon ("Pers."), cited
   * after a colon: {@code "Boletus edulis Bull. : Fr."}, and for a sanctioned basionym inside its brackets:
   * {@code "Merulius lacrimans (Wulfen : Fr.) Schum."}.
   * See <a href="https://www.iapt-taxon.org/nomen/pages/main/art_15.html">ICN Article 15</a> and Recommendation 50E.
   */
  private String sanctioningAuthor;

  public static Authorship authors(String... authors) {
    return yearAuthors(null, authors);
  }

  public static Authorship yearAuthors(String year, String... authors) {
    List<String> authorList = authors == null ? null : new ArrayList<>(Arrays.asList(authors));
    return new Authorship(authorList, year);
  }


  public Authorship() {
  }

  public Authorship(List<String> authors) {
    this.authors = authors;
  }

  public Authorship(List<String> authors, String year) {
    this.authors = authors;
    this.year = year;
  }

  public Authorship(List<String> authors, List<String> exAuthors, String year) {
    this.authors = authors;
    this.exAuthors = exAuthors;
    this.year = year;
  }

  /**
   * Returns {@code true} if the authors are null or an empty list, false otherwise
   */
  public boolean hasAuthors() {
    return authors != null && !authors.isEmpty();
  }

  /**
   * @return the authors or null if there are none, see {@link #hasAuthors()}
   */
  public List<String> getAuthors() {
    return authors;
  }

  /**
   * Adds a non blank author, creating the author list if it does not exist yet.
   */
  public void addAuthor(String author) {
    if (author != null && !author.isBlank()) {
      if (authors == null) authors = new ArrayList<>();
      authors.add(author);
    }
  }

  /**
   * Adds all non blank authors, creating the author list if it does not exist yet.
   */
  public void addAuthors(Collection<String> authors) {
    if (authors != null) {
      authors.forEach(this::addAuthor);
    }
  }

  public void setAuthors(List<String> authors) {
    this.authors = authors;
  }

  /**
   * Returns {@code true} if the ex authorship is null or an empty list, false otherwise
   */
  public boolean hasExAuthors() {
    return exAuthors != null && !exAuthors.isEmpty();
  }

  /**
   * @return the ex-authors or null if there are none, see {@link #hasExAuthors()}
   */
  public List<String> getExAuthors() {
    return exAuthors;
  }

  /**
   * Adds a non blank ex-author, creating the ex-author list if it does not exist yet.
   */
  public void addExAuthor(String author) {
    if (author != null && !author.isBlank()) {
      if (exAuthors == null) exAuthors = new ArrayList<>();
      exAuthors.add(author);
    }
  }

  /**
   * Adds all non blank ex-authors, creating the ex-author list if it does not exist yet.
   */
  public void addExAuthors(Collection<String> authors) {
    if (authors != null) {
      authors.forEach(this::addExAuthor);
    }
  }

  public void setExAuthors(List<String> exAuthors) {
    this.exAuthors = exAuthors;
  }

  /**
   * Returns {@code true} if the in authorship is null or an empty list, false otherwise
   */
  public boolean hasInAuthors() {
    return inAuthors != null && !inAuthors.isEmpty();
  }

  /**
   * @return the in-authors or null if there are none, see {@link #hasInAuthors()}
   */
  public List<String> getInAuthors() {
    return inAuthors;
  }

  /**
   * Adds a non blank in-author, creating the in-author list if it does not exist yet.
   */
  public void addInAuthor(String author) {
    if (author != null && !author.isBlank()) {
      if (inAuthors == null) inAuthors = new ArrayList<>();
      inAuthors.add(author);
    }
  }

  /**
   * Adds all non blank in-authors, creating the in-author list if it does not exist yet.
   */
  public void addInAuthors(Collection<String> authors) {
    if (authors != null) {
      authors.forEach(this::addInAuthor);
    }
  }

  public void setInAuthors(List<String> inAuthors) {
    this.inAuthors = inAuthors;
  }
  
  public String getYear() {
    return year;
  }
  
  public void setYear(String year) {
    this.year = year;
  }

  public String getImprintYear() {
    return imprintYear;
  }

  public void setImprintYear(String imprintYear) {
    this.imprintYear = imprintYear;
  }

  public boolean hasImprintYear() {
    return imprintYear != null;
  }

  /**
   * @return true if the year was established from external evidence and is cited in square brackets,
   *         see {@link #setBracketedYear(boolean)}
   */
  public boolean isBracketedYear() {
    return bracketedYear;
  }

  /**
   * @param bracketedYear true if the year is not given in the work itself but established from external
   *                      evidence, e.g. {@code "Westwood, [1851]"} (ICZN Recommendation 22A.2.3)
   */
  public void setBracketedYear(boolean bracketedYear) {
    this.bracketedYear = bracketedYear;
  }

  /**
   * @return true if the work was published anonymously, see {@link #setAnonymous(boolean)}
   */
  public boolean isAnonymous() {
    return anonymous;
  }

  /**
   * @param anonymous true if the work was published anonymously; any authors are then attributed from external
   *                  evidence, e.g. {@code "[Lightfoot], 1786"}
   */
  public void setAnonymous(boolean anonymous) {
    this.anonymous = anonymous;
  }

  /**
   * @return the sanctioning author (Fr. / Pers.) of a sanctioned fungal name, or null
   */
  public String getSanctioningAuthor() {
    return sanctioningAuthor;
  }

  public void setSanctioningAuthor(String sanctioningAuthor) {
    this.sanctioningAuthor = sanctioningAuthor;
  }

  public boolean hasSanctioningAuthor() {
    return sanctioningAuthor != null;
  }

  public boolean isEmpty() {
    return !anonymous && (authors == null || authors.isEmpty()) && year == null;
  }
  
  public boolean exists() {
    return !isEmpty();
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Authorship that = (Authorship) o;
    return Objects.equals(orEmpty(authors), orEmpty(that.authors)) &&
        Objects.equals(orEmpty(exAuthors), orEmpty(that.exAuthors)) &&
        Objects.equals(orEmpty(inAuthors), orEmpty(that.inAuthors)) &&
        Objects.equals(year, that.year) &&
        Objects.equals(imprintYear, that.imprintYear) &&
        bracketedYear == that.bracketedYear &&
        anonymous == that.anonymous &&
        Objects.equals(sanctioningAuthor, that.sanctioningAuthor);
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(orEmpty(authors), orEmpty(exAuthors), orEmpty(inAuthors), year, imprintYear, bracketedYear, anonymous, sanctioningAuthor);
  }

  /**
   * Author lists are null until set, so unset and empty ones must not tell two authorships apart.
   */
  private static List<String> orEmpty(List<String> list) {
    return list == null ? List.of() : list;
  }
  
  /**
   * @return the full authorship string with ex authors, in authors and year
   */
  @Override
  public String toString() {
    if (exists()) {
      StringBuilder sb = new StringBuilder();
      NameFormatter.appendAuthorship(sb, this,true, null);
      return sb.toString();
    }
    return null;
  }

}
