package com.cloud.api.user;

import com.cloud.common.domain.Result;
import com.cloud.common.domain.dto.auth.RegisterRequestDTO;
import com.cloud.common.domain.dto.user.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户服务Feign客户端
 */
@FeignClient(name = "user-service", path = "/user")
public interface UserFeignClient {

    /**
     * 根据用户名查找用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    @GetMapping("/username/{username}")
    UserDTO findByUsername(@PathVariable("username") String username);

    /**
     * 根据ID查找用户
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @GetMapping("/users/{id}")
    UserDTO findById(@PathVariable("id") Long id);

    /**
     * 保存用户信息
     *
     * @param registerRequest 用户注册信息
     * @return 保存结果
     */
    @PostMapping("/users")
    Result<Boolean> register(@RequestBody RegisterRequestDTO registerRequest);

    /**
     * 更新用户信息
     *
     * @param id      用户ID
     * @param userDTO 用户信息
     * @return 更新结果
     */
    @PutMapping("/users/{id}")
    Result<Void> update(@PathVariable("id") Long id, @RequestBody UserDTO userDTO);

    /**
     * 获取所有用户
     *
     * @return 用户列表
     */
    @GetMapping("/users")
    Result<List<UserDTO>> getAllUsers();
}