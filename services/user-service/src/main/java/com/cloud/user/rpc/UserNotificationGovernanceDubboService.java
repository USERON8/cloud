package com.cloud.user.rpc;

import com.cloud.api.user.UserNotificationGovernanceDubboApi;
import com.cloud.common.domain.dto.user.UserNotificationBatchRequestDTO;
import com.cloud.common.domain.dto.user.UserNotificationStatusChangeRequestDTO;
import com.cloud.common.domain.dto.user.UserSystemAnnouncementRequestDTO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;
import com.cloud.common.messaging.event.UserNotificationEvent;
import com.cloud.user.messaging.UserNotificationProducer;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService
@RequiredArgsConstructor
public class UserNotificationGovernanceDubboService implements UserNotificationGovernanceDubboApi {

  private final UserNotificationProducer userNotificationProducer;

  @Override
  public boolean sendWelcomeNotification(Long userId) {
    validateUserId(userId);
    UserNotificationEvent event =
        UserNotificationEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(UserNotificationEvent.TYPE_WELCOME)
            .userId(userId)
            .timestamp(System.currentTimeMillis())
            .build();
    return userNotificationProducer.send(event);
  }

  @Override
  public boolean sendStatusChangeNotification(
      Long userId, UserNotificationStatusChangeRequestDTO requestDTO) {
    validateUserId(userId);
    if (requestDTO == null || requestDTO.getNewStatus() == null) {
      throw new BizException(ResultCode.BAD_REQUEST, "new status is required");
    }
    UserNotificationEvent event =
        UserNotificationEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(UserNotificationEvent.TYPE_STATUS_CHANGE)
            .userId(userId)
            .newStatus(requestDTO.getNewStatus())
            .reason(requestDTO.getReason())
            .timestamp(System.currentTimeMillis())
            .build();
    return userNotificationProducer.send(event);
  }

  @Override
  public boolean sendBatchNotification(UserNotificationBatchRequestDTO requestDTO) {
    if (requestDTO == null
        || requestDTO.getUserIds() == null
        || requestDTO.getUserIds().isEmpty()
        || requestDTO.getTitle() == null
        || requestDTO.getTitle().isBlank()
        || requestDTO.getContent() == null
        || requestDTO.getContent().isBlank()) {
      throw new BizException(ResultCode.BAD_REQUEST, "batch notification request is invalid");
    }
    UserNotificationEvent event =
        UserNotificationEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(UserNotificationEvent.TYPE_BATCH)
            .userIds(requestDTO.getUserIds())
            .title(requestDTO.getTitle())
            .content(requestDTO.getContent())
            .timestamp(System.currentTimeMillis())
            .build();
    return userNotificationProducer.send(event);
  }

  @Override
  public boolean sendSystemAnnouncement(UserSystemAnnouncementRequestDTO requestDTO) {
    if (requestDTO == null
        || requestDTO.getTitle() == null
        || requestDTO.getTitle().isBlank()
        || requestDTO.getContent() == null
        || requestDTO.getContent().isBlank()) {
      throw new BizException(ResultCode.BAD_REQUEST, "system announcement request is invalid");
    }
    UserNotificationEvent event =
        UserNotificationEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(UserNotificationEvent.TYPE_SYSTEM)
            .title(requestDTO.getTitle())
            .content(requestDTO.getContent())
            .timestamp(System.currentTimeMillis())
            .build();
    return userNotificationProducer.send(event);
  }

  private void validateUserId(Long userId) {
    if (userId == null || userId <= 0) {
      throw new BizException(ResultCode.BAD_REQUEST, "user id is invalid");
    }
  }
}
