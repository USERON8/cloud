package com.cloud.order.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.common.messaging.event.OrderAutoReceiveEvent;
import com.cloud.common.messaging.event.OrderShippedEvent;
import java.util.List;
import org.apache.rocketmq.common.message.MessageConst;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class OrderDelayedMessageProducerTest {

  @Mock private StreamBridge streamBridge;

  @AfterEach
  void tearDown() {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void autoReceiveDispatchesImmediatelyWithoutSynchronization() {
    when(streamBridge.send(eq("orderAutoReceiveProducer-out-0"), any(Message.class)))
        .thenReturn(true);
    OrderAutoReceiveMessageProducer producer = newAutoReceiveProducer();
    OrderAutoReceiveEvent event = autoReceiveEvent();

    producer.sendAfterCommit(event);

    Message<?> message = captureMessage("orderAutoReceiveProducer-out-0");
    assertThat(message.getPayload()).isSameAs(event);
    assertThat(message.getHeaders().get(MessageConst.PROPERTY_KEYS)).isEqualTo("SO-1001");
    assertThat(message.getHeaders().get(MessageConst.PROPERTY_TAGS))
        .isEqualTo("ORDER_AUTO_RECEIVE");
    assertThat(message.getHeaders().get(MessageConst.PROPERTY_DELAY_TIME_LEVEL)).isEqualTo("16");
    assertThat(message.getHeaders().get("eventId")).isNotNull();
    assertThat(message.getHeaders().get("eventType")).isEqualTo("ORDER_AUTO_RECEIVE");
    assertThat(event.getTimestamp()).isNotNull();
  }

  @Test
  void autoReceiveDispatchesOnlyAfterCommit() {
    when(streamBridge.send(eq("orderAutoReceiveProducer-out-0"), any(Message.class)))
        .thenReturn(true);
    OrderAutoReceiveMessageProducer producer = newAutoReceiveProducer();
    TransactionSynchronizationManager.initSynchronization();

    producer.sendAfterCommit(autoReceiveEvent());

    verify(streamBridge, never()).send(eq("orderAutoReceiveProducer-out-0"), any(Message.class));
    runAfterCommitSynchronizations();

    verify(streamBridge).send(eq("orderAutoReceiveProducer-out-0"), any(Message.class));
  }

  @Test
  void autoReceiveDoesNotDispatchOnRollback() {
    OrderAutoReceiveMessageProducer producer = newAutoReceiveProducer();
    TransactionSynchronizationManager.initSynchronization();

    producer.sendAfterCommit(autoReceiveEvent());

    runRollbackSynchronizations();

    verify(streamBridge, never()).send(eq("orderAutoReceiveProducer-out-0"), any(Message.class));
  }

  @Test
  void shippedDispatchesOnlyAfterCommit() {
    when(streamBridge.send(eq("orderShippedProducer-out-0"), any(Message.class))).thenReturn(true);
    OrderShippedMessageProducer producer = new OrderShippedMessageProducer(streamBridge);
    TransactionSynchronizationManager.initSynchronization();
    OrderShippedEvent event =
        OrderShippedEvent.builder().subOrderNo("SO-2001").mainOrderNo("MO-2001").build();

    producer.sendAfterCommit(event);

    verify(streamBridge, never()).send(eq("orderShippedProducer-out-0"), any(Message.class));
    runAfterCommitSynchronizations();

    Message<?> message = captureMessage("orderShippedProducer-out-0");
    assertThat(message.getPayload()).isSameAs(event);
    assertThat(message.getHeaders().get(MessageConst.PROPERTY_KEYS)).isEqualTo("SO-2001");
    assertThat(message.getHeaders().get(MessageConst.PROPERTY_TAGS)).isEqualTo("ORDER_SHIPPED");
    assertThat(message.getHeaders().get("eventId")).isNotNull();
    assertThat(message.getHeaders().get("eventType")).isEqualTo("ORDER_SHIPPED");
  }

  private OrderAutoReceiveMessageProducer newAutoReceiveProducer() {
    OrderAutoReceiveMessageProducer producer = new OrderAutoReceiveMessageProducer(streamBridge);
    ReflectionTestUtils.setField(producer, "delayLevel", 16);
    return producer;
  }

  private OrderAutoReceiveEvent autoReceiveEvent() {
    return OrderAutoReceiveEvent.builder().subOrderNo("SO-1001").mainOrderNo("MO-1001").build();
  }

  private Message<?> captureMessage(String bindingName) {
    ArgumentCaptor<Message<?>> messageCaptor = ArgumentCaptor.forClass(Message.class);
    verify(streamBridge).send(eq(bindingName), messageCaptor.capture());
    return messageCaptor.getValue();
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
