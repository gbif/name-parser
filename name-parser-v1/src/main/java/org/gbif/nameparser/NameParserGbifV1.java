package org.gbif.nameparser;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.gbif.api.exception.UnparsableException;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.service.checklistbank.NameParser;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.Rank;
import org.gbif.nameparser.api.NamePart;
import org.gbif.nameparser.api.NomCode;
import org.gbif.nameparser.api.UnparsableNameException;
import org.gbif.nameparser.api.Warnings;
import org.gbif.nameparser.util.NameFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Map;

import static org.gbif.nameparser.api.ParsedName.State;

/**
 * A name parser wrapped to return classic ParsedName objects from the GBIF API v1.
 */
public class NameParserGbifV1 implements NameParser {

  private static Logger LOG = LoggerFactory.getLogger(NameParserGbifV1.class);
  private static final Map<org.gbif.nameparser.api.NameType, NameType> NAME_TYPE_MAP = ImmutableMap.<org.gbif.nameparser.api.NameType, NameType>builder()
      .put(org.gbif.nameparser.api.NameType.SCIENTIFIC, NameType.SCIENTIFIC)
      .put(org.gbif.nameparser.api.NameType.VIRUS, NameType.VIRUS)
      .put(org.gbif.nameparser.api.NameType.HYBRID_FORMULA, NameType.HYBRID)
      .put(org.gbif.nameparser.api.NameType.INFORMAL, NameType.INFORMAL)
      .put(org.gbif.nameparser.api.NameType.OTU, NameType.OTU)
      .put(org.gbif.nameparser.api.NameType.PLACEHOLDER, NameType.PLACEHOLDER)
      .put(org.gbif.nameparser.api.NameType.NO_NAME, NameType.NO_NAME)
      .build();

  private final org.gbif.nameparser.api.NameParser parser;

  /**
   * Using the default GBIF RegEx Name Parser.
   */
  public NameParserGbifV1() {
    this.parser = new NameParserGBIF();
  }

  /**
   * Using the default GBIF RegEx Name Parser with a given timeout for parsing a single name.
   * @param timeout in milliseconds before returning an Unparsable name
   */
  public NameParserGbifV1(long timeout) {
    this.parser = new NameParserGBIF(timeout);
  }

  public NameParserGbifV1(org.gbif.nameparser.api.NameParser parser) {
    this.parser = parser;
  }

  @Override
  public ParsedName parse(String s, @Nullable Rank rank) throws UnparsableException {
    try {
      return convert(s, rank, parser.parse(s, fromGbif(rank)));

    } catch (UnparsableNameException e) {
      throw new UnparsableException(NAME_TYPE_MAP.getOrDefault(e.getType(), NameType.DOUBTFUL), e.getName());
    }
  }

  @Override
  public ParsedName parse(String scientificName) throws UnparsableException {
    return parse(scientificName, null);
  }

  @Override
  public ParsedName parseQuietly(String scientificName, @Nullable Rank rank) {
    ParsedName p;
    try {
      p = parse(scientificName, rank);

    } catch (UnparsableException e) {
      p = new ParsedName();
      p.setScientificName(scientificName);
      p.setRank(rank);
      p.setType(e.type);
      p.setParsed(false);
      p.setParsedPartially(false);
    }

    return p;
  }

  @Override
  public ParsedName parseQuietly(String scientificName) {
    return parseQuietly(scientificName, null);
  }

  @Override
  // parses the name without authorship and returns the ParsedName.canonicalName() string
  public String parseToCanonical(String scientificName, @Nullable Rank rank) {
    if (Strings.isNullOrEmpty(scientificName)) {
      return null;
    }
    try {
      ParsedName pn = parse(scientificName, rank);
      if (pn != null) {
        return pn.canonicalName();
      }
    } catch (UnparsableException e) {
      logUnparsable(e);
    }
    return null;
  }

  private static void logUnparsable(UnparsableException e) {
    if (e.type.isParsable()) {
      LOG.debug("Unparsable {} {} >>> {}", e.type, e.name, e.getMessage());
    } else {
      LOG.warn("Unparsable {} {} >>> {}", e.type, e.name, e.getMessage());
    }
  }

  @Override
  public String parseToCanonical(String scientificName) {
    return parseToCanonical(scientificName, null);
  }

  public String parseToCanonicalOrScientificName(String scientificName) {
    return parseToCanonicalOrScientificName(scientificName, null);
  }

  /**
   * Tries to parses the name without authorship and returns the ParsedName.canonicalName() string
   * For unparsable types and other UnparsableExceptions the original scientific name is returned.
   * @param rank the rank of the name if it is known externally. Helps identifying infrageneric names vs bracket authors
   */
  public String parseToCanonicalOrScientificName(String scientificName, @Nullable Rank rank) {
    if (Strings.isNullOrEmpty(scientificName)) {
      return null;
    }
    try {
      ParsedName pn = parse(scientificName, rank);
      if (pn != null) {
        return pn.canonicalName();
      }
    } catch (UnparsableException e) {
      logUnparsable(e);
    }
    return StringUtils.normalizeSpace(scientificName.trim());
  }



  private ParsedName convert(String scientificName, Rank rank, org.gbif.nameparser.api.ParsedName pn) throws UnparsableException {
    // throw unparsable for all unparsable types but placeholder and for all names that have a not parsed state
    if ((!pn.getType().isParsable() && pn.getType() != org.gbif.nameparser.api.NameType.PLACEHOLDER)
        || pn.getState() == org.gbif.nameparser.api.ParsedName.State.NONE) {
      throw new UnparsableException(gbifNameType(pn), scientificName);
    }

    ParsedName gbif = new ParsedName();

    gbif.setType(gbifNameType(pn));
    gbif.setScientificName(scientificName);

    gbif.setGenusOrAbove(pn.getGenus() != null ? pn.getGenus(): pn.getUninomial());
    gbif.setInfraGeneric(pn.getInfragenericEpithet());
    gbif.setSpecificEpithet(pn.getSpecificEpithet());
    gbif.setInfraSpecificEpithet(pn.getInfraspecificEpithet());
    gbif.setCultivarEpithet(pn.getCultivarEpithet());
    gbif.setNotho(toGbif(pn.getNotho()));
    gbif.setRank(toGbif(pn.getRank()));
    // in the old API we used null instead of unranked
    if (gbif.getRank() == Rank.UNRANKED && Rank.UNRANKED != rank) {
      gbif.setRank(null);
    }
    gbif.setStrain(pn.getPhrase());
    gbif.setSensu(pn.getTaxonomicNote());

    gbif.setAuthorship(NameFormatter.authorString(pn.getCombinationAuthorship(), false));
    gbif.setYear(pn.getCombinationAuthorship().getYear());
    gbif.setBracketAuthorship(NameFormatter.authorString(pn.getBasionymAuthorship(), false));
    gbif.setBracketYear(pn.getBasionymAuthorship().getYear());

    gbif.setNomStatus(pn.getNomenclaturalNote());
    if (pn.getEpithetQualifier() != null && !pn.getEpithetQualifier().isEmpty()) {
      StringBuilder sb = new StringBuilder();
      for (Map.Entry<NamePart, String> pq : pn.getEpithetQualifier().entrySet()) {
        if (sb.length() < 1) {
          sb.append(" ");
        }
        sb.append(pq.getValue())
          .append(" ")
          .append(pn.getEpithet(pq.getKey()));
      }
      gbif.setRemarks(sb.toString());
    }

    // we throw UnparsableException above already for State.NONE
    gbif.setParsed(true);
    gbif.setParsedPartially(pn.getState() == State.PARTIAL);

    return gbif;
  }


  public static NameType gbifNameType(org.gbif.nameparser.api.ParsedName pn) {
    NameType t;
    // detect name types that only exist in the GBIF API v1
    if (pn.isDoubtful() && pn.getWarnings().contains(Warnings.BLACKLISTED_EPITHET)) {
      t = NameType.BLACKLISTED;
    } else if (pn.isCandidatus()) {
      t = NameType.CANDIDATUS;
    } else if (pn.getCode() == NomCode.CULTIVARS || pn.getCultivarEpithet() != null) {
      t = NameType.CULTIVAR;
    } else {
      // convert all others
      t = NAME_TYPE_MAP.get(pn.getType());
    }
    // use doubtful in too good cases
    if (pn.isDoubtful() && (t == NameType.SCIENTIFIC || t == NameType.CULTIVAR)) {
      return NameType.DOUBTFUL;
    }
    return t;
  }

  public static org.gbif.api.vocabulary.NamePart toGbif(NamePart notho) {
    return convertEnum(org.gbif.api.vocabulary.NamePart.class, notho);
  }

  public static Rank toGbif(org.gbif.nameparser.api.Rank rank) {
    if (rank == null) return null;
    switch (rank) {
      case SUPERDIVISION: return Rank.SUPERPHYLUM;
      case DIVISION: return Rank.PHYLUM;
      case SUBDIVISION: return Rank.SUBPHYLUM;
      case INFRADIVISION: return Rank.INFRAPHYLUM;

      case SUPERSECTION: return Rank.INFRAGENERIC_NAME;
      case SUPERSERIES: return Rank.INFRAGENERIC_NAME;
      
      case MEGAFAMILY: return Rank.SUPRAGENERIC_NAME;
      case GRANDFAMILY: return Rank.SUPRAGENERIC_NAME;
      case EPIFAMILY: return Rank.SUPRAGENERIC_NAME;

      case GIGAORDER: return Rank.SUPRAGENERIC_NAME;
      case MIRORDER: return Rank.SUPRAGENERIC_NAME;
      case NANORDER: return Rank.SUPRAGENERIC_NAME;
      case HYPOORDER: return Rank.SUPRAGENERIC_NAME;
      case MINORDER: return Rank.SUPRAGENERIC_NAME;

      case SUBTERCLASS: return Rank.SUPRAGENERIC_NAME;

      case REALM: return Rank.SUPRAGENERIC_NAME;
      case SUBREALM: return Rank.SUPRAGENERIC_NAME;
    }
    return convertEnum(Rank.class, rank);
  }


  public static org.gbif.nameparser.api.Rank fromGbif(Rank rank) {
    if (rank == null) return null;
    switch (rank) {
      case RACE: return org.gbif.nameparser.api.Rank.PROLES;
    }
    return convertEnum(org.gbif.nameparser.api.Rank.class, rank);
  }

  /**
   * Converts an enumeration value into a constant with the exact same name from a different enumeration class.
   * In case the enumeration constant name does not exist an error is thrown.
   *
   * @param targetClass class of the target enumeration
   * @param value
   * @throws IllegalArgumentException in case the enumeration name does not exist in the target class
   */
  private static <G extends Enum<G>> G convertEnum(Class<G> targetClass, Enum<?> value) {
    try {
      return value == null ? null : Enum.valueOf(targetClass, value.name());
    } catch (IllegalArgumentException e) {
      LOG.warn("Unable to convert {} into {}", value, targetClass);
      return null;
    }
  }
}
