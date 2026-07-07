# GBIF Name Parser

A library and command-line tool that parses scientific names — including the
authorship, rank, hybrid markers and nomenclatural notes — into a structured
[`ParsedName`](name-parser-api/src/main/java/org/gbif/nameparser/api/ParsedName.java)
model.

## Modules

| Module | Purpose |
|---|---|
| `name-parser-api`  | Pure model + interface module: `ParsedName`, `Authorship`, `Rank`, `NomCode`, `NameType`, the `NameParser` interface, plus formatter / Unicode utilities. Depend on this if you only need the data model. |
| `name-parser`      | The parser implementation. Single public entry point: `org.gbif.nameparser.NameParserImpl`. |
| `name-parser-cli`  | Command-line tools (`parse`, `compare`, `benchmark`, `validate`) wrapping the parser, packaged as an executable shaded jar. |

Build everything with `mvn install` from the repo root.

## Library use

```xml
<dependency>
  <groupId>org.gbif</groupId>
  <artifactId>name-parser</artifactId>
  <version>4.2.0</version>
</dependency>
```

```java
NameParser parser = new NameParserImpl();
ParsedName pn = parser.parse("Vulpes vulpes silaceus Miller, 1907", null, null, null);
```

## Migrating from 3.x to 4.x

4.x is a ground-up rewrite: the monolithic regex parser (`NameParserGBIF` / `ParsingJob`)
is replaced by a staged pipeline behind the **same `NameParser` interface**.
`org.gbif.nameparser.NameParserImpl` is the single entry point. Most callers only need to
react to the changes below.

### Runtime

* **No built-in timeout.** 3.x wrapped each parse in a `Future` with a timeout and an
  `InterruptibleCharSequence`; the pipeline drops both. A parse now runs synchronously on the
  calling thread, guarded only by a `MAX_LENGTH` (1000-char) input cap. **If you parse
  untrusted input, impose your own timeout.** `NameParserImpl` is stateless and thread-safe, so
  a single instance can be shared across threads.

### API / model changes (recompile required)

* **`NameType.VIRUS` and `NameType.OTU` removed.** The enum is now just `SCIENTIFIC`,
  `FORMULA`, `INFORMAL`, `PLACEHOLDER`, `OTHER`.
  * A legacy vernacular virus throws `UnparsableNameException` with `NameType.OTHER` and a new
    `getCode() == NomCode.VIRUS`. ICTV-style names (`Lausannevirus`,
    `Marseillevirus marseillevirus`, families ending `-viridae`) instead parse as ordinary
    scientific names with `code = VIRUS`.
  * OTU / specimen codes (`BOLD:ACW2100`, SH/UBA/GTDB identifiers) throw
    `UnparsableNameException` with `NameType.OTHER` (no code).
* **`Rank.DIVISION` renamed to `Rank.DIVISION_ZOOLOGY`**, and a new `Rank.DIVISION_BOTANY`
  added (Lindley-style infrageneric `div.`). Update any references to `DIVISION`.
* **Imprint years moved from `ParsedName` to `Authorship`** (`Authorship.getImprintYear()`,
  next to `getYear()`). They now render whenever the authorship is shown — including
  `NameFormatter.canonical()`, not only `canonicalComplete()`.
* **`NameFormatter` signatures changed.** `buildName(…)` dropped its `showImprintYear`
  boolean; `appendAuthorship(CombinedAuthorship, StringBuilder, NomCode)` is now the public
  `appendAuthorship(StringBuilder, CombinedAuthorshipIF, boolean includeYear, NomCode)`.

### New API (additive)

* **`CombinedAuthorshipIF`** — an interface (implemented by `CombinedAuthorship`,
  `ParsedAuthorship` and `ParsedName`) that your own model can implement to be rendered by
  `NameFormatter`.
* **`ParsedAuthorship.getPublishedInYear()`** (`Integer`) — the publication year extracted
  from the `publishedIn` reference; the reference string keeps the year verbatim.
* **`ParsedName.getGenericAuthorship()` / `getSpecificAuthorship()`** — a name that
  *redundantly* carries the genus author on an infrageneric name
  (`Cordia (Adans.) Kuntze sect. Salimori`) or the species author on a below-species name
  (`Acer campestre L. cv. 'Elsrijk' Broerse`) now captures those authorships separately
  (as `CombinedAuthorship`) instead of dropping them or merging them into the main authorship.

### Parsing behaviour changes (may shift stored/rendered output)

* **The year is kept on botanical/fungal authorship** — `Myosotis palustris (L.) L., 1753`
  renders `1753`. 3.16 dropped the year for botanical names; 4.x renders any year present in
  the input.
* **Bare trinomials default to `INFRASPECIFIC_NAME`, not `SUBSPECIES`**, unless the
  nomenclatural code is zoological (caller-supplied or inferred) — ICZN convention still
  upgrades `Vulpes vulpes silaceus Miller, 1907` to `SUBSPECIES`, while a botanical trinomial
  without an explicit `subsp.`/`var.`/`f.` marker stays `INFRASPECIFIC_NAME`.
* **A bare author initial gains a dot** — `Audouin & H Milne Edwards` →
  `Audouin & H.Milne Edwards`.
* **The anonymous-author placeholder is lower-cased** — `Anon.` → `anon.`.

## Command-line interface

After `mvn install`, the executable jar is at
`name-parser-cli/target/name-parser-cli-<version>-shaded.jar`.

```
java -jar name-parser-cli-<version>-shaded.jar <command> [options]
```

| Command | What it does |
|---|---|
| `parse`     | Stream a text file with one name per row through the parser and write a JSONL file (one JSON object per row). |
| `compare`   | Stream two JSONL files in lockstep, report aggregate metrics and a per-row dump of every differing parsed value. |
| `benchmark` | Measure parser throughput against a name-per-line input file (count, total / avg / min / p50 / p95 / max). |
| `validate`  | Offline LLM audit of parser output over a corpus — an LLM judges each parse and flags likely-wrong ones to turn into regression tests. See [`name-parser-cli/VALIDATE.md`](name-parser-cli/VALIDATE.md). |

Run `<command> --help` for the full per-command option list.

All commands stream their input — memory use stays flat regardless of input size,
so multi-million-row inputs are fine.

### Bundled sample corpora

Sample inputs ship in `name-parser-cli/data/`:

* `benchmark-data.txt` — ~8k mixed names (hand-picked + test-assertion inputs +
  random Catalogue of Life rows with authorship) used for throughput benchmarking.
  Top up with more random names anytime via:
  ```sh
  python3 name-parser-cli/scripts/append-colnames-sample.py [-n 2000] [--seed 17]
  ```
  The script reservoir-samples col-names.tsv in a single pass and appends rows
  as `scientificName authorship` — manual edits to the benchmark file are
  preserved.
* `col-names.tsv` — the full Catalogue of Life names dump (~6.3M rows, ~340 MB,
  not tracked in git — drop your own copy here)

Each command's `--input` defaults assume you run it from the repo root.

### `parse`

```
Usage: name-parser-cli parse [options]

Options:
  --input=PATH    source file (default: data/col-names.tsv; '-' = stdin)
  --output=PATH   target file (default: <input>.<format-ext>; '-' = stdout)
  --format=FMT    output format: jsonl (default), json, csv, tsv
                  csv / tsv produce a flat ColDP Name file with header
  --quiet         suppress progress output
  -h --help       print this message and exit
```

Use `-` as the input or output path to stream from stdin / to stdout — the
command is fully unix-pipe friendly. Progress messages and the final summary
are written to **stderr** so stdout stays a clean data stream:

```sh
cat names.txt | name-parser-cli parse --input=- --output=- --format=tsv | head
xz -dc col-names.tsv.xz | name-parser-cli parse --input=- --output=- --format=jsonl > col.jsonl
```

#### Input

The input format is auto-detected from the first non-blank, non-comment line:

* **ColDP Name file** (TSV or CSV) — recognised when the header row contains
  any [`ColdpTerm`](https://github.com/CatalogueOfLife/coldp/blob/master/README.md#name)
  property names (looked up via `ColdpTerm.find`). Only the columns the parser
  interface accepts are honoured: `ID`, `scientificName`, `authorship`, `rank`,
  `code`. Other columns are read but ignored.
* **Plain text** — one name per line. If a line contains a tab, only the
  substring before the first tab is treated as the name (so `col-names.tsv` is
  usable both as ColDP-style TSV and as bare plain text).

Lines starting with `#` and blank lines are skipped.

#### Output formats

| Format | Description |
|---|---|
| `jsonl` (default) | One self-contained JSON object per line; consumed by `compare`. |
| `json` | Single document containing a JSON array of all rows (streamed; not held in memory). |
| `csv` / `tsv` | Flat [ColDP Name file](https://github.com/CatalogueOfLife/coldp/blob/master/README.md#name) with header row. |

JSON / JSONL rows look like:

```json
{"line":42,"id":"42","input":"Felis catus","parsed":{ ...full ParsedName... }}
{"line":99,"id":"99","input":"Tobacco mosaic virus","error":{"type":"OTHER","code":"VIRUS","message":"..."}}
```

The `id` field is populated from the ColDP `ID` column when present; otherwise
it is omitted.

#### ColDP CSV/TSV column mapping

Every structural `ParsedName` field maps to a ColDP column. Where the ColDP
`Name` entity lacks a column but the `NameUsage` entity defines one, that
NameUsage term is used (`nameStatus`, `namePhrase`, `namePublishedInPage`,
`provisional`, `extinct`). Parser-only fields without a ColDP equivalent are
written into custom columns prefixed with `np:` — strict ColDP readers ignore
unknown columns, so the file stays valid ColDP.

Multi-value rules: author lists join with `|` (the ColDP convention); `notho`
parts join with `,`.

| `ParsedName` field | ColDP column |
|---|---|
| `id` (from input) | `ID` (falls back to verbatim scientificName when absent) |
| `canonicalNameWithoutAuthorship()` (`Candidatus ` prefixed when applicable) | `scientificName` |
| `authorshipComplete()` | `authorship` |
| `rank`, `code` | `rank`, `code` (lower-cased) |
| `nomenclaturalNote` (or `manuscript` flag) | `nameStatus` |
| `uninomial`, `genus`, `infragenericEpithet`, `specificEpithet`, `infraspecificEpithet`, `cultivarEpithet` | same column names |
| `notho` (every flagged part, comma-joined) | `notho` |
| `originalSpelling` | `originalSpelling` |
| `combinationAuthorship.{authors,exAuthors,year}` | `combinationAuthorship`, `combinationExAuthorship`, `combinationAuthorshipYear` (authors joined with `\|`) |
| `basionymAuthorship.{authors,exAuthors,year}` | `basionymAuthorship`, `basionymExAuthorship`, `basionymAuthorshipYear` (authors joined with `\|`) |
| `publishedIn` (free text) | `namePublishedInPage` |
| `extinct` | `extinct` |
| `phrase` | `namePhrase` |
| `doubtful` | `provisional` |
| `type` (when not `SCIENTIFIC`) | `np:type` |
| `sanctioningAuthor` | `np:sanctioningAuthor` |
| `taxonomicNote` (sensu) | `np:taxonomicNote` |
| `unparsed` | `np:unparsed` |
| `warnings` (joined with `\|`) | `np:warnings` |
| (parser failure message) | `np:error` |

Unparsable rows are still written: `ID`, `scientificName` (the verbatim input)
and the `np:type` / `np:error` columns are populated.

### `compare`

```
Usage: name-parser-cli compare [options] <a.jsonl> <b.jsonl> [diffs.txt]

Options:
  --a=PATH              first JSONL file (alt. to first positional arg)
  --b=PATH              second JSONL file (alt. to second positional arg)
  --output=PATH         write per-row diffs here (default: stdout)
  --ignore-whitespace   strip whitespace from string leaves before compare
  --max-diffs=N         cap per-row diff dump at N rows (default: 100)
  -h --help             print this message and exit
```

Both inputs are expected to come from the same source file (matching line
numbers, same row order). The summary reports rows compared / identical /
differing, status transitions (`PARSED→ERROR`, `ERROR→PARSED`, …) and the top
differing field paths. Whitespace inside parsed string values is significant by
default — pass `--ignore-whitespace` to suppress whitespace-only differences in
parsed values (the JSON formatting itself is ignored either way).

### `benchmark`

```
Usage: name-parser-cli benchmark [options]

Options:
  --input=PATH    source file (default: data/benchmark-data.txt)
  --warmup        do an extra untimed pass over the input first to warm the JIT
  -h --help       print this message and exit
```

Pure throughput measurement — every input row is parsed and timed. JIT warmup
is opt-in via `--warmup`, in which case the input is streamed through the
parser once without timing before the timed pass; on subsequent runs the
HotSpot-warmed numbers tend to be ~10× lower. Nothing is written to disk; the
report goes to stdout.

## License

Apache 2.0.
