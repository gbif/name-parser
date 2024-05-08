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
 * including authors, ex authors and the year
 * but no in authors which are regarded as part of the publishedIn citation.
 * <p>
 * The parsed authorship for basionyms does not include brackets.
 * Note that the sanctioning author for fungi is part of the ParsedName class.
 */
public class ExAuthorship extends Authorship {

  /**
   * list of authors excluding ex- authors
   */
  private List<String> exAuthors = new ArrayList<>();

  public static ExAuthorship authors(String... authors) {
    return yearAuthors(null, authors);
  }

  public static ExAuthorship yearAuthors(String year, String... authors) {
    List<String> authorList = authors == null ? null : new ArrayList<>(Arrays.asList(authors));
    return new ExAuthorship(authorList, year);
  }


  public ExAuthorship() {
  }

  public ExAuthorship(List<String> authors) {
    super(authors);
  }

  public ExAuthorship(List<String> authors, String year) {
    super(authors, year);
  }

  public ExAuthorship(List<String> authors, List<String> exAuthors, String year) {
    super(authors, year);
    this.exAuthors = exAuthors;
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
    if (StringUtils.isBlank(author)) {
      if (exAuthors == null) exAuthors = new ArrayList<>();
      exAuthors.add(author);
    }
  }
  public void setExAuthors(List<String> exAuthors) {
    this.exAuthors = exAuthors;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ExAuthorship)) return false;
    if (!super.equals(o)) return false;
    ExAuthorship that = (ExAuthorship) o;
    return Objects.equals(exAuthors, that.exAuthors);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), exAuthors);
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
