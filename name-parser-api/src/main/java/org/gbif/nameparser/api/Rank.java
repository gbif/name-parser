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

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An ordered taxonomic rank enumeration with most commonly used values.
 * The ranks listed are code agnostic and if a rank is used both in zoology and botany it is the same enum value.
 *
 * As some ranks are regulary placed in different orders in botany and zoology (e.g. section or series)
 * we follow just one tradition, but mark those ranks as "ambiguous".
 *
 * Ranks also expose a variety of methods to deal with them programmatically, e.g map them to major ranks only,
 * list Linnean ranks only, etc.
 * Several static methods, lists, sets and maps are provided to help with ordering and lookup from strings.
 */
public enum Rank {

  DOMAIN("dom."),
  REALM("realm"),
  SUBREALM("subrealm"),
  SUPERKINGDOM("superreg."),
  KINGDOM("reg."),
  SUBKINGDOM("subreg."),
  INFRAKINGDOM("infrareg."),
  SUPERPHYLUM("superphyla", "superphyl."),
  PHYLUM("phyla", "phyl."),
  SUBPHYLUM("subphyla", "subphyl."),
  INFRAPHYLUM("infraphyla", "infraphyl."),
  SUPERCLASS("superclasses", "supercl."),
  CLASS("classes", "cl."),
  SUBCLASS("subclasses", "subcl."),
  INFRACLASS("infraclasses", "infracl."),
  SUBTERCLASS("subterclasses", "subtercl."),
  PARVCLASS("parvclasses", "parvcl."),
  SUPERDIVISION("superdiv."),
  DIVISION("div."),
  SUBDIVISION("subdiv."),
  INFRADIVISION("infradiv."),
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

  GIGAORDER("gigaord."),

  MAGNORDER("magnord."),
  
  GRANDORDER("grandord."),

  MIRORDER("mirord."),

  SUPERORDER("superord."),

  ORDER("ord."),

  NANORDER("nanord."),

  HYPOORDER("hypoord."),

  MINORDER("minord."),

  SUBORDER("subord."),
  
  INFRAORDER("infraord."),
  
  PARVORDER("parvord."),

  MEGAFAMILY("megafamilies", "megafam."),
  
  GRANDFAMILY("grandfamilies", "grandfam."),
  
  SUPERFAMILY("superfamilies", "superfam."),
  
  EPIFAMILY("epifamilies", "epifam."),
  
  FAMILY("families", "fam."),
  
  SUBFAMILY("subfamilies", "subfam."),
  
  INFRAFAMILY("infrafamilies", "infrafam."),
  
  SUPERTRIBE("supertrib."),
  
  TRIBE("trib."),
  
  SUBTRIBE("subtrib."),
  
  INFRATRIBE("infratrib."),
  
  /**
   * Used for any other unspecific rank above genera.
   */
  SUPRAGENERIC_NAME("supragen."),
  
  GENUS("genera", "gen."),
  
  SUBGENUS("subgenera", "subgen."),
  
  INFRAGENUS("infragenera", "infrag."),
  
  SUPERSECTION("supersect."),
  
  SECTION("sect."),
  
  SUBSECTION("subsect."),
  
  SUPERSERIES("superseries", "superser."),
  
  SERIES("series", "ser."),
  
  SUBSERIES("subseries", "subser."),
  
  /**
   * Used for any other unspecific rank below genera and above species aggregates.
   */
  INFRAGENERIC_NAME("infragen."),
  
  /**
   * A loosely defined group of species, often in flux.
   * Often also called species complex, or superspecies.
   */
  SPECIES_AGGREGATE("agg."),
  
  SPECIES("species", "sp."),
  
  /**
   * Used for any unspecific rank below species.
   */
  INFRASPECIFIC_NAME("infrasp."),
  
  /**
   * The term grex has been coined to expand botanical nomenclature to describe hybrids of orchids.
   * Grex names are one of the three categories of plant names governed by the International Code of Nomenclature for Cultivated Plants
   * Within a grex the Groups category can be used to refer to plants by their shared characteristics (rather than by their parentage),
   * and individual orchid plants can be selected (and propagated) and named as cultivars
   * https://en.wikipedia.org/wiki/Grex_(horticulture)
   */
  GREX("gx"),
  
  SUBSPECIES("subspecies", "subsp."),
  
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
  CONVARIETY("convarieties", "convar."),
  
  /**
   * Used also for any unspecific rank below subspecies.
   */
  INFRASUBSPECIFIC_NAME("infrasubsp."),
  
  /**
   * Botanical legacy rank for a race, recommended in botanical code from 1868
   */
  PROLES("proles", "prol."),
  
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
  
  VARIETY("varieties", "var."),
  
  SUBVARIETY("subvarieties", "subvar."),
  
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
   * The default if not given instead of null.
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
      SUPRAGENERIC_NAME,
      INFRAGENERIC_NAME,
      INFRASPECIFIC_NAME,
      INFRASUBSPECIFIC_NAME,
      OTHER,
      UNRANKED
  );
  
  /**
   * A set of ranks which are treated differently in different groups of organisms and usually between botany and zoology.
   */
  private static final Set<Rank> AMBIGUOUS_RANKS = ImmutableSet.of(
      SUPERSECTION,
      SECTION,
      SUBSECTION,
      SUPERSERIES,
      SERIES,
      SUBSERIES,
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

  private static final Map<Rank, Rank> MAJOR_RANKS;
  static {
    Map<Rank, Rank> map = new EnumMap<>(Rank.class);
    Pattern prefixes = Pattern.compile("^(SUPER|SUB(?:TER)?|INFRA|GIGA|MAGN|GRAND|MIR|NAN|HYPO|MIN|PARV|MEGA|EPI)");
    for (Rank r : Rank.values()) {
      Rank major = r;
      if (r.isInfraspecific()) {
        major = Rank.INFRASPECIFIC_NAME;
      } else {
        Matcher m = prefixes.matcher(r.name());
        if (m.find()) {
          String name = m.replaceFirst("");
          try {
            major = Rank.valueOf(name);
          } catch (IllegalArgumentException e) {
          }
        }
      }
      map.put(r, major);
    }
    // manual fixes
    map.put(Rank.SPECIES_AGGREGATE, Rank.SPECIES);
    map.put(Rank.INFRAGENERIC_NAME, Rank.GENUS);
    MAJOR_RANKS = ImmutableMap.copyOf(map);
  }

  private static final Map<Rank, NomCode> RANK2CODE = ImmutableMap.<Rank, NomCode>builder()
      .put(SUPERDIVISION, NomCode.ZOOLOGICAL)
      .put(DIVISION, NomCode.ZOOLOGICAL)
      .put(SUBDIVISION, NomCode.ZOOLOGICAL)
      .put(INFRADIVISION, NomCode.ZOOLOGICAL)
      .put(SUPERLEGION, NomCode.ZOOLOGICAL)
      .put(LEGION, NomCode.ZOOLOGICAL)
      .put(SUBLEGION, NomCode.ZOOLOGICAL)
      .put(INFRALEGION, NomCode.ZOOLOGICAL)
      .put(SUBTERCLASS, NomCode.ZOOLOGICAL)
      .put(PARVCLASS, NomCode.ZOOLOGICAL)
      .put(GIGAORDER, NomCode.ZOOLOGICAL)
      .put(MAGNORDER, NomCode.ZOOLOGICAL)
      .put(GRANDORDER, NomCode.ZOOLOGICAL)
      .put(MIRORDER, NomCode.ZOOLOGICAL)
      .put(NANORDER, NomCode.ZOOLOGICAL)
      .put(HYPOORDER, NomCode.ZOOLOGICAL)
      .put(MINORDER, NomCode.ZOOLOGICAL)
      .put(PARVORDER, NomCode.ZOOLOGICAL)
      .put(SUPERCOHORT, NomCode.ZOOLOGICAL)
      .put(COHORT, NomCode.ZOOLOGICAL)
      .put(SUBCOHORT, NomCode.ZOOLOGICAL)
      .put(INFRACOHORT, NomCode.ZOOLOGICAL)
      .put(MEGAFAMILY, NomCode.ZOOLOGICAL)
      .put(GRANDFAMILY, NomCode.ZOOLOGICAL)
      .put(EPIFAMILY, NomCode.ZOOLOGICAL)
      .put(MORPH, NomCode.ZOOLOGICAL)
      .put(ABERRATION, NomCode.ZOOLOGICAL)
      .put(NATIO, NomCode.ZOOLOGICAL)

      .put(PROLES, NomCode.BOTANICAL)
      .put(SUPERSECTION, NomCode.BOTANICAL)
      .put(SECTION, NomCode.BOTANICAL)
      .put(SUBSECTION, NomCode.BOTANICAL)
      .put(SUPERSERIES, NomCode.BOTANICAL)
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
    
      .put(REALM, NomCode.VIRUS)
      .put(SUBREALM, NomCode.VIRUS)
      .build();
  
  private final String marker;
  private final String plural;

  Rank() {
    this(null);
  }
  
  Rank(String marker) {
    this.marker = marker;
    this.plural = name().toLowerCase() + "s";
  }

  Rank(String plural, String marker) {
    this.plural = plural;
    this.marker = marker;
  }

  public String getMarker() {
    return marker;
  }
  
  /**
   * @return true for infraspecific ranks excluding species.
   */
  public boolean isInfraspecific() {
    return ordinal() > SPECIES.ordinal() && notOtherOrUnranked();
  }
  
  /**
   * @return true for infra subspecific ranks.
   */
  public boolean isInfrasubspecific() {
    return ordinal() > SUBSPECIES.ordinal() && notOtherOrUnranked();
  }
  
  /**
   * @return true for rank is below genus. Also includes species and infraspecific ranks
   */
  public boolean isInfrageneric() {
    return ordinal() > GENUS.ordinal() && notOtherOrUnranked();
  }
  
  /**
   * @return true for real infrageneric ranks with an infragenericEpithet below genus and above species aggregate.
   */
  public boolean isInfragenericStrictly() {
    return isInfrageneric() && ordinal() < SPECIES_AGGREGATE.ordinal();
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

  /**
   * Return the major rank (incl all Linnean ranks) this rank belongs to stripping of its prefix, e.g. phylum for subphylum.
   * For infraspecific ranks INFRASPECIFIC_NAME is returned.
   * Ranks which cannot be mapped to a major rank return itself, never null.
   */
  public Rank getMajorRank() {
    return MAJOR_RANKS.get(this);
  }

  public boolean isSpeciesOrBelow() {
    return ordinal() >= SPECIES_AGGREGATE.ordinal() && notOtherOrUnranked();
  }
  
  public boolean notOtherOrUnranked() {
    return this != OTHER && this != UNRANKED;
  }
  
  public boolean otherOrUnranked() {
    return this == OTHER || this == UNRANKED;
  }
  
  /**
   * @return true if the rank is for family group names, i.e. between family (inclusive) and genus (exclusive).
   */
  public boolean isFamilyGroup() {
    return MEGAFAMILY.ordinal() <= ordinal() && ordinal() < SUPRAGENERIC_NAME.ordinal();
  }
  
  /**
   * @return true if the rank is for genus group names, i.e. between genus (inclusive) and species aggregate (exclusive).
   */
  public boolean isGenusGroup() {
    return GENUS.ordinal() <= ordinal() && ordinal() < SPECIES_AGGREGATE.ordinal();
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
   * @return true if the rank is above the rank species aggregate.
   */
  public boolean isSupraspecific() {
    return ordinal() < SPECIES_AGGREGATE.ordinal();
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
   * True for ranks which are treated differently in different groups of organisms, usually between botany and zoology.
   * Example ranks are SERIES or SECTION
   *
   * @return true if ambiguous
   */
  public boolean isAmbiguous() {
    return AMBIGUOUS_RANKS.contains(this);
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
   * @return true if the rank is restricted to Cultivated Plants
   */
  public boolean isCultivarRank() {
    return NomCode.CULTIVARS == isRestrictedToCode();
  }
  
  /**
   * @return true if this rank is higher than the given other
   */
  public boolean higherThan(Rank other) {
    return ordinal() < other.ordinal();
  }
  
  /**
   * @return true if this rank is higher or equal to the given other
   */
  public boolean higherOrEqualsTo(Rank other) {
    return ordinal() <= other.ordinal();
  }
}
