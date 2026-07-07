package org.gbif.nameparser.cli.llm;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * On-disk cache of LLM verdicts, keyed by a hash of the exact thing that was
 * judged (prompt version + model + input + serialized parser output). Re-running
 * the validator over an unchanged corpus therefore costs nothing for names already
 * judged — only new or changed parses hit the API.
 *
 * <p>Backed by an append-only JSONL file: each line is
 * {@code {"key":"…","verdict":{…}}}. Loaded fully into memory on construction
 * (verdict records are tiny). Not thread-safe; guard {@link #put} externally when
 * judging concurrently.
 */
public final class VerdictCache {

  private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

  private final Map<String, Verdict> byKey = new HashMap<>();
  private final BufferedWriter appender;

  private VerdictCache(Map<String, Verdict> loaded, BufferedWriter appender) {
    this.byKey.putAll(loaded);
    this.appender = appender;
  }

  /** Open (creating if needed) the cache at {@code file}, loading any existing entries. */
  public static VerdictCache open(Path file) throws IOException {
    Map<String, Verdict> loaded = new HashMap<>();
    if (Files.exists(file)) {
      for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
        if (line.isBlank()) continue;
        JsonObject o = JsonParser.parseString(line).getAsJsonObject();
        loaded.put(o.get("key").getAsString(), GSON.fromJson(o.get("verdict"), Verdict.class));
      }
    } else if (file.getParent() != null) {
      Files.createDirectories(file.getParent());
    }
    BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    return new VerdictCache(loaded, w);
  }

  /** A no-op cache that never hits or persists — used when {@code --cache} is disabled. */
  public static VerdictCache disabled() {
    return new VerdictCache(Map.of(), null);
  }

  public int size() {
    return byKey.size();
  }

  public Verdict get(String key) {
    return byKey.get(key);
  }

  /** Record a verdict and (unless disabled) append it to the backing file. */
  public void put(String key, Verdict verdict) throws IOException {
    byKey.put(key, verdict);
    if (appender == null) return;
    JsonObject o = new JsonObject();
    o.addProperty("key", key);
    o.add("verdict", GSON.toJsonTree(verdict));
    appender.write(GSON.toJson(o));
    appender.newLine();
    appender.flush();
  }

  public void close() throws IOException {
    if (appender != null) appender.close();
  }

  /** SHA-256 hex of the joined parts — the cache key for one judged item. */
  public static String key(String... parts) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      for (String p : parts) {
        md.update((p == null ? "" : p).getBytes(StandardCharsets.UTF_8));
        md.update((byte) 0);
      }
      byte[] digest = md.digest();
      StringBuilder sb = new StringBuilder(digest.length * 2);
      for (byte b : digest) {
        sb.append(Character.forDigit((b >> 4) & 0xF, 16));
        sb.append(Character.forDigit(b & 0xF, 16));
      }
      return sb.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }
}
