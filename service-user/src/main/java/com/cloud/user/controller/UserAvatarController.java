package com.cloud.user.controller;

import com.cloud.common.domain.Result;
import com.cloud.user.service.UserAvatarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/users")
@Tag(name = "用户头像", description = "用户头像上传和管理")
public class UserAvatarController {

    private final UserAvatarService userAvatarService;

    public UserAvatarController(UserAvatarService userAvatarService) {
        this.userAvatarService = userAvatarService;
    }

    /**
     * 上传用户头像
     *
     * @param file 头像文件
     * @return 上传结果和文件URL
     */
    @PostMapping("/avatar")
    @Operation(summary = "上传用户头像", description = "上传并设置当前用户的头像")
    @ApiResponse(responseCode = "200", description = "上传成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, String>> uploadAvatar(
            @Parameter(description = "头像文件") @RequestParam("file") MultipartFile file) {
        log.info("上传用户头像, file: {}", file.getOriginalFilename());

        if (file.isEmpty()) {
            log.warn("上传的文件为空");
            return Result.error("文件不能为空");
        }

        String fileUrl = userAvatarService.uploadAvatar(file);
        log.info("头像上传成功, url: {}", fileUrl);
        return Result.success("上传成功", Map.of("url", fileUrl));
    }

    /**
     * 获取用户头像
     *
     * @param userId 用户ID
     * @return 头像URL
     */
    @GetMapping("/avatar/{userId}")
    @Operation(summary = "获取用户头像", description = "根据用户ID获取用户头像URL")
    @ApiResponse(responseCode = "200", description = "获取成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<Map<String, String>> getAvatar(
            @Parameter(description = "用户ID") @PathVariable("userId") Long userId) {
        log.info("获取用户头像, userId: {}", userId);

        String fileUrl = userAvatarService.getAvatar(userId);
        log.info("获取用户头像成功, userId: {}, url: {}", userId, fileUrl);
        return Result.success(Map.of("url", fileUrl));
    }

    /**
     * 删除用户头像
     *
     * @param userId 用户ID
     * @return 删除结果
     */
    @DeleteMapping("/avatar/{userId}")
    @Operation(summary = "删除用户头像", description = "删除指定用户的头像")
    @ApiResponse(responseCode = "200", description = "删除成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    @PreAuthorize("@permissionService.hasPermission(#userId) or hasAuthority('ROLE_ADMIN')")
    public Result<?> deleteAvatar(
            @Parameter(description = "用户ID") @PathVariable("userId") Long userId) {
        log.info("删除用户头像, userId: {}", userId);

        userAvatarService.deleteAvatar(userId);
        log.info("用户头像删除成功, userId: {}", userId);
        return Result.success("删除成功");
    }
}