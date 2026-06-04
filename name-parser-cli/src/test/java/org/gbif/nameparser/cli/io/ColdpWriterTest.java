package org.gbif.nameparser.cli.io;

import org.gbif.nameparser.NameParserImpl;
import org.gbif.nameparser.api.NameParser;
import org.gbif.nameparser.api.NameType;
import org.gbif.nameparser.api.UnparsableNameException;
import org.gbif.nameparser.cli.ParseResult;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ColdpWriterTest {

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  /** Index column headers → values for one row, using tab as the delimiter. */
  private static Map<String, String> rowAsMap(String header, String row) {
    String[] hCols = Csv.split(header, '\t');
    String[] vCols = Csv.split(row, '\t');
    Map<String, String> m = new HashMap<>();
    for (int i = 0; i < hCols.length; i++) {
      m.put(hCols[i], i < vCols.length ? vCols[i] : "");
    }
    return m;
  }

  @Test
  public void mapsKnownAndCustomColumns() throws IOException {
    NameParser parser = new NameParserImpl();
    Path out = tmp.newFile("names.tsv").toPath();

    try (NameOutputWriter w = NameOutputWriter.open(out, OutputFormat.TSV)) {
      ParseResult ok = new ParseResult();
      ok.line = 1;
      ok.id = "felcat";
      ok.input = "Felis catus";
      try {
        ok.parsed = parser.parse("Felis catus Linnaeus, 1758", null, null, null);
      } catch (UnparsableNameException e) {
        throw new AssertionError(e);
      }
      w.write(ok);

      ParseResult bad = new ParseResult();
      bad.line = 2;
      bad.id = "virus1";
      bad.input = "Iridoviridae sp.";
      bad.error = new ParseResult.Err(NameType.VIRUS, "boom");
      w.write(bad);
    }

    List<String> lines = Files.readAllLines(out, StandardCharsets.UTF_8);
    assertEquals(3, lines.size());
    String header = lines.get(0);

    // Standard ColDP columns present.
    assertTrue(header, header.startsWith("ID\t"));
    assertTrue(header, header.contains("\tscientificName\t"));
    assertTrue(header, header.contains("\tnameStatus\t"));
    assertTrue(header, header.contains("\tnamePublishedInPage\t"));
    assertTrue(header, header.contains("\tnamePhrase\t"));
    assertTrue(header, header.contains("\tprovisional\t"));
    assertTrue(header, header.contains("\textinct\t"));
    // No remarks column for parser-specific overflow.
    assertTrue("remarks must not be a column", !header.contains("\tremarks"));
    // np: namespace columns present.
    assertTrue(header, header.contains("\tnp:type"));
    assertTrue(header, header.contains("\tnp:sanctioningAuthor"));
    assertTrue(header, header.contains("\tnp:taxonomicNote"));
    assertTrue(header, header.contains("\tnp:unparsed"));
    assertTrue(header, header.contains("\tnp:warnings"));
    assertTrue(header, header.endsWith("\tnp:error"));

    Map<String, String> okRow = rowAsMap(header, lines.get(1));
    assertEquals("felcat", okRow.get("ID"));
    assertEquals("Felis catus", okRow.get("scientificName"));
    assertEquals("Linnaeus, 1758", okRow.get("authorship"));
    assertEquals("species", okRow.get("rank"));
    assertEquals("zoological", okRow.get("code"));
    assertEquals("Linnaeus", okRow.get("combinationAuthorship"));
    assertEquals("1758", okRow.get("combinationAuthorshipYear"));
    assertEquals("", okRow.get("np:type"));
    assertEquals("", okRow.get("np:error"));

    Map<String, String> errRow = rowAsMap(header, lines.get(2));
    assertEquals("virus1", errRow.get("ID"));
    assertEquals("Iridoviridae sp.", errRow.get("scientificName"));
    assertEquals("VIRUS", errRow.get("np:type"));
    assertEquals("boom", errRow.get("np:error"));
    assertEquals("", errRow.get("authorship"));
    assertEquals("", errRow.get("rank"));
  }

  @Test
  public void nothoIsCommaJoined() throws IOException {
    NameParser parser = new NameParserImpl();
    Path out = tmp.newFile("notho.tsv").toPath();

    try (NameOutputWriter w = NameOutputWriter.open(out, OutputFormat.TSV)) {
      ParseResult row = new ParseResult();
      row.line = 1;
      row.id = "h1";
      row.input = "× Aegilotriticum × requienii";
      try {
        row.parsed = parser.parse(row.input, null, null, null);
      } catch (UnparsableNameException e) {
        throw new AssertionError(e);
      }
      assertNotNull(row.parsed);
      w.write(row);
    }

    List<String> lines = Files.readAllLines(out, StandardCharsets.UTF_8);
    Map<String, String> r = rowAsMap(lines.get(0), lines.get(1));
    String notho = r.get("notho");
    // The parser should mark at least the generic part; if it also marks the
    // specific part the joined value is "generic,specific".
    assertNotNull(notho);
    assertTrue("expected at least 'generic' in notho, was: " + notho,
        notho.contains("generic"));
    assertNull("notho should not contain spaces around comma: " + notho,
        notho.contains(", ") ? notho : null);
  }
}
