package com.cloud.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class TransactionCommitSupportTest {

  @AfterEach
  void clearTransactionSynchronization() {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void runAfterCommitRunsImmediatelyWithoutSynchronization() {
    AtomicInteger counter = new AtomicInteger();

    TransactionCommitSupport.runAfterCommit(counter::incrementAndGet);

    assertThat(counter).hasValue(1);
  }

  @Test
  void runAfterCommitDefersUntilCommitWhenSynchronizationIsActive() {
    AtomicInteger counter = new AtomicInteger();
    TransactionSynchronizationManager.initSynchronization();

    TransactionCommitSupport.runAfterCommit(counter::incrementAndGet);

    assertThat(counter).hasValue(0);
    TransactionSynchronizationManager.getSynchronizations()
        .forEach(synchronization -> synchronization.afterCommit());
    assertThat(counter).hasValue(1);
  }

  @Test
  void runAfterCommitDoesNotRunOnRollback() {
    AtomicInteger counter = new AtomicInteger();
    TransactionSynchronizationManager.initSynchronization();

    TransactionCommitSupport.runAfterCommit(counter::incrementAndGet);

    TransactionSynchronizationManager.getSynchronizations()
        .forEach(
            synchronization ->
                synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
    assertThat(counter).hasValue(0);
  }
}
