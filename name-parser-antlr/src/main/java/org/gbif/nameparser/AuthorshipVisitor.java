package org.gbif.nameparser;

import org.gbif.nameparser.antlr.SciNameBaseVisitor;
import org.gbif.nameparser.antlr.SciNameParser;
import org.gbif.nameparser.api.Authorship;

/**
 *
 */
public class AuthorshipVisitor extends SciNameBaseVisitor<Authorship> {

  @Override
  public Authorship visitScientificName(SciNameParser.ScientificNameContext ctx) {
    return super.visitScientificName(ctx);
  }
}
