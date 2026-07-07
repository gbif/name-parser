# `validate` — LLM-assisted parser-output auditing

An **offline** tool that streams a name corpus through the parser and asks an LLM to
flag parses that look wrong, so you can turn confirmed cases into regression tests
(`NameParserImplTest`) and rule fixes. It never touches the parser or its behaviour —
it only *reads* parser output and reports on it.

The model acts as a **judge**: it sees the raw name plus the parser's structured
`ParsedName` and answers *is this correct? if not, what's wrong?* — verification, not
re-parsing. Every flagged row still needs a human to confirm.

## Build

Build the executable fat jar (shade plugin is already configured):

```bash
mvn -pl name-parser-cli -am -DskipTests package
```

Produces `name-parser-cli/target/name-parser-cli-*-shaded.jar`. Rebuild after any code change.

## Run

Run from a directory where your `--input` path resolves (or pass an absolute path).

```bash
JAR=name-parser-cli/target/name-parser-cli-*-shaded.jar

# Dry run — select + build batches, no API calls, no cost. Always start here.
java -jar $JAR validate --input=name-parser-cli/data/benchmark-data.txt --dry-run

# Cloud (metered API cost; needs ANTHROPIC_API_KEY)
java -jar $JAR validate --input=data/col-names.tsv --budget=200 --batch=25

# Local, free (Ollama / LM Studio / llama.cpp — no key needed)
ollama pull qwen2.5:32b-instruct
java -jar $JAR validate --provider=local --model=qwen2.5:32b-instruct \
  --input=data/col-names.tsv --budget=200 --batch=15
```

### Key options

| Option | Default | Meaning |
|---|---|---|
| `--provider=P` | `anthropic` | `anthropic` (cloud Claude) or `openai`/`local` (OpenAI-compatible local server) |
| `--model=ID` | `claude-opus-4-8` / `qwen2.5:14b-instruct` | model id; passed straight through |
| `--input=PATH` | `data/col-names.tsv` | corpus (plain text or ColDP TSV/CSV) |
| `--output=PATH` | `validate-report.jsonl` | JSONL report |
| `--budget=N` | `2000` | max names sent to the LLM |
| `--sample-normal=N` | `200` | of those, ordinary names as a baseline (rest is the suspicious tail) |
| `--batch=N` | `25` | names per request (smaller = more frequent progress, less input) |
| `--seed=N` | `17` | selection seed — same seed selects the same names (reproducible) |
| `--cache=PATH` | `validate-cache.jsonl` | verdict cache (content-hashed); `none` to disable |
| `--api-url=URL` | — | endpoint override; local default is `http://localhost:11434` (Ollama) |
| `--dry-run` | off | select + build batches but make no API calls |

**Coverage:** the whole file is scanned, but only a bounded, seeded sample is judged
(`--budget`): the *suspicious tail* (unparsable, warnings, `PARTIAL` state, non-scientific
types) plus a random baseline. UNITE SH (`SH…FU`) and BOLD BIN (`BOLD:…`) codes are
excluded automatically. Excluded/dropped counts are logged.

**Cost/speed:** cloud Opus is a few dollars for `--budget=2000`, cents for small runs.
Local is free but slower, and reasoning models (e.g. Qwen3) are slower still. The cache
is keyed by `(model, input, parser output)`, so re-runs and budget bumps don't re-judge
work already done — and cloud vs local verdicts never collide.

## Progress

Progress prints to **stderr** after each batch: `judged 15/50 (0 from cache)`. For live,
per-batch progress watch the cache file (flushed as each batch lands):

```bash
tail -f validate-cache.jsonl
```

The `--output` report is buffered and is complete once the run's summary prints.

## Report format

One JSON object per line in `--output`:

```json
{
  "line": 4211,
  "input": "Vulpes vulpes silaceus Miller, 1907",
  "parsed": { "...": "the full ParsedName" },
  "verdict": "wrong",
  "confidence": "high",
  "note": "zoological trinomial should be SUBSPECIES",
  "fields": [
    { "name": "rank", "parsed": "INFRASPECIFIC_NAME", "expected": "SUBSPECIES",
      "reason": "ICZN trinomials are subspecies by convention" }
  ]
}
```

- `verdict` ∈ `ok` | `suspect` | `wrong`; `confidence` ∈ `low` | `med` | `high`.
- `fields` lists the specific fields the model believes are wrong (empty/absent when `ok`).
- Unparsable inputs carry `error` instead of `parsed`.

The run also prints a stderr summary: counts by verdict and a *most-flagged fields*
histogram — the quickest tell for where the parser most often looks wrong.

## Filtering the report with `jq`

`jq` reads JSONL line-by-line, so no `cat` and no leading `.` needed.

```bash
# Everything the model did NOT call ok (the review queue)
jq 'select(.verdict != "ok")' validate-report.jsonl

# One line per record (compact) — easier to skim
jq -c 'select(.verdict != "ok")' validate-report.jsonl

# Only the confident "wrong" calls — the highest-signal ones first
jq -c 'select(.verdict == "wrong" and .confidence == "high")' validate-report.jsonl

# Count by verdict
jq -r '.verdict' validate-report.jsonl | sort | uniq -c

# Histogram of which fields get flagged (where the parser most often slips)
jq -r 'select(.fields != null) | .fields[].name' validate-report.jsonl | sort | uniq -c | sort -rn

# Only rows where a specific field (e.g. code) was flagged
jq -c 'select(.fields != null and any(.fields[]; .name == "code"))' validate-report.jsonl

# A compact review view: input + verdict + the field problems
jq -r 'select(.verdict != "ok")
  | "\(.verdict)\t\(.input)\t" + ((.fields // []) | map("\(.name): \(.parsed)→\(.expected)") | join("; "))' \
  validate-report.jsonl

# Just the raw input strings of flagged rows (feed elsewhere, e.g. re-parse/debug)
jq -r 'select(.verdict != "ok") | .input' validate-report.jsonl

# The facts you need to write a NameParserImplTest case, per wrong row
jq -c 'select(.verdict == "wrong")
  | {input, note, fixes: (.fields | map({(.name): .expected}) | add)}' \
  validate-report.jsonl
```

**Workflow:** skim the field histogram → pull the confident `wrong` rows → for each one
you confirm by hand, add an assertion in `NameParserImplTest` (and, if it's a rule gap,
fix the pipeline). Treat local-model verdicts as a noisier screening pass than cloud:
judge on *precision* — of the flagged rows, how many are genuine parser bugs.
