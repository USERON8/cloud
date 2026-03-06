package com.cloud.auth.service.support;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.auth.mapper.AuthOauthAccountMapper;
import com.cloud.auth.mapper.AuthUserMapper;
import com.cloud.auth.module.entity.AuthOauthAccount;
import com.cloud.auth.module.entity.AuthUser;
import com.cloud.common.domain.dto.auth.AuthPrincipalDTO;
import com.cloud.common.domain.dto.auth.RegisterRequestDTO;
import com.cloud.common.domain.dto.oauth.GitHubUserDTO;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthIdentityService {

    private static final String GITHUB_PROVIDER = "github";

    private final AuthUserMapper authUserMapper;
    private final AuthOauthAccountMapper authOauthAccountMapper;
    private final AuthRoleAssignmentService authRoleAssignmentService;
    private final AuthProfileSyncService authProfileSyncService;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public AuthUser findByUsername(String username) {
        if (StrUtil.isBlank(username)) {
            return null;
        }
        return authUserMapper.selectOne(new LambdaQueryWrapper<AuthUser>()
                .eq(AuthUser::getUsername, username.trim())
                .last("limit 1"));
    }

    @Transactional(readOnly = true)
    public AuthUser findById(Long userId) {
        return userId == null ? null : authUserMapper.selectById(userId);
    }

    @Transactional(readOnly = true)
    public List<String> getRoleCodes(Long userId) {
        return authRoleAssignmentService.getRoleCodesByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Map<Long, List<String>> getRoleCodesByUserIds(List<Long> userIds) {
        return authRoleAssignmentService.getRoleCodesByUserIds(userIds);
    }

    @Transactional(readOnly = true)
    public List<Long> getUserIdsByRoleCode(String roleCode) {
        return authRoleAssignmentService.getUserIdsByRoleCode(roleCode);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getRoleDistribution() {
        return authRoleAssignmentService.getRoleDistribution();
    }

    @Transactional(readOnly = true)
    public AuthPrincipalDTO findPrincipalByUsername(String username) {
        return toPrincipalDTO(findByUsername(username));
    }

    @Transactional(readOnly = true)
    public AuthPrincipalDTO findPrincipalById(Long userId) {
        return toPrincipalDTO(findById(userId));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createPrincipal(AuthPrincipalDTO authPrincipalDTO) {
        if (authPrincipalDTO == null || StrUtil.isBlank(authPrincipalDTO.getUsername())) {
            throw new BusinessException("username is required");
        }
        if (findByUsername(authPrincipalDTO.getUsername()) != null) {
            throw new BusinessException("username already exists");
        }

        AuthUser authUser = new AuthUser();
        authUser.setId(authPrincipalDTO.getId());
        authUser.setUsername(StrUtil.trim(authPrincipalDTO.getUsername()));
        authUser.setPassword(normalizePassword(authPrincipalDTO.getPassword()));
        authUser.setStatus(authPrincipalDTO.getStatus() == null ? 1 : authPrincipalDTO.getStatus());
        authUserMapper.insert(authUser);
        authRoleAssignmentService.replaceRoles(authUser.getId(), authPrincipalDTO.getRoles());
        return authUser.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean updatePrincipal(AuthPrincipalDTO authPrincipalDTO) {
        if (authPrincipalDTO == null || authPrincipalDTO.getId() == null) {
            throw new BusinessException("principal id is required");
        }

        AuthUser existing = authUserMapper.selectById(authPrincipalDTO.getId());
        if (existing == null) {
            throw new BusinessException("principal not found");
        }

        String newUsername = StrUtil.trim(authPrincipalDTO.getUsername());
        if (StrUtil.isNotBlank(newUsername) && !StrUtil.equals(newUsername, existing.getUsername())) {
            AuthUser duplicate = findByUsername(newUsername);
            if (duplicate != null && !duplicate.getId().equals(authPrincipalDTO.getId())) {
                throw new BusinessException("username already exists");
            }
        }

        AuthUser update = new AuthUser();
        update.setId(authPrincipalDTO.getId());
        if (StrUtil.isNotBlank(newUsername)) {
            update.setUsername(newUsername);
        }
        if (StrUtil.isNotBlank(authPrincipalDTO.getPassword())) {
            update.setPassword(normalizePassword(authPrincipalDTO.getPassword()));
        }
        if (authPrincipalDTO.getStatus() != null) {
            update.setStatus(authPrincipalDTO.getStatus());
        }
        boolean updated = authUserMapper.updateById(update) > 0;
        if (updated && authPrincipalDTO.getRoles() != null) {
            authRoleAssignmentService.replaceRoles(authPrincipalDTO.getId(), authPrincipalDTO.getRoles());
        }
        return updated;
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean deletePrincipal(Long userId) {
        if (userId == null) {
            return false;
        }
        authRoleAssignmentService.replaceRoles(userId, List.of());
        authOauthAccountMapper.delete(new LambdaQueryWrapper<AuthOauthAccount>()
                .eq(AuthOauthAccount::getUserId, userId));
        return authUserMapper.deleteById(userId) > 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean changePassword(Long userId, String oldPassword, String newPassword) {
        if (userId == null) {
            throw new BusinessException("principal id is required");
        }
        if (StrUtil.isBlank(oldPassword) || StrUtil.isBlank(newPassword)) {
            throw new BusinessException("old password and new password are required");
        }

        AuthUser existing = authUserMapper.selectById(userId);
        if (existing == null) {
            throw new BusinessException("principal not found");
        }
        if (!passwordEncoder.matches(oldPassword, existing.getPassword())) {
            return false;
        }

        AuthUser update = new AuthUser();
        update.setId(userId);
        update.setPassword(normalizePassword(newPassword));
        return authUserMapper.updateById(update) > 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public UserDTO register(RegisterRequestDTO registerRequest) {
        String username = StrUtil.trim(registerRequest.getUsername());
        if (findByUsername(username) != null) {
            throw new BusinessException("username already exists");
        }

        AuthUser authUser = new AuthUser();
        authUser.setUsername(username);
        authUser.setPassword(passwordEncoder.encode(StrUtil.trim(registerRequest.getPassword())));
        authUser.setStatus(1);
        authUserMapper.insert(authUser);

        authRoleAssignmentService.replaceRoles(authUser.getId(), List.of("USER"));
        List<String> roles = authRoleAssignmentService.getRoleCodesByUserId(authUser.getId());
        UserDTO profile = authProfileSyncService.createRegisteredProfile(authUser, roles, registerRequest);
        return mergeProfile(profile, authUser, roles, registerRequest.getPhone(), registerRequest.getNickname(), null, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public UserDTO getOrCreateGitHubUser(GitHubUserDTO githubUserDTO) {
        String providerUserId = String.valueOf(githubUserDTO.getGithubId());
        AuthOauthAccount existingAccount = authOauthAccountMapper.selectOne(new LambdaQueryWrapper<AuthOauthAccount>()
                .eq(AuthOauthAccount::getProvider, GITHUB_PROVIDER)
                .eq(AuthOauthAccount::getProviderUserId, providerUserId)
                .last("limit 1"));

        AuthUser authUser;
        if (existingAccount == null) {
            authUser = new AuthUser();
            authUser.setUsername(buildUniqueGithubUsername(githubUserDTO.getLogin()));
            authUser.setPassword(passwordEncoder.encode("github_oauth2_" + githubUserDTO.getGithubId() + "_" + IdUtil.fastSimpleUUID()));
            authUser.setStatus(1);
            authUserMapper.insert(authUser);
            authRoleAssignmentService.replaceRoles(authUser.getId(), List.of("USER"));

            AuthOauthAccount account = new AuthOauthAccount();
            account.setUserId(authUser.getId());
            account.setProvider(GITHUB_PROVIDER);
            account.setProviderUserId(providerUserId);
            account.setProviderUsername(githubUserDTO.getLogin());
            account.setEmail(githubUserDTO.getEmail());
            account.setAvatarUrl(githubUserDTO.getAvatarUrl());
            authOauthAccountMapper.insert(account);
        } else {
            authUser = authUserMapper.selectById(existingAccount.getUserId());
            if (authUser == null) {
                throw new IllegalStateException("OAuth account exists but auth user is missing");
            }
            AuthOauthAccount update = new AuthOauthAccount();
            update.setId(existingAccount.getId());
            update.setProviderUsername(githubUserDTO.getLogin());
            update.setEmail(githubUserDTO.getEmail());
            update.setAvatarUrl(githubUserDTO.getAvatarUrl());
            authOauthAccountMapper.updateById(update);
        }

        List<String> roles = authRoleAssignmentService.getRoleCodesByUserId(authUser.getId());
        UserDTO profile = authProfileSyncService.syncGitHubProfile(authUser, roles, githubUserDTO);
        return mergeProfile(profile, authUser, roles, null, githubUserDTO.getDisplayName(), githubUserDTO.getEmail(), githubUserDTO.getAvatarUrl());
    }

    @Transactional(readOnly = true)
    public UserDTO getDisplayUser(Long userId) {
        AuthUser authUser = findById(userId);
        if (authUser == null) {
            return null;
        }
        List<String> roles = authRoleAssignmentService.getRoleCodesByUserId(userId);
        UserDTO profile = authProfileSyncService.getProfile(userId);
        return mergeProfile(profile, authUser, roles, null, null, null, null);
    }

    private UserDTO mergeProfile(UserDTO profile,
                                 AuthUser authUser,
                                 List<String> roles,
                                 String phone,
                                 String nickname,
                                 String email,
                                 String avatarUrl) {
        UserDTO result = profile == null ? new UserDTO() : profile;
        result.setId(authUser.getId());
        result.setUsername(authUser.getUsername());
        result.setStatus(authUser.getStatus());
        result.setRoles(roles);
        if (StrUtil.isBlank(result.getPhone())) {
            result.setPhone(phone);
        }
        if (StrUtil.isBlank(result.getNickname())) {
            result.setNickname(StrUtil.blankToDefault(nickname, authUser.getUsername()));
        }
        if (StrUtil.isBlank(result.getEmail())) {
            result.setEmail(email);
        }
        if (StrUtil.isBlank(result.getAvatarUrl())) {
            result.setAvatarUrl(avatarUrl);
        }
        return result;
    }

    private String buildUniqueGithubUsername(String login) {
        String baseLogin = StrUtil.blankToDefault(StrUtil.trim(login), "user");
        String candidateUsername = "github_" + baseLogin;
        for (int suffix = 1; suffix <= 1000; suffix++) {
            if (findByUsername(candidateUsername) == null) {
                return candidateUsername;
            }
            candidateUsername = StrUtil.format("github_{}_{}", baseLogin, suffix);
        }
        return "github_" + baseLogin + "_" + IdUtil.fastSimpleUUID();
    }

    private AuthPrincipalDTO toPrincipalDTO(AuthUser authUser) {
        if (authUser == null) {
            return null;
        }
        AuthPrincipalDTO dto = new AuthPrincipalDTO();
        dto.setId(authUser.getId());
        dto.setUsername(authUser.getUsername());
        dto.setStatus(authUser.getStatus());
        dto.setRoles(authRoleAssignmentService.getRoleCodesByUserId(authUser.getId()));
        return dto;
    }

    private String normalizePassword(String password) {
        String trimmed = password == null ? null : password.trim();
        if (StrUtil.isBlank(trimmed)) {
            throw new BusinessException("password is required");
        }
        if (isBCryptHash(trimmed)) {
            return trimmed;
        }
        return passwordEncoder.encode(trimmed);
    }

    private boolean isBCryptHash(String value) {
        return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$");
    }
}
