package org.gbif.nameparser.token;

import java.util.ArrayList;
import java.util.List;

/**
 * Single-pass tokenizer. Produces a flat list of tokens preserving original offsets.
 * Whitespace is consumed and not emitted. Letter runs may include hyphens and apostrophes
 * that sit between two letters (e.g. "Hartmann-Schroder", "d'urvilleana").
 */
public final class Tokenizer {

  private Tokenizer() {}

  private static boolean isAllUpperCase(String s) {
    for (int i = 0; i < s.length(); ) {
      int cp = s.codePointAt(i);
      if (Character.isLetter(cp) && !Character.isUpperCase(cp)) return false;
      i += Character.charCount(cp);
    }
    return true;
  }

  public static List<Token> tokenize(String input) {
    final int n = input.length();
    List<Token> out = new ArrayList<>(Math.max(4, n / 4));
    int i = 0;
    while (i < n) {
      int cp = input.codePointAt(i);
      int charLen = Character.charCount(cp);

      if (Character.isWhitespace(cp)) {
        i += charLen;
        continue;
      }

      if (Character.isLetter(cp)) {
        int wordStart = i;
        i += charLen;
        while (i < n) {
          int c = input.codePointAt(i);
          int cl = Character.charCount(c);
          if (Character.isLetter(c) || Character.isDigit(c)) {
            i += cl;
            continue;
          }
          // allow internal hyphen, apostrophe or a stray "!" (OCR/typo artefact, e.g.
          // "pu!chra") if the next char is a letter or digit, so the word is kept intact
          if ((c == '-' || c == '\'' || c == '’' || c == '_' || c == '!'
              || c == '‐' || c == '‑' || c == '‒' || c == '–' || c == '—')
              && i + cl < n) {
            int next = input.codePointAt(i + cl);
            if (Character.isLetter(next) || Character.isDigit(next)) {
              i += cl;
              continue;
            }
          }
          break;
        }
        String word = input.substring(wordStart, i);
        TokenKind kind = TokenKind.WORD;
        if (word.length() == 1 && (word.charAt(0) == 'x' || word.charAt(0) == 'X')) {
          // Bare 'x' between two spaces (or at start/end) acts as a hybrid mark.
          boolean leftOk = wordStart == 0 || Character.isWhitespace(input.codePointAt(wordStart - 1));
          boolean rightOk = i == n || Character.isWhitespace(input.codePointAt(i));
          if (leftOk && rightOk) {
            kind = TokenKind.HYBRID_MARK;
          }
        } else if (word.length() >= 2
            && (word.charAt(0) == 'x' || word.charAt(0) == 'X')
            && Character.isUpperCase(word.codePointAt(1))
            && !isAllUpperCase(word)) {
          // xFoo / XFoo: leading x/X directly attached to a capitalised word — split
          // into a HYBRID_MARK token followed by the remaining word.
          out.add(new Token(TokenKind.HYBRID_MARK, word.substring(0, 1), wordStart, wordStart + 1));
          out.add(new Token(TokenKind.WORD, word.substring(1), wordStart + 1, i));
          continue;
        }
        out.add(new Token(kind, word, wordStart, i));
        continue;
      }

      if (Character.isDigit(cp)) {
        int numStart = i;
        i += charLen;
        while (i < n && Character.isDigit(input.codePointAt(i))) {
          i++;
        }
        // "11-punctata" / "2-pustulata": a number glued to a hyphen + letter is the
        // leading-numeral epithet form, not a bare number.
        if (i + 1 < n && input.charAt(i) == '-' && Character.isLetter(input.codePointAt(i + 1))) {
          i++; // consume hyphen
          while (i < n) {
            int c = input.codePointAt(i);
            int cl = Character.charCount(c);
            if (Character.isLetter(c) || Character.isDigit(c)) { i += cl; continue; }
            if (c == '-' && i + cl < n) {
              int next = input.codePointAt(i + cl);
              if (Character.isLetter(next) || Character.isDigit(next)) { i += cl; continue; }
            }
            break;
          }
          out.add(new Token(TokenKind.WORD, input.substring(numStart, i), numStart, i));
          continue;
        }
        out.add(new Token(TokenKind.NUMBER, input.substring(numStart, i), numStart, i));
        continue;
      }

      TokenKind kind;
      switch (cp) {
        case '(': kind = TokenKind.OPEN_PAREN; break;
        case ')': kind = TokenKind.CLOSE_PAREN; break;
        case '[': kind = TokenKind.OPEN_BRACKET; break;
        case ']': kind = TokenKind.CLOSE_BRACKET; break;
        case '{': kind = TokenKind.OPEN_BRACE; break;
        case '}': kind = TokenKind.CLOSE_BRACE; break;
        case ',': kind = TokenKind.COMMA; break;
        case ';': kind = TokenKind.SEMICOLON; break;
        case ':': kind = TokenKind.COLON; break;
        case '.': kind = TokenKind.DOT; break;
        case '&': kind = TokenKind.AMPERSAND; break;
        case '×': kind = TokenKind.HYBRID_MARK; break;
        case '+': kind = TokenKind.HYBRID_MARK; break;
        case '†': kind = TokenKind.DAGGER; break;
        default: kind = TokenKind.OTHER;
      }
      out.add(new Token(kind, input.substring(i, i + charLen), i, i + charLen));
      i += charLen;
    }
    return out;
  }
}
