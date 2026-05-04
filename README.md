# GBIF Name Parser

The project contains a parser for scientific names.

This branch is **experimental**: it replaces the regex-based core match in the parser with an ANTLR grammar.
Pre-cleaning, normalization and post-processing remain regex/Java; only the structural match step is now grammar-driven.
The modules provided by this project are:

 - __name-parser__: The main GBIF Name Parser implementing the API natively (now ANTLR-backed)
 - __name-parser-api__: The minimal API to represent parsed names.

The GBIF name parser has been tested with millions of GBIF names over many years.
An extensive body of [unit tests](name-parser/src/test/java/org/gbif/nameparser/NameParserAntlrTest.java) has been created over the years that guarantee high parsing qualities.


