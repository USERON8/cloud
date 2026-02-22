package com.cloud.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.common.annotation.DistributedLock;
import com.cloud.common.domain.dto.auth.RegisterRequestDTO;
import com.cloud.common.domain.dto.oauth.GitHubUserDTO;
import com.cloud.common.domain.dto.user.MerchantDTO;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.domain.dto.user.UserPageDTO;
import com.cloud.common.domain.vo.user.UserVO;
import com.cloud.common.enums.UserType;
import com.cloud.common.exception.BusinessException;
import com.cloud.common.exception.EntityNotFoundException;
import com.cloud.common.result.PageResult;
import com.cloud.common.utils.PageUtils;
import com.cloud.user.converter.MerchantConverter;
import com.cloud.user.converter.UserConverter;
import com.cloud.user.mapper.UserMapper;
import com.cloud.user.module.entity.User;
import com.cloud.user.service.MerchantService;
import com.cloud.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final UserConverter userConverter;
    private final PasswordEncoder passwordEncoder;
    private final MerchantService merchantService;
    private final MerchantConverter merchantConverter;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "user", key = "'username:' + #username", unless = "#result == null")
    public UserDTO findByUsername(String username) {
        if (StringUtils.isBlank(username)) {
            throw new BusinessException("username is required");
        }

        User user = getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        return user == null ? null : userConverter.toDTO(user);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = "userList",
            key = "'page:' + #pageDTO.current + ':size:' + #pageDTO.size + ':username:' + (#pageDTO.username == null ? '' : #pageDTO.username) + ':phone:' + (#pageDTO.phone == null ? '' : #pageDTO.phone) + ':nickname:' + (#pageDTO.nickname == null ? '' : #pageDTO.nickname) + ':status:' + (#pageDTO.status == null ? '' : #pageDTO.status) + ':userType:' + (#pageDTO.userType == null ? '' : #pageDTO.userType)",
            condition = "#pageDTO.current <= 10",
            unless = "#result == null || #result.total == 0"
    )
    public PageResult<UserVO> pageQuery(UserPageDTO pageDTO) {
        Page<User> page = PageUtils.buildPage(pageDTO);

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(pageDTO.getUsername())) {
            queryWrapper.like(User::getUsername, pageDTO.getUsername());
        }
        if (StringUtils.isNotBlank(pageDTO.getPhone())) {
            queryWrapper.like(User::getPhone, pageDTO.getPhone());
        }
        if (StringUtils.isNotBlank(pageDTO.getNickname())) {
            queryWrapper.like(User::getNickname, pageDTO.getNickname());
        }
        if (pageDTO.getStatus() != null) {
            queryWrapper.eq(User::getStatus, pageDTO.getStatus());
        }
        if (StringUtils.isNotBlank(pageDTO.getUserType())) {
            queryWrapper.eq(User::getUserType, pageDTO.getUserType());
        }
        queryWrapper.orderByDesc(User::getCreatedAt);

        Page<User> resultPage = this.page(page, queryWrapper);
        List<UserVO> userVOList = userConverter.toVOList(resultPage.getRecords());
        return PageResult.of(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal(), userVOList);
    }

    @Override
    @DistributedLock(
            key = "'user:delete:' + #id",
            waitTime = 5,
            leaseTime = 15,
            failMessage = "failed to acquire user delete lock"
    )
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(cacheNames = "user", key = "#id"),
            @CacheEvict(cacheNames = "userList", allEntries = true)
    })
    public boolean deleteUserById(Long id) {
        if (id == null) {
            throw new BusinessException("user id is required");
        }
        User user = getById(id);
        if (user == null) {
            throw new EntityNotFoundException("user", id);
        }
        return removeById(id);
    }

    @Override
    @DistributedLock(
            key = "'user:batch:delete:' + #userIds.toString()",
            waitTime = 10,
            leaseTime = 30,
            failMessage = "failed to acquire user batch delete lock"
    )
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(cacheNames = "user", allEntries = true),
            @CacheEvict(cacheNames = "userList", allEntries = true)
    })
    public boolean deleteUsersByIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            throw new BusinessException("user ids are required");
        }
        return removeByIds(userIds);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "userInfo", key = "#id", unless = "#result == null")
    public UserDTO getUserById(Long id) {
        if (id == null) {
            throw new BusinessException("user id is required");
        }
        User user = getById(id);
        if (user == null) {
            throw new EntityNotFoundException("user", id);
        }
        return userConverter.toDTO(user);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "user", key = "'username:' + #username", unless = "#result == null")
    public UserDTO getUserByUsername(String username) {
        return findByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = "user",
            key = "'batch:' + #userIds.toString()",
            condition = "#userIds != null && #userIds.size() <= 100",
            unless = "#result == null || #result.isEmpty()"
    )
    public List<UserDTO> getUsersByIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }
        return listByIds(userIds).stream().map(userConverter::toDTO).collect(Collectors.toList());
    }

    @Override
    @DistributedLock(
            key = "'user:register:' + #registerRequest.username",
            waitTime = 3,
            leaseTime = 15,
            failMessage = "failed to acquire user register lock"
    )
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(cacheNames = "user", key = "'username:' + #registerRequest.username", beforeInvocation = true),
            @CacheEvict(cacheNames = "userList", allEntries = true)
    })
    public UserDTO registerUser(RegisterRequestDTO registerRequest) {
        if (findByUsername(registerRequest.getUsername()) != null) {
            throw new BusinessException("username already exists");
        }

        User user = userConverter.toEntity(registerRequest);
        if (StringUtils.isBlank(registerRequest.getPassword())) {
            throw new BusinessException("password is required");
        }
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword().trim()));
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        if (StringUtils.isBlank(user.getUserType())) {
            user.setUserType(registerRequest.getUserType() == null ? UserType.USER.getCode() : registerRequest.getUserType());
        }

        if (!save(user)) {
            throw new BusinessException("failed to register user");
        }

        UserDTO userDTO = userConverter.toDTO(user);
        if (UserType.MERCHANT.getCode().equalsIgnoreCase(registerRequest.getUserType())) {
            createMerchantForUser(userDTO);
        }
        return userDTO;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "auth", key = "'password:' + #username", unless = "#result == null")
    public String getUserPassword(String username) {
        try {
            User user = getOne(new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, username)
                    .select(User::getUsername, User::getPassword, User::getStatus));
            if (user == null) {
                return null;
            }
            if (user.getStatus() == null || user.getStatus() != 1) {
                return null;
            }
            return user.getPassword();
        } catch (Exception e) {
            log.error("Failed to get user password", e);
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "user", key = "'github_id:' + #githubId", unless = "#result == null")
    public UserDTO findByGitHubId(Long githubId) {
        if (githubId == null) {
            return null;
        }
        User user = getOne(new LambdaQueryWrapper<User>()
                .eq(User::getGithubId, githubId)
                .eq(User::getOauthProvider, "github"));
        return user == null ? null : userConverter.toDTO(user);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "user", key = "'github_username:' + #githubUsername", unless = "#result == null")
    public UserDTO findByGitHubUsername(String githubUsername) {
        if (StringUtils.isBlank(githubUsername)) {
            return null;
        }
        User user = getOne(new LambdaQueryWrapper<User>()
                .eq(User::getGithubUsername, githubUsername)
                .eq(User::getOauthProvider, "github"));
        return user == null ? null : userConverter.toDTO(user);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "user", key = "'oauth:' + #oauthProvider + ':' + #oauthProviderId", unless = "#result == null")
    public UserDTO findByOAuthProvider(String oauthProvider, String oauthProviderId) {
        if (StringUtils.isBlank(oauthProvider) || StringUtils.isBlank(oauthProviderId)) {
            return null;
        }
        User user = getOne(new LambdaQueryWrapper<User>()
                .eq(User::getOauthProvider, oauthProvider)
                .eq(User::getOauthProviderId, oauthProviderId));
        return user == null ? null : userConverter.toDTO(user);
    }

    @Override
    @DistributedLock(
            key = "'user:github:create:' + #githubUserDTO.githubId",
            waitTime = 3,
            leaseTime = 15,
            failMessage = "failed to acquire github user create lock"
    )
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = "user", key = "'github_id:' + #githubUserDTO.githubId", beforeInvocation = true)
    public UserDTO createGitHubUser(GitHubUserDTO githubUserDTO) {
        UserDTO existing = findByGitHubId(githubUserDTO.getGithubId());
        if (existing != null) {
            return existing;
        }

        String username = buildUniqueGithubUsername(githubUserDTO.getLogin());
        User user = new User();
        user.setUsername(username);
        user.setNickname(githubUserDTO.getDisplayName());
        user.setEmail(githubUserDTO.getEmail());
        user.setAvatarUrl(githubUserDTO.getAvatarUrl());
        user.setUserType(UserType.USER.getCode());
        user.setStatus(1);
        user.setPhone("00000000000");
        user.setGithubId(githubUserDTO.getGithubId());
        user.setGithubUsername(githubUserDTO.getLogin());
        user.setOauthProvider("github");
        user.setOauthProviderId(String.valueOf(githubUserDTO.getGithubId()));
        user.setPassword(passwordEncoder.encode("github_oauth2_" + githubUserDTO.getGithubId()));

        if (!save(user)) {
            throw new BusinessException("failed to create github user");
        }
        return userConverter.toDTO(user);
    }

    @Override
    @DistributedLock(
            key = "'user:github:update:' + #userId",
            waitTime = 3,
            leaseTime = 10,
            failMessage = "failed to acquire github user update lock"
    )
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(cacheNames = "user", key = "#userId"),
            @CacheEvict(cacheNames = "user", key = "'github_id:' + #githubUserDTO.githubId")
    })
    public boolean updateGitHubUserInfo(Long userId, GitHubUserDTO githubUserDTO) {
        User existingUser = getById(userId);
        if (existingUser == null) {
            throw new EntityNotFoundException("user", userId);
        }

        User updatedUser = new User();
        updatedUser.setId(userId);
        updatedUser.setNickname(githubUserDTO.getDisplayName());
        updatedUser.setEmail(githubUserDTO.getEmail());
        updatedUser.setAvatarUrl(githubUserDTO.getAvatarUrl());
        updatedUser.setGithubId(githubUserDTO.getGithubId());
        updatedUser.setGithubUsername(githubUserDTO.getLogin());
        updatedUser.setOauthProvider("github");
        updatedUser.setOauthProviderId(String.valueOf(githubUserDTO.getGithubId()));
        return updateById(updatedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDTO> getUsersPage(Integer page, Integer size) {
        Page<User> pageParam = new Page<>(page, size);
        Page<User> userPage = page(pageParam);

        Page<UserDTO> dtoPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        dtoPage.setRecords(userPage.getRecords().stream().map(userConverter::toDTO).collect(Collectors.toList()));
        return dtoPage;
    }

    @Override
    @DistributedLock(
            key = "'user:create:' + #userDTO.username",
            waitTime = 5,
            leaseTime = 15,
            failMessage = "failed to acquire user create lock"
    )
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(cacheNames = "user", allEntries = true),
            @CacheEvict(cacheNames = "userList", allEntries = true)
    })
    public Long createUser(UserDTO userDTO) {
        User user = userConverter.toEntity(userDTO);
        if (org.springframework.util.StringUtils.hasText(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        save(user);
        return user.getId();
    }

    @Override
    @DistributedLock(
            key = "'user:update:' + #userDTO.id",
            waitTime = 5,
            leaseTime = 15,
            failMessage = "failed to acquire user update lock"
    )
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(cacheNames = "user", key = "#userDTO.id"),
            @CacheEvict(cacheNames = "user", key = "'username:' + #userDTO.username"),
            @CacheEvict(cacheNames = "userList", allEntries = true)
    })
    public Boolean updateUser(UserDTO userDTO) {
        User user = userConverter.toEntity(userDTO);
        return updateById(user);
    }

    @Override
    @DistributedLock(
            key = "'user:delete:' + #id",
            waitTime = 5,
            leaseTime = 15,
            failMessage = "failed to acquire user delete lock"
    )
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(cacheNames = "user", key = "#id"),
            @CacheEvict(cacheNames = "user", allEntries = true),
            @CacheEvict(cacheNames = "userList", allEntries = true)
    })
    public Boolean deleteUser(Long id) {
        return removeById(id);
    }

    @Override
    @DistributedLock(
            key = "'user:status:' + #id",
            waitTime = 5,
            leaseTime = 15,
            failMessage = "failed to acquire user status lock"
    )
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(cacheNames = "user", key = "#id"),
            @CacheEvict(cacheNames = "userList", allEntries = true)
    })
    public Boolean updateUserStatus(Long id, Integer status) {
        User user = new User();
        user.setId(id);
        user.setStatus(status);
        return updateById(user);
    }

    @Override
    @DistributedLock(
            key = "'user:password:reset:' + #id",
            waitTime = 5,
            leaseTime = 15,
            failMessage = "failed to acquire reset password lock"
    )
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = "user", key = "#id")
    public String resetPassword(Long id) {
        String newPassword = "123456";
        User user = new User();
        user.setId(id);
        user.setPassword(passwordEncoder.encode(newPassword));
        updateById(user);
        return newPassword;
    }

    @Override
    @DistributedLock(
            key = "'user:password:change:' + #id",
            waitTime = 5,
            leaseTime = 15,
            failMessage = "failed to acquire change password lock"
    )
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = "user", key = "#id")
    public Boolean changePassword(Long id, String oldPassword, String newPassword) {
        User user = getById(id);
        if (user == null) {
            throw new EntityNotFoundException("user", id);
        }

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("old password mismatch");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        return updateById(user);
    }

    @Override
    @DistributedLock(
            key = "'user:batch:status:' + #userIds.toString()",
            waitTime = 10,
            leaseTime = 30,
            failMessage = "failed to acquire batch update status lock"
    )
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(cacheNames = "user", allEntries = true),
            @CacheEvict(cacheNames = "userList", allEntries = true)
    })
    public Integer batchUpdateUserStatus(Collection<Long> userIds, Integer status) {
        if (userIds == null || userIds.isEmpty()) {
            throw new BusinessException("user ids are required");
        }
        if (status == null) {
            throw new BusinessException("status is required");
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(User::getId, userIds);

        User updateEntity = new User();
        updateEntity.setStatus(status);
        return update(updateEntity, wrapper) ? userIds.size() : 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CachePut(cacheNames = "user", key = "#entity.id")
    public boolean save(User entity) {
        return super.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "user", key = "#entity.id"),
                    @CacheEvict(cacheNames = "user", key = "'username:' + #entity.username", condition = "#entity.username != null"),
                    @CacheEvict(cacheNames = "userList", allEntries = true)
            },
            put = @CachePut(cacheNames = "user", key = "#entity.id")
    )
    public boolean updateById(User entity) {
        return super.updateById(entity);
    }

    private void createMerchantForUser(UserDTO userDTO) {
        MerchantDTO merchantDTO = new MerchantDTO();
        merchantDTO.setId(userDTO.getId());
        merchantDTO.setUsername(userDTO.getUsername());
        merchantDTO.setMerchantName(StringUtils.isNotBlank(userDTO.getNickname()) ? userDTO.getNickname() : userDTO.getUsername());
        merchantDTO.setEmail(userDTO.getEmail());
        merchantDTO.setPhone(userDTO.getPhone());
        merchantDTO.setUserType(String.valueOf(userDTO.getUserType()));
        merchantDTO.setStatus(userDTO.getStatus());
        merchantDTO.setAuthStatus(0);

        boolean saved = merchantService.save(merchantConverter.toEntity(merchantDTO));
        if (!saved) {
            throw new BusinessException("failed to create merchant for user");
        }
    }

    private String buildUniqueGithubUsername(String login) {
        return com.cloud.common.utils.StringUtils.generateUniqueUsername(
                login,
                "github_",
                username -> findByUsername(username) != null
        );
    }
}