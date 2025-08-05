package com.cloud.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.common.domain.Result;
import com.cloud.common.domain.dto.UserDTO;
import com.cloud.user.converter.UserConverter;
import com.cloud.user.module.entity.User;
import com.cloud.user.service.FileUploadService;
import com.cloud.user.service.UserInternalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.rpc.RpcContext;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/user")
@Tag(name = "用户管理", description = "用户相关操作接口")
public class UserController {
    private final UserInternalService userService;
    private final UserConverter userConverter;
    private final FileUploadService fileUploadService;

    public UserController(UserInternalService userService, UserConverter userConverter, FileUploadService fileUploadService) {
        this.userService = userService;
        this.userConverter = userConverter;
        this.fileUploadService = fileUploadService;
    }


    @GetMapping("/user/info")
    @Operation(summary = "获取当前用户信息", description = "根据JWT获取当前用户信息")
    public Map<String, Object> getUserInfo(@Parameter(description = "JWT信息") @AuthenticationPrincipal Jwt jwt) {
        log.info("获取用户信息, username: {}", jwt.getSubject());
        return Collections.singletonMap("username", jwt.getSubject());
    }

    // 为auth服务提供根据用户名查询用户的方法
    @GetMapping("/users/findByUsername")
    @Operation(summary = "根据用户名查找用户", description = "根据用户名查找用户信息，供auth服务调用")
    @ApiResponse(responseCode = "200", description = "查询成功", 
        content = @Content(mediaType = "application/json", 
        schema = @Schema(implementation = UserDTO.class)))
    public UserDTO findByUsername(@Parameter(description = "用户名") @RequestParam("username") String username) {
        log.info("根据用户名查找用户, username: {}", username);
        User user = userService.getOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                .eq("username", username));
        UserDTO userDTO = userConverter.toDTO(user);
        log.info("用户查询结果, username: {}, found: {}", username, userDTO != null);
        return userDTO;
    }

    @GetMapping("/users/findById")
    @Operation(summary = "根据ID查找用户", description = "根据用户ID查找用户信息")
    @ApiResponse(responseCode = "200", description = "查询成功", 
        content = @Content(mediaType = "application/json", 
        schema = @Schema(implementation = UserDTO.class)))
    public UserDTO findById(@Parameter(description = "用户ID") @RequestParam("id") Long id) {
        log.info("根据ID查找用户, id: {}", id);
        User user = userService.getById(id);
        UserDTO userDTO = userConverter.toDTO(user);
        log.info("用户查询结果, id: {}, found: {}", id, userDTO != null);
        return userDTO;
    }

    /**
     * 创建用户
     *
     * @param userDTO 用户信息
     * @return 创建的用户信息
     */
    @PostMapping("/users")
    @Operation(summary = "创建用户", description = "创建新用户")
    @ApiResponse(responseCode = "200", description = "创建成功", 
        content = @Content(mediaType = "application/json", 
        schema = @Schema(implementation = Result.class)))
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
    @PutMapping("/users/{id}")
    @Operation(summary = "更新用户信息", description = "根据用户ID更新用户信息")
    @ApiResponse(responseCode = "200", description = "更新成功", 
        content = @Content(mediaType = "application/json", 
        schema = @Schema(implementation = Result.class)))
    public Result<UserDTO> update(@Parameter(description = "用户ID") @PathVariable("id") Long id, 
                                  @Parameter(description = "用户信息") @Valid @RequestBody UserDTO userDTO) {
        log.info("更新用户信息, userId: {}", id);
        
        // 验证操作权限
        if (!hasPermission(id)) {
            return Result.error("无权限操作该用户");
        }
        
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
    @DeleteMapping("/users/{id}")
    @Operation(summary = "删除用户", description = "逻辑删除用户（标记为已删除）")
    @ApiResponse(responseCode = "200", description = "删除成功", 
        content = @Content(mediaType = "application/json", 
        schema = @Schema(implementation = Result.class)))
    public Result<?> delete(@Parameter(description = "用户ID") @PathVariable("id") Long id) {
        log.info("删除用户, userId: {}", id);
        
        // 验证操作权限
        if (!hasPermission(id)) {
            return Result.error("无权限操作该用户");
        }
        
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
     * 根据ID获取用户信息
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @GetMapping("/users/{id}")
    @Operation(summary = "获取用户详情", description = "根据用户ID获取用户详细信息")
    @ApiResponse(responseCode = "200", description = "查询成功", 
        content = @Content(mediaType = "application/json", 
        schema = @Schema(implementation = Result.class)))
    public Result<UserDTO> getUserById(@Parameter(description = "用户ID") @PathVariable("id") Long id) {
        log.info("获取用户详情, userId: {}", id);
        
        // 验证操作权限
        if (!hasPermission(id)) {
            return Result.error("无权限查看该用户");
        }
        
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
    @GetMapping("/users")
    @Operation(summary = "分页查询用户列表", description = "分页查询用户列表，支持按用户名模糊查询")
    @ApiResponse(responseCode = "200", description = "查询成功", 
        content = @Content(mediaType = "application/json", 
        schema = @Schema(implementation = Result.class)))
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

    /**
     * 禁用用户
     *
     * @param id 用户ID
     * @return 操作结果
     */
    @PutMapping("/users/{id}/disable")
    @Operation(summary = "禁用用户", description = "禁用指定用户")
    @ApiResponse(responseCode = "200", description = "禁用成功", 
        content = @Content(mediaType = "application/json", 
        schema = @Schema(implementation = Result.class)))
    public Result<?> disableUser(@Parameter(description = "用户ID") @PathVariable("id") Long id) {
        log.info("禁用用户, userId: {}", id);
        
        // 验证操作权限
        if (!hasPermission(id)) {
            return Result.error("无权限操作该用户");
        }
        
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
    @PutMapping("/users/{id}/enable")
    @Operation(summary = "启用用户", description = "启用指定用户")
    @ApiResponse(responseCode = "200", description = "启用成功", 
        content = @Content(mediaType = "application/json", 
        schema = @Schema(implementation = Result.class)))
    public Result<?> enableUser(@Parameter(description = "用户ID") @PathVariable("id") Long id) {
        log.info("启用用户, userId: {}", id);
        
        // 验证操作权限
        if (!hasPermission(id)) {
            return Result.error("无权限操作该用户");
        }
        
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

    @PostMapping("/avatar/upload")
    @Operation(summary = "上传头像", description = "为指定用户上传头像")
    @ApiResponse(responseCode = "200", description = "上传成功", 
        content = @Content(mediaType = "application/json", 
        schema = @Schema(implementation = Result.class)))
    public Result<String> uploadAvatar(
            @Parameter(description = "用户ID") @RequestParam("userId") Long userId,
            @Parameter(description = "头像文件") @RequestParam("avatar") MultipartFile avatar) {

        log.info("上传头像, userId: {}, fileName: {}", userId, avatar.getOriginalFilename());
        
        // 验证操作权限
        if (!hasPermission(userId)) {
            return Result.error("无权限操作该用户头像");
        }

        // 上传文件到MinIO
        String avatarUrl = fileUploadService.uploadAvatar(avatar, userId);

        // 更新用户头像信息
        userService.updateUserAvatar(userId, avatarUrl, extractFileName(avatarUrl));
        
        log.info("头像上传成功, userId: {}, avatarUrl: {}", userId, avatarUrl);
        return Result.success(avatarUrl);
    }

    @DeleteMapping("/avatar/{userId}")
    @Operation(summary = "删除头像", description = "删除指定用户的头像")
    @ApiResponse(responseCode = "200", description = "删除成功", 
        content = @Content(mediaType = "application/json", 
        schema = @Schema(implementation = Result.class)))
    public Result<Void> deleteAvatar(@Parameter(description = "用户ID") @PathVariable Long userId) {

        log.info("删除头像, userId: {}", userId);
        
        // 验证操作权限
        if (!hasPermission(userId)) {
            return Result.error("无权限操作该用户头像");
        }

        userService.deleteUserAvatar(userId);
        
        log.info("头像删除成功, userId: {}", userId);
        return Result.success();
    }

    /**
     * 从URL中提取文件名
     */
    private String extractFileName(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        return url.substring(url.lastIndexOf("/") + 1);
    }
    
    /**
     * 检查用户是否有权限操作指定用户
     * @param targetUserId 目标用户ID
     * @return 是否有权限
     */
    private boolean hasPermission(Long targetUserId) {
        // 从Dubbo上下文中获取当前用户ID
        String currentUserIdStr = RpcContext.getServerAttachment().getAttachment("userId");
        if (currentUserIdStr == null) {
            // 如果没有上下文信息，可能是内部调用或其他情况
            return true;
        }
        
        Long currentUserId = Long.valueOf(currentUserIdStr);
        
        // 如果是管理员用户，则可以操作所有用户
        User currentUser = userService.getById(currentUserId);
        if (currentUser != null && "ADMIN".equals(currentUser.getUserType())) {
            return true;
        }
        
        // 普通用户只能操作自己的信息
        return currentUserId.equals(targetUserId);
    }
}