package com.cloud.common.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAlertEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventId;

    private String eventType;

    private Long merchantId;

    private Long skuId;

    private Integer salableQty;

    private Integer alertThreshold;

    private Long timestamp;
}
