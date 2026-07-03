package org.gbif.nameparser.cli.llm;

import com.google.gson.JsonObject;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AnthropicClientTest {

  @Test
  public void parsesStructuredVerdicts() {
    // A representative Messages response: an (empty) thinking block, then a text
    // block whose content is the structured-output JSON.
    String innerJson = "{\"verdicts\":["
        + "{\"index\":0,\"verdict\":\"ok\",\"confidence\":\"high\",\"fields\":[],\"note\":\"\"},"
        + "{\"index\":1,\"verdict\":\"wrong\",\"confidence\":\"med\","
        + "\"fields\":[{\"name\":\"rank\",\"parsed\":\"INFRASPECIFIC_NAME\","
        + "\"expected\":\"SUBSPECIES\",\"reason\":\"zoological trinomial\"}],\"note\":\"x\"}"
        + "]}";
    String response = "{\"content\":["
        + "{\"type\":\"thinking\",\"thinking\":\"\"},"
        + "{\"type\":\"text\",\"text\":" + quote(innerJson) + "}"
        + "]}";

    List<Verdict> verdicts = AnthropicClient.parseVerdicts(response);
    assertEquals(2, verdicts.size());

    Verdict ok = verdicts.get(0);
    assertEquals(0, ok.index);
    assertTrue(ok.isOk());

    Verdict wrong = verdicts.get(1);
    assertEquals(1, wrong.index);
    assertEquals("wrong", wrong.verdict);
    assertEquals(1, wrong.fields.size());
    assertEquals("rank", wrong.fields.get(0).name);
    assertEquals("SUBSPECIES", wrong.fields.get(0).expected);
  }

  @Test
  public void schemaIsWellFormed() {
    JsonObject schema = AnthropicClient.verdictSchema();
    assertEquals("object", schema.get("type").getAsString());
    assertTrue(schema.getAsJsonObject("properties").has("verdicts"));
    assertTrue(schema.has("additionalProperties"));
    // required nested arrays are present
    assertTrue(schema.getAsJsonArray("required").toString().contains("verdicts"));
  }

  /** JSON-encode a string as a quoted literal (the model's text block content). */
  private static String quote(String s) {
    StringBuilder b = new StringBuilder("\"");
    for (char c : s.toCharArray()) {
      switch (c) {
        case '"': b.append("\\\""); break;
        case '\\': b.append("\\\\"); break;
        default: b.append(c);
      }
    }
    return b.append('"').toString();
  }
}
