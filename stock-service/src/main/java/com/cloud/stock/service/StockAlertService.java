package com.cloud.stock.service;

import com.cloud.stock.module.entity.Stock;

import java.util.List;

/**
 * 库存预警服务接口
 *
 * @author what's up
 */
public interface StockAlertService {
    /**
     * 获取低库存商品列表
     *
     * @return 低库存商品列表
     */
    List<Stock> getLowStockProducts();

    /**
     * 获取指定阈值以下的库存商品
     *
     * @param threshold 库存阈值
     * @return 低库存商品列表
     */
    List<Stock> getLowStockProductsByThreshold(Integer threshold);

    /**
     * 更新商品的库存预警阈值
     *
     * @param productId 商品ID
     * @param threshold 预警阈值
     * @return 更新结果
     */
    boolean updateLowStockThreshold(Long productId, Integer threshold);

    /**
     * 批量更新库存预警阈值
     *
     * @param productIds 商品ID列表
     * @param threshold  预警阈值
     * @return 更新成功的数量
     */
    int batchUpdateLowStockThreshold(List<Long> productIds, Integer threshold);

    /**
     * 检查并发送低库存预警通知
     * (用于定时任务调用)
     *
     * @return 预警商品数量
     */
    int checkAndSendLowStockAlerts();

    /**
     * 发送低库存预警通知
     *
     * @param stock 库存信息
     */
    void sendLowStockAlert(Stock stock);

    /**
     * 批量发送低库存预警通知
     *
     * @param stocks 库存信息列表
     */
    void batchSendLowStockAlerts(List<Stock> stocks);
}
