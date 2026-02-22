package com.cloud.stock.service;

import com.cloud.stock.module.entity.StockCount;

import java.time.LocalDateTime;
import java.util.List;






public interface StockCountService {
    









    Long createStockCount(Long productId, Integer actualQuantity,
                          Long operatorId, String operatorName, String remark);

    







    boolean confirmStockCount(Long countId, Long confirmUserId, String confirmUserName);

    





    boolean cancelStockCount(Long countId);

    





    StockCount getStockCountById(Long countId);

    





    StockCount getStockCountByNo(String countNo);

    







    List<StockCount> getStockCountsByProductId(Long productId, LocalDateTime startTime, LocalDateTime endTime);

    







    List<StockCount> getStockCountsByStatus(String status, LocalDateTime startTime, LocalDateTime endTime);

    




    int countPendingRecords();

    




    String generateCountNo();
}
