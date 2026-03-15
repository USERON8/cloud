package com.cloud.user.service.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.user.mapper.PermissionMapper;
import com.cloud.user.mapper.RoleMapper;
import com.cloud.user.mapper.RolePermissionMapper;
import com.cloud.user.module.entity.Permission;
import com.cloud.user.module.entity.Role;
import com.cloud.user.module.entity.RolePermission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionQueryService {

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final RoleAssignmentService roleAssignmentService;

    @Transactional(readOnly = true)
    public Map<String, List<String>> getPermissionsByRoles(Collection<String> roles) {
        Set<String> normalizedRoleCodes = normalizeRoleCodes(roles);
        if (normalizedRoleCodes.isEmpty()) {
            return Map.of();
        }

        List<Role> roleEntities = roleMapper.selectList(new LambdaQueryWrapper<Role>()
                .in(Role::getCode, normalizedRoleCodes));
        if (roleEntities.isEmpty()) {
            return Map.of();
        }

        Map<Long, String> roleCodeById = roleEntities.stream()
                .collect(Collectors.toMap(
                        Role::getId,
                        Role::getCode,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        List<RolePermission> rolePermissions = rolePermissionMapper.selectList(new LambdaQueryWrapper<RolePermission>()
                .in(RolePermission::getRoleId, roleCodeById.keySet()));
        if (rolePermissions.isEmpty()) {
            return Map.of();
        }

        Set<Long> permissionIds = rolePermissions.stream()
                .map(RolePermission::getPermissionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (permissionIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, String> permissionCodeById = permissionMapper.selectList(new LambdaQueryWrapper<Permission>()
                        .in(Permission::getId, permissionIds)).stream()
                .collect(Collectors.toMap(
                        Permission::getId,
                        Permission::getCode,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        Map<String, LinkedHashSet<String>> permissionsByRole = new LinkedHashMap<>();
        for (RolePermission rolePermission : rolePermissions) {
            String roleCode = roleCodeById.get(rolePermission.getRoleId());
            String permissionCode = permissionCodeById.get(rolePermission.getPermissionId());
            if (roleCode == null || permissionCode == null) {
                continue;
            }
            permissionsByRole
                    .computeIfAbsent(stripRolePrefix(roleCode), ignored -> new LinkedHashSet<>())
                    .add(permissionCode);
        }

        Map<String, List<String>> result = new LinkedHashMap<>();
        permissionsByRole.forEach((roleCode, permissionCodes) -> result.put(roleCode, List.copyOf(permissionCodes)));
        return result;
    }

    @Transactional(readOnly = true)
    public List<String> getPermissionCodesByUserId(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return getPermissionCodesByUserIds(List.of(userId)).getOrDefault(userId, List.of());
    }

    @Transactional(readOnly = true)
    public Map<Long, List<String>> getPermissionCodesByUserIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<String>> roleCodesByUserId = roleAssignmentService.getRoleCodesByUserIds(userIds);
        if (roleCodesByUserId.isEmpty()) {
            return Map.of();
        }

        Set<String> allRoles = roleCodesByUserId.values().stream()
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (allRoles.isEmpty()) {
            return Map.of();
        }

        Map<String, List<String>> permissionsByRole = getPermissionsByRoles(allRoles);
        Map<Long, List<String>> result = new LinkedHashMap<>();

        roleCodesByUserId.forEach((userId, roles) -> {
            if (roles == null || roles.isEmpty()) {
                result.put(userId, List.of());
                return;
            }
            LinkedHashSet<String> permissions = new LinkedHashSet<>();
            for (String role : roles) {
                permissions.addAll(permissionsByRole.getOrDefault(role, List.of()));
            }
            result.put(userId, List.copyOf(permissions));
        });

        return result;
    }

    private Set<String> normalizeRoleCodes(Collection<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return Set.of();
        }
        return roles.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .map(String::toUpperCase)
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String stripRolePrefix(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return roleCode;
        }
        return roleCode.startsWith("ROLE_") ? roleCode.substring("ROLE_".length()) : roleCode;
    }
}
