package com.cloud.order.module.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单状态变更结果
 * 封装订单状态变更操作的执行结果和相关信息
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusChangeResult {

    /**
     * 操作是否成功
     */
    private Boolean success;

    /**
     * 操作类型
     */
    private String operationType;

    /**
     * 订单ID（单个订单操作）
     */
    private Long orderId;

    /**
     * 订单ID列表（批量操作）
     */
    private List<Long> orderIds;

    /**
     * 操作前状态
     */
    private Integer beforeStatus;

    /**
     * 操作后状态
     */
    private Integer afterStatus;

    /**
     * 影响的订单数量
     */
    private Integer affectedCount;

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
     * 创建成功结果（单个订单）
     *
     * @param operationType 操作类型
     * @param orderId       订单ID
     * @param beforeStatus  操作前状态
     * @param afterStatus   操作后状态
     * @param operatorId    操作人ID
     * @param remark        备注
     * @return 操作结果
     */
    public static OrderStatusChangeResult success(String operationType, Long orderId,
                                                  Integer beforeStatus, Integer afterStatus,
                                                  Long operatorId, String remark) {
        return OrderStatusChangeResult.builder()
                .success(true)
                .operationType(operationType)
                .orderId(orderId)
                .beforeStatus(beforeStatus)
                .afterStatus(afterStatus)
                .affectedCount(1)
                .operationTime(LocalDateTime.now())
                .operatorId(operatorId)
                .remark(remark)
                .usedDistributedLock(true)
                .build();
    }

    /**
     * 创建成功结果（批量订单）
     *
     * @param operationType 操作类型
     * @param orderIds      订单ID列表
     * @param beforeStatus  操作前状态
     * @param afterStatus   操作后状态
     * @param affectedCount 影响数量
     * @param operatorId    操作人ID
     * @param remark        备注
     * @return 操作结果
     */
    public static OrderStatusChangeResult batchSuccess(String operationType, List<Long> orderIds,
                                                       Integer beforeStatus, Integer afterStatus,
                                                       Integer affectedCount, Long operatorId, String remark) {
        return OrderStatusChangeResult.builder()
                .success(true)
                .operationType(operationType)
                .orderIds(orderIds)
                .beforeStatus(beforeStatus)
                .afterStatus(afterStatus)
                .affectedCount(affectedCount)
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
     * @param orderId       订单ID
     * @param errorCode     错误码
     * @param errorMessage  错误消息
     * @param operatorId    操作人ID
     * @return 操作结果
     */
    public static OrderStatusChangeResult failure(String operationType, Long orderId,
                                                  String errorCode, String errorMessage, Long operatorId) {
        return OrderStatusChangeResult.builder()
                .success(false)
                .operationType(operationType)
                .orderId(orderId)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .operationTime(LocalDateTime.now())
                .operatorId(operatorId)
                .usedDistributedLock(true)
                .build();
    }

    /**
     * 创建批量失败结果
     *
     * @param operationType 操作类型
     * @param orderIds      订单ID列表
     * @param errorCode     错误码
     * @param errorMessage  错误消息
     * @param operatorId    操作人ID
     * @return 操作结果
     */
    public static OrderStatusChangeResult batchFailure(String operationType, List<Long> orderIds,
                                                       String errorCode, String errorMessage, Long operatorId) {
        return OrderStatusChangeResult.builder()
                .success(false)
                .operationType(operationType)
                .orderIds(orderIds)
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
    public OrderStatusChangeResult withTiming(Long executionTime, Long lockWaitTime) {
        this.executionTime = executionTime;
        this.lockWaitTime = lockWaitTime;
        return this;
    }

    /**
     * 操作类型常量
     */
    public static class OperationType {
        public static final String PAY_ORDER = "PAY_ORDER";
        public static final String SHIP_ORDER = "SHIP_ORDER";
        public static final String COMPLETE_ORDER = "COMPLETE_ORDER";
        public static final String CANCEL_PENDING_ORDER = "CANCEL_PENDING_ORDER";
        public static final String CANCEL_PAID_ORDER = "CANCEL_PAID_ORDER";
        public static final String BATCH_STATUS_CHANGE = "BATCH_STATUS_CHANGE";
    }

    /**
     * 错误码常量
     */
    public static class ErrorCode {
        public static final String ORDER_NOT_FOUND = "ORDER_NOT_FOUND";
        public static final String INVALID_STATUS_TRANSITION = "INVALID_STATUS_TRANSITION";
        public static final String CONCURRENT_UPDATE_FAILED = "CONCURRENT_UPDATE_FAILED";
        public static final String LOCK_ACQUIRE_FAILED = "LOCK_ACQUIRE_FAILED";
        public static final String INVALID_PARAMETERS = "INVALID_PARAMETERS";
        public static final String SYSTEM_ERROR = "SYSTEM_ERROR";
    }

    /**
     * 订单状态常量
     */
    public static class OrderStatus {
        public static final Integer PENDING = 0;    // 待支付
        public static final Integer PAID = 1;       // 已支付
        public static final Integer SHIPPED = 2;    // 已发货
        public static final Integer COMPLETED = 3;  // 已完成
        public static final Integer CANCELLED = 4;  // 已取消
    }
}
