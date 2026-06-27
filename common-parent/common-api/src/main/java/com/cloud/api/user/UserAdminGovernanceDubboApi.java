package com.cloud.api.user;

import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.domain.dto.user.UserPageDTO;
import com.cloud.common.domain.dto.user.UserUpsertRequestDTO;
import com.cloud.common.domain.vo.user.UserPageVO;
import java.util.List;

/**
 * 用户后台治理内部调用契约。
 *
 * <p>治理服务通过该接口完成用户查询、批量维护和状态调整，避免直接依赖用户服务内部类。
 */
public interface UserAdminGovernanceDubboApi {

  /** 按用户名查询用户。 */
  UserDTO findByUsername(String username);

  /** 分页搜索用户。 */
  UserPageVO searchUsers(UserPageDTO request);

  /** 更新用户资料。 */
  Boolean updateUser(Long id, UserUpsertRequestDTO request);

  /** 删除单个用户。 */
  Boolean deleteUser(Long id);

  /** 批量删除用户。 */
  Boolean deleteUsers(List<Long> ids);

  /** 批量更新用户资料。 */
  Boolean updateUsersBatch(List<UserUpsertRequestDTO> requests);

  /** 批量更新用户状态。 */
  Integer updateUserStatusBatch(List<Long> ids, Integer status);
}
