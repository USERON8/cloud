package com.cloud.common.domain.dto.stock;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StockDTO {
    


    private Long id;

    


    private Long productId;

    


    private String productName;

    


    private Integer stockQuantity;

    


    private Integer frozenQuantity;

    


    private Integer availableQuantity;

    


    private Integer stockStatus;

    


    private LocalDateTime createdAt;

    


    private LocalDateTime updatedAt;
}
