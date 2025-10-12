package com.cloud.auth.controller;

import com.cloud.api.user.UserFeignClient;
import com.cloud.auth.service.OAuth2TokenManagementService;
import com.cloud.auth.util.OAuth2ResponseUtil;
import com.cloud.common.domain.dto.auth.LoginRequestDTO;
import com.cloud.common.domain.dto.auth.LoginResponseDTO;
import com.cloud.common.domain.dto.auth.RegisterRequestDTO;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BusinessException;
import com.cloud.common.exception.ResourceNotFoundException;
import com.cloud.common.exception.ValidationException;
import com.cloud.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * OAuth2.1认证控制器
 * 提供简化的用户认证相关操作，严格遵循OAuth2.1标准
 * <p>
 * 特点：
 * - 不在控制器层捕获异常，由底层业务方法抛出
 * - 异常由全局异常处理器统一处理
 * - 支持OAuth2.1标准的PKCE、令牌轮转等特性
 * <p>
 * 推荐使用标准OAuth2.1流程：
 * - 授权码模式：/oauth2/authorize -> /oauth2/token
 * - 客户端凭证模式：/oauth2/token 传递 grant_type=client_credentials
 *
 * @author what's up
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/auth")
@Tag(name = "认证服务接口", description = "用户认证、登录注册、令牌管理相关的 RESTful API 接口")
public class AuthController {
    private final UserFeignClient userFeignClient;
    private final OAuth2TokenManagementService tokenManagementService;
    private final PasswordEncoder passwordEncoder;
    private final OAuth2ResponseUtil oauth2ResponseUtil;

    /**
     * 用户注册接口（OAuth2.1标准版）
     * 检查用户是否存在，如果不存在则注册新用户并返回OAuth2.1标准令牌
     * <p>
     * 遵循OAuth2.1标准，不捕获异常，由全局异常处理器统一处理
     * 生产环境推荐使用标准OAuth2.1授权码流程（PKCE支持）
     *
     * @param registerRequestDTO 用户注册请求参数
     * @return 注册结果包含OAuth2.1标准令牌
     * @throws UserAlreadyExistsException 用户已存在时抛出
     * @throws ValidationException        请求参数验证失败时抛出
     */
    @PostMapping("/users/register")
    @Operation(
            summary = "用户注册",
            description = "注册新用户并返回 OAuth2 令牌。注册成功后自动登录并返回访问令牌和刷新令牌。"
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "用户注册信息",
            required = true,
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = RegisterRequestDTO.class),
                    examples = {
                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "用户注册示例",
                                    summary = "正常注册用户的请求示例",
                                    value = """
                                            {
                                              "username": "newuser",
                                              "password": "password123",
                                              "email": "user@example.com",
                                              "phone": "13800138000",
                                              "nickname": "新用户",
                                              "userType": "USER"
                                            }
                                            """
                            )
                    }
            )
    )
    @ApiResponse(
            responseCode = "201",
            description = "注册成功",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = Result.class),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "注册成功响应",
                            value = """
                                    {
                                      "code": 201,
                                      "message": "注册成功",
                                      "data": {
                                        "accessToken": "eyJhbGciOiJSUzI1NiIs...",
                                        "refreshToken": "eyJhbGciOiJSUzI1NiIs...",
                                        "tokenType": "Bearer",
                                        "expiresIn": 7200,
                                        "userInfo": {
                                          "id": 1,
                                          "username": "newuser",
                                          "email": "user@example.com"
                                        }
                                      },
                                      "timestamp": 1704067200000
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "注册失败 - 用户已存在或参数无效",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "用户已存在错误",
                            value = """
                                    {
                                      "code": 1002,
                                      "message": "用户名已存在",
                                      "data": null,
                                      "timestamp": 1704067200000
                                    }
                                    """
                    )
            )
    )
    public Result<LoginResponseDTO> register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "用户注册信息",
                    required = true
            )
            @Valid
            @NotNull(message = "注册信息不能为空") RegisterRequestDTO registerRequestDTO) {

        log.info("用户注册开始, username: {}, userType: {}",
                registerRequestDTO.getUsername(), registerRequestDTO.getUserType());

        // 尝试注册用户，由底层服务保证原子性操作
        UserDTO registeredUser = userFeignClient.register(registerRequestDTO);

        if (registeredUser != null) {
            log.info("用户注册成功, username: {}, userId: {}, userType: {}",
                    registerRequestDTO.getUsername(), registeredUser.getId(), registeredUser.getUserType());
            // 通过Authorization Server生成并入库令牌
            OAuth2Authorization authorization = tokenManagementService.generateTokensForUser(registeredUser, null);
            LoginResponseDTO response = oauth2ResponseUtil.buildLoginResponse(authorization, registeredUser);
            return Result.success(response);
        } else {
            log.warn("用户注册失败，用户名已存在或服务不可用, username: {}", registerRequestDTO.getUsername());
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
        }
    }

    /**
     * 用户登录接口（带密码验证）
     * 验证用户名和密码后返回OAuth2.1标准令牌
     * <p>
     * 遵循OAuth2.1标准，支持PKCE、令牌轮转等安全特性
     * 不捕获异常，由全局异常处理器统一处理所有业务异常
     *
     * @param loginRequestDTO 用户登录请求参数
     * @return 登录结果包含OAuth2.1标准令牌
     * @throws AuthenticationException 认证失败时抛出
     * @throws ValidationException     请求参数验证失败时抛出
     */
    @PostMapping("/sessions")
    @Operation(
            summary = "用户登录",
            description = "验证用户名密码后返回OAuth2.1标准令牌。支持多种用户类型登录，包括普通用户和管理员。"
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "用户登录信息",
            required = true,
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = LoginRequestDTO.class),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "用户登录示例",
                            summary = "正常用户登录请求示例",
                            value = """
                                    {
                                      "username": "testuser",
                                      "password": "password123",
                                      "userType": "USER"
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(
            responseCode = "200",
            description = "登录成功",
            content = @Content(
                    schema = @Schema(implementation = Result.class),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "登录成功响应",
                            value = """
                                    {
                                      "code": 200,
                                      "message": "登录成功",
                                      "data": {
                                        "accessToken": "eyJhbGciOiJSUzI1NiIs...",
                                        "refreshToken": "eyJhbGciOiJSUzI1NiIs...",
                                        "tokenType": "Bearer",
                                        "expiresIn": 7200,
                                        "userInfo": {
                                          "id": 1,
                                          "username": "testuser",
                                          "email": "user@example.com",
                                          "userType": "USER"
                                        }
                                      },
                                      "timestamp": 1704067200000
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(
            responseCode = "401",
            description = "登录失败 - 用户名或密码错误",
            content = @Content(
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "认证失败错误",
                            value = """
                                    {
                                      "code": 1003,
                                      "message": "用户名或密码错误",
                                      "data": null,
                                      "timestamp": 1704067200000
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(
            responseCode = "403",
            description = "登录失败 - 账户已被禁用",
            content = @Content(
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "账户禁用错误",
                            value = """
                                    {
                                      "code": 1004,
                                      "message": "账户已被禁用",
                                      "data": null,
                                      "timestamp": 1704067200000
                                    }
                                    """
                    )
            )
    )
    public Result<LoginResponseDTO> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "用户登录信息",
                    required = true
            )
            @Valid @NotNull(message = "登录信息不能为空") LoginRequestDTO loginRequestDTO) {

        String username = loginRequestDTO.getUsername();
        log.info("🔐 用户登录请求开始, username: {}, userType: {}", username, loginRequestDTO.getUserType());

        // 1. 参数验证（抛出IllegalArgumentException）
        validateLoginRequest(loginRequestDTO);

        // 2. 检查用户是否存在（抛出ResourceNotFoundException）
        UserDTO user = userFeignClient.findByUsername(username);
        if (user == null) {
            log.warn("❌ 用户登录失败，用户不存在, username: {}", username);
            throw new ResourceNotFoundException("User", username);
        }

        // 3. 验证用户状态（抛出BusinessException）
        if (user.getStatus() == null || user.getStatus() != 1) {
            log.warn("❌ 用户登录失败，账户已被禁用, username: {}, status: {}", username, user.getStatus());
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        // 4. 验证用户类型（抛出BusinessException）
        if (loginRequestDTO.getUserType() != null &&
                !loginRequestDTO.getUserType().trim().isEmpty() &&
                !loginRequestDTO.getUserType().equals(user.getUserType())) {
            log.warn("❌ 用户登录失败，用户类型不匹配, username: {}, requestedType: {}, actualType: {}",
                    username, loginRequestDTO.getUserType(), user.getUserType());
            throw new BusinessException(ResultCode.USER_TYPE_MISMATCH);
        }

        // 5. 验证密码（抛出BusinessException）
        String storedPassword = userFeignClient.getUserPassword(username);
        if (storedPassword == null) {
            log.warn("❌ 用户登录失败，无法获取用户密码, username: {}", username);
            throw new ResourceNotFoundException("User password", username);
        }

        // 使用PasswordEncoder验证密码
        if (!passwordEncoder.matches(loginRequestDTO.getPassword(), storedPassword)) {
            log.warn("❌ 用户登录失败，密码错误, username: {}", username);
            // 密码错误可以考虑加入登录尝试限制逻辑
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }

        // 6. 生成OAuth2.1标准令牌（抛出OAuth2Exception）
        OAuth2Authorization authorization = tokenManagementService.generateTokensForUser(user, null);
        log.info("✅ 用户登录成功, username: {}, scopes: {}, tokenId: {}",
                username, authorization.getAuthorizedScopes(),
                authorization.getId().substring(0, 8) + "...");

        // 7. 返回标准OAuth2.1响应
        LoginResponseDTO response = oauth2ResponseUtil.buildLoginResponse(authorization, user);
        return Result.success(response);
    }

    /**
     * 验证登录请求参数
     *
     * @param loginRequestDTO 登录请求
     */
    private void validateLoginRequest(LoginRequestDTO loginRequestDTO) {
        if (loginRequestDTO.getUsername() == null || loginRequestDTO.getUsername().trim().isEmpty()) {
            throw new ValidationException("username", loginRequestDTO.getUsername(), "用户名不能为空");
        }
        if (loginRequestDTO.getPassword() == null || loginRequestDTO.getPassword().trim().isEmpty()) {
            throw new ValidationException("password", loginRequestDTO.getPassword(), "密码不能为空");
        }
    }

    /**
     * 用户注册并自动登录接口
     * 注册新用户并返回OAuth2.1标准的访问令牌信息
     * <p>
     * 遵循OAuth2.1标准，不捕获异常，由全局异常处理器统一处理
     *
     * @param registerRequestDTO 用户注册请求参数
     * @return 登录响应信息（包含访问令牌等）
     * @throws UserAlreadyExistsException 用户已存在时抛出
     * @throws ValidationException        请求参数验证失败时抛出
     */
    @PostMapping("/users/register-and-login")
    public Result<LoginResponseDTO> registerAndLogin(@RequestBody @NotNull RegisterRequestDTO registerRequestDTO) {
        log.info("用户注册并登录开始, username: {}, userType: {}", registerRequestDTO.getUsername(), registerRequestDTO.getUserType());

        // 直接尝试注册用户，不捕获异常
        UserDTO registeredUser = userFeignClient.register(registerRequestDTO);

        if (registeredUser != null) {
            log.info("用户注册并登录成功, username: {}, userId: {}, userType: {}",
                    registerRequestDTO.getUsername(), registeredUser.getId(), registeredUser.getUserType());

            // 通过OAuth2.1 Authorization Server生成并存储令牌
            OAuth2Authorization authorization = tokenManagementService.generateTokensForUser(registeredUser, null);
            LoginResponseDTO response = oauth2ResponseUtil.buildLoginResponse(authorization, registeredUser);
            return Result.success(response);
        } else {
            log.warn("用户注册并登录失败，用户名已存在或服务不可用, username: {}", registerRequestDTO.getUsername());
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
        }
    }

    /**
     * 用户登出接口
     * 撤销指定的访问令牌，使其立即失效
     * <p>
     * 遵循OAuth2.1标准，不捕获异常，由全局异常处理器统一处理
     *
     * @param request HTTP请求（从中提取Authorization头）
     * @return 登出结果
     * @throws InvalidTokenException 令牌无效时抛出
     * @throws MissingTokenException 缺少令牌时抛出
     */
    @DeleteMapping("/sessions")
    @Operation(
            summary = "用户登出",
            description = "撤销指定的访问令牌，使其立即失效。需要在请求头中提供Bearer令牌。"
    )
    @ApiResponse(
            responseCode = "200",
            description = "登出成功",
            content = @Content(
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "登出成功响应",
                            value = """
                                    {
                                      "code": 200,
                                      "message": "登出成功",
                                      "data": null,
                                      "timestamp": 1704067200000
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(
            responseCode = "401",
            description = "登出失败 - 令牌无效或缺失",
            content = @Content(
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "令牌无效错误",
                            value = """
                                    {
                                      "code": 1005,
                                      "message": "请求头中缺少有效的访问令牌",
                                      "data": null,
                                      "timestamp": 1704067200000
                                    }
                                    """
                    )
            )
    )
    public Result<Void> logout(jakarta.servlet.http.HttpServletRequest request) {
        // 从请求头中提取令牌
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            log.warn("登出请求缺少有效的Authorization头");
            throw new ValidationException("Authorization header", authorizationHeader, "请求头中缺少有效的访问令牌");
        }

        String accessToken = authorizationHeader.substring(7); // 移除"Bearer "前缀

        // 调用令牌管理服务撤销令牌
        boolean logoutSuccess = tokenManagementService.logout(accessToken, null);

        if (logoutSuccess) {
            log.info("用户登出成功, tokenPrefix: {}",
                    accessToken.substring(0, Math.min(accessToken.length(), 10)) + "...");
            return Result.success("登出成功", null);
        } else {
            log.warn("用户登出失败, tokenPrefix: {}",
                    accessToken.substring(0, Math.min(accessToken.length(), 10)) + "...");
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
    }

    /**
     * 批量登出接口（撤销用户的所有会话）
     * 需要管理员权限或用户本人操作
     *
     * @param username 要登出的用户名
     * @return 登出结果
     */
    @DeleteMapping("/users/{username}/sessions")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(
            summary = "批量登出",
            description = "撤销用户的所有活跃会话"
    )
    public Result<String> logoutAllSessions(
            @PathVariable
            @Parameter(description = "用户名", required = true)
            @NotBlank(message = "用户名不能为空") String username) {

        log.info("开始批量登出用户的所有会话, username: {}", username);

        int revokedCount = tokenManagementService.logoutAllSessions(username);

        String message = String.format("成功撤销用户 %s 的 %d 个活跃会话", username, revokedCount);
        log.info(message);

        return Result.success(message);
    }

    /**
     * 验证令牌有效性接口
     * 用于其他服务验证OAuth2.1令牌是否有效
     * <p>
     * 遵循OAuth2.1标准，不捕获异常，由全局异常处理器统一处理
     *
     * @param request HTTP请求（从中提取Authorization头）
     * @return 验证结果
     * @throws InvalidTokenException 令牌无效时抛出
     * @throws MissingTokenException 缺少令牌时抛出
     */
    @GetMapping("/tokens/validate")
    @PreAuthorize("isAuthenticated()")
    public Result<String> validateToken(jakarta.servlet.http.HttpServletRequest request) {
        // 从请求头中提取令牌
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ValidationException("Authorization header", authorizationHeader, "请求头中缺少有效的访问令牌");
        }

        String accessToken = authorizationHeader.substring(7);

        // 验证令牌有效性
        boolean isValid = tokenManagementService.isTokenValid(accessToken);

        if (isValid) {
            // 获取令牌详细信息
            OAuth2Authorization authorization = tokenManagementService.findByToken(accessToken);
            if (authorization != null) {
                String message = String.format("令牌有效, 用户: %s, 权限: %s",
                        authorization.getPrincipalName(),
                        String.join(", ", authorization.getAuthorizedScopes()));
                return Result.success(message);
            }
        }

        throw new BusinessException(ResultCode.UNAUTHORIZED);
    }

    /**
     * 令牌刷新接口
     * 使用刷新令牌获取新的访问令牌，遵循OAuth2.1标准
     * <p>
     * 遵循OAuth2.1标准，不捕获异常，由全局异常处理器统一处理
     * 支持令牌轮转（Token Rotation）特性，提高安全性
     * <p>
     * 注意：推荐使用标准OAuth2.1端点 POST /oauth2/token 进行令牌刷新：
     * - grant_type=refresh_token
     * - refresh_token={your_refresh_token}
     * - client_id={your_client_id}
     * - client_secret={your_client_secret}
     *
     * @param refreshToken 刷新令牌
     * @return 新的访问令牌信息
     * @throws InvalidTokenException 刷新令牌无效时抛出
     * @throws ValidationException   参数验证失败时抛出
     * @throws UserNotFoundException 用户不存在时抛出
     */
    @Operation(summary = "令牌刷新（简化版）",
            description = "使用刷新令牌获取新的访问令牌。推荐使用标准OAuth2.1端点 POST /oauth2/token")
    @PostMapping("/tokens/refresh")
    public Result<LoginResponseDTO> refreshToken(@RequestParam("refresh_token") String refreshToken) {
        // 参数验证
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new ValidationException("refresh_token", refreshToken, "刷新令牌不能为空");
        }

        log.info("开始刷新令牌, refreshTokenPrefix: {}",
                refreshToken.substring(0, Math.min(refreshToken.length(), 10)) + "...");

        // 获取现有授权信息
        OAuth2Authorization existingAuth = tokenManagementService.findByToken(refreshToken);
        if (existingAuth == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        // 获取用户信息重新生成令牌
        String username = existingAuth.getPrincipalName();
        UserDTO user = userFeignClient.findByUsername(username);
        if (user == null) {
            throw new ResourceNotFoundException("User", username);
        }

        // 撤销旧的授权并生成新的（OAuth2.1令牌轮转特性）
        tokenManagementService.revokeToken(refreshToken);
        OAuth2Authorization newAuth = tokenManagementService.generateTokensForUser(user, existingAuth.getAuthorizedScopes());

        log.info("令牌刷新成功, username: {}", username);

        LoginResponseDTO response = oauth2ResponseUtil.buildLoginResponse(newAuth, user);
        return Result.success(response);
    }
}