package com.cloud.api.user;

import com.cloud.common.domain.dto.auth.RegisterRequestDTO;
import com.cloud.common.domain.dto.user.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * 用户服务Feign客户端
 * 用于服务间调用用户服务的接口
 * 直接返回业务对象，仅用于服务内部调用
 *
 * @author cloud
 */
@FeignClient(name = "user-service", path = "/internal/user", contextId = "userFeignClient")
public interface UserFeignClient {

    /**
     * 根据用户名查找用户
     *
     * @param username 用户名
     * @return 用户信息，不存在时返回null
     */
    @GetMapping("/username/{username}")
    UserDTO findByUsername(@PathVariable("username") String username);

    /**
     * 根据ID查找用户
     *
     * @param id 用户ID
     * @return 用户信息，不存在时返回null
     */
    @GetMapping("/id/{id}")
    UserDTO findById(@PathVariable("id") Long id);

    /**
     * 用户注册
     *
     * @param registerRequest 用户注册信息
     * @return 注册后的用户信息
     */
    @PostMapping("/register")
    UserDTO register(@RequestBody RegisterRequestDTO registerRequest);

    /**
     * 更新用户信息
     *
     * @param userDTO 用户信息
     * @return 是否更新成功
     */
    @PutMapping("/update")
    Boolean update(@RequestBody UserDTO userDTO);

    /**
     * 获取用户密码（仅供auth-service使用）
     *
     * @param username 用户名
     * @return 加密后的密码，不存在时返回null
     */
    @GetMapping("/password/{username}")
    String getUserPassword(@PathVariable("username") String username);

    /**
     * 根据GitHub ID查找用户（仅供auth-service使用）
     *
     * @param githubId GitHub用户ID
     * @return 用户信息，不存在时返回null
     */
    @GetMapping("/github-id/{githubId}")
    UserDTO findByGitHubId(@PathVariable("githubId") Long githubId);

    /**
     * 创建GitHub OAuth用户（仅供auth-service使用）
     *
     * @param githubUserDTO GitHub用户信息
     * @return 创建后的用户信息
     */
    @PostMapping("/github/create")
    UserDTO createGitHubUser(@RequestBody com.cloud.common.domain.dto.oauth.GitHubUserDTO githubUserDTO);

    /**
     * 更新GitHub OAuth用户信息（仅供auth-service使用）
     *
     * @param userId        系统用户ID
     * @param githubUserDTO GitHub用户信息
     * @return 是否更新成功
     */
    @PutMapping("/github/update/{userId}")
    Boolean updateGitHubUserInfo(@PathVariable("userId") Long userId,
                                 @RequestBody com.cloud.common.domain.dto.oauth.GitHubUserDTO githubUserDTO);
}