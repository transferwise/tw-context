package com.transferwise.common.context;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

public class DurationFormatUtilsTest {

  @Test
  void formattingWorks() {
    assertThat(DurationFormatUtils.formatDuration(Duration.ofDays(10))).as("Hour is largest unit").isEqualTo("240 hours");
    assertThat(DurationFormatUtils.formatDuration(Duration.ofMillis(1500))).isEqualTo("1 seconds 500 milliseconds");
    assertThat(DurationFormatUtils.formatDuration(Duration.ofMinutes(61))).isEqualTo("1 hours 1 minutes");
    assertThat(DurationFormatUtils.formatDuration(Duration.ofNanos(100000))).as("Millis is smallest unit").isEqualTo("0 milliseconds");
  }
}
