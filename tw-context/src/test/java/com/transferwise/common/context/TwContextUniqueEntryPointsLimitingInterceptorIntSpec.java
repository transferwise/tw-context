package com.transferwise.common.context;

import static org.assertj.core.api.Assertions.assertThat;

import com.transferwise.common.baseutils.ExceptionUtils;
import com.transferwise.common.baseutils.clock.ClockHolder;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
public class TwContextUniqueEntryPointsLimitingInterceptorIntSpec {

  // Was a production bug.
  @Test
  @SneakyThrows
  void testThatThreadsCanNotBlockEachOther() {
    final int sleepTimeMs = 5000;

    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    TwContext.addExecutionInterceptor(new TwContextUniqueEntryPointsLimitingInterceptor(meterRegistry, 1));

    final long startTimeMs = ClockHolder.getClock().millis();

    CountDownLatch countDownLatch = new CountDownLatch(1);
    CountDownLatch countDownLatch1 = new CountDownLatch(1);

    Thread t0 = new Thread(() -> {
      TwContext.current().createSubContext().asEntryPoint("A", "A").execute(() -> {
        ExceptionUtils.doUnchecked(() -> {
          countDownLatch.countDown();
          countDownLatch1.await(sleepTimeMs, TimeUnit.MILLISECONDS);
        });
      });
    });

    Thread t1 = new Thread(() -> {
      TwContext.current().createSubContext().asEntryPoint("B", "B").execute(() -> {
        log.info("Hello World!");
        countDownLatch1.countDown();
      });
    });

    t0.start();
    countDownLatch.await(sleepTimeMs, TimeUnit.MILLISECONDS);
    t1.start();

    t1.join();
    t0.join();

    assertThat(ClockHolder.getClock().millis() - startTimeMs).isLessThan(sleepTimeMs / 2);
  }
}
