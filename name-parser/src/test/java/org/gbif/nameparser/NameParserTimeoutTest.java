package org.gbif.nameparser;

import org.apache.commons.lang3.time.StopWatch;
import org.gbif.nameparser.api.NameParser;
import org.gbif.nameparser.api.Rank;
import org.gbif.nameparser.api.UnparsableNameException;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

/**
 *
 */
public class NameParserTimeoutTest {

  @Test
  public void timeoutLongNames() throws Exception {
    final int timeout = 10;
    final int extra = 250;

    NameParser parser = new NameParserGBIF(timeout);
    StopWatch watch = new StopWatch();
    // warm up parser
    try {
      parser.parse("Abies", Rank.GENUS);
    } catch (UnparsableNameException ex) {
      // too short on your machine? ignore
    }

    // this name takes 13993ms on a new macbook pro !!!
    String name = "Desmarestia ligulata subsp. muelleri (M.E.Ramirez, A.F.Peters, S.S.Y. Wong, A.H.Y. Ngan, Riggs, J.L.L. Teng, A.F.Peters, E.C.Yang, A.F.Peters, E.C.Yang & F.C.Küpper & van Reine, 2014) S.S.Y. Wong, A.H.Y. Ngan, Riggs, J.L.L. Teng, A.F.Peters, E.C.Yang, A.F.Peters, E.C.Yang, F.C.Küpper, van Reine, S.S.Y. Wong, A.H.Y. Ngan, Riggs, J.L.L. Teng, A.F.Peters, E.C.Yang, A.F.Peters, E.C.Yang, F.C.Küpper & van Reine, 2014";
    watch.start();
    try {
      parser.parse(name);
      fail("Expected to be unparsable, but only took " + watch.getTime() + "ms");

    } catch (UnparsableNameException ex) {

      final long duration = watch.getTime();
      System.out.println("Parsing took " + duration + "ms");

      // make sure no further parser threads are running. Give the JVM a few milliseconds to interrupt the task
      Thread.sleep(extra);

      Set<Thread> threads = Thread.getAllStackTraces().keySet();
      for (Thread t : threads) {
        if (t.getName().startsWith(NameParserGBIF.THREAD_NAME)) {
          System.out.println(t.getName() + "  -->  " + t.getState());
          assertFalse("Running parser thread detected", t.getState() == Thread.State.RUNNABLE);
        }
      }
    }
  }

}