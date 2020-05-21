package com.transferwise.common.context;

import com.transferwise.common.baseutils.clock.ClockHolder;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class DeadlineExceededException extends RuntimeException {

  static final long serialVersionUID = 1L;

  public DeadlineExceededException(Instant deadline, Instant unitOfWorkCreationTime) {
    super(createMessage(deadline, unitOfWorkCreationTime));
  }

  private static String createMessage(Instant deadline, Instant unitOfWorkCreationTime) {
    long sinceDeadlineExceededMillis = Math.max(0, ClockHolder.getClock().millis() - deadline.toEpochMilli());
    String formattedDeadline = DurationFormatUtils.formatDuration(Duration.ofMillis(sinceDeadlineExceededMillis));
    if (unitOfWorkCreationTime == null) {
      return "Deadline exceeded " + formattedDeadline + " ago.";
    } else {
      long durationMillis = Math.max(0, ClockHolder.getClock().millis() - unitOfWorkCreationTime.toEpochMilli());
      String formattedDuration = DurationFormatUtils.formatDuration(Duration.ofMillis(durationMillis));
      return "Deadline exceeded " + formattedDeadline + " ago. Time taken in current unit of work was " + formattedDuration + ".";
    }
  }

  public DeadlineExceededException(Instant deadline) {
    this(deadline, null);
  }

  public DeadlineExceededException(String message) {
    super(message);
  }
}
