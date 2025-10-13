package com.cloud.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.common.annotation.DistributedLock;
import com.cloud.common.domain.dto.auth.RegisterRequestDTO;
import com.cloud.common.domain.dto.user.MerchantDTO;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.domain.dto.user.UserPageDTO;
import com.cloud.common.domain.vo.user.UserVO;
import com.cloud.common.exception.BusinessException;
import com.cloud.common.exception.EntityNotFoundException;
import com.cloud.common.result.PageResult;
import com.cloud.common.utils.PageUtils;
import com.cloud.user.converter.MerchantConverter;
import com.cloud.user.converter.UserConverter;
import com.cloud.user.exception.UserServiceException;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * @author what's up
 * @description 针对表【users(用户表)】的数据库操作Service实现
 * @createDate 2025-09-06 19:31:12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {
    private final UserConverter userConverter;
    private final PasswordEncoder passwordEncoder;
    private final MerchantService merchantService;
    private final MerchantConverter merchantConverter;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = "user",  // 使用配置的user缓存(30分钟TTL)
            key = "'username:' + #username",
            unless = "#result == null"
    )
    public UserDTO findByUsername(String username) {
        if (StringUtils.isBlank(username)) {
            log.warn("用户名不能为空");
            throw new BusinessException("用户名不能为空");
        }

        log.info("开始调用用户服务, 获取用户信息, username: {}", username);

        // 使用Lambda表达式查询，避免SQL注入风险
        User user = getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));

        return user != null ? userConverter.toDTO(user) : null;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("@permissionManager.hasAdminAccess(authentication)")
    public PageResult<UserVO> pageQuery(UserPageDTO pageDTO) {
        try {
            log.info("分页查询用户，查询条件：{}", pageDTO);

            // 1. 构造分页对象
            Page<User> page = PageUtils.buildPage(pageDTO);

            // 2. 构造查询条件
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

            // 3. 执行分页查询
            Page<User> resultPage = this.page(page, queryWrapper);

            // 4. 转换实体列表为VO列表
            List<UserVO> userVOList = userConverter.toVOList(resultPage.getRecords());

            // 5. 封装分页结果
            PageResult<UserVO> pageResult = PageResult.of(
                    resultPage.getCurrent(),
                    resultPage.getSize(),
                    resultPage.getTotal(),
                    userVOList
            );

            log.info("分页查询完成，总记录数：{}，当前页：{}，每页大小：{}",
                    pageResult.getTotal(), pageResult.getCurrent(), pageResult.getSize());

            return pageResult;
        } catch (Exception e) {
            log.error("分页查询用户时发生异常，查询条件：{}", pageDTO, e);
            throw new BusinessException("分页查询用户失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @PreAuthorize("@permissionManager.hasAdminAccess(authentication)")
    @CacheEvict(
            cacheNames = "user",
            key = "#id"
    )
    public boolean deleteUserById(Long id) {
        if (id == null) {
            log.warn("用户ID不能为空");
            throw new BusinessException("用户ID不能为空");
        }

        try {
            log.info("开始逻辑删除用户, id: {}", id);

            // 检查用户是否存在
            User user = getById(id);
            if (user == null) {
                log.warn("要删除的用户不存在, id: {}", id);
                throw new EntityNotFoundException("用户", id);
            }

            // 使用MyBatis-Plus的逻辑删除
            boolean result = removeById(id);

            log.info("用户逻辑删除完成, id: {}, result: {}", id, result);
            return result;
        } catch (UserServiceException e) {
            throw e; // 重新抛出业务异常
        } catch (Exception e) {
            log.error("删除用户时发生系统异常，用户ID：{}", id, e);
            throw new BusinessException("删除用户失败", e);
        }
    }

    @Override
    @PreAuthorize("@permissionManager.hasAdminAccess(authentication)")
    @DistributedLock(
            key = "'user:batch:delete:' + T(String).join(',', #userIds)",
            waitTime = 10,
            leaseTime = 30,
            failMessage = "批量删除用户操作获取锁失败"
    )
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(
            cacheNames = "user",
            allEntries = true, // 批量删除时清空整个缓存，简单粗暴但有效
            condition = "#userIds != null && !#userIds.isEmpty()"
    )
    public boolean deleteUsersByIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            log.warn("用户ID集合不能为空");
            throw new BusinessException("用户ID集合不能为空");
        }

        try {
            log.info("开始批量逻辑删除用户, 用户数量: {}, 用户IDs: {}", userIds.size(), userIds);

            // 使用MyBatis-Plus的批量逻辑删除
            boolean result = removeByIds(userIds);
            log.info("批量用户逻辑删除完成, 删除数量: {}, result: {}", userIds.size(), result);
            return result;
        } catch (Exception e) {
            log.error("批量删除用户时发生异常, 用户IDs: {}", userIds, e);
            throw new BusinessException("批量删除用户失败", e);
        }
    }

    @Override
    @Transactional(readOnly = true) // 只读事务
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @Cacheable(
            cacheNames = "userInfo",  // 使用配置的userInfo缓存(30分钟TTL)
            key = "#id",
            unless = "#result == null"
    )
    public UserDTO getUserById(Long id) {
        if (id == null) {
            log.warn("用户ID不能为空");
            throw new BusinessException("用户ID不能为空");
        }

        try {
            log.info("根据ID查找用户: {}", id);
            User user = getById(id); // 使用MyBatis-Plus方法
            if (user == null) {
                throw new EntityNotFoundException("用户", id);
            }
            return userConverter.toDTO(user);
        } catch (UserServiceException e) {
            log.warn("根据ID查找用户失败，用户不存在: {}", id);
            throw e; // 重新抛出业务异常
        } catch (Exception e) {
            log.error("根据ID查找用户时发生系统异常，用户ID: {}", id, e);
            throw new BusinessException("获取用户信息失败", e);
        }
    }

    @Override
    @Transactional(readOnly = true) // 只读事务
    @Cacheable(
            cacheNames = "user",
            key = "'username:' + #username", // 30分钟
            unless = "#result == null"
    )
    public UserDTO getUserByUsername(String username) {

        return findByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("@permissionManager.hasAdminAccess(authentication)")
    @Cacheable(
            cacheNames = "user",
            key = "'batch:' + #userIds.toString()", // 15分钟，批量查询缓存时间短一些
            condition = "#userIds != null && #userIds.size() <= 100", // 只对小批量查询启用缓存
            unless = "#result == null || #result.isEmpty()"
    )
    public List<UserDTO> getUsersByIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        List<User> users = listByIds(userIds);
        return userConverter.toDTOList(users);
    }

    /**
     * 保存用户信息
     *
     * @param entity 用户实体
     * @return 保存结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CachePut(
            cacheNames = "user",
            key = "#entity.id"
    )
    public boolean save(User entity) {
        log.info("保存用户信息, username: {}", entity.getUsername());
        return super.save(entity);
    }

    /**
     * 更新用户信息
     *
     * @param entity 用户实体
     * @return 更新结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "user", key = "#entity.id"),
                    @CacheEvict(cacheNames = "user", key = "'username:' + #entity.username", condition = "#entity.username != null")
            },
            put = {
                    @CachePut(cacheNames = "user", key = "#entity.id")
            }
    )
    public boolean updateById(User entity) {
        log.info("更新用户信息, userId: {}", entity.getId());

        boolean result = super.updateById(entity);

        return result;
    }

    @Override
    @DistributedLock(
            key = "'user:register:' + #registerRequest.username",
            waitTime = 3,
            leaseTime = 15,
            failMessage = "用户注册操作获取锁失败，请稍后重试"
    )
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(
            cacheNames = "user",
            key = "'username:' + #registerRequest.username",
            beforeInvocation = true
    )
    public UserDTO registerUser(RegisterRequestDTO registerRequest) {
        log.info("🚀 开始用户注册流程, username: {}, userType: {}",
                registerRequest.getUsername(), registerRequest.getUserType());

        try {
            // 1. 检查用户是否已存在
            UserDTO existingUser = findByUsername(registerRequest.getUsername());
            if (existingUser != null) {
                log.warn("⚠️ 用户注册失败，用户名已存在: {}", registerRequest.getUsername());
                throw new BusinessException("用户名已存在: " + registerRequest.getUsername());
            }

            // 2. 转换并准备用户实体
            User user = prepareUserEntity(registerRequest);
            log.debug("✅ 用户实体准备完成: username={}, userType={}",
                    user.getUsername(), user.getUserType());

            // 3. 保存用户（使用缓存注解的save方法）
            boolean saved = save(user);

            if (!saved) {
                log.error("❌ 用户注册失败，数据保存失败: {}", registerRequest.getUsername());
                throw new BusinessException("用户注册失败");
            }

            // 4. 重新查询用户以获取完整信息
            UserDTO userDTO = findByUsername(registerRequest.getUsername());
            if (userDTO == null) {
                log.error("❌ 用户注册后查询失败: {}", registerRequest.getUsername());
                throw new BusinessException("用户注册失败，无法获取用户信息");
            }

            // 5. 处理商家用户的特殊逻辑
            if ("MERCHANT".equals(registerRequest.getUserType())) {
                handleMerchantUserRegistration(userDTO);
            }

            log.info("🎉 用户注册成功: username={}, userId={}, userType={}",
                    userDTO.getUsername(), userDTO.getId(), userDTO.getUserType());

            return userDTO;


        } catch (BusinessException e) {
            throw e; // 重新抛出业务异常
        } catch (Exception e) {
            log.error("💥 用户注册过程中发生未预期异常, username: {}",
                    registerRequest.getUsername(), e);
            throw new BusinessException("用户注册失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = "auth",
            key = "'password:' + #username", // 5分钟缓存
            unless = "#result == null"
    )
    public String getUserPassword(String username) {
        log.debug("获取用户密码: {}", username);

        try {
            User user = getOne(new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, username.trim())
                    .select(User::getUsername, User::getPassword, User::getStatus)
            );

            if (user == null) {
                log.warn("用户不存在: {}", username);
                return null;
            }

            if (user.getStatus() == null || user.getStatus() != 1) {
                log.warn("用户账户已禁用: {}", username);
                return null;
            }

            return user.getPassword();

        } catch (Exception e) {
            log.error("获取用户密码异常: {}", username, e);
            return null;
        }
    }

    /**
     * 准备用户实体对象
     */
    private User prepareUserEntity(RegisterRequestDTO registerRequest) {
        // 使用converter转换
        User user = userConverter.toEntity(registerRequest);

        // 设置加密密码
        String rawPassword = registerRequest.getPassword();
        if (StringUtils.isNotBlank(rawPassword)) {
            String encodedPassword = passwordEncoder.encode(rawPassword.trim());
            user.setPassword(encodedPassword);
            log.debug("🔐 密码已加密, username: {}", registerRequest.getUsername());
        } else {
            // 如果没有提供密码，设置默认密码
            String encodedPassword = passwordEncoder.encode("123456");
            user.setPassword(encodedPassword);
            log.debug("🔐 使用默认密码, username: {}", registerRequest.getUsername());
        }

        // 设置默认值
        if (user.getStatus() == null) {
            user.setStatus(1); // 默认启用
        }
        if (StringUtils.isBlank(user.getUserType())) {
            user.setUserType("USER"); // 默认用户类型
        }

        return user;
    }

    /**
     * 处理商家用户注册的特殊逻辑
     */
    private void handleMerchantUserRegistration(UserDTO userDTO) {
        try {
            log.info("🏪 开始创建商家记录, username: {}", userDTO.getUsername());

            MerchantDTO merchantDTO = new MerchantDTO();
            merchantDTO.setId(userDTO.getId()); // 使用用户ID作为商家ID
            merchantDTO.setUsername(userDTO.getUsername());
            merchantDTO.setMerchantName(StringUtils.isNotBlank(userDTO.getNickname()) ?
                    userDTO.getNickname() : userDTO.getUsername());
            merchantDTO.setEmail(userDTO.getEmail());
            merchantDTO.setPhone(userDTO.getPhone());
            merchantDTO.setUserType(String.valueOf(userDTO.getUserType()));
            merchantDTO.setStatus(userDTO.getStatus());
            merchantDTO.setAuthStatus(0); // 默认为待审核状态

            // 调用商家服务创建商家记录
            boolean merchantSaved = merchantService.save(merchantConverter.toEntity(merchantDTO));
            if (merchantSaved) {
                log.info("✅ 成功为用户 {} 创建商家记录", userDTO.getUsername());
            } else {
                log.warn("⚠️ 为用户 {} 创建商家记录失败", userDTO.getUsername());
                // 这里可以考虑回滚整个用户注册事务，或者记录失败日志供后续处理
                throw new BusinessException("创建商家记录失败");
            }

        } catch (Exception e) {
            log.error("❌ 为用户 {} 创建商家记录时发生异常", userDTO.getUsername(), e);
            // 在事务中抛出异常，触发回滚
            throw new BusinessException("创建商家记录失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = "user",
            key = "'github_id:' + #githubId", // 30分钟
            unless = "#result == null"
    )
    public UserDTO findByGitHubId(Long githubId) {
        if (githubId == null) {
            log.warn("GitHub用户ID不能为空");
            return null;
        }

        log.debug("根据GitHub ID查找用户: {}", githubId);

        try {
            User user = getOne(new LambdaQueryWrapper<User>()
                    .eq(User::getGithubId, githubId)
                    .eq(User::getOauthProvider, "github"));

            return user != null ? userConverter.toDTO(user) : null;
        } catch (Exception e) {
            log.error("根据GitHub ID查找用户时发生异常，GitHub ID: {}", githubId, e);
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = "user",
            key = "'github_username:' + #githubUsername", // 30分钟
            unless = "#result == null"
    )
    public UserDTO findByGitHubUsername(String githubUsername) {
        if (StringUtils.isBlank(githubUsername)) {
            log.warn("GitHub用户名不能为空");
            return null;
        }

        log.debug("根据GitHub用户名查找用户: {}", githubUsername);

        try {
            User user = getOne(new LambdaQueryWrapper<User>()
                    .eq(User::getGithubUsername, githubUsername)
                    .eq(User::getOauthProvider, "github"));

            return user != null ? userConverter.toDTO(user) : null;
        } catch (Exception e) {
            log.error("根据GitHub用户名查找用户时发生异常，GitHub用户名: {}", githubUsername, e);
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = "user",
            key = "'oauth:' + #oauthProvider + ':' + #oauthProviderId", // 30分钟
            unless = "#result == null"
    )
    public UserDTO findByOAuthProvider(String oauthProvider, String oauthProviderId) {
        if (StringUtils.isBlank(oauthProvider) || StringUtils.isBlank(oauthProviderId)) {
            log.warn("OAuth提供商和提供商ID不能为空");
            return null;
        }

        log.debug("根据OAuth提供商信息查找用户: provider={}, providerId={}", oauthProvider, oauthProviderId);

        try {
            User user = getOne(new LambdaQueryWrapper<User>()
                    .eq(User::getOauthProvider, oauthProvider)
                    .eq(User::getOauthProviderId, oauthProviderId));

            return user != null ? userConverter.toDTO(user) : null;
        } catch (Exception e) {
            log.error("根据OAuth提供商信息查找用户时发生异常，provider: {}, providerId: {}",
                    oauthProvider, oauthProviderId, e);
            return null;
        }
    }

    @Override
    @DistributedLock(
            key = "'user:github:create:' + #githubUserDTO.githubId",
            waitTime = 3,
            leaseTime = 15,
            failMessage = "GitHub用户创建操作获取锁失败"
    )
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(
            cacheNames = "user",
            key = "'github_id:' + #githubUserDTO.githubId",
            beforeInvocation = true
    )
    public UserDTO createGitHubUser(com.cloud.common.domain.dto.oauth.GitHubUserDTO githubUserDTO) {
        log.info("🚀 开始创建GitHub OAuth用户, githubId: {}, login: {}",
                githubUserDTO.getGithubId(), githubUserDTO.getLogin());

        try {
            // 1. 检查GitHub用户是否已存在
            UserDTO existingUser = findByGitHubId(githubUserDTO.getGithubId());
            if (existingUser != null) {
                log.warn("⚠️ GitHub用户已存在: githubId={}, 系统用户ID={}",
                        githubUserDTO.getGithubId(), existingUser.getId());
                return existingUser;
            }

            // 2. 检查系统用户名是否冲突
            String systemUsername = githubUserDTO.buildSystemUsername();
            UserDTO userWithSameUsername = findByUsername(systemUsername);
            if (userWithSameUsername != null) {
                log.warn("⚠️ 系统用户名已存在，需要生成唯一用户名: {}", systemUsername);
                systemUsername = generateUniqueUsername(githubUserDTO.getLogin());
            }

            // 3. 创建用户实体
            User user = buildGitHubUser(githubUserDTO, systemUsername);

            // 4. 保存用户
            boolean saved = save(user);
            if (!saved) {
                log.error("❌ GitHub用户创建失败，数据保存失败: githubId={}", githubUserDTO.getGithubId());
                throw new BusinessException("GitHub用户创建失败");
            }

            // 5. 查询完整的用户信息
            UserDTO userDTO = findByUsername(systemUsername);
            if (userDTO == null) {
                log.error("❌ GitHub用户创建后查询失败: username={}", systemUsername);
                throw new BusinessException("GitHub用户创建失败，无法获取用户信息");
            }

            log.info("🎉 GitHub OAuth用户创建成功: username={}, userId={}, githubId={}",
                    userDTO.getUsername(), userDTO.getId(), githubUserDTO.getGithubId());

            return userDTO;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("💥 创建GitHub OAuth用户过程中发生未预期异常, githubId: {}",
                    githubUserDTO.getGithubId(), e);
            throw new BusinessException("GitHub用户创建失败: " + e.getMessage(), e);
        }
    }

    @Override
    @DistributedLock(
            key = "'user:github:update:' + #userId",
            waitTime = 3,
            leaseTime = 10,
            failMessage = "GitHub用户信息更新操作获取锁失败"
    )
    @Transactional(rollbackFor = Exception.class)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "user", key = "#userId"),
                    @CacheEvict(cacheNames = "user", key = "'github_id:' + #githubUserDTO.githubId")
            }
    )
    public boolean updateGitHubUserInfo(Long userId, com.cloud.common.domain.dto.oauth.GitHubUserDTO githubUserDTO) {
        log.info("🔄 开始更新GitHub用户信息, userId: {}, githubId: {}",
                userId, githubUserDTO.getGithubId());

        try {
            // 1. 检查用户是否存在
            User existingUser = getById(userId);
            if (existingUser == null) {
                log.warn("⚠️ 要更新的用户不存在: userId={}", userId);
                throw new EntityNotFoundException("用户", userId);
            }

            // 2. 更新GitHub相关信息
            User updatedUser = new User();
            updatedUser.setId(userId);
            updatedUser.setNickname(githubUserDTO.getDisplayName());
            updatedUser.setEmail(githubUserDTO.getEmail());
            updatedUser.setAvatarUrl(githubUserDTO.getAvatarUrl());
            updatedUser.setGithubId(githubUserDTO.getGithubId());
            updatedUser.setGithubUsername(githubUserDTO.getLogin());
            updatedUser.setOauthProvider("github");
            updatedUser.setOauthProviderId(githubUserDTO.getGithubId().toString());

            // 3. 执行更新
            boolean result = updateById(updatedUser);

            if (result) {
                log.info("✅ GitHub用户信息更新成功: userId={}, githubId={}",
                        userId, githubUserDTO.getGithubId());
            } else {
                log.error("❌ GitHub用户信息更新失败: userId={}, githubId={}",
                        userId, githubUserDTO.getGithubId());
            }

            return result;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("💥 更新GitHub用户信息时发生未预期异常, userId: {}, githubId: {}",
                    userId, githubUserDTO.getGithubId(), e);
            throw new BusinessException("更新GitHub用户信息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建GitHub用户实体
     */
    private User buildGitHubUser(com.cloud.common.domain.dto.oauth.GitHubUserDTO githubUserDTO, String systemUsername) {
        User user = new User();
        user.setUsername(systemUsername);
        user.setNickname(githubUserDTO.getDisplayName());
        user.setEmail(githubUserDTO.getEmail());
        user.setAvatarUrl(githubUserDTO.getAvatarUrl());
        user.setUserType("USER"); // GitHub用户默认为普通用户
        user.setStatus(1); // 默认启用
        user.setPhone("000-0000-0000"); // GitHub用户默认手机号

        // GitHub OAuth相关信息
        user.setGithubId(githubUserDTO.getGithubId());
        user.setGithubUsername(githubUserDTO.getLogin());
        user.setOauthProvider("github");
        user.setOauthProviderId(githubUserDTO.getGithubId().toString());

        // OAuth用户使用特殊密码（不用于登录）
        String oauthPassword = "github_oauth2_" + githubUserDTO.getGithubId();
        user.setPassword(passwordEncoder.encode(oauthPassword));

        return user;
    }

    /**
     * 生成唯一的用户名（处理用户名冲突）
     */
    private String generateUniqueUsername(String baseUsername) {
        String result = com.cloud.common.utils.StringUtils.generateUniqueUsername(
                baseUsername,
                "github_",
                username -> findByUsername(username) != null
        );

        log.info("生成唯一用户名: github_{} -> {}", baseUsername, result);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public com.baomidou.mybatisplus.extension.plugins.pagination.Page<UserDTO> getUsersPage(Integer page, Integer size) {
        log.info("分页获取用户列表, page: {}, size: {}", page, size);

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<User> pageParam =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<User> userPage = page(pageParam);

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<UserDTO> dtoPage =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                        userPage.getCurrent(),
                        userPage.getSize(),
                        userPage.getTotal()
                );

        List<UserDTO> dtoList = userPage.getRecords().stream()
                .map(userConverter::toDTO)
                .collect(java.util.stream.Collectors.toList());
        dtoPage.setRecords(dtoList);

        return dtoPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "user", allEntries = true)
            }
    )
    public Long createUser(UserDTO userDTO) {
        log.info("创建用户, username: {}", userDTO.getUsername());

        User user = userConverter.toEntity(userDTO);
        if (org.springframework.util.StringUtils.hasText(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        save(user);
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "user", key = "#userDTO.id"),
                    @CacheEvict(cacheNames = "user", key = "'username:' + #userDTO.username")
            }
    )
    public Boolean updateUser(UserDTO userDTO) {
        log.info("更新用户, userId: {}", userDTO.getId());

        User user = userConverter.toEntity(userDTO);
        return updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "user", key = "#id"),
                    @CacheEvict(cacheNames = "user", allEntries = true)
            }
    )
    public Boolean deleteUser(Long id) {
        log.info("删除用户, userId: {}", id);
        return removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = "user", key = "#id")
    public Boolean updateUserStatus(Long id, Integer status) {
        log.info("更新用户状态, userId: {}, status: {}", id, status);

        User user = new User();
        user.setId(id);
        user.setStatus(status);
        return updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = "user", key = "#id")
    public String resetPassword(Long id) {
        log.info("重置用户密码, userId: {}", id);

        String newPassword = "123456"; // 默认密码
        User user = new User();
        user.setId(id);
        user.setPassword(passwordEncoder.encode(newPassword));
        updateById(user);

        return newPassword;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = "user", key = "#id")
    public Boolean changePassword(Long id, String oldPassword, String newPassword) {
        log.info("修改用户密码, userId: {}", id);

        User user = getById(id);
        if (user == null) {
            throw new EntityNotFoundException("用户", id);
        }

        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("旧密码错误");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        return updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = "user", allEntries = true)
    public Integer batchUpdateUserStatus(Collection<Long> userIds, Integer status) {
        if (userIds == null || userIds.isEmpty()) {
            log.warn("批量更新用户状态失败，用户ID集合为空");
            throw new BusinessException("用户ID集合不能为空");
        }

        if (status == null) {
            log.warn("批量更新用户状态失败，状态值为空");
            throw new BusinessException("状态值不能为空");
        }

        log.info("开始批量更新用户状态，用户数量: {}, 状态值: {}", userIds.size(), status);

        try {
            // 使用 MyBatis Plus 的 lambdaUpdate 批量更新
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(User::getId, userIds);

            User updateEntity = new User();
            updateEntity.setStatus(status);

            boolean result = update(updateEntity, wrapper);

            if (result) {
                log.info("批量更新用户状态成功，用户数量: {}", userIds.size());
                return userIds.size();
            } else {
                log.warn("批量更新用户状态失败");
                return 0;
            }
        } catch (Exception e) {
            log.error("批量更新用户状态时发生异常，用户IDs: {}", userIds, e);
            throw new BusinessException("批量更新用户状态失败: " + e.getMessage(), e);
        }
    }

}
