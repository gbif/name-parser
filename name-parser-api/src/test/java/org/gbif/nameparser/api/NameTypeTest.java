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

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NameTypeTest {
  
  @Test
  public void testIsParsable() throws Exception {
    assertTrue(NameType.SCIENTIFIC.isParsable());
    assertTrue(NameType.INFORMAL.isParsable());
    assertFalse(NameType.VIRUS.isParsable());
    assertFalse(NameType.NO_NAME.isParsable());
    assertFalse(NameType.HYBRID_FORMULA.isParsable());
    assertFalse(NameType.OTU.isParsable());
  }
  
}