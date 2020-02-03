package com.transferwise.common.context;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TwContextAutoConfiguration {

  @Bean
  public UnitOfWorkFactory twContextUnitOfWorkFactory(MeterRegistry meterRegistry) {
    return new DefaultUnitOfWorkFactory(meterRegistry);
  }

  @Bean
  public TwContextUniqueEntryPointsLimitingInterceptor twContextUniqueEpLimitingInterceptor(
      MeterRegistry meterRegistry) {
    TwContextUniqueEntryPointsLimitingInterceptor interceptor =
        new TwContextUniqueEntryPointsLimitingInterceptor(meterRegistry);
    TwContext.addExecutionInterceptor(interceptor);
    return interceptor;
  }
}
