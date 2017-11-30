/*
 * Copyright 2014 Global Biodiversity Information Facility (GBIF)
 *
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An ordered taxonomic rank enumeration with the most frequently used values.
 * Several static methods, lists, sets and maps are provided to help with ordering and lookup from strings.
 *
 * @see <a href="http://rs.gbif.org/vocabulary/gbif/rank.xml">rs.gbif.org vocabulary</a>
 */
public enum Rank {

  DOMAIN("dom."),

  SUPERKINGDOM("superreg."),

  KINGDOM("reg."),

  SUBKINGDOM("subreg."),

  INFRAKINGDOM("infrareg."),

  SUPERPHYLUM("superphyl."),

  PHYLUM("phyl."),

  SUBPHYLUM("subphyl."),

  INFRAPHYLUM("infraphyl."),

  SUPERCLASS("supercl."),

  CLASS("cl."),

  SUBCLASS("subcl."),

  INFRACLASS("infracl."),

  PARVCLASS("parvcl."),

  SUPERLEGION("superleg."),

  /**
   * Sometimes used in zoology, e.g. for birds and mammals
   */
  LEGION("leg."),

  SUBLEGION("subleg."),

  INFRALEGION("infraleg."),

  SUPERCOHORT("supercohort"),

  /**
   * Sometimes used in zoology, e.g. for birds and mammals
   */
  COHORT("cohort"),

  SUBCOHORT("subcohort"),

  INFRACOHORT("infracohort"),

  MAGNORDER("magnord."),

  SUPERORDER("superord."),

  GRANDORDER("grandord."),

  ORDER("ord."),

  SUBORDER("subord."),

  INFRAORDER("infraord."),

  PARVORDER("parvord."),

  SUPERFAMILY("superfam."),

  FAMILY("fam."),

  SUBFAMILY("subfam."),

  INFRAFAMILY("infrafam."),

  SUPERTRIBE("supertrib."),

  TRIBE("trib."),

  SUBTRIBE("subtrib."),

  INFRATRIBE("infratrib."),

  /**
   * Used for any other unspecific rank above genera.
   */
  SUPRAGENERIC_NAME("supragen."),

  GENUS("gen."),

  SUBGENUS("subgen."),

  INFRAGENUS("infrag."),

  SUPERSECTION("supersect."),

  SECTION("sect."),

  SUBSECTION("subsect."),

  SUPERSERIES("superser."),

  SERIES("ser."),

  SUBSERIES("subser."),

  /**
   * used for any other unspecific rank below genera and above species.
   */
  INFRAGENERIC_NAME("infragen."),

  /**
   * A loosely defined group of species.
   * Zoology: Aggregate - a group of species, other than a subgenus, within a genus. An aggregate may be denoted by a group name interpolated in parentheses.
   * The Berlin/MoreTax model notes: [these] aren't taxonomic ranks but cirumscriptions because on the one hand they are necessary for the concatenation
   * of the fullname and on the other hand they are necessary for distinguishing the aggregate or species group from the microspecies.
   */
  SPECIES_AGGREGATE("agg."),

  SPECIES("sp."),

  /**
   * used for any other unspecific rank below species.
   */
  INFRASPECIFIC_NAME("infrasp."),

  /**
   * The term grex has been coined to expand botanical nomenclature to describe hybrids of orchids.
   * Grex names are one of the three categories of plant names governed by the International Code of Nomenclature for Cultivated Plants
   * Within a grex the Groups category can be used to refer to plants by their shared characteristics (rather than by their parentage),
   * and individual orchid plants can be selected (and propagated) and named as cultivars
   * https://en.wikipedia.org/wiki/Grex_(horticulture)
   */
  GREX("grex"),

  SUBSPECIES("subsp."),

  /**
   * Rank in use from the code for cultivated plants.
   * It does not use a classic rank marker but indicated the Group rank after the actual groups name
   * For example Rhododendron boothii Mishmiense Group
   * or Primula Border Auricula Group
   * <p>
   * Sometimes authors also used the words "sort", "type", "selections" or "hybrids" instead of Group which is not legal according to the code.
   */
  CULTIVAR_GROUP,

  /**
   * A group of cultivars. These can be roughly comparable to cultivar groups, but convarieties, unlike cultivar groups,
   * do not necessarily contain named varieties, and convarieties are members of traditional "Linnaean" ranks.
   * The ICNCP replaced this term with the term cultivar-group, and convarieties should not be used in modern cultivated plant taxonomy.
   * <p>
   * From Spooner et al., Horticultural Reviews 28 (2003): 1-60
   */
  CONVARIETY("convar."),

  /**
   * used also for any other unspecific rank below subspecies.
   */
  INFRASUBSPECIFIC_NAME("infrasubsp."),

  /**
   * Botanical legacy rank for a race, recommended in botanical code from 1868
   */
  PROLES("prol."),

  /**
   * Zoological legacy rank
   */
  NATIO("natio"),

  /**
   * Zoological legacy rank
   */
  ABERRATION("ab."),

  /**
   * Zoological legacy rank
   */
  MORPH("morph"),

  VARIETY("var."),

  SUBVARIETY("subvar."),

  FORM("f."),

  SUBFORM("subf."),

  /**
   * Microbial rank based on pathogenic reactions in one or more hosts.
   * For recommendations on designating pathovars and use of designations when reviving names see
   * Dye et al. (1980) Standards for naming pathovars of phytopathogenic bacteria and a list of pathovar names and pathotype strains.
   * Rev. Plant Pathol. 59:153–168.
   * See <a href="http://www.ncbi.nlm.nih.gov/books/NBK8812/table/A844/?report=objectonly">Bacteriological Code</a>
   * See <a href="http://www.isppweb.org/about_tppb_naming.asp">International Standards for Naming Pathovars of Phytopathogenic Bacteria</a>
   * See <a href="http://sipav.org/main/jpp/index.php/jpp/article/view/682">Demystifying the nomenclature of bacterial plant pathogens</a>
   * See <a href="http://link.springer.com/chapter/10.1007/978-94-009-3555-6_171">Problems with the Pathovar Concept</a>
   * For example Pseudomonas syringae pv. lachrymans
   */
  PATHOVAR("pv."),

  /**
   * Microbial rank based on biochemical or physiological properties.
   * See <a href="http://www.ncbi.nlm.nih.gov/books/NBK8812/table/A844/?report=objectonly">Bacteriological Code</a>
   * For example Francisella tularensis biovar tularensis
   */
  BIOVAR("biovar"),

  /**
   * Microbial rank based on production or amount of production of a particular chemical.
   * See <a href="http://www.ncbi.nlm.nih.gov/books/NBK8812/table/A844/?report=objectonly">Bacteriological Code</a>
   * For example Vibrio alginolyticus chemovar iophagus
   */
  CHEMOVAR("chemovar"),

  /**
   * Microbial rank based on morphological characterislics.
   * See <a href="http://www.ncbi.nlm.nih.gov/books/NBK8812/table/A844/?report=objectonly">Bacteriological Code</a>
   * For example Acinetobacter junii morphovar I
   */
  MORPHOVAR("morphovar"),

  /**
   * Microbial infrasubspecific rank based on reactions to bacteriophage.
   * See <a href="http://www.ncbi.nlm.nih.gov/books/NBK8812/table/A844/?report=objectonly">Bacteriological Code</a>
   * For example Staphyloccocus aureus phagovar 42D
   */
  PHAGOVAR("phagovar"),

  /**
   * Microbial infrasubspecific rank based on antigenic characteristics.
   * See <a href="http://www.ncbi.nlm.nih.gov/books/NBK8812/table/A844/?report=objectonly">Bacteriological Code</a>
   * For example Salmonella enterica serovar Dublin
   */
  SEROVAR("serovar"),

  /**
   * Microbial infrasubspecific rank based on chemical constitution.
   * See <a href="http://www.ncbi.nlm.nih.gov/books/NBK8812/table/A844/?report=objectonly">Bacteriological Code</a>
   * For example Thymus vulgaris ct. geraniol
   */
  CHEMOFORM("chemoform"),

  /**
   * Microbial infrasubspecific rank.
   * A parasitic, symbiotic, or commensal microorganism distinguished primarily by adaptation to a particular host or habitat.
   * Named preferably by the scientific name of the host in the genitive.
   * See <a href="http://www.ncbi.nlm.nih.gov/books/NBK8812/table/A844/?report=objectonly">Bacteriological Code</a>
   * For example Puccinia graminis f. sp. avenae
   */
  FORMA_SPECIALIS("f.sp."),

  CULTIVAR("cv."),

  /**
   * A microbial strain.
   */
  STRAIN("strain"),

  /**
   * Any other rank we cannot map to this enumeration
   */
  OTHER,

  /**
   * Rank used for unknown or explicitly not assigned rank.
   * The default if not given.
   */
  UNRANKED;

  /**
   * All main Linnean ranks ordered.
   */
  public static final List<Rank> LINNEAN_RANKS = ImmutableList.of(
      KINGDOM,
      PHYLUM,
      CLASS,
      ORDER,
      FAMILY,
      GENUS,
      SPECIES
  );

  /**
   * An ordered list of all ranks that appear in Darwin Core with their own term.
   */
  public static final List<Rank> DWC_RANKS = ImmutableList.of(
      KINGDOM,
      PHYLUM,
      CLASS,
      ORDER,
      FAMILY,
      GENUS,
      SUBGENUS,
      SPECIES
  );

  /**
   * A set of ranks which cannot clearly be compared to any other rank as they represent rank "ranges".
   * For example a subgeneric rank is anything below genus,
   * so one cannot say if its higher or lower than a species for example.
   */
  private static final Set<Rank> UNCOMPARABLE_RANKS = ImmutableSet.of(
      INFRAGENERIC_NAME,
      INFRASPECIFIC_NAME,
      INFRASUBSPECIFIC_NAME,
      OTHER,
      UNRANKED
  );

  private static final Set<Rank> LEGACY_RANKS = ImmutableSet.of(
      MORPH,
      ABERRATION,
      NATIO,
      PROLES,
      CONVARIETY
  );

  private static final Map<Rank, NomCode> RANK2CODE = ImmutableMap.<Rank, NomCode>builder()
      .put(PARVCLASS, NomCode.ZOOLOGICAL)
      .put(MAGNORDER, NomCode.ZOOLOGICAL)
      .put(GRANDORDER, NomCode.ZOOLOGICAL)
      .put(PARVORDER, NomCode.ZOOLOGICAL)
      .put(SUPERLEGION, NomCode.ZOOLOGICAL)
      .put(LEGION, NomCode.ZOOLOGICAL)
      .put(SUBLEGION, NomCode.ZOOLOGICAL)
      .put(INFRALEGION, NomCode.ZOOLOGICAL)
      .put(SUPERCOHORT, NomCode.ZOOLOGICAL)
      .put(COHORT, NomCode.ZOOLOGICAL)
      .put(SUBCOHORT, NomCode.ZOOLOGICAL)
      .put(INFRACOHORT, NomCode.ZOOLOGICAL)
      .put(MORPH, NomCode.ZOOLOGICAL)
      .put(ABERRATION, NomCode.ZOOLOGICAL)
      .put(NATIO, NomCode.ZOOLOGICAL)

      .put(PROLES, NomCode.BOTANICAL)
      .put(SECTION, NomCode.BOTANICAL)
      .put(SUBSECTION, NomCode.BOTANICAL)
      .put(SERIES, NomCode.BOTANICAL)
      .put(SUBSERIES, NomCode.BOTANICAL)

      .put(CULTIVAR, NomCode.CULTIVARS)
      .put(CULTIVAR_GROUP, NomCode.CULTIVARS)
      .put(CONVARIETY, NomCode.CULTIVARS)
      .put(GREX, NomCode.CULTIVARS)

      .put(PATHOVAR, NomCode.BACTERIAL)
      .put(BIOVAR, NomCode.BACTERIAL)
      .put(CHEMOVAR, NomCode.BACTERIAL)
      .put(MORPHOVAR, NomCode.BACTERIAL)
      .put(PHAGOVAR, NomCode.BACTERIAL)
      .put(SEROVAR, NomCode.BACTERIAL)
      .put(CHEMOFORM, NomCode.BACTERIAL)
      .put(FORMA_SPECIALIS, NomCode.BACTERIAL)
      .build();

  private final String marker;

  Rank() {
    this(null);
  }

  Rank(String marker) {
    this.marker = marker;
  }

  public String getMarker() {
    return marker;
  }

  /**
   * @return true for infraspecific ranks.
   */
  public boolean isInfraspecific() {
    return this != SPECIES && isSpeciesOrBelow();
  }

  /**
   * @return true for infra subspecific ranks.
   */
  public boolean isInfrasubspecific() {
    return ordinal() > SUBSPECIES.ordinal() && notOtherOrUnranked();
  }

  /**
   * @return true for rank is below genus. Also incluse species and infraspecific ranks
   */
  public boolean isInfrageneric() {
    return ordinal() > GENUS.ordinal() && notOtherOrUnranked();
  }

  /**
   * True for all mayor Linnéan ranks, ie kingdom,phylum,class,order,family,genus and species.
   */
  public boolean isLinnean() {
    for (Rank r : LINNEAN_RANKS) {
      if (r == this) {
        return true;
      }
    }
    return false;
  }

  public boolean isSpeciesOrBelow() {
    return ordinal() >= SPECIES.ordinal() && notOtherOrUnranked();
  }

  public boolean notOtherOrUnranked() {
    return this != OTHER && this != UNRANKED;
  }

  public boolean otherOrUnranked() {
    return this == OTHER || this == UNRANKED;
  }

  /**
   * @return true if the rank is above genus.
   */
  public boolean isSuprageneric() {
    return ordinal() < GENUS.ordinal();
  }

  /**
   * @return true if the rank is above genus.
   */
  public boolean isGenusOrSuprageneric() {
    return ordinal() <= GENUS.ordinal();
  }

  /**
   * @return true if the rank is above rank species.
   */
  public boolean isSupraspecific() {
    return ordinal() < SPECIES.ordinal();
  }

  /**
   * True for names of informal ranks that represent a range of ranks really and therefore cannot safely be compared to
   * other ranks in all cases.
   * Example ranks are INFRASPECIFIC_NAME or INFRAGENERIC_NAME
   *
   * @return true if uncomparable
   */
  public boolean isUncomparable() {
    return UNCOMPARABLE_RANKS.contains(this);
  }

  /**
   * @return true if the rank is considered a legacy rank not used anymore in current nomenclature.
   */
  public boolean isLegacy() {
    return LEGACY_RANKS.contains(this);
  }

  /**
   * @return the nomenclatural code if the rank is restricted to just one code or null otherwise
   */
  public NomCode isRestrictedToCode() {
    return RANK2CODE.get(this);
  }

  /**
   * @return true if this rank is higher than the given other
   */
  public boolean higherThan(Rank other) {
    return ordinal() < other.ordinal();
  }
}
