package com.cloud.common.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Setter
@Getter
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "app.permission")
public class PermissionConfig {

    private boolean enabled = true;
    private boolean strictMode = false;
    private List<String> defaultPermissions = new ArrayList<>();

    public List<String> resolvePermissions(String role, List<String> permissions) {
        if (!enabled) {
            return Collections.emptyList();
        }

        if (permissions == null || permissions.isEmpty()) {
            if (strictMode) {
                log.warn("No permissions mapped for role in strict mode: {}", role);
                return Collections.emptyList();
            }
            return new ArrayList<>(defaultPermissions);
        }

        return permissions.stream()
                .filter(permission -> permission != null && !permission.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    public Set<String> getAllPermissions() {
        return new HashSet<>(defaultPermissions);
    }
}
