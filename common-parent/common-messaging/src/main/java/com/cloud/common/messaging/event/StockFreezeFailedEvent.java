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
public class StockFreezeFailedEvent implements Serializable {

  private static final long serialVersionUID = 1L;

  private Long orderId;

  private String orderNo;

  private String reason;

  private Long timestamp;

  private String eventId;

  private String eventType;
}
