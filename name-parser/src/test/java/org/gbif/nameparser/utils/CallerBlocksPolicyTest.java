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
package org.gbif.nameparser.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CallerBlocksPolicyTest {
  private static final Logger LOG = LoggerFactory.getLogger(CallerBlocksPolicyTest.class);
  static final String threadName = "myThread";

  @Test
  public void testBlocking() throws Exception {
    // we expect all jobs to run in the workers
    testPolicy(5, new CallerBlocksPolicy(25));
  }

  @Test
  public void testCalling() throws Exception {
    // we expect 2 jobs being executed by workers, the rest runs on the caller which returns quick
    testPolicy(2, new ThreadPoolExecutor.CallerRunsPolicy());
  }

  void testPolicy(int expected, RejectedExecutionHandler policy) throws Exception {
    ExecutorService exec = new ThreadPoolExecutor(0, 2, 10, TimeUnit.MILLISECONDS,
        new SynchronousQueue<>(),
        new NamedThreadFactory(threadName, Thread.NORM_PRIORITY, true),
        policy);

    final int wait = 100;
    List<Future<Boolean>> futures = new ArrayList<>();
    for (int x = 1; x<=5; x++) {
      futures.add(exec.submit(new WaitingJob(wait)));
    }
    int workerJobs = 0;
    for (Future<Boolean> f : futures) {
      if (f.get()) {
        workerJobs++;
      }
    }
    Assert.assertEquals(expected, workerJobs);
  }

  static class WaitingJob implements Callable<Boolean> {
    final int ms;

    WaitingJob(int ms) {
      this.ms = ms;
    }

    @Override
    public Boolean call() throws Exception {
      if (!Thread.currentThread().getName().startsWith(threadName)) {
        LOG.warn("The calling thread {} is not a worker", Thread.currentThread().getName());
        return false;
      }
      TimeUnit.MILLISECONDS.sleep(ms);
      return true;
    }
  }
}