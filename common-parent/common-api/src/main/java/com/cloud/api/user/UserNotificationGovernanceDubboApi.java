package com.cloud.api.user;

import com.cloud.common.domain.dto.user.UserNotificationBatchRequestDTO;
import com.cloud.common.domain.dto.user.UserNotificationStatusChangeRequestDTO;
import com.cloud.common.domain.dto.user.UserSystemAnnouncementRequestDTO;

public interface UserNotificationGovernanceDubboApi {

  boolean sendWelcomeNotification(Long userId);

  boolean sendStatusChangeNotification(
      Long userId, UserNotificationStatusChangeRequestDTO requestDTO);

  boolean sendBatchNotification(UserNotificationBatchRequestDTO requestDTO);

  boolean sendSystemAnnouncement(UserSystemAnnouncementRequestDTO requestDTO);
}
