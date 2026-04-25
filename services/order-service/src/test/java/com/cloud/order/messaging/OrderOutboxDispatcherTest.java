package com.cloud.order.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class OrderOutboxDispatcherTest {

  @Mock private ApplicationEventPublisher applicationEventPublisher;
  @Mock private OrderOutboxRelay orderOutboxRelay;

  @InjectMocks private OrderOutboxDispatcher orderOutboxDispatcher;

  @Test
  void dispatchAfterCommitPublishesEventWithinTransaction() {
    try (MockedStatic<TransactionSynchronizationManager> transactionSynchronizationManager =
        mockStatic(TransactionSynchronizationManager.class)) {
      transactionSynchronizationManager
          .when(TransactionSynchronizationManager::isActualTransactionActive)
          .thenReturn(true);

      orderOutboxDispatcher.dispatchAfterCommit();

      verify(applicationEventPublisher).publishEvent(any(OrderOutboxDispatchEvent.class));
      verify(orderOutboxRelay, never()).dispatch();
    }
  }

  @Test
  void dispatchAfterCommitRunsRelayImmediatelyWithoutTransaction() {
    try (MockedStatic<TransactionSynchronizationManager> transactionSynchronizationManager =
        mockStatic(TransactionSynchronizationManager.class)) {
      transactionSynchronizationManager
          .when(TransactionSynchronizationManager::isActualTransactionActive)
          .thenReturn(false);

      orderOutboxDispatcher.dispatchAfterCommit();

      verify(orderOutboxRelay).dispatch();
      verify(applicationEventPublisher, never()).publishEvent(any());
    }
  }

  @Test
  void onOutboxDispatchUsesOrderAsyncExecutor() throws Exception {
    Method method =
        OrderOutboxDispatcher.class.getDeclaredMethod(
            "onOutboxDispatch", OrderOutboxDispatchEvent.class);

    Async async = method.getAnnotation(Async.class);

    assertNotNull(async);
    assertEquals("orderAsyncExecutor", async.value());
  }
}
