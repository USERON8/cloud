package com.cloud.user.controller.user;

import com.cloud.common.result.Result;
import com.cloud.user.module.dto.UserNotificationBatchRequestDTO;
import com.cloud.user.module.dto.UserNotificationStatusChangeRequestDTO;
import com.cloud.user.module.dto.UserSystemAnnouncementRequestDTO;
import com.cloud.user.service.UserNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

    private final UserNotificationService userNotificationService;

    @PostMapping("/welcome/{userId}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "Send welcome notification", description = "Send welcome notification to one user")
    public Result<Boolean> sendWelcomeNotification(
            @PathVariable
            @Parameter(description = "User ID") Long userId) {
        if (userId == null || userId <= 0) {
            return Result.badRequest("user id is invalid");
        }

        try {
            boolean sent = Boolean.TRUE.equals(userNotificationService.sendWelcomeEmailAsync(userId).join());
            return Result.success("welcome notification sent", sent);
        } catch (Exception e) {
            log.error("Failed to send welcome notification, userId={}", userId, e);
            return Result.error("failed to send welcome notification");
        }
    }

    @PostMapping("/status-change/{userId}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "Send status change notification", description = "Send user status change notification")
    public Result<Boolean> sendStatusChangeNotification(
            @PathVariable
            @Parameter(description = "User ID") Long userId,
            @RequestBody @Valid UserNotificationStatusChangeRequestDTO requestDTO) {
        if (userId == null || userId <= 0) {
            return Result.badRequest("user id is invalid");
        }

        try {
            boolean sent = Boolean.TRUE.equals(
                    userNotificationService.sendStatusChangeNotificationAsync(
                            userId,
                            requestDTO.getNewStatus(),
                            requestDTO.getReason()
                    ).join()
            );
            return Result.success("status change notification sent", sent);
        } catch (Exception e) {
            log.error("Failed to send status change notification, userId={}", userId, e);
            return Result.error("failed to send status change notification");
        }
    }

    @PostMapping("/batch")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "Send batch notification", description = "Send one notification to multiple users")
    public Result<Boolean> sendBatchNotification(
            @RequestBody @Valid UserNotificationBatchRequestDTO requestDTO) {
        try {
            boolean sent = Boolean.TRUE.equals(
                    userNotificationService.sendBatchNotificationAsync(
                            requestDTO.getUserIds(),
                            requestDTO.getTitle(),
                            requestDTO.getContent()
                    ).join()
            );
            return Result.success("batch notification sent", sent);
        } catch (Exception e) {
            log.error("Failed to send batch notification", e);
            return Result.error("failed to send batch notification");
        }
    }

    @PostMapping("/system")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "Send system announcement", description = "Send one system announcement")
    public Result<Boolean> sendSystemAnnouncement(
            @RequestBody @Valid UserSystemAnnouncementRequestDTO requestDTO) {
        try {
            boolean sent = Boolean.TRUE.equals(
                    userNotificationService.sendSystemAnnouncementAsync(
                            requestDTO.getTitle(),
                            requestDTO.getContent()
                    ).join()
            );
            return Result.success("system announcement sent", sent);
        } catch (Exception e) {
            log.error("Failed to send system announcement", e);
            return Result.error("failed to send system announcement");
        }
    }
}
