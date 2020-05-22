package com.transferwise.common.context;

import com.google.common.util.concurrent.RateLimiter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;

@Slf4j
public class TwContext {

  public static final String GROUP_KEY = "TwContextGroup";
  public static final String NAME_KEY = "TwContextName";
  public static final String OWNER_KEY = "TwContextOwner";
  public static final String GROUP_GENERIC = "Generic";
  public static final String NAME_GENERIC = "Generic";
  public static final String OWNER_GENERIC = "Generic";

  public static final String MDC_KEY_EP_NAME = "tw_entrypoint_name";
  public static final String MDC_KEY_EP_GROUP = "tw_entrypoint_group";
  public static final String MDC_KEY_EP_OWNER = "tw_entrypoint_owner";

  private static final ThreadLocal<TwContext> contextTl = new ThreadLocal<>();
  private static final Set<TwContextExecutionInterceptor> interceptors = new CopyOnWriteArraySet<>();
  private static final Set<TwContextAttributeChangeListener> attributeChangeListeners = new CopyOnWriteArraySet<>();
  private static final TwContext ROOT_CONTEXT = new TwContext(null, true);
  private static final RateLimiter throwableLoggingRateLimiter = RateLimiter.create(2);

  public static TwContext current() {
    TwContext twContext = contextTl.get();
    return twContext == null ? ROOT_CONTEXT : twContext;
  }

  public static void addExecutionInterceptor(@NonNull TwContextExecutionInterceptor interceptor) {
    interceptors.add(interceptor);
  }

  public static boolean removeExecutionInterceptor(@NonNull TwContextExecutionInterceptor interceptor) {
    return interceptors.remove(interceptor);
  }

  public static Set<TwContextExecutionInterceptor> getExecutionInterceptors() {
    return interceptors;
  }

  public static void addAttributeChangeListener(@NonNull TwContextAttributeChangeListener listener) {
    attributeChangeListeners.add(listener);
  }

  public static boolean removeAttributeChangeListener(TwContextAttributeChangeListener listener) {
    return attributeChangeListeners.remove(listener);
  }

  public static Set<TwContextAttributeChangeListener> getAttributeChangeListeners() {
    return attributeChangeListeners;
  }

  public static void putCurrentMdc(@NonNull String key, String value) {
    if (value == null) {
      MDC.remove(key);
    } else {
      MDC.put(key, value);
    }
    TwContext context = TwContext.current();
    if (!context.isRoot()) {
      context.mdc.put(key, value);
    }
  }

  @Getter
  private TwContext parent;

  private final Map<String, Object> attributes;
  private final Map<String, Object> newAttributes;

  private Map<String, String> preAttachMdc;
  private Map<String, String> mdc;

  @Getter
  private boolean root;

  private Function<Supplier<?>, Object> executionWrapper;

  public TwContext(@NonNull TwContext parent) {
    this(parent, false);
  }

  private TwContext(TwContext parent, boolean root) {
    this.parent = parent;
    this.root = root;
    attributes = parent == null ? new HashMap<>() : new HashMap<>(parent.attributes);
    newAttributes = new HashMap<>();
    mdc = new HashMap<>();
  }

  public TwContext createSubContext() {
    return new TwContext(this);
  }

  public TwContext asEntryPoint(@NonNull String group, @NonNull String name) {
    if (StringUtils.trimToNull(group) == null) {
      throw new IllegalStateException("Empty group provided.");
    }
    if (StringUtils.trimToNull(name) == null) {
      throw new IllegalStateException("Empty name provided.");
    }

    setName(group, name);
    return this;
  }

  public <T> TwContext withExecutionWrapper(Function<Supplier<?>, Object> wrapper) {
    this.executionWrapper = wrapper;
    return this;
  }

  public boolean isNewEntryPoint() {
    return getNew(NAME_KEY) != null;
  }

  public void putMdc(@NonNull String key, String value) {
    mdc.put(key, value);
  }

  public TwContext attach() {
    final TwContext current = contextTl.get();
    contextTl.set(this);

    preAttachMdc = MDC.getCopyOfContextMap();
    mdc.entrySet().forEach(e -> {
      String key = e.getKey();
      String value = e.getValue();
      if (value == null) {
        MDC.remove(key);
      } else {
        MDC.put(key, value);
      }
    });
    return current;
  }

  public void detach(TwContext previous) {
    if (previous == null || previous.isRoot()) {
      contextTl.remove();
    } else {
      contextTl.set(previous);
    }

    mdc.keySet().forEach(key -> {
      String prevValue = preAttachMdc == null ? null : preAttachMdc.get(key);
      if (prevValue == null) {
        MDC.remove(key);
      } else {
        MDC.put(key, prevValue);
      }
    });
  }

  @SuppressWarnings("unchecked")
  public <T> T get(String key) {
    return (T) attributes.get(key);
  }

  @SuppressWarnings("unchecked")
  public <T> T getNew(String key) {
    return (T) newAttributes.get(key);
  }

  @SuppressWarnings("UnusedReturnValue")
  public TwContext put(String key, Object newValue) {
    if (root) {
      throw new IllegalStateException("You can not add values into root context.");
    }
    Object oldValue = attributes.get(key);

    if (!Objects.equals(oldValue, newValue)) {
      if (newValue == null) {
        attributes.remove(key);
        newAttributes.remove(key);
      } else {
        attributes.put(key, newValue);
        newAttributes.put(key, newValue);
      }
      fireAttributeChangeEvent(key, oldValue, newValue);
    }

    return this;
  }

  @SuppressWarnings("UnusedReturnValue")
  public TwContext setName(@NonNull String group, @NonNull String name) {
    put(GROUP_KEY, group);
    put(NAME_KEY, name);

    putMdc(MDC_KEY_EP_GROUP, group);
    putMdc(MDC_KEY_EP_NAME, name);

    return this;
  }

  public TwContext setOwner(@NonNull String owner) {
    put(OWNER_KEY, owner);
    putMdc(MDC_KEY_EP_OWNER, owner);

    return this;
  }

  public String getName() {
    String name = get(NAME_KEY);
    return name == null ? NAME_GENERIC : name;
  }

  public String getGroup() {
    String group = get(GROUP_KEY);
    return group == null ? GROUP_GENERIC : group;
  }

  public String getOwner() {
    String owner = get(OWNER_KEY);
    return owner == null ? OWNER_GENERIC : owner;
  }

  @SuppressWarnings("unchecked")
  private <T> Supplier<T> getWrappedSupplier(Supplier<T> supplier) {
    if (executionWrapper == null) {
      return supplier;
    }
    return () -> (T) executionWrapper.apply(supplier);
  }

  // We are copy pasting code to avoid additional stack frames.

  // Sadly we need another method for Groovy, as it can not distinguish always which execute to use.
  public <T> T call(@NonNull Supplier<T> supplier) {
    TwContext previous = attach();
    try {
      return executeWithInterceptors(getWrappedSupplier(supplier));
    } finally {
      detach(previous);
    }
  }

  public <T> T execute(@NonNull Supplier<T> supplier) {
    TwContext previous = attach();
    try {
      return executeWithInterceptors(getWrappedSupplier(supplier));
    } finally {
      detach(previous);
    }
  }

  public void execute(@NonNull Runnable runnable) {
    TwContext previous = attach();
    try {
      executeWithInterceptors(getWrappedSupplier(() -> {
        runnable.run();
        return null;
      }));
    } finally {
      detach(previous);
    }
  }

  private <T> T executeWithInterceptors(Supplier<T> supplier) {
    List<TwContextExecutionInterceptor> applicableInterceptors = new ArrayList<>();
    for (TwContextExecutionInterceptor interceptor : TwContext.interceptors) {
      if (interceptor.applies(this)) {
        applicableInterceptors.add(interceptor);
      }
    }
    return executeWithInterceptors(supplier, applicableInterceptors, 0);
  }

  private <T> T executeWithInterceptors(Supplier<T> supplier, List<TwContextExecutionInterceptor> interceptors, int interceptorIdx) {
    if (interceptorIdx >= interceptors.size()) {
      return supplier.get();
    }
    return interceptors.get(interceptorIdx)
        .intercept(this, () -> executeWithInterceptors(supplier, interceptors, interceptorIdx + 1));
  }

  private void fireAttributeChangeEvent(String key, Object oldValue, Object newValue) {
    if (attributeChangeListeners != null) {
      try {
        attributeChangeListeners.forEach((l) -> l.attributeChanged(this, key, oldValue, newValue));
      } catch (Throwable t) {
        // This is just a safety net, every listener needs to be bullet-proof by themselves.
        if (throwableLoggingRateLimiter.tryAcquire()) {
          // Don't log value, could be PII.
          log.error("Attribute change listener failed for key '" + key + "'.", t);
        }
      }
    }
  }
}
