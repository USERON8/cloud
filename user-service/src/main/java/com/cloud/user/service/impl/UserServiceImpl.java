package com.cloud.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.domain.vo.UserVO;
import com.cloud.common.exception.BusinessException;
import com.cloud.common.result.PageResult;
import com.cloud.common.utils.PageUtils;
import com.cloud.user.annotation.MultiLevelCacheEvict;
import com.cloud.user.annotation.MultiLevelCachePut;
import com.cloud.user.annotation.MultiLevelCacheable;
import com.cloud.user.converter.UserConverter;
import com.cloud.user.exception.UserServiceException;
import com.cloud.user.mapper.UserMapper;
import com.cloud.common.domain.dto.user.UserPageDTO;
import com.cloud.user.module.entity.User;
import com.cloud.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
    private final UserMapper userMapper;
    private final UserConverter userConverter;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    @MultiLevelCacheable(
            cacheName = "userCache",
            key = "'username:' + #username",
            expire = 1800, // 30分钟
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
    @MultiLevelCacheEvict(
            cacheName = "userCache",
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
                throw new UserServiceException.UserNotFoundException(id);
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
    @Transactional(rollbackFor = Exception.class)
    @MultiLevelCacheEvict(
            cacheName = "userCache",
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
    @MultiLevelCacheable(
            cacheName = "userCache",
            key = "#id",
            expire = 1800, // 30分钟
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
                throw new UserServiceException.UserNotFoundException(id);
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
    @MultiLevelCacheable(
            cacheName = "userCache",
            key = "'username:' + #username",
            expire = 1800, // 30分钟
            unless = "#result == null"
    )
    public UserDTO getUserByUsername(String username) {
        // 直接调用findByUsername方法，避免重复实现相同逻辑
        // 这里保留缓存注解，因为可能有不同的缓存策略
        return findByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    @MultiLevelCacheable(
            cacheName = "userCache",
            key = "'batch:' + #userIds.toString()",
            expire = 900, // 15分钟，批量查询缓存时间短一些
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
    @MultiLevelCachePut(
            cacheName = "userCache",
            key = "#entity.id",
            expire = 1800
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
    @com.cloud.user.annotation.MultiLevelCaching(
            evict = {
                    @MultiLevelCacheEvict(cacheName = "userCache", key = "#entity.id"),
                    @MultiLevelCacheEvict(cacheName = "userCache", key = "'username:' + #entity.username", condition = "#entity.username != null")
            },
            put = {
                    @com.cloud.user.annotation.MultiLevelCachePut(cacheName = "userCache", key = "#entity.id", expire = 1800)
            }
    )
    public boolean updateById(User entity) {
        log.info("更新用户信息, userId: {}", entity.getId());
        return super.updateById(entity);
    }

}
