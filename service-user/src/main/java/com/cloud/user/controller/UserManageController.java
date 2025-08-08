package com.cloud.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cloud.common.domain.Result;
import com.cloud.common.domain.dto.UserDTO;
import com.cloud.user.converter.UserConverter;
import com.cloud.user.module.entity.User;
import com.cloud.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/admin/users")
@Tag(name = "用户管理", description = "用户增删改及状态管理")
public class UserManageController {
    private final UserService userService;
    private final UserConverter userConverter;


    public UserManageController(UserService userService, UserConverter userConverter) {
        this.userService = userService;
        this.userConverter = userConverter;

    }

    /**
     * 创建用户
     *
     * @param userDTO 用户信息
     * @return 创建的用户信息
     */
    @PostMapping("/create/user")
    @Operation(summary = "创建用户", description = "创建新用户")
    @ApiResponse(responseCode = "200", description = "创建成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Result<UserDTO> create(@Parameter(description = "用户信息") @Valid @RequestBody UserDTO userDTO) {
        log.info("创建用户, username: {}", userDTO.getUsername());

        // 检查用户名是否已存在
        User existingUser = userService.getOne(new QueryWrapper<User>().eq("username", userDTO.getUsername()));
        if (existingUser != null) {
            log.warn("用户名已存在, username: {}", userDTO.getUsername());
            return Result.error("用户名已存在");
        }

        User user = userConverter.toEntity(userDTO);
        // 默认启用状态
        user.setStatus(1);
        userService.save(user);
        log.info("用户创建成功, userId: {}, username: {}", user.getId(), user.getUsername());
        return Result.success("创建成功", userConverter.toDTO(user));
    }

    /**
     * 更新用户信息
     *
     * @param id      用户ID
     * @param userDTO 用户信息
     * @return 更新后的用户信息
     */
    @PutMapping("/update/{id}")
    @Operation(summary = "更新用户信息", description = "根据用户ID更新用户信息")
    @ApiResponse(responseCode = "200", description = "更新成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    @PreAuthorize("@permissionService.hasPermission(#id) or hasAuthority('ROLE_ADMIN')")
    public Result<UserDTO> update(@Parameter(description = "用户ID") @PathVariable("id") Long id,
                                  @Parameter(description = "用户信息") @Valid @RequestBody UserDTO userDTO) {
        log.info("更新用户信息, userId: {}", id);

        User user = userService.getById(id);
        if (user == null) {
            log.warn("用户不存在, userId: {}", id);
            return Result.error("用户不存在");
        }

        // 检查用户名是否已存在（排除自己）
        if (userDTO.getUsername() != null && !Objects.equals(user.getUsername(), userDTO.getUsername())) {
            User existingUser = userService.getOne(new QueryWrapper<User>().eq("username", userDTO.getUsername()));
            if (existingUser != null) {
                log.warn("用户名已存在, username: {}", userDTO.getUsername());
                return Result.error("用户名已存在");
            }
        }

        User updateUser = userConverter.toEntity(userDTO);
        updateUser.setId(id);
        userService.updateById(updateUser);
        log.info("用户信息更新成功, userId: {}", id);
        return Result.success("更新成功", userConverter.toDTO(updateUser));
    }

    /**
     * 删除用户（逻辑删除）
     *
     * @param id 用户ID
     * @return 删除结果
     */
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除用户", description = "逻辑删除用户（标记为已删除）")
    @ApiResponse(responseCode = "200", description = "删除成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    @PreAuthorize("@permissionService.hasPermission(#id) or hasAuthority('ROLE_ADMIN')")
    public Result<?> delete(@Parameter(description = "用户ID") @PathVariable("id") Long id) {
        log.info("删除用户, userId: {}", id);

        User user = userService.getById(id);
        if (user == null) {
            log.warn("用户不存在, userId: {}", id);
            return Result.error("用户不存在");
        }
        user.setDeleted(1); // 逻辑删除
        userService.updateById(user);
        log.info("用户删除成功, userId: {}", id);
        return Result.success("删除成功");
    }

    /**
     * 禁用用户
     *
     * @param id 用户ID
     * @return 操作结果
     */
    @PutMapping("/disable/{id}")
    @Operation(summary = "禁用用户", description = "禁用指定用户")
    @ApiResponse(responseCode = "200", description = "禁用成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    @PreAuthorize("@permissionService.hasPermission(#id) or hasAuthority('ROLE_ADMIN')")
    public Result<?> disableUser(@Parameter(description = "用户ID") @PathVariable("id") Long id) {
        log.info("禁用用户, userId: {}", id);

        User user = userService.getById(id);
        if (user == null) {
            log.warn("用户不存在, userId: {}", id);
            return Result.error("用户不存在");
        }
        user.setStatus(0); // 禁用
        userService.updateById(user);
        log.info("用户已禁用, userId: {}", id);
        return Result.success("用户已禁用");
    }

    /**
     * 启用用户
     *
     * @param id 用户ID
     * @return 操作结果
     */
    @PutMapping("/enable/{id}")
    @Operation(summary = "启用用户", description = "启用指定用户")
    @ApiResponse(responseCode = "200", description = "启用成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    @PreAuthorize("@permissionService.hasPermission(#id) or hasAuthority('ROLE_ADMIN')")
    public Result<?> enableUser(@Parameter(description = "用户ID") @PathVariable("id") Long id) {
        log.info("启用用户, userId: {}", id);

        User user = userService.getById(id);
        if (user == null) {
            log.warn("用户不存在, userId: {}", id);
            return Result.error("用户不存在");
        }
        user.setStatus(1); // 启用
        userService.updateById(user);
        log.info("用户已启用, userId: {}", id);
        return Result.success("用户已启用");
    }

}