package com.cloud.auth.service.support;

import com.cloud.api.user.UserDubboApi;
import com.cloud.common.domain.dto.auth.AuthPrincipalDTO;
import com.cloud.common.domain.dto.auth.RegisterRequestDTO;
import com.cloud.common.domain.dto.oauth.GitHubUserDTO;
import com.cloud.common.domain.dto.user.UserProfileDTO;
import com.cloud.common.domain.dto.user.UserProfileUpsertDTO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.RemoteException;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.rpc.RpcException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthProfileSyncService {

  @DubboReference(check = false, timeout = 5000, retries = 0)
  private UserDubboApi userDubboApi;

  public UserProfileDTO getProfile(Long userId) {
    if (userId == null) {
      return null;
    }
    return invokeUserService("loading user profile", () -> userDubboApi.findById(userId));
  }

  public UserProfileDTO createRegisteredProfile(
      AuthPrincipalDTO principal, RegisterRequestDTO request) {
    if (principal == null) {
      return null;
    }
    UserProfileUpsertDTO profile = new UserProfileUpsertDTO();
    profile.setId(principal.getId());
    profile.setUsername(principal.getUsername());
    profile.setPhone(request.getPhone());
    profile.setNickname(request.getNickname());
    profile.setStatus(principal.getStatus());
    createProfile(profile);
    return getProfile(principal.getId());
  }

  public UserProfileDTO syncGitHubProfile(AuthPrincipalDTO principal, GitHubUserDTO githubUserDTO) {
    if (principal == null) {
      return null;
    }
    UserProfileUpsertDTO profile = new UserProfileUpsertDTO();
    profile.setId(principal.getId());
    profile.setUsername(principal.getUsername());
    profile.setNickname(githubUserDTO.getDisplayName());
    profile.setEmail(githubUserDTO.getEmail());
    profile.setAvatarUrl(githubUserDTO.getAvatarUrl());
    profile.setStatus(principal.getStatus());

    UserProfileDTO existing = getProfile(principal.getId());
    if (existing == null) {
      createProfile(profile);
    } else {
      runUserService("updating user profile", () -> userDubboApi.update(profile));
    }
    return getProfile(principal.getId());
  }

  private void createProfile(UserProfileUpsertDTO profileUpsertDTO) {
    runUserService("creating user profile", () -> userDubboApi.create(profileUpsertDTO));
  }

  private <T> T invokeUserService(String action, Supplier<T> supplier) {
    try {
      return supplier.get();
    } catch (RpcException ex) {
      log.error("User-service call failed: action={}", action, ex);
      throw new RemoteException(
          ResultCode.REMOTE_SERVICE_UNAVAILABLE, "user-service unavailable when " + action, ex);
    }
  }

  private void runUserService(String action, Runnable runnable) {
    invokeUserService(
        action,
        () -> {
          runnable.run();
          return null;
        });
  }
}
