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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Standalone authorship matcher used by {@code AuthorshipParsingJob} for
 * {@link org.gbif.nameparser.api.NameParser#parseAuthorship}. Returns ex/auth/sanct/year
 * strings already split for both basionym and combination — the existing
 * {@code ParsingJob.parseAuthorship(ex, auth, year)} helper turns those into an
 * {@code Authorship} object.
 *
 * Implementation note: reuses the {@link SciNameParser#authorship()} rule from the main
 * grammar so that the basionym-in-parens / combination split logic is defined in one place.
 */
public final class AntlrAuthorshipMatcher {
  private static final Logger LOG = LoggerFactory.getLogger(AntlrAuthorshipMatcher.class);

  public static final class AuthorshipMatch {
    public String basionymExAuthors;
    public String basionymAuthors;
    public String basionymSanctAuthor;
    public String basionymYear;
    public String combinationExAuthors;
    public String combinationAuthors;
    public String combinationSanctAuthor;
    public String combinationYear;
    public String remainder;
  }

  private AntlrAuthorshipMatcher() {}

  public static Optional<AuthorshipMatch> match(String authorship) {
    if (authorship == null || authorship.isEmpty()) {
      return Optional.empty();
    }

    final String source = authorship;
    InterruptibleCharStream cs = new InterruptibleCharStream(source);
    SciNameLexer lexer = new SciNameLexer(cs);
    lexer.removeErrorListeners();
    ThrowingErrorListener errorListener = new ThrowingErrorListener();
    lexer.addErrorListener(errorListener);

    CommonTokenStream tokens = new CommonTokenStream(lexer);
    SciNameParser parser = new SciNameParser(tokens);
    parser.removeErrorListeners();
    parser.addErrorListener(errorListener);

    SciNameParser.AuthorshipContext tree;
    try {
      tree = parser.authorship();
    } catch (ParseFailedException e) {
      LOG.debug("ANTLR authorship parse failed for '{}': {}", authorship, e.getMessage());
      return Optional.empty();
    }

    AuthorshipMatch m = new AuthorshipMatch();

    SciNameParser.BasionymGroupContext bg = tree.basionymGroup();
    if (bg != null && bg.combPart() != null) {
      AuthorshipParts ap = extractAuthorshipParts(bg.combPart(), source);
      m.basionymExAuthors = ap.exAuthors;
      m.basionymAuthors = ap.authors;
      m.basionymSanctAuthor = ap.sanct;
      m.basionymYear = ap.year;
    }
    SciNameParser.CombPartContext cp = tree.combPart();
    if (cp != null) {
      AuthorshipParts ap = extractAuthorshipParts(cp, source);
      m.combinationExAuthors = ap.exAuthors;
      m.combinationAuthors = ap.authors;
      m.combinationSanctAuthor = ap.sanct;
      m.combinationYear = ap.year;
    }

    tokens.fill();
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
    if (firstStart >= 0 && lastStop >= firstStart && lastStop < source.length()) {
      m.remainder = source.substring(firstStart, lastStop + 1);
    }
    return Optional.of(m);
  }

  /** See {@link AntlrNameMatcher#splitOnEx} — same regex, lifted here so the standalone
   *  authorship matcher splits "Wedd.ex Sch.Bip." too even after NORM_PUNCTUATIONS has
   *  collapsed the whitespace around the dot. */
  private static final java.util.regex.Pattern EX_AUTHOR_SPLIT =
      java.util.regex.Pattern.compile("(?i)(?<=\\W)ex\\.?(?=\\s|\\b)");

  private static AuthorshipParts extractAuthorshipParts(SciNameParser.CombPartContext cp, String source) {
    AuthorshipParts out = new AuthorshipParts();
    if (cp.authorBlob() != null) {
      String blob = sourceTextOf(cp.authorBlob(), source);
      if (blob != null) {
        java.util.regex.Matcher m = EX_AUTHOR_SPLIT.matcher(blob);
        int matchStart = -1;
        int matchEnd = -1;
        while (m.find()) {
          matchStart = m.start();
          matchEnd = m.end();
        }
        if (matchStart >= 0) {
          out.exAuthors = blob.substring(0, matchStart).trim();
          String tail = blob.substring(matchEnd).trim();
          if (tail.startsWith(".")) tail = tail.substring(1).trim();
          out.authors = tail;
        } else {
          out.authors = blob.trim();
        }
      }
    }
    if (cp.sanctAuth() != null) {
      out.sanct = sourceTextOf(cp.sanctAuth(), source);
    }
    if (cp.yearMaybe() != null && cp.yearMaybe().YEAR() != null) {
      out.year = cp.yearMaybe().YEAR().getText();
    }
    return out;
  }

  private static String sourceTextOf(org.antlr.v4.runtime.ParserRuleContext ctx, String source) {
    if (ctx == null || ctx.getStart() == null || ctx.getStop() == null) return null;
    int s = ctx.getStart().getStartIndex();
    int e = ctx.getStop().getStopIndex();
    if (s < 0 || e < s || e >= source.length()) return null;
    return source.substring(s, e + 1);
  }

  private static class AuthorshipParts {
    String exAuthors;
    String authors;
    String sanct;
    String year;
  }

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
}
