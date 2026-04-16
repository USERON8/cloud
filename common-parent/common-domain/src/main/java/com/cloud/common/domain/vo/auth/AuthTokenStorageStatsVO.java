package com.cloud.common.domain.vo.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "OAuth2 token storage statistics")
public class AuthTokenStorageStatsVO implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  @Schema(description = "Authorization object count")
  private Long authorizationCount;

  @Schema(description = "Access token index count")
  private Long accessIndexCount;

  @Schema(description = "Refresh token index count")
  private Long refreshIndexCount;

  @Schema(description = "Authorization code index count")
  private Long codeIndexCount;

  @Schema(description = "Principal index count")
  private Long principalIndexCount;

  @Schema(description = "Redis storage information")
  private String redisInfo;

  @Schema(description = "Storage type")
  private String storageType;
}
