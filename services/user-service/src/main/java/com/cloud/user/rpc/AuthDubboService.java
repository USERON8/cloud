package com.cloud.user.rpc;

import com.cloud.api.auth.AuthDubboApi;
import com.cloud.common.domain.dto.auth.AuthPrincipalDTO;
import com.cloud.user.service.support.AuthPrincipalService;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService(interfaceClass = AuthDubboApi.class, timeout = 5000, retries = 0)
@RequiredArgsConstructor
public class AuthDubboService implements AuthDubboApi {

  private final AuthPrincipalService authPrincipalService;

  @Override
  public AuthPrincipalDTO findPrincipalByUsername(String username) {
    return authPrincipalService.findPrincipalByUsername(username);
  }

  @Override
  public AuthPrincipalDTO findPrincipalById(Long userId) {
    return authPrincipalService.findPrincipalById(userId);
  }

  @Override
  public Long createPrincipal(AuthPrincipalDTO authPrincipalDTO) {
    return authPrincipalService.createPrincipal(authPrincipalDTO);
  }

  @Override
  public Boolean updatePrincipal(AuthPrincipalDTO authPrincipalDTO) {
    return authPrincipalService.updatePrincipal(authPrincipalDTO);
  }

  @Override
  public Boolean deletePrincipal(Long userId) {
    return authPrincipalService.deletePrincipal(userId);
  }

  @Override
  public Boolean changePassword(Long userId, String oldPassword, String newPassword) {
    return authPrincipalService.changePassword(userId, oldPassword, newPassword);
  }

  @Override
  public List<String> getRoleCodes(Long userId) {
    return authPrincipalService.getRoleCodes(userId);
  }

  @Override
  public Map<Long, List<String>> getRoleCodesByUserIds(Collection<Long> userIds) {
    return authPrincipalService.getRoleCodesByUserIds(userIds);
  }

  @Override
  public List<Long> getUserIdsByRoleCode(String roleCode) {
    return authPrincipalService.getUserIdsByRoleCode(roleCode);
  }

  @Override
  public Map<String, Long> getRoleDistribution() {
    return authPrincipalService.getRoleDistribution();
  }
}
