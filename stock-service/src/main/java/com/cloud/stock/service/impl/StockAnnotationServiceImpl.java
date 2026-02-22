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

import java.util.List;
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
            failMessage = "Acquire stock out lock failed"
    )
    @Transactional(rollbackFor = Exception.class)
    public boolean stockOutWithAnnotation(Long productId, Integer quantity) {
        return stockMapper.stockOutWithCondition(productId, quantity) > 0;
    }

    @DistributedLock(
            key = "'stock:reserve:' + #productId",
            lockType = DistributedLock.LockType.FAIR,
            waitTime = 3,
            leaseTime = 15,
            timeUnit = TimeUnit.SECONDS,
            failStrategy = DistributedLock.LockFailStrategy.RETURN_DEFAULT,
            failMessage = "Acquire reserve lock failed"
    )
    @Transactional(rollbackFor = Exception.class)
    public boolean reserveStockWithAnnotation(Long productId, Integer quantity) {
        return stockMapper.reserveStockWithCondition(productId, quantity) > 0;
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
        Stock stock = stockMapper.selectByProductIdForUpdate(productId);
        return stock == null ? null : stockConverter.toDTO(stock);
    }

    @DistributedLock(
            key = "'stock:update:' + #productId",
            lockType = DistributedLock.LockType.WRITE,
            waitTime = 5,
            leaseTime = 20,
            timeUnit = TimeUnit.SECONDS,
            failStrategy = DistributedLock.LockFailStrategy.FAIL_FAST
    )
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStockWithAnnotation(Long productId, Integer newQuantity) {
        Stock stock = stockMapper.selectByProductIdForUpdate(productId);
        if (stock == null) {
            return false;
        }
        stock.setStockQuantity(newQuantity);
        return stockMapper.updateById(stock) > 0;
    }

    @DistributedLock(
            key = "'stock:batch:' + #operation + ':' + T(String).join(',', #productIds)",
            prefix = "batch",
            waitTime = 10,
            leaseTime = 30,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "Acquire batch stock operation lock failed"
    )
    @Transactional(rollbackFor = Exception.class)
    public int batchStockOperationWithAnnotation(List<Long> productIds, String operation) {
        if (productIds == null || productIds.isEmpty()) {
            return 0;
        }

        int processedCount = 0;
        for (Long productId : productIds) {
            Stock stock = stockMapper.selectByProductIdForUpdate(productId);
            if (stock == null) {
                continue;
            }

            switch (operation) {
                case "refresh" -> {
                    stockMapper.updateById(stock);
                    processedCount++;
                }
                case "check" -> {
                    if (stock.getStockQuantity() != null && stock.getStockQuantity() > 0) {
                        processedCount++;
                    }
                }
                default -> log.warn("Unknown operation in batchStockOperationWithAnnotation: {}", operation);
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
            failMessage = "Quick stock query lock failed"
    )
    public StockDTO quickGetStockWithAnnotation(Long productId) {
        Stock stock = stockMapper.selectByProductIdForUpdate(productId);
        return stock == null ? null : stockConverter.toDTO(stock);
    }
}
