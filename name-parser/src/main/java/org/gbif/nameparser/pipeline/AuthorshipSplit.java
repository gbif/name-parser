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

  public static int findBoundary(List<Token> tokens, ParseContext ctx) {
    final int n = tokens.size();
    if (n == 0) return 0;

    int i = 0;
    int nameWords = 0;
    boolean afterGenus = false;
    boolean afterSubgenus = false;
    boolean haveEpithet = false;
    boolean genusAllCaps = false;
    boolean genusFamilyShape = false;
    String genusText = null;

    while (i < n) {
      Token t = tokens.get(i);

      if (t.kind == TokenKind.HYBRID_MARK) {
        i++;
        continue;
      }

      // Missing-genus placeholder: "?" as the genus stand-in.
      if (nameWords == 0 && t.kind == TokenKind.OTHER && t.text.equals("?")) {
        nameWords++;
        afterGenus = true;
        i++;
        continue;
      }
      // Open-nomenclature doubtful-identification "?" between epithets — like cf./aff.
      // Skip the marker so the next epithet is included in the name section.
      if (afterGenus && t.kind == TokenKind.OTHER && t.text.equals("?")) {
        i++;
        continue;
      }
      if (t.kind == TokenKind.WORD) {
        if (nameWords == 0) {
          if (t.startsUpper()) {
            nameWords++;
            afterGenus = true;
            genusAllCaps = t.text.length() > 1 && isAllUpper(t.text);
            genusFamilyShape = isFamilyShape(t.text);
            genusText = t.text;
            i++;
            // Abbreviated genus: 1-letter ("M.") always; 2-4 letters ("Mo.", "Phl.")
            // only when the next non-dot token is a lowercase epithet — so we don't
            // fold a real binomial like "Mo Bing 1980" into "Mo." + "Bing".
            if (t.text.length() >= 1 && t.text.length() <= 4
                && i < n && tokens.get(i).kind == TokenKind.DOT) {
              boolean shortEnoughForAbbrev = t.text.length() == 1
                  || (i + 1 < n
                      && tokens.get(i + 1).kind == TokenKind.WORD
                      && tokens.get(i + 1).startsLower());
              if (shortEnoughForAbbrev) {
                i++;
              }
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
          // "anon" / "anon." — anonymous-author placeholder. Treated as the start of
          // authorship even though it's lowercase.
          if (w.equalsIgnoreCase("anon")) {
            return i;
          }
          // cf./aff. qualifiers and indet markers — keep walking
          if (w.equalsIgnoreCase("cf") || w.equalsIgnoreCase("aff")
              || w.equalsIgnoreCase("sp") || w.equalsIgnoreCase("spec")
              || w.equalsIgnoreCase("species") || w.equalsIgnoreCase("indet")) {
            boolean isSp = w.equalsIgnoreCase("sp") || w.equalsIgnoreCase("spec");
            i++;
            if (i < n && tokens.get(i).kind == TokenKind.DOT) i++;
            // A number immediately after an indet marker is the informal phrase, not authorship
            if (i < n && tokens.get(i).kind == TokenKind.NUMBER) i++;
            // Strain-code-shaped trailing token(s) ("Lepidoptera sp. JGP0404") OR a
            // single uppercase letter ("Bryozoan sp. E") form the species epithet
            // payload / phrase, not authorship — include them in the name span.
            else if (isSp && i < n && tokens.get(i).kind == TokenKind.WORD
                && (tokens.get(i).text.length() >= 2
                    || (tokens.get(i).text.length() == 1 && tokens.get(i).startsUpper()))
                && (i + 1 == n
                    || (i + 1 < n && tokens.get(i + 1).kind == TokenKind.NUMBER && i + 2 == n))) {
              i++;
              if (i < n && tokens.get(i).kind == TokenKind.NUMBER) i++;
            }
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
            // Single uppercase letter immediately after a rank marker is an informal
            // infra epithet ("form A", "f. B"), not the start of authorship.
            if (i < n && tokens.get(i).kind == TokenKind.WORD
                && tokens.get(i).text.length() == 1
                && tokens.get(i).startsUpper()) {
              i++;
              haveEpithet = true;
            }
            continue;
          }
          // Infrageneric rank marker, e.g. "subg." / "nothosect." — consume marker,
          // dot, and the following capitalised epithet.
          {
            boolean[] gnotho = new boolean[1];
            if (afterGenus && !haveEpithet && !afterSubgenus
                && RankMarkers.matchInfragenericAllowNotho(w, gnotho) != null) {
              i++;
              if (i < n && tokens.get(i).kind == TokenKind.DOT) i++;
              if (i < n && tokens.get(i).kind == TokenKind.WORD && tokens.get(i).startsUpper()) {
                i++;
                afterSubgenus = true;
              }
              continue;
            }
          }
          if (AuthorParticles.isParticle(t.text)
              || looksLikeApostropheParticle(t.text)) {
            // Particle authors may be followed by a structural rank marker — try to
            // skip past the author span as a mid-name author so the marker still gets
            // consumed by the name section.
            int afterAuthor = consumeMidNameAuthor(tokens, i, n);
            if (afterAuthor > i) {
              i = afterAuthor;
              continue;
            }
            return i;
          }
          // "hort." — horticultural marker, used as an ex-author placeholder
          // ("Acacia hort. ex Dallim."). Treat as authorship boundary.
          if (w.equalsIgnoreCase("hort")) {
            return i;
          }
          nameWords++;
          haveEpithet = true;
          afterSubgenus = false;
          i++;
          continue;
        }
        if (afterGenus && t.startsDigitEpithet()) {
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
        // epithet when the genus itself was all-caps (so the whole input is shouted) and it
        // isn't followed by an abbreviation dot (ELEV. → author). A diacritic no longer
        // disqualifies it: "CHIONE ELEVÄTA" is read like "CHIONE ELEVATA".
        if (genusAllCaps && afterGenus && t.text.length() > 1 && isAllUpper(t.text)) {
          boolean isAbbrev = i + 1 < n && tokens.get(i + 1).kind == TokenKind.DOT;
          if (!isAbbrev) {
            nameWords++;
            haveEpithet = true;
            afterSubgenus = false;
            i++;
            continue;
          }
        }
        // Upper-case word in non-first position → authorship.
        return i;
      }

      if (t.kind == TokenKind.OPEN_PAREN) {
        if (afterGenus && !haveEpithet && !afterSubgenus && !genusFamilyShape) {
          int j = i + 1;
          // The subgenus word is normally Title-cased; a lower-case word ("(acanthoderes)")
          // is a malformed subgenus that NameTokens capitalises and flags doubtful.
          if (j < n && tokens.get(j).kind == TokenKind.WORD
              && (tokens.get(j).startsUpper() || tokens.get(j).startsLower())) {
            // A single parenthesised word — plain "(Word)" or abbreviated "(Word.)".
            int k = j + 1;
            int afterParen = -1;
            boolean abbreviated = false;
            if (k < n && tokens.get(k).kind == TokenKind.CLOSE_PAREN) {
              afterParen = k + 1;
            } else if (tokens.get(j).startsUpper() && k + 1 < n
                && tokens.get(k).kind == TokenKind.DOT
                && tokens.get(k + 1).kind == TokenKind.CLOSE_PAREN) {
              afterParen = k + 2;
              abbreviated = true;
            }
            if (afterParen >= 0) {
              // Subgenus vs. parenthesised basionym author. A genus CAN carry a basionym, so
              // decide in this order:
              //  1. a species epithet (lower-case, non-particle) follows → subgenus, the name
              //     continues below it ("Amnicola (Amnicola) dubrueilliana",
              //     "Phalaena (Tin.) guttella Fab.") — a basionym author cannot sit before a
              //     species epithet, so even an abbreviated "(Tin.)" is the subgenus here;
              //  2. otherwise an abbreviated / initialled word ("(Griseb.)", "(Grev.)") is a
              //     basionym author — subgenera are always a single UNabbreviated capitalised
              //     word, never initials ("Thliphthisa (Griseb.) P.Caputo & Del Guacchio",
              //     "Genus (Grev.) Kütz. 1849");
              //  3. nothing follows ("Arrhoges (Antarctohoges)") → subgenus;
              //  4. the word repeats the genus — a nominotypical subgenus ("Morea (Morea) …");
              //  5. the caller asked for an infrageneric rank → subgenus;
              //  6. the trailing authorship carries a year OUTSIDE the parens — the zoological
              //     "Genus (Subgenus) Author, year" form ("Dicromita (Pterodicromita) Fowler,
              //     1925") → subgenus;
              //  7. otherwise a trailing author with no year makes "(Word)" the basionym author
              //     of a botanical genus recombination ("Kyphocarpa (Fenzl) Lopr.").
              // (A "(Author, year)" with the year INSIDE the parens is a multi-token paren that
              // never reaches this single-word branch and is treated as a basionym below.)
              boolean hasTrailing = afterParen < n;
              Token next = hasTrailing ? tokens.get(afterParen) : null;
              boolean trailingIsEpithet = next != null && next.kind == TokenKind.WORD
                  && next.startsLower() && !AuthorParticles.isParticle(next.text);
              boolean nominotypical = genusText != null
                  && genusText.equalsIgnoreCase(tokens.get(j).text);
              boolean rankRequestsInfragen = ctx != null && ctx.requestedRank != null
                  && ctx.requestedRank.isInfragenericStrictly();
              boolean subgenus;
              if (trailingIsEpithet) {
                subgenus = true;
              } else if (abbreviated) {
                subgenus = false;
              } else {
                subgenus = !hasTrailing || nominotypical || rankRequestsInfragen
                    || hasYearToken(tokens, afterParen, n);
              }
              if (subgenus) {
                i = afterParen;
                afterSubgenus = true;
                continue;
              }
              return i;
            }
          }
        }
        // After the species epithet, an "(BasAuth) CombAuth var. infraspecific" pattern
        // means the parenthesised basionym + combination author span sits between the
        // species and the infraspecific portion. Skip it so the rank marker + epithet
        // can be consumed as part of the name span.
        if (haveEpithet && !afterSubgenus) {
          int afterSpan = skipParenAuthorBlock(tokens, i, n);
          if (afterSpan > i) {
            i = afterSpan;
            continue;
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
    if (first.kind != TokenKind.WORD) return -1;
    // Author span starts with an uppercase word OR a particle ("d'", "de", "van", …).
    if (!first.startsUpper()
        && !AuthorParticles.isParticle(first.text)
        && !looksLikeApostropheParticle(first.text)) {
      return -1;
    }
    int j = from;
    while (j < n) {
      Token t = tokens.get(j);
      if (t.kind == TokenKind.WORD) {
        if (t.startsUpper()) { j++; continue; }
        if (AuthorParticles.isParticle(t.text)) { j++; continue; }
        // Apostrophe-particle word ("d'Urv", "L'Hér") — keep walking.
        if (looksLikeApostropheParticle(t.text)) { j++; continue; }
        // "al" / "al." inside an author span ("Boiss. & al. var. paryadrica") —
        // the "et al." abbreviation. Keep walking.
        if (t.text.equalsIgnoreCase("al")) { j++; continue; }
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
      // Apostrophe inside an author (M'Coy, d'Urv., L'Hér.) — keep walking.
      if (t.kind == TokenKind.OTHER && t.text.equals("'")) {
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
    if (k >= n) {
      // "f" is ambiguous: could be forma-rank or the "filius" author suffix.
      // Treat a trailing "f." as filius (not a rank marker) to avoid misclassification.
      String mw = tokens.get(markerIdx).text.toLowerCase();
      return !mw.equals("f");
    }
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

  /**
   * If a "(...) Author. ranklabel." span sits between the species and an infraspecific
   * epithet, returns the index of the rank-marker word. Otherwise returns -1.
   */
  /** True when tokens[from, to) contain a 4-digit year-shaped number (1xxx / 2xxx). */
  private static boolean hasYearToken(List<Token> tokens, int from, int to) {
    for (int i = from; i < to; i++) {
      Token t = tokens.get(i);
      if (t.kind == TokenKind.NUMBER && t.text.length() == 4
          && (t.text.charAt(0) == '1' || t.text.charAt(0) == '2')) {
        return true;
      }
    }
    return false;
  }

  private static int skipParenAuthorBlock(List<Token> tokens, int openIdx, int n) {
    // Match the closing paren.
    int depth = 1;
    int j = openIdx + 1;
    while (j < n && depth > 0) {
      TokenKind k = tokens.get(j).kind;
      if (k == TokenKind.OPEN_PAREN) depth++;
      else if (k == TokenKind.CLOSE_PAREN) depth--;
      if (depth == 0) break;
      j++;
    }
    if (j >= n || depth != 0) return -1;
    j++; // skip past the close paren
    // Walk over an author span (uppercase words, dots, particles) until a rank marker.
    while (j < n) {
      Token t = tokens.get(j);
      if (t.kind == TokenKind.WORD) {
        if (t.startsUpper()) { j++; continue; }
        if (AuthorParticles.isParticle(t.text)) { j++; continue; }
        String w = stripDot(t.text);
        boolean[] notho = new boolean[1];
        boolean isInfraMarker = RankMarkers.matchInfraspecificAllowNotho(w, notho) != null;
        if (isInfraMarker && hasEpithetAfterMarker(tokens, j, n, false)) {
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

  /** Globally-unambiguous family-shape suffix: a leading word ending in -aceae or
   * -oideae is always a botanical family-group name (per RankUtils.GLOBAL_SUFFICES). */
  private static boolean isFamilyShape(String s) {
    String lower = s.toLowerCase();
    return lower.endsWith("aceae") || lower.endsWith("oideae");
  }

  /** Bridge so NameTokens shares the same apostrophe-particle test ("d'Urv", "L'Hér"). */
  public static boolean isApostropheParticle(String s) {
    return looksLikeApostropheParticle(s);
  }

  private static boolean looksLikeApostropheParticle(String s) {
    int apo = s.indexOf('\'');
    if (apo < 1 || apo + 1 >= s.length()) return false;
    int next = s.codePointAt(apo + 1);
    return Character.isUpperCase(next);
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
