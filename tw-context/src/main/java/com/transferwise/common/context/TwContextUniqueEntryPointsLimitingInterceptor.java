package com.transferwise.common.context;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

@Slf4j
public class TwContextUniqueEntryPointsLimitingInterceptor implements
    TwContextExecutionInterceptor {

  private static final int DEFAULT_MAX_ENTRIES = 1000;

  private final int maxEntries;
  private final Map<Pair<String, String>, Boolean> uniqueEntryPointsMap = new ConcurrentHashMap<>();
  private int entriesCount;
  private final Lock lock = new ReentrantLock();

  public TwContextUniqueEntryPointsLimitingInterceptor(MeterRegistry meterRegistry) {
    this(meterRegistry, DEFAULT_MAX_ENTRIES);
  }

  public TwContextUniqueEntryPointsLimitingInterceptor(MeterRegistry meterRegistry,
      int maxEntries) {
    this.maxEntries = maxEntries;

    Gauge.builder("TwContext.UniqueEntryPoints.count", () -> entriesCount).register(meterRegistry);
    Gauge.builder("TwContext.UniqueEntryPoints.max", () -> maxEntries).register(meterRegistry);
  }

  @Override
  public boolean applies(TwContext context) {
    return context.getNew(TwContext.NAME_KEY) != null;
  }

  @Override
  public <T> T intercept(TwContext context, Supplier<T> supplier) {
    Pair<String, String> key = Pair.of(context.getGroup(), context.getName());

    if (uniqueEntryPointsMap.containsKey(key)) {
      return supplier.get();
    }

    lock.lock();
    try {
      boolean full = entriesCount == maxEntries;

      if (!full) {
        uniqueEntryPointsMap.put(key, Boolean.TRUE);
        if (++entriesCount == maxEntries) {
          warnAboutFull();
        }
      }

      if (full) {
        context.setGroup(TwContext.GROUP_GENERIC);
        context.setName(TwContext.NAME_GENERIC);
      }
      return supplier.get();
    } finally {
      lock.unlock();
    }
  }

  private void warnAboutFull() {
    log.error("{} unique entrypoints registered. Dumping out the currently registered entrypoints.",
        maxEntries);
    uniqueEntryPointsMap.forEach((k, v) -> log.info("Unique ep: '{}'-'{}'", k.getLeft(), k.getRight()));
  }
}
