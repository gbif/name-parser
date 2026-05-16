package org.gbif.nameparser.cli.io;

import org.gbif.nameparser.api.NomCode;
import org.gbif.nameparser.api.Rank;

/**
 * One row of input destined for the parser.
 *
 * @param line       1-based line number from the source file (header row included
 *                   in the count for ColDP inputs).
 * @param id         optional record identifier copied from the input (ColDP
 *                   {@code ID} column); {@code null} for plain-text input.
 * @param name       the scientific name to parse — never {@code null}/empty.
 * @param authorship optional pre-split authorship string ({@code null} when the
 *                   authorship is embedded in {@code name} or unknown).
 * @param rank       optional caller-supplied rank ({@code null} to let the parser infer).
 * @param code       optional caller-supplied nomenclatural code ({@code null} to let
 *                   the parser infer).
 */
public record NameInput(long line, String id, String name, String authorship, Rank rank, NomCode code) {
}
