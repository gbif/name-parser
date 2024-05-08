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
package org.gbif.nameparser.utils;

import org.junit.Test;

import java.util.regex.Pattern;

import static org.junit.Assert.*;


public class NormalizeUtilsTest {

  @Test
  public void testNormalizeCitation() {
    assertEquals("Hallo", NormalizeUtils.normalizeCitation("   Hallo "));
    assertEquals(
      "Benois, R. 1964. Contribution a la connaissance des Halictus malgaches (Hym. Apidae). Revue Française d'Entomologie 31: 45 60.",
      NormalizeUtils.normalizeCitation(
        "Benois, R. 1964. Contribution a la connaissance des Halictus malgaches (Hym. Apidae). Revue Française d'Entomologie 31: 45 60. "));

    Pattern PRO_PARTE_SPLITTER = Pattern.compile("\\|+");
    String[] ids = PRO_PARTE_SPLITTER.split("123|456|783942|1|");
    for (String id : ids) {
      System.out.println(id);
    }
  }

  @Test
  public void testTrimToNull() {
    assertEquals("aCcepTed", NormalizeUtils.trimToNull("aCcepTed "));
    assertEquals("nuller", NormalizeUtils.trimToNull("nuller "));
    assertNull(NormalizeUtils.trimToNull("NuLL "));
    assertNull(NormalizeUtils.trimToNull(" "));
    assertNull(NormalizeUtils.trimToNull(" NULL "));
    assertNull(NormalizeUtils.trimToNull("       "));
    assertNull(NormalizeUtils.trimToNull("\\N "));
  }

  @Test
  public void testNormalizeTerm() {
    assertEquals("accepted", NormalizeUtils.normalizeTerm("aCcepTed "));
    assertEquals("accepted", NormalizeUtils.normalizeTerm("accepted"));
  }

  @Test
  public void testReplaceUnicodeEntities() {
    assertEquals("Markus&Pia", NormalizeUtils.replaceUnicodeEntities("Markus&Pia"));
    assertEquals("Markus&Pia;", NormalizeUtils.replaceUnicodeEntities("Markus&Pia;"));
    assertEquals("Markus & Pia ; ", NormalizeUtils.replaceUnicodeEntities("Markus & Pia ; "));
    assertEquals("&#pia;", NormalizeUtils.replaceUnicodeEntities("&#pia;"));
    assertEquals("&#12pia;", NormalizeUtils.replaceUnicodeEntities("&#12pia;"));
    assertTrue(NormalizeUtils.replaceUnicodeEntities(null) == null);
    assertEquals("лобан", NormalizeUtils.replaceUnicodeEntities("&#1083;&#1086;&#1073;&#1072;&#1085;"));
    assertEquals("лобан", NormalizeUtils.replaceUnicodeEntities("&#x43b;&#x43e;&#x431;&#x430;&#x43d;"));
    assertEquals("лобан", NormalizeUtils.replaceUnicodeEntities("&#x43B;&#x043e;&#x0431;&#x430;&#x43D;"));
    //    assertEquals("лобан", NormalizeUtils.replaceUnicodeEntities("\\u043b\\u0u43e\\u0431\\u0430\\u043d"));
    //    assertEquals("лобан", NormalizeUtils.replaceUnicodeEntities("\\u043B\\u0u43E\\u0431\\u0430\\u043D"));
    //    assertEquals("\\u43Bобан", NormalizeUtils.replaceUnicodeEntities("\\u43B\\0u43E\\u0431\\u0430\\u043D"));
    assertEquals("\\u43b\\u43e\\u431\\u430\\u43d",
      NormalizeUtils.replaceUnicodeEntities("\\u43b\\u43e\\u431\\u430\\u43d"));
  }
}
