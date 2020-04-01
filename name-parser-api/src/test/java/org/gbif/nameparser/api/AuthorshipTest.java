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
    Authorship auth = new Authorship();
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
