# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

This branch (`antlr-parser`) is **experimental**: the core structural match in the parser uses an ANTLR grammar instead of the previous large `NAME_PATTERN` regex. Pre-cleaning, normalization, NameType detection (virus/hybrid/OTU/phrase/cultivar/placeholder), and post-cleanup remain regex/Java. Only `ParsingJob.parseNormalisedName` and `AuthorshipParsingJob.parseNormalisedAuthorship` were swapped.

## Build & Test

Maven multi-module project, Java 11. From the repo root:

- `mvn install` — build & test all modules (also generates ANTLR sources from grammars in `src/main/antlr4`)
- `mvn install -DskipTests` — build skipping tests
- `mvn -pl name-parser test` — run tests for a single module
- `mvn -pl name-parser -Dtest=NameParserAntlrTest test` — run a single test class
- `mvn -pl name-parser -Dtest=NameParserAntlrTest#testParseSpecies test` — run a single test method
- `mvn -pl name-parser-v1 -am install` — build a downstream module and its dependencies

Artifacts publish to the GBIF Nexus (`repository.gbif.org`); releases are driven by `maven-release-plugin` against the `motherpom` parent.

## Module layout

The repo is a parent POM (`name-parser-motherpom`) over three modules. Dependencies flow strictly: `name-parser-api` ← `name-parser` ← `name-parser-v1`.

- **`name-parser-api`** — pure model + interface module. Defines `ParsedName`, `ParsedAuthorship`, `Rank`, `NomCode`, `NameType`, `Authorship`, `Warnings`, and the `NameParser` interface. Also ships `NameFormatter`, `RankUtils`, and `UnicodeUtils` (loads `unicode/homoglyphs.txt`). Publishes a test-jar (`classifier=tests`) that other modules import to share test utilities. No dependency on the parser implementation — downstream callers can depend on this alone for the data model.
- **`name-parser`** — the main parser implementation. Public entry point: `NameParserAntlr` (this branch) which implements `NameParser` and runs each parse in a background thread pool to enforce timeouts. `ParsingJob` orchestrates the phases; the structural match step calls into the ANTLR adapter under `org.gbif.nameparser.antlr` (grammar sources under `src/main/antlr4/org/gbif/nameparser/antlr`). `ParserConfigs` fetches known name overrides from `https://api.checklistbank.org/parser/name/config` and caches them in memory — it also supports loading from a custom URL. Resource files `blacklist-epithets.txt` and `latin-endings.txt` are loaded from the classpath at startup.
- **`name-parser-v1`** — adapter that wraps the GBIF parser and exposes the legacy `org.gbif.api.service.checklistbank.NameParser` interface (from `gbif-api`), translating between the new `org.gbif.nameparser.api.ParsedName` and the v1 `org.gbif.api.model.checklistbank.ParsedName`. Touch this when v1 callers need a behavior change that's already in the underlying parser.

## Key Design Points

**Timeout mechanism**: `NameParserAntlr` wraps each `ParsingJob` in a `Future` submitted to a `ThreadPoolExecutor`. Two interruption hooks cooperate so `Future.cancel(true)` can yank a stuck parse: `InterruptibleCharSequence` wraps regex inputs and `InterruptibleCharStream` wraps ANTLR inputs — both check `Thread.currentThread().isInterrupted()` on every character read. **Reuse a `NameParserAntlr` instance** and call `close()` on shutdown; constructing one per parse spins up a new thread pool.

**Two-phase parsing**: `parseAuthorship(...)` actually invokes the full parser with a synthetic `"Abies alba <authorship>"` and pulls authorship out of the result.

**Manual overrides**: Before parsing, `NameParserAntlr` consults `ParserConfigs` for a pre-canned `ParsedName`/`ParsedAuthorship` keyed by a normalized form of the input. Overrides can be added programmatically via `parser.configs().add(...)` or bulk-loaded from the ChecklistBank API via `ParserConfigs.loadFromCLB()` / `load(URI)`.

**Result states**: A successful parse returns a `ParsedName` with `State.COMPLETE` or `State.PARTIAL`. Names that cannot be expressed by the model (viruses, hybrid formulas, blank input) throw `UnparsableNameException` carrying a `NameType` — callers must catch it.

**`ParsedName` structure**: Extends `ParsedAuthorship`. Fields map directly to the parts of a Linnean name: `uninomial` (genus/family/higher), `genus`, `infragenericEpithet`, `specificEpithet`, `infraspecificEpithet`, plus `rank`, `code`, authorship fields, `state` (`COMPLETE`/`PARTIAL`/`NONE`), `type` (`NameType`), and a set of `Warnings`.

**Preferred `parse` method**: The two-argument overloads taking only name or name+rank are deprecated. Always use `parse(scientificName, rank, nomCode)`.

## Testing

The primary test class is `NameParserAntlrTest` in `name-parser`. It sets a very long timeout (99999999 ms) when `DEBUG=true` so tests don't time out in a debugger. The `NameAssertion` helper class provides a fluent assertion API for parsed names. `NameParserAntlrTimeoutTest` is the regression check that the ANTLR interrupt hook actually works — if it hangs, `InterruptibleCharStream` is broken.

Auxiliary test corpora live in `name-parser/src/test/resources/` (`doubtful.txt`, `hybrids.txt`, `nonames.txt`, `occurrence-names.txt`, `placeholder.txt`, `unparsable.txt`, `viruses.txt`, `names-with-authors.txt`, `all-names.txt`). `TODO-names.txt` tracks known-failing inputs.

`NameParserGnaTest` runs a large GNA corpus (`all-names.txt`) and is `@Ignore`d by default — most of those inputs still fail to parse.

Resource files driving parser behavior are in `name-parser/src/main/resources/nameparser/`: `blacklist-epithets.txt` (flags doubtful names) and `latin-endings.txt` (common 4-char Latin suffixes — sorted by *reverse* of the ending; see the resource README before re-sorting).

## Conventions

- Java 11 source/target; no `var` is enforced but the codebase uses it freely in newer files.
- Logging is SLF4J; tests pull in `logback-classic`. Test logging config: `name-parser/src/test/resources/logback-test.xml`.
- Apache 2.0 license header is on every source file.
