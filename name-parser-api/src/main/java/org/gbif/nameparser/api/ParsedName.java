package org.gbif.nameparser.api;

import com.google.common.collect.Lists;
import org.gbif.nameparser.util.NameFormatter;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;

import static org.gbif.nameparser.util.NameFormatter.HYBRID_MARKER;

/**
 *
 */
public class ParsedName {

  /**
   * Degree of parsing this instance reflects.
   */
  public enum State {
    /**
     * The entire string was parsed to the very end.
     **/
    COMPLETE,

    /**
     * The name epithets and the authorship have been successfully parsed,
     * but some more characters were found at the end of the name string that have been ignored.
     */
    NAME_AND_AUTHOR,

    /**
     * Only the name was parsed, ignoring any authorship
     **/
    NAME_ONLY,

    /**
     * An unparsable name
     **/
    NONE;

    /**
     * @return true if the name epithets, rank and notho property have been parsed
     */
    public boolean isNameParsed() {
      return this != NONE;
    }

    /**
     * @return true if in addition to the name also the authorship has been parsed
     */
    public boolean isAuthorshipParsed() {
      return this == COMPLETE || this == NAME_AND_AUTHOR;
    }
  }

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
	 * The genus part of a bi- or trinomial name. Not used for genus names which are
	 * represented by the scientificName alone.
	 */
	private String genus;

	/**
	 * The infrageneric epithet. Used only as the terminal epithet for names at
	 * infrageneric ranks, not for species
	 */
	private String infragenericEpithet;

	private String specificEpithet;

	private String infraspecificEpithet;

  private String cultivarEpithet;

  private String strain;

  /**
   * A bacterial candidate name.
   * Candidatus is a provisional status for incompletely described procaryotes
   * (e.g. that cannot be maintained in a Bacteriology Culture Collection)
   * which was published in the January 1995.
   * The category Candidatus is not covered by the Rules of the Bacteriological Code but is a taxonomic assignment.
   *
   * The names included in the category Candidatus are usually written as follows:
   * Candidatus (in italics), the subsequent name(s) in roman type and the entire name in quotation marks.
   * For example, "Candidatus Phytoplasma", "Candidatus Phytoplasma allocasuarinae".
   *
   * See http://www.bacterio.net/-candidatus.html
   * and https://en.wikipedia.org/wiki/Candidatus
   */
  private boolean candidatus;

  /**
	 * The part of the named hybrid which is considered a hybrid
	 */
	private NamePart notho;

  /**
   * Nomenclatural status remarks of the name.
   */
  private String sensu;

	/**
	 * Nomenclatural status remarks of the name.
	 */
	private String nomenclaturalNotes;

  /**
   * Any informal remarks found in the name
   */
  private String remarks;


	/**
	 * The kind of name classified in broad catagories based on their syntactical
	 * structure
	 */
	private NameType type;

  /**
   * Indicates some doubts that this is a name of the given type.
   * Usually indicates the existance of unusual characters not normally found in scientific names.
   */
  private boolean doubtful;

  private State state = State.NONE;

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

  public Rank getRank() {
    return rank;
  }

  public void setRank(Rank rank) {
    this.rank = rank == null ? Rank.UNRANKED : rank;
  }

  public NomCode getCode() {
    return code;
  }

  public void setCode(NomCode code) {
    this.code = code;
  }

  public String getUninomial() {
    return uninomial;
  }

  public void setUninomial(String uni) {
    if (uni != null && !uni.isEmpty() && uni.charAt(0) == HYBRID_MARKER) {
      this.uninomial = uni.substring(1);
      notho = NamePart.GENERIC;
    } else {
      this.uninomial = uni;
    }
  }

  public String getGenus() {
    return genus;
  }

  public void setGenus(String genus) {
    if (genus != null && !genus.isEmpty() && genus.charAt(0) == HYBRID_MARKER) {
      this.genus = genus.substring(1);
      notho = NamePart.GENERIC;
    } else {
      this.genus = genus;
    }
  }

  public String getInfragenericEpithet() {
    return infragenericEpithet;
  }

  public void setInfragenericEpithet(String infraGeneric) {
    if (infraGeneric != null && !infraGeneric.isEmpty() && infraGeneric.charAt(0) == HYBRID_MARKER) {
      this.infragenericEpithet = infraGeneric.substring(1);
      notho = NamePart.INFRAGENERIC;
    } else {
      this.infragenericEpithet = infraGeneric;
    }
  }

  public String getSpecificEpithet() {
    return specificEpithet;
  }

  public void setSpecificEpithet(String species) {
    if (species != null && !species.isEmpty() && species.charAt(0) == HYBRID_MARKER) {
      specificEpithet = species.substring(1);
      notho = NamePart.SPECIFIC;
    } else {
      specificEpithet = species;
    }
  }

  public String getInfraspecificEpithet() {
    return infraspecificEpithet;
  }

  public void setInfraspecificEpithet(String infraSpecies) {
    if (infraSpecies != null && !infraSpecies.isEmpty() && infraSpecies.charAt(0) == HYBRID_MARKER) {
      this.infraspecificEpithet = infraSpecies.substring(1);
      this.notho = NamePart.INFRASPECIFIC;
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

  public String getStrain() {
    return strain;
  }

  public void setStrain(String strain) {
    this.strain = strain;
  }

  public boolean isCandidatus() {
    return candidatus;
  }

  public void setCandidatus(boolean candidatus) {
    this.candidatus = candidatus;
  }

  public NamePart getNotho() {
    return notho;
  }

  public void setNotho(NamePart notho) {
    this.notho = notho;
  }

  public String getNomenclaturalNotes() {
    return nomenclaturalNotes;
  }

  public void setNomenclaturalNotes(String nomenclaturalNotes) {
    this.nomenclaturalNotes = nomenclaturalNotes;
  }

  public String getSensu() {
    return sensu;
  }

  public void setSensu(String sensu) {
    this.sensu = sensu;
  }

  public String getRemarks() {
    return remarks;
  }

  public void setRemarks(String remarks) {
    this.remarks = remarks;
  }

  public State getState() {
    return state;
  }

  public void setState(State state) {
    this.state = state;
  }

  public NameType getType() {
    return type;
  }

  public void setType(NameType type) {
    this.type = type;
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

  public void addWarning(String ... warnings) {
    for (String warn : warnings) {
      this.warnings.add(warn);
    }
  }


  /**
   * @return the terminal epithet. Infraspecific epithet if existing, the species epithet or null
   */
  public String getTerminalEpithet() {
    return infraspecificEpithet == null ? specificEpithet : infraspecificEpithet;
  }

  /**
   * @return true if any kind of authorship exists
   */
  public boolean hasAuthorship() {
    return combinationAuthorship.exists() || basionymAuthorship.exists();
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
    return isBinomial() && infraspecificEpithet!= null;
  }

  public boolean isIndetermined() {
    return rank != null && (rank.isInfrageneric() && rank.isSupraspecific() && infragenericEpithet == null || rank.isSpeciesOrBelow() && specificEpithet == null || rank.isInfraspecific() && infraspecificEpithet == null);
  }

  /**
	 * Validates consistency of name properties. This method checks if the given
	 * rank matches populated properties and available properties make sense
	 * together.
	 */
	public boolean isConsistent() {
		if (specificEpithet != null && genus == null) {
			return false;

		} else if (infraspecificEpithet != null && specificEpithet == null) {
			return false;

		} else if (infragenericEpithet != null && specificEpithet != null) {
			return false;

		}
		// verify ranks
		if (rank != null && rank.notOtherOrUnranked()) {
			if (rank.isGenusOrSuprageneric()) {
				if (genus != null || uninomial == null)
					return false;

			} else if (rank.isInfrageneric() && rank.isSupraspecific()) {
				if (infragenericEpithet == null)
					return false;

			} else if (rank.isSpeciesOrBelow()) {
				if (specificEpithet == null)
					return false;
				if (!rank.isInfraspecific() && infraspecificEpithet != null)
					return false;
			}

			if (rank.isInfraspecific()) {
				if (infraspecificEpithet == null)
					return false;
			}
		}
		return true;
	}

  /**
   * @See NameFormatter.canonical()
   */
  public String canonicalName() {
    return NameFormatter.canonical(this);
  }

  /**
   * @See NameFormatter.canonicalNameWithoutAuthorship()
   */
  public String canonicalNameWithoutAuthorship() {
    return NameFormatter.canonicalWithoutAuthorship(this);
  }

  /**
   * @See NameFormatter.canonicalMinimal()
   */
  public String canonicalNameMinimal() {
    return NameFormatter.canonicalMinimal(this);
  }

  /**
   * @See NameFormatter.canonicalComplete()
   */
  public String canonicalNameComplete() {
    return NameFormatter.canonicalComplete(this);
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
    ParsedName that = (ParsedName) o;
    return candidatus == that.candidatus &&
        doubtful == that.doubtful &&
        state == that.state &&
        Objects.equals(combinationAuthorship, that.combinationAuthorship) &&
        Objects.equals(basionymAuthorship, that.basionymAuthorship) &&
        Objects.equals(sanctioningAuthor, that.sanctioningAuthor) &&
        rank == that.rank &&
        code == that.code &&
        Objects.equals(uninomial, that.uninomial) &&
        Objects.equals(genus, that.genus) &&
        Objects.equals(infragenericEpithet, that.infragenericEpithet) &&
        Objects.equals(specificEpithet, that.specificEpithet) &&
        Objects.equals(infraspecificEpithet, that.infraspecificEpithet) &&
        Objects.equals(cultivarEpithet, that.cultivarEpithet) &&
        Objects.equals(strain, that.strain) &&
        notho == that.notho &&
        Objects.equals(sensu, that.sensu) &&
        Objects.equals(nomenclaturalNotes, that.nomenclaturalNotes) &&
        Objects.equals(remarks, that.remarks) &&
        type == that.type &&
        Objects.equals(warnings, that.warnings);
  }

  @Override
  public int hashCode() {
    return Objects.hash(combinationAuthorship, basionymAuthorship, sanctioningAuthor, rank, code, uninomial, genus, infragenericEpithet, specificEpithet, infraspecificEpithet, cultivarEpithet, strain, candidatus, notho, sensu, nomenclaturalNotes, remarks, type, doubtful, state, warnings);
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
    if (genus!= null) {
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
    if (strain != null) {
      sb.append(" STR:").append(strain);
    }
    if (combinationAuthorship != null) {
      sb.append(" A:").append(combinationAuthorship);
    }
    if (basionymAuthorship != null) {
      sb.append(" BA:").append(basionymAuthorship);
    }
    return sb.toString();
  }
}
