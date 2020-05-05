package com.transferwise.common.context;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.util.function.Supplier;
import lombok.NonNull;

public class TwContextMetricsTemplate {

  public static final String METRIC_PREFIX = "tw.context";

  public static final String METRIC_UNIQUE_ENTRYPOINTS = METRIC_PREFIX + ".entrypoints.unique";
  public static final String METRIC_UNIQUE_ENTRYPOINTS_LIMIT = METRIC_PREFIX + ".entrypoints.unique.limit";
  public static final String METRIC_DEADLINE_EXTENDED = METRIC_PREFIX + ".deadline.extended";
  public static final String METRIC_CRITICALITY_CHANGED = METRIC_PREFIX + ".criticality.changed";
  public static final String METRIC_DEADLINE_EXCEEDED = METRIC_PREFIX + ".deadline.exceeded";

  public static final String TAG_EP_NAME = "epName";
  public static final String TAG_EP_GROUP = "epGroup";
  public static final String TAG_CRITICALITY = "criticality";
  public static final String TAG_SOURCE = "source";
  public static final String TAG_VALUE_UNKNOWN = "unknown";

  protected MeterRegistry meterRegistry;

  public TwContextMetricsTemplate(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  public void registerUniqueEntryPointsGauges(Supplier<Number> entriesCount, Supplier<Number> maxEntries) {
    Gauge.builder(METRIC_UNIQUE_ENTRYPOINTS, entriesCount).register(meterRegistry);
    Gauge.builder(METRIC_UNIQUE_ENTRYPOINTS_LIMIT, maxEntries).register(meterRegistry);
  }

  public void registerDeadlineExtending(@NonNull String group, @NonNull String name, Criticality criticality) {
    meterRegistry.counter(METRIC_DEADLINE_EXTENDED, tagsFor(group, name, criticality)).increment();
  }

  public void registerCriticalityChange(@NonNull String group, @NonNull String name, Criticality criticality) {
    meterRegistry.counter(METRIC_CRITICALITY_CHANGED, tagsFor(group, name, criticality)).increment();
  }

  public void registerDeadlineExceeded(@NonNull String group, @NonNull String name, Criticality criticality, String source) {
    meterRegistry.counter(METRIC_DEADLINE_EXCEEDED, tagsFor(group, name, criticality).and(TAG_SOURCE, source)).increment();
  }

  private Tags tagsFor(String group, String name, Criticality criticality) {
    return Tags.of(TAG_EP_GROUP, group, TAG_EP_NAME, name, TAG_CRITICALITY, criticality == null ? TAG_VALUE_UNKNOWN : criticality.name());
  }
}
