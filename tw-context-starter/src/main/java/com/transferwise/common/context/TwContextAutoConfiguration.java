package com.transferwise.common.context;

import com.transferwise.common.baseutils.meters.cache.IMeterCache;
import com.transferwise.common.baseutils.meters.cache.MeterCache;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TwContextAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public UnitOfWorkManager twContextUnitOfWorkManager(MeterRegistry meterRegistry, IMeterCache meterCache) {
    return new DefaultUnitOfWorkManager(meterRegistry, meterCache);
  }

  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnProperty(value = "tw-context.core.mdcRestoringEntryPointInterceptorEnabled", havingValue = "true", matchIfMissing = true)
  public MdcRestoringEntryPointInterceptor twContextMdcRestoringEntryPointInterceptor() {
    MdcRestoringEntryPointInterceptor interceptor = new MdcRestoringEntryPointInterceptor();
    TwContext.addExecutionInterceptor(interceptor);
    return interceptor;
  }

  @Bean
  @ConditionalOnMissingBean
  public TwContextUniqueEntryPointsLimitingInterceptor twContextUniqueEpLimitingInterceptor(MeterRegistry meterRegistry, IMeterCache meterCache) {
    TwContextUniqueEntryPointsLimitingInterceptor interceptor =
        new TwContextUniqueEntryPointsLimitingInterceptor(meterRegistry, meterCache);
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

  @Bean
  @ConditionalOnMissingBean(IMeterCache.class)
  public IMeterCache twDefaultMeterCache(MeterRegistry meterRegistry) {
    return new MeterCache(meterRegistry);
  }

}
