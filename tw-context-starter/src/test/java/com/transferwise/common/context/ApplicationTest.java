package com.transferwise.common.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.transferwise.common.baseutils.clock.TestClock;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = TestApplication.class)
@TestMethodOrder(OrderAnnotation.class)
@Slf4j
public class ApplicationTest {

  @Autowired
  private UnitOfWorkManager unitOfWorkManager;
  @Autowired
  private MeterRegistry meterRegistry;

  @Test
  @Order(0)
  void applicationIsConfigured() {
    assertEquals("123", unitOfWorkManager.createEntryPoint("A", "B").toContext().execute(() -> "123"));

    assertTrue(TwContext.getExecutionInterceptors().stream()
        .anyMatch(i -> i instanceof TwContextUniqueEntryPointsLimitingInterceptor));

    AtomicInteger genericCount = new AtomicInteger();
    for (int i = 0; i < 2000; i++) {
      unitOfWorkManager.createEntryPoint(String.valueOf(i), String.valueOf(i)).toContext().execute(() -> {
        String group = TwContext.current().getGroup();
        String name = TwContext.current().getName();

        if ("Generic".equals(group) && "Generic".equals(name)) {
          genericCount.incrementAndGet();
        }
      });
    }

    assertEquals(1001, genericCount.get());
  }

  @Test
  @Order(1)
  void mdcValuesAreCorrectlySet() {
    TestClock clock = TestClock.createAndRegister();

    String testGroup = "TestGroup";
    String testName = "TestName";

    unitOfWorkManager.createEntryPoint(testGroup, testName)
        .deadline(clock.instant()).criticality(Criticality.CRITICAL_PLUS).toContext()
        .execute(() -> {
          assertThat(MDC.get(TwContext.MDC_KEY_EP_NAME)).isEqualTo(testName);
          assertThat(MDC.get(TwContext.MDC_KEY_EP_GROUP)).isEqualTo(testGroup);
        });

    assertThat(MDC.get(TwContext.MDC_KEY_EP_NAME)).isNull();
    assertThat(MDC.get(TwContext.MDC_KEY_EP_GROUP)).isNull();
  }

  @Test
  void deadlineExceededIsCorrectlyCounted() {
    meterRegistry.getMeters().forEach(meter -> {
      if (meter.getId().equals(TwContextMetricsTemplate.METRIC_DEADLINE_EXCEEDED)) {
        meterRegistry.remove(meter);
      }
    });
    boolean deadlineExceeded = false;
    try {
      unitOfWorkManager.createEntryPoint("TestGroup", "TestName").deadline(Instant.now().minusSeconds(100)).toContext().execute(() -> {
        unitOfWorkManager.checkDeadLine("MyTest");
      });

    } catch (DeadlineExceededException e) {
      deadlineExceeded = true;
      log.warn(e.getMessage());
    }

    assertThat(deadlineExceeded).isTrue();

    assertThat(meterRegistry.get(TwContextMetricsTemplate.METRIC_DEADLINE_EXCEEDED).counter().count()).isEqualTo(1);
  }
}
