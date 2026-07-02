package com.cloud.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HtmlEscapeUtilsTest {

  @Test
  void shouldReturnEmptyStringForNullInput() {
    assertEquals("", HtmlEscapeUtils.escape(null));
  }

  @Test
  void shouldEscapeHtmlSensitiveCharacters() {
    assertEquals(
        "&lt;span data-x=&quot;1&quot;&gt;Tom &amp; &#39;Jerry&#39;&lt;/span&gt;",
        HtmlEscapeUtils.escape("<span data-x=\"1\">Tom & 'Jerry'</span>"));
  }
}
