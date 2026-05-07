package org.gbif.nameparser.cli;

import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ArgsTest {

  @Test
  public void namedAndPositional() {
    Args a = Args.parse(new String[]{
        "--input=foo.tsv", "--max-diffs=42", "--ignore-whitespace", "out.jsonl"
    });
    assertEquals(Paths.get("foo.tsv"), a.path("input", null));
    assertEquals(42, a.integer("max-diffs", 1));
    assertTrue(a.flag("ignore-whitespace"));
    assertFalse(a.flag("missing"));
    assertEquals(Paths.get("out.jsonl"), a.positionalPath(0));
  }

  @Test
  public void helpAliases() {
    assertTrue(Args.parse(new String[]{"-h"}).wantsHelp());
    assertTrue(Args.parse(new String[]{"--help"}).wantsHelp());
    assertFalse(Args.parse(new String[]{"--input=x"}).wantsHelp());
  }

  @Test
  public void defaults() {
    Args a = Args.parse(new String[]{});
    Path defPath = Paths.get("default.jsonl");
    assertEquals("hello", a.string("missing", "hello"));
    assertEquals(7, a.integer("missing", 7));
    assertEquals(defPath, a.path("missing", defPath));
    assertNull(a.positionalPath(0));
  }

  @Test
  public void flagWithoutValue() {
    Args a = Args.parse(new String[]{"--quiet"});
    assertTrue(a.flag("quiet"));
    assertEquals("true", a.string("quiet", null));
  }
}
