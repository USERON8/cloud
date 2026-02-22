package com.cloud.stock.aspect;

import com.cloud.common.domain.dto.stock.StockDTO;
import com.cloud.stock.service.StockLogService;
import com.cloud.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;







@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class StockOperationLogAspect {

    private final StockLogService stockLogService;
    private final StockService stockService;

    


    @Pointcut("execution(* com.cloud.stock.service.StockService.stockIn(..)) || " +
            "execution(* com.cloud.stock.service.StockService.stockOut(..)) || " +
            "execution(* com.cloud.stock.service.StockService.reserveStock(..)) || " +
            "execution(* com.cloud.stock.service.StockService.releaseStock(..)) || " +
            "execution(* com.cloud.stock.service.StockService.seckillReserveStock(..))")
    public void stockOperationPointcut() {
    }

    


    @Around("stockOperationPointcut()")
    public Object logStockOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        
        if (args.length == 0 || !(args[0] instanceof Long)) {
            return joinPoint.proceed();
        }

        Long productId = (Long) args[0];
        Integer quantity = args.length > 1 && args[1] instanceof Integer ? (Integer) args[1] : null;

        
        StockDTO stockBefore = null;
        try {
            stockBefore = stockService.getStockByProductId(productId);
        } catch (Exception e) {
            log.warn("鑾峰彇鎿嶄綔鍓嶅簱瀛樹俊鎭け璐? productId: {}", productId, e);
        }

        
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Exception e) {
            
            if (stockBefore != null) {
                logOperation(productId, stockBefore.getProductName(), methodName,
                        stockBefore.getStockQuantity(), stockBefore.getStockQuantity(),
                        null, null, "鎿嶄綔澶辫触: " + e.getMessage());
            }
            throw e;
        }

        
        try {
            StockDTO stockAfter = stockService.getStockByProductId(productId);
            if (stockBefore != null && stockAfter != null) {
                logOperation(productId, stockAfter.getProductName(), methodName,
                        stockBefore.getStockQuantity(), stockAfter.getStockQuantity(),
                        null, null, String.format("鏁伴噺: %d", quantity != null ? quantity : 0));
            }
        } catch (Exception e) {
            log.warn("璁板綍搴撳瓨鎿嶄綔鏃ュ織澶辫触, productId: {}, method: {}", productId, methodName, e);
        }

        return result;
    }

    


    private void logOperation(Long productId, String productName, String methodName,
                              Integer quantityBefore, Integer quantityAfter,
                              Long orderId, String orderNo, String remark) {
        
        String operationType = convertMethodToOperationType(methodName);

        try {
            stockLogService.logStockChange(productId, productName, operationType,
                    quantityBefore, quantityAfter, orderId, orderNo, remark);
        } catch (Exception e) {
            log.error("璁板綍搴撳瓨鎿嶄綔鏃ュ織澶辫触", e);
        }
    }

    


    private String convertMethodToOperationType(String methodName) {
        switch (methodName) {
            case "stockIn":
                return "IN";
            case "stockOut":
                return "OUT";
            case "reserveStock":
            case "seckillReserveStock":
                return "RESERVE";
            case "releaseStock":
                return "RELEASE";
            default:
                return "UNKNOWN";
        }
    }
}
