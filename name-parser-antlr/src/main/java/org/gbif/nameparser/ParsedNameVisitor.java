package org.gbif.nameparser;

import org.gbif.nameparser.antlr.SciNameBaseVisitor;
import org.gbif.nameparser.antlr.SciNameParser;
import org.gbif.nameparser.api.ParsedName;

/**
 *
 */
public class ParsedNameVisitor extends SciNameBaseVisitor<ParsedName> {

  @Override
  public ParsedName visitScientificName(SciNameParser.ScientificNameContext ctx) {
    return super.visitScientificName(ctx);
  }
}
