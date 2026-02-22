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
            throw new BusinessException("鍟嗗搧ID涓嶈兘涓虹┖");
        }
        if (actualQuantity == null || actualQuantity < 0) {
            throw new BusinessException("鐩樼偣鏁伴噺鏃犳晥");
        }

        
        LambdaQueryWrapper<Stock> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Stock::getProductId, productId);
        Stock stock = stockMapper.selectOne(queryWrapper);
        if (stock == null) {
            throw new BusinessException("鍟嗗搧搴撳瓨涓嶅瓨鍦?);
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
            throw new BusinessException("鐩樼偣璁板綍涓嶅瓨鍦?);
        }

        
        if (!"PENDING".equals(stockCount.getStatus())) {
            throw new BusinessException("鐩樼偣璁板綍鐘舵€佷笉鏄緟纭锛屾棤娉曠‘璁?);
        }

        
        LambdaQueryWrapper<Stock> queryWrapper2 = new LambdaQueryWrapper<>();
        queryWrapper2.eq(Stock::getProductId, stockCount.getProductId());
        Stock stock = stockMapper.selectOne(queryWrapper2);
        if (stock == null) {
            throw new BusinessException("鍟嗗搧搴撳瓨涓嶅瓨鍦?);
        }

        Integer quantityBefore = stock.getStockQuantity();

        
        if (stockCount.getDifference() != 0) {
            


            
            stock.setStockQuantity(stockCount.getActualQuantity());
            boolean updateSuccess = stockService.updateById(stock);

            if (!updateSuccess) {
                throw new BusinessException("璋冩暣搴撳瓨澶辫触");
            }

            
            stockLogService.logStockChange(
                    stock.getProductId(),
                    stock.getProductName(),
                    "ADJUST",
                    quantityBefore,
                    stockCount.getActualQuantity(),
                    null,
                    null,
                    String.format("鐩樼偣璋冩暣, 鐩樼偣鍗曞彿: %s, %s",
                            stockCount.getCountNo(),
                            stockCount.getCountType())
            );
        }

        
        stockCount.setStatus("CONFIRMED");
        stockCount.setConfirmUserId(confirmUserId);
        stockCount.setConfirmUserName(confirmUserName);
        stockCount.setConfirmTime(LocalDateTime.now());

        boolean success = updateById(stockCount);

        

        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelStockCount(Long countId) {
        

        
        StockCount stockCount = getById(countId);
        if (stockCount == null) {
            throw new BusinessException("鐩樼偣璁板綍涓嶅瓨鍦?);
        }

        
        if (!"PENDING".equals(stockCount.getStatus())) {
            throw new BusinessException("鍙兘鍙栨秷寰呯‘璁ょ殑鐩樼偣璁板綍");
        }

        
        stockCount.setStatus("CANCELLED");
        boolean success = updateById(stockCount);

        

        return success;
    }

    @Override
    public StockCount getStockCountById(Long countId) {
        return getById(countId);
    }

    @Override
    public StockCount getStockCountByNo(String countNo) {
        return stockCountMapper.selectByCountNo(countNo);
    }

    @Override
    public List<StockCount> getStockCountsByProductId(Long productId, LocalDateTime startTime, LocalDateTime endTime) {
        
        return stockCountMapper.selectByProductId(productId, startTime, endTime);
    }

    @Override
    public List<StockCount> getStockCountsByStatus(String status, LocalDateTime startTime, LocalDateTime endTime) {
        
        return stockCountMapper.selectByStatus(status, startTime, endTime);
    }

    @Override
    public int countPendingRecords() {
        return stockCountMapper.countPendingRecords();
    }

    @Override
    public String generateCountNo() {
        
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long sequence = COUNTER.incrementAndGet() % 1000000;
        return String.format("COUNT%s%06d", datePart, sequence);
    }
}
