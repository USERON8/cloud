package com.cloud.auth.controller;

import com.cloud.api.user.UserFeignClient;
import com.cloud.auth.service.AuthService;
import com.cloud.auth.service.JwtService;
import com.cloud.common.domain.Result;
import com.cloud.common.domain.dto.auth.LoginRequestDTO;
import com.cloud.common.domain.dto.auth.LoginResponseDTO;
import com.cloud.common.domain.dto.auth.RegisterRequestDTO;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.enums.UserType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
@CrossOrigin
@Tag(name = "认证授权", description = "用户认证和授权相关接口")
public class AuthController {
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final AuthService authService;
    private final UserFeignClient userFeignClient;

    public AuthController(JwtService jwtService, AuthenticationManager authenticationManager, AuthService authService, UserFeignClient userFeignClient) {
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.authService = authService;
        this.userFeignClient = userFeignClient;
    }


    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "注册新用户")
    @ApiResponse(responseCode = "200", description = "注册成功")
    public Result<Void> register(@Parameter(description = "用户注册信息") @Valid @RequestBody RegisterRequestDTO registerRequest) {
        try {
            log.info("用户注册请求, username: {}, userType: {}", registerRequest.getUsername(), registerRequest.getUserType());

            // 检查用户类型是否合法（只允许普通用户和商家注册）
            String userTypeStr = registerRequest.getUserType();
            UserType userType = UserType.valueOf(userTypeStr);
            if (userType != UserType.USER && userType != UserType.MERCHANT) {
                log.warn("不支持的用户类型注册, username: {}, userType: {}", registerRequest.getUsername(), userType);
                return Result.error(ResultCode.PARAM_ERROR.getCode(), "不支持的用户类型，只允许注册普通用户或商家");
            }

            // 执行注册
            authService.register(registerRequest);
            log.info("用户注册成功, username: {}, userType: {}", registerRequest.getUsername(), userType);
            return Result.success();
        } catch (Exception e) {
            log.error("注册失败, username: {}", registerRequest.getUsername(), e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "注册失败: " + e.getMessage());
        }
    }

    @PostMapping("/register-and-login")
    @Operation(summary = "用户注册并登录", description = "注册新用户并自动登录")
    @ApiResponse(responseCode = "200", description = "注册并登录成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = LoginResponseDTO.class)))
    public Result<LoginResponseDTO> registerAndLogin(@Parameter(description = "用户注册信息") @Valid @RequestBody RegisterRequestDTO registerRequest) {
        try {
            log.info("用户注册并登录请求, username: {}, userType: {}", registerRequest.getUsername(), registerRequest.getUserType());

            // 检查用户类型是否合法（只允许普通用户和商家注册）
            String userTypeStr = registerRequest.getUserType();
            UserType userType = UserType.valueOf(userTypeStr);
            if (userType != UserType.USER && userType != UserType.MERCHANT) {
                log.warn("不支持的用户类型注册并登录, username: {}, userType: {}", registerRequest.getUsername(), userType);
                return Result.error(ResultCode.PARAM_ERROR.getCode(), "不支持的用户类型，只允许注册普通用户或商家");
            }

            // 执行注册并登录
            LoginResponseDTO response = authService.registerAndLogin(registerRequest);
            log.info("用户注册并登录成功, username: {}, userType: {}", registerRequest.getUsername(), userType);
            return Result.success(response);
        } catch (Exception e) {
            log.error("注册并登录失败, username: {}", registerRequest.getUsername(), e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "注册并登录失败: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户登录接口")
    @ApiResponse(responseCode = "200", description = "登录成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = LoginResponseDTO.class)))
    public Result<LoginResponseDTO> login(@Parameter(description = "用户登录信息") @Valid @RequestBody LoginRequestDTO loginRequest) {
        try {
            log.info("用户登录请求, username: {}", loginRequest.getUsername());

            // 认证用户
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            // 设置认证信息到安全上下文
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 生成JWT令牌
            String token = jwtService.generateToken(authentication);
            long expiresIn = jwtService.getExpirationTime();

            // 获取用户信息
            UserDTO userDTO = userFeignClient.findByUsername(loginRequest.getUsername());
            if (userDTO == null) {
                log.warn("用户不存在, username: {}", loginRequest.getUsername());
                return Result.error(ResultCode.USER_NOT_FOUND);
            }

            log.info("用户登录成功, username: {}, userType: {}", loginRequest.getUsername(), userDTO.getUserType());
            return Result.success(new LoginResponseDTO(token, expiresIn, userDTO.getUserType(), userDTO.getNickname()));
        } catch (Exception e) {
            log.error("登录失败, username: {}", loginRequest.getUsername(), e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "登录失败: " + e.getMessage());
        }
    }
}