package com.transferwise.common.context;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DefaultTimeoutCustomizer implements TimeoutCustomizer {

  private final TwContextProperties twContextProperties;

  @Override
  public Duration customize(String source, Duration timeout) {
    if (timeout == null) {
      return null;
    }

    if (areAdjustmentsDisabled()) {
      return timeout;
    }

    Duration result;
    if (twContextProperties.getTimeoutMultiplier() != null) {
      result = Duration.ofNanos((long) (timeout.toNanos() * twContextProperties.getTimeoutMultiplier()));
    } else {
      result = timeout;
    }

    if (twContextProperties.getTimeoutAdditive() != null) {
      result = result.plus(twContextProperties.getTimeoutAdditive());
    }
    return result;
  }

  @Override
  public long customize(String source, long timeout, TimeUnit unit) {
    if (unit == null) {
      return timeout;
    }
    if (areAdjustmentsDisabled()) {
      return timeout;
    }
    double result;
    if (twContextProperties.getTimeoutMultiplier() != null) {
      result = timeout * twContextProperties.getTimeoutMultiplier();
    } else {
      result = timeout;
    }

    if (twContextProperties.getTimeoutAdditive() != null) {
      // This can be done more efficiently and elegantly when move the target level to java 11.
      result += unit.convert(twContextProperties.getTimeoutAdditive().toNanos(), TimeUnit.NANOSECONDS);
    }
    return (long) result;
  }

  private boolean areAdjustmentsDisabled() {
    return (twContextProperties.getTimeoutMultiplier() == null || Math.abs(twContextProperties.getTimeoutMultiplier() - 1) < 0.00001d)
        && (twContextProperties.getTimeoutAdditive() == null || twContextProperties.getTimeoutAdditive().isZero());
  }
}
