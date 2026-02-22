package com.cloud.common.config;

import com.cloud.common.annotation.DistributedLock;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Setter
@Getter
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "app.permission")
public class PermissionConfig {

    private Map<String, List<String>> rolePermissions = new HashMap<>();
    private Map<String, List<String>> userTypePermissions = new HashMap<>();
    private boolean enabled = true;
    private boolean strictMode = false;
    private List<String> defaultPermissions = new ArrayList<>();
    private List<String> adminPermissions = new ArrayList<>();

    public PermissionConfig() {
        initializeDefaultConfig();
    }

    private void initializeDefaultConfig() {
        defaultPermissions = Arrays.asList("read", "write");

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

        userTypePermissions.put("USER", Arrays.asList("read", "user.read", "user.write"));
        userTypePermissions.put("MERCHANT", Arrays.asList(
                "read", "write",
                "user.read", "user.write",
                "product.read", "product.write",
                "order.read", "order.write",
                "stock.read", "stock.write"
        ));
        userTypePermissions.put("ADMIN", new ArrayList<>(adminPermissions));

        rolePermissions.put("ROLE_USER", Arrays.asList("read", "user.read", "user.write"));
        rolePermissions.put("ROLE_MERCHANT", Arrays.asList(
                "read", "write",
                "user.read", "user.write",
                "product.read", "product.write"
        ));
        rolePermissions.put("ROLE_ADMIN", new ArrayList<>(adminPermissions));
    }

    public List<String> getPermissionsByUserType(String userType) {
        if (!enabled) {
            return Collections.emptyList();
        }

        List<String> permissions = userTypePermissions.get(userType);
        if (permissions == null) {
            if (strictMode) {
                log.warn("Unknown userType in strict mode: {}", userType);
                return Collections.emptyList();
            }
            return new ArrayList<>(defaultPermissions);
        }

        return new ArrayList<>(permissions);
    }

    public List<String> getPermissionsByRole(String role) {
        if (!enabled) {
            return Collections.emptyList();
        }

        List<String> permissions = rolePermissions.get(role);
        if (permissions == null) {
            if (strictMode) {
                log.warn("Unknown role in strict mode: {}", role);
                return Collections.emptyList();
            }
            return new ArrayList<>(defaultPermissions);
        }

        return new ArrayList<>(permissions);
    }

    public boolean hasPermission(String userType, String permission) {
        if (!enabled) {
            return true;
        }
        return getPermissionsByUserType(userType).contains(permission);
    }

    public boolean hasAnyPermission(String userType, String... permissions) {
        if (!enabled) {
            return true;
        }
        List<String> userPermissions = getPermissionsByUserType(userType);
        return Arrays.stream(permissions).anyMatch(userPermissions::contains);
    }

    public boolean hasAllPermissions(String userType, String... permissions) {
        if (!enabled) {
            return true;
        }
        List<String> userPermissions = getPermissionsByUserType(userType);
        return Arrays.stream(permissions).allMatch(userPermissions::contains);
    }

    public Set<String> getAllPermissions() {
        Set<String> all = new HashSet<>();
        userTypePermissions.values().forEach(all::addAll);
        rolePermissions.values().forEach(all::addAll);
        all.addAll(defaultPermissions);
        return all;
    }

    public void addUserTypePermissions(String userType, List<String> permissions) {
        List<String> existing = userTypePermissions.computeIfAbsent(userType, k -> new ArrayList<>());
        permissions.forEach(permission -> {
            if (!existing.contains(permission)) {
                existing.add(permission);
            }
        });
    }

    public void removeUserTypePermissions(String userType, List<String> permissions) {
        List<String> existing = userTypePermissions.get(userType);
        if (existing != null) {
            existing.removeAll(permissions);
        }
    }

    @DistributedLock(
            key = "'permission:config:role:' + #roleName",
            waitTime = 5,
            leaseTime = 10,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "Acquire role permission update lock failed"
    )
    public void updateRolePermissions(String roleName, List<String> permissions) {
        if (roleName == null || roleName.trim().isEmpty()) {
            throw new IllegalArgumentException("roleName cannot be blank");
        }
        if (permissions == null) {
            permissions = new ArrayList<>();
        }
        this.rolePermissions.put(roleName, new ArrayList<>(permissions));
    }

    @DistributedLock(
            key = "'permission:config:usertype:' + #userType",
            waitTime = 5,
            leaseTime = 10,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "Acquire user type permission update lock failed"
    )
    public void updateUserTypePermissions(String userType, List<String> permissions) {
        if (userType == null || userType.trim().isEmpty()) {
            throw new IllegalArgumentException("userType cannot be blank");
        }
        if (permissions == null) {
            permissions = new ArrayList<>();
        }
        this.userTypePermissions.put(userType, new ArrayList<>(permissions));
    }

    @DistributedLock(
            key = "'permission:config:batch:update'",
            waitTime = 10,
            leaseTime = 30,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "Acquire batch permission update lock failed"
    )
    public void batchUpdatePermissions(Map<String, List<String>> rolePermissions,
                                       Map<String, List<String>> userTypePermissions) {
        try {
            if (rolePermissions != null) {
                this.rolePermissions.clear();
                this.rolePermissions.putAll(rolePermissions);
            }
            if (userTypePermissions != null) {
                this.userTypePermissions.clear();
                this.userTypePermissions.putAll(userTypePermissions);
            }
        } catch (Exception e) {
            log.error("Failed to batch update permissions", e);
            throw new RuntimeException("Failed to batch update permissions: " + e.getMessage(), e);
        }
    }

    @DistributedLock(
            key = "'permission:config:reset'",
            waitTime = 5,
            leaseTime = 15,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "Acquire reset permission lock failed"
    )
    public void resetToDefault() {
        this.rolePermissions.clear();
        this.userTypePermissions.clear();
        this.enabled = true;
        this.strictMode = false;
        this.defaultPermissions.clear();
        this.adminPermissions.clear();
        initializeDefaultConfig();
    }
}
