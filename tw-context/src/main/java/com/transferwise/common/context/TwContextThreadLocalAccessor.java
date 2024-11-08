package com.transferwise.common.context;

import io.micrometer.context.ThreadLocalAccessor;

import static com.transferwise.common.context.UnitOfWork.TW_CONTEXT_KEY;

public class TwContextThreadLocalAccessor implements ThreadLocalAccessor<TwContext> {

  @Override
  public Object key() {
    return TW_CONTEXT_KEY;
  }

  @Override
  public TwContext getValue() {
    return TwContext.current();
  }

  @Override
  public void setValue(TwContext context) {
    context.attach();
  }

  @Override
  public void reset() {
    TwContext.ROOT_CONTEXT.attach();
  }
}
