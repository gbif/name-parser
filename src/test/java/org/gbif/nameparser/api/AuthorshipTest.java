package org.gbif.nameparser.api;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class AuthorshipTest {
  
  @Test
  public void testAuthorship() throws Exception {
    Authorship auth = new Authorship();
    assertNull(auth.toString());
    
    auth.setAuthors(new ArrayList<>(List.of("L."))); // mutable
    assertEquals("L.", auth.toString());
    
    auth.getAuthors().add("Rohe");
    assertEquals("L. & Rohe", auth.toString());
    
    auth.getAuthors().clear();
    auth.setYear("1878");
    assertEquals("1878", auth.toString());
    
    auth.getAuthors().add("L.");
    auth.getAuthors().add("Rohe");
    assertEquals("L. & Rohe, 1878", auth.toString());
    
    auth.setExAuthors(List.of("Bassier"));
    assertEquals("Bassier ex L. & Rohe, 1878", auth.toString());
  }

  @Test
  public void testAnonymous() {
    Authorship auth = new Authorship();
    auth.setAnonymous(true);
    // an anonymous authorship exists even without an author string
    assertEquals(false, auth.isEmpty());
    assertEquals("Anon.", auth.toString());
    auth.setYear("1830");
    assertEquals("Anon., 1830", auth.toString());
    // attributed from external evidence: square brackets (ICZN Recommendation 51D)
    auth.setAuthors(new ArrayList<>(List.of("Denis", "Schiffermüller")));
    assertEquals("[Denis & Schiffermüller], 1830", auth.toString());

    // the flag is part of equality
    Authorship plain = Authorship.yearAuthors("1830", "Denis", "Schiffermüller");
    assertNotEquals(plain, auth);
    plain.setAnonymous(true);
    assertEquals(plain, auth);
    assertEquals(plain.hashCode(), auth.hashCode());
  }

  @Test
  public void testSanctioningAuthor() {
    Authorship auth = Authorship.authors("Wulfen");
    assertFalse(auth.hasSanctioningAuthor());
    auth.setSanctioningAuthor("Fr.");
    assertTrue(auth.hasSanctioningAuthor());
    assertEquals("Wulfen : Fr.", auth.toString());

    // the sanctioning author is part of equality
    Authorship plain = Authorship.authors("Wulfen");
    assertNotEquals(plain, auth);
    plain.setSanctioningAuthor("Fr.");
    assertEquals(plain, auth);
    assertEquals(plain.hashCode(), auth.hashCode());
  }

  @Test
  public void testAddAuthor() {
    Authorship auth = new Authorship();

    // real authors are added, blank/null ones are ignored
    auth.addAuthor("L.");
    auth.addAuthor("Rohe");
    auth.addAuthor("  ");
    auth.addAuthor(null);
    assertEquals(List.of("L.", "Rohe"), auth.getAuthors());

    auth.addExAuthor("Bassier");
    auth.addExAuthor("");
    assertEquals(List.of("Bassier"), auth.getExAuthors());
  }

  @Test
  public void testEqualsIncludesAuthors() {
    Authorship a = Authorship.yearAuthors("1878", "L.");
    Authorship b = Authorship.yearAuthors("1878", "L.");
    Authorship c = Authorship.yearAuthors("1878", "Mill.");
    assertEquals(a, b);
    assertNotEquals(a, c);
  }
}
