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

import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.ObjectUtils;

import com.google.common.collect.Lists;

import static org.gbif.nameparser.util.NameFormatter.HYBRID_MARKER;

/**
 *
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
  
  /**
   * The infrageneric epithet.
   */
  private String infragenericEpithet;
  
  private String specificEpithet;
  
  private String infraspecificEpithet;
  
  private String cultivarEpithet;
  
  private String phrase;

  /**
   * The voucher in a phrase name
   *
   * @see NameType#INFORMAL
   */
  private String voucher;

  /**
   * The nominating party for a phrase name
   *
   * @see NameType#INFORMAL
   */
  private String nominatingParty;

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
   * The part of the named hybrid which is considered a hybrid
   */
  private NamePart notho;
  
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
   * Copies all values from the given parsed authorship
   */
  public void copy(ParsedName pn) {
    super.copy(pn);
    rank = pn.rank;
    code = pn.code;
    uninomial = pn.uninomial;
    genus = pn.genus;
    infragenericEpithet = pn.infragenericEpithet;
    specificEpithet = pn.specificEpithet;
    infraspecificEpithet = pn.infraspecificEpithet;
    cultivarEpithet = pn.cultivarEpithet;
    phrase = pn.phrase;
    voucher = pn.voucher;
    nominatingParty = pn.nominatingParty;
    candidatus = pn.candidatus;
    notho = pn.notho;
    epithetQualifier = pn.epithetQualifier;
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
      notho = NamePart.GENERIC;
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
      notho = NamePart.GENERIC;
    } else {
      this.genus = genus;
    }
  }
  
  @Override
  public String getInfragenericEpithet() {
    return infragenericEpithet;
  }
  
  @Override
  public void setInfragenericEpithet(String infraGeneric) {
    if (infraGeneric != null && !infraGeneric.isEmpty() && infraGeneric.charAt(0) == HYBRID_MARKER) {
      this.infragenericEpithet = infraGeneric.substring(1);
      notho = NamePart.INFRAGENERIC;
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
      notho = NamePart.SPECIFIC;
    } else {
      specificEpithet = species;
    }
  }
  
  @Override
  public String getInfraspecificEpithet() {
    return infraspecificEpithet;
  }
  
  @Override
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
  
  public String getPhrase() {
    return phrase;
  }
  
  public void setPhrase(String phrase) {
    this.phrase = phrase;
  }

  public String getVoucher() {
    return voucher;
  }

  public void setVoucher(String voucher) {
    this.voucher = voucher;
  }

  public String getNominatingParty() {
    return nominatingParty;
  }

  public void setNominatingParty(String nominatingParty) {
    this.nominatingParty = nominatingParty;
  }

  public boolean isCandidatus() {
    return candidatus;
  }
  
  public void setCandidatus(boolean candidatus) {
    this.candidatus = candidatus;
  }
  
  @Override
  public NamePart getNotho() {
    return notho;
  }
  
  @Override
  public void setNotho(NamePart notho) {
    this.notho = notho;
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
    return Lists.newArrayList(infragenericEpithet, specificEpithet, infraspecificEpithet, cultivarEpithet).stream()
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }
  
  /**
   * @return true if the parsed name has non null name properties or a scientific name. Remarks will not count as a name
   */
  public boolean hasName() {
    return ObjectUtils.firstNonNull(uninomial, genus, infragenericEpithet, specificEpithet, infraspecificEpithet, phrase, cultivarEpithet) != null;
  }

  public boolean isHybridName() {
    return notho != null;
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
    return phrase != null && !this.phrase.isEmpty() && this.voucher != null && !this.voucher.isEmpty();
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
  @Override
  public String authorshipComplete() {
    return NameFormatter.authorshipComplete(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    ParsedName that = (ParsedName) o;
    return candidatus == that.candidatus &&
        rank == that.rank &&
        code == that.code &&
           Objects.equals(uninomial, that.uninomial) &&
           Objects.equals(genus, that.genus) &&
           Objects.equals(infragenericEpithet, that.infragenericEpithet) &&
           Objects.equals(specificEpithet, that.specificEpithet) &&
           Objects.equals(infraspecificEpithet, that.infraspecificEpithet) &&
           Objects.equals(cultivarEpithet, that.cultivarEpithet) &&
           Objects.equals(phrase, that.phrase) &&
           Objects.equals(this.voucher, that.voucher) &&
           Objects.equals(this.nominatingParty, that.nominatingParty) &&
        notho == that.notho &&
           Objects.equals(epithetQualifier, that.epithetQualifier) &&
        type == that.type;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), rank, code, uninomial, genus, infragenericEpithet, specificEpithet, infraspecificEpithet,
        cultivarEpithet, phrase, voucher, nominatingParty, candidatus, notho, epithetQualifier, type);
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
    if (phrase != null) {
      sb.append(" STR:").append(phrase);
    }
    if (voucher != null) {
      sb.append(" VOU:").append(voucher);
    }
    if (nominatingParty != null) {
      sb.append(" NP:").append(nominatingParty);
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
