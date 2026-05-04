grammar SciName;

// Parses a strongly-normalized scientific name (i.e. the output of ParsingJob.normalizeStrong)
// into the same structural slots that the old NAME_PATTERN regex captured. The listener layer
// validates rank markers, splits ex-authors, and feeds the existing parseAuthorship Java helper.
//
// Author-team disambiguation (initials, particles like "van der", sanctioning authors, year
// formats) is intentionally kept loose at the grammar level — author tokens are captured as a
// flat blob and the listener / existing Java helpers refine them.
//
// Grammar disambiguation: ranked rules require a DOT after the rank marker, so a binomial like
// "Tilia americana" cannot accidentally match `rankedInfrageneric` and a trinomial with a real
// rank marker like "Tilia americana subsp. baltica" routes through `rankedInfraspec`.

// Note: no EOF — remainder tokens are read programmatically by the matcher so that names with
// trailing junk still parse to PARTIAL state instead of failing outright.
name
    : monomial
      ( subgenusParens
      | rankedInfrageneric
      )?
      ( epithet epithetTail? )?
      authorship?
    ;

monomial
    : HYBRID? (UPPER_WORD | INITIAL | QMARK)
    ;

// (Subgenus) form
subgenusParens
    : LPAREN UPPER_WORD RPAREN
    ;

// subg. Subgenus | sect. Section | etc. — listener validates the rank marker against RankUtils
rankedInfrageneric
    : LOWER_WORD DOT UPPER_WORD
    ;

epithet
    : HYBRID? LOWER_WORD
    ;

// After the species epithet there can be:
//   - a ranked infraspecific (subsp. baltica)               → 3-parted with rank marker
//   - a middle epithet followed by a ranked infraspec       → 4-parted with rank marker
//   - a bare last epithet (no rank marker)                  → 3-parted, last token → infraspec slot
epithetTail
    : rankedInfraspec
    | middleEpithet rankedInfraspec
    | bareInfraspec
    ;

middleEpithet
    : HYBRID? LOWER_WORD
    ;

// last epithet without rank marker (e.g. "Hieracium vulgatum arrectariicaule")
bareInfraspec
    : HYBRID? LOWER_WORD
    ;

// rank-marker. epithet  — listener validates the rank marker against RankUtils
rankedInfraspec
    : LOWER_WORD DOT HYBRID? LOWER_WORD
    ;

// Optional basionym in parens followed by optional combination part.
authorship
    : basionymGroup combPart?
    | combPart
    ;

basionymGroup
    : LPAREN combPart? RPAREN
    ;

combPart
    : authorBlob (COLON sanctAuth)? yearMaybe?
    | yearMaybe
    ;

// Flat token list — the listener splits on the literal "ex" to separate ex-authors,
// and reconstructs the raw author-team string for the existing parseAuthorship helper.
authorBlob
    : authorToken+
    ;

authorToken
    : UPPER_WORD DOT?
    | LOWER_WORD DOT?
    | INITIAL
    | AMP
    | COMMA
    | SEMI
    ;

sanctAuth
    : UPPER_WORD DOT?
    ;

yearMaybe
    : COMMA? LPAREN? YEAR RPAREN?
    ;


// ---------- Lexer ----------

QMARK   : '?' ;
HYBRID  : '×' ;
LPAREN  : '(' ;
RPAREN  : ')' ;
COMMA   : ',' ;
AMP     : '&' ;
SEMI    : ';' ;
COLON   : ':' ;
DOT     : '.' ;

INITIAL : [A-Z] '.' ([A-Z] '.')* ;

YEAR    : [12] [0-9] [0-9] [0-9?] [a-h?]? ;

// Latin letter classes with diacritics. Approximates \p{Lu}/\p{Ll} via the BMP Latin blocks
// commonly seen in scientific names (Latin-1 Supplement, Latin Extended A/B, Greek lowercase).
fragment UPPER : [A-ZÀ-ÖØ-Þ] ;
fragment LOWER : [a-zà-öø-ÿα-ωāăąćčďđēěğīĭıłńňōőřśšţťůūűźżž] ;

UPPER_WORD
    : UPPER (UPPER | LOWER)+ ('-' (UPPER | LOWER)+)?
    ;

// Apostrophes in epithets: "o'donelli", "d'urvilleana".
LOWER_WORD
    : LOWER+ (('\'' | '-') LOWER+)*
    ;

WS      : [ \t\r\n]+ -> skip ;
