package org.gbif.nameparser;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.NamePart;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.Rank;
import org.gbif.utils.concurrent.NamedThreadFactory;
import org.gbif.utils.file.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Core parser class of the name parser that tries to take a clean name into its pieces by using regular expressions.
 * It runs the actual regex matching in another thread that stops whenever the configured timeout is reached.
 */
public class NormalisedNameParser {
  private static Logger LOG = LoggerFactory.getLogger(NormalisedNameParser.class);

  /**
   * We use a cached threadpool to run the normalised parsing in the background so we can control
   * timeouts. If idle the pool shrinks to no threads after 10 seconds.
   */
  private static final ExecutorService EXEC = new ThreadPoolExecutor(0, 100,
      10L, TimeUnit.SECONDS,
      new SynchronousQueue<Runnable>(),
      new NamedThreadFactory("NormalisedNameParser", Thread.MAX_PRIORITY, true),
      new ThreadPoolExecutor.CallerRunsPolicy());

  private final long timeout;  // max parsing time in milliseconds

  public NormalisedNameParser(long timeout) {
    this.timeout = timeout;
  }

  // name parsing
  protected static final String NAME_LETTERS = "A-ZÏËÖÜÄÉÈČÁÀÆŒ";
  protected static final String name_letters = "a-zïëöüäåéèčáàæœ";
  protected static final String AUTHOR_LETTERS = NAME_LETTERS + "\\p{Lu}"; // upper case unicode letter, not numerical
  // (\W is alphanum)
  protected static final String author_letters = name_letters + "\\p{Ll}-"; // lower case unicode letter, not numerical
  // (\W is alphanum)
  protected static final String AUTHOR_PREFIXES =
    "(?:[vV](?:an)(?:[ -](?:den|der) )? ?|von[ -](?:den |der |dem )?|(?:del|de|di|da)[`' _]|(?:Des|De|Di|N)[`' _]?|(?:de )?(?:la|le) |d'|D'|Mac|Mc|Le|St\\.? ?|Ou|O')";
  protected static final String AUTHOR = "(?:" +
                                         // author initials
                                         "(?:" + "(?:[" + AUTHOR_LETTERS + "]{1,3}\\.?[ -]?){0,3}" +
                                         // or full first name
                                         "|[" + AUTHOR_LETTERS + "][" + author_letters + "?]{3,}" + " )?" +
                                         // common prefixes
                                         AUTHOR_PREFIXES + "?" +
                                         // only allow v. in front of Capital Authornames - if included in AUTHOR_PREFIXES parseIgnoreAuthors fails
                                         "(?:v\\. )?" +
                                         // regular author name
                                         "[" + AUTHOR_LETTERS + "]+[" + author_letters + "?]*\\.?" +
                                         // potential double names, e.g. Solms-Laub.
                                         "(?:(?:[- ](?:de|da|du)?[- ]?)[" + AUTHOR_LETTERS + "]+[" + author_letters
                                         + "?]*\\.?)?" +
                                         // common name suffices (ms=manuscript, not yet published)
                                         "(?: ?(?:f|fil|j|jr|jun|junior|sr|sen|senior|ms)\\.?)?" +
                                         // at last there might be 2 well known sanction authors for fungus, see POR-2454
                                         "(?: *: *(?:Pers|Fr)\\.?)?" +
                                         ")";
  protected static final String AUTHOR_TEAM =
    AUTHOR + "?(?:(?: ?ex\\.? | & | et | in |, ?|; ?|\\.)(?:" + AUTHOR + "|al\\.?))*";
  protected static final Pattern AUTHOR_TEAM_PATTERN = Pattern.compile("^" + AUTHOR_TEAM + "$");
  protected static final String YEAR = "[12][0-9][0-9][0-9?][abcdh?]?(?:[/-][0-9]{1,4})?";
  // protected static final String YEAR_RANGE = YEAR+"(?: ?-? ?"+YEAR+")?";

  private static final String NOTHO = "notho";
  protected static final String RANK_MARKER_SPECIES =
    "(?:"+NOTHO+")?(?:(?<!f[ .] ?)sp|" + StringUtils.join(RankUtils.RANK_MARKER_MAP_INFRASPECIFIC.keySet(), "|") + ")\\.?";

  private static final Function<Rank,String> REMOVE_RANK_MARKER = new Function<Rank, String>() {
    @Override
    public String apply(Rank rank) {
      return rank.getMarker().replaceAll("\\.", "\\\\.");
    }
  };

  protected static final String RANK_MARKER_MICROBIAL =
    "(?:bv\\.|ct\\.|f\\. ?sp\\.|"
    + StringUtils.join(Lists.transform(Lists.newArrayList(RankUtils.INFRASUBSPECIFIC_MICROBIAL_RANKS), REMOVE_RANK_MARKER
    ), "|") + ")";

  protected static final String EPHITHET_PREFIXES = "van|novae";
  protected static final String GENETIC_EPHITHETS = "bacilliform|coliform|coryneform|cytoform|chemoform|biovar|serovar|genomovar|agamovar|cultivar|genotype|serotype|subtype|ribotype|isolate";
  protected static final String EPHITHET = "(?:[0-9]+-?|[doml]')?"
            + "(?:(?:" + EPHITHET_PREFIXES + ") [a-z])?"
            + "[" + name_letters + "+-]{1,}(?<! d)[" + name_letters + "]"
            // avoid epithets ending with the unallowed endings, e.g. serovar
            + "(?<!(?:\\bex|"+GENETIC_EPHITHETS+"))(?=\\b)";
  protected static final String MONOMIAL =
    "[" + NAME_LETTERS + "](?:\\.|[" + name_letters + "]+)(?:-[" + NAME_LETTERS + "]?[" + name_letters + "]+)?";
  // a pattern matching typical latin word endings. Helps identify name parts from authors
  private static final Pattern LATIN_ENDINGS;
  static {
      try {
          List<String> endings = FileUtils.streamToList(FileUtils.classpathStream("latin-endings.txt"));
          LATIN_ENDINGS = Pattern.compile("(" + Joiner.on('|').skipNulls().join(endings) + ")$");
      } catch (IOException e) {
          throw new IllegalStateException("Failed to read latin-endings.txt from classpath resources", e);
      }
  }
  protected static final String INFRAGENERIC =
    "(?:" + "\\( ?([" + NAME_LETTERS + "][" + name_letters + "-]+) ?\\)" + "|" + "(" + StringUtils
      .join(RankUtils.RANK_MARKER_MAP_INFRAGENERIC.keySet(), "|") + ")\\.? ?([" + NAME_LETTERS + "][" + name_letters + "-]+)"
    + ")";

  protected static final String RANK_MARKER_ALL = "("+NOTHO+")? *(" + StringUtils.join(RankUtils.RANK_MARKER_MAP.keySet(), "|") + ")\\.?";
  private static final Pattern RANK_MARKER_ONLY = Pattern.compile("^" + RANK_MARKER_ALL + "$");

  // main name matcher
  public static final Pattern CANON_NAME_IGNORE_AUTHORS = Pattern.compile("^" +
              // #1 genus/monomial
              "(×?(?:\\?|" + MONOMIAL + "))" +
              // #2 or #4 subgenus/section with #3 infrageneric rank marker
              "(?:(?<!ceae) " + INFRAGENERIC + ")?" +
              // catch author name prefixes just to ignore them so they dont become wrong epithets
              "(?: " + AUTHOR_PREFIXES + ")?" +
              // #5 species
              "(?: (×?" + EPHITHET + "))?" +
              // catch author name prefixes just to ignore them so they dont become wrong epithets
              "(?: " + AUTHOR_PREFIXES + ")?" +
              "(?:" +
                // either directly a infraspecific epitheton or a author but then mandate rank marker
                // #6 superfluent intermediate (subspecies) epithet in quadrinomials
                "( " + EPHITHET + ")?" +
                "(?:" +
                  // anything in between
                  "(?: .+)?" +
                  // #7 infraspecies rank
                  "( " + RANK_MARKER_SPECIES + "[ .])" +
                ")?" +
                // #8 infraspecies epithet
                " (×?" + EPHITHET + ")" +
              ")?" +
              "(?: " +
                // #9 microbial rank
                "(" + RANK_MARKER_MICROBIAL + ")[ .]" +
                // #10 microbial infrasubspecific epithet
                "(\\S+)" +
              ")?");

  public static final Pattern NAME_PATTERN = Pattern.compile("^" +
             // #1 genus/monomial
             "(×?(?:\\?|" + MONOMIAL + "))" +
             // #2 or #4 subgenus/section with #3 infrageneric rank marker
             "(?:(?<!ceae) " + INFRAGENERIC + ")?" +
             // #5 species
             "(?: (×?" + EPHITHET + "))?" +

             "(?:" +
               // #6 superfluent intermediate (subspecies) epithet in quadrinomials
               "( " + EPHITHET + ")??" +
               "(?:" +
                 // strip out intermediate, irrelevant authors
                 "(?: .+)??" +
                 // #7 infraspecies rank
                 "( " + RANK_MARKER_SPECIES + ")" +
               ")?" +
               // #8 infraspecies epitheton
               "(?: (×?\"?" + EPHITHET + "\"?))" +
             ")?" +

             "(?: " +
               // #9 microbial rank
               "(" + RANK_MARKER_MICROBIAL + ")[ .]" +
               // #10 microbial infrasubspecific epithet
               "(\\S+)" +
             ")?" +

             // #11 entire authorship incl basionyms and year
             "(,?" +
               "(?: ?\\(" +
                 // #12 basionym authors
                 "(" + AUTHOR_TEAM + ")?" +
                 // #13 basionym year
                 ",?( ?" + YEAR + ")?" +
               "\\))?" +

               // #14 authors
               "( " + AUTHOR_TEAM + ")?" +
               // #15 year with or without brackets
               "(?: ?\\(?,? ?(" + YEAR + ")\\)?)?" +
             ")" +
             "$");


  private class MatcherCallable implements Callable<Matcher> {
    private final String scientificName;

    MatcherCallable(String scientificName) {
      this.scientificName = scientificName;
    }

    @Override
    public Matcher call() throws Exception {
      Matcher matcher = NAME_PATTERN.matcher(scientificName);
      matcher.find();
      return matcher;
    }
  }

  /**
   * Tries to parse a name string with the full regular expression.
   * In very few, extreme cases names with very long authorships might cause the regex to never finish or take hours
   * we run this parsing in a separate thread that can be stopped if it runs too long.
   * @param cn
   * @param scientificName
   * @param rank the rank of the name if it is known externally. Helps identifying infrageneric names vs bracket authors
   * @return  true if the name could be parsed, false in case of failure
   */
  public boolean parseNormalisedName(ParsedName cn, String scientificName, @Nullable Rank rank) {
    LOG.debug("Parse normed name string: {}", scientificName);
    FutureTask<Matcher> task = new FutureTask<Matcher>(new MatcherCallable(scientificName));
    EXEC.execute(task);

    try {
      Matcher matcher = task.get(timeout, TimeUnit.MILLISECONDS);
      // if there was no match in the callable, this can yield an IllegalStateException
      if (matcher.group(0).equals(scientificName)) {
        if (LOG.isDebugEnabled()) {
          logMatcher(matcher);
        }
        cn.setGenusOrAbove(StringUtils.trimToNull(matcher.group(1)));
        boolean bracketSubrankFound = false;
        if (matcher.group(2) != null) {
          bracketSubrankFound = true;
          cn.setInfraGeneric(StringUtils.trimToNull(matcher.group(2)));
        } else if (matcher.group(4) != null) {
          setRank(cn, matcher.group(3));
          cn.setInfraGeneric(StringUtils.trimToNull(matcher.group(4)));
        }
        cn.setSpecificEpithet(StringUtils.trimToNull(matcher.group(5)));
        if (matcher.group(6) != null && matcher.group(6).length() > 1 && !matcher.group(6).contains("null")) {
          // 4 parted name, so its below subspecies
          cn.setRank(Rank.INFRASUBSPECIFIC_NAME);
        }
        if (matcher.group(7) != null && matcher.group(7).length() > 1) {
          setRank(cn, matcher.group(7));
        }
        cn.setInfraSpecificEpithet(StringUtils.trimToNull(matcher.group(8)));

        // microbial ranks
        if (matcher.group(9) != null) {
          setRank(cn, matcher.group(9));
          cn.setInfraSpecificEpithet(matcher.group(10));
        }

        // #11 is entire authorship, not stored in ParsedName
        cn.setBracketAuthorship(StringUtils.trimToNull(matcher.group(12)));
        if (bracketSubrankFound && infragenericIsAuthor(cn, rank)) {
          // rather an author than a infrageneric rank. Swap
          cn.setBracketAuthorship(cn.getInfraGeneric());
          cn.setInfraGeneric(null);
          LOG.debug("swapped subrank with bracket author: {}", cn.getBracketAuthorship());
        }
        if (matcher.group(13) != null && matcher.group(13).length() > 2) {
          String yearAsString = matcher.group(13).trim();
          cn.setBracketYear(yearAsString);
        }
        cn.setAuthorship(StringUtils.trimToNull(matcher.group(14)));
        if (matcher.group(15) != null && matcher.group(15).length() > 2) {
          String yearAsString = matcher.group(15).trim();
          cn.setYear(yearAsString);
        }

        // make sure (infra)specific epithet is not a rank marker!
        lookForIrregularRankMarker(cn);
        // 2 letter epitheta can also be author prefixes - check that programmatically, not in regex
        checkEpithetVsAuthorPrefx(cn);

        // if no rank was parsed but given externally use it!
        if (cn.getRank() == null && rank != null) {
            cn.setRank(rank);
        }
        return true;
      }

    } catch (InterruptedException e) {
      LOG.warn("Thread got interrupted, shutdown executor", e);
      EXEC.shutdown();

    } catch (ExecutionException e) {
      LOG.warn("ExecutionException for name: {}", scientificName, e);

    } catch (IllegalStateException e) {
      // we simply had no match

    } catch (TimeoutException e) {
      // timeout
      LOG.info("Parsing timeout for name: {}", scientificName);
    }

    return false;
  }

  /**
   * Sets the parsed names rank based on a found rank marker
   * Potentially also sets the notho field in case the rank marker indicates a hybrid
   * @param pn
   * @param rankMarker
   */
  protected static void setRank(ParsedName pn, String rankMarker) {
    rankMarker = StringUtils.trimToNull(rankMarker);
    Rank rank = RankUtils.inferRank(rankMarker);
    pn.setRank(rank);
    if (rank != null && rankMarker.startsWith(NOTHO)) {
      if (rank.isInfraspecific()) {
        pn.setNotho(NamePart.INFRASPECIFIC);
      } else if (rank == Rank.SPECIES) {
        pn.setNotho(NamePart.SPECIFIC);
      } else if (rank.isInfrageneric()) {
        pn.setNotho(NamePart.INFRAGENERIC);
      } else if (rank == Rank.GENUS) {
        pn.setNotho(NamePart.GENERIC);
      }
    }
  }

    private static boolean infragenericIsAuthor(ParsedName pn, Rank rank) {
        return pn.getBracketAuthorship() == null && pn.getSpecificEpithet() == null && (
                rank != null && !(rank.isInfrageneric() && !rank.isSpeciesOrBelow())
                        //|| pn.getInfraGeneric().contains(" ")
                        || rank == null && !LATIN_ENDINGS.matcher(pn.getInfraGeneric()).find()
        );
    }

  /**
   *
   * @param cn
   * @param scientificName
   * @param rank the rank of the name if it is known externally. Helps identifying infrageneric names vs bracket authors
   * @return  true if the name could be parsed, false in case of failure
   */
  public boolean parseNormalisedNameIgnoreAuthors(ParsedName cn, String scientificName, @Nullable Rank rank) {
    LOG.debug("Parse normed name string ignoring authors: {}", scientificName);

    // match for canonical
    Matcher matcher = CANON_NAME_IGNORE_AUTHORS.matcher(scientificName);
    boolean matchFound = matcher.find();
    if (matchFound) {
      if (LOG.isDebugEnabled()) {
        logMatcher(matcher);
      }
      cn.setGenusOrAbove(StringUtils.trimToNull(matcher.group(1)));
      if (matcher.group(2) != null) {
        // subrank in paranthesis. Not an author?
        cn.setInfraGeneric(StringUtils.trimToNull(matcher.group(2)));
        if (infragenericIsAuthor(cn, rank)) {
            // rather an author...
            cn.setInfraGeneric(null);
        }
      } else if (matcher.group(4) != null) {
        // infrageneric with rank indicator given
        setRank(cn, matcher.group(3));
        cn.setInfraGeneric(StringUtils.trimToNull(matcher.group(4)));
      }
      cn.setSpecificEpithet(StringUtils.trimToNull(matcher.group(5)));
      if (matcher.group(6) != null && matcher.group(6).length() > 1 && !matcher.group(6).contains("null")) {
        // 4 parted name, so its below subspecies
        cn.setRank(Rank.INFRASUBSPECIFIC_NAME);
      }
      if (matcher.group(7) != null && matcher.group(7).length() > 1) {
        setRank(cn, matcher.group(7));
      }
      if (matcher.group(8) != null && matcher.group(8).length() >= 2) {
        setCanonicalInfraSpecies(cn, matcher.group(8));
      }
      if (matcher.group(9) != null) {
        setRank(cn, matcher.group(9));
        cn.setInfraSpecificEpithet(matcher.group(10));
      }

      // make sure (infra)specific epithet is not a rank marker!
      lookForIrregularRankMarker(cn);

      return true;
    }
    return false;
  }

  private static void setCanonicalInfraSpecies(ParsedName pn, String epi) {
    if (epi == null || epi.equalsIgnoreCase("sec") || epi.equalsIgnoreCase("sensu")) {
      return;
    }
    pn.setInfraSpecificEpithet(StringUtils.trimToNull(epi));
  }

  /**
   * if no rank marker is set, inspect epitheta for wrongly placed rank markers and modify parsed name accordingly.
   * This is sometimes the case for informal names like: Coccyzus americanus ssp.
   *
   * @param cn the already parsed name
   */
  private void lookForIrregularRankMarker(ParsedName cn) {
    if (cn.getRank() == null) {
      if (cn.getInfraSpecificEpithet() != null) {
        Matcher m = RANK_MARKER_ONLY.matcher(cn.getInfraSpecificEpithet());
        if (m.find()) {
          // we found a rank marker, make it one
          setRank(cn, cn.getInfraSpecificEpithet());
          cn.setInfraSpecificEpithet(null);
        }
      } else if (cn.getSpecificEpithet() != null) {
        Matcher m = RANK_MARKER_ONLY.matcher(cn.getSpecificEpithet());
        if (m.find()) {
          // we found a rank marker, make it one
          setRank(cn, cn.getSpecificEpithet());
          cn.setSpecificEpithet(null);
        }
      }
    } else if(cn.getRank() == Rank.SPECIES && cn.getInfraSpecificEpithet() != null) {
      // sometimes sp. is wrongly used as a subspecies rankmarker
      cn.setRank(Rank.SUBSPECIES);
      cn.setType(NameType.DOUBTFUL);
    }
  }

  /**
   * 2 letter epitheta can also be author prefixes - check that programmatically, not in regex
   */
  private void checkEpithetVsAuthorPrefx(ParsedName cn) {
    if (cn.getRank() == null) {
      if (cn.getInfraSpecificEpithet() != null) {
        // might be subspecies without rank marker
        // or short authorship prefix in epithet. test
        String extendedAuthor = cn.getInfraSpecificEpithet() + " " + cn.getAuthorship();
        Matcher m = AUTHOR_TEAM_PATTERN.matcher(extendedAuthor);
        if (m.find()) {
          // matches author. Prefer that
          LOG.debug("use infraspecific epithet as author prefix");
          cn.setInfraSpecificEpithet(null);
          cn.setAuthorship(extendedAuthor);
        }
      } else {
        // might be monomial with the author prefix erroneously taken as the species epithet
        String extendedAuthor = cn.getSpecificEpithet() + " " + cn.getAuthorship();
        Matcher m = AUTHOR_TEAM_PATTERN.matcher(extendedAuthor);
        if (m.find()) {
          // matches author. Prefer that
          LOG.debug("use specific epithet as author prefix");
          cn.setSpecificEpithet(null);
          cn.setAuthorship(extendedAuthor);
        }
      }
    }
  }

  private void logMatcher(Matcher matcher) {
    int i = -1;
    while (i < matcher.groupCount()) {
      i++;
      LOG.debug("  {}: >{}<", i, matcher.group(i));
    }
  }

}
