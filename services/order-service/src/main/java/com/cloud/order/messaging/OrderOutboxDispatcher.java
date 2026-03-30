package com.cloud.order.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
public class OrderOutboxDispatcher {

  private final ApplicationEventPublisher applicationEventPublisher;
  private final OrderOutboxRelay orderOutboxRelay;

  public void dispatchAfterCommit() {
    if (TransactionSynchronizationManager.isActualTransactionActive()) {
      applicationEventPublisher.publishEvent(new OrderOutboxDispatchEvent());
      return;
    }
    orderOutboxRelay.dispatch();
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onOutboxDispatch(OrderOutboxDispatchEvent event) {
    orderOutboxRelay.dispatch();
  }
}
