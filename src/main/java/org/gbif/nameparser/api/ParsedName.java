package org.gbif.nameparser.api;

import org.apache.commons.lang3.ObjectUtils;
import org.gbif.nameparser.util.NameFormatter;

import javax.annotation.Nonnull;
import java.util.*;

import static org.gbif.nameparser.util.NameFormatter.HYBRID_MARKER;

/**
 * The structured form of a parsed scientific name: the Linnean name parts (uninomial / genus /
 * subgenus / epithets), its {@link Rank}, {@link NomCode} and {@link NameType}, the hybrid, cultivar
 * and phrase details, plus the authorship and parse {@link State} inherited from {@link ParsedAuthorship}.
 */
public class ParsedName extends ParsedAuthorship implements LinneanName {
  /**
   * Degree of parsing this instance reflects.
   */
  public enum State {
    /**
     * The entire string was parsed to the very end.
     **/
    COMPLETE,
    
    /**
     * name & authorship has been parsed, but parts of the input string have not been understood.
     * Should be flagged as doubtful.
     **/
    PARTIAL,
    
    /**
     * An unparsable name
     **/
    NONE;
    
    /**
     * @return true if the name could be parsed into a structured form
     */
    public boolean isParsed() {
      return this != NONE;
    }
  }
  
  /**
   * Rank of the name from enumeration
   */
  @Nonnull
  private Rank rank = Rank.UNRANKED;
  
  private NomCode code;

  /**
   * Represents the monomial for genus, families or names at higher ranks which do not have further epithets.
   */
  private String uninomial;
  
  /**
   * The genus part of an infrageneric, bi- or trinomial name.
   * Not used for standalone genus names which are represented as uninomials.
   */
  private String genus;

  private CombinedAuthorship genericAuthorship;

  /**
   * The infrageneric epithet.
   */
  private String infragenericEpithet;
  
  private String specificEpithet;

  /**
   * The species authorship when the name is an infraspecific trinomial and might contain both
   * the species and infraspecies authorship. The main authorship is used for the infraspecies.
   */
  private CombinedAuthorship specificAuthorship;
  
  private String infraspecificEpithet;
  
  private String cultivarEpithet;

  /**
   * Final phrase part of the name when type=INFORMAL.
   */
  private String phrase;

  /**
   * A bacterial candidate name.
   * Candidatus is a provisional status for incompletely described procaryotes
   * (e.g. that cannot be maintained in a Bacteriology Culture Collection)
   * which was published in the January 1995.
   * The category Candidatus is not covered by the Rules of the Bacteriological Code but is a taxonomic assignment.
   * <p>
   * The names included in the category Candidatus are usually written as follows:
   * Candidatus (in italics), the subsequent name(s) in roman type and the entire name in quotation marks.
   * For example, "Candidatus Phytoplasma", "Candidatus Phytoplasma allocasuarinae".
   * <p>
   * See http://www.bacterio.net/-candidatus.html
   * and https://en.wikipedia.org/wiki/Candidatus
   */
  private boolean candidatus;
  
  /**
   * The name parts that carry a hybrid marker (×). More than one part can be
   * marked when the formula spans genus and species, for example.
   */
  private EnumSet<NamePart> notho;

  /**
   * If true indicates that the parsed name is the original spelling of the name.
   * This is usually indicated by placing [sic] after the name.
   *
   * If false it instead indicates that the parsed name is a corrected spelling of the name,
   * usually indicated by placing corrig. after the name.
   *
   * If null it is unknown or the original spelling was never revised.
   */
  private Boolean originalSpelling;

  /**
   * Optional qualifiers like cf. or aff. that can precede an epithet.
   */
  private Map<NamePart, String> epithetQualifier;
  
  /**
   * The kind of name classified in broad catagories based on their syntactical
   * structure
   */
  private NameType type;

  /**
   * Copies all values from the given parsed name.
   * <p>
   * The mutable collections {@code notho} and {@code epithetQualifier} (and {@code warnings}, via
   * {@link ParsedAuthorship#copy}) are deep-copied. The {@link Authorship} objects are shared by
   * reference — see {@link ParsedAuthorship#copy(ParsedAuthorship)}.
   */
  public void copy(ParsedName pn) {
    super.copy(pn);
    rank = pn.rank;
    code = pn.code;
    uninomial = pn.uninomial;
    genus = pn.genus;
    genericAuthorship = pn.genericAuthorship;
    infragenericEpithet = pn.infragenericEpithet;
    specificEpithet = pn.specificEpithet;
    specificAuthorship = pn.specificAuthorship;
    infraspecificEpithet = pn.infraspecificEpithet;
    cultivarEpithet = pn.cultivarEpithet;
    phrase = pn.phrase;
    candidatus = pn.candidatus;
    notho = pn.notho != null ? EnumSet.copyOf(pn.notho) : null;
    originalSpelling = pn.originalSpelling;
    epithetQualifier = pn.epithetQualifier == null ? null : new EnumMap<>(pn.epithetQualifier);
    type = pn.type;
  }

  @Override
  public Rank getRank() {
    return rank;
  }
  
  @Override
  public void setRank(Rank rank) {
    this.rank = rank == null ? Rank.UNRANKED : rank;
  }
  
  @Override
  public NomCode getCode() {
    return code;
  }
  
  @Override
  public void setCode(NomCode code) {
    this.code = code;
  }
  
  @Override
  public String getUninomial() {
    return uninomial;
  }
  
  @Override
  public void setUninomial(String uni) {
    if (uni != null && !uni.isEmpty() && uni.charAt(0) == HYBRID_MARKER) {
      this.uninomial = uni.substring(1);
      addNotho(NamePart.GENERIC);
    } else {
      this.uninomial = uni;
    }
  }
  
  @Override
  public String getGenus() {
    return genus;
  }
  
  @Override
  public void setGenus(String genus) {
    if (genus != null && !genus.isEmpty() && genus.charAt(0) == HYBRID_MARKER) {
      this.genus = genus.substring(1);
      addNotho(NamePart.GENERIC);
    } else {
      this.genus = genus;
    }
  }

  public CombinedAuthorship getGenericAuthorship() {
    return genericAuthorship;
  }

  public void setGenericAuthorship(CombinedAuthorship genericAuthorship) {
    this.genericAuthorship = genericAuthorship;
  }

  public boolean hasGenericAuthorship() {
    return genericAuthorship != null && genericAuthorship.hasAuthorship();
  }

  @Override
  public String getInfragenericEpithet() {
    return infragenericEpithet;
  }
  
  @Override
  public void setInfragenericEpithet(String infraGeneric) {
    if (infraGeneric != null && !infraGeneric.isEmpty() && infraGeneric.charAt(0) == HYBRID_MARKER) {
      this.infragenericEpithet = infraGeneric.substring(1);
      addNotho(NamePart.INFRAGENERIC);
    } else {
      this.infragenericEpithet = infraGeneric;
    }
  }
  
  @Override
  public String getSpecificEpithet() {
    return specificEpithet;
  }
  
  @Override
  public void setSpecificEpithet(String species) {
    if (species != null && !species.isEmpty() && species.charAt(0) == HYBRID_MARKER) {
      specificEpithet = species.substring(1);
      addNotho(NamePart.SPECIFIC);
    } else {
      specificEpithet = species;
    }
  }

  public CombinedAuthorship getSpecificAuthorship() {
    return specificAuthorship;
  }

  public void setSpecificAuthorship(CombinedAuthorship specificAuthorship) {
    this.specificAuthorship = specificAuthorship;
  }

  public boolean hasSpecificAuthorship() {
    return specificAuthorship != null && specificAuthorship.hasAuthorship();
  }

  @Override
  public String getInfraspecificEpithet() {
    return infraspecificEpithet;
  }
  
  @Override
  public void setInfraspecificEpithet(String infraSpecies) {
    if (infraSpecies != null && !infraSpecies.isEmpty() && infraSpecies.charAt(0) == HYBRID_MARKER) {
      this.infraspecificEpithet = infraSpecies.substring(1);
      addNotho(NamePart.INFRASPECIFIC);
    } else {
      this.infraspecificEpithet = infraSpecies;
    }
  }
  
  public String getCultivarEpithet() {
    return cultivarEpithet;
  }
  
  public void setCultivarEpithet(String cultivarEpithet) {
    this.cultivarEpithet = cultivarEpithet;
  }
  
  public String getPhrase() {
    return phrase;
  }
  
  public void setPhrase(String phrase) {
    this.phrase = phrase;
  }

  public boolean isCandidatus() {
    return candidatus;
  }
  
  public void setCandidatus(boolean candidatus) {
    this.candidatus = candidatus;
  }
  
  @Override
  public Set<NamePart> getNotho() {
    return notho;
  }

  @Override
  public void setNotho(NamePart part) {
    this.notho = part == null ? null : EnumSet.of(part);
  }

  @Override
  public void addNotho(NamePart part) {
    if (part != null) {
      if (this.notho == null) {
        this.notho = EnumSet.of(part);
      } else {
        this.notho.add(part);
      }
    }
  }

  public Boolean isOriginalSpelling() {
    return originalSpelling;
  }

  public void setOriginalSpelling(Boolean originalSpelling) {
    this.originalSpelling = originalSpelling;
  }

  public String getEpithet(NamePart part) {
    switch (part) {
      case GENERIC:
        return getGenus();
      case INFRAGENERIC:
        return getInfragenericEpithet();
      case SPECIFIC:
        return getSpecificEpithet();
      case INFRASPECIFIC:
        return getInfraspecificEpithet();
    }
    return null;
  }

  public Map<NamePart, String> getEpithetQualifier() {
    return epithetQualifier;
  }
  
  public String getEpithetQualifier(NamePart part) {
    return epithetQualifier.getOrDefault(part, null);
  }

  public boolean hasEpithetQualifier(NamePart part) {
    return epithetQualifier != null && epithetQualifier.containsKey(part);
  }

  public void setEpithetQualifier(Map<NamePart, String> epithetQualifier) {
    this.epithetQualifier = epithetQualifier;
  }
  
  public void setEpithetQualifier(NamePart part, String qualifier) {
    if (part != null && qualifier != null) {
      if (epithetQualifier == null) {
        epithetQualifier = new EnumMap<>(NamePart.class);
      }
      epithetQualifier.put(part, qualifier);
    }
  }

  public NameType getType() {
    return type;
  }
  
  public void setType(NameType type) {
    this.type = type;
  }
  
  /**
   * @return the terminal epithet. Infraspecific epithet if existing, the species epithet or null
   */
  public String getTerminalEpithet() {
    return infraspecificEpithet == null ? specificEpithet : infraspecificEpithet;
  }
  
  public List<String> listEpithets() {
    List<String> epis = new ArrayList<>();
    Collections.addAll(epis, infragenericEpithet, specificEpithet, infraspecificEpithet, cultivarEpithet);
    epis.removeIf(Objects::isNull);
    return epis;
  }
  
  /**
   * @return true if the parsed name has non null name properties or a scientific name. Remarks will not count as a name
   */
  public boolean hasName() {
    return ObjectUtils.firstNonNull(uninomial, genus, infragenericEpithet, specificEpithet, infraspecificEpithet, phrase, cultivarEpithet) != null;
  }

  public boolean isHybridName() {
    return notho != null && !notho.isEmpty();
  }

  public boolean isAutonym() {
    return specificEpithet != null && infraspecificEpithet != null && specificEpithet.equals(infraspecificEpithet);
  }
  
  /**
   * @return true if the name is a bi- or trinomial with at least a genus and species epithet given.
   */
  public boolean isBinomial() {
    return genus != null && specificEpithet != null;
  }
  
  /**
   * @return true if the name is a trinomial with at least a genus, species and infraspecific epithet given.
   */
  public boolean isTrinomial() {
    return isBinomial() && infraspecificEpithet != null;
  }
  
  /**
   * Checks if a parsed name is missing final epithets compared to what is indicated by its rank.
   *
   * @return true if the name is not fully determined
   */
  public boolean isIndetermined() {
    if (isPhraseName())
      return false;
    return rank.isInfragenericStrictly() && uninomial == null && infragenericEpithet == null && specificEpithet == null
        || rank.isSpeciesOrBelow() && !rank.isCultivarRank() && specificEpithet == null
        || rank.isInfraspecific() && !rank.isCultivarRank() && infraspecificEpithet == null
        || rank.isCultivarRank() && cultivarEpithet == null;
  }
  
  /**
   * @return true if some "higher" epithet of a name is missing, e.g. the genus in case of a species.
   */
  public boolean isIncomplete() {
    return (specificEpithet != null || cultivarEpithet != null) && genus == null
        || infraspecificEpithet != null && specificEpithet == null;
  }
  
  /**
   * @return true if the name contains an abbreviated genus or uninomial
   */
  public boolean isAbbreviated() {
    return uninomial != null && uninomial.endsWith(".") ||
        genus != null && genus.endsWith(".") ||
        specificEpithet != null && specificEpithet.endsWith(".");
  }

  /**
   * @return True if this is a phrase name
   */
  public boolean isPhraseName() {
    return phrase != null && !this.phrase.isEmpty();
  }
  
  /**
   * @see NameFormatter#canonical(ParsedName)
   */
  public String canonicalName() {
    return NameFormatter.canonical(this);
  }

  /**
   * @see NameFormatter#canonicalWithoutAuthorship(ParsedName)
   */
  public String canonicalNameWithoutAuthorship() {
    return NameFormatter.canonicalWithoutAuthorship(this);
  }

  /**
   * @see NameFormatter#canonicalMinimal(ParsedName)
   */
  public String canonicalNameMinimal() {
    return NameFormatter.canonicalMinimal(this);
  }

  /**
   * @see NameFormatter#canonicalComplete(ParsedName)
   */
  public String canonicalNameComplete() {
    return NameFormatter.canonicalComplete(this);
  }

  /**
   * @see NameFormatter#authorshipComplete(ParsedAuthorship, NomCode)
   */
  public String authorshipComplete() {
    return NameFormatter.authorshipComplete(this, getCode());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ParsedName)) return false;
    if (!super.equals(o)) return false;
    ParsedName that = (ParsedName) o;
    return candidatus == that.candidatus
           && Objects.equals(originalSpelling, that.originalSpelling)
           && rank == that.rank
           && code == that.code
           && Objects.equals(uninomial, that.uninomial)
           && Objects.equals(genus, that.genus)
           && Objects.equals(infragenericEpithet, that.infragenericEpithet)
           && Objects.equals(specificEpithet, that.specificEpithet)
           && Objects.equals(infraspecificEpithet, that.infraspecificEpithet)
           && Objects.equals(cultivarEpithet, that.cultivarEpithet)
           && Objects.equals(phrase, that.phrase)
           && Objects.equals(notho, that.notho)
           && Objects.equals(epithetQualifier, that.epithetQualifier)
           && type == that.type;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), rank, code, uninomial, genus, infragenericEpithet, specificEpithet, infraspecificEpithet, cultivarEpithet, phrase, candidatus, notho, originalSpelling, epithetQualifier, type);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (type != null) {
      sb.append("[");
      sb.append(type);
      sb.append("] ");
    }
    if (uninomial != null) {
      sb.append(" U:").append(uninomial);
    }
    if (genus != null) {
      sb.append(" G:").append(genus);
    }
    if (infragenericEpithet != null) {
      sb.append(" IG:").append(infragenericEpithet);
    }
    if (specificEpithet != null) {
      sb.append(" S:").append(specificEpithet);
    }
    if (rank != null) {
      sb.append(" R:").append(rank);
    }
    if (infraspecificEpithet != null) {
      sb.append(" IS:").append(infraspecificEpithet);
    }
    if (cultivarEpithet != null) {
      sb.append(" CV:").append(cultivarEpithet);
    }
    if (Boolean.TRUE.equals(originalSpelling)) {
      sb.append(" [sic]");
    } else if (Boolean.FALSE.equals(originalSpelling)) {
      sb.append(" corrig.");
    }
    if (phrase != null) {
      sb.append(" STR:").append(phrase);
    }
    if (getCombinationAuthorship() != null) {
      sb.append(" A:").append(getCombinationAuthorship());
    }
    if (getBasionymAuthorship() != null) {
      sb.append(" BA:").append(getBasionymAuthorship());
    }
    return sb.toString();
  }
}
