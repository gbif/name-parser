package org.gbif.nameparser.cli.io;

import life.catalogue.coldp.ColdpTerm;
import org.gbif.nameparser.api.NomCode;
import org.gbif.nameparser.api.Rank;
import org.gbif.nameparser.util.RankUtils;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Streaming reader for ColDP Name files in TSV or CSV form. Only the five columns
 * the parser interface accepts are honoured:
 * <ul>
 *   <li>{@link ColdpTerm#ID} — record identifier (carried through to output)</li>
 *   <li>{@link ColdpTerm#scientificName}</li>
 *   <li>{@link ColdpTerm#authorship}</li>
 *   <li>{@link ColdpTerm#rank}</li>
 *   <li>{@link ColdpTerm#code}</li>
 * </ul>
 * Other columns in the file are read but ignored — keeping the reader tolerant to
 * full ColDP exports without requiring a strict column subset.
 */
final class ColdpReader implements NameInputReader {
  private final BufferedReader reader;
  private final char delimiter;
  private final InputFormat format;
  private final int idCol;
  private final int nameCol;
  private final int authorshipCol;
  private final int rankCol;
  private final int codeCol;
  private long lineNumber;

  /**
   * @param reader        stream positioned just after the header line.
   * @param delimiter     {@code '\t'} or {@code ','}.
   * @param header        the already-consumed header line.
   * @param headerLine    1-based line number of {@code header}.
   */
  ColdpReader(BufferedReader reader, char delimiter, String header, long headerLine) throws IOException {
    this.reader = reader;
    this.delimiter = delimiter;
    this.format = delimiter == '\t' ? InputFormat.COLDP_TSV : InputFormat.COLDP_CSV;
    this.lineNumber = headerLine;

    if (header == null) {
      throw new IOException("ColDP input is empty");
    }
    String[] cols = Csv.split(header, delimiter);
    int id = -1, name = -1, auth = -1, rank = -1, code = -1;
    for (int i = 0; i < cols.length; i++) {
      String h = cols[i] == null ? "" : cols[i].trim();
      if (h.isEmpty()) continue;
      ColdpTerm term = ColdpTerm.find(h, false);
      if (term == null) continue;
      switch (term) {
        case ID:             id = i; break;
        case scientificName: name = i; break;
        case authorship:     auth = i; break;
        case rank:           rank = i; break;
        case code:           code = i; break;
        default: /* ignore */
      }
    }
    if (name < 0) {
      throw new IOException("ColDP input is missing the required scientificName column");
    }
    this.idCol = id;
    this.nameCol = name;
    this.authorshipCol = auth;
    this.rankCol = rank;
    this.codeCol = code;
  }

  @Override
  public NameInput next() throws IOException {
    String raw;
    while ((raw = reader.readLine()) != null) {
      lineNumber++;
      if (raw.isEmpty() || raw.startsWith("#")) continue;
      String[] cols = Csv.split(raw, delimiter);
      String name = trimToNull(get(cols, nameCol));
      if (name == null) continue;
      return new NameInput(
          lineNumber,
          trimToNull(get(cols, idCol)),
          name,
          trimToNull(get(cols, authorshipCol)),
          parseRank(trimToNull(get(cols, rankCol))),
          parseCode(trimToNull(get(cols, codeCol)))
      );
    }
    return null;
  }

  @Override
  public InputFormat format() {
    return format;
  }

  @Override
  public void close() throws IOException {
    reader.close();
  }

  private static String get(String[] cols, int idx) {
    return idx < 0 || idx >= cols.length ? null : cols[idx];
  }

  private static String trimToNull(String s) {
    if (s == null) return null;
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }

  static Rank parseRank(String raw) {
    if (raw == null) return null;
    Rank r = RankUtils.inferRank(raw);
    if (r != null) return r;
    try {
      return Rank.valueOf(raw.toUpperCase().replace(' ', '_'));
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  static NomCode parseCode(String raw) {
    if (raw == null) return null;
    String s = raw.trim().toUpperCase();
    if (s.isEmpty()) return null;
    try {
      return NomCode.valueOf(s);
    } catch (IllegalArgumentException ignored) {
      switch (s) {
        case "ICN":
        case "ICBN":
        case "ICNAFP":
        case "BOTANY":
          return NomCode.BOTANICAL;
        case "ICZN":
        case "ZOOLOGY":
          return NomCode.ZOOLOGICAL;
        case "ICNP":
        case "ICNB":
        case "BACTERIOLOGICAL":
        case "BACTERIOLOGY":
          return NomCode.BACTERIAL;
        case "ICVCN":
        case "ICTV":
        case "VIROLOGY":
          return NomCode.VIRUS;
        case "ICNCP":
        case "CULTIVAR":
          return NomCode.CULTIVARS;
        case "ICPN":
        case "PHYTO":
        case "PHYTOSOCIOLOGICAL":
          return NomCode.PHYTO;
        case "PHYLO":
        case "PHYLOCODE":
          return NomCode.PHYLO;
        default:
          return null;
      }
    }
  }
}
