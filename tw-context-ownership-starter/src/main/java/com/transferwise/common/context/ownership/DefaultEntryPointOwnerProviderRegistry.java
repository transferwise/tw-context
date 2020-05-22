package com.transferwise.common.context.ownership;

import com.google.common.util.concurrent.RateLimiter;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class DefaultEntryPointOwnerProviderRegistry implements EntryPointOwnerProviderRegistry {

  @Autowired
  private List<EntryPointOwnerProvider> entryPointOwnerProviders;

  private RateLimiter errorLogRateLimiter = RateLimiter.create(2);

  @Override
  public String getOwner(String entryPointGroup, String entryPointName) {
    if (entryPointGroup == null || entryPointName == null) {
      return null;
    }
    for (EntryPointOwnerProvider entryPointOwnerProvider : entryPointOwnerProviders) {
      try {
        String owner = entryPointOwnerProvider.getOwner(entryPointGroup, entryPointName);
        if (owner != null) {
          return owner;
        }
      } catch (Throwable t) {
        if (errorLogRateLimiter.tryAcquire()) {
          log.error("Determining entryPoint's '" + entryPointGroup + ":" + entryPointName + "' owner failed. "
              + entryPointOwnerProvider.getClass().getName() + " should handle all it's own errors.", t);
        }
      }
    }
    return null;
  }
}
