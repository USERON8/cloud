package com.cloud.common.domain.vo.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Token blacklist check result")
public class TokenBlacklistCheckVO implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  @Schema(description = "Masked token preview")
  private String tokenPreview;

  @Schema(description = "Whether the token is blacklisted")
  private Boolean blacklisted;

  @Schema(description = "Check time")
  private Instant checkedAt;
}
