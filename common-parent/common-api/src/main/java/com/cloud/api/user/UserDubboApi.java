package com.cloud.api.user;

import com.cloud.common.domain.dto.user.UserProfileDTO;
import com.cloud.common.domain.dto.user.UserProfileUpsertDTO;

public interface UserDubboApi {

  UserProfileDTO findById(Long id);

  Long create(UserProfileUpsertDTO profileUpsertDTO);

  Boolean update(UserProfileUpsertDTO profileUpsertDTO);
}
