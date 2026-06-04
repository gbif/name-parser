# NameParserImpl

The main name parser implementation to parse scientific, mostly latin, names including authorship, notes and other rare details.
It is used to parse names found in taxonomic, nomenclatural and occurrence/specimen datasets imported to checklistbank.org and gbif.org.

## API
The name-parser-api contains the model classes and enumerations.
In particular the NameType enum is crucial, as it divides parsed names into different broad (syntax) categories,
with some of them not being parsable at all.

## requirements
The parser needs to be performant as it is used on every record GBIF and Catalogue of Life process.
There is a org.gbif.nameparser.Benchmark class that can test a file with one name per row and report metrics about the parser.
Use this to evaluate changing performance. The default test file is not representative for the average names in checklistbank or gbif, but they cover various
cases including tricky ones for regex using backtracking. Avoid catastrophic backtracking in regex.
The average and p95 parsing time on this local macbook pro m4 Pro should be below 250 µs.
You can find the old implementations performance at benchmarks.md.

## architecture
The old implementation (the dev branch) is a huge pile of regex developed over 20 years of small improvements. 
It suffers from catastrophic backtracking and is hard to maintain or add new features to.

Investigate which approach is best suited to implement the parser. 
ANTLR or PEG grammars, deterministic regex engines, custom scanners or whatever does the job best.
Even relying on a C parser that we talk to locally is an option if there are good reasons - although not my preference.

There is another project with a similar goal (but different API) written in GO that uses PEG grammars for the job:
https://github.com/gnames/gnparser

There will always be some ambiguous names which also a human cannot reliably determine without knowing the background of those names.
Especially hard is to decide whether some short words like "della" or "des" are species epithets or author prefixes.
Zoological subgenera do also look just like basionym authors in brackets.
It gets even harder when you cannot rely on the case - although all caps or lower case names are exceptions.

It might be useful to keep a known list of genera, epithets, authors etc to break ambiguity in special cases.
The COL names would be a good starting point - if needed I can probably exgract more names from checklistbank.


## tests
There is a comprehensive NameParserImplTest that tests various aspects of parsing different type of names.
In addition there is a NameParserGnaTest which was added very recently, but these are less important than the main NameParserImplTest.

## test data
The name-parser/src/test/resources contains various test files with names to be parsed.
The benchmark-data.txt is used by default for Benchmark runs.
An export of all names in Catalogue of Life, a comprehensive list of all species in the world, is available at name-parser/src/test/resources/col-names.tsv
This file should be good to understand the distribution of words in names and typical syntax styles to design useful strategies and heuristics.
