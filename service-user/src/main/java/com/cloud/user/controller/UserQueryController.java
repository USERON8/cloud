package com.cloud.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.common.domain.Result;
import com.cloud.common.domain.dto.RegisterRequestDTO;
import com.cloud.common.domain.dto.UserDTO;
import com.cloud.common.enums.ResultCode;
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

    @PostMapping("/register")
    public Result<String> register(@RequestBody RegisterRequestDTO registerRequestDTO) {
        try {
            log.info("用户注册, username: {}, email: {}", registerRequestDTO.getUsername(), registerRequestDTO.getEmail());

            if (registerRequestDTO.getUsername() == null || registerRequestDTO.getUsername().isEmpty()) {
                return Result.error(ResultCode.PARAM_ERROR.getCode(), "用户名不能为空");
            }

            // 检查用户名是否已存在
            User existingUser = userService.getOne(new QueryWrapper<User>().eq("username", registerRequestDTO.getUsername()));
            if (existingUser != null) {
                log.warn("用户名已存在, username: {}", registerRequestDTO.getUsername());
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "用户名已存在");
            }

            User user = userConverter.toEntity(registerRequestDTO);
            user.setPasswordHash(registerRequestDTO.getPassword());
            user.setUserType("USER");
            userService.save(user);
            log.info("用户注册成功, userId: {}, username: {}", user.getId(), user.getUsername());
            return Result.success("注册成功");
        } catch (Exception e) {
            log.error("用户注册失败, username: {}", registerRequestDTO.getUsername(), e);
            return Result.error(ResultCode.SYSTEM_ERROR.getCode(), "注册失败: " + e.getMessage());
        }
    }


    @GetMapping("/info")
    @Operation(summary = "获取当前用户信息", description = "根据JWT获取当前用户信息")
    public Result<Map<String, Object>> getUserInfo(@Parameter(description = "JWT信息") @AuthenticationPrincipal Jwt jwt) {
        try {
            log.info("获取用户信息, username: {}", jwt.getSubject());
            return Result.success(Collections.singletonMap("username", jwt.getSubject()));
        } catch (Exception e) {
            log.error("获取用户信息失败, username: {}", jwt.getSubject(), e);
            return Result.error(ResultCode.SYSTEM_ERROR.getCode(), "获取用户信息失败: " + e.getMessage());
        }
    }

    // 为auth服务提供根据用户名查询用户的方法
    @GetMapping("/username/{username}")
    @Operation(summary = "根据用户名查找用户", description = "根据用户名查找用户信息，供auth服务调用")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UserDTO.class)))
    public Result<UserDTO> findByUsername(@Parameter(description = "用户名") @PathVariable("username") String username) {
        try {
            log.info("根据用户名查找用户, username: {}", username);
            if (username == null || username.isEmpty()) {
                return Result.error(ResultCode.PARAM_ERROR.getCode(), "用户名不能为空");
            }

            User user = userService.getOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                    .eq("username", username));
            UserDTO userDTO = userConverter.toDTO(user);
            log.info("用户查询结果, username: {}, found: {}", username, userDTO != null);
            if (userDTO == null) {
                return Result.error(ResultCode.USER_NOT_FOUND.getCode(), "用户不存在");
            }
            return Result.success(userDTO);
        } catch (Exception e) {
            log.error("根据用户名查找用户失败, username: {}", username, e);
            return Result.error(ResultCode.SYSTEM_ERROR.getCode(), "查询用户失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查找用户", description = "根据用户ID查找用户信息")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UserDTO.class)))
    public Result<UserDTO> findById(@Parameter(description = "用户ID") @PathVariable("id") Long id) {
        try {
            log.info("根据ID查找用户, id: {}", id);
            if (id == null) {
                return Result.error(ResultCode.PARAM_ERROR.getCode(), "用户ID不能为空");
            }

            User user = userService.getById(id);
            UserDTO userDTO = userConverter.toDTO(user);
            log.info("用户查询结果, id: {}, found: {}", id, userDTO != null);
            if (userDTO == null) {
                return Result.error(ResultCode.USER_NOT_FOUND.getCode(), "用户不存在");
            }
            return Result.success(userDTO);
        } catch (Exception e) {
            log.error("根据ID查找用户失败, id: {}", id, e);
            return Result.error(ResultCode.SYSTEM_ERROR.getCode(), "查询用户失败: " + e.getMessage());
        }
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
        try {
            log.info("获取用户详情, userId: {}", id);
            if (id == null) {
                return Result.error(ResultCode.PARAM_ERROR.getCode(), "用户ID不能为空");
            }

            User user = userService.getById(id);
            if (user == null) {
                log.warn("用户不存在, userId: {}", id);
                return Result.error(ResultCode.USER_NOT_FOUND.getCode(), "用户不存在");
            }
            log.info("用户详情查询成功, userId: {}", id);
            return Result.success(userConverter.toDTO(user));
        } catch (Exception e) {
            log.error("获取用户详情失败, userId: {}", id, e);
            return Result.error(ResultCode.SYSTEM_ERROR.getCode(), "查询用户详情失败: " + e.getMessage());
        }
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
        try {
            log.info("分页查询用户列表, page: {}, size: {}, username: {}", page, size, username);

            if (page <= 0) {
                return Result.error(ResultCode.PARAM_ERROR.getCode(), "页码必须大于0");
            }
            if (size <= 0) {
                return Result.error(ResultCode.PARAM_ERROR.getCode(), "每页大小必须大于0");
            }

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
        } catch (Exception e) {
            log.error("分页查询用户列表失败, page: {}, size: {}, username: {}", page, size, username, e);
            return Result.error(ResultCode.SYSTEM_ERROR.getCode(), "查询用户列表失败: " + e.getMessage());
        }
    }
}