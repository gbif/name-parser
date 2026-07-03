package org.gbif.nameparser.cli;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Exercises the select → build-batch half of {@code validate} end-to-end via
 * {@code --dry-run}, so no API call is made. Confirms barcode/OTU exclusion and
 * reproducible output.
 */
public class ValidateCliTest {

  private static final String NAMES = String.join("\n",
      "Abies alba Mill.",
      "Vulpes vulpes silaceus Miller, 1907",
      "BOLD:AAA0001",          // excluded
      "SH1957732.10FU",        // excluded
      "Puma concolor (Linnaeus, 1771)") + "\n";

  @Test
  public void dryRunSelectsAndExcludes() throws Exception {
    Path in = Files.createTempFile("validate-in", ".txt");
    Path out = Files.createTempFile("validate-out", ".jsonl");
    Files.writeString(in, NAMES);

    ValidateCli.main(new String[]{
        "--input=" + in, "--output=" + out, "--dry-run", "--cache=none"
    });

    List<String> lines = Files.readAllLines(out, StandardCharsets.UTF_8);
    // three real names selected; the two barcode/OTU codes excluded
    assertEquals(3, lines.size());
    String joined = String.join("\n", lines);
    assertTrue(joined.contains("Abies alba"));
    assertTrue(joined.contains("Vulpes vulpes"));
    assertTrue(joined.contains("Puma concolor"));
    assertFalse(joined.contains("BOLD:AAA0001"));
    assertFalse(joined.contains("SH1957732"));

    Files.deleteIfExists(in);
    Files.deleteIfExists(out);
  }

  @Test
  public void dryRunIsReproducible() throws Exception {
    Path in = Files.createTempFile("validate-in", ".txt");
    Files.writeString(in, NAMES);
    Path out1 = Files.createTempFile("validate-out1", ".jsonl");
    Path out2 = Files.createTempFile("validate-out2", ".jsonl");

    String[] argsBase = {"--input=" + in, "--dry-run", "--cache=none", "--seed=17"};
    ValidateCli.main(concat(argsBase, "--output=" + out1));
    ValidateCli.main(concat(argsBase, "--output=" + out2));

    assertEquals(Files.readString(out1), Files.readString(out2));

    Files.deleteIfExists(in);
    Files.deleteIfExists(out1);
    Files.deleteIfExists(out2);
  }

  private static String[] concat(String[] base, String extra) {
    String[] r = new String[base.length + 1];
    System.arraycopy(base, 0, r, 0, base.length);
    r[base.length] = extra;
    return r;
  }
}
