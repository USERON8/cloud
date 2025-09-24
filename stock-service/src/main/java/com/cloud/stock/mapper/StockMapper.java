package com.cloud.stock.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.stock.module.entity.Stock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 库存Mapper接口
 * 提供库存相关的数据库操作，包括条件更新SQL确保并发安全
 *
 * @author what's up
 * @date 2025-01-15
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

    // ==================== 条件更新SQL方法（并发安全） ====================

    /**
     * 条件出库 - 扣减可用库存
     * 使用条件更新确保不会超卖
     *
     * @param productId 商品ID
     * @param quantity  出库数量
     * @return 影响行数，0表示库存不足或并发冲突
     */
    int stockOutWithCondition(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    /**
     * 条件预留库存 - 冻结可用库存
     * 使用条件更新确保不会超预留
     *
     * @param productId 商品ID
     * @param quantity  预留数量
     * @return 影响行数，0表示库存不足或并发冲突
     */
    int reserveStockWithCondition(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    /**
     * 条件释放预留库存 - 解冻库存
     * 使用条件更新确保操作安全
     *
     * @param productId 商品ID
     * @param quantity  释放数量
     * @return 影响行数，0表示冻结库存不足或并发冲突
     */
    int releaseReservedStockWithCondition(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    /**
     * 条件确认出库 - 从冻结库存中扣减
     * 用于订单确认后从冻结库存中真正扣减
     *
     * @param productId 商品ID
     * @param quantity  确认出库数量
     * @return 影响行数，0表示冻结库存不足或并发冲突
     */
    int confirmStockOutWithCondition(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    /**
     * 条件入库 - 增加总库存
     * 使用条件更新确保操作安全
     *
     * @param productId 商品ID
     * @param quantity  入库数量
     * @return 影响行数，0表示操作失败
     */
    int stockInWithCondition(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    /**
     * 根据商品ID查询库存（加行锁）
     * 用于需要加锁查询的场景
     *
     * @param productId 商品ID
     * @return 库存信息
     */
    Stock selectByProductIdForUpdate(@Param("productId") Long productId);
}



