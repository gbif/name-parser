package org.gbif.nameparser;

import com.google.common.base.Stopwatch;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.service.checklistbank.NameParser;
import org.gbif.utils.file.csv.CSVReader;
import org.gbif.utils.file.csv.CSVReaderFactory;

import java.util.concurrent.TimeUnit;

/**
 *
 */
public class ParserComparison implements Runnable {
    private Stopwatch watch = Stopwatch.createUnstarted();

    private NameParser gbif = new GBIFNameParser(100);
    private NameParser gna = new GNANameParser();

    private long gbifTime;
    private long gnaTime;

    @Override
    public void run() {
        CSVReader names = null;
        try {
            names = CSVReaderFactory.buildUtf8TabReader(getClass().getResourceAsStream("/names.txt"));
            for (String[] row : names) {
                System.out.println(row[0]);
                parse(true, row[0]);
                parse(false, row[0]);
            }

        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            names.close();
            System.out.println("TOTAL TIME GBIF: " + gbifTime);
            System.out.println("TOTAL TIME GNA: " + gnaTime);
        }
    }
    private void parse(boolean isGbif, String name) {
        NameParser parser = isGbif ? gbif : gna;
        watch.reset();
        try {
            watch.start();
            ParsedName pn = parser.parseQuietly(name);
            long time = watch.elapsed(TimeUnit.MICROSECONDS);
            System.out.println(String.format("  %s %5s %12s: %s", (isGbif ? "GBIF" : "GNA "), time, pn.getType(), pn.canonicalNameComplete()));
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
