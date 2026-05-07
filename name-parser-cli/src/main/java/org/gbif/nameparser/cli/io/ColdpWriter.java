package org.gbif.nameparser.cli.io;

import life.catalogue.coldp.ColdpTerm;
import org.gbif.nameparser.api.Authorship;
import org.gbif.nameparser.api.NameType;
import org.gbif.nameparser.api.NamePart;
import org.gbif.nameparser.api.ParsedName;
import org.gbif.nameparser.cli.ParseResult;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Streams ParsedName rows into a flat ColDP Name file (TSV or CSV) with a fixed
 * column order. Mapping rules:
 *
 * <ul>
 *   <li>Standard ColDP columns (recognised by {@link ColdpTerm}) carry every
 *       structural ParsedName field that has a direct counterpart.</li>
 *   <li>Where the ColDP {@code Name} entity lacks a column but the {@code NameUsage}
 *       entity defines one, that NameUsage term is used —
 *       {@link ColdpTerm#nameStatus}, {@link ColdpTerm#namePhrase},
 *       {@link ColdpTerm#namePublishedInPage}, {@link ColdpTerm#provisional},
 *       {@link ColdpTerm#extinct}.</li>
 *   <li>ParsedName fields without a ColDP counterpart are written into custom
 *       columns prefixed with {@code np:} (the parser's namespace). Conformant
 *       ColDP readers ignore unknown columns, so the file remains valid ColDP.</li>
 * </ul>
 *
 * <p>{@link ColdpTerm#notho} is a comma-joined list of every flagged hybrid part
 * (e.g. {@code "generic,specific"}) — the parser model permits multiple parts
 * simultaneously even if that is rare in practice.
 *
 * <p>Unparsable rows are still written: {@code ID}, {@code scientificName} (the
 * verbatim input) and the parser-namespace columns ({@code np:type},
 * {@code np:error}) are populated.
 */
final class ColdpWriter implements NameOutputWriter {

  /** A single output column with its header and value-extracting function. */
  private record Column(String header, Function<ParseResult, String> extractor) {}

  private static final Column[] COLUMNS = buildColumns();

  private static Column[] buildColumns() {
    return new Column[]{
        col(ColdpTerm.ID,                          ColdpWriter::idValue),
        col(ColdpTerm.scientificName,              ColdpWriter::scientificName),
        col(ColdpTerm.authorship,                  pn(p -> blankToNull(p.authorshipComplete()))),
        col(ColdpTerm.rank,                        pn(p -> p.getRank() == null ? null : p.getRank().name().toLowerCase())),
        col(ColdpTerm.code,                        pn(p -> p.getCode() == null ? null : p.getCode().name().toLowerCase())),
        col(ColdpTerm.nameStatus,                  pn(ColdpWriter::derivedNameStatus)),
        col(ColdpTerm.uninomial,                   pn(ParsedName::getUninomial)),
        col(ColdpTerm.genus,                       pn(ParsedName::getGenus)),
        col(ColdpTerm.infragenericEpithet,         pn(ParsedName::getInfragenericEpithet)),
        col(ColdpTerm.specificEpithet,             pn(ParsedName::getSpecificEpithet)),
        col(ColdpTerm.infraspecificEpithet,        pn(ParsedName::getInfraspecificEpithet)),
        col(ColdpTerm.cultivarEpithet,             pn(ParsedName::getCultivarEpithet)),
        col(ColdpTerm.notho,                       pn(p -> joinNotho(p.getNotho()))),
        col(ColdpTerm.originalSpelling,            pn(p -> p.isOriginalSpelling() == null ? null : p.isOriginalSpelling().toString())),
        col(ColdpTerm.combinationAuthorship,       pn(p -> joinAuthors(authorsOf(p.getCombinationAuthorship())))),
        col(ColdpTerm.combinationExAuthorship,     pn(p -> joinAuthors(exAuthorsOf(p.getCombinationAuthorship())))),
        col(ColdpTerm.combinationAuthorshipYear,   pn(p -> yearOf(p.getCombinationAuthorship()))),
        col(ColdpTerm.basionymAuthorship,          pn(p -> joinAuthors(authorsOf(p.getBasionymAuthorship())))),
        col(ColdpTerm.basionymExAuthorship,        pn(p -> joinAuthors(exAuthorsOf(p.getBasionymAuthorship())))),
        col(ColdpTerm.basionymAuthorshipYear,      pn(p -> yearOf(p.getBasionymAuthorship()))),
        col(ColdpTerm.namePublishedInPage,         pn(ParsedName::getPublishedIn)),
        col(ColdpTerm.extinct,                     pn(p -> p.isExtinct() ? "true" : null)),
        col(ColdpTerm.namePhrase,                  pn(ParsedName::getPhrase)),
        col(ColdpTerm.provisional,                 pn(p -> p.isDoubtful() ? "true" : null)),
        // np: namespace — fields without a ColDP equivalent
        new Column("np:type",                row -> npType(row)),
        new Column("np:sanctioningAuthor",   pn(ParsedName::getSanctioningAuthor)),
        new Column("np:taxonomicNote",       pn(ParsedName::getTaxonomicNote)),
        new Column("np:unparsed",            pn(ParsedName::getUnparsed)),
        new Column("np:warnings",            pn(p -> p.getWarnings() == null || p.getWarnings().isEmpty()
            ? null : String.join("|", p.getWarnings()))),
        new Column("np:error",               row -> row.error == null ? null : row.error.message)
    };
  }

  private final PrintWriter out;
  private final char delimiter;

  ColdpWriter(PrintWriter out, char delimiter) {
    this.out = out;
    this.delimiter = delimiter;
    writeHeader();
  }

  private void writeHeader() {
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < COLUMNS.length; i++) {
      if (i > 0) b.append(delimiter);
      b.append(format(COLUMNS[i].header()));
    }
    out.println(b);
  }

  @Override
  public void write(ParseResult row) {
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < COLUMNS.length; i++) {
      if (i > 0) b.append(delimiter);
      b.append(format(COLUMNS[i].extractor().apply(row)));
    }
    out.println(b);
  }

  @Override
  public void close() throws IOException {
    out.flush();
    out.close();
  }

  private String format(String value) {
    if (value == null) return "";
    return delimiter == '\t' ? Csv.sanitiseTsv(value) : Csv.quoteCsv(value);
  }

  // -------------------- value extractors --------------------

  /** Wrap a {@link ParsedName} extractor so it returns null when the row is unparsable. */
  private static Function<ParseResult, String> pn(Function<ParsedName, String> f) {
    return row -> row.parsed == null ? null : f.apply(row.parsed);
  }

  private static Column col(ColdpTerm term, Function<ParseResult, String> extractor) {
    return new Column(term.simpleName(), extractor);
  }

  private static String idValue(ParseResult row) {
    if (row.id != null && !row.id.isBlank()) return row.id;
    return row.input != null && !row.input.isBlank() ? row.input : null;
  }

  private static String scientificName(ParseResult row) {
    if (row.parsed == null) {
      // Unparsable row — fall back to verbatim input so the file still has a name.
      return row.input;
    }
    String canon = row.parsed.canonicalNameWithoutAuthorship();
    if (canon == null || canon.isBlank()) return null;
    return row.parsed.isCandidatus() ? "Candidatus " + canon : canon;
  }

  private static String derivedNameStatus(ParsedName pn) {
    String note = blankToNull(pn.getNomenclaturalNote());
    if (note != null) return note;
    if (pn.isManuscript()) return "manuscript";
    return null;
  }

  private static String npType(ParseResult row) {
    if (row.parsed != null) {
      NameType t = row.parsed.getType();
      return t == null || t == NameType.SCIENTIFIC ? null : t.name();
    }
    return row.error == null || row.error.type == null ? null : row.error.type.name();
  }

  private static String joinNotho(Set<NamePart> notho) {
    if (notho == null || notho.isEmpty()) return null;
    StringBuilder b = new StringBuilder();
    for (NamePart p : NamePart.values()) {
      if (notho.contains(p)) {
        if (b.length() > 0) b.append(',');
        b.append(p.name().toLowerCase());
      }
    }
    return b.length() == 0 ? null : b.toString();
  }

  private static List<String> authorsOf(Authorship a) {
    return a == null ? null : a.getAuthors();
  }

  private static List<String> exAuthorsOf(Authorship a) {
    return a == null ? null : a.getExAuthors();
  }

  private static String yearOf(Authorship a) {
    return a == null ? null : a.getYear();
  }

  /**
   * Join author tokens per the ColDP convention: a single pipe character. ColDP's
   * spec prescribes {@code |} as the multi-value separator inside a single field,
   * which removes the ambiguity of comma-and-space (commas appear inside Asian
   * surname-first authorships and inside year ranges).
   */
  private static String joinAuthors(List<String> authors) {
    if (authors == null || authors.isEmpty()) return null;
    return String.join("|", authors);
  }

  private static String blankToNull(String s) {
    return s == null || s.isBlank() ? null : s;
  }
}
