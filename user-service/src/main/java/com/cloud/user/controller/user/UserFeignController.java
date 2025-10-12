package com.cloud.user.controller.user;

import com.cloud.common.domain.dto.auth.RegisterRequestDTO;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.result.Result;
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
@RequestMapping("/internal/user")
@RequiredArgsConstructor
@Tag(name = "用户内部接口", description = "供其他服务调用的用户相关内部接口")
public class UserFeignController {
    private final UserService userService;
    private final UserConverter userConverter;

    @GetMapping("/username/{username}")
    @Operation(summary = "根据用户名查询用户", description = "根据用户名查询用户详细信息")
    public Result<UserDTO> findByUsername(
            @PathVariable
            @Parameter(description = "用户名")
            @NotBlank(message = "用户名不能为空") String username) {

        log.debug("开始调用用户服务查询用户信息, username: {}", username);

        try {
            UserDTO user = userService.findByUsername(username);
            if (user == null) {
                return Result.error(ResultCode.USER_NOT_FOUND);
            }
            return Result.success(user);
        } catch (Exception e) {
            log.error("查询用户失败，username: {}", username, e);
            return Result.systemError("查询用户失败");
        }
    }

    @GetMapping("/id/{id}")
    @Operation(summary = "根据ID查询用户", description = "根据用户ID查询用户详细信息")
    public Result<UserDTO> findById(
            @PathVariable
            @Parameter(description = "用户ID")
            @NotNull(message = "用户ID不能为空") Long id) {

        log.debug("开始调用用户服务查询用户信息, id: {}", id);

        try {
            UserDTO user = userService.getUserById(id);
            if (user == null) {
                return Result.error(ResultCode.USER_NOT_FOUND);
            }
            return Result.success(user);
        } catch (Exception e) {
            log.error("查询用户失败，id: {}", id, e);
            return Result.systemError("查询用户失败");
        }
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "注册新用户，支持普通用户和商家用户注册")
    public Result<UserDTO> register(
            @RequestBody
            @Parameter(description = "注册请求信息")
            @Valid @NotNull(message = "注册请求信息不能为空") RegisterRequestDTO registerRequest) {

        log.info("🚀 用户注册开始, username: {}, userType: {}",
                registerRequest.getUsername(), registerRequest.getUserType());

        try {
            UserDTO user = userService.registerUser(registerRequest);
            if (user == null) {
                return Result.error(ResultCode.USER_CREATE_FAILED);
            }
            return Result.success("用户注册成功", user);
        } catch (Exception e) {
            log.error("用户注册失败，username: {}", registerRequest.getUsername(), e);
            return Result.error(ResultCode.USER_CREATE_FAILED);
        }
    }

    @PutMapping("/update")
    @Operation(summary = "更新用户信息", description = "更新用户信息")
    public Result<Boolean> update(
            @RequestBody
            @Parameter(description = "用户信息")
            @Valid @NotNull(message = "用户信息不能为空") UserDTO userDTO) {

        log.debug("开始调用用户服务更新用户信息, userId: {}", userDTO.getId());

        try {
            boolean result = userService.updateById(userConverter.toEntity(userDTO));
            return Result.success("用户更新成功", result);
        } catch (Exception e) {
            log.error("更新用户信息失败，userId: {}", userDTO.getId(), e);
            return Result.error(ResultCode.USER_UPDATE_FAILED);
        }
    }

    @GetMapping("/password/{username}")
    @Operation(summary = "获取用户密码", description = "仅供 auth-service 认证使用")
    public Result<String> getUserPassword(
            @PathVariable
            @Parameter(description = "用户名")
            @NotBlank(message = "用户名不能为空") String username) {

        log.debug("获取用户密码: {}", username);

        try {
            String password = userService.getUserPassword(username);
            if (password == null) {
                return Result.error(ResultCode.USER_NOT_FOUND);
            }
            return Result.success(password);
        } catch (Exception e) {
            log.error("获取用户密码失败，username: {}", username, e);
            return Result.systemError("获取用户密码失败");
        }
    }

    @GetMapping("/github-id/{githubId}")
    @Operation(summary = "根据GitHub ID查询用户", description = "仅供 auth-service GitHub OAuth 使用")
    public Result<UserDTO> findByGitHubId(
            @PathVariable
            @Parameter(description = "GitHub用户ID")
            @NotNull(message = "GitHub用户ID不能为空") Long githubId) {

        log.debug("根据GitHub ID查询用户: {}", githubId);

        try {
            UserDTO user = userService.findByGitHubId(githubId);
            if (user == null) {
                return Result.error(ResultCode.USER_NOT_FOUND);
            }
            return Result.success(user);
        } catch (Exception e) {
            log.error("根据GitHub ID查询用户失败，githubId: {}", githubId, e);
            return Result.systemError("查询用户失败");
        }
    }

    @PostMapping("/github/create")
    @Operation(summary = "创建GitHub OAuth用户", description = "仅供 auth-service GitHub OAuth 使用")
    public Result<UserDTO> createGitHubUser(
            @RequestBody
            @Parameter(description = "GitHub用户信息")
            @Valid @NotNull(message = "GitHub用户信息不能为空") com.cloud.common.domain.dto.oauth.GitHubUserDTO githubUserDTO) {

        log.info("🚀 创建GitHub OAuth用户, githubId: {}, login: {}",
                githubUserDTO.getGithubId(), githubUserDTO.getLogin());

        try {
            UserDTO user = userService.createGitHubUser(githubUserDTO);
            if (user == null) {
                return Result.error(ResultCode.USER_CREATE_FAILED);
            }
            return Result.success("GitHub用户创建成功", user);
        } catch (Exception e) {
            log.error("创建GitHub用户失败，githubId: {}", githubUserDTO.getGithubId(), e);
            return Result.error(ResultCode.USER_CREATE_FAILED);
        }
    }

    @PutMapping("/github/update/{userId}")
    @Operation(summary = "更新GitHub OAuth用户信息", description = "仅供 auth-service GitHub OAuth 使用")
    public Result<Boolean> updateGitHubUserInfo(
            @PathVariable
            @Parameter(description = "系统用户ID")
            @NotNull(message = "用户ID不能为空") Long userId,
            @RequestBody
            @Parameter(description = "GitHub用户信息")
            @Valid @NotNull(message = "GitHub用户信息不能为空") com.cloud.common.domain.dto.oauth.GitHubUserDTO githubUserDTO) {

        log.debug("更新GitHub用户信息, userId: {}, githubId: {}", userId, githubUserDTO.getGithubId());

        try {
            boolean result = userService.updateGitHubUserInfo(userId, githubUserDTO);
            return Result.success("GitHub用户信息更新成功", result);
        } catch (Exception e) {
            log.error("更新GitHub用户信息失败，userId: {}, githubId: {}", userId, githubUserDTO.getGithubId(), e);
            return Result.error(ResultCode.USER_UPDATE_FAILED);
        }
    }

}