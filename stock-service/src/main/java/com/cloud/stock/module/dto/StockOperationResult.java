package com.cloud.stock.module.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 库存操作结果
 * 封装库存操作的执行结果和相关信息
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockOperationResult {

    /**
     * 操作是否成功
     */
    private Boolean success;

    /**
     * 操作类型
     */
    private String operationType;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 操作数量
     */
    private Integer quantity;

    /**
     * 操作前库存数量
     */
    private Integer beforeStockQuantity;

    /**
     * 操作后库存数量
     */
    private Integer afterStockQuantity;

    /**
     * 操作前冻结数量
     */
    private Integer beforeFrozenQuantity;

    /**
     * 操作后冻结数量
     */
    private Integer afterFrozenQuantity;

    /**
     * 错误码
     */
    private String errorCode;

    /**
     * 错误消息
     */
    private String errorMessage;

    /**
     * 操作时间
     */
    private LocalDateTime operationTime;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 备注
     */
    private String remark;

    /**
     * 执行耗时（毫秒）
     */
    private Long executionTime;

    /**
     * 是否使用了分布式锁
     */
    private Boolean usedDistributedLock;

    /**
     * 锁等待时间（毫秒）
     */
    private Long lockWaitTime;

    /**
     * 创建成功结果
     *
     * @param operationType 操作类型
     * @param productId     商品ID
     * @param quantity      操作数量
     * @param beforeStock   操作前库存
     * @param afterStock    操作后库存
     * @param beforeFrozen  操作前冻结数量
     * @param afterFrozen   操作后冻结数量
     * @param operatorId    操作人ID
     * @param remark        备注
     * @return 操作结果
     */
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

    /**
     * 创建失败结果
     *
     * @param operationType 操作类型
     * @param productId     商品ID
     * @param quantity      操作数量
     * @param errorCode     错误码
     * @param errorMessage  错误消息
     * @param operatorId    操作人ID
     * @return 操作结果
     */
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

    /**
     * 设置执行时间信息
     *
     * @param executionTime 执行耗时
     * @param lockWaitTime  锁等待时间
     * @return 当前对象
     */
    public StockOperationResult withTiming(Long executionTime, Long lockWaitTime) {
        this.executionTime = executionTime;
        this.lockWaitTime = lockWaitTime;
        return this;
    }

    /**
     * 获取可用库存数量（操作前）
     *
     * @return 可用库存数量
     */
    public Integer getBeforeAvailableQuantity() {
        if (beforeStockQuantity == null || beforeFrozenQuantity == null) {
            return null;
        }
        return beforeStockQuantity - beforeFrozenQuantity;
    }

    /**
     * 获取可用库存数量（操作后）
     *
     * @return 可用库存数量
     */
    public Integer getAfterAvailableQuantity() {
        if (afterStockQuantity == null || afterFrozenQuantity == null) {
            return null;
        }
        return afterStockQuantity - afterFrozenQuantity;
    }

    /**
     * 操作类型常量
     */
    public static class OperationType {
        public static final String STOCK_OUT = "STOCK_OUT";
        public static final String STOCK_IN = "STOCK_IN";
        public static final String RESERVE = "RESERVE";
        public static final String RELEASE_RESERVE = "RELEASE_RESERVE";
        public static final String CONFIRM_OUT = "CONFIRM_OUT";
    }

    /**
     * 错误码常量
     */
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
