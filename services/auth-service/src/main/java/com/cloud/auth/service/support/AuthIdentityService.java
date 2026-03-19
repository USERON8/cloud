package com.cloud.auth.service.support;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.cloud.api.auth.AuthDubboApi;
import com.cloud.auth.module.model.OAuthAccountRecord;
import com.cloud.common.domain.dto.auth.AuthPrincipalDTO;
import com.cloud.common.domain.dto.auth.RegisterRequestDTO;
import com.cloud.common.domain.dto.oauth.GitHubUserDTO;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.domain.dto.user.UserProfileDTO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;
import com.cloud.common.exception.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.rpc.RpcException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthIdentityService {

  private static final String GITHUB_PROVIDER = "github";

  @DubboReference(check = false, timeout = 5000, retries = 0)
  private AuthDubboApi authDubboApi;

  private final AuthProfileSyncService authProfileSyncService;
  private final OAuthAccountCacheService oauthAccountCacheService;

  @Transactional(readOnly = true)
  public AuthPrincipalDTO findByUsername(String username) {
    if (StrUtil.isBlank(username)) {
      return null;
    }
    return invokeAuthService(
        "find principal by username", () -> authDubboApi.findPrincipalByUsername(username.trim()));
  }

  @Transactional(readOnly = true)
  public AuthPrincipalDTO findById(Long userId) {
    return userId == null
        ? null
        : invokeAuthService("find principal by id", () -> authDubboApi.findPrincipalById(userId));
  }

  @Transactional(readOnly = true)
  public List<String> getRoleCodes(Long userId) {
    if (userId == null) {
      return List.of();
    }
    List<String> roles =
        invokeAuthService("get role codes", () -> authDubboApi.getRoleCodes(userId));
    return roles == null ? List.of() : roles;
  }

  @Transactional(readOnly = true)
  public Map<Long, List<String>> getRoleCodesByUserIds(List<Long> userIds) {
    if (userIds == null || userIds.isEmpty()) {
      return Map.of();
    }
    Map<Long, List<String>> roles =
        invokeAuthService(
            "get role codes by user ids", () -> authDubboApi.getRoleCodesByUserIds(userIds));
    return roles == null ? Map.of() : roles;
  }

  @Transactional(readOnly = true)
  public List<Long> getUserIdsByRoleCode(String roleCode) {
    if (StrUtil.isBlank(roleCode)) {
      return List.of();
    }
    List<Long> userIds =
        invokeAuthService(
            "get user ids by role code", () -> authDubboApi.getUserIdsByRoleCode(roleCode));
    return userIds == null ? List.of() : userIds;
  }

  @Transactional(readOnly = true)
  public Map<String, Long> getRoleDistribution() {
    Map<String, Long> distribution =
        invokeAuthService("get role distribution", () -> authDubboApi.getRoleDistribution());
    return distribution == null ? Map.of() : distribution;
  }

  @Transactional(readOnly = true)
  public AuthPrincipalDTO findPrincipalByUsername(String username) {
    return findByUsername(username);
  }

  @Transactional(readOnly = true)
  public AuthPrincipalDTO findPrincipalById(Long userId) {
    return findById(userId);
  }

  @Transactional(rollbackFor = Exception.class)
  public Long createPrincipal(AuthPrincipalDTO authPrincipalDTO) {
    if (authPrincipalDTO == null || StrUtil.isBlank(authPrincipalDTO.getUsername())) {
      throw new BizException("username is required");
    }
    return invokeAuthService(
        "create principal", () -> authDubboApi.createPrincipal(authPrincipalDTO));
  }

  @Transactional(rollbackFor = Exception.class)
  public Boolean updatePrincipal(AuthPrincipalDTO authPrincipalDTO) {
    if (authPrincipalDTO == null || authPrincipalDTO.getId() == null) {
      throw new BizException("principal id is required");
    }
    return invokeAuthService(
        "update principal", () -> authDubboApi.updatePrincipal(authPrincipalDTO));
  }

  @Transactional(rollbackFor = Exception.class)
  public Boolean deletePrincipal(Long userId) {
    if (userId == null) {
      return false;
    }
    return invokeAuthService("delete principal", () -> authDubboApi.deletePrincipal(userId));
  }

  @Transactional(rollbackFor = Exception.class)
  public Boolean changePassword(Long userId, String oldPassword, String newPassword) {
    if (userId == null) {
      throw new BizException("principal id is required");
    }
    if (StrUtil.isBlank(oldPassword) || StrUtil.isBlank(newPassword)) {
      throw new BizException("old password and new password are required");
    }
    return invokeAuthService(
        "change password", () -> authDubboApi.changePassword(userId, oldPassword, newPassword));
  }

  @Transactional(rollbackFor = Exception.class)
  public UserDTO register(RegisterRequestDTO registerRequest) {
    String username = StrUtil.trim(registerRequest.getUsername());
    if (findByUsername(username) != null) {
      throw new BizException("username already exists");
    }

    AuthPrincipalDTO principal = new AuthPrincipalDTO();
    principal.setUsername(username);
    principal.setPassword(StrUtil.trim(registerRequest.getPassword()));
    principal.setPhone(registerRequest.getPhone());
    principal.setNickname(registerRequest.getNickname());
    principal.setStatus(1);
    principal.setEnabled(1);
    principal.setRoles(List.of("ROLE_USER"));

    Long userId =
        invokeAuthService("create principal", () -> authDubboApi.createPrincipal(principal));
    if (userId == null) {
      throw new BizException("failed to create user");
    }
    principal.setId(userId);

    UserProfileDTO profile =
        authProfileSyncService.createRegisteredProfile(principal, registerRequest);
    return mergeProfile(
        profile,
        principal,
        principal.getRoles(),
        registerRequest.getPhone(),
        registerRequest.getNickname(),
        null,
        null);
  }

  @Transactional(rollbackFor = Exception.class)
  public UserDTO getOrCreateGitHubUser(GitHubUserDTO githubUserDTO) {
    String providerUserId = String.valueOf(githubUserDTO.getGithubId());
    OAuthAccountRecord account =
        oauthAccountCacheService.getByProviderUserId(GITHUB_PROVIDER, providerUserId);

    AuthPrincipalDTO principal;
    if (account == null) {
      principal = createGitHubPrincipal(githubUserDTO);
      OAuthAccountRecord newAccount =
          OAuthAccountRecord.builder()
              .provider(GITHUB_PROVIDER)
              .providerUserId(providerUserId)
              .userId(principal.getId())
              .providerUsername(githubUserDTO.getLogin())
              .email(githubUserDTO.getEmail())
              .avatarUrl(githubUserDTO.getAvatarUrl())
              .lastSyncAt(System.currentTimeMillis())
              .build();
      oauthAccountCacheService.save(newAccount);
    } else {
      principal =
          invokeAuthService(
              "find principal by id", () -> authDubboApi.findPrincipalById(account.getUserId()));
      if (principal == null) {
        principal = createGitHubPrincipal(githubUserDTO);
        account.setUserId(principal.getId());
      }
      account.setProviderUsername(githubUserDTO.getLogin());
      account.setEmail(githubUserDTO.getEmail());
      account.setAvatarUrl(githubUserDTO.getAvatarUrl());
      account.setLastSyncAt(System.currentTimeMillis());
      oauthAccountCacheService.save(account);
    }

    UserProfileDTO profile = authProfileSyncService.syncGitHubProfile(principal, githubUserDTO);
    return mergeProfile(
        profile,
        principal,
        principal.getRoles(),
        null,
        githubUserDTO.getDisplayName(),
        githubUserDTO.getEmail(),
        githubUserDTO.getAvatarUrl());
  }

  @Transactional(readOnly = true)
  public UserDTO getDisplayUser(Long userId) {
    AuthPrincipalDTO principal = findById(userId);
    if (principal == null) {
      return null;
    }
    UserProfileDTO profile = authProfileSyncService.getProfile(userId);
    return mergeProfile(profile, principal, principal.getRoles(), null, null, null, null);
  }

  private AuthPrincipalDTO createGitHubPrincipal(GitHubUserDTO githubUserDTO) {
    AuthPrincipalDTO principal = new AuthPrincipalDTO();
    principal.setUsername(buildUniqueGithubUsername(githubUserDTO.getLogin()));
    principal.setNickname(
        StrUtil.blankToDefault(githubUserDTO.getDisplayName(), githubUserDTO.getLogin()));
    principal.setEmail(githubUserDTO.getEmail());
    principal.setPassword(
        "github_oauth2_" + githubUserDTO.getGithubId() + "_" + IdUtil.fastSimpleUUID());
    principal.setStatus(1);
    principal.setEnabled(1);
    principal.setRoles(List.of("ROLE_USER"));
    Long userId =
        invokeAuthService("create principal", () -> authDubboApi.createPrincipal(principal));
    if (userId == null) {
      throw new IllegalStateException("Failed to create OAuth user principal");
    }
    principal.setId(userId);
    return principal;
  }

  private UserDTO mergeProfile(
      UserProfileDTO profile,
      AuthPrincipalDTO principal,
      List<String> roles,
      String phone,
      String nickname,
      String email,
      String avatarUrl) {
    UserDTO result = new UserDTO();
    if (profile != null) {
      result.setPhone(profile.getPhone());
      result.setNickname(profile.getNickname());
      result.setEmail(profile.getEmail());
      result.setAvatarUrl(profile.getAvatarUrl());
    }
    result.setId(principal.getId());
    result.setUsername(principal.getUsername());
    result.setStatus(principal.getStatus());
    result.setRoles(roles == null ? List.of() : roles);
    if (StrUtil.isBlank(result.getPhone())) {
      result.setPhone(phone);
    }
    if (StrUtil.isBlank(result.getNickname())) {
      result.setNickname(StrUtil.blankToDefault(nickname, principal.getUsername()));
    }
    if (StrUtil.isBlank(result.getEmail())) {
      result.setEmail(email);
    }
    if (StrUtil.isBlank(result.getAvatarUrl())) {
      result.setAvatarUrl(avatarUrl);
    }
    return result;
  }

  private String buildUniqueGithubUsername(String login) {
    String baseLogin = StrUtil.blankToDefault(StrUtil.trim(login), "user");
    String candidateUsername = "github_" + baseLogin;
    for (int suffix = 1; suffix <= 1000; suffix++) {
      if (findByUsername(candidateUsername) == null) {
        return candidateUsername;
      }
      candidateUsername = StrUtil.format("github_{}_{}", baseLogin, suffix);
    }
    return "github_" + baseLogin + "_" + IdUtil.fastSimpleUUID();
  }

  private <T> T invokeAuthService(String action, Supplier<T> supplier) {
    try {
      return supplier.get();
    } catch (RpcException ex) {
      throw new RemoteException(
          ResultCode.REMOTE_SERVICE_UNAVAILABLE, "auth-service unavailable when " + action, ex);
    }
  }
}
