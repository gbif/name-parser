# ANTLR grammar TODO

Backlog of grammar / listener work needed on the `antlr-parser` branch to close the gap with the
old regex parser.

## Current scoreboard
`mvn test -pl name-parser`:

| Suite                          | Pass | Fail | Err | Skip |
|--------------------------------|-----:|-----:|----:|-----:|
| `NameParserAntlrTest`          |   84 |   10 |   0 |    4 |
| `NameParserAntlrTimeoutTest`   |    1 |    1 |   0 |    0 |
| `NameParserAntlrThreadTest`    |    0 |    0 |   0 |    1 |
| `ParsingJobTest`               |   13 |    0 |   0 |    0 |
| `ParserConfigsTest`            |    3 |    0 |   0 |    1 |
| `InterruptibleCharSequenceTest`|    1 |    0 |   0 |    0 |
| `utils/*`                      |    6 |    0 |   0 |    0 |

The `NameParserAntlrTimeoutTest` failure is **not a regression** — ANTLR parses the pathological
authorship in 3 ms instead of catastrophically backtracking. The test expected the old regex
timeout behaviour. Either rewrite the test or accept the new behaviour as a perf win.

## Resolved in this round (was failing before)

- **Slot-not-filled cluster** — `subg`, `rechf`, `tinfr`, `vulpes`, `apostropheEpithets`,
  parts of `infraSpecies`, `fourPartedNames`, `authorVariations`. Fixed by adding the
  `inlineAuthor` rule pinned next to a rank marker, the bare-marker / composite-marker /
  indet variants of `rankedInfraspec`, the `t.infr` / `f.sp` `COMPOSITE_RANK` tokens, and
  expanding `isInfraspecRank` to accept SPECIFIC markers (`sp.` in zoological trinomials).
- **`notho` / `agamo` rank-prefix preservation** — `parseRank` now strips the prefix
  before lookup; `setRank` still detects the prefix on the original string and sets
  `pn.setNotho(...)`.
- **`var.` indet rank marker** (`Nitzschia sinuata var. (Grunow) Lange-Bert.`) — handled
  via the new indet alternative of `rankedInfraspec`.
- **Hybrid prefix rendering** (`× Pyrocrataegus willei`) — `ParsingJob.parseNormalisedName`
  now wires `nc.isHybridGenus/Species/Infraspecies()` to `pn.setNotho(...)`.
- **`(auct.) Rolfe` / sensu-only authorships** — `AuthorshipParsingJob` no longer fails when
  `extractSecReference` empties the input.
- **`ex` author splitting after NORM_PUNCTUATIONS** — both matchers use the same
  `(?i)(?<=\W)ex\.?(?=\s|\b)` regex so `Wedd.ex Sch.Bip.` splits correctly even when the
  whitespace around the dot has been normalised away.
- **PR2-style underscore strings** — the lexer's `OTHER : ~[_]` keeps the parser tolerant
  of `=`, `/`, `:`, digits and other junk while still rejecting underscores so
  `Basal_Cryptophyceae-1` stays unparseable.
- **`namesWithAuthorFile` / `noHybrids` lex errors** — `UPPER_WORD` now accepts single-letter
  hyphen compounds (`Z-X`) and a new `SINGLE_UPPER` token covers lone author initials (`F`).
- **`occNameFile`** — failure count dropped from 63 to 5 (baseline is 4).

## Known remaining failures

These are tracked in `NameParserAntlrTest`'s failure list and need either grammar work or
specialised pre-cleanup:

1. **`cv. ex` author marker** (`exAuthors:680`, `authorVariations:1804`) — the cultivar
   marker followed by an ex-author indicator should be transformed into `hort. ex Author`
   per the legacy regex parser. Currently `cv.` gets recognised as a CULTIVAR rank marker
   and the ex-author isn't surfaced.
2. **Lowercase author block in parens** (`manuscriptNames:3120`, `sic:184`) — inputs like
   `Micromeria cristata subsp. kosaninii ( ilic) ined.` and
   `Turbo porphyrites [sic, porphyria]` produce a `basionymGroup` whose contents are pure
   lowercase. The legacy parser left these as remainder; the grammar needs a predicate to
   reject all-lowercase basionym groups (or detect `(sic, ...)` specifically and route to
   the warning slot).
3. **Trailing single LOWER_WORD as remainder** (`fourPartedNames:727` —
   `Cymbella cistula var. sinus regis`). After a complete trinomial, `regis` shouldn't be
   pulled into authorship. A bare LOWER_WORD with no continuation should fall through to
   remainder; my last attempt at this regressed other tests.
4. **`Arrhoges (Antarctohoges)` swap** (`infraGeneric:887`) — zoological subgenus that is
   actually an author needs swapping in `ParsingJob`. `infragenericIsAuthor` returns false
   because `Antarctohoges` ends in a Latin ending. Needs a code-aware override.
5. **`Canis lupus subsp. Linnaeus, 1758`** (`indetNames:2581`) — `setRank("subsp")`
   unconditionally stamps BOTANICAL even though there's no infraspecific epithet and the
   trailing year suggests zoological. Conditional BOTANICAL stamping caused regressions
   elsewhere — needs a more careful rule.
6. **`(= Grislea L. 1753).` remainder leading char** (`nomNotes:2153`) — the legacy parser
   captured the closing `)` of `(1758)` as the start of the remainder. The balanced
   `yearMaybe` consumes it instead, so the partial string is missing the leading `)`.
7. **`Passiflora eglandulosa ... shit ...`** (`blacklisted:507`) — the test expects the
   blacklisted "shit" epithet to be detected and slotted as the infraspecific epithet. The
   pre-cleanup would need to identify the blacklist word inside a bibliographic reference.

## Architectural notes

- The `epithet` / `bareInfraspec` author-particle predicate uses a hardcoded
  `AUTHOR_PARTICLES` set in `SciName.g4` `@parser::members`. The original parser builds its
  particle list from regex constants in `ParsingJob`. Sync drift is possible — consider
  extracting the particle set to a shared Java helper.
- `rankedInfrageneric` / `rankedInfraspec` rank-marker lookups go through
  `RankUtils.RANK_MARKER_MAP_*` via the parser predicate; `notho` / `agamo` prefix stripping
  is duplicated between the predicate and `ParsingJob.parseRank`.
- Both `ParsingJob.parseNormalisedName` and `AuthorshipParsingJob.parseNormalisedAuthorship`
  forward to ANTLR via `org.gbif.nameparser.antlr.AntlrNameMatcher` /
  `AntlrAuthorshipMatcher`. The standalone authorship rule in the grammar is shared with
  the inline-authorship slot used by `name`.
- The matcher uses ANTLR's default error strategy (no throwing listener on the parser side)
  because adaptive LL prediction occasionally bails on legitimate names with trailing junk.
  Error-recovered tokens are picked up by the `TerminalNode` walk in
  `AntlrNameMatcher.match` and re-merged into the remainder slice from the original source.
- `inlineAuthor` is structurally pinned next to a rank-marker rule at the genus level and
  gated by a semantic predicate (`hasInfraspecRankAhead`) at the species level. Without
  the species-level predicate ANTLR's prediction greedily consumes UPPER tokens as inline
  authors even when no rank marker follows.

## Out of scope

These would need new grammar work but are low priority:
- Microbial rank markers (`bv.`, `ct.`) — only one or two tests rely on them.
- Phrase names (`Genus sp. 'descriptive phrase' (voucher)`) — already handled by the
  existing `PHRASE_NAME` regex *before* the ANTLR step fires; fine to leave there.
- Hybrid formulas (`Polypodium x vulgare`) — same; handled by `HYBRID_FORMULA_PATTERN`
  early in `ParsingJob.parse`.

## How to iterate

1. Run `mvn test -pl name-parser -Dtest=NameParserAntlrTest`.
2. Pick a failing test, read its assertion in `NameParserAntlrTest.java`.
3. Trace the input through `ParsingJob.normalizeStrong()` first (logging available via
   `LOG.debug` in `ParsingJob`).
4. Edit `name-parser/src/main/antlr4/org/gbif/nameparser/antlr/SciName.g4` and rebuild —
   the `antlr4-maven-plugin` regenerates the parser on `mvn generate-sources` or any
   compile.
