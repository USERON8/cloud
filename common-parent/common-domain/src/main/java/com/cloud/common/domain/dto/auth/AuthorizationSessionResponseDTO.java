package com.cloud.common.domain.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizationSessionResponseDTO {

  private String authorizationUri;

  private String clientId;

  private String redirectUri;

  private String scope;

  private String state;

  private String codeChallengeMethod;

  private boolean sessionEstablished;
}
