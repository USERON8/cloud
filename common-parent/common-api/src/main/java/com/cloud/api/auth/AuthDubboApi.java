package com.cloud.api.auth;

import com.cloud.common.domain.dto.auth.LoginRequestDTO;
import com.cloud.common.domain.dto.auth.LoginResponseDTO;
import com.cloud.common.domain.dto.auth.RegisterRequestDTO;
import com.cloud.common.result.Result;

public interface AuthDubboApi {

    Result<LoginResponseDTO> login(LoginRequestDTO loginRequest);

    Result<LoginResponseDTO> register(RegisterRequestDTO registerRequest);

    Result<LoginResponseDTO> refreshToken(String refreshToken);
}

