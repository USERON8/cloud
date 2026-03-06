package com.cloud.auth.service.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.auth.mapper.PermissionMapper;
import com.cloud.auth.mapper.RoleMapper;
import com.cloud.auth.mapper.RolePermissionMapper;
import com.cloud.auth.module.entity.Permission;
import com.cloud.auth.module.entity.Role;
import com.cloud.auth.module.entity.RolePermission;
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
public class AuthPermissionQueryService {

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;

    @Transactional(readOnly = true)
    public Map<String, List<String>> getPermissionsByRoles(Collection<String> roles) {
        Set<String> normalizedRoleCodes = normalizeRoleCodes(roles);
        if (normalizedRoleCodes.isEmpty()) {
            return Map.of();
        }

        List<Role> roleEntities = roleMapper.selectList(new LambdaQueryWrapper<Role>()
                .in(Role::getRoleCode, normalizedRoleCodes));
        if (roleEntities.isEmpty()) {
            return Map.of();
        }

        Map<Long, String> roleCodeById = roleEntities.stream()
                .collect(Collectors.toMap(
                        Role::getId,
                        Role::getRoleCode,
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
                        Permission::getPermissionCode,
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
            permissionsByRole.computeIfAbsent(roleCode, ignored -> new LinkedHashSet<>()).add(permissionCode);
        }

        Map<String, List<String>> result = new LinkedHashMap<>();
        permissionsByRole.forEach((roleCode, permissionCodes) -> result.put(roleCode, List.copyOf(permissionCodes)));
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
}
