package com.cloud.stock.module.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;









@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockOperationResult {

    


    private Boolean success;

    


    private String operationType;

    


    private Long productId;

    


    private Integer quantity;

    


    private Integer beforeStockQuantity;

    


    private Integer afterStockQuantity;

    


    private Integer beforeFrozenQuantity;

    


    private Integer afterFrozenQuantity;

    


    private String errorCode;

    


    private String errorMessage;

    


    private LocalDateTime operationTime;

    


    private Long operatorId;

    


    private String remark;

    


    private Long executionTime;

    


    private Boolean usedDistributedLock;

    


    private Long lockWaitTime;

    






    public StockOperationResult(int successCount, int failureCount, List<String> errors) {
        this.success = failureCount == 0;
        this.quantity = successCount + failureCount;
        this.errorMessage = errors.isEmpty() ? null : String.join("; ", errors);
        this.operationTime = LocalDateTime.now();
        this.usedDistributedLock = true;
    }

    













    public static StockOperationResult success(String operationType, Long productId, Integer quantity,
                                               Integer beforeStock, Integer afterStock,
                                               Integer beforeFrozen, Integer afterFrozen,
                                               Long operatorId, String remark) {
        return StockOperationResult.builder()
                .success(true)
                .operationType(operationType)
                .productId(productId)
                .quantity(quantity)
                .beforeStockQuantity(beforeStock)
                .afterStockQuantity(afterStock)
                .beforeFrozenQuantity(beforeFrozen)
                .afterFrozenQuantity(afterFrozen)
                .operationTime(LocalDateTime.now())
                .operatorId(operatorId)
                .remark(remark)
                .usedDistributedLock(true)
                .build();
    }

    










    public static StockOperationResult failure(String operationType, Long productId, Integer quantity,
                                               String errorCode, String errorMessage, Long operatorId) {
        return StockOperationResult.builder()
                .success(false)
                .operationType(operationType)
                .productId(productId)
                .quantity(quantity)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .operationTime(LocalDateTime.now())
                .operatorId(operatorId)
                .usedDistributedLock(true)
                .build();
    }

    






    public StockOperationResult withTiming(Long executionTime, Long lockWaitTime) {
        this.executionTime = executionTime;
        this.lockWaitTime = lockWaitTime;
        return this;
    }

    




    public Integer getBeforeAvailableQuantity() {
        if (beforeStockQuantity == null || beforeFrozenQuantity == null) {
            return null;
        }
        return beforeStockQuantity - beforeFrozenQuantity;
    }

    




    public Integer getAfterAvailableQuantity() {
        if (afterStockQuantity == null || afterFrozenQuantity == null) {
            return null;
        }
        return afterStockQuantity - afterFrozenQuantity;
    }

    


    public static class OperationType {
        public static final String STOCK_OUT = "STOCK_OUT";
        public static final String STOCK_IN = "STOCK_IN";
        public static final String RESERVE = "RESERVE";
        public static final String RELEASE_RESERVE = "RELEASE_RESERVE";
        public static final String CONFIRM_OUT = "CONFIRM_OUT";
    }

    


    public static class ErrorCode {
        public static final String INSUFFICIENT_STOCK = "INSUFFICIENT_STOCK";
        public static final String INSUFFICIENT_FROZEN = "INSUFFICIENT_FROZEN";
        public static final String STOCK_NOT_FOUND = "STOCK_NOT_FOUND";
        public static final String INVALID_QUANTITY = "INVALID_QUANTITY";
        public static final String LOCK_ACQUIRE_FAILED = "LOCK_ACQUIRE_FAILED";
        public static final String CONCURRENT_UPDATE_FAILED = "CONCURRENT_UPDATE_FAILED";
        public static final String SYSTEM_ERROR = "SYSTEM_ERROR";
    }
}
