package com.cloud.common.util;

public final class SearchInputNormalizer {

  private SearchInputNormalizer() {}

  public static String normalizeKeyword(String keyword) {
    if (keyword == null) {
      return "";
    }
    String normalized = keyword.trim();
    return normalized.isEmpty() ? "" : normalized;
  }
}
