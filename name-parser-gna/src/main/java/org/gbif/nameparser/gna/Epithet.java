package org.gbif.nameparser.gna;

import scala.collection.Map;

/**
 *
 */
public class Epithet {
  private final String epithet;
  private final String parent;
  private final String rank;
  private final Authorship authorship;

  public Epithet(Map map) {
    epithet = ScalaUtils.mapString(map, "value");
    parent = ScalaUtils.mapString(map, "parent");
    rank = ScalaUtils.mapString(map, "rank");
    if (map.contains("authorship")) {
      authorship = new Authorship((Map<String, Object>) map.get("authorship").get());
    } else {
      authorship = null;
    }
  }

  public String getEpithet() {
    return epithet;
  }

  public String getParent() {
    return parent;
  }

  public boolean hasParent() {
    return parent != null;
  }

  public String getRank() {
    return rank;
  }

  public Authorship getAuthorship() {
    return authorship;
  }

  public boolean hasAuthorship() {
    return authorship != null;
  }

}
