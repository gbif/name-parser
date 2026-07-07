package org.gbif.nameparser.cli.llm;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Parses the {@code {"verdicts":[...]}} object a judging model returns. Tolerant of
 * the wrapping local models add — Claude's structured output is already clean, so
 * this is a no-op there and a safety net for local backends:
 * <ul>
 *   <li>reasoning models emit {@code <think>…</think>} traces (which may themselves
 *       contain braces) before the answer — stripped first;</li>
 *   <li>markdown {@code ```json} fences and prose preamble — ignored;</li>
 *   <li>the verdicts array is located by the {@code "verdicts"} key and walked
 *       element by element with brace matching, not a naive first-brace/last-brace span;</li>
 *   <li>a reply truncated at {@code max_tokens} (verbose local models routinely overrun)
 *       leaves a trailing object unbalanced — the complete verdicts before it are
 *       salvaged rather than losing the whole batch to an "unbalanced JSON" error.</li>
 * </ul>
 */
final class Verdicts {

  private static final Gson GSON = new GsonBuilder()
      .disableHtmlEscaping()
      // Local models (e.g. gemma) sometimes echo the whole parsed ParsedName object,
      // or a number/boolean, as a field's parsed/expected value instead of a flat
      // string. Coerce any JSON shape to a display string so one loose field value
      // doesn't abort the whole judging run.
      .registerTypeAdapter(Verdict.FieldIssue.class, fieldIssueDeserializer())
      .create();
  private static final Pattern THINK =
      Pattern.compile("(?is)<think(?:ing)?>.*?</think(?:ing)?>");

  private static JsonDeserializer<Verdict.FieldIssue> fieldIssueDeserializer() {
    return (json, type, ctx) -> {
      Verdict.FieldIssue fi = new Verdict.FieldIssue();
      if (json != null && json.isJsonObject()) {
        JsonObject o = json.getAsJsonObject();
        fi.name = asString(o.get("name"));
        fi.parsed = asString(o.get("parsed"));
        fi.expected = asString(o.get("expected"));
        fi.reason = asString(o.get("reason"));
      }
      return fi;
    };
  }

  /** Coerce any JSON value to a string: primitives verbatim, objects/arrays as compact JSON. */
  private static String asString(JsonElement el) {
    if (el == null || el.isJsonNull()) {
      return null;
    }
    return el.isJsonPrimitive() ? el.getAsString() : el.toString();
  }

  private Verdicts() {}

  static List<Verdict> parse(String modelText) {
    if (modelText == null || modelText.isBlank()) {
      throw new IllegalStateException("Empty model output");
    }
    String cleaned = THINK.matcher(modelText).replaceAll(" ");
    List<Verdict> out = new ArrayList<>();
    for (String obj : extractVerdictObjects(cleaned)) {
      out.add(GSON.fromJson(obj, Verdict.class));
    }
    return out;
  }

  /**
   * Walk the {@code "verdicts"} array and return each element's raw JSON text, one per
   * complete top-level {@code &#123;…&#125;} object. Anchors on the {@code "verdicts"}
   * key, finds the opening {@code [}, then brace-matches each object in turn. A trailing
   * object left unbalanced — the model hit {@code max_tokens} mid-reply — is dropped so
   * the complete verdicts already emitted are salvaged instead of losing the whole batch.
   */
  private static List<String> extractVerdictObjects(String text) {
    int key = text.indexOf("\"verdicts\"");
    int arr = key >= 0 ? text.indexOf('[', key) : -1;
    if (arr < 0) {
      throw new IllegalStateException("Model output has no 'verdicts' array: " + brief(text));
    }
    List<String> objects = new ArrayList<>();
    int i = arr + 1;
    int n = text.length();
    while (i < n) {
      char c = text.charAt(i);
      if (c == ']') {
        break; // array closed cleanly
      }
      if (c != '{') {
        i++; // whitespace, commas, stray characters between elements
        continue;
      }
      int end = matchObject(text, i);
      if (end < 0) {
        break; // trailing object truncated at max_tokens — salvage what came before
      }
      objects.add(text.substring(i, end + 1));
      i = end + 1;
    }
    return objects;
  }

  /**
   * Index of the {@code &#125;} closing the object that opens at {@code open}, honouring
   * string literals and escapes; {@code -1} if the object never closes (truncated input).
   */
  private static int matchObject(String text, int open) {
    int depth = 0;
    boolean inString = false;
    boolean escaped = false;
    for (int i = open; i < text.length(); i++) {
      char c = text.charAt(i);
      if (inString) {
        if (escaped) {
          escaped = false;
        } else if (c == '\\') {
          escaped = true;
        } else if (c == '"') {
          inString = false;
        }
        continue;
      }
      if (c == '"') {
        inString = true;
      } else if (c == '{') {
        depth++;
      } else if (c == '}') {
        depth--;
        if (depth == 0) {
          return i;
        }
      }
    }
    return -1;
  }

  static String brief(String s) {
    if (s == null) return "";
    String t = s.strip();
    return t.length() > 500 ? t.substring(0, 500) + "…" : t;
  }
}
