grammar SciName;

options {
    language=Java;
}

// Lexer Rules
// for unicode codepoint classifications see https://github.com/antlr/grammars-v4/tree/master/unicode


NOTHO:                  'notho';
RANK:                   'subsp'|'ssp'|'var'|'form'|'f';
ETAL:                   ('&' | 'and' | 'et') SPACE 'al' DOT?;
MONOMIAL:               LETTER_NAME_UC LETTER_NAME_LC+;
EPITHET:                LETTER_NAME_LC LETTER_NAME_LC+;
AUTHOR_INITIALS:        (LETTER_AUTHOR_UC (DOT|SPACE) SPACE*)+?;
AUTHOR:                 LETTER_AUTHOR_UC LETTER_AUTHOR_UC* LETTER_AUTHOR_LC* DOT?;
AUTHOR2:                AUTHOR_INITIALS? LETTER_AUTHOR_UC LETTER_AUTHOR_UC* LETTER_AUTHOR_LC* DOT?;
AUTHOR_DELIM:           ',' | '&';
YEAR:                   [12] DIGIT DIGIT DIGIT;
DOT:                    '.';
LR_BRACKET:             '(';
RR_BRACKET:             ')';
COMMA:                  ',';
SEMI:                   ';';
SINGLE_QUOTE:           '\'';
DOUBLE_QUOTE:           '"';
COLON:                  ':';
HYBRID_MARKER:          '×' SPACE?;
EXTINCT_MARKER:         '†';

OTU_BOLD:               ('BOLD'|'bold')':'ALPHANUM ALPHANUM ALPHANUM ALPHANUM ALPHANUM ALPHANUM ALPHANUM;
OTU_SH:                 'SH' DIGIT DIGIT DIGIT DIGIT DIGIT DIGIT '.' DIGIT DIGIT 'FU';
VIRUS:                  'virus';

CONTROL:                [\u0000-\u001F]+    -> skip;
WS:                     SPACE+    -> skip;
ANY:                    . ;     // match any char

NUC: LETTER_NAME_UC+;
NLC: LETTER_NAME_LC+;
AUC: LETTER_AUTHOR_UC+;
ALC: LETTER_AUTHOR_LC+;
ALPH: ALPHANUM+;

fragment LETTER_NAME_UC:     [A-ZÏËÖÜÄÉÈČÁÀÆŒ];
fragment LETTER_NAME_LC:     [a-zïëöüäåéèčáàæœ];
fragment LETTER_AUTHOR_UC:   [\p{Lu}];   // upper case unicode letter, not numerical
fragment LETTER_AUTHOR_LC:   [\p{Ll}?-]; // lower case unicode letter, not numerical
fragment NOT_CYRILLIC:       [\P{Script=Cyrillic}];       // example for unicode usage
fragment DQUOTA_STRING:      '"' ( '\\'. | '""' | ~('"'| '\\') )* '"';
fragment SQUOTA_STRING:      '\'' ('\\'. | '\'\'' | ~('\'' | '\\'))* '\'';
fragment DIGIT:              [0-9];
fragment ALPHANUM:           [0-9A-Za-z];
fragment SPACE :             ' ' | '\t' | '\r' | '\n';


author:
    AUTHOR
    EOF
    ;

dot: DOT+ EOF;
auc: AUC EOF;
alc: ALC EOF;
nuc: NUC EOF;
nlc: NLC EOF;
alphanum: ALPH  EOF;

// ParserRules

scientificName:
    otu
    | latin
    | virus
    | hybridformula
    EOF
    ;

latin:
    monomial
    (species
        (subspecies? infraspecies)?
    )?
    ;

monomial:
    HYBRID_MARKER?
    MONOMIAL
    authorship
    ;

epithet:
    HYBRID_MARKER?
    EPITHET
    authorship
    ;

species: epithet;

rank:
    NOTHO?
    RANK
    DOT?
    ;

subspecies:
    epithet
    ;

infraspecies:
    rank?
    epithet
    ;

authorship:
    basauthorship?
    combauthorship?
    ;

combauthorship:
    authorteam
    (COMMA? YEAR)?
    ;

basauthorship:
    LR_BRACKET
    authorteam
    (COMMA? YEAR)?
    RR_BRACKET
    ;

authorteam:
    AUTHOR
    (AUTHOR_DELIM AUTHOR)*
    ETAL?
    ;

virus:
    VIRUS
    ;

otu:
    OTU_BOLD
    | OTU_SH
    ;

hybridformula:
    latin (HYBRID_MARKER latin)+
    ;
