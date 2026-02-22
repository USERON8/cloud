package com.cloud.stock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.common.exception.BusinessException;
import com.cloud.stock.mapper.StockCountMapper;
import com.cloud.stock.mapper.StockMapper;
import com.cloud.stock.module.entity.Stock;
import com.cloud.stock.module.entity.StockCount;
import com.cloud.stock.service.StockCountService;
import com.cloud.stock.service.StockLogService;
import com.cloud.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockCountServiceImpl extends ServiceImpl<StockCountMapper, StockCount>
        implements StockCountService {

    private static final AtomicLong COUNTER = new AtomicLong(0);
    private final StockCountMapper stockCountMapper;
    private final StockService stockService;
    private final StockLogService stockLogService;
    private final StockMapper stockMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createStockCount(Long productId, Integer actualQuantity,
                                 Long operatorId, String operatorName, String remark) {
        if (productId == null) {
            throw new BusinessException("productId is required");
        }
        if (actualQuantity == null || actualQuantity < 0) {
            throw new BusinessException("actualQuantity must be >= 0");
        }

        Stock stock = stockMapper.selectOne(new LambdaQueryWrapper<Stock>().eq(Stock::getProductId, productId));
        if (stock == null) {
            throw new BusinessException("Stock not found");
        }

        StockCount stockCount = new StockCount();
        stockCount.setCountNo(generateCountNo());
        stockCount.setProductId(productId);
        stockCount.setProductName(stock.getProductName());
        stockCount.setExpectedQuantity(stock.getStockQuantity());
        stockCount.setActualQuantity(actualQuantity);
        stockCount.setDifference(actualQuantity - stock.getStockQuantity());
        stockCount.setStatus("PENDING");
        stockCount.setOperatorId(operatorId);
        stockCount.setOperatorName(operatorName);
        stockCount.setCountTime(LocalDateTime.now());
        stockCount.setRemark(remark);

        save(stockCount);
        return stockCount.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmStockCount(Long countId, Long confirmUserId, String confirmUserName) {
        StockCount stockCount = getById(countId);
        if (stockCount == null) {
            throw new BusinessException("Stock count not found");
        }
        if (!"PENDING".equals(stockCount.getStatus())) {
            throw new BusinessException("Only pending stock count can be confirmed");
        }

        Stock stock = stockMapper.selectOne(new LambdaQueryWrapper<Stock>()
                .eq(Stock::getProductId, stockCount.getProductId()));
        if (stock == null) {
            throw new BusinessException("Stock not found");
        }

        int beforeQuantity = stock.getStockQuantity();
        if (stockCount.getDifference() != 0) {
            stock.setStockQuantity(stockCount.getActualQuantity());
            boolean updateSuccess = stockService.updateById(stock);
            if (!updateSuccess) {
                throw new BusinessException("Adjust stock quantity failed");
            }

            stockLogService.logStockChange(
                    stock.getProductId(),
                    stock.getProductName(),
                    "ADJUST",
                    beforeQuantity,
                    stockCount.getActualQuantity(),
                    null,
                    null,
                    "Stock count confirmation, countNo=" + stockCount.getCountNo()
            );
        }

        stockCount.setStatus("CONFIRMED");
        stockCount.setConfirmUserId(confirmUserId);
        stockCount.setConfirmUserName(confirmUserName);
        stockCount.setConfirmTime(LocalDateTime.now());
        return updateById(stockCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelStockCount(Long countId) {
        StockCount stockCount = getById(countId);
        if (stockCount == null) {
            throw new BusinessException("Stock count not found");
        }
        if (!"PENDING".equals(stockCount.getStatus())) {
            throw new BusinessException("Only pending stock count can be cancelled");
        }

        stockCount.setStatus("CANCELLED");
        return updateById(stockCount);
    }

    @Override
    public StockCount getStockCountById(Long countId) {
        return getById(countId);
    }

    @Override
    public StockCount getStockCountByNo(String countNo) {
        return getOne(new LambdaQueryWrapper<StockCount>()
                .eq(StockCount::getCountNo, countNo)
                .last("LIMIT 1"));
    }

    @Override
    public List<StockCount> getStockCountsByProductId(Long productId, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<StockCount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StockCount::getProductId, productId);
        if (startTime != null) {
            wrapper.ge(StockCount::getCountTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(StockCount::getCountTime, endTime);
        }
        wrapper.orderByDesc(StockCount::getCountTime);
        return list(wrapper);
    }

    @Override
    public List<StockCount> getStockCountsByStatus(String status, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<StockCount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StockCount::getStatus, status);
        if (startTime != null) {
            wrapper.ge(StockCount::getCountTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(StockCount::getCountTime, endTime);
        }
        wrapper.orderByDesc(StockCount::getCountTime);
        return list(wrapper);
    }

    @Override
    public int countPendingRecords() {
        return Math.toIntExact(count(new LambdaQueryWrapper<StockCount>().eq(StockCount::getStatus, "PENDING")));
    }

    @Override
    public String generateCountNo() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long sequence = COUNTER.incrementAndGet() % 1_000_000;
        return String.format("COUNT%s%06d", datePart, sequence);
    }
}
