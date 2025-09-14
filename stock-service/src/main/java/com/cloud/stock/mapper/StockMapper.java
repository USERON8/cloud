package com.cloud.stock.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.stock.module.entity.Stock;
import org.apache.ibatis.annotations.Mapper;

/**
 * 库存Mapper接口
 *
 * @author what's up
 */
@Mapper
public interface StockMapper extends BaseMapper<Stock> {

    /**
     * 更新库存数量（支持增加和减少）
     *
     * @param stockId  库存ID
     * @param quantity 变动数量（正数为增加，负数为减少）
     * @return 影响行数
     */
    int updateStockQuantity(Long stockId, Integer quantity);

    /**
     * 冻结库存
     *
     * @param stockId  库存ID
     * @param quantity 冻结数量
     * @return 影响行数
     */
    int freezeStock(Long stockId, Integer quantity);

    /**
     * 解冻库存
     *
     * @param stockId  库存ID
     * @param quantity 解冻数量
     * @return 影响行数
     */
    int unfreezeStock(Long stockId, Integer quantity);
}



