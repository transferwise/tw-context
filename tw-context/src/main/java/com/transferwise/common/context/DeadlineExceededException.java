package com.transferwise.common.context;

import java.time.Duration;
import java.time.Instant;

public class DeadlineExceededException extends RuntimeException {

  static final long serialVersionUID = 1L;

  public DeadlineExceededException(Instant deadline, Instant unitOfWorkCreationTime) {
    super(createMessage(deadline, unitOfWorkCreationTime));
  }

  private static String createMessage(Instant deadline, Instant unitOfWorkCreationTime) {
    Duration sinceDeadlineExceeded = Duration.between(deadline, TwContextClockHolder.getClock().instant());
    String formattedDeadline = DurationFormatUtils.formatDuration(sinceDeadlineExceeded);
    if (unitOfWorkCreationTime == null) {
      return "Deadline exceeded " + formattedDeadline + " ago.";
    } else {
      Duration duration = Duration.between(unitOfWorkCreationTime, TwContextClockHolder.getClock().instant());
      String formattedDuration = DurationFormatUtils.formatDuration(duration);
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
