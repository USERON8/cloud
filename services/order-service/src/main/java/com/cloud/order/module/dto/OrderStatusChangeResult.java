package com.cloud.order.module.dto;

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
public class OrderStatusChangeResult {

    


    private Boolean success;

    


    private String operationType;

    


    private Long orderId;

    


    private List<Long> orderIds;

    


    private Integer beforeStatus;

    


    private Integer afterStatus;

    


    private Integer affectedCount;

    


    private String errorCode;

    


    private String errorMessage;

    


    private LocalDateTime operationTime;

    


    private Long operatorId;

    


    private String remark;

    


    private Long executionTime;

    


    private Boolean usedDistributedLock;

    


    private Long lockWaitTime;

    










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

    






    public OrderStatusChangeResult withTiming(Long executionTime, Long lockWaitTime) {
        this.executionTime = executionTime;
        this.lockWaitTime = lockWaitTime;
        return this;
    }

    


    public static class OperationType {
        public static final String PAY_ORDER = "PAY_ORDER";
        public static final String SHIP_ORDER = "SHIP_ORDER";
        public static final String COMPLETE_ORDER = "COMPLETE_ORDER";
        public static final String CANCEL_PENDING_ORDER = "CANCEL_PENDING_ORDER";
        public static final String CANCEL_PAID_ORDER = "CANCEL_PAID_ORDER";
        public static final String BATCH_STATUS_CHANGE = "BATCH_STATUS_CHANGE";
    }

    


    public static class ErrorCode {
        public static final String ORDER_NOT_FOUND = "ORDER_NOT_FOUND";
        public static final String INVALID_STATUS_TRANSITION = "INVALID_STATUS_TRANSITION";
        public static final String CONCURRENT_UPDATE_FAILED = "CONCURRENT_UPDATE_FAILED";
        public static final String LOCK_ACQUIRE_FAILED = "LOCK_ACQUIRE_FAILED";
        public static final String INVALID_PARAMETERS = "INVALID_PARAMETERS";
        public static final String SYSTEM_ERROR = "SYSTEM_ERROR";
    }

    


    public static class OrderStatus {
        public static final Integer PENDING = 0;    
        public static final Integer PAID = 1;       
        public static final Integer SHIPPED = 2;    
        public static final Integer COMPLETED = 3;  
        public static final Integer CANCELLED = 4;  
    }
}
