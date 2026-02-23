package com.cloud.api.auth;

import com.cloud.common.domain.dto.auth.LoginRequestDTO;
import com.cloud.common.domain.dto.auth.LoginResponseDTO;
import com.cloud.common.domain.dto.auth.RegisterRequestDTO;
import com.cloud.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "auth-service", contextId = "authFeignClient")
public interface AuthFeignClient {

    @PostMapping("/auth/sessions")
    Result<LoginResponseDTO> login(@RequestBody LoginRequestDTO loginRequest);

    @PostMapping("/auth/users/register")
    Result<LoginResponseDTO> register(@RequestBody RegisterRequestDTO registerRequest);

    @PostMapping("/auth/tokens/refresh")
    Result<LoginResponseDTO> refreshToken(@RequestParam("refresh_token") String refreshToken);
}
