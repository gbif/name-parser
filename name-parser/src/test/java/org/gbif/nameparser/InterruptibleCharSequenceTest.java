package org.gbif.nameparser;

import org.gbif.nameparser.utils.NamedThreadFactory;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

/**
 *
 */
public class InterruptibleCharSequenceTest {
  /**
   * Runs for about 10s
   */
  static class LongRegxJob implements Callable<Long> {
    private static Logger LOG = LoggerFactory.getLogger(LongRegxJob.class);
    private static final String TEMPLATE;
    private static final Pattern PATTERN = Pattern.compile("(0*)*A");
    static {
      StringBuilder sb = new StringBuilder();
      for (int i=1; i<10000; i++) {
        sb.append("0");
      }
      TEMPLATE = sb.toString();
    }

    private final CharSequence input;

    static LongRegxJob interruptable() {
      return new LongRegxJob(true);
    }
    static LongRegxJob regular() {
      return new LongRegxJob(false);
    }

    private LongRegxJob(boolean interruptable) {
      input = interruptable ? new InterruptibleCharSequence(TEMPLATE) : TEMPLATE;
    }

    @Override
    public Long call() throws Exception {

      long startTime = System.currentTimeMillis();
      Matcher matcher = PATTERN.matcher(input);
      matcher.find(); // runs for roughly a minute!
      System.out.println(matcher.group());
      long duration = System.currentTimeMillis() - startTime;
      LOG.info("Regex finished in {}ms", duration);
      return duration;
    }
  }

  @Test
  public void testRegexTimeout() throws InterruptedException {
    final String threadName = "regex-worker";
    final ExecutorService exec = new ThreadPoolExecutor(0, 20, 100L, TimeUnit.MILLISECONDS,
        new SynchronousQueue<Runnable>(),
        new NamedThreadFactory(threadName, Thread.NORM_PRIORITY, true),
        new ThreadPoolExecutor.CallerRunsPolicy());

    for (int x = 0; x<8; x++) {
      System.out.println("Executing task " + x);
      Future<Long> task = exec.submit(LongRegxJob.interruptable());
      try {
        Long duration = task.get(100, TimeUnit.MILLISECONDS);
        fail("Expected to timeout but parsed in " + duration + "ms");

      } catch (TimeoutException e) {
        // timeout happend, expected. Add more tasks
        System.out.println("Task " + x + " timed out as expected. Interrupt");
        task.cancel(true);

      } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
      }
    }

    //allow for some time to interrupt
    long sleep = 100;
    System.out.println("Wait for " + sleep + "ms");
    TimeUnit.MILLISECONDS.sleep(sleep);

    // now make sure the regex runner thread is dead!
    Set<Thread> threads = Thread.getAllStackTraces().keySet();
    for (Thread t : threads) {
      if (t.getName().startsWith(threadName)) {
        System.out.println(t.getName() + "  -->  " + t.getState());
      }
    }
    for (Thread t : threads) {
      if (t.getName().startsWith(threadName)) {
        assertNotSame("Running executor thread detected", t.getState(), Thread.State.RUNNABLE);
      }
    }

    exec.shutdown();
  }

}