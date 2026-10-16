package org.gbif.nameparser.util;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Utilities dealing with unicode strings
 */
public class UnicodeUtils {
  private static final boolean DEBUG = false;
  private static Pattern MARKER = Pattern.compile("\\p{M}");
  private static final Pattern OCT = Pattern.compile("^[0-7]+$");
  private static final Pattern HEX = Pattern.compile("^[0-9abcdefABCDEF]+$");
  // Codepoint lookups are hot paths, so all tables are primitive: a BitSet indexed by codepoint
  // for membership, guarded by a lowest/highest codepoint range check that rejects plain ASCII early.
  private static final BitSet DIACRITICS;
  private static final int DIACRITICS_LOWEST_CP;
  private static final int DIACRITICS_HIGHEST_CP;
  static {
    BitSet diacritics = new BitSet();
    "´˝` ̏ˆˇ˘ ̑¸¨· ̡ ̢ ̉ ̛ˉ˛ ˚˳῾᾿".codePoints()
                                  .filter(cp -> cp != 32) // ignore whitespace - this is hard to remove from the input
                                  .forEach(cp -> {
      if (DEBUG) {
        System.out.print(Character.toChars(cp));
        System.out.print(" ");
        System.out.print(cp);
        System.out.println("  " + Character.getName(cp));
      }
      diacritics.set(cp);
    });
    DIACRITICS = diacritics;
    DIACRITICS_LOWEST_CP = diacritics.nextSetBit(0);
    DIACRITICS_HIGHEST_CP = diacritics.length() - 1;
  }

  // loads homoglyphs from resources taken from https://raw.githubusercontent.com/codebox/homoglyph/master/raw_data/chars.txt
  // membership is a BitSet, the canonical char is looked up by binary search in the sorted codepoint array on a hit only
  private static final BitSet HOMOGLYHPS;
  private static final int[] HOMOGLYHPS_CPS; // sorted unicode codepoints to avoid dealing with chars & surrogate pairs
  private static final char[] HOMOGLYHPS_CANONICALS; // canonical char for the codepoint at the same index
  private static final int HOMOGLYHPS_LOWEST_CP;
  private static final int HOMOGLYHPS_HIGHEST_CP;
  private static int toCodePoint(char c) {
    return String.valueOf(c).codePoints().findFirst().getAsInt();
  }
  static {
    // canonicals to be ignored from the homoglyph list
    final String ignoredCanonicals = " '-﹘";
    // ignore some frequently found quotation marks
    // https://www.cl.cam.ac.uk/~mgk25/ucs/quotes.html
    final String ignoredQuotes = "‘’“”";
    try (LineReader lr = new LineReader(UnicodeUtils.class.getResourceAsStream("/unicode/homoglyphs.txt"))) {
      // we manage some codepoints differently from the official homoglyph list
      Map<Integer, Character> homoglyphs = new TreeMap<>();
      homoglyphs.put(toCodePoint('ſ'), 's');

      StringBuilder canonicals = new StringBuilder();
      for (String line : lr) {
        // the canonical is never a surrogate pair
        char canonical = line.charAt(0);
        // ignore all whitespace codepoints
        if (ignoredCanonicals.indexOf(canonical) >= 0) {
          continue;
        }
        if (DEBUG) {
          System.out.print(canonical + " ");
          System.out.println((int)canonical);
        }
        canonicals.append(canonical);

        // ignore all ASCII chars from homoglyphs
        final AtomicInteger counter = new AtomicInteger();
        line.substring(1).codePoints()
            // remove hybrid marker which we use often and treat separately
            .filter(cp -> cp > 128
                          && cp != NameFormatter.HYBRID_MARKER
                          && !DIACRITICS.get(cp)
                          && !homoglyphs.containsKey(cp)
                          && ignoredQuotes.indexOf(cp) < 0
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
                counter.incrementAndGet();
              }
            );
        canonicals.append("[" + counter + "] ");
        if (lr.getRow() > 175 || 'ɸ' == canonical) {
          // skip all rare chars
          break;
        }
      }
      HOMOGLYHPS = new BitSet();
      HOMOGLYHPS_CPS = new int[homoglyphs.size()];
      HOMOGLYHPS_CANONICALS = new char[homoglyphs.size()];
      int idx = 0;
      for (Map.Entry<Integer, Character> e : homoglyphs.entrySet()) {
        HOMOGLYHPS.set(e.getKey());
        HOMOGLYHPS_CPS[idx] = e.getKey();
        HOMOGLYHPS_CANONICALS[idx++] = e.getValue();
      }
      HOMOGLYHPS_LOWEST_CP = HOMOGLYHPS_CPS[0];
      HOMOGLYHPS_HIGHEST_CP = HOMOGLYHPS_CPS[HOMOGLYHPS_CPS.length - 1];
      if (DEBUG) {
        System.out.println("Loaded known homoglyphs for: " + canonicals);
        System.out.println("Min/max homoglyph codepoint: " + HOMOGLYHPS_LOWEST_CP + " / " + HOMOGLYHPS_HIGHEST_CP);
      }
    }
  }
  // unicode codepoints considered ASCII hyphens
  private static final int HYPHEN_HOMOGLYHPS_LOWEST_CP;
  private static final int HYPHEN_HOMOGLYHPS_HIGHEST_CP;
  private static final BitSet HYPHEN_HOMOGLYHPS;
  static {
    BitSet hyp = new BitSet();
    "-˗۔‐‑‒–⁃−➖Ⲻ﹘‑".codePoints().forEach(hyp::set);
    hyp.clear(45); // ignore canonical ascii hyphen - this yields a higher lower bound and better performance as we skip ascii char checks
    if (DEBUG) {
      StringBuilder sb = new StringBuilder();
      hyp.stream().forEach(cp -> {
        sb.append(cp);
        sb.append(" -> ");
        sb.appendCodePoint(cp);
        sb.append("\n");
      });
      System.out.println(sb);
    }

    HYPHEN_HOMOGLYHPS = hyp;
    HYPHEN_HOMOGLYHPS_LOWEST_CP = hyp.nextSetBit(0);
    HYPHEN_HOMOGLYHPS_HIGHEST_CP = hyp.length() - 1;
    if (DEBUG) {
      System.out.println("Loaded " + hyp.cardinality() + " known hyphen homoglyphs");
      System.out.println("Min/max hyphen homoglyph codepoint: " + HYPHEN_HOMOGLYHPS_LOWEST_CP + " / " + HYPHEN_HOMOGLYHPS_HIGHEST_CP);
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
    StringBuilder sb = null;
    for (int i = 0, n = x.length(); i < n; i++) {
      char c = x.charAt(i);
      String repl = switch (c) {
        case 'æ' -> "ae";
        case 'Æ' -> "Ae";
        case 'œ' -> "oe";
        case 'Œ' -> "Oe";
        case 'Ĳ' -> "Ij";
        case 'ĳ' -> "ij";
        case 'ǈ' -> "Lj";
        case 'ǉ' -> "lj";
        case 'ȸ' -> "db";
        case 'ȹ' -> "qp";
        case 'ß' -> "ss";
        case 'ﬆ' -> "st";
        case 'ﬅ' -> "ft";
        case 'ﬀ' -> "ff";
        case 'ﬁ' -> "fi";
        case 'ﬂ' -> "fl";
        case 'ﬃ' -> "ffi";
        case 'ﬄ' -> "ffl";
        default -> null;
      };
      if (repl != null) {
        if (sb == null) sb = new StringBuilder(n + 8).append(x, 0, i);
        sb.append(repl);
      } else if (sb != null) {
        sb.append(c);
      }
    }
    return sb == null ? x : sb.toString();
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
    for (int i = 0, n = cs.length(); i < n; ) {
      final int cp = Character.codePointAt(cs, i);
      if (isHomoglyph(cp)) {
        return cp;
      }
      i += Character.charCount(cp);
    }
    return -1;
  }

  private static boolean isHomoglyph(int cp) {
    return HOMOGLYHPS_LOWEST_CP <= cp && cp <= HOMOGLYHPS_HIGHEST_CP && HOMOGLYHPS.get(cp);
  }

  private static boolean isHyphenHomoglyph(int cp) {
    return HYPHEN_HOMOGLYHPS_LOWEST_CP <= cp && cp <= HYPHEN_HOMOGLYHPS_HIGHEST_CP && HYPHEN_HOMOGLYHPS.get(cp);
  }

  /**
   * Returns the unicode codepoint of the first character which is a known homoglyph of a latin character
   * or -1 if none could be found.
   */
  public static int findDiacritics(final CharSequence cs) {
    if (cs == null) {
      return -1;
    }
    for (int i = 0, n = cs.length(); i < n; ) {
      final int cp = Character.codePointAt(cs, i);
      if (DIACRITICS_LOWEST_CP <= cp && cp <= DIACRITICS_HIGHEST_CP && DIACRITICS.get(cp)) {
        return cp;
      }
      i += Character.charCount(cp);
    }
    return -1;
  }

  /**
   * Replaces all known homoglyphs with their canonical character.
   * @param inclHyphens if true homoglyphs for hyphens (which are sometimes wanted) will be replaced
   */
  public static String replaceHomoglyphs(final CharSequence cs, boolean inclHyphens) {
    return replaceHomoglyphs(cs, inclHyphens, null);
  }

  /**
   * Replaces all known homoglyphs with their canonical character.
   * @param inclHyphens if true homoglyphs for hyphens (which are sometimes wanted) will be replaced
   * @param keep optional list of unicode characters/codepoints to not replace even though they are considered homoglyphs
   */
  public static String replaceHomoglyphs(final CharSequence cs, boolean inclHyphens, @Nullable String keep) {
    if (cs == null) {
      return null;
    }
    final int n = cs.length();
    StringBuilder sb = null; // only allocated once the first replacement is needed
    for (int i = 0; i < n; ) {
      final int cp = Character.codePointAt(cs, i);
      final int len = Character.charCount(cp);
      char repl = 0;
      if (keep == null || keep.indexOf(cp) < 0) {
        if (inclHyphens && isHyphenHomoglyph(cp)) {
          repl = '-';
        } else if (isHomoglyph(cp)) {
          repl = HOMOGLYHPS_CANONICALS[Arrays.binarySearch(HOMOGLYHPS_CPS, cp)];
        }
      }
      if (repl != 0) {
        if (sb == null) sb = new StringBuilder(n).append(cs, 0, i);
        sb.append(repl);
      } else if (sb != null) {
        sb.append(cs, i, i + len);
      }
      i += len;
    }
    return sb == null ? cs.toString() : sb.toString();
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

  // Unicode apostrophe / single-quote variants that normalise to the ASCII apostrophe '
  private static final String SINGLE_QUOTES =
      "\u0060\u00B4\u02B9\u02BB\u02BC\u02BD\u02CA\u02CB\u0091\u0092"
          + "\u2018\u2019\u201A\u201B\u2032\u2035\u275B\u275C\uFF07";
  // Unicode double-quote / double-prime variants that normalise to the ASCII double quote "
  private static final String DOUBLE_QUOTES =
      "\u0093\u0094\u201C\u201D\u201E\u201F\u2033\u2036\u275D\u275E\u301D\u301E\u301F\uFF02";

  /**
   * Normalises the many unicode apostrophe / single-quote variants to the ASCII apostrophe
   * (') and the unicode double-quote variants to the ASCII double quote ("). Author names and
   * quoted/provisional names routinely arrive with curly, prime, modifier-letter, low-9,
   * fullwidth or Windows-1252 quotes; collapsing them to ASCII keeps tokenisation and the
   * parsed output consistent regardless of the input's quote style.
   */
  public static String normalizeQuotes(String x) {
    if (x == null) return null;
    StringBuilder sb = null;
    for (int i = 0, n = x.length(); i < n; i++) {
      char c = x.charAt(i);
      char repl = c;
      if (SINGLE_QUOTES.indexOf(c) >= 0) {
        repl = '\'';
      } else if (DOUBLE_QUOTES.indexOf(c) >= 0) {
        repl = '"';
      }
      if (repl != c) {
        if (sb == null) sb = new StringBuilder(x.length()).append(x, 0, i);
        sb.append(repl);
      } else if (sb != null) {
        sb.append(c);
      }
    }
    return sb == null ? x : sb.toString();
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
      byte[] bytes = text.getBytes(StandardCharsets.ISO_8859_1);
      final CharsetDecoder utf8Decoder = StandardCharsets.UTF_8.newDecoder();
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
