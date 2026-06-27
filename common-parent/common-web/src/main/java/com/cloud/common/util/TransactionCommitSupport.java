package com.cloud.common.util;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public final class TransactionCommitSupport {

  private TransactionCommitSupport() {}

  public static void runAfterCommit(Runnable task) {
    if (task == null) {
      return;
    }
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              task.run();
            }
          });
      return;
    }
    task.run();
  }

  public static void runAfterCommitRepeated(
      Runnable task, TaskScheduler taskScheduler, long delayMs) {
    runAfterCommit(
        () -> {
          task.run();
          TaskSchedulerSupport.scheduleDelayed(taskScheduler, delayMs, task);
        });
  }
}
