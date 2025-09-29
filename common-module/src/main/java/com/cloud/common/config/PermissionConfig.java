package com.cloud.common.config;

import com.cloud.common.annotation.DistributedLock;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 权限配置类
 * 用于配置不同角色和权限的映射关系，支持动态权限配置
 *
 * @author what's up
 */
@Setter
@Getter
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "app.permission")
public class PermissionConfig {

    // Getters and Setters
    /**
     * 角色权限映射
     * 格式：角色名 -> 权限列表
     */
    private Map<String, List<String>> rolePermissions = new HashMap<>();

    /**
     * 用户类型默认权限
     * 格式：用户类型 -> 权限列表
     */
    private Map<String, List<String>> userTypePermissions = new HashMap<>();

    /**
     * 是否启用权限检查
     */
    private boolean enabled = true;

    /**
     * 是否启用严格模式
     * 严格模式下，未配置的权限将被拒绝
     */
    private boolean strictMode = false;

    /**
     * 默认权限列表
     */
    private List<String> defaultPermissions = new ArrayList<>();

    /**
     * 管理员权限列表
     */
    private List<String> adminPermissions = new ArrayList<>();

    public PermissionConfig() {
        initializeDefaultConfig();
    }

    /**
     * 初始化默认配置
     */
    private void initializeDefaultConfig() {
        // 初始化默认权限
        defaultPermissions = Arrays.asList("read", "write");

        // 初始化管理员权限
        adminPermissions = Arrays.asList(
                "read", "write", "delete", "admin.read", "admin.write",
                "user.read", "user.write", "user.delete",
                "product.read", "product.write", "product.delete",
                "order.read", "order.write", "order.delete",
                "payment.read", "payment.write",
                "stock.read", "stock.write",
                "search.read", "search.write",
                "log.read", "log.write"
        );

        // 初始化用户类型权限
        userTypePermissions.put("USER", Arrays.asList("read", "user.read", "user.write"));
        userTypePermissions.put("MERCHANT", Arrays.asList(
                "read", "write",
                "user.read", "user.write",
                "product.read", "product.write",
                "order.read", "order.write",
                "stock.read", "stock.write"
        ));
        userTypePermissions.put("ADMIN", adminPermissions);

        // 初始化角色权限（可以根据具体业务需求配置）
        rolePermissions.put("ROLE_USER", Arrays.asList("read", "user.read", "user.write"));
        rolePermissions.put("ROLE_MERCHANT", Arrays.asList(
                "read", "write",
                "user.read", "user.write",
                "product.read", "product.write"
        ));
        rolePermissions.put("ROLE_ADMIN", adminPermissions);

        log.info("权限配置初始化完成，用户类型数量: {}, 角色数量: {}",
                userTypePermissions.size(), rolePermissions.size());
    }

    /**
     * 根据用户类型获取权限列表
     *
     * @param userType 用户类型
     * @return 权限列表
     */
    public List<String> getPermissionsByUserType(String userType) {
        if (!enabled) {
            return Collections.emptyList();
        }

        List<String> permissions = userTypePermissions.get(userType);
        if (permissions == null) {
            if (strictMode) {
                log.warn("严格模式下，未找到用户类型 {} 的权限配置", userType);
                return Collections.emptyList();
            } else {
                log.debug("未找到用户类型 {} 的权限配置，返回默认权限", userType);
                return new ArrayList<>(defaultPermissions);
            }
        }

        return new ArrayList<>(permissions);
    }

    /**
     * 根据角色获取权限列表
     *
     * @param role 角色名
     * @return 权限列表
     */
    public List<String> getPermissionsByRole(String role) {
        if (!enabled) {
            return Collections.emptyList();
        }

        List<String> permissions = rolePermissions.get(role);
        if (permissions == null) {
            if (strictMode) {
                log.warn("严格模式下，未找到角色 {} 的权限配置", role);
                return Collections.emptyList();
            } else {
                log.debug("未找到角色 {} 的权限配置，返回默认权限", role);
                return new ArrayList<>(defaultPermissions);
            }
        }

        return new ArrayList<>(permissions);
    }

    /**
     * 检查用户类型是否拥有指定权限
     *
     * @param userType   用户类型
     * @param permission 权限
     * @return 是否拥有权限
     */
    public boolean hasPermission(String userType, String permission) {
        if (!enabled) {
            return true; // 如果禁用权限检查，则默认有权限
        }

        List<String> permissions = getPermissionsByUserType(userType);
        return permissions.contains(permission);
    }

    /**
     * 检查用户类型是否拥有任意一个指定权限
     *
     * @param userType    用户类型
     * @param permissions 权限列表
     * @return 是否拥有任意权限
     */
    public boolean hasAnyPermission(String userType, String... permissions) {
        if (!enabled) {
            return true;
        }

        List<String> userPermissions = getPermissionsByUserType(userType);
        return Arrays.stream(permissions)
                .anyMatch(userPermissions::contains);
    }

    /**
     * 检查用户类型是否拥有所有指定权限
     *
     * @param userType    用户类型
     * @param permissions 权限列表
     * @return 是否拥有所有权限
     */
    public boolean hasAllPermissions(String userType, String... permissions) {
        if (!enabled) {
            return true;
        }

        List<String> userPermissions = getPermissionsByUserType(userType);
        return Arrays.stream(permissions)
                .allMatch(userPermissions::contains);
    }

    /**
     * 获取所有配置的权限
     *
     * @return 所有权限的集合
     */
    public Set<String> getAllPermissions() {
        Set<String> allPermissions = new HashSet<>();

        // 收集所有用户类型的权限
        userTypePermissions.values().forEach(allPermissions::addAll);

        // 收集所有角色的权限
        rolePermissions.values().forEach(allPermissions::addAll);

        // 添加默认权限
        allPermissions.addAll(defaultPermissions);

        return allPermissions;
    }

    /**
     * 动态添加用户类型权限
     *
     * @param userType    用户类型
     * @param permissions 权限列表
     */
    public void addUserTypePermissions(String userType, List<String> permissions) {
        List<String> existingPermissions = userTypePermissions.computeIfAbsent(userType, k -> new ArrayList<>());
        permissions.forEach(permission -> {
            if (!existingPermissions.contains(permission)) {
                existingPermissions.add(permission);
            }
        });
        log.info("为用户类型 {} 添加权限: {}", userType, permissions);
    }

    /**
     * 动态移除用户类型权限
     *
     * @param userType    用户类型
     * @param permissions 要移除的权限列表
     */
    public void removeUserTypePermissions(String userType, List<String> permissions) {
        List<String> existingPermissions = userTypePermissions.get(userType);
        if (existingPermissions != null) {
            existingPermissions.removeAll(permissions);
            log.info("从用户类型 {} 移除权限: {}", userType, permissions);
        }
    }

    // ==================== 动态配置更新方法 ====================

    /**
     * 动态更新角色权限
     * 使用分布式锁确保配置更新的一致性
     */
    @DistributedLock(
            key = "'permission:config:role:' + #roleName",
            waitTime = 5,
            leaseTime = 10,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "权限配置更新获取锁失败"
    )
    public void updateRolePermissions(String roleName, List<String> permissions) {
        log.info("🔄 动态更新角色权限 - 角色: {}, 权限数量: {}", roleName, permissions.size());

        if (roleName == null || roleName.trim().isEmpty()) {
            throw new IllegalArgumentException("角色名不能为空");
        }

        if (permissions == null) {
            permissions = new ArrayList<>();
        }

        this.rolePermissions.put(roleName, new ArrayList<>(permissions));
        log.info("✅ 角色权限更新成功 - 角色: {}", roleName);
    }

    /**
     * 动态更新用户类型权限
     * 使用分布式锁确保配置更新的一致性
     */
    @DistributedLock(
            key = "'permission:config:usertype:' + #userType",
            waitTime = 5,
            leaseTime = 10,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "用户类型权限配置更新获取锁失败"
    )
    public void updateUserTypePermissions(String userType, List<String> permissions) {
        log.info("🔄 动态更新用户类型权限 - 用户类型: {}, 权限数量: {}", userType, permissions.size());

        if (userType == null || userType.trim().isEmpty()) {
            throw new IllegalArgumentException("用户类型不能为空");
        }

        if (permissions == null) {
            permissions = new ArrayList<>();
        }

        this.userTypePermissions.put(userType, new ArrayList<>(permissions));
        log.info("✅ 用户类型权限更新成功 - 用户类型: {}", userType);
    }

    /**
     * 批量更新权限配置
     * 使用分布式锁确保批量更新的原子性
     */
    @DistributedLock(
            key = "'permission:config:batch:update'",
            waitTime = 10,
            leaseTime = 30,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "权限配置批量更新获取锁失败"
    )
    public void batchUpdatePermissions(Map<String, List<String>> rolePermissions,
                                       Map<String, List<String>> userTypePermissions) {
        log.info("🔄 批量更新权限配置 - 角色数量: {}, 用户类型数量: {}",
                rolePermissions != null ? rolePermissions.size() : 0,
                userTypePermissions != null ? userTypePermissions.size() : 0);

        try {
            if (rolePermissions != null) {
                this.rolePermissions.clear();
                this.rolePermissions.putAll(rolePermissions);
            }

            if (userTypePermissions != null) {
                this.userTypePermissions.clear();
                this.userTypePermissions.putAll(userTypePermissions);
            }

            log.info("✅ 权限配置批量更新成功");

        } catch (Exception e) {
            log.error("❌ 权限配置批量更新失败", e);
            throw new RuntimeException("权限配置批量更新失败: " + e.getMessage(), e);
        }
    }

    /**
     * 重置权限配置为默认值
     * 使用分布式锁确保重置操作的安全性
     */
    @DistributedLock(
            key = "'permission:config:reset'",
            waitTime = 5,
            leaseTime = 15,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "权限配置重置获取锁失败"
    )
    public void resetToDefault() {
        log.warn("⚠️ 重置权限配置为默认值");

        this.rolePermissions.clear();
        this.userTypePermissions.clear();
        this.enabled = true;
        this.strictMode = false;
        this.defaultPermissions.clear();
        this.adminPermissions.clear();

        // 重新初始化默认配置
        initializeDefaultConfig();

        log.info("✅ 权限配置重置完成");
    }
}
