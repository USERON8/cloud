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
            "execution(* com.cloud.stock.service.StockService.releaseReservedStock(..)) || " +
            "execution(* com.cloud.stock.service.StockService.confirmReservedStockOut(..))")
    public void stockOperationPointcut() {
    }

    @Around("stockOperationPointcut()")
    public Object logStockOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        if (args.length == 0 || !(args[0] instanceof Long productId)) {
            return joinPoint.proceed();
        }

        Integer quantity = args.length > 1 && args[1] instanceof Integer q ? q : null;

        StockDTO stockBefore = null;
        try {
            stockBefore = stockService.getStockByProductId(productId);
        } catch (Exception e) {
            log.warn("Failed to read stock before operation, productId={}", productId, e);
        }

        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Exception e) {
            if (stockBefore != null) {
                logOperation(
                        productId,
                        stockBefore.getProductName(),
                        methodName,
                        stockBefore.getStockQuantity(),
                        stockBefore.getStockQuantity(),
                        null,
                        null,
                        "Operation failed: " + e.getMessage()
                );
            }
            throw e;
        }

        try {
            StockDTO stockAfter = stockService.getStockByProductId(productId);
            if (stockBefore != null && stockAfter != null) {
                logOperation(
                        productId,
                        stockAfter.getProductName(),
                        methodName,
                        stockBefore.getStockQuantity(),
                        stockAfter.getStockQuantity(),
                        null,
                        null,
                        String.format("Quantity: %d", quantity != null ? quantity : 0)
                );
            }
        } catch (Exception e) {
            log.warn("Failed to record stock operation log, productId={}, method={}", productId, methodName, e);
        }

        return result;
    }

    private void logOperation(Long productId, String productName, String methodName,
                              Integer quantityBefore, Integer quantityAfter,
                              Long orderId, String orderNo, String remark) {
        String operationType = convertMethodToOperationType(methodName);
        try {
            stockLogService.logStockChange(
                    productId,
                    productName,
                    operationType,
                    quantityBefore,
                    quantityAfter,
                    orderId,
                    orderNo,
                    remark
            );
        } catch (Exception e) {
            log.error("Failed to persist stock operation log", e);
        }
    }

    private String convertMethodToOperationType(String methodName) {
        return switch (methodName) {
            case "stockIn" -> "IN";
            case "stockOut" -> "OUT";
            case "reserveStock" -> "RESERVE";
            case "releaseReservedStock" -> "RELEASE";
            case "confirmReservedStockOut" -> "CONFIRM_OUT";
            default -> "UNKNOWN";
        };
    }
}
