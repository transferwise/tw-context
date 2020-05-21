package com.transferwise.common.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.transferwise.common.baseutils.clock.TestClock;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
public class ApplicationIntTest {

  @Autowired
  private UnitOfWorkManager unitOfWorkManager;
  @Autowired
  private MeterRegistry meterRegistry;

  private TestClock testClock;

  @BeforeEach
  void setup() {
    testClock = TestClock.createAndRegister();
  }

  @AfterEach
  void cleanup() {
    TestClock.reset();
    meterRegistry.clear();
  }

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
    boolean deadlineExceeded = false;
    try {
      unitOfWorkManager.createEntryPoint("TestGroup", "TestName").deadline(Instant.now().minusSeconds(100)).toContext()
          .execute(() -> unitOfWorkManager.checkDeadLine("MyTest"));
    } catch (DeadlineExceededException e) {
      deadlineExceeded = true;
      log.warn(e.getMessage());
    }

    assertThat(deadlineExceeded).isTrue();

    assertThat(meterRegistry.get(TwContextMetricsTemplate.METRIC_DEADLINE_EXCEEDED).counter().count()).isEqualTo(1);
  }

  @Test
  void deadlineCanBeExpandedForCertainSpecialCases() {
    unitOfWorkManager.createUnitOfWork().deadline(testClock.instant().plusSeconds(10)).toContext().execute(() -> {
      assertThat(unitOfWorkManager.getUnitOfWork().getDeadline()).isEqualTo(testClock.instant().plusSeconds(10));
      unitOfWorkManager.createUnitOfWork().deadline(testClock.instant().plusSeconds(30)).toContext().execute(() -> {
        assertThat(unitOfWorkManager.getUnitOfWork().getDeadline()).isEqualTo(testClock.instant().plusSeconds(30));
      });
    });

    assertThat(meterRegistry.get(TwContextMetricsTemplate.METRIC_DEADLINE_EXTENDED).counter().count()).isEqualTo(1);
  }


  @Test
  void deadlineExceededExceptionHasExpectedMessage() {
    Instant start = OffsetDateTime.parse("2020-05-20T12:37:21.532Z").toInstant();
    Instant deadline = OffsetDateTime.parse("2020-05-20T12:37:23.531Z").toInstant();

    testClock.set(start.plusSeconds(5));
    assertThat(new DeadlineExceededException(deadline).getMessage()).isEqualTo("Deadline exceeded 3 seconds 1 milliseconds ago.");
    assertThat(new DeadlineExceededException(deadline, start).getMessage())
        .isEqualTo("Deadline exceeded 3 seconds 1 milliseconds ago. Time taken in current unit of work was 5 seconds.");

    testClock.set(start);
    assertThatThrownBy(() -> unitOfWorkManager.createUnitOfWork().deadline(deadline).toContext().execute(() -> {
      testClock.tick(Duration.ofSeconds(3));
      unitOfWorkManager.checkDeadLine("Test");
    })).isInstanceOf(DeadlineExceededException.class)
        .hasMessage("Deadline exceeded 1 seconds 1 milliseconds ago. Time taken in current unit of work was 3 seconds.");
  }
}
