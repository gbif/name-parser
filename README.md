# Name Parser Implementation

The implementation of the core GBIF scientific name parser library.
The project provides two modules with different implementations.

# GBIF Name Parser

A regex based parser for scientific names adjusted to work with all name strings found in the GBIF network.

## Usage

````java
NameParser parser = new GBIFNameParser();
ParsedName n = parser.parse("Kyrpidia tusciae (Bonjour & Aragno 1985) Klenk et al. 2012");

````

There are more examples of usage in [NameParserTest](name-parser-gbif/src/test/java/org/gbif/nameparser/NameParserTest.java).

# GNA Name Parser
The [Global Names Architecture scala name parser](https://github.com/GlobalNamesArchitecture/gnparser) wrapped to comply with the GBIF API NameParser interface.
