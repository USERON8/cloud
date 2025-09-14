package com.cloud.auth.controller;

import com.cloud.api.user.UserFeignClient;
import com.cloud.auth.service.OAuth2TokenManagementService;
import com.cloud.auth.util.OAuth2ResponseUtil;
import com.cloud.common.domain.dto.auth.LoginRequestDTO;
import com.cloud.common.domain.dto.auth.LoginResponseDTO;
import com.cloud.common.domain.dto.auth.RegisterRequestDTO;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.enums.ResultCode;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 * 提供简化的用户认证相关操作，包括注册、登录、登出等
 * <p>
 * 注意：这些接口为简化登录流程，生产环境建议使用标准OAuth2流程：
 * - 授权码模式：/oauth2/authorize -> /oauth2/token
 * - 客户端凭证模式：/oauth2/token 传递 grant_type=client_credentials
 *
 * @author what's up
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/v1/auth")
@Tag(name = "认证服务接口", description = "用户认证、登录注册、令牌管理相关的 RESTful API 接口")
public class AuthController {
    private final UserFeignClient userFeignClient;
    private final OAuth2TokenManagementService tokenManagementService;
    private final PasswordEncoder passwordEncoder;

    /**
     * 用户注册接口（简化版）
     * 检查用户是否存在，如果不存在则注册新用户并返回OAuth2令牌
     * <p>
     * 注意：这是一个简化接口，内部使用授权码模式生成令牌
     * 生产环境建议使用标准OAuth2授权码流程
     *
     * @param registerRequestDTO 用户注册请求参数
     * @return 注册结果包含OAuth2令牌
     */
    @PostMapping("/register")
    @Operation(
            summary = "用户注册", 
            description = "注册新用户并返回 OAuth2 令牌"
    )
    @ApiResponse(
            responseCode = "200", 
            description = "注册成功",
            content = @Content(mediaType = "application/json", 
                    schema = @Schema(implementation = Result.class))
    )
    public Result<LoginResponseDTO> register(
            @RequestBody
            @Parameter(description = "用户注册信息", required = true)
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
            LoginResponseDTO response = OAuth2ResponseUtil.buildLoginResponse(authorization, registeredUser);
            return Result.success(response);
        } else {
            log.warn("用户注册失败，用户名已存在或服务不可用, username: {}", registerRequestDTO.getUsername());
            return Result.error(ResultCode.USER_ALREADY_EXISTS.getCode(), "用户名已存在或服务不可用");
        }
    }

    /**
     * 用户登录接口（带密码验证）
     * 验证用户名和密码后返回OAuth2令牌
     * <p>
     * 注意：这是一个简化接口，内部使用授权码模式生成令牌
     * 生产环境建议使用标准OAuth2授权码流程
     *
     * @param loginRequestDTO 用户登录请求参数
     * @return 登录结果包含OAuth2令牌
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "验证用户名密码后返回OAuth2.1标准令牌")
    public Result<LoginResponseDTO> login(
            @RequestBody 
            @Parameter(description = "用户登录信息", required = true)
            @Valid @NotNull(message = "登录信息不能为空") LoginRequestDTO loginRequestDTO) {
        
        String username = loginRequestDTO.getUsername();
        log.info("🔐 用户登录请求开始, username: {}, userType: {}", username, loginRequestDTO.getUserType());

        try {
            // 1. 参数验证
            validateLoginRequest(loginRequestDTO);
            
            // 2. 检查用户是否存在
            UserDTO user = userFeignClient.findByUsername(username);
            if (user == null) {
                log.warn("❌ 用户登录失败，用户不存在, username: {}", username);
                return Result.error(ResultCode.USER_NOT_FOUND.getCode(), "用户名或密码错误");
            }

            // 3. 验证用户状态（先验证状态，再验证密码）
            if (user.getStatus() == null || user.getStatus() != 1) {
                log.warn("❌ 用户登录失败，账户已被禁用, username: {}, status: {}", username, user.getStatus());
                return Result.error(ResultCode.USER_DISABLED.getCode(), "账户已被禁用");
            }
            
            // 4. 验证用户类型（如果请求中指定了用户类型）
            if (loginRequestDTO.getUserType() != null && 
                !loginRequestDTO.getUserType().trim().isEmpty() &&
                !loginRequestDTO.getUserType().equals(user.getUserType())) {
                log.warn("❌ 用户登录失败，用户类型不匹配, username: {}, requestedType: {}, actualType: {}",
                        username, loginRequestDTO.getUserType(), user.getUserType());
                return Result.error(ResultCode.USER_TYPE_MISMATCH.getCode(), "用户类型不匹配");
            }

            // 5. 验证密码（放在最后验证，避免无效请求的性能影响）
            String storedPassword = userFeignClient.getUserPassword(username);
            if (storedPassword == null) {
                log.warn("❌ 用户登录失败，无法获取用户密码, username: {}", username);
                return Result.error(ResultCode.USER_NOT_FOUND.getCode(), "用户名或密码错误");
            }

            // 使用PasswordEncoder验证密码
            if (!passwordEncoder.matches(loginRequestDTO.getPassword(), storedPassword)) {
                log.warn("❌ 用户登录失败，密码错误, username: {}", username);
                // 密码错误可以考虑加入登录尝试限制逻辑
                return Result.error(ResultCode.PASSWORD_ERROR.getCode(), "用户名或密码错误");
            }

            // 6. 生成OAuth2.1符合的令牌（含 PKCE 支持）
            OAuth2Authorization authorization = tokenManagementService.generateTokensForUser(user, null);
            log.info("✅ 用户登录成功, username: {}, scopes: {}, tokenId: {}", 
                    username, authorization.getAuthorizedScopes(), 
                    authorization.getId().substring(0, 8) + "...");

            // 7. 返回标准OAuth2.1响应
            LoginResponseDTO response = OAuth2ResponseUtil.buildLoginResponse(authorization, user);
            return Result.success(response);
            
        } catch (Exception e) {
            log.error("💥 用户登录过程中发生异常, username: {}", username, e);
            return Result.error(ResultCode.SYSTEM_ERROR.getCode(), "登录失败，系统异常");
        }
    }
    
    /**
     * 验证登录请求参数
     * 
     * @param loginRequestDTO 登录请求
     */
    private void validateLoginRequest(LoginRequestDTO loginRequestDTO) {
        if (loginRequestDTO.getUsername() == null || loginRequestDTO.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (loginRequestDTO.getPassword() == null || loginRequestDTO.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }
    }

    /**
     * 用户注册并自动登录接口
     * 注册新用户并返回OAuth2.0标准的访问令牌信息
     *
     * @param registerRequestDTO 用户注册请求参数
     * @return 登录响应信息（包含访问令牌等）
     */
    @RequestMapping("/register-and-login")
    public Result<LoginResponseDTO> registerAndLogin(@RequestBody @NotNull RegisterRequestDTO registerRequestDTO) {
        log.info("用户注册并登录开始, username: {}, userType: {}", registerRequestDTO.getUsername(), registerRequestDTO.getUserType());

        try {
            // 尝试注册用户
            UserDTO registeredUser = userFeignClient.register(registerRequestDTO);

            if (registeredUser != null) {
                log.info("用户注册并登录成功, username: {}, userId: {}, userType: {}",
                        registerRequestDTO.getUsername(), registeredUser.getId(), registeredUser.getUserType());
                // 通过Authorization Server生成并入库令牌
                OAuth2Authorization authorization = tokenManagementService.generateTokensForUser(registeredUser, null);
                LoginResponseDTO response = OAuth2ResponseUtil.buildLoginResponse(authorization, registeredUser);
                return Result.success(response);
            } else {
                log.warn("用户注册并登录失败，用户名已存在或服务不可用, username: {}", registerRequestDTO.getUsername());
                return Result.error(ResultCode.USER_ALREADY_EXISTS.getCode(), "用户名已存在或服务不可用");
            }
        } catch (Exception e) {
            log.error("用户注册并登录异常, username: {}", registerRequestDTO.getUsername(), e);
            return Result.error(ResultCode.SYSTEM_ERROR.getCode(), "注册并登录失败，请稍后重试");
        }
    }

    /**
     * 用户登出接口
     * 撤销指定的访问令牌，使其立即失效
     *
     * @param request HTTP请求（从中提取Authorization头）
     * @return 登出结果
     */
    @RequestMapping("/logout")
    public Result<Void> logout(jakarta.servlet.http.HttpServletRequest request) {
        try {
            // 从请求头中提取令牌
            String authorizationHeader = request.getHeader("Authorization");
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                log.warn("登出请求缺少有效的Authorization头");
                return Result.error(ResultCode.UNAUTHORIZED.getCode(), "请求头中缺少有效的访问令牌");
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
                return Result.error(ResultCode.UNAUTHORIZED.getCode(), "令牌无效或已过期");
            }

        } catch (Exception e) {
            log.error("用户登出异常", e);
            return Result.error(ResultCode.SYSTEM_ERROR.getCode(), "登出失败，系统异常");
        }
    }

    /**
     * 批量登出接口（撤销用户的所有会话）
     * 需要管理员权限或用户本人操作
     *
     * @param username 要登出的用户名
     * @return 登出结果
     */
    @PostMapping("/logout-all")
    @Operation(
            summary = "批量登出", 
            description = "撤销用户的所有活跃会话"
    )
    public Result<String> logoutAllSessions(
            @RequestParam("username")
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
     * 用于其他服务验证令牌是否有效
     *
     * @param request HTTP请求（从中提取Authorization头）
     * @return 验证结果
     */
    @RequestMapping("/validate-token")
    public Result<String> validateToken(jakarta.servlet.http.HttpServletRequest request) {
        try {
            // 从请求头中提取令牌
            String authorizationHeader = request.getHeader("Authorization");
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                return Result.error(ResultCode.UNAUTHORIZED.getCode(), "请求头中缺少有效的访问令牌");
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

            return Result.error(ResultCode.UNAUTHORIZED.getCode(), "令牌无效或已过期");

        } catch (Exception e) {
            log.error("令牌验证异常", e);
            return Result.error(ResultCode.SYSTEM_ERROR.getCode(), "令牌验证失败，系统异常");
        }
    }

    /**
     * 令牌刷新接口
     * 使用刷新令牌获取新的访问令牌
     *
     * @param refreshToken 刷新令牌
     * @return 新的访问令牌信息
     */
    @RequestMapping("/refresh-token")
    public Result<LoginResponseDTO> refreshToken(@RequestParam("refresh_token") String refreshToken) {
        try {
            if (refreshToken == null || refreshToken.trim().isEmpty()) {
                return Result.error(ResultCode.PARAM_ERROR.getCode(), "刷新令牌不能为空");
            }

            log.info("开始刷新令牌, refreshTokenPrefix: {}",
                    refreshToken.substring(0, Math.min(refreshToken.length(), 10)) + "...");

            // 获取现有授权信息
            OAuth2Authorization existingAuth = tokenManagementService.findByToken(refreshToken);
            if (existingAuth == null) {
                return Result.error(ResultCode.UNAUTHORIZED.getCode(), "刷新令牌无效");
            }

            // 获取用户信息重新生成令牌
            String username = existingAuth.getPrincipalName();
            UserDTO user = userFeignClient.findByUsername(username);
            if (user == null) {
                return Result.error(ResultCode.USER_NOT_FOUND.getCode(), "用户不存在");
            }

            // 撤销旧的授权并生成新的
            tokenManagementService.revokeToken(refreshToken);
            OAuth2Authorization newAuth = tokenManagementService.generateTokensForUser(user, existingAuth.getAuthorizedScopes());

            log.info("令牌刷新成功, username: {}", username);

            LoginResponseDTO response = OAuth2ResponseUtil.buildLoginResponse(newAuth, user);
            return Result.success(response);

        } catch (Exception e) {
            log.error("令牌刷新异常", e);
            return Result.error(ResultCode.SYSTEM_ERROR.getCode(), "令牌刷新失败，系统异常");
        }
    }

}
