package org.gbif.nameparser.cli.llm;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.gbif.nameparser.api.ParsedName;
import org.gbif.nameparser.cli.ParseResult;

import java.util.List;

/**
 * Builds the request payload for an LLM judging pass. The model is shown the raw
 * input name together with the parser's structured {@link ParsedName} output and
 * asked whether the parse is correct — this is <b>verification</b>, not parsing,
 * which is far more reliable for an LLM and produces fewer false disagreements
 * than an independent re-parse-and-diff.
 *
 * <p>The system prompt encodes the parser's own documented conventions (see the
 * repository {@code CLAUDE.md} "Authorship conventions") so the model holds the
 * parser to <i>its own contract</i> rather than to the model's guesswork about how
 * names "should" be structured. Change {@link #VERSION} whenever the prompt or the
 * payload shape changes so cached verdicts from an older prompt are not reused.
 */
public final class ValidationPrompt {

  /** Bumped on any change to the system prompt or payload shape; part of the cache key. */
  public static final String VERSION = "v1";

  /** Gson that omits null fields, giving a compact ParsedName rendering. */
  private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

  public static final String SYSTEM = String.join("\n",
      "You are a meticulous reviewer of scientific-name parsing results.",
      "",
      "The GBIF name parser is a deterministic, rule-based parser. It takes a raw",
      "scientific name string and produces a structured ParsedName. Your job is to",
      "judge whether each ParsedName faithfully represents the raw input, according to",
      "the parser's own documented conventions below. You are NOT re-parsing from",
      "scratch and you are NOT imposing your own preferences — you are checking the",
      "parser against its contract.",
      "",
      "Be conservative. Only flag a result as 'suspect' or 'wrong' when you can point",
      "to a concrete field and say what it should be and why. When in doubt, answer",
      "'ok'. Formatting/whitespace differences and equally-valid alternatives are NOT",
      "errors. Prefer high precision over high recall — a human reviews every non-ok",
      "verdict, so false alarms waste their time.",
      "",
      "Parser conventions you must respect:",
      "- Zoological trinomials default to SUBSPECIES: ICZN uses no rank marker, so",
      "  'Vulpes vulpes silaceus Miller, 1907' is rank SUBSPECIES, not a generic",
      "  INFRASPECIFIC_NAME. Botanical infraspecific names DO require an explicit",
      "  subsp./var./f. marker, so absent a marker they stay INFRASPECIFIC_NAME.",
      "- Code inference signals (priority order): a sanctioning author (e.g. ': Fr.')",
      "  => BOTANICAL; '(BasAuthor) RecombAuthor, year' with an explicit infraspecific",
      "  marker => BOTANICAL (the year is the publication year); any other year on the",
      "  author span => ZOOLOGICAL; a filius (f./fil.) suffix on a non-ex author with",
      "  NO year => BOTANICAL; basionym + combination authors without years => BOTANICAL;",
      "  basionym-only without years => ZOOLOGICAL.",
      "- A year extracted from a stripped 'published in' reference is the publication",
      "  year of the work, is code-NEUTRAL, and must NOT by itself imply ZOOLOGICAL.",
      "- Abbreviation of authors or journals is only a weak hint, never a code signal.",
      "- Taxonomic-concept references (sensu, sec., auct., non/nec, emend., fide, ...)",
      "  belong in taxonomicNote, not in the name.",
      "- Viruses, hybrid formulas, OTU/specimen codes, and placeholders are legitimately",
      "  UNPARSABLE — for an unparsable input, judge whether the reported NameType and",
      "  the fact that it was rejected are appropriate, not that it failed to parse.",
      "",
      "For every item you are given, return exactly one verdict object. Echo the item's",
      "'index'. Use verdict 'ok' | 'suspect' | 'wrong' and confidence 'low' | 'med' |",
      "'high'. List only the fields you believe are wrong.");

  /**
   * Spells out the exact reply shape as JSON. Cloud backends constrain this with a
   * structured-output schema, but local models get no such schema, so this instruction
   * is appended to their prompt.
   */
  public static final String OUTPUT_INSTRUCTION = String.join("\n",
      "Respond with ONLY a JSON object, no prose and no markdown fences, of the form:",
      "{\"verdicts\":[{\"index\":0,\"verdict\":\"ok|suspect|wrong\",",
      "\"confidence\":\"low|med|high\",\"fields\":[{\"name\":\"...\",\"parsed\":\"...\",",
      "\"expected\":\"...\",\"reason\":\"...\"}],\"note\":\"...\"}]}",
      "Return exactly one verdict per input item and echo its 'index'. Use an empty",
      "'fields' array when the verdict is 'ok'.");

  private ValidationPrompt() {}

  /**
   * Build the user-message text for a batch: a JSON array of items, each carrying
   * the batch index, the raw input, the parser's structured output (or the
   * unparsable error), and a human-readable rendering of the parse.
   */
  public static String userMessage(List<ParseResult> batch) {
    JsonArray items = new JsonArray();
    for (int i = 0; i < batch.size(); i++) {
      items.add(item(i, batch.get(i)));
    }
    return "Judge each of the following " + batch.size() + " parser results.\n"
        + GSON.toJson(items);
  }

  private static JsonObject item(int index, ParseResult r) {
    JsonObject o = new JsonObject();
    o.addProperty("index", index);
    o.addProperty("input", r.input);
    if (r.parsed != null) {
      o.add("parsed", GSON.toJsonTree(r.parsed));
      String canonical = safeCanonical(r.parsed);
      if (canonical != null) {
        o.addProperty("canonical", canonical);
      }
    }
    if (r.error != null) {
      JsonObject err = new JsonObject();
      if (r.error.type != null) err.addProperty("type", r.error.type.name());
      err.addProperty("message", r.error.message);
      o.add("unparsable", err);
    }
    return o;
  }

  private static String safeCanonical(ParsedName pn) {
    try {
      String c = pn.canonicalNameComplete();
      return c == null || c.isBlank() ? null : c;
    } catch (RuntimeException e) {
      return null;
    }
  }
}
