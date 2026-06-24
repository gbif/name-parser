# Virus Name Parsing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Parse ICTV binomial virus names as ordinary scientific names (inferring `NomCode.VIRUS` from the genus suffix), rescue the false-positive animals whose epithet collides with a viral token, and support digit-bearing epithets — while keeping legacy vernacular virus strings unparsable `VIRUS`.

**Architecture:** All virus-vs-not routing stays in `Preflight` (one place). A new `ViralSuffix` helper recognises the ICTV rank suffixes. `Preflight` sets a `ParseContext.viralShape` flag; `Assemble` turns that flag into `NomCode.VIRUS` when the caller supplied no code. Digit-bearing epithets are handled in the `Tokenizer` (gluing digits into epithet tokens) plus small accommodations in `NameTokens` and `AuthorshipSplit`.

**Tech Stack:** Java 17, Maven multi-module (`name-parser-api`, `name-parser`, `name-parser-cli`), JUnit 4, the `NameAssertion` fluent test helper.

## Global Constraints

- Java 17 source/target. Apache 2.0 license header on every new source file (copy from any existing file in the same package).
- Regexes use Unicode classes (`\p{Lu}`, `\p{Ll}`) and must be **linear-time** — the parser has no execution timeout. No nested unbounded quantifiers.
- Behaviour is verified through `NameParserImplTest` (canonical) and `NameParserGnaTest` using the `NameAssertion` helper. Add a failing test before each behaviour change.
- Run a single test class: `mvn -pl name-parser -Dtest=NameParserImplTest test`. Single method: `mvn -pl name-parser -Dtest=NameParserImplTest#methodName test`.
- The reference design spec is `docs/superpowers/specs/2026-06-24-virus-name-parsing-design.md`.
- `NomCode.VIRUS` and `NameType.VIRUS` already exist in `name-parser-api`. Do not add enum values.

## Key facts about the existing code (read before starting)

- `Preflight.run(String original, String working)` is called once from `Pipeline.run` at `Pipeline.java:68`. It only sees the name portion — **not** the `authorship` argument. The current virus gate is `Preflight.java:180-182`:
  ```java
  if (VIRUS.matcher(s).find() && !ZOOLOGICAL_BINOMIAL.matcher(s).find()) {
    throw new UnparsableNameException(NameType.VIRUS, original);
  }
  ```
- `VIRUS` (`Preflight.java:20-35`) and `ZOOLOGICAL_BINOMIAL` (`Preflight.java:43-51`) patterns stay as-is and are reused.
- `ParseContext` (`ParseContext.java`) holds `requestedCode`, `authorshipInput`, `working`, and the mutable `name`. The caller's code is also pre-set on `name` in the constructor (`name.setCode(code)`).
- `Assemble.finish` (`Assemble.java:20`) sets the code: the `else if (n.getCode() == null) { CodeInference.infer(...); }` block at `Assemble.java:126-130`.
- `Tokenizer.tokenize` (`Tokenizer.java:24`): letters form a `WORD` token (absorbing internal hyphen/apostrophe when between letters, `Tokenizer.java:43-58`); a digit run forms a separate `NUMBER` token (`Tokenizer.java:83-91`).
- `NameAssertion` API: `assertName(raw, expectedCanonical)`, `assertName(raw, rawAuthorship, rank, code, expectedCanonical)` and overloads; `.monomial(name)`, `.monomial(name, rank)`, `.species(genus, epithet)`, `.species(genus, infrageneric, epithet)`, `.infraSpecies(genus, epithet, rank, infraEpithet)`, `.combAuthors(year, authors...)`, `.basAuthors(year, authors...)`, `.type(NameType)`, `.code(NomCode)`, `.nothingElse()`. `assertUnparsable(name, NameType)`.

---

## Phase 1 — Virus binomial parsing

### Task 1: `ViralSuffix` helper

**Files:**
- Create: `name-parser/src/main/java/org/gbif/nameparser/pipeline/ViralSuffix.java`
- Test: `name-parser/src/test/java/org/gbif/nameparser/pipeline/ViralSuffixTest.java`

**Interfaces:**
- Produces: `static boolean ViralSuffix.isViral(String word)` — true when `word` ends in an ICTV viral rank suffix (genus..realm). Uses **singular** suffixes only, so plural Linnaean look-alikes (`Crassatellites`) are NOT matched.

- [ ] **Step 1: Write the failing test**

```java
// name-parser/src/test/java/org/gbif/nameparser/pipeline/ViralSuffixTest.java
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
    assertTrue(ViralSuffix.isViral("Coronaviridae"));   // family
    assertTrue(ViralSuffix.isViral("Nidovirales"));     // order
    assertTrue(ViralSuffix.isViral("Pisuviricota"));    // phylum
  }

  @Test
  public void nonViralLookAlikes() {
    assertFalse(ViralSuffix.isViral("Crassatellites")); // mollusk, plural -satellites
    assertFalse(ViralSuffix.isViral("Aspilota"));
    assertFalse(ViralSuffix.isViral("Adomaviruses"));   // plural -viruses (legacy, not bucket A)
    assertFalse(ViralSuffix.isViral(null));
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl name-parser -Dtest=ViralSuffixTest test`
Expected: FAIL — `ViralSuffix` does not exist (compilation error).

- [ ] **Step 3: Write minimal implementation**

```java
// name-parser/src/main/java/org/gbif/nameparser/pipeline/ViralSuffix.java
package org.gbif.nameparser.pipeline;

import java.util.regex.Pattern;

/**
 * Recognises the standardized ICTV viral rank suffixes on a single word (a genus,
 * monomial, or higher-taxon name). Per MSL41 every virus genus ends in one of these,
 * so the suffix alone is a reliable "this is a virus name" signal.
 *
 * <p>Only the <b>singular</b> canonical suffixes are matched. Plural legacy spellings
 * ({@code -viruses}, {@code -satellites}) are intentionally excluded so Linnaean
 * look-alikes such as the mollusk genus {@code Crassatellites} ({@code -satellites})
 * are not misread as viral.
 */
final class ViralSuffix {
  private ViralSuffix() {}

  // genus / species / viroid / satellite rank suffixes
  private static final Pattern GENUS = Pattern.compile(
      "(?:virus|viroid|satellite|viriform)$", Pattern.CASE_INSENSITIVE);

  // family … realm rank suffixes
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

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -pl name-parser -Dtest=ViralSuffixTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add name-parser/src/main/java/org/gbif/nameparser/pipeline/ViralSuffix.java \
        name-parser/src/test/java/org/gbif/nameparser/pipeline/ViralSuffixTest.java
git commit -m "Add ViralSuffix helper for ICTV rank suffix detection"
```

---

### Task 2: `ParseContext.viralShape` + Preflight virus-gate rewrite

This is the core behaviour change. `Preflight.run` starts taking the full `ParseContext` (so it can see `requestedCode` and `authorshipInput` and set `viralShape`).

**Files:**
- Modify: `name-parser/src/main/java/org/gbif/nameparser/pipeline/ParseContext.java` (add field)
- Modify: `name-parser/src/main/java/org/gbif/nameparser/pipeline/Preflight.java` (signature + gate)
- Modify: `name-parser/src/main/java/org/gbif/nameparser/pipeline/Pipeline.java:68` (call site)
- Test: `name-parser/src/test/java/org/gbif/nameparser/NameParserImplTest.java` (new method)

**Interfaces:**
- Consumes: `ViralSuffix.isViral(String)` (Task 1).
- Produces: `ParseContext.viralShape` (public boolean field, default false); `Preflight.run(String original, ParseContext ctx)`.

- [ ] **Step 1: Write the failing test** (append a new method to `NameParserImplTest`)

```java
  @Test
  public void virusBinomialsParse() throws Exception {
    // Bucket A: clean uni/binomial whose genus carries a viral suffix → SCIENTIFIC + VIRUS
    assertName("Tobamovirus tabaci", "Tobamovirus tabaci")
        .species("Tobamovirus", "tabaci")
        .code(NomCode.VIRUS)
        .nothingElse();
    assertName("Orthoebolavirus zairense", "Orthoebolavirus zairense")
        .species("Orthoebolavirus", "zairense")
        .code(NomCode.VIRUS)
        .nothingElse();
    assertName("Lausannevirus", "Lausannevirus")
        .monomial("Lausannevirus")
        .code(NomCode.VIRUS)
        .nothingElse();
    // higher taxon monomial
    assertName("Coronaviridae", "Coronaviridae")
        .monomial("Coronaviridae", Rank.FAMILY)
        .code(NomCode.VIRUS)
        .nothingElse();
    // legacy vernacular names stay unparsable VIRUS
    assertUnparsable("Tobacco mosaic virus", NameType.VIRUS);
    assertUnparsable("Human papillomavirus", NameType.VIRUS);
    assertUnparsable("Acara virus", NameType.VIRUS);
  }
```

> Note on `Coronaviridae` rank: `Assemble.rankFromGlobalSuffix` only knows `-aceae`/`-oideae`. If `Rank.FAMILY` is not produced for `-viridae`, change that one assertion to `.monomial("Coronaviridae")` (rank UNRANKED) — rank inference for viral suffixes is explicitly out of scope (see spec). Decide by running the test.

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl name-parser -Dtest=NameParserImplTest#virusBinomialsParse test`
Expected: FAIL — `Tobamovirus tabaci` etc. currently throw `UnparsableNameException(VIRUS)`.

- [ ] **Step 3a: Add the `viralShape` field to `ParseContext`**

Insert after the `aggregate` field (`ParseContext.java:26`):

```java
  /**
   * Set by {@link Preflight} when the input is a clean uni/binomial whose genus (or
   * monomial) carries an ICTV viral rank suffix — i.e. a virus name that fits the
   * binomial model. {@link Assemble} turns this into {@link NomCode#VIRUS} when the
   * caller supplied no code.
   */
  public boolean viralShape;
```

- [ ] **Step 3b: Rewrite the Preflight virus gate**

In `Preflight.java`, add these patterns next to `ZOOLOGICAL_BINOMIAL` (after line 51):

```java
  // A clean uni/binomial shape: Genus [ (Subgenus) ] epithet, nothing else. The epithet
  // may carry digits/hyphens (virus epithets like "humanalpha1", zoological "11-punctata").
  private static final Pattern CLEAN_BINOMIAL = Pattern.compile(
      "^\\p{Lu}\\p{Ll}+(?:\\s+\\(\\p{Lu}\\p{Ll}+\\))?\\s+\\p{Ll}[\\p{Ll}\\d\\-]*$",
      Pattern.UNICODE_CHARACTER_CLASS);
  private static final Pattern CLEAN_MONOMIAL = Pattern.compile(
      "^\\p{Lu}[\\p{Ll}\\-]+$", Pattern.UNICODE_CHARACTER_CLASS);

  // Epithet that is a "soft" viral token: a real organism, never a standalone virus
  // binomial. Matched against the last whitespace-separated word (so it also covers the
  // monomial petrel genus "Prion").
  private static final Pattern SOFT_WORD = Pattern.compile(
      "(?:vector|prions?|particles?|replicons?|rna)$", Pattern.CASE_INSENSITIVE);
  // Epithet ending in a "hard" viral token (bare "virus"/"phage" or a compound like
  // "papillomavirus", "attavirus"). Rescuable only by a real zoological author+year.
  private static final Pattern HARD_WORD = Pattern.compile(
      "(?:virus|viroid|phages?|virion|satellite)$", Pattern.CASE_INSENSITIVE);
  // A separately supplied authorship that looks like a Linnaean citation: a Title-cased
  // surname at the very start (optionally bracketed) and a 4-digit year somewhere after.
  // Committee citations ("ICTV", "ICTV 7th Report (2000)") start all-caps and do NOT match.
  private static final Pattern AUTH_ZOO = Pattern.compile(
      "^\\(?\\p{Lu}\\p{Ll}{2,}.*\\b(?:1[6-9]\\d\\d|20\\d\\d)\\b",
      Pattern.UNICODE_CHARACTER_CLASS);
```

Replace the gate at `Preflight.java:180-182` with a call:

```java
    applyVirusGate(s, ctx, original);
```

Add the method (place it near the bottom of the class, before `looksLikeHybridFormula`):

```java
  /**
   * Decides whether a trigger-bearing input is a virus. Sets {@link ParseContext#viralShape}
   * for bucket-A virus binomials; throws {@link UnparsableNameException} for legacy
   * vernacular virus strings. See the design spec for the full bucket model.
   */
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

    // No viral genus: only the legacy trigger words matter.
    if (!VIRUS.matcher(s).find()) {
      return;
    }
    // Inline "Genus [(Subgenus)] epithet Author, YYYY" zoological citation overrides a
    // stray viral token in the epithet (e.g. "Turkozelotes attavirus Chatzaki, 2019").
    if (ZOOLOGICAL_BINOMIAL.matcher(s).find()) {
      return;
    }
    if (req == org.gbif.nameparser.api.NomCode.VIRUS) {
      if (clean) { ctx.viralShape = true; return; }
      throw new UnparsableNameException(NameType.VIRUS, original);
    }
    if (clean && req != null) {
      return; // caller asserts a non-virus code for a clean binomial
    }
    if (clean && SOFT_WORD.matcher(lastWord(s)).find()) {
      return; // bucket B soft: real animal/plant
    }
    if (clean && HARD_WORD.matcher(lastWord(s)).find()
        && ctx.authorshipInput != null
        && AUTH_ZOO.matcher(ctx.authorshipInput.trim()).find()) {
      return; // bucket B hard: rescued by a separately supplied zoological author+year
    }
    throw new UnparsableNameException(NameType.VIRUS, original);
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

Change the method signature `Preflight.run` at `Preflight.java:124` from
`static void run(String original, String working)` to
`static void run(String original, ParseContext ctx)` and, at its top, replace
`String s = working.trim();` with `String s = ctx.working.trim();`.

- [ ] **Step 3c: Update the call site in `Pipeline`**

`Pipeline.java:68`: change
```java
    Preflight.run(scientificName, ctx.working);
```
to
```java
    Preflight.run(scientificName, ctx);
```

- [ ] **Step 3d: Set `VIRUS` from `viralShape` in `Assemble`**

In `Assemble.finish`, immediately after the `if (...) { ... } else if (n.getCode() == null) { CodeInference.infer(ctx, authState); }` block (after `Assemble.java:130`), add:

```java
    // A clean virus binomial (genus carries an ICTV viral suffix) with no caller code
    // → infer the virus code. Caller-supplied codes are already on the name and win.
    if (ctx.viralShape && n.getCode() == null) {
      n.setCode(NomCode.VIRUS);
    }
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn -pl name-parser -Dtest=NameParserImplTest#virusBinomialsParse test`
Expected: PASS (adjust the `Coronaviridae` rank assertion per the Step 1 note if needed).

- [ ] **Step 5: Run the full parser test suite to catch regressions**

Run: `mvn -pl name-parser test`
Expected: Only `virusesPlasmidsPrionsEtc` (and possibly `NameParserGnaTest`) may fail now — those are addressed in Task 3. If any OTHER test fails, fix before continuing.

- [ ] **Step 6: Commit**

```bash
git add name-parser/src/main/java/org/gbif/nameparser/pipeline/ParseContext.java \
        name-parser/src/main/java/org/gbif/nameparser/pipeline/Preflight.java \
        name-parser/src/main/java/org/gbif/nameparser/pipeline/Pipeline.java \
        name-parser/src/main/java/org/gbif/nameparser/pipeline/Assemble.java \
        name-parser/src/test/java/org/gbif/nameparser/NameParserImplTest.java
git commit -m "Parse ICTV virus binomials; infer NomCode.VIRUS from genus suffix"
```

---

### Task 3: Re-curate the `viruses.txt` corpus assertion

The all-`isViralName` assertion in `virusesPlasmidsPrionsEtc` now fails for the handful of entries that have become parsable virus binomials. Move them out of the corpus and assert them positively.

**Files:**
- Modify: `name-parser/src/test/resources/viruses.txt` (remove the now-parsable lines)
- Modify: `name-parser/src/test/java/org/gbif/nameparser/NameParserImplTest.java` (`virusesPlasmidsPrionsEtc`)

**Interfaces:** none (test-only).

- [ ] **Step 1: Identify the entries that now parse**

Run: `mvn -pl name-parser -Dtest=NameParserImplTest#virusesPlasmidsPrionsEtc test`
Expected: FAIL, listing lines that are no longer `isViralName`. They are the viral-suffix monomials and lowercase-epithet binomials:
```
Lausannevirus
Tunisvirus
Clecrusatellite
Milvetsatellite
Subclovsatellite
Marseillevirus marseillevirus
Senegalvirus marseillevirus
```

- [ ] **Step 2: Remove those 7 lines from `viruses.txt`**

Delete exactly those 7 lines from `name-parser/src/test/resources/viruses.txt` (leave everything else). Keep the file otherwise intact.

- [ ] **Step 3: Add positive assertions for them**

In `virusesPlasmidsPrionsEtc`, just before the `Reader reader = resourceReader("viruses.txt");` line (`NameParserImplTest.java:3011`), add:

```java
    // ICTV binomial nomenclature: virus genera/species now parse as SCIENTIFIC + VIRUS.
    assertName("Lausannevirus", "Lausannevirus").monomial("Lausannevirus").code(NomCode.VIRUS).nothingElse();
    assertName("Clecrusatellite", "Clecrusatellite").monomial("Clecrusatellite").code(NomCode.VIRUS).nothingElse();
    assertName("Marseillevirus marseillevirus", "Marseillevirus marseillevirus")
        .species("Marseillevirus", "marseillevirus").code(NomCode.VIRUS).nothingElse();
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn -pl name-parser -Dtest=NameParserImplTest#virusesPlasmidsPrionsEtc test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add name-parser/src/test/resources/viruses.txt \
        name-parser/src/test/java/org/gbif/nameparser/NameParserImplTest.java
git commit -m "Re-curate viruses.txt: ICTV binomials now parse as SCIENTIFIC+VIRUS"
```

---

### Task 4: False-positive animals (bucket B) + caller-code override

Closes `PREFLIGHT_VIRUS_FALSE_POSITIVES.md`: real animals whose epithet collides with a viral token now parse, and the caller `code` overrides the virus gate.

**Files:**
- Test: `name-parser/src/test/java/org/gbif/nameparser/NameParserImplTest.java` (new methods)

**Interfaces:** none (behaviour from Task 2).

- [ ] **Step 1: Write the failing tests**

```java
  @Test
  public void virusFalsePositiveAnimals() throws Exception {
    // soft epithet — parse without needing an author, real (zoological) code, NOT virus
    assertName("Aspilota vector", "Belokobylskij, 2007", Rank.SPECIES, NomCode.ZOOLOGICAL, "Aspilota vector")
        .species("Aspilota", "vector").combAuthors("2007", "Belokobylskij").code(NomCode.ZOOLOGICAL).nothingElse();
    assertName("Euragallia prion", "Euragallia prion")
        .species("Euragallia", "prion").nothingElse();
    assertName("Cryptops (Cryptops) vector", "Chamberlin, 1939", Rank.SPECIES, NomCode.ZOOLOGICAL, "Cryptops vector")
        .species("Cryptops", "Cryptops", "vector").combAuthors("1939", "Chamberlin").code(NomCode.ZOOLOGICAL).nothingElse();
    // soft monomial — the petrel genus Prion
    assertName("Prion", "Prion").monomial("Prion").nothingElse();
    // hard epithet rescued by a separately supplied zoological author+year
    assertName("Exochus virus", "Gauld & Sithole, 2002", Rank.SPECIES, NomCode.ZOOLOGICAL, "Exochus virus")
        .species("Exochus", "virus").combAuthors("2002", "Gauld", "Sithole").code(NomCode.ZOOLOGICAL).nothingElse();
    // hard epithet, committee "authorship" → stays VIRUS
    assertUnparsable("Acara virus", NameType.VIRUS);
  }

  @Test
  public void virusCallerCodeOverride() throws Exception {
    // caller asserts a non-virus code on an otherwise viral-looking clean binomial
    assertName("Exochus virus", "Gauld & Sithole, 2002", Rank.SPECIES, NomCode.ZOOLOGICAL, "Exochus virus")
        .species("Exochus", "virus").code(NomCode.ZOOLOGICAL);
    // caller forces VIRUS on a legacy bare-virus binomial → still unparsable, type VIRUS
    assertUnparsable("Acara virus", null, NameType.VIRUS); // uses assertUnparsable(name, rank, type); rank null
  }
```

> If `assertUnparsable(String, Rank, NameType)` requires a non-null rank in your tree, drop the second method's last assertion and instead assert `assertName("Tobamovirus tabaci", NomCode.ZOOLOGICAL, "Tobamovirus tabaci").species("Tobamovirus","tabaci").code(NomCode.ZOOLOGICAL);` to prove caller code wins on a bucket-A name. Verify the available overloads at `NameParserImplTest.java:4166-4235`.

- [ ] **Step 2: Run to verify it fails / passes**

Run: `mvn -pl name-parser -Dtest=NameParserImplTest#virusFalsePositiveAnimals+virusCallerCodeOverride test`
Expected: PASS if Task 2 is correct (these are regression guards for the bucket-B logic). If any FAIL, fix `applyVirusGate` in `Preflight` — do **not** weaken the tests.

- [ ] **Step 3: Delete the obsolete handoff note**

```bash
git rm PREFLIGHT_VIRUS_FALSE_POSITIVES.md
```

- [ ] **Step 4: Run the full suite**

Run: `mvn -pl name-parser test`
Expected: PASS (all).

- [ ] **Step 5: Commit**

```bash
git add name-parser/src/test/java/org/gbif/nameparser/NameParserImplTest.java
git commit -m "Test virus false-positive animals + caller-code override; drop handoff note"
```

---

## Phase 2 — Digit-bearing epithets

> Independent of Phase 1's routing, but virus epithets need it (1/3 of ICTV species carry digits). Land Phase 1 first.

### Task 5: Trailing / internal digits in epithets

Today `Tokenizer` splits `humanalpha1` into `WORD("humanalpha") + NUMBER("1")` and `NameTokens` drops the number — silently merging `humanalpha1` and `humanalpha2`. Fix: glue glued (no-whitespace) trailing digits and hyphen-digit runs into the letter-started `WORD` token.

**Files:**
- Modify: `name-parser/src/main/java/org/gbif/nameparser/token/Tokenizer.java:43-58`
- Test: `name-parser/src/test/java/org/gbif/nameparser/NameParserImplTest.java` (new method)

**Interfaces:**
- Produces: a single `WORD` token for a letter-started run with glued trailing digits / hyphen-digits (e.g. `WORD("humanalpha1")`, `WORD("herpesvirus-1")`).

- [ ] **Step 1: Write the failing test**

```java
  @Test
  public void digitEpithetsTrailing() throws Exception {
    // distinct epithets must NOT collapse
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
Expected: FAIL — epithet comes back as `humanalpha` (digit dropped), so `.nothingElse()`/epithet assertions fail.

- [ ] **Step 3: Implement the tokenizer glue**

In `Tokenizer.tokenize`, inside the WORD continuation loop (`Tokenizer.java:40-59`), extend the accepted continuation characters. Replace the loop body so it also absorbs digits and a hyphen followed by a digit:

```java
        while (i < n) {
          int c = input.codePointAt(i);
          int cl = Character.charCount(c);
          if (Character.isLetter(c) || Character.isDigit(c)) {
            i += cl;
            continue;
          }
          // internal hyphen / apostrophe / underscore / stray "!" when followed by a
          // letter OR digit, so alphanumeric epithets ("herpesvirus-1") stay intact.
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

(The only changes vs the original are the added `|| Character.isDigit(c)` on the first `if`, and `|| Character.isDigit(next)` on the hyphen look-ahead.)

- [ ] **Step 4: Run the targeted test, then the full suite**

Run: `mvn -pl name-parser -Dtest=NameParserImplTest#digitEpithetsTrailing test` → Expected: PASS
Run: `mvn -pl name-parser test` → Expected: PASS. If a previously-passing test now fails because a code/strain token gained digits, inspect: the most likely spots are the `sp.`-phrase strain-code handling in `NameTokens.java:265-283` (it already re-globs `WORD + NUMBER`, so a single glued WORD is fine) and any test asserting an authorship token with glued digits. Fix the implementation, not the unrelated tests.

- [ ] **Step 5: Commit**

```bash
git add name-parser/src/main/java/org/gbif/nameparser/token/Tokenizer.java \
        name-parser/src/test/java/org/gbif/nameparser/NameParserImplTest.java
git commit -m "Preserve trailing/internal digits in epithet tokens"
```

---

### Task 6: Leading-numeral historical zoological epithets

`Coccinella 11-punctata` is corrupted today: `AuthorshipSplit` ends the name at the `NUMBER` after the genus (treating `11-punctata Linnaeus, 1758` as authorship). Fix in three coordinated changes: tokenize `11-punctata` as one alphanumeric `WORD`; let `AuthorshipSplit` keep a digit-led epithet word inside the name; let `NameTokens` classify it as an epithet.

**Files:**
- Modify: `name-parser/src/main/java/org/gbif/nameparser/token/Tokenizer.java:83-91` (NUMBER branch)
- Modify: `name-parser/src/main/java/org/gbif/nameparser/pipeline/AuthorshipSplit.java`
- Modify: `name-parser/src/main/java/org/gbif/nameparser/pipeline/NameTokens.java`
- Test: `name-parser/src/test/java/org/gbif/nameparser/NameParserImplTest.java` (new method)

**Interfaces:**
- Produces: a single `WORD` token for a digit-run glued to `-` + letter (e.g. `WORD("11-punctata")`), classified as a specific/infraspecific epithet.

- [ ] **Step 1: Write the failing test**

```java
  @Test
  public void digitEpithetsLeadingNumeral() throws Exception {
    assertName("Coccinella 11-punctata Linnaeus, 1758", "Coccinella 11-punctata")
        .species("Coccinella", "11-punctata")
        .combAuthors("1758", "Linnaeus")
        .code(NomCode.ZOOLOGICAL)
        .nothingElse();
    assertName("Coccinella 2-pustulata", "Coccinella 2-pustulata")
        .species("Coccinella", "2-pustulata")
        .nothingElse();
  }
```

- [ ] **Step 2: Run to verify it fails**

Run: `mvn -pl name-parser -Dtest=NameParserImplTest#digitEpithetsLeadingNumeral test`
Expected: FAIL — epithet missing; `punctata` lands in authorship.

- [ ] **Step 3a: Tokenizer — glue number + hyphen + letter into one WORD**

In `Tokenizer.tokenize`, replace the NUMBER branch (`Tokenizer.java:83-91`) so a digit run that is immediately followed by a hyphen + letter becomes a single alphanumeric `WORD` token; otherwise it stays a `NUMBER`:

```java
      if (Character.isDigit(cp)) {
        int numStart = i;
        i += charLen;
        while (i < n && Character.isDigit(input.codePointAt(i))) {
          i++;
        }
        // "11-punctata" / "2-pustulata": a number glued to a hyphen + letter is the
        // leading-numeral epithet form, not a bare number. Absorb the rest as a WORD.
        if (i + 1 < n && input.charAt(i) == '-' && Character.isLetter(input.codePointAt(i + 1))) {
          i++; // consume the hyphen
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

- [ ] **Step 3b: `Token` — recognise a digit-led alphanumeric epithet word**

Check `name-parser/src/main/java/org/gbif/nameparser/token/Token.java` for a `startsLower()` helper. Add a small helper used by the pipeline:

```java
  /** True for an alphanumeric epithet word that begins with a digit, e.g. "11-punctata". */
  public boolean startsDigitEpithet() {
    return kind == TokenKind.WORD && !text.isEmpty()
        && Character.isDigit(text.codePointAt(0))
        && text.chars().anyMatch(Character::isLetter);
  }
```

- [ ] **Step 3c: `AuthorshipSplit` — keep a digit-led epithet inside the name**

In `AuthorshipSplit.findBoundary`, the WORD branch only handles `startsLower()` / uppercase. A digit-led WORD currently falls through to `return i` at the end of the WORD block (`AuthorshipSplit.java:206-207`) — actually it reaches the bottom `return i` because neither `startsLower()` nor the uppercase branches match. Add, right after the `if (t.startsLower()) { ... }` block closes (after `AuthorshipSplit.java:176`) and before the mid-name-author handling:

```java
        // Digit-led alphanumeric epithet ("11-punctata") in epithet position — part of
        // the name, not authorship.
        if (afterGenus && t.startsDigitEpithet()) {
          nameWords++;
          haveEpithet = true;
          afterSubgenus = false;
          i++;
          continue;
        }
```

- [ ] **Step 3d: `NameTokens` — classify a digit-led epithet word**

In `NameTokens.classify`, the WORD handling (`NameTokens.java:151`) processes uppercase and `startsLower()` words. Add a branch for the digit-led epithet just before the `if (t.startsLower()) {` block (`NameTokens.java:210`):

```java
        // Digit-led alphanumeric epithet ("11-punctata"): treat as an ordinary epithet.
        if (genus != null && t.startsDigitEpithet()) {
          lowerEpithets.add(t.text);
          i++;
          continue;
        }
```

- [ ] **Step 4: Run the targeted test, then the full suite**

Run: `mvn -pl name-parser -Dtest=NameParserImplTest#digitEpithetsLeadingNumeral test` → Expected: PASS
Run: `mvn -pl name-parser test` → Expected: PASS. Watch for: year-range tokens (`1976-1981`) must stay `NUMBER`-based — the Step 3a guard only absorbs hyphen+**letter**, so `1976-1981` is unaffected; verify any author/year-range test still passes.

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

### Task 7: End-to-end validation against real data

**Files:** none (verification only).

- [ ] **Step 1: Full build + tests across all modules**

Run: `mvn install`
Expected: BUILD SUCCESS, all modules.

- [ ] **Step 2: Re-run the MSL41 corpus through the CLI (optional but recommended)**

If the MSL41 species file from brainstorming is still in the scratchpad, re-run it and confirm the parse rate jumped from 131 to the vast majority. Pure-alpha and trailing-digit epithets should parse with `code=VIRUS`; `Genus LETTER` serotype forms remain `VIRUS`.

```bash
JAR=name-parser-cli/target/name-parser-cli-*-shaded.jar
mvn -pl name-parser-cli -am install -DskipTests -q
java -jar $JAR parse --input=<msl_species.txt> --output=- --format=jsonl --quiet 2>/dev/null \
  | python3 -c "import sys,json,collections; c=collections.Counter('VIRUS' if 'error' in json.loads(l) else 'ok' for l in sys.stdin); print(c)"
```
Expected: `ok` now dominates (≈17k), down from 131.

- [ ] **Step 3: Commit any final doc updates**

Mark the spec as implemented if you keep a status line, then:
```bash
git add -A && git commit -m "Virus name parsing: full build green"
```

---

## Self-Review notes (addressed)

- **Spec rule #4 refinement:** the spec's "compound epithet ⇒ always virus" is superseded by a unified rule — *any* hard epithet (bare or compound) is rescued only by a real `Surname + year` (inline `ZOOLOGICAL_BINOMIAL`, or the `AUTH_ZOO` pattern on a separately supplied authorship). Committee citations (`ICTV …`) don't match either, so legacy names stay `VIRUS`. This is required to keep the existing `Turkozelotes attavirus Chatzaki, 2019` test green. The spec will be updated to match.
- **Plural suffix safety:** `ViralSuffix` uses singular suffixes only, so `Crassatellites` (mollusk) is not flagged; the existing `Crassatellites janus` test stays green.
- **Type consistency:** `ParseContext.viralShape` (set in `Preflight.applyVirusGate`, read in `Assemble.finish`); `ViralSuffix.isViral`; `Token.startsDigitEpithet` (added in Task 6, used by `AuthorshipSplit` and `NameTokens`).
