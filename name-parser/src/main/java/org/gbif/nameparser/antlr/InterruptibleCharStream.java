/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.nameparser.antlr;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.misc.Interval;

/**
 * ANTLR {@link CharStream} that notices {@link Thread} interrupts on every read so a parse
 * stuck inside the lexer or parser can be cancelled via {@link java.util.concurrent.Future#cancel(boolean)}.
 * Direct counterpart of {@link org.gbif.nameparser.InterruptibleCharSequence} which guards
 * the regex pipeline.
 */
public class InterruptibleCharStream implements CharStream {
  private final CharStream inner;

  public InterruptibleCharStream(String text) {
    this.inner = CharStreams.fromString(text);
  }

  private static void checkInterrupt() {
    if (Thread.currentThread().isInterrupted()) {
      throw new RuntimeException("Interrupted!");
    }
  }

  @Override
  public String getText(Interval interval) {
    checkInterrupt();
    return inner.getText(interval);
  }

  @Override
  public void consume() {
    checkInterrupt();
    inner.consume();
  }

  @Override
  public int LA(int i) {
    checkInterrupt();
    return inner.LA(i);
  }

  @Override
  public int mark() {
    return inner.mark();
  }

  @Override
  public void release(int marker) {
    inner.release(marker);
  }

  @Override
  public int index() {
    return inner.index();
  }

  @Override
  public void seek(int index) {
    inner.seek(index);
  }

  @Override
  public int size() {
    return inner.size();
  }

  @Override
  public String getSourceName() {
    return inner.getSourceName();
  }
}
