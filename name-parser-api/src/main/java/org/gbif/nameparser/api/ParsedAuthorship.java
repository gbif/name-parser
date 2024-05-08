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

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 *
 */
public class ParsedAuthorship {

  /**
   * Extinct dagger symbol found
   */
  protected boolean extinct;

  /**
   * Authorship with years of the name, but excluding any basionym authorship.
   * For binomials the combination authors.
   */
  private ExAuthorship combinationAuthorship = new ExAuthorship();
  
  /**
   * Basionym authorship with years of the name
   */
  private ExAuthorship basionymAuthorship = new ExAuthorship();

  /**
   * Emendation authorship with years used by bacterial code.
   */
  private Authorship emendAuthorship = new Authorship();

  /**
   * The sanctioning author for sanctioned fungal names.
   * Fr. or Pers.
   */
  private String sanctioningAuthor;

  /**
   * Taxonomic concept remarks of the name.
   * For example sensu Miller, sec. Pyle 2007, s.l., etc.
   */
  private String taxonomicNote;
  
  /**
   * Nomenclatural status remarks of the name.
   */
  private String nomenclaturalNote;

  /**
   * In reference stripped from the authorship
   */
  private String publishedIn;

  /**
   * Any additional unparsed string found at the end of the name.
   * Only ever set when state=PARTIAL
   */
  private String unparsed;

  /**
   * Indicates some doubts that this is a name of the given type.
   * Usually indicates the existance of unusual characters not normally found in scientific names.
   */
  private boolean doubtful;

  /**
   * Indicates a manuscript name identified by ined. or ms.
   * Can be either of type scientific name or informal
   */
  private boolean manuscript;

  private ParsedName.State state = ParsedName.State.NONE;
  
  private Set<String> warnings = new HashSet<>();

  /**
   * Copies all values from the given parsed authorship
   */
  public void copy(ParsedAuthorship pa) {
    combinationAuthorship = pa.combinationAuthorship;
    basionymAuthorship = pa.basionymAuthorship;
    emendAuthorship = pa.emendAuthorship;
    sanctioningAuthor = pa.sanctioningAuthor;
    taxonomicNote = pa.taxonomicNote;
    nomenclaturalNote = pa.nomenclaturalNote;
    unparsed = pa.unparsed;
    doubtful = pa.doubtful;
    manuscript = pa.manuscript;
    state = pa.state;
    warnings = pa.warnings;
    extinct = pa.extinct;
  }

  public boolean hasCombinationAuthorship() {
    return combinationAuthorship != null && !combinationAuthorship.isEmpty();
  }

  public ExAuthorship getCombinationAuthorship() {
    return combinationAuthorship;
  }
  
  public void setCombinationAuthorship(ExAuthorship combinationAuthorship) {
    this.combinationAuthorship = combinationAuthorship;
  }

  public boolean hasBasionymAuthorship() {
    return basionymAuthorship != null && !basionymAuthorship.isEmpty();
  }

  public ExAuthorship getBasionymAuthorship() {
    return basionymAuthorship;
  }
  
  public void setBasionymAuthorship(ExAuthorship basionymAuthorship) {
    this.basionymAuthorship = basionymAuthorship;
  }

  public boolean hasEmendAuthorship() {
    return emendAuthorship != null && !emendAuthorship.isEmpty();
  }
  public Authorship getEmendAuthorship() {
    return emendAuthorship;
  }

  public void setEmendAuthorship(Authorship emendAuthorship) {
    this.emendAuthorship = emendAuthorship;
  }

  public String getSanctioningAuthor() {
    return sanctioningAuthor;
  }
  
  public void setSanctioningAuthor(String sanctioningAuthor) {
    this.sanctioningAuthor = sanctioningAuthor;
  }

  public String getNomenclaturalNote() {
    return nomenclaturalNote;
  }
  
  public void setNomenclaturalNote(String nomenclaturalNote) {
    this.nomenclaturalNote = nomenclaturalNote;
  }
  
  public void addNomenclaturalNote(String note) {
    if (!StringUtils.isBlank(note)) {
      this.nomenclaturalNote = nomenclaturalNote == null ? note.trim() : nomenclaturalNote + " " + note.trim();
    }
  }

  public String getPublishedIn() {
    return publishedIn;
  }

  public void setPublishedIn(String publishedIn) {
    this.publishedIn = publishedIn;
  }

  public String getTaxonomicNote() {
    return taxonomicNote;
  }
  
  public void setTaxonomicNote(String taxonomicNote) {
    this.taxonomicNote = taxonomicNote;
  }
  
  public boolean isManuscript() {
    return manuscript;
  }
  
  public void setManuscript(boolean manuscript) {
    this.manuscript = manuscript;
  }
  
  public String getUnparsed() {
    return unparsed;
  }
  
  public void setUnparsed(String unparsed) {
    this.unparsed = unparsed;
  }
  
  public void addUnparsed(String unparsed) {
    if (!StringUtils.isBlank(unparsed)) {
      this.unparsed = this.unparsed == null ? unparsed : this.unparsed + unparsed;
    }
  }

  public ParsedName.State getState() {
    return state;
  }
  
  public void setState(ParsedName.State state) {
    this.state = state;
  }

  public boolean isDoubtful() {
    return doubtful;
  }
  
  public void setDoubtful(boolean doubtful) {
    this.doubtful = doubtful;
  }
  
  public Set<String> getWarnings() {
    return warnings;
  }
  
  public void addWarning(String... warnings) {
    for (String warn : warnings) {
      this.warnings.add(warn);
    }
  }

  /**
   * @return true if any kind of authorship exists
   */
  public boolean hasAuthorship() {
    return (combinationAuthorship != null && combinationAuthorship.exists())
            || (basionymAuthorship != null && basionymAuthorship.exists())
            || (emendAuthorship != null && emendAuthorship.exists());
  }

  public boolean isExtinct() {
    return extinct;
  }

  public void setExtinct(boolean extinct) {
    this.extinct = extinct;
  }

  /**
   * @See NameFormatter.authorshipComplete()
   */
  public String authorshipComplete(NomCode code) {
    return NameFormatter.authorshipComplete(this, code);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ParsedAuthorship)) return false;
    ParsedAuthorship that = (ParsedAuthorship) o;
    return extinct == that.extinct &&
        doubtful == that.doubtful &&
        manuscript == that.manuscript &&
        Objects.equals(combinationAuthorship, that.combinationAuthorship) &&
        Objects.equals(basionymAuthorship, that.basionymAuthorship) &&
        Objects.equals(emendAuthorship, that.emendAuthorship) &&
        Objects.equals(sanctioningAuthor, that.sanctioningAuthor) &&
        Objects.equals(taxonomicNote, that.taxonomicNote) &&
        Objects.equals(nomenclaturalNote, that.nomenclaturalNote) &&
        Objects.equals(publishedIn, that.publishedIn) &&
        Objects.equals(unparsed, that.unparsed) &&
        state == that.state &&
        Objects.equals(warnings, that.warnings);
  }

  @Override
  public int hashCode() {
    return Objects.hash(extinct, combinationAuthorship, basionymAuthorship, emendAuthorship, sanctioningAuthor, taxonomicNote, nomenclaturalNote, publishedIn, unparsed, doubtful, manuscript, state, warnings);
  }

  @Override
  public String toString() {
    return authorshipComplete(null);
  }
}
