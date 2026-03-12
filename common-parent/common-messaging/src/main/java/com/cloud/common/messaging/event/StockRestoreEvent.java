package com.cloud.common.messaging.event;

import com.cloud.common.domain.dto.stock.StockOperateCommandDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockRestoreEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String refundNo;

    private String mainOrderNo;

    private String subOrderNo;

    private List<StockOperateCommandDTO> items;

    private Long timestamp;

    private String eventId;

    private String eventType;
}
