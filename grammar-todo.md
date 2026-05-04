# ANTLR grammar TODO

Backlog of grammar / listener work needed on the `antlr-parser` branch to close the gap with the
old regex parser. Generated after iteration 4 (commit `077b0a6`).

## Current scoreboard
`mvn test -pl name-parser`:

| Suite                          | Pass | Fail | Err | Skip |
|--------------------------------|-----:|-----:|----:|-----:|
| `NameParserAntlrTest`          |   68 |   21 |   5 |    4 |
| `NameParserAntlrTimeoutTest`   |    1 |    1 |   0 |    0 |
| `NameParserAntlrThreadTest`    |    0 |    0 |   0 |    1 |
| `ParsingJobTest`               |   13 |    0 |   0 |    0 |
| `ParserConfigsTest`            |    3 |    0 |   0 |    1 |
| `InterruptibleCharSequenceTest`|    1 |    0 |   0 |    0 |
| `utils/*`                      |    6 |    0 |   0 |    0 |

The `NameParserAntlrTimeoutTest` failure is **not a regression** — ANTLR parses the pathological
authorship in 3 ms instead of catastrophically backtracking. The test expected the old regex
timeout behaviour. Either rewrite the test or accept the new behaviour as a perf win.

## High-impact known issues

### 1. Slot-not-filled cluster
Several tests show the structural slot is empty when it should be populated. The grammar /
listener fails to attach the matched piece to `NameComponents`. Investigate ANTLR adaptive
prediction behaviour around the affected rules.

| Test                     | Input                                  | Expected slot                  |
|--------------------------|----------------------------------------|--------------------------------|
| `subg`                   | `Centaurea subg. Jacea`                | `infrageneric=Jacea`           |
| `rechf`                  | `Salix repens subsp. galeifolia`       | `infraspec=galeifolia`         |
| `tinfr`                  | `Hieracium vulgatum arrectariicaule`   | `infraspec=arrectariicaule`    |
| `vulpes`                 | `Vulpes vulpes sp. silaceus, 1907`     | `infraspec=silaceus`           |
| `infraSpecies`           | `Festuca ovina subvar. gracilis`       | `infraspec=gracilis`           |
| `apostropheEpithets`     | `… meridionalis var. o'donelli`        | `infraspec=o'donelli`          |
| `fourPartedNames`        | `… gueldenstaedti natio danubicus`     | `middle=natio, infraspec=danu…`|
| `authorVariations`       | `Cirsium creticum subsp. creticum`     | `infraspec=creticum`           |

Likely root cause: the validating semantic predicates on `rankedInfrageneric` /
`rankedInfraspec` and the author-particle predicate on `epithet` interact with adaptive LL(*)
prediction in a way that prevents the rule from being chosen even when it would match. May need
to switch to lexer-mode promotion (emit a dedicated `RANK_MARKER` token type for known markers
post-lex) instead of parser-side predicates. Worth a small standalone repro to confirm.

### 2. PARTIAL state heuristics
The permissive grammar consumes things that should land in the `remainder` slot.

| Test               | Input                                                | Expected         | Got                |
|--------------------|------------------------------------------------------|------------------|--------------------|
| `sic`              | `Ameiva plei (sic) Duméril & Bibron, 1839`           | `state=PARTIAL`  | `state=COMPLETE`   |
| `manuscriptNames`  | various `[ms]`, `nom. nud.` etc.                     | `state=PARTIAL`  | `state=COMPLETE`   |
| `unsupportedAuthors`| various                                             | `state=PARTIAL`  | `state=COMPLETE`   |
| `nomNotes`         | `(= Grislea L. 1753)` should be unparsed             | non-empty unparsed | empty            |

Need a heuristic in the listener / matcher: if the input contained `(sic)`, `[ms]`,
`nom. nud.`, `(= …)`, etc., flag PARTIAL even if the structural parse covers the rest.

### 3. `notho` / `agamo` rank prefix preservation
Test `nothotaxa`: `Abies alba nothovar. alpina` — listener strips `notho` before the rank-marker
lookup but then stores only `var` in `infraspecificRankMarker`, losing the `notho` prefix that
the formatter uses to render `nothovar.`. Store the **original** marker text alongside the
normalised lookup key.

### 4. `var.` indet rank marker (no following epithet)
`Nitzschia sinuata var. (Grunow) Lange-Bert.` — `var.` is followed by `(` not an epithet.
Currently `Unparsable`. Add an indeterminate rule `rankedInfraspec : LOWER_WORD DOT` (no
trailing epithet) that surfaces only the rank marker.

### 5. `(auct.)` markers in authorship
`(auct.) Rolfe` is currently unparsable. The `auct.` literal inside parens should be treated as
a *sec/sensu* marker, not as the basionym authors. Surface a `sensu` slot from the grammar.

### 6. `<author> in <author>` ex-author indicator
`Grunow in Van Heurck, 1880-1885` — the `in` keyword introduces the publication author as a
follow-on. Existing `splitTeam` doesn't see it; the grammar would need an `inAuthors` slot on
`combPart`. Affects `noHybrids`, `namesWithAuthorFile`.

### 7. Hybrid prefix `× ` rendering
Test `hybridNames`: `× Pyrocrataegus willei` — formatter produces `Pyrocrataegus willei`
without the leading `× `. Listener needs to set the `notho` flag on the genus when monomial has
a `HYBRID` prefix.

### 8. `aggregates` rank inference
`Foo bar L.` with `Rank.SPECIES_AGGREGATE` hint — the rank is being downgraded to `SPECIES` by
`setRank`/`setUninomialOrGenus` interaction. Don't override an externally-supplied
species-aggregate rank.

### 9. Unparsable / placeholder detection regression
`blacklisted` test has very long mixed-content input expecting a structured PARTIAL parse with
specific epithet detection. Probably needs better fall-through from author-blob into
`publishedIn` extraction.

### 10. `et` author splitting
`Hernández-García et. al., 2023` — passes today, but the author list output is
`["Hernández-García et. al."]` rather than `["Hernández-García", "al."]`. The matcher should
split the author blob on standalone `et` (Latin "and") before passing it to `parseAuthorship`,
mirroring the existing `NORM_ET_AL` behaviour.

## occNameFile bulk regression

`mvn test -Dtest=NameParserAntlrTest#occNameFile` now fails 63 lines from
`name-parser/src/test/resources/occurrence-names.txt` (was failing 4 against the regex parser).
Most of these are downstream consequences of items 1–7. Each fix typically unblocks several
lines from this file too. Re-run after each grammar change to track progress.

## Grammar maintenance

- The `epithet` / `bareInfraspec` author-particle predicate uses a hardcoded `AUTHOR_PARTICLES`
  set in `SciName.g4` `@parser::members`. The original parser builds its particle list from
  regex constants in `ParsingJob`. Sync drift is possible — consider extracting the particle set
  to a shared Java helper that both grammar and `ParsingJob` import.
- The `rankedInfrageneric` / `rankedInfraspec` rank-marker lookups go through
  `RankUtils.RANK_MARKER_MAP_*` via the parser predicate. Adding a new rank to `RankUtils` will
  automatically work, but the `notho` / `agamo` prefix stripping is duplicated in the predicate.
- Both `ParsingJob.parseNormalisedName` and `AuthorshipParsingJob.parseNormalisedAuthorship`
  forward to ANTLR via `org.gbif.nameparser.antlr.AntlrNameMatcher` /
  `AntlrAuthorshipMatcher`. The standalone authorship rule in the grammar is shared with the
  inline-authorship slot used by `name`, so improvements to authorship parsing benefit both.

## Out of scope

These would need new grammar work but are low priority:
- Microbial rank markers (`bv.`, `ct.`, `f.sp.`) — only one or two tests rely on these.
- Phrase names (`Genus sp. 'descriptive phrase' (voucher)`) — already handled by the existing
  `PHRASE_NAME` regex *before* the ANTLR step fires; fine to leave there.
- Hybrid formulas (`Polypodium x vulgare`) — same; handled by `HYBRID_FORMULA_PATTERN` early in
  `ParsingJob.parse`.

## How to iterate

1. Run `mvn test -pl name-parser -Dtest=NameParserAntlrTest`.
2. Pick a failing test, read its assertion in `NameParserAntlrTest.java`.
3. Trace the input through `ParsingJob.normalizeStrong()` first (logging available via
   `LOG.debug` in `ParsingJob`).
4. Write a focused unit test against `AntlrNameMatcher.match(normalizedName)` to confirm whether
   the gap is in the grammar or the listener glue.
5. Edit `name-parser/src/main/antlr4/org/gbif/nameparser/antlr/SciName.g4` and rebuild — the
   `antlr4-maven-plugin` regenerates the parser on `mvn generate-sources` or any compile.
