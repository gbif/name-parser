package org.gbif.nameparser;

import org.gbif.api.exception.UnparsableException;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.service.checklistbank.NameParser;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.Rank;
import org.globalnames.parser.ScientificName;
import org.globalnames.parser.ScientificNameParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Option;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Wrapper around the GNA parser to match our ParsedName class.
 */
public class GNANameParser implements NameParser {
  private static Logger LOG = LoggerFactory.getLogger(GNANameParser.class);

  private final ScientificNameParser parser = ScientificNameParser.instance();

  public ParsedName parse(final String scientificName, @Nullable Rank rank) throws UnparsableException {
    return convert(scientificName, rank, parser.fromString(scientificName));
  }

  @Override
  public ParsedName parse(String s) throws UnparsableException {
    return parse(s, null);
  }

  public ParsedName parseQuietly(final String scientificName, @Nullable Rank rank) {
    try {
      return parse(scientificName, rank);

    } catch (UnparsableException e) {
      ParsedName pn = new ParsedName();
      pn.setType(e.type);
      pn.setScientificName(scientificName);
      pn.setRank(rank);
      return pn;
    }
  }

  @Override
  public ParsedName parseQuietly(String s) {
    return parseQuietly(s, null);
  }

  public String parseToCanonical(String name, @Nullable Rank rank) {
    ScientificNameParser.Result sn = parser.fromString(name);
    return sn.canonized(false).get();
  }

  @Override
  public String parseToCanonical(String s) {
    return parseToCanonical(s, null);
  }

  @Deprecated
  public String normalize(String x) {
    return x;
  }

  private ParsedName convert(String name, @Nullable Rank rank, ScientificNameParser.Result result) throws UnparsableException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("GNA result: %s", result);
    }

    if (result.preprocessorResult().virus()) {
      throw new UnparsableException(NameType.VIRUS, name);
    }

    ParsedName pn = new ParsedName();
    pn.setScientificName(name);
    pn.setRank(rank);
    try {
      ScientificName sn = result.scientificName();

      Option<String> canonical = result.canonized(true);
      pn.setParsed(canonical.isDefined());

      if (result.scientificName().surrogate()) {
        pn.setType(NameType.PLACEHOLDER);

      } else if (sn.hybrid().isDefined() && (Boolean) sn.hybrid().get()) {
        throw new UnparsableException(NameType.HYBRID, name);

      } else if (!pn.isParsed()) {
        throw new UnparsableException(NameType.NO_NAME, name);

      } else {
        pn.setType(typeFromQuality(sn.quality()));
        ScinameMap map = ScinameMap.create(result);

        Optional<Epithet> authorship = Optional.empty();
        Optional<Epithet> uninomial = map.uninomial();
        if (uninomial.isPresent()) {
          pn.setGenusOrAbove(uninomial.get().getEpithet());
          authorship = uninomial;

        } else {
          // bi/trinomials do not come with a uninomial
          Optional<Epithet> genus = map.genus();
          if (genus.isPresent()) {
            pn.setGenusOrAbove(genus.get().getEpithet());
            authorship = genus;
          }

          Optional<Epithet> infraGenus = map.infraGeneric();
          if (infraGenus.isPresent()) {
            pn.setInfraGeneric(infraGenus.get().getEpithet());
            authorship = infraGenus;
          }

          Optional<Epithet> species = map.specificEpithet();
          if (species.isPresent()) {
            pn.setSpecificEpithet(species.get().getEpithet());
            authorship = species;
          }

          Optional<Epithet> infraSpecies = map.infraSpecificEpithet();
          if (infraSpecies.isPresent()) {
            pn.setInfraSpecificEpithet(infraSpecies.get().getEpithet());
            Rank rank2 = GnaRankUtils.inferRank(infraSpecies.get().getRank());
            if (rank2 != null) {
              pn.setRank(rank2);
            }
            authorship = infraSpecies;
          }
        }

        // set authorship from the lowest epithet
        setAuthorship(pn, authorship);

        //TODO: see if we can handle annotations, do they map to ParsedName at all ???
        //Optional anno = map.annotation();
        //if (anno.isPresent()) {
        //  System.out.println(anno.get().getClass());
        //  System.out.println(anno.get());
        //}
      }

    } catch (UnparsableException e) {
        // rethrow UnparsableException as we throw these on purpose
        throw e;
    } catch (Exception e) {
      // convert all other unhandled exceptions into UnparsableException
        throw new UnparsableException(NameType.DOUBTFUL, name);
    }

    //System.out.println("  GNA pn: " + pn.canonicalNameComplete());
    return pn;
  }

  private void setAuthorship(ParsedName pn, Optional<Epithet> epi) {
    if (epi.isPresent()) {
      Authorship auth = epi.get().getAuthorship();
      pn.setAuthorship(auth.getCombAuthor());
      pn.setYear(auth.getCombYear());
      pn.setBracketAuthorship(auth.getBasAuthor());
      pn.setBracketYear(auth.getBasYear());
    }
  }

  private static NameType typeFromQuality(Integer quality) {
    switch (quality) {
      case 1: return NameType.SCIENTIFIC;
      case 2: return NameType.SCIENTIFIC;
      case 3: return NameType.DOUBTFUL;
    }
    return NameType.DOUBTFUL;
  }

  public static void main (String[] args) throws UnparsableException {
    GNANameParser parser = new GNANameParser();
    ParsedName pn = parser.parse("Chorizagrotis auxiliaris entomopoxvirus", null);
    System.out.println(pn.getScientificName());
    System.out.println(pn.canonicalNameComplete());
  }
}
