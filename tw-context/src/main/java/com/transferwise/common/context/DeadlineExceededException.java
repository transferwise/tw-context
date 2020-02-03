package com.transferwise.common.context;

import java.time.Instant;

public class DeadlineExceededException extends RuntimeException {

  static final long serialVersionUID = 1L;

  public DeadlineExceededException(Instant deadline) {
    this("Deadline " + deadline.toEpochMilli() + " exceeded.");
  }

  public DeadlineExceededException(String message) {
    super(message);
  }
}
