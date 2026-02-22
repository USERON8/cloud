package com.cloud.common.domain.vo.order;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;




@Data
public class OrderVO {
    


    private Long id;

    


    private String orderNo;

    


    private Long userId;

    


    private BigDecimal totalAmount;

    


    private BigDecimal payAmount;

    


    private Integer status;

    


    private Long addressId;

    


    private LocalDateTime createdAt;

    


    private LocalDateTime updatedAt;
}
