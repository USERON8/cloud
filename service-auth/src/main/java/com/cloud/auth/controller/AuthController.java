package com.cloud.auth.controller;

import com.cloud.auth.service.AuthService;
import com.cloud.common.domain.Result;
import com.cloud.common.domain.dto.ChangePasswordRequest;
import com.cloud.common.domain.dto.LoginRequestDTO;
import com.cloud.common.domain.dto.LoginResponseDTO;
import com.cloud.common.domain.dto.RegisterRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
@CrossOrigin
@Tag(name = "认证授权", description = "用户认证和授权相关接口")
public class AuthController {


    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
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
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = LoginResponseDTO.class)))
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
    public Result<Void> changePassword(@RequestBody @Valid ChangePasswordRequest changePasswordRequest) {
        try {
            log.info("修改密码请求");
            authService.changePassword(changePasswordRequest);
            return Result.success();
        } catch (Exception e) {
            log.error("修改密码失败", e);
            return Result.error("修改密码失败: " + e.getMessage());
        }
    }

}