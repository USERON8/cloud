package com.cloud.product.module.dto;

import com.cloud.common.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;






@Data
@EqualsAndHashCode(callSuper = true)
public class ProductPageDTO extends PageQuery {
    


    private String name;

    


    private Long shopId;

    


    private Long categoryId;

    


    private String categoryName;

    


    private Long brandId;

    


    private String brandName;

    


    private Integer status;

    


    private BigDecimal minPrice;

    


    private BigDecimal maxPrice;

    


    private Integer minStock;

    


    private Integer maxStock;

    


    private String priceSort;

    


    private String stockSort;

    


    private String salesSort;

    


    private String createTimeSort;

    


    private String updateTimeSort;
}
