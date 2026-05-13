# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test

Maven multi-module project, Java 11. From the repo root:

- `mvn install` — build & test all three modules (default goal of `name-parser` is `install`)
- `mvn -pl name-parser test` — run tests for a single module
- `mvn -pl name-parser -Dtest=NameParserGBIFTest test` — run a single test class
- `mvn -pl name-parser -Dtest=NameParserGBIFTest#parseSubgenera test` — run a single test method
- `mvn -pl name-parser-v1 -am install` — build a downstream module and its dependencies

Artifacts publish to the GBIF Nexus (`repository.gbif.org`); releases are driven by `maven-release-plugin` against the `motherpom` parent.

## Module layout

The repo is a parent POM (`name-parser-motherpom`) over three modules. Dependencies flow strictly: `name-parser-api` ← `name-parser` ← `name-parser-v1`.

- **`name-parser-api`** — pure model + interface module. Defines `ParsedName`, `ParsedAuthorship`, `Rank`, `NomCode`, `NameType`, `Authorship`, `Warnings`, and the `NameParser` interface. Also ships `NameFormatter`, `RankUtils`, and `UnicodeUtils` (loads `unicode/homoglyphs.txt`). No dependency on the parser implementation — downstream callers can depend on this alone for the data model.
- **`name-parser`** — the regex-based GBIF implementation. Single public entry point: `NameParserGBIF`. Most parsing logic lives in `ParsingJob` (~1.9k lines of regex constants and methods) and `AuthorshipParsingJob`.
- **`name-parser-v1`** — adapter that wraps `NameParserGBIF` and exposes the legacy `org.gbif.api.service.checklistbank.NameParser` interface (from `gbif-api`), translating between the new `org.gbif.nameparser.api.ParsedName` and the v1 `org.gbif.api.model.checklistbank.ParsedName`. Touch this when v1 callers need a behavior change that's already in the underlying parser.

## How parsing works

`NameParserGBIF` is the only public parser class. Important behaviors that aren't obvious from any single file:

- **Timeout via thread pool, not regex flags.** Each parse runs as a `Callable` (`ParsingJob` / `AuthorshipParsingJob`) submitted to a shared `ThreadPoolExecutor`. The caller waits with `Future.get(timeout)`; on timeout the task is cancelled. The job uses `InterruptibleCharSequence` to make Java's regex engine respect `Thread.interrupt()` — without it long-running regex matches would not abort. **Reuse a `NameParserGBIF` instance** and call `close()` on shutdown; constructing one per parse spins up a new thread pool.
- **Two-phase parsing.** `parseAuthorship(...)` actually invokes the full parser with a synthetic `"Abies alba <authorship>"` and pulls authorship out of the result.
- **Manual overrides.** Before regex parsing, `NameParserGBIF` consults `ParserConfigs` for a pre-canned `ParsedName`/`ParsedAuthorship` keyed by a normalized form of the input (lowercased, unicode-decomposed, whitespace/punctuation collapsed — see `ParserConfigs.norm`). Overrides can be added programmatically via `parser.configs().add(...)` or bulk-loaded from the ChecklistBank API via `ParserConfigs.loadFromCLB()` / `load(URI)`.
- **Result states.** A successful parse returns a `ParsedName` with `State.COMPLETE` or `State.PARTIAL`. Names that cannot be expressed by the model (viruses, hybrid formulas, blank input) throw `UnparsableNameException` carrying a `NameType` — callers must catch it.

## Working with `ParsingJob`

`ParsingJob.java` is the heart of the parser and is intentionally a large pile of regex constants composed from sub-patterns (`NAME_LETTERS`, `AUTHOR_TOKEN`, `AUTHOR`, `AUTHOR_TEAM`, `AUTHORSHIP`, …). When changing or adding patterns:

- The composed regexes are case-sensitive about Unicode classes (`\p{Lu}`, `\p{Ll}`) and the explicit letter-set constants — be careful not to accidentally restrict the alphabet.
- Behavior is verified almost entirely through `NameParserGBIFTest` (the canonical regression suite — see `README.md`) using the `NameAssertion` fluent helper. Add a test there for any parsing change before tweaking patterns.
- Auxiliary test corpora live in `name-parser/src/test/resources/` (`doubtful.txt`, `hybrids.txt`, `nonames.txt`, `occurrence-names.txt`, `placeholder.txt`, `unparsable.txt`, `viruses.txt`, `names-with-authors.txt`). `TODO-names.txt` tracks known-failing inputs.
- Resource files driving parser behavior are in `name-parser/src/main/resources/nameparser/`: `blacklist-epithets.txt` (flags doubtful names) and `latin-endings.txt` (common 4-char Latin suffixes — sorted by *reverse* of the ending; see the resource README before re-sorting).

## Conventions

- Java 11 source/target; no `var` is enforced but the codebase uses it freely in newer files.
- Logging is SLF4J; tests pull in `logback-classic`. Test logging config: `name-parser/src/test/resources/logback-test.xml`.
- Apache 2.0 license header is on every source file.

## Authorship conventions

- **Surname-first authors with all-caps trailing initials** are flipped to initial-prefixed form. `Walker F` → `F.Walker`, `Balsamo M Fregni E Tongiorgi MA` → `M.Balsamo, E.Fregni, M.A.Tongiorgi`. The flip only applies when the buffer ends with a real surname (not just particles): `H.da C.` followed by `Monteiro` keeps the initials-particle-initial chain intact and renders `H.da C.Monteiro …`. Isolated `Zhang F` inside a separator-bounded segment (e.g. `Zhang F & Pan Z-X`) also flips to `F.Zhang`.
- **Hyphenated initials** (`Y.-j. Wang`, `C.-K. Yang`) preserve both the hyphen AND the input case of the single-letter follow-up: `Y.-j.Wang`, `C.-K.Yang`. Don't uppercase the post-hyphen letter.
- **Code inference signals** (in priority order) — sanctioning author → `BOTANICAL`; `(BasAuthor) RecombAuthor, year` with an explicit infraspecific marker (subsp./var./f.) → `BOTANICAL` (botanical recombination, year is the publication year); any other year on the author span → `ZOOLOGICAL`; `f.`/`fil.`/`filius` suffix on a non-ex author *without any year* → `BOTANICAL` (zoological literature does use filius for father/son, e.g. *Lacerta agilis* Linnaeus f., 1789, but those cases always carry a year and are caught by the year rule first); basionym + combination authors without years → `BOTANICAL`; basionym-only without years → `ZOOLOGICAL`. Filius on an *ex* author (`Baker f. ex Rose`) is *not* a code signal — only the validating author counts.
- **PublishedIn-derived years are code-neutral.** A year extracted from a stripped publishedIn reference (`Author in Source, 1845`, `Author., Title (1792)`, `Author. Annals of … 1988 …`) is the publication year of the work, not an author-year citation. It propagates onto the combination authorship for rendering but is *not* used as a code signal — the same year may attach to a zoological, botanical, or bacteriological name. `ParseContext.pendingYearFromPublication` carries the flag; `Pipeline.run` applies the year *after* code inference in that case so it never triggers a spurious `ZOOLOGICAL`. Only a year that came directly off the author span (no publishedIn extraction) is the zoological author-year and is applied *before* inference.
- **Abbreviation is a hint, not a code signal.** Botanical citations *tend* to use abbreviated authors (`Rich.`, `Müll.Arg.`) and abbreviated journals (`Symb. Antill.`, `Prodr.`), while zoological citations *tend* to spell things out, but both forms appear in both codes. Don't infer the code from whether the author span or the publication ref is abbreviated.
- **Zoological trinomials default to `SUBSPECIES`.** ICZN doesn't use rank markers for subspecies — `Vulpes vulpes silaceus Miller, 1907` is by convention a subspecies, not the generic `INFRASPECIFIC_NAME`. `Assemble.finish` upgrades a parsed `INFRASPECIFIC_NAME` to `SUBSPECIES` whenever the resolved code is `ZOOLOGICAL` (caller-supplied or inferred). For botanical names the absence of an explicit `subsp.`/`var.`/`f.` marker keeps the rank as `INFRASPECIFIC_NAME` because botany requires the marker.
- **Caller-supplied rank that doesn't fit the parsed structure.** A monomial parsed as a uninomial but the caller asked for a species-level-or-below rank (`SPECIES`, `SUBSPECIES`, `CULTIVAR`, `GREX`, …) is promoted to genus + indet placeholder: `INFORMAL` type, `INDETERMINED` warning, no authorship retained. The caller's rank wins and pins any rank-restricted code (`CULTIVAR` → `CULTIVARS`). Conversely, when the caller asks for a rank *higher than* the parsed structure (`Polygonum alba` + `GENUS`) the parsed structure is kept but the rank is forced to the caller's value with `INFORMAL` + doubtful + `RANK_MISMATCH` warning.
- **`sp.` between two lower epithets is a misspelled `ssp.`** A bare `sp.` after a species epithet and before another lower epithet (`Vulpes vulpes sp. silaceus`) is silently treated as the subspecies marker — almost always a typo for `ssp.`.
- **Taxonomic-concept references** are stripped to the `taxonomicNote` (sensu) field. Recognised forms (in `StripAndStash.TAX_NOTE` and the colon/parenthesis helpers): `auct.` / `auctt.` (lower-cased on output regardless of input case), `sensu …`, `sec.`/`s.l.`/`s.str.`/`s.lat.`/`s.ampl.`, `ss.`/`ss. auct. europ.`, `nec` / `non` / `not` / `non. (…)`, `emend. <Author>`, `fide <Author>`, `according to <Author>`, `excl. var.`, parenthesised `(non/nec/not …, YYYY)` homonym citations at end, and `:Author, YYYY` colon-trailing concept references (`Vespa emarginata Linnaeus, 1758: Fabricius, 1793` → sensu `Fabricius, 1793`). The colon form *requires* the trailing year so the simpler sanctioning-author form `L. : Fr.` isn't swept into sensu.
- **Embedded nomenclatural notes inside an IPNI publication ref** (e.g. `Phytologia 1: 204, in obs., pro syn. (1937)`) are extracted into `nomenclaturalNote` and removed from the ref text. Recognised: `pro syn.`, `in obs., pro syn.`, `nom. …`, `comb. …`, `orth. …` directly before the year parens.
- **Standalone "delete" / "[delete]" / "[none]" / leading "non "** marks the input unparsable as `OTHER` — these are checklist housekeeping artefacts, not names.
- **`<Unspecified Agent>`-style angle-bracket placeholders** (and `Not applicable` / `Not given` / `Not known` / `Not recorded`) trailing the name are stripped silently with `AUTHORSHIP_REMOVED` (and, for the angle-bracket form, `UNUSUAL_CHARACTERS` plus a `doubtful` flag).
- **Trailing synonymy reference** in square brackets (`[= Grislea L. 1753]`) is parked as `unparsed` with the `doubtful` flag so the leading name still parses cleanly.
- **`nom.` nomenclatural note as a botanical signal.** A `nom.cons.` / `nom.illeg.` / `nom.nud.` / etc. note combined with a comb author and *no* year, *no* explicit infraspecific marker, and *no* ex-author chain, infers `BOTANICAL`. The richer signals (year → ZOOLOGICAL, marker, ex-authors) all take precedence and disable this fallback.
- **Explicit infraspecific marker without authorship but with a homonym citation** (`Puntius arulius subsp. tambraparniei (non Silas 1954)`) infers `BOTANICAL` — zoological trinomials never use rank markers, so a marker plus a parenthesised `(non …)` reads as a botanical citation.
- **`?` placeholder vs virus precedence.** `Preflight` runs the explicit placeholder keyword check first (`uncultured`, `unidentified`, …), then the `VIRUS` pattern, then the leading-`?` placeholder check. That ordering keeps `? circular satellites` (a satellite virus) from being misclassified as a `?`-prefixed placeholder.
- **Apostrophe / hyphen in author tokens** (`L.'t Mannetje`, `M'Coy`, `B.-E.van Wyk`) are preserved through the author-buffer chain. `appendSpace` skips emitting a space when `cur` ends in `.`, `-`, or `'` so the punctuation glues directly to the next token.
