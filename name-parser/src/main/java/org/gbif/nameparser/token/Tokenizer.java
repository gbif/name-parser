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
          if (Character.isLetter(c)) {
            i += cl;
            continue;
          }
          // allow internal hyphen or apostrophe if next char is a letter
          if ((c == '-' || c == '\'' || c == '’' || c == '_'
              || c == '‐' || c == '‑' || c == '‒' || c == '–' || c == '—')
              && i + cl < n) {
            int next = input.codePointAt(i + cl);
            if (Character.isLetter(next)) {
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
