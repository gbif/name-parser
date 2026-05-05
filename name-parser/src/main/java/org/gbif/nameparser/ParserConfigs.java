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
import com.google.gson.JsonSyntaxException;
import org.apache.commons.lang3.StringUtils;
import org.gbif.nameparser.api.NameType;
import org.gbif.nameparser.api.ParsedAuthorship;
import org.gbif.nameparser.api.ParsedName;
import org.gbif.nameparser.api.Rank;
import org.gbif.nameparser.util.UnicodeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class ParserConfigs {
  private static final Logger LOG = LoggerFactory.getLogger(ParserConfigs.class);
  private final static URI CONFIG_DEFAULT_URL = URI.create("https://api.checklistbank.org/parser/name/config?limit=1000");
  private final static String INTERNAL_DEFAULTS_RESOURCE = "/nameparser/prefix-epithet-binomials.tsv";
  private final static Pattern MORE_WS = Pattern.compile("[\\s,.+'\"&_—|-]+");
  private final static Pattern MORE_WS2 = Pattern.compile("\\s*([(){}\\[\\]]+)\\s*");

  private final Map<String, ParsedName> names = new HashMap<>();
  private final Map<String, ParsedAuthorship> authorships = new HashMap<>();

  public ParsedName forName(String name) {
    return names.get(norm(name));
  }

  /**
   * Looks up a binomial override that matches the leading "Genus epithet" of the input
   * and returns both the override and the trailing slice (typically authorship) so the
   * caller can parse that separately. Returns null if no binomial override matches the
   * first two whitespace-separated tokens.
   */
  public PrefixMatch forPrefix(String name) {
    if (name == null) return null;
    String trimmed = name.trim();
    int firstSpace = trimmed.indexOf(' ');
    if (firstSpace < 0) return null;
    int secondSpace = trimmed.indexOf(' ', firstSpace + 1);
    String binomial;
    String remainder;
    if (secondSpace < 0) {
      binomial = trimmed;
      remainder = null;
    } else {
      binomial = trimmed.substring(0, secondSpace);
      remainder = trimmed.substring(secondSpace + 1).trim();
      if (remainder.isEmpty()) remainder = null;
    }
    ParsedName pn = names.get(norm(binomial));
    if (pn == null) return null;
    return new PrefixMatch(pn, remainder);
  }

  public static final class PrefixMatch {
    public final ParsedName parsedName;
    public final String remainder;
    PrefixMatch(ParsedName parsedName, String remainder) {
      this.parsedName = parsedName;
      this.remainder = remainder;
    }
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
   * Registers a curated list of binomials whose specific epithet looks like an author
   * particle (la, van, von, zu, da, du, ...) — see prefix-epithet-binomials.tsv.
   * These are matched by {@link #forPrefix(String)} so the parser can recover the
   * binomial from inputs that the regex would otherwise interpret as a monomial with
   * a multi-token author surname.
   *
   * @return number of binomials registered
   */
  public int loadInternalDefaults() {
    int loaded = 0;
    try (InputStream in = ParserConfigs.class.getResourceAsStream(INTERNAL_DEFAULTS_RESOURCE);
         BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
      String line;
      while ((line = r.readLine()) != null) {
        if (line.isBlank() || line.startsWith("#")) continue;
        String[] parts = line.split("\t", 2);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
          LOG.warn("Skipping malformed line in {}: {}", INTERNAL_DEFAULTS_RESOURCE, line);
          continue;
        }
        String genus = parts[0].trim();
        String epithet = parts[1].trim();
        ParsedName pn = new ParsedName();
        pn.setGenus(genus);
        pn.setSpecificEpithet(epithet);
        pn.setRank(Rank.SPECIES);
        pn.setType(NameType.SCIENTIFIC);
        pn.setState(ParsedName.State.COMPLETE);
        add(genus + " " + epithet, null, pn);
        loaded++;
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read " + INTERNAL_DEFAULTS_RESOURCE + " from classpath", e);
    }
    LOG.debug("Loaded {} internal default parser configs", loaded);
    return loaded;
  }

/**
 * Loads all configs from the central ChecklistBank API
 * @return number of configs added
 */
  public int loadFromCLB() throws IOException, InterruptedException {
      return load(CONFIG_DEFAULT_URL);
  }

    /**
     * Loads all configs from the given URL
     * @return number of configs added
     */
  public int load(URI configUrl) throws IOException, JsonSyntaxException, InterruptedException {
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
            .uri(configUrl)
            .header("Accept", "application/json")
            .GET()
            .build();
    HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());

    Gson gson = new Gson();
    //Type type = new TypeToken<ArrayList<ParsedName>>(){}.getType();
      Wrapper configs = null;
      configs = gson.fromJson(resp.body(), Wrapper.class);
      int loaded = 0;
      int failed = 0;
      if (configs.result != null) {
          for (var pnc : configs.result) {
            try {
              String[] ids = pnc.id.split("\\|");
              add(ids[0], ids[1], pnc);
              loaded++;
            } catch (Exception e) {
              LOG.warn("Failed to load parser config {}: {}", pnc.id, pnc, e);
              failed++;
            }
          }
      }
      LOG.info("Loaded {} parser configs from ChecklistBank. {} failed", loaded, failed);
    return loaded - failed;
  }

  static class Wrapper {
    public List<PNConfig> result;
  }
  static class PNConfig extends ParsedName{
    public String id;
  }
}
