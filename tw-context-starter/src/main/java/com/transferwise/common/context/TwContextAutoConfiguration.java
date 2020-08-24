package com.transferwise.common.context;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TwContextAutoConfiguration {

  @Bean
  public UnitOfWorkManager twContextUnitOfWorkManager(MeterRegistry meterRegistry) {
    return new DefaultUnitOfWorkManager(meterRegistry);
  }

  @Bean
  public TwContextUniqueEntryPointsLimitingInterceptor twContextUniqueEpLimitingInterceptor(MeterRegistry meterRegistry) {
    TwContextUniqueEntryPointsLimitingInterceptor interceptor =
        new TwContextUniqueEntryPointsLimitingInterceptor(meterRegistry);
    TwContext.addExecutionInterceptor(interceptor);
    return interceptor;
  }

  @Bean
  @ConditionalOnMissingBean(TwContextProperties.class)
  @ConfigurationProperties(prefix = "tw-context.core", ignoreUnknownFields = false)
  public TwContextProperties twContextProperties() {
    return new TwContextProperties();
  }

  @Bean
  @ConditionalOnMissingBean(TimeoutCustomizer.class)
  public DefaultTimeoutCustomizer twContextTimeoutCustomizer(TwContextProperties twContextProperties) {
    return new DefaultTimeoutCustomizer(twContextProperties);
  }
}
