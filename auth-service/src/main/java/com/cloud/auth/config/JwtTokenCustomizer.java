package com.cloud.auth.config;

import com.cloud.api.user.UserFeignClient;
import com.cloud.common.domain.dto.user.UserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;

/**
 * JWT令牌自定义增强器
 * 用于在JWT令牌中添加额外的用户信息，如用户ID和用户类型
 */
@Component
@RequiredArgsConstructor
public class JwtTokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

    private final UserFeignClient userFeignClient;

    /**
     * 自定义JWT令牌
     * 添加用户ID和用户类型到JWT声明中
     *
     * @param context JWT编码上下文
     */
    @Override
    public void customize(JwtEncodingContext context) {
        // 检查是否是访问令牌
        if (context.getTokenType().equals(org.springframework.security.oauth2.server.authorization.OAuth2TokenType.ACCESS_TOKEN)) {
            // 从授权信息中获取用户信息
            Object principal = context.getPrincipal().getPrincipal();

            // 如果是UserDetails类型，则添加额外信息到JWT声明中
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                String username = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();

                // 通过Feign客户端获取用户详细信息
                UserDTO user = userFeignClient.findByUsername(username);
                if (user != null) {
                    context.getClaims()
                            .claim("user_id", user.getId())              // 添加用户ID
                            .claim("user_type", user.getUserType())      // 添加用户类型
                            .claim("nickname", user.getNickname());   // 添加用户昵称
                }
            }
        }
    }
}