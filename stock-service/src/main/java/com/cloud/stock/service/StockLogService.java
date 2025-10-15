package com.cloud.stock.service;

import com.cloud.stock.module.entity.StockLog;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 库存操作日志服务接口
 *
 * @author what's up
 */
public interface StockLogService {
    /**
     * 创建库存操作日志
     *
     * @param stockLog 日志信息
     * @return 日志ID
     */
    Long createLog(StockLog stockLog);

    /**
     * 批量创建库存操作日志
     *
     * @param stockLogs 日志列表
     * @return 成功数量
     */
    int batchCreateLogs(List<StockLog> stockLogs);

    /**
     * 根据商品ID查询日志
     *
     * @param productId 商品ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 日志列表
     */
    List<StockLog> getLogsByProductId(Long productId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据订单ID查询日志
     *
     * @param orderId 订单ID
     * @return 日志列表
     */
    List<StockLog> getLogsByOrderId(Long orderId);

    /**
     * 根据操作类型查询日志
     *
     * @param operationType 操作类型
     * @param startTime     开始时间
     * @param endTime       结束时间
     * @return 日志列表
     */
    List<StockLog> getLogsByOperationType(String operationType, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 记录库存变更日志
     *
     * @param productId      商品ID
     * @param productName    商品名称
     * @param operationType  操作类型
     * @param quantityBefore 操作前数量
     * @param quantityAfter  操作后数量
     * @param orderId        订单ID (可选)
     * @param orderNo        订单号 (可选)
     * @param remark         备注 (可选)
     */
    void logStockChange(Long productId, String productName, String operationType,
                        Integer quantityBefore, Integer quantityAfter,
                        Long orderId, String orderNo, String remark);
}
