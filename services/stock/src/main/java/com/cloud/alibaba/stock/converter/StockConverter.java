package com.cloud.alibaba.stock.converter;

import com.cloud.alibaba.stock.constant.StockConstant;
import com.cloud.alibaba.stock.module.entity.Stock;
import com.cloud.alibaba.stock.module.vo.StockVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

/**
 * 库存实体转换器
 */
@Mapper(componentModel = "spring")
public interface StockConverter {

    /**
     * Stock实体转StockVO
     */
    @Mapping(target = "stockStatus", source = "availableCount", qualifiedByName = "calculateStockStatus")
    @Mapping(target = "stockStatusDesc", source = "availableCount", qualifiedByName = "getStockStatusDesc")
    StockVO toVO(Stock stock);

    /**
     * Stock实体列表转StockVO列表
     */
    List<StockVO> toVOList(List<Stock> stockList);

    /**
     * 计算库存状态
     */
    @Named("calculateStockStatus")
    default Integer calculateStockStatus(Integer availableCount) {
        if (availableCount == null || availableCount <= 0) {
            return StockConstant.Status.OUT_OF_STOCK;
        } else if (availableCount < StockConstant.Threshold.LOW_STOCK_THRESHOLD) {
            return StockConstant.Status.LOW_STOCK;
        } else {
            return StockConstant.Status.SUFFICIENT_STOCK;
        }
    }

    /**
     * 获取库存状态描述
     */
    @Named("getStockStatusDesc")
    default String getStockStatusDesc(Integer availableCount) {
        Integer status = calculateStockStatus(availableCount);

        // 使用传统的if-else替代switch表达式，避免Integer比较问题
        if (StockConstant.Status.OUT_OF_STOCK.equals(status)) {
            return StockConstant.StatusDesc.OUT_OF_STOCK;
        } else if (StockConstant.Status.LOW_STOCK.equals(status)) {
            return StockConstant.StatusDesc.LOW_STOCK;
        } else if (StockConstant.Status.SUFFICIENT_STOCK.equals(status)) {
            return StockConstant.StatusDesc.SUFFICIENT_STOCK;
        } else {
            return StockConstant.StatusDesc.UNKNOWN;
        }
    }
}