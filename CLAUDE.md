# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test

Maven multi-module project, Java 17. From the repo root:

- `mvn install` — build & test all three modules (default goal of `name-parser` is `install`)
- `mvn -pl name-parser test` — run tests for the parser module
- `mvn -pl name-parser -Dtest=NameParserImplTest test` — run a single test class
- `mvn -pl name-parser -Dtest=NameParserImplTest#squareGenera test` — run a single test method
- `mvn -pl name-parser-cli -am install` — build the CLI module and its dependencies

Artifacts publish to the GBIF Nexus (`repository.gbif.org`); releases are driven by `maven-release-plugin` against the `motherpom` parent (currently v61).

## Module layout

The repo is a parent POM over three modules. The data model and the parser are a dependency chain (`name-parser-api` ← `name-parser`); the CLI is a separate consumer (`name-parser` ← `name-parser-cli`).

- **`name-parser-api`** — pure model + interface module. Defines `ParsedName`, `ParsedAuthorship`, `Rank`, `NomCode`, `NameType`, `Authorship`, `Warnings`, and the `NameParser` interface. Also ships `NameFormatter`, `RankUtils`, and `UnicodeUtils` (loads `unicode/homoglyphs.txt`). No dependency on the parser implementation — downstream callers can depend on this alone for the data model.
- **`name-parser`** — the GBIF implementation. Single public entry point: `NameParserImpl` (implements the api `NameParser`). It is a thin delegate to a staged `pipeline` (see "How parsing works"); the older monolithic `NameParserGBIF`/`ParsingJob` regex parser has been fully replaced.
- **`name-parser-cli`** — a standalone command-line tool (`Main`, `ParseCli`, `CompareCli`, `BenchmarkCli`) that batch-parses names and reads/writes CoLDP and CSV via the `cli.io` package. A consumer of `name-parser`, not part of the parsing core.

## How parsing works

`NameParserImpl` is the only public parser class; it delegates to `pipeline.Pipeline.run(...)`. Behaviours that aren't obvious from any single file:

- **Synchronous and thread-safe; no built-in timeout.** Each parse runs on the calling thread — there is no thread pool, `Future`, or `InterruptibleCharSequence` (the old `NameParserGBIF` had these; the pipeline dropped them). `NameParserImpl` is stateless, so a single instance can be shared across threads. **Callers must impose their own timeout** if they parse untrusted input — see the catastrophic-backtracking note below.
- **Staged pipeline over a shared `ParseContext`.** `Pipeline.run` normalises unicode quotes, splits glued phrase names, then runs ordered stages, each mutating the shared mutable `ParseContext`:
  1. `Preflight` — regex gate that throws `UnparsableNameException` for viruses, hybrid formulas, OTU/specimen codes, and placeholders.
  2. `StripAndStash` — removes annotations (nom/taxonomic notes, publishedIn refs, imprint years, homoglyphs, quoted monomials, missing-genus placeholders, …) and stashes them on the `ParsedName`. Its `run` is an explicit ordered list of named strip steps — the order is load-bearing.
  3. `token.Tokenizer` — single-pass tokeniser producing a flat `Token` list (WORD/NUMBER/paren/comma/dot/hybrid-mark/… with offsets).
  4. `AuthorshipSplit` — finds the boundary between the name section and the authorship section.
  5. `NameTokens` — classifies the name tokens into uninomial/genus/subgenus/epithets + rank markers.
  6. `AuthorshipParser` — parses author lists, ex authors, years, sanctioning author, basionym/combination, and author inversion.
  7. `CodeInference` — infers the `NomCode` from authorship shape and notes (all code-setting logic lives here).
  8. `Assemble` — final invariants: rank defaults, zoological-trinomial → SUBSPECIES, suffix-based rank, blacklisted-epithet flagging, quoted-monomial re-wrap.
- **Two-phase authorship parsing.** `parseAuthorship(...)` (api default method) invokes the full parser with a synthetic `"Abies alba <authorship>"` and pulls authorship out of the result. A separately supplied `authorship` argument is tokenised and parsed independently, then merged onto the name (`Pipeline.applyAuthorship`).
- **Result states.** A successful parse returns a `ParsedName` with `State.COMPLETE` or `State.PARTIAL`. Names that cannot be expressed by the model (viruses, hybrid formulas, blank input) throw `UnparsableNameException` carrying a `NameType` — callers must catch it.

## Working with the pipeline

The parsing logic is split across small single-responsibility classes in `org.gbif.nameparser.pipeline` (plus the `token` package). When changing behaviour:

- Regex constants use Unicode classes (`\p{Lu}`, `\p{Ll}`) and explicit letter-set constants — be careful not to accidentally restrict the alphabet, and prefer linear-time patterns (the parser has no timeout guard).
- **`StripAndStash.run` is the ordered list of strip steps.** Each step is its own named method that takes and returns the working string; add a new step by inserting it at the right position in `run` (order matters). **`CodeInference`** owns all `NomCode`-setting heuristics. **`Assemble.finish`** owns final rank/code invariants.
- Behaviour is verified almost entirely through `NameParserImplTest` (the canonical regression suite) and `NameParserGnaTest`, using the `NameAssertion` fluent helper. Add a test there for any parsing change before tweaking code.
- Auxiliary test corpora live in `name-parser/src/test/resources/` (`doubtful.txt`, `hybrids.txt`, `other.txt`, `otu.txt`, `placeholder.txt`, `viruses.txt`, `names-with-authors.txt`).
- Resource files in `name-parser/src/main/resources/nameparser/`: `blacklist-epithets.txt` (loaded by `pipeline.BlacklistedEpithets` to flag doubtful epithets). `latin-endings.txt` and `prefix-epithet-binomials.tsv` are present but no longer loaded by the pipeline — treat them as vestigial until re-wired.

## Conventions

- Java 17 source/target; no `var` is enforced but the codebase uses it freely in newer files.
- Logging is SLF4J; tests pull in `logback-classic`. Test logging config: `name-parser/src/test/resources/logback-test.xml`.
- Apache 2.0 license header is on every source file.

## Authorship conventions

- **Surname-first authors with all-caps trailing initials** are flipped to initial-prefixed form. `Walker F` → `F.Walker`, `Balsamo M Fregni E Tongiorgi MA` → `M.Balsamo, E.Fregni, M.A.Tongiorgi`. The flip only applies when the buffer ends with a real surname (not just particles): `H.da C.` followed by `Monteiro` keeps the initials-particle-initial chain intact and renders `H.da C.Monteiro …`. Isolated `Zhang F` inside a separator-bounded segment (e.g. `Zhang F & Pan Z-X`) also flips to `F.Zhang`.
- **Hyphenated initials** (`Y.-j. Wang`, `C.-K. Yang`) preserve both the hyphen AND the input case of the single-letter follow-up: `Y.-j.Wang`, `C.-K.Yang`. Don't uppercase the post-hyphen letter.
- **Code inference signals** (in priority order, in `CodeInference.fromAuthorship`; the note/marker fallbacks are in `CodeInference.infer`) — sanctioning author → `BOTANICAL`; `(BasAuthor) RecombAuthor, year` with an explicit infraspecific marker (subsp./var./f.) → `BOTANICAL` (botanical recombination, year is the publication year); any other year on the author span → `ZOOLOGICAL`; `f.`/`fil.`/`filius` suffix on a non-ex author *without any year* → `BOTANICAL` (zoological literature does use filius for father/son, e.g. *Lacerta agilis* Linnaeus f., 1789, but those cases always carry a year and are caught by the year rule first); basionym + combination authors without years → `BOTANICAL`; basionym-only without years → `ZOOLOGICAL`. Filius on an *ex* author (`Baker f. ex Rose`) is *not* a code signal — only the validating author counts.
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
