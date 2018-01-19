# GBIF Name Parser(s)

The project contains various implementations of parsers for scientific names.

At the core there is an independent parser mainly based on regular expression with minimal dependencies.
The modules provided by this project are:

 - __name-parser__: The main GBIF Name Parser implementing the API natively
 - __name-parser-api__: The minimal API to represent parsed names.
 - __name-parser-comparison__: Some comparisons to showcase different features and benchmarks of the different parsers
 - __name-parser-gna__: The [Global Names Architecture name parser](https://github.com/GlobalNamesArchitecture/gnparser) implementing the above name-parser API as an alternative implementation written in Scala
 - __name-parser-antlr__: A highly experimental parser using an ANTLR grammar for parsing.
 - __name-parser-v1__: The GBIF Name Parser wrapped to implement the [GBIF API](https://github.com/gbif/gbif-api/blob/master/src/main/java/org/gbif/api/service/checklistbank/NameParser.java)

The GBIF name parser has been tested with millions of GBIF names over many years.
An extensive body of [unit tests](name-parser/src/test/java/org/gbif/nameparser/NameParserGBIFTest.java) has been created over the years that guarantee high parsing qualities.

