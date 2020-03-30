package com.transferwise.common.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;

public class TwContext {

  public static final String GROUP_KEY = "TwContextGroup";
  public static final String NAME_KEY = "TwContextName";
  public static final String GROUP_GENERIC = "Generic";
  public static final String NAME_GENERIC = "Generic";

  public static final String MDC_KEY_EP_NAME = "tw_entrypoint_name";
  public static final String MDC_KEY_EP_GROUP = "tw_entrypoint_group";

  private static final ThreadLocal<TwContext> contextTl = new ThreadLocal<>();
  private static final List<TwContextExecutionInterceptor> interceptors =
      new CopyOnWriteArrayList<>();
  private static final TwContext ROOT_CONTEXT = new TwContext(null, true);

  public static TwContext current() {
    TwContext twContext = contextTl.get();
    return twContext == null ? ROOT_CONTEXT : twContext;
  }

  public static void addExecutionInterceptor(@NonNull TwContextExecutionInterceptor interceptor) {
    interceptors.add(interceptor);
  }

  public static List<TwContextExecutionInterceptor> getExecutionInterceptors() {
    return interceptors;
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
  public TwContext put(String key, Object value) {
    if (root) {
      throw new IllegalStateException("You can not add values into root context.");
    }
    if (value == null) {
      attributes.remove(key);
      newAttributes.remove(key);
    } else {
      if (!Objects.equals(attributes.get(key), value)) {
        attributes.put(key, value);
        newAttributes.put(key, value);
      }
    }
    return this;
  }

  @SuppressWarnings("UnusedReturnValue")
  public TwContext setName(@NonNull String group, @NonNull String name) {
    put(NAME_KEY, name);
    put(GROUP_KEY, group);

    putMdc(MDC_KEY_EP_GROUP, group);
    putMdc(MDC_KEY_EP_NAME, name);

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
}
