package org.gbif.nameparser;

import javax.annotation.Nullable;

import com.google.common.base.Strings;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.gbif.nameparser.antlr.SciNameLexer;
import org.gbif.nameparser.antlr.SciNameParser;
import org.gbif.nameparser.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The GBIF name parser build on ANTLR v4 ALL(*) grammar.
 */
public class NameParserANTLR implements org.gbif.nameparser.api.NameParser {
  private static Logger LOG = LoggerFactory.getLogger(NameParserANTLR.class);

  private static ParsedName unparsable(NameType type, String name) throws UnparsableNameException {
    throw new UnparsableNameException(type, name);
  }

  public ParsedName parse(String scientificName) throws UnparsableNameException {
    return parse(scientificName, Rank.UNRANKED);
  }
  
  @Override
  public ParsedName parse(final String scientificName, Rank rank) throws UnparsableNameException {
    return parse(scientificName, Rank.UNRANKED, null);
  }
  
  /**
   * Fully parse the supplied name also trying to extract authorships, a conceptual sec reference, remarks or notes
   * on the nomenclatural status. In some cases the authorship parsing proves impossible and this nameparser will
   * return null.
   *
   * For strings which are no scientific names and scientific names that cannot be expressed by the ParsedName class
   * the parser will throw an UnparsableException with a given NameType and the original, unparsed name. This is the
   * case for all virus names and proper hybrid formulas, so make sure you catch and process this exception.
   *
   * @param scientificName the full scientific name to parse
   * @param rank the rank of the name if it is known externally. Helps identifying infrageneric names vs bracket authors
   *
   * @throws UnparsableNameException
   */
  public ParsedName parse(String scientificName, Rank rank, @Nullable NomCode code) throws UnparsableNameException {
    if (Strings.isNullOrEmpty(scientificName)) {
      unparsable(NameType.NO_NAME, null);
    }
    System.out.println("\n" + scientificName);

    SciNameLexer lexer = new SciNameLexer(CharStreams.fromString(scientificName));

    // Get a list of matched tokens
    CommonTokenStream tokens = new CommonTokenStream(lexer);

    SciNameParser parser = new SciNameParser(tokens);

    // use visitor to transform to ParsedName
    ParsedNameVisitor visitor = new ParsedNameVisitor();
    return visitor.visit(parser.scientificName());

  }
  
  @Override
  public void close() throws Exception {
    // nothing to do
  }
}
