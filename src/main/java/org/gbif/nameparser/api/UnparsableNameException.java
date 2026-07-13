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
package org.gbif.nameparser.api;

import javax.annotation.Nullable;

/**
 * An <em>unchecked</em> exception indicating a string that cannot be parsed into a {@link ParsedName}
 * in a meaningful way. This explicitly includes hybrid formulas, and carries a {@link NameType} plus
 * an optional {@link NomCode} for code-known unparsables such as viruses.
 * <p>
 * Parsing itself no longer throws — {@link NameParser#parse} returns a {@link ParseResult}. This
 * exception is only raised by the opt-in {@link ParseResult#orElseThrow()} for callers that prefer a
 * fail-fast style, which is why it is unchecked (it composes in lambdas and streams).
 */
public class UnparsableNameException extends RuntimeException {
  private final NameType type;
  private final NomCode code;
  private final String name;

  public UnparsableNameException(ParseResult.Unparsable unparsable) {
    this(unparsable.type(), unparsable.code(), unparsable.name());
  }

  public UnparsableNameException(NameType type, String name, String message) {
    this(type, null, name, message);
  }

  public UnparsableNameException(NameType type, String name) {
    this(type, (NomCode) null, name);
  }

  public UnparsableNameException(NameType type, @Nullable NomCode code, String name) {
    super("Unparsable " + type + " name: " + name);
    this.type = type;
    this.code = code;
    this.name = name;
  }

  public UnparsableNameException(NameType type, @Nullable NomCode code, String name, String message) {
    super(message);
    this.type = type;
    this.code = code;
    this.name = name;
  }

  public NameType getType() {
    return type;
  }

  /** The nomenclatural code when known despite the name being unparsable (e.g. VIRUS), else null. */
  @Nullable
  public NomCode getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  /** This exception as a {@link ParseResult.Unparsable} value. */
  public ParseResult.Unparsable asResult() {
    return new ParseResult.Unparsable(type, code, name);
  }

  public static class UnparsableAuthorshipException extends UnparsableNameException {

    public UnparsableAuthorshipException(String authorship) {
      super(NameType.OTHER, authorship, "Unparsable authorship: " + authorship);
    }
  }
}
