package com.cloud.user.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.common.annotation.DistributedLock;
import com.cloud.common.domain.dto.auth.AuthPrincipalDTO;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.domain.dto.user.UserPageDTO;
import com.cloud.common.domain.dto.user.UserProfileDTO;
import com.cloud.common.domain.dto.user.UserProfileUpsertDTO;
import com.cloud.common.domain.dto.user.UserUpsertRequestDTO;
import com.cloud.common.domain.vo.user.UserVO;
import com.cloud.common.exception.BusinessException;
import com.cloud.common.exception.EntityNotFoundException;
import com.cloud.common.result.PageResult;
import com.cloud.common.utils.PageUtils;
import com.cloud.user.converter.UserConverter;
import com.cloud.user.mapper.UserMapper;
import com.cloud.user.module.entity.User;
import com.cloud.user.service.UserService;
import com.cloud.user.service.support.AuthPrincipalRemoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final UserConverter userConverter;
    private final AuthPrincipalRemoteService authPrincipalRemoteService;

    @Override
    @Transactional(readOnly = true)
    public UserDTO findByUsername(String username) {
        if (StrUtil.isBlank(username)) {
            throw new BusinessException("username is required");
        }

        User user = getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        return user == null ? null : toDTOWithRoles(user);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<UserVO> pageQuery(UserPageDTO pageDTO) {
        Page<User> page = PageUtils.buildPage(pageDTO);

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(pageDTO.getUsername())) {
            queryWrapper.like(User::getUsername, pageDTO.getUsername());
        }
        if (StrUtil.isNotBlank(pageDTO.getEmail())) {
            queryWrapper.like(User::getEmail, pageDTO.getEmail());
        }
        if (StrUtil.isNotBlank(pageDTO.getPhone())) {
            queryWrapper.like(User::getPhone, pageDTO.getPhone());
        }
        if (StrUtil.isNotBlank(pageDTO.getNickname())) {
            queryWrapper.like(User::getNickname, pageDTO.getNickname());
        }
        if (pageDTO.getStatus() != null) {
            queryWrapper.eq(User::getStatus, pageDTO.getStatus());
        }
        if (StrUtil.isNotBlank(pageDTO.getRoleCode())) {
            Collection<Long> userIds = authPrincipalRemoteService.getUserIdsByRoleCode(pageDTO.getRoleCode());
            if (userIds.isEmpty()) {
                return PageResult.of(page.getCurrent(), page.getSize(), 0L, List.of());
            }
            queryWrapper.in(User::getId, userIds);
        }
        queryWrapper.orderByDesc(User::getCreatedAt);

        Page<User> resultPage = this.page(page, queryWrapper);
        List<UserVO> userVOList = toVOListWithRoles(resultPage.getRecords());
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
            @CacheEvict(cacheNames = "userList", allEntries = true),
            @CacheEvict(cacheNames = "auth", allEntries = true)
    })
    public boolean deleteUserById(Long id) {
        if (id == null) {
            throw new BusinessException("user id is required");
        }
        User user = getById(id);
        if (user == null) {
            throw new EntityNotFoundException("user", id);
        }
        boolean deleted = removeById(id);
        if (deleted) {
            authPrincipalRemoteService.deletePrincipal(id);
        }
        return deleted;
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
            @CacheEvict(cacheNames = "userList", allEntries = true),
            @CacheEvict(cacheNames = "auth", allEntries = true)
    })
    public boolean deleteUsersByIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            throw new BusinessException("user ids are required");
        }
        boolean deleted = removeByIds(userIds);
        if (deleted) {
            userIds.forEach(authPrincipalRemoteService::deletePrincipal);
        }
        return deleted;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        if (id == null) {
            throw new BusinessException("user id is required");
        }
        User user = getById(id);
        if (user == null) {
            throw new EntityNotFoundException("user", id);
        }
        return toDTOWithRoles(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileDTO getProfileById(Long id) {
        if (id == null) {
            throw new BusinessException("user id is required");
        }
        User user = getById(id);
        if (user == null) {
            throw new EntityNotFoundException("user", id);
        }
        return toProfileDTO(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO getUserByUsername(String username) {
        return findByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> getUsersByIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }
        return toDTOListWithRoles(listByIds(userIds));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDTO> getUsersPage(Integer page, Integer size) {
        Page<User> pageParam = new Page<>(page, size);
        Page<User> userPage = page(pageParam);

        Page<UserDTO> dtoPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        dtoPage.setRecords(toDTOListWithRoles(userPage.getRecords()));
        return dtoPage;
    }

    @Override
    @DistributedLock(
            key = "'user:create:' + #requestDTO.username",
            waitTime = 5,
            leaseTime = 15,
            failMessage = "failed to acquire user create lock"
    )
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(cacheNames = "user", allEntries = true),
            @CacheEvict(cacheNames = "userList", allEntries = true)
    })
    public Long createUser(UserUpsertRequestDTO requestDTO) {
        if (requestDTO == null) {
            throw new BusinessException("user payload is required");
        }
        if (StrUtil.isBlank(requestDTO.getPassword())) {
            throw new BusinessException("password is required");
        }
        authPrincipalRemoteService.assertUsernameAvailable(requestDTO.getUsername(), null);
        User user = toUserEntity(requestDTO);
        save(user);
        List<String> roles = requestDTO.getRoles() == null || requestDTO.getRoles().isEmpty()
                ? List.of("USER")
                : requestDTO.getRoles();
        authPrincipalRemoteService.createPrincipal(toCreatePrincipalDTO(requestDTO, user.getId(), roles));
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(cacheNames = "user", allEntries = true),
            @CacheEvict(cacheNames = "userList", allEntries = true)
    })
    public Long createProfile(UserProfileUpsertDTO profileUpsertDTO) {
        User user = toUserEntity(profileUpsertDTO);
        save(user);
        return user.getId();
    }

    @Override
    @DistributedLock(
            key = "'user:update:' + #id",
            waitTime = 5,
            leaseTime = 15,
            failMessage = "failed to acquire user update lock"
    )
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(cacheNames = "user", key = "#id"),
            @CacheEvict(cacheNames = "user", key = "'username:' + #requestDTO.username"),
            @CacheEvict(cacheNames = "userList", allEntries = true),
            @CacheEvict(cacheNames = "auth", allEntries = true)
    })
    public Boolean updateUser(Long id, UserUpsertRequestDTO requestDTO) {
        if (id == null || requestDTO == null) {
            throw new BusinessException("user id is required");
        }

        User existingUser = getById(id);
        if (existingUser == null) {
            throw new EntityNotFoundException("user", id);
        }

        if (StrUtil.isNotBlank(requestDTO.getUsername())
                && !StrUtil.equals(requestDTO.getUsername(), existingUser.getUsername())) {
            long count = lambdaQuery()
                    .eq(User::getUsername, requestDTO.getUsername())
                    .ne(User::getId, id)
                    .count();
            if (count > 0) {
                throw new BusinessException("username already exists");
            }
            authPrincipalRemoteService.assertUsernameAvailable(requestDTO.getUsername(), id);
        }

        User user = toUserEntity(requestDTO);
        user.setId(id);
        boolean updated = updateById(user);
        if (updated) {
            authPrincipalRemoteService.updatePrincipal(toAuthPrincipalDTO(id, requestDTO, existingUser));
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(cacheNames = "user", key = "#profileUpsertDTO.id"),
            @CacheEvict(cacheNames = "user", key = "'username:' + #profileUpsertDTO.username"),
            @CacheEvict(cacheNames = "userList", allEntries = true),
            @CacheEvict(cacheNames = "auth", allEntries = true)
    })
    public Boolean updateProfile(UserProfileUpsertDTO profileUpsertDTO) {
        if (profileUpsertDTO == null || profileUpsertDTO.getId() == null) {
            throw new BusinessException("user id is required");
        }
        User user = toUserEntity(profileUpsertDTO);
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
            @CacheEvict(cacheNames = "userList", allEntries = true),
            @CacheEvict(cacheNames = "auth", allEntries = true)
    })
    public Boolean deleteUser(Long id) {
        boolean deleted = removeById(id);
        if (deleted) {
            authPrincipalRemoteService.deletePrincipal(id);
        }
        return deleted;
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
        boolean updated = updateById(user);
        if (updated) {
            AuthPrincipalDTO authPrincipalDTO = new AuthPrincipalDTO();
            authPrincipalDTO.setId(id);
            authPrincipalDTO.setStatus(status);
            authPrincipalRemoteService.updatePrincipal(authPrincipalDTO);
        }
        return updated;
    }

    @Override
    @DistributedLock(
            key = "'user:password:reset:' + #id",
            waitTime = 5,
            leaseTime = 15,
            failMessage = "failed to acquire reset password lock"
    )
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(cacheNames = "user", key = "#id"),
            @CacheEvict(cacheNames = "auth", allEntries = true)
    })
    public String resetPassword(Long id) {
        if (getById(id) == null) {
            throw new EntityNotFoundException("user", id);
        }
        String newPassword = "Tmp#" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        AuthPrincipalDTO authPrincipalDTO = new AuthPrincipalDTO();
        authPrincipalDTO.setId(id);
        authPrincipalDTO.setPassword(newPassword);
        authPrincipalRemoteService.updatePrincipal(authPrincipalDTO);
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
    @Caching(evict = {
            @CacheEvict(cacheNames = "user", key = "#id"),
            @CacheEvict(cacheNames = "auth", allEntries = true)
    })
    public Boolean changePassword(Long id, String oldPassword, String newPassword) {
        if (getById(id) == null) {
            throw new EntityNotFoundException("user", id);
        }
        if (!authPrincipalRemoteService.changePassword(id, oldPassword, newPassword)) {
            throw new BusinessException("old password mismatch");
        }
        return true;
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
        boolean updated = update(updateEntity, wrapper);
        if (updated) {
            userIds.forEach(userId -> {
                AuthPrincipalDTO authPrincipalDTO = new AuthPrincipalDTO();
                authPrincipalDTO.setId(userId);
                authPrincipalDTO.setStatus(status);
                authPrincipalRemoteService.updatePrincipal(authPrincipalDTO);
            });
        }
        return updated ? userIds.size() : 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(cacheNames = "user", allEntries = true),
            @CacheEvict(cacheNames = "userList", allEntries = true),
            @CacheEvict(cacheNames = "auth", allEntries = true)
    })
    public boolean updateBatchById(Collection<User> entityList) {
        if (entityList == null || entityList.isEmpty()) {
            return false;
        }
        boolean updated = super.updateBatchById(entityList);
        if (updated) {
            entityList.stream()
                    .filter(entity -> entity != null && entity.getId() != null)
                    .forEach(entity -> {
                        AuthPrincipalDTO authPrincipalDTO = new AuthPrincipalDTO();
                        authPrincipalDTO.setId(entity.getId());
                        authPrincipalDTO.setUsername(entity.getUsername());
                        authPrincipalDTO.setStatus(entity.getStatus());
                        authPrincipalRemoteService.updatePrincipal(authPrincipalDTO);
                    });
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(cacheNames = "user", key = "#entity.id"),
            @CacheEvict(cacheNames = "userList", allEntries = true)
    })
    public boolean save(User entity) {
        return super.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "user", key = "#entity.id"),
                    @CacheEvict(cacheNames = "user", key = "'username:' + #entity.username", condition = "#entity.username != null"),
                    @CacheEvict(cacheNames = "userList", allEntries = true),
                    @CacheEvict(cacheNames = "auth", allEntries = true)
            }
    )
    public boolean updateById(User entity) {
        return super.updateById(entity);
    }

    private UserDTO toDTOWithRoles(User user) {
        return toDTOWithRoles(user, null);
    }

    private UserDTO toDTOWithRoles(User user, List<String> roles) {
        if (user == null) {
            return null;
        }
        UserDTO dto = userConverter.toDTO(user);
        dto.setRoles(roles == null ? authPrincipalRemoteService.getRoleCodesByUserId(user.getId()) : roles);
        return dto;
    }

    private List<UserDTO> toDTOListWithRoles(List<User> users) {
        if (users == null || users.isEmpty()) {
            return List.of();
        }
        List<UserDTO> dtos = userConverter.toDTOList(users);
        Map<Long, List<String>> roleMap = authPrincipalRemoteService.getRoleCodesByUserIds(
                users.stream().map(User::getId).toList());
        dtos.forEach(dto -> dto.setRoles(roleMap.getOrDefault(dto.getId(), List.of())));
        return dtos;
    }

    private List<UserVO> toVOListWithRoles(List<User> users) {
        if (users == null || users.isEmpty()) {
            return List.of();
        }
        List<UserVO> vos = userConverter.toVOList(users);
        Map<Long, List<String>> roleMap = authPrincipalRemoteService.getRoleCodesByUserIds(
                users.stream().map(User::getId).toList());
        vos.forEach(vo -> vo.setRoles(roleMap.getOrDefault(vo.getId(), List.of())));
        return vos;
    }

    private User toUserEntity(UserUpsertRequestDTO requestDTO) {
        User user = new User();
        user.setUsername(requestDTO.getUsername());
        user.setPhone(requestDTO.getPhone());
        user.setNickname(requestDTO.getNickname());
        user.setAvatarUrl(requestDTO.getAvatarUrl());
        user.setEmail(requestDTO.getEmail());
        user.setStatus(requestDTO.getStatus());
        return user;
    }

    private User toUserEntity(UserProfileUpsertDTO profileUpsertDTO) {
        User user = new User();
        user.setId(profileUpsertDTO.getId());
        user.setUsername(profileUpsertDTO.getUsername());
        user.setPhone(profileUpsertDTO.getPhone());
        user.setNickname(profileUpsertDTO.getNickname());
        user.setAvatarUrl(profileUpsertDTO.getAvatarUrl());
        user.setEmail(profileUpsertDTO.getEmail());
        user.setStatus(profileUpsertDTO.getStatus());
        return user;
    }

    private UserProfileDTO toProfileDTO(User user) {
        if (user == null) {
            return null;
        }
        UserProfileDTO profile = new UserProfileDTO();
        profile.setId(user.getId());
        profile.setUsername(user.getUsername());
        profile.setPhone(user.getPhone());
        profile.setNickname(user.getNickname());
        profile.setAvatarUrl(user.getAvatarUrl());
        profile.setEmail(user.getEmail());
        profile.setStatus(user.getStatus());
        return profile;
    }

    private AuthPrincipalDTO toCreatePrincipalDTO(UserUpsertRequestDTO requestDTO, Long userId, List<String> roles) {
        AuthPrincipalDTO authPrincipalDTO = new AuthPrincipalDTO();
        authPrincipalDTO.setId(userId);
        authPrincipalDTO.setUsername(requestDTO.getUsername());
        authPrincipalDTO.setPassword(requestDTO.getPassword());
        authPrincipalDTO.setNickname(requestDTO.getNickname());
        authPrincipalDTO.setEmail(requestDTO.getEmail());
        authPrincipalDTO.setPhone(requestDTO.getPhone());
        authPrincipalDTO.setStatus(requestDTO.getStatus());
        authPrincipalDTO.setRoles(roles);
        return authPrincipalDTO;
    }

    private AuthPrincipalDTO toAuthPrincipalDTO(Long userId, UserUpsertRequestDTO requestDTO, User existingUser) {
        AuthPrincipalDTO authPrincipalDTO = new AuthPrincipalDTO();
        authPrincipalDTO.setId(userId);
        authPrincipalDTO.setUsername(StrUtil.blankToDefault(requestDTO.getUsername(), existingUser.getUsername()));
        authPrincipalDTO.setPassword(requestDTO.getPassword());
        authPrincipalDTO.setNickname(StrUtil.blankToDefault(requestDTO.getNickname(), existingUser.getNickname()));
        authPrincipalDTO.setEmail(requestDTO.getEmail() == null ? existingUser.getEmail() : requestDTO.getEmail());
        authPrincipalDTO.setPhone(requestDTO.getPhone() == null ? existingUser.getPhone() : requestDTO.getPhone());
        authPrincipalDTO.setStatus(requestDTO.getStatus() == null ? existingUser.getStatus() : requestDTO.getStatus());
        authPrincipalDTO.setRoles(requestDTO.getRoles());
        return authPrincipalDTO;
    }
}

