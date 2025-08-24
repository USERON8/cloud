package com.cloud.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.common.domain.dto.auth.RegisterRequestDTO;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.exception.BusinessException;
import com.cloud.common.exception.ResourceNotFoundException;
import com.cloud.common.utils.CollectionUtils;
import com.cloud.user.converter.UserConverter;
import com.cloud.user.mapper.UserMapper;
import com.cloud.user.module.entity.User;
import com.cloud.user.service.UserLogMessageService;
import com.cloud.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author what's up
 * @description 针对表【users(用户表)】的数据库操作Service实现
 * @createDate 2025-08-20 12:35:31
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {
    private final UserConverter userConverter;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserLogMessageService userLogMessageService;


    // 用户信息缓存前缀
    private static final String USER_CACHE_PREFIX = "user:info:";
    // 用户权限缓存前缀
    private static final String USER_PERMISSIONS_CACHE_PREFIX = "user:permissions:";
    // 用户会话缓存前缀
    private static final String USER_SESSION_CACHE_PREFIX = "user:session:";

    /**
     * 根据用户ID获取用户信息
     * 该方法首先从Redis缓存中查找用户信息，如果缓存未命中，则从数据库中查询，
     * 并将查询结果存入缓存，缓存有效期为30分钟
     *
     * @param id 用户ID
     * @return 用户信息DTO，如果用户不存在则返回null
     */
    @Override
    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        try {
            // 参数验证
            if (id == null || id <= 0) {
                log.warn("获取用户信息失败：用户ID无效, userId: {}", id);
                throw new BusinessException(400, "用户ID无效");
            }

            // 先从缓存中获取
            String cacheKey = USER_CACHE_PREFIX + id;
            UserDTO cachedUser = (UserDTO) redisTemplate.opsForValue().get(cacheKey);
            if (cachedUser != null) {
                log.info("从缓存中获取用户信息, userId: {}", id);
                return cachedUser;
            }

            // 缓存未命中，查询数据库
            User user = this.getById(id);
            if (user == null) {
                log.warn("获取用户信息失败：用户不存在, userId: {}", id);
                throw new ResourceNotFoundException("user", String.valueOf(id));
            }

            UserDTO userDTO = userConverter.toDTO(user);

            // 将结果存入缓存
            redisTemplate.opsForValue().set(cacheKey, userDTO, 30, TimeUnit.MINUTES);
            log.info("将用户信息存入缓存, userId: {}", id);

            return userDTO;
        } catch (BusinessException e) {
            // 直接抛出已知的BusinessException
            throw e;
        } catch (Exception e) {
            log.error("获取用户信息时发生异常, userId: {}", id, e);
            throw new BusinessException(500, "获取用户信息失败: " + e.getMessage());
        }
    }

    /**
     * 分页查询用户信息
     * 支持根据用户名进行模糊查询，并对查询结果进行分页处理
     *
     * @param page     页码，从1开始
     * @param size     每页大小，有效范围为1-100
     * @param username 用户名，支持模糊查询
     * @return 分页用户信息DTO结果
     * @throws BusinessException 当分页参数无效或其他服务异常时抛出
     */
    @Override
    public IPage<UserDTO> getUsersWithPagination(int page, int size, String username) {
        try {
            // 参数验证
            if (page < 1) {
                log.warn("分页查询用户信息失败：页码不能小于1, page: {}", page);
                throw new BusinessException(400, "页码不能小于1");
            }
            if (size < 1 || size > 100) {
                log.warn("分页查询用户信息失败：每页大小必须在1-100之间, size: {}", size);
                throw new BusinessException(400, "每页大小必须在1-100之间");
            }

            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.like(username != null && !username.isEmpty(), "username", username);

            Page<User> pageResult = new Page<>(page, size);
            IPage<User> result = this.page(pageResult, queryWrapper);

            List<UserDTO> userDTOList = CollectionUtils.map(result.getRecords(), userConverter::toDTO);

            Page<UserDTO> userDTOPage = new Page<>(page, size, result.getTotal());
            userDTOPage.setRecords(userDTOList);

            log.info("分页查询用户信息成功, 页码: {}, 每页数量: {}, 查询条件: {}, 总数: {}",
                    page, size, username != null ? "已设置" : "未设置", userDTOPage.getTotal());

            return userDTOPage;
        } catch (BusinessException e) {
            // 直接抛出已知的BusinessException
            throw e;
        } catch (Exception e) {
            log.error("分页查询用户信息失败, 页码: {}, 每页数量: {}, 查询条件: {}", page, size, username, e);
            throw new BusinessException(500, "分页查询用户信息失败: " + e.getMessage());
        }
    }

    /**
     * 用户注册
     * 实现用户注册功能，包括参数验证、用户名唯一性检查、密码加密和数据保存
     *
     * @param registerRequestDTO 用户注册信息
     * @return 注册成功返回true，失败返回false
     */
    @Override
    public boolean register(RegisterRequestDTO registerRequestDTO) {
        try {
            // 参数验证
            if (registerRequestDTO == null) {
                log.warn("注册失败：注册请求参数为空");
                throw new BusinessException(400, "注册请求参数为空");
            }

            if (registerRequestDTO.getUsername() == null || registerRequestDTO.getUsername().isEmpty()) {
                log.warn("注册失败：用户名不能为空");
                throw new BusinessException(400, "用户名不能为空");
            }

            if (registerRequestDTO.getPassword() == null || registerRequestDTO.getPassword().isEmpty()) {
                log.warn("注册失败：密码不能为空");
                throw new BusinessException(400, "密码不能为空");
            }

            // 检查用户名是否已存在
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("username", registerRequestDTO.getUsername());
            User existingUser = this.getOne(queryWrapper);
            if (existingUser != null) {
                log.warn("注册失败：用户名已存在, username: {}", registerRequestDTO.getUsername());
                throw new BusinessException(409, "用户名已存在: " + registerRequestDTO.getUsername());
            }

            // 转换DTO为实体
            User user = userConverter.toEntity(registerRequestDTO);
            // 如果转换失败，手动创建用户对象
            if (user == null) {
                user = new User();
                // 手动设置必要字段
                user.setUsername(registerRequestDTO.getUsername());
                user.setPhone(registerRequestDTO.getPhone());
                user.setNickname(registerRequestDTO.getNickname());
                user.setUserType(registerRequestDTO.getUserType() != null ? registerRequestDTO.getUserType() : "USER");
            }

            // 编码密码
            user.setPassword(passwordEncoder.encode(registerRequestDTO.getPassword()));

            // 设置默认用户类型和状态
            if (user.getUserType() == null || user.getUserType().isEmpty()) {
                user.setUserType("USER");
            }
            user.setStatus(1); // 默认启用状态

            // 保存用户
            boolean saved = this.save(user);
            if (saved) {
                log.info("用户注册成功, userId: {}, username: {}", user.getId(), user.getUsername());

                // 同步将用户信息存入Redis缓存
                try {
                    UserDTO userDTO = userConverter.toDTO(user);
                    // 缓存用户信息，使用用户ID作为键
                    String userIdCacheKey = USER_CACHE_PREFIX + user.getId();
                    redisTemplate.opsForValue().set(userIdCacheKey, userDTO, 30, TimeUnit.MINUTES);

                    // 缓存用户信息，使用用户名作为键
                    String usernameCacheKey = USER_CACHE_PREFIX + "username:" + user.getUsername();
                    redisTemplate.opsForValue().set(usernameCacheKey, userDTO, 30, TimeUnit.MINUTES);
                    
                    // 发送用户变更消息到日志服务
                    userLogMessageService.sendUserChangeMessage(
                            user.getId(), 
                            user.getUsername(), 
                            null, 
                            user.getStatus(), 
                            1, // 创建用户
                            "SYSTEM");
                    
                    log.info("用户信息已同步存入Redis缓存, userId: {}, username: {}", user.getId(), user.getUsername());
                } catch (Exception e) {
                    log.error("将用户信息存入Redis缓存时发生异常, userId: {}, username: {}", user.getId(), user.getUsername(), e);
                    // 缓存存储失败不应影响主流程，仅记录日志即可
                }
            } else {
                log.error("用户注册失败, username: {}", registerRequestDTO.getUsername());
                throw new BusinessException(500, "用户注册失败");
            }

            return true;
        } catch (DuplicateKeyException e) {
            assert registerRequestDTO != null;
            log.error("保存用户信息时发生唯一约束冲突, username: {}", registerRequestDTO.getUsername(), e);
            throw new BusinessException(409, "用户名已存在: " + registerRequestDTO.getUsername());
        } catch (BusinessException e) {
            // 直接抛出已知的BusinessException
            throw e;
        } catch (Exception e) {
            log.error("用户注册时发生异常, username: {}", registerRequestDTO != null ? registerRequestDTO.getUsername() : "unknown", e);
            throw new BusinessException(500, "用户注册失败: " + e.getMessage());
        }
    }

    /**
     * 根据用户名获取用户信息
     * 该方法首先从Redis缓存中查找用户信息，如果缓存未命中，则从数据库中查询，
     * 并将查询结果存入缓存，缓存有效期为30分钟
     *
     * @param username 用户名
     * @return 用户信息DTO，如果用户不存在则返回null
     */
    @Override
    public UserDTO getUserByUsername(String username) {
        try {
            // 参数验证
            if (username == null || username.isEmpty()) {
                log.warn("获取用户信息失败：用户名不能为空");
                throw new BusinessException(400, "用户名不能为空");
            }

            // 先从缓存中获取
            String cacheKey = USER_CACHE_PREFIX + "username:" + username;
            UserDTO cachedUser = (UserDTO) redisTemplate.opsForValue().get(cacheKey);
            if (cachedUser != null) {
                log.info("从缓存中获取用户信息, username: {}", username);
                return cachedUser;
            }

            // 缓存未命中，查询数据库
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("username", username);
            User user = userMapper.selectOne(queryWrapper);
            if (user == null) {
                log.warn("获取用户信息失败：用户不存在, username: {}", username);
                throw new ResourceNotFoundException("user", username);
            }

            UserDTO userDTO = userConverter.toDTO(user);

            // 将结果存入缓存
            redisTemplate.opsForValue().set(cacheKey, userDTO, 30, TimeUnit.MINUTES);
            log.info("将用户信息存入缓存, username: {}", username);

            return userDTO;
        } catch (BusinessException e) {
            // 直接抛出已知的BusinessException
            throw e;
        } catch (Exception e) {
            log.error("根据用户名获取用户信息时发生异常, username: {}", username, e);
            throw new BusinessException(500, "获取用户信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前用户类型
     *
     * @param currentUser 当前用户实体
     * @return 用户类型，如果用户为空则返回null
     */
    @Override
    public Object getCurrentUserType(User currentUser) {
        if (currentUser != null) {
            return currentUser.getUserType();
        }
        return null;
    }

    /**
     * 清除用户相关缓存
     * 包括用户信息缓存、用户权限缓存和用户会话缓存
     *
     * @param username 用户名
     * @param userId   用户ID
     */
    @Override
    public void clearUserCache(String username, Long userId) {
        try {
            // 清除用户信息缓存
            if (username != null) {
                redisTemplate.delete(USER_CACHE_PREFIX + "username:" + username);
            }
            if (userId != null) {
                redisTemplate.delete(USER_CACHE_PREFIX + userId);
            }

            // 清除用户权限缓存
            if (userId != null) {
                redisTemplate.delete(USER_PERMISSIONS_CACHE_PREFIX + userId);
            }

            // 清除用户会话缓存
            if (userId != null) {
                redisTemplate.delete(USER_SESSION_CACHE_PREFIX + userId);
            }

            log.info("清除用户相关缓存, username: {}, userId: {}", username, userId);
        } catch (Exception e) {
            log.error("清除用户缓存时发生异常, username: {}, userId: {}", username, userId, e);
            // 缓存清除失败不应影响主流程，仅记录日志即可
        }
    }
}