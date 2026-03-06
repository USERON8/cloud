package com.cloud.common.domain.vo.stock;

import lombok.Data;




@Data
public class StockStatisticsVO {

    


    private Long totalProducts;

    


    private Long outOfStockCount;

    


    private Long lowStockCount;

    


    private Long sufficientStockCount;

    


    private Long totalStockCount;

    


    private Long totalAvailableCount;

    


    private Long totalFrozenCount;
}
