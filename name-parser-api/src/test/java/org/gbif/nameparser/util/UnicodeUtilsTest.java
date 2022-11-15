package org.gbif.nameparser.util;

import org.apache.commons.lang3.time.StopWatch;
import org.junit.Test;

import java.util.List;

import static org.gbif.nameparser.util.UnicodeUtils.foldToAscii;
import static org.junit.Assert.*;


public class UnicodeUtilsTest {
  final String input1 = "rdtrfvgb3weñ54drtfvgxá+ä+.p, …-!§%&\"´`'ꓢᏞᎪ";
  final String input2 = "rfvgb3çw\uD835\uDEC3\uD835\uDEFD54d\uD835\uDE08tfvgx+ä+.p, …-!§%&\"´`'";
  final int iterations = 1000000;

  @Test
  public void containsDiacritics() {
    noDiacrits("Héllö m| frieñd");
    noDiacrits("coup d'arrêt");
    noDiacrits("l’arrêt est un coup « lourd »");
    noDiacrits("In French the grave accent on the letters a and u has no effect on pronunciation and just distinguishes homonyms otherwise spelled the same, for example the preposition à (\"to/belonging to/towards\") from the verb a (\"[he/she/it] has\") as well as the adverb là (\"there\") and the feminine definite article la; it is also used in the words déjà (\"already\"), deçà (preceded by en or au, and meaning \"closer than\" or \"inferior to (a given value)\"), the phrase çà et là (\"hither and thither\"; without the accents, it would literally mean \"it and the\") and its functional synonym deçà, delà. It is used on the letter u only to distinguish où (\"where\") and ou (\"or\"). È is rarely used to distinguish homonyms except in dès/des (\"since/some\"), ès/es (\"in/(thou) art\"), and lès/les (\"near/the\").");
    noDiacrits("gìa lètg \"marriage\"");
    noDiacrits("jàkkàr (\"fish-hook\"), Yoruba àgbọ̀n (\"chin\"), Hausa màcè ");
    noDiacrits("vis-à-vis, pièce de résistance or crème brûlée.");
    noDiacrits("instead of Brian's Theater");
    noDiacrits("Latin: À àẦ ầĀ̀ ā̀Ằ ằÆ̀ æ̀È èỀ ềḔ ḕÈ̩ è̩ə̀ ɚ̀H̀ h̀Ì ìĪ̀ ī̀ i̇̀K̀ k̀M̀ m̀Ǹ ǹÒ òỜ ờỒ ồṐ ṑÒ̩ ò̩ ɔ̀R̀ r̀S̀ s̀T̀ t̀Ù ùŪ̀ ū̀Ǜ ǜỪ ừV̀ v̀ ʌ̀Ẁ ẁX̀ x̀Ỳ ỳȲ̀ ȳ̀Z̀ z̀    Greek: Ὰ ὰῈ ὲῊ ὴῚ ὶ ῒῸ ὸῪ ὺ ῢῺ ὼ    Cyrillic: Ѐ ѐЍ ѝ");

    assertTrue(UnicodeUtils.containsDiacritics("Brian`s Theater"));
    assertTrue(UnicodeUtils.containsDiacritics("Brian´s Theater"));
    assertTrue(UnicodeUtils.containsDiacritics("Hello my´friend"));
    assertTrue(UnicodeUtils.containsDiacritics("Helloˆfriend"));
    assertTrue(UnicodeUtils.containsDiacritics("x´"));
    assertTrue(UnicodeUtils.containsDiacritics("x˝"));
    assertTrue(UnicodeUtils.containsDiacritics("x`"));
    assertTrue(UnicodeUtils.containsDiacritics("x̏ "));
    assertTrue(UnicodeUtils.containsDiacritics("xˇ"));
    assertTrue(UnicodeUtils.containsDiacritics("x˘"));
    assertTrue(UnicodeUtils.containsDiacritics("x ̑"));
    assertTrue(UnicodeUtils.containsDiacritics("x¸"));
    assertTrue(UnicodeUtils.containsDiacritics("x¨"));
    assertTrue(UnicodeUtils.containsDiacritics("x ̡"));
    assertTrue(UnicodeUtils.containsDiacritics("x ̢"));
    assertTrue(UnicodeUtils.containsDiacritics("x ̉"));
    assertTrue(UnicodeUtils.containsDiacritics("x ̛"));
    assertTrue(UnicodeUtils.containsDiacritics("xˉ"));
    assertTrue(UnicodeUtils.containsDiacritics("x˛"));
    assertTrue(UnicodeUtils.containsDiacritics("x˚"));
    assertTrue(UnicodeUtils.containsDiacritics("x˳"));
    assertTrue(UnicodeUtils.containsDiacritics("x῾"));
    assertTrue(UnicodeUtils.containsDiacritics("x᾿"));
  }

  void noDiacrits(String x) {
    boolean found = UnicodeUtils.containsDiacritics(x);
    if (found) {
      System.out.println(x);
      int cp = UnicodeUtils.findDiacritics(x);
      System.out.print(cp);
      System.out.println(" " + Character.getName(cp));
      System.out.println(new StringBuilder().appendCodePoint(cp));
      fail("diacritic wrongly detected");
    }
  }

  @Test
  public void containsHomoglyphs() {
    assertTrue(UnicodeUtils.containsHomoglyphs(input1));
    assertEquals(42210, UnicodeUtils.findHomoglyph(input1));
    System.out.print(Character.toChars(42210));
    assertTrue(UnicodeUtils.containsHomoglyphs(input2));
    assertEquals(120515, UnicodeUtils.findHomoglyph(input2));

    // no dashes
    assertFalse(UnicodeUtils.containsHomoglyphs("A word like e-mail uses a hyphen. Dashes denote a sub-clause or aside – like this – where they are more significant than commas but less than brackets. British usage prefers an en-dash, but Americans typically use an em-dash without spaces—like this."));

    // hybrid marker is fine in out domain!
    assertFalse(UnicodeUtils.containsHomoglyphs("Abies × Picea"));

    for (String x : new String[]{
        "coup d'arrêt",
        "l’arrêt est un coup « lourd »"
    }) {
      if (UnicodeUtils.containsHomoglyphs(x)) {
        System.out.println(x);
        System.out.println(UnicodeUtils.replaceHomoglyphs(x, false));
        int cp = UnicodeUtils.findHomoglyph(x);
        System.out.print(cp);
        System.out.println(" " + Character.getName(cp));
        System.out.println(new StringBuilder().appendCodePoint(cp));
        fail("homoglyph wrongly detected");
      }
    }
  }

  @Test
  public void containsHomoglyphsPerformance() {
    StopWatch watch = StopWatch.createStarted();
    for (int x=0; x<iterations; x++) {
      UnicodeUtils.containsHomoglyphs(input2 + x);
    }
    watch.stop();
    System.out.println(watch);
  }

  @Test
  public void replaceHomoglyphs() {
    assertEquals("rdtrfvgb3weñ54drtfvgxá+ä+.p, …-!§%&\"´`'SLA", UnicodeUtils.replaceHomoglyphs(input1, false));
    assertEquals("rfvgb3çwßß54dAtfvgx+ä+.p, …-!§%&\"´`'", UnicodeUtils.replaceHomoglyphs(input2, false));
    assertEquals("Abies × Picea", UnicodeUtils.replaceHomoglyphs("Abies × Picea", true));
    assertEquals("Jiménez-Ferbans", UnicodeUtils.replaceHomoglyphs("Jiménez‑Ferbans", true));
    assertEquals("Jiménez-Ferbans", UnicodeUtils.replaceHomoglyphs("Jiménez‒Ferbans", true));
    assertEquals("Jiménez-Ferbans", UnicodeUtils.replaceHomoglyphs("Jiménez﹘Ferbans", true));
    assertEquals("Jiménez-Ferbans", UnicodeUtils.replaceHomoglyphs("Jiménez-Ferbans", true));
    // should this be a homoglyph?
    assertEquals("¡i", UnicodeUtils.replaceHomoglyphs("¡i", true));
    // we keep this as s, not f
    assertEquals("Coccinella 2-pustulata Linnæus, 1758", UnicodeUtils.replaceHomoglyphs("Coccinella 2-puſtulata Linnæus, 1758", true));
  }

  @Test
  public void testFoldToAscii() throws Exception {
    assertEquals("Navas, 1929", foldToAscii("Navás, 1929"));
    assertEquals(null, foldToAscii(null));
    assertEquals("", foldToAscii(""));
    assertEquals("Schulhof, Gymnasium Hurth", foldToAscii("Schulhof, Gymnasium Hürth"));
    assertEquals("Doring", foldToAscii("Döring"));
    assertEquals("Desireno", foldToAscii("Désírèñø"));
    assertEquals("Debreczy & I. Racz", foldToAscii("Debreçzÿ & Ï. Rácz"));
    assertEquals("Donatia novae-zelandiae", foldToAscii("Donatia novae-zelandiæ"));
    assertEquals("Carex ×cayouettei", foldToAscii("Carex ×cayouettei"));
    assertEquals("Carex comosa × Carex lupulina", foldToAscii("Carex comosa × Carex lupulina"));
    assertEquals("Aeropyrum coil-shaped virus", foldToAscii("Aeropyrum coil-shaped virus"));
    assertEquals("†Lachnus bonneti", foldToAscii("†Lachnus bonneti"));

    assertEquals("lachs", foldToAscii("łachs"));
    assertEquals("Coccinella 2-pustulata", foldToAscii("Coccinella 2-puſtulata"));

    String test = "ŠŒŽšœžŸ¥µÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýÿ";
    assertEquals("SOEZsoezY¥µAAAAAAAECEEEEIIIIDNOOOOOOUUUUYssaaaaaaaeceeeeiiiidnoooooouuuuyy", foldToAscii(test));
  }


  @Test
  public void testDecodeUtf8Garbage() {
    assertUtf8(null, null);
    assertUtf8("", "");
    assertUtf8("a", "a");
    assertUtf8("ä-üOØ", "ä-üOØ");
    assertUtf8("(Günther, 1887)", "(GÃ¼nther, 1887)");
    assertUtf8("Böhlke, 1955", "BÃ¶hlke, 1955");
    assertUtf8("Nielsen & Quéro, 1991\n", "Nielsen & QuÃ©ro, 1991\n");
    assertUtf8("Rosinés", "RosinÃ©s");
    assertUtf8("S. Calderón & Standl.", "S. CalderÃ³n & Standl.");
    assertUtf8("Strömman, 1896", "StrÃ¶mman, 1896");
    assertUtf8("Sérus.", "SÃ©rus.");
    assertUtf8("Thér.", "ThÃ©r.");
    assertUtf8("Trécul", "TrÃ©cul");
    assertUtf8("Hale & López-Fig.\n", "Hale & LÃ³pez-Fig.\n");
  }

  private void assertUtf8(String expected, String src) {
    String decoded = UnicodeUtils.decodeUtf8Garbage(src);
    assertEquals(expected, decoded);
    // make sure if we had gotten the correct string it would not be modified
    assertEquals(expected, UnicodeUtils.decodeUtf8Garbage(decoded));
  }
}