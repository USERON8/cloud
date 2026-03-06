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
public class PaymentSuccessEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    


    private Long paymentId;

    


    private Long orderId;

    


    private String orderNo;

    


    private Long userId;

    


    private BigDecimal amount;

    


    private String paymentMethod;

    

    private String transactionNo;

    


    private Map<Long, Integer> productQuantityMap;

    


    private Long timestamp;

    


    private String eventId;

    

    private String eventType;
}
