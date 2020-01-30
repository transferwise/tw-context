package com.transferwise.common.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;

public class TwContext {

  public static final String GROUP_KEY = "TwContextGroup";
  public static final String NAME_KEY = "TwContextName";
  public static final String GROUP_GENERIC = "Generic";

  private static final ThreadLocal<TwContext> contextTl = new ThreadLocal<>();
  private static final List<TwContextInterceptor> interceptors = new ArrayList<>();
  private static final TwContext ROOT = new TwContext(null);
  private static final int MAX_DEPTH = 1000;

  public static TwContext current() {
    TwContext context = contextTl.get();
    if (context == null) {
      return ROOT;
    }
    return context;
  }

  public static TwContext newSubContext() {
    return new TwContext();
  }

  public static void addInterceptor(TwContextInterceptor interceptor) {
    interceptors.add(interceptor);
  }

  @Getter
  private TwContext parent;
  @Getter
  private Map<String, Object> attributes;
  @Getter
  private Map<String, Object> currentAttributes;

  public TwContext() {
    this(TwContext.current());
  }

  public TwContext(TwContext parent) {
    this.parent = parent;
    if (parent == null) {
      attributes = new HashMap<>();
      currentAttributes = new HashMap<>();
      setGroup(GROUP_GENERIC);
    } else {
      attributes = new HashMap<>(parent.attributes);
      currentAttributes = new HashMap<>();
    }
    attributes = new HashMap<>();
  }

  public TwContext asEntryPoint(@NonNull String group, @NonNull String name) {
    if (StringUtils.trimToNull(group) == null) {
      throw new IllegalStateException("Empty group provided.");
    }
    if (StringUtils.trimToNull(name) == null) {
      throw new IllegalStateException("Empty name provided.");
    }

    set(GROUP_KEY, group);
    set(NAME_KEY, name);
    return this;
  }

  public TwContext attach() {
    TwContext current = contextTl.get();
    contextTl.set(this);
    return current;
  }

  public void detach(TwContext previous) {
    contextTl.set(previous);
  }

  public <T> T get(String key) {
    return (T) attributes.get(key);
  }

  public <T> T getCurrent(String key) {
    return (T) currentAttributes.get(key);
  }

  public TwContext set(String key, Object value) {
    if (value == null) {
      attributes.remove(key);
      currentAttributes.remove(key);
    } else {
      if (!Objects.equals(attributes.get(key), value)) {
        attributes.put(key, value);
        currentAttributes.put(key, value);
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
      if (Objects.equals(context.getCurrent(key), search)) {
        context.currentAttributes.put(key, value);
      }
      context = context.getParent();
      if (i++ > MAX_DEPTH) {
        throw new IllegalStateException("Indefinite loop detected. Most likely the parent-chain is circular.");
      }
    }
  }

  public TwContext setName(@NonNull String name) {
    set(NAME_KEY, name);
    return this;
  }

  public TwContext setGroup(@NonNull String group) {
    set(GROUP_KEY, group);
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
    List<TwContextInterceptor> applicableInterceptors = new ArrayList<>();
    for (TwContextInterceptor interceptor : TwContext.interceptors) {
      if (interceptor.applies(this)) {
        applicableInterceptors.add(interceptor);
      }
    }
    return executeWithInterceptors(supplier, applicableInterceptors, 0);
  }

  private <T> T executeWithInterceptors(Supplier<T> supplier, List<TwContextInterceptor> interceptors, int interceptorIdx) {
    if (interceptorIdx >= interceptors.size()) {
      return supplier.get();
    }
    return interceptors.get(interceptorIdx).intercept(() -> executeWithInterceptors(supplier, interceptors, interceptorIdx + 1));
  }
}
