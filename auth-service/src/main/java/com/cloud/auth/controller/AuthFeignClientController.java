package com.cloud.auth.controller;

import com.cloud.api.auth.AuthFeignClient;
import com.cloud.common.domain.Result;
import com.cloud.common.domain.dto.auth.LoginRequestDTO;
import com.cloud.common.domain.dto.auth.LoginResponseDTO;
import com.cloud.common.domain.dto.auth.RegisterRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证服务Feign客户端接口实现控制器
 * 实现认证服务对外提供的Feign接口
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class AuthFeignClientController implements AuthFeignClient {

    /**
     * 用户登录
     *
     * @param loginRequest 登录请求参数
     * @return 登录响应信息
     */
    @Override
    public Result<LoginResponseDTO> login(LoginRequestDTO loginRequest) {
        // 此方法在Feign调用中不会被使用，因为登录是通过网关直接访问的
        log.info("Feign调用：用户登录，用户名: {}", loginRequest.getUsername());
        return Result.success();
    }

    /**
     * 用户注册
     *
     * @param registerRequest 注册请求参数
     * @return 登录响应信息
     */
    @Override
    public Result<LoginResponseDTO> register(RegisterRequestDTO registerRequest) {
        // 此方法在Feign调用中不会被使用，因为注册是通过网关直接访问的
        log.info("Feign调用：用户注册，用户名: {}", registerRequest.getUsername());
        return Result.success();
    }

    /**
     * 用户注册并自动登录
     *
     * @param registerRequest 注册请求参数
     * @return 登录响应信息
     */
    @Override
    public Result<LoginResponseDTO> registerAndLogin(RegisterRequestDTO registerRequest) {
        // 此方法在Feign调用中不会被使用，因为注册并登录是通过网关直接访问的
        log.info("Feign调用：用户注册并自动登录，用户名: {}", registerRequest.getUsername());
        return Result.success();
    }
}