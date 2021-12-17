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
package org.gbif.nameparser;

/**
 * {@link CharSequence} that notices {@link Thread} interrupts
 * Enables parsing timeout.
 *
 * @author gojomo - http://stackoverflow.com/questions/910740/cancelling-a-long-running-regex-match
 */
public class InterruptibleCharSequence implements CharSequence {
  final CharSequence inner;

  public InterruptibleCharSequence(CharSequence inner) {
    super();
    this.inner = inner;
  }

  @Override
  public char charAt(int index) {
    if (Thread.currentThread().isInterrupted()) {
      throw new RuntimeException("Interrupted!");
    }
    return inner.charAt(index);
  }

  @Override
  public int length() {
    if (Thread.currentThread().isInterrupted()) {
      throw new RuntimeException("Interrupted!");
    }
    return inner.length();
  }

  @Override
  public CharSequence subSequence(int start, int end) {
    return new InterruptibleCharSequence(inner.subSequence(start, end));
  }

  @Override
  public String toString() {
    return inner.toString();
  }
}
