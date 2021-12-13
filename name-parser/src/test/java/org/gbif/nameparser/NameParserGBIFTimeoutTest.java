package org.gbif.nameparser;

import org.apache.commons.lang3.time.StopWatch;
import org.gbif.nameparser.api.NameParser;
import org.gbif.nameparser.api.Rank;
import org.gbif.nameparser.api.UnparsableNameException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 *
 */
public class NameParserGBIFTimeoutTest {

  public static final String TIMEOUT_NAME = "Desmarestia ligulata subsp. muelleri (M.E.Ramirez, A.F.Peters, S.S.Y. Wong, A.H.Y. Ngan, Riggs, J.L.L. Teng, A.F.Peters, E.C.Yang, A.F.Peters, E.C.Yang & F.C.Küpper & van Reine, 2014) S.S.Y. Wong, A.H.Y. Ngan, Riggs, J.L.L. Teng, A.F.Peters, E.C.Yang, A.F.Peters, E.C.Yang, F.C.Küpper, van Reine, S.S.Y. Wong, A.H.Y. Ngan, Riggs, J.L.L. Teng, A.F.Peters, E.C.Yang, A.F.Peters, E.C.Yang, A.F.Peters, S.S.Y. Wong, A.H.Y. Ngan, Riggs, J.L.L. Teng, A.F.Peters, E.C.Yang, A.F.Peters, E.C.Yang & F.C.Küpper & van Reine, 2014) S.S.Y. Wong, A.H.Y. Ngan, Riggs, J.L.L. Teng, A.F.Peters, E.C.Yang, A.F.Peters, E.C.Yang, F.C.Küpper, van Reine, S.S.Y. Wong, A.H.Y. Ngan, Riggs, J.L.L. Teng, A.F.Peters, E.C.Yang, A.F.Peters, E.C.Yang, F.C.Küpper & van Reine, 2014";
  public static final String TIMEOUT_AUTHORSHIP = "Coloma, Carvajal-Endara, Dueñas, Paredes-Recalde, Morales-Mite, Almeida-Reinoso et al., 2012)";

  final int extra = 100;

  NameParser parser = new NameParserGBIF(10, 0, 10);
  StopWatch watch = new StopWatch();

  @Before
  public void init() {
    // warm up parser
    try {
      parser.parse("Abies", Rank.GENUS);
    } catch (UnparsableNameException ex) {
      // too short on your machine? ignore
    }
  }

  @After
  public void teardown() throws Exception {
    parser.close();
  }

  @Test
  public void timeoutLongNames() throws Exception {
    // this name takes 13993ms on a new macbook pro !!!
    watch.start();
    try {
      parser.parse(TIMEOUT_NAME);
      fail("Expected to be unparsable, but only took " + watch.getTime() + "ms");

    } catch (UnparsableNameException ex) {

      final long duration = watch.getTime();
      System.out.println("Parsing took " + duration + "ms");

      checkRemainingThreads(extra);
    }
  }

  @Test
  public void timeoutLongAuthorships() throws Exception {
    // this name takes 13993ms on a new macbook pro !!!
    watch.start();
    try {
      parser.parseAuthorship(TIMEOUT_AUTHORSHIP);
      fail("Expected to time out, but only took " + watch.getTime() + "ms");

    } catch (UnparsableNameException ex) {

      final long duration = watch.getTime();
      System.out.println("Parsing took " + duration + "ms");

      checkRemainingThreads(extra);
    }
  }

  void checkRemainingThreads(int extra) throws Exception {
    // make sure no further parser threads are running. Give the JVM a few milliseconds to interrupt the task
    TimeUnit.MILLISECONDS.sleep(extra);

    Set<Thread> threads = Thread.getAllStackTraces().keySet().stream()
                                .filter(t -> t.getName().startsWith(NameParserGBIF.THREAD_NAME))
                                .collect(Collectors.toSet());
    for (Thread t : threads) {
      System.out.println(t.getName() + "  -->  " + t.getState());
      assertNotSame("Running executor thread detected", Thread.State.RUNNABLE, t.getState());
    }
    assertTrue(threads.isEmpty());

  }
}