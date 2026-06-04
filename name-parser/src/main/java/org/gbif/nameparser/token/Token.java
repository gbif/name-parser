package org.gbif.nameparser.token;

public final class Token {
  public final TokenKind kind;
  public final String text;
  public final int start;
  public final int end;

  public Token(TokenKind kind, String text, int start, int end) {
    this.kind = kind;
    this.text = text;
    this.start = start;
    this.end = end;
  }

  public boolean isWord() {
    return kind == TokenKind.WORD;
  }

  public boolean startsUpper() {
    return !text.isEmpty() && Character.isUpperCase(text.codePointAt(0));
  }

  public boolean startsLower() {
    return !text.isEmpty() && Character.isLowerCase(text.codePointAt(0));
  }

  @Override
  public String toString() {
    return kind + "(" + text + ")";
  }
}
