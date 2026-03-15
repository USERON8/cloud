package com.cloud.user.service.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.user.mapper.RoleMapper;
import com.cloud.user.mapper.UserRoleMapper;
import com.cloud.user.module.entity.Role;
import com.cloud.user.module.entity.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleAssignmentService {

    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;

    @Transactional(readOnly = true)
    public List<String> getRoleCodesByUserId(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return getRoleCodesByUserIds(List.of(userId)).getOrDefault(userId, List.of());
    }

    @Transactional(readOnly = true)
    public Map<Long, List<String>> getRoleCodesByUserIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<UserRole> userRoles = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                .in(UserRole::getUserId, userIds));
        if (userRoles.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<Long> roleIds = userRoles.stream()
                .map(UserRole::getRoleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (roleIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, String> roleCodeById = roleMapper.selectList(new LambdaQueryWrapper<Role>()
                        .in(Role::getId, roleIds))
                .stream()
                .collect(Collectors.toMap(
                        Role::getId,
                        role -> stripRolePrefix(role.getCode()),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        Map<Long, List<String>> result = new LinkedHashMap<>();
        for (UserRole userRole : userRoles) {
            String roleCode = roleCodeById.get(userRole.getRoleId());
            if (roleCode == null) {
                continue;
            }
            result.computeIfAbsent(userRole.getUserId(), ignored -> new ArrayList<>()).add(roleCode);
        }
        result.replaceAll((ignored, roles) -> roles.stream().filter(Objects::nonNull).distinct().toList());
        return result;
    }

    @Transactional(readOnly = true)
    public List<Long> getUserIdsByRoleCode(String roleCode) {
        String normalizedRoleCode = normalizeRoleCode(roleCode);
        if (normalizedRoleCode == null) {
            return List.of();
        }
        Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                .eq(Role::getCode, normalizedRoleCode)
                .last("limit 1"));
        if (role == null) {
            return List.of();
        }
        return userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                        .eq(UserRole::getRoleId, role.getId()))
                .stream()
                .map(UserRole::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getRoleDistribution() {
        List<UserRole> userRoles = userRoleMapper.selectList(null);
        if (userRoles.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<Long> roleIds = userRoles.stream()
                .map(UserRole::getRoleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (roleIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, String> roleCodeById = roleMapper.selectList(new LambdaQueryWrapper<Role>()
                        .in(Role::getId, roleIds))
                .stream()
                .collect(Collectors.toMap(
                        Role::getId,
                        Role::getCode,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        Map<String, Long> distribution = new LinkedHashMap<>();
        for (UserRole userRole : userRoles) {
            String roleCode = roleCodeById.get(userRole.getRoleId());
            if (roleCode == null) {
                continue;
            }
            distribution.merge(stripRolePrefix(roleCode), 1L, Long::sum);
        }
        return distribution;
    }

    @Transactional
    public void replaceRoles(Long userId, Collection<String> roles) {
        if (userId == null) {
            return;
        }
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        addRoles(userId, roles);
    }

    @Transactional
    public void addRoles(Long userId, Collection<String> roles) {
        if (userId == null || roles == null || roles.isEmpty()) {
            return;
        }

        Map<String, Role> roleByCode = loadRolesByCode(roles);
        if (roleByCode.isEmpty()) {
            return;
        }

        Set<Long> existingRoleIds = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                        .eq(UserRole::getUserId, userId))
                .stream()
                .map(UserRole::getRoleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (String role : normalizeRoleCodes(roles)) {
            Role roleEntity = roleByCode.get(role);
            if (roleEntity == null || existingRoleIds.contains(roleEntity.getId())) {
                continue;
            }
            UserRole userRole = new UserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(roleEntity.getId());
            userRoleMapper.insert(userRole);
        }
    }

    private Map<String, Role> loadRolesByCode(Collection<String> roles) {
        Set<String> normalized = normalizeRoleCodes(roles);
        if (normalized.isEmpty()) {
            return Collections.emptyMap();
        }
        return roleMapper.selectList(new LambdaQueryWrapper<Role>()
                        .in(Role::getCode, normalized))
                .stream()
                .collect(Collectors.toMap(
                        Role::getCode,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private Set<String> normalizeRoleCodes(Collection<String> roles) {
        return roles.stream()
                .map(this::normalizeRoleCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String normalizeRoleCode(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        String trimmed = role.trim().toUpperCase();
        return trimmed.startsWith("ROLE_") ? trimmed : "ROLE_" + trimmed;
    }

    private String stripRolePrefix(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return roleCode;
        }
        return roleCode.startsWith("ROLE_") ? roleCode.substring("ROLE_".length()) : roleCode;
    }
}
