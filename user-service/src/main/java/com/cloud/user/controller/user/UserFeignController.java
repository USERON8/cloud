package com.cloud.user.controller.user;

import com.cloud.common.domain.dto.auth.RegisterRequestDTO;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.user.converter.UserConverter;
import com.cloud.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Tag(name = "用户内部接口", description = "供其他服务调用的用户相关内部接口")
public class UserFeignController {
    private final UserService userService;
    private final UserConverter userConverter;

    @GetMapping("/internal/username/{username}")
    @Operation(summary = "根据用户名查询用户", description = "根据用户名查询用户详细信息")
    public UserDTO findByUsername(
            @PathVariable
            @Parameter(description = "用户名")
            @NotBlank(message = "用户名不能为空") String username) {

        log.debug("开始调用用户服务查询用户信息, username: {}", username);

        // 直接委托给Service层，享受多级缓存和事务管理
        return userService.findByUsername(username);
    }

    @GetMapping("/internal/id/{id}")
    @Operation(summary = "根据ID查询用户", description = "根据用户ID查询用户详细信息")
    public UserDTO findById(
            @PathVariable
            @Parameter(description = "用户ID")
            @NotNull(message = "用户ID不能为空") Long id) {

        log.debug("开始调用用户服务查询用户信息, id: {}", id);

        // 直接委托给Service层，享受多级缓存和事务管理
        return userService.getUserById(id);
    }

    @PostMapping("/internal/register")
    @Operation(summary = "用户注册", description = "注册新用户，支持普通用户和商家用户注册")
    public UserDTO register(
            @RequestBody
            @Parameter(description = "注册请求信息")
            @Valid @NotNull(message = "注册请求信息不能为空") RegisterRequestDTO registerRequest) {

        log.info("🚀 用户注册开始, username: {}, userType: {}",
                registerRequest.getUsername(), registerRequest.getUserType());

        // 直接委托给Service层处理，享受完整的事务管理和缓存策略
        return userService.registerUser(registerRequest);
    }

    @PutMapping("/internal/update")
    @Operation(summary = "更新用户信息", description = "更新用户信息")
    public Boolean update(
            @RequestBody
            @Parameter(description = "用户信息")
            @Valid @NotNull(message = "用户信息不能为空") UserDTO userDTO) {

        log.debug("开始调用用户服务更新用户信息, userId: {}", userDTO.getId());

        // 直接委托给Service层，享受多级缓存删除和事务管理
        return userService.updateById(userConverter.toEntity(userDTO));
    }

    @GetMapping("/internal/password/{username}")
    @Operation(summary = "获取用户密码", description = "仅供 auth-service 认证使用")
    public String getUserPassword(
            @PathVariable
            @Parameter(description = "用户名")
            @NotBlank(message = "用户名不能为空") String username) {

        log.debug("获取用户密码: {}", username);

        // 直接委托给Service层，享受缓存策略和事务管理
        return userService.getUserPassword(username);
    }

    @GetMapping("/internal/github-id/{githubId}")
    @Operation(summary = "根据GitHub ID查询用户", description = "仅供 auth-service GitHub OAuth 使用")
    public UserDTO findByGitHubId(
            @PathVariable
            @Parameter(description = "GitHub用户ID")
            @NotNull(message = "GitHub用户ID不能为空") Long githubId) {

        log.debug("根据GitHub ID查询用户: {}", githubId);

        // 直接委托给Service层，享受多级缓存和事务管理
        return userService.findByGitHubId(githubId);
    }

    @PostMapping("/internal/github/create")
    @Operation(summary = "创建GitHub OAuth用户", description = "仅供 auth-service GitHub OAuth 使用")
    public UserDTO createGitHubUser(
            @RequestBody
            @Parameter(description = "GitHub用户信息")
            @Valid @NotNull(message = "GitHub用户信息不能为空") com.cloud.common.domain.dto.oauth.GitHubUserDTO githubUserDTO) {

        log.info("🚀 创建GitHub OAuth用户, githubId: {}, login: {}", 
                githubUserDTO.getGithubId(), githubUserDTO.getLogin());

        // 直接委托给Service层处理，享受完整的事务管理和缓存策略
        return userService.createGitHubUser(githubUserDTO);
    }

    @PutMapping("/internal/github/update/{userId}")
    @Operation(summary = "更新GitHub OAuth用户信息", description = "仅供 auth-service GitHub OAuth 使用")
    public Boolean updateGitHubUserInfo(
            @PathVariable
            @Parameter(description = "系统用户ID")
            @NotNull(message = "用户ID不能为空") Long userId,
            @RequestBody
            @Parameter(description = "GitHub用户信息")
            @Valid @NotNull(message = "GitHub用户信息不能为空") com.cloud.common.domain.dto.oauth.GitHubUserDTO githubUserDTO) {

        log.debug("更新GitHub用户信息, userId: {}, githubId: {}", userId, githubUserDTO.getGithubId());

        // 直接委托给Service层，享受多级缓存清除和事务管理
        return userService.updateGitHubUserInfo(userId, githubUserDTO);
    }

}
