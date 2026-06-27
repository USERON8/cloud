package com.cloud.user.controller.user;

import cn.hutool.core.util.StrUtil;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.domain.dto.user.UserProfileUpsertDTO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;
import com.cloud.common.result.Result;
import com.cloud.common.security.SecurityPermissionUtils;
import com.cloud.user.converter.UserProfileCommandConverter;
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
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
@Tag(name = "用户个人资料", description = "当前用户个人资料接口")
public class UserProfileController {

  private final UserService userService;
  private final MinioService minioService;
  private final UserProfileCommandConverter userProfileCommandConverter;

  @GetMapping("/profile")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "查询当前用户资料", description = "查询当前登录用户资料")
  public Result<UserDTO> getCurrentProfile(Authentication authentication) {
    Long currentUserId = parseCurrentUserId(authentication);
    if (currentUserId == null) {
      throw new BizException(ResultCode.UNAUTHORIZED, "current user is not available");
    }

    return Result.success(userService.getUserById(currentUserId));
  }

  @PutMapping("/profile")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "更新当前用户资料", description = "更新当前用户资料字段")
  public Result<Boolean> updateCurrentProfile(
      @RequestBody @Valid UserProfileUpdateDTO updateDTO, Authentication authentication) {
    Long currentUserId = parseCurrentUserId(authentication);
    if (currentUserId == null) {
      throw new BizException(ResultCode.UNAUTHORIZED, "current user is not available");
    }

    if (StrUtil.isBlank(updateDTO.getNickname())
        && StrUtil.isBlank(updateDTO.getAvatarUrl())
        && StrUtil.isBlank(updateDTO.getEmail())
        && StrUtil.isBlank(updateDTO.getPhone())) {
      throw new BizException(ResultCode.BAD_REQUEST, "at least one profile field is required");
    }

    UserProfileUpsertDTO profileUpsertDTO = userProfileCommandConverter.toUpsertDTO(updateDTO);
    profileUpsertDTO.setId(currentUserId);

    boolean updated = userService.updateProfile(profileUpsertDTO);
    return Result.success("profile updated", updated);
  }

  @PutMapping("/password")
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "修改当前用户密码",
      description = "修改当前登录用户密码")
  public Result<Boolean> changeCurrentPassword(
      @RequestBody @Valid UserProfilePasswordChangeDTO requestDTO, Authentication authentication) {
    Long currentUserId = parseCurrentUserId(authentication);
    if (currentUserId == null) {
      throw new BizException(ResultCode.UNAUTHORIZED, "current user is not available");
    }

    Boolean changed =
        userService.changePassword(
            currentUserId, requestDTO.getOldPassword(), requestDTO.getNewPassword());
    return Result.success("password changed", Boolean.TRUE.equals(changed));
  }

  @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "上传当前用户头像",
      description = "上传当前登录用户头像图片")
  public Result<String> uploadCurrentAvatar(
      @RequestPart("file") MultipartFile file, Authentication authentication) {
    Long currentUserId = parseCurrentUserId(authentication);
    if (currentUserId == null) {
      throw new BizException(ResultCode.UNAUTHORIZED, "current user is not available");
    }

    String avatarUrl = minioService.uploadAvatar(file);
    UserProfileUpsertDTO profileUpsertDTO = new UserProfileUpsertDTO();
    profileUpsertDTO.setId(currentUserId);
    profileUpsertDTO.setAvatarUrl(avatarUrl);
    boolean updated = userService.updateProfile(profileUpsertDTO);
    if (!updated) {
      throw new BizException(ResultCode.BUSINESS_ERROR, "failed to persist avatar url");
    }
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
