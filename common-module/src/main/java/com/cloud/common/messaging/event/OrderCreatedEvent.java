package com.cloud.common.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;







@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    


    private Long orderId;

    


    private String orderNo;

    


    private Long userId;

    


    private BigDecimal totalAmount;

    


    private Map<Long, Integer> productQuantityMap;

    


    private String remark;

    


    private Long timestamp;

    


    private String eventId;
}
