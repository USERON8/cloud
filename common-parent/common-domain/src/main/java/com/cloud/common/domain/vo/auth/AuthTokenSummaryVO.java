package com.cloud.common.domain.vo.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "OAuth2 token summary")
public class AuthTokenSummaryVO implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  @Schema(description = "Token issue time")
  private Instant issuedAt;

  @Schema(description = "Token expiry time")
  private Instant expiresAt;

  @Schema(description = "Authorized scopes")
  private Set<String> scopes;
}
