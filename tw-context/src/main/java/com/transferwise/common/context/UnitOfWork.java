package com.transferwise.common.context;

import com.transferwise.common.baseutils.clock.ClockHolder;
import java.time.Instant;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class UnitOfWork {

  public static final String KEY_DEADLINE = "TwContextDeadline";
  public static final String KEY_CRITICALITY = "TwContextCriticality";
  public static final String KEY_UNIT_OF_WORK = "TwContextUnitOfWork";

  public static Instant getDeadline() {
    return getDeadline(TwContext.current());
  }

  public static Instant getDeadline(@NonNull TwContext context) {
    return context.get(KEY_DEADLINE);
  }

  public static Criticality getCriticality() {
    return getCriticality(TwContext.current());
  }

  public static Criticality getCriticality(@NonNull TwContext context) {
    Criticality criticality = context.get(KEY_CRITICALITY);
    return criticality == null ? Criticality.SHEDDABLE_PLUS : criticality;
  }

  public static boolean hasDeadlinePassed() {
    return hasDeadlinePassed(TwContext.current());
  }

  public static boolean hasDeadlinePassed(@NonNull TwContext context) {
    Instant deadline = getDeadline(context);
    return deadline != null && deadline.isBefore(ClockHolder.getClock().instant());
  }

  public static void checkDeadLine() {
    checkDeadLine(TwContext.current());
  }

  public static void checkDeadLine(TwContext context) {
    if (hasDeadlinePassed(context)) {
      throw new DeadlineExceededException(getDeadline());
    }
  }

  public static boolean hasBeenDefined(TwContext context) {
    return Boolean.TRUE.equals(context.get(KEY_UNIT_OF_WORK));
  }

  public static boolean hasBeenDefined() {
    return hasBeenDefined(TwContext.current());
  }

  private UnitOfWork() {
  }
}
