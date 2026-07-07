package org.gbif.nameparser.cli.llm;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * Minimal client for the Anthropic Messages API, built on the JDK's
 * {@link HttpClient} so the CLI needs no extra dependency. It sends one judging
 * batch per request and uses <b>structured outputs</b> ({@code output_config.format}
 * with a JSON schema) so the reply is guaranteed to be a JSON object matching
 * {@link Verdict} — no brittle free-text parsing.
 *
 * <p>The wire format is Anthropic's Messages API. {@code --api-url} may point at
 * any endpoint that speaks that same format (e.g. an Anthropic-compatible local
 * gateway/proxy); a raw OpenAI/Ollama endpoint is a different wire format and is
 * not directly supported.
 *
 * <p>Auth: {@code x-api-key} from {@code ANTHROPIC_API_KEY}. Profile users can
 * export a bearer token instead — see {@link #fromEnv(String, String)}.
 */
public final class AnthropicClient implements Judge {

  private static final String ANTHROPIC_VERSION = "2023-06-01";
  private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
  private static final int MAX_ATTEMPTS = 4;

  private final HttpClient http;
  private final String endpoint;
  private final String model;
  private final String apiKey;
  private final String bearerToken;
  private final int maxTokensPerName;

  private AnthropicClient(String baseUrl, String model, String apiKey, String bearerToken,
                          int maxTokensPerName) {
    this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
    this.endpoint = trimTrailingSlash(baseUrl) + "/v1/messages";
    this.model = model;
    this.apiKey = apiKey;
    this.bearerToken = bearerToken;
    this.maxTokensPerName = maxTokensPerName;
  }

  /**
   * Build a client from the environment. Prefers {@code ANTHROPIC_API_KEY}; if that
   * is unset, falls back to {@code ANTHROPIC_AUTH_TOKEN} (a bearer token, e.g. from
   * {@code ant auth print-credentials --access-token}). {@code baseUrl} may be
   * {@code null} to use {@code ANTHROPIC_BASE_URL} or the public default.
   *
   * @throws IllegalStateException if no credential is available.
   */
  public static AnthropicClient fromEnv(String baseUrl, String model) {
    String url = baseUrl != null ? baseUrl
        : envOr("ANTHROPIC_BASE_URL", "https://api.anthropic.com");
    String key = System.getenv("ANTHROPIC_API_KEY");
    String token = System.getenv("ANTHROPIC_AUTH_TOKEN");
    if ((key == null || key.isBlank()) && (token == null || token.isBlank())) {
      throw new IllegalStateException(
          "No Anthropic credential found. Set ANTHROPIC_API_KEY, or export a bearer token:\n"
          + "  export ANTHROPIC_AUTH_TOKEN=$(ant auth print-credentials --access-token)\n"
          + "Alternatively run with --dry-run to build batches without calling the API.");
    }
    return new AnthropicClient(url, model, blankToNull(key), blankToNull(token), 400);
  }

  /**
   * Judge a single batch. {@code userMessage} is the payload from
   * {@link ValidationPrompt#userMessage}; {@code batchSize} sizes {@code max_tokens}.
   * Returns the verdicts the model produced (indexed within the batch).
   */
  @Override
  public List<Verdict> judge(String userMessage, int batchSize) throws IOException, InterruptedException {
    String body = GSON.toJson(requestBody(userMessage, batchSize));
    HttpRequest.Builder req = HttpRequest.newBuilder()
        .uri(URI.create(endpoint))
        .timeout(Duration.ofMinutes(5))
        .header("content-type", "application/json")
        .header("anthropic-version", ANTHROPIC_VERSION)
        .POST(HttpRequest.BodyPublishers.ofString(body));
    if (apiKey != null) {
      req.header("x-api-key", apiKey);
    } else {
      req.header("authorization", "Bearer " + bearerToken)
         .header("anthropic-beta", "oauth-2025-04-20");
    }
    HttpRequest request = req.build();

    IOException last = null;
    for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
      HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
      int sc = resp.statusCode();
      if (sc == 200) {
        return parseVerdicts(resp.body());
      }
      if (sc == 429 || sc >= 500) {
        long waitMs = retryAfterMillis(resp, attempt);
        last = new IOException("Anthropic API " + sc + " (attempt " + attempt + "): " + brief(resp.body()));
        if (attempt < MAX_ATTEMPTS) {
          Thread.sleep(waitMs);
          continue;
        }
      }
      throw new IOException("Anthropic API error " + sc + ": " + brief(resp.body()));
    }
    throw last;
  }

  // -------------------- request / response --------------------

  private JsonObject requestBody(String userMessage, int batchSize) {
    JsonObject root = new JsonObject();
    root.addProperty("model", model);
    // room for one verdict per name plus adaptive thinking headroom
    root.addProperty("max_tokens", Math.min(32000, 2000 + batchSize * maxTokensPerName));

    JsonObject thinking = new JsonObject();
    thinking.addProperty("type", "adaptive");
    root.add("thinking", thinking);

    root.addProperty("system", ValidationPrompt.SYSTEM);

    JsonArray messages = new JsonArray();
    JsonObject userMsg = new JsonObject();
    userMsg.addProperty("role", "user");
    userMsg.addProperty("content", userMessage);
    messages.add(userMsg);
    root.add("messages", messages);

    JsonObject format = new JsonObject();
    format.addProperty("type", "json_schema");
    format.add("schema", verdictSchema());
    JsonObject outputConfig = new JsonObject();
    outputConfig.add("format", format);
    root.add("output_config", outputConfig);
    return root;
  }

  /** JSON schema constraining the reply to {@code {"verdicts":[Verdict, ...]}}. */
  static JsonObject verdictSchema() {
    JsonObject issue = objectSchema(
        prop("name", "string"),
        prop("parsed", "string"),
        prop("expected", "string"),
        prop("reason", "string"));
    requireAll(issue, "name", "parsed", "expected", "reason");

    JsonObject issues = new JsonObject();
    issues.addProperty("type", "array");
    issues.add("items", issue);

    JsonObject verdict = new JsonObject();
    verdict.addProperty("type", "object");
    JsonObject props = new JsonObject();
    props.add("index", typed("integer"));
    props.add("verdict", enumOf("ok", "suspect", "wrong"));
    props.add("confidence", enumOf("low", "med", "high"));
    props.add("fields", issues);
    props.add("note", typed("string"));
    verdict.add("properties", props);
    verdict.add("required", jsonArray("index", "verdict", "confidence", "fields", "note"));
    verdict.addProperty("additionalProperties", false);

    JsonObject verdicts = new JsonObject();
    verdicts.addProperty("type", "array");
    verdicts.add("items", verdict);

    JsonObject root = new JsonObject();
    root.addProperty("type", "object");
    JsonObject rootProps = new JsonObject();
    rootProps.add("verdicts", verdicts);
    root.add("properties", rootProps);
    root.add("required", jsonArray("verdicts"));
    root.addProperty("additionalProperties", false);
    return root;
  }

  /** Extract and parse the structured-output JSON from a Messages response. */
  static List<Verdict> parseVerdicts(String responseBody) {
    JsonObject resp = JsonParser.parseString(responseBody).getAsJsonObject();
    StringBuilder text = new StringBuilder();
    for (var block : resp.getAsJsonArray("content")) {
      JsonObject b = block.getAsJsonObject();
      if (b.has("type") && "text".equals(b.get("type").getAsString()) && b.has("text")) {
        text.append(b.get("text").getAsString());
      }
    }
    if (text.length() == 0) {
      throw new IllegalStateException("No text block in Anthropic response: " + brief(responseBody));
    }
    return Verdicts.parse(text.toString());
  }

  // -------------------- schema helpers --------------------

  private static JsonObject typed(String type) {
    JsonObject o = new JsonObject();
    o.addProperty("type", type);
    return o;
  }

  private static JsonObject enumOf(String... values) {
    JsonObject o = typed("string");
    o.add("enum", jsonArray(values));
    return o;
  }

  private static JsonObject prop(String name, String type) {
    JsonObject o = typed(type);
    o.addProperty("__name", name); // marker consumed by objectSchema
    return o;
  }

  private static JsonObject objectSchema(JsonObject... props) {
    JsonObject o = new JsonObject();
    o.addProperty("type", "object");
    JsonObject p = new JsonObject();
    for (JsonObject prop : props) {
      String name = prop.get("__name").getAsString();
      prop.remove("__name");
      p.add(name, prop);
    }
    o.add("properties", p);
    o.addProperty("additionalProperties", false);
    return o;
  }

  private static void requireAll(JsonObject objectSchema, String... names) {
    objectSchema.add("required", jsonArray(names));
  }

  private static JsonArray jsonArray(String... values) {
    JsonArray a = new JsonArray();
    for (String v : values) a.add(v);
    return a;
  }

  // -------------------- misc --------------------

  private static long retryAfterMillis(HttpResponse<String> resp, int attempt) {
    return resp.headers().firstValue("retry-after")
        .map(s -> {
          try {
            return Long.parseLong(s.trim()) * 1000L;
          } catch (NumberFormatException e) {
            return null;
          }
        })
        .filter(ms -> ms != null && ms > 0)
        .orElse((long) (Math.pow(2, attempt) * 500L));
  }

  private static String brief(String body) {
    if (body == null) return "";
    String s = body.strip();
    return s.length() > 500 ? s.substring(0, 500) + "…" : s;
  }

  private static String trimTrailingSlash(String s) {
    return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
  }

  private static String envOr(String key, String fallback) {
    String v = System.getenv(key);
    return v == null || v.isBlank() ? fallback : v;
  }

  private static String blankToNull(String s) {
    return s == null || s.isBlank() ? null : s;
  }
}
