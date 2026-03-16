package com.cloud.user.controller.user;

import com.cloud.common.messaging.event.UserNotificationEvent;
import com.cloud.common.result.Result;
import com.cloud.user.messaging.UserNotificationProducer;
import com.cloud.user.module.dto.UserNotificationBatchRequestDTO;
import com.cloud.user.module.dto.UserNotificationStatusChangeRequestDTO;
import com.cloud.user.module.dto.UserSystemAnnouncementRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/user/notification")
@RequiredArgsConstructor
@Tag(name = "User Notification", description = "User notification management APIs")
public class UserNotificationController {

  private final UserNotificationProducer userNotificationProducer;

  @PostMapping("/welcome/{userId}")
  @PreAuthorize("hasAuthority('admin:all')")
  @Operation(
      summary = "Send welcome notification",
      description = "Send welcome notification to one user")
  public Result<Boolean> sendWelcomeNotification(
      @PathVariable @Parameter(description = "User ID") Long userId) {
    if (userId == null || userId <= 0) {
      return Result.badRequest("user id is invalid");
    }

    try {
      UserNotificationEvent event =
          UserNotificationEvent.builder()
              .eventId(UUID.randomUUID().toString())
              .eventType(UserNotificationEvent.TYPE_WELCOME)
              .userId(userId)
              .timestamp(System.currentTimeMillis())
              .build();
      boolean sent = userNotificationProducer.send(event);
      return Result.success("welcome notification enqueued", sent);
    } catch (Exception e) {
      log.error("Failed to send welcome notification, userId={}", userId, e);
      return Result.error("failed to send welcome notification");
    }
  }

  @PostMapping("/status-change/{userId}")
  @PreAuthorize("hasAuthority('admin:all')")
  @Operation(
      summary = "Send status change notification",
      description = "Send user status change notification")
  public Result<Boolean> sendStatusChangeNotification(
      @PathVariable @Parameter(description = "User ID") Long userId,
      @RequestBody @Valid UserNotificationStatusChangeRequestDTO requestDTO) {
    if (userId == null || userId <= 0) {
      return Result.badRequest("user id is invalid");
    }

    try {
      UserNotificationEvent event =
          UserNotificationEvent.builder()
              .eventId(UUID.randomUUID().toString())
              .eventType(UserNotificationEvent.TYPE_STATUS_CHANGE)
              .userId(userId)
              .newStatus(requestDTO.getNewStatus())
              .reason(requestDTO.getReason())
              .timestamp(System.currentTimeMillis())
              .build();
      boolean sent = userNotificationProducer.send(event);
      return Result.success("status change notification enqueued", sent);
    } catch (Exception e) {
      log.error("Failed to send status change notification, userId={}", userId, e);
      return Result.error("failed to send status change notification");
    }
  }

  @PostMapping("/batch")
  @PreAuthorize("hasAuthority('admin:all')")
  @Operation(
      summary = "Send batch notification",
      description = "Send one notification to multiple users")
  public Result<Boolean> sendBatchNotification(
      @RequestBody @Valid UserNotificationBatchRequestDTO requestDTO) {
    try {
      UserNotificationEvent event =
          UserNotificationEvent.builder()
              .eventId(UUID.randomUUID().toString())
              .eventType(UserNotificationEvent.TYPE_BATCH)
              .userIds(requestDTO.getUserIds())
              .title(requestDTO.getTitle())
              .content(requestDTO.getContent())
              .timestamp(System.currentTimeMillis())
              .build();
      boolean sent = userNotificationProducer.send(event);
      return Result.success("batch notification enqueued", sent);
    } catch (Exception e) {
      log.error("Failed to send batch notification", e);
      return Result.error("failed to send batch notification");
    }
  }

  @PostMapping("/system")
  @PreAuthorize("hasAuthority('admin:all')")
  @Operation(summary = "Send system announcement", description = "Send one system announcement")
  public Result<Boolean> sendSystemAnnouncement(
      @RequestBody @Valid UserSystemAnnouncementRequestDTO requestDTO) {
    try {
      UserNotificationEvent event =
          UserNotificationEvent.builder()
              .eventId(UUID.randomUUID().toString())
              .eventType(UserNotificationEvent.TYPE_SYSTEM)
              .title(requestDTO.getTitle())
              .content(requestDTO.getContent())
              .timestamp(System.currentTimeMillis())
              .build();
      boolean sent = userNotificationProducer.send(event);
      return Result.success("system announcement enqueued", sent);
    } catch (Exception e) {
      log.error("Failed to send system announcement", e);
      return Result.error("failed to send system announcement");
    }
  }
}
