package com.cloud.auth.service;

import com.cloud.common.domain.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "user-server", url = "http://user-server")
public interface AuthUserService {
    @GetMapping("/user/users/findByUsername")
    UserDTO findByUsername(@RequestParam("username") String username);
}
