package org.gbif.nameparser;

import org.globalnames.parser.ScientificNameParser;
import scala.Option;
import scala.collection.JavaConversions;
import scala.collection.Map;

import java.util.Optional;

/**
 *
 */
public class ScinameMap {
    private final java.util.Map<String, Object> map;


    public static ScinameMap create(ScientificNameParser.Result result) {
        return new ScinameMap((scala.collection.immutable.Map)
                ((org.json4s.JsonAST.JArray)result.detailed()).values().iterator().next()
        );
    }

    private ScinameMap(Map map) {
        this.map = JavaConversions.mapAsJavaMap(map);
    }

    public Optional<Epithet> uninomial() {
        return epithet("uninomial");
    }

    public Optional<Epithet> genus() {
        return epithet("genus");
    }

    public Optional<Epithet> infraGeneric() {
        return epithet("infrageneric_epithet");
    }

    public Optional<Epithet> specificEpithet() {
        return epithet("specific_epithet");
    }

    public Optional<Epithet> infraSpecificEpithet() {
        Optional opt = mapValue(map, "infraspecific_epithets");
        if (opt.isPresent()) {
            scala.collection.immutable.List list = (scala.collection.immutable.List) opt.get();
            if (list.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new Epithet((Map) list.last()));
        }
        return Optional.empty();
    }

    Optional<Object> annotation() {
        return mapValue(map, "annotation_identification");
    }

    static Optional<Object> mapValue(java.util.Map<String, Object> map, String key) {
        if (map.containsKey(key) && !(map.get(key) instanceof Option)) {
            return Optional.of(map.get(key));
        }
        return Optional.empty();
    }

    private Optional<Epithet> epithet(String key) {
        Optional val = mapValue(this.map, key);
        if (val.isPresent()) {
            return Optional.of(new Epithet((Map) val.get()));
        }
        return Optional.empty();
    }

}
