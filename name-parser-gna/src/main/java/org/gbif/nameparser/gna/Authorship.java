package org.gbif.nameparser.gna;

import com.google.common.collect.Lists;
import scala.Option;
import scala.collection.JavaConversions;
import scala.collection.Map;

import java.util.List;

/**
 *
 */
public class Authorship {
  public final Map<String, Object> combination;
  public final Map<String, Object> basionym;


  /**
   * Lazily initializes the authorship maps when needed.
   * This needs to be called manually before any authorship getters
   */
  Authorship(Map<String, Object> authorshipMap) {
    Map<String, Object> comb = ScalaUtils.optionMap(authorshipMap.get("combination_authorship"));
    Map<String, Object> bas = ScalaUtils.optionMap(authorshipMap.get("basionym_authorship"));
    // in case of just a combination author it comes as the basionym author, swap!
    String authorship = (String) authorshipMap.get("value").get();
    if (comb.isEmpty() && !bas.isEmpty() && !authorship.startsWith("(")) {
      combination = bas;
      basionym = comb;
    } else {
      combination = comb;
      basionym = bas;
    }
  }

  public static String year(Map<String, Object> auth) {
    return mapValueString(auth,"year");
  }

  public static List<String> authors(Map<String, Object> auth, boolean ex) {
    String key = ex ? "ex_authors" : "authors";
    if (auth.contains(key)) {
      Option val = ScalaUtils.unwrap(auth.get(key));
      if (val.isDefined()) {
        return JavaConversions.seqAsJavaList((scala.collection.immutable.List) val.get());
      }
    }
    return Lists.newArrayList();
  }

  /**
   * Return the nested map value for the key and use "value" as key for the second,nested map.
   */
  private static String mapValueString(Map map, String key) {
    Option val = ScalaUtils.unwrap(map.get(key));
    if (val.isDefined()) {
      return ScalaUtils.mapString((Map)val.get(), "value");
    }
    return null;
  }
}
