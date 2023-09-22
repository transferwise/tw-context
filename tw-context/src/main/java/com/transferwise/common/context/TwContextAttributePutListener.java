package com.transferwise.common.context;

import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public interface TwContextAttributePutListener {

  void attributePut(TwContext context, String key, Object oldValue, Object newValue);
}
