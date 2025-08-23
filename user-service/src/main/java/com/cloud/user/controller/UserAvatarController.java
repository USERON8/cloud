package com.cloud.user.controller;

import com.cloud.common.domain.Result;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BusinessException;
import com.cloud.user.service.UserAvatarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("user/avatar")
@RequiredArgsConstructor
@Tag(name = "用户头像", description = "用户头像管理接口")
public class UserAvatarController {

    private final UserAvatarService userAvatarService;

    /**
     * 上传用户头像
     *
     * @param file 头像文件
     * @return 上传结果
     */
    @PostMapping("/upload")
    @Operation(summary = "上传头像", description = "上传用户头像文件")
    @Parameters({
            @Parameter(name = "file", description = "头像文件", required = true),
            @Parameter(name = "X-User-ID", description = "当前用户ID", required = true)
    })
    @ApiResponse(responseCode = "200", description = "上传成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file,
                                       @RequestHeader("X-User-ID") String currentUserId) {
        try {
            log.info("开始上传用户头像, 用户ID: {}", currentUserId);

            // 参数验证
            if (currentUserId == null || currentUserId.isEmpty()) {
                log.warn("上传用户头像失败: 用户ID为空");
                return Result.error(ResultCode.PARAM_ERROR.getCode(), "用户ID不能为空");
            }

            Long userId = Long.valueOf(currentUserId);

            String avatarUrl = userAvatarService.uploadAvatar(file, userId);
            log.info("上传用户头像成功, 用户ID: {}, 头像URL: {}", currentUserId, avatarUrl);
            return Result.success("上传成功", avatarUrl);
        } catch (NumberFormatException e) {
            log.error("上传用户头像失败: 用户ID格式错误, 用户ID: {}", currentUserId, e);
            return Result.error(ResultCode.PARAM_ERROR.getCode(), "用户ID格式错误");
        } catch (MaxUploadSizeExceededException e) {
            log.error("上传用户头像失败: 文件大小超出限制, 用户ID: {}", currentUserId, e);
            return Result.error(ResultCode.FILE_SIZE_EXCEEDED.getCode(), "文件大小超出限制");
        } catch (BusinessException e) {
            log.error("上传用户头像失败: 业务异常, 用户ID: {}", currentUserId, e);
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("上传用户头像失败: 系统异常, 用户ID: {}", currentUserId, e);
            return Result.error(ResultCode.SYSTEM_ERROR.getCode(), "上传头像失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户头像
     *
     * @param currentUserId 当前用户ID
     * @return 头像URL
     */
    @GetMapping("/get")
    @Operation(summary = "获取头像", description = "获取当前用户头像URL")
    @Parameters({
            @Parameter(name = "X-User-ID", description = "当前用户ID", required = true)
    })
    @ApiResponse(responseCode = "200", description = "获取成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<String> getAvatar(@RequestHeader("X-User-ID") String currentUserId) {
        try {
            log.info("开始获取用户头像URL, 用户ID: {}", currentUserId);

            // 参数验证
            if (currentUserId == null || currentUserId.isEmpty()) {
                log.warn("获取用户头像URL失败: 用户ID为空");
                return Result.error(ResultCode.PARAM_ERROR.getCode(), "用户ID不能为空");
            }

            Long userId = Long.valueOf(currentUserId);

            String avatarUrl = userAvatarService.getAvatar(userId);
            log.info("获取用户头像URL成功, 用户ID: {}, 头像URL: {}", currentUserId, avatarUrl);
            return Result.success(avatarUrl);
        } catch (NumberFormatException e) {
            log.error("获取用户头像URL失败: 用户ID格式错误, 用户ID: {}", currentUserId, e);
            return Result.error(ResultCode.PARAM_ERROR.getCode(), "用户ID格式错误");
        } catch (BusinessException e) {
            log.error("获取用户头像URL失败: 业务异常, 用户ID: {}", currentUserId, e);
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("获取用户头像URL失败: 系统异常, 用户ID: {}", currentUserId, e);
            return Result.error(ResultCode.SYSTEM_ERROR.getCode(), "获取头像失败: " + e.getMessage());
        }
    }
}