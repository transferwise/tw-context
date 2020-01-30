package com.transferwise.common.context;

import java.util.function.Supplier;

public interface TwContextInterceptor {

  boolean applies(TwContext context);

  <T> T intercept(Supplier<T> supplier);
}
