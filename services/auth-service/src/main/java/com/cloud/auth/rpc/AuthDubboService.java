package com.cloud.auth.rpc;

import com.cloud.api.auth.AuthDubboApi;
import com.cloud.auth.service.support.AuthIdentityService;
import com.cloud.common.domain.dto.auth.AuthPrincipalDTO;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@DubboService(interfaceClass = AuthDubboApi.class, timeout = 5000, retries = 0)
@RequiredArgsConstructor
public class AuthDubboService implements AuthDubboApi {

    private final AuthIdentityService authIdentityService;

    @Override
    public AuthPrincipalDTO findPrincipalByUsername(String username) {
        return authIdentityService.findPrincipalByUsername(username);
    }

    @Override
    public AuthPrincipalDTO findPrincipalById(Long userId) {
        return authIdentityService.findPrincipalById(userId);
    }

    @Override
    public Long createPrincipal(AuthPrincipalDTO authPrincipalDTO) {
        return authIdentityService.createPrincipal(authPrincipalDTO);
    }

    @Override
    public Boolean updatePrincipal(AuthPrincipalDTO authPrincipalDTO) {
        return authIdentityService.updatePrincipal(authPrincipalDTO);
    }

    @Override
    public Boolean deletePrincipal(Long userId) {
        return authIdentityService.deletePrincipal(userId);
    }

    @Override
    public List<String> getRoleCodes(Long userId) {
        return authIdentityService.getRoleCodes(userId);
    }

    @Override
    public Map<Long, List<String>> getRoleCodesByUserIds(Collection<Long> userIds) {
        return authIdentityService.getRoleCodesByUserIds(userIds == null ? List.of() : List.copyOf(userIds));
    }

    @Override
    public List<Long> getUserIdsByRoleCode(String roleCode) {
        return authIdentityService.getUserIdsByRoleCode(roleCode);
    }

    @Override
    public Map<String, Long> getRoleDistribution() {
        return authIdentityService.getRoleDistribution();
    }
}
