# GBIF Name Parser API

The data model and parser contract for scientific names — `ParsedName`,
`Authorship`, `Rank`, `NomCode`, `NameType`, the `NameParser` interface and the
`ParseResult` it returns — plus name formatting and rank/unicode utilities.

As of **5.0.0 this repository is API-only**: it ships the single
`name-parser-api` artifact. The parsing engine has been reimplemented in Rust
(with the full regression corpus), and is consumed through this interface via a
native binding. The previous Java implementation (`NameParserImpl` and the
`name-parser` / `name-parser-cli` modules) lives on the **`4.x` branch** for
maintenance; see [Migrating from 4.x to 5.0](#migrating-from-4x-to-50).

## What's in here

| Type | Purpose |
|---|---|
| `ParsedName`, `ParsedAuthorship`, `Authorship` | the structured name / authorship model |
| `Rank`, `NomCode`, `NameType`, `NamePart` | the controlled vocabularies |
| `NameParser` | the parser contract — returns a `ParseResult` |
| `ParseResult` (`Parsed` \| `Unparsable`) | the parse outcome; see below |
| `UnparsableNameException` | unchecked; raised only by `ParseResult.orElseThrow()` |
| `NameFormatter` | render a `ParsedName` / any `CombinedAuthorshipIF` back to a string |
| `RankUtils`, `UnicodeUtils` | rank relationships and unicode/homoglyph helpers |

## Library use

```xml
<dependency>
  <groupId>org.gbif</groupId>
  <artifactId>name-parser-api</artifactId>
  <version>5.0.0</version>
</dependency>
```

`parse(...)` never throws for an unparsable name — it returns a sealed
`ParseResult` that is either a `Parsed` name or an `Unparsable` classification
(virus, hybrid formula, placeholder, BOLD BIN, …). Both variants expose
`type()` and `code()`, so you can classify a failure without catching anything:

```java
NameParser parser = new NameParserRust(); // the native (Rust-backed) implementation

switch (parser.parse("Vulpes vulpes silaceus Miller, 1907", null, null, null)) {
  case ParseResult.Parsed p     -> index(p.name());          // a ParsedName
  case ParseResult.Unparsable u -> record(u.type(), u.code()); // e.g. FORMULA / VIRUS
}
```

It composes in streams:

```java
List<ParsedName> parsed = names.stream()
    .map(n -> parser.parse(n, null, rank, code))
    .flatMap(r -> r.parsed().stream())
    .toList();
```

…and offers an opt-in fail-fast path for callers that want it:

```java
ParsedName pn = parser.parse(name).orElseThrow(); // throws unchecked UnparsableNameException
```

Only need the model or the formatter (no parsing)? Depend on this artifact and
use `ParsedName` / `NameFormatter` directly — no implementation required.

## The parsing engine (Rust)

From 5.0 the reference implementation lives in a separate project,
[**gbif/name-parser-rust**](https://github.com/gbif/name-parser-rust). The
parser core is a Rust crate (`nameparser`), which also carries the full
regression corpus ported from the old Java suite, and is exposed through several
bindings — a C-ABI cdylib (`nameparser-ffi`) and a Python module
(`nameparser-py`).

Java callers get a `NameParser` from that project's Panama binding:
`org.gbif.nameparser.rust.NameParserRust` implements
`org.gbif.nameparser.api.NameParser` from *this* module by downcalling the Rust
cdylib in-process via `java.lang.foreign` (FFM/Panama, stable since JDK 22 — no
`--enable-preview`). Each `parse` marshals across the FFI boundary and rebuilds
a `ParsedName` from a flat binary struct. Because `java.lang.foreign` needs a
modern JDK, that binding targets **JDK 22+** (GBIF's CI runs it on 25) and is
built and released independently of this Java-17 API module — which is exactly
why the API stays on 17: model- and formatter-only consumers keep the broad
baseline, while only the native binding requires the newer JDK.

## Migrating from 4.x to 5.0

5.0 keeps the same model but changes the parser contract and drops the bundled
Java engine.

* **`parse(...)` returns `ParseResult`, not `ParsedName`, and no longer throws.**
  Replace `try { ParsedName pn = parser.parse(…); } catch (UnparsableNameException e) { … }`
  with a `switch` over `Parsed | Unparsable`, or `parser.parse(…).parsed()` /
  `.orElseThrow()`. `type()` and `code()` are on both variants, so failure
  classification no longer needs the exception.
* **`parseAuthorship(...)` returns `Optional<ParsedAuthorship>`** instead of
  throwing.
* **`UnparsableNameException` is now unchecked** (`extends RuntimeException`) and
  is only raised by `ParseResult.orElseThrow()`.
* **The Java parser is gone from this repo.** `new NameParserImpl()` no longer
  exists in 5.0 — obtain a `NameParser` from the native binding instead. If you
  need the pure-Java engine, stay on the **`4.x` branch** (`name-parser` 4.2.x),
  which keeps the old throwing API and `NameParserImpl`.

Model/vocabulary changes introduced across the 4.x line (still current in 5.0)
are documented in the `4.x` branch README — notably `NameType.VIRUS`/`OTU`
removal (viruses now carry `code = VIRUS`), `Rank.DIVISION` → `DIVISION_ZOOLOGY`
plus a new `DIVISION_BOTANY`, imprint years moving to `Authorship`, and the
`CombinedAuthorshipIF` / `publishedInYear` / generic-&-specific-authorship
additions.

## Build

`mvn install` from the repo root — a single-module Java 17 build.

## License

Apache 2.0.
