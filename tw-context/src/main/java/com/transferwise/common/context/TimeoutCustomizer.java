package com.transferwise.common.context;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public interface TimeoutCustomizer {

  Duration customize(String source, Duration timeout);

  long customize(String source, long timeout, TimeUnit unit);
}
