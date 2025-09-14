package com.cloud.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.domain.vo.UserVO;
import com.cloud.common.result.PageResult;
import com.cloud.common.domain.dto.user.UserPageDTO;
import com.cloud.user.module.entity.User;

import java.util.Collection;
import java.util.List;

/**
 * @author what's up
 * @description 针对表【users(用户表)】的数据库操作Service
 * @createDate 2025-09-06 19:31:12
 */
public interface UserService extends IService<User> {

    /**
     * 根据用户名查找用户
     *
     * @param username 用户名
     * @return 用户DTO
     */
    UserDTO findByUsername(String username);

    /**
     * 分页查询用户
     *
     * @param pageDTO 分页查询条件
     * @return 分页结果
     */
    PageResult<UserVO> pageQuery(UserPageDTO pageDTO);

    /**
     * 逻辑删除单个用户
     *
     * @param id 用户ID
     * @return 删除结果
     */
    boolean deleteUserById(Long id);

    /**
     * 批量逻辑删除用户（修复参数类型）
     *
     * @param userIds 用户ID集合
     * @return 删除结果
     */
    boolean deleteUsersByIds(Collection<Long> userIds);

    /**
     * 根据ID获取用户信息
     *
     * @param id 用户ID
     * @return 用户DTO
     */
    UserDTO getUserById(Long id);

    /**
     * 根据用户名获取用户信息
     *
     * @param username 用户名
     * @return 用户DTO
     */
    UserDTO getUserByUsername(String username);

    /**
     * 批量根据ID获取用户信息
     *
     * @param userIds 用户ID集合
     * @return 用户DTO列表
     */
    List<UserDTO> getUsersByIds(Collection<Long> userIds);
}
