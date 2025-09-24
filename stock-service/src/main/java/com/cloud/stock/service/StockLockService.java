package com.cloud.stock.service;

import com.cloud.common.domain.dto.stock.StockDTO;
import com.cloud.stock.module.dto.StockOperationResult;

/**
 * 库存锁服务接口
 * 提供基于分布式锁的库存操作，确保并发安全
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
public interface StockLockService {

    /**
     * 安全出库 - 使用分布式锁保护
     *
     * @param productId  商品ID
     * @param quantity   出库数量
     * @param operatorId 操作人ID
     * @param remark     备注
     * @return 操作结果
     */
    StockOperationResult safeStockOut(Long productId, Integer quantity, Long operatorId, String remark);

    /**
     * 安全预留库存 - 使用分布式锁保护
     *
     * @param productId  商品ID
     * @param quantity   预留数量
     * @param operatorId 操作人ID
     * @param remark     备注
     * @return 操作结果
     */
    StockOperationResult safeReserveStock(Long productId, Integer quantity, Long operatorId, String remark);

    /**
     * 安全释放预留库存 - 使用分布式锁保护
     *
     * @param productId  商品ID
     * @param quantity   释放数量
     * @param operatorId 操作人ID
     * @param remark     备注
     * @return 操作结果
     */
    StockOperationResult safeReleaseReservedStock(Long productId, Integer quantity, Long operatorId, String remark);

    /**
     * 安全确认出库 - 使用分布式锁保护
     *
     * @param productId  商品ID
     * @param quantity   确认出库数量
     * @param operatorId 操作人ID
     * @param remark     备注
     * @return 操作结果
     */
    StockOperationResult safeConfirmStockOut(Long productId, Integer quantity, Long operatorId, String remark);

    /**
     * 安全入库 - 使用分布式锁保护
     *
     * @param productId  商品ID
     * @param quantity   入库数量
     * @param operatorId 操作人ID
     * @param remark     备注
     * @return 操作结果
     */
    StockOperationResult safeStockIn(Long productId, Integer quantity, Long operatorId, String remark);

    /**
     * 获取库存信息 - 使用分布式锁保护
     *
     * @param productId 商品ID
     * @return 库存信息
     */
    StockDTO getStockWithLock(Long productId);
}
