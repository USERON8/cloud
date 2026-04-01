package com.cloud.common.messaging.event;

import com.cloud.common.domain.dto.stock.StockOperateCommandDTO;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReserveRequestEvent implements Serializable {

  private static final long serialVersionUID = 1L;

  private String orderNo;

  private List<StockOperateCommandDTO> items;

  private Long timestamp;

  private String eventId;

  private String eventType;
}
