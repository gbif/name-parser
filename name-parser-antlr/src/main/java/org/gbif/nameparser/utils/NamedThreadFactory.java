package org.gbif.nameparser.utils;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Modified Executors DefaultThreadFactory to allow custom named thread pools.
 * Otherwise, this factory yields the same semantics as the thread factory returned by
 * {@link Executors#defaultThreadFactory()}.
 *
 * Optionally a priority or daemon flag can be provided.
 */
public class NamedThreadFactory implements ThreadFactory {
  private final ThreadGroup group;
  private final AtomicInteger threadNumber = new AtomicInteger(1);
  private final String namePrefix;
  private final int priority;
  private final boolean daemon;


  /**
   * Creates a new named user thread factory using a normal priority.
   * @param poolName the name prefix of the thread pool which will be appended -number for the individual thread
   */
  public NamedThreadFactory(String poolName) {
    this(poolName, Thread.NORM_PRIORITY, false);
  }

  /**
   * Creates a new named thread factory using explicit priority and daemon settings.
   */
  public NamedThreadFactory(String poolName, int priority, boolean daemon) {
    SecurityManager s = System.getSecurityManager();
    group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
    namePrefix = poolName + "-";
    this.priority = priority;
    this.daemon = daemon;
  }

  public Thread newThread(Runnable r) {
    Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
    t.setPriority(priority);
    t.setDaemon(daemon);
    return t;
  }
}
