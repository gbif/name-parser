package org.gbif.nameparser.cli.llm;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OpenAiClientTest {

  @Test
  public void extractsChatCompletionContent() {
    String resp = "{\"choices\":[{\"index\":0,\"message\":"
        + "{\"role\":\"assistant\",\"content\":\"the-content\"}}]}";
    assertEquals("the-content", OpenAiClient.extractContent(resp));
  }

  @Test
  public void toleratesFencesAndPreamble() {
    // Local models often wrap JSON in prose + markdown fences; Verdicts.parse copes.
    String content = "Sure, here are my verdicts:\n```json\n"
        + "{\"verdicts\":[{\"index\":0,\"verdict\":\"ok\",\"confidence\":\"high\","
        + "\"fields\":[],\"note\":\"\"},"
        + "{\"index\":1,\"verdict\":\"suspect\",\"confidence\":\"low\","
        + "\"fields\":[{\"name\":\"code\",\"parsed\":\"ZOOLOGICAL\",\"expected\":\"BOTANICAL\","
        + "\"reason\":\"sanctioning author\"}],\"note\":\"maybe\"}]}"
        + "\n```\n";
    List<Verdict> verdicts = Verdicts.parse(content);
    assertEquals(2, verdicts.size());
    assertTrue(verdicts.get(0).isOk());
    assertEquals("suspect", verdicts.get(1).verdict);
    assertEquals("code", verdicts.get(1).fields.get(0).name);
  }

  @Test
  public void stripsReasoningTraceWithBraces() {
    // A Qwen3-style reasoning model: a <think> trace that itself contains braces,
    // then the real answer. The naive first-{/last-} span would break here.
    String content = "<think>Let me check item 0: rank looks like {SUBSPECIES}? "
        + "Actually the parsed {genus} is fine.</think>\n"
        + "{\"verdicts\":[{\"index\":0,\"verdict\":\"wrong\",\"confidence\":\"high\","
        + "\"fields\":[{\"name\":\"rank\",\"parsed\":\"INFRASPECIFIC_NAME\","
        + "\"expected\":\"SUBSPECIES\",\"reason\":\"zoological trinomial\"}],\"note\":\"\"}]}";
    List<Verdict> verdicts = Verdicts.parse(content);
    assertEquals(1, verdicts.size());
    assertEquals("wrong", verdicts.get(0).verdict);
    assertEquals("SUBSPECIES", verdicts.get(0).fields.get(0).expected);
  }

  @Test
  public void ignoresBracesInsideStringValues() {
    String content = "{\"verdicts\":[{\"index\":0,\"verdict\":\"suspect\",\"confidence\":\"low\","
        + "\"fields\":[],\"note\":\"odd char } in the name\"}]}";
    List<Verdict> verdicts = Verdicts.parse(content);
    assertEquals(1, verdicts.size());
    assertEquals("odd char } in the name", verdicts.get(0).note);
  }

  @Test(expected = IllegalStateException.class)
  public void rejectsNonJson() {
    Verdicts.parse("I could not produce JSON, sorry.");
  }

  @Test
  public void salvagesCompleteVerdictsFromTruncatedOutput() {
    // gemma ran into max_tokens mid-array: two complete verdicts, then a third cut
    // off inside a string. Salvage the complete ones instead of losing the batch.
    String content = "{\"verdicts\":[{\"index\":0,\"verdict\":\"ok\",\"confidence\":\"high\",\"fields\":[]},"
        + "{\"index\":1,\"verdict\":\"wrong\",\"confidence\":\"high\","
        + "\"fields\":[{\"name\":\"rank\",\"parsed\":\"SPECIES\",\"expected\":\"DIVISION\","
        + "\"reason\":\"div. is a rank indicator\"}]},"
        + "{\"index\":2,\"verdict\":\"wrong\",\"confidence\":\"high\",\"fields\":[{\"name\":"
        + "\"specificEpithet\",\"parsed\":\"div\",\"expected\":\"\",\"reason\":\"div. is a rank indicator, not a specific …";
    List<Verdict> verdicts = Verdicts.parse(content);
    assertEquals(2, verdicts.size());
    assertEquals(0, verdicts.get(0).index);
    assertEquals(1, verdicts.get(1).index);
    assertEquals("DIVISION", verdicts.get(1).fields.get(0).expected);
  }

  @Test
  public void parseReplySkipsUnusableBatchInsteadOfThrowing() {
    // A flaky local model must not abort the whole run: empty content, a reply with no
    // verdicts array, and outright garbage all degrade to an empty (unjudged) batch.
    OpenAiClient client = OpenAiClient.fromEnv("http://localhost:11434", "test-model");
    String empty = "{\"choices\":[{\"finish_reason\":\"length\",\"message\":"
        + "{\"role\":\"assistant\",\"content\":\"\"}}]}";
    assertTrue(client.parseReply(empty, 10).isEmpty());

    String noVerdicts = "{\"choices\":[{\"finish_reason\":\"stop\",\"message\":"
        + "{\"role\":\"assistant\",\"content\":\"I could not produce JSON, sorry.\"}}]}";
    assertTrue(client.parseReply(noVerdicts, 10).isEmpty());
  }

  @Test
  public void parseReplyReturnsVerdictsForGoodBatch() {
    OpenAiClient client = OpenAiClient.fromEnv("http://localhost:11434", "test-model");
    String good = "{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"role\":\"assistant\","
        + "\"content\":\"{\\\"verdicts\\\":[{\\\"index\\\":0,\\\"verdict\\\":\\\"ok\\\","
        + "\\\"confidence\\\":\\\"high\\\",\\\"fields\\\":[]}]}\"}}]}";
    List<Verdict> verdicts = client.parseReply(good, 10);
    assertEquals(1, verdicts.size());
    assertTrue(verdicts.get(0).isOk());
  }

  @Test
  public void detectsTruncatedFinishReason() {
    String resp = "{\"choices\":[{\"index\":0,\"finish_reason\":\"length\",\"message\":"
        + "{\"role\":\"assistant\",\"content\":\"...\"}}]}";
    assertEquals("length", OpenAiClient.finishReason(resp));
    String ok = "{\"choices\":[{\"index\":0,\"finish_reason\":\"stop\",\"message\":"
        + "{\"role\":\"assistant\",\"content\":\"...\"}}]}";
    assertEquals("stop", OpenAiClient.finishReason(ok));
  }

  @Test
  public void coercesObjectAndNonStringFieldValues() {
    // Some local models (e.g. gemma) echo the whole parsed ParsedName object as the
    // 'parsed' value instead of a flat string, and emit numbers/booleans elsewhere.
    // Verdicts.parse must coerce these to strings rather than blow up the whole run.
    String content = "{\"verdicts\":[{\"index\":0,\"verdict\":\"wrong\",\"confidence\":\"high\","
        + "\"fields\":[{\"name\":\"code\",\"parsed\":\"ZOOLOGICAL\",\"expected\":\"BOTANICAL\","
        + "\"reason\":\"ok\"},"
        + "{\"name\":\"combinationAuthorship\",\"parsed\":{\"authors\":[\"Miller\"],\"year\":1907},"
        + "\"expected\":\"Miller, 1907\",\"reason\":\"nested\"},"
        + "{\"name\":\"rank\",\"parsed\":true,\"expected\":42,\"reason\":\"scalar\"}],"
        + "\"note\":\"\"}]}";
    List<Verdict> verdicts = Verdicts.parse(content);
    assertEquals(1, verdicts.size());
    Verdict v = verdicts.get(0);
    assertEquals("ZOOLOGICAL", v.fields.get(0).parsed);
    assertEquals("{\"authors\":[\"Miller\"],\"year\":1907}", v.fields.get(1).parsed);
    assertEquals("true", v.fields.get(2).parsed);
    assertEquals("42", v.fields.get(2).expected);
  }
}
