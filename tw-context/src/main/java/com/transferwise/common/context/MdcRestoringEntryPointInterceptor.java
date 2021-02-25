package com.transferwise.common.context;

import java.util.Map;
import java.util.function.Supplier;
import org.slf4j.MDC;

public class MdcRestoringEntryPointInterceptor implements TwContextExecutionInterceptor {

  @Override
  public boolean applies(TwContext context) {
    return context.isNewEntryPoint(true);
  }

  @Override
  public <T> T intercept(TwContext context, Supplier<T> supplier) {
    Map<String, String> contextMap = MDC.getCopyOfContextMap();
    try {
      return supplier.get();
    } finally {
      if (contextMap == null) {
        MDC.clear();
      } else {
        MDC.setContextMap(contextMap);
      }
    }
  }
}