package com.transferwise.common.context;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Useful for unit tests.
 */
public class NoOpTimeoutCustomizer implements TimeoutCustomizer {

  @Override
  public Duration customize(String source, Duration timeout) {
    return timeout;
  }

  @Override
  public long customize(String source, long timeout, TimeUnit unit) {
    return timeout;
  }
}
