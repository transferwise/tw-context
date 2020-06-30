package com.transferwise.common.context;

import java.time.Clock;

public class TwContextClockHolder {

  private static Clock clock = Clock.systemUTC();

  public static void setClock(Clock clock) {
    TwContextClockHolder.clock = clock;
  }

  public static void reset() {
    clock = Clock.systemUTC();
  }

  public static Clock getClock() {
    return clock;
  }
}
