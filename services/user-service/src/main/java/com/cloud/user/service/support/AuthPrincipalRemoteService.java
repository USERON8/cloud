package com.cloud.user.service.support;

import cn.hutool.core.util.StrUtil;
import com.cloud.api.auth.AuthDubboApi;
import com.cloud.common.domain.dto.auth.AuthPrincipalDTO;
import com.cloud.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthPrincipalRemoteService {

    @DubboReference(check = false, timeout = 5000, retries = 0)
    private AuthDubboApi authDubboApi;

    public void assertUsernameAvailable(String username, Long currentUserId) {
        if (StrUtil.isBlank(username)) {
            return;
        }
        AuthPrincipalDTO existing = authDubboApi.findPrincipalByUsername(username.trim());
        if (existing != null && (currentUserId == null || !existing.getId().equals(currentUserId))) {
            throw new BusinessException("username already exists");
        }
    }

    public Long createPrincipal(AuthPrincipalDTO authPrincipalDTO) {
        Long userId = authDubboApi.createPrincipal(authPrincipalDTO);
        if (userId == null) {
            throw new BusinessException("failed to create auth principal");
        }
        return userId;
    }

    public void updatePrincipal(AuthPrincipalDTO authPrincipalDTO) {
        if (!Boolean.TRUE.equals(authDubboApi.updatePrincipal(authPrincipalDTO))) {
            throw new BusinessException("failed to update auth principal");
        }
    }

    public void deletePrincipal(Long userId) {
        if (userId == null) {
            return;
        }
        authDubboApi.deletePrincipal(userId);
    }

    public List<String> getRoleCodesByUserId(Long userId) {
        return authDubboApi.getRoleCodes(userId);
    }

    public Map<Long, List<String>> getRoleCodesByUserIds(Collection<Long> userIds) {
        return authDubboApi.getRoleCodesByUserIds(userIds);
    }

    public List<Long> getUserIdsByRoleCode(String roleCode) {
        return authDubboApi.getUserIdsByRoleCode(roleCode);
    }

    public Map<String, Long> getRoleDistribution() {
        return authDubboApi.getRoleDistribution();
    }
}
