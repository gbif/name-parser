# name-parser v4.2 — Catalogue of Life integration findings

Single handover record of every **parser-side** issue found while integrating name-parser
4.2.0-SNAPSHOT into the ChecklistBank backend (`CatalogueOfLife/backend`, branch
`feature/name-parser-v4`). Living document — append new findings here.

All commits below are on the local `dev` branch and **not yet pushed/published**. The backend
builds against the installed `4.2.0-SNAPSHOT`, so CI/others won't see them until published.

Background: the GBIF Name Parser handles the scientific name and an optional, separately supplied
authorship string in one call: `parse(scientificName, authorship, rank, code)`. Several findings
below are specifically about the **separately-supplied-authorship** path being weaker than the
embedded (authorship-in-the-name-string) path.

---

## 1. `NameType.VIRUS` removed → `OTHER` + `NomCode.VIRUS`  — DONE (4.2)

v4.2 dropped the `VIRUS` name type. Viruses are now `NameType.OTHER` and the
`UnparsableNameException` carries `NomCode.VIRUS` (new `getCode()`), e.g.
`assertUnparsable("Tobacco mosaic virus", OTHER, VIRUS)`. ICTV binomials/monomials now parse as
proper scientific names with `code=VIRUS` (`Lausannevirus`, `Marseillevirus marseillevirus`).

Backend impact: removed `NameType.VIRUS` everywhere; record `e.getCode()` on unparsable names; the
`parser_config` override feature (which existed only to curate virus false-positives) was removed.

## 2. VIRUS false positives on zoological binomials — DONE (4.2 Preflight)

Clean Latin binomials whose epithet collides with a viral marker (`vector`, `virus`, `prion`,
`cevirus`) were rejected as viruses. The `Preflight` `ZOOLOGICAL_BINOMIAL` guard + `viruses.txt`
re-curation fixed this; `virusFalsePositiveAnimals` covers `Aspilota vector`, `Ceylonesmus vector`, …

## 3. Bracketed / leading `corrig.` not stripped — FIXED `c719301`

`StripAndStash.CORRIG` matched only a bare ` corrig.` token, so:
- parenthesised `(corrig.)` / `[corrig.]` (handled for `(sic)`) was left in the name, and
- a leading `corrig.` at the start of a **standalone authorship** ("corrig. Golyshin et al., 2005")
  was not stripped.

Fix: match the marker like `SIC` (optional surrounding `()`/`[]`) and prepend a space before
removal so a leading marker matches too. Test: `sic()` parenthesised-corrig case.

## 4. No code inferred from a separately supplied recombination authorship — FIXED `da05fca`

Code inference fell back to the auxiliary (separately supplied) authorship only when its basionym
carried a **year** — the zoological `(Author, YYYY)` pattern. A **botanical** recombination
`(Basionym) Combination` supplied as separate authorship has no year, so `code` stayed null even
though the same authorship embedded in the name string infers `BOTANICAL`. Net effect downstream:
botanical infraspecific names rendered without their `subsp.`/`var.` marker.

Fix (`Pipeline`): also promote the auxiliary state to code inference when it pairs a parenthesised
basionym with a combination author (no year required). Test:
`botanicalCodeFromSeparateRecombinationAuthorship` (`Cerastium ligusticum subsp. granulatum` +
`(Huter et al.) P. D. Sell & Whitehead` → BOTANICAL).

## 5. Standalone `ined.`/`ms.` authorship parsed as an author — FIXED `db1d0b7`

`ined`/`ms` are in `AuthorshipParser.AUTHOR_SUFFIXES` (they glue onto a *preceding* author, e.g.
`Monterosato ms.`). Supplied as the **whole** authorship they had nothing to glue to and became a
lone combination author instead of a manuscript flag; the manuscript-marker strip required a
preceding token. Fix (`StripAndStash`): a standalone `ined.`/`ms.`/`msc.`/`unpublished` authorship
sets the manuscript flag with no author, keeping the `Author ms.` suffix behaviour. Test:
`standaloneManuscriptAuthorship`.

## 6. Author initial gets a dot — `H Milne` → `H.Milne`  — INTENTIONAL `80cd92a`

v4.2 normalises a bare author initial to carry a dot (`Audouin & H Milne Edwards` →
`Audouin & H.Milne Edwards`). Confirmed a deliberate, good change; CoL tree fixtures updated to match.

## 7. Year retained on botanical/fungal authorship  — INTENTIONAL

When a year is present in the parsed authorship it is now kept, even for a botanical recombination
(`Myosotis palustris (L.) L., 1753`). 3.16 dropped the year for botanical names; 4.2 keeps it —
intended, because fungal names (ICN) and other botanical-code names do cite the year. Rule of thumb:
**if the year is present in the input, render it.** CoL tree fixtures updated to match.

---

## Things confirmed NOT to be parser issues (recorded so they aren't re-investigated here)

- **Merge/matching drops the richer authorship variant.** In merge syncs, names like
  `Protoperidinium antarcticum (Schimper) Balech`, `Hypsicera femoralis (Geoffroy, 1785)`,
  `Xystophora Wocke, 1876` render in the merged tree with components dropped (`Schimper`,
  `Geoffroy`, `Wocke`). Verified the **parser/wrapper parse these correctly** (full bas/comb/year),
  and reverting `da05fca` does not change it. This is a **backend assembly/matching** regression
  (3.16 master renders them correctly), not a parser bug. Tracked on the CoL side.

- **text-tree 1.7.0 is stricter about ` [rank]` markers.** Legacy CoL test data with `name[rank]`
  (no space before the bracket) now mis-parses the rank into the name/authorship
  (`Pargaini Uvarov, 1953[tribe]` → `tribe` became an author). 3.16 + older text-tree tolerated it.
  This is the **text-tree** library, not name-parser; fixed by adding the spaces in the CoL test
  data. Flag for text-tree if no-space tolerance is desired.

## 8. Evaluated v4.2 author/rank behaviours (mostly intended; two need a decision)

Surfaced by `SectorSyncMergeIT`; investigated and found **not** to be clear bugs:

- **`Anon.` → `anon.` — INTENTIONAL.** `StripAndStash.normaliseAnon` deliberately lower-cases the
  anonymous-author placeholder. Note: the CoL backend's `NameParser.NORM_ANON` re-capitalises to
  `Anon.` in `setNormalizeAuthorship`, but the merge renders from atoms (stored `anon.`), so the two
  conventions disagree. CoL to reconcile (accept `anon.` or capitalise on rebuild).
- **Bare trinomial → `INFRASPECIFIC_NAME`, not `SUBSPECIES` — INTENTIONAL.** `Biota orientalis
  fortunei Jacob-Makoy` (no rank marker) now infers the generic infraspecific rank rather than
  guessing subspecies. Conservative and reasonable; CoL fixtures to follow.
- **`John M.Mill.` → `M.John & Mill.` — NEEDS A DECISION.** The surname-initials inversion
  (`AuthorshipParser`, "<Surname> <Initials>" → "<initials>.<Surname>") splits a single
  `Forename Initial.Surname` author into two. Source data is ambiguous; flag whether the inversion
  should be suppressed when the leading token is a plausible forename.
- **Zoological autonym `Tetralobus flabellicornis subsp. flabellicornis (Linnaeus, 1767) Linnaeus
  1767` — NEEDS A DECISION.** Infers `BOTANICAL` (explicit-marker rule) despite the year-in-parens
  zoological citation, and the merge keeps a doubled `(Linnaeus, 1767) Linnaeus, 1767`. Decide the
  intended code + whether a nominotypical subspecies should repeat the species author.

## Open parser items

- `John M.Mill.` inversion over-split (finding 8) — decision needed.
- Zoological autonym code + doubled author (finding 8) — decision needed.
