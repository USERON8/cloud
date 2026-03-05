package com.cloud.common.domain.dto.product;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;






@Data
public class ProductDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    


    private Long id;

    


    private Long shopId;

    


    private String name;

    


    private BigDecimal price;

    


    private Integer stockQuantity;

    


    private Long categoryId;

    


    private Long brandId;

    


    private Integer status;

    


    private String description;

    


    private String imageUrl;

    


    private LocalDateTime createdAt;

    


    private LocalDateTime updatedAt;

    


    private Boolean deleted;
}
