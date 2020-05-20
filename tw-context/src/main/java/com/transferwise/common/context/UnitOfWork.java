package com.transferwise.common.context;

import com.transferwise.common.baseutils.clock.ClockHolder;
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
  private Instant creationTime = ClockHolder.getClock().instant();

  public boolean hasDeadlinePassed() {
    return deadline != null && deadline.isBefore(ClockHolder.getClock().instant());
  }

}
