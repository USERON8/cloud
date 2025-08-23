package com.cloud.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cloud.common.domain.Result;
import com.cloud.common.domain.dto.auth.RegisterRequestDTO;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户管理接口")
public class UserManageController {

    private final UserService userService;
    private final UserLogMessageService userLogMessageService;
    private final UserConverter userConverter = UserConverter.INSTANCE;
    private final PasswordEncoder passwordEncoder;


    @PostMapping("/create/user")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "创建用户", description = "管理员创建新用户")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "用户注册信息", required = true)
    @ApiResponse(responseCode = "200", description = "创建成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<UserDTO> createUser(@Parameter(description = "用户注册信息") @Valid @RequestBody RegisterRequestDTO registerRequest) {
        log.info("管理员创建用户, username: {}", registerRequest.getUsername());
        try {
            // 检查用户名是否已存在
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("username", registerRequest.getUsername());
            User existingUser = userService.getOne(queryWrapper);
            if (existingUser != null) {
                log.warn("创建用户失败: 用户名已存在, username: {}", registerRequest.getUsername());
                return Result.error(ResultCode.USER_ALREADY_EXISTS.getCode(), "用户名已存在");
            }

            // 转换并设置默认值
            User user = userConverter.toEntity(registerRequest);
            user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
            if (user.getUserType() == null || user.getUserType().isEmpty()) {
                user.setUserType("USER");
            }
            user.setStatus(1); // 默认启用状态

            // 保存用户
            boolean saved = userService.save(user);
            if (saved) {
                UserDTO userDTO = userConverter.toDTO(user);
                log.info("管理员创建用户成功, userId: {}, username: {}", user.getId(), user.getUsername());
                
                // 发送用户变更消息到日志服务
                userLogMessageService.sendUserChangeMessage(
                        user.getId(), 
                        user.getUsername(), 
                        null, 
                        user.getStatus(), 
                        1, // 创建用户
                        "ADMIN");
                
                return Result.success("创建成功", userDTO);
            } else {
                log.error("管理员创建用户失败, username: {}", registerRequest.getUsername());
                return Result.error("创建用户失败");
            }
        } catch (BusinessException e) {
            log.error("管理员创建用户失败: 业务异常, username: {}", registerRequest.getUsername(), e);
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("管理员创建用户失败: 系统异常, username: {}", registerRequest.getUsername(), e);
            return Result.error(ResultCode.SYSTEM_ERROR.getCode(), "创建用户失败: " + e.getMessage());
        }
    }

    @PutMapping("/update/user")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "更新用户", description = "管理员更新用户信息")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "用户信息", required = true)
    @ApiResponse(responseCode = "200", description = "更新成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<UserDTO> updateUser(@Parameter(description = "用户信息") @Valid @RequestBody UserDTO userDTO,
                                      @RequestHeader(value = "X-User-ID", required = false) String currentUserId) {
        log.info("管理员更新用户, userId: {}", userDTO.getId());
        try {
            // 参数验证
            if (userDTO.getId() == null) {
                log.warn("更新用户失败: 用户ID不能为空");
                return Result.error(ResultCode.PARAM_ERROR.getCode(), "用户ID不能为空");
            }

            // 检查用户是否存在
            User existingUser = userService.getById(userDTO.getId());
            if (existingUser == null) {
                log.warn("更新用户失败: 用户不存在, userId: {}", userDTO.getId());
                return Result.error(ResultCode.USER_NOT_FOUND.getCode(), "用户不存在");
            }

            // 转换并更新用户信息
            User user = userConverter.toEntity(userDTO);
            // 保留原密码
            user.setPassword(existingUser.getPassword());
            // 保留原用户类型
            user.setUserType(existingUser.getUserType());

            boolean updated = userService.updateById(user);
            if (updated) {
                UserDTO updatedUserDTO = userConverter.toDTO(user);
                log.info("管理员更新用户成功, userId: {}, username: {}", user.getId(), user.getUsername());
                
                // 发送用户变更消息到日志服务
                userLogMessageService.sendUserChangeMessage(
                        user.getId(), 
                        user.getUsername(), 
                        existingUser.getStatus(), 
                        user.getStatus(), 
                        2, // 更新用户
                        currentUserId != null ? currentUserId : "ADMIN");
                
                return Result.success("更新成功", updatedUserDTO);
            } else {
                log.error("管理员更新用户失败, userId: {}", userDTO.getId());
                return Result.error("更新用户失败");
            }
        } catch (BusinessException e) {
            log.error("管理员更新用户失败: 业务异常, userId: {}", userDTO.getId(), e);
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("管理员更新用户失败: 系统异常, userId: {}", userDTO.getId(), e);
            return Result.error(ResultCode.SYSTEM_ERROR.getCode(), "更新用户失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete/user/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "删除用户", description = "管理员删除用户")
    @Parameters({
            @Parameter(name = "id", description = "用户ID", required = true)
    })
    @ApiResponse(responseCode = "200", description = "删除成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<String> deleteUser(@PathVariable Long id,
                                     @RequestHeader(value = "X-User-ID", required = false) String currentUserId) {
        log.info("管理员删除用户, userId: {}", id);
        try {
            // 参数验证
            if (id == null) {
                log.warn("删除用户失败: 用户ID不能为空");
                return Result.error(ResultCode.PARAM_ERROR.getCode(), "用户ID不能为空");
            }

            // 检查用户是否存在
            User existingUser = userService.getById(id);
            if (existingUser == null) {
                log.warn("删除用户失败: 用户不存在, userId: {}", id);
                return Result.error(ResultCode.USER_NOT_FOUND.getCode(), "用户不存在");
            }

            // 删除用户
            boolean removed = userService.removeById(id);
            if (removed) {
                log.info("管理员删除用户成功, userId: {}, username: {}", id, existingUser.getUsername());
                
                // 发送用户变更消息到日志服务
                userLogMessageService.sendUserChangeMessage(
                        id, 
                        existingUser.getUsername(), 
                        existingUser.getStatus(), 
                        null, 
                        3, // 删除用户
                        currentUserId != null ? currentUserId : "ADMIN");
                
                return Result.success("删除成功");
            } else {
                log.error("管理员删除用户失败, userId: {}", id);
                return Result.error("删除用户失败");
            }
        } catch (BusinessException e) {
            log.error("管理员删除用户失败: 业务异常, userId: {}", id, e);
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("管理员删除用户失败: 系统异常, userId: {}", id, e);
            return Result.error(ResultCode.SYSTEM_ERROR.getCode(), "删除用户失败: " + e.getMessage());
        }
    }

    @PostMapping("/change-status/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "更改用户状态", description = "管理员更改用户状态（启用/禁用）")
    @Parameters({
            @Parameter(name = "id", description = "用户ID", required = true),
            @Parameter(name = "status", description = "状态（0-禁用，1-启用）", required = true)
    })
    @ApiResponse(responseCode = "200", description = "状态更改成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<String> changeUserStatus(@PathVariable Long id, @RequestParam Integer status,
                                           @RequestHeader(value = "X-User-ID", required = false) String currentUserId) {
        log.info("管理员更改用户状态, userId: {}, status: {}", id, status);
        try {
            // 参数验证
            if (id == null) {
                log.warn("更改用户状态失败: 用户ID不能为空");
                return Result.error(ResultCode.PARAM_ERROR.getCode(), "用户ID不能为空");
            }

            if (status == null || (status != 0 && status != 1)) {
                log.warn("更改用户状态失败: 状态值无效, status: {}", status);
                return Result.error(ResultCode.PARAM_ERROR.getCode(), "状态值无效，应为0（禁用）或1（启用）");
            }

            // 检查用户是否存在
            User existingUser = userService.getById(id);
            if (existingUser == null) {
                log.warn("更改用户状态失败: 用户不存在, userId: {}", id);
                return Result.error(ResultCode.USER_NOT_FOUND.getCode(), "用户不存在");
            }

            // 更新用户状态
            existingUser.setStatus(status);
            boolean updated = userService.updateById(existingUser);
            if (updated) {
                log.info("管理员更改用户状态成功, userId: {}, username: {}, status: {}", id, existingUser.getUsername(), status);
                String statusText = status == 1 ? "启用" : "禁用";
                
                // 发送用户变更消息到日志服务
                userLogMessageService.sendUserChangeMessage(
                        id, 
                        existingUser.getUsername(), 
                        existingUser.getStatus() == 1 ? 1 : 0, // 原状态
                        status, // 新状态
                        4, // 状态变更
                        currentUserId != null ? currentUserId : "ADMIN");
                
                return Result.success("用户已" + statusText);
            } else {
                log.error("管理员更改用户状态失败, userId: {}", id);
                return Result.error("更改用户状态失败");
            }
        } catch (BusinessException e) {
            log.error("管理员更改用户状态失败: 业务异常, userId: {}", id, e);
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("管理员更改用户状态失败: 系统异常, userId: {}", id, e);
            return Result.error(ResultCode.SYSTEM_ERROR.getCode(), "更改用户状态失败: " + e.getMessage());
        }
    }

    @PostMapping("/reset-password/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "重置用户密码", description = "管理员重置用户密码")
    @Parameters({
            @Parameter(name = "id", description = "用户ID", required = true),
            @Parameter(name = "newPassword", description = "新密码", required = true)
    })
    @ApiResponse(responseCode = "200", description = "密码重置成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<String> resetUserPassword(@PathVariable Long id, @RequestParam String newPassword,
                                            @RequestHeader(value = "X-User-ID", required = false) String currentUserId) {
        log.info("管理员重置用户密码, userId: {}", id);
        try {
            // 参数验证
            if (id == null) {
                log.warn("重置用户密码失败: 用户ID不能为空");
                return Result.error(ResultCode.PARAM_ERROR.getCode(), "用户ID不能为空");
            }

            if (newPassword == null || newPassword.isEmpty()) {
                log.warn("重置用户密码失败: 新密码不能为空");
                return Result.error(ResultCode.PARAM_ERROR.getCode(), "新密码不能为空");
            }

            // 检查用户是否存在
            User existingUser = userService.getById(id);
            if (existingUser == null) {
                log.warn("重置用户密码失败: 用户不存在, userId: {}", id);
                return Result.error(ResultCode.USER_NOT_FOUND.getCode(), "用户不存在");
            }

            // 更新用户密码
            existingUser.setPassword(passwordEncoder.encode(newPassword));
            boolean updated = userService.updateById(existingUser);
            if (updated) {
                log.info("管理员重置用户密码成功, userId: {}, username: {}", id, existingUser.getUsername());
                // 清除用户相关缓存
                userService.clearUserCache(existingUser.getUsername(), existingUser.getId());
                
                // 发送用户变更消息到日志服务
                userLogMessageService.sendUserChangeMessage(
                        id, 
                        existingUser.getUsername(), 
                        existingUser.getStatus(), 
                        existingUser.getStatus(), 
                        2, // 更新用户
                        currentUserId != null ? currentUserId : "ADMIN");
                
                return Result.success("密码重置成功");
            } else {
                log.error("管理员重置用户密码失败, userId: {}", id);
                return Result.error("密码重置失败");
            }
        } catch (BusinessException e) {
            log.error("管理员重置用户密码失败: 业务异常, userId: {}", id, e);
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("管理员重置用户密码失败: 系统异常, userId: {}", id, e);
            return Result.error(ResultCode.SYSTEM_ERROR.getCode(), "密码重置失败: " + e.getMessage());
        }
    }
}