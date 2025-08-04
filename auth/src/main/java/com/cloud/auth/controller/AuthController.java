package com.cloud.auth.controller;


import com.cloud.auth.service.JwtService;
import com.cloud.common.domain.dto.LoginRequestDTO;
import com.cloud.common.domain.dto.LoginResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin // 允许跨域
public class AuthController {
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthController(JwtService jwtService, AuthenticationManager authenticationManager) {
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO loginRequest) {
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

        return ResponseEntity.ok(new LoginResponseDTO(token, expiresIn));
    }
}
