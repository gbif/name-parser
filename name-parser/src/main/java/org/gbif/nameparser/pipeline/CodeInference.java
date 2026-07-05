package org.gbif.nameparser.pipeline;

import org.gbif.nameparser.api.NomCode;
import org.gbif.nameparser.api.ParsedName;
import org.gbif.nameparser.api.Rank;
import org.gbif.nameparser.api.Warnings;

import java.util.EnumSet;

/**
 * Infers a name's nomenclatural {@link NomCode} from the shape of its authorship.
 *
 * <p>Inference is a <b>vote tally</b>, not a priority cascade: each independent signal
 * casts a vote for a code, and a code is assigned only when every vote agrees.
 * Contradicting signals — or no signal at all — leave the code unset rather than guessing.
 * {@link Assemble} calls {@link #infer} once, only when the name has no code yet, and
 * {@link #applyRankCodeMismatch} unconditionally afterwards.
 */
public final class CodeInference {

  private CodeInference() {}

  /**
   * Tallies the authorship signals onto a name whose code is not yet set. Called by
   * {@link Assemble} only when {@code ctx.name.getCode() == null}.
   *
   * <p>Two decisive signals are checked first and pin the code outright:
   * <ol>
   *   <li>a <b>code-exclusive rank</b> ({@link Rank#getCode()} != null) — the cultivar,
   *       viral and bacterial {@code *-var} ranks, forma specialis, botanical section/series,
   *       etc. The generic markers subsp./var./f. carry <i>no</i> code (they are used across
   *       codes, mostly on old zoological synonyms) and so are deliberately not a signal.</li>
   *   <li>a <b>code-exclusive nomenclatural status</b> ({@link #codeFromNomNote}) — several
   *       ICN and ICZN statuses exist only under their own code.</li>
   * </ol>
   *
   * <p>Otherwise a <b>vote tally</b> over authorship shape decides:
   * <ul>
   *   <li>BOTANICAL — a sanctioning author; a {@code (Basionym) Recombination} two-author
   *       citation; a filius suffix with no year</li>
   *   <li>ZOOLOGICAL — a basionym-only {@code (Author, year)} citation; a year on an
   *       authored basionym or combination</li>
   *   <li>BACTERIAL — a {@code Candidatus} name</li>
   * </ul>
   * A single distinct vote wins; zero or contradicting votes leave the code null.
   */
  static void infer(ParseContext ctx, AuthorshipParser.AuthState authState) {
    ParsedName n = ctx.name;
    // 1. A code-exclusive rank pins the code outright — no vote needed.
    Rank r = n.getRank();
    NomCode rankCode = r == null ? null : r.getCode();
    if (rankCode != null) {
      n.setCode(rankCode);
      return;
    }

    // 2. A code-exclusive nomenclatural status pins the code. An ICN-only or ICZN-only
    // status is decisive even against a contradicting authorship year (e.g. the year on
    // "Polygala vulgaris L., 1753, nom. cons." doesn't make it zoological).
    NomCode noteCode = codeFromNomNote(n.getNomenclaturalNote());
    if (noteCode != null) {
      n.setCode(noteCode);
      return;
    }

    EnumSet<NomCode> votes = EnumSet.noneOf(NomCode.class);

    // Bacterial: a Candidatus name is a provisional prokaryote name.
    if (n.isCandidatus()) {
      votes.add(NomCode.BACTERIAL);
    }

    if (authState != null) {
      boolean basYear = authState.basionymPresent
          && authState.basionym.getYear() != null && authState.basionym.hasAuthors();
      boolean combYear = authState.combination.getYear() != null && authState.combination.hasAuthors();
      boolean anyAuthorYear = basYear || combYear;

      // --- botanical votes ---
      // Sanctioning author (": Fr." / ": Pers.").
      if (authState.sanctioningAuthor != null || n.getSanctioningAuthor() != null) {
        votes.add(NomCode.BOTANICAL);
      }
      // "(Basionym) Recombination" — a parenthesised basionym plus a recombination author.
      if (authState.basionymPresent && authState.combination.hasAuthors()) {
        votes.add(NomCode.BOTANICAL);
      }
      // Filius ("f." / "fil.") without any year.
      if (authState.hasFilius && !anyAuthorYear) {
        votes.add(NomCode.BOTANICAL);
      }

      // --- zoological votes ---
      // Basionym-only "(Author, year)" recombination with no recombination author.
      if (authState.basionymPresent && !authState.combination.hasAuthors()) {
        votes.add(NomCode.ZOOLOGICAL);
      }
      // A year on an authored basionym or combination.
      if (anyAuthorYear) {
        votes.add(NomCode.ZOOLOGICAL);
      }
    }

    if (votes.size() == 1) {
      n.setCode(votes.iterator().next());
    }
  }

  /**
   * Maps a nomenclatural status note to the code it is exclusive to, or {@code null} when
   * the status is shared between codes and therefore carries no code signal.
   *
   * <p>Botanical-only (ICN): nom. inval. / illeg. / superfl. (super.) / cons. / rej. (rejic.) /
   * ambig. / confus. Zoological-only (ICZN): nomen oblitum / nomen protectum. Shared and thus
   * neutral: nom. nud. / dub. / nov., comb. nov., stat. nov., orth., correct, transf.
   *
   * <p>The zoological free-text statuses the ICZN also defines (unavailable, incorrect original
   * spelling, justified/unjustified emendation) are not extracted into the nomenclaturalNote by
   * the strip stage today, so they can't be voted on here yet.
   */
  static NomCode codeFromNomNote(String note) {
    if (note == null) return null;
    String s = note.toLowerCase();
    if (s.contains("illeg") || s.contains("inval") || s.contains("super")
        || s.contains("cons") || s.contains("rej") || s.contains("ambig")
        || s.contains("confus")) {
      return NomCode.BOTANICAL;
    }
    if (s.contains("oblitum") || s.contains("protectum")) {
      return NomCode.ZOOLOGICAL;
    }
    return null;
  }

  /**
   * Rank-restricted code mismatch with the caller-supplied code → override the code to
   * what the rank requires and warn about it. E.g. supersect. is only valid in botany;
   * if the caller asked for ZOOLOGICAL, surface a CODE_MISMATCH and pin BOTANICAL.
   */
  static void applyRankCodeMismatch(ParseContext ctx) {
    ParsedName n = ctx.name;
    Rank rmm = n.getRank();
    NomCode pinnedRank = rmm == null ? null : rmm.getCode();
    if (pinnedRank != null && n.getCode() != null && n.getCode() != pinnedRank
        && ctx.requestedCode != null && ctx.requestedCode != pinnedRank) {
      n.setCode(pinnedRank);
      n.addWarning(Warnings.CODE_MISMATCH);
    }
  }
}
