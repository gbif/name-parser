grammar SciName;

@parser::header {
import org.antlr.v4.runtime.Token;
import org.gbif.nameparser.util.RankUtils;
}

@parser::members {
/** True if the lexeme (lower-cased, with notho/agamo prefix stripped) is a known infraspecific
 *  rank marker. We also accept SPECIFIC markers (sp, ssp, agg, ...) — in zoological trinomials
 *  "sp." is used as a subspecies-rank stand-in. */
private static boolean isInfraspecRank(String text) {
  if (text == null) return false;
  String key = text.toLowerCase();
  if (key.startsWith("notho") || key.startsWith("agamo")) key = key.substring(5);
  return RankUtils.RANK_MARKER_MAP_INFRASPECIFIC.containsKey(key)
      || RankUtils.RANK_MARKER_MAP_SPECIFIC.containsKey(key);
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
    "al","ex","f","j","jr","sr","v","van","von","zu","zur","zum","bis",
    "da","de","di","do","du","den","der","del","dem","degli",
    "e","la","le","las","les","s","ter","t","y","fil","filius",
    "hort","jun","junior","sen","senior","cv","auct"
);

/** True if the lexeme should never be matched as an epithet (it's an author-team particle). */
private static boolean isAuthorParticle(String text) {
  return text != null && AUTHOR_PARTICLES.contains(text.toLowerCase());
}

/** rankedInfraspec dotted form ("var. baltica" / "var. ×alpina"): LT(1) is a known rank
 *  marker, LT(2) is DOT, and the trailing position (skipping an optional HYBRID) is a
 *  LOWER_WORD that is not an author particle. */
private boolean isDottedRankFollowedByEpithet() {
  Token t1 = _input.LT(1);
  if (t1 == null || !isInfraspecRank(t1.getText())) return false;
  Token t2 = _input.LT(2);
  if (t2 == null || t2.getType() != DOT) return false;
  int idx = 3;
  Token t = _input.LT(idx);
  if (t != null && t.getType() == HYBRID) {
    idx++;
    t = _input.LT(idx);
  }
  if (t == null || t.getType() != LOWER_WORD) return false;
  return !isAuthorParticle(t.getText());
}

/** rankedInfraspec bare form ("natio danubicus"): LT(1) is a known rank marker, LT(2)
 *  (skipping HYBRID) is a LOWER_WORD that is not an author particle. */
private boolean isBareRankFollowedByEpithet() {
  Token t1 = _input.LT(1);
  if (t1 == null || !isInfraspecRank(t1.getText())) return false;
  int idx = 2;
  Token t = _input.LT(idx);
  if (t != null && t.getType() == HYBRID) {
    idx++;
    t = _input.LT(idx);
  }
  if (t == null || t.getType() != LOWER_WORD) return false;
  return !isAuthorParticle(t.getText());
}

/** rankedInfraspec indet form ("var." with nothing usable after): the dotted form would
 *  otherwise consume the rank+epithet, so indet only fires when there's no usable epithet
 *  (end-of-input, an author-particle LOWER_WORD, an UPPER_WORD author, or LPAREN basionym). */
private boolean isIndetRank() {
  Token t1 = _input.LT(1);
  if (t1 == null || !isInfraspecRank(t1.getText())) return false;
  Token t2 = _input.LT(2);
  if (t2 == null || t2.getType() != DOT) return false;
  Token t3 = _input.LT(3);
  if (t3 == null || t3.getType() == Token.EOF) return true;
  if (t3.getType() == HYBRID) {
    Token t4 = _input.LT(4);
    return t4 == null || t4.getType() != LOWER_WORD || isAuthorParticle(t4.getText());
  }
  if (t3.getType() != LOWER_WORD) return true;
  return isAuthorParticle(t3.getText());
}

/** Look ahead for an infraspecific-rank marker between the species epithet and the next
 *  structural slot. Used to gate the `inlineAuthor epithetTail` alternative at the species
 *  level — without this, ANTLR's adaptive prediction greedily consumes UPPER author tokens
 *  even when no rank marker follows ("Taraxacum vulgaris Backer ex K.Heyne" should keep
 *  Backer in authorship, not as an inline author).
 *
 *  Scans up to 40 tokens; stops at LPAREN (basionym group) since rank markers don't sit
 *  inside parens. A LOWER_WORD candidate counts only when followed by either a DOT plus
 *  another non-author-particle LOWER_WORD/UPPER_WORD/LPAREN epithet, or directly by a
 *  non-author-particle LOWER_WORD (bare-marker form like "natio danubicus"). This guards
 *  against author-particle confusables — "f." in "Baker f. ex Rose" is FORM in the rank
 *  map but actually filius here. */
private boolean hasInfraspecRankAhead() {
  for (int i = 1; i < 40; i++) {
    Token t = _input.LT(i);
    if (t == null || t.getType() == Token.EOF) return false;
    int type = t.getType();
    if (type == LPAREN) return false;
    if (type == COMPOSITE_RANK || type == GREEK_RANK) return true;
    if (type == LOWER_WORD) {
      String s = t.getText();
      if (isInfraspecRank(s)) {
        Token next = _input.LT(i + 1);
        if (next == null) continue;
        int nt = next.getType();
        if (nt == DOT) {
          Token after = _input.LT(i + 2);
          if (after == null || after.getType() == Token.EOF) return true;
          int at = after.getType();
          if (at == UPPER_WORD || at == LPAREN) return true;
          if (at == LOWER_WORD && !isAuthorParticle(after.getText())) return true;
        } else if (nt == LOWER_WORD && !isAuthorParticle(next.getText())) {
          return true;
        }
      }
    }
  }
  return false;
}

/** Heuristic: a lowercase-starting word that contains an uppercase letter looks like an
 *  author name (e.g. "d'Urv", "deBary"). Used to gate inlineAuthor against epithets. */
private static boolean looksLikeAuthorWord(String text) {
  if (text == null || text.isEmpty()) return false;
  if (!Character.isLowerCase(text.charAt(0))) return false;
  for (int i = 1; i < text.length(); i++) {
    if (Character.isUpperCase(text.charAt(i))) return true;
  }
  return false;
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
// Inline authors: a genus or species author can appear between the structural slot and the
// rank marker that follows ("Centaurea L. subg. Jacea", "Salix repens L. subsp. galeifolia").
// They are surfaced via inlineAuthor (which the parent rule pins next to a rank marker so
// adaptive prediction can disambiguate) and discarded by the listener.

// Note: no EOF — remainder tokens are read programmatically by the matcher so that names with
// trailing junk still parse to PARTIAL state instead of failing outright.
name
    : monomial
      ( inlineAuthor ( subgenusParens | rankedInfrageneric )
      | subgenusParens
      | rankedInfrageneric
      )?
      ( epithet
        ( { hasInfraspecRankAhead() }? inlineAuthor epithetTail
        | epithetTail
        )?
      )?
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
    : middleEpithet rankedInfraspec
    | rankedInfraspec
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
// Forms supported:
//   - classic dotted rank marker:    "subsp. baltica"
//   - bare lower-case rank word:     "natio danubicus"   (no dot — used by NATIO etc.)
//   - composite tokens:              "t.infr. arrectariicaule", "f.sp. avenae"
//   - indeterminate (no epithet):    "var."              (used by indetNames test)
//   - Greek-letter substitute:       "β mucosus"
// Optional infraspecific qualifier placed before the rank marker ("cf. var. brachynodus").
//
// The dotted and bare forms additionally check that the trailing epithet token is not a
// known author particle ("ex", "f", "filius" ...) — otherwise inputs like "Baker f. ex Rose"
// would mis-parse "f. ex" as a FORM rank marker.
rankedInfraspec
    : qualifier? { isDottedRankFollowedByEpithet() }? LOWER_WORD DOT HYBRID? LOWER_WORD
    | qualifier? COMPOSITE_RANK HYBRID? LOWER_WORD
    | qualifier? { isBareRankFollowedByEpithet() }? LOWER_WORD HYBRID? LOWER_WORD
    | qualifier? { isIndetRank() }? LOWER_WORD DOT
    | qualifier? GREEK_RANK HYBRID? LOWER_WORD
    ;

// Inline authors that may sit between a structural slot and the next rank marker —
// "Centaurea L. subg. Jacea", "Salix repens L. subsp. galeifolia ...". The listener does
// not surface these; they are consumed and discarded so the rest of the structural parse
// can succeed.
//
// Structurally pinned: the parent rule wraps `inlineAuthor` so that a rank-marker rule
// MUST follow. ANTLR's adaptive prediction enumerates alternatives with and without the
// inline author and picks the one whose follow-on actually matches — no extra semantic
// predicate needed.
//
// Token shapes are constrained to author-shaped tokens (initials, capitalised author
// names, "Z-X" hyphen-initials, or apostrophe-mixed words like "d'Urv") so an actual
// epithet cannot be mis-consumed.
inlineAuthor
    : ( INITIAL
      | UPPER_WORD DOT?
      | SINGLE_UPPER DOT?
      | { looksLikeAuthorWord(_input.LT(1).getText()) }? LOWER_WORD DOT?
      )+
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
    | SINGLE_UPPER DOT?
    | AMP
    | COMMA
    | SEMI
    ;

sanctAuth
    : UPPER_WORD DOT?
    ;

// Balanced LPAREN/RPAREN around the year so adaptive prediction can't drop the RPAREN
// when there's a second paren group trailing ("Loefl. (1758) (= Grislea L. 1753)" — without
// the balance constraint the closing RPAREN of "(1758)" would be left for remainder).
yearMaybe
    : COMMA? LPAREN YEAR RPAREN
    | COMMA? YEAR
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

// Composite rank markers that contain an embedded dot (so the lexer can't split them on DOT
// without losing structure). The trailing dot is optional because NORM_PUNCTUATIONS may have
// stripped it. Declared before LOWER_WORD so longest-match wins.
COMPOSITE_RANK : 't.infr' '.'? | 'f.sp' '.'? ;

// Identification qualifier keywords. Must precede LOWER_WORD so that "aff" / "cf" / "nr" win
// the lexer tie against the more general lowercase-word rule.
QUALIFIER : 'aff' | 'cf' | 'nr' ;

// Initial like "L." or "J.D.", optionally with a hyphenated lowercase tail like "C.-k."
INITIAL : [A-Z] '.' ('-' [a-z]+ '.')? ([A-Z] '.' ('-' [a-z]+ '.')?)* ;

YEAR    : [12] [0-9] [0-9] [0-9?] [a-h?]? ;

// Bare numbers that are not a year (page references, ":377" trailers etc.). Captured as
// tokens so they end up in the remainder slot rather than crashing the lexer.
NUMBER  : [0-9]+ ;

// Latin/extended Latin letter classes via Unicode properties. ANTLR4 supports \p{...} ranges.
fragment UPPER : [\p{Lu}] ;
fragment LOWER : [\p{Ll}] ;

// UPPER_WORD: must start with uppercase. Requires either two or more letters (regular form
// like "Müller-Aargauer" / "O'Donelli") or a hyphen-compound containing a single leading
// letter ("Z-X" — author initial in the noHybrids test). A bare single uppercase letter
// without a continuation is matched by SINGLE_UPPER instead, so monomials still need at
// least two characters.
UPPER_WORD
    : UPPER (UPPER | LOWER)+ (('-' | '\'') (UPPER | LOWER)+)*
    | UPPER ('-' (UPPER | LOWER)+)+
    ;

// Lone uppercase letter — used as an author-team token ("Wang F"). Declared after UPPER_WORD
// so the longest-match rule keeps "L." → INITIAL (2 chars beats SINGLE_UPPER's 1 char).
SINGLE_UPPER : UPPER ;

// Apostrophes in epithets: "o'donelli", "d'urvilleana". Multiple hyphens are common too
// ("friderici-et-pauli"). After an apostrophe, uppercase is allowed so author names like
// "d'Urv." lex as a single LOWER_WORD token instead of breaking on the apostrophe.
LOWER_WORD
    : LOWER+ (('\'' | '-') (UPPER | LOWER)+)*
    ;

WS      : [ \t\r\n]+ -> skip ;

// Catch-all for any character the rules above don't recognise (digit separators like the
// "-" in "1880-1885", the "/" in date ranges, the "377" page-reference junk after a colon).
// Declared last so longest-match keeps it from stealing matches from typed tokens. Anything
// captured here is left for the matcher to surface as remainder.
//
// Underscore is intentionally excluded: PR2-style OTU strings like "Basal_Cryptophyceae-1"
// must remain unparseable (see ParsingJob's note about not replacing underscores).
OTHER   : ~[_] ;
