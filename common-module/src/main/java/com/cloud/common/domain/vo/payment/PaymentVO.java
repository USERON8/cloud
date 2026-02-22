package com.cloud.common.domain.vo.payment;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;




@Data
public class PaymentVO {
    


    private Long id;

    


    private Long orderId;

    


    private Long userId;

    


    private BigDecimal amount;

    


    private Integer status;

    


    private Integer channel;

    


    private String transactionId;

    


    private LocalDateTime createdAt;

    


    private LocalDateTime updatedAt;
}
