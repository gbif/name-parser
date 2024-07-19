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

import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.gbif.nameparser.api.NameType;
import org.gbif.nameparser.api.ParsedAuthorship;
import org.gbif.nameparser.api.ParsedName;
import org.gbif.nameparser.util.UnicodeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class ParserConfigs {
  private static final Logger LOG = LoggerFactory.getLogger(ParserConfigs.class);
  private final static URI CONFIG_URL = URI.create("https://api.checklistbank.org/parser/name/config?limit=1000");
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
    x = UnicodeUtils.decompose(x);
    x = MORE_WS.matcher(x).replaceAll(" ");
    return MORE_WS2.matcher(x).replaceAll(" $1 ");
  }

  private static String concat(String name, String authorship) {
    if (!StringUtils.isBlank(authorship)) {
      return name.trim() + ' ' + authorship.trim();
    }
    return name.trim();
  }
  /**
   * Adds new parser configurations for a name & authorship combination.
   * Adds 3 entries to the configs if both are given:
   * 1. full name with authorship
   * 2. canonical name without authorship
   * 3. authorship alone
   * @param scientificName the raw scientific name without authorship
   * @param authorship optional raw authorship
   * @param pn the expected parsed name
   */
  public void add(String scientificName, @Nullable String authorship, ParsedName pn){
    LOG.debug("Add config for {} {}", scientificName, authorship);
    // defaults
    if (pn.getType() == null) {
      pn.setType(NameType.SCIENTIFIC);
    }
    if (pn.getState() == null || pn.getState() == ParsedName.State.NONE) {
      pn.setState(ParsedName.State.COMPLETE); // if we leave state None we get unparsed issues when parsing this name
    }
    setName(concat(scientificName, authorship), pn);
    // configure name without authorship and authorship standalone if we have that
    if (authorship != null && pn.hasAuthorship()) {
      setAuthorship(authorship, pn);
      ParsedName pnNoAuthor = new ParsedName();
      pnNoAuthor.copy(pn);
      pnNoAuthor.setCombinationAuthorship(null);
      pnNoAuthor.setBasionymAuthorship(null);
      pnNoAuthor.setSanctioningAuthor(null);
      setName(scientificName, pnNoAuthor);
    }
  }

  /**
   * Loads all configs from the central ChecklistBank API
   * @return number of configs added
   */
  public int loadFromCLB() throws IOException, InterruptedException {
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
            .uri(CONFIG_URL)
            .header("Accept", "application/json")
            .GET()
            .build();
    HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());

    Gson gson = new Gson();
    //Type type = new TypeToken<ArrayList<ParsedName>>(){}.getType();
    var configs = gson.fromJson(resp.body(), Wrapper.class);
    int failed = 0;
    for (var pnc : configs.result) {
      try {
        String[] ids = pnc.id.split("\\|");
        add(ids[0], ids[1], pnc);
      } catch (Exception e) {
        LOG.warn("Failed to load parser config {}: {}", pnc.id, pnc, e);
        failed++;
      }
    }
    LOG.info("Loaded {} parser configs from ChecklistBank. {} failed", configs.result.size(), failed);
    return configs.result.size() - failed;
  }

  static class Wrapper {
    public List<PNConfig> result;
  }
  static class PNConfig extends ParsedName{
    public String id;
  }
}
