package com.transferwise.common.context;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;
import lombok.NonNull;

public interface UnitOfWorkFactory {

  Builder asEntryPoint(String group, String name);

  Builder create();

  interface Builder {

    Builder deadline(@NonNull Instant deadline);

    Builder deadline(@NonNull Duration duration);

    Builder criticality(@NonNull Criticality criticality);

    TwContext toContext();

    <T> T execute(Supplier<T> supplier);

    void execute(Runnable runnable);
  }

}
