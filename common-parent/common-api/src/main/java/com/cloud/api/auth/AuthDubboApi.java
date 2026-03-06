package com.cloud.api.auth;

import com.cloud.common.domain.dto.auth.AuthPrincipalDTO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface AuthDubboApi {

    AuthPrincipalDTO findPrincipalByUsername(String username);

    AuthPrincipalDTO findPrincipalById(Long userId);

    Long createPrincipal(AuthPrincipalDTO authPrincipalDTO);

    Boolean updatePrincipal(AuthPrincipalDTO authPrincipalDTO);

    Boolean deletePrincipal(Long userId);

    List<String> getRoleCodes(Long userId);

    Map<Long, List<String>> getRoleCodesByUserIds(Collection<Long> userIds);

    List<Long> getUserIdsByRoleCode(String roleCode);

    Map<String, Long> getRoleDistribution();
}

