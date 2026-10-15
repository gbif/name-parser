package org.gbif.nameparser.api;

/**
 * Coarse groups of ranks, ordered from high to low, inside which the same name often keeps its identity while its
 * exact rank varies between classifications, e.g. Osteichthyes as a class or a parvphylum.
 *
 * These are not the groups of the zoological code: SPECIES excludes subspecies, which are INFRASPECIFIC.
 * SUPRAFAMILIAL means above the family group, so the rank SUPERFAMILY belongs to FAMILY.
 *
 * The vague rank SUPRAGENERIC_NAME spans SUPRAFAMILIAL and FAMILY and therefore has no group, nor have OTHER and
 * UNRANKED. See {@link Rank#getGroup()} and {@link Rank#covers(Rank)}.
 */
public enum RankGroup {

  /**
   * All ranks above the family group, see {@link Rank#isSuprafamilial()}.
   */
  SUPRAFAMILIAL,

  /**
   * The family group from gigafamily down to infratribe, see {@link Rank#isFamilyGroup()}.
   */
  FAMILY,

  /**
   * The genus group from supergenus down to the infrageneric ranks, see {@link Rank#isGenusGroup()}.
   */
  GENUS,

  /**
   * Species and species aggregates.
   */
  SPECIES,

  /**
   * All ranks below species, see {@link Rank#isInfraspecific()}.
   */
  INFRASPECIFIC
}
