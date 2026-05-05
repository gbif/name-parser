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
