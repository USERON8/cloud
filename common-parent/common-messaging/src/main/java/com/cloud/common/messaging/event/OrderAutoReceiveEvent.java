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
public class OrderAutoReceiveEvent implements Serializable {

  private static final long serialVersionUID = 1L;

  private Long subOrderId;
  private String mainOrderNo;
  private String subOrderNo;
  private Long userId;
  private Long timestamp;
  private String eventId;
  private String eventType;
}
