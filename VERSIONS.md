# Versions

The major versions of the GBIF name parser and, since 5.0.0, of its api. The parser itself now lives
in [gbif/name-parser-rust](https://github.com/gbif/name-parser-rust), which has its own
`VERSIONS.md`.

## 6.0.0 (unreleased, `dev`)

- `ParseResult.Informal` is gone. A name in open nomenclature is a `Parsed` result of type
  `INFORMAL`, and `ParsedName.anchor()` gives the taxon it hangs off.
- `Authorship.inAuthors` holds the authors of the work a name was published in (`L. in DC.`).
- The author lists of an `Authorship` stay null until set.
- The api has no runtime dependencies any more.
- `NomCode` links the Madrid Code, the 2025 Prokaryotic Code and the PhyloCode by DOI.

## 5.x (2026-07 to 2026-10): the api only

5.0.0 removed the Java parser implementation, which became the Rust engine. This repository became
the api module alone: the model (`ParsedName`, `Authorship`, `Rank`, `NomCode`, `NameType`, …), the
`NameParser` interface and the `NameFormatter`.

- 5.5.0 (2026-10-15): `RankGroup` and `Rank.getGroup`, `sameGroup`, `covers`, `isConcrete`,
  `isSuprafamilial`.
- 5.4.0 (2026-10-14): `Authorship.bracketedYear`, `ParseResult.Informal.candidatus`.
- 5.3.0 (2026-10-08): `ParseResult.canonicalNameComplete()`.
- 5.2.0 (2026-10-07): the sanctioning author is on `Authorship`, a sanctioned basionym's too.
- 5.1.0 (2026-10-06): `Authorship.anonymous`, rendered per nomenclatural code.
- 5.0.1 (2026-08-25): the informal phrase is the verbatim tail of a name, its authorship included.
- 5.0.0 (2026-07-14): a return-based parse api. `NameParser.parse` returns a `ParseResult`
  (`Parsed`, `Informal` or `Unparsable`) and throws only an unchecked exception.
  `NameType.IDENTIFIER` for identifiers such as BOLD BINs or UNITE species hypotheses.

## 4.x (2026-06 to 2026-07): a new Java parser

4.0.0 replaced the regex parser of 3.x with a step pipeline: strip and stash the parts that are no
name, tokenize, classify and assemble. It parsed authorships in more detail and inferred the
nomenclatural code. Java 17.

- 4.2.0 (2026-07-07): `NameType.VIRUS` gave way to `OTHER` with the `VIRUS` code, carried on the
  unparsable exception. The code inferred by a vote of the authorship's features, the generic,
  specific and imprint authorship rendered in the complete canonical name, `publishedInYear`, and
  many accuracy fixes found by validating parses with a language model.

## 3.x (2017-12 to 2026-05): the name-parser-api

3.0 split the model out of the GBIF api into its own `name-parser-api` module, with no dependency on
GBIF. Authorships were parsed down to individual authors, split into combination and basionym
authorship, and a `State` enum replaced the parsed flags. `name-parser-v1` kept the old GBIF api
`ParsedName` for existing users.

- 3.x added the OTU and cultivar handling, the original-spelling flag (`[sic]`, 3.9), the zoological
  sections beside the botanical ones (3.10, 2023), and loading parser configuration from
  ChecklistBank.
- Java 11 from 3.13 (2025), Java 17 from 3.16 (2026). An ANTLR grammar was tried and dropped
  (archived as `archive/antlr-parser`).

## 2.x (2014 to 2017): the GBIF regex parser

The parser of GBIF's ChecklistBank, written with regular expressions and moved from Google Code to
GitHub in January 2014. It filled the `ParsedName` of the GBIF api.

- Microbial ranks, strains and sanctioned fungal names (`Fr.` after a colon) in 2014 and 2015.
- 2016: rank markers moved from the GBIF api into the parser's `RankUtils`, and the project split
  into the GBIF parser and a wrapper of the Global Names (GNA) parser. Java 8.
