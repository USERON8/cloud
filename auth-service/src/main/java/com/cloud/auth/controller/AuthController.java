package com.cloud.auth.controller;

import com.cloud.api.user.UserFeign;
import com.cloud.auth.service.AuthService;
import com.cloud.auth.service.JwtService;
import com.cloud.common.domain.Result;
import com.cloud.common.domain.dto.*;
import com.cloud.common.enums.ResultCode;
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
    private final UserFeign userFeign;

    public AuthController(JwtService jwtService, AuthenticationManager authenticationManager, AuthService authService, UserFeign userFeign) {
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.authService = authService;
        this.userFeign = userFeign;
    }


    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "注册新用户")
    @ApiResponse(responseCode = "200", description = "注册成功")
    public Result<LoginResponseDTO> register(@Parameter(description = "用户注册信息") @Valid @RequestBody RegisterRequestDTO registerRequest) {
        try {
            log.info("用户注册, username: {}", registerRequest.getUsername());
            LoginResponseDTO response = authService.registerAndLogin(registerRequest);
            return Result.success("注册成功", response);
        } catch (Exception e) {
            log.error("用户注册失败, username: {}", registerRequest.getUsername(), e);
            return Result.error(ResultCode.SYSTEM_ERROR.getCode(), "注册失败: " + e.getMessage());
        }
    }

    @PostMapping("/register-and-login")
    @Operation(summary = "用户注册并登录", description = "注册新用户并自动登录")
    @ApiResponse(responseCode = "200", description = "注册并登录成功",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponseDTO.class)))
    public Result<LoginResponseDTO> registerAndLogin(@Parameter(description = "用户注册信息") @Valid @RequestBody RegisterRequestDTO registerRequest) {
        try {
            log.info("用户注册并登录, username: {}", registerRequest.getUsername());
            LoginResponseDTO response = authService.registerAndLogin(registerRequest);
            return Result.success("注册并登录成功", response);
        } catch (Exception e) {
            log.error("用户注册并登录失败, username: {}", registerRequest.getUsername(), e);
            return Result.error(ResultCode.SYSTEM_ERROR.getCode(), "注册并登录失败: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户登录系统")
    @ApiResponse(responseCode = "200", description = "登录成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = LoginResponseDTO.class)))
    public Result<LoginResponseDTO> login(@Parameter(description = "登录请求信息") @Valid @RequestBody LoginRequestDTO loginRequest) {
        try {
            log.info("用户登录, username: {}", loginRequest.getUsername());
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserDTO userDTO = userFeign.findByUsername(loginRequest.getUsername());
            String token = jwtService.generateToken(authentication);
            long expiresIn = jwtService.getExpirationTime();
            LoginResponseDTO response = new LoginResponseDTO(token, expiresIn, userDTO.getUserType(), userDTO.getNickname());
            return Result.success("登录成功", response);
        } catch (Exception e) {
            log.error("用户登录失败, username: {}", loginRequest.getUsername(), e);
            return Result.error(ResultCode.SYSTEM_ERROR.getCode(), "登录失败: " + e.getMessage());
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "用户登出系统")
    public Result<Void> logout() {
        try {
            log.info("用户登出");
            SecurityContextHolder.clearContext();
            return Result.success();
        } catch (Exception e) {
            log.error("用户登出失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR.getCode(), "登出失败: " + e.getMessage());
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新令牌", description = "刷新用户访问令牌")
    public Result<LoginResponseDTO> refresh() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            log.info("刷新令牌, username: {}", authentication.getName());
            String token = jwtService.generateToken(authentication);
            long expiresIn = jwtService.getExpirationTime();
            UserDTO userDTO = userFeign.findByUsername(authentication.getName());
            LoginResponseDTO response = new LoginResponseDTO(token, expiresIn, userDTO.getUserType(), userDTO.getNickname());
            return Result.success("令牌刷新成功", response);
        } catch (Exception e) {
            log.error("刷新令牌失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR.getCode(), "刷新令牌失败: " + e.getMessage());
        }
    }

    @PostMapping("/register-merchant")
    @Operation(summary = "商家注册", description = "注册新商家，需要上传营业执照和身份证正反面图片，注册后需要管理员审核")
    @ApiResponse(responseCode = "200", description = "注册成功")
    public Result<LoginResponseDTO> registerMerchant(@Parameter(description = "商家注册信息") @Valid @ModelAttribute MerchantRegisterRequestDTO registerRequest) {
        try {
            log.info("商家注册, username: {}", registerRequest.getUsername());

            // 先检查用户是否已存在
            UserDTO existingUser = userFeign.findByUsername(registerRequest.getUsername());
            if (existingUser != null) {
                log.warn("用户已存在: {}", registerRequest.getUsername());
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "用户已存在");
            }
            
            // 注册用户（默认禁用状态）
            RegisterRequestDTO userRegisterRequest = new RegisterRequestDTO();
            userRegisterRequest.setUsername(registerRequest.getUsername());
            userRegisterRequest.setPassword(registerRequest.getPassword());
            userRegisterRequest.setEmail(registerRequest.getEmail());
            userRegisterRequest.setPhone(registerRequest.getPhone());
            userRegisterRequest.setNickname(registerRequest.getNickname());
            userRegisterRequest.setUserType("MERCHANT");

            // 注册用户（默认为禁用状态，等待审核）
            userFeign.register(userRegisterRequest);

            // 用户注册成功后自动登录
            LoginRequestDTO loginRequest = new LoginRequestDTO();
            loginRequest.setUsername(registerRequest.getUsername());
            loginRequest.setPassword(registerRequest.getPassword());

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserDTO userDTO = userFeign.findByUsername(loginRequest.getUsername());
            String token = jwtService.generateToken(authentication);
            long expiresIn = jwtService.getExpirationTime();
            LoginResponseDTO response = new LoginResponseDTO(token, expiresIn, userDTO.getUserType(), userDTO.getNickname());

            return Result.success("注册成功，请上传认证材料等待审核", response);
        } catch (Exception e) {
            log.error("商家注册失败, username: {}", registerRequest.getUsername(), e);
            return Result.error(ResultCode.SYSTEM_ERROR.getCode(), "注册失败: " + e.getMessage());
        }
    }
}