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
