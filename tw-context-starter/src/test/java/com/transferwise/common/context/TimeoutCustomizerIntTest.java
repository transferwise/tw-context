package com.transferwise.common.context;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("timeout-int-test")
@TestMethodOrder(OrderAnnotation.class)
@Slf4j
public class TimeoutCustomizerIntTest {

  @Autowired
  private TimeoutCustomizer timeoutCustomizer;

  @Autowired
  private TwContextProperties twContextProperties;

  @Test
  public void testThatTimeoutIsCorrectlyCustomized() {
    assertThat(timeoutCustomizer.customize("test", null)).isNull();
    assertThat(timeoutCustomizer.customize("test", Duration.ofSeconds(2))).isEqualByComparingTo(Duration.ofSeconds(5));
  }

  @Test
  public void testThatEmptyPropertiesWork() {
    Duration additive = twContextProperties.getTimeoutAdditive();
    Double multiplier = twContextProperties.getTimeoutMultiplier();

    try {
      twContextProperties.setTimeoutAdditive(null);
      assertThat(timeoutCustomizer.customize("test", Duration.ofSeconds(2))).isEqualByComparingTo(Duration.ofSeconds(3));
      twContextProperties.setTimeoutMultiplier(null);
      assertThat(timeoutCustomizer.customize("test", Duration.ofSeconds(2))).isEqualByComparingTo(Duration.ofSeconds(2));
      twContextProperties.setTimeoutAdditive(Duration.ofSeconds(2));
      assertThat(timeoutCustomizer.customize("test", Duration.ofSeconds(2))).isEqualByComparingTo(Duration.ofSeconds(4));
    } finally {
      twContextProperties.setTimeoutAdditive(additive);
      twContextProperties.setTimeoutMultiplier(multiplier);
    }
  }
}
