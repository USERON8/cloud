package com.cloud.api.user;

import com.cloud.common.domain.dto.user.UserDTO;

public interface UserDubboApi {

    UserDTO findById(Long id);

    Long create(UserDTO userDTO);

    Boolean update(UserDTO userDTO);
}

