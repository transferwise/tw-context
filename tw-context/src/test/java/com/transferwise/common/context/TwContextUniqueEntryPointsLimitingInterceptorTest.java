package com.transferwise.common.context;

import static org.assertj.core.api.Assertions.assertThat;

import com.transferwise.common.baseutils.ExceptionUtils;
import com.transferwise.common.baseutils.meters.cache.MeterCache;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@Slf4j
public class TwContextUniqueEntryPointsLimitingInterceptorTest {

  @AfterEach
  void setup() {
    TwContext.removeExecutionInterceptors();
  }

  // Was a production bug.
  @Test
  @SneakyThrows
  void testThatThreadsCanNotBlockEachOther() {
    final int sleepTimeMs = 5000;

    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    var meterCache = new MeterCache(meterRegistry);
    TwContext.addExecutionInterceptor(new TwContextUniqueEntryPointsLimitingInterceptor(meterCache, 1));

    final long startTimeMs = TwContextClockHolder.getClock().millis();

    CountDownLatch countDownLatch = new CountDownLatch(1);
    CountDownLatch countDownLatch1 = new CountDownLatch(1);

    Thread t0 = new Thread(() -> {
      TwContext.current().createSubContext().asEntryPoint("A", "A").execute(() -> {
        ExceptionUtils.doUnchecked(() -> {
          countDownLatch.countDown();
          countDownLatch1.await(sleepTimeMs, TimeUnit.MILLISECONDS);
          assertThat(TwContext.current().getName()).isEqualTo("A");
        });
      });
    });

    Thread t1 = new Thread(() -> {
      TwContext.current().createSubContext().asEntryPoint("B", "B").execute(() -> {
        log.info("Hello World!");
        // We were already full.
        assertThat(TwContext.current().getName()).isEqualTo("Generic");
        countDownLatch1.countDown();
      });
    });

    t0.start();
    countDownLatch.await(sleepTimeMs, TimeUnit.MILLISECONDS);
    t1.start();

    t1.join();
    t0.join();

    assertThat(TwContextClockHolder.getClock().millis() - startTimeMs).isLessThan(sleepTimeMs / 2);
  }

  /**
   * Technically this test has a chance to succeed on buggy code, but it is very very small. Alternative is to mutate the code, but then there are
   * another, bigger risks.
   */
  @Test
  @SneakyThrows
  public void testThatUniqueEntryPointIsCountedOnce() {
    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    var meterCache = new MeterCache(meterRegistry);
    TwContext.addExecutionInterceptor(new TwContextUniqueEntryPointsLimitingInterceptor(meterCache, 100));

    final int n = 100;
    Thread[] threads = new Thread[n];
    for (int i = 0; i < n; i++) {
      threads[i] = new Thread(() -> {
        TwContext.current().createSubContext().asEntryPoint("A", "A").execute(() -> {
          //no-op
        });
      });
    }
    for (int i = 0; i < n; i++) {
      threads[i].start();
    }
    for (int i = 0; i < n; i++) {
      threads[i].join();
    }

    assertThat(meterRegistry.get(TwContextMetricsTemplate.METRIC_UNIQUE_ENTRYPOINTS).gauge().value()).isEqualTo(1);
  }
}
