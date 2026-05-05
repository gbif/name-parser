/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.nameparser.antlr;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Runs the {@link SciNameParser} against a strongly-normalized scientific name string and
 * surfaces a {@link NameComponents} POJO matching the slot layout that the old NAME_PATTERN
 * regex used to expose via {@code matcher.group(N)}.
 *
 * Returns {@link Optional#empty()} when the input cannot even reach a monomial, or when a
 * structural rule was matched but the rank-marker token is not in {@link RankUtils} — same
 * semantic as {@code matcher.find() == false} in the old code.
 */
public final class AntlrNameMatcher {
  private static final Logger LOG = LoggerFactory.getLogger(AntlrNameMatcher.class);

  private AntlrNameMatcher() {}

  public static Optional<NameComponents> match(String normalizedName) {
    if (normalizedName == null || normalizedName.isEmpty()) {
      return Optional.empty();
    }

    final String source = normalizedName;
    InterruptibleCharStream cs = new InterruptibleCharStream(source);
    SciNameLexer lexer = new SciNameLexer(cs);
    lexer.removeErrorListeners();
    ThrowingErrorListener errorListener = new ThrowingErrorListener();
    lexer.addErrorListener(errorListener);

    CommonTokenStream tokens = new CommonTokenStream(lexer);
    SciNameParser parser = new SciNameParser(tokens);
    // Stay silent on parser errors. ANTLR's default error strategy will try to recover by
    // skipping tokens — for our use that's fine because the unconsumed tail becomes the
    // remainder slot, mirroring NAME_PATTERN group 23. The grammar has no EOF anchor so
    // the parser stops naturally at the first token that can't extend the rule.
    parser.removeErrorListeners();

    SciNameParser.NameContext tree;
    try {
      tree = parser.name();
    } catch (ParseFailedException e) {
      LOG.debug("ANTLR parse failed for '{}': {}", normalizedName, e.getMessage());
      return Optional.empty();
    }

    if (tree.monomial() == null) {
      return Optional.empty();
    }

    NameComponents nc = new NameComponents();
    PopulatingListener listener = new PopulatingListener(nc, source);
    new ParseTreeWalker().walk(listener, tree);

    // collect unconsumed tokens as the remainder string (mirrors NAME_PATTERN group 23).
    // Source-substring slicing keeps original spacing (":377", "(sic)" etc).
    //
    // Two sources of "unconsumed" tokens:
    //   1. The trailing slice — tokens beyond parser.getCurrentToken() that the parser
    //      never reached because the rule simply ran out of matchable input.
    //   2. Error-recovered tokens — when adaptive prediction commits to an alternative
    //      whose body can't fully match, ANTLR's default error strategy advances past the
    //      offending tokens. Those land directly under the root rule context as terminal
    //      children rather than under a named subrule, so we walk the tree once to detect
    //      them. Without this, "Combretum Loefl. (1758) (= Grislea L. 1753)." would lose
    //      its trailing synonym block when the second LPAREN couldn't be re-parsed.
    // Force the lexer to drain the rest of the stream so we see ALL trailing tokens. The
    // parser may have stopped early (lazy buffering) before pulling the full input.
    tokens.fill();
    java.util.List<int[]> spans = new java.util.ArrayList<>();
    int nextIdx = parser.getCurrentToken().getTokenIndex();
    int lastIdx = tokens.size() - 1;
    int firstStart = -1;
    int lastStop = -1;
    for (int i = nextIdx; i < lastIdx; i++) {
      Token t = tokens.get(i);
      if (t.getType() == Token.EOF) break;
      if (firstStart < 0) firstStart = t.getStartIndex();
      lastStop = t.getStopIndex();
    }
    if (firstStart >= 0 && lastStop >= firstStart) {
      spans.add(new int[]{firstStart, lastStop});
    }
    // Pick up tokens absorbed by error recovery: any TerminalNode child of `name` whose
    // parent isn't one of the rule contexts is an error node.
    for (int i = 0; i < tree.getChildCount(); i++) {
      org.antlr.v4.runtime.tree.ParseTree child = tree.getChild(i);
      if (child instanceof org.antlr.v4.runtime.tree.TerminalNode) {
        Token t = ((org.antlr.v4.runtime.tree.TerminalNode) child).getSymbol();
        if (t.getType() == Token.EOF) continue;
        spans.add(new int[]{t.getStartIndex(), t.getStopIndex()});
      }
    }
    if (!spans.isEmpty()) {
      spans.sort(java.util.Comparator.comparingInt(a -> a[0]));
      int s = spans.get(0)[0];
      int e = spans.get(spans.size() - 1)[1];
      if (s >= 0 && e >= s && e < source.length()) {
        nc.remainder = source.substring(s, e + 1);
      }
    }
    return Optional.of(nc);
  }

  /** Error listener that converts the first ANTLR error into a checked exception we can catch. */
  private static class ThrowingErrorListener extends BaseErrorListener {
    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                            int line, int charPositionInLine, String msg,
                            RecognitionException e) {
      throw new ParseFailedException(msg);
    }
  }

  private static class ParseFailedException extends RuntimeException {
    ParseFailedException(String msg) { super(msg); }
  }

  /** Walks the parse tree and populates {@link NameComponents}. */
  private static class PopulatingListener extends SciNameBaseListener {
    private final NameComponents nc;
    private final String source;

    PopulatingListener(NameComponents nc, String source) {
      this.nc = nc;
      this.source = source;
    }

    /** Returns the original input substring covered by ctx, preserving whitespace as-is. */
    private String sourceTextOf(org.antlr.v4.runtime.ParserRuleContext ctx) {
      if (ctx == null || ctx.getStart() == null || ctx.getStop() == null) return null;
      int s = ctx.getStart().getStartIndex();
      int e = ctx.getStop().getStopIndex();
      if (s < 0 || e < s || e >= source.length()) return null;
      return source.substring(s, e + 1);
    }

    @Override
    public void enterMonomial(SciNameParser.MonomialContext ctx) {
      if (ctx.HYBRID() != null) {
        nc.hybridGenus = true;
      }
      if (ctx.QMARK() != null) {
        nc.placeholderGenus = true;
        nc.monomialOrGenus = "?";
      } else if (ctx.UPPER_WORD() != null) {
        nc.monomialOrGenus = ctx.UPPER_WORD().getText();
      } else if (ctx.INITIAL() != null) {
        // abbreviated genus, e.g. "N." in "N. giraldo"
        nc.monomialOrGenus = ctx.INITIAL().getText();
      }
    }

    @Override
    public void enterSubgenusParens(SciNameParser.SubgenusParensContext ctx) {
      nc.subgenusParens = ctx.UPPER_WORD().getText();
    }

    @Override
    public void enterRankedInfrageneric(SciNameParser.RankedInfragenericContext ctx) {
      // grammar predicate already validated the rank marker
      nc.infragenericRankMarker = ctx.LOWER_WORD().getText();
      nc.infragenericEpithet = ctx.UPPER_WORD().getText();
    }

    @Override
    public void enterEpithet(SciNameParser.EpithetContext ctx) {
      if (ctx.qualifier() != null) {
        nc.specificQualifier = qualifierText(ctx.qualifier());
      }
      if (ctx.HYBRID() != null) {
        nc.hybridSpecies = true;
      }
      nc.specificEpithet = ctx.LOWER_WORD().getText();
    }

    private static String qualifierText(SciNameParser.QualifierContext q) {
      // surface as "aff", "cf", "nr" — ParsingJob.setEpithetQualifier appends the trailing dot
      return q.QUALIFIER().getText();
    }

    @Override
    public void enterMiddleEpithet(SciNameParser.MiddleEpithetContext ctx) {
      nc.middleEpithet = ctx.LOWER_WORD().getText();
    }

    @Override
    public void enterBareInfraspec(SciNameParser.BareInfraspecContext ctx) {
      // a bare last epithet without a rank marker — slot it as the infraspecific epithet
      if (ctx.qualifier() != null) {
        nc.infraspecificQualifier = qualifierText(ctx.qualifier());
      }
      if (ctx.HYBRID() != null) {
        nc.hybridInfraspecies = true;
      }
      nc.infraspecificEpithet = ctx.LOWER_WORD().getText();
    }

    @Override
    public void enterRankedInfraspec(SciNameParser.RankedInfraspecContext ctx) {
      if (ctx.qualifier() != null) {
        nc.infraspecificQualifier = qualifierText(ctx.qualifier());
      }
      String marker;
      String epithet = null;
      if (ctx.GREEK_RANK() != null) {
        // Greek-letter rank substitute (α, β, γ ...) — treat as an infraspecific rank marker
        marker = ctx.GREEK_RANK().getText();
        epithet = ctx.LOWER_WORD(0).getText();
      } else if (ctx.COMPOSITE_RANK() != null) {
        // composite token with embedded dot, e.g. "t.infr.", "f.sp." — strip the dot(s)
        // for the rank-marker key so the downstream lookup hits "tinfr" / "fsp"
        marker = ctx.COMPOSITE_RANK().getText().replace(".", "");
        epithet = ctx.LOWER_WORD(0).getText();
      } else {
        // dotted form ("subsp. baltica"), bare form ("natio danubicus") or indet ("var.")
        marker = ctx.LOWER_WORD(0).getText();
        if (ctx.LOWER_WORD().size() > 1) {
          epithet = ctx.LOWER_WORD(1).getText();
        }
      }
      nc.infraspecificRankMarker = marker;
      if (ctx.HYBRID() != null) {
        nc.hybridInfraspecies = true;
      }
      nc.infraspecificEpithet = epithet;
    }

    @Override
    public void enterAuthorship(SciNameParser.AuthorshipContext ctx) {
      nc.hasAuthorship = true;
    }

    @Override
    public void enterBasionymGroup(SciNameParser.BasionymGroupContext ctx) {
      SciNameParser.CombPartContext cp = ctx.combPart();
      if (cp != null) {
        AuthorshipParts ap = extractAuthorshipParts(cp);
        nc.basionymExAuthors = ap.exAuthors;
        nc.basionymAuthors = ap.authors;
        nc.basionymSanctAuthor = ap.sanct;
        nc.basionymYear = ap.year;
      }
    }

    @Override
    public void exitAuthorship(SciNameParser.AuthorshipContext ctx) {
      // The combination authorship is whatever combPart sits at the top of the authorship rule.
      // It is the immediate combPart child if present; otherwise nothing to do.
      SciNameParser.CombPartContext cp = ctx.combPart();
      if (cp != null) {
        AuthorshipParts ap = extractAuthorshipParts(cp);
        nc.combinationExAuthors = ap.exAuthors;
        nc.combinationAuthors = ap.authors;
        nc.combinationSanctAuthor = ap.sanct;
        nc.combinationYear = ap.year;
      }
    }

    private AuthorshipParts extractAuthorshipParts(SciNameParser.CombPartContext cp) {
      AuthorshipParts out = new AuthorshipParts();
      if (cp.authorBlob() != null) {
        // pull the original input text (preserving whitespace) so e.g. "A.R.Bean" doesn't
        // come out as "A.R. Bean" after token rejoin
        String blob = sourceTextOf(cp.authorBlob());
        ExSplit es = splitOnEx(blob);
        out.exAuthors = es.ex;
        out.authors = es.main;
      }
      if (cp.sanctAuth() != null) {
        out.sanct = sourceTextOf(cp.sanctAuth());
      }
      if (cp.yearMaybe() != null && cp.yearMaybe().YEAR() != null) {
        out.year = cp.yearMaybe().YEAR().getText();
      }
      return out;
    }
  }

  private static class AuthorshipParts {
    String exAuthors;
    String authors;
    String sanct;
    String year;
  }

  private static class ExSplit {
    String ex;
    String main;
  }

  /** Matches the literal "ex" (with optional trailing dot) when surrounded by a word boundary
   *  on each side. Compatible with "hort. ex Hoffmanns.", "hort.ex Hoffmanns." (post-NORM
   *  punctuation that strips spaces around dots), and "Backer ex K.Heyne". */
  private static final java.util.regex.Pattern EX_AUTHOR_SPLIT =
      java.util.regex.Pattern.compile("(?i)(?<=\\W)ex\\.?(?=\\s|\\b)");

  private static ExSplit splitOnEx(String blob) {
    ExSplit r = new ExSplit();
    if (blob == null) return r;
    // find the LAST occurrence — an early "ex" could be part of an author name, the canonical
    // ex-author marker comes between authors so it sits at most one author from the end
    java.util.regex.Matcher m = EX_AUTHOR_SPLIT.matcher(blob);
    int matchStart = -1;
    int matchEnd = -1;
    while (m.find()) {
      matchStart = m.start();
      matchEnd = m.end();
    }
    if (matchStart >= 0) {
      r.ex = blob.substring(0, matchStart).trim();
      r.main = blob.substring(matchEnd).trim();
      // drop a leading dot left over after splitting "hort.ex" → "hort." / "Hoffmanns."
      if (r.main.startsWith(".")) {
        r.main = r.main.substring(1).trim();
      }
    } else {
      r.main = blob.trim();
    }
    return r;
  }

}
