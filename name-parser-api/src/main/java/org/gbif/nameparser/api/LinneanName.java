package org.gbif.nameparser.api;

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

  NamePart getNotho();

  void setNotho(NamePart notho);
}
