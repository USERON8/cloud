package com.cloud.common.util;

import java.time.Instant;
import org.springframework.scheduling.TaskScheduler;

public final class TaskSchedulerSupport {

  private TaskSchedulerSupport() {}

  public static void scheduleDelayed(TaskScheduler taskScheduler, long delayMs, Runnable task) {
    long safeDelayMs = Math.max(0L, delayMs);
    if (safeDelayMs <= 0L) {
      return;
    }
    taskScheduler.schedule(task, Instant.now().plusMillis(safeDelayMs));
  }
}
