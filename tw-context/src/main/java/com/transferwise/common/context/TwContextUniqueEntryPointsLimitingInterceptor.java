package com.transferwise.common.context;

import com.transferwise.common.baseutils.meters.cache.IMeterCache;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

@Slf4j
public class TwContextUniqueEntryPointsLimitingInterceptor implements TwContextExecutionInterceptor {

  private static final int DEFAULT_MAX_ENTRIES = 1000;

  private final int maxEntries;
  private final Map<Pair<String, String>, Boolean> uniqueEntryPointsMap = new ConcurrentHashMap<>();
  private int entriesCount;
  private final Lock lock = new ReentrantLock();

  public TwContextUniqueEntryPointsLimitingInterceptor(IMeterCache meterCache) {
    this(meterCache, DEFAULT_MAX_ENTRIES);
  }

  public TwContextUniqueEntryPointsLimitingInterceptor(IMeterCache meterCache, int maxEntries) {
    this.maxEntries = maxEntries;

    TwContextMetricsTemplate metricsTemplate = new TwContextMetricsTemplate(meterCache);
    metricsTemplate.registerUniqueEntryPointsGauges(() -> entriesCount, () -> maxEntries);
  }

  @Override
  public boolean applies(TwContext context) {
    return context.getNew(TwContext.NAME_KEY) != null;
  }

  @Override
  public <T> T intercept(TwContext context, Supplier<T> supplier) {
    String group = context.getGroup();
    String name = context.getName();

    Pair<String, String> key = Pair.of(group, name);

    if (uniqueEntryPointsMap.containsKey(key)) {
      return supplier.get();
    }

    boolean full;
    boolean warnAboutFull = false;
    lock.lock();
    try {
      full = entriesCount == maxEntries;

      if (!full) {
        Boolean result = uniqueEntryPointsMap.put(key, Boolean.TRUE);

        if (result == null && ++entriesCount == maxEntries) {
          warnAboutFull = true;
        }
      }
    } finally {
      lock.unlock();
    }

    if (full) {
      context.setName(TwContext.GROUP_GENERIC, TwContext.NAME_GENERIC);
    }
    if (warnAboutFull) {
      warnAboutFull();
    }

    return supplier.get();
  }

  private void warnAboutFull() {
    log.error("{} unique entrypoints registered. Dumping out the currently registered entrypoints.",
        maxEntries);
    uniqueEntryPointsMap.forEach((k, v) -> log.info("Unique ep: '{}'-'{}'", k.getLeft(), k.getRight()));
  }
}
