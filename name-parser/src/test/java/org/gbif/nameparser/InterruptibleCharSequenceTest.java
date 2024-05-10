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
package org.gbif.nameparser;

import org.gbif.nameparser.utils.NamedThreadFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    final ExecutorService exec = new ThreadPoolExecutor(0, 10, 250L, TimeUnit.MILLISECONDS,
        new SynchronousQueue<Runnable>(),
        new NamedThreadFactory(threadName, Thread.NORM_PRIORITY, true),
        new ThreadPoolExecutor.CallerRunsPolicy());

    List<Future<Long>> futures = new ArrayList<>();
    for (int x = 0; x<8; x++) {
      System.out.println("Submitting task " + x);
      futures.add(exec.submit(LongRegxJob.interruptable()));
    }

    for (Future<Long> f : futures) {
      try {
        Long duration = f.get(100, TimeUnit.MILLISECONDS);
        fail("Expected to timeout but parsed in " + duration + "ms");

      } catch (TimeoutException e) {
        // timeout happend, expected. Add more tasks
        System.out.println("Task " + f + " timed out as expected. Cancel");
        f.cancel(true);

      } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
      }
    }

    //allow for some time to interrupt and destroy the threads
    long sleep = 500;
    System.out.println("Wait for " + sleep + "ms");
    TimeUnit.MILLISECONDS.sleep(sleep);

    // now make sure the regex runner thread is dead!
    Set<Thread> threads = Thread.getAllStackTraces().keySet().stream()
                                .filter(t -> t.getName().startsWith(threadName))
                                .collect(Collectors.toSet());
    for (Thread t : threads) {
      assertNotSame("Running executor thread detected", Thread.State.RUNNABLE, t.getState());
    }
    // there should be no idle threads kept by the pool
    assertEquals(0, threads.size());
    exec.shutdown();
  }

}