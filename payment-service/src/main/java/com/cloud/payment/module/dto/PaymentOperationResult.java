package com.cloud.payment.module.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;









@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOperationResult {

    


    private Boolean success;

    


    private String operationType;

    


    private Long paymentId;

    


    private Long orderId;

    


    private Long userId;

    


    private BigDecimal amount;

    


    private Integer beforeStatus;

    


    private Integer afterStatus;

    


    private String transactionId;

    


    private String traceId;

    


    private Boolean isIdempotentDuplicate;

    


    private String errorCode;

    


    private String errorMessage;

    


    private LocalDateTime operationTime;

    


    private Long operatorId;

    


    private String remark;

    


    private Long executionTime;

    


    private Boolean usedDistributedLock;

    


    private Long lockWaitTime;

    















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
                .remark("骞傜瓑閲嶅璇锋眰")
                .usedDistributedLock(true)
                .build();
    }

    










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

    






    public PaymentOperationResult withTiming(Long executionTime, Long lockWaitTime) {
        this.executionTime = executionTime;
        this.lockWaitTime = lockWaitTime;
        return this;
    }

    


    public static class OperationType {
        public static final String CREATE_PAYMENT = "CREATE_PAYMENT";
        public static final String PAYMENT_SUCCESS = "PAYMENT_SUCCESS";
        public static final String PAYMENT_FAILED = "PAYMENT_FAILED";
        public static final String REFUND_PAYMENT = "REFUND_PAYMENT";
        public static final String RETRY_PAYMENT = "RETRY_PAYMENT";
    }

    


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

    


    public static class PaymentStatus {
        public static final Integer PENDING = 0;    
        public static final Integer SUCCESS = 1;    
        public static final Integer FAILED = 2;     
        public static final Integer REFUNDED = 3;   
    }

    


    public static class PaymentChannel {
        public static final Integer ALIPAY = 1;     
        public static final Integer WECHAT = 2;     
        public static final Integer BANK_CARD = 3;  
    }
}
