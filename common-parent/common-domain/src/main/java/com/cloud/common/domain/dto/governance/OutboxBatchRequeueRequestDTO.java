package com.cloud.common.domain.dto.governance;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Data
public class OutboxBatchRequeueRequestDTO {

  @NotEmpty(message = "ids are required")
  private List<@NotNull(message = "id cannot be null") Long> ids;
}
