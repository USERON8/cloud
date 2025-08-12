package com.cloud.auth.service;

import com.cloud.api.user.UserFeign;
import com.cloud.common.domain.dto.LoginRequestDTO;
import com.cloud.common.domain.dto.LoginResponseDTO;
import com.cloud.common.domain.dto.RegisterRequestDTO;
import com.cloud.common.domain.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuthService {

    private final UserFeign userFeign;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserFeign userFeign,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager) {
        this.userFeign = userFeign;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;

    }

    public void register(RegisterRequestDTO registerRequest) {
        String username = registerRequest.getUsername();
        log.info("用户注册, username: {}", username);

        try {
            // 检查用户是否已存在
            UserDTO existingUser = userFeign.findByUsername(username);
            if (existingUser != null) {
                log.warn("用户已存在: {}", username);
                throw new RuntimeException("用户已存在");
            }

            // 直接调用user服务的save方法进行注册
            userFeign.register(registerRequest);
            log.info("用户注册成功, username: {}", username);
        } catch (Exception e) {
            log.error("注册过程中发生错误, username: {}", username, e);
            throw new RuntimeException("注册失败: " + e.getMessage(), e);
        }
    }

    /**
     * 处理用户注册并自动登录
     *
     * @param registerRequest 用户注册请求信息
     * @return 登录响应信息
     */
    public LoginResponseDTO registerAndLogin(RegisterRequestDTO registerRequest) {
        log.info("用户注册并登录, username: {}", registerRequest.getUsername());

        try {
            // 执行注册
            register(registerRequest);

            // 自动登录
            LoginRequestDTO loginRequest = new LoginRequestDTO();
            loginRequest.setUsername(registerRequest.getUsername());
            loginRequest.setPassword(registerRequest.getPassword());

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
            UserDTO userDTO = userFeign.findByUsername(registerRequest.getUsername());

            log.info("用户注册并登录成功, username: {}", registerRequest.getUsername());
            return new LoginResponseDTO(token, expiresIn, userDTO.getUserType(), userDTO.getNickname());
        } catch (Exception e) {
            log.error("注册并登录过程中发生错误, username: {}", registerRequest.getUsername(), e);
            throw new RuntimeException("注册并登录失败: " + e.getMessage(), e);
        }
    }
}