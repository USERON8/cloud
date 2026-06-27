package com.cloud.api.auth;

import com.cloud.common.domain.dto.auth.AuthPrincipalDTO;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 认证服务内部调用契约。
 *
 * <p>主要由用户服务、治理服务和权限校验链路调用，用于读取登录主体、角色关系以及维护认证侧账号资料。
 */
public interface AuthDubboApi {

  /** 按用户名查询认证主体，用于登录和权限校验。 */
  AuthPrincipalDTO findPrincipalByUsername(String username);

  /** 按用户 ID 查询认证主体，用于跨服务补全用户身份。 */
  AuthPrincipalDTO findPrincipalById(Long userId);

  /** 创建认证主体，通常在用户注册链路中由用户资料同步触发。 */
  Long createPrincipal(AuthPrincipalDTO authPrincipalDTO);

  /** 更新认证主体基础信息，保持用户服务与认证服务的数据一致。 */
  Boolean updatePrincipal(AuthPrincipalDTO authPrincipalDTO);

  /** 删除认证主体，供用户治理或注销链路调用。 */
  Boolean deletePrincipal(Long userId);

  /** 修改指定用户密码。 */
  Boolean changePassword(Long userId, String oldPassword, String newPassword);

  /** 查询单个用户拥有的角色编码。 */
  List<String> getRoleCodes(Long userId);

  /** 批量查询用户角色编码，减少治理统计场景的远程调用次数。 */
  Map<Long, List<String>> getRoleCodesByUserIds(Collection<Long> userIds);

  /** 查询指定角色下的用户 ID。 */
  List<Long> getUserIdsByRoleCode(String roleCode);

  /** 获取角色分布统计，供用户画像和治理看板使用。 */
  Map<String, Long> getRoleDistribution();
}
