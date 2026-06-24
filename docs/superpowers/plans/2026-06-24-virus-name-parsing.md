# Virus Name Parsing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Parse ICTV binomial virus names as ordinary scientific names (inferring `NomCode.VIRUS` from the genus suffix), rescue the false-positive animals whose epithet collides with a viral token, support digit-bearing epithets, and remove `NameType.VIRUS` — legacy virus strings become `NameType.OTHER` + `NomCode.VIRUS`.

**Architecture:** "Virus-ness" is carried uniformly by `NomCode.VIRUS` (on parsable and unparsable results); `NameType` describes only parse quality. All virus routing stays in `Preflight`. A new `ViralSuffix` helper recognises the ICTV rank suffixes; `Preflight` sets `ParseContext.viralShape`, which `Assemble` turns into `NomCode.VIRUS`. Unparsable legacy viruses throw `UnparsableNameException(OTHER, VIRUS, name)` — the exception gains an optional `NomCode`. Digit-bearing epithets are handled in the `Tokenizer` plus small accommodations in `NameTokens`/`AuthorshipSplit`.

**Tech Stack:** Java 17, Maven multi-module (`name-parser-api`, `name-parser`, `name-parser-cli`), JUnit 4, the `NameAssertion` fluent test helper.

## Global Constraints

- Java 17 source/target. Apache 2.0 license header on every new source file (copy from any existing file in the same package).
- Regexes use Unicode classes (`\p{Lu}`, `\p{Ll}`) and must be **linear-time** — no execution timeout exists. No nested unbounded quantifiers.
- Behaviour is verified through `NameParserImplTest` (canonical) and `NameParserGnaTest` via the `NameAssertion` helper. Add a failing test before each behaviour change.
- Single class: `mvn -pl name-parser -Dtest=NameParserImplTest test`. Single method: append `#methodName`. API module: `mvn -pl name-parser-api test`. Full: `mvn install`.
- Reference design: `docs/superpowers/specs/2026-06-24-virus-name-parsing-design.md`.
- `NomCode.VIRUS` already exists. `NameType.VIRUS` is being **removed** (breaking API change — warrants a major version bump).

## Key facts about the existing code (read before starting)

- `NameType.VIRUS` is *thrown* in exactly one main-code location: `Preflight.java:181`. Everything else referencing it is tests.
- `RankUtils` already has a `NomCode.VIRUS` suffix→rank map (`RankUtils.java:246`), and `RankUtilsTest` asserts `Negarnaviricota → VIRUS, PHYLUM`. So viral higher-taxon rank inference works once `code=VIRUS` is set.
- `UnparsableNameException` (`name-parser-api/.../UnparsableNameException.java`) currently has `NameType type` + `String name`, no code.
- `Preflight.run(String original, String working)` is called once from `Pipeline.java:68`; it does NOT see the `authorship` argument.
- `ParseContext` holds `requestedCode`, `authorshipInput`, `working`, and mutable `name` (with the caller's code pre-set in the constructor).
- `Assemble.finish` sets code in the block at `Assemble.java:126-130`; suffix-based rank inference is at `Assemble.java:149-155` and deliberately uses `ctx.requestedCode` only (not inferred code).
- `Tokenizer.tokenize`: letters → `WORD` (absorbing internal hyphen/apostrophe between letters, lines 43-58); digit run → separate `NUMBER` (lines 83-91).
- `NameParserImplTest.isViralName` (line ~4150) catches `UnparsableNameException` and returns `NameType.VIRUS == e.getType()`. `assertUnparsableName` (line ~4174) asserts `assertEquals(type, ex.getType())`.
- `NameAssertion` API: `.monomial(name)`, `.monomial(name, rank)`, `.species(g, ep)`, `.species(g, infrageneric, ep)`, `.infraSpecies(g, ep, rank, infraEp)`, `.combAuthors(year, authors...)`, `.basAuthors(year, authors...)`, `.type(NameType)`, `.code(NomCode)`, `.nothingElse()`.

---

## Phase 0 — API change (foundational)

### Task 1: Add `NomCode` to `UnparsableNameException`; remove `NameType.VIRUS`

This is a mechanical, cross-module API change. Do it first; later tasks build on the new shape. After this task the **api module** compiles and its tests pass; the `name-parser` and `name-parser-cli` modules will not compile until Tasks 3 and 6 migrate their references — that is expected and acceptable mid-plan, but run the api-module tests in isolation here.

**Files:**
- Modify: `name-parser-api/src/main/java/org/gbif/nameparser/api/UnparsableNameException.java`
- Modify: `name-parser-api/src/main/java/org/gbif/nameparser/api/NameType.java`
- Modify: `name-parser-api/src/test/java/org/gbif/nameparser/api/NameTypeTest.java:14`

**Interfaces:**
- Produces: `UnparsableNameException(NameType type, NomCode code, String name)` and `UnparsableNameException(NameType type, NomCode code, String name, String message)`; `NomCode UnparsableNameException.getCode()` (null when unknown). `NameType.VIRUS` no longer exists.

- [ ] **Step 1: Write/adjust the failing test**

Edit `NameTypeTest.java` — remove the line referencing the deleted constant:
```java
    // DELETE this line (NameType.VIRUS no longer exists):
    // assertFalse(NameType.VIRUS.isParsable());
```
Add a test of the new exception field in the same test class:
```java
  @Test
  public void unparsableCarriesCode() {
    UnparsableNameException e =
        new UnparsableNameException(NameType.OTHER, NomCode.VIRUS, "Tobacco mosaic virus");
    assertEquals(NameType.OTHER, e.getType());
    assertEquals(NomCode.VIRUS, e.getCode());
    assertNull(new UnparsableNameException(NameType.OTHER, "x").getCode());
  }
```
Add imports as needed: `import org.gbif.nameparser.api.NomCode;` is same package (no import needed); ensure `assertNull` is statically imported.

- [ ] **Step 2: Run to verify it fails**

Run: `mvn -pl name-parser-api -Dtest=NameTypeTest test`
Expected: FAIL — `getCode()` / the 3-arg constructor don't exist yet.

- [ ] **Step 3a: Add the code field + constructors to `UnparsableNameException`**

```java
public class UnparsableNameException extends Exception {
  private final NameType type;
  private final NomCode code;
  private final String name;

  public UnparsableNameException(NameType type, String name, String message) {
    this(type, null, name, message);
  }

  public UnparsableNameException(NameType type, String name) {
    this(type, null, name);
  }

  public UnparsableNameException(NameType type, NomCode code, String name) {
    super("Unparsable " + type + " name: " + name);
    this.type = type;
    this.code = code;
    this.name = name;
  }

  public UnparsableNameException(NameType type, NomCode code, String name, String message) {
    super(message);
    this.type = type;
    this.code = code;
    this.name = name;
  }

  public NameType getType() {
    return type;
  }

  /** The nomenclatural code when known despite the name being unparsable (e.g. VIRUS), else null. */
  public NomCode getCode() {
    return code;
  }

  public String getName() {
    return name;
  }
  // ... keep the nested UnparsableAuthorshipException unchanged ...
}
```

- [ ] **Step 3b: Remove the `VIRUS` constant from `NameType`**

In `NameType.java`, delete the `VIRUS` enum constant and its javadoc block:
```java
  /**
   * An unparsable virus name.
   */
  VIRUS,
```
Update the `SCIENTIFIC` javadoc that mentions "(virus, hybrid, cultivar, etc.)" only if it now reads oddly — leave the word "virus" as a description of a concept; no functional change. Update the class-level note in `UnparsableNameException.java` that says "explicitly includes virus names" → "explicitly includes hybrid formulas, and carries a NomCode for code-known unparsables such as viruses."

- [ ] **Step 4: Run to verify it passes**

Run: `mvn -pl name-parser-api test`
Expected: PASS (whole api module).

- [ ] **Step 5: Commit**

```bash
git add name-parser-api/src/main/java/org/gbif/nameparser/api/UnparsableNameException.java \
        name-parser-api/src/main/java/org/gbif/nameparser/api/NameType.java \
        name-parser-api/src/test/java/org/gbif/nameparser/api/NameTypeTest.java
git commit -m "Remove NameType.VIRUS; carry NomCode on UnparsableNameException"
```

---

## Phase 1 — Virus binomial parsing

### Task 2: `ViralSuffix` helper

**Files:**
- Create: `name-parser/src/main/java/org/gbif/nameparser/pipeline/ViralSuffix.java`
- Test: `name-parser/src/test/java/org/gbif/nameparser/pipeline/ViralSuffixTest.java`

**Interfaces:**
- Produces: `static boolean ViralSuffix.isViral(String word)` — true when `word` ends in an ICTV viral rank suffix (genus..realm), **singular** suffixes only.

- [ ] **Step 1: Write the failing test**

```java
package org.gbif.nameparser.pipeline;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ViralSuffixTest {
  @Test
  public void viralGenusAndHigherSuffixes() {
    assertTrue(ViralSuffix.isViral("Tobamovirus"));
    assertTrue(ViralSuffix.isViral("Orthoebolavirus"));
    assertTrue(ViralSuffix.isViral("Lausannevirus"));
    assertTrue(ViralSuffix.isViral("Pospiviroid"));
    assertTrue(ViralSuffix.isViral("Colecusatellite"));
    assertTrue(ViralSuffix.isViral("Coronaviridae"));
    assertTrue(ViralSuffix.isViral("Nidovirales"));
    assertTrue(ViralSuffix.isViral("Pisuviricota"));
  }

  @Test
  public void nonViralLookAlikes() {
    assertFalse(ViralSuffix.isViral("Crassatellites")); // mollusk, plural -satellites
    assertFalse(ViralSuffix.isViral("Aspilota"));
    assertFalse(ViralSuffix.isViral("Adomaviruses"));   // plural -viruses
    assertFalse(ViralSuffix.isViral(null));
  }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `mvn -pl name-parser -Dtest=ViralSuffixTest test`
Expected: FAIL — `ViralSuffix` does not exist.

- [ ] **Step 3: Implement**

```java
package org.gbif.nameparser.pipeline;

import java.util.regex.Pattern;

/**
 * Recognises the standardized ICTV viral rank suffixes on a single word (genus,
 * monomial, or higher-taxon name). Per MSL41 every virus genus ends in one of these,
 * so the suffix alone is a reliable virus signal.
 *
 * <p>Only the <b>singular</b> canonical suffixes are matched, so Linnaean look-alikes
 * such as the mollusk genus {@code Crassatellites} ({@code -satellites}) are not
 * misread as viral.
 */
final class ViralSuffix {
  private ViralSuffix() {}

  private static final Pattern GENUS = Pattern.compile(
      "(?:virus|viroid|satellite|viriform)$", Pattern.CASE_INSENSITIVE);

  private static final Pattern HIGHER = Pattern.compile(
      "(?:viridae|viroidae|satellitidae"
      + "|virinae|viroinae|satellitinae"
      + "|virales|virineae"
      + "|viricetes|viricetidae|viricotina|viricota"
      + "|virites|virae|viria|vira)$",
      Pattern.CASE_INSENSITIVE);

  static boolean isViral(String word) {
    return word != null && (GENUS.matcher(word).find() || HIGHER.matcher(word).find());
  }
}
```

- [ ] **Step 4: Run to verify it passes**

Run: `mvn -pl name-parser -Dtest=ViralSuffixTest test`
Expected: PASS (compiles only `ViralSuffix` + its test; the rest of the module is migrated in Task 3).

> If the module fails to compile because of unmigrated `NameType.VIRUS` test references, do Task 3 next and run this test as part of Task 3's green build.

- [ ] **Step 5: Commit**

```bash
git add name-parser/src/main/java/org/gbif/nameparser/pipeline/ViralSuffix.java \
        name-parser/src/test/java/org/gbif/nameparser/pipeline/ViralSuffixTest.java
git commit -m "Add ViralSuffix helper for ICTV rank suffix detection"
```

---

### Task 3: `ParseContext.viralShape` + Preflight virus-gate rewrite + name-parser test migration

The core behaviour change plus the test migration that makes the `name-parser` module compile again after Task 1.

**Files:**
- Modify: `name-parser/src/main/java/org/gbif/nameparser/pipeline/ParseContext.java`
- Modify: `name-parser/src/main/java/org/gbif/nameparser/pipeline/Preflight.java`
- Modify: `name-parser/src/main/java/org/gbif/nameparser/pipeline/Pipeline.java:68`
- Modify: `name-parser/src/main/java/org/gbif/nameparser/pipeline/Assemble.java`
- Modify: `name-parser/src/test/java/org/gbif/nameparser/NameParserImplTest.java` (migrate `isViralName`, `assertUnparsable`; new test)

**Interfaces:**
- Consumes: `ViralSuffix.isViral` (Task 2); `UnparsableNameException(NameType, NomCode, String)` (Task 1).
- Produces: `ParseContext.viralShape` (public boolean, default false); `Preflight.run(String original, ParseContext ctx)`.

- [ ] **Step 1: Migrate the test helpers so the module compiles, then write the failing behaviour test**

In `NameParserImplTest`, change `isViralName` to the new model:
```java
  public boolean isViralName(String name) throws InterruptedException {
    try {
      parser.parse(name, null);
    } catch (UnparsableNameException e) {
      return e.getType() == NameType.OTHER && e.getCode() == NomCode.VIRUS;
    }
    return false;
  }
```
Add an `assertUnparsable` overload that also checks the code (place next to the existing overloads ~line 4166):
```java
  private void assertUnparsable(String name, NameType type, NomCode code) {
    try {
      parser.parse(name, null, Rank.UNRANKED, null);
      fail("Expected " + name + " to be unparsable");
    } catch (UnparsableNameException ex) {
      assertEquals(type, ex.getType());
      assertEquals(code, ex.getCode());
    }
  }
```
Add the new behaviour test:
```java
  @Test
  public void virusBinomialsParse() throws Exception {
    assertName("Tobamovirus tabaci", "Tobamovirus tabaci")
        .species("Tobamovirus", "tabaci").code(NomCode.VIRUS).nothingElse();
    assertName("Orthoebolavirus zairense", "Orthoebolavirus zairense")
        .species("Orthoebolavirus", "zairense").code(NomCode.VIRUS).nothingElse();
    assertName("Lausannevirus", "Lausannevirus")
        .monomial("Lausannevirus").code(NomCode.VIRUS).nothingElse();
    assertName("Coronaviridae", "Coronaviridae")
        .monomial("Coronaviridae", Rank.FAMILY).code(NomCode.VIRUS).nothingElse();
    // legacy vernacular → unparsable OTHER + code VIRUS
    assertUnparsable("Tobacco mosaic virus", NameType.OTHER, NomCode.VIRUS);
    assertUnparsable("Human papillomavirus", NameType.OTHER, NomCode.VIRUS);
    assertUnparsable("Acara virus", NameType.OTHER, NomCode.VIRUS);
  }
```

- [ ] **Step 2: Run to verify it fails**

Run: `mvn -pl name-parser -Dtest=NameParserImplTest#virusBinomialsParse test`
Expected: FAIL — virus binomials still throw; legacy names throw `VIRUS`-less / wrong type until the gate is rewritten.

- [ ] **Step 3a: Add `viralShape` to `ParseContext`** (after the `aggregate` field, ~line 26)
```java
  /**
   * Set by {@link Preflight} when the input is a clean uni/binomial whose genus (or
   * monomial) carries an ICTV viral rank suffix. {@link Assemble} turns this into
   * {@link NomCode#VIRUS} when the caller supplied no code.
   */
  public boolean viralShape;
```

- [ ] **Step 3b: Rewrite the Preflight virus gate**

Add patterns after `ZOOLOGICAL_BINOMIAL` (`Preflight.java:51`):
```java
  private static final Pattern CLEAN_BINOMIAL = Pattern.compile(
      "^\\p{Lu}\\p{Ll}+(?:\\s+\\(\\p{Lu}\\p{Ll}+\\))?\\s+\\p{Ll}[\\p{Ll}\\d\\-]*$",
      Pattern.UNICODE_CHARACTER_CLASS);
  private static final Pattern CLEAN_MONOMIAL = Pattern.compile(
      "^\\p{Lu}[\\p{Ll}\\-]+$", Pattern.UNICODE_CHARACTER_CLASS);
  private static final Pattern SOFT_WORD = Pattern.compile(
      "(?:vector|prions?|particles?|replicons?|rna)$", Pattern.CASE_INSENSITIVE);
  private static final Pattern HARD_WORD = Pattern.compile(
      "(?:virus|viroid|phages?|virion|satellite)$", Pattern.CASE_INSENSITIVE);
  private static final Pattern AUTH_ZOO = Pattern.compile(
      "^\\(?\\p{Lu}\\p{Ll}{2,}.*\\b(?:1[6-9]\\d\\d|20\\d\\d)\\b",
      Pattern.UNICODE_CHARACTER_CLASS);
```

Replace the gate at `Preflight.java:180-182` with `applyVirusGate(s, ctx, original);` and add:
```java
  private static void applyVirusGate(String s, ParseContext ctx, String original)
      throws UnparsableNameException {
    boolean clean = CLEAN_BINOMIAL.matcher(s).matches() || CLEAN_MONOMIAL.matcher(s).matches();
    org.gbif.nameparser.api.NomCode req = ctx.requestedCode;

    // Bucket A: clean uni/binomial whose genus/monomial carries a viral rank suffix.
    if (clean && ViralSuffix.isViral(firstWord(s))) {
      if (req == null || req == org.gbif.nameparser.api.NomCode.VIRUS) {
        ctx.viralShape = true;
      }
      return; // parse; a non-virus caller code is kept and wins over inference
    }
    if (!VIRUS.matcher(s).find()) {
      return; // no viral trigger at all
    }
    // Inline "Genus [(Subgenus)] epithet Author, YYYY" overrides a stray viral token.
    if (ZOOLOGICAL_BINOMIAL.matcher(s).find()) {
      return;
    }
    if (req == org.gbif.nameparser.api.NomCode.VIRUS) {
      if (clean) { ctx.viralShape = true; return; }
      throw new UnparsableNameException(NameType.OTHER, org.gbif.nameparser.api.NomCode.VIRUS, original);
    }
    if (clean && req != null) {
      return; // caller asserts a non-virus code for a clean binomial
    }
    if (clean && SOFT_WORD.matcher(lastWord(s)).find()) {
      return; // bucket B soft
    }
    if (clean && HARD_WORD.matcher(lastWord(s)).find()
        && ctx.authorshipInput != null
        && AUTH_ZOO.matcher(ctx.authorshipInput.trim()).find()) {
      return; // bucket B hard, rescued by a separately supplied zoological author+year
    }
    throw new UnparsableNameException(NameType.OTHER, org.gbif.nameparser.api.NomCode.VIRUS, original);
  }

  private static String firstWord(String s) {
    int sp = s.indexOf(' ');
    return sp < 0 ? s : s.substring(0, sp);
  }

  private static String lastWord(String s) {
    int sp = s.lastIndexOf(' ');
    return sp < 0 ? s : s.substring(sp + 1);
  }
```

Change `Preflight.run` signature (`Preflight.java:124`) from `(String original, String working)` to `(String original, ParseContext ctx)`; at the top replace `String s = working.trim();` with `String s = ctx.working.trim();`.

- [ ] **Step 3c: Update the call site** — `Pipeline.java:68`: `Preflight.run(scientificName, ctx);`

- [ ] **Step 3d: Set `VIRUS` from `viralShape` in `Assemble` + enable viral rank inference**

After the code-setting block (`Assemble.java:130`), add:
```java
    if (ctx.viralShape && n.getCode() == null) {
      n.setCode(NomCode.VIRUS);
    }
```
And let the suffix-rank block use the viral code. Change `Assemble.java:150`:
```java
      NomCode codeForInference = ctx.requestedCode;
```
to
```java
      // Viral code is inferred from a highly reliable suffix, so it is safe to drive
      // suffix-based rank inference (e.g. "Coronaviridae" -> FAMILY).
      NomCode codeForInference = ctx.requestedCode != null ? ctx.requestedCode
          : (ctx.viralShape ? n.getCode() : null);
```

- [ ] **Step 4: Run targeted test, then full suite**

Run: `mvn -pl name-parser -Dtest=NameParserImplTest#virusBinomialsParse test` → Expected: PASS.
> If `Coronaviridae` does not resolve to `Rank.FAMILY`, inspect `RankUtils.SUFFICES_RANK_MAP.get(NomCode.VIRUS)` for the `-viridae` key; if absent, relax that assertion to `.monomial("Coronaviridae")` and note rank inference as out of scope.

Run: `mvn -pl name-parser test` → Expected: only `virusesPlasmidsPrionsEtc` may still fail (Task 4). Any other failure (e.g. an `assertUnparsable(name, NameType.VIRUS)` leftover) must be migrated to `NameType.OTHER` / `NomCode.VIRUS` now.

- [ ] **Step 5: Commit**

```bash
git add name-parser/src/main/java/org/gbif/nameparser/pipeline/ParseContext.java \
        name-parser/src/main/java/org/gbif/nameparser/pipeline/Preflight.java \
        name-parser/src/main/java/org/gbif/nameparser/pipeline/Pipeline.java \
        name-parser/src/main/java/org/gbif/nameparser/pipeline/Assemble.java \
        name-parser/src/test/java/org/gbif/nameparser/NameParserImplTest.java
git commit -m "Parse ICTV virus binomials; infer VIRUS code; legacy viruses -> OTHER+VIRUS"
```

---

### Task 4: Re-curate the `viruses.txt` corpus assertion

**Files:**
- Modify: `name-parser/src/test/resources/viruses.txt`
- Modify: `name-parser/src/test/java/org/gbif/nameparser/NameParserImplTest.java` (`virusesPlasmidsPrionsEtc`)

- [ ] **Step 1: Identify the now-parsable entries**

Run: `mvn -pl name-parser -Dtest=NameParserImplTest#virusesPlasmidsPrionsEtc test`
Expected: FAIL listing the viral-suffix monomials and lowercase-epithet binomials:
```
Lausannevirus
Tunisvirus
Clecrusatellite
Milvetsatellite
Subclovsatellite
Marseillevirus marseillevirus
Senegalvirus marseillevirus
```

- [ ] **Step 2: Remove those 7 lines from `viruses.txt`** (leave everything else intact).

- [ ] **Step 3: Add positive assertions** just before `Reader reader = resourceReader("viruses.txt");` (~line 3011):
```java
    assertName("Lausannevirus", "Lausannevirus").monomial("Lausannevirus").code(NomCode.VIRUS).nothingElse();
    assertName("Clecrusatellite", "Clecrusatellite").monomial("Clecrusatellite").code(NomCode.VIRUS).nothingElse();
    assertName("Marseillevirus marseillevirus", "Marseillevirus marseillevirus")
        .species("Marseillevirus", "marseillevirus").code(NomCode.VIRUS).nothingElse();
```

- [ ] **Step 4: Run to verify it passes**

Run: `mvn -pl name-parser -Dtest=NameParserImplTest#virusesPlasmidsPrionsEtc test` → Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add name-parser/src/test/resources/viruses.txt \
        name-parser/src/test/java/org/gbif/nameparser/NameParserImplTest.java
git commit -m "Re-curate viruses.txt: ICTV binomials now parse as SCIENTIFIC+VIRUS"
```

---

### Task 5: False-positive animals (bucket B) + caller-code override

Closes `PREFLIGHT_VIRUS_FALSE_POSITIVES.md`.

**Files:**
- Test: `name-parser/src/test/java/org/gbif/nameparser/NameParserImplTest.java`

- [ ] **Step 1: Write the tests**
```java
  @Test
  public void virusFalsePositiveAnimals() throws Exception {
    assertName("Aspilota vector", "Belokobylskij, 2007", Rank.SPECIES, NomCode.ZOOLOGICAL, "Aspilota vector")
        .species("Aspilota", "vector").combAuthors("2007", "Belokobylskij").code(NomCode.ZOOLOGICAL).nothingElse();
    assertName("Euragallia prion", "Euragallia prion")
        .species("Euragallia", "prion").nothingElse();
    assertName("Cryptops (Cryptops) vector", "Chamberlin, 1939", Rank.SPECIES, NomCode.ZOOLOGICAL, "Cryptops vector")
        .species("Cryptops", "Cryptops", "vector").combAuthors("1939", "Chamberlin").code(NomCode.ZOOLOGICAL).nothingElse();
    assertName("Prion", "Prion").monomial("Prion").nothingElse();
    assertName("Exochus virus", "Gauld & Sithole, 2002", Rank.SPECIES, NomCode.ZOOLOGICAL, "Exochus virus")
        .species("Exochus", "virus").combAuthors("2002", "Gauld", "Sithole").code(NomCode.ZOOLOGICAL).nothingElse();
    assertUnparsable("Acara virus", NameType.OTHER, NomCode.VIRUS);
  }

  @Test
  public void virusCallerCodeOverride() throws Exception {
    // caller asserts a non-virus code → bucket-A name parses under that code
    assertName("Tobamovirus tabaci", NomCode.ZOOLOGICAL, "Tobamovirus tabaci")
        .species("Tobamovirus", "tabaci").code(NomCode.ZOOLOGICAL);
    // caller forces VIRUS on a legacy bare-virus binomial → unparsable OTHER + VIRUS
    assertUnparsable("Acara virus", NameType.OTHER, NomCode.VIRUS);
  }
```

- [ ] **Step 2: Run**

Run: `mvn -pl name-parser -Dtest=NameParserImplTest#virusFalsePositiveAnimals+virusCallerCodeOverride test`
Expected: PASS (regression guards for Task 3). If any FAIL, fix `applyVirusGate` — do not weaken tests.

- [ ] **Step 3: Delete the obsolete handoff note**
```bash
git rm PREFLIGHT_VIRUS_FALSE_POSITIVES.md
```

- [ ] **Step 4: Commit**
```bash
git add name-parser/src/test/java/org/gbif/nameparser/NameParserImplTest.java
git commit -m "Test virus false-positive animals + caller-code override; drop handoff note"
```

---

### Task 6: name-parser-cli migration

Make the CLI module compile and reflect the new model (capture/emit `code` on unparsable results).

**Files:**
- Modify: `name-parser-cli/src/main/java/org/gbif/nameparser/cli/ParseResult.java`
- Modify: `name-parser-cli/src/main/java/org/gbif/nameparser/cli/ParseCli.java` (wherever it builds an `Err` from a caught `UnparsableNameException`)
- Modify: `name-parser-cli/src/test/java/org/gbif/nameparser/cli/io/ColdpWriterTest.java:62,102`

**Interfaces:**
- Produces: `ParseResult.Err` gains an optional `NomCode code` field, populated from `UnparsableNameException.getCode()`.

- [ ] **Step 1: Adjust the failing test** — in `ColdpWriterTest`, change the constructor call and assertion:
```java
      bad.error = new ParseResult.Err(NameType.OTHER, NomCode.VIRUS, "boom");
      // ...
    assertEquals("OTHER", errRow.get("np:type"));
```
(Add a `np:code`/`VIRUS` assertion if the writer emits the code — see Step 3.)

- [ ] **Step 2: Run to verify it fails**

Run: `mvn -pl name-parser-cli -am -Dtest=ColdpWriterTest test`
Expected: FAIL (compile) — `Err` has no 3-arg constructor / `NameType.VIRUS` gone.

- [ ] **Step 3: Implement**

In `ParseResult.Err`, add an optional code:
```java
    public NameType type;
    public NomCode code;          // may be null
    public String message;

    public Err(NameType type, String message) { this(type, null, message); }

    public Err(NameType type, NomCode code, String message) {
      this.type = type;
      this.code = code;
      this.message = message;
    }
```
Where `ParseCli` catches `UnparsableNameException`, pass the code through:
```java
    } catch (UnparsableNameException ex) {
      result.error = new ParseResult.Err(ex.getType(), ex.getCode(), ex.getMessage());
    }
```
(Use the exact existing variable names at the catch site.)

- [ ] **Step 4: Run module tests, then full build**

Run: `mvn -pl name-parser-cli -am test` → Expected: PASS.
Run: `mvn install` → Expected: BUILD SUCCESS, all modules.

- [ ] **Step 5: Commit**
```bash
git add name-parser-cli/src/main/java/org/gbif/nameparser/cli/ParseResult.java \
        name-parser-cli/src/main/java/org/gbif/nameparser/cli/ParseCli.java \
        name-parser-cli/src/test/java/org/gbif/nameparser/cli/io/ColdpWriterTest.java
git commit -m "CLI: migrate off NameType.VIRUS; carry code on parse errors"
```

---

## Phase 2 — Digit-bearing epithets

### Task 7: Trailing / internal digits in epithets

**Files:**
- Modify: `name-parser/src/main/java/org/gbif/nameparser/token/Tokenizer.java:40-59`
- Test: `name-parser/src/test/java/org/gbif/nameparser/NameParserImplTest.java`

- [ ] **Step 1: Write the failing test**
```java
  @Test
  public void digitEpithetsTrailing() throws Exception {
    assertName("Simplexvirus humanalpha1", "Simplexvirus humanalpha1")
        .species("Simplexvirus", "humanalpha1").code(NomCode.VIRUS).nothingElse();
    assertName("Simplexvirus humanalpha2", "Simplexvirus humanalpha2")
        .species("Simplexvirus", "humanalpha2").code(NomCode.VIRUS).nothingElse();
    assertName("Lentivirus humimdef1", "Lentivirus humimdef1")
        .species("Lentivirus", "humimdef1").code(NomCode.VIRUS).nothingElse();
  }
```

- [ ] **Step 2: Run to verify it fails**

Run: `mvn -pl name-parser -Dtest=NameParserImplTest#digitEpithetsTrailing test`
Expected: FAIL — epithet returns as `humanalpha` (digit dropped).

- [ ] **Step 3: Implement the tokenizer glue** — replace the WORD continuation loop (`Tokenizer.java:40-59`):
```java
        while (i < n) {
          int c = input.codePointAt(i);
          int cl = Character.charCount(c);
          if (Character.isLetter(c) || Character.isDigit(c)) {
            i += cl;
            continue;
          }
          if ((c == '-' || c == '\'' || c == '’' || c == '_' || c == '!'
              || c == '‐' || c == '‑' || c == '‒' || c == '–' || c == '—')
              && i + cl < n) {
            int next = input.codePointAt(i + cl);
            if (Character.isLetter(next) || Character.isDigit(next)) {
              i += cl;
              continue;
            }
          }
          break;
        }
```
(Changes: `|| Character.isDigit(c)` on the first `if`; `|| Character.isDigit(next)` in the hyphen look-ahead.)

- [ ] **Step 4: Run targeted, then full suite**

Run: `mvn -pl name-parser -Dtest=NameParserImplTest#digitEpithetsTrailing test` → PASS.
Run: `mvn -pl name-parser test` → PASS. If a strain-code/`sp.`-phrase test changes (`NameTokens.java:265-283` already re-globs WORD+NUMBER, so a single glued WORD is fine), fix the implementation, not unrelated tests.

- [ ] **Step 5: Commit**
```bash
git add name-parser/src/main/java/org/gbif/nameparser/token/Tokenizer.java \
        name-parser/src/test/java/org/gbif/nameparser/NameParserImplTest.java
git commit -m "Preserve trailing/internal digits in epithet tokens"
```

---

### Task 8: Leading-numeral historical zoological epithets

**Files:**
- Modify: `name-parser/src/main/java/org/gbif/nameparser/token/Tokenizer.java:83-91`
- Modify: `name-parser/src/main/java/org/gbif/nameparser/token/Token.java`
- Modify: `name-parser/src/main/java/org/gbif/nameparser/pipeline/AuthorshipSplit.java`
- Modify: `name-parser/src/main/java/org/gbif/nameparser/pipeline/NameTokens.java`
- Test: `name-parser/src/test/java/org/gbif/nameparser/NameParserImplTest.java`

- [ ] **Step 1: Write the failing test**
```java
  @Test
  public void digitEpithetsLeadingNumeral() throws Exception {
    assertName("Coccinella 11-punctata Linnaeus, 1758", "Coccinella 11-punctata")
        .species("Coccinella", "11-punctata").combAuthors("1758", "Linnaeus").code(NomCode.ZOOLOGICAL).nothingElse();
    assertName("Coccinella 2-pustulata", "Coccinella 2-pustulata")
        .species("Coccinella", "2-pustulata").nothingElse();
  }
```

- [ ] **Step 2: Run to verify it fails**

Run: `mvn -pl name-parser -Dtest=NameParserImplTest#digitEpithetsLeadingNumeral test`
Expected: FAIL — `punctata` lands in authorship; epithet missing.

- [ ] **Step 3a: Tokenizer — glue number + hyphen + letter into one WORD** (replace `Tokenizer.java:83-91`):
```java
      if (Character.isDigit(cp)) {
        int numStart = i;
        i += charLen;
        while (i < n && Character.isDigit(input.codePointAt(i))) {
          i++;
        }
        // "11-punctata" / "2-pustulata": a number glued to a hyphen + letter is the
        // leading-numeral epithet form, not a bare number.
        if (i + 1 < n && input.charAt(i) == '-' && Character.isLetter(input.codePointAt(i + 1))) {
          i++; // consume hyphen
          while (i < n) {
            int c = input.codePointAt(i);
            int cl = Character.charCount(c);
            if (Character.isLetter(c) || Character.isDigit(c)) { i += cl; continue; }
            if (c == '-' && i + cl < n) {
              int next = input.codePointAt(i + cl);
              if (Character.isLetter(next) || Character.isDigit(next)) { i += cl; continue; }
            }
            break;
          }
          out.add(new Token(TokenKind.WORD, input.substring(numStart, i), numStart, i));
          continue;
        }
        out.add(new Token(TokenKind.NUMBER, input.substring(numStart, i), numStart, i));
        continue;
      }
```

- [ ] **Step 3b: `Token` — add `startsDigitEpithet()`** (in `Token.java`, near `startsLower()`):
```java
  /** True for an alphanumeric epithet word that begins with a digit, e.g. "11-punctata". */
  public boolean startsDigitEpithet() {
    return kind == TokenKind.WORD && !text.isEmpty()
        && Character.isDigit(text.codePointAt(0))
        && text.chars().anyMatch(Character::isLetter);
  }
```

- [ ] **Step 3c: `AuthorshipSplit` — keep a digit-led epithet in the name** — after the `if (t.startsLower()) { ... }` block closes (`AuthorshipSplit.java:176`):
```java
        if (afterGenus && t.startsDigitEpithet()) {
          nameWords++;
          haveEpithet = true;
          afterSubgenus = false;
          i++;
          continue;
        }
```

- [ ] **Step 3d: `NameTokens` — classify a digit-led epithet** — just before the `if (t.startsLower()) {` block (`NameTokens.java:210`):
```java
        if (genus != null && t.startsDigitEpithet()) {
          lowerEpithets.add(t.text);
          i++;
          continue;
        }
```

- [ ] **Step 4: Run targeted, then full suite**

Run: `mvn -pl name-parser -Dtest=NameParserImplTest#digitEpithetsLeadingNumeral test` → PASS.
Run: `mvn -pl name-parser test` → PASS. Year-range tokens (`1976-1981`) stay `NUMBER` (Step 3a only absorbs hyphen+letter); verify any author year-range test still passes.

- [ ] **Step 5: Commit**
```bash
git add name-parser/src/main/java/org/gbif/nameparser/token/Tokenizer.java \
        name-parser/src/main/java/org/gbif/nameparser/token/Token.java \
        name-parser/src/main/java/org/gbif/nameparser/pipeline/AuthorshipSplit.java \
        name-parser/src/main/java/org/gbif/nameparser/pipeline/NameTokens.java \
        name-parser/src/test/java/org/gbif/nameparser/NameParserImplTest.java
git commit -m "Parse leading-numeral zoological epithets (Coccinella 11-punctata)"
```

---

## Phase 3 — Validation

### Task 9: End-to-end validation

- [ ] **Step 1: Full build + tests**

Run: `mvn install` → Expected: BUILD SUCCESS, all modules.

- [ ] **Step 2: Re-run MSL41 through the CLI (recommended)**
```bash
mvn -pl name-parser-cli -am install -DskipTests -q
JAR=name-parser-cli/target/name-parser-cli-*-shaded.jar
java -jar $JAR parse --input=<msl_species.txt> --output=- --format=jsonl --quiet 2>/dev/null \
  | python3 -c "import sys,json,collections; c=collections.Counter('err' if 'error' in json.loads(l) else 'ok' for l in sys.stdin); print(c)"
```
Expected: `ok` now dominates (≈17k of 17,554), up from 131. Remaining errors are `Genus LETTER` serotype forms (type OTHER, code VIRUS).

- [ ] **Step 3: Final commit (docs/status)**
```bash
git add -A && git commit -m "Virus name parsing: full build green"
```

---

## Self-Review notes (addressed)

- **Spec coverage:** API change (Task 1), ViralSuffix (Task 2), gate + code inference + viral rank inference (Task 3), corpus re-curation (Task 4), bucket-B + caller override (Task 5), CLI (Task 6), digit epithets both shapes (Tasks 7-8), validation (Task 9). Known limitations (lab vectors; `Necocli virus`) are accepted in the spec — no task.
- **Spec rule #4 refinement:** unified hard-epithet rescue (any viral epithet rescued by a real `Surname + year`; committee citations like `ICTV` don't match), required to keep `Turkozelotes attavirus Chatzaki, 2019` green.
- **Compile-order caveat:** Task 1 removes `NameType.VIRUS`, breaking `name-parser` (fixed in Task 3) and `name-parser-cli` (fixed in Task 6) until those tasks run. Run per-module tests as noted; `mvn install` only green after Task 6.
- **Type consistency:** `ParseContext.viralShape`, `ViralSuffix.isViral`, `UnparsableNameException(NameType, NomCode, String)` / `getCode()`, `ParseResult.Err(NameType, NomCode, String)`, `Token.startsDigitEpithet()`.
