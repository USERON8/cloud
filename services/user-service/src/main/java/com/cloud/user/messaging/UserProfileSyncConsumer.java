package com.cloud.user.messaging;

import com.cloud.common.domain.dto.user.UserProfileUpsertDTO;
import com.cloud.common.messaging.consumer.AbstractJsonMqConsumer;
import com.cloud.common.messaging.event.UserProfileSyncEvent;
import com.cloud.common.metrics.TradeMetrics;
import com.cloud.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
    topic = "user-profile-sync",
    consumerGroup = "user-profile-sync-consumer-group")
public class UserProfileSyncConsumer extends AbstractJsonMqConsumer<UserProfileSyncEvent> {

  private static final String NS_USER_PROFILE_SYNC = "user:profile:sync";

  private final UserService userService;
  private final TradeMetrics tradeMetrics;

  @Override
  protected void doConsume(UserProfileSyncEvent event, MessageExt msgExt) {
    if (event == null || event.getUserId() == null) {
      tradeMetrics.incrementMessageConsume("user_profile_sync", "failed");
      return;
    }
    UserProfileUpsertDTO command = new UserProfileUpsertDTO();
    command.setId(event.getUserId());
    command.setUsername(event.getUsername());
    command.setPhone(event.getPhone());
    command.setNickname(event.getNickname());
    command.setEmail(event.getEmail());
    command.setAvatarUrl(event.getAvatarUrl());
    command.setStatus(event.getStatus());
    userService.createProfile(command);
  }

  @Override
  protected Class<UserProfileSyncEvent> payloadClass() {
    return UserProfileSyncEvent.class;
  }

  @Override
  protected String payloadDescription() {
    return "UserProfileSyncEvent";
  }

  @Override
  protected String resolveIdempotentNamespace(
      String topic, MessageExt msgExt, UserProfileSyncEvent payload) {
    return NS_USER_PROFILE_SYNC;
  }

  @Override
  protected String buildIdempotentKey(
      String topic, String msgId, UserProfileSyncEvent payload, MessageExt msgExt) {
    return resolveEventId(
        "USER_PROFILE_SYNC",
        payload == null ? null : payload.getEventId(),
        payload == null ? null : String.valueOf(payload.getUserId()));
  }

  @Override
  protected void onConsumeSuccess(MessageExt msgExt, UserProfileSyncEvent payload) {
    tradeMetrics.incrementMessageConsume("user_profile_sync", "success");
  }

  @Override
  protected void onBizException(
      MessageExt msgExt, UserProfileSyncEvent payload, com.cloud.common.exception.BizException ex) {
    tradeMetrics.incrementMessageConsume("user_profile_sync", "biz");
  }

  @Override
  protected void onSystemException(
      MessageExt msgExt,
      UserProfileSyncEvent payload,
      com.cloud.common.exception.SystemException ex,
      boolean retryable) {
    tradeMetrics.incrementMessageConsume("user_profile_sync", retryable ? "retry" : "failed");
  }

  @Override
  protected void onUnknownException(MessageExt msgExt, UserProfileSyncEvent payload, Exception ex) {
    tradeMetrics.incrementMessageConsume("user_profile_sync", "retry");
  }
}
