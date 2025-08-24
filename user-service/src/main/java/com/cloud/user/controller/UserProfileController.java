package com.cloud.user.controller;

import com.cloud.common.domain.Result;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BusinessException;
import com.cloud.user.converter.UserConverter;
import com.cloud.user.module.entity.User;
import com.cloud.user.service.UserLogMessageService;
import com.cloud.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/user/profile")
@RequiredArgsConstructor
@Tag(name = "用户个人中心", description = "用户个人中心接口，用于用户管理自己的信息")
public class UserProfileController {

    private final UserService userService;
    private final UserLogMessageService userLogMessageService;
    private final UserConverter userConverter = UserConverter.INSTANCE;
    private final PasswordEncoder passwordEncoder;

    @PutMapping("/update")
    @Operation(summary = "用户更新个人信息", description = "用户更新自己的个人信息（仅限用户自己操作）")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "用户信息", required = true)
    @ApiResponse(responseCode = "200", description = "更新成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<UserDTO> updateUserProfile(
            @Parameter(description = "用户信息") @Valid @RequestBody UserDTO userDTO,
            @RequestHeader("X-User-ID") String currentUserId) {
        log.info("用户更新个人信息, userId: {}", currentUserId);
        try {
            // 参数验证
            if (userDTO.getId() == null) {
                log.warn("更新用户信息失败: 用户ID不能为空");
                return Result.error(ResultCode.PARAM_ERROR.getCode(), "用户ID不能为空");
            }

            // 验证权限：只能更新自己的信息
            if (!userDTO.getId().toString().equals(currentUserId)) {
                log.warn("更新用户信息失败: 无权限操作其他用户信息, currentUserId: {}, targetUserId: {}", currentUserId, userDTO.getId());
                return Result.error(ResultCode.FORBIDDEN.getCode(), "无权限操作其他用户信息");
            }

            // 检查用户是否存在
            User existingUser = userService.getById(userDTO.getId());
            if (existingUser == null) {
                log.warn("更新用户信息失败: 用户不存在, userId: {}", userDTO.getId());
                return Result.error(ResultCode.USER_NOT_FOUND.getCode(), "用户不存在");
            }

            // 转换并更新用户信息
            User user = userConverter.toEntity(userDTO);
            // 保留原密码
            user.setPassword(existingUser.getPassword());
            // 保留原用户类型
            user.setUserType(existingUser.getUserType());
            // 保留原状态
            user.setStatus(existingUser.getStatus());

            boolean updated = userService.updateById(user);
            if (updated) {
                UserDTO updatedUserDTO = userConverter.toDTO(user);
                log.info("用户更新个人信息成功, userId: {}, username: {}", user.getId(), user.getUsername());
                
                // 发送用户变更消息到日志服务
                userLogMessageService.sendUserChangeMessage(
                        user.getId(), 
                        user.getUsername(), 
                        existingUser.getStatus(), 
                        user.getStatus(), 
                        2, // 更新用户
                        currentUserId);
                
                return Result.success("更新成功", updatedUserDTO);
            } else {
                log.error("用户更新个人信息失败, userId: {}", userDTO.getId());
                return Result.error("更新用户信息失败");
            }
        } catch (BusinessException e) {
            log.error("用户更新个人信息失败: 业务异常, userId: {}", userDTO.getId(), e);
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("用户更新个人信息失败: 系统异常, userId: {}", userDTO.getId(), e);
            return Result.error(ResultCode.SYSTEM_ERROR.getCode(), "更新用户信息失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete")
    @Operation(summary = "用户注销账号", description = "用户注销自己的账号（仅限用户自己操作）")
    @ApiResponse(responseCode = "200", description = "注销成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<String> deleteUserProfile(@RequestHeader("X-User-ID") String currentUserId) {
        log.info("用户注销账号, userId: {}", currentUserId);
        try {
            // 参数验证
            if (currentUserId == null) {
                log.warn("注销账号失败: 用户ID不能为空");
                return Result.error(ResultCode.PARAM_ERROR.getCode(), "用户ID不能为空");
            }

            Long userId = Long.valueOf(currentUserId);

            // 检查用户是否存在
            User existingUser = userService.getById(userId);
            if (existingUser == null) {
                log.warn("注销账号失败: 用户不存在, userId: {}", userId);
                return Result.error(ResultCode.USER_NOT_FOUND.getCode(), "用户不存在");
            }

            // 删除用户
            boolean removed = userService.removeById(userId);
            if (removed) {
                log.info("用户注销账号成功, userId: {}, username: {}", userId, existingUser.getUsername());
                
                // 发送用户变更消息到日志服务
                userLogMessageService.sendUserChangeMessage(
                        userId, 
                        existingUser.getUsername(), 
                        existingUser.getStatus(), 
                        null, 
                        3, // 删除用户
                        currentUserId);
                
                // 清除用户相关缓存
                userService.clearUserCache(existingUser.getUsername(), userId);
                return Result.success("注销成功");
            } else {
                log.error("用户注销账号失败, userId: {}", userId);
                return Result.error("注销账号失败");
            }
        } catch (NumberFormatException e) {
            log.error("注销账号失败: 用户ID格式错误, userId: {}", currentUserId, e);
            return Result.error(ResultCode.PARAM_ERROR.getCode(), "用户ID格式错误");
        } catch (BusinessException e) {
            log.error("注销账号失败: 业务异常, userId: {}", currentUserId, e);
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("注销账号失败: 系统异常, userId: {}", currentUserId, e);
            return Result.error(ResultCode.SYSTEM_ERROR.getCode(), "注销账号失败: " + e.getMessage());
        }
    }

    @PostMapping("/change-password")
    @Operation(summary = "用户修改密码", description = "用户修改自己的密码（仅限用户自己操作）")
    @Parameters({
            @Parameter(name = "oldPassword", description = "原密码", required = true),
            @Parameter(name = "newPassword", description = "新密码", required = true)
    })
    @ApiResponse(responseCode = "200", description = "密码修改成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<String> changePassword(
            @RequestParam String oldPassword,
            @RequestParam String newPassword,
            @RequestHeader("X-User-ID") String currentUserId) {
        log.info("用户修改密码, userId: {}", currentUserId);
        try {
            // 参数验证
            if (currentUserId == null) {
                log.warn("修改密码失败: 用户ID不能为空");
                return Result.error(ResultCode.PARAM_ERROR.getCode(), "用户ID不能为空");
            }

            if (oldPassword == null || oldPassword.isEmpty()) {
                log.warn("修改密码失败: 原密码不能为空");
                return Result.error(ResultCode.PARAM_ERROR.getCode(), "原密码不能为空");
            }

            if (newPassword == null || newPassword.isEmpty()) {
                log.warn("修改密码失败: 新密码不能为空");
                return Result.error(ResultCode.PARAM_ERROR.getCode(), "新密码不能为空");
            }

            Long userId = Long.valueOf(currentUserId);

            // 检查用户是否存在
            User existingUser = userService.getById(userId);
            if (existingUser == null) {
                log.warn("修改密码失败: 用户不存在, userId: {}", userId);
                return Result.error(ResultCode.USER_NOT_FOUND.getCode(), "用户不存在");
            }

            // 验证原密码
            if (!passwordEncoder.matches(oldPassword, existingUser.getPassword())) {
                log.warn("修改密码失败: 原密码错误, userId: {}", userId);
                return Result.error(ResultCode.PARAM_ERROR.getCode(), "原密码错误");
            }

            // 更新用户密码
            existingUser.setPassword(passwordEncoder.encode(newPassword));
            boolean updated = userService.updateById(existingUser);
            if (updated) {
                log.info("用户修改密码成功, userId: {}, username: {}", userId, existingUser.getUsername());
                
                // 发送用户变更消息到日志服务
                userLogMessageService.sendUserChangeMessage(
                        userId, 
                        existingUser.getUsername(), 
                        existingUser.getStatus(), 
                        existingUser.getStatus(), 
                        2, // 更新用户
                        currentUserId);
                
                // 清除用户相关缓存
                userService.clearUserCache(existingUser.getUsername(), userId);
                return Result.success("密码修改成功");
            } else {
                log.error("用户修改密码失败, userId: {}", userId);
                return Result.error("密码修改失败");
            }
        } catch (NumberFormatException e) {
            log.error("修改密码失败: 用户ID格式错误, userId: {}", currentUserId, e);
            return Result.error(ResultCode.PARAM_ERROR.getCode(), "用户ID格式错误");
        } catch (BusinessException e) {
            log.error("修改密码失败: 业务异常, userId: {}", currentUserId, e);
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("修改密码失败: 系统异常, userId: {}", currentUserId, e);
            return Result.error(ResultCode.SYSTEM_ERROR.getCode(), "密码修改失败: " + e.getMessage());
        }
    }
}