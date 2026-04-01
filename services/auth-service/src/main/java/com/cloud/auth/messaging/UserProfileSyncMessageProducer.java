package com.cloud.auth.messaging;

import com.cloud.common.messaging.event.UserProfileSyncEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserProfileSyncMessageProducer {

  private static final String BINDING_NAME = "userProfileSyncProducer-out-0";

  private final ApplicationEventPublisher applicationEventPublisher;
  private final StreamBridge streamBridge;

  public void sendAfterCommit(UserProfileSyncEvent event) {
    if (event == null) {
      return;
    }
    applicationEventPublisher.publishEvent(event);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void onUserProfileSyncEvent(UserProfileSyncEvent event) {
    Message<UserProfileSyncEvent> message =
        MessageBuilder.withPayload(event)
            .setHeader("eventType", event.getEventType())
            .setHeader("eventId", event.getEventId())
            .build();
    boolean sent = streamBridge.send(BINDING_NAME, message);
    if (!sent) {
      throw new IllegalStateException(
          "failed to enqueue user profile sync event: eventId=" + event.getEventId());
    }
    log.info(
        "User profile sync event enqueued: eventId={}, userId={}, eventType={}",
        event.getEventId(),
        event.getUserId(),
        event.getEventType());
  }
}
