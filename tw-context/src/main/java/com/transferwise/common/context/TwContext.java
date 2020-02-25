package com.transferwise.common.context;

import com.newrelic.api.agent.NewRelic;
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

public class TwContext {

  public static final String GROUP_KEY = "TwContextGroup";
  public static final String NAME_KEY = "TwContextName";
  public static final String GROUP_GENERIC = "Generic";
  public static final String NAME_GENERIC = "Generic";

  private static final ThreadLocal<TwContext> contextTl = new ThreadLocal<>();
  private static final List<TwContextExecutionInterceptor> interceptors =
      new CopyOnWriteArrayList<>();
  private static final TwContext ROOT_CONTEXT = new TwContext(null, true);

  public static TwContext current() {
    TwContext twContext = contextTl.get();
    return twContext == null ? ROOT_CONTEXT : twContext;
  }

  public static void addExecutionInterceptor(TwContextExecutionInterceptor interceptor) {
    interceptors.add(interceptor);
  }

  public static List<TwContextExecutionInterceptor> getExecutionInterceptors() {
    return interceptors;
  }

  @Getter
  private TwContext parent;

  private final Map<String, Object> attributes;
  private final Map<String, Object> newAttributes;

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

  public TwContext attach() {
    TwContext current = contextTl.get();
    contextTl.set(this);
    return current;
  }

  public void detach(TwContext previous) {
    if (previous == null || previous.isRoot()) {
      contextTl.remove();
    } else {
      contextTl.set(previous);
    }
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
    NewRelic.setTransactionName(group, name);
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
  public <T> T call(Supplier<T> supplier) {
    TwContext previous = attach();
    try {
      return executeWithInterceptors(getWrappedSupplier(supplier));
    } finally {
      detach(previous);
    }
  }

  public <T> T execute(Supplier<T> supplier) {
    TwContext previous = attach();
    try {
      return executeWithInterceptors(getWrappedSupplier(supplier));
    } finally {
      detach(previous);
    }
  }

  public void execute(Runnable runnable) {
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
