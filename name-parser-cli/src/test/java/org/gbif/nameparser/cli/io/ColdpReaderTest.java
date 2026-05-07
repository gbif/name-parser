package org.gbif.nameparser.cli.io;

import org.gbif.nameparser.api.NomCode;
import org.gbif.nameparser.api.Rank;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ColdpReaderTest {

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  @Test
  public void parseRankAccepts() {
    assertEquals(Rank.SPECIES, ColdpReader.parseRank("species"));
    assertEquals(Rank.GENUS, ColdpReader.parseRank("Genus"));
    assertEquals(Rank.SUBSPECIES, ColdpReader.parseRank("subspecies"));
    assertNull(ColdpReader.parseRank(null));
    assertNull(ColdpReader.parseRank("not-a-rank"));
  }

  @Test
  public void parseCodeAcceptsAcronyms() {
    assertEquals(NomCode.BOTANICAL, ColdpReader.parseCode("botanical"));
    assertEquals(NomCode.BOTANICAL, ColdpReader.parseCode("ICN"));
    assertEquals(NomCode.ZOOLOGICAL, ColdpReader.parseCode("ICZN"));
    assertEquals(NomCode.BACTERIAL, ColdpReader.parseCode("ICNP"));
    assertEquals(NomCode.VIRUS, ColdpReader.parseCode("ICVCN"));
    assertNull(ColdpReader.parseCode(null));
    assertNull(ColdpReader.parseCode("totally-unknown"));
  }

  @Test
  public void readsTsvAndIgnoresUnknownColumns() throws IOException {
    Path f = tmp.newFile("names.tsv").toPath();
    Files.writeString(f, String.join("\n",
        "ID\tscientificName\tauthorship\trank\tcode\tparentID",
        "1\tFelis catus\tLinnaeus, 1758\tspecies\tICZN\troot",
        "2\tQuercus robur\tL.\tspecies\tbotanical\troot",
        ""), StandardCharsets.UTF_8);

    try (NameInputReader r = NameInputReader.open(f)) {
      assertEquals(InputFormat.COLDP_TSV, r.format());
      NameInput a = r.next();
      assertEquals("1", a.id());
      assertEquals("Felis catus", a.name());
      assertEquals("Linnaeus, 1758", a.authorship());
      assertEquals(Rank.SPECIES, a.rank());
      assertEquals(NomCode.ZOOLOGICAL, a.code());

      NameInput b = r.next();
      assertEquals("2", b.id());
      assertEquals(NomCode.BOTANICAL, b.code());

      assertNull(r.next());
    }
  }

  @Test
  public void plainTextFallback() throws IOException {
    Path f = tmp.newFile("plain.txt").toPath();
    Files.writeString(f, "Felis catus\nQuercus robur\n", StandardCharsets.UTF_8);

    try (NameInputReader r = NameInputReader.open(f)) {
      assertEquals(InputFormat.PLAIN, r.format());
      assertEquals("Felis catus", r.next().name());
      assertEquals("Quercus robur", r.next().name());
      assertNull(r.next());
    }
  }
}
