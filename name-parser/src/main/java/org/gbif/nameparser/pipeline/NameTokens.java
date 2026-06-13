package org.gbif.nameparser.pipeline;

import org.gbif.nameparser.api.NamePart;
import org.gbif.nameparser.api.NameType;
import org.gbif.nameparser.api.ParsedName;
import org.gbif.nameparser.api.Rank;
import org.gbif.nameparser.api.Warnings;
import org.gbif.nameparser.token.AuthorParticles;
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
    // Tracks the most recently skipped mid-name author span so that, when a second
    // infraspecific marker overrides the first, we can describe the dropped middle
    // classification ("Intermediate classification removed: subsp.X Author") in a warning.
    String pendingMidNameAuthor = null;

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
      // "(Word)" → subgenus, but only before any species epithet. After a species
      // epithet "(Klatt)" is a basionym author span (e.g. the autonym "Trimezia spathata
      // (Klatt) Baker subsp. spathata"), handled by the skipParenAuthorBlock branch below.
      if (t.kind == TokenKind.OPEN_PAREN
          && lowerEpithets.isEmpty() && subgenus == null
          && i + 2 < boundary
          && ts.get(i + 1).kind == TokenKind.WORD
          && ts.get(i + 1).startsUpper()
          && ts.get(i + 2).kind == TokenKind.CLOSE_PAREN) {
        subgenus = ts.get(i + 1).text;
        i += 3;
        continue;
      }
      // Abbreviated subgenus: "(Tin.)" / "(G.)" — Title-cased word + DOT inside parens.
      // Only fires before any species epithet, otherwise "(Aubl.)" after a species is
      // a basionym author span and the next block (skipParenAuthorBlock) handles it.
      if (t.kind == TokenKind.OPEN_PAREN
          && lowerEpithets.isEmpty() && subgenus == null
          && i + 3 < boundary
          && ts.get(i + 1).kind == TokenKind.WORD
          && ts.get(i + 1).startsUpper()
          && ts.get(i + 2).kind == TokenKind.DOT
          && ts.get(i + 3).kind == TokenKind.CLOSE_PAREN) {
        subgenus = ts.get(i + 1).text + ".";
        ctx.name.addWarning(Warnings.ABBREVIATED_SUBGENUS);
        i += 4;
        continue;
      }
      // After the species epithet, "(BasionymAuth) CombAuth …" sits between species and
      // an infraspecific rank marker. Skip the parens + comb-author span so the rank
      // marker that follows can be classified normally.
      if (t.kind == TokenKind.OPEN_PAREN
          && genus != null && !lowerEpithets.isEmpty()) {
        int after = skipParenAuthorBlock(ts, i, boundary);
        if (after > i) {
          // Record the species-level author span [i, after) so the pipeline can attach it
          // to an autonym (ICN Art. 22.1/26.1). Only the first span, right after the
          // species epithet and before any infraspecific marker, is the species author.
          if (ctx.midAuthorFrom < 0 && lowerEpithets.size() == 1 && markerIdxInEpithets < 0) {
            ctx.midAuthorFrom = i;
            ctx.midAuthorTo = after;
          }
          i = after;
          continue;
        }
      }
      // Abbreviated genus: Title-cased word of 1-4 chars then DOT, only when no genus
      // yet. The single-letter form ("M. alpium") is unambiguous; 2-4 letter forms
      // ("Mo. alpium", "Phl. guttella", "Pseud. dendrobii") are recognised when the
      // next non-DOT token is a lowercase epithet (so an authorship-sequence like
      // "Mo.J.Wong, 1990" doesn't trip).
      if (genus == null
          && t.kind == TokenKind.WORD
          && t.text.length() >= 1 && t.text.length() <= 4
          && t.startsUpper()
          && i + 1 < boundary
          && ts.get(i + 1).kind == TokenKind.DOT) {
        boolean nextIsLowerEpithet = i + 2 < boundary
            && ts.get(i + 2).kind == TokenKind.WORD
            && ts.get(i + 2).startsLower();
        if (t.text.length() == 1 || nextIsLowerEpithet) {
          genus = t.text + ".";
          ctx.name.setType(NameType.INFORMAL);
          ctx.name.addWarning(Warnings.ABBREVIATED_GENUS);
          i += 2;
          continue;
        }
      }
      // Missing-genus placeholder: "?" as the genus stand-in. Only at the very start.
      if (genus == null && t.kind == TokenKind.OTHER && t.text.equals("?")) {
        genus = "?";
        i++;
        continue;
      }
      // Open-nomenclature doubtful-identification marker: "?" between epithets, like
      // "Ferganoconcha? oblonga" or "Buteo borealis ? ventralis". Treat exactly like
      // cf./aff.: skip the token, attach the qualifier to the next epithet (specific
      // when no species yet, infraspecific when one exists), set type=INFORMAL,
      // doubtful, and emit QUESTION_MARKS_REMOVED.
      if (genus != null && cfAffQualifier == null
          && t.kind == TokenKind.OTHER && t.text.equals("?")
          && lowerEpithets.size() < 2) {
        cfAffQualifier = "?";
        ctx.name.setType(NameType.INFORMAL);
        ctx.name.setDoubtful(true);
        ctx.name.addWarning(Warnings.QUESTION_MARKS_REMOVED);
        i++;
        continue;
      }
      if (t.kind == TokenKind.WORD) {
        // Mid-name author span (uppercase Author abbreviations followed by a rank
        // marker, or particle-starting authors like "d'Urv. subsp.") — silently
        // skipped so that downstream classification operates only on the structural
        // tokens.
        boolean canStartAuthor = t.startsUpper()
            || (t.startsLower() && (AuthorParticles.isParticle(t.text)
                || AuthorshipSplit.isApostropheParticle(t.text)));
        if (genus != null && canStartAuthor) {
          int after = AuthorshipSplit.midNameAuthorEnd(ts, i, boundary);
          if (after > i) {
            // Record the species-level author span so the pipeline can attach it to an
            // autonym (ICN Art. 22.1/26.1): only the first span, sitting directly after
            // the species epithet and before any infraspecific marker.
            if (ctx.midAuthorFrom < 0 && lowerEpithets.size() == 1 && markerIdxInEpithets < 0) {
              ctx.midAuthorFrom = i;
              ctx.midAuthorTo = after;
            }
            pendingMidNameAuthor = renderAuthorSpan(ts, i, after);
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
        // upper-case epithets (e.g. all-caps "ELEVATA") — treat as lower-case epithets after recovery.
        // Don't synthesise an epithet from an all-caps token that contains a non-ASCII
        // letter ("ELEVÄTA") or that's followed by an abbreviation dot ("ELEV." → "ELEV"
        // + "."): those look like all-caps author surnames, not epithets.
        if (t.startsUpper() && genus != null && t.text.length() > 1
            && isAllUpperLetters(t.text)) {
          boolean hasNonAscii = false;
          for (int j = 0; j < t.text.length(); ) {
            int cp = t.text.codePointAt(j);
            if (cp > 0x7F) { hasNonAscii = true; break; }
            j += Character.charCount(cp);
          }
          boolean isAbbrev = i + 1 < boundary && ts.get(i + 1).kind == TokenKind.DOT;
          if (!hasNonAscii && !isAbbrev) {
            String w = t.text.toLowerCase();
            lowerEpithets.add(w);
            i++;
            continue;
          }
          // Otherwise fall through to author-recovery handling below.
        }
        if (t.startsLower()) {
          String w = stripDot(t.text);
          // 0. Single Greek letter or short ASCII letter between two lowercase epithets
          // ("Agaricus collinitus β mucosus", "Cyphelium disseminatum c subsessile") is
          // an informal infra-rank marker, not an epithet. Skip silently when there's
          // already a species epithet AND another lower-case epithet follows — otherwise
          // the letter IS the epithet (e.g. "var. β").
          if (w.length() == 1 && !lowerEpithets.isEmpty()) {
            int cp = w.codePointAt(0);
            boolean isGreek = cp >= 0x03B1 && cp <= 0x03C9;
            boolean isFungiAscii = cp == '⍺' || (cp >= 'a' && cp <= 'g');
            if (isGreek || isFungiAscii) {
              int peek = i + 1;
              if (peek < boundary && ts.get(peek).kind == TokenKind.WORD
                  && ts.get(peek).startsLower()) {
                i++;
                continue;
              }
            }
          }
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
            // A number immediately following the indet marker becomes the informal phrase.
            // When the source spelled out the marker as the full word "species" we keep it
            // verbatim in the phrase ("Allium species 1" → phrase "species 1") rather than
            // collapsing it to the synthetic "sp." marker; the formatter then renders the
            // phrase as-is. Abbreviated "sp."/"spec." keep the number-only phrase.
            if (i < boundary && ts.get(i).kind == TokenKind.NUMBER) {
              if (w.equalsIgnoreCase("species")) {
                ctx.name.setPhrase("species " + ts.get(i).text);
              } else {
                ctx.name.setPhrase(ts.get(i).text);
              }
              i++;
            } else if (i < boundary && ts.get(i).kind == TokenKind.WORD
                && ts.get(i).text.length() == 1 && ts.get(i).startsUpper()
                && i + 1 == boundary) {
              // Single uppercase letter following sp. — informal phrase identifier
              // ("Bryozoan sp. E"). Stored as phrase, leaves indet=true.
              ctx.name.setPhrase(ts.get(i).text);
              i++;
            } else if (i < boundary && ts.get(i).kind == TokenKind.WORD
                && ts.get(i).text.length() >= 2
                && (i + 1 == boundary
                    || (i + 1 < boundary && ts.get(i + 1).kind == TokenKind.NUMBER
                        && i + 2 == boundary))) {
              // "Genus sp. JYr4" / "Genus sp. JGP0404" — strain code immediately after
              // the marker with nothing else following. Capture as informal phrase
              // (species stays indet). Allow WORD or WORD+NUMBER (when the tokenizer
              // splits a mixed letters-and-digits code like "JGP" + "0404").
              StringBuilder code = new StringBuilder(ts.get(i).text);
              i++;
              if (i < boundary && ts.get(i).kind == TokenKind.NUMBER) {
                code.append(ts.get(i).text);
                i++;
              }
              if (isStrainCode(code.toString())) {
                ctx.name.setPhrase(code.toString());
              }
            }
            continue;
          }
          // 2b. "sp." between species and infraspecific epithet is almost always a
          // misspelling of "ssp." (subspecies). Only triggers when there's already a
          // species epithet and a lower epithet follows.
          if (w.equalsIgnoreCase("sp") && lowerEpithets.size() == 1
              && markerIdxInEpithets < 0
              && hasInfraspecificEpithetAfter(ts, i, boundary)) {
            inlineRank = Rank.SUBSPECIES;
            markerIdxInEpithets = lowerEpithets.size();
            i++;
            if (i < boundary && ts.get(i).kind == TokenKind.DOT) i++;
            continue;
          }
          // 3. infraspecific rank marker (with notho-prefix support)
          boolean[] notho = new boolean[1];
          Rank rmInfra = RankMarkers.matchInfraspecificAllowNotho(w, notho);
          if (rmInfra != null) {
            if (hasInfraspecificEpithetAfter(ts, i, boundary)) {
              ctx.explicitInfraMarker = true;
              // Second marker overriding the first: the previous classification
              // (oldRank.epithet + author) was an intermediate level the model can't
              // hold, so warn about the drop.
              if (inlineRank != null && markerIdxInEpithets >= 0
                  && markerIdxInEpithets < lowerEpithets.size()) {
                String oldMarker = inlineRank.getMarker();
                String oldEpithet = lowerEpithets.get(markerIdxInEpithets);
                StringBuilder sb = new StringBuilder(Warnings.REMOVED_PREFIX);
                if (oldMarker != null) sb.append(oldMarker).append(' ');
                sb.append(oldEpithet);
                if (pendingMidNameAuthor != null && !pendingMidNameAuthor.isEmpty()) {
                  sb.append(' ').append(pendingMidNameAuthor);
                }
                ctx.name.addWarning(sb.toString());
                ctx.name.addWarning(Warnings.QUADRINOMIAL);
              }
              inlineRank = rmInfra;
              inlineRankNotho = notho[0];
              markerIdxInEpithets = lowerEpithets.size();
              pendingMidNameAuthor = null;
              i++;
              if (i < boundary && ts.get(i).kind == TokenKind.DOT) i++;
              // microbial "f. sp." → forma specialis: skip the "sp" + dot too
              if (i + 1 < boundary && ts.get(i).kind == TokenKind.WORD
                  && ts.get(i).text.equalsIgnoreCase("sp")) {
                inlineRank = Rank.FORMA_SPECIALIS;
                i++;
                if (i < boundary && ts.get(i).kind == TokenKind.DOT) i++;
              }
              // Informal infra epithet: a single uppercase letter / digit immediately
              // following the rank marker ("form A", "f. B") — consume it here so the
              // normal lowercase-epithet path doesn't drop it as an "upper word".
              if (i < boundary && ts.get(i).kind == TokenKind.WORD
                  && ts.get(i).text.length() == 1 && ts.get(i).startsUpper()) {
                lowerEpithets.add(ts.get(i).text);
                indet = true; // INFORMAL informal infra epithet
                i++;
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
          // 4. infrageneric marker (with optional "notho-" prefix)
          boolean[] gnotho = new boolean[1];
          Rank rmInfragen = RankMarkers.matchInfragenericAllowNotho(w, gnotho);
          if (rmInfragen != null && lowerEpithets.isEmpty() && subgenus == null && infragenEpithet == null) {
            i++;
            if (i < boundary && ts.get(i).kind == TokenKind.DOT) i++;
            if (i < boundary && ts.get(i).kind == TokenKind.WORD && ts.get(i).startsUpper()) {
              infragenEpithet = ts.get(i).text;
              infrageneric = rmInfragen;
              if (gnotho[0]) {
                ctx.name.addNotho(NamePart.INFRAGENERIC);
              }
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
    } else if (subgenus != null && !cultivarSet
        && specific == null && infraspecific == null
        && (ctx.name.getRank() == null || ctx.name.getRank() == Rank.UNRANKED)) {
      // Paren-based subgenus ("Calathus (Lindrothius)") without an explicit rank
      // marker — default to the generic infrageneric rank.
      ctx.name.setRank(Rank.INFRAGENERIC_NAME);
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

  /**
   * Renders the WORD/DOT tokens in [from, to) into the canonical inline author form
   * ("B. Boivin" → "B.Boivin"). Used to record the dropped author span when an
   * intermediate classification is overridden by a later rank marker.
   */
  private static String renderAuthorSpan(List<Token> ts, int from, int to) {
    StringBuilder sb = new StringBuilder();
    for (int j = from; j < to; j++) {
      Token t = ts.get(j);
      if (t.kind == TokenKind.WORD) {
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '.') sb.append(' ');
        sb.append(t.text);
      } else if (t.kind == TokenKind.DOT) {
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '.') sb.append('.');
      }
    }
    return sb.toString();
  }

  /**
   * If a "(...) Author" span sits between the species epithet and an infraspecific rank
   * marker, returns the index of the rank-marker token. Otherwise returns -1.
   */
  private static int skipParenAuthorBlock(List<Token> ts, int openIdx, int boundary) {
    int depth = 1;
    int j = openIdx + 1;
    while (j < boundary && depth > 0) {
      TokenKind k = ts.get(j).kind;
      if (k == TokenKind.OPEN_PAREN) depth++;
      else if (k == TokenKind.CLOSE_PAREN) depth--;
      if (depth == 0) break;
      j++;
    }
    if (j >= boundary || depth != 0) return -1;
    j++; // skip past the close paren
    while (j < boundary) {
      Token t = ts.get(j);
      if (t.kind == TokenKind.WORD) {
        if (t.startsUpper()) { j++; continue; }
        if (AuthorParticles.isParticle(t.text)) { j++; continue; }
        String w = stripDot(t.text);
        boolean[] notho = new boolean[1];
        if (RankMarkers.matchInfraspecificAllowNotho(w, notho) != null) {
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

  /**
   * True if the marker at {@code markerIdx} is followed by a recognisable infraspecific
   * epithet — either a lowercase word, or a single uppercase letter / digit (informal
   * collector tag like "f. A" / "form A").
   */
  private static boolean hasInfraspecificEpithetAfter(List<Token> ts, int markerIdx, int boundary) {
    int k = markerIdx + 1;
    if (k < boundary && ts.get(k).kind == TokenKind.DOT) k++;
    // tolerate an interleaving hybrid mark before the epithet ("var. ×alpina")
    if (k < boundary && ts.get(k).kind == TokenKind.HYBRID_MARK) k++;
    if (k >= boundary) return false;
    Token nx = ts.get(k);
    if (nx.kind == TokenKind.WORD && nx.startsLower()) return true;
    if (nx.kind == TokenKind.WORD && nx.text.length() == 1 && nx.startsUpper()) return true;
    return false;
  }

  /**
   * True for strain-code-shaped tokens — mixed letters and digits, no spaces, length
   * ≥ 3 (e.g. "JGP0404", "ANIC_3", "Sb1"). Used to glue the code onto an "sp." indet
   * marker so the species epithet rendering keeps the strain identifier.
   */
  private static boolean isStrainCode(String s) {
    if (s.length() < 3) return false;
    boolean hasLetter = false;
    boolean hasDigit = false;
    for (int i = 0; i < s.length(); ) {
      int cp = s.codePointAt(i);
      if (Character.isLetter(cp)) hasLetter = true;
      else if (Character.isDigit(cp)) hasDigit = true;
      i += Character.charCount(cp);
    }
    return hasLetter && hasDigit;
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
