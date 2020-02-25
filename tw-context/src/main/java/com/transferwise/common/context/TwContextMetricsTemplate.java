package com.transferwise.common.context;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.function.Supplier;

public class TwContextMetricsTemplate {

  public static final String TAG_EP_NAME = "epName";
  public static final String TAG_EP_GROUP = "epGroup";
  public static final String TAG_CRITICALITY = "criticality";
  public static final String TAG_SOURCE = "source";

  protected MeterRegistry meterRegistry;

  public TwContextMetricsTemplate(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  public void registerUniqueEntryPointsCounts(Supplier<Number> entriesCount, Supplier<Number> maxEntries) {
    Gauge.builder("TwContext_UniqueEntryPoints_count", entriesCount).register(meterRegistry);
    Gauge.builder("TwContext_UniqueEntryPoints_limit", maxEntries).register(meterRegistry);
  }

  public void registerDeadlineReduction(String group, String name) {
    meterRegistry.counter("TwContext_UnitOfWork_DeadlineReduced", TAG_EP_GROUP, group, TAG_EP_NAME, name).increment();
  }

  public void registerCriticalityChange(String group, String name) {
    meterRegistry.counter("TwContext_UnitOfWork_CriticalityChanged", TAG_EP_GROUP, group, TAG_EP_NAME, name).increment();
  }

  public void registerDeadlineExceeded(String group, String name, String source) {
    meterRegistry.counter("TwServiceComms_DeadlinePassed", TAG_EP_GROUP, group, TAG_EP_NAME, name, TAG_SOURCE, source);
  }
}
