package com.cloud.api.user;

import com.cloud.common.domain.dto.user.UserNotificationBatchRequestDTO;
import com.cloud.common.domain.dto.user.UserNotificationStatusChangeRequestDTO;
import com.cloud.common.domain.dto.user.UserSystemAnnouncementRequestDTO;

/**
 * 用户通知治理内部调用契约。
 *
 * <p>治理服务通过该接口触达用户通知能力，用户服务负责具体通知模板和投递策略。
 */
public interface UserNotificationGovernanceDubboApi {

  /** 发送欢迎通知。 */
  boolean sendWelcomeNotification(Long userId);

  /** 发送用户状态变更通知。 */
  boolean sendStatusChangeNotification(
      Long userId, UserNotificationStatusChangeRequestDTO requestDTO);

  /** 批量发送用户通知。 */
  boolean sendBatchNotification(UserNotificationBatchRequestDTO requestDTO);

  /** 发送系统公告。 */
  boolean sendSystemAnnouncement(UserSystemAnnouncementRequestDTO requestDTO);
}
