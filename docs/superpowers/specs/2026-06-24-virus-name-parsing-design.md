# Parse virus names as binomials

**Date:** 2026-06-24
**Status:** design — approved, ready for implementation plan
**Supersedes:** `PREFLIGHT_VIRUS_FALSE_POSITIVES.md` (this design subsumes and closes it)

## Motivation

Since the parser project began (~15+ years ago), virus names were treated as
*unparsable* (`NameType.VIRUS`, thrown as `UnparsableNameException`, no atoms)
because they did not fit the binomial model of plants, animals, and bacteria.

That premise changed. ICTV ratified **binomial nomenclature for virus species**
(Zerbini et al. 2022; https://doi.org/10.1007/s00705-021-05323-4), in force
across the 2022+ Master Species Lists. Virus species names are now real
`Genus epithet` binomials — e.g. *Tobamovirus tabaci* (tobacco mosaic virus),
*Orthoebolavirus zairense* (Ebola), *Betacoronavirus pandemicum* (SARS-CoV-2).

So virus names should be **parsed** like any other name, with the nomenclatural
code inferred as `NomCode.VIRUS` from the characteristic genus suffix. The code
can be overridden by a caller-supplied `code`. Legacy vernacular virus strings
that still do not fit the binomial model remain unparsable `VIRUS`.

This work also closes the older `PREFLIGHT_VIRUS_FALSE_POSITIVES.md` issue: real
animals whose epithet collides with a viral token (`Ceylonesmus vector`,
`Euragallia prion`) were wrongly thrown as `VIRUS`; under this design they parse
as ordinary species.

## Evidence (data-driven)

Two real datasets were analysed (artifacts produced during brainstorming):

### ICTV Master Species List 2025 (MSL41) — the modern, formal taxonomy
- 17,554 species, **all binomial** (`Genus epithet`). Current parser:
  **131 parse, 17,423 rejected as VIRUS** — i.e. ~99.3 % of the modern virus
  taxonomy is unparsable today.
- The genus suffix is a near-perfect signal: **all 4,149 genera** end in
  `-virus` (4,111), `-satellite` (22), `-viriform` (8), or `-viroid` (8).
  Higher ranks have unique suffixes (realm `-viria`, kingdom `-virae`, phylum
  `-viricota`, class `-viricetes`, order `-virales`, family
  `-viridae`/`-viroidae`/`-satellitidae`, subfamily `-virinae`). **No genus
  lacks a viral suffix.**
- The 131 accidental current parses are all `…satellite epithet` species
  (`Colecusatellite capsici`) that slip through only because the old
  `\bsatellite\b` token needs a word boundary that `Colecusatellite` lacks —
  and they parse with **no** `code=VIRUS`, itself a bug.
- **34 % of species (5,933) have a digit in the epithet** —
  `Simplexvirus humanalpha1`, `Batravirus ranidallo1`, `Lentivirus humimdef1`.
  ICTV epithets are legitimately alphanumeric.

### ChecklistBank pre-2021 virus names — the messy legacy reality
359,332 names that the 3.x parser flagged as virus. Bucketed by the proposed
rules:
- **~20,700** clean viral-suffix uni/binomials (`A_binom` 15,715 + `A_mono`
  4,964) → become parsable `code=VIRUS`. Almost **none carry authors** (1 and
  10 respectively).
- **~8,900** legacy "hard epithet" names (`Abadina virus`, `Abalone
  herpesvirus`, `Anatid herpesvirus-1`) → stay `VIRUS`. Of these, ~740 carry an
  "authorship".
- **33** soft-epithet false-positive animals (`Ceylonesmus vector`,
  `Euragallia prion`, `Microgoneplax prion`, `Desmoxytes vector`, …) — plus a
  few lab constructs (`Binary vector`, `Integration vector`).
- The rest are non-binomial polynomials / strain codes / parenthetical noise.

### The crucial disambiguation result
The genuinely ambiguous case is a **bare** `Capitalized virus` binomial: real
animal (`Exochus virus` Gauld & Sithole, 2002) vs real virus (`Abadina virus`).
Of the **548** authored bare-`virus` collisions in the legacy data:
- **547** have a *committee* "authorship" (`ICTV`, `ICTV 7th Report (2000)`, …)
  — which does **not** match a Linnaean `Surname + year` pattern;
- **1** (`Necocli virus | Londono et al. 2011`) looks zoological, and is in fact
  a virus.

So a proper `Surname + 4-digit-year` rescue rule mis-classifies **1 in
359,332** — and even that is fixed when the virus dataset supplies
`code=VIRUS`. The caller-supplied `code` is the ultimate discriminator.

## Design

### Buckets

| bucket | shape | outcome |
|---|---|---|
| **A** formal ICTV name | genus/monomial (any rank) carries a viral suffix | parse as normal uni/binomial; infer `code=VIRUS` |
| **B** coincidental binomial | normal genus, trigger only in epithet, real organism | parse; infer the *real* code (zoological/etc.) |
| **C** legacy vernacular | anything with a trigger that is not A or B | unchanged: unparsable `NameType.VIRUS`, **no** code attached |

### Detection logic in `Preflight` (precedence order)

Replace the current blunt rule
(`VIRUS.find() && !ZOOLOGICAL_BINOMIAL.find() → throw`) with:

1. **Caller-supplied `code` wins.**
   - `code != null && code != VIRUS` → do **not** apply the virus gate to a
     clean uni/binomial; let it parse as that code. (Rescues `Exochus virus`
     when the source dataset is zoological.)
   - `code == VIRUS` → it is a virus; parse if a clean uni/binomial (→ A),
     otherwise `VIRUS` (→ C).
2. **No code, genus/monomial ends in a viral rank suffix** → bucket A: do not
   throw; parse normally; mark context so code inference sets `VIRUS`.
   Suffix set (case-insensitive, anchored at end of the first word / monomial):
   - genus/species: `virus`, `viruses`, `viroid`, `viroids`, `satellite`,
     `satellites`, `viriform`
   - higher taxa: `viridae`, `viroidae`, `satellitidae`, `virinae`, `viroinae`,
     `satellitinae`, `virales`, `virineae`, `viricetes`, `viricetidae`,
     `viricotina`, `viricota`, `virites`, `virae`, `vira`, `viria`
   - The plural `-viruses`/`-satellites` forms appear in real legacy data
     (`Adomaviruses`, `Alphacoronaviruses amsterdamense`) and are included so
     those parse too.
3. **No code, normal genus, soft trigger in epithet** (`vector`, `prion`,
   `particle`, `replicon`, `rna`; also a bare monomial like `Prion`, a valid
   petrel genus) → bucket B: do not throw; parse; infer the real code via the
   normal pipeline (not virus). No genuine virus collides with these as a
   binomial epithet.
4. **No code, normal genus, hard trigger in epithet:**
   - epithet is a *compound* group-word ending in a viral suffix
     (`herpesvirus`, `parvovirus`, `bracovirus`, `cevirus`, …) → `VIRUS` (C).
     Never rescued, even with an author.
   - epithet is the *bare* trigger word (`virus`, `phage`, `viroid`, `virion`,
     `satellite`) → rescue as a species only when a real `Surname + 4-digit
     year` citation is present (inline **or** in the supplied `authorship`
     argument); otherwise `VIRUS` (C).
5. **Non-binomial input carrying a trigger** (polynomials, strain codes,
   parenthetical / colon / slash noise) → `VIRUS` (C), unchanged.

`Preflight.run` must receive the `authorship` argument (today it sees only the
name portion) so the bare-trigger rescue and the clean-shape tests can consider
name + authorship together. This also fixes gap #1 in the old handoff note.

### Code inference

- A new context signal (e.g. `ParseContext.viralShape`) is set by Preflight
  when bucket A matched.
- In `CodeInference` / `Assemble`: if `viralShape` and the caller passed no
  `code`, set `NomCode.VIRUS`. A caller-supplied `code` always wins.
- Bucket B (soft false-positive animals) gets the **normal** inferred code
  (zoological/botanical/…), **not** virus.

### Digit-bearing epithets (general fix, not virus-gated)

The parser currently mishandles both digit-epithet shapes; both are fixed:

1. **Trailing / internal digits** — `Simplexvirus humanalpha1`,
   `Anatid herpesvirus-1`, `Aspilota abc-1`. Today the digit is **silently
   dropped** (`humanalpha1` → `humanalpha`), which collapses distinct taxa
   (`humanalpha1` and `humanalpha2` → one epithet). Fix: preserve digits as part
   of the epithet token (tokenizer / `NameTokens` epithet classification).
2. **Leading numeral + hyphen (historical zoological)** —
   `Coccinella 11-punctata` Linnaeus, 1758, `Hippodamia 13-punctata`. Today the
   epithet is corrupted into an "author". Fix: recognise a `<number>-<latin>`
   token in epithet position as a specific epithet, not authorship.

These are general improvements that benefit any alphanumeric epithet, applied
across the pipeline (not limited to the virus path).

## Affected components

- `org.gbif.nameparser.pipeline.Preflight` — virus-gate rewrite; accept the
  `authorship` argument; suffix detection; soft/hard/bare epithet logic.
- `org.gbif.nameparser.pipeline.Pipeline` — pass `authorship` into
  `Preflight.run`; carry the `viralShape` signal through to code inference.
- `org.gbif.nameparser.pipeline.ParseContext` — `viralShape` flag.
- `org.gbif.nameparser.pipeline.CodeInference` / `Assemble` — set `VIRUS` when
  `viralShape` and no caller code.
- `org.gbif.nameparser.token.Tokenizer` / `pipeline.NameTokens` — digit-bearing
  epithet handling (both shapes).

## Test plan

- **`viruses.txt` + `NameParserImplTest.virusesPlasmidsPrionsEtc`** — move the
  now-parsable entries (monomial genera `Lausannevirus`, `Tunisvirus`,
  `Clecrusatellite`, `Milvetsatellite`, `Subclovsatellite`; binomials
  `Marseillevirus marseillevirus`, `Senegalvirus marseillevirus`; the
  viral-genus binomials and plural forms) out of the all-`isViralName`
  assertion. Add positive parse assertions for them (`SCIENTIFIC`,
  `code=VIRUS`). The remaining legacy polynomials must still assert `VIRUS`.
- **New: bucket A** — `Tobamovirus tabaci`, `Orthoebolavirus zairense`,
  `Betacoronavirus pandemicum`, monomial `Lausannevirus`, higher taxa
  `Coronaviridae`, `Nidovirales` → `SCIENTIFIC`, `code=VIRUS`. Sample from
  MSL41.
- **New: digit epithets** — `Simplexvirus humanalpha1` /
  `Simplexvirus humanalpha2` parse to *distinct* epithets; `Anatid
  herpesvirus-1`; `Coccinella 11-punctata Linnaeus, 1758` → species
  `Coccinella 11-punctata`, zoological, 1758.
- **New: bucket B false-positive animals** (from the old handoff) —
  `Ceylonesmus vector` (`Chamberlin, 1941`), `Euragallia prion`,
  `Microgoneplax prion`, `Desmoxytes vector` (`(Chamberlin, 1941)`),
  `Cryptops (Cryptops) vector`, the `Dasyproctus cevirus` / `Psenulus trevirus`
  cases, `Exochus virus` (`Gauld & Sithole, 2002`), `Culex vector`
  (`Dyar & Knab, 1906`) → `SCIENTIFIC`, real code, **not** virus.
- **New: caller-code override** — `parse("Acara virus", null, …, VIRUS)` →
  virus; `parse("Exochus virus", "Gauld & Sithole, 2002", SPECIES, ZOOLOGICAL)`
  → zoological species; `parse("Tobamovirus tabaci", null, …, ZOOLOGICAL)`
  honours the caller code.
- **Regression guards** — keep `Crassatellites janus` (mollusk, `-satellites`),
  `Fakus prioni`, `Ypsolophus satellitella`, `Nephodia satellites` parsing
  correctly; genuine legacy viruses (`Tobacco mosaic virus`, `Bacillus phage
  SPβ`, `Human papillomavirus`, `Acara virus`) still `VIRUS`.

## Known limitations (accepted, documented)

- Lab constructs `Binary vector`, `Integration vector`, `Transformation vector`
  (no author) parse as bogus species under the soft-epithet rule. A handful in
  359k; resolved if the caller passes a non-zoological code. A small denylist
  could be added later if it proves noisy.
- One real edge case (`Necocli virus | Londono et al. 2011`) would be rescued
  as a species; resolved when the virus dataset passes `code=VIRUS`.
- `Genus LETTER` serotype forms (`Enterovirus A`, `Rotavirus B`) are not clean
  binomials (uppercase single-letter "epithet") and remain `VIRUS`.

## Out of scope

- A curated authority list of virus genus-words. The genus suffix is a ~100 %
  signal on real data, so a data file is unnecessary; a denylist may be added
  later only if false positives appear.
- Rank inference from viral higher-taxon suffixes (e.g. `-viridae` → FAMILY)
  beyond whatever `RankUtils` already derives — not required to parse the names.
- Parsing strain designations, isolate codes, or the parenthetical
  `Influenza A virus (A/.../2009(H1N1))` detail.
