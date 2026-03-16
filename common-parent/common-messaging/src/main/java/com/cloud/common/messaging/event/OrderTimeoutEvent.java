package com.cloud.common.messaging.event;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderTimeoutEvent implements Serializable {

  private static final long serialVersionUID = 1L;

  private Long subOrderId;

  private String mainOrderNo;

  private String subOrderNo;

  private Long userId;

  private Integer timeoutMinutes;

  private String eventId;

  private String eventType;

  private Long timestamp;
}
