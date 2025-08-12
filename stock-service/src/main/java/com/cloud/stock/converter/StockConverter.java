package com.cloud.stock.converter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.common.domain.dto.StockDTO;
import com.cloud.common.domain.dto.StockPageDTO;
import com.cloud.common.domain.vo.StockVO;
import com.cloud.stock.constant.StockConstant;
import com.cloud.stock.module.entity.Stock;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 库存实体转换器
 */
@Mapper(componentModel = "spring")
public interface StockConverter {

    @Mapping(target = "stockStatus", source = ".", qualifiedByName = "calculateStockStatus")
    @Mapping(target = "stockStatusDesc", source = ".", qualifiedByName = "getStockStatusDesc")
    @Mapping(target = "availableCount", expression = "java(stock.getStockCount() - stock.getFrozenCount())")
    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    StockVO toVO(Stock stock);

    /**
     * 复制StockPageDTO对象
     */
    StockPageDTO copyStockPageDTO(StockPageDTO source);

    Stock toEntity(StockDTO stockDTO);

    /**
     * 计算库存状态
     */
    @Named("calculateStockStatus")
    default Integer calculateStockStatus(Stock stock) {
        if (stock == null) return null;

        int availableCount = stock.getStockCount() - stock.getFrozenCount();
        if (availableCount <= 0) {
            return StockConstant.Status.OUT_OF_STOCK;
        } else if (availableCount <= StockConstant.Threshold.LOW_STOCK_THRESHOLD) {
            return StockConstant.Status.LOW_STOCK;
        } else {
            return StockConstant.Status.SUFFICIENT_STOCK;
        }
    }

    /**
     * 获取库存状态描述
     */
    @Named("getStockStatusDesc")
    default String getStockStatusDesc(Stock stock) {
        Integer status = calculateStockStatus(stock);
        if (status == null) return StockConstant.StatusDesc.UNKNOWN;

        return switch (status) {
            case 0 -> StockConstant.StatusDesc.OUT_OF_STOCK;
            case 1 -> StockConstant.StatusDesc.LOW_STOCK;
            case 2 -> StockConstant.StatusDesc.SUFFICIENT_STOCK;
            default -> StockConstant.StatusDesc.UNKNOWN;
        };
    }

    /**
     * 实体列表转换为VO列表
     */
    List<StockVO> toVOList(List<Stock> stockList);

    /**
     * 构建查询条件
     */
    default LambdaQueryWrapper<Stock> buildQueryWrapper(StockPageDTO pageDTO) {
        LambdaQueryWrapper<Stock> wrapper = new LambdaQueryWrapper<>();

        // 商品ID精确查询
        wrapper.eq(pageDTO.getProductId() != null, Stock::getProductId, pageDTO.getProductId());

        // 商品名称模糊查询
        wrapper.like(StringUtils.hasText(pageDTO.getProductName()), Stock::getProductName, pageDTO.getProductName());

        // 可用库存范围查询
        wrapper.ge(pageDTO.getMinAvailableCount() != null, Stock::getAvailableCount, pageDTO.getMinAvailableCount());
        wrapper.le(pageDTO.getMaxAvailableCount() != null, Stock::getAvailableCount, pageDTO.getMaxAvailableCount());

        // 排序处理
        if (StringUtils.hasText(pageDTO.getOrderBy())) {
            boolean isAsc = "asc".equalsIgnoreCase(pageDTO.getOrderType());
            switch (pageDTO.getOrderBy()) {
                case "stock_count" -> wrapper.orderBy(true, isAsc, Stock::getStockCount);
                case "available_count" -> wrapper.orderBy(true, isAsc, Stock::getAvailableCount);
                case "frozen_count" -> wrapper.orderBy(true, isAsc, Stock::getFrozenCount);
                case "create_time" -> wrapper.orderBy(true, isAsc, Stock::getCreatedAt);
                case "update_time" -> wrapper.orderBy(true, isAsc, Stock::getUpdatedAt);
                default -> wrapper.orderByDesc(Stock::getUpdatedAt);
            }
        } else {
            wrapper.orderByDesc(Stock::getUpdatedAt);
        }

        return wrapper;
    }
}