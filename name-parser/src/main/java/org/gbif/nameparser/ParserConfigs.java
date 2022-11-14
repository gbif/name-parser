/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.nameparser;

import org.gbif.nameparser.api.ParsedAuthorship;
import org.gbif.nameparser.api.ParsedName;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class ParserConfigs {
  private final static Pattern MORE_WS = Pattern.compile("[\\s,.+'\"&_—|-]+");
  private final static Pattern MORE_WS2 = Pattern.compile("\\s*([(){}\\[\\]]+)\\s*");

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

  static String norm(String x) {
    x = ParsingJob.preClean(x, null).toLowerCase();
    x = MORE_WS.matcher(x).replaceAll(" ");
    return MORE_WS2.matcher(x).replaceAll(" $1 ");
  }
}
