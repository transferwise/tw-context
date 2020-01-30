package com.transferwise.common.context;

import static com.transferwise.common.context.UnitOfWork.KEY_CRITICALITY;
import static com.transferwise.common.context.UnitOfWork.KEY_DEADLINE;

import com.transferwise.common.baseutils.clock.ClockHolder;
import com.transferwise.common.context.UnitOfWork.Builder;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

public class DefaultUnitOfWorkFactory implements UnitOfWorkFactory {

  private MeterRegistry meterRegistry;

  @Override
  public Builder asEntryPoint(String group, String name) {
    return new DefaultBuilder(meterRegistry, group, name);
  }

  @Override
  public Builder create() {
    return new DefaultBuilder(meterRegistry, null, null);
  }

  protected static class DefaultBuilder implements Builder {

    private MeterRegistry meterRegistry;
    private String entryPointGroup;
    private String entryPointName;
    private Instant deadline;
    private Criticality criticality;

    public DefaultBuilder(MeterRegistry meterRegistry, String entryPointGroup, String entryPointName) {
      this.meterRegistry = meterRegistry;
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
      if (entryPointGroup == null) {
        context.setGroup(entryPointGroup);
      }
      if (entryPointName != null) {
        context.setName(entryPointName);
      }

      String group = context.getGroup();
      String name = context.getName();

      if (group == null) {
        throw new IllegalStateException("Unit of Work has no Group defined.");
      }
      if (name == null) {
        throw new IllegalStateException("Unit of Work has no Name defined.");
      }

      if (deadline != null) {
        Instant currentDeadline = context.get(KEY_DEADLINE);
        if (currentDeadline != null && currentDeadline.isBefore(deadline)) {
          // Code smell we want to know about.
          meterRegistry.counter("TwContext.UnitOfWork.DeadlineShrinked", "group", group).increment();
          deadline = currentDeadline;
        }
        context.set(KEY_DEADLINE, deadline);
      }
      if (criticality != null) {
        Criticality currentCriticality = context.get(KEY_CRITICALITY);
        if (currentCriticality != null) {
          // Code smell we want to know about.
          meterRegistry.counter("TwContext.UnitOfWork.CriticalityChanged", "group", group).increment();
        } else {
          context.set(KEY_CRITICALITY, criticality);
        }
      }
      return context;
    }

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
