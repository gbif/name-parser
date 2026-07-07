package org.gbif.nameparser.cli.llm;

import java.io.IOException;
import java.util.List;

/**
 * A backend that judges a batch of parser results. Implemented by
 * {@link AnthropicClient} (cloud) and {@link OpenAiClient} (local, OpenAI-compatible
 * servers such as Ollama, LM Studio, llama.cpp), so {@code ValidateCli} is agnostic
 * to where the model runs.
 */
public interface Judge {

  /**
   * Judge one batch. {@code userMessage} is the payload from
   * {@link ValidationPrompt#userMessage}; {@code batchSize} sizes the token budget.
   * Returns the verdicts the model produced, indexed within the batch.
   */
  List<Verdict> judge(String userMessage, int batchSize) throws IOException, InterruptedException;
}
