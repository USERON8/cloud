package com.cloud.auth.controller;

import com.cloud.api.user.UserService;
import com.cloud.auth.service.JwtService;
import com.cloud.common.domain.dto.LoginRequestDTO;
import com.cloud.common.domain.dto.LoginResponseDTO;
import com.cloud.common.domain.dto.UserDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.rpc.RpcContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
@CrossOrigin // 允许跨域
@Tag(name = "认证授权", description = "用户认证和授权相关接口")
public class AuthController {
    @Autowired
    private JwtService jwtService;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @DubboReference
    private UserService userService;

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "注册新用户")
    @ApiResponse(responseCode = "200", description = "注册成功")
    public ResponseEntity<Void> register(@Parameter(description = "用户信息") @RequestBody UserDTO userDTO) {
        log.info("用户注册, username: {}", userDTO.getUsername());
        userService.save(userDTO);
        log.info("用户注册成功, username: {}", userDTO.getUsername());
        return ResponseEntity.ok().build();
    }


    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户登录并获取JWT令牌")
    @ApiResponse(responseCode = "200", description = "登录成功", 
        content = @Content(mediaType = "application/json", 
        schema = @Schema(implementation = LoginResponseDTO.class)))
    public ResponseEntity<LoginResponseDTO> login(@Parameter(description = "登录请求信息") @RequestBody LoginRequestDTO loginRequest) {
        log.info("用户登录, username: {}", loginRequest.getUsername());
        
        // 认证用户
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );
        
        try {
            // 设置认证信息到安全上下文
            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserDTO userDTO = userService.findByUsername(loginRequest.getUsername());

            // 使用Dubbo上下文传递用户ID
            RpcContext.getServerAttachment().setAttachment("userId", userDTO.getId().toString());

            // 生成JWT令牌，传递UserDTO用于获取用户类型
            String token = jwtService.generateToken(authentication, userDTO);
            long expiresIn = jwtService.getExpirationTime();
            
            log.info("用户登录成功, username: {}, token: {}", loginRequest.getUsername(), token);
            return ResponseEntity.ok(new LoginResponseDTO(token, expiresIn, userDTO.getUserType(), userDTO.getNickname()));
        } catch (Exception e) {
            // 清理安全上下文，防止信息泄露
            SecurityContextHolder.clearContext();
            log.error("登录过程中发生错误", e);
            throw e;
        }
    }

    @PostMapping("/change-password")
    @Operation(summary = "修改密码", description = "修改当前用户密码")
    @ApiResponse(responseCode = "200", description = "密码修改成功")
    @ApiResponse(responseCode = "400", description = "密码修改失败")
    public ResponseEntity<String> changePassword(@Parameter(description = "密码修改请求信息") @Valid @RequestBody ChangePasswordRequest request) {
        log.info("修改密码请求");
        
        // 获取当前登录用户
        String userIdStr = RpcContext.getServerAttachment().getAttachment("userId");
        if (userIdStr == null) {
            log.warn("用户未登录");
            return ResponseEntity.badRequest().body("用户未登录");
        }
        
        Long currentUserId = Long.valueOf(userIdStr);

        // 获取当前用户信息
        UserDTO currentUser = userService.findById(currentUserId);
        if (currentUser == null) {
            log.warn("用户不存在, userId: {}", currentUserId);
            return ResponseEntity.badRequest().body("用户不存在");
        }

        // 验证旧密码是否正确
        if (!passwordEncoder.matches(request.getOldPassword(), currentUser.getPassword())) {
            log.warn("旧密码不正确, userId: {}", currentUserId);
            return ResponseEntity.badRequest().body("旧密码不正确");
        }

        // 验证新密码复杂度
        if (!isValidPassword(request.getNewPassword())) {
            log.warn("新密码不符合复杂度要求, userId: {}", currentUserId);
            return ResponseEntity.badRequest().body("密码必须至少包含8个字符，包括大小写字母、数字和特殊字符");
        }

        // 验证新密码不能与旧密码相同
        if (request.getOldPassword().equals(request.getNewPassword())) {
            log.warn("新密码不能与旧密码相同, userId: {}", currentUserId);
            return ResponseEntity.badRequest().body("新密码不能与旧密码相同");
        }

        // 调用user服务更新密码
        boolean success = userService.updatePassword(currentUserId, request.getNewPassword());
        if (success) {
            log.info("密码修改成功, userId: {}", currentUserId);
            return ResponseEntity.ok("密码修改成功");
        } else {
            log.error("密码修改失败, userId: {}", currentUserId);
            return ResponseEntity.badRequest().body("密码修改失败");
        }
    }

    // 密码复杂度校验方法
    private boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) {
                hasUpper = true;
            } else if (Character.isLowerCase(c)) {
                hasLower = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            } else if (!Character.isLetterOrDigit(c)) {
                hasSpecial = true;
            }
        }
        
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }
    
    /**
     * 检查用户是否有管理员权限
     * @param userDTO 用户信息
     * @return 是否有管理员权限
     */
    private boolean isAdmin(UserDTO userDTO) {
        return userDTO != null && "ADMIN".equals(userDTO.getUserType());
    }

    @GetMapping("/logout")
    @Operation(summary = "用户登出", description = "用户登出系统")
    @ApiResponse(responseCode = "200", description = "登出成功")
    public ResponseEntity<Void> logout() {
        log.info("用户登出");

        // 清除安全上下文
        SecurityContextHolder.clearContext();

        log.info("用户登出成功");
        return ResponseEntity.ok().build();
    }

    @GetMapping("/refresh")
    @Operation(summary = "刷新令牌", description = "刷新JWT令牌")
    @ApiResponse(responseCode = "200", description = "令牌刷新成功", 
        content = @Content(mediaType = "application/json", 
        schema = @Schema(implementation = LoginResponseDTO.class)))
    public ResponseEntity<LoginResponseDTO> refresh() {
        log.info("刷新令牌");
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserDTO userDTO = userService.findByUsername(authentication.getName());

            // 使用Dubbo上下文传递用户ID
            RpcContext.getServerAttachment().setAttachment("userId", userDTO.getId().toString());

            // 生成JWT令牌，传递UserDTO用于获取用户类型
            String token = jwtService.generateToken(authentication, userDTO);
            long expiresIn = jwtService.getExpirationTime();
            
            log.info("令牌刷新成功, username: {}, token: {}", authentication.getName(), token);
            return ResponseEntity.ok(new LoginResponseDTO(token, expiresIn, userDTO.getUserType(), userDTO.getNickname()));
        } catch (Exception e) {
            // 清理安全上下文，防止信息泄露
            SecurityContextHolder.clearContext();
            log.error("刷新令牌过程中发生错误", e);
            throw e;
        }
    }

    // 密码修改请求的数据传输对象
    @Setter
    @Getter
    public static class ChangePasswordRequest {
        @Parameter(description = "旧密码")
        @NotBlank(message = "旧密码不能为空")
        private String oldPassword;

        @Parameter(description = "新密码")
        @NotBlank(message = "新密码不能为空")
        @Size(min = 8, message = "密码长度不能少于8位")
        private String newPassword;
    }
}