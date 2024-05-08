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

import com.google.common.collect.Lists;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 *
 */
public class AuthorshipTest {
  
  @Test
  public void testAuthorship() throws Exception {
    ExAuthorship auth = new ExAuthorship();
    assertNull(auth.toString());
    
    auth.setAuthors(Lists.newArrayList("L."));
    assertEquals("L.", auth.toString());
    
    auth.getAuthors().add("Rohe");
    assertEquals("L. & Rohe", auth.toString());
    
    auth.getAuthors().clear();
    auth.setYear("1878");
    assertEquals("1878", auth.toString());
    
    auth.getAuthors().add("L.");
    auth.getAuthors().add("Rohe");
    assertEquals("L. & Rohe, 1878", auth.toString());
    
    auth.setExAuthors(Lists.newArrayList("Bassier"));
    assertEquals("Bassier ex L. & Rohe, 1878", auth.toString());
  }
}
