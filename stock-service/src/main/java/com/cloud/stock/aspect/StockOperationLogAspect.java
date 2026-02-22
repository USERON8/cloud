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

/**
 * 库存操作日志AOP切面
 * 自动记录所有库存变更操作
 *
 * @author what's up
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class StockOperationLogAspect {

    private final StockLogService stockLogService;
    private final StockService stockService;

    /**
     * 切入点: 库存入库、出库、预留、释放等操作
     */
    @Pointcut("execution(* com.cloud.stock.service.StockService.stockIn(..)) || " +
            "execution(* com.cloud.stock.service.StockService.stockOut(..)) || " +
            "execution(* com.cloud.stock.service.StockService.reserveStock(..)) || " +
            "execution(* com.cloud.stock.service.StockService.releaseStock(..)) || " +
            "execution(* com.cloud.stock.service.StockService.seckillReserveStock(..))")
    public void stockOperationPointcut() {
    }

    /**
     * 环绕通知: 记录库存操作前后的数量变化
     */
    @Around("stockOperationPointcut()")
    public Object logStockOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        // 获取商品ID (第一个参数)
        if (args.length == 0 || !(args[0] instanceof Long)) {
            return joinPoint.proceed();
        }

        Long productId = (Long) args[0];
        Integer quantity = args.length > 1 && args[1] instanceof Integer ? (Integer) args[1] : null;

        // 获取操作前的库存信息
        StockDTO stockBefore = null;
        try {
            stockBefore = stockService.getStockByProductId(productId);
        } catch (Exception e) {
            log.warn("获取操作前库存信息失败, productId: {}", productId, e);
        }

        // 执行实际的库存操作
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Exception e) {
            // 操作失败也记录日志
            if (stockBefore != null) {
                logOperation(productId, stockBefore.getProductName(), methodName,
                        stockBefore.getStockQuantity(), stockBefore.getStockQuantity(),
                        null, null, "操作失败: " + e.getMessage());
            }
            throw e;
        }

        // 获取操作后的库存信息并记录日志
        try {
            StockDTO stockAfter = stockService.getStockByProductId(productId);
            if (stockBefore != null && stockAfter != null) {
                logOperation(productId, stockAfter.getProductName(), methodName,
                        stockBefore.getStockQuantity(), stockAfter.getStockQuantity(),
                        null, null, String.format("数量: %d", quantity != null ? quantity : 0));
            }
        } catch (Exception e) {
            log.warn("记录库存操作日志失败, productId: {}, method: {}", productId, methodName, e);
        }

        return result;
    }

    /**
     * 记录操作日志
     */
    private void logOperation(Long productId, String productName, String methodName,
                              Integer quantityBefore, Integer quantityAfter,
                              Long orderId, String orderNo, String remark) {
        // 将方法名转换为操作类型
        String operationType = convertMethodToOperationType(methodName);

        try {
            stockLogService.logStockChange(productId, productName, operationType,
                    quantityBefore, quantityAfter, orderId, orderNo, remark);
        } catch (Exception e) {
            log.error("记录库存操作日志失败", e);
        }
    }

    /**
     * 将方法名转换为操作类型
     */
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
