package com.cloud.common.domain.vo.stock;

import lombok.Data;

import java.time.LocalDateTime;




@Data
public class StockVO {
    


    private Long id;

    


    private String productName;

    


    private Long productId;

    


    private Integer stockQuantity;

    


    private Integer frozenQuantity;

    


    private Integer availableQuantity;

    


    private Integer stockStatus;

    


    private LocalDateTime createdAt;

    


    private LocalDateTime updatedAt;
}
