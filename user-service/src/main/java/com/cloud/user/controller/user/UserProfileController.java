package com.cloud.user.controller.user;

import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.result.Result;
import com.cloud.common.security.SecurityPermissionUtils;
import com.cloud.user.module.dto.UserProfilePasswordChangeDTO;
import com.cloud.user.module.dto.UserProfileUpdateDTO;
import com.cloud.user.module.entity.User;
import com.cloud.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/user/profile")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "Current user profile APIs")
public class UserProfileController {

    private final UserService userService;

    @GetMapping("/current")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current profile", description = "Get current logged-in user profile")
    public Result<UserDTO> getCurrentProfile(Authentication authentication) {
        Long currentUserId = parseCurrentUserId(authentication);
        if (currentUserId == null) {
            return Result.unauthorized("current user is not available");
        }

        try {
            return Result.success(userService.getUserById(currentUserId));
        } catch (Exception e) {
            log.error("Failed to query current user profile, userId={}", currentUserId, e);
            return Result.error("failed to query current profile");
        }
    }

    @PutMapping("/current")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update current profile", description = "Update current user profile fields")
    public Result<Boolean> updateCurrentProfile(
            @RequestBody @Valid UserProfileUpdateDTO updateDTO,
            Authentication authentication) {
        Long currentUserId = parseCurrentUserId(authentication);
        if (currentUserId == null) {
            return Result.unauthorized("current user is not available");
        }

        if (!StringUtils.hasText(updateDTO.getNickname())
                && !StringUtils.hasText(updateDTO.getAvatarUrl())
                && !StringUtils.hasText(updateDTO.getEmail())
                && !StringUtils.hasText(updateDTO.getPhone())) {
            return Result.badRequest("at least one profile field is required");
        }

        User updateEntity = new User();
        updateEntity.setId(currentUserId);
        if (StringUtils.hasText(updateDTO.getNickname())) {
            updateEntity.setNickname(updateDTO.getNickname());
        }
        if (StringUtils.hasText(updateDTO.getAvatarUrl())) {
            updateEntity.setAvatarUrl(updateDTO.getAvatarUrl());
        }
        if (StringUtils.hasText(updateDTO.getEmail())) {
            updateEntity.setEmail(updateDTO.getEmail());
        }
        if (StringUtils.hasText(updateDTO.getPhone())) {
            updateEntity.setPhone(updateDTO.getPhone());
        }

        try {
            boolean updated = userService.updateById(updateEntity);
            return Result.success("profile updated", updated);
        } catch (Exception e) {
            log.error("Failed to update current user profile, userId={}", currentUserId, e);
            return Result.error("failed to update current profile");
        }
    }

    @PutMapping("/current/password")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Change current password", description = "Change current logged-in user password")
    public Result<Boolean> changeCurrentPassword(
            @RequestBody @Valid UserProfilePasswordChangeDTO requestDTO,
            Authentication authentication) {
        Long currentUserId = parseCurrentUserId(authentication);
        if (currentUserId == null) {
            return Result.unauthorized("current user is not available");
        }

        try {
            Boolean changed = userService.changePassword(currentUserId, requestDTO.getOldPassword(), requestDTO.getNewPassword());
            return Result.success("password changed", Boolean.TRUE.equals(changed));
        } catch (Exception e) {
            log.error("Failed to change current user password, userId={}", currentUserId, e);
            return Result.error("failed to change password: " + e.getMessage());
        }
    }

    private Long parseCurrentUserId(Authentication authentication) {
        String currentUserId = SecurityPermissionUtils.getCurrentUserId(authentication);
        if (!StringUtils.hasText(currentUserId)) {
            return null;
        }
        try {
            return Long.parseLong(currentUserId);
        } catch (NumberFormatException e) {
            log.warn("Invalid user_id claim value: {}", currentUserId);
            return null;
        }
    }
}
