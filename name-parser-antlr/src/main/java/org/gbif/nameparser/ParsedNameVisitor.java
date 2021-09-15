package org.gbif.nameparser;

import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.StringUtils;
import org.gbif.nameparser.antlr.SciNameBaseVisitor;
import org.gbif.nameparser.antlr.SciNameParser;
import org.gbif.nameparser.api.NameType;
import org.gbif.nameparser.api.ParsedName;
import org.gbif.nameparser.api.Rank;
import org.gbif.nameparser.util.RankUtils;

/**
 *
 */
public class ParsedNameVisitor extends SciNameBaseVisitor<ParsedName> {
  final ParsedName pn = new ParsedName();

  @Override
  public ParsedName visitScientificName(SciNameParser.ScientificNameContext ctx) {
    return super.visitScientificName(ctx);
  }

  @Override
  public ParsedName visitVirus(SciNameParser.VirusContext ctx) {
    pn.setType(NameType.VIRUS);
    return visitChildren(ctx);
  }

  @Override
  public ParsedName visitOtu(SciNameParser.OtuContext ctx) {
    pn.setType(NameType.OTU);
    TerminalNode otu = ctx.OTU_BOLD() == null ? ctx.OTU_SH() : ctx.OTU_BOLD();
    pn.setUninomial(otu.toString().toUpperCase());
    pn.setRank(Rank.SPECIES);
    pn.setState(ParsedName.State.COMPLETE);
    return pn;
  }

  @Override
  public ParsedName visitLatin(SciNameParser.LatinContext ctx) {
    pn.setType(NameType.SCIENTIFIC);
    if (ctx.species() == null) {
      pn.setUninomial( ctx.monomial().MONOMIAL().getText() );
      updateAuthorship(ctx.monomial().authorship());

    } else {
      pn.setGenus( ctx.monomial().MONOMIAL().getText() );
      pn.setSpecificEpithet(ctx.species().epithet().EPITHET().getText());

      if (ctx.subspecies() != null) {
        pn.setRank(Rank.SUBSPECIES);
        pn.setInfraspecificEpithet(ctx.subspecies().epithet().EPITHET().getText());
        updateAuthorship(ctx.subspecies().epithet().authorship());

      } else if (ctx.infraspecies() != null) {
        pn.setInfraspecificEpithet(ctx.infraspecies().epithet().EPITHET().getText());
        if (ctx.infraspecies().rank() != null) {
          Rank rank = RankUtils.inferRank(StringUtils.trimToNull(ctx.infraspecies().rank().RANK().getText()));
          pn.setRank(rank);
        } else {
          pn.setRank(Rank.INFRASPECIFIC_NAME);
        }
        updateAuthorship(ctx.infraspecies().epithet().authorship());

      } else {
        pn.setRank(Rank.SPECIES);
        updateAuthorship(ctx.species().epithet().authorship());
      }
    }
    return pn;
  }

  void updateAuthorship(SciNameParser.AuthorshipContext ctx) {
    if (ctx.basauthorship() != null) {
      pn.getBasionymAuthorship().setYear(textOrNull(ctx.basauthorship().YEAR()));
      for (TerminalNode a : ctx.basauthorship().authorteam().AUTHOR()) {
        pn.getBasionymAuthorship().getAuthors().add(a.getText());
      }
    }
    if (ctx.combauthorship() != null) {
      pn.getCombinationAuthorship().setYear(textOrNull(ctx.combauthorship().YEAR()));
      for (TerminalNode a : ctx.combauthorship().authorteam().AUTHOR()) {
        pn.getCombinationAuthorship().getAuthors().add(a.getText());
      }
    }
  }

  private static String textOrNull(TerminalNode tn){
    return tn == null ? null : tn.getText();
  }
}
