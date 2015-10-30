# GBIF Name Parser

The core GBIF scientific name parser library

# Building

````shell
mvn clean install
````

# Usage

````java
NameParser parser = new NameParser(50);
ParsedName n = parser.parse("Beta vulgaris L.", null);
n.getGenusOrAbove();
````

There are more examples of usage in [NameParserTest](src/test/java/org/gbif/nameparser/NameParserTest.java).
