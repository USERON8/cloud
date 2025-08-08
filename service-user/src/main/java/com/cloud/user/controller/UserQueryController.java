package com.cloud.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/users")
@Tag(name = "用户查询", description = "用户查询相关操作接口")
public class UserQueryController {
    private final UserService userService;
    private final UserConverter userConverter;


    public UserQueryController(UserService userService, UserConverter userConverter) {
        this.userService = userService;
        this.userConverter = userConverter;
    }


    @GetMapping("/info")
    @Operation(summary = "获取当前用户信息", description = "根据JWT获取当前用户信息")
    public Map<String, Object> getUserInfo(@Parameter(description = "JWT信息") @AuthenticationPrincipal Jwt jwt) {
        log.info("获取用户信息, username: {}", jwt.getSubject());
        return Collections.singletonMap("username", jwt.getSubject());
    }

    // 为auth服务提供根据用户名查询用户的方法
    @GetMapping("/username/{username}")
    @Operation(summary = "根据用户名查找用户", description = "根据用户名查找用户信息，供auth服务调用")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UserDTO.class)))
    public UserDTO findByUsername(@Parameter(description = "用户名") @PathVariable("username") String username) {
        log.info("根据用户名查找用户, username: {}", username);
        User user = userService.getOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                .eq("username", username));
        UserDTO userDTO = userConverter.toDTO(user);
        log.info("用户查询结果, username: {}, found: {}", username, userDTO != null);
        return userDTO;
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查找用户", description = "根据用户ID查找用户信息")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UserDTO.class)))
    public UserDTO findById(@Parameter(description = "用户ID") @PathVariable("id") Long id) {
        log.info("根据ID查找用户, id: {}", id);
        User user = userService.getById(id);
        UserDTO userDTO = userConverter.toDTO(user);
        log.info("用户查询结果, id: {}, found: {}", id, userDTO != null);
        return userDTO;
    }


    /**
     * 根据ID获取用户信息
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @GetMapping("/detail/{id}")
    @Operation(summary = "获取用户详情", description = "根据用户ID获取用户详细信息")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    @PreAuthorize("@permissionService.hasPermission(#id) or hasAuthority('ROLE_ADMIN')")
    public Result<UserDTO> getUserById(@Parameter(description = "用户ID") @PathVariable("id") Long id) {
        log.info("获取用户详情, userId: {}", id);

        User user = userService.getById(id);
        if (user == null) {
            log.warn("用户不存在, userId: {}", id);
            return Result.error("用户不存在");
        }
        log.info("用户详情查询成功, userId: {}", id);
        return Result.success(userConverter.toDTO(user));
    }

    /**
     * 分页查询用户列表
     *
     * @param page     页码
     * @param size     每页大小
     * @param username 用户名（可选）
     * @return 用户列表
     */
    @GetMapping
    @Operation(summary = "分页查询用户列表", description = "分页查询用户列表，支持按用户名模糊查询")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Result<Page<UserDTO>> listUsers(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "用户名（模糊查询）") @RequestParam(required = false) String username) {
        log.info("分页查询用户列表, page: {}, size: {}, username: {}", page, size, username);

        Page<User> userPage = new Page<>(page, size);
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("deleted", 0); // 只查询未删除的用户
        if (username != null && !username.isEmpty()) {
            queryWrapper.like("username", username);
        }
        queryWrapper.orderByDesc("created_at");
        Page<User> result = userService.page(userPage, queryWrapper);

        Page<UserDTO> dtoPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        dtoPage.setRecords(result.getRecords().stream().map(userConverter::toDTO).toList());

        log.info("用户列表查询成功, total: {}", result.getTotal());
        return Result.success(dtoPage);
    }


}