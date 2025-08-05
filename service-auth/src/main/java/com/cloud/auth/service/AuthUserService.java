package com.cloud.auth.service;

import com.cloud.common.domain.dto.UserDTO;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;
import com.cloud.api.user.UserService;

@Service
public class AuthUserService {

    @DubboReference
    private UserService userService;

    public UserDTO findByUsername(String username) {
        return userService.findByUsername(username);
    }
}