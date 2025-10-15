package com.cloud.stock.service;

import com.cloud.stock.module.entity.StockCount;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 库存盘点服务接口
 *
 * @author what's up
 */
public interface StockCountService {
    /**
     * 创建库存盘点记录
     *
     * @param productId      商品ID
     * @param actualQuantity 实际盘点数量
     * @param operatorId     操作人ID
     * @param operatorName   操作人名称
     * @param remark         备注
     * @return 盘点记录ID
     */
    Long createStockCount(Long productId, Integer actualQuantity,
                          Long operatorId, String operatorName, String remark);

    /**
     * 确认盘点并调整库存
     *
     * @param countId        盘点记录ID
     * @param confirmUserId  确认人ID
     * @param confirmUserName 确认人名称
     * @return 确认结果
     */
    boolean confirmStockCount(Long countId, Long confirmUserId, String confirmUserName);

    /**
     * 取消盘点记录
     *
     * @param countId 盘点记录ID
     * @return 取消结果
     */
    boolean cancelStockCount(Long countId);

    /**
     * 根据ID查询盘点记录
     *
     * @param countId 盘点记录ID
     * @return 盘点记录
     */
    StockCount getStockCountById(Long countId);

    /**
     * 根据盘点单号查询盘点记录
     *
     * @param countNo 盘点单号
     * @return 盘点记录
     */
    StockCount getStockCountByNo(String countNo);

    /**
     * 根据商品ID查询盘点记录
     *
     * @param productId 商品ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 盘点记录列表
     */
    List<StockCount> getStockCountsByProductId(Long productId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据状态查询盘点记录
     *
     * @param status    盘点状态
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 盘点记录列表
     */
    List<StockCount> getStockCountsByStatus(String status, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查询待确认的盘点记录数量
     *
     * @return 待确认数量
     */
    int countPendingRecords();

    /**
     * 生成盘点单号
     *
     * @return 盘点单号
     */
    String generateCountNo();
}
