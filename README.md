# GBIF Name Parser(s)

The project contains various implementations of parsers for scientific names.

At the core there is an independent parser mainly based on regular expression with minimal dependencies.
The modules provided by this project are:

 - __name-parser__: The main GBIF Name Parser implementing the API natively
 - __name-parser-api__: The minimal API to represent parsed names.
 - __name-parser-v1__: The GBIF Name Parser wrapped to implement the [GBIF API](https://github.com/gbif/gbif-api/blob/master/src/main/java/org/gbif/api/service/checklistbank/NameParser.java)

The GBIF name parser has been tested with millions of GBIF names over many years.
An extensive body of [unit tests](name-parser/src/test/java/org/gbif/nameparser/NameParserGBIFTest.java) has been created over the years that guarantee high parsing qualities.


A library and command-line tool that parses scientific names — including the
authorship, rank, hybrid markers and nomenclatural notes — into a structured
[`ParsedName`](name-parser-api/src/main/java/org/gbif/nameparser/api/ParsedName.java)
model.

## Modules

| Module | Purpose |
|---|---|
| `name-parser-api`  | Pure model + interface module: `ParsedName`, `Authorship`, `Rank`, `NomCode`, `NameType`, the `NameParser` interface, plus formatter / Unicode utilities. Depend on this if you only need the data model. |
| `name-parser`      | The parser implementation. Single public entry point: `org.gbif.nameparser.NameParserImpl`. |
| `name-parser-cli`  | Command-line tools (`parse`, `compare`, `benchmark`) wrapping the parser, packaged as an executable shaded jar. |

Build everything with `mvn install` from the repo root.

## Library use

```xml
<dependency>
  <groupId>org.gbif</groupId>
  <artifactId>name-parser</artifactId>
  <version>4.0.0-SNAPSHOT</version>
</dependency>
```

```java
NameParser parser = new NameParserImpl();
ParsedName pn = parser.parse("Vulpes vulpes silaceus Miller, 1907", null, null, null);
```

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

Plain text only — one name per line. Lines starting with `#` and blank lines
are skipped. If a line contains a tab, only the substring before the first
tab is treated as the name, so a bare TSV like `col-names.tsv` can be fed in
verbatim and the extra columns are silently ignored.

#### Output formats

| Format | Description |
|---|---|
| `jsonl` (default) | One self-contained JSON object per line; consumed by `compare`. |
| `json` | Single document containing a JSON array of all rows (streamed; not held in memory). |
| `csv` / `tsv` | Flat [ColDP Name file](https://github.com/CatalogueOfLife/coldp/blob/master/README.md#name) with header row. |

JSON / JSONL rows look like:

```json
{"line":42,"input":"Felis catus","parsed":{ ...full ParsedName... }}
{"line":99,"input":"Iridoviridae","error":{"type":"VIRUS","message":"..."}}
```

#### ColDP CSV/TSV column mapping

Every structural `ParsedName` field maps to a ColDP column. Where the ColDP
`Name` entity lacks a column but the `NameUsage` entity defines one, that
NameUsage term is used (`nameStatus`, `namePhrase`, `namePublishedInPage`,
`provisional`, `extinct`). Parser-only fields without a ColDP equivalent are
written into custom columns prefixed with `np:` — strict ColDP readers ignore
unknown columns, so the file stays valid ColDP.

Multi-value rules: author lists join with `|` (the ColDP convention).

| `ParsedName` field | ColDP column |
|---|---|
| `id` (from input) | `ID` (falls back to verbatim scientificName when absent) |
| `canonicalNameWithoutAuthorship()` (`Candidatus ` prefixed when applicable) | `scientificName` |
| `authorshipComplete()` | `authorship` |
| `rank`, `code` | `rank`, `code` (lower-cased) |
| `nomenclaturalNote` (or `manuscript` flag) | `nameStatus` |
| `uninomial`, `genus`, `infragenericEpithet`, `specificEpithet`, `infraspecificEpithet`, `cultivarEpithet` | same column names |
| `notho` (single hybrid-marker part, lower-cased) | `notho` |
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
