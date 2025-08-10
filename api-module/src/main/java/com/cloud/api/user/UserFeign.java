package com.cloud.api.user;

import com.cloud.common.domain.dto.RegisterRequestDTO;
import com.cloud.common.domain.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "user-service")
public interface UserFeign {

    /**
     * 根据用户名查找用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    @GetMapping("/users/username/{username}")
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
     * @param userDTO 用户信息
     */
    @PostMapping("/users")
    void save(@RequestBody UserDTO userDTO);

    /**
     * 保存注册用户信息
     *
     * @param registerRequest 注册请求信息
     */
    @PostMapping("/users/register")
    void register(@RequestBody RegisterRequestDTO registerRequest);

    /**
     * 更新用户密码
     *
     * @param userId      用户ID
     * @param newPassword 新密码
     * @return 是否更新成功
     */
    @PostMapping("/users/{userId}/password")
    Boolean updatePassword(@PathVariable("userId") Long userId, @RequestBody String newPassword);
}