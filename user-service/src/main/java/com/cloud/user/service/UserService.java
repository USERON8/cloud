package com.cloud.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.domain.dto.user.UserPageDTO;
import com.cloud.common.domain.vo.UserVO;
import com.cloud.common.result.PageResult;
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

    /**
     * 用户注册
     * 支持普通用户和商家用户注册，包含完整的事务处理
     *
     * @param registerRequest 注册请求数据
     * @return 注册成功的用户DTO
     */
    UserDTO registerUser(com.cloud.common.domain.dto.auth.RegisterRequestDTO registerRequest);

    /**
     * 获取用户密码（仅供认证服务使用）
     *
     * @param username 用户名
     * @return 加密后的密码
     */
    String getUserPassword(String username);

    /**
     * 根据GitHub ID查找用户
     *
     * @param githubId GitHub用户ID
     * @return 用户DTO，如果不存在则返回null
     */
    UserDTO findByGitHubId(Long githubId);

    /**
     * 根据GitHub用户名查找用户
     *
     * @param githubUsername GitHub用户名
     * @return 用户DTO，如果不存在则返回null
     */
    UserDTO findByGitHubUsername(String githubUsername);

    /**
     * 根据OAuth提供商和提供商ID查找用户
     *
     * @param oauthProvider OAuth提供商（如：github）
     * @param oauthProviderId OAuth提供商用户ID
     * @return 用户DTO，如果不存在则返回null
     */
    UserDTO findByOAuthProvider(String oauthProvider, String oauthProviderId);

    /**
     * 创建GitHub OAuth用户
     * 专门用于处理GitHub OAuth登录的用户创建逻辑
     *
     * @param githubUserDTO GitHub用户信息
     * @return 创建的用户DTO
     */
    UserDTO createGitHubUser(com.cloud.common.domain.dto.oauth.GitHubUserDTO githubUserDTO);

    /**
     * 更新GitHub OAuth用户信息
     * 用于同步GitHub用户信息变更
     *
     * @param userId 系统用户ID
     * @param githubUserDTO 最新的GitHub用户信息
     * @return 更新结果
     */
    boolean updateGitHubUserInfo(Long userId, com.cloud.common.domain.dto.oauth.GitHubUserDTO githubUserDTO);

}
