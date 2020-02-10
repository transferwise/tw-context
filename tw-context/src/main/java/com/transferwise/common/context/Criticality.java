package com.transferwise.common.context;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.NonNull;

/**
 * Levels are taken from https://landing.google.com/sre/sre-book/chapters/handling-overload/
 */
public enum Criticality {
  // Ordered from highest to lowest priority
  // Sheddable_plus should be used as a default
  CRITICAL_PLUS,
  CRITICAL,
  SHEDDABLE_PLUS,
  SHEDDABLE;

  private static final Map<String, Criticality> nameIndex = Arrays.stream(Criticality.values())
      .collect(Collectors.toMap(Criticality::name, c -> c));

  @SuppressWarnings("unused")
  public static Criticality findByName(@NonNull String name) {
    return nameIndex.get(name.toUpperCase());
  }
}
