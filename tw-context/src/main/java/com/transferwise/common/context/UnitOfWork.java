package com.transferwise.common.context;

import java.time.Instant;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class UnitOfWork {

  public static final String TW_CONTEXT_KEY = "TwContextUnitOfWork";

  private Criticality criticality;
  private Instant deadline;
  @NonNull
  private Instant creationTime = Instant.ofEpochMilli(TwContextClockHolder.getClock().millis());

  public boolean hasDeadlinePassed() {
    return deadline != null && deadline.isBefore(Instant.ofEpochMilli(TwContextClockHolder.getClock().millis()));
  }

}
