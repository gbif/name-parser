package org.gbif.nameparser.cli.llm;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
 *   <li>the actual object is located by the {@code "verdicts"} key and extracted with
 *       brace matching, not a naive first-brace/last-brace span.</li>
 * </ul>
 */
final class Verdicts {

  private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
  private static final Pattern THINK =
      Pattern.compile("(?is)<think(?:ing)?>.*?</think(?:ing)?>");

  private Verdicts() {}

  static List<Verdict> parse(String modelText) {
    if (modelText == null || modelText.isBlank()) {
      throw new IllegalStateException("Empty model output");
    }
    String cleaned = THINK.matcher(modelText).replaceAll(" ");
    String json = extractVerdictsObject(cleaned);
    JsonObject root = JsonParser.parseString(json).getAsJsonObject();
    if (!root.has("verdicts")) {
      throw new IllegalStateException("Model output has no 'verdicts' array: " + brief(modelText));
    }
    List<Verdict> out = new ArrayList<>();
    for (var v : root.getAsJsonArray("verdicts")) {
      out.add(GSON.fromJson(v, Verdict.class));
    }
    return out;
  }

  /**
   * Extract the JSON object containing the {@code "verdicts"} key: anchor on the key,
   * back up to the enclosing {@code &#123;}, then brace-match forward to its close.
   * Falls back to the first balanced object when the key isn't found.
   */
  private static String extractVerdictsObject(String text) {
    int key = text.indexOf("\"verdicts\"");
    int open = key >= 0 ? text.lastIndexOf('{', key) : text.indexOf('{');
    if (open < 0) {
      throw new IllegalStateException("No JSON object in model output: " + brief(text));
    }
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
          return text.substring(open, i + 1);
        }
      }
    }
    throw new IllegalStateException("Unbalanced JSON object in model output: " + brief(text));
  }

  static String brief(String s) {
    if (s == null) return "";
    String t = s.strip();
    return t.length() > 500 ? t.substring(0, 500) + "…" : t;
  }
}
