package com.cloud.user.controller;

import com.cloud.common.domain.dto.UserDTO;
import com.cloud.user.converter.UserConverter;
import com.cloud.user.module.entity.User;
import com.cloud.user.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;


@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final UserConverter userConverter;

    public UserController(UserService userService, UserConverter userConverter) {
        this.userService = userService;
        this.userConverter = userConverter;
    }

    @GetMapping("/user/info")
    public Map<String, Object> getUserInfo(@AuthenticationPrincipal Jwt jwt) {
        return Collections.singletonMap("username", jwt.getSubject());
    }

    // 为auth服务提供根据用户名查询用户的方法
    @GetMapping("/users/findByUsername")
    public UserDTO findByUsername(@RequestParam("username") String username) {
        User user = userService.getOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                .eq("username", username));
        return userConverter.toDTO(user);
    }
}
