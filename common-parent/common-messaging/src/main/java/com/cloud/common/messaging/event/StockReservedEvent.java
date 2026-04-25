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
public class StockReservedEvent implements Serializable {

  private static final long serialVersionUID = 1L;

  private String orderNo;

  private Long timestamp;

  private String eventId;

  private String eventType;
}
