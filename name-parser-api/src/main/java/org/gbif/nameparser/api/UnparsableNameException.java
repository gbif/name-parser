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

/**
 * An exception thrown to indicate a string that cannot be parsed into a ParsedName in a meaningful way.
 * This explicitly includes virus names and hybrid formulas which have a structure
 * that cannot be accomodated by the ParsedName class.
 */
public class UnparsableNameException extends Exception {
  private final NameType type;
  private final String name;

  public UnparsableNameException(NameType type, String name, String message) {
    super(message);
    this.type = type;
    this.name = name;
  }

  public UnparsableNameException(NameType type, String name) {
    super("Unparsable " + type + " name: " + name);
    this.type = type;
    this.name = name;
  }
  
  public NameType getType() {
    return type;
  }
  
  public String getName() {
    return name;
  }

  public static class UnparsableAuthorshipException extends UnparsableNameException {

    public UnparsableAuthorshipException(String authorship) {
      super(NameType.NO_NAME, authorship, "Unparsable authorship: " + authorship);
    }
  }
}
