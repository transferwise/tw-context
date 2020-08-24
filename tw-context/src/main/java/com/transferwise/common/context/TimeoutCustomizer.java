package com.transferwise.common.context;

import java.time.Duration;

public interface TimeoutCustomizer {
  Duration customize(String source, Duration timeout);
}
