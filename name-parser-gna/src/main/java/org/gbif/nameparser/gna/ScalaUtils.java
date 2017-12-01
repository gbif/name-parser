package org.gbif.nameparser.gna;

import scala.Option;
import scala.collection.Map;

/**
 *
 */
public class ScalaUtils {
  private static Map<String, Object> EMPTY_MAP = scala.collection.immutable.Map$.MODULE$.empty();

  private ScalaUtils() {
  }

  /**
   * Unwraps nested Options. No idea why these come out of the parser, but we gotta deal with that.
   */
  public static Option unwrap(Option o) {
    if (o.isDefined() && o.get() instanceof Option) {
      return unwrap((Option)o.get());
    }
    return o;
  }

  /**
   * Takes a scala map, None or Some of a map and returns a scala map which will be empty in case of None values.
   * Deals with nested, wrapped Options.
   * @return a scala map, empty or full but never null
   */
  public static Map<String, Object> optionMap(Object obj) {
    if (obj instanceof Option) {
      Option opt = unwrap( (Option) obj);
      return opt.isEmpty() ? EMPTY_MAP : (Map<String, Object>) opt.get();

    } else if (obj instanceof Map) {
      return (Map) obj;
    }
    return EMPTY_MAP;
  }

  /**
   * @return the string value of a map entry or null if not existing or with value None
   */
  public static String mapString(Map map, String key) {
      Option val = unwrap(map.get(key));
      if (val.isDefined()) {
        return (String) val.get();
      }
      return null;
  }

}
