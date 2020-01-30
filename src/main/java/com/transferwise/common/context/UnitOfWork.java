package com.transferwise.common.context;

import com.transferwise.common.baseutils.clock.ClockHolder;
import io.micrometer.core.instrument.MeterRegistry;
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

  public static Builder asEntryPoint(String group, String name) {
    return new DefaultBuilder(group, name);
  }

  public static Builder builder() {
    return new DefaultBuilder();
  }

  public interface Builder {

    Builder deadline(@NonNull Instant deadline);

    Builder deadline(@NonNull Duration duration);

    Builder criticality(@NonNull Criticality criticality);

    TwContext toContext();

    <T> T execute(Supplier<T> supplier);

    void execute(Runnable runnable);
  }

  public static class DefaultBuilder implements Builder {

    private String entryPointGroup;
    private String entryPointName;
    private Instant deadline;
    private Criticality criticality;

    private UnitOfWork unitOfWork = new UnitOfWork();

    public DefaultBuilder() {
    }

    public DefaultBuilder(String entryPointGroup, String entryPointName) {
      this.entryPointGroup = entryPointGroup;
      this.entryPointName = entryPointName;
    }

    @Override
    public Builder deadline(Instant deadline) {
      this.deadline = deadline;
      return this;
    }

    @Override
    public Builder deadline(Duration duration) {
      this.deadline = ClockHolder.getClock().instant().plus(duration);
      return this;
    }

    @Override
    public Builder criticality(Criticality criticality) {
      this.criticality = criticality;
      return this;
    }

    @Override
    public TwContext toContext() {
      TwContext context = TwContext.newSubContext();
      if (entryPointGroup != null) {
        context.setGroup(entryPointGroup);
      }
      if (entryPointName != null) {
        context.setName(entryPointName);
      }

      if (context.getGroup() == null) {
        throw new IllegalStateException("Unit of Work has no Group defined.");
      }
      if (context.getName() == null) {
        throw new IllegalStateException("Unit of Work has no Name defined.");
      }

      if (deadline != null) {
        Instant currentDeadline = context.get(KEY_DEADLINE);
        if (currentDeadline != null && currentDeadline.isBefore(deadline)) {
          MeterRegistry
          //TODO: Add metric for code smell
          deadline = currentDeadline;
        }
        context.set(KEY_DEADLINE, deadline);
      }
      if (criticality != null) {
        Criticality currentCriticality = context.get(KEY_CRITICALITY);
        if (currentCriticality != null) {
          //TODO: Add metric for code smell
        } else {
          context.set(KEY_CRITICALITY, criticality);
        }
      }
      return context;
    }

    // 2 Convenience methods

    @Override
    public <T> T execute(Supplier<T> supplier) {
      return toContext().execute(supplier);
    }

    @Override
    public void execute(Runnable runnable) {
      toContext().execute(runnable);
    }
  }
}
