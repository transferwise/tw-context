package com.transferwise.common.context;

import java.time.Duration;
import java.time.Instant;
import lombok.NonNull;

public interface UnitOfWorkFactory {

  Builder asEntryPoint(String group, String name);

  Builder newUnitOfWork();

  interface Builder {

    Builder deadline(@NonNull Instant deadline);

    Builder deadline(@NonNull Duration duration);

    Builder criticality(@NonNull Criticality criticality);

    TwContext toContext();
  }
}
