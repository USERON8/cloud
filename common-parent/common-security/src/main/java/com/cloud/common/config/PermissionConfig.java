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
                "read", "write", "delete", "admin:read", "admin:write",
                "user:read", "user:write", "user:delete",
                "product:read", "product:write", "product:delete",
                "order:read", "order:write", "order:delete",
                "payment:read", "payment:write",
                "stock:read", "stock:write",
                "search:read", "search:write",
                "log:read", "log:write"
        );

        rolePermissions.put("ROLE_USER", Arrays.asList(
                "read", "write",
                "user:read", "user:write",
                "order:read", "order:write"
        ));
        rolePermissions.put("ROLE_MERCHANT", Arrays.asList(
                "read", "write",
                "user:read", "user:write",
                "merchant:read", "merchant:write",
                "product:read", "product:write",
                "stock:read", "stock:write",
                "order:read", "order:write"
        ));
        rolePermissions.put("ROLE_ADMIN", new ArrayList<>(adminPermissions));
        rolePermissions.put("ROLE_SUPER_ADMIN", new ArrayList<>(adminPermissions));
        rolePermissions.put("ROLE_OPS_ADMIN", Arrays.asList(
                "read", "write",
                "order:read", "order:write",
                "product:read", "product:write",
                "stock:read", "stock:write",
                "payment:read", "payment:write",
                "search:read", "search:write"
        ));
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

    public boolean hasPermissionByRole(String role, String permission) {
        if (!enabled) {
            return true;
        }
        return getPermissionsByRole(role).contains(permission);
    }

    public boolean hasAnyPermissionByRole(String role, String... permissions) {
        if (!enabled) {
            return true;
        }
        List<String> rolePermissionList = getPermissionsByRole(role);
        return Arrays.stream(permissions).anyMatch(rolePermissionList::contains);
    }

    public boolean hasAllPermissionsByRole(String role, String... permissions) {
        if (!enabled) {
            return true;
        }
        List<String> rolePermissionList = getPermissionsByRole(role);
        return Arrays.stream(permissions).allMatch(rolePermissionList::contains);
    }

    public Set<String> getAllPermissions() {
        Set<String> all = new HashSet<>();
        rolePermissions.values().forEach(all::addAll);
        all.addAll(defaultPermissions);
        return all;
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
            key = "'permission:config:batch:update'",
            waitTime = 10,
            leaseTime = 30,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "Acquire batch permission update lock failed"
    )
    public void batchUpdatePermissions(Map<String, List<String>> rolePermissions) {
        try {
            if (rolePermissions != null) {
                this.rolePermissions.clear();
                this.rolePermissions.putAll(rolePermissions);
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
        this.enabled = true;
        this.strictMode = false;
        this.defaultPermissions.clear();
        this.adminPermissions.clear();
        initializeDefaultConfig();
    }
}
