# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repo is (5.0.0+)

As of 5.0.0 this is an **API-only, single-module** project: it ships just
`name-parser-api` — the scientific-name data model, the parser *contract*, and
name/rank/unicode utilities. **There is no Java parser implementation here
anymore.** The parsing engine was reimplemented in Rust; this module only
defines the types a parser produces and the interface it implements.

- **Parsing engine:** [github.com/gbif/name-parser-rust](https://github.com/gbif/name-parser-rust)
  — the `nameparser` Rust crate (which also holds the full regression corpus ported
  from the old Java test suite), plus a C-ABI cdylib (`nameparser-ffi`), a Python
  module (`nameparser-py`), and a Java/Panama binding
  (`org.gbif.nameparser.rust.NameParserRust`, JDK 22+) that implements the
  `NameParser` interface defined *here*. All questions about *parsing behaviour*
  (how a given name is tokenised, authorship parsed, code inferred, …) belong in
  that repo, not this one.
- **Java 4.x line:** the previous pure-Java implementation (`NameParserImpl` + the
  `name-parser` / `name-parser-cli` modules, throwing `NameParser` API) lives on the
  **`4.x` branch**, cut from the `name-parser-4.2.0` release, for maintenance fixes.
  The older regex parser (`NameParserGBIF`) is on **`3.x`**.

## Build & Test

Single-module Maven build, Java 17. From the repo root:

- `mvn install` — build, test and install `name-parser-api`
- `mvn test` — run the API test suite (`RankTest`, `RankUtilsTest`, `NameFormatterTest`,
  `UnicodeUtilsTest`, `ParsedNameTest`, `AuthorshipTest`, `NameTypeTest`)
- `mvn -Dtest=NameFormatterTest test` — a single test class

Artifacts publish to the GBIF Nexus (`repository.gbif.org`); releases are driven by
`maven-release-plugin` against the `motherpom` parent.

## The model & contract

Everything is in `org.gbif.nameparser.api` (model + contract) and
`org.gbif.nameparser.util` (helpers):

- **`ParsedName`** (extends `ParsedAuthorship`) — the structured name: uninomial/genus/
  subgenus/epithets, `rank`, `code`, `type` (`NameType`), authorships, notes/flags, and a
  `State` (`COMPLETE` / `PARTIAL` / `NONE`).
- **`ParsedAuthorship`** / **`Authorship`** — combination & basionym authors, ex-authors, year,
  `imprintYear`, sanctioning author, `publishedIn` (+ structured `publishedInYear`).
- **`Rank`**, **`NomCode`**, **`NameType`**, **`NamePart`** — the controlled vocabularies.
  `NameType.isParsable()` is `SCIENTIFIC || INFORMAL`; the rest (`FORMULA`, `PLACEHOLDER`,
  `OTHER`) are the unparsable classifications.
- **`NameParser`** — the contract. `parse(name, authorship, rank, code)` returns a
  **`ParseResult`** and never throws for an unparsable name. `parseAuthorship(...)` returns
  `Optional<ParsedAuthorship>`.
- **`ParseResult`** — a sealed interface, `Parsed(ParsedName)` | `Unparsable(type, code, name)`.
  `type()` and `code()` are on **both** variants (so a failure can be classified without a
  cast or a catch); `parsed()` returns `Optional<ParsedName>`; `orElseThrow()` is the opt-in
  fail-fast path. This mirrors the Rust parser's `Result<ParsedName, Unparsable>` so both
  implementations share one contract.
- **`UnparsableNameException`** — **unchecked** (`extends RuntimeException`); only raised by
  `ParseResult.orElseThrow()`, and bridges to/from `ParseResult.Unparsable`.
- **`NameFormatter`** — renders a `ParsedName` (or any `CombinedAuthorshipIF`) back to a
  string. **`RankUtils`** — rank relationships/markers. **`UnicodeUtils`** — unicode/homoglyph
  helpers (loads `unicode/homoglyphs.txt`).

When changing the model or the contract, keep it in lockstep with the Rust binding in
`name-parser-rust` — a field/enum/signature change here is a wire-format or interface change
there. Model behaviour is verified by the small API test suite listed above; parsing behaviour
is verified in the Rust repo.

## Conventions

- Java 17 source/target (kept deliberately low so model-/formatter-only consumers — e.g.
  `gbif-api`, `taxon-ws`, the CoL backend — aren't forced onto a newer JDK; only the native
  Rust binding needs JDK 22+).
- `var` is used freely; no enforcement either way.
- Logging is SLF4J; tests pull in `logback-classic` (`src/test/resources/logback-test.xml`).
- Apache 2.0 license header on every source file.
