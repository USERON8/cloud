package com.cloud.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SearchInputNormalizerTest {

  @Test
  void shouldNormalizeNullAndBlankKeywordsToEmptyString() {
    assertEquals("", SearchInputNormalizer.normalizeKeyword(null));
    assertEquals("", SearchInputNormalizer.normalizeKeyword("   "));
  }

  @Test
  void shouldTrimKeywordWithoutChangingContent() {
    assertEquals("phone case", SearchInputNormalizer.normalizeKeyword("  phone case  "));
  }
}
