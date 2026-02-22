package com.cloud.stock.module.dto;

import com.cloud.common.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;






@Data
@EqualsAndHashCode(callSuper = true)
public class StockPageDTO extends PageQuery {
    


    private Long productId;

    


    private String productName;

    


    private Integer stockStatus;
}
