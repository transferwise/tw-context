package com.transferwise.common.context;

import com.transferwise.common.context.UnitOfWork.Builder;

public interface UnitOfWorkFactory {

  Builder asEntryPoint(String group, String name);

  public Builder create();
}
