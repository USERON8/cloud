package com.cloud.common.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import com.cloud.common.domain.dto.stock.StockOperateCommandDTO;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundCompletedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long refundId;

    private String refundNo;

    private String paymentNo;

    private String afterSaleNo;

    private String mainOrderNo;

    private String subOrderNo;

    private List<StockOperateCommandDTO> items;

    private Long timestamp;

    private String eventId;

    private String eventType;
}
