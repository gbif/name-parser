/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.nameparser.api;

import org.gbif.nameparser.util.NameFormatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.google.common.base.Strings;

/**
 * Authorship of the name (recombination) or basionym
 * including authors, ex authors and the year
 * but no in authors which are regarded as part of the publishedIn citation.
 * <p>
 * The parsed authorship for basionyms does not include brackets.
 * Note that the sanctioning author for fungi is part of the ParsedName class.
 */
public class Authorship {
  
  /**
   * list of authors.
   */
  private List<String> authors = new ArrayList<>();
  
  /**
   * list of authors excluding ex- authors
   */
  private List<String> exAuthors = new ArrayList<>();
  
  /**
   * The year the combination or basionym was first published, usually the same as the publishedIn reference.
   * It is used for sorting names and ought to be populated even for botanical names which do not use it in the complete authorship string.
   */
  private String year;

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

  public List<String> getAuthors() {
    return authors;
  }

  public void addAuthor(String author) {
    if (Strings.isNullOrEmpty(author)) {
      if (authors == null) authors = new ArrayList<>();
      authors.add(author);
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

  public List<String> getExAuthors() {
    return exAuthors;
  }

  public void addExAuthor(String author) {
    if (com.google.common.base.Strings.isNullOrEmpty(author)) {
      if (exAuthors == null) exAuthors = new ArrayList<>();
      exAuthors.add(author);
    }
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
    return (authors == null || authors.isEmpty()) && year == null;
  }
  
  public boolean exists() {
    return !isEmpty();
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
