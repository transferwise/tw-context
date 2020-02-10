package com.transferwise.common.context;

import static com.transferwise.common.context.TwContextMetrics.TAG_EP_GROUP;
import static com.transferwise.common.context.TwContextMetrics.TAG_EP_NAME;
import static com.transferwise.common.context.UnitOfWork.KEY_CRITICALITY;
import static com.transferwise.common.context.UnitOfWork.KEY_DEADLINE;

import com.transferwise.common.baseutils.clock.ClockHolder;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
import lombok.NonNull;

public class DefaultUnitOfWorkFactory implements UnitOfWorkFactory {

  private final MeterRegistry meterRegistry;

  public DefaultUnitOfWorkFactory(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  @Override
  public Builder asEntryPoint(@NonNull String group, @NonNull String name) {
    return new DefaultBuilder(meterRegistry, group, name);
  }

  @Override
  public Builder newUnitOfWork() {
    return new DefaultBuilder(meterRegistry, null, null);
  }

  protected static class DefaultBuilder implements Builder {

    private final MeterRegistry meterRegistry;
    private final String entryPointGroup;
    private final String entryPointName;
    private Instant deadline;
    private Criticality criticality;

    public DefaultBuilder(MeterRegistry meterRegistry, String entryPointGroup,
        String entryPointName) {
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
      TwContext context = TwContext.current().createSubContext();

      if (entryPointGroup != null || entryPointName != null) {
        if (entryPointName == null || entryPointGroup == null) {
          throw new IllegalStateException(
              "Both group and name has to be set for an entrypoint. group='" + entryPointGroup + "', name='"
                  + entryPointName + "'");
        }
        context = context.asEntryPoint(entryPointGroup, entryPointName);
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
          meterRegistry.counter("TwContext.UnitOfWork.DeadlineReduced", TAG_EP_GROUP, group)
              .increment();
          deadline = currentDeadline;
        }
        context.put(KEY_DEADLINE, deadline);
      }
      if (criticality != null) {
        Criticality currentCriticality = context.get(KEY_CRITICALITY);
        if (currentCriticality != null) {
          // Code smell we want to know about.
          meterRegistry.counter("TwContext.UnitOfWork.CriticalityChanged", TAG_EP_NAME, group)
              .increment();
        } else {
          context.put(KEY_CRITICALITY, criticality);
        }
      }

      context.put(UnitOfWork.KEY_UNIT_OF_WORK, Boolean.TRUE);
      return context;
    }
  }
}
