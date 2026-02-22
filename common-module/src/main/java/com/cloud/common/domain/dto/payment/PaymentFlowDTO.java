package com.cloud.common.domain.dto.payment;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;




@Data
public class PaymentFlowDTO {
    


    private Long id;

    


    private String paymentId;

    


    private Integer flowType;

    


    private BigDecimal amount;

    


    private String traceId;

    


    private LocalDateTime createdAt;

    


    private LocalDateTime updatedAt;

    


    private Integer deleted;
}
