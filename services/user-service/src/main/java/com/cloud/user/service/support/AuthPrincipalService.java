package com.cloud.user.service.support;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.common.domain.dto.auth.AuthPrincipalDTO;
import com.cloud.common.exception.BizException;
import com.cloud.user.converter.AuthPrincipalConverter;
import com.cloud.user.mapper.UserMapper;
import com.cloud.user.module.entity.User;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthPrincipalService {

  private final UserMapper userMapper;
  private final RoleAssignmentService roleAssignmentService;
  private final PermissionQueryService permissionQueryService;
  private final PasswordEncoder passwordEncoder;
  private final AuthPrincipalConverter authPrincipalConverter;

  @Transactional(readOnly = true)
  public void assertUsernameAvailable(String username, Long currentUserId) {
    if (StrUtil.isBlank(username)) {
      return;
    }
    User existing =
        userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getUsername, username.trim()).last("limit 1"));
    if (existing != null && (currentUserId == null || !existing.getId().equals(currentUserId))) {
      throw new BizException("username already exists");
    }
  }

  @Transactional(readOnly = true)
  public AuthPrincipalDTO findPrincipalByUsername(String username) {
    if (StrUtil.isBlank(username)) {
      return null;
    }
    User user =
        userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getUsername, username.trim()).last("limit 1"));
    return toPrincipalDTO(user);
  }

  @Transactional(readOnly = true)
  public AuthPrincipalDTO findPrincipalById(Long userId) {
    if (userId == null) {
      return null;
    }
    return toPrincipalDTO(userMapper.selectById(userId));
  }

  @Transactional(rollbackFor = Exception.class)
  public Long createPrincipal(AuthPrincipalDTO authPrincipalDTO) {
    if (authPrincipalDTO == null || StrUtil.isBlank(authPrincipalDTO.getUsername())) {
      throw new BizException("username is required");
    }
    assertUsernameAvailable(authPrincipalDTO.getUsername(), authPrincipalDTO.getId());

    User existing =
        authPrincipalDTO.getId() == null ? null : userMapper.selectById(authPrincipalDTO.getId());
    User user = existing == null ? new User() : new User();
    if (authPrincipalDTO.getId() != null) {
      user.setId(authPrincipalDTO.getId());
    }
    user.setUsername(StrUtil.trim(authPrincipalDTO.getUsername()));
    if (StrUtil.isBlank(authPrincipalDTO.getPassword())) {
      throw new BizException("password is required");
    }
    user.setPassword(normalizePassword(authPrincipalDTO.getPassword()));
    user.setNickname(StrUtil.blankToDefault(authPrincipalDTO.getNickname(), user.getUsername()));
    user.setEmail(authPrincipalDTO.getEmail());
    user.setPhone(authPrincipalDTO.getPhone());
    Integer status = authPrincipalDTO.getStatus() == null ? 1 : authPrincipalDTO.getStatus();
    user.setStatus(status);
    user.setEnabled(resolveEnabled(authPrincipalDTO.getEnabled(), status, 1));

    if (existing == null) {
      userMapper.insert(user);
    } else {
      userMapper.updateById(user);
    }

    List<String> roles =
        authPrincipalDTO.getRoles() == null || authPrincipalDTO.getRoles().isEmpty()
            ? List.of("ROLE_USER")
            : authPrincipalDTO.getRoles();
    roleAssignmentService.replaceRoles(user.getId(), roles);
    return user.getId();
  }

  @Transactional(rollbackFor = Exception.class)
  public Boolean updatePrincipal(AuthPrincipalDTO authPrincipalDTO) {
    if (authPrincipalDTO == null || authPrincipalDTO.getId() == null) {
      throw new BizException("principal id is required");
    }

    User existing = userMapper.selectById(authPrincipalDTO.getId());
    if (existing == null) {
      throw new BizException("principal not found");
    }

    String newUsername = StrUtil.trim(authPrincipalDTO.getUsername());
    if (StrUtil.isNotBlank(newUsername) && !StrUtil.equals(newUsername, existing.getUsername())) {
      assertUsernameAvailable(newUsername, authPrincipalDTO.getId());
    }

    User update = new User();
    update.setId(authPrincipalDTO.getId());
    if (StrUtil.isNotBlank(newUsername)) {
      update.setUsername(newUsername);
    }
    if (StrUtil.isNotBlank(authPrincipalDTO.getPassword())) {
      update.setPassword(normalizePassword(authPrincipalDTO.getPassword()));
    }
    if (authPrincipalDTO.getStatus() != null) {
      update.setStatus(authPrincipalDTO.getStatus());
      update.setEnabled(
          resolveEnabled(
              authPrincipalDTO.getEnabled(), authPrincipalDTO.getStatus(), existing.getEnabled()));
    } else if (authPrincipalDTO.getEnabled() != null) {
      update.setEnabled(authPrincipalDTO.getEnabled());
    }
    if (authPrincipalDTO.getNickname() != null) {
      update.setNickname(authPrincipalDTO.getNickname());
    }
    if (authPrincipalDTO.getEmail() != null) {
      update.setEmail(authPrincipalDTO.getEmail());
    }
    if (authPrincipalDTO.getPhone() != null) {
      update.setPhone(authPrincipalDTO.getPhone());
    }

    boolean updated = userMapper.updateById(update) > 0;
    if (updated && authPrincipalDTO.getRoles() != null) {
      roleAssignmentService.replaceRoles(authPrincipalDTO.getId(), authPrincipalDTO.getRoles());
    }
    return updated;
  }

  @Transactional(rollbackFor = Exception.class)
  public Boolean deletePrincipal(Long userId) {
    if (userId == null) {
      return false;
    }
    roleAssignmentService.replaceRoles(userId, List.of());
    return userMapper.deleteById(userId) > 0;
  }

  @Transactional(rollbackFor = Exception.class)
  public Boolean changePassword(Long userId, String oldPassword, String newPassword) {
    if (userId == null) {
      throw new BizException("principal id is required");
    }
    if (StrUtil.isBlank(oldPassword) || StrUtil.isBlank(newPassword)) {
      throw new BizException("old password and new password are required");
    }

    User existing = userMapper.selectById(userId);
    if (existing == null) {
      throw new BizException("principal not found");
    }
    if (!passwordEncoder.matches(oldPassword, existing.getPassword())) {
      return false;
    }

    User update = new User();
    update.setId(userId);
    update.setPassword(normalizePassword(newPassword));
    return userMapper.updateById(update) > 0;
  }

  @Transactional(readOnly = true)
  public List<String> getRoleCodes(Long userId) {
    return roleAssignmentService.getRoleCodesByUserId(userId);
  }

  @Transactional(readOnly = true)
  public List<String> getRoleCodesByUserId(Long userId) {
    return roleAssignmentService.getRoleCodesByUserId(userId);
  }

  @Transactional(readOnly = true)
  public Map<Long, List<String>> getRoleCodesByUserIds(Collection<Long> userIds) {
    return roleAssignmentService.getRoleCodesByUserIds(userIds);
  }

  @Transactional(readOnly = true)
  public List<Long> getUserIdsByRoleCode(String roleCode) {
    return roleAssignmentService.getUserIdsByRoleCode(roleCode);
  }

  @Transactional(readOnly = true)
  public Map<String, Long> getRoleDistribution() {
    return roleAssignmentService.getRoleDistribution();
  }

  @Transactional(readOnly = true)
  public List<String> getPermissionCodes(Long userId) {
    return permissionQueryService.getPermissionCodesByUserId(userId);
  }

  @Transactional(readOnly = true)
  public Map<Long, List<String>> getPermissionCodesByUserIds(Collection<Long> userIds) {
    return permissionQueryService.getPermissionCodesByUserIds(userIds);
  }

  private AuthPrincipalDTO toPrincipalDTO(User user) {
    if (user == null) {
      return null;
    }
    AuthPrincipalDTO dto = authPrincipalConverter.toDTO(user);
    dto.setRoles(roleAssignmentService.getRoleCodesByUserId(user.getId()));
    dto.setPermissions(permissionQueryService.getPermissionCodesByUserId(user.getId()));
    return dto;
  }

  private Integer resolveEnabled(Integer enabled, Integer status, Integer fallback) {
    if (enabled != null) {
      return enabled;
    }
    if (status != null) {
      return status;
    }
    return fallback;
  }

  private String normalizePassword(String password) {
    String trimmed = password == null ? null : password.trim();
    if (StrUtil.isBlank(trimmed)) {
      throw new BizException("password is required");
    }
    if (isBCryptHash(trimmed)) {
      return trimmed;
    }
    return passwordEncoder.encode(trimmed);
  }

  private boolean isBCryptHash(String value) {
    return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$");
  }
}
