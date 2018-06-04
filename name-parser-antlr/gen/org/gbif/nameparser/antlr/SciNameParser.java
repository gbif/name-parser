// Generated from /Users/markus/code/name-parser/name-parser-antlr/src/main/resources/antlr/SciName.g4 by ANTLR 4.7
package org.gbif.nameparser.antlr;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class SciNameParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.7", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		NOTHO=1, RANK=2, ETAL=3, MONOMIAL=4, EPITHET=5, AUTHOR_INITIALS=6, AUTHOR=7, 
		AUTHOR2=8, AUTHOR_DELIM=9, YEAR=10, DOT=11, LR_BRACKET=12, RR_BRACKET=13, 
		COMMA=14, SEMI=15, SINGLE_QUOTE=16, DOUBLE_QUOTE=17, COLON=18, HYBRID_MARKER=19, 
		EXTINCT_MARKER=20, OTU_BOLD=21, OTU_SH=22, VIRUS=23, CONTROL=24, WS=25, 
		ANY=26, NUC=27, NLC=28, AUC=29, ALC=30, ALPH=31;
	public static final int
		RULE_author = 0, RULE_dot = 1, RULE_auc = 2, RULE_alc = 3, RULE_nuc = 4, 
		RULE_nlc = 5, RULE_alphanum = 6, RULE_scientificName = 7, RULE_latin = 8, 
		RULE_monomial = 9, RULE_epithet = 10, RULE_species = 11, RULE_rank = 12, 
		RULE_subspecies = 13, RULE_infraspecies = 14, RULE_authorship = 15, RULE_combauthorship = 16, 
		RULE_basauthorship = 17, RULE_authorteam = 18, RULE_virus = 19, RULE_otu = 20, 
		RULE_hybridformula = 21;
	public static final String[] ruleNames = {
		"author", "dot", "auc", "alc", "nuc", "nlc", "alphanum", "scientificName", 
		"latin", "monomial", "epithet", "species", "rank", "subspecies", "infraspecies", 
		"authorship", "combauthorship", "basauthorship", "authorteam", "virus", 
		"otu", "hybridformula"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "'notho'", null, null, null, null, null, null, null, null, null, 
		"'.'", "'('", "')'", "','", "';'", "'''", "'\"'", "':'", null, "'\u2020'", 
		null, null, "'virus'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, "NOTHO", "RANK", "ETAL", "MONOMIAL", "EPITHET", "AUTHOR_INITIALS", 
		"AUTHOR", "AUTHOR2", "AUTHOR_DELIM", "YEAR", "DOT", "LR_BRACKET", "RR_BRACKET", 
		"COMMA", "SEMI", "SINGLE_QUOTE", "DOUBLE_QUOTE", "COLON", "HYBRID_MARKER", 
		"EXTINCT_MARKER", "OTU_BOLD", "OTU_SH", "VIRUS", "CONTROL", "WS", "ANY", 
		"NUC", "NLC", "AUC", "ALC", "ALPH"
	};
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "SciName.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public SciNameParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}
	public static class AuthorContext extends ParserRuleContext {
		public TerminalNode AUTHOR() { return getToken(SciNameParser.AUTHOR, 0); }
		public TerminalNode EOF() { return getToken(SciNameParser.EOF, 0); }
		public AuthorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_author; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SciNameVisitor ) return ((SciNameVisitor<? extends T>)visitor).visitAuthor(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AuthorContext author() throws RecognitionException {
		AuthorContext _localctx = new AuthorContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_author);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(44);
			match(AUTHOR);
			setState(45);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class DotContext extends ParserRuleContext {
		public TerminalNode EOF() { return getToken(SciNameParser.EOF, 0); }
		public List<TerminalNode> DOT() { return getTokens(SciNameParser.DOT); }
		public TerminalNode DOT(int i) {
			return getToken(SciNameParser.DOT, i);
		}
		public DotContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dot; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SciNameVisitor ) return ((SciNameVisitor<? extends T>)visitor).visitDot(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DotContext dot() throws RecognitionException {
		DotContext _localctx = new DotContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_dot);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(48); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(47);
				match(DOT);
				}
				}
				setState(50); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==DOT );
			setState(52);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AucContext extends ParserRuleContext {
		public TerminalNode AUC() { return getToken(SciNameParser.AUC, 0); }
		public TerminalNode EOF() { return getToken(SciNameParser.EOF, 0); }
		public AucContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_auc; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SciNameVisitor ) return ((SciNameVisitor<? extends T>)visitor).visitAuc(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AucContext auc() throws RecognitionException {
		AucContext _localctx = new AucContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_auc);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(54);
			match(AUC);
			setState(55);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AlcContext extends ParserRuleContext {
		public TerminalNode ALC() { return getToken(SciNameParser.ALC, 0); }
		public TerminalNode EOF() { return getToken(SciNameParser.EOF, 0); }
		public AlcContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_alc; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SciNameVisitor ) return ((SciNameVisitor<? extends T>)visitor).visitAlc(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AlcContext alc() throws RecognitionException {
		AlcContext _localctx = new AlcContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_alc);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(57);
			match(ALC);
			setState(58);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class NucContext extends ParserRuleContext {
		public TerminalNode NUC() { return getToken(SciNameParser.NUC, 0); }
		public TerminalNode EOF() { return getToken(SciNameParser.EOF, 0); }
		public NucContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nuc; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SciNameVisitor ) return ((SciNameVisitor<? extends T>)visitor).visitNuc(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NucContext nuc() throws RecognitionException {
		NucContext _localctx = new NucContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_nuc);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(60);
			match(NUC);
			setState(61);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class NlcContext extends ParserRuleContext {
		public TerminalNode NLC() { return getToken(SciNameParser.NLC, 0); }
		public TerminalNode EOF() { return getToken(SciNameParser.EOF, 0); }
		public NlcContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nlc; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SciNameVisitor ) return ((SciNameVisitor<? extends T>)visitor).visitNlc(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NlcContext nlc() throws RecognitionException {
		NlcContext _localctx = new NlcContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_nlc);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(63);
			match(NLC);
			setState(64);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AlphanumContext extends ParserRuleContext {
		public TerminalNode ALPH() { return getToken(SciNameParser.ALPH, 0); }
		public TerminalNode EOF() { return getToken(SciNameParser.EOF, 0); }
		public AlphanumContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_alphanum; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SciNameVisitor ) return ((SciNameVisitor<? extends T>)visitor).visitAlphanum(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AlphanumContext alphanum() throws RecognitionException {
		AlphanumContext _localctx = new AlphanumContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_alphanum);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(66);
			match(ALPH);
			setState(67);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ScientificNameContext extends ParserRuleContext {
		public OtuContext otu() {
			return getRuleContext(OtuContext.class,0);
		}
		public LatinContext latin() {
			return getRuleContext(LatinContext.class,0);
		}
		public VirusContext virus() {
			return getRuleContext(VirusContext.class,0);
		}
		public HybridformulaContext hybridformula() {
			return getRuleContext(HybridformulaContext.class,0);
		}
		public TerminalNode EOF() { return getToken(SciNameParser.EOF, 0); }
		public ScientificNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_scientificName; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SciNameVisitor ) return ((SciNameVisitor<? extends T>)visitor).visitScientificName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ScientificNameContext scientificName() throws RecognitionException {
		ScientificNameContext _localctx = new ScientificNameContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_scientificName);
		try {
			setState(75);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,1,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(69);
				otu();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(70);
				latin();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(71);
				virus();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(72);
				hybridformula();
				setState(73);
				match(EOF);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class LatinContext extends ParserRuleContext {
		public MonomialContext monomial() {
			return getRuleContext(MonomialContext.class,0);
		}
		public SpeciesContext species() {
			return getRuleContext(SpeciesContext.class,0);
		}
		public InfraspeciesContext infraspecies() {
			return getRuleContext(InfraspeciesContext.class,0);
		}
		public SubspeciesContext subspecies() {
			return getRuleContext(SubspeciesContext.class,0);
		}
		public LatinContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_latin; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SciNameVisitor ) return ((SciNameVisitor<? extends T>)visitor).visitLatin(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LatinContext latin() throws RecognitionException {
		LatinContext _localctx = new LatinContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_latin);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(77);
			monomial();
			setState(85);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,4,_ctx) ) {
			case 1:
				{
				setState(78);
				species();
				setState(83);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,3,_ctx) ) {
				case 1:
					{
					setState(80);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,2,_ctx) ) {
					case 1:
						{
						setState(79);
						subspecies();
						}
						break;
					}
					setState(82);
					infraspecies();
					}
					break;
				}
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class MonomialContext extends ParserRuleContext {
		public TerminalNode MONOMIAL() { return getToken(SciNameParser.MONOMIAL, 0); }
		public AuthorshipContext authorship() {
			return getRuleContext(AuthorshipContext.class,0);
		}
		public TerminalNode HYBRID_MARKER() { return getToken(SciNameParser.HYBRID_MARKER, 0); }
		public MonomialContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_monomial; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SciNameVisitor ) return ((SciNameVisitor<? extends T>)visitor).visitMonomial(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MonomialContext monomial() throws RecognitionException {
		MonomialContext _localctx = new MonomialContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_monomial);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(88);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==HYBRID_MARKER) {
				{
				setState(87);
				match(HYBRID_MARKER);
				}
			}

			setState(90);
			match(MONOMIAL);
			setState(91);
			authorship();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class EpithetContext extends ParserRuleContext {
		public TerminalNode EPITHET() { return getToken(SciNameParser.EPITHET, 0); }
		public AuthorshipContext authorship() {
			return getRuleContext(AuthorshipContext.class,0);
		}
		public TerminalNode HYBRID_MARKER() { return getToken(SciNameParser.HYBRID_MARKER, 0); }
		public EpithetContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_epithet; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SciNameVisitor ) return ((SciNameVisitor<? extends T>)visitor).visitEpithet(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EpithetContext epithet() throws RecognitionException {
		EpithetContext _localctx = new EpithetContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_epithet);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(94);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==HYBRID_MARKER) {
				{
				setState(93);
				match(HYBRID_MARKER);
				}
			}

			setState(96);
			match(EPITHET);
			setState(97);
			authorship();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SpeciesContext extends ParserRuleContext {
		public EpithetContext epithet() {
			return getRuleContext(EpithetContext.class,0);
		}
		public SpeciesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_species; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SciNameVisitor ) return ((SciNameVisitor<? extends T>)visitor).visitSpecies(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SpeciesContext species() throws RecognitionException {
		SpeciesContext _localctx = new SpeciesContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_species);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(99);
			epithet();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class RankContext extends ParserRuleContext {
		public TerminalNode RANK() { return getToken(SciNameParser.RANK, 0); }
		public TerminalNode NOTHO() { return getToken(SciNameParser.NOTHO, 0); }
		public TerminalNode DOT() { return getToken(SciNameParser.DOT, 0); }
		public RankContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_rank; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SciNameVisitor ) return ((SciNameVisitor<? extends T>)visitor).visitRank(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RankContext rank() throws RecognitionException {
		RankContext _localctx = new RankContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_rank);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(102);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==NOTHO) {
				{
				setState(101);
				match(NOTHO);
				}
			}

			setState(104);
			match(RANK);
			setState(106);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==DOT) {
				{
				setState(105);
				match(DOT);
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SubspeciesContext extends ParserRuleContext {
		public EpithetContext epithet() {
			return getRuleContext(EpithetContext.class,0);
		}
		public SubspeciesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_subspecies; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SciNameVisitor ) return ((SciNameVisitor<? extends T>)visitor).visitSubspecies(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SubspeciesContext subspecies() throws RecognitionException {
		SubspeciesContext _localctx = new SubspeciesContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_subspecies);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(108);
			epithet();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class InfraspeciesContext extends ParserRuleContext {
		public EpithetContext epithet() {
			return getRuleContext(EpithetContext.class,0);
		}
		public RankContext rank() {
			return getRuleContext(RankContext.class,0);
		}
		public InfraspeciesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_infraspecies; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SciNameVisitor ) return ((SciNameVisitor<? extends T>)visitor).visitInfraspecies(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InfraspeciesContext infraspecies() throws RecognitionException {
		InfraspeciesContext _localctx = new InfraspeciesContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_infraspecies);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(111);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==NOTHO || _la==RANK) {
				{
				setState(110);
				rank();
				}
			}

			setState(113);
			epithet();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AuthorshipContext extends ParserRuleContext {
		public BasauthorshipContext basauthorship() {
			return getRuleContext(BasauthorshipContext.class,0);
		}
		public CombauthorshipContext combauthorship() {
			return getRuleContext(CombauthorshipContext.class,0);
		}
		public AuthorshipContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_authorship; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SciNameVisitor ) return ((SciNameVisitor<? extends T>)visitor).visitAuthorship(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AuthorshipContext authorship() throws RecognitionException {
		AuthorshipContext _localctx = new AuthorshipContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_authorship);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(116);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LR_BRACKET) {
				{
				setState(115);
				basauthorship();
				}
			}

			setState(119);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==AUTHOR) {
				{
				setState(118);
				combauthorship();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class CombauthorshipContext extends ParserRuleContext {
		public AuthorteamContext authorteam() {
			return getRuleContext(AuthorteamContext.class,0);
		}
		public TerminalNode YEAR() { return getToken(SciNameParser.YEAR, 0); }
		public TerminalNode COMMA() { return getToken(SciNameParser.COMMA, 0); }
		public CombauthorshipContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_combauthorship; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SciNameVisitor ) return ((SciNameVisitor<? extends T>)visitor).visitCombauthorship(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CombauthorshipContext combauthorship() throws RecognitionException {
		CombauthorshipContext _localctx = new CombauthorshipContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_combauthorship);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(121);
			authorteam();
			setState(126);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==YEAR || _la==COMMA) {
				{
				setState(123);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==COMMA) {
					{
					setState(122);
					match(COMMA);
					}
				}

				setState(125);
				match(YEAR);
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class BasauthorshipContext extends ParserRuleContext {
		public TerminalNode LR_BRACKET() { return getToken(SciNameParser.LR_BRACKET, 0); }
		public AuthorteamContext authorteam() {
			return getRuleContext(AuthorteamContext.class,0);
		}
		public TerminalNode RR_BRACKET() { return getToken(SciNameParser.RR_BRACKET, 0); }
		public TerminalNode YEAR() { return getToken(SciNameParser.YEAR, 0); }
		public TerminalNode COMMA() { return getToken(SciNameParser.COMMA, 0); }
		public BasauthorshipContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_basauthorship; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SciNameVisitor ) return ((SciNameVisitor<? extends T>)visitor).visitBasauthorship(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BasauthorshipContext basauthorship() throws RecognitionException {
		BasauthorshipContext _localctx = new BasauthorshipContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_basauthorship);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(128);
			match(LR_BRACKET);
			setState(129);
			authorteam();
			setState(134);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==YEAR || _la==COMMA) {
				{
				setState(131);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==COMMA) {
					{
					setState(130);
					match(COMMA);
					}
				}

				setState(133);
				match(YEAR);
				}
			}

			setState(136);
			match(RR_BRACKET);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AuthorteamContext extends ParserRuleContext {
		public List<TerminalNode> AUTHOR() { return getTokens(SciNameParser.AUTHOR); }
		public TerminalNode AUTHOR(int i) {
			return getToken(SciNameParser.AUTHOR, i);
		}
		public List<TerminalNode> AUTHOR_DELIM() { return getTokens(SciNameParser.AUTHOR_DELIM); }
		public TerminalNode AUTHOR_DELIM(int i) {
			return getToken(SciNameParser.AUTHOR_DELIM, i);
		}
		public TerminalNode ETAL() { return getToken(SciNameParser.ETAL, 0); }
		public AuthorteamContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_authorteam; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SciNameVisitor ) return ((SciNameVisitor<? extends T>)visitor).visitAuthorteam(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AuthorteamContext authorteam() throws RecognitionException {
		AuthorteamContext _localctx = new AuthorteamContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_authorteam);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(138);
			match(AUTHOR);
			setState(143);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AUTHOR_DELIM) {
				{
				{
				setState(139);
				match(AUTHOR_DELIM);
				setState(140);
				match(AUTHOR);
				}
				}
				setState(145);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(147);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ETAL) {
				{
				setState(146);
				match(ETAL);
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class VirusContext extends ParserRuleContext {
		public TerminalNode VIRUS() { return getToken(SciNameParser.VIRUS, 0); }
		public VirusContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_virus; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SciNameVisitor ) return ((SciNameVisitor<? extends T>)visitor).visitVirus(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VirusContext virus() throws RecognitionException {
		VirusContext _localctx = new VirusContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_virus);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(149);
			match(VIRUS);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class OtuContext extends ParserRuleContext {
		public TerminalNode OTU_BOLD() { return getToken(SciNameParser.OTU_BOLD, 0); }
		public TerminalNode OTU_SH() { return getToken(SciNameParser.OTU_SH, 0); }
		public OtuContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_otu; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SciNameVisitor ) return ((SciNameVisitor<? extends T>)visitor).visitOtu(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OtuContext otu() throws RecognitionException {
		OtuContext _localctx = new OtuContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_otu);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(151);
			_la = _input.LA(1);
			if ( !(_la==OTU_BOLD || _la==OTU_SH) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class HybridformulaContext extends ParserRuleContext {
		public List<LatinContext> latin() {
			return getRuleContexts(LatinContext.class);
		}
		public LatinContext latin(int i) {
			return getRuleContext(LatinContext.class,i);
		}
		public List<TerminalNode> HYBRID_MARKER() { return getTokens(SciNameParser.HYBRID_MARKER); }
		public TerminalNode HYBRID_MARKER(int i) {
			return getToken(SciNameParser.HYBRID_MARKER, i);
		}
		public HybridformulaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_hybridformula; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SciNameVisitor ) return ((SciNameVisitor<? extends T>)visitor).visitHybridformula(this);
			else return visitor.visitChildren(this);
		}
	}

	public final HybridformulaContext hybridformula() throws RecognitionException {
		HybridformulaContext _localctx = new HybridformulaContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_hybridformula);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(153);
			latin();
			setState(156); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(154);
				match(HYBRID_MARKER);
				setState(155);
				latin();
				}
				}
				setState(158); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==HYBRID_MARKER );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3!\u00a3\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t"+
		"\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\3\2\3\2\3\2\3\3\6\3"+
		"\63\n\3\r\3\16\3\64\3\3\3\3\3\4\3\4\3\4\3\5\3\5\3\5\3\6\3\6\3\6\3\7\3"+
		"\7\3\7\3\b\3\b\3\b\3\t\3\t\3\t\3\t\3\t\3\t\5\tN\n\t\3\n\3\n\3\n\5\nS\n"+
		"\n\3\n\5\nV\n\n\5\nX\n\n\3\13\5\13[\n\13\3\13\3\13\3\13\3\f\5\fa\n\f\3"+
		"\f\3\f\3\f\3\r\3\r\3\16\5\16i\n\16\3\16\3\16\5\16m\n\16\3\17\3\17\3\20"+
		"\5\20r\n\20\3\20\3\20\3\21\5\21w\n\21\3\21\5\21z\n\21\3\22\3\22\5\22~"+
		"\n\22\3\22\5\22\u0081\n\22\3\23\3\23\3\23\5\23\u0086\n\23\3\23\5\23\u0089"+
		"\n\23\3\23\3\23\3\24\3\24\3\24\7\24\u0090\n\24\f\24\16\24\u0093\13\24"+
		"\3\24\5\24\u0096\n\24\3\25\3\25\3\26\3\26\3\27\3\27\3\27\6\27\u009f\n"+
		"\27\r\27\16\27\u00a0\3\27\2\2\30\2\4\6\b\n\f\16\20\22\24\26\30\32\34\36"+
		" \"$&(*,\2\3\3\2\27\30\2\u00a1\2.\3\2\2\2\4\62\3\2\2\2\68\3\2\2\2\b;\3"+
		"\2\2\2\n>\3\2\2\2\fA\3\2\2\2\16D\3\2\2\2\20M\3\2\2\2\22O\3\2\2\2\24Z\3"+
		"\2\2\2\26`\3\2\2\2\30e\3\2\2\2\32h\3\2\2\2\34n\3\2\2\2\36q\3\2\2\2 v\3"+
		"\2\2\2\"{\3\2\2\2$\u0082\3\2\2\2&\u008c\3\2\2\2(\u0097\3\2\2\2*\u0099"+
		"\3\2\2\2,\u009b\3\2\2\2./\7\t\2\2/\60\7\2\2\3\60\3\3\2\2\2\61\63\7\r\2"+
		"\2\62\61\3\2\2\2\63\64\3\2\2\2\64\62\3\2\2\2\64\65\3\2\2\2\65\66\3\2\2"+
		"\2\66\67\7\2\2\3\67\5\3\2\2\289\7\37\2\29:\7\2\2\3:\7\3\2\2\2;<\7 \2\2"+
		"<=\7\2\2\3=\t\3\2\2\2>?\7\35\2\2?@\7\2\2\3@\13\3\2\2\2AB\7\36\2\2BC\7"+
		"\2\2\3C\r\3\2\2\2DE\7!\2\2EF\7\2\2\3F\17\3\2\2\2GN\5*\26\2HN\5\22\n\2"+
		"IN\5(\25\2JK\5,\27\2KL\7\2\2\3LN\3\2\2\2MG\3\2\2\2MH\3\2\2\2MI\3\2\2\2"+
		"MJ\3\2\2\2N\21\3\2\2\2OW\5\24\13\2PU\5\30\r\2QS\5\34\17\2RQ\3\2\2\2RS"+
		"\3\2\2\2ST\3\2\2\2TV\5\36\20\2UR\3\2\2\2UV\3\2\2\2VX\3\2\2\2WP\3\2\2\2"+
		"WX\3\2\2\2X\23\3\2\2\2Y[\7\25\2\2ZY\3\2\2\2Z[\3\2\2\2[\\\3\2\2\2\\]\7"+
		"\6\2\2]^\5 \21\2^\25\3\2\2\2_a\7\25\2\2`_\3\2\2\2`a\3\2\2\2ab\3\2\2\2"+
		"bc\7\7\2\2cd\5 \21\2d\27\3\2\2\2ef\5\26\f\2f\31\3\2\2\2gi\7\3\2\2hg\3"+
		"\2\2\2hi\3\2\2\2ij\3\2\2\2jl\7\4\2\2km\7\r\2\2lk\3\2\2\2lm\3\2\2\2m\33"+
		"\3\2\2\2no\5\26\f\2o\35\3\2\2\2pr\5\32\16\2qp\3\2\2\2qr\3\2\2\2rs\3\2"+
		"\2\2st\5\26\f\2t\37\3\2\2\2uw\5$\23\2vu\3\2\2\2vw\3\2\2\2wy\3\2\2\2xz"+
		"\5\"\22\2yx\3\2\2\2yz\3\2\2\2z!\3\2\2\2{\u0080\5&\24\2|~\7\20\2\2}|\3"+
		"\2\2\2}~\3\2\2\2~\177\3\2\2\2\177\u0081\7\f\2\2\u0080}\3\2\2\2\u0080\u0081"+
		"\3\2\2\2\u0081#\3\2\2\2\u0082\u0083\7\16\2\2\u0083\u0088\5&\24\2\u0084"+
		"\u0086\7\20\2\2\u0085\u0084\3\2\2\2\u0085\u0086\3\2\2\2\u0086\u0087\3"+
		"\2\2\2\u0087\u0089\7\f\2\2\u0088\u0085\3\2\2\2\u0088\u0089\3\2\2\2\u0089"+
		"\u008a\3\2\2\2\u008a\u008b\7\17\2\2\u008b%\3\2\2\2\u008c\u0091\7\t\2\2"+
		"\u008d\u008e\7\13\2\2\u008e\u0090\7\t\2\2\u008f\u008d\3\2\2\2\u0090\u0093"+
		"\3\2\2\2\u0091\u008f\3\2\2\2\u0091\u0092\3\2\2\2\u0092\u0095\3\2\2\2\u0093"+
		"\u0091\3\2\2\2\u0094\u0096\7\5\2\2\u0095\u0094\3\2\2\2\u0095\u0096\3\2"+
		"\2\2\u0096\'\3\2\2\2\u0097\u0098\7\31\2\2\u0098)\3\2\2\2\u0099\u009a\t"+
		"\2\2\2\u009a+\3\2\2\2\u009b\u009e\5\22\n\2\u009c\u009d\7\25\2\2\u009d"+
		"\u009f\5\22\n\2\u009e\u009c\3\2\2\2\u009f\u00a0\3\2\2\2\u00a0\u009e\3"+
		"\2\2\2\u00a0\u00a1\3\2\2\2\u00a1-\3\2\2\2\25\64MRUWZ`hlqvy}\u0080\u0085"+
		"\u0088\u0091\u0095\u00a0";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}