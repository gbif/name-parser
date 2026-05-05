package org.gbif.nameparser;

import org.gbif.nameparser.api.NameParser;
import org.gbif.nameparser.api.NomCode;
import org.gbif.nameparser.api.ParsedName;
import org.gbif.nameparser.api.Rank;
import org.gbif.nameparser.api.UnparsableNameException;
import org.gbif.nameparser.pipeline.Pipeline;

import javax.annotation.Nullable;

/**
 * Pure synchronous, thread-safe scientific name parser. Each call runs on the calling
 * thread; callers manage their own timeouts.
 */
public class NameParserImpl implements NameParser {

  @Override
  public ParsedName parse(String scientificName,
                          @Nullable String authorship,
                          @Nullable Rank rank,
                          @Nullable NomCode code) throws UnparsableNameException {
    return Pipeline.run(scientificName, authorship, rank, code);
  }
}
