package org.gbif.nameparser.util;

import com.google.common.base.Charsets;
import it.unimi.dsi.fastutil.chars.CharSet;
import it.unimi.dsi.fastutil.ints.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.text.Normalizer;
import java.util.PrimitiveIterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Utilities dealing with unicode strings
 */
public class UnicodeUtils {
  private static final Logger LOG = LoggerFactory.getLogger(UnicodeUtils.class);
  private static final boolean DEBUG = false;
  private static Pattern MARKER = Pattern.compile("\\p{M}");
  private static final Pattern OCT = Pattern.compile("^[0-7]+$");
  private static final Pattern HEX = Pattern.compile("^[0-9abcdefABCDEF]+$");
  private static final IntSet DIACRITICS; // unicode codepoints as keys to avoid dealing with chars & surrogate pairs
  private static final int DIACRITICS_LOWEST_CP;
  private static final int DIACRITICS_HIGHEST_CP;
  static {
    IntSet diacritics = new IntOpenHashSet();
    final AtomicInteger minCP = new AtomicInteger(Integer.MAX_VALUE);
    final AtomicInteger maxCP = new AtomicInteger(Integer.MIN_VALUE);
    "´˝` ̏ˆˇ˘ ̑¸¨· ̡ ̢ ̉ ̛ˉ˛ ˚˳῾᾿".codePoints()
                                  .filter(cp -> cp != 32) // ignore whitespace - this is hard to remove from the input
                                  .forEach(cp -> {
      if (DEBUG) {
        System.out.print(Character.toChars(cp));
        System.out.print(" ");
        System.out.print(cp);
        System.out.println("  " + Character.getName(cp));
      }
      diacritics.add(cp);
      minCP.set( Math.min(minCP.get(), cp) );
      maxCP.set( Math.max(maxCP.get(), cp) );
    });
    DIACRITICS = IntSets.unmodifiable(diacritics);
    DIACRITICS_LOWEST_CP = minCP.get();
    DIACRITICS_HIGHEST_CP = maxCP.get();
  }

  // loads homoglyphs from resources taken from https://raw.githubusercontent.com/codebox/homoglyph/master/raw_data/chars.txt
  private static final Int2CharMap HOMOGLYHPS; // unicode codepoints as keys to avoid dealing with chars & surrogate pairs
  private static final int HOMOGLYHPS_LOWEST_CP;
  private static final int HOMOGLYHPS_HIGHEST_CP;
  static {
    // canonicals to be ignored from the homoglyph list
    final CharSet ignoredCanonicals = CharSet.of(' ', '\'', '-', '﹘');
    try (LineReader lr = new LineReader(ClassLoader.getSystemResourceAsStream("unicode/homoglyphs.txt"))) {
      Int2CharMap homoglyphs = new Int2CharOpenHashMap();
      final AtomicInteger minCP = new AtomicInteger(Integer.MAX_VALUE);
      final AtomicInteger maxCP = new AtomicInteger(Integer.MIN_VALUE);
      StringBuilder canonicals = new StringBuilder();
      for (String line : lr) {
        // the canonical is never a surrogate pair
        char canonical = line.charAt(0);
        // ignore all whitespace codepoints
        if (ignoredCanonicals.contains(canonical)) {
          continue;
        }
        if (DEBUG) {
          System.out.print(canonical + " ");
          System.out.println((int)canonical);
        }
        canonicals.append(canonical);

        // ignore all ASCII chars from homoglyphs
        final AtomicInteger counter = new AtomicInteger();
        // ignore some frequently found quotation marks
        // https://www.cl.cam.ac.uk/~mgk25/ucs/quotes.html
        final IntSet ignore = new IntOpenHashSet();
        "\u2018\u2019\u201C\u201D".codePoints().forEach(cp -> {
          if (DEBUG) {
            System.out.print("IGNORE ");
            System.out.print(Character.toChars(cp));
            System.out.print(" ");
            System.out.print(cp);
            System.out.println("  " + Character.getName(cp));
          }
          ignore.add(cp);
        });
        line.substring(1).codePoints()
            // remove hybrid marker which we use often
            .filter(cp -> cp > 128
                          && cp != NameFormatter.HYBRID_MARKER
                          && !DIACRITICS.contains(cp)
                          && !ignore.contains(cp)
            )
            .forEach(
              cp -> {
                if (DEBUG) {
                  System.out.print("  ");
                  System.out.print(Character.toChars(cp));
                  System.out.print(" ");
                  System.out.print(cp);
                  System.out.println("  " + Character.getName(cp));
                }
                homoglyphs.put(cp, canonical);
                minCP.set( Math.min(minCP.get(), cp) );
                maxCP.set( Math.max(maxCP.get(), cp) );
                counter.incrementAndGet();
              }
            );
        canonicals.append("[" + counter + "] ");
        if (lr.getRow() > 175 || 'ɸ' == canonical) {
          // skip all rare chars
          break;
        }
      }
      HOMOGLYHPS = Int2CharMaps.unmodifiable(homoglyphs);
      HOMOGLYHPS_LOWEST_CP = minCP.get();
      HOMOGLYHPS_HIGHEST_CP = maxCP.get();
      LOG.info("Loaded known homoglyphs for: {}", canonicals);
      LOG.debug("Min homoglyph codepoint: {}", minCP);
      LOG.debug("Max homoglyph codepoint: {}", maxCP);
    }
  }

  /**
   * Replaces all digraphs and ligatures with their underlying 2 latin letters.
   *
   * @param x the string to decompose
   */
  public static String decompose(String x) {
    if (x == null) {
      return null;
    }
    return x.replaceAll("æ", "ae")
        .replaceAll("Æ", "Ae")
        .replaceAll("œ", "oe")
        .replaceAll("Œ", "Oe")
        .replaceAll("Ĳ", "Ij")
        .replaceAll("ĳ", "ij")
        .replaceAll("ǈ", "Lj")
        .replaceAll("ǉ", "lj")
        .replaceAll("ȸ", "db")
        .replaceAll("ȹ", "qp")
        .replaceAll("ß", "ss")
        .replaceAll("ﬆ", "st")
        .replaceAll("ﬅ", "ft")
        .replaceAll("ﬀ", "ff")
        .replaceAll("ﬁ", "fi")
        .replaceAll("ﬂ", "fl")
        .replaceAll("ﬃ", "ffi")
        .replaceAll("ﬄ", "ffl");
  }

  /**
   * Returns true if there is at least one character which is a known standalone diacritic character.
   * Diacritics combined with a letter, e.g. ö, é or ñ are not flagged!
   */
  public static boolean containsDiacritics(final CharSequence cs) {
    return findDiacritics(cs) >= 0;
  }

  /**
   * Returns true if there is at least on character which is a known homoglyph of a latin character.
   */
  public static boolean containsHomoglyphs(final CharSequence cs) {
    return findHomoglyph(cs) >= 0;
  }


  /**
   * Returns the unicode codepoint of the first character which is a known homoglyph of a latin character
   * or -1 if none could be found.
   */
  public static int findHomoglyph(final CharSequence cs) {
    if (cs == null) {
      return -1;
    }
    PrimitiveIterator.OfInt iter = cs.codePoints().iterator();
    while(iter.hasNext()) {
      final int cp = iter.nextInt();
      if (HOMOGLYHPS_LOWEST_CP <= cp && cp <= HOMOGLYHPS_HIGHEST_CP && HOMOGLYHPS.containsKey(cp)) {
        return cp;
      }
    }
    return -1;
  }

  /**
   * Returns the unicode codepoint of the first character which is a known homoglyph of a latin character
   * or -1 if none could be found.
   */
  public static int findDiacritics(final CharSequence cs) {
    if (cs == null) {
      return -1;
    }
    PrimitiveIterator.OfInt iter = cs.codePoints().iterator();
    while(iter.hasNext()) {
      final int cp = iter.nextInt();
      if (DIACRITICS_LOWEST_CP <= cp && cp <= DIACRITICS_HIGHEST_CP && DIACRITICS.contains(cp)) {
        return cp;
      }
    }
    return -1;
  }

  /**
   * Replaces all known homoglyphs with their canonical character.
   */
  public static String replaceHomoglyphs(final CharSequence cs) {
    if (cs == null) {
      return null;
    }
    StringBuilder sb = new StringBuilder();
    PrimitiveIterator.OfInt iter = cs.codePoints().iterator();
    while(iter.hasNext()) {
      final int cp = iter.nextInt();
      if (HOMOGLYHPS_LOWEST_CP <= cp && cp <= HOMOGLYHPS_HIGHEST_CP && HOMOGLYHPS.containsKey(cp)) {
        sb.append(HOMOGLYHPS.get(cp));
      } else {
        sb.appendCodePoint(cp);
      }
    }
    return sb.toString();
  }


  /**
   * Removes accents & diacretics and converts ligatures into several chars
   *
   * There are still a few unicode characters which are not captured by the java Normalizer and this method,
   * so if you rely on true ASCII to be generated make sure to call the removeNonAscii(x) method afterwards!
   *
   * @param x string to fold into ASCII
   * @return string converted to ASCII equivalent, expanding common ligatures
   */
  public static String foldToAscii(String x) {
    if (x == null) {
      return null;
    }
    x = replaceSpecialCases(x);
    // use java unicode normalizer to remove accents
    x = Normalizer.normalize(x, Normalizer.Form.NFD);
    return MARKER.matcher(x).replaceAll("");
  }

  /**
   * Removes all characters that are not ASCII chars, i.e. above the first 7 bits
   */
  public static String removeNonAscii(String x) {
    if (x == null) return null;
    char[] out = new char[x.length()];
    int j = 0;
    for (int i = 0, n = x.length(); i < n; ++i) {
      char c = x.charAt(i);
      if (c <= '\u007F') out[j++] = c;
    }
    return new String(out);
  }

  /**
   * Replaces all characters that are not ASCII chars, i.e. above the first 7 bits, with the given replacement char
   */
  public static String replaceNonAscii(String x, char replacement) {
    if (x == null) return null;
    char[] out = new char[x.length()];
    int j = 0;
    for (int i = 0, n = x.length(); i < n; ++i) {
      char c = x.charAt(i);
      out[j++] = c <= '\u007F' ? c : replacement;
    }
    return new String(out);
  }

  /**
   * The java Normalizer misses a few cases and 2 char ligatures which we deal with here
   */
  private static String replaceSpecialCases(String x) {
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < x.length(); i++) {
      char c = x.charAt(i);
      switch (c) {
        case 'ß':
          sb.append("ss");
          break;
        case 'ſ':
          sb.append("s");
          break;
        case 'Æ':
          sb.append("AE");
          break;
        case 'æ':
          sb.append("ae");
          break;
        case 'Ð':
          sb.append("D");
          break;
        case 'đ':
          sb.append("d");
          break;
        case 'ð':
          sb.append("d");
          break;
        case 'Ø':
          sb.append("O");
          break;
        case 'ø':
          sb.append("o");
          break;
        case 'Œ':
          sb.append("OE");
          break;
        case 'œ':
          sb.append("oe");
          break;
        case 'Ŧ':
          sb.append("T");
          break;
        case 'ŧ':
          sb.append("t");
          break;
        case 'Ł':
          sb.append("L");
          break;
        case 'ł':
          sb.append("l");
          break;
        default:
          sb.append(c);
      }
    }
    return sb.toString();
  }

  /**
   * Unescapes various unicode escapes if existing:
   * <p>
   * java unicode escape, four hexadecimal digits \ uhhhh
   * <p>
   * octal escape \nnn The octal value nnn, where nnn stands for 1 to 3 digits between ‘0’ and ‘7’. For example, the code for the ASCII ESC
   * (escape) character is ‘\033’.
   * <p>
   * hexadecimal escape \xhh... The hexadecimal value hh, where hh stands for a sequence of hexadecimal digits (‘0’–‘9’, and either ‘A’–‘F’
   * or ‘a’–‘f’).Like the same construct in ISO C, the escape sequence continues until the first nonhexadecimal digit is seen. However,
   * using more than two hexadecimal digits produces undefined results. (The ‘\x’ escape sequence is not allowed in POSIX awk.)
   *
   * @param text string potentially containing unicode escape chars
   * @return the unescaped string
   */
  public static String unescapeUnicodeChars(String text) {
    if (text == null) {
      return null;
    }
    // replace unicode, hexadecimal or octal character encodings by iterating over the chars once
    //
    // java unicode escape, four hexadecimal digits
    // \ uhhhh
    //
    // octal escape
    // \nnn
    // The octal value nnn, where nnn stands for 1 to 3 digits between ‘0’ and ‘7’. For example, the
    // code for the ASCII
    // ESC (escape) character is ‘\033’.
    //
    // hexadecimal escape
    // \xhh...
    // The hexadecimal value hh, where hh stands for a sequence of hexadecimal digits (‘0’–‘9’, and
    // either ‘A’–‘F’ or
    // ‘a’–‘f’).
    // Like the same construct in ISO C, the escape sequence continues until the first
    // nonhexadecimal digit is seen.
    // However, using more than two hexadecimal digits produces undefined results. (The ‘\x’ escape
    // sequence is not allowed
    // in POSIX awk.)
    int i = 0, len = text.length();
    char c;
    StringBuffer sb = new StringBuffer(len);
    while (i < len) {
      c = text.charAt(i++);
      if (c == '\\') {
        if (i < len) {
          c = text.charAt(i++);
          try {
            if (c == 'u' && text.length() >= i + 4) {
              // make sure we have only hexadecimals
              String hex = text.substring(i, i + 4);
              if (HEX.matcher(hex).find()) {
                c = (char) Integer.parseInt(hex, 16);
                i += 4;
              } else {
                throw new NumberFormatException("No hex value: " + hex);
              }
            } else if (c == 'n' && text.length() >= i + 2) {
              // make sure we have only 0-7 digits
              String oct = text.substring(i, i + 2);
              if (OCT.matcher(oct).find()) {
                c = (char) Integer.parseInt(oct, 8);
                i += 2;
              } else {
                throw new NumberFormatException("No octal value: " + oct);
              }
            } else if (c == 'x' && text.length() >= i + 2) {
              // make sure we have only hexadecimals
              String hex = text.substring(i, i + 2);
              if (HEX.matcher(hex).find()) {
                c = (char) Integer.parseInt(hex, 16);
                i += 2;
              } else {
                throw new NumberFormatException("No hex value: " + hex);
              }
            } else if (c == 'r' || c == 'n' || c == 't') {
              // escaped newline or tab. Replace with simple space
              c = ' ';
            } else {
              throw new NumberFormatException("No char escape");
            }
          } catch (NumberFormatException e) {
            // keep original characters including \ if escape sequence was invalid
            // but replace \n with space instead
            if (c == 'n') {
              c = ' ';
            } else {
              c = '\\';
              i--;
            }
          }
        }
      } // fall through: \ escapes itself, quotes any character but u
      sb.append(c);
    }
    return sb.toString();
  }

  /**
   * Tries to decode a UTF8 string only if common UTF8 character combinations are found which are unlikely to be correctly encoded text.
   * E.g. Ã¼ is the German Umlaut ü and indicates we have encoded utf8 text still.
   */
  public static String decodeUtf8Garbage(String text) {
    Pattern UTF8_TEST = Pattern.compile("(Ã¤|Ã¼|Ã¶|Ã\u0084|Ã\u009C|Ã\u0096|" + // äüöÄÜÖ
                                        "Ã±|Ã¸|Ã§|Ã®|Ã´|Ã»|Ã\u0091|Ã\u0098|Ã\u0087|Ã\u008E|Ã\u0094|Ã\u009B" + // ñøçîôûÑØÇÎÔÛ
                                        "Ã¡|Ã©|Ã³|Ãº|Ã\u00AD|Ã\u0081|Ã\u0089|Ã\u0093|Ã\u009A|Ã\u008D)" // áéóúíÁÉÓÚÍ
        , Pattern.CASE_INSENSITIVE);
    if (text != null && UTF8_TEST.matcher(text).find()) {
      // typical utf8 combinations found. Try to decode from latin1 to utf8
      byte[] bytes = text.getBytes(Charsets.ISO_8859_1);
      final CharsetDecoder utf8Decoder = Charsets.UTF_8.newDecoder();
      ByteBuffer buffer = ByteBuffer.wrap(bytes);
      try {
        return utf8Decoder.decode(buffer).toString();
      } catch (CharacterCodingException e) {
        // maybe wasnt a good idea, return original
      }
    }
    return text;
  }

}
