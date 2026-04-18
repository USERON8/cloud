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
@Schema(description = "Token blacklist statistics")
public class TokenBlacklistStatsVO implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  @Schema(description = "Total blacklisted token count")
  private Long totalBlacklisted;

  @Schema(description = "Current active blacklisted token count")
  private Integer activeBlacklisted;

  @Schema(description = "Statistics last updated time")
  private Instant lastUpdated;
}
