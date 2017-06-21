package org.gbif.nameparser;

import scala.Option;
import scala.collection.JavaConversions;
import scala.collection.Map;

/**
 *
 */
public class Epithet {
    private final java.util.Map<String, Object> map;

    public Epithet(Map map) {
        this.map = JavaConversions.mapAsJavaMap(map);
    }

    public String getEpithet() {
        return str(map.get("value"));
    }

    public String getRank() {
        return str(map.get("rank"));
    }

    public Authorship getAuthorship() {
        return new Authorship(map);
    }

    private static String str(Object val) {
        if (val instanceof Option) {
            return null;
        }
        return (String) val;
    }
}
