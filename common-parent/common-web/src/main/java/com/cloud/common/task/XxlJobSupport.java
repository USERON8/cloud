package com.cloud.common.task;

import com.xxl.job.core.context.XxlJobHelper;
import org.slf4j.Logger;

public final class XxlJobSupport {

  private XxlJobSupport() {}

  public static void logMessage(Logger log, String message) {
    XxlJobHelper.log(message);
    log.info(message);
  }

  public static void logHandledCount(Logger log, String jobName, int handledCount) {
    logMessage(log, jobName + " finished, handled records: " + handledCount);
  }

  public static void logCount(Logger log, String jobName, String field, int count) {
    logMessage(log, jobName + " finished, " + field + "=" + count);
  }
}
