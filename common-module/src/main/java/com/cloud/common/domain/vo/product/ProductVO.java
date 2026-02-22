package com.cloud.common.domain.vo.product;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;




@Data
public class ProductVO {
    


    private Long id;

    


    private Long shopId;

    


    private String name;

    


    private BigDecimal price;

    


    private Integer stockQuantity;

    


    private Integer categoryId;

    


    private Integer status;

    


    private LocalDateTime createdAt;

    


    private LocalDateTime updatedAt;
}
