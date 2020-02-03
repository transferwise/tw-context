package com.transferwise.common.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
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
  private static final int MAX_DEPTH = 1000;

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

    put(GROUP_KEY, group);
    put(NAME_KEY, name);
    return this;
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

  public void replaceValueDeep(String key, Object search, Object value) {
    TwContext context = this;
    int i = 0;
    while (context != null) {
      if (Objects.equals(context.get(key), search)) {
        context.attributes.put(key, value);
      }
      if (Objects.equals(context.getNew(key), search)) {
        context.newAttributes.put(key, value);
      }
      context = context.getParent();
      if (i++ > MAX_DEPTH) {
        throw new IllegalStateException(
            "Indefinite loop detected. Most likely the parent-chain is circular.");
      }
    }
  }

  @SuppressWarnings("UnusedReturnValue")
  public TwContext setName(@NonNull String name) {
    put(NAME_KEY, name);
    if (getGroup() == null) {
      setGroup(GROUP_GENERIC);
    }
    return this;
  }

  @SuppressWarnings("UnusedReturnValue")
  public TwContext setGroup(@NonNull String group) {
    put(GROUP_KEY, group);
    return this;
  }

  public String getName() {
    return get(NAME_KEY);
  }

  public String getGroup() {
    return get(GROUP_KEY);
  }

  public <T> T execute(Supplier<T> supplier) {
    TwContext previous = attach();
    try {
      return executeWithInterceptors(supplier);
    } finally {
      detach(previous);
    }
  }

  public void execute(Runnable runnable) {
    execute(() -> {
      runnable.run();
      return null;
    });
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

  private <T> T executeWithInterceptors(Supplier<T> supplier,
      List<TwContextExecutionInterceptor> interceptors, int interceptorIdx) {
    if (interceptorIdx >= interceptors.size()) {
      return supplier.get();
    }
    return interceptors.get(interceptorIdx)
        .intercept(this, () -> executeWithInterceptors(supplier, interceptors, interceptorIdx + 1));
  }
}
