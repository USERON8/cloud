package com.cloud.common.domain.vo.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "OAuth2 authorization detail")
public class AuthAuthorizationDetailVO implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  @Schema(description = "Authorization id")
  private String id;

  @Schema(description = "Registered client id")
  private String clientId;

  @Schema(description = "Principal name")
  private String principalName;

  @Schema(description = "Grant type")
  private String grantType;

  @Schema(description = "Authorized scopes")
  private Set<String> scopes;

  @Schema(description = "Access token summary")
  private AuthTokenSummaryVO accessToken;

  @Schema(description = "Refresh token summary")
  private AuthTokenSummaryVO refreshToken;
}
