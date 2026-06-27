package com.cloud.common.security;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public final class SecurityTextParsers {

  private SecurityTextParsers() {}

  public static Set<String> parseCsv(String raw) {
    if (raw == null || raw.isBlank()) {
      return Set.of();
    }
    return Arrays.stream(raw.split(","))
        .map(String::trim)
        .filter(value -> !value.isBlank())
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }
}
