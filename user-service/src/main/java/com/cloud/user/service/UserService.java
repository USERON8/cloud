package com.cloud.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cloud.common.domain.dto.auth.RegisterRequestDTO;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.user.module.entity.User;

/**
 * @author what's up
 * @description 针对表【users(用户表)】的数据库操作Service
 * @createDate 2025-08-20 12:35:31
 */
public interface UserService extends IService<User> {
    /**
     * 获取当前用户类型
     *
     * @param currentUser 当前用户
     * @return 用户类型
     */
    Object getCurrentUserType(User currentUser);

    /**
     * 根据ID获取用户信息
     *
     * @param id 用户ID
     * @return 用户信息DTO
     */
    UserDTO getUserById(Long id);

    /**
     * 根据用户名获取用户信息
     *
     * @param username 用户名
     * @return 用户信息DTO
     */
    UserDTO getUserByUsername(String username);

    /**
     * 分页获取用户列表
     *
     * @param page     页码
     * @param size     每页大小
     * @param username 用户名（可选）
     * @return 用户列表
     */
    IPage<UserDTO> getUsersWithPagination(int page, int size, String username);

    /**
     * 用户注册
     *
     * @param registerRequestDTO 注册请求DTO
     * @return 是否注册成功
     */
    boolean register(RegisterRequestDTO registerRequestDTO);

    /**
     * 清除用户相关缓存
     *
     * @param username 用户名
     * @param userId   用户ID
     */
    void clearUserCache(String username, Long userId);
}