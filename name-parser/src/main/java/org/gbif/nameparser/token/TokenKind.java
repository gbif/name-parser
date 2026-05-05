package org.gbif.nameparser.token;

public enum TokenKind {
  /** A run of letters (incl. letter-with-mark, hyphens internal to a word, internal apostrophes). */
  WORD,
  /** A run of decimal digits. */
  NUMBER,
  /** A literal `×` (Unicode hybrid marker) or ASCII `x` standing alone between names. */
  HYBRID_MARK,
  OPEN_PAREN,
  CLOSE_PAREN,
  OPEN_BRACKET,
  CLOSE_BRACKET,
  OPEN_BRACE,
  CLOSE_BRACE,
  COMMA,
  SEMICOLON,
  COLON,
  DOT,
  AMPERSAND,
  DAGGER,
  /** Anything we did not classify; passed through verbatim. */
  OTHER
}
