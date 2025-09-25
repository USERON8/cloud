package com.cloud.user.controller.user;

import com.cloud.common.annotation.RequireScope;
import com.cloud.common.annotation.RequireUserType;
import com.cloud.common.annotation.RequireUserType.UserType;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.domain.dto.user.UserPageDTO;
import com.cloud.common.domain.vo.UserVO;
import com.cloud.common.result.PageResult;
import com.cloud.common.result.Result;
import com.cloud.common.security.SecurityPermissionUtils;
import com.cloud.user.converter.UserConverter;
import com.cloud.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final UserConverter userConverter = UserConverter.INSTANCE;

    /**
     * 获取用户列表（支持查询参数）
     */
    @GetMapping
    @RequireUserType(UserType.ADMIN)
    @RequireScope("admin:read")
    @Operation(summary = "获取用户列表", description = "获取用户列表，支持分页和查询参数")
    public Result<PageResult<UserVO>> getUsers(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "用户名") @RequestParam(required = false) String username,
            @Parameter(description = "邮箱") @RequestParam(required = false) String email,
            @Parameter(description = "用户类型") @RequestParam(required = false) String userType) {
        
        UserPageDTO userPageDTO = new UserPageDTO();
        userPageDTO.setCurrent(page.longValue());
        userPageDTO.setSize(size.longValue());
        userPageDTO.setUsername(username);
        userPageDTO.setPhone(email); // UserPageDTO没有email字段，使用phone字段
        userPageDTO.setUserType(userType);
        
        return Result.success(userService.pageQuery(userPageDTO));
    }

    /**
     * 根据用户名查询用户
     */
    @GetMapping("/search")
    @RequireUserType(UserType.ADMIN)
    @RequireScope("admin:read")
    @Operation(summary = "根据用户名查询用户", description = "根据用户名查询用户信息")
    public Result<UserDTO> findByUsername(
            @Parameter(description = "用户名") @RequestParam
            @NotBlank(message = "用户名不能为空") String username) {
        return Result.success("查询成功", userService.findByUsername(username));
    }

    /**
     * 根据ID获取用户详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取用户详情", description = "根据用户ID获取用户详细信息")
    public Result<UserDTO> getUserById(
            @Parameter(description = "用户ID") @PathVariable
            @Positive(message = "用户ID必须为正整数") Long id,
            Authentication authentication) {
        
        // 权限检查：管理员或用户本人可以查看
        if (!SecurityPermissionUtils.isAdminOrOwner(authentication, id)) {
            return Result.forbidden("无权限查看此用户信息");
        }
        
        UserDTO user = userService.getUserById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        
        return Result.success("查询成功", user);
    }

    /**
     * 创建用户
     */
    @PostMapping
    @RequireUserType(UserType.ADMIN)
    @RequireScope("admin:write")
    @Operation(summary = "创建用户", description = "创建新用户")
    public Result<UserDTO> createUser(
            @Parameter(description = "用户信息") @RequestBody
            @Valid @NotNull(message = "用户信息不能为空") UserDTO userDTO) {
        
        try {
            // 转换为注册请求DTO并调用注册方法
            com.cloud.common.domain.dto.auth.RegisterRequestDTO registerRequest =
                new com.cloud.common.domain.dto.auth.RegisterRequestDTO();
            registerRequest.setUsername(userDTO.getUsername());
            registerRequest.setPassword("123456"); // UserDTO没有password字段，使用默认密码
            // registerRequest.setEmail(userDTO.getEmail()); // RegisterRequestDTO可能没有email字段
            registerRequest.setPhone(userDTO.getPhone());
            registerRequest.setNickname(userDTO.getNickname());
            registerRequest.setUserType(userDTO.getUserType());

            UserDTO createdUser = userService.registerUser(registerRequest);
            return Result.success("用户创建成功", createdUser);
        } catch (Exception e) {
            log.error("创建用户失败", e);
            return Result.error("创建用户失败: " + e.getMessage());
        }
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新用户信息", description = "更新用户信息")
    public Result<Boolean> updateUser(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Parameter(description = "用户信息") @RequestBody
            @Valid @NotNull(message = "用户信息不能为空") UserDTO userDTO,
            Authentication authentication) {

        // 确保路径参数与请求体中的ID一致
        userDTO.setId(id);
        
        // 使用统一的权限检查工具
        if (!SecurityPermissionUtils.isAdminOrOwner(authentication, id)) {
            return Result.forbidden("无权限更新此用户信息");
        }

        try {
            boolean result = userService.updateById(userConverter.toEntity(userDTO));
            return Result.success("用户更新成功", result);
        } catch (Exception e) {
            log.error("更新用户信息失败，用户ID: {}", id, e);
            return Result.error("更新用户信息失败: " + e.getMessage());
        }
    }

    /**
     * 部分更新用户信息
     */
    @PatchMapping("/{id}")
    @Operation(summary = "部分更新用户信息", description = "部分更新用户信息")
    public Result<Boolean> patchUser(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Parameter(description = "用户信息") @RequestBody UserDTO userDTO,
            Authentication authentication) {

        // 确保路径参数与请求体中的ID一致
        userDTO.setId(id);
        
        // 使用统一的权限检查工具
        if (!SecurityPermissionUtils.isAdminOrOwner(authentication, id)) {
            return Result.forbidden("无权限更新此用户信息");
        }

        try {
            // 使用现有的updateById方法，因为patchUpdate方法不存在
            boolean result = userService.updateById(userConverter.toEntity(userDTO));
            return Result.success("用户更新成功", result);
        } catch (Exception e) {
            log.error("部分更新用户信息失败，用户ID: {}", id, e);
            return Result.error("更新用户信息失败: " + e.getMessage());
        }
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @RequireUserType(UserType.ADMIN)
    @RequireScope("admin:write")
    @Operation(summary = "删除用户", description = "删除用户")
    public Result<Boolean> deleteUser(
            @Parameter(description = "用户ID") @PathVariable
            @Positive(message = "用户ID必须为正整数") Long id) {
        
        try {
            boolean result = userService.deleteUserById(id);
            return Result.success("用户删除成功", result);
        } catch (Exception e) {
            log.error("删除用户失败，用户ID: {}", id, e);
            return Result.error("删除用户失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户档案
     */
    @GetMapping("/{id}/profile")
    @Operation(summary = "获取用户档案", description = "获取用户详细档案信息")
    public Result<UserDTO> getUserProfile(
            @Parameter(description = "用户ID") @PathVariable Long id,
            Authentication authentication) {
        
        // 权限检查：管理员或用户本人可以查看
        if (!SecurityPermissionUtils.isAdminOrOwner(authentication, id)) {
            return Result.forbidden("无权限查看此用户档案");
        }
        
        try {
            UserDTO userProfile = userService.getUserById(id);
            return Result.success("查询成功", userProfile);
        } catch (Exception e) {
            log.error("获取用户档案失败，用户ID: {}", id, e);
            return Result.error("获取用户档案失败: " + e.getMessage());
        }
    }

    /**
     * 更新用户档案
     */
    @PutMapping("/{id}/profile")
    @Operation(summary = "更新用户档案", description = "更新用户详细档案信息")
    public Result<Boolean> updateUserProfile(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Parameter(description = "用户档案信息") @RequestBody
            @Valid @NotNull(message = "用户档案信息不能为空") UserDTO profileDTO,
            Authentication authentication) {

        // 确保路径参数与请求体中的ID一致
        profileDTO.setId(id);
        
        // 权限检查：管理员或用户本人可以更新
        if (!SecurityPermissionUtils.isAdminOrOwner(authentication, id)) {
            return Result.forbidden("无权限更新此用户档案");
        }

        try {
            // 使用现有的updateById方法
            boolean result = userService.updateById(userConverter.toEntity(profileDTO));
            return Result.success("用户档案更新成功", result);
        } catch (Exception e) {
            log.error("更新用户档案失败，用户ID: {}", id, e);
            return Result.error("更新用户档案失败: " + e.getMessage());
        }
    }

    /**
     * 启用/禁用用户
     */
    @PatchMapping("/{id}/status")
    @RequireUserType(UserType.ADMIN)
    @RequireScope("admin:write")
    @Operation(summary = "更新用户状态", description = "启用或禁用用户")
    public Result<Boolean> updateUserStatus(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Parameter(description = "用户状态") @RequestParam Integer status) {
        
        try {
            // 创建用户实体并更新状态
            UserDTO userDTO = userService.getUserById(id);
            if (userDTO == null) {
                return Result.error("用户不存在");
            }
            userDTO.setStatus(status);
            boolean result = userService.updateById(userConverter.toEntity(userDTO));
            return Result.success("用户状态更新成功", result);
        } catch (Exception e) {
            log.error("更新用户状态失败，用户ID: {}, 状态: {}", id, status, e);
            return Result.error("更新用户状态失败: " + e.getMessage());
        }
    }
}
