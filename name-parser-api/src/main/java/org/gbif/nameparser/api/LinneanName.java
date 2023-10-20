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
