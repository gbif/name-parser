package org.gbif.nameparser;

import org.gbif.api.model.checklistbank.ParsedName;

import java.io.File;
import java.io.Reader;
import java.nio.charset.Charset;

import com.google.common.io.Files;
import org.apache.commons.io.LineIterator;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.gbif.nameparser.NameParser.normalize;

/**
 * A test class for manually debugging name parsing.
 * To get the full debug logs please set the org.gbif.nameparser logger in logback-test to DEBUG.
 */
@Ignore("This test class is for manual use only")
public class ParseNames {
  private static Logger LOG = LoggerFactory.getLogger(ParseNames.class);
  private static NameParser parser = new NameParser();

  @Test
  public void testFile() throws Exception {
    LOG.info("\n\nSTARTING FULL PARSER\n");
    Reader reader = Files.newReader(new File("/Users/markus/Downloads/col/scinames.txt"), Charset.forName("UTF8"));
    LineIterator iter = new LineIterator(reader);

    int parseFails = 0;
    int parseNoAuthors = 0;
    int cnt = 0;
    final long start = System.currentTimeMillis();

    while (iter.hasNext()) {
      cnt++;
      String name = iter.nextLine();
      if (name == null || name.startsWith("#") || name.trim().isEmpty()) {
        continue;
      }
      try {
        final long pStart = System.currentTimeMillis();
        ParsedName n = parser.parse(name);
        final long duration = System.currentTimeMillis() - pStart;
        if (!n.isAuthorsParsed()) {
          parseNoAuthors++;
        }
        if (duration > 100) {
          LOG.info("SLOOOOW {}ms\t{}", duration, name);
        } else if (duration > 20) {
          LOG.info("SLOW {}ms\t{}", duration, name);
        }
        // copy scientific name, we dont wanna compare it, it might be slightly different
      } catch (UnparsableException e) {
        parseFails++;
        LOG.warn("FAIL {}\t{}", e.type, name);
      }
    }
    long end = System.currentTimeMillis();
    LOG.info("\n\nNames tested: " + cnt);
    LOG.info("Names failed: {}", parseFails);
    LOG.info("Names ignoring authors: {}", parseNoAuthors);
    LOG.info("Total time: {}", (end - start));
    LOG.info("Average per name: {}", (((double) end - start) / cnt));
  }

  @Test
  public void debugName() throws Exception {
    String name = "Oreocharis aurea var. cordato-ovata (C.Y. Wu ex H.W. Li) K.Y. Pan, A.L. Weitzman, & L.E. Skog";
    name = "Torulopsis deparaffina H.T. Gao, C.J. Mu, R.H. Li, L.G. Wei, W.Y. Tan, Yue Y. Li, Zhong Q. Li, X.Z. Zhang & J.E. Wang 1979";
    name = "Ophiocordyceps mrciensis (Aung, J.C. Kang, Z.Q. Liang, Soytong & K.D. Hyde) G.H. Sung, J.M. Sung, Hywel-Jones & Spatafora 200";
    name = "Paecilomyces hepiali Q.T. Chen & R.Q. Dai ex R.Q. Dai, X.M. Li, A.J. Shao, Shu F. Lin, J.L. Lan, Wei H. Chen & C.Y. Shen";
    name = "Fusarium mexicanum Otero-Colina, Rodr.-Alvar., Fern.-Pavía, M. Maymon, R.C. Ploetz, T. Aoki, O'Donnell & S. Freeman 201";
    name = "Cyrtodactylus bintangrendah Grismer, Wood Jr,  Quah, Anuar, Muin, Sumontha, Ahmad, Bauer, Wangkulangkul, Grismer9 & Pauwels, 201";
    name = "Equicapillimyces hongkongensis S.S.Y. Wong, A.H.Y. Ngan, Riggs, J.L.L. Teng, G.K.Y. Choi, R.W.S. Poon, J.J.Y. Hui, F.J. Low, Luk &";
    name = "Candida mesorugosa G.M. Chaves, G.R. Terçarioli, A.C.B. Padovan, R. Rosas, R.C. Ferreira, A.S.A. Melo & A.L. Colombo 20";

    final long pStart = System.currentTimeMillis();
    ParsedName n = parser.parse(name);
    final long duration = System.currentTimeMillis() - pStart;
    LOG.info("Parsed name in {}ms: {}", duration, n);
  }

  @Test
  public void testNewProblemNames() {
    // if no args supplied, then use some default examples
    String[] names = new String[] {"Candidatus Liberibacter solanacearum","Advenella kashmirensis W13003","Garra cf. dampaensis M23","Sphingobium lucknowense F2","Pseudomonas syringae pv. atrofaciens LMG 5095"};

    for (String name : names) {
      LOG.debug("\n\nIN   : " + name);
      ParsedName pn = null;
      try {
        pn = parser.parse(name);
      } catch (UnparsableException e) {
        LOG.error("UnparsableException", e);
      }
      LOG.debug("NORM : " + normalize(name));

      if (pn != null) {
        LOG.info("FULL : " + pn);
      } else {
        LOG.info("FULL : CANNOT PARSE");
      }
    }
  }

}
