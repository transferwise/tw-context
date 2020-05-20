package com.transferwise.common.context;

import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public interface TwContextAttributeChangeListener {

  void attributeChanged(TwContext context, String key, Object oldValue, Object newValue);
}
