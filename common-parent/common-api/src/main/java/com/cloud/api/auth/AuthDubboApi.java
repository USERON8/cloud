package com.cloud.api.auth;

import com.cloud.common.domain.dto.auth.RegisterRequestDTO;
import com.cloud.common.domain.dto.auth.RegisterResponseDTO;
import com.cloud.common.result.Result;

public interface AuthDubboApi {

    Result<RegisterResponseDTO> register(RegisterRequestDTO registerRequest);
}

