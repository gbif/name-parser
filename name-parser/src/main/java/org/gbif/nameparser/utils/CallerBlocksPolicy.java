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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

public class CallerBlocksPolicy implements RejectedExecutionHandler {
  private static final Logger LOG = LoggerFactory.getLogger(CallerBlocksPolicy.class);
  final long blockTime;

  /**
   * @param blockTime in milliseconds
   */
  public CallerBlocksPolicy(long blockTime) {
    this.blockTime = blockTime;
  }

  @Override
  public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
    if (!executor.isShutdown()) {
      LOG.warn("Block job submission for {}ms from thread {}: {}", blockTime, Thread.currentThread().getName(), r);
      try {
        //LockSupport.parkNanos(blockTime * 1000);
        TimeUnit.MILLISECONDS.sleep(blockTime);
        executor.submit(r).get();

      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RejectedExecutionException(e);

      } catch (ExecutionException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
