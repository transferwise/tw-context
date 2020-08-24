package com.transferwise.common.context;

import java.time.Duration;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DefaultTimeoutCustomizer implements TimeoutCustomizer {

  private final TwContextProperties twContextProperties;

  @Override
  public Duration customize(String source, Duration timeout) {
    if (timeout == null) {
      return null;
    }

    if (twContextProperties.getTimeoutMultiplier() == null && twContextProperties.getTimeoutAdditive() == null) {
      return timeout;
    }

    Duration result;
    if (twContextProperties.getTimeoutMultiplier() != null) {
      result = Duration.ofMillis((long) (timeout.toMillis() * twContextProperties.getTimeoutMultiplier()));
    } else {
      result = timeout;
    }

    if (twContextProperties.getTimeoutAdditive() != null) {
      result = result.plus(twContextProperties.getTimeoutAdditive());
    }
    return result;
  }
}
