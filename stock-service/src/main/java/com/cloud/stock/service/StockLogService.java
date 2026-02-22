package com.cloud.stock.service;

import com.cloud.stock.module.entity.StockLog;

import java.time.LocalDateTime;
import java.util.List;






public interface StockLogService {
    





    Long createLog(StockLog stockLog);

    





    int batchCreateLogs(List<StockLog> stockLogs);

    







    List<StockLog> getLogsByProductId(Long productId, LocalDateTime startTime, LocalDateTime endTime);

    





    List<StockLog> getLogsByOrderId(Long orderId);

    







    List<StockLog> getLogsByOperationType(String operationType, LocalDateTime startTime, LocalDateTime endTime);

    











    void logStockChange(Long productId, String productName, String operationType,
                        Integer quantityBefore, Integer quantityAfter,
                        Long orderId, String orderNo, String remark);
}
