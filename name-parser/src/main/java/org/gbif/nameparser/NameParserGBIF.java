package org.gbif.nameparser;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.gbif.nameparser.api.*;
import org.gbif.nameparser.utils.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

import static org.gbif.nameparser.ParsingJob.unparsable;

/**
 * The default GBIF name parser build on regular expressions.
 * In order to avoid long running regex matches it runs the core parsing in a background threadpool
 * which is shared across all instances of the parser.
 *
 * Make sure to reuse the instance as much as possible and don't forget to close it for the threads to shutdown properly.
 */
public class NameParserGBIF implements NameParser {

  private static Logger LOG = LoggerFactory.getLogger(NameParserGBIF.class);
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


  /**
   * The default name parser without an explicit monomials list using the default timeout of 1s for parsing.
   */
  public NameParserGBIF() {
    this(1000);  // max default parsing time is one second;
  }

  /**
   * The default name parser without an explicit monomials list using the given timeout in milliseconds for parsing.
   */
  public NameParserGBIF(long timeout) {
    Preconditions.checkArgument(timeout > 0, "Timeout needs to be at least 1ms");
    this.timeout = timeout;  // max default parsing time is one second;
  }

  public ParsedName parse(String scientificName) throws UnparsableNameException {
    return parse(scientificName, Rank.UNRANKED);
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
  public ParsedName parse(final String scientificName, Rank rank) throws UnparsableNameException {
    if (Strings.isNullOrEmpty(scientificName)) {
      unparsable(NameType.NO_NAME, null);
    }

    FutureTask<ParsedName> task = new FutureTask<ParsedName>(new ParsingJob(scientificName, rank == null ? Rank.UNRANKED : rank));
    EXEC.execute(task);

    try {
      return task.get(timeout, TimeUnit.MILLISECONDS);

    } catch (InterruptedException e) {
      LOG.warn("Thread got interrupted, shutdown executor", e);
      EXEC.shutdown();

    } catch (ExecutionException e) {
      // unwrap UnparsableNameException
      if (e.getCause() instanceof UnparsableNameException) {
        throw (UnparsableNameException) e.getCause();

      } else {
        LOG.warn("ExecutionException for name: {}", scientificName, e);
      }

    } catch (TimeoutException e) {
      // parsing timeout
      LOG.warn("Parsing timeout for name: {}", scientificName);
    }

    throw new UnparsableNameException(NameType.SCIENTIFIC, scientificName);
  }

}
