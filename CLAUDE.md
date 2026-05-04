# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

This branch (`antlr-parser`) is **experimental**: the core structural match in the parser uses an ANTLR grammar instead of the previous large `NAME_PATTERN` regex. Pre-cleaning, normalization, NameType detection (virus/hybrid/OTU/phrase/cultivar/placeholder), and post-cleanup remain regex/Java. Only `ParsingJob.parseNormalisedName` and `AuthorshipParsingJob.parseNormalisedAuthorship` were swapped.

## Build & Test Commands

```bash
# Build all modules (also generates ANTLR sources from grammars in src/main/antlr4)
mvn clean install

# Build skipping tests
mvn clean install -DskipTests

# Run all tests
mvn test

# Run tests in a single module
mvn test -pl name-parser

# Run a specific test class
mvn test -pl name-parser -Dtest=NameParserAntlrTest

# Run a single test method
mvn test -pl name-parser -Dtest=NameParserAntlrTest#testParseSpecies
```

Dependencies are fetched from the GBIF Maven repository (`https://repository.gbif.org/content/groups/gbif`), not Maven Central.

## Module Architecture

The project is a Maven multi-module project with two modules:

- **`name-parser-api`** — Data model and parser interface only; minimal dependencies. Contains `ParsedName`, `ParsedAuthorship`, `Rank`, `NameType`, `NomCode`, `NameParser` interface, `NameFormatter`, and `RankUtils`. Publishes a test-jar (`classifier=tests`) that other modules import to share test utilities.

- **`name-parser`** — The main implementation. `NameParserAntlr` implements `NameParser` and runs parsing in a background thread pool to enforce timeouts. `ParsingJob` orchestrates the phases; the structural match step calls into the ANTLR adapter under `org.gbif.nameparser.antlr` (grammar sources under `src/main/antlr4/org/gbif/nameparser/antlr`). `ParserConfigs` fetches known name overrides from `https://api.checklistbank.org/parser/name/config` and caches them in memory — it also supports loading from a custom URL. Resource files `blacklist-epithets.txt` and `latin-endings.txt` are loaded from the classpath at startup.

## Key Design Points

**Timeout mechanism**: `NameParserAntlr` wraps each `ParsingJob` in a `Future` submitted to a `ThreadPoolExecutor`. Two interruption hooks cooperate so `Future.cancel(true)` can yank a stuck parse: `InterruptibleCharSequence` wraps regex inputs and `InterruptibleCharStream` wraps ANTLR inputs — both check `Thread.currentThread().isInterrupted()` on every character read.

**Parser configs / overrides**: `ParserConfigs` holds a map of normalized name strings → `ParsedName` (and authorship) overrides that bypass the parsing pipeline entirely. These are loaded from ChecklistBank's REST API at startup and can be reloaded at runtime.

**`ParsedName` structure**: Extends `ParsedAuthorship`. Fields map directly to the parts of a Linnean name: `uninomial` (genus/family/higher), `genus`, `infragenericEpithet`, `specificEpithet`, `infraspecificEpithet`, plus `rank`, `code`, authorship fields, `state` (`COMPLETE`/`PARTIAL`/`NONE`), `type` (`NameType`), and a set of `Warnings`.

**Preferred `parse` method**: The two-argument overloads taking only name or name+rank are deprecated. Always use `parse(scientificName, rank, nomCode)`.

## Testing

The primary test class is `NameParserAntlrTest` in `name-parser`. It sets a very long timeout (99999999 ms) when `DEBUG=true` so tests don't time out in a debugger. The `NameAssertion` helper class provides a fluent assertion API for parsed names. `NameParserAntlrTimeoutTest` is the regression check that the ANTLR interrupt hook actually works — if it hangs, `InterruptibleCharStream` is broken.
