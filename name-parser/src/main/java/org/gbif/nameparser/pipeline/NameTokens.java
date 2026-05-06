package org.gbif.nameparser.pipeline;

import org.gbif.nameparser.api.NamePart;
import org.gbif.nameparser.api.NameType;
import org.gbif.nameparser.api.ParsedName;
import org.gbif.nameparser.api.Rank;
import org.gbif.nameparser.api.Warnings;
import org.gbif.nameparser.token.Token;
import org.gbif.nameparser.token.TokenKind;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits the name tokens (range [0, boundary)) into the structural slots of a
 * scientific name: uninomial / genus / subgenus / specific / infraspecific.
 */
public final class NameTokens {

  private static final String[] AGG_HYPHEN_SUFFIXES = {"-group", "-complex", "-aggregate"};

  private NameTokens() {}

  static void classify(ParseContext ctx, int boundary) {
    List<Token> ts = ctx.tokens;
    if (boundary == 0) {
      ctx.name.setState(ParsedName.State.PARTIAL);
      ctx.name.setUnparsed(ctx.original);
      return;
    }

    String genus = null;
    String subgenus = null;
    Rank infrageneric = null;
    String infragenEpithet = null;
    List<String> lowerEpithets = new ArrayList<>(4);
    int markerIdxInEpithets = -1;
    Rank inlineRank = null;
    boolean inlineRankNotho = false;
    boolean indet = false;
    String cfAffQualifier = null;

    int i = 0;
    while (i < boundary) {
      Token t = ts.get(i);
      if (t.kind == TokenKind.HYBRID_MARK) {
        if (genus != null && lowerEpithets.isEmpty()) {
          ctx.name.addNotho(NamePart.SPECIFIC);
        } else if (genus == null) {
          ctx.name.addNotho(NamePart.GENERIC);
        } else {
          ctx.name.addNotho(NamePart.INFRASPECIFIC);
        }
        i++;
        continue;
      }
      if (t.kind == TokenKind.OPEN_PAREN
          && i + 2 < boundary
          && ts.get(i + 1).kind == TokenKind.WORD
          && ts.get(i + 1).startsUpper()
          && ts.get(i + 2).kind == TokenKind.CLOSE_PAREN) {
        subgenus = ts.get(i + 1).text;
        i += 3;
        continue;
      }
      // abbreviated genus: single capital letter then DOT, only when no genus yet
      if (genus == null
          && t.kind == TokenKind.WORD
          && t.text.length() == 1
          && t.startsUpper()
          && i + 1 < boundary
          && ts.get(i + 1).kind == TokenKind.DOT) {
        genus = t.text + ".";
        ctx.name.setType(NameType.INFORMAL);
        i += 2;
        continue;
      }
      if (t.kind == TokenKind.WORD) {
        // Mid-name author span (uppercase Author abbreviations followed by a rank
        // marker) — silently skipped so that downstream classification operates only
        // on the structural tokens.
        if (genus != null && t.startsUpper()) {
          int after = AuthorshipSplit.midNameAuthorEnd(ts, i, boundary);
          if (after > i) {
            i = after;
            continue;
          }
        }
        if (genus == null) {
          // For a regular capital-letter genus we keep the input as-is; for all-caps or
          // all-lowercase we recover the canonical form.
          genus = recoverCase(ctx, t.text, true);
          i++;
          // pull abbreviated-genus dot through
          if (genus.length() == 2 && genus.charAt(genus.length() - 1) == '.') {
            // already a dot — nothing to do
          } else if (genus.length() == 1 && i < boundary && ts.get(i).kind == TokenKind.DOT) {
            genus = genus + ".";
            ctx.name.setType(NameType.INFORMAL);
            i++;
          }
          continue;
        }
        // upper-case epithets (e.g. all-caps "ELEVATA") — treat as lower-case epithets after recovery
        if (t.startsUpper() && genus != null && t.text.length() > 1
            && isAllUpperLetters(t.text)) {
          // synthesise a lower-case version into lowerEpithets
          String w = t.text.toLowerCase();
          lowerEpithets.add(w);
          i++;
          continue;
        }
        if (t.startsLower()) {
          String w = stripDot(t.text);
          // 1. cf./aff. qualifier
          if ((w.equalsIgnoreCase("cf") || w.equalsIgnoreCase("aff"))
              && lowerEpithets.size() < 2) {
            cfAffQualifier = w + ".";
            ctx.name.setType(NameType.INFORMAL);
            i++;
            if (i < boundary && ts.get(i).kind == TokenKind.DOT) i++;
            continue;
          }
          // 2. indet markers — sp./spec./indet
          if ((w.equalsIgnoreCase("sp") || w.equalsIgnoreCase("spec") || w.equalsIgnoreCase("species") || w.equalsIgnoreCase("indet"))
              && (lowerEpithets.isEmpty() || markerIdxInEpithets >= 0)) {
            indet = true;
            ctx.name.setType(NameType.INFORMAL);
            i++;
            if (i < boundary && ts.get(i).kind == TokenKind.DOT) i++;
            // A number immediately following the indet marker becomes the informal phrase
            if (i < boundary && ts.get(i).kind == TokenKind.NUMBER) {
              ctx.name.setPhrase(ts.get(i).text);
              i++;
            }
            continue;
          }
          // 3. infraspecific rank marker (with notho-prefix support)
          boolean[] notho = new boolean[1];
          Rank rmInfra = RankMarkers.matchInfraspecificAllowNotho(w, notho);
          if (rmInfra != null) {
            if (hasInfraspecificEpithetAfter(ts, i, boundary)) {
              inlineRank = rmInfra;
              inlineRankNotho = notho[0];
              markerIdxInEpithets = lowerEpithets.size();
              i++;
              if (i < boundary && ts.get(i).kind == TokenKind.DOT) i++;
              // microbial "f. sp." → forma specialis: skip the "sp" + dot too
              if (i + 1 < boundary && ts.get(i).kind == TokenKind.WORD
                  && ts.get(i).text.equalsIgnoreCase("sp")) {
                inlineRank = Rank.FORMA_SPECIALIS;
                i++;
                if (i < boundary && ts.get(i).kind == TokenKind.DOT) i++;
              }
            } else if (!lowerEpithets.isEmpty()) {
              // Trailing rank marker with no following epithet = indetermined infraspecific
              indet = true;
              inlineRank = rmInfra;
              inlineRankNotho = notho[0];
              markerIdxInEpithets = lowerEpithets.size();
              i++;
              if (i < boundary && ts.get(i).kind == TokenKind.DOT) i++;
            } else {
              // Rank marker before any lower epithet and no following epithet:
              // treat it as the specific epithet (e.g. "Foa fo" — "fo" is the
              // species name, not a forma rank indicator).
              lowerEpithets.add(w);
              i++;
              if (i < boundary && ts.get(i).kind == TokenKind.DOT) i++;
            }
            continue;
          }
          // 4. infrageneric marker
          Rank rmInfragen = RankMarkers.matchInfrageneric(w);
          if (rmInfragen != null && lowerEpithets.isEmpty() && subgenus == null && infragenEpithet == null) {
            i++;
            if (i < boundary && ts.get(i).kind == TokenKind.DOT) i++;
            if (i < boundary && ts.get(i).kind == TokenKind.WORD && ts.get(i).startsUpper()) {
              infragenEpithet = ts.get(i).text;
              infrageneric = rmInfragen;
              i++;
            }
            continue;
          }
          // 5. hyphenated aggregate suffix: "X-group"
          {
            String low = t.text.toLowerCase();
            String stripped = null;
            for (String suf : AGG_HYPHEN_SUFFIXES) {
              if (low.endsWith(suf) && t.text.length() > suf.length()) {
                stripped = t.text.substring(0, t.text.length() - suf.length());
                break;
              }
            }
            if (stripped != null) {
              ctx.aggregate = true;
              lowerEpithets.add(stripped);
              i++;
              continue;
            }
          }
          // 6. standalone aggregate marker after a species epithet
          if (!lowerEpithets.isEmpty()
              && (w.equalsIgnoreCase("agg") || w.equalsIgnoreCase("aggregate")
                  || w.equalsIgnoreCase("group") || w.equalsIgnoreCase("complex"))) {
            ctx.aggregate = true;
            i++;
            if (i < boundary && ts.get(i).kind == TokenKind.DOT) i++;
            continue;
          }
          // 7. "species group" two-word marker
          if (!lowerEpithets.isEmpty() && w.equalsIgnoreCase("species")
              && i + 1 < boundary && ts.get(i + 1).kind == TokenKind.WORD
              && ts.get(i + 1).text.equalsIgnoreCase("group")) {
            ctx.aggregate = true;
            i += 2;
            continue;
          }
          // 8. ordinary epithet
          lowerEpithets.add(t.text);
          i++;
          continue;
        }
        // upper word inside name section: skip
        i++;
        continue;
      }
      i++;
    }

    String specific = null;
    String infraspecific = null;
    Rank rank = null;

    if (markerIdxInEpithets >= 0) {
      if (markerIdxInEpithets >= 1) {
        specific = lowerEpithets.get(0);
      }
      if (markerIdxInEpithets < lowerEpithets.size()) {
        infraspecific = lowerEpithets.get(markerIdxInEpithets);
      }
      rank = inlineRank;
      if (lowerEpithets.size() > markerIdxInEpithets + 1) {
        StringBuilder rem = new StringBuilder();
        for (int k = markerIdxInEpithets + 1; k < lowerEpithets.size(); k++) {
          if (rem.length() > 0) rem.append(' ');
          rem.append(lowerEpithets.get(k));
        }
        ctx.name.setState(ParsedName.State.PARTIAL);
        ctx.name.setUnparsed(rem.toString());
      }
    } else if (!lowerEpithets.isEmpty()) {
      specific = lowerEpithets.get(0);
      if (lowerEpithets.size() >= 2) {
        infraspecific = lowerEpithets.get(lowerEpithets.size() - 1);
        rank = lowerEpithets.size() == 2 ? Rank.INFRASPECIFIC_NAME : Rank.INFRASUBSPECIFIC_NAME;
      } else {
        rank = Rank.SPECIES;
      }
    }

    boolean treatAsGenus = ctx.name.getCultivarEpithet() != null
        || (indet && lowerEpithets.isEmpty() && genus != null);
    if (!treatAsGenus
        && genus != null
        && specific == null
        && subgenus == null
        && infragenEpithet == null) {
      ctx.name.setUninomial(genus);
    } else {
      ctx.name.setGenus(genus);
      if (subgenus != null) ctx.name.setInfragenericEpithet(subgenus);
      if (infragenEpithet != null) {
        ctx.name.setInfragenericEpithet(infragenEpithet);
      }
      if (specific != null) ctx.name.setSpecificEpithet(specific);
      if (infraspecific != null) ctx.name.setInfraspecificEpithet(infraspecific);
    }

    Rank requested = ctx.requestedRank;
    boolean cultivarSet = ctx.name.getCultivarEpithet() != null;
    if (rank != null && !cultivarSet) {
      ctx.name.setRank(rank);
    } else if (infrageneric != null && !cultivarSet) {
      ctx.name.setRank(infrageneric);
    }

    if (inlineRankNotho) {
      ctx.name.setNotho(NamePart.INFRASPECIFIC);
    }
    if (cfAffQualifier != null) {
      // Qualifier applies to the specific or infraspecific epithet, whichever is the
      // most-specific present.
      NamePart part = infraspecific != null ? NamePart.INFRASPECIFIC : NamePart.SPECIFIC;
      ctx.name.setEpithetQualifier(part, cfAffQualifier);
    }
    if (indet) {
      ctx.name.setType(NameType.INFORMAL);
      if (infraspecific == null) {
        if (lowerEpithets.isEmpty()
            && (ctx.name.getRank() == null || ctx.name.getRank() == Rank.UNRANKED)) {
          ctx.name.setRank(Rank.SPECIES);
        }
        if (ctx.name.getPhrase() == null) {
          ctx.name.addWarning(Warnings.INDETERMINED);
        }
      }
    }
    // Caller-supplied infraspecific rank on a binomial with no infraspecific epithet
    // → treat as indeterminate infraspecific (e.g. "Lepidoptera alba DC." + SUBSPECIES)
    if (!indet && requested != null && requested != Rank.UNRANKED
        && requested.isInfraspecific() && specific != null && infraspecific == null) {
      ctx.name.setType(NameType.INFORMAL);
      ctx.name.setRank(requested);
      ctx.name.addWarning(Warnings.INDETERMINED);
    }
    if (requested == Rank.SPECIES && infraspecific != null) {
      ctx.name.addWarning(Warnings.SUBSPECIES_ASSIGNED);
    }
  }

  /** Title-cases all-caps or all-lower-case Latin words used as a genus/uninomial. */
  private static String recoverCase(ParseContext ctx, String text, boolean isGenus) {
    if (text.length() < 2) return text;

    // For hyphenated genera, normalize subsequent segments to lowercase under specific conditions:
    // - first segment is ≤ 3 chars (short prefix like "Eu-", "Le-", "Uva-"), OR
    // - name has 3+ hyphenated segments (e.g. "Prunus-Lauro-Cerasus")
    // This does NOT apply when subsequent segments already start lowercase.
    if (isGenus && text.indexOf('-') >= 0) {
      String[] parts = text.split("-", -1);
      if (parts.length >= 2) {
        boolean needsNorm = false;
        for (int k = 1; k < parts.length; k++) {
          if (!parts[k].isEmpty() && Character.isUpperCase(parts[k].codePointAt(0))) {
            needsNorm = true;
            break;
          }
        }
        if (needsNorm && (parts[0].length() <= 3 || parts.length >= 3)) {
          StringBuilder sb = new StringBuilder(parts[0]);
          for (int k = 1; k < parts.length; k++) {
            sb.append('-');
            String p = parts[k];
            if (!p.isEmpty() && Character.isUpperCase(p.codePointAt(0))) {
              int cpLen = Character.charCount(p.codePointAt(0));
              sb.appendCodePoint(Character.toLowerCase(p.codePointAt(0)));
              sb.append(p.substring(cpLen));
            } else {
              sb.append(p);
            }
          }
          return sb.toString();
        }
      }
    }

    boolean upper = isAllLetterCase(text, true);
    boolean lower = !upper && isAllLetterCase(text, false);
    if (!upper && !lower) return text;
    StringBuilder b = new StringBuilder(text.length());
    boolean first = true;
    for (int i = 0; i < text.length(); ) {
      int cp = text.codePointAt(i);
      int len = Character.charCount(cp);
      if (Character.isLetter(cp)) {
        if (first) {
          b.appendCodePoint(Character.toUpperCase(cp));
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

  private static boolean isAllUpperLetters(String s) {
    return isAllLetterCase(s, true);
  }

  /** True if the marker at {@code markerIdx} is followed by a lowercase epithet word. */
  private static boolean hasInfraspecificEpithetAfter(List<Token> ts, int markerIdx, int boundary) {
    int k = markerIdx + 1;
    if (k < boundary && ts.get(k).kind == TokenKind.DOT) k++;
    // tolerate an interleaving hybrid mark before the epithet ("var. ×alpina")
    if (k < boundary && ts.get(k).kind == TokenKind.HYBRID_MARK) k++;
    if (k >= boundary) return false;
    Token nx = ts.get(k);
    return nx.kind == TokenKind.WORD && nx.startsLower();
  }

  private static boolean isAllLetterCase(String s, boolean upper) {
    boolean haveLetter = false;
    for (int i = 0; i < s.length(); ) {
      int cp = s.codePointAt(i);
      if (Character.isLetter(cp)) {
        haveLetter = true;
        if (upper && !Character.isUpperCase(cp)) return false;
        if (!upper && !Character.isLowerCase(cp)) return false;
      }
      i += Character.charCount(cp);
    }
    return haveLetter;
  }

  private static String stripDot(String s) {
    return s.endsWith(".") ? s.substring(0, s.length() - 1) : s;
  }
}
