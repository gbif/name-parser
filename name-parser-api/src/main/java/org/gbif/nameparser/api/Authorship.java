package org.gbif.nameparser.api;

import com.google.common.collect.Lists;
import org.gbif.nameparser.util.NameFormatter;

import java.util.List;
import java.util.Objects;

/**
 * Authorship of the name (recombination) or basionym
 * including authors, ex authors and the year
 * but no in authors which are regarded as part of the publishedIn citation.
 *
 * The parsed authorship for basionyms does not include brackets.
 * Note that the sanctioning author for fungi is part of the ParsedName class.
 */
public class Authorship {

  /**
   * list of authors.
   */
  private List<String> authors = Lists.newArrayList();

  /**
   * list of authors excluding ex- authors
   */
  private List<String> exAuthors = Lists.newArrayList();

  /**
   * The year the combination or basionym was first published, usually the same as the publishedIn reference.
   * It is used for sorting names and ought to be populated even for botanical names which do not use it in the complete authorship string.
   */
  private String year;

  public List<String> getAuthors() {
    return authors;
  }

  public void setAuthors(List<String> authors) {
    this.authors = authors;
  }

  public List<String> getExAuthors() {
    return exAuthors;
  }

  public void setExAuthors(List<String> exAuthors) {
    this.exAuthors = exAuthors;
  }

  public String getYear() {
    return year;
  }

  public void setYear(String year) {
    this.year = year;
  }

  public boolean isEmpty() {
    return authors.isEmpty() && year == null;
  }

  public boolean exists() {
    return !authors.isEmpty() || year != null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Authorship that = (Authorship) o;
    return Objects.equals(authors, that.authors) &&
        Objects.equals(exAuthors, that.exAuthors) &&
        Objects.equals(year, that.year);
  }

  @Override
  public int hashCode() {
    return Objects.hash(authors, exAuthors, year);
  }

  /**
   * @return the full authorship string with ex authors and year
   */
  @Override
  public String toString() {
    if (exists()) {
      StringBuilder sb = new StringBuilder();
      NameFormatter.appendAuthorship(sb, this, true);
      return sb.toString();
    }
    return null;
  }
}
