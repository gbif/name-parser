package org.gbif.nameparser.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Kind of deprecated class, for scientific name normalising please.
 */
public class NormalizeUtils {

  private static final Pattern NORM_TERM = Pattern.compile("[-_\\.\\s]+");
  private static final Pattern NORM_CITATION_PUNCT = Pattern.compile("\\. *");
  private static final Pattern NORM_CITATION_WHITE = Pattern.compile("[_\\s]+");
  private static final Pattern XML_ENTITIES_DEC = Pattern.compile("&#([0-9]{2,});");
  private static final Pattern XML_ENTITIES_HEX = Pattern.compile("&#x([0-9abcdefABCDEF]{2,});");
  private static final Pattern UNICODE_HEX = Pattern.compile("\\\\u([0-9abcdefABCDEF]{4});");

  public static String normalizeCitation(String citation) {
    return trimToNull(
      NORM_CITATION_WHITE.matcher(NORM_CITATION_PUNCT.matcher(citation).replaceAll(". ")).replaceAll(" "));
  }

  public static String normalizeTerm(String term) {
    return trimToNull(NORM_TERM.matcher(term).replaceAll(" ").toLowerCase());
  }

  public static String replaceUnicodeEntities(String x) {
    if (x == null) {
      return x;
    }
    // decimal entities
    Matcher m = XML_ENTITIES_DEC.matcher(x);
    if (m.find()) {
      m.reset();
      StringBuffer sb = new StringBuffer();
      while (m.find()) {
        try {
          Character uc = (char) Integer.parseInt(m.group(1));
          m.appendReplacement(sb, uc.toString());
        } catch (NumberFormatException e) {
          // use original string
          m.appendReplacement(sb, m.group());
        }
      }
      m.appendTail(sb);
      x = sb.toString();
    }
    // hexadecimal entities
    m = XML_ENTITIES_HEX.matcher(x);
    if (m.find()) {
      m.reset();
      StringBuffer sb = new StringBuffer();
      while (m.find()) {
        try {
          Character uc = (char) Integer.parseInt(m.group(1), 16);
          m.appendReplacement(sb, uc.toString());
        } catch (NumberFormatException e) {
          // use original string
          m.appendReplacement(sb, m.group());
        }
      }
      m.appendTail(sb);
      x = sb.toString();
    }
    // java unicode
    m = UNICODE_HEX.matcher(x);
    if (m.find()) {
      m.reset();
      StringBuffer sb = new StringBuffer();
      while (m.find()) {
        try {
          Character uc = (char) Integer.parseInt(m.group(1), 16);
          m.appendReplacement(sb, uc.toString());
        } catch (NumberFormatException e) {
          // use original string
          m.appendReplacement(sb, m.group());
        }
      }
      m.appendTail(sb);
      x = sb.toString();
    }

    return x;
  }

  /**
   * trims to null but also takes the verbatim NULL or \N as a null value
   * which is often found in db dumps
   */
  public static String trimToNull(String x) {
    x = StringUtils.trimToNull(x);
    if (x != null && (x.equals("\\N") || x.equalsIgnoreCase("NULL"))) {
      x = null;
    }
    return x;
  }
}
