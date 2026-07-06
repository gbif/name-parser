package org.gbif.nameparser.api;

import org.apache.commons.lang3.StringUtils;
import org.gbif.nameparser.util.NameFormatter;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 *
 */
public class ParsedAuthorship extends CombinedAuthorship {

  /**
   * Extinct dagger symbol found
   */
  protected boolean extinct;

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
   * The publication year extracted from {@link #publishedIn}, if the reference carries one.
   * The year is left in {@link #publishedIn} verbatim; this is only an additional structured copy.
   */
  private Integer publishedInYear;

  /**
   * The exact page of the in reference stripped from the authorship
   */
  private String publishedInPage;

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

  public ParsedAuthorship() {
  }

  public ParsedAuthorship(ParsedAuthorship pa) {
    copy(pa);
  }

  /**
   * Copies all values from the given parsed authorship
   */
  public void copy(ParsedAuthorship pa) {
    setCombinationAuthorship(pa.getCombinationAuthorship());
    setBasionymAuthorship(pa.getBasionymAuthorship());
    setSanctioningAuthor(pa.getSanctioningAuthor());
    taxonomicNote = pa.getTaxonomicNote();
    nomenclaturalNote = pa.getNomenclaturalNote();
    publishedIn = pa.getPublishedIn();
    publishedInYear = pa.getPublishedInYear();
    publishedInPage = pa.getPublishedInPage();
    unparsed = pa.getUnparsed();
    doubtful = pa.doubtful;
    manuscript = pa.manuscript;
    state = pa.state;
    warnings = pa.warnings;
    extinct = pa.extinct;
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

  /**
   * Sets the publishedIn reference and, in addition, extracts its publication year into
   * {@link #publishedInYear} (the year stays in the reference string verbatim). When several
   * year-shaped numbers are present the last one is taken — publication references list page
   * numbers (which can look like years) before the trailing year.
   */
  public void setPublishedIn(String publishedIn) {
    this.publishedIn = publishedIn;
    this.publishedInYear = extractYear(publishedIn);
  }

  public Integer getPublishedInYear() {
    return publishedInYear;
  }

  public void setPublishedInYear(Integer publishedInYear) {
    this.publishedInYear = publishedInYear;
  }

  /** 4-digit year in the range 1500–2100, standing as its own token. */
  private static final java.util.regex.Pattern PUBLISHED_IN_YEAR =
      java.util.regex.Pattern.compile("\\b(1[5-9]\\d{2}|20\\d{2}|2100)\\b");

  private static Integer extractYear(String publishedIn) {
    if (publishedIn == null) return null;
    java.util.regex.Matcher m = PUBLISHED_IN_YEAR.matcher(publishedIn);
    Integer year = null;
    while (m.find()) {
      year = Integer.valueOf(m.group(1)); // keep the last match — the trailing year
    }
    return year;
  }

  public String getPublishedInPage() {
    return publishedInPage;
  }

  public void setPublishedInPage(String publishedInPage) {
    this.publishedInPage = publishedInPage;
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
    if (!(o instanceof ParsedAuthorship that)) return false;

    return extinct == that.extinct &&
        doubtful == that.doubtful &&
        manuscript == that.manuscript &&
        Objects.equals(taxonomicNote, that.taxonomicNote) &&
        Objects.equals(nomenclaturalNote, that.nomenclaturalNote) &&
        Objects.equals(publishedIn, that.publishedIn) &&
        Objects.equals(publishedInYear, that.publishedInYear) &&
        Objects.equals(publishedInPage, that.publishedInPage) &&
        Objects.equals(unparsed, that.unparsed) &&
        state == that.state &&
        Objects.equals(warnings, that.warnings);
  }

  @Override
  public int hashCode() {
    return Objects.hash(extinct, taxonomicNote, nomenclaturalNote, publishedIn, publishedInYear, publishedInPage, unparsed, doubtful, manuscript, state, warnings);
  }

  @Override
  public String toString() {
    return authorshipComplete(null);
  }
}
