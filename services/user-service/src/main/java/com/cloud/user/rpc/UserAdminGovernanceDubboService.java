package com.cloud.user.rpc;

import com.cloud.api.user.UserAdminGovernanceDubboApi;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.domain.dto.user.UserPageDTO;
import com.cloud.common.domain.dto.user.UserUpsertRequestDTO;
import com.cloud.common.domain.vo.user.UserPageVO;
import com.cloud.common.domain.vo.user.UserVO;
import com.cloud.common.result.PageResult;
import com.cloud.user.service.UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService(interfaceClass = UserAdminGovernanceDubboApi.class, timeout = 5000, retries = 0)
@RequiredArgsConstructor
public class UserAdminGovernanceDubboService implements UserAdminGovernanceDubboApi {

  private final UserService userService;

  @Override
  public UserDTO findByUsername(String username) {
    return userService.findByUsername(username);
  }

  @Override
  public UserPageVO searchUsers(UserPageDTO request) {
    PageResult<UserVO> pageResult = userService.pageQuery(request);
    return UserPageVO.builder()
        .current(pageResult.getCurrent())
        .size(pageResult.getSize())
        .total(pageResult.getTotal())
        .pages(pageResult.getPages())
        .records(pageResult.getRecords())
        .hasPrevious(pageResult.getHasPrevious())
        .hasNext(pageResult.getHasNext())
        .build();
  }

  @Override
  public Boolean updateUser(Long id, UserUpsertRequestDTO request) {
    return userService.updateUser(id, request);
  }

  @Override
  public Boolean deleteUser(Long id) {
    return userService.deleteUserById(id);
  }

  @Override
  public Boolean deleteUsers(List<Long> ids) {
    return userService.deleteUsersByIds(ids);
  }

  @Override
  public Boolean updateUsersBatch(List<UserUpsertRequestDTO> requests) {
    return userService.updateUsersBatch(requests);
  }

  @Override
  public Integer updateUserStatusBatch(List<Long> ids, Integer status) {
    return userService.batchUpdateUserStatus(ids, status);
  }
}
