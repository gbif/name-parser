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

  public void setName(String sciname, ParsedName pn) {
    names.put(ParsingJob.preClean(sciname, null), pn);
  }

  public ParsedName deleteName(String name) {
    return names.remove(ParsingJob.preClean(name, null));
  }

  public void setAuthorship(String authorship, ParsedAuthorship pn) {
    authorships.put(ParsingJob.preClean(authorship, null), pn);
  }

  public ParsedAuthorship deleteAuthorship(String authorship) {
    return authorships.remove(ParsingJob.preClean(authorship, null));
  }
}
