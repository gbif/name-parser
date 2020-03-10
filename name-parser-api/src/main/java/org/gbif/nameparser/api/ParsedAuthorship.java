package org.gbif.nameparser.api;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.gbif.nameparser.util.NameFormatter;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.gbif.nameparser.util.NameFormatter.HYBRID_MARKER;

/**
 *
 */
public class ParsedAuthorship {

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

  /**
   * Nomenclatural status remarks of the name.
   */
  private String taxonomicNote;
  
  /**
   * Nomenclatural status remarks of the name.
   */
  private String nomenclaturalNotes;
  
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
  
  private List<String> warnings = Lists.newArrayList();
  
  
  public Authorship getCombinationAuthorship() {
    return combinationAuthorship;
  }
  
  public void setCombinationAuthorship(Authorship combinationAuthorship) {
    this.combinationAuthorship = combinationAuthorship;
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

  public String getNomenclaturalNotes() {
    return nomenclaturalNotes;
  }
  
  public void setNomenclaturalNotes(String nomenclaturalNotes) {
    this.nomenclaturalNotes = nomenclaturalNotes;
  }
  
  public void addNomenclaturalNote(String note) {
    if (!StringUtils.isBlank(note)) {
      this.nomenclaturalNotes = nomenclaturalNotes == null ? note.trim() : nomenclaturalNotes + " " + note.trim();
    }
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
  
  public List<String> getWarnings() {
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
    return combinationAuthorship.exists() || basionymAuthorship.exists();
  }

  /**
   * @See NameFormatter.authorshipComplete()
   */
  public String authorshipComplete() {
    return NameFormatter.authorshipComplete(this);
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ParsedAuthorship that = (ParsedAuthorship) o;
    return doubtful == that.doubtful &&
        state == that.state &&
        Objects.equals(combinationAuthorship, that.combinationAuthorship) &&
        Objects.equals(basionymAuthorship, that.basionymAuthorship) &&
        Objects.equals(sanctioningAuthor, that.sanctioningAuthor) &&
        Objects.equals(taxonomicNote, that.taxonomicNote) &&
        Objects.equals(nomenclaturalNotes, that.nomenclaturalNotes) &&
        Objects.equals(unparsed, that.unparsed) &&
        manuscript == that.manuscript &&
        Objects.equals(warnings, that.warnings);
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(combinationAuthorship, basionymAuthorship, sanctioningAuthor, taxonomicNote, nomenclaturalNotes, unparsed,
        doubtful, manuscript, state, warnings);
  }
  
  @Override
  public String toString() {
    return authorshipComplete();
  }
}
