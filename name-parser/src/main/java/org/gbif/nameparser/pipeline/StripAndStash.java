package org.gbif.nameparser.pipeline;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pre-tokenisation stripper. Removes annotations from the working string and
 * stashes them onto the {@link ParseContext}/{@link org.gbif.nameparser.api.ParsedName}.
 *
 * <p>Order matters — markers are stripped from the most specific to the most
 * general so that, for instance, "[sic, porphyria]" doesn't leak through the
 * plain "[sic]" path.
 */
public final class StripAndStash {

  // [sic], (sic), [sic!] (with no comma inside)
  private static final Pattern SIC =
      Pattern.compile("\\s*[\\(\\[]\\s*sic\\s*!?\\s*[\\)\\]]");
  // [sic, ...] / (sic, ...) — keep the inner text in the unparsed remainder.
  private static final Pattern SIC_WITH_COMMENT =
      Pattern.compile("\\s*[\\(\\[]\\s*sic\\s*,([^)\\]]+)[\\)\\]]");
  private static final Pattern CORRIG =
      Pattern.compile("(?<=\\s)corrig\\.?(?=\\s|$)");
  private static final Pattern EXTINCT_PREFIX =
      Pattern.compile("^[†✝]\\s*");
  private static final Pattern EXTINCT_INLINE =
      Pattern.compile("\\s*[†✝]\\s*");

  // ---- Nomenclatural notes ----
  // Anchors on a nom/comb/orth/spec keyword and captures from there to end of string.
  // Stops before " non " / " nec " (those are taxonomic-note tails).
  private static final Pattern NOM_NOTE = Pattern.compile(
      "\\s*,?\\s+(" +
          "(?:nom|comb|orth)\\b\\.?(?:\\s+[a-zA-Z][a-zA-Z.]*)*" +
          "|spec\\b\\.?\\s*nov\\b\\.?" +
          ")\\s*(?=$|,\\s*non(?:n\\.?)?\\b|,\\s*nec\\b)",
      Pattern.CASE_INSENSITIVE);

  // Bracketed / parenthesised nomenclatural annotation at end:
  // "[nom. et typ. cons.]" / "(nom. nud.)" / "[orth. error]"
  private static final Pattern BRACKETED_NOM_NOTE = Pattern.compile(
      "\\s*[\\[\\(]\\s*((?:nom|comb|orth|typ)\\b[^\\]\\)]*)[\\]\\)]\\s*$",
      Pattern.CASE_INSENSITIVE);

  // ---- Taxonomic notes ----
  // Once an anchor (auct., sensu, sec., s.l., s.str., …) is hit, the note runs to end of string.
  private static final Pattern TAX_NOTE = Pattern.compile(
      "\\s+,?\\s*(auct\\.?(?:\\s+non(?:n\\.?)?)?(?:\\s.*)?" +
          "|sensu(?:\\s.*)?" +
          "|sec\\.?(?:\\s.*)?" +
          "|nec\\b(?:\\s.*)?" +
          "|nonn?\\.?\\s+\\p{Lu}.*" +
          "|s\\.\\s*l\\.?|s\\.\\s*str\\.?|s\\.\\s*lat\\.?|s\\.\\s*ampl\\.?" +
          ")$",
      Pattern.CASE_INSENSITIVE);

  // ---- Aggregate markers (suffix forms) ----
  private static final Pattern AGGREGATE = Pattern.compile(
      "(?:\\s+(?:agg\\.?|aggregate|species\\s+group|group|complex)" +
          "|\\s*-\\s*group|\\s*-\\s*aggregate)\\s*$",
      Pattern.CASE_INSENSITIVE);

  // ---- Published-in / nomenclatural reference ----
  private static final Pattern IN_PRESS = Pattern.compile(
      "\\s+in\\s+press\\b\\.?", Pattern.CASE_INSENSITIVE);
  // " in <Author>" tail — e.g. "Busk in Chimonides, 1987".
  private static final Pattern IN_AUTHOR = Pattern.compile(
      "\\s+in\\s+([\\p{Lu}][^\\s].*)$");

  private StripAndStash() {}

  static void run(ParseContext ctx) {
    String s = ctx.working;
    // Normalise unicode hyphens / apostrophes to their ASCII counterparts so that
    // downstream tokenisation and canonical output use a consistent character.
    s = s.replace('‐', '-')
         .replace('‑', '-')
         .replace('‒', '-')
         .replace('–', '-')
         .replace('—', '-')
         .replace('‘', '\'')
         .replace('’', '\'');
    // Replace known homoglyphs (Latin look-alikes from other scripts) with their
    // canonical Latin counterpart. Emit a HOMOGLYHPS warning when anything actually
    // changed. Hyphen homoglyphs are intentionally excluded — those are normalised
    // above already.
    if (org.gbif.nameparser.util.UnicodeUtils.containsHomoglyphs(s)) {
      String repl = org.gbif.nameparser.util.UnicodeUtils.replaceHomoglyphs(s, false);
      if (!repl.equals(s)) {
        ctx.name.addWarning(org.gbif.nameparser.api.Warnings.HOMOGLYHPS);
        s = repl;
      }
    }
    // Win-1252 → UTF-8 transcription artefacts that the homoglyph table doesn't cover
    // (e.g. "Plesn¡k" should read as "Plesnik"). Map a small set of high-bit punctuation
    // characters to their Latin look-alikes when they sit between letters.
    if (s.indexOf('¡') >= 0 || s.indexOf('¢') >= 0 || s.indexOf('£') >= 0
        || s.indexOf('‚') >= 0 || s.indexOf('„') >= 0 || s.indexOf('‰') >= 0) {
      String before = s;
      s = s.replace('¡', 'i')
           .replace('¢', 'c')
           .replace('£', 'L')
           .replace('‚', 'e')
           .replace('„', 'a')
           .replace('‰', 'e');
      if (!s.equals(before)) {
        ctx.name.addWarning(org.gbif.nameparser.api.Warnings.HOMOGLYHPS);
      }
    }

    // Candidatus prefix — quoted "Candidatus …" or bare "Candidatus …" / "Ca. …".
    {
      java.util.regex.Matcher cm = java.util.regex.Pattern
          .compile("^[\"']?(?:Candidatus|Ca\\.)\\s+", java.util.regex.Pattern.CASE_INSENSITIVE)
          .matcher(s);
      if (cm.find()) {
        ctx.name.setCandidatus(true);
        ctx.name.setCode(org.gbif.nameparser.api.NomCode.BACTERIAL);
        s = s.substring(cm.end());
        if (s.endsWith("\"") || s.endsWith("'")) s = s.substring(0, s.length() - 1);
      }
    }

    // Quoted cultivar epithet at end: " 'Name'" or " \"Name\"" → cultivarEpithet (quoted)
    {
      java.util.regex.Matcher cm = java.util.regex.Pattern
          .compile("\\s+(?:cv\\.?\\s+)?(['\"])([^'\"]+)\\1\\s*$").matcher(s);
      if (cm.find()) {
        ctx.name.setCultivarEpithet(cm.group(2).trim());
        ctx.name.setCode(org.gbif.nameparser.api.NomCode.CULTIVARS);
        ctx.name.setRank(org.gbif.nameparser.api.Rank.CULTIVAR);
        s = s.substring(0, cm.start()).trim();
        // strip a trailing " cv." marker if it survived
        s = s.replaceAll("\\s+cv\\.?\\s*$", "").trim();
      }
    }

    // Extinct dagger(s) anywhere — strip all occurrences
    if (s.indexOf('†') >= 0 || s.indexOf('✝') >= 0) {
      ctx.name.setExtinct(true);
      s = s.replaceAll("[†✝]", " ").replaceAll("\\s+", " ").trim();
    }
    Matcher m = null;

    // [sic, comment] first
    m = SIC_WITH_COMMENT.matcher(s);
    if (m.find()) {
      ctx.name.setOriginalSpelling(Boolean.TRUE);
      String inner = m.group(1).trim();
      // Park the parenthetical comment as unparsed; canonical drops it.
      ctx.pendingUnparsed = "(sic," + inner.replaceAll("\\s+", "") + ")";
      s = m.replaceFirst("");
    }
    m = SIC.matcher(s);
    if (m.find()) {
      ctx.name.setOriginalSpelling(Boolean.TRUE);
      s = m.replaceFirst("");
    }
    m = CORRIG.matcher(" " + s);
    if (m.find()) {
      ctx.name.setOriginalSpelling(Boolean.FALSE);
      // remove "corrig." token from working string
      s = s.replaceAll("(?<=\\s)corrig\\.?(?=\\s|$)", "").replaceAll("\\s+", " ").trim();
    }

    // Bracketed nom notes at end — e.g. "[nom. et typ. cons.]" / "[orth. error]" / "(nom. nud.)"
    m = BRACKETED_NOM_NOTE.matcher(s);
    if (m.find()) {
      String raw = m.group(1).trim();
      String norm = raw.replaceAll("\\s+", " ")
                       .replaceAll("\\s*\\.\\s*", ".")
                       .replaceAll("\\bet\\b", "&")
                       .replaceAll("\\s+", "")
                       .trim();
      ctx.name.setNomenclaturalNote(norm);
      s = s.substring(0, m.start()).trim();
      if (s.endsWith(",")) s = s.substring(0, s.length() - 1).trim();
    }

    // Nomenclatural notes — anchored after a nom/comb/orth keyword. May appear in the
    // middle of the string when followed by a comma + non/nec/end, so splice rather
    // than truncate.
    m = NOM_NOTE.matcher(s);
    if (m.find()) {
      String raw = m.group(1).trim();
      String norm = raw.replaceAll("\\s+", " ")
                       .replaceAll("\\s*\\.\\s*", ".")
                       .trim();
      String existing = ctx.name.getNomenclaturalNote();
      ctx.name.setNomenclaturalNote(existing == null ? norm : existing + " " + norm);
      s = (s.substring(0, m.start()) + s.substring(m.end())).trim();
      while (s.endsWith(",")) s = s.substring(0, s.length() - 1).trim();
    }

    // Taxonomic notes (anchor at end). Collapse the dotted-initial-no-space convention
    // ("A. lancea" → "A.lancea") inside the captured note.
    m = TAX_NOTE.matcher(s);
    if (m.find()) {
      String raw = m.group(1).trim();
      if (!raw.isEmpty()) {
        // Apply dotted-initial-no-space convention to abbreviated authors, but keep the
        // space before short keywords like "non", "nec".
        String norm = raw.replaceAll("\\.\\s+([\\p{Ll}][\\p{Ll}]{3,})", ".$1");
        ctx.name.setTaxonomicNote(norm);
        s = s.substring(0, m.start()).trim();
        while (s.endsWith(",")) s = s.substring(0, s.length() - 1).trim();
      }
    }

    // Aggregate suffix → rank promoted to SPECIES_AGGREGATE.
    m = AGGREGATE.matcher(s);
    if (m.find()) {
      ctx.aggregate = true;
      s = s.substring(0, m.start()).trim();
    }

    // " in press" → manuscript + nomenclaturalNote.
    m = IN_PRESS.matcher(s);
    if (m.find()) {
      ctx.name.setManuscript(true);
      String existing = ctx.name.getNomenclaturalNote();
      ctx.name.setNomenclaturalNote(existing == null ? "in press" : existing + " in press");
      s = m.replaceFirst("");
    }

    // " in <Author>" trailing tail
    m = IN_AUTHOR.matcher(s);
    if (m.find()) {
      String ref = m.group(1).trim();
      if (ref.length() >= 2) {
        String existing = ctx.name.getPublishedIn();
        ctx.name.setPublishedIn(existing == null ? ref : existing + " " + ref);
        // pull a trailing 3-4 digit year out of the captured ref so we can put it on
        // the combination authorship later.
        java.util.regex.Matcher ym = java.util.regex.Pattern
            .compile(",?\\s*(\\d{3,4})\\s*\\.?\\s*$").matcher(ref);
        if (ym.find()) {
          ctx.pendingYear = ym.group(1);
        }
        s = s.substring(0, m.start()).trim();
      }
    }

    // Manuscript marker "ined." / "ms." / "msc." / "unpublished" at end → manuscript flag.
    // Runs AFTER in-author so a trailing "Busk ms in Chimonides, 1987" cleanly strips both.
    {
      java.util.regex.Matcher mm = java.util.regex.Pattern
          .compile("\\s*,?\\s+(ined|ms|msc|unpublished)\\.?\\s*$",
              java.util.regex.Pattern.CASE_INSENSITIVE)
          .matcher(s);
      if (mm.find()) {
        ctx.name.setManuscript(true);
        String tag = mm.group(1).toLowerCase();
        if (!"unpublished".equals(tag)) {
          String existing = ctx.name.getNomenclaturalNote();
          ctx.name.setNomenclaturalNote(existing == null ? tag : existing + " " + tag);
        }
        s = s.substring(0, mm.start()).trim();
      }
    }

    ctx.working = s;
  }
}
