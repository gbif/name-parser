package org.gbif.nameparser.pipeline;

import org.gbif.nameparser.api.Authorship;
import org.gbif.nameparser.token.AuthorParticles;
import org.gbif.nameparser.token.Token;
import org.gbif.nameparser.token.TokenKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Parses the authorship section into {@link Authorship} pair (basionym + combination)
 * plus year, ex authors, and sanctioning author.
 */
public final class AuthorshipParser {

  /** Tokens that are filius/junior/etc. abbreviations gluing onto the previous author. */
  private static final Set<String> AUTHOR_SUFFIXES = Set.of(
      "f", "fil", "filius",
      "j", "jr", "junior", "jun",
      "sr", "senior", "sen",
      "ms", "ined", "Bis", "bis"
  );

  private AuthorshipParser() {}

  static class AuthState {
    Authorship combination = new Authorship();
    Authorship basionym = new Authorship();
    boolean basionymPresent;
    boolean yearRange;
    /** True when an "f."/"fil."/"filius" suffix appeared on any author — botanical signal. */
    boolean hasFilius;
    String sanctioningAuthor;
    int unparsedFrom = -1;
    String unparsedText;
    /** Secondary 4-digit year encountered after the publication year — imprint year. */
    String imprintYear;
  }

  static AuthState parse(List<Token> tokens, int from) {
    AuthState s = new AuthState();
    int i = from;
    final int n = tokens.size();

    if (i < n && tokens.get(i).kind == TokenKind.OPEN_PAREN) {
      int close = findClose(tokens, i);
      if (close > i) {
        // Inside the basionym brackets, a colon also separates the original author
        // from the sanctioning author ("(Fr. : Fr.)"). Drop the sanctioning span;
        // canonical names attribute the sanctioning to the species level.
        int basFrom = i + 1;
        int basEnd = close;
        int basColon = findLastColon(tokens, basFrom, basEnd);
        if (basColon > basFrom) {
          basEnd = basColon;
        }
        // Reject a basionym made up of only lowercase tokens (no real surname).
        // "(ilic)" is malformed — capture it as unparsed instead.
        if (hasUpperWord(tokens, basFrom, basEnd)) {
          s.yearRange |= parseAuthors(tokens, basFrom, basEnd, s.basionym, s);
          s.hasFilius |= containsFiliusSuffix(tokens, basFrom, basEnd);
          s.basionymPresent = true;
        } else {
          // Park the whole "(...)" span as unparsed and skip past it.
          Token openTok = tokens.get(i);
          Token closeTok = tokens.get(close);
          s.unparsedFrom = i;
          s.unparsedText = sliceText(tokens, openTok.start, closeTok.end);
        }
        i = close + 1;
      }
    }

    if (i < n) {
      int combFrom = i;
      int combEnd = n;
      // pull out a colon + sanctioning author at the end of the combination span
      int colon = findLastColon(tokens, combFrom, combEnd);
      if (colon > combFrom) {
        StringBuilder sb = new StringBuilder();
        appendAuthorWords(tokens, colon + 1, combEnd, sb);
        if (sb.length() > 0) {
          s.sanctioningAuthor = sb.toString();
          combEnd = colon;
        }
      }
      s.yearRange |= parseAuthors(tokens, combFrom, combEnd, s.combination, s);
      s.hasFilius |= containsFiliusSuffix(tokens, combFrom, combEnd);
      // Whether or not a sanctioning author was extracted, the entire trailing span
      // belongs to combination + sanctioning; nothing is unparsed afterwards.
      i = n;
    }

    if (i < n) {
      Token first = tokens.get(i);
      Token last = tokens.get(n - 1);
      s.unparsedFrom = i;
      s.unparsedText = sliceText(tokens, first.start, last.end);
    }
    return s;
  }

  private static int findLastColon(List<Token> tokens, int from, int to) {
    int depth = 0;
    int last = -1;
    for (int j = from; j < to; j++) {
      TokenKind k = tokens.get(j).kind;
      if (k == TokenKind.OPEN_PAREN || k == TokenKind.OPEN_BRACKET) depth++;
      else if (k == TokenKind.CLOSE_PAREN || k == TokenKind.CLOSE_BRACKET) depth--;
      else if (depth == 0 && k == TokenKind.COLON) last = j;
    }
    return last;
  }

  private static int findClose(List<Token> tokens, int openIdx) {
    int depth = 1;
    for (int j = openIdx + 1; j < tokens.size(); j++) {
      TokenKind k = tokens.get(j).kind;
      if (k == TokenKind.OPEN_PAREN) depth++;
      else if (k == TokenKind.CLOSE_PAREN) {
        depth--;
        if (depth == 0) return j;
      }
    }
    return -1;
  }

  /**
   * Parses author list within [from, to), populating {@code into}. Handles "ex"
   * splitting (ex authors come before main authors). When a second 4-digit year is
   * encountered after the first, it's recorded into {@code state.imprintYear} (if
   * non-null) so callers can surface it on the parsed name.
   * @return true if a year range was detected (e.g. "1845-1847", "1987-92")
   */
  private static boolean parseAuthors(List<Token> tokens, int from, int to, Authorship into, AuthState state) {
    List<String> authors = new ArrayList<>();
    List<String> exAuthors = null;
    StringBuilder cur = new StringBuilder();
    boolean yearRange = false;
    int i = from;

    while (i < to) {
      Token t = tokens.get(i);

      // year
      if (t.kind == TokenKind.NUMBER && t.text.length() >= 3 && t.text.length() <= 4) {
        flush(cur, authors);
        String year = t.text;
        // Detect a bracketed year: "[YYYY]" / "[YYYY?]". A year inside square brackets
        // is by convention the imprint year (the year actually printed on the work),
        // not the nominal publication year — even when it is the only year given.
        boolean inBrackets = i > from
            && tokens.get(i - 1).kind == TokenKind.OPEN_BRACKET;
        i++;
        // Uncertain year: a trailing "?" is part of the year ("198?" → year="198?").
        if (i < to && tokens.get(i).kind == TokenKind.OTHER && tokens.get(i).text.equals("?")) {
          year = year + "?";
          i++;
        }
        // Confirm we're still inside the brackets (close-bracket follows the year).
        if (inBrackets) {
          if (!(i < to && tokens.get(i).kind == TokenKind.CLOSE_BRACKET)) {
            inBrackets = false;
          }
        }
        if (inBrackets) {
          // Imprint year always goes to state.imprintYear; never overrides into.year.
          if (state != null && state.imprintYear == null) {
            state.imprintYear = year;
          }
          i++; // skip CLOSE_BRACKET
          continue;
        }
        // First year wins — imprint dates ("Linnaeus, 1898, 1897") keep the first
        // (the actual publication year); the second is recorded into state.imprintYear
        // so the caller can surface it on the parsed name.
        if (into.getYear() == null) {
          into.setYear(year);
        } else if (state != null && state.imprintYear == null) {
          state.imprintYear = year;
        }
        // Detect year range: NUMBER + OTHER("-" or "/") + NUMBER → keep first year only
        if (i + 1 < to) {
          Token sep = tokens.get(i);
          if (sep.kind == TokenKind.OTHER && (sep.text.equals("-") || sep.text.equals("/"))) {
            Token nx = tokens.get(i + 1);
            if (nx.kind == TokenKind.NUMBER && nx.text.length() >= 1 && nx.text.length() <= 4) {
              yearRange = true;
              i += 2; // skip separator and trailing year
            }
          }
        }
        // Drop a single trailing lowercase-letter year disambiguator ("1935h" / "1935 h",
        // or "193k7" where the k is an OCR/typo artifact followed by digits).
        // Matches: a single lowercase letter, optionally followed by digits only (e.g. "k7").
        if (i < to) {
          Token nx = tokens.get(i);
          if (nx.kind == TokenKind.WORD && nx.startsLower() && isYearDisambiguator(nx.text)) {
            i++;
          }
        }
        continue;
      }

      // ex separator
      if (t.kind == TokenKind.WORD && t.text.equals("ex")) {
        flush(cur, authors);
        // everything collected so far becomes ex authors
        exAuthors = new ArrayList<>(authors);
        authors.clear();
        i++;
        continue;
      }

      // separators
      if (t.kind == TokenKind.AMPERSAND
          || (t.kind == TokenKind.WORD && (t.text.equalsIgnoreCase("and") || t.text.equalsIgnoreCase("et")))
          || (t.kind == TokenKind.WORD && t.text.equals("y"))) {
        flush(cur, authors);
        i++;
        continue;
      }

      if (t.kind == TokenKind.SEMICOLON) {
        // Semicolons separate authors in citation lists ("Choi,J.H.; Im,W.T.; …")
        flush(cur, authors);
        i++;
        continue;
      }

      if (t.kind == TokenKind.COMMA) {
        // peek next token to decide: comma+year, comma+author, comma+& etc.
        int j = i + 1;
        while (j < to && (tokens.get(j).kind == TokenKind.AMPERSAND
            || (tokens.get(j).kind == TokenKind.WORD
                && (tokens.get(j).text.equalsIgnoreCase("and") || tokens.get(j).text.equalsIgnoreCase("et"))))) {
          j++;
        }
        if (j < to) {
          Token next = tokens.get(j);
          if (next.kind == TokenKind.NUMBER) {
            flush(cur, authors);
            i++;
            continue;
          }
          if (next.kind == TokenKind.WORD
              && (next.startsUpper() || AuthorParticles.isParticle(next.text))) {
            flush(cur, authors);
            i++;
            continue;
          }
        }
        flush(cur, authors);
        i++;
        continue;
      }

      // particle
      if (t.kind == TokenKind.WORD && t.startsLower() && AuthorParticles.isParticle(t.text)) {
        appendSpace(cur);
        cur.append(t.text);
        i++;
        // Pull through abbreviation dots that follow ("v." / "v.d." style particles).
        while (i < to && tokens.get(i).kind == TokenKind.DOT) {
          cur.append('.');
          i++;
        }
        continue;
      }

      // surname token
      if (t.kind == TokenKind.WORD && t.startsUpper()) {
        // No-comma "<Surname> <Initials>" inversion pattern: if cur already holds a Latin
        // surname AND the incoming token is a short all-caps word, treat it as the
        // initials trailing the surname and flush as a single inverted author.
        // A trailing dot in cur means we are mid-abbreviation (e.g. "v.d." awaiting "L"),
        // not after a complete surname — skip the inversion in that case.
        if (cur.length() > 0 && t.text.length() <= 3 && isAllUpper(t.text)
            && containsLower(cur)
            && cur.charAt(cur.length() - 1) != '.'
            && !endsWithParticleOnly(cur)) {
          StringBuilder initials = new StringBuilder(t.text);
          int j = i + 1;
          while (j < to && tokens.get(j).kind == TokenKind.DOT) {
            initials.append('.');
            j++;
          }
          // Decide whether to flip "<Surname> <ALLCAPS>" → "<initials>.<Surname>" or to
          // keep the CJK surname-first form ("Zhang F", "Pan Z-X") verbatim.
          //   - Dotted trailing initials always flip ("Foo X." → "X.Foo").
          //   - Without a trailing dot, only flip when the next token is yet another
          //     capitalised surname-shaped word — that means we are inside a run-on author
          //     list ("Balsamo M Fregni E Tongiorgi MA") and the all-caps token marks the
          //     end of the current author. An isolated "Zhang F" (end of segment or
          //     followed by separator) keeps its CJK surname-first form.
          // Pick up an optional "-X" continuation so "Pan Z-X" is treated as one author
          // pair (initials "Z-X") rather than two.
          int k = j;
          if (k < to) {
            Token peek = tokens.get(k);
            if (peek.kind == TokenKind.OTHER && peek.text.equals("-")
                && k + 1 < to && tokens.get(k + 1).kind == TokenKind.WORD
                && tokens.get(k + 1).text.length() == 1) {
              initials.append('-').append(tokens.get(k + 1).text);
              k += 2;
              while (k < to && tokens.get(k).kind == TokenKind.DOT) {
                initials.append('.');
                k++;
              }
            }
          }
          String surname = cur.toString().trim();
          cur.setLength(0);
          authors.add(formatInitials(initials.toString()) + surname);
          i = k;
          continue;
        }
        // If full ALL-CAPS author name (e.g. FISCHER) length > 1, normalise to title case
        String text = normaliseAuthorCase(t.text);
        appendSpace(cur);
        cur.append(text);
        i++;
        // A single capital letter without a trailing dot is an abbreviated initial —
        // supply the dot so subsequent tokens chain ("A S. Xu" → "A.S.Xu").
        if (text.length() == 1 && Character.isUpperCase(text.codePointAt(0))
            && (i >= to || tokens.get(i).kind != TokenKind.DOT)) {
          cur.append('.');
        }
        // chain "<DOT> [WORD]" sequences for "Müll.Arg." style and "L. f" style.
        while (i < to) {
          Token nx = tokens.get(i);
          if (nx.kind == TokenKind.DOT) {
            cur.append('.');
            i++;
            continue;
          }
          if (nx.kind == TokenKind.WORD) {
            String nxt = nx.text;
            // Filius / junior / etc. — case-sensitive: lowercase only. An uppercase "F"
            // following a surname is an initial, not the filius suffix, so we don't
            // collapse it here.
            if (AUTHOR_SUFFIXES.contains(nxt)) {
              // abbreviated surname ends with '.': "Burm.f." — no separator needed
              // full surname ends with a letter: "Hooker f." — use a space
              if (cur.length() > 0 && cur.charAt(cur.length() - 1) != '.') {
                cur.append(' ');
              }
              cur.append(nxt);
              i++;
              // optional dot after suffix
              if (i < to && tokens.get(i).kind == TokenKind.DOT) {
                cur.append('.');
                i++;
              }
              continue;
            }
            // continued upper-case piece (common in compound surnames "Saint-Lager", "Müll.Arg.")
            if (nx.startsUpper() && cur.length() > 0 && cur.charAt(cur.length() - 1) == '.') {
              cur.append(normaliseAuthorCase(nx.text));
              i++;
              continue;
            }
            break;
          }
          break;
        }
        continue;
      }

      // lone "f." or "f" sitting after a separator: treat as filius glued to last author
      if (t.kind == TokenKind.WORD
          && t.text.length() <= 3
          && AUTHOR_SUFFIXES.contains(t.text.toLowerCase())) {
        // attach to the last completed author if any, else to current buffer
        if (cur.length() == 0 && !authors.isEmpty()) {
          String last = authors.remove(authors.size() - 1);
          if (!last.endsWith(".")) last = last + ".";
          authors.add(last + t.text);
        } else {
          if (cur.length() > 0 && cur.charAt(cur.length() - 1) != '.') cur.append('.');
          cur.append(t.text);
        }
        i++;
        if (i < to && tokens.get(i).kind == TokenKind.DOT) {
          if (cur.length() > 0 && cur.charAt(cur.length() - 1) != '.') cur.append('.');
          else if (!authors.isEmpty()) {
            String last = authors.get(authors.size() - 1);
            if (!last.endsWith(".")) authors.set(authors.size() - 1, last + ".");
          }
          i++;
        }
        continue;
      }

      if (t.kind == TokenKind.WORD) {
        appendSpace(cur);
        cur.append(t.text);
        i++;
        // chain trailing dot for abbreviations like "al."
        if (i < to && tokens.get(i).kind == TokenKind.DOT) {
          cur.append('.');
          i++;
        }
        continue;
      }

      // Apostrophe between authors / inside an author span — preserve it so that
      // names with internal apostrophes ("L.'t Mannetje", "M'Coy", "d'Urv.", "'t Hart")
      // render verbatim. Glue to the preceding character when there's no whitespace
      // gap in the input ("d'Urv"); otherwise insert a space ("Henk 't").
      if (t.kind == TokenKind.OTHER && t.text.equals("'")) {
        boolean hasGap = cur.length() > 0 && i > 0 && tokens.get(i - 1).end < t.start;
        if (hasGap) appendSpace(cur);
        cur.append('\'');
        i++;
        if (i < to && tokens.get(i).kind == TokenKind.WORD) {
          cur.append(tokens.get(i).text);
          i++;
        }
        continue;
      }
      // "?" inside an author span (typically a transcription artefact for a missing
      // letter, "Istv?nffi") — silently glue the next word onto cur without a space.
      if (t.kind == TokenKind.OTHER && t.text.equals("?") && cur.length() > 0) {
        i++;
        if (i < to && tokens.get(i).kind == TokenKind.WORD) {
          cur.append(tokens.get(i).text);
          i++;
        }
        continue;
      }
      // Hyphen between abbreviated initial parts ("C.-K.", "J.-j.") — keep the hyphen
      // and preserve the input case of the single-letter follow-up so compound initials
      // round-trip cleanly ("Y.-j." stays "Y.-j.", "C.-K." stays "C.-K.").
      if (t.kind == TokenKind.OTHER && t.text.equals("-")
          && cur.length() > 0 && cur.charAt(cur.length() - 1) == '.'
          && i + 1 < to && tokens.get(i + 1).kind == TokenKind.WORD) {
        Token nx = tokens.get(i + 1);
        cur.append('-');
        if (nx.text.length() == 1) {
          cur.append(nx.text);
          i += 2;
          if (i < to && tokens.get(i).kind == TokenKind.DOT) {
            cur.append('.');
            i++;
          }
          continue;
        }
        i++;
        continue;
      }
      // Unknown punctuation in an author run — skip silently
      i++;
    }
    flush(cur, authors);

    if (!authors.isEmpty()) {
      into.setAuthors(invertAll(authors));
    }
    if (exAuthors != null && !exAuthors.isEmpty()) {
      into.setExAuthors(invertAll(exAuthors));
    }
    return yearRange;
  }

  /**
   * Walks the author list applying two transforms:
   * <ol>
   *   <li>Merge pairs where author N is a surname and author N+1 is initials only,
   *       producing the combined "<initials>.<surname>" form (e.g. "LeConte" + "J.L." → "J.L.LeConte").</li>
   *   <li>Invert single authors of the form "Surname X.Y." or "Surname, X.Y." into the same
   *       canonical form.</li>
   * </ol>
   */
  private static List<String> invertAll(List<String> authors) {
    List<String> out = new ArrayList<>(authors.size());
    int i = 0;
    while (i < authors.size()) {
      String cur = authors.get(i);
      if (i + 1 < authors.size()) {
        String next = authors.get(i + 1);
        if (looksLikeSurname(cur) && looksLikeInitials(next)) {
          out.add(formatInitials(next) + cur);
          i += 2;
          continue;
        }
      }
      out.add(invertAuthor(cur));
      i++;
    }
    return out;
  }

  /**
   * Re-orders names like "Surname, J.L." or "Surname Initials" into the canonical
   * "J.L.Surname" form (no space between dotted initials and the surname).
   */
  static String invertAuthor(String s) {
    s = s.trim();
    if (s.isEmpty()) return s;

    // Pattern A: "Surname, X.Y." or "Surname, XY"
    int comma = s.indexOf(',');
    if (comma > 0 && s.indexOf(',', comma + 1) < 0) {
      String surname = s.substring(0, comma).trim();
      String initials = s.substring(comma + 1).trim();
      if (looksLikeSurname(surname) && looksLikeInitials(initials)) {
        return formatInitials(initials) + surname;
      }
    }
    // Pattern B: "Surname X.Y." (trailing dotted initials). Without a dot the
    // trailing all-caps part is a CJK-style surname-first author ("Zhang F",
    // "Pan Z-X") and must be preserved verbatim. Particles like "Van", "de"
    // must be kept on the surname side.
    int lastSpace = s.lastIndexOf(' ');
    if (lastSpace > 0) {
      String first = s.substring(0, lastSpace).trim();
      String last = s.substring(lastSpace + 1).trim();
      if (last.indexOf('.') >= 0
          && looksLikeSurname(first) && looksLikeInitials(last) && !containsParticle(first)) {
        return formatInitials(last) + first;
      }
    }
    return s;
  }

  private static boolean looksLikeSurname(String s) {
    if (s.length() < 2) return false;
    if (!Character.isUpperCase(s.codePointAt(0))) return false;
    boolean hasLower = false;
    for (int i = 0; i < s.length(); ) {
      int cp = s.codePointAt(i);
      if (Character.isLowerCase(cp)) { hasLower = true; break; }
      i += Character.charCount(cp);
    }
    return hasLower;
  }

  private static boolean looksLikeInitials(String s) {
    if (s.isEmpty()) return false;
    String stripped = s.replace(".", "").replace("-", "").replace(" ", "");
    if (stripped.isEmpty() || stripped.length() > 4) return false;
    // All characters must be UPPER-case letters (true initials, not a short surname
    // like "Liu"/"Fr."/"Sacc." which would have lower-case follow-up letters).
    for (int i = 0; i < stripped.length(); ) {
      int cp = stripped.codePointAt(i);
      if (!Character.isLetter(cp) || !Character.isUpperCase(cp)) return false;
      i += Character.charCount(cp);
    }
    return true;
  }

  private static String formatInitials(String s) {
    String cleaned = s.replace(".", "").replace(" ", "");
    // Hyphenated CJK-style initials ("Z-X", "Y-H") keep the hyphen and get a single
    // trailing dot: "Z-X" → "Z-X.". Non-hyphenated initials emit one dot per letter.
    if (cleaned.contains("-")) {
      StringBuilder b = new StringBuilder();
      for (int i = 0; i < cleaned.length(); ) {
        int cp = cleaned.codePointAt(i);
        if (cp == '-') {
          b.append('-');
        } else {
          b.appendCodePoint(Character.toUpperCase(cp));
        }
        i += Character.charCount(cp);
      }
      b.append('.');
      return b.toString();
    }
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < cleaned.length(); ) {
      int cp = cleaned.codePointAt(i);
      b.appendCodePoint(Character.toUpperCase(cp)).append('.');
      i += Character.charCount(cp);
    }
    return b.toString();
  }

  /**
   * True when the token text looks like a year-disambiguator suffix that should be
   * dropped after a year token. Matches a single lowercase letter optionally followed by
   * all-digit characters — e.g. "h" (from "1935h"), "k7" (OCR-garbled year-suffix artifact in "193k7").
   */
  private static boolean isYearDisambiguator(String s) {
    if (s.isEmpty() || !Character.isLowerCase(s.codePointAt(0))) return false;
    for (int i = Character.charCount(s.codePointAt(0)); i < s.length(); ) {
      int cp = s.codePointAt(i);
      if (!Character.isDigit(cp)) return false;
      i += Character.charCount(cp);
    }
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

  /**
   * True when the accumulated buffer ends with a particle word (or is just particles).
   * In that case the next all-caps token is the next author's initial, not a trailing
   * surname-inversion signal — there's no real surname accumulated yet.
   */
  private static boolean endsWithParticleOnly(StringBuilder cur) {
    String s = cur.toString().trim();
    if (s.isEmpty()) return true;
    // Particle attached to a preceding dot ("H.da") is still considered a particle
    // tail — split on whitespace and dots to find the last word-ish token.
    String[] parts = s.split("[\\s.]+");
    String last = parts.length > 0 ? parts[parts.length - 1] : "";
    return AuthorParticles.isParticle(last);
  }

  private static boolean containsLower(StringBuilder cur) {
    for (int i = 0; i < cur.length(); ) {
      int cp = cur.codePointAt(i);
      if (Character.isLowerCase(cp)) return true;
      i += Character.charCount(cp);
    }
    return false;
  }

  private static boolean containsParticle(String s) {
    for (String word : s.split("\\s+")) {
      if (AuthorParticles.isParticle(word)) return true;
    }
    return false;
  }

  private static void flush(StringBuilder cur, List<String> authors) {
    if (cur.length() > 0) {
      authors.add(cur.toString().trim());
      cur.setLength(0);
    }
  }

  private static void appendSpace(StringBuilder cur) {
    if (cur.length() == 0) return;
    char last = cur.charAt(cur.length() - 1);
    if (last == ' ' || last == '.' || last == '-') return;
    cur.append(' ');
  }

  private static void appendAuthorWords(List<Token> tokens, int from, int to, StringBuilder sb) {
    int i = from;
    while (i < to) {
      Token t = tokens.get(i);
      if (t.kind == TokenKind.WORD) {
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '.') sb.append(' ');
        sb.append(normaliseAuthorCase(t.text));
        i++;
        while (i < to && tokens.get(i).kind == TokenKind.DOT) {
          sb.append('.');
          i++;
        }
      } else {
        i++;
      }
    }
  }

  /** Normalises an ALL-CAPS author word to title case ("FISCHER" → "Fischer"). Short
   *  all-caps tokens (≤3 chars) are kept as-is — they are likely initials ("MA", "DC"). */
  static String normaliseAuthorCase(String s) {
    if (s.length() < 4) return s;
    boolean allUpper = true;
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (Character.isLetter(c) && !Character.isUpperCase(c)) {
        allUpper = false;
        break;
      }
    }
    if (!allUpper) return s;
    StringBuilder b = new StringBuilder(s.length());
    boolean first = true;
    for (int i = 0; i < s.length(); ) {
      int cp = s.codePointAt(i);
      int len = Character.charCount(cp);
      if (Character.isLetter(cp)) {
        if (first) {
          b.appendCodePoint(cp);
          first = false;
        } else {
          b.appendCodePoint(Character.toLowerCase(cp));
        }
      } else {
        b.appendCodePoint(cp);
        first = true;
      }
      i += len;
    }
    return b.toString();
  }

  private static String sliceText(List<Token> tokens, int start, int end) {
    StringBuilder sb = new StringBuilder();
    for (Token t : tokens) {
      if (t.start >= start && t.end <= end) {
        sb.append(t.text);
      }
    }
    return sb.toString();
  }

  /** True if any token in [from, to) is a WORD that starts with an upper-case letter. */
  private static boolean hasUpperWord(List<Token> tokens, int from, int to) {
    for (int j = from; j < to; j++) {
      Token t = tokens.get(j);
      if (t.kind == TokenKind.WORD && t.startsUpper()) return true;
    }
    return false;
  }

  /**
   * Scans the token range for an "f"/"fil"/"filius" word — botanical filius marker.
   * Pre-"ex" tokens are skipped: a filius on an ex-author isn't a code signal because
   * the author itself was never the validating author. The "f"/"fil" token must be
   * preceded by whitespace in the input to count as a filius marker; an adjacent "L.f"
   * (no space) is just another initial.
   */
  private static boolean containsFiliusSuffix(List<Token> tokens, int from, int to) {
    int start = from;
    for (int j = from; j < to; j++) {
      Token t = tokens.get(j);
      if (t.kind == TokenKind.WORD && t.text.equals("ex")) {
        start = j + 1;
      }
    }
    for (int j = start; j < to; j++) {
      Token t = tokens.get(j);
      if (t.kind != TokenKind.WORD) continue;
      String w = t.text;
      if (!(w.equals("f") || w.equals("fil") || w.equals("filius"))) continue;
      // Require a whitespace gap between the previous token and this "f" — otherwise
      // it's a glued initial like "L.f", not the filius marker.
      if (j > 0 && tokens.get(j - 1).end < t.start) {
        return true;
      }
      // No previous token (first token in segment): treat as filius if it's at the
      // very start (rare — usually filius follows a surname).
      if (j == 0) return true;
    }
    return false;
  }
}
