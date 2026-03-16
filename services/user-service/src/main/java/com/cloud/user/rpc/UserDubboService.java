package com.cloud.user.rpc;

import com.cloud.api.user.UserDubboApi;
import com.cloud.common.domain.dto.user.UserProfileDTO;
import com.cloud.common.domain.dto.user.UserProfileUpsertDTO;
import com.cloud.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService(interfaceClass = UserDubboApi.class, timeout = 5000, retries = 0)
@RequiredArgsConstructor
public class UserDubboService implements UserDubboApi {

  private final UserService userService;

  @Override
  public UserProfileDTO findById(Long id) {
    return userService.getProfileById(id);
  }

  @Override
  public Long create(UserProfileUpsertDTO profileUpsertDTO) {
    return userService.createProfile(profileUpsertDTO);
  }

  @Override
  public Boolean update(UserProfileUpsertDTO profileUpsertDTO) {
    return userService.updateProfile(profileUpsertDTO);
  }
}
