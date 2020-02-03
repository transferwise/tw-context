package com.transferwise.common.context;

import java.util.function.Supplier;

public interface TwContextExecutionInterceptor {

  boolean applies(TwContext context);

  <T> T intercept(TwContext context, Supplier<T> supplier);
}
