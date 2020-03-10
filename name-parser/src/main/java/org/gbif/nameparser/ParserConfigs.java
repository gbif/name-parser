package org.gbif.nameparser;

import org.gbif.nameparser.api.ParsedAuthorship;
import org.gbif.nameparser.api.ParsedName;

import java.util.HashMap;
import java.util.Map;

public class ParserConfigs {

  private final Map<String, ParsedName> names = new HashMap<>();
  private final Map<String, ParsedAuthorship> authorships = new HashMap<>();

  public ParsedName forName(String name) {
    name = ParsingJob.preClean(name, null);
    return names.get(name);
  }

  public ParsedAuthorship forAuthorship(String authorship) {
    authorship = ParsingJob.preClean(authorship, null);
    return authorships.get(authorship);
  }
}
