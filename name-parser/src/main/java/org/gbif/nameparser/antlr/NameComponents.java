/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.nameparser.antlr;

/**
 * Plain holder mirroring the slot layout that the regex {@code NAME_PATTERN} used to surface via
 * {@code matcher.group(N)}. Field names match the spirit of the 23 NAME_PATTERN groups so the
 * existing extraction code in {@code ParsingJob.parseNormalisedName} can be ported with minimal
 * churn.
 *
 * Fields are package-default so the listener can populate them directly. Consumers read via
 * the getter methods.
 */
public class NameComponents {
  // Group 1: monomial / genus
  String monomialOrGenus;
  boolean hybridGenus;
  boolean placeholderGenus;     // "?" form

  // Group 2: (Subgenus) — the parenthesised infrageneric form
  String subgenusParens;

  // Group 3 + 4: rank-marker form: "subg. Foo" → infragenericRankMarker="subg", infragenericEpithet="Foo"
  String infragenericRankMarker;
  String infragenericEpithet;

  // Group 5: specific epithet identification qualifier ("aff.", "cf.", "nr.")
  String specificQualifier;

  // Group 6: specific epithet (the first lowercase epithet after the genus)
  String specificEpithet;
  boolean hybridSpecies;

  // Group 7: bare middle epithet (4-parted name, no rank marker)
  String middleEpithet;

  // Group 8: infraspecific epithet identification qualifier
  String infraspecificQualifier;

  // Group 9 + 10: ranked infraspecific: "subsp. baltica"
  String infraspecificRankMarker;
  String infraspecificEpithet;
  boolean hybridInfraspecies;

  // Group 14 gating + group 15-22 split for basionym (in parens) and combination authorship.
  // Each authorship part is captured as raw author-blob substrings so the existing
  // ParsingJob.parseAuthorship(ex, auth, year) helper can do the author-team splitting.
  boolean hasAuthorship;
  String basionymExAuthors;
  String basionymAuthors;
  String basionymSanctAuthor;
  String basionymYear;
  String combinationExAuthors;
  String combinationAuthors;
  String combinationSanctAuthor;
  String combinationYear;

  // Group 23: anything left over after a successful structural parse — flags PARTIAL state
  String remainder;

  public String getMonomialOrGenus() { return monomialOrGenus; }
  public boolean isHybridGenus() { return hybridGenus; }
  public boolean isPlaceholderGenus() { return placeholderGenus; }
  public String getSubgenusParens() { return subgenusParens; }
  public String getInfragenericRankMarker() { return infragenericRankMarker; }
  public String getInfragenericEpithet() { return infragenericEpithet; }
  public String getSpecificQualifier() { return specificQualifier; }
  public String getSpecificEpithet() { return specificEpithet; }
  public boolean isHybridSpecies() { return hybridSpecies; }
  public String getMiddleEpithet() { return middleEpithet; }
  public String getInfraspecificQualifier() { return infraspecificQualifier; }
  public String getInfraspecificRankMarker() { return infraspecificRankMarker; }
  public String getInfraspecificEpithet() { return infraspecificEpithet; }
  public boolean isHybridInfraspecies() { return hybridInfraspecies; }
  public boolean hasAuthorship() { return hasAuthorship; }
  public String getBasionymExAuthors() { return basionymExAuthors; }
  public String getBasionymAuthors() { return basionymAuthors; }
  public String getBasionymSanctAuthor() { return basionymSanctAuthor; }
  public String getBasionymYear() { return basionymYear; }
  public String getCombinationExAuthors() { return combinationExAuthors; }
  public String getCombinationAuthors() { return combinationAuthors; }
  public String getCombinationSanctAuthor() { return combinationSanctAuthor; }
  public String getCombinationYear() { return combinationYear; }
  public String getRemainder() { return remainder; }
}
