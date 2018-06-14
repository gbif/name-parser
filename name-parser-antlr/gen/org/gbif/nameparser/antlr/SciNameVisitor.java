// Generated from /Users/markus/code/name-parser/name-parser-antlr/src/main/resources/antlr/SciName.g4 by ANTLR 4.7
package org.gbif.nameparser.antlr;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link SciNameParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface SciNameVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link SciNameParser#epithet2}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEpithet2(SciNameParser.Epithet2Context ctx);
	/**
	 * Visit a parse tree produced by {@link SciNameParser#author}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAuthor(SciNameParser.AuthorContext ctx);
	/**
	 * Visit a parse tree produced by {@link SciNameParser#dot}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDot(SciNameParser.DotContext ctx);
	/**
	 * Visit a parse tree produced by {@link SciNameParser#auc}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAuc(SciNameParser.AucContext ctx);
	/**
	 * Visit a parse tree produced by {@link SciNameParser#alc}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlc(SciNameParser.AlcContext ctx);
	/**
	 * Visit a parse tree produced by {@link SciNameParser#nuc}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNuc(SciNameParser.NucContext ctx);
	/**
	 * Visit a parse tree produced by {@link SciNameParser#nlc}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNlc(SciNameParser.NlcContext ctx);
	/**
	 * Visit a parse tree produced by {@link SciNameParser#alphanum}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlphanum(SciNameParser.AlphanumContext ctx);
	/**
	 * Visit a parse tree produced by {@link SciNameParser#scientificName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScientificName(SciNameParser.ScientificNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link SciNameParser#latin}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLatin(SciNameParser.LatinContext ctx);
	/**
	 * Visit a parse tree produced by {@link SciNameParser#monomial}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMonomial(SciNameParser.MonomialContext ctx);
	/**
	 * Visit a parse tree produced by {@link SciNameParser#epithet}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEpithet(SciNameParser.EpithetContext ctx);
	/**
	 * Visit a parse tree produced by {@link SciNameParser#species}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSpecies(SciNameParser.SpeciesContext ctx);
	/**
	 * Visit a parse tree produced by {@link SciNameParser#rank}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRank(SciNameParser.RankContext ctx);
	/**
	 * Visit a parse tree produced by {@link SciNameParser#subspecies}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubspecies(SciNameParser.SubspeciesContext ctx);
	/**
	 * Visit a parse tree produced by {@link SciNameParser#infraspecies}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInfraspecies(SciNameParser.InfraspeciesContext ctx);
	/**
	 * Visit a parse tree produced by {@link SciNameParser#authorship}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAuthorship(SciNameParser.AuthorshipContext ctx);
	/**
	 * Visit a parse tree produced by {@link SciNameParser#combauthorship}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCombauthorship(SciNameParser.CombauthorshipContext ctx);
	/**
	 * Visit a parse tree produced by {@link SciNameParser#basauthorship}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBasauthorship(SciNameParser.BasauthorshipContext ctx);
	/**
	 * Visit a parse tree produced by {@link SciNameParser#authorteam}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAuthorteam(SciNameParser.AuthorteamContext ctx);
	/**
	 * Visit a parse tree produced by {@link SciNameParser#virus}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVirus(SciNameParser.VirusContext ctx);
	/**
	 * Visit a parse tree produced by {@link SciNameParser#otu}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOtu(SciNameParser.OtuContext ctx);
	/**
	 * Visit a parse tree produced by {@link SciNameParser#hybridformula}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHybridformula(SciNameParser.HybridformulaContext ctx);
}