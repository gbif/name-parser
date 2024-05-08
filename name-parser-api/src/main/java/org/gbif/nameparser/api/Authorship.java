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

import org.apache.commons.lang3.StringUtils;
import org.gbif.nameparser.util.NameFormatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Authorship of the name (recombination) or basionym
 * including authors and the year
 * but ex authors or in authors which are regarded as part of the publishedIn citation.
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
    if (StringUtils.isBlank(author)) {
      if (authors == null) authors = new ArrayList<>();
      authors.add(author);
    }
  }

  public void setAuthors(List<String> authors) {
    this.authors = authors;
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
        Objects.equals(year, that.year);
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(authors, year);
  }
  
  /**
   * @return the full authorship string with ex authors and year
   */
  @Override
  public String toString() {
    if (exists()) {
      StringBuilder sb = new StringBuilder();
      NameFormatter.appendAuthorship(sb, this, true, null);
      return sb.toString();
    }
    return null;
  }

}
