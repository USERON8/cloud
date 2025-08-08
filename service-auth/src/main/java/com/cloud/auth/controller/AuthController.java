package com.cloud.auth.controller;

import com.cloud.api.user.UserServiceInternal;
import com.cloud.auth.service.AuthService;
import com.cloud.auth.service.TokenService;
import com.cloud.common.domain.Result;
import com.cloud.common.domain.dto.LoginRequestDTO;
import com.cloud.common.domain.dto.LoginResponseDTO;
import com.cloud.common.domain.dto.RegisterRequestDTO;
import com.cloud.common.domain.dto.UserDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
@CrossOrigin
@Tag(name = "认证授权", description = "用户认证和授权相关接口")
public class AuthController {
    
    @DubboReference
    private final UserServiceInternal userServiceInternal;
    private final AuthService authService;
    private final TokenService tokenService;

    public AuthController(UserServiceInternal userServiceInternal,
                          AuthService authService,
                          TokenService tokenService) {
        this.userServiceInternal = userServiceInternal;
        this.authService = authService;
        this.tokenService = tokenService;
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "注册新用户")
    @ApiResponse(responseCode = "200", description = "注册成功")
    public Result<Void> register(@Parameter(description = "用户注册信息") @Valid @RequestBody RegisterRequestDTO registerRequest) {
        try {
            log.info("用户注册请求, username: {}", registerRequest.getUsername());
            authService.register(registerRequest);
            return Result.success();
        } catch (Exception e) {
            log.error("注册失败, username: {}", registerRequest.getUsername(), e);
            return Result.error("注册失败，请稍后重试");
        }
    }

    @PostMapping("/register-and-login")
    @Operation(summary = "用户注册并登录", description = "注册新用户并自动登录")
    @ApiResponse(responseCode = "200", description = "注册并登录成功",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponseDTO.class)))
    public Result<LoginResponseDTO> registerAndLogin(@Parameter(description = "用户注册信息") @Valid @RequestBody RegisterRequestDTO registerRequest) {
        try {
            log.info("用户注册并登录请求, username: {}", registerRequest.getUsername());
            LoginResponseDTO response = authService.registerAndLogin(registerRequest);
            return Result.success(response);
        } catch (Exception e) {
            log.error("注册并登录失败, username: {}", registerRequest.getUsername(), e);
            return Result.error("注册并登录失败，请稍后重试");
        }
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户登录系统")
    @ApiResponse(responseCode = "200", description = "登录成功",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponseDTO.class)))
    public Result<LoginResponseDTO> login(@Parameter(description = "登录请求信息") @Valid @RequestBody LoginRequestDTO loginRequest) {
        try {
            log.info("用户登录请求, username: {}", loginRequest.getUsername());
            LoginResponseDTO response = authService.login(loginRequest);
            return Result.success(response);
        } catch (Exception e) {
            log.error("登录失败, username: {}", loginRequest.getUsername(), e);
            return Result.error("登录失败，请检查用户名和密码");
        }
    }

    @PostMapping("/change-password")
    @Operation(summary = "修改密码", description = "修改用户密码")
    @ApiResponse(responseCode = "200", description = "密码修改成功")
    public ResponseEntity<String> changePassword(@Parameter(description = "密码修改请求信息") @Valid @RequestBody ChangePasswordRequest request,
                                                 @RequestHeader("Authorization") String authorizationHeader) {
        log.info("修改密码请求");

        try {
            // 验证token并获取用户ID
            Long userId = validateTokenAndGetUserId(authorizationHeader);
            if (userId == null) {
                return ResponseEntity.badRequest().body("无效的认证信息");
            }

            userServiceInternal.updatePassword(userId, request.getNewPassword());
            return ResponseEntity.ok("密码修改成功");
        } catch (Exception e) {
            log.error("修改密码失败", e);
            return ResponseEntity.badRequest().body("密码修改失败: " + e.getMessage());
        }
    }

    @GetMapping("/user-info")
    @Operation(summary = "获取当前用户信息", description = "根据token获取当前用户信息")
    @ApiResponse(responseCode = "200", description = "获取用户信息成功",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO.class)))
    public Result<UserDTO> getCurrentUserInfo(@RequestHeader("Authorization") String authorizationHeader) {
        try {
            log.info("获取当前用户信息请求");

            // 验证token并获取用户ID
            Long userId = validateTokenAndGetUserId(authorizationHeader);
            if (userId == null) {
                return Result.error("无效的认证信息");
            }

            UserDTO userDTO = userServiceInternal.findById(userId);
            return Result.success(userDTO);
        } catch (Exception e) {
            log.error("获取用户信息失败", e);
            return Result.error("获取用户信息失败: " + e.getMessage());
        }
    }

    @GetMapping("/validate-token")
    @Operation(summary = "验证token有效性", description = "验证JWT token是否有效")
    @ApiResponse(responseCode = "200", description = "token验证结果")
    public Result<Boolean> validateToken(@RequestHeader("Authorization") String authorizationHeader) {
        try {
            log.info("验证token有效性请求");
            Long userId = validateTokenAndGetUserId(authorizationHeader);
            return Result.success(userId != null);
        } catch (Exception e) {
            log.error("验证token有效性失败", e);
            return Result.error("token验证失败: " + e.getMessage());
        }
    }

    @GetMapping("/logout")
    @Operation(summary = "用户登出", description = "用户登出系统")
    @ApiResponse(responseCode = "200", description = "登出成功")
    public Result<Void> logout(@RequestHeader("Authorization") String authorizationHeader) {
        try {
            log.info("用户登出");

            // 验证token并获取用户ID
            Long userId = validateTokenAndGetUserId(authorizationHeader);
            if (userId != null) {
                String token = extractToken(authorizationHeader);
                authService.logout(token);
                log.info("用户token已从Redis中删除，用户ID: {}", userId);
            } else {
                log.warn("尝试删除无效的token");
            }

            log.info("用户登出成功");
            return Result.success();
        } catch (Exception e) {
            log.error("用户登出失败", e);
            return Result.error("登出失败: " + e.getMessage());
        }
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "刷新token", description = "使用现有token获取新的token")
    @ApiResponse(responseCode = "200", description = "token刷新成功",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponseDTO.class)))
    public Result<LoginResponseDTO> refreshToken(@RequestHeader("Authorization") String authorizationHeader) {
        try {
            log.info("刷新token请求");

            // 验证token并获取用户ID
            Long userId = validateTokenAndGetUserId(authorizationHeader);
            if (userId == null) {
                log.warn("尝试刷新token时发现无效的token");
                return Result.error("无效的认证信息");
            }

            String token = extractToken(authorizationHeader);
            // 刷新token有效期
            if (tokenService.refreshToken(token)) {
                // 获取token过期时间
                long expiresIn = tokenService.getExpireTime(token);
                // 创建响应对象
                LoginResponseDTO response = new LoginResponseDTO(token, expiresIn, "", "");
                return Result.success(response);
            } else {
                return Result.error("token刷新失败");
            }
        } catch (Exception e) {
            log.error("刷新token失败", e);
            return Result.error("token刷新失败: " + e.getMessage());
        }
    }

    /**
     * 从Authorization头中提取token并验证其有效性
     * @param authorizationHeader Authorization头
     * @return 用户ID，如果token无效则返回null
     */
    private Long validateTokenAndGetUserId(String authorizationHeader) {
        String token = extractToken(authorizationHeader);
        if (token != null) {
            return tokenService.validateToken(token);
        }
        return null;
    }

    /**
     * 从Authorization头中提取token
     * @param authorizationHeader Authorization头
     * @return token字符串，如果无效则返回null
     */
    private String extractToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }

    // 密码修改请求的数据传输对象
    @Setter
    @Getter
    public static class ChangePasswordRequest {
        @Parameter(description = "旧密码")
        private String oldPassword;

        @Parameter(description = "新密码")
        private String newPassword;
    }
}