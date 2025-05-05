package com.transferwise.common.context;

import java.time.Duration;
import java.time.Instant;
import lombok.Data;

@Data
public class DeadlineExceededException extends RuntimeException {

  static final long serialVersionUID = 1L;

  private long sinceDeadlineExceededMillis;
  private long durationMillis;

  public DeadlineExceededException(String message) {
    super(message);
  }

  public DeadlineExceededException(Instant deadline) {
    this(deadline, null);
  }

  public DeadlineExceededException(Instant deadline, Instant unitOfWorkCreationTime) {
    super(createMessage(deadline, unitOfWorkCreationTime));
    sinceDeadlineExceededMillis = sinceDeadlineExceededMillis(deadline);
    durationMillis = durationMillis(unitOfWorkCreationTime);
  }

  private static String createMessage(Instant deadline, Instant unitOfWorkCreationTime) {
    long sinceDeadlineExceededMillis = sinceDeadlineExceededMillis(deadline);
    String formattedDeadline = DurationFormatUtils.formatDuration(Duration.ofMillis(sinceDeadlineExceededMillis));
    if (unitOfWorkCreationTime == null) {
      return "Deadline exceeded " + formattedDeadline + " ago.";
    } else {
      long durationMillis = durationMillis(unitOfWorkCreationTime);
      String formattedDuration = DurationFormatUtils.formatDuration(Duration.ofMillis(durationMillis));
      return "Deadline exceeded " + formattedDeadline + " ago. Time taken in current unit of work was " + formattedDuration + ".";
    }
  }

  private static long sinceDeadlineExceededMillis(Instant deadline) {
    return Math.max(0, TwContextClockHolder.getClock().millis() - deadline.toEpochMilli());
  }

  private static long durationMillis(Instant unitOfWorkCreationTime) {
    return unitOfWorkCreationTime == null
        ? 0
        : Math.max(0, TwContextClockHolder.getClock().millis() - unitOfWorkCreationTime.toEpochMilli());
  }
}
