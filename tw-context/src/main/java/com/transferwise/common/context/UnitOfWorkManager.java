package com.transferwise.common.context;

import java.time.Duration;
import java.time.Instant;
import lombok.NonNull;

public interface UnitOfWorkManager {

  Builder createEntryPoint(@NonNull String group, @NonNull String name);

  Builder createEntryPoint(@NonNull String group, @NonNull String name, String owner);

  Builder createUnitOfWork();

  UnitOfWork getUnitOfWork();

  UnitOfWork getUnitOfWork(@NonNull TwContext context);

  void checkDeadLine(String sourceKey);

  void checkDeadLine(@NonNull TwContext context, String sourceKey);

  interface Builder {

    Builder deadline(@NonNull Instant deadline);

    Builder deadline(@NonNull Duration duration);

    Builder criticality(@NonNull Criticality criticality);

    TwContext toContext();
  }
}
