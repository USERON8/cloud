package com.cloud.common.util;

import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class DateRangeValidator {

  private DateRangeValidator() {}

  public static void validateInclusiveRange(LocalDate startDate, LocalDate endDate, long maxDays) {
    if (endDate.isBefore(startDate)) {
      throw new BizException(
          ResultCode.BAD_REQUEST, "endDate must be greater than or equal to startDate");
    }
    if (ChronoUnit.DAYS.between(startDate, endDate) > maxDays) {
      throw new BizException(
          ResultCode.BAD_REQUEST, "date range cannot exceed " + maxDays + " days");
    }
  }
}
