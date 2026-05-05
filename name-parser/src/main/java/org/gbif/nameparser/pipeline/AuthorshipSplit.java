package org.gbif.nameparser.pipeline;

import org.gbif.nameparser.token.AuthorParticles;
import org.gbif.nameparser.token.Token;
import org.gbif.nameparser.token.TokenKind;

import java.util.List;

/**
 * Locates the boundary between the name section and the authorship section in a
 * tokenised input.
 */
public final class AuthorshipSplit {

  private AuthorshipSplit() {}

  public static int findBoundary(List<Token> tokens) {
    final int n = tokens.size();
    if (n == 0) return 0;

    int i = 0;
    int nameWords = 0;
    boolean afterGenus = false;
    boolean afterSubgenus = false;
    boolean haveEpithet = false;
    boolean genusAllCaps = false;

    while (i < n) {
      Token t = tokens.get(i);

      if (t.kind == TokenKind.HYBRID_MARK) {
        i++;
        continue;
      }

      if (t.kind == TokenKind.WORD) {
        if (nameWords == 0) {
          if (t.startsUpper()) {
            nameWords++;
            afterGenus = true;
            genusAllCaps = t.text.length() > 1 && isAllUpper(t.text);
            i++;
            // abbreviated genus: single capital letter + dot
            if (t.text.length() == 1 && i < n && tokens.get(i).kind == TokenKind.DOT) {
              i++;
            }
            continue;
          }
          // Lower-case first token — accept as a recovered genus and continue.
          if (t.text.length() >= 2) {
            nameWords++;
            afterGenus = true;
            i++;
            continue;
          }
          return i;
        }
        if (t.startsLower()) {
          String w = stripDot(t.text);
          // cf./aff. qualifiers and indet markers — keep walking
          if (w.equalsIgnoreCase("cf") || w.equalsIgnoreCase("aff")
              || w.equalsIgnoreCase("sp") || w.equalsIgnoreCase("spec")
              || w.equalsIgnoreCase("species") || w.equalsIgnoreCase("indet")) {
            i++;
            if (i < n && tokens.get(i).kind == TokenKind.DOT) i++;
            continue;
          }
          // Aggregate suffix words within the name section
          if (w.equalsIgnoreCase("agg") || w.equalsIgnoreCase("aggregate")
              || w.equalsIgnoreCase("group") || w.equalsIgnoreCase("complex")) {
            i++;
            if (i < n && tokens.get(i).kind == TokenKind.DOT) i++;
            continue;
          }
          // Infraspecific rank marker (incl. "notho" prefix variants)
          boolean[] notho = new boolean[1];
          if (RankMarkers.matchInfraspecificAllowNotho(w, notho) != null) {
            i++;
            if (i < n && tokens.get(i).kind == TokenKind.DOT) i++;
            // microbial f. sp.
            if (i + 1 < n && tokens.get(i).kind == TokenKind.WORD
                && tokens.get(i).text.equalsIgnoreCase("sp")) {
              i++;
              if (i < n && tokens.get(i).kind == TokenKind.DOT) i++;
            }
            continue;
          }
          // Infrageneric rank marker, e.g. "subg." — consume marker, dot, and the following capitalised epithet.
          if (afterGenus && !haveEpithet && !afterSubgenus
              && RankMarkers.matchInfrageneric(w) != null) {
            i++;
            if (i < n && tokens.get(i).kind == TokenKind.DOT) i++;
            if (i < n && tokens.get(i).kind == TokenKind.WORD && tokens.get(i).startsUpper()) {
              i++;
              afterSubgenus = true;
            }
            continue;
          }
          if (AuthorParticles.isParticle(t.text)) {
            return i;
          }
          nameWords++;
          haveEpithet = true;
          afterSubgenus = false;
          i++;
          continue;
        }
        // Mid-name author span: an Author abbreviation between the genus (or
        // species epithet) and a following rank marker. e.g. "Centaurea L. subg.
        // Jacea" or "Festuca ovina L. subvar. gracilis Hackel". The author tokens
        // are silently consumed so the boundary stays at the structural marker.
        int afterAuthor = consumeMidNameAuthor(tokens, i, n);
        if (afterAuthor > i) {
          i = afterAuthor;
          continue;
        }
        // All-caps multi-letter word in epithet position only counts as an upper-cased
        // epithet when the genus itself was all-caps (so the whole input is shouted).
        if (genusAllCaps && afterGenus && t.text.length() > 1 && isAllUpper(t.text)) {
          nameWords++;
          haveEpithet = true;
          afterSubgenus = false;
          i++;
          continue;
        }
        // Upper-case word in non-first position → authorship.
        return i;
      }

      if (t.kind == TokenKind.OPEN_PAREN) {
        if (afterGenus && !haveEpithet && !afterSubgenus) {
          int j = i + 1;
          if (j < n && tokens.get(j).kind == TokenKind.WORD && tokens.get(j).startsUpper()) {
            int k = j + 1;
            if (k < n && tokens.get(k).kind == TokenKind.CLOSE_PAREN) {
              i = k + 1;
              afterSubgenus = true;
              continue;
            }
          }
        }
        return i;
      }

      // any other token (number, dot, comma, dagger, etc.) → authorship boundary
      return i;
    }
    return n;
  }

  private static String stripDot(String s) {
    return s.endsWith(".") ? s.substring(0, s.length() - 1) : s;
  }

  /**
   * If {@code tokens[from]} starts an author span that is followed by a rank marker
   * (e.g. "L. subg.", "L. subvar.", "Asch. subsp."), returns the index just past the
   * author span (the index of the rank marker). Otherwise returns -1.
   */
  /** Public bridge so NameTokens can apply the same mid-name author skipping. */
  public static int midNameAuthorEnd(List<Token> tokens, int from, int n) {
    return consumeMidNameAuthor(tokens, from, n);
  }

  private static int consumeMidNameAuthor(List<Token> tokens, int from, int n) {
    if (from >= n) return -1;
    Token first = tokens.get(from);
    if (first.kind != TokenKind.WORD || !first.startsUpper()) return -1;
    int j = from;
    while (j < n) {
      Token t = tokens.get(j);
      if (t.kind == TokenKind.WORD) {
        if (t.startsUpper()) { j++; continue; }
        if (AuthorParticles.isParticle(t.text)) { j++; continue; }
        String w = stripDot(t.text);
        boolean[] notho = new boolean[1];
        boolean isInfraMarker = RankMarkers.matchInfraspecific(w) != null
            || RankMarkers.matchInfraspecificAllowNotho(w, notho) != null;
        boolean isInfraGenMarker = RankMarkers.matchInfrageneric(w) != null;
        if ((isInfraMarker || isInfraGenMarker) && j > from && hasEpithetAfterMarker(tokens, j, n, isInfraGenMarker)) {
          return j;
        }
        return -1;
      }
      if (t.kind == TokenKind.DOT
          || t.kind == TokenKind.AMPERSAND
          || t.kind == TokenKind.COMMA) {
        j++;
        continue;
      }
      return -1;
    }
    return -1;
  }

  /**
   * After a rank marker we expect an epithet — lowercase for infraspecific markers,
   * uppercase for infrageneric ones. Without it, the apparent "marker" was just a
   * lowercase token (e.g. "f.") that happened to spell a known marker.
   */
  private static boolean hasEpithetAfterMarker(List<Token> tokens, int markerIdx, int n, boolean infrageneric) {
    int k = markerIdx + 1;
    if (k < n && tokens.get(k).kind == TokenKind.DOT) k++;
    if (k >= n) return false;
    Token t = tokens.get(k);
    if (t.kind != TokenKind.WORD) return false;
    if (infrageneric) return t.startsUpper();
    if (!t.startsLower()) return false;
    // Reject lowercase tokens that aren't real epithets (ex/and/et/y separators,
    // particles).
    if (t.text.equalsIgnoreCase("ex") || t.text.equalsIgnoreCase("and")
        || t.text.equalsIgnoreCase("et") || t.text.equals("y")) return false;
    if (AuthorParticles.isParticle(t.text)) return false;
    return true;
  }

  private static boolean isAllUpper(String s) {
    boolean any = false;
    for (int i = 0; i < s.length(); ) {
      int cp = s.codePointAt(i);
      if (Character.isLetter(cp)) {
        any = true;
        if (!Character.isUpperCase(cp)) return false;
      }
      i += Character.charCount(cp);
    }
    return any;
  }
}
