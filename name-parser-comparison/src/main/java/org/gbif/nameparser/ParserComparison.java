package org.gbif.nameparser;

import com.google.common.base.Stopwatch;
import com.google.common.io.Closeables;
import org.apache.commons.io.IOUtils;
import org.gbif.nameparser.api.NameParser;
import org.gbif.nameparser.api.ParsedName;
import org.gbif.nameparser.api.UnparsableNameException;
import org.gbif.nameparser.gna.NameParserGNA;
import org.gbif.nameparser.utils.Closer;
import org.gbif.utils.file.csv.CSVReader;
import org.gbif.utils.file.csv.CSVReaderFactory;
import org.gbif.utils.text.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 *
 */
public class ParserComparison implements Runnable {
    private Stopwatch watch = Stopwatch.createUnstarted();

    private NameParser gbif = new NameParserGBIF();
    private NameParser gna = new NameParserGNA();

    private int counter;
    private long gbifTime;
    private long gnaTime;

    @Override
    public void run() {
        CSVReader names = null;
        counter = 0;
        try {
            names = CSVReaderFactory.buildUtf8TabReader(getClass().getResourceAsStream("/names.txt"));
            for (int x=0; x<1000000; x++) {
                String name = StringUtils.randomSpecies() + " " + StringUtils.randomAuthor() + ", " + StringUtils.randomSpeciesYear();
                System.out.println(name);
                counter++;
                parse(true, name);
                parse(false, name);
            }

/*
            for (String[] row : names) {
                String name = row[0];
                System.out.println(name);
                for (int x=0; x<1000; x++) {
                    name = StringUtils.randomSpecies() + " " + StringUtils.randomAuthor();
                    counter++;
                    parse(true, name);
                    parse(false, name);
                }
            }
*/

        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            names.close();
            System.out.println(String.format("GBIF - total time parsing %s names: %s", counter, gbifTime));
            System.out.println(String.format("GNA  - total time parsing %s names: %s", counter, gnaTime));
            System.out.println(String.format("GBIF - total time parsing %s names: %s ms", counter, gbifTime/1000));
            System.out.println(String.format("GNA  - total time parsing %s names: %s ms", counter, gnaTime/1000));
            shutdown();
        }
    }

    public void shutdown(){
      Closer.closeQuitely(gbif);
      Closer.closeQuitely(gna);
    }
    
    private void parse(boolean isGbif, String name) {
        NameParser parser = isGbif ? gbif : gna;
        watch.reset();
        try {
          watch.start();
          try {
            ParsedName pn = parser.parse(name);
            //System.out.println(String.format("  %s %5s %12s: %s", (isGbif ? "GBIF" : "GNA "), time, pn.getType(), pn.canonicalNameComplete()));
          } catch (UnparsableNameException e) {
            System.err.println(String.format("UNPARSABLE %s: %s", (isGbif ? "GBIF" : "GNA "), name));
            e.printStackTrace();
          }
          long time = watch.elapsed(TimeUnit.MICROSECONDS);
          if (isGbif) {
              gbifTime += time;
          } else {
              gnaTime += time;
          }
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
    }

    public static void main(String[] args) {
        ParserComparison comparison = new ParserComparison();
        comparison.run();
    }
}
