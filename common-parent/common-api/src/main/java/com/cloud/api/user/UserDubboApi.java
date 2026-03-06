package com.cloud.api.user;

import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.domain.dto.user.UserProfileUpsertDTO;

public interface UserDubboApi {

    UserDTO findById(Long id);

    Long create(UserProfileUpsertDTO profileUpsertDTO);

    Boolean update(UserProfileUpsertDTO profileUpsertDTO);
}

