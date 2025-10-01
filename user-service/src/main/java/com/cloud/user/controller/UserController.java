package com.cloud.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.result.PageResult;
import com.cloud.common.result.Result;
import com.cloud.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 用户RESTful API控制器
 * 提供用户资源的CRUD操作
 *
 * @author what's up
 */
@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "用户服务", description = "用户资源的RESTful API接口")
public class UserController {

    private final UserService userService;

    /**
     * 分页查询用户
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "分页查询用户", description = "获取用户列表，支持分页")
    public Result<PageResult<UserDTO>> getUsers(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "页码必须大于0") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "每页数量必须大于0")
            @Max(value = 100, message = "每页数量不能超过100") Integer size,
            Authentication authentication) {

        try {
            Page<UserDTO> pageResult = userService.getUsersPage(page, size);
            PageResult<UserDTO> result = PageResult.of(
                    pageResult.getCurrent(),
                    pageResult.getSize(),
                    pageResult.getTotal(),
                    pageResult.getRecords()
            );
            return Result.success(result);
        } catch (Exception e) {
            log.error("分页查询用户失败", e);
            return Result.error("分页查询用户失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取用户详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @permissionManager.isCurrentUser(#id, authentication)")
    @Operation(summary = "获取用户详情", description = "根据用户ID获取详细信息")
    public Result<UserDTO> getUserById(
            @Parameter(description = "用户ID") @PathVariable
            @NotNull(message = "用户ID不能为空")
            @Positive(message = "用户ID必须为正整数") Long id,
            Authentication authentication) {

        try {
            UserDTO user = userService.getUserById(id);
            if (user == null) {
                return Result.error("用户不存在");
            }
            return Result.success("查询成功", user);
        } catch (Exception e) {
            log.error("获取用户详情失败，用户ID: {}", id, e);
            return Result.error("获取用户详情失败: " + e.getMessage());
        }
    }

    /**
     * 根据用户名获取用户
     */
    @GetMapping("/username/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "根据用户名获取用户", description = "根据用户名获取用户信息")
    public Result<UserDTO> getUserByUsername(
            @Parameter(description = "用户名") @PathVariable String username,
            Authentication authentication) {

        try {
            UserDTO user = userService.getUserByUsername(username);
            if (user == null) {
                return Result.error("用户不存在");
            }
            return Result.success("查询成功", user);
        } catch (Exception e) {
            log.error("根据用户名获取用户失败，用户名: {}", username, e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 创建用户
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "创建用户", description = "创建新的用户")
    public Result<UserDTO> createUser(
            @Parameter(description = "用户信息") @RequestBody
            @Valid @NotNull(message = "用户信息不能为空") UserDTO userDTO) {

        try {
            Long userId = userService.createUser(userDTO);
            userDTO.setId(userId);
            return Result.success("用户创建成功", userDTO);
        } catch (Exception e) {
            log.error("创建用户失败", e);
            return Result.error("创建用户失败: " + e.getMessage());
        }
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @permissionManager.isCurrentUser(#id, authentication)")
    @Operation(summary = "更新用户信息", description = "更新用户信息")
    public Result<Boolean> updateUser(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Parameter(description = "用户信息") @RequestBody
            @Valid @NotNull(message = "用户信息不能为空") UserDTO userDTO,
            Authentication authentication) {

        userDTO.setId(id);

        try {
            boolean result = userService.updateUser(userDTO);
            return Result.success("用户更新成功", result);
        } catch (Exception e) {
            log.error("更新用户失败，用户ID: {}", id, e);
            return Result.error("更新用户失败: " + e.getMessage());
        }
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "删除用户", description = "删除用户")
    public Result<Boolean> deleteUser(
            @Parameter(description = "用户ID") @PathVariable
            @NotNull(message = "用户ID不能为空") Long id) {

        try {
            boolean result = userService.deleteUser(id);
            return Result.success("删除成功", result);
        } catch (Exception e) {
            log.error("删除用户失败，用户ID: {}", id, e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * 更新用户状态
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "更新用户状态", description = "启用或禁用用户")
    public Result<Boolean> updateUserStatus(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Parameter(description = "用户状态") @RequestParam Integer status) {

        try {
            boolean result = userService.updateUserStatus(id, status);
            return Result.success("状态更新成功", result);
        } catch (Exception e) {
            log.error("更新用户状态失败，用户ID: {}, 状态: {}", id, status, e);
            return Result.error("更新状态失败: " + e.getMessage());
        }
    }

    /**
     * 重置用户密码
     */
    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "重置用户密码", description = "重置用户密码为默认密码")
    public Result<Boolean> resetPassword(
            @Parameter(description = "用户ID") @PathVariable Long id) {

        try {
            String newPassword = userService.resetPassword(id);
            return Result.success("密码重置成功，新密码为: " + newPassword, true);
        } catch (Exception e) {
            log.error("重置密码失败，用户ID: {}", id, e);
            return Result.error("密码重置失败: " + e.getMessage());
        }
    }

    /**
     * 修改密码
     */
    @PostMapping("/{id}/change-password")
    @PreAuthorize("@permissionManager.isCurrentUser(#id, authentication)")
    @Operation(summary = "修改密码", description = "用户修改自己的密码")
    public Result<Boolean> changePassword(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Parameter(description = "旧密码") @RequestParam String oldPassword,
            @Parameter(description = "新密码") @RequestParam String newPassword,
            Authentication authentication) {

        try {
            boolean result = userService.changePassword(id, oldPassword, newPassword);
            return Result.success("密码修改成功", result);
        } catch (Exception e) {
            log.error("修改密码失败，用户ID: {}", id, e);
            return Result.error("密码修改失败: " + e.getMessage());
        }
    }
}
