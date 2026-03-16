package com.cloud.search.service.support;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class HotKeywordKeys {

  public static final String TOTAL_KEY = "search:hot:total";
  private static final String DAILY_PREFIX = "search:hot:";
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

  private HotKeywordKeys() {}

  public static String dailyKey(LocalDate date) {
    if (date == null) {
      return DAILY_PREFIX + DATE_FORMATTER.format(LocalDate.now());
    }
    return DAILY_PREFIX + DATE_FORMATTER.format(date);
  }

  public static String todayKey() {
    return dailyKey(LocalDate.now());
  }
}
