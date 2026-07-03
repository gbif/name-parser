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
}
