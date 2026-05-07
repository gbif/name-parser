package org.gbif.nameparser.api;

import java.util.Set;

public interface LinneanName {
  Rank getRank();

  void setRank(Rank rank);

  NomCode getCode();

  void setCode(NomCode code);

  String getUninomial();

  void setUninomial(String uni);

  String getGenus();

  void setGenus(String genus);

  String getInfragenericEpithet();

  void setInfragenericEpithet(String infraGeneric);

  String getSpecificEpithet();

  void setSpecificEpithet(String species);

  String getInfraspecificEpithet();

  void setInfraspecificEpithet(String infraSpecies);

  Set<NamePart> getNotho();

  void setNotho(NamePart notho);

  void addNotho(NamePart notho);

  default String getNamePart(NamePart part) {
    switch (part) {
      case GENERIC:
        return getGenus();
      case INFRAGENERIC:
        return getInfragenericEpithet();
      case SPECIFIC:
        return getSpecificEpithet();
      case INFRASPECIFIC:
        return getInfraspecificEpithet();
      default:
        return null;
    }
  }
}
