package com.transferwise.common.context;

import static com.transferwise.common.context.UnitOfWork.TW_CONTEXT_KEY;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
import lombok.NonNull;

public class DefaultUnitOfWorkManager implements UnitOfWorkManager {

  private final TwContextMetricsTemplate metricsTemplate;

  public DefaultUnitOfWorkManager(MeterRegistry meterRegistry) {
    this.metricsTemplate = new TwContextMetricsTemplate(meterRegistry);
  }

  @Override
  public Builder createEntryPoint(@NonNull String group, @NonNull String name) {
    return new DefaultBuilder(group, name, null);
  }

  @Override
  public Builder createEntryPoint(@NonNull String group, @NonNull String name, String owner) {
    return new DefaultBuilder(group, name, owner);
  }

  @Override
  public Builder createUnitOfWork() {
    return new DefaultBuilder(null, null, null);
  }

  @Override
  public UnitOfWork getUnitOfWork() {
    return getUnitOfWork(TwContext.current());
  }

  @Override
  public UnitOfWork getUnitOfWork(@NonNull TwContext context) {
    return context.get(TW_CONTEXT_KEY);
  }

  @Override
  public void checkDeadLine(String sourceKey) {
    checkDeadLine(TwContext.current(), sourceKey);
  }

  @Override
  public void checkDeadLine(TwContext context, String sourceKey) {
    UnitOfWork unitOfWork = getUnitOfWork(context);

    if (unitOfWork != null && unitOfWork.hasDeadlinePassed()) {
      metricsTemplate.registerDeadlineExceeded(context.getGroup(), context.getName(), context.getOwner(), unitOfWork.getCriticality(), sourceKey);
      throw new DeadlineExceededException(unitOfWork.getDeadline(), unitOfWork.getCreationTime());
    }
  }

  protected class DefaultBuilder implements Builder {

    private final String entryPointGroup;
    private final String entryPointName;
    private Instant deadline;
    private Criticality criticality;
    private String owner;

    public DefaultBuilder(String entryPointGroup, String entryPointName, String owner) {
      this.entryPointGroup = entryPointGroup;
      this.entryPointName = entryPointName;
      this.owner = owner;
    }

    @Override
    public Builder deadline(Instant deadline) {
      this.deadline = deadline;
      return this;
    }

    @Override
    public Builder deadline(Duration duration) {
      this.deadline = TwContextClockHolder.getClock().instant().plus(duration);
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

      if (owner != null) {
        context.setOwner(owner);
      }

      UnitOfWork currentUnitOfWork = getUnitOfWork(context);
      UnitOfWork newUnitOfWork = new UnitOfWork();
      if (currentUnitOfWork != null) {
        newUnitOfWork.setCriticality(currentUnitOfWork.getCriticality())
            .setDeadline(currentUnitOfWork.getDeadline());
      }

      if (deadline != null) {
        Instant currentDeadline = newUnitOfWork.getDeadline();
        if (currentDeadline != null && currentDeadline.isBefore(deadline)) {
          // Code smell we want to know about.
          metricsTemplate.registerDeadlineExtending(group, name, criticality);
        }
        newUnitOfWork.setDeadline(deadline);
      }
      if (criticality != null) {
        Criticality currentCriticality = newUnitOfWork.getCriticality();
        if (currentCriticality != null && !currentCriticality.equals(criticality)) {
          // Code smell we want to know about.
          metricsTemplate.registerCriticalityChange(group, name, criticality);
        }
        newUnitOfWork.setCriticality(criticality);
      }

      context.put(TW_CONTEXT_KEY, newUnitOfWork);

      TwContext finalContext = context;
      context.withExecutionWrapper(supplier -> {
        checkDeadLine(finalContext, "UnitOfWork");
        return supplier.get();
      });

      return context;
    }
  }
}
