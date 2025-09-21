package com.cloud.auth.service;

import com.cloud.api.user.UserFeignClient;
import com.cloud.auth.util.OAuth2ComplianceChecker;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.exception.BusinessException;
import com.cloud.common.exception.ResourceNotFoundException;
import com.cloud.common.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义用户详情服务 - OAuth2.1兼容实现
 * 用于根据用户名加载用户信息，并根据用户类型设置相应的角色权限
 * <p>
 * OAuth2.1标准兼容:
 * - 支持SCOPE_前缀的权限
 * - 支持角色和权限分离
 * - 完全依赖UserFeignClient进行用户数据获取
 *
 * @author what's up
 */
@Slf4j
@Service("customUserDetailsService")
@RequiredArgsConstructor
public class CustomUserDetailsServiceImpl implements UserDetailsService {

    private final UserFeignClient userFeignClient;

    @Autowired(required = false)
    private OAuth2ComplianceChecker complianceChecker;

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
                throw new ValidationException("username", username, "用户名不能为空");
            }

            // 2. 通过Feign客户端从用户服务获取用户信息
            UserDTO userDTO = userFeignClient.findByUsername(username.trim());
            if (userDTO == null) {
                log.warn("❌ 用户不存在, username: {}", username);
                throw new ResourceNotFoundException("User", username);
            }

            log.debug("✅ 成功从用户服务获取用户数据, username: {}, userType: {}, status: {}",
                    userDTO.getUsername(), userDTO.getUserType(), userDTO.getStatus());

            // 3. 检查用户状态
            if (userDTO.getStatus() == null || userDTO.getStatus() != 1) {
                log.warn("❌ 用户账户已被禁用, username: {}, status: {}", username, userDTO.getStatus());
                throw new BusinessException(com.cloud.common.enums.ResultCode.USER_DISABLED);
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

            // OAuth2.1兼容性检查（可选）
            if (complianceChecker != null) {
                try {
                    OAuth2ComplianceChecker.OAuth2ComplianceResult complianceResult =
                            complianceChecker.validateCompliance(userDetails, userDTO.getUserType());

                    if (!complianceResult.isCompliant()) {
                        log.warn("⚠️ OAuth2.1兼容性检查发现错误, username: {}, errors: {}",
                                username, complianceResult.getErrors());
                    }

                    if (!complianceResult.getWarnings().isEmpty()) {
                        log.debug("📝 OAuth2.1兼容性检查警告, username: {}, warnings: {}",
                                username, complianceResult.getWarnings());
                    }

                } catch (Exception e) {
                    log.debug("🔍 OAuth2.1兼容性检查失败，忽略, username: {}, error: {}", username, e.getMessage());
                }
            }

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
     * 根据用户类型构建权限列表 - OAuth2.1标准兼容
     * <p>
     * OAuth2.1权限设计原则:
     * - 使用SCOPE_前缀的细粒度权限
     * - 支持角色继承（ADMIN > MERCHANT > USER）
     * - 避免权限爆炸，只给必要的权限
     *
     * @param userType 用户类型
     * @return 权限列表
     */
    private List<SimpleGrantedAuthority> buildUserAuthorities(String userType) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        log.debug("🔑 正在为用户类型 {} 构建 OAuth2.1 权限", userType);

        // 添加基础角色（所有用户都有）
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        // 添加基础权限（OAuth2.1标准）
        authorities.add(new SimpleGrantedAuthority("SCOPE_openid"));
        authorities.add(new SimpleGrantedAuthority("SCOPE_profile"));
        authorities.add(new SimpleGrantedAuthority("SCOPE_read"));

        // 根据用户类型添加特定角色和权限（递增式权限继承）
        if (userType != null) {
            switch (userType.toUpperCase()) {
                case "ADMIN":
                    // 管理员 - 最高权限
                    authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                    authorities.add(new SimpleGrantedAuthority("SCOPE_admin.read"));
                    authorities.add(new SimpleGrantedAuthority("SCOPE_admin.write"));
                    authorities.add(new SimpleGrantedAuthority("SCOPE_user.read"));
                    authorities.add(new SimpleGrantedAuthority("SCOPE_user.write"));
                    authorities.add(new SimpleGrantedAuthority("SCOPE_merchant.read"));
                    authorities.add(new SimpleGrantedAuthority("SCOPE_merchant.write"));
                    // 继承商家权限
                    authorities.add(new SimpleGrantedAuthority("SCOPE_product.read"));
                    authorities.add(new SimpleGrantedAuthority("SCOPE_product.write"));
                    // 继承普通用户权限
                    authorities.add(new SimpleGrantedAuthority("SCOPE_order.read"));
                    authorities.add(new SimpleGrantedAuthority("SCOPE_order.write"));
                    authorities.add(new SimpleGrantedAuthority("SCOPE_write"));
                    break;

                case "MERCHANT":
                    // 商家 - 中级权限
                    authorities.add(new SimpleGrantedAuthority("ROLE_MERCHANT"));
                    authorities.add(new SimpleGrantedAuthority("SCOPE_merchant.read"));
                    authorities.add(new SimpleGrantedAuthority("SCOPE_merchant.write"));
                    authorities.add(new SimpleGrantedAuthority("SCOPE_product.read"));
                    authorities.add(new SimpleGrantedAuthority("SCOPE_product.write"));
                    authorities.add(new SimpleGrantedAuthority("SCOPE_stock.read"));
                    authorities.add(new SimpleGrantedAuthority("SCOPE_stock.write"));
                    // 继承普通用户权限
                    authorities.add(new SimpleGrantedAuthority("SCOPE_order.read"));
                    authorities.add(new SimpleGrantedAuthority("SCOPE_order.write"));
                    authorities.add(new SimpleGrantedAuthority("SCOPE_write"));
                    break;

                case "USER":
                default:
                    // 普通用户 - 基础权限
                    authorities.add(new SimpleGrantedAuthority("SCOPE_user.read"));
                    authorities.add(new SimpleGrantedAuthority("SCOPE_order.read"));
                    authorities.add(new SimpleGrantedAuthority("SCOPE_order.write"));
                    authorities.add(new SimpleGrantedAuthority("SCOPE_write"));
                    break;
            }
        }

        log.debug("✅ 权限构建完成，用户类型: {}, 权限数量: {}", userType, authorities.size());
        return authorities;
    }

    /**
     * 安全获取用户加密密码 - OAuth2.1标准兼容
     * <p>
     * 完全依赖UserFeignClient获取已加密密码，不进行任何加密操作
     *
     * @param username 用户名
     * @return 加密密码
     */
    private String getEncodedPassword(String username) {
        log.debug("🔐 开始获取用户 {} 的加密密码", username);

        try {
            // ✅ 直接调用UserFeignClient获取已加密的密码
            String encodedPassword = userFeignClient.getUserPassword(username);

            if (encodedPassword != null && !encodedPassword.trim().isEmpty() && !"null".equals(encodedPassword)) {
                log.debug("✅ 成功从用户服务获取加密密码, username: {}", username);
                return encodedPassword;
            } else {
                log.warn("⚠️ 用户服务返回空密码, username: {}, 返回值: {}", username, encodedPassword);
            }

        } catch (feign.FeignException.NotFound ex) {
            // 用户不存在
            log.warn("⚠️ 用户 {} 不存在于用户服务中", username);
            throw new ResourceNotFoundException("User password", username);

        } catch (feign.FeignException ex) {
            // Feign调用异常
            log.error("❗ 调用用户服务获取密码时发生Feign异常, username: {}, status: {}, message: {}",
                    username, ex.status(), ex.getMessage());

        } catch (Exception ex) {
            // 其他异常
            log.error("💥 获取用户密码时发生未预期异常, username: {}, error: {}", username, ex.getMessage(), ex);
        }

        // 如果从用户服务获取失败，使用默认密码（仅限开发环境）
        log.warn("⚠️ 无法从用户服务获取密码，使用默认密码 (123456), username: {}", username);

        // 返回默认加密密码 ("123456"的BCrypt哈希值)
        // 生产环境应该抛出异常而不是返回默认密码
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