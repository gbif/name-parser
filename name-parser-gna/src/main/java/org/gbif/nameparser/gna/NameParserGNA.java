package org.gbif.nameparser.gna;

import com.google.common.base.CharMatcher;
import com.google.common.collect.Lists;
import org.gbif.nameparser.api.*;
import org.gbif.nameparser.util.RankUtils;
import org.globalnames.parser.ScientificName;
import org.globalnames.parser.ScientificNameParser;
import org.globalnames.parser.WarningInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Option;
import scala.collection.Iterator;
import scala.collection.Map;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Wrapper around the GNA Name parser to deal with ParsedName and API.
 */
public class NameParserGNA implements NameParser {
  private static final Logger LOG = LoggerFactory.getLogger(NameParserGNA.class);

  public static final NameParser PARSER = new NameParserGNA();
  private final static Pattern PLACEHOLDER = Pattern.compile("(?:unnamed|mixed|unassigned|unallocated|unplaced|undetermined|unclassified|uncultured|unknown|unspecified|uncertain|incertae sedis|not assigned|awaiting allocation|temp|dummy)", Pattern.CASE_INSENSITIVE);
  private final static CharMatcher EMPTY = CharMatcher.invisible().and(CharMatcher.whitespace());
  private final ScientificNameParser parser = ScientificNameParser.instance();

  @Override
  public ParsedName parse(String scientificName) throws UnparsableNameException {
    return parse(scientificName, null);
  }

  @Override
  public ParsedName parse(String scientificName, Rank rank) throws UnparsableNameException {
    if (scientificName == null || EMPTY.matchesAllOf(scientificName)) {
      return null;
    }

    ScientificNameParser.Result sn = parser.fromString(scientificName);
    ParsedName n = convert(scientificName, sn);

    NameType type = preparse(scientificName);
    if (type != null) {
      n.setType(type);
    }
    return n;
  }

  /**
   * Catch known parsing oddities the GNA Parser does to expect or tries to capture.
   * E.g. placeholder names
   * @return a name instance or null if preparsing did not capture anything
   */
  private NameType preparse(String name) {
    // placeholders names
    if (PLACEHOLDER.matcher(name).find()) {
      return NameType.PLACEHOLDER;
    }
    return null;
  }

  private static ParsedName build(NameType type) {
    ParsedName n = new ParsedName();
    n.setType(type);
    return n;
  }

  private ParsedName convert(String name, ScientificNameParser.Result result) throws UnparsableNameException {
    ParsedName n = new ParsedName();

    Iterator<WarningInfo> iter = result.preprocessorResult().warnings().seq().iterator();
    while (iter.hasNext()) {
      WarningInfo warn = iter.next();
      LOG.warn(warn.toString());
    }

    if (result.preprocessorResult().noParse() || result.detailed() instanceof org.json4s.JsonAST.JNothing$) {
      n.setType(NameType.NO_NAME);

    } else if (result.preprocessorResult().virus()) {
      n.setType(NameType.VIRUS);

    } else {
      ScientificName sn = result.scientificName();

      if (sn.surrogate()) {
        n.setType(NameType.PLACEHOLDER);

      } else if (sn.hybrid().isDefined() && (Boolean) sn.hybrid().get()) {
        n.setType(NameType.HYBRID_FORMULA);

      } else {

        ScinameMap map = ScinameMap.create(name, result);
        n.setType(typeFromQuality(sn.quality()));
        Option<Epithet> authorship = Option.empty();
        Option<Epithet> uninomial = map.uninomial();
        if (uninomial.isDefined()) {
          // we differ between uninomials and infragenerics that have a genus
          if (uninomial.get().hasParent()) {
            // infrageneric
            n.setGenus(uninomial.get().getParent());
            n.setInfragenericEpithet(uninomial.get().getEpithet());
            Rank rankCol = RankUtils.inferRank(uninomial.get().getRank());
            n.setRank(rankCol);

          } else {
            // use scientificName for uninomials
            n.setUninomial(uninomial.get().getEpithet());
          }
          //pn.setGenus(uninomial.get().getEpithet());
          authorship = uninomial;

        } else {
          // bi/trinomials do not come with a uninomial
          Option<Epithet> genus = map.genus();
          if (genus.isDefined()) {
            n.setGenus(genus.get().getEpithet());
            authorship = genus;
          }

          Option<Epithet> infraGenus = map.infraGeneric();
          if (infraGenus.isDefined()) {
            n.setInfragenericEpithet(infraGenus.get().getEpithet());
            authorship = infraGenus;
          }

          Rank parsedRank = null;
          Option<Epithet> species = map.specificEpithet();
          if (species.isDefined()) {
            n.setSpecificEpithet(species.get().getEpithet());
            authorship = species;
            parsedRank = Rank.SPECIES;
          }

          Option<Epithet> infraSpecies = map.infraSpecificEpithet();
          if (infraSpecies.isDefined()) {
            n.setInfraspecificEpithet(infraSpecies.get().getEpithet());
            Rank rankCol = RankUtils.inferRank(infraSpecies.get().getRank());
            if (rankCol != null) {
              n.setRank(rankCol);
            } else {
              parsedRank = map.infraSpecificEpithetCount() > 1 ? Rank.INFRASUBSPECIFIC_NAME : Rank.INFRASPECIFIC_NAME;
            }
            authorship = infraSpecies;
          }

          if (parsedRank != null && (n.getRank() == null || n.getRank().equals(Rank.UNRANKED))) {
            n.setRank(parsedRank);
          }
        }

        // set authorship from the lowest epithet
        setFullAuthorship(n, authorship);

        //TODO: see if we can handle annotations, do they map to ParsedName at all ???
        //Optional anno = map.annotation();
        //if (anno.isPresent()) {
        //  System.out.println(anno.get().getClass());
        //  System.out.println(anno.get());
        //}
      }
    }
    return n;
  }

  private void setFullAuthorship(ParsedName n, Option<Epithet> epi) {
    if (epi.isDefined() && epi.get().hasAuthorship()) {
      Authorship auth = epi.get().getAuthorship();
      setAuthorship(n.getCombinationAuthorship(), auth.combination);
      setAuthorship(n.getBasionymAuthorship(), auth.basionym);
    }
  }

  private void setAuthorship(org.gbif.nameparser.api.Authorship auth, Map<String, Object> map) {
    // if ex authors exist we will swap them to follow the botanical order convention by default
    // as ex authors are mainly used in the botanical world
    List<String> ex = Lists.newArrayList(Authorship.authors(map, true));
    if (ex.isEmpty()) {
      auth.setAuthors(Lists.newArrayList(Authorship.authors(map, false)));
    } else {
      auth.setAuthors(ex);
      auth.setExAuthors(Lists.newArrayList(Authorship.authors(map, false)));
    }
    auth.setYear(Authorship.year(map));
  }

  private static NameType typeFromQuality(Integer quality) {
    //TODO: log issues in case of 3 and higher???
    switch (quality) {
      case 1: return NameType.SCIENTIFIC;
      case 2: return NameType.SCIENTIFIC;
      case 3: return NameType.SCIENTIFIC;
    }
    return NameType.NO_NAME;
  }

}
