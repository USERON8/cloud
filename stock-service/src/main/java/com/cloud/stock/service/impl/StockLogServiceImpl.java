package com.cloud.stock.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.stock.mapper.StockLogMapper;
import com.cloud.stock.module.entity.StockLog;
import com.cloud.stock.service.StockLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;






@Slf4j
@Service
@RequiredArgsConstructor
public class StockLogServiceImpl extends ServiceImpl<StockLogMapper, StockLog>
        implements StockLogService {

    private final StockLogMapper stockLogMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createLog(StockLog stockLog) {
        if (stockLog.getOperateTime() == null) {
            stockLog.setOperateTime(LocalDateTime.now());
        }

        
        if (stockLog.getQuantityBefore() != null && stockLog.getQuantityAfter() != null) {
            stockLog.setQuantityChange(stockLog.getQuantityAfter() - stockLog.getQuantityBefore());
        }

        save(stockLog);
        

        return stockLog.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchCreateLogs(List<StockLog> stockLogs) {
        if (stockLogs == null || stockLogs.isEmpty()) {
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();
        stockLogs.forEach(log -> {
            if (log.getOperateTime() == null) {
                log.setOperateTime(now);
            }
            
            if (log.getQuantityBefore() != null && log.getQuantityAfter() != null) {
                log.setQuantityChange(log.getQuantityAfter() - log.getQuantityBefore());
            }
        });

        boolean success = saveBatch(stockLogs);
        
        return success ? stockLogs.size() : 0;
    }

    @Override
    public List<StockLog> getLogsByProductId(Long productId, LocalDateTime startTime, LocalDateTime endTime) {
        
        return stockLogMapper.selectByProductId(productId, startTime, endTime);
    }

    @Override
    public List<StockLog> getLogsByOrderId(Long orderId) {
        
        return stockLogMapper.selectByOrderId(orderId);
    }

    @Override
    public List<StockLog> getLogsByOperationType(String operationType, LocalDateTime startTime, LocalDateTime endTime) {
        
        return stockLogMapper.selectByOperationType(operationType, startTime, endTime);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void logStockChange(Long productId, String productName, String operationType,
                               Integer quantityBefore, Integer quantityAfter,
                               Long orderId, String orderNo, String remark) {
        StockLog stockLog = new StockLog();
        stockLog.setProductId(productId);
        stockLog.setProductName(productName);
        stockLog.setOperationType(operationType);
        stockLog.setQuantityBefore(quantityBefore);
        stockLog.setQuantityAfter(quantityAfter);
        stockLog.setQuantityChange(quantityAfter - quantityBefore);
        stockLog.setOrderId(orderId);
        stockLog.setOrderNo(orderNo);
        stockLog.setRemark(remark);
        stockLog.setOperateTime(LocalDateTime.now());

        save(stockLog);
        log.debug("璁板綍搴撳瓨鍙樻洿: productId={}, type={}, {}鈫抺}",
                productId, operationType, quantityBefore, quantityAfter);
    }
}
