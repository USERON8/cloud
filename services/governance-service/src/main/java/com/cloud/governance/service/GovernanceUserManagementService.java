package com.cloud.governance.service;

import com.cloud.api.user.AdminGovernanceDubboApi;
import com.cloud.api.user.UserAdminGovernanceDubboApi;
import com.cloud.common.domain.dto.user.AdminDTO;
import com.cloud.common.domain.dto.user.AdminUpsertRequestDTO;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.domain.dto.user.UserPageDTO;
import com.cloud.common.domain.dto.user.UserUpsertRequestDTO;
import com.cloud.common.domain.vo.user.AdminPageVO;
import com.cloud.common.domain.vo.user.UserPageVO;
import com.cloud.common.remote.RemoteCallSupport;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GovernanceUserManagementService {

  private final RemoteCallSupport remoteCallSupport;

  @DubboReference(check = false, timeout = 5000, retries = 0)
  private AdminGovernanceDubboApi adminGovernanceDubboApi;

  @DubboReference(check = false, timeout = 5000, retries = 0)
  private UserAdminGovernanceDubboApi userAdminGovernanceDubboApi;

  public AdminPageVO getAdmins(Integer page, Integer size) {
    return remoteCallSupport.query(
        "user-service.governance.getAdminsPage",
        () -> adminGovernanceDubboApi.getAdminsPage(page, size));
  }

  public AdminDTO getAdminById(Long id) {
    return remoteCallSupport.query(
        "user-service.governance.getAdminById", () -> adminGovernanceDubboApi.getAdminById(id));
  }

  public AdminDTO createAdmin(AdminUpsertRequestDTO requestDTO) {
    return remoteCallSupport.command(
        "user-service.governance.createAdmin",
        () -> adminGovernanceDubboApi.createAdmin(requestDTO));
  }

  public boolean updateAdmin(Long id, AdminUpsertRequestDTO requestDTO) {
    Boolean updated =
        remoteCallSupport.command(
            "user-service.governance.updateAdmin",
            () -> adminGovernanceDubboApi.updateAdmin(id, requestDTO));
    return Boolean.TRUE.equals(updated);
  }

  public boolean deleteAdmin(Long id) {
    Boolean deleted =
        remoteCallSupport.command(
            "user-service.governance.deleteAdmin", () -> adminGovernanceDubboApi.deleteAdmin(id));
    return Boolean.TRUE.equals(deleted);
  }

  public boolean updateAdminStatus(Long id, Integer status) {
    Boolean updated =
        remoteCallSupport.command(
            "user-service.governance.updateAdminStatus",
            () -> adminGovernanceDubboApi.updateAdminStatus(id, status));
    return Boolean.TRUE.equals(updated);
  }

  public String resetPassword(Long id) {
    return remoteCallSupport.command(
        "user-service.governance.resetPassword", () -> adminGovernanceDubboApi.resetPassword(id));
  }

  public UserDTO findUserByUsername(String username) {
    return remoteCallSupport.query(
        "user-service.governance.findByUsername",
        () -> userAdminGovernanceDubboApi.findByUsername(username));
  }

  public UserPageVO searchUsers(
      Integer page,
      Integer size,
      String username,
      String email,
      String phone,
      String nickname,
      Integer status,
      String roleCode) {
    UserPageDTO request = new UserPageDTO();
    request.setCurrent(page.longValue());
    request.setSize(size.longValue());
    request.setUsername(username);
    request.setEmail(email);
    request.setPhone(phone);
    request.setNickname(nickname);
    request.setStatus(status);
    request.setRoleCode(roleCode);
    return remoteCallSupport.query(
        "user-service.governance.searchUsers",
        () -> userAdminGovernanceDubboApi.searchUsers(request));
  }

  public boolean updateUser(Long id, UserUpsertRequestDTO requestDTO) {
    Boolean updated =
        remoteCallSupport.command(
            "user-service.governance.updateUser",
            () -> userAdminGovernanceDubboApi.updateUser(id, requestDTO));
    return Boolean.TRUE.equals(updated);
  }

  public boolean deleteUser(Long id) {
    Boolean deleted =
        remoteCallSupport.command(
            "user-service.governance.deleteUser", () -> userAdminGovernanceDubboApi.deleteUser(id));
    return Boolean.TRUE.equals(deleted);
  }

  public boolean deleteUsers(List<Long> ids) {
    Boolean deleted =
        remoteCallSupport.command(
            "user-service.governance.deleteUsers",
            () -> userAdminGovernanceDubboApi.deleteUsers(ids));
    return Boolean.TRUE.equals(deleted);
  }

  public boolean updateUsersBatch(List<UserUpsertRequestDTO> requestDTOList) {
    Boolean updated =
        remoteCallSupport.command(
            "user-service.governance.updateUsersBatch",
            () -> userAdminGovernanceDubboApi.updateUsersBatch(requestDTOList));
    return Boolean.TRUE.equals(updated);
  }

  public int updateUserStatusBatch(List<Long> ids, Integer status) {
    Integer successCount =
        remoteCallSupport.command(
            "user-service.governance.updateUserStatusBatch",
            () -> userAdminGovernanceDubboApi.updateUserStatusBatch(ids, status));
    return successCount == null ? 0 : successCount;
  }
}
