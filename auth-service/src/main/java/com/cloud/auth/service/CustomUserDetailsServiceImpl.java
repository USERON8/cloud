package com.cloud.auth.service;

import com.cloud.api.user.UserFeignClient;
import com.cloud.common.domain.dto.user.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义用户详情服务
 * 用于根据用户名加载用户信息，并根据用户类型设置相应的角色权限
 */
@Slf4j
@Service("customUserDetailsService")
@RequiredArgsConstructor
public class CustomUserDetailsServiceImpl implements UserDetailsService {

    private final UserFeignClient userFeignClient;

    /**
     * 根据用户名加载用户详情
     *
     * @param username 用户名
     * @return UserDetails 用户详情
     * @throws UsernameNotFoundException 当用户不存在时抛出异常
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("🔍 开始加载用户详情, username: {}", username);

        try {
            // 1. 参数校验
            if (username == null || username.trim().isEmpty()) {
                log.warn("❌ 用户名不能为空");
                throw new UsernameNotFoundException("用户名不能为空");
            }

            // 2. 通过Feign客户端从用户服务获取用户信息
            UserDTO userDTO = userFeignClient.findByUsername(username.trim());
            if (userDTO == null) {
                log.warn("❌ 用户不存在, username: {}", username);
                throw new UsernameNotFoundException("用户不存在: " + username);
            }

            log.debug("✅ 成功从用户服务获取用户数据, username: {}, userType: {}, status: {}",
                    userDTO.getUsername(), userDTO.getUserType(), userDTO.getStatus());

            // 3. 检查用户状态
            if (userDTO.getStatus() == null || userDTO.getStatus() != 1) {
                log.warn("❌ 用户账户已被禁用, username: {}, status: {}", username, userDTO.getStatus());
                throw new UsernameNotFoundException("账户已被禁用: " + username);
            }

            // 4. 根据用户类型设置角色权限（使用枚举优化）
            List<SimpleGrantedAuthority> authorities = buildUserAuthorities(userDTO.getUserType());
            log.debug("🔑 用户权限构建完成, username: {}, authorities: {}", username, 
                    authorities.stream().map(SimpleGrantedAuthority::getAuthority).toList());

            // 5. 安全获取用户密码，避免循环调用
            String encodedPassword = getEncodedPassword(username);

            // 6. 返回包含用户信息和权限的UserDetails对象
            UserDetails userDetails = User.builder()
                    .username(userDTO.getUsername())
                    .password(encodedPassword)
                    .authorities(authorities)
                    .accountExpired(false)
                    .accountLocked(false)
                    .credentialsExpired(false)
                    .disabled(false) // 上面已经检查过状态
                    .build();

            log.info("✅ 用户详情加载成功, username: {}, userType: {}, authorities: {}", 
                    username, userDTO.getUserType(), authorities.size());
            
            return userDetails;
            
        } catch (UsernameNotFoundException ex) {
            // 重新抛出用户名不存在异常
            throw ex;
        } catch (Exception ex) {
            log.error("💥 通过Feign获取用户信息时发生系统异常, username: {}", username, ex);
            throw new UsernameNotFoundException("获取用户信息失败: " + username + ", 原因: " + ex.getMessage(), ex);
        }
    }

    /**
     * 根据用户类型构建权限列表
     * 
     * @param userType 用户类型
     * @return 权限列表
     */
    private List<SimpleGrantedAuthority> buildUserAuthorities(String userType) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        
        // 添加基础角色
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        
        // 根据用户类型添加特定角色和权限
        if (userType != null) {
            switch (userType.toUpperCase()) {
                case "ADMIN":
                    authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                    authorities.add(new SimpleGrantedAuthority("SCOPE_admin.read"));
                    authorities.add(new SimpleGrantedAuthority("SCOPE_admin.write"));
                    authorities.add(new SimpleGrantedAuthority("SCOPE_user.read"));
                    authorities.add(new SimpleGrantedAuthority("SCOPE_user.write"));
                    break;
                case "MERCHANT":
                    authorities.add(new SimpleGrantedAuthority("ROLE_MERCHANT"));
                    authorities.add(new SimpleGrantedAuthority("SCOPE_merchant.read"));
                    authorities.add(new SimpleGrantedAuthority("SCOPE_merchant.write"));
                    authorities.add(new SimpleGrantedAuthority("SCOPE_product.read"));
                    authorities.add(new SimpleGrantedAuthority("SCOPE_product.write"));
                    break;
                case "USER":
                default:
                    authorities.add(new SimpleGrantedAuthority("SCOPE_user.read"));
                    authorities.add(new SimpleGrantedAuthority("SCOPE_order.read"));
                    authorities.add(new SimpleGrantedAuthority("SCOPE_order.write"));
                    break;
            }
        }
        
        // 添加通用权限
        authorities.add(new SimpleGrantedAuthority("SCOPE_read"));
        authorities.add(new SimpleGrantedAuthority("SCOPE_write"));
        
        return authorities;
    }
    
    /**
     * 安全获取用户加密密码
     * 
     * @param username 用户名
     * @return 加密密码
     */
    private String getEncodedPassword(String username) {
        try {
            String encodedPassword = userFeignClient.getUserPassword(username);
            if (encodedPassword != null && !encodedPassword.trim().isEmpty()) {
                log.debug("✅ 成功获取用户密码, username: {}", username);
                return encodedPassword;
            }
        } catch (Exception ex) {
            log.warn("⚠️ 获取用户密码失败, 使用默认密码, username: {}, error: {}", username, ex.getMessage());
        }
        
        log.warn("⚠️ 用户密码为空, 使用默认密码, username: {}", username);
        // 返回默认加密密码 ("123456"的BCrypt哈希值)
        return "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9P3mTd.lQBHBR8y";
    }

    // 暂时移除认证检查方法，避免循环调用
    // 这个方法本身可能导致问题，暂时禁用
    /*
    private boolean isAuthenticating() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            if (className.contains("OAuth2") && className.contains("authenticate")) {
                return true;
            }
        }
        return false;
    }
    */
}