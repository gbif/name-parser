package org.gbif.nameparser;

import org.gbif.api.exception.UnparsableException;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.service.checklistbank.NameParser;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.Rank;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.globalnames.parser.ScientificNameParser;

/**
 * Wrapper around the GNA parser to match our ParsedName class.
 */
public class GNANameParser implements NameParser {
  private final ScientificNameParser parser = ScientificNameParser.instance();
  private final ObjectMapper mapper = new ObjectMapper();
  private static final Joiner authorJoiner = Joiner.on(", ").skipNulls();

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

  private ParsedName convert(String name, @Nullable Rank rank, ScientificNameParser.Result sn) {
    ParsedName pn = new ParsedName();

    pn.setScientificName(name);
    pn.setRank(rank);

    try {
      System.out.println(sn.renderCompactJson());
      Map<String,Object> json = mapper.readValue(sn.renderCompactJson(), Map.class);

      pn.setParsed(bool(json, "parsed"));
      if (!pn.isParsed()) {
        pn.setType(NameType.NO_NAME);

      } else {
        pn.setType(typeFromQuality((Integer)json.get("quality")));
        final boolean hybrid = bool(json, "hybrid");
        if (hybrid) {
          pn.setType(NameType.HYBRID);

        } else {
          List<Map<String,Object>> details = (List<Map<String,Object>>) json.get("details");

          if (details.size()>1) {
            throw new IllegalArgumentException(details.toString());
          }
          Map<String,Object> detail = details.get(0);
          pn.setGenusOrAbove(strVal(detail, "genus"));
          pn.setSpecificEpithet(strVal(detail, "specific_epithet"));
          pn.setInfraGeneric(strVal(detail, "infrageneric_epithet"));

          List<Map<String,Object>> infraEpithets = (List<Map<String,Object>>) detail.getOrDefault("infraspecific_epithets", Lists.<Map<String,Object>>newLinkedList());
          if (!infraEpithets.isEmpty()) {
            // use last one
            Map<String,Object> infraEpi = infraEpithets.get(infraEpithets.size()-1);
            pn.setInfraSpecificEpithet(str(infraEpi, "value"));

            if (infraEpi.containsKey("authorship")) {
              Map<String,Object> authorship = (Map<String, Object>) infraEpi.get("authorship");
              if (authorship.containsKey("basionym_authorship")) {
                Map<String,Object> bas = (Map<String, Object>) authorship.get("basionym_authorship");
                pn.setBracketAuthorship(authors(bas));
                pn.setBracketYear(strVal(bas, "year"));
              }
              if (authorship.containsKey("combination_authorship")) {
                Map<String,Object> comb = (Map<String, Object>) authorship.get("combination_authorship");
                pn.setAuthorship(authors(comb));
                pn.setYear(strVal(comb, "year"));
              }
            }
            pn.setInfraSpecificEpithet(str(infraEpi, "value"));
          }
        }
      }

    } catch (IOException e) {
      pn.setParsed(false);
      pn.setType(NameType.DOUBTFUL);
    }

    System.out.println(pn);
    return pn;
  }

  private static String authors(Map<String,Object> map) {
    if (map.containsKey("authors")) {
      List<String> authors = (List<String>) map.get("authors");
      return authorJoiner.join(authors);
    }
    return null;
  }
  private static String str(Map<String,Object> map, String key) {
    return (String) map.getOrDefault(key, null);
  }
  private static String strVal(Map<String,Object> map, String key) {
    if (map.containsKey(key)) {
      return ((Map<String, String>) map.get(key)).get("value");
    }
    return null;
  }
  private static Integer num(Map<String,Object> map, String key) {
    return (Integer) map.getOrDefault(key, null);
  }
  private static Boolean bool(Map<String,Object> map, String key) {
    return (Boolean)map.getOrDefault(key, false);
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
    parser.parse("Neobisium (Neobisium) carcinoides Döring balcanicum (Klausen et al., 1879) Hadži & Pamphlatschki, 1937", null);
  }
}
