package com.cloud.auth.service.support;

import cn.hutool.core.util.IdUtil;
import com.cloud.api.user.UserDubboApi;
import com.cloud.auth.messaging.UserProfileSyncMessageProducer;
import com.cloud.common.domain.dto.auth.AuthPrincipalDTO;
import com.cloud.common.domain.dto.auth.RegisterRequestDTO;
import com.cloud.common.domain.dto.oauth.GitHubUserDTO;
import com.cloud.common.domain.dto.user.UserProfileDTO;
import com.cloud.common.domain.dto.user.UserProfileUpsertDTO;
import com.cloud.common.messaging.event.UserProfileSyncEvent;
import com.cloud.common.remote.RemoteCallSupport;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthProfileSyncService {

  @DubboReference(check = false, timeout = 5000, retries = 0)
  private UserDubboApi userDubboApi;

  private final UserProfileSyncMessageProducer userProfileSyncMessageProducer;
  private final RemoteCallSupport remoteCallSupport;

  public UserProfileDTO getProfile(Long userId) {
    if (userId == null) {
      return null;
    }
    return remoteCallSupport.queryOrFallback(
        "user-service.findProfileById",
        () -> userDubboApi.findById(userId),
        ex -> {
          log.warn("Falling back to empty profile: userId={}, reason={}", userId, ex.getMessage());
          return null;
        });
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
    publishUpsert(profile);
    return toProfileDTO(profile);
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

    publishUpsert(profile);
    return toProfileDTO(profile);
  }

  private <T> T invokeUserService(String action, Supplier<T> supplier) {
    return remoteCallSupport.query("user-service." + action, supplier);
  }

  private void runUserService(String action, Runnable runnable) {
    invokeUserService(
        action,
        () -> {
          runnable.run();
          return null;
        });
  }

  private void publishUpsert(UserProfileUpsertDTO profileUpsertDTO) {
    userProfileSyncMessageProducer.sendAfterCommit(
        UserProfileSyncEvent.builder()
            .eventId("USER_PROFILE_SYNC_" + IdUtil.fastSimpleUUID())
            .eventType(UserProfileSyncEvent.TYPE_UPSERT)
            .userId(profileUpsertDTO.getId())
            .username(profileUpsertDTO.getUsername())
            .phone(profileUpsertDTO.getPhone())
            .nickname(profileUpsertDTO.getNickname())
            .email(profileUpsertDTO.getEmail())
            .avatarUrl(profileUpsertDTO.getAvatarUrl())
            .status(profileUpsertDTO.getStatus())
            .build());
  }

  private UserProfileDTO toProfileDTO(UserProfileUpsertDTO profileUpsertDTO) {
    UserProfileDTO profile = new UserProfileDTO();
    profile.setId(profileUpsertDTO.getId());
    profile.setUsername(profileUpsertDTO.getUsername());
    profile.setPhone(profileUpsertDTO.getPhone());
    profile.setNickname(profileUpsertDTO.getNickname());
    profile.setEmail(profileUpsertDTO.getEmail());
    profile.setAvatarUrl(profileUpsertDTO.getAvatarUrl());
    profile.setStatus(profileUpsertDTO.getStatus());
    return profile;
  }
}
