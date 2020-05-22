package com.transferwise.common.context.ownership;

import com.transferwise.common.context.TwContext;
import com.transferwise.common.context.TwContextAttributeChangeListener;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class EntryPointOwnerAttributesChangeListener implements TwContextAttributeChangeListener {

  @Autowired
  private EntryPointOwnerProviderRegistry entryPointOwnerProviderRegistry;
  @Autowired
  private TwContextOwnershipProperties properties;

  private final Map<Pair<String, String>, Boolean> defaultOwners = new LinkedHashMap<Pair<String, String>, Boolean>() {
    static final long serialVersionUID = 1L;

    @Override
    public boolean removeEldestEntry(Map.Entry<Pair<String, String>, Boolean> eldest) {
      // Let's not OOM on misconfigured apps.
      return size() > 10000;
    }
  };

  private final Lock defaultOwnersLock = new ReentrantLock();

  @Override
  public void attributeChanged(TwContext context, String key, Object oldValue, Object newValue) {
    if (TwContext.NAME_KEY.equals(key)) {
      String epGroup = context.getGroup();
      String epName = context.getName();
      String owner = entryPointOwnerProviderRegistry.getOwner(epGroup, epName);
      if (owner == null) {
        if (properties.getDefaultOwner() != null) {
          // If owner was already set programmatically by `TwContext.setOwner`, we don't mess with that.
          if (context.getOwner() != null) {
            warnAboutDefaultOwnership(epGroup, epName);
            context.setOwner(properties.getDefaultOwner());
          }
        }
      } else {
        context.setOwner(owner);
      }
    }
  }

  private void warnAboutDefaultOwnership(String epGroup, String epName) {
    if (properties.isWarnAboutEntryPointsWithoutOwner() && (!TwContext.GROUP_GENERIC.equals(epGroup) || !TwContext.NAME_GENERIC
        .equals(epName))) {
      Pair<String, String> entrypoint = Pair.of(epGroup, epName);
      boolean shouldLog = false;
      defaultOwnersLock.lock();
      try {
        if (!defaultOwners.containsKey(entrypoint)) {
          shouldLog = true;
          defaultOwners.put(entrypoint, Boolean.TRUE);
        }
      } finally {
        defaultOwnersLock.unlock();
      }
      if (shouldLog) {
        log.warn("Entrypoint '" + epGroup + ":" + epName + "' does not have an owner.");
      }
    }
  }

  protected void clearDefaultOwners() {
    defaultOwnersLock.lock();
    try {
      defaultOwners.clear();
    } finally {
      defaultOwnersLock.unlock();
    }
  }
}
