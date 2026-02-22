package com.cloud.stock.service;

import com.cloud.stock.module.entity.Stock;

import java.util.List;






public interface StockAlertService {
    




    List<Stock> getLowStockProducts();

    





    List<Stock> getLowStockProductsByThreshold(Integer threshold);

    






    boolean updateLowStockThreshold(Long productId, Integer threshold);

    






    int batchUpdateLowStockThreshold(List<Long> productIds, Integer threshold);

    





    int checkAndSendLowStockAlerts();

    




    void sendLowStockAlert(Stock stock);

    




    void batchSendLowStockAlerts(List<Stock> stocks);
}
