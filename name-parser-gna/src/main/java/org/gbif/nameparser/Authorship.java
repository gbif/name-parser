package org.gbif.nameparser;

import com.google.common.collect.Maps;
import scala.Option;
import scala.Some;
import scala.collection.Iterator;
import scala.collection.JavaConversions;
import scala.collection.immutable.List;

import java.util.Map;
import java.util.Optional;

/**
 *
 */
public class Authorship {
    private final Map<String, Object> combAuthorship;
    private final Map<String, Object> basAuthorship;

    private static java.util.Map<String, Object> someMap(Object obj) {
        if (obj instanceof Some) {
            return JavaConversions.mapAsJavaMap(((Some<scala.collection.Map>) obj).get());
        } else if (obj instanceof scala.collection.Map) {
            return JavaConversions.mapAsJavaMap((scala.collection.Map) obj);
        }
        return Maps.newHashMap();
    }

    /**
     * Lazily initializes the authorship maps when needed.
     * This needs to be called manually before any authorship getters
     */
    public Authorship(Map<String, Object> map) {
        if (map.containsKey("authorship")) {
            java.util.Map<String, Object> auth = someMap(map.get("authorship"));
            java.util.Map<String, Object> comb = someMap(auth.get("combination_authorship"));
            java.util.Map<String, Object> bas  = someMap(auth.get("basionym_authorship"));
            // in case of just a combination author it comes as the basionym author, swap!
            if (comb.isEmpty() && !bas.isEmpty() && !((String)auth.get("value")).startsWith("(")) {
                combAuthorship = bas;
                basAuthorship = comb;
            } else {
                combAuthorship = comb;
                basAuthorship = bas;
            }
        } else {
            combAuthorship = Maps.newHashMap();
            basAuthorship = Maps.newHashMap();
        }
    }

    public String getCombAuthor() {
        return authorString(combAuthorship);
    }

    public String getBasAuthor() {
        return authorString(basAuthorship);
    }

    public String getCombYear() {
        return mapValue(combAuthorship.get("year"));
    }

    public String getBasYear() {
        return mapValue(basAuthorship.get("year"));
    }

    private static String authorString(java.util.Map<String, Object> auth) {
        if (auth.containsKey("authors")) {
            StringBuilder sb = new StringBuilder();
            Iterator iter = ((List) auth.get("authors")).iterator();
            while (iter.hasNext()) {
                Object next = iter.next();
                if (sb.length() > 0) {
                    if (iter.hasNext()) {
                        sb.append(", ");
                    } else {
                        // this is the last author from a list
                        sb.append(" & ");
                    }
                }
                sb.append(next);
            }
            // add ex authors
            Optional<Object> ex = ScinameMap.mapValue(auth, "ex_authors");
            if (ex.isPresent()) {
                boolean first = true;
                iter = ((List) ex.get()).iterator();
                if (iter.hasNext()) {
                    sb.append(" ex ");
                }
                while (iter.hasNext()) {
                    Object next = iter.next();
                    if (first) {
                        first = false;
                    } else {
                        if (iter.hasNext()) {
                            sb.append(", ");
                        } else {
                            // this is the last author from a list
                            sb.append(" & ");
                        }
                    }
                    sb.append(next);
                }
            }
            return sb.toString();
        }
        return null;
    }

    private static String mapValue(Object val) {
        if (val == null || val instanceof Option) {
            return null;
        }
        return (String) JavaConversions.mapAsJavaMap((scala.collection.Map) val).get("value");
    }
}
