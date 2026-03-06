package com.cloud.common.domain.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthorizationRequestDTO {

    @NotBlank(message = "clientId cannot be blank")
    private String clientId;

    @NotBlank(message = "redirectUri cannot be blank")
    private String redirectUri;

    @NotBlank(message = "scope cannot be blank")
    private String scope;

    @NotBlank(message = "state cannot be blank")
    private String state;

    @NotBlank(message = "codeChallenge cannot be blank")
    private String codeChallenge;

    private String codeChallengeMethod = "S256";

    private String nonce;
}
