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
public class ProductSyncEvent implements Serializable {

  private static final long serialVersionUID = 1L;

  private Long spuId;

  private String eventType;

  private String eventId;

  private Long timestamp;
}
