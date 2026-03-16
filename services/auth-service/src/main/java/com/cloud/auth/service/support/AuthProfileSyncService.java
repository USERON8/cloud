package com.cloud.auth.service.support;

import com.cloud.api.user.UserDubboApi;
import com.cloud.common.domain.dto.auth.AuthPrincipalDTO;
import com.cloud.common.domain.dto.auth.RegisterRequestDTO;
import com.cloud.common.domain.dto.oauth.GitHubUserDTO;
import com.cloud.common.domain.dto.user.UserProfileDTO;
import com.cloud.common.domain.dto.user.UserProfileUpsertDTO;
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

  public UserProfileDTO getProfile(Long userId) {
    if (userId == null) {
      return null;
    }
    try {
      return userDubboApi.findById(userId);
    } catch (Exception ex) {
      log.warn("Failed to load user profile from user-service, userId={}", userId, ex);
      return null;
    }
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
    try {
      if (existing == null) {
        createProfile(profile);
      } else {
        userDubboApi.update(profile);
      }
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to sync OAuth profile to user-service", ex);
    }
    return getProfile(principal.getId());
  }

  private void createProfile(UserProfileUpsertDTO profileUpsertDTO) {
    try {
      userDubboApi.create(profileUpsertDTO);
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to create user profile in user-service", ex);
    }
  }
}
