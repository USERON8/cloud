package com.cloud.common.domain.dto.user;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserNotificationStatusChangeRequestDTO {

  @NotNull(message = "new status is required")
  private Integer newStatus;

  private String reason;
}
