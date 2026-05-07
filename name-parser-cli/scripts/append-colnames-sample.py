#!/usr/bin/env python3
"""Reservoir-sample N random rows from name-parser-cli/data/col-names.tsv and
append them to name-parser-cli/data/benchmark-data.txt as
``scientificName authorship`` (authorship column appended with a space when
present).

Single-pass over col-names.tsv — never loads the whole file into memory, so
the 6.3M-row / 340 MB corpus is fine. The benchmark file is appended to, not
overwritten, so manual curation between runs is preserved.

Usage:
    python3 name-parser-cli/scripts/append-colnames-sample.py [-n N] [--seed S]

Defaults: N = 2000, seed = 17 (changing the seed yields a different sample).
"""
from __future__ import annotations
import argparse
import random
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
SRC = REPO / "name-parser-cli" / "data" / "col-names.tsv"
DEST = REPO / "name-parser-cli" / "data" / "benchmark-data.txt"


def reservoir_sample(path: Path, k: int, seed: int) -> tuple[list[str], int]:
    """Single-pass uniform sample of *k* rows. For each kept row, returns
    ``scientificName`` + ``" " + authorship`` when authorship is non-empty,
    otherwise just ``scientificName``. Skips the TSV header and any blank or
    comment lines."""
    rng = random.Random(seed)
    reservoir: list[str] = []
    n = 0
    with path.open("r", encoding="utf-8") as fh:
        first = True
        for line in fh:
            line = line.rstrip("\n").rstrip("\r")
            if not line or line.startswith("#"):
                continue
            if first:
                first = False
                if line.split("\t", 1)[0] == "scientificName":
                    continue
            cols = line.split("\t")
            sci = cols[0].strip() if cols else ""
            if not sci:
                continue
            auth = cols[1].strip() if len(cols) > 1 else ""
            full = (sci + " " + auth).strip() if auth else sci
            n += 1
            if len(reservoir) < k:
                reservoir.append(full)
            else:
                j = rng.randrange(n)
                if j < k:
                    reservoir[j] = full
    return reservoir, n


def main() -> int:
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("-n", "--count", type=int, default=2000,
                   help="number of names to append (default: 2000)")
    p.add_argument("--seed", type=int, default=17,
                   help="reservoir sampling seed (default: 17)")
    args = p.parse_args()

    if not SRC.exists():
        sys.exit(f"missing {SRC}")
    if not DEST.exists():
        sys.exit(f"missing {DEST}")

    sample, total = reservoir_sample(SRC, args.count, args.seed)

    existing = DEST.read_bytes()
    sep = b"" if existing.endswith(b"\n") else b"\n"
    with DEST.open("ab") as out:
        out.write(sep)
        for name in sample:
            out.write(name.encode("utf-8"))
            out.write(b"\n")

    print(f"appended {len(sample)} names from {SRC.name} (seed={args.seed}); "
          f"sampled from {total:,} rows")
    return 0


if __name__ == "__main__":
    sys.exit(main())
