package org.gbif.nameparser.pipeline;

import org.gbif.nameparser.api.NomCode;
import org.gbif.nameparser.api.ParsedName;
import org.gbif.nameparser.api.Rank;
import org.gbif.nameparser.api.Warnings;
import org.gbif.nameparser.util.UnicodeUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pre-tokenisation stripper. Removes annotations from the working string and
 * stashes them onto the {@link ParseContext}/{@link ParsedName}.
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
          "(?:nom|comb|orth)\\b\\.?(?:[\\s.]*[a-zA-Z][a-zA-Z.]*)*" +
          "|spec\\b\\.?\\s*nov\\b\\.?" +
          "|nov\\b\\.?\\s+spec\\b\\.?" +
          "|(?:in\\s+obs\\b\\.?,?\\s*)?pro\\s+syn\\b\\.?" +
          ")\\s*(?=$|,\\s*non(?:n\\.?)?\\b|,\\s*nec\\b)",
      Pattern.CASE_INSENSITIVE);

  // Bracketed / parenthesised nomenclatural annotation at end:
  // "[nom. et typ. cons.]" / "(nom. nud.)" / "[orth. error]"
  private static final Pattern BRACKETED_NOM_NOTE = Pattern.compile(
      "\\s*[\\[\\(]\\s*((?:nom|comb|orth|typ)\\b[^\\]\\)]*)[\\]\\)]\\s*$",
      Pattern.CASE_INSENSITIVE);

  // ---- Taxonomic notes ----
  // Once an anchor (auct., sensu, sec., s.l., s.str., emend., fide, according to, …)
  // is hit, the note runs to end of string.
  private static final Pattern TAX_NOTE = Pattern.compile(
      "\\s+,?\\s*(auctt?\\b\\.?(?:[,.]?\\s.*)?" +
          "|sensu(?:\\s.*)?" +
          "|sec\\.?(?:\\s.*)?" +
          "|nec\\b(?:\\s.*)?" +
          "|nonn?\\.?\\s+\\(?\\p{Lu}.*" +
          "|emend\\b\\.?\\s+\\(?\\p{Lu}.*" +
          "|fide\\b\\.?\\s+\\(?\\p{Lu}.*" +
          "|according\\s+to\\s+\\p{Lu}.*" +
          "|excl\\.\\s+.*" +
          "|ss\\b\\.?\\s+.*" +
          "|s\\.\\s*l\\.?|s\\.\\s*str\\.?|s\\.\\s*lat\\.?|s\\.\\s*ampl\\.?" +
          ")$",
      Pattern.CASE_INSENSITIVE);

  // Parenthesised "(nec ..., YYYY)" / "(non ..., YYYY)" / "(not ..., YYYY)" at end —
  // homonym citation, captured as taxonomic note.
  private static final Pattern PAREN_TAX_NOTE = Pattern.compile(
      "\\s*\\(\\s*((?:nec|non|not)\\s+[^)]+)\\)\\s*\\.?\\s*$",
      Pattern.CASE_INSENSITIVE);

  // Trailing synonymy reference in square brackets: "[= Grislea L. 1753]".
  private static final Pattern SYNONYM_BRACKET = Pattern.compile(
      "\\s*\\[\\s*=\\s*[^\\]]+\\]\\s*\\.?\\s*$");

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

  /**
   * Strips inline annotations from an externally-supplied authorship string and applies
   * any flags they imply directly to the {@link ParsedName}.
   * Returns the cleaned authorship ready for tokenisation.
   */
  static String stripAuthorshipMarkers(String authorship, ParsedName name) {
    String s = authorship.trim();
    if (s.isEmpty()) return s;
    Matcher m = SIC_WITH_COMMENT.matcher(s);
    if (m.find()) {
      name.setOriginalSpelling(Boolean.TRUE);
      s = m.replaceFirst("");
    }
    m = SIC.matcher(s);
    if (m.find()) {
      name.setOriginalSpelling(Boolean.TRUE);
      s = m.replaceFirst("");
    }
    m = CORRIG.matcher(" " + s);
    if (m.find()) {
      name.setOriginalSpelling(Boolean.FALSE);
      s = s.replaceAll("(?<=\\s)corrig\\.?(?=\\s|$)", "").replaceAll("\\s+", " ").trim();
    }
    return s.trim();
  }

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
    if (UnicodeUtils.containsHomoglyphs(s)) {
      String repl = UnicodeUtils.replaceHomoglyphs(s, false);
      if (!repl.equals(s)) {
        ctx.name.addWarning(Warnings.HOMOGLYHPS);
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
        ctx.name.addWarning(Warnings.HOMOGLYHPS);
      }
    }

    // Normalize double (or more) underscores between letters to a single space
    // (e.g. "Pseudocercospora__dendrobii" → "Pseudocercospora dendrobii").
    if (s.indexOf("__") >= 0) {
      s = s.replaceAll("_{2,}", " ").trim();
    }

    // Strip trailing OTU-code identifiers (e.g. "Oxalis barrelieri XXZ_21243") — store
    // as pendingUnparsed so the name portion is still parsed normally.
    if (s.contains(" ") && ctx.pendingUnparsed == null) {
      Matcher otuM = Pattern.compile("\\s+([A-Z0-9]{3,}_\\d{3,})$").matcher(s);
      if (otuM.find()) {
        ctx.pendingUnparsed = otuM.group(1);
        s = s.substring(0, otuM.start()).trim();
      }
    }

    // Angle-bracketed authorship placeholder ("Doradidae <Unspecified Agent>") — the
    // bracketed text starts with a capital and contains spaces (i.e. it isn't an
    // HTML tag). Flag the strip with AUTHORSHIP_REMOVED + UNUSUAL_CHARACTERS warnings
    // and mark the name doubtful so callers know the authorship couldn't be parsed.
    if (s.indexOf('<') >= 0) {
      Matcher br = Pattern.compile("\\s+<\\s*(\\p{Lu}[^>]*\\s[^>]*)>\\s*$",
          Pattern.UNICODE_CHARACTER_CLASS).matcher(s);
      if (br.find()) {
        ctx.name.addWarning(Warnings.AUTHORSHIP_REMOVED);
        ctx.name.addWarning(Warnings.UNUSUAL_CHARACTERS);
        ctx.name.setDoubtful(true);
        s = s.substring(0, br.start()).trim();
      }
    }

    // Strip HTML tags and decode HTML entities (e.g. "<i>sensu</i> Author" or "&amp;").
    if (s.indexOf('<') >= 0 || s.indexOf('&') >= 0) {
      // Strip HTML-tagged taxonomic connectors entirely (tag + content), e.g. <i>sensu</i>
      s = s.replaceAll("<[^>]+>(?:sensu|auct\\.?|s\\.l\\.?|s\\.str\\.?|sec\\.?)</[^>]+>", "");
      // Strip remaining HTML tags but keep their text content
      s = s.replaceAll("<[^>]+>", "");
      // Decode basic HTML entities
      s = s.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&nbsp;", " ");
      // Clean up any extra whitespace introduced by tag removal
      s = s.replaceAll("\\s{2,}", " ").trim();
    }

    // Candidatus prefix — quoted "Candidatus …" or bare "Candidatus …" / "Ca. …".
    {
      Matcher cm = Pattern
          .compile("^[\"']?(?:Candidatus|Ca\\.)\\s+", Pattern.CASE_INSENSITIVE)
          .matcher(s);
      if (cm.find()) {
        ctx.name.setCandidatus(true);
        ctx.name.setCode(NomCode.BACTERIAL);
        s = s.substring(cm.end());
        if (s.endsWith("\"") || s.endsWith("'")) s = s.substring(0, s.length() - 1);
      }
    }

    // "cv. ex Author" — "cv." here is the horticultural placeholder for the unknown
    // gardener-author and is conventionally rendered as "hort." in canonical form.
    s = s.replaceAll("\\bcv\\.(?=\\s+ex\\s+)", "hort.");

    // Cultivar Group / grex names: "Genus [species] CapWord(s) (Group|grex|gx)" at end.
    // Capture the capitalised epithet sequence as the cultivarEpithet and pin the rank
    // accordingly. Trailing word is stripped from the working string.
    {
      Matcher gm = Pattern.compile(
          "\\s+([\\p{Lu}][\\p{L}]+(?:\\s+[\\p{Lu}][\\p{L}]+)*)\\s+(Group|grex|gx)\\s*$",
          Pattern.UNICODE_CHARACTER_CLASS).matcher(s);
      if (gm.find()) {
        ctx.name.setCultivarEpithet(gm.group(1).trim());
        ctx.name.setCode(NomCode.CULTIVARS);
        ctx.name.setRank("Group".equals(gm.group(2)) ? Rank.CULTIVAR_GROUP : Rank.GREX);
        s = s.substring(0, gm.start()).trim();
      }
    }

    // Quoted cultivar epithet: " 'Name'" / " \"Name\"" → cultivarEpithet (quoted).
    // Two positions: at end of input, OR in the middle followed by an author span.
    {
      Matcher cm = Pattern.compile("\\s+(?:cv\\.?\\s+)?(['\"])([^'\"]+)\\1\\s*$").matcher(s);
      if (cm.find()) {
        ctx.name.setCultivarEpithet(cm.group(2).trim());
        ctx.name.setCode(NomCode.CULTIVARS);
        ctx.name.setRank(Rank.CULTIVAR);
        s = s.substring(0, cm.start()).trim();
        // strip a trailing " cv." marker if it survived
        s = s.replaceAll("\\s+cv\\.?\\s*$", "").trim();
      } else {
        // Mid-string quoted epithet followed by an author span
        // ("Verpericola megasoma \"Dall\" Pils.").
        Matcher cmMid = Pattern.compile(
            "\\s+(?:cv\\.?\\s+)?(['\"])([^'\"]+)\\1(\\s+[\\p{Lu}].*)$",
            Pattern.UNICODE_CHARACTER_CLASS).matcher(s);
        if (cmMid.find()) {
          ctx.name.setCultivarEpithet(cmMid.group(2).trim());
          ctx.name.setCode(NomCode.CULTIVARS);
          ctx.name.setRank(Rank.CULTIVAR);
          s = (s.substring(0, cmMid.start()) + cmMid.group(3)).trim();
          s = s.replaceAll("\\s+cv\\.?(?=\\s|$)", "").trim();
        }
      }
    }

    // Extinct dagger(s) anywhere — strip all occurrences
    if (s.indexOf('†') >= 0 || s.indexOf('✝') >= 0) {
      ctx.name.setExtinct(true);
      s = s.replaceAll("[†✝]", " ").replaceAll("\\s+", " ").trim();
    }

    // "t.infr." infraspecific abbreviation (Hieracium "the infrasubspecific epithet"
    // notation). Strip the marker so the trailing epithet is parsed normally; the
    // resulting binomial+infra structure already maps to INFRASPECIFIC_NAME rank.
    if (s.indexOf("infr") >= 0) {
      s = s.replaceAll("\\b[tT]\\.?\\s*infr\\.?\\s+", "");
    }

    // Doubtful genus in square brackets at the start: "[Acontia] chia ..." or just "[Dexia]".
    // Strip the brackets, mark the name doubtful and emit the DOUBTFUL_GENUS warning.
    {
      Matcher dg = Pattern.compile("^\\[\\s*([\\p{Lu}][\\p{L}\\-]+)\\s*\\](\\s|$)",
              Pattern.UNICODE_CHARACTER_CLASS)
          .matcher(s);
      if (dg.find()) {
        ctx.name.setDoubtful(true);
        ctx.name.addWarning(Warnings.DOUBTFUL_GENUS);
        s = (dg.group(1) + dg.group(2) + s.substring(dg.end())).trim();
      }
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

    // Trailing synonymy reference in square brackets: "[= Grislea L. 1753]" — park as
    // unparsed first so subsequent nom-note / tax-note checks see a clean tail.
    m = SYNONYM_BRACKET.matcher(s);
    if (m.find()) {
      String tail = s.substring(m.start()).trim();
      ctx.pendingUnparsed = tail;
      ctx.name.setDoubtful(true);
      s = s.substring(0, m.start()).trim();
      while (s.endsWith(",")) s = s.substring(0, s.length() - 1).trim();
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

    // Authorship placeholders: "Not applicable", "Not given", "<Unspecified Agent>" etc.
    // Stripped silently with an AUTHORSHIP_REMOVED warning so the bare name still parses.
    {
      Matcher pm = Pattern.compile(
          "\\s+(?:Not\\s+(?:applicable|given|known|recorded)|<[^>]+>)\\s*$",
          Pattern.CASE_INSENSITIVE).matcher(s);
      if (pm.find()) {
        ctx.name.addWarning(Warnings.AUTHORSHIP_REMOVED);
        s = s.substring(0, pm.start()).trim();
      }
    }

    // ": <Author>, YYYY" trailing concept reference — botanical taxonomic-concept
    // citation form ("Vespa emarginata Linnaeus, 1758: Fabricius, 1793"). The
    // Linnaeus year is the original publication; Fabricius is the sensu author. The
    // explicit ", YYYY" requirement keeps the simpler ": SanctAuthor" sanctioning-
    // author form (e.g. "Boletus versicolor L. : Fr.") out of this strip.
    {
      Matcher pm = Pattern.compile(
          "\\s*:\\s+(\\p{Lu}[^:]*,\\s*\\d{3,4})\\s*\\.?\\s*$",
          Pattern.UNICODE_CHARACTER_CLASS).matcher(s);
      if (pm.find()) {
        String note = pm.group(1).trim();
        String existing = ctx.name.getTaxonomicNote();
        ctx.name.setTaxonomicNote(existing == null ? note : existing + " " + note);
        s = s.substring(0, pm.start()).trim();
      }
    }

    // Parenthesised "(nec/non/not …, YYYY)" homonym citation at end → taxonomic note.
    m = PAREN_TAX_NOTE.matcher(s);
    if (m.find()) {
      String note = m.group(1).trim();
      String existing = ctx.name.getTaxonomicNote();
      ctx.name.setTaxonomicNote(existing == null ? note : existing + " " + note);
      s = s.substring(0, m.start()).trim();
    }

    // Taxonomic notes (anchor at end). Apply the dotted-initial-no-space convention to
    // abbreviated authors only ("F. Schmidt" → "F.Schmidt"); keep spacing for everything
    // else, so abbreviated taxonomic keywords like "ss. auct. europ." render verbatim.
    m = TAX_NOTE.matcher(s);
    if (m.find()) {
      String raw = m.group(1).trim();
      if (!raw.isEmpty()) {
        // Only collapse when the left side of the dot is a single capital letter — a
        // genuine author initial. "ss." or "auct." (multi-letter abbreviations) must
        // keep the trailing space.
        String norm = raw.replaceAll("\\b(\\p{Lu})\\.\\s+([\\p{Ll}][\\p{Ll}]{3,})", "$1.$2");
        // Lowercase a leading "Auct." / "Auctt." — the keyword is by convention
        // rendered in lower case regardless of how it appeared in the input.
        norm = norm.replaceAll("^(Auct)", "auct").replaceAll("^(Auctt)", "auctt");
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

    // " in <Author>" trailing tail — runs first so an "Author in Source, Title (Year)"
    // tail doesn't get partially consumed by the IPNI / period-separator patterns that
    // follow. Years pulled from a publishedIn reference are publication years and are
    // marked code-neutral (pendingYearFromPublication=true) so they don't influence
    // code inference; the same year may attach to a zoological, botanical, or
    // bacteriological name.
    m = IN_AUTHOR.matcher(s);
    if (m.find()) {
      String ref = m.group(1).trim();
      if (ref.endsWith(".")) ref = ref.substring(0, ref.length() - 1).trim();
      if (ref.length() >= 2) {
        String existing = ctx.name.getPublishedIn();
        ctx.name.setPublishedIn(existing == null ? ref : existing + " " + ref);
        Matcher ym = Pattern.compile(",?\\s*(\\d{3,4})\\s*\\.?\\s*$").matcher(ref);
        if (ym.find()) {
          ctx.pendingYear = ym.group(1);
          ctx.pendingYearFromPublication = true;
        }
        Matcher pyear = Pattern.compile("\\((\\d{4})\\)").matcher(ref);
        if (pyear.find()) {
          ctx.pendingYear = pyear.group(1);
          ctx.pendingYearFromPublication = true;
        }
        s = s.substring(0, m.start()).trim();
      }
    }

    // IPNI-style citation: "Author., Title (Year)." — comma after the author then
    // a publication title ending with the year in parentheses. Pull the reference
    // (and year) out so the leading author span is what's parsed downstream.
    {
      Matcher pm = Pattern.compile(
          "(?<=\\s)[\\p{Lu}][\\p{L}.]+,\\s+(.+\\(\\d{4}\\))\\.?\\s*$",
          Pattern.UNICODE_CHARACTER_CLASS).matcher(s);
      if (pm.find()) {
        String ref = pm.group(1).trim();
        // Embedded nomNote ("in obs., pro syn.") that sits before the year parens —
        // pull it out into the nomenclaturalNote and drop from the ref text.
        Matcher nm = Pattern.compile(
            "\\s+((?:in\\s+obs\\b\\.?,?\\s*)?pro\\s+syn\\b\\.?|nom\\b\\.?(?:\\s+[a-zA-Z][a-zA-Z.]*)*"
                + "|comb\\b\\.?(?:\\s+[a-zA-Z][a-zA-Z.]*)*"
                + "|orth\\b\\.?(?:\\s+[a-zA-Z][a-zA-Z.]*)*)\\s*(?=\\(\\d{4}\\))",
            Pattern.CASE_INSENSITIVE).matcher(ref);
        if (nm.find()) {
          String note = nm.group(1).trim();
          String existing = ctx.name.getNomenclaturalNote();
          ctx.name.setNomenclaturalNote(existing == null ? note : existing + " " + note);
          ref = (ref.substring(0, nm.start()) + " " + ref.substring(nm.end()))
              .replaceAll("\\s{2,}", " ").trim();
        }
        ctx.name.setPublishedIn(ref);
        Matcher ym = Pattern.compile("\\((\\d{4})\\)\\s*\\.?\\s*$").matcher(ref);
        if (ym.find()) {
          ctx.pendingYear = ym.group(1);
          ctx.pendingYearFromPublication = true;
        }
        s = s.substring(0, pm.start(1)).trim();
        if (s.endsWith(",")) s = s.substring(0, s.length() - 1).trim();
      }
    }

    // "Surname. <Reference Title> ... <year> ..." — citation where the publication
    // reference is concatenated after the author with a period. We recognise the
    // reference by an English/Latin preposition ("Annals of the …", "Journal of
    // the …") inside it, which is rare inside author names. The leading surname must
    // be at least three letters so we don't truncate at an initial.
    {
      Matcher pm = Pattern.compile(
          "\\s+[\\p{Lu}][\\p{L}]{2,}\\.\\s+"
              + "([\\p{Lu}][\\p{L}.]+(?:\\s+(?:[\\p{Lu}][\\p{L}.]+|of|in|de|et|the|und|für))*"
              + "\\s+(?:of|in|de|et|the|und|für)\\s+.*)$",
          Pattern.UNICODE_CHARACTER_CLASS).matcher(s);
      if (pm.find()) {
        String ref = pm.group(1).trim();
        if (ref.endsWith(".")) ref = ref.substring(0, ref.length() - 1);
        ctx.name.setPublishedIn(ref);
        Matcher ym = Pattern.compile("\\b(\\d{4})\\b").matcher(ref);
        if (ym.find()) {
          ctx.pendingYear = ym.group(1);
          ctx.pendingYearFromPublication = true;
        }
        s = s.substring(0, pm.start(1)).trim();
        if (s.endsWith(".")) s = s.substring(0, s.length() - 1).trim();
      }
    }

    // Manuscript marker "ined." / "ms." / "msc." / "unpublished" at end → manuscript flag.
    // Runs AFTER in-author so a trailing "Busk ms in Chimonides, 1987" cleanly strips both.
    {
      Matcher mm = Pattern.compile("\\s*,?\\s+(ined|ms|msc|unpublished)\\.?\\s*$",
              Pattern.CASE_INSENSITIVE)
          .matcher(s);
      if (mm.find()) {
        ctx.name.setManuscript(true);
        String tag = mm.group(1).toLowerCase();
        String existing = ctx.name.getNomenclaturalNote();
        ctx.name.setNomenclaturalNote(existing == null ? tag : existing + " " + tag);
        s = s.substring(0, mm.start()).trim();
      }
    }

    ctx.working = s;
  }
}
