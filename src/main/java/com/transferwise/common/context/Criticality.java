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

  private static Map<String, Criticality> nameIndex = Arrays.stream(Criticality.values()).collect(Collectors.toMap(c -> c.name(), c -> c));

  public static Criticality findByName(@NonNull String name) {
    if (name == null) {
      return null;
    }
    return nameIndex.get(name.toUpperCase());
  }
}
