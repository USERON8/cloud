package com.cloud.stock.service.support;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.cloud.stock.mapper.StockSegmentMapper;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class StockRedisCacheServiceTest {

  @Mock private StringRedisTemplate stringRedisTemplate;
  @Mock private StockSegmentMapper stockSegmentMapper;
  @Mock private TaskScheduler taskScheduler;

  @AfterEach
  void tearDown() {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void evictLedgerDeletesImmediatelyWithoutSynchronization() {
    StockRedisCacheService cacheService = newCacheService();

    cacheService.evictLedgerAfterCommit(10L);

    verify(stringRedisTemplate).delete("stock:summary:10");
  }

  @Test
  void evictLedgerDeletesOnlyAfterCommit() {
    StockRedisCacheService cacheService = newCacheService();
    TransactionSynchronizationManager.initSynchronization();

    cacheService.evictLedgerAfterCommit(10L);

    verify(stringRedisTemplate, never()).delete("stock:summary:10");
    runAfterCommitSynchronizations();

    verify(stringRedisTemplate).delete("stock:summary:10");
  }

  @Test
  void evictLedgerDoesNotDeleteOnRollback() {
    StockRedisCacheService cacheService = newCacheService();
    TransactionSynchronizationManager.initSynchronization();

    cacheService.evictLedgerAfterCommit(10L);

    runRollbackSynchronizations();

    verify(stringRedisTemplate, never()).delete("stock:summary:10");
  }

  private StockRedisCacheService newCacheService() {
    StockRedisCacheService cacheService =
        new StockRedisCacheService(stringRedisTemplate, stockSegmentMapper, taskScheduler);
    ReflectionTestUtils.setField(cacheService, "delayedDoubleDeleteMs", 0L);
    return cacheService;
  }

  private void runAfterCommitSynchronizations() {
    List<TransactionSynchronization> synchronizations =
        TransactionSynchronizationManager.getSynchronizations();
    synchronizations.forEach(TransactionSynchronization::afterCommit);
    synchronizations.forEach(
        synchronization ->
            synchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
  }

  private void runRollbackSynchronizations() {
    TransactionSynchronizationManager.getSynchronizations()
        .forEach(
            synchronization ->
                synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
  }
}
