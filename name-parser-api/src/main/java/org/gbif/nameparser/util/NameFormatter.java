package org.gbif.nameparser.util;

import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import org.gbif.nameparser.api.*;

/**
 *
 */
public class NameFormatter {
  public static final char HYBRID_MARKER = '×';
  private static final String NOTHO_PREFIX = "notho";
  private static final Joiner AUTHORSHIP_JOINER = Joiner.on(", ").skipNulls();
  private static final String ITALICS_OPEN = "<i>";
  private static final String ITALICS_CLOSE = "</i>";
  private static final Pattern AL = Pattern.compile("^al\\.?$");
  
  private NameFormatter() {
  
  }
  
  /**
   * A full scientific name with authorship from the individual properties in its canonical form.
   * Autonyms are rendered without authorship and subspecies are using the subsp rank marker
   * unless a name is assigned to the zoological code.
   */
  public static String canonical(ParsedName n) {
    // TODO: show authorship for zoological autonyms?
    // TODO: how can we best remove subsp from zoological names?
    // https://github.com/gbif/portal-feedback/issues/640
    return buildName(n, true, true, true, true, false, true, false, true, true, false,  false, true, true, false);
  }
  
  /**
   * A full scientific name just as canonicalName, but without any authorship.
   */
  public static String canonicalWithoutAuthorship(ParsedName n) {
    return buildName(n, true, true, false, true, false, true, false, true, true, false,  false, true, true,  false);
  }
  
  /**
   * A minimal canonical name with nothing else but the 3 main name parts (genus, species, infraspecific).
   * No rank or hybrid markers and no authorship, cultivar or strain information is rendered.
   * Infrageneric names are represented without a leading genus.
   * Unicode characters will be replaced by their matching ASCII characters.
   * <p/>
   * For example:
   * Abies alba
   * Abies alba alpina
   * Bracteata
   */
  public static String canonicalMinimal(ParsedName n) {
    return buildName(n, false, false, false, false, false, true, true, false, false, false,  false, false, false, false);
  }
  
  /**
   * Assembles a full name with all details including non code compliant, informal remarks.
   */
  public static String canonicalComplete(ParsedName n) {
    return buildName(n, true, true, true, true, true, true, false, true, true, true,  true, true, true, false);
  }
  
  /**
   * Assembles a full name with all details including non code compliant, informal remarks and html markup.
   */
  public static String canonicalCompleteHtml(ParsedName n) {
    return buildName(n, true, true, true, true, true, true, false, true, true, true,  true, true, true, true);
  }
  
  /**
   * The full concatenated authorship for parsed names including the sanctioning author.
   */
  public static String authorshipComplete(ParsedAuthorship n) {
    StringBuilder sb = new StringBuilder();
    appendAuthorship(n, sb);
    return sb.length() == 0 ? null : sb.toString();
  }
  
  /**
   * Renders the authors of an authorship including ex authors, optionally with the year included.
   */
  public static String authorString(Authorship authors, boolean inclYear) {
    StringBuilder sb = new StringBuilder();
    appendAuthorship(sb, authors, inclYear);
    return sb.length() == 0 ? null : sb.toString();
  }
  
  private static void openItalics(StringBuilder sb) {
    sb.append(ITALICS_OPEN);
  }
  
  private static void closeItalics(StringBuilder sb) {
    sb.append(ITALICS_CLOSE);
  }
  
  private static void appendInItalics(StringBuilder sb, String x, boolean html) {
    if (html) {
      sb.append(ITALICS_OPEN)
          .append(x)
          .append(ITALICS_CLOSE);
      
    } else {
      sb.append(x);
    }
  }
  
  /**
   * build a name controlling all available flags for name parts to be included in the resulting name.
   *
   * @param hybridMarker         include the hybrid marker with the name if existing
   * @param rankMarker           include the infraspecific or infrageneric rank marker with the name if existing
   * @param authorship           include the names authorship (authorteam and year)
   * @param genusForinfrageneric show the genus for infrageneric names
   * @param infrageneric         include the infrageneric name in brackets for species or infraspecies
   * @param decomposition        decompose unicode ligatures into their corresponding ascii ones, e.g. æ beomes ae
   * @param asciiOnly            transform unicode letters into their corresponding ascii ones, e.g. ø beomes o and ü u
   * @param showIndet            if true include the rank marker for incomplete determinations, for example Puma spec.
   * @param showQualifier        if true include the epithet qualifiers
   * @param nomNote              include nomenclatural notes
   * @param html                 add html markup
   */
  public static String buildName(ParsedName n,
                                 boolean hybridMarker,
                                 boolean rankMarker,
                                 boolean authorship,
                                 boolean genusForinfrageneric,
                                 boolean infrageneric,
                                 boolean decomposition,
                                 boolean asciiOnly,
                                 boolean showQualifier,
                                 boolean showIndet,
                                 boolean nomNote,
                                 boolean showSensu,
                                 boolean showCultivar,
                                 boolean showStrain,
                                 boolean html
  ) {
    StringBuilder sb = new StringBuilder();
    
    boolean candidateItalics = false;
    if (n.isCandidatus()) {
      sb.append("\"");
      if (html) {
        openItalics(sb);
        candidateItalics = true;
        // we turn off html here cause the entire name should be in italics!
        html = false;
      }
      sb.append("Candidatus ");
    }
    
    if (n.getUninomial() != null) {
      // higher rank names being just a uninomial!
      if (hybridMarker && NamePart.GENERIC == n.getNotho()) {
        sb.append(HYBRID_MARKER)
            .append(" ");
      }
      appendInItalics(sb, n.getUninomial(), html);
      
    } else {
      // bi- or trinomials or infrageneric names
      if (n.getInfragenericEpithet() != null) {
        if ((isUnknown(n.getRank()) && n.getSpecificEpithet() == null) || (n.getRank() != null && n.getRank().isInfragenericStrictly())) {
          boolean showInfraGen = true;
          // the infrageneric is the terminal rank. Always show it and wrap it with its genus if requested
          if (n.getGenus() != null && genusForinfrageneric) {
            appendGenus(sb, n, hybridMarker, showQualifier, html);
            sb.append(" ");
            // we show zoological infragenerics in brackets,
            // but use rank markers for botanical names (unless its no defined rank)
            if (NomCode.ZOOLOGICAL == n.getCode()) {
              sb.append("(");
              if (hybridMarker && NamePart.INFRAGENERIC == n.getNotho()) {
                sb.append(HYBRID_MARKER)
                    .append(' ');
              }
              appendInItalics(sb, n.getInfragenericEpithet(), html);
              sb.append(")");
              showInfraGen = false;
            }
          }
          if (showInfraGen) {
            if (rankMarker) {
              // If we know the rank we use explicit rank markers
              // this is how botanical infrageneric names are formed, see http://www.iapt-taxon.org/nomen/main.php?page=art21
              if (appendRankMarker(sb, n.getRank(), hybridMarker && NamePart.INFRAGENERIC == n.getNotho())) {
                sb.append(' ');
              }
            }
            appendInItalics(sb, n.getInfragenericEpithet(), html);
          }
          
        } else {
          if (n.getGenus() != null) {
            appendGenus(sb, n, hybridMarker, showQualifier, html);
          }
          if (infrageneric) {
            // additional subgenus shown for binomial. Always shown in brackets
            sb.append(" (");
            appendInItalics(sb, n.getInfragenericEpithet(), html);
            sb.append(")");
          }
        }
        
      } else if (n.getGenus() != null) {
        appendGenus(sb, n, hybridMarker, showQualifier, html);
      }
      
      if (n.getSpecificEpithet() == null) {
        if (showIndet && n.getGenus() != null && n.getCultivarEpithet() == null) {
          if (n.getRank() != null && n.getRank().isSpeciesOrBelow()) {
            // no species epithet given, indetermined!
            if (n.getRank().isInfraspecific()) {
              // maybe we have an infraspecific epithet? force to show the rank marker
              appendInfraspecific(sb, n, hybridMarker, showQualifier, rankMarker, true, html);
            } else {
              sb.append(" ");
              sb.append(n.getRank().getMarker());
            }
            authorship = false;
          }
        } else if (n.getInfraspecificEpithet() != null) {
          appendInfraspecific(sb, n, hybridMarker, showQualifier, rankMarker, false, html);
        }
        
      } else {
        // species part
        sb.append(' ');
        if (showQualifier && n.hasEpithetQualifier(NamePart.SPECIFIC )) {
          sb.append(n.getEpithetQualifier().get(NamePart.SPECIFIC))
              .append(" ");
        }
        if (hybridMarker && NamePart.SPECIFIC == n.getNotho()) {
          sb.append(HYBRID_MARKER)
              .append(" ");
        }
        appendInItalics(sb, n.getSpecificEpithet(), html);
        
        if (n.getInfraspecificEpithet() == null) {
          // Indetermined infraspecies? Only show indet cultivar marker if no cultivar epithet exists
          if (showIndet
              && n.getRank() != null
              && n.getRank().isInfraspecific()
              && (NomCode.CULTIVARS != n.getRank().isRestrictedToCode() || n.getCultivarEpithet() == null)
          ) {
            // no infraspecific epitheton given, but rank below species. Indetermined!
            // use ssp. for subspecies in case of indetermined names
            if (n.getRank() == Rank.SUBSPECIES) {
              sb.append(" ssp.");
            } else {
              sb.append(' ');
              sb.append(n.getRank().getMarker());
            }
            authorship = false;
          }
          
        } else {
          // infraspecific part
          appendInfraspecific(sb, n, hybridMarker, showQualifier, rankMarker, false, html);
          // non autonym authorship ?
          if (n.isAutonym()) {
            authorship = false;
          }
        }
      }
    }
    
    // closing quotes for Candidatus names
    if (n.isCandidatus()) {
      if (candidateItalics) {
        closeItalics(sb);
      }
      sb.append("\"");
    }
    
    // uninomial, genus, infragen, species or infraspecies authorship
    if (authorship && n.hasAuthorship()) {
      sb.append(" ");
      appendAuthorship(n, sb);
    }
    
    // add strain name
    if (showStrain && n.getStrain() != null) {
      sb.append(" ")
          .append(n.getStrain());
    }
    
    // add cultivar name
    if (showCultivar && n.getCultivarEpithet() != null) {
      if (Rank.CULTIVAR_GROUP == n.getRank()) {
        sb.append(" ")
            .append(n.getCultivarEpithet())
            .append(" Group");
        
      } else if (Rank.GREX == n.getRank()) {
        sb.append(" ")
            .append(n.getCultivarEpithet())
            .append(" gx");
        
      } else {
        sb.append(" '")
            .append(n.getCultivarEpithet())
            .append("'");
      }
    }
    
    // add sensu/sec reference
    if (showSensu && n.getTaxonomicNote() != null) {
      appendIfNotEmpty(sb, " ")
          .append(n.getTaxonomicNote());
    }
    
    // add nom status
    if (nomNote && n.getNomenclaturalNote() != null) {
      appendIfNotEmpty(sb, ", ")
          .append(n.getNomenclaturalNote());
    }
    
    // final char transformations
    String name = sb.toString().trim();
    if (decomposition) {
      name = UnicodeUtils.decompose(name);
    }
    if (asciiOnly) {
      name = UnicodeUtils.ascii(name);
    }
    return Strings.emptyToNull(name);
  }
  
  private static StringBuilder appendInfraspecific(StringBuilder sb, ParsedName n, boolean hybridMarker, boolean showQualifier, boolean rankMarker, boolean forceRankMarker, boolean html) {
    // infraspecific part
    sb.append(' ');
    if (showQualifier && n.hasEpithetQualifier(NamePart.INFRASPECIFIC )) {
      sb.append(n.getEpithetQualifier().get(NamePart.INFRASPECIFIC))
          .append(" ");
    }
    if (hybridMarker && NamePart.INFRASPECIFIC == n.getNotho()) {
      if (rankMarker && n.getRank() != null && isInfraspecificMarker(n.getRank())) {
        sb.append("notho");
      } else {
        sb.append(HYBRID_MARKER);
        sb.append(" ");
      }
    }
    // hide subsp. from zoological names
    if (forceRankMarker || rankMarker && (isNotZoo(n.getCode()) || Rank.SUBSPECIES != n.getRank() || n.isHybridName())) {
      if (appendRankMarker(sb, n.getRank(), NameFormatter::isInfraspecificMarker, false) && n.getInfraspecificEpithet() != null) {
        sb.append(' ');
      }
    }
    if (n.getInfraspecificEpithet() != null) {
      appendInItalics(sb, n.getInfraspecificEpithet(), html);
    }
    return sb;
  }
  
  private static StringBuilder appendIfNotEmpty(StringBuilder sb, String toAppend) {
    if (sb.length() > 0) {
      sb.append(toAppend);
    }
    return sb;
  }
  
  private static boolean isNotZoo(NomCode code) {
    return code != null && code != NomCode.ZOOLOGICAL;
  }

  private static boolean isUnknown(Rank r) {
    return r == null || r.otherOrUnranked();
  }
  
  private static boolean isInfragenericMarker(Rank r) {
    return r != null && r.isInfrageneric() && !r.isUncomparable();
  }
  
  private static boolean isInfraspecificMarker(Rank r) {
    return r.isInfraspecific() && !r.isUncomparable();
  }
  
  /**
   * @return true if rank marker was added
   */
  private static boolean appendRankMarker(StringBuilder sb, Rank rank, boolean nothoPrefix) {
    return appendRankMarker(sb, rank, null, nothoPrefix);
  }
  
  /**
   * @return true if rank marker was added
   */
  private static boolean appendRankMarker(StringBuilder sb, Rank rank, Predicate<Rank> ifRank, boolean nothoPrefix) {
    if (rank != null
        && rank.getMarker() != null
        && (ifRank == null || ifRank.test(rank))
    ) {
      if (nothoPrefix) {
        sb.append(NOTHO_PREFIX);
      }
      sb.append(rank.getMarker());
      return true;
    }
    return false;
  }
  
  private static StringBuilder appendGenus(StringBuilder sb, ParsedName n, boolean hybridMarker, boolean showQualifier, boolean html) {
    if (showQualifier && n.hasEpithetQualifier(NamePart.GENERIC )) {
      sb.append(n.getEpithetQualifier().get(NamePart.GENERIC ))
        .append(" ");
    }
    if (hybridMarker && NamePart.GENERIC == n.getNotho()) {
      sb.append(HYBRID_MARKER)
          .append(" ");
    }
    appendInItalics(sb, n.getGenus(), html);
    return sb;
  }
  
  private static String joinAuthors(List<String> authors, boolean abbrevWithEtAl) {
    if (abbrevWithEtAl && authors.size() > 2) {
      return AUTHORSHIP_JOINER.join(authors.subList(0, 1)) + " et al.";
      
    } else if (authors.size() > 1) {
      String end;
      if (AL.matcher(authors.get(authors.size() - 1)).find()) {
        end = " et al.";
      } else {
        end = " & " + authors.get(authors.size() - 1);
      }
      return AUTHORSHIP_JOINER.join(authors.subList(0, authors.size() - 1)) + end;
      
    } else {
      return AUTHORSHIP_JOINER.join(authors);
    }
  }
  
  /**
   * Renders the authorship with ex authors and year
   *
   * @param sb StringBuilder to append to
   */
  public static void appendAuthorship(StringBuilder sb, Authorship auth, boolean includeYear) {
    if (auth.exists()) {
      boolean authorsAppended = false;
      if (!auth.getExAuthors().isEmpty()) {
        sb.append(joinAuthors(auth.getExAuthors(), false));
        sb.append(" ex ");
        authorsAppended = true;
      }
      if (!auth.getAuthors().isEmpty()) {
        sb.append(joinAuthors(auth.getAuthors(), false));
        authorsAppended = true;
      }
      if (auth.getYear() != null && includeYear) {
        if (authorsAppended) {
          sb.append(", ");
        }
        sb.append(auth.getYear());
      }
    }
  }
  
  private static void appendAuthorship(ParsedAuthorship a, StringBuilder sb) {
    if (a.getBasionymAuthorship().exists()) {
      sb.append("(");
      appendAuthorship(sb, a.getBasionymAuthorship(), true);
      sb.append(")");
    }
    if (a.getCombinationAuthorship().exists()) {
      if (a.getBasionymAuthorship().exists()) {
        sb.append(" ");
      }
      appendAuthorship(sb, a.getCombinationAuthorship(), true);
      // Render sanctioning author via colon:
      // http://www.iapt-taxon.org/nomen/main.php?page=r50E
      //TODO: remove rendering of sanctioning author according to Paul Kirk!
      if (a.getSanctioningAuthor() != null) {
        sb.append(" : ");
        sb.append(a.getSanctioningAuthor());
      }
    }
  }
  
}
