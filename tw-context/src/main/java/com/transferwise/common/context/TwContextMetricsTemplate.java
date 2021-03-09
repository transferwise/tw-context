package com.transferwise.common.context;

import com.transferwise.common.baseutils.meters.cache.IMeterCache;
import com.transferwise.common.baseutils.meters.cache.MeterCache;
import com.transferwise.common.baseutils.meters.cache.TagsSet;
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
  public static final String TAG_EP_OWNER = "epOwner";
  public static final String TAG_CRITICALITY = "criticality";
  public static final String TAG_SOURCE = "source";
  public static final String TAG_VALUE_UNKNOWN = "unknown";

  protected IMeterCache meterCache;

  /**
   * Older tw-service-comms need this constructor.
   *
   * <p>Remove after 2020-04-01.
   */
  @Deprecated
  public TwContextMetricsTemplate(MeterRegistry meterRegistry) {
    this.meterCache = new MeterCache(meterRegistry);
  }

  public TwContextMetricsTemplate(IMeterCache meterCache) {
    this.meterCache = meterCache;
  }

  public void registerUniqueEntryPointsGauges(Supplier<Number> entriesCount, Supplier<Number> maxEntries) {
    Gauge.builder(METRIC_UNIQUE_ENTRYPOINTS, entriesCount).register(meterCache.getMeterRegistry());
    Gauge.builder(METRIC_UNIQUE_ENTRYPOINTS_LIMIT, maxEntries).register(meterCache.getMeterRegistry());
  }

  public void registerDeadlineExtending(@NonNull String group, @NonNull String name, Criticality criticality) {
    meterCache.counter(METRIC_DEADLINE_EXTENDED, tagsFor(group, name, null, criticality)).increment();
  }

  public void registerCriticalityChange(@NonNull String group, @NonNull String name, Criticality criticality) {
    meterCache.counter(METRIC_CRITICALITY_CHANGED, tagsFor(group, name, null, criticality)).increment();
  }

  public void registerDeadlineExceeded(@NonNull String group, @NonNull String name, @NonNull String owner, Criticality criticality, String source) {
    meterCache.counter(METRIC_DEADLINE_EXCEEDED, tagsFor(group, name, owner, criticality, source)).increment();
  }

  /**
   * Deprecated, but lets keep it around for 2 months, as older (tw-service-comms) libs may be using it.
   */
  @Deprecated
  public void registerDeadlineExceeded(@NonNull String group, @NonNull String name, Criticality criticality, String source) {
    meterCache.counter(METRIC_DEADLINE_EXCEEDED, tagsFor(group, name, null, criticality, source)).increment();
  }

  private TagsSet tagsFor(@NonNull String group, @NonNull String name, String owner, Criticality criticality) {
    return TagsSet.of(TAG_EP_GROUP, group, TAG_EP_NAME, name, TAG_EP_OWNER, owner == null ? "Generic" : owner, TAG_CRITICALITY,
        criticality == null ? TAG_VALUE_UNKNOWN : criticality.name());
  }

  private TagsSet tagsFor(@NonNull String group, @NonNull String name, String owner, Criticality criticality, String source) {
    return TagsSet.of(TAG_EP_GROUP, group, TAG_EP_NAME, name, TAG_EP_OWNER, owner == null ? "Generic" : owner, TAG_CRITICALITY,
        criticality == null ? TAG_VALUE_UNKNOWN : criticality.name(), TAG_SOURCE, source);
  }
}
