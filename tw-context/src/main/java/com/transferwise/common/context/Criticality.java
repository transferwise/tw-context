package com.transferwise.common.context;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.NonNull;

public enum Criticality {
  CRITICAL_PLUS,
  CRITICAL,
  SCHEDDABLE_PLUS,
  SCHEDDABLE;

  private static final Map<String, Criticality> nameIndex = Arrays.stream(Criticality.values())
      .collect(Collectors.toMap(Criticality::name, c -> c));

  @SuppressWarnings("unused")
  public static Criticality findByName(@NonNull String name) {
    return nameIndex.get(name.toUpperCase());
  }
}
