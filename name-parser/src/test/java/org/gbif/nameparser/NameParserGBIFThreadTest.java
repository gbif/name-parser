package org.gbif.nameparser;


import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import org.gbif.nameparser.api.ParsedName;
import org.gbif.nameparser.api.UnparsableNameException;
import org.gbif.nameparser.utils.NamedThreadFactory;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


@Ignore("Manual longer runnng test to test parse in multithreaded environmen")
public class NameParserGBIFThreadTest {
  static final String NAME = "Oreocharis aurea var. cordato-ovata K.Y. Pan, A.L. Weitzman & Skog, ";
  static final int REPEAT = 10;

  @Test
  public void testThreads() throws Exception {
    warm();

    Stopwatch watch = Stopwatch.createStarted();
    for (int i = 0; i < REPEAT; i++) {
      NameParserGBIF parser = new NameParserGBIF();
      for (int year = 1800; year < 2016; year++) {
        ParsedName pn = parser.parse(NAME+year);
      }
    }
    System.out.println(watch.elapsed(TimeUnit.MILLISECONDS));
  }

  private void warm() throws UnparsableNameException {
    NameParserGBIF parser = new NameParserGBIF();
    ParsedName pn = parser.parse(NAME);
  }

  @Test
  public void testMultiThreaded() throws Exception {
    ExecutorService exec = Executors.newFixedThreadPool(10, new NamedThreadFactory("test-executor"));
    warm();

    List<ParseMe> jobs = Lists.newArrayList();
    Stopwatch watch = Stopwatch.createStarted();
    for (int i = 0; i < REPEAT; i++) {
      for (int year = 1800; year < 2016; year++) {
        jobs.add(new ParseMe(year));
      }
    }
    List done = exec.invokeAll(jobs);
    System.out.println(watch.elapsed(TimeUnit.MILLISECONDS));

    Thread.sleep(1000*16);
    System.out.println("Bye bye");
  }

  @Test
  public void testMultiThreaded2() throws Exception {
    ExecutorService exec = Executors.newFixedThreadPool(10, new NamedThreadFactory("test-executor"));
    warm();

    List<ParseMeStatic> jobs = Lists.newArrayList();
    Stopwatch watch = Stopwatch.createStarted();
    for (int i = 0; i < REPEAT; i++) {
      for (int year = 1800; year < 2016; year++) {
        jobs.add(new ParseMeStatic(year));
      }
    }
    List done = exec.invokeAll(jobs);
    System.out.println(watch.elapsed(TimeUnit.MILLISECONDS));

    Thread.sleep(1000*16);
    System.out.println("Bye bye");
  }

  class ParseMe implements Callable<ParsedName> {
    NameParserGBIF parser = new NameParserGBIF();
    final int i;

    ParseMe(int i) {
      this.i = i;
    }

    @Override
    public ParsedName call() throws Exception {
      return parser.parse(NAME+i);
    }
  }

  static class ParseMeStatic implements Callable<ParsedName> {
    static final NameParserGBIF parser = new NameParserGBIF();
    final int i;

    ParseMeStatic(int i) {
      this.i = i;
    }

    @Override
    public ParsedName call() throws Exception {
      return parser.parse(NAME+i);
    }
  }
}