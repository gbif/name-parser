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
 * {@link Judge} backed by any OpenAI-compatible {@code /v1/chat/completions} endpoint
 * — chiefly local runtimes such as Ollama, LM Studio, and llama.cpp — so validation
 * can run for free on your own machine instead of the metered cloud API.
 *
 * <p>It requests {@code response_format: json_object} and repeats the exact reply
 * shape in the prompt ({@link ValidationPrompt#OUTPUT_INSTRUCTION}), then parses the
 * reply tolerantly ({@link Verdicts#parse}) since local models are looser than Claude
 * about wrapping their output. No API key is required for a typical local server.
 *
 * <p>Default endpoint is Ollama's ({@code http://localhost:11434}); override with
 * {@code --api-url} or {@code OPENAI_BASE_URL} for LM Studio ({@code :1234}),
 * llama.cpp ({@code :8080}), or a hosted OpenAI-compatible service.
 */
public final class OpenAiClient implements Judge {

  private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
  private static final int MAX_ATTEMPTS = 4;

  private final HttpClient http;
  private final String endpoint;
  private final String model;
  private final String apiKey;
  private final int maxTokensPerName;

  private OpenAiClient(String baseUrl, String model, String apiKey, int maxTokensPerName) {
    this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
    this.endpoint = trimTrailingSlash(baseUrl) + "/v1/chat/completions";
    this.model = model;
    this.apiKey = apiKey;
    this.maxTokensPerName = maxTokensPerName;
  }

  /**
   * Build from the environment. {@code baseUrl} may be {@code null} to use
   * {@code OPENAI_BASE_URL} or the Ollama default. {@code OPENAI_API_KEY} is optional
   * — sent only if present (local servers ignore it).
   */
  public static OpenAiClient fromEnv(String baseUrl, String model) {
    String url = baseUrl != null ? baseUrl : envOr("OPENAI_BASE_URL", "http://localhost:11434");
    return new OpenAiClient(url, model, blankToNull(System.getenv("OPENAI_API_KEY")), 400);
  }

  @Override
  public List<Verdict> judge(String userMessage, int batchSize) throws IOException, InterruptedException {
    String body = GSON.toJson(requestBody(userMessage, batchSize));
    HttpRequest.Builder req = HttpRequest.newBuilder()
        .uri(URI.create(endpoint))
        .timeout(Duration.ofMinutes(10)) // local generation can be slow
        .header("content-type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body));
    if (apiKey != null) {
      req.header("authorization", "Bearer " + apiKey);
    }
    HttpRequest request = req.build();

    IOException last = null;
    for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
      HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
      int sc = resp.statusCode();
      if (sc == 200) {
        return Verdicts.parse(extractContent(resp.body()));
      }
      if (sc == 429 || sc >= 500) {
        last = new IOException("OpenAI-compatible API " + sc + " (attempt " + attempt + "): "
            + Verdicts.brief(resp.body()));
        if (attempt < MAX_ATTEMPTS) {
          Thread.sleep((long) (Math.pow(2, attempt) * 500L));
          continue;
        }
      }
      throw new IOException("OpenAI-compatible API error " + sc + ": " + Verdicts.brief(resp.body())
          + "\nEndpoint: " + endpoint + " (is the local server running and the model pulled?)");
    }
    throw last;
  }

  private JsonObject requestBody(String userMessage, int batchSize) {
    JsonObject root = new JsonObject();
    root.addProperty("model", model);
    root.addProperty("temperature", 0);
    root.addProperty("max_tokens", Math.min(32000, 2000 + batchSize * maxTokensPerName));
    root.addProperty("stream", false);

    JsonObject format = new JsonObject();
    format.addProperty("type", "json_object");
    root.add("response_format", format);

    JsonArray messages = new JsonArray();
    messages.add(message("system", ValidationPrompt.SYSTEM + "\n\n" + ValidationPrompt.OUTPUT_INSTRUCTION));
    messages.add(message("user", userMessage));
    root.add("messages", messages);
    return root;
  }

  private static JsonObject message(String role, String content) {
    JsonObject m = new JsonObject();
    m.addProperty("role", role);
    m.addProperty("content", content);
    return m;
  }

  /** Pull {@code choices[0].message.content} out of an OpenAI chat-completions reply. */
  static String extractContent(String responseBody) {
    JsonObject resp = JsonParser.parseString(responseBody).getAsJsonObject();
    JsonArray choices = resp.getAsJsonArray("choices");
    if (choices == null || choices.isEmpty()) {
      throw new IllegalStateException("No choices in response: " + Verdicts.brief(responseBody));
    }
    JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
    if (message == null || !message.has("content")) {
      throw new IllegalStateException("No message content in response: " + Verdicts.brief(responseBody));
    }
    return message.get("content").getAsString();
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
