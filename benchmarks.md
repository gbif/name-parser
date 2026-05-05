# Benchmarks

results from a macbook pro M4 Pro via IntelliJ:

## Current DEV
```
Parsed names: 4763 (3307 failed)
Total:   482.02 ms
Average: 101.20 µs
Min:     17.92 µs
p50:     55.17 µs
p95:     224.38 µs
Max:     16.59 ms
```

## DEV with smaller patterns
```
Parsed names: 4763 (3308 failed)
Total:   410.24 ms
Average: 86.13 µs
Min:     19.33 µs
p50:     46.96 µs
p95:     199.04 µs
Max:     15.64 ms
```


## Joni
```
Parsed names: 4763 (3308 failed)
Total:   791.99 ms
Average: 166.28 µs
Min:     24.38 µs
p50:     86.29 µs
p95:     372.46 µs
Max:     71.51 ms
```


## ANTLR
```
Parsed names: 4763 (3316 failed)
Total:   494.68 ms
Average: 103.86 µs
Min:     19.04 µs
p50:     49.83 µs
p95:     301.71 µs
Max:     9.76 ms
```

## v4 Authorship polish (mid-name + homoglyphs)
```
Parsed names: 4763 (3300 failed)
Total:   64.61 ms
Average: 13.56 µs
Min:     958 ns
p50:     4.46 µs
p95:     45.83 µs
Max:     1.34 ms
```
Adds: mid-name author span detection — capital-cased Author abbreviations between
the genus/epithet and a following rank marker are silently consumed
("Centaurea L. subg. Jacea", "Festuca ovina L. subvar. gracilis Hackel",
"Salix repens L. subsp. galeifolia Neumann ex Rech. f."); rank-restricted
code inference via `Rank.isRestrictedToCode()` (PATHOVAR/BIOVAR/etc. pin
BACTERIAL, CULTIVAR pins CULTIVARS, etc.); homoglyph normalisation via
`UnicodeUtils.replaceHomoglyphs(..., false)` plus a small Win-1252 →
Latin map (`¡`/`¢`/`£`/`‚`/`„`/`‰`) emitting `Warnings.HOMOGLYHPS`;
basionym sanctioning split inside parens ("(Fr. : Fr.)" → basionym "Fr.",
sanctioning silently dropped at species level); colon sanctioning at end
of combination ("Boletus versicolor L. : Fr." → comb "L.", sanct "Fr.").

## v4 Authorship polish
```
Parsed names: 4763 (3300 failed)
Total:   71.62 ms
Average: 15.04 µs
Min:     959 ns
p50:     4.42 µs
p95:     58.04 µs
Max:     1.17 ms
```
Adds: author inversion ("Walker, F." / "Balsamo M, Fregni E, Tongiorgi MA" /
"LeConte, J.L." → "F.Walker" / "M.Balsamo, E.Fregni, M.A.Tongiorgi" /
"J.L.LeConte"), all in the canonical no-space "<initials>.<surname>" form;
case-sensitive filius/junior glue (uppercase "F" → initial, lowercase "f" →
filius); bracketed nom annotations ("[nom. et typ. cons.]" / "[orth. error]"
→ nomenclaturalNote); inline nom notes that survive past commas+non/nec
(splice-strip rather than truncate); manuscript synonyms (ined./ms./msc./
unpublished); year extraction from "in <Reference>, <year>" tail applied to
combination authorship after code inference; dotted-initial-no-space
collapse inside taxonomic notes ("non. A. lancea." → "non. A.lancea.");
nom+tax-note pair → BOTANICAL signal. p95 ≈ 58 µs, well under the 250 µs
ceiling.

## v4 Tier 4
```
Parsed names: 4763 (3300 failed)
Total:   66.14 ms
Average: 13.89 µs
Min:     875 ns
p50:     4.29 µs
p95:     52.46 µs
Max:     1.24 ms
```
Adds a `Preflight` stage that throws `UnparsableNameException` with the right
`NameType` for non-scientific inputs:

- VIRUS — keyword/suffix matcher: virus / viroid / phage(s) / virion /
  satellite (alone, or alpha/beta/delta/circular variants) / vector /
  prion / particle / replicon / RNA / NPV / GV / ICTV.
- HYBRID_FORMULA — × or " x " between two name spans where the left side
  is at least a binomial (or carries an author abbreviation) and the right
  side starts with a Latin word; `×` glued to the next epithet is correctly
  treated as a notho marker (not a formula).
- NO_NAME — BOLD: / SH / UBA / GTDB / GCA / GCF code patterns;
  pure-alphanumeric monomials with digits; PR2-style underscored names
  with hyphenated digit suffix; "Gen.nov.", "@…", "tobedeleted",
  "(delete)" markers.
- PLACEHOLDER — keywords (incertae sedis, not assigned, unknown,
  unaccepted, unidentified, undetermined, indet, indeterminate, uncultured,
  undescribed, temp dummy), prefix-only forms (Unident-, Undescribed-,
  IncertaeSedis, Undet), "N.N." variants, leading "?" + epithet, and
  "[unassigned]…".

Failure count is now meaningful — 3300 of 4763 fixture rows fail to parse,
which is in the same ballpark as the regex baselines (3307 / 3308 /
3316). p95 stays at 52 µs, well under the 250 µs ceiling.

