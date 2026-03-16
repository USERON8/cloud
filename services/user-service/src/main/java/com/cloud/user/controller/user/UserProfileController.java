package com.cloud.user.controller.user;

import cn.hutool.core.util.StrUtil;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.domain.dto.user.UserProfileUpsertDTO;
import com.cloud.common.result.Result;
import com.cloud.common.security.SecurityPermissionUtils;
import com.cloud.user.module.dto.UserProfilePasswordChangeDTO;
import com.cloud.user.module.dto.UserProfileUpdateDTO;
import com.cloud.user.service.MinioService;
import com.cloud.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/user/profile")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "Current user profile APIs")
public class UserProfileController {

  private final UserService userService;
  private final MinioService minioService;

  @GetMapping("/current")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get current profile", description = "Get current logged-in user profile")
  public Result<UserDTO> getCurrentProfile(Authentication authentication) {
    Long currentUserId = parseCurrentUserId(authentication);
    if (currentUserId == null) {
      return Result.unauthorized("current user is not available");
    }

    return Result.success(userService.getUserById(currentUserId));
  }

  @PutMapping("/current")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Update current profile", description = "Update current user profile fields")
  public Result<Boolean> updateCurrentProfile(
      @RequestBody @Valid UserProfileUpdateDTO updateDTO, Authentication authentication) {
    Long currentUserId = parseCurrentUserId(authentication);
    if (currentUserId == null) {
      return Result.unauthorized("current user is not available");
    }

    if (StrUtil.isBlank(updateDTO.getNickname())
        && StrUtil.isBlank(updateDTO.getAvatarUrl())
        && StrUtil.isBlank(updateDTO.getEmail())
        && StrUtil.isBlank(updateDTO.getPhone())) {
      return Result.badRequest("at least one profile field is required");
    }

    UserProfileUpsertDTO profileUpsertDTO = new UserProfileUpsertDTO();
    profileUpsertDTO.setId(currentUserId);
    if (StrUtil.isNotBlank(updateDTO.getNickname())) {
      profileUpsertDTO.setNickname(updateDTO.getNickname());
    }
    if (StrUtil.isNotBlank(updateDTO.getAvatarUrl())) {
      profileUpsertDTO.setAvatarUrl(updateDTO.getAvatarUrl());
    }
    if (StrUtil.isNotBlank(updateDTO.getEmail())) {
      profileUpsertDTO.setEmail(updateDTO.getEmail());
    }
    if (StrUtil.isNotBlank(updateDTO.getPhone())) {
      profileUpsertDTO.setPhone(updateDTO.getPhone());
    }

    boolean updated = userService.updateProfile(profileUpsertDTO);
    return Result.success("profile updated", updated);
  }

  @PutMapping("/current/password")
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "Change current password",
      description = "Change current logged-in user password")
  public Result<Boolean> changeCurrentPassword(
      @RequestBody @Valid UserProfilePasswordChangeDTO requestDTO, Authentication authentication) {
    Long currentUserId = parseCurrentUserId(authentication);
    if (currentUserId == null) {
      return Result.unauthorized("current user is not available");
    }

    Boolean changed =
        userService.changePassword(
            currentUserId, requestDTO.getOldPassword(), requestDTO.getNewPassword());
    return Result.success("password changed", Boolean.TRUE.equals(changed));
  }

  @PostMapping(value = "/current/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "Upload current avatar",
      description = "Upload avatar image for current logged-in user")
  public Result<String> uploadCurrentAvatar(
      @RequestPart("file") MultipartFile file, Authentication authentication) {
    Long currentUserId = parseCurrentUserId(authentication);
    if (currentUserId == null) {
      return Result.unauthorized("current user is not available");
    }

    String avatarUrl = minioService.uploadAvatar(file);
    return Result.success("avatar uploaded", avatarUrl);
  }

  private Long parseCurrentUserId(Authentication authentication) {
    String currentUserId = SecurityPermissionUtils.getCurrentUserId(authentication);
    if (StrUtil.isBlank(currentUserId)) {
      return null;
    }
    if (!StrUtil.isNumeric(currentUserId)) {
      return null;
    }
    return Long.parseLong(currentUserId);
  }
}
