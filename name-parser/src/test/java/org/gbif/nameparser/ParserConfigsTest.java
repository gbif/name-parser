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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ParserConfigsTest {

  @Test
  public void norm() throws InterruptedException {
    assertNorm("zur strassen 1973", "zur Strassen,  1973");
    assertNorm("zur strassen 1973", "zur Strassen , 1973");
    assertNorm("zur strassen 1973", " zur Strassen, 1973");
    assertNorm("zur strassen 1973", "zur Strassen,1973");
    assertNorm("zur strassen 1973", "zur  Strassen 1973 ");
    assertNorm(" ( zur strassen 1973 ) ", "(zur Strassen, 1973)");
    assertNorm(" ( zur ) strassen 1973", "(zur) Strassen 1973");
    assertNorm(" ( zur strassen 1973 ) markus", "(zur Strassen, 1973) Markus");
    assertNorm(" ( zur strassen 1973 ) markus", "( zur Strassen 1973 )  Markus  ");
    assertNorm(" ( zur strassen 1973 ) markus", " ( zur Strassen, 1973 )Markus");
    assertNorm(" ( zur strassen 1973 ) markus", "( zur Strassen, 1973 )Markus");
    assertNorm(" ( zúr strassen 1973 ) märkus", "( zúr Straßen, 1973 )Märkus");
    assertNorm(" ( zur strassen 1973 ) markus robert", "( zur Strassen, 1973 ) Markus & RObert");
    assertNorm("coccinella 2 pustulata linnaeus 1758", "Coccinella 2-puſtulata Linnæus, 1758");
  }

  private void assertNorm(String expect, String x) throws InterruptedException {
    //System.out.println(String.format("%50s -> %s", x, ParserConfigs.norm(x)));
    assertEquals(expect, ParserConfigs.norm(x));
  }
}