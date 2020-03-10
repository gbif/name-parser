package org.gbif.nameparser;

import org.gbif.nameparser.api.ParsedAuthorship;
import org.gbif.nameparser.api.ParsedName;

import java.util.HashMap;
import java.util.Map;

public class ParserConfigs {

  private final Map<String, ParsedName> names = new HashMap<>();
  private final Map<String, ParsedAuthorship> authorships = new HashMap<>();

  public ParsedName forName(String name) {
    return names.get(norm(name));
  }

  public ParsedAuthorship forAuthorship(String authorship) {
    return authorships.get(norm(authorship));
  }

  public void setName(String name, ParsedName pn) {
    names.put(norm(name), pn);
  }

  public ParsedName deleteName(String name) {
    return names.remove(norm(name));
  }

  public void setAuthorship(String authorship, ParsedAuthorship pn) {
    authorships.put(norm(authorship), pn);
  }

  public ParsedAuthorship deleteAuthorship(String authorship) {
    return authorships.remove(norm(authorship));
  }

  private static String norm(String x) {
    return ParsingJob.preClean(x, null).toLowerCase();
  }
}
