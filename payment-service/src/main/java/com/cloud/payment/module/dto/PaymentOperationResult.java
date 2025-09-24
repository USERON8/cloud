package com.cloud.payment.module.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付操作结果
 * 封装支付操作的执行结果和相关信息
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOperationResult {

    /**
     * 操作是否成功
     */
    private Boolean success;

    /**
     * 操作类型
     */
    private String operationType;

    /**
     * 支付ID
     */
    private Long paymentId;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 支付金额
     */
    private BigDecimal amount;

    /**
     * 操作前状态
     */
    private Integer beforeStatus;

    /**
     * 操作后状态
     */
    private Integer afterStatus;

    /**
     * 第三方流水号
     */
    private String transactionId;

    /**
     * 跟踪ID（幂等标识）
     */
    private String traceId;

    /**
     * 是否为幂等重复请求
     */
    private Boolean isIdempotentDuplicate;

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
     * @param paymentId     支付ID
     * @param orderId       订单ID
     * @param userId        用户ID
     * @param amount        支付金额
     * @param beforeStatus  操作前状态
     * @param afterStatus   操作后状态
     * @param transactionId 第三方流水号
     * @param traceId       跟踪ID
     * @param operatorId    操作人ID
     * @param remark        备注
     * @return 操作结果
     */
    public static PaymentOperationResult success(String operationType, Long paymentId, Long orderId, Long userId,
                                                 BigDecimal amount, Integer beforeStatus, Integer afterStatus,
                                                 String transactionId, String traceId, Long operatorId, String remark) {
        return PaymentOperationResult.builder()
                .success(true)
                .operationType(operationType)
                .paymentId(paymentId)
                .orderId(orderId)
                .userId(userId)
                .amount(amount)
                .beforeStatus(beforeStatus)
                .afterStatus(afterStatus)
                .transactionId(transactionId)
                .traceId(traceId)
                .isIdempotentDuplicate(false)
                .operationTime(LocalDateTime.now())
                .operatorId(operatorId)
                .remark(remark)
                .usedDistributedLock(true)
                .build();
    }

    /**
     * 创建幂等重复结果
     *
     * @param operationType 操作类型
     * @param paymentId     支付ID
     * @param orderId       订单ID
     * @param userId        用户ID
     * @param amount        支付金额
     * @param status        当前状态
     * @param transactionId 第三方流水号
     * @param traceId       跟踪ID
     * @param operatorId    操作人ID
     * @return 操作结果
     */
    public static PaymentOperationResult idempotentDuplicate(String operationType, Long paymentId, Long orderId,
                                                             Long userId, BigDecimal amount, Integer status,
                                                             String transactionId, String traceId, Long operatorId) {
        return PaymentOperationResult.builder()
                .success(true)
                .operationType(operationType)
                .paymentId(paymentId)
                .orderId(orderId)
                .userId(userId)
                .amount(amount)
                .beforeStatus(status)
                .afterStatus(status)
                .transactionId(transactionId)
                .traceId(traceId)
                .isIdempotentDuplicate(true)
                .operationTime(LocalDateTime.now())
                .operatorId(operatorId)
                .remark("幂等重复请求")
                .usedDistributedLock(true)
                .build();
    }

    /**
     * 创建失败结果
     *
     * @param operationType 操作类型
     * @param paymentId     支付ID
     * @param orderId       订单ID
     * @param errorCode     错误码
     * @param errorMessage  错误消息
     * @param operatorId    操作人ID
     * @return 操作结果
     */
    public static PaymentOperationResult failure(String operationType, Long paymentId, Long orderId,
                                                 String errorCode, String errorMessage, Long operatorId) {
        return PaymentOperationResult.builder()
                .success(false)
                .operationType(operationType)
                .paymentId(paymentId)
                .orderId(orderId)
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
    public PaymentOperationResult withTiming(Long executionTime, Long lockWaitTime) {
        this.executionTime = executionTime;
        this.lockWaitTime = lockWaitTime;
        return this;
    }

    /**
     * 操作类型常量
     */
    public static class OperationType {
        public static final String CREATE_PAYMENT = "CREATE_PAYMENT";
        public static final String PAYMENT_SUCCESS = "PAYMENT_SUCCESS";
        public static final String PAYMENT_FAILED = "PAYMENT_FAILED";
        public static final String REFUND_PAYMENT = "REFUND_PAYMENT";
        public static final String RETRY_PAYMENT = "RETRY_PAYMENT";
    }

    /**
     * 错误码常量
     */
    public static class ErrorCode {
        public static final String PAYMENT_NOT_FOUND = "PAYMENT_NOT_FOUND";
        public static final String INVALID_STATUS_TRANSITION = "INVALID_STATUS_TRANSITION";
        public static final String CONCURRENT_UPDATE_FAILED = "CONCURRENT_UPDATE_FAILED";
        public static final String LOCK_ACQUIRE_FAILED = "LOCK_ACQUIRE_FAILED";
        public static final String INVALID_PARAMETERS = "INVALID_PARAMETERS";
        public static final String DUPLICATE_TRACE_ID = "DUPLICATE_TRACE_ID";
        public static final String INSUFFICIENT_BALANCE = "INSUFFICIENT_BALANCE";
        public static final String SYSTEM_ERROR = "SYSTEM_ERROR";
    }

    /**
     * 支付状态常量
     */
    public static class PaymentStatus {
        public static final Integer PENDING = 0;    // 待支付
        public static final Integer SUCCESS = 1;    // 成功
        public static final Integer FAILED = 2;     // 失败
        public static final Integer REFUNDED = 3;   // 已退款
    }

    /**
     * 支付渠道常量
     */
    public static class PaymentChannel {
        public static final Integer ALIPAY = 1;     // 支付宝
        public static final Integer WECHAT = 2;     // 微信
        public static final Integer BANK_CARD = 3;  // 银行卡
    }
}
