grammar SciName;

@parser::header {
import org.gbif.nameparser.util.RankUtils;
}

@parser::members {
/** True if the lexeme (lower-cased, with notho/agamo prefix stripped) is a known infraspecific rank marker. */
private static boolean isInfraspecRank(String text) {
  if (text == null) return false;
  String key = text.toLowerCase();
  if (key.startsWith("notho") || key.startsWith("agamo")) key = key.substring(5);
  return RankUtils.RANK_MARKER_MAP_INFRASPECIFIC.containsKey(key);
}

/** True if the lexeme is a known infrageneric rank marker. */
private static boolean isInfragenRank(String text) {
  if (text == null) return false;
  String key = text.toLowerCase();
  if (key.startsWith("notho") || key.startsWith("agamo")) key = key.substring(5);
  return RankUtils.RANK_MARKER_MAP_INFRAGENERIC.containsKey(key);
}

/**
 * Mirror of the original parser's UNALLOWED_EPITHETS plus AUTHOR_TOKEN small-word list. These
 * lowercase words almost never appear as scientific-name epithets — they are author-team
 * particles ("van", "der", "des") or generic English/Latin glue ("of", "the"). Treating them
 * as epithets in the structural grammar would steal them from the authorship rule.
 */
private static final java.util.Set<String> AUTHOR_PARTICLES = java.util.Set.of(
    // UNALLOWED_EPITHETS in ParsingJob (minus aff/cf/nr which are now their own QUALIFIER token)
    "and","des","from","ms","of","the","where",
    // short author particles
    "al","f","j","jr","sr","v","van","von","zu","zur","zum","bis",
    "da","de","di","do","du","den","der","del","dem","degli",
    "e","la","le","las","les","s","ter","t","y","fil","filius",
    "hort","jun","junior","sen","senior","cv","auct"
);

/** True if the lexeme should never be matched as an epithet (it's an author-team particle). */
private static boolean isAuthorParticle(String text) {
  return text != null && AUTHOR_PARTICLES.contains(text.toLowerCase());
}
}

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

// subg. Subgenus | sect. Section | etc. The semantic predicate only commits if the lower-case
// word is a known infrageneric rank marker — otherwise we let the parser try other alternatives
// (epithet sequences, authorship).
rankedInfrageneric
    : { isInfragenRank(_input.LT(1).getText()) }? LOWER_WORD DOT UPPER_WORD
    ;

// Author-particle gate: short Latin glue words ("van", "der", "des", "de") are not epithets.
// Rejecting them at prediction time lets the parser route them into the authorship rule.
epithet
    : { !isAuthorParticle(_input.LT(1).getText()) }? qualifier? HYBRID? LOWER_WORD
    ;

// Identification qualifiers placed in front of an epithet: "aff. nana", "cf. fugax", "nr. xx".
// The dot is optional ("Solenopsis cf fugax").
qualifier
    : QUALIFIER DOT?
    ;

// After the species epithet there can be:
//   - a ranked infraspecific (subsp. baltica)               → 3-parted with rank marker
//   - a middle epithet followed by a ranked infraspec       → 4-parted with rank marker
//   - a Greek-letter rank substitute (β mucosus)            → 3-parted with greek rank
//   - a middle epithet then a bare last epithet             → 4-parted without rank marker
//   - a bare last epithet (no rank marker)                  → 3-parted, last token → infraspec slot
epithetTail
    : rankedInfraspec
    | middleEpithet rankedInfraspec
    | middleEpithet bareInfraspec
    | bareInfraspec
    ;

middleEpithet
    : { !isAuthorParticle(_input.LT(1).getText()) }? HYBRID? LOWER_WORD
    ;

// last epithet without rank marker (e.g. "Hieracium vulgatum arrectariicaule")
bareInfraspec
    : { !isAuthorParticle(_input.LT(1).getText()) }? qualifier? HYBRID? LOWER_WORD
    ;

// rank-marker. epithet  — semantic predicate gates on the rank marker being known to RankUtils.
// Two forms: classic dotted rank marker, or a Greek-letter substitute. Optional infraspecific
// qualifier placed before the rank marker ("cf. var. brachynodus"). The predicate runs against
// the lower-case word that would become the rank marker (skipping the optional qualifier slot).
rankedInfraspec
    : qualifier? { isInfraspecRank(_input.LT(1).getText()) }? LOWER_WORD DOT HYBRID? LOWER_WORD
    | qualifier? GREEK_RANK HYBRID? LOWER_WORD
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

// Greek letters and other single-letter rank substitutes ("Genus species α epi") that the
// existing parser treats like a rank marker. Also "***" used in old fungi names as a rank stand-in.
GREEK_RANK : [αβγδεζηθικλμνξοπρστυφχψω⍺] | '***' ;

// Identification qualifier keywords. Must precede LOWER_WORD so that "aff" / "cf" / "nr" win
// the lexer tie against the more general lowercase-word rule.
QUALIFIER : 'aff' | 'cf' | 'nr' ;

// Initial like "L." or "J.D.", optionally with a hyphenated lowercase tail like "C.-k."
INITIAL : [A-Z] '.' ('-' [a-z]+ '.')? ([A-Z] '.' ('-' [a-z]+ '.')?)* ;

YEAR    : [12] [0-9] [0-9] [0-9?] [a-h?]? ;

// Latin/extended Latin letter classes via Unicode properties. ANTLR4 supports \p{...} ranges.
fragment UPPER : [\p{Lu}] ;
fragment LOWER : [\p{Ll}] ;

// UPPER_WORD: must start with uppercase, may contain hyphens or apostrophes mid-word (e.g.
// "Müller-Aargauer", "O'Donelli"). The leading uppercase is required so we don't consume
// epithets like "d'urvilleana".
UPPER_WORD
    : UPPER (UPPER | LOWER)+ (('-' | '\'') (UPPER | LOWER)+)*
    ;

// Apostrophes in epithets: "o'donelli", "d'urvilleana". Multiple hyphens are common too
// ("friderici-et-pauli"). After an apostrophe, uppercase is allowed so author names like
// "d'Urv." lex as a single LOWER_WORD token instead of breaking on the apostrophe.
LOWER_WORD
    : LOWER+ (('\'' | '-') (UPPER | LOWER)+)*
    ;

WS      : [ \t\r\n]+ -> skip ;
