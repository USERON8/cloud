package com.cloud.api.auth;

import com.cloud.common.domain.Result;
import com.cloud.common.domain.dto.auth.LoginRequestDTO;
import com.cloud.common.domain.dto.auth.LoginResponseDTO;
import com.cloud.common.domain.dto.auth.RegisterRequestDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 认证服务Feign客户端
 */
@FeignClient(name = "auth-service")
public interface AuthFeignClient {

    /**
     * 用户登录
     *
     * @param loginRequest 登录请求参数
     * @return 登录响应信息
     */
    @PostMapping("/auth/login")
    Result<LoginResponseDTO> login(@RequestBody LoginRequestDTO loginRequest);

    /**
     * 用户注册
     *
     * @param registerRequest 注册请求参数
     * @return 登录响应信息
     */
    @PostMapping("/auth/register")
    Result<LoginResponseDTO> register(@RequestBody RegisterRequestDTO registerRequest);

    /**
     * 用户注册并自动登录
     *
     * @param registerRequest 注册请求参数
     * @return 登录响应信息
     */
    @PostMapping("/auth/register-and-login")
    Result<LoginResponseDTO> registerAndLogin(@RequestBody RegisterRequestDTO registerRequest);
}