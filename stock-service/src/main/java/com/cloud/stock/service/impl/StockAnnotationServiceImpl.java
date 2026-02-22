package com.cloud.stock.service.impl;

import com.cloud.common.annotation.DistributedLock;
import com.cloud.common.domain.dto.stock.StockDTO;
import com.cloud.stock.converter.StockConverter;
import com.cloud.stock.mapper.StockMapper;
import com.cloud.stock.module.entity.Stock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;









@Slf4j
@Service
@RequiredArgsConstructor
public class StockAnnotationServiceImpl {

    private final StockMapper stockMapper;
    private final StockConverter stockConverter;

    







    @DistributedLock(
            key = "'stock:product:' + #productId",
            waitTime = 5,
            leaseTime = 10,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "搴撳瓨鍑哄簱鎿嶄綔鑾峰彇閿佸け璐?
    )
    @Transactional
    public boolean stockOutWithAnnotation(Long productId, Integer quantity) {
        

        
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        
        int affectedRows = stockMapper.stockOutWithCondition(productId, quantity);

        boolean success = affectedRows > 0;
        


        return success;
    }

    







    @DistributedLock(
            key = "'stock:reserve:' + #productId",
            lockType = DistributedLock.LockType.FAIR,
            waitTime = 3,
            leaseTime = 15,
            timeUnit = TimeUnit.SECONDS,
            failStrategy = DistributedLock.LockFailStrategy.RETURN_DEFAULT,
            failMessage = "搴撳瓨棰勭暀鎿嶄綔鑾峰彇鍏钩閿佸け璐?
    )
    @Transactional
    public boolean reserveStockWithAnnotation(Long productId, Integer quantity) {
        

        
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        
        int affectedRows = stockMapper.reserveStockWithCondition(productId, quantity);

        boolean success = affectedRows > 0;
        


        return success;
    }

    






    @DistributedLock(
            key = "'stock:query:' + #productId",
            lockType = DistributedLock.LockType.READ,
            waitTime = 2,
            leaseTime = 5,
            timeUnit = TimeUnit.SECONDS,
            failStrategy = DistributedLock.LockFailStrategy.RETURN_NULL
    )
    @Transactional(readOnly = true)
    public StockDTO getStockWithAnnotation(Long productId) {
        

        
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Stock stock = stockMapper.selectByProductIdForUpdate(productId);
        if (stock == null) {
            log.warn("鈿狅笍 搴撳瓨淇℃伅涓嶅瓨鍦?- 鍟嗗搧ID: {}", productId);
            return null;
        }

        StockDTO stockDTO = stockConverter.toDTO(stock);
        


        return stockDTO;
    }

    







    @DistributedLock(
            key = "'stock:update:' + #productId",
            lockType = DistributedLock.LockType.WRITE,
            waitTime = 5,
            leaseTime = 20,
            timeUnit = TimeUnit.SECONDS,
            failStrategy = DistributedLock.LockFailStrategy.FAIL_FAST
    )
    @Transactional
    public boolean updateStockWithAnnotation(Long productId, Integer newQuantity) {
        

        
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        
        Stock stock = stockMapper.selectByProductIdForUpdate(productId);
        if (stock == null) {
            log.warn("鈿狅笍 搴撳瓨淇℃伅涓嶅瓨鍦?- 鍟嗗搧ID: {}", productId);
            return false;
        }

        
        stock.setStockQuantity(newQuantity);
        int affectedRows = stockMapper.updateById(stock);

        boolean success = affectedRows > 0;
        


        return success;
    }

    







    @DistributedLock(
            key = "'stock:batch:' + #operation + ':' + T(String).join(',', #productIds)",
            prefix = "batch",
            waitTime = 10,
            leaseTime = 30,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "鎵归噺搴撳瓨鎿嶄綔鑾峰彇閿佸け璐?
    )
    @Transactional
    public int batchStockOperationWithAnnotation(java.util.List<Long> productIds, String operation) {
        

        int processedCount = 0;

        for (Long productId : productIds) {
            try {
                
                Thread.sleep(10);

                Stock stock = stockMapper.selectByProductIdForUpdate(productId);
                if (stock != null) {
                    
                    switch (operation) {
                        case "refresh" -> {
                            
                            stockMapper.updateById(stock);
                            processedCount++;
                        }
                        case "check" -> {
                            
                            if (stock.getStockQuantity() > 0) {
                                processedCount++;
                            }
                        }
                        default -> log.warn("鈿狅笍 鏈煡鎿嶄綔绫诲瀷: {}", operation);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("鉂?澶勭悊鍟嗗搧搴撳瓨寮傚父 - 鍟嗗搧ID: {}", productId, e);
            }
        }

        


        return processedCount;
    }

    





    @DistributedLock(
            key = "'stock:safe:' + #productId",
            waitTime = 1,
            leaseTime = 5,
            timeUnit = TimeUnit.SECONDS,
            failStrategy = DistributedLock.LockFailStrategy.RETURN_NULL,
            failMessage = "蹇€熸煡璇㈠簱瀛樿幏鍙栭攣澶辫触"
    )
    public StockDTO quickGetStockWithAnnotation(Long productId) {
        

        Stock stock = stockMapper.selectById(productId);
        return stock != null ? stockConverter.toDTO(stock) : null;
    }
}
