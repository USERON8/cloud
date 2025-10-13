package com.cloud.stock.service;

import com.cloud.common.domain.dto.stock.StockDTO;
import com.cloud.stock.module.dto.StockOperationResult;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 库存异步服务接口
 * 提供高并发场景下的异步库存操作
 *
 * @author what's up
 */
public interface StockAsyncService {

    /**
     * 异步批量查询库存信息（并发优化）
     * 自动分批处理大量查询，提升性能
     *
     * @param productIds 商品ID集合
     * @return 库存DTO列表的Future
     */
    CompletableFuture<List<StockDTO>> getStocksByProductIdsAsync(Collection<Long> productIds);

    /**
     * 异步批量检查库存是否充足
     * 并发检查多个商品的库存
     *
     * @param productQuantityMap 商品ID和所需数量的映射
     * @return Map<商品ID, 是否充足>
     */
    CompletableFuture<Map<Long, Boolean>> checkStocksSufficientAsync(Map<Long, Integer> productQuantityMap);

    /**
     * 异步批量预留库存
     * 高并发场景下的库存预留操作
     *
     * @param productQuantityMap 商品ID和预留数量的映射
     * @return 预留操作结果
     */
    CompletableFuture<StockOperationResult> batchReserveStockAsync(Map<Long, Integer> productQuantityMap);

    /**
     * 异步批量释放库存
     *
     * @param productQuantityMap 商品ID和释放数量的映射
     * @return 释放操作结果
     */
    CompletableFuture<StockOperationResult> batchReleaseStockAsync(Map<Long, Integer> productQuantityMap);

    /**
     * 异步批量入库操作
     *
     * @param stockInList 入库信息列表
     * @return 入库操作结果
     */
    CompletableFuture<StockOperationResult> batchStockInAsync(List<StockInRequest> stockInList);

    /**
     * 异步批量出库操作
     *
     * @param stockOutList 出库信息列表
     * @return 出库操作结果
     */
    CompletableFuture<StockOperationResult> batchStockOutAsync(List<StockOutRequest> stockOutList);

    /**
     * 异步刷新库存缓存
     *
     * @param productId 商品ID
     * @return 刷新结果
     */
    CompletableFuture<Void> refreshStockCacheAsync(Long productId);

    /**
     * 异步批量刷新库存缓存
     *
     * @param productIds 商品ID集合
     * @return 刷新结果
     */
    CompletableFuture<Void> batchRefreshStockCacheAsync(Collection<Long> productIds);

    /**
     * 异步预加载热门商品库存
     *
     * @param limit 预加载数量
     * @return 预加载结果
     */
    CompletableFuture<Integer> preloadPopularStocksAsync(Integer limit);

    /**
     * 异步统计库存预警信息
     * 查找库存不足、即将售罄的商品
     *
     * @param threshold 预警阈值
     * @return 预警商品列表
     */
    CompletableFuture<List<StockDTO>> getStockAlertListAsync(Integer threshold);

    /**
     * 异步统计库存总值
     *
     * @return 库存总值
     */
    CompletableFuture<Map<String, Object>> calculateStockValueAsync();

    /**
     * 库存入库请求
     */
    class StockInRequest {
        private Long productId;
        private Integer quantity;
        private String remark;

        public StockInRequest(Long productId, Integer quantity, String remark) {
            this.productId = productId;
            this.quantity = quantity;
            this.remark = remark;
        }

        public Long getProductId() {
            return productId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public String getRemark() {
            return remark;
        }
    }

    /**
     * 库存出库请求
     */
    class StockOutRequest {
        private Long productId;
        private Integer quantity;
        private Long orderId;
        private String orderNo;
        private String remark;

        public StockOutRequest(Long productId, Integer quantity, Long orderId, String orderNo, String remark) {
            this.productId = productId;
            this.quantity = quantity;
            this.orderId = orderId;
            this.orderNo = orderNo;
            this.remark = remark;
        }

        public Long getProductId() {
            return productId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public Long getOrderId() {
            return orderId;
        }

        public String getOrderNo() {
            return orderNo;
        }

        public String getRemark() {
            return remark;
        }
    }
}
