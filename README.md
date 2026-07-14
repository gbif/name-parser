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
| `ParseResult` (`Parsed` \| `Informal` \| `Unparsable`) | the three-way parse outcome; see below |
| `UnparsableNameException` | unchecked; raised only by `ParseResult.orElseThrow()` |
| `NameFormatter` | render a `ParsedName`, an `Informal`, or a whole `ParseResult` (also any `CombinedAuthorshipIF`) back to a string |
| `RankUtils`, `UnicodeUtils` | rank relationships and unicode/homoglyph helpers |

## Library use

```xml
<dependency>
  <groupId>org.gbif</groupId>
  <artifactId>name-parser-api</artifactId>
  <version>5.0.0</version>
</dependency>
```

`parse(...)` never throws — it returns a sealed, **three-way** `ParseResult`:

* **`Parsed`** — a fully structured `ParsedName` (its `state()` may be `COMPLETE` or `PARTIAL`).
* **`Informal`** — a semistructured name: a real supraspecific `taxon` carrying a provisional,
  non-code designation instead of a determined species epithet — a molecular provisional species
  (`Rhizobium sp. RMCC TR1811`), a numbered placeholder (`Allium sp. 1`), or an informal group
  (`Bartonella group`). It is a flat `taxon` / `taxonRank` / `rank` / `phrase` / `code` and carries
  **no** `ParsedName` — the anchor is unvalidated, so it is never mislabelled as a determined genus.
  Names that keep a species epithet — including cf./aff. and infraspecific-indeterminate ones —
  stay `Parsed`, so their `specificAuthorship` (which a flat anchor could not hold) survives.
* **`Unparsable`** — not a scientific name at all: a virus, a hybrid formula, a placeholder, or a
  machine identifier such as a BOLD BIN / UNITE SH / OTU / culture-collection accession. `type()`
  classifies it — `FORMULA`, `PLACEHOLDER`, `IDENTIFIER`, or `OTHER`.

`type()` and `code()` are available on all three variants, so you can classify without catching
anything:

```java
NameParser parser = new NameParserRust(); // the native (Rust-backed) implementation — see below

switch (parser.parse("Rhizobium sp. RMCC TR1811", null, null, null)) {
  case ParseResult.Parsed p     -> index(p.name());                     // a full ParsedName
  case ParseResult.Informal i   -> indexInformal(i.taxon(), i.phrase()); // "Rhizobium" + "RMCC TR1811"
  case ParseResult.Unparsable u -> record(u.type(), u.code());          // type FORMULA/PLACEHOLDER/IDENTIFIER/OTHER, code e.g. VIRUS
}
```

Any variant round-trips back to a string via `NameFormatter.canonical(result)` — the reconstructed
name for `Parsed`, the informal name (`Rhizobium sp. RMCC TR1811`) for `Informal`, the verbatim
input for `Unparsable` — or call `NameFormatter.canonical(informal)` on an `Informal` directly.

It composes in streams:

```java
List<ParsedName> parsed = names.stream()
    .map(n -> parser.parse(n, null, rank, code))
    .flatMap(r -> r.parsed().stream()) // keeps only Parsed; Informal + Unparsable have an empty parsed()
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
modern JDK, that binding targets **JDK 25+** and is
built and released independently of this Java-17 API module — which is exactly
why the API stays on 17: model- and formatter-only consumers keep the broad
baseline, while only the native binding requires the newer JDK.

### Depending on the Rust binding (to actually parse)

This api artifact carries **no parser** on its own. For a working `NameParser`, add the native
binding — `org.gbif.nameparser:name-parser-rust`, from
[**gbif/name-parser-rust**](https://github.com/gbif/name-parser-rust) — which pulls this
`name-parser-api` in transitively. It ships as a thin main JAR plus one native classifier JAR per
platform (netty-tcnative style), so you download only your own architecture's cdylib. Requires
**JDK 25+**.

```xml
<build><extensions>
  <!-- resolves ${os.detected.classifier}: linux-x86_64, osx-aarch_64, windows-x86_64, … -->
  <extension>
    <groupId>kr.motd.maven</groupId>
    <artifactId>os-maven-plugin</artifactId>
    <version>1.7.1</version>
  </extension>
</extensions></build>

<dependencies>
  <dependency>                    <!-- thin main JAR: Java + FFM loader (brings name-parser-api) -->
    <groupId>org.gbif.nameparser</groupId>
    <artifactId>name-parser-rust</artifactId>
    <version>0.1.0-SNAPSHOT</version>
  </dependency>
  <dependency>                    <!-- your platform's native cdylib -->
    <groupId>org.gbif.nameparser</groupId>
    <artifactId>name-parser-rust</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <classifier>${os.detected.classifier}</classifier>
  </dependency>
</dependencies>
```

The binding is published to GBIF's Nexus; if your build doesn't already resolve from it, add:

```xml
<repositories>
  <repository>
    <id>gbif-all</id>
    <url>https://repository.gbif.org/content/groups/gbif</url>
  </repository>
</repositories>
```

`0.1.0-SNAPSHOT` is auto-deployed on every push to `main` (a released `0.1.0` will follow); the
binding tracks the Rust engine's own version line, independent of this api's `5.0.0`. Then just
construct it — no other wiring, and the same `NameParser` interface as before:

```java
NameParser parser = new org.gbif.nameparser.rust.NameParserRust();
```

## Migrating from 4.x to 5.0

5.0 keeps the same model but changes the parser contract and drops the bundled
Java engine.

* **`parse(...)` returns a three-way `ParseResult`, not `ParsedName`, and no longer throws.**
  Replace `try { ParsedName pn = parser.parse(…); } catch (UnparsableNameException e) { … }`
  with a `switch` over `Parsed | Informal | Unparsable`, or `parser.parse(…).parsed()` /
  `.orElseThrow()`. `type()` and `code()` are on all three variants, so failure
  classification no longer needs the exception.
* **Informal / semistructured names are now their own `Informal` result, not a `ParsedName`.**
  Names that 4.x returned as a `ParsedName` with `type = INFORMAL` and no species epithet — a
  supraspecific taxon plus a provisional designation (`Genus sp. <tag>`, `Bartonella group`) — now
  come back as a flat `ParseResult.Informal` (`taxon` / `taxonRank` / `rank` / `phrase` / `code`)
  that carries no `ParsedName`. A `switch` that previously handled only `Parsed` and `Unparsable`
  must add the `Informal` arm. (Names that keep a species epithet — cf./aff., strain, infraspecific
  indet — stay `Parsed` as before.)
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
