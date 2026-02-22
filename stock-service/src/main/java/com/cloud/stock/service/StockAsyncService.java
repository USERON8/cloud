package com.cloud.stock.service;

import com.cloud.common.domain.dto.stock.StockDTO;
import com.cloud.stock.module.dto.StockOperationResult;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;







public interface StockAsyncService {

    






    CompletableFuture<List<StockDTO>> getStocksByProductIdsAsync(Collection<Long> productIds);

    






    CompletableFuture<Map<Long, Boolean>> checkStocksSufficientAsync(Map<Long, Integer> productQuantityMap);

    






    CompletableFuture<StockOperationResult> batchReserveStockAsync(Map<Long, Integer> productQuantityMap);

    





    CompletableFuture<StockOperationResult> batchReleaseStockAsync(Map<Long, Integer> productQuantityMap);

    





    CompletableFuture<StockOperationResult> batchStockInAsync(List<StockInRequest> stockInList);

    





    CompletableFuture<StockOperationResult> batchStockOutAsync(List<StockOutRequest> stockOutList);

    





    CompletableFuture<Void> refreshStockCacheAsync(Long productId);

    





    CompletableFuture<Void> batchRefreshStockCacheAsync(Collection<Long> productIds);

    





    CompletableFuture<Integer> preloadPopularStocksAsync(Integer limit);

    






    CompletableFuture<List<StockDTO>> getStockAlertListAsync(Integer threshold);

    




    CompletableFuture<Map<String, Object>> calculateStockValueAsync();

    


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
