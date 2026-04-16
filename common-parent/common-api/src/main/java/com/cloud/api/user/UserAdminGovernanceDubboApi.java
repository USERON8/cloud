package com.cloud.api.user;

import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.domain.dto.user.UserPageDTO;
import com.cloud.common.domain.dto.user.UserUpsertRequestDTO;
import com.cloud.common.domain.vo.user.UserPageVO;
import java.util.List;

public interface UserAdminGovernanceDubboApi {

  UserDTO findByUsername(String username);

  UserPageVO searchUsers(UserPageDTO request);

  Boolean updateUser(Long id, UserUpsertRequestDTO request);

  Boolean deleteUser(Long id);

  Boolean deleteUsers(List<Long> ids);

  Boolean updateUsersBatch(List<UserUpsertRequestDTO> requests);

  Integer updateUserStatusBatch(List<Long> ids, Integer status);
}
