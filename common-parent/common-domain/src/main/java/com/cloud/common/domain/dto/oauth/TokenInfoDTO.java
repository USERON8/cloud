package com.cloud.common.domain.dto.oauth;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenInfoDTO {

  private String accessToken;

  private String refreshToken;

  private String tokenType;

  private LocalDateTime expiresAt;

  private List<String> permissions;

  private List<String> roles;
}
