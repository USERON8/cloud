package com.cloud.user.rpc;

import com.cloud.api.user.UserDubboApi;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService(interfaceClass = UserDubboApi.class, timeout = 5000, retries = 0)
@RequiredArgsConstructor
public class UserDubboService implements UserDubboApi {

    private final UserService userService;

    @Override
    public UserDTO findById(Long id) {
        return userService.getProfileById(id);
    }

    @Override
    public Long create(UserDTO userDTO) {
        return userService.createProfile(userDTO);
    }

    @Override
    public Boolean update(UserDTO userDTO) {
        return userService.updateProfile(userDTO);
    }
}

