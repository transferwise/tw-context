package com.transferwise.common.context;

import com.transferwise.common.baseutils.clock.ClockHolder;
import io.micrometer.core.instrument.Metrics;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class UnitOfWork {
  public static final String KEY_DEADLINE = "TwContextDeadline";
  public static final String KEY_CRITICALITY = "TwContextCriticality";

  public static Instant getDeadline() {
    return TwContext.current().get(KEY_DEADLINE);
  }

  public static Criticality getCriticality() {
    Criticality criticality = TwContext.current().get(KEY_CRITICALITY);
    return criticality == null ? Criticality.SCHEDDABLE : criticality;
  }

  public static boolean hasDeadlinePassed() {
    Instant deadline = getDeadline();
    return deadline != null && deadline.isBefore(ClockHolder.getClock().instant());
  }

  public static void checkDeadLine() {
    if (hasDeadlinePassed()) {
      throw new DeadlineExceededException(getDeadline());
    }
  }

  public interface Builder {

    Builder deadline(@NonNull Instant deadline);

    Builder deadline(@NonNull Duration duration);

    Builder criticality(@NonNull Criticality criticality);

    TwContext toContext();

    <T> T execute(Supplier<T> supplier);

    void execute(Runnable runnable);
  }

}
