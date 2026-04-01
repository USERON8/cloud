package com.cloud.stock.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
public class StockOutboxDispatcher {

  private final ApplicationEventPublisher applicationEventPublisher;
  private final StockOutboxRelay stockOutboxRelay;

  public void dispatchAfterCommit() {
    if (TransactionSynchronizationManager.isActualTransactionActive()) {
      applicationEventPublisher.publishEvent(new StockOutboxDispatchEvent());
      return;
    }
    stockOutboxRelay.dispatch();
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onOutboxDispatch(StockOutboxDispatchEvent event) {
    stockOutboxRelay.dispatch();
  }
}
