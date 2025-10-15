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

/**
 * 库存盘点服务实现
 *
 * @author what's up
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockCountServiceImpl extends ServiceImpl<StockCountMapper, StockCount>
        implements StockCountService {

    private final StockCountMapper stockCountMapper;
    private final StockService stockService;
    private final StockLogService stockLogService;
    private final StockMapper stockMapper;

    private static final AtomicLong COUNTER = new AtomicLong(0);

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createStockCount(Long productId, Integer actualQuantity,
                                  Long operatorId, String operatorName, String remark) {
        log.info("创建库存盘点记录, productId: {}, actualQuantity: {}, operatorId: {}",
                productId, actualQuantity, operatorId);

        // 参数校验
        if (productId == null) {
            throw new BusinessException("商品ID不能为空");
        }
        if (actualQuantity == null || actualQuantity < 0) {
            throw new BusinessException("盘点数量无效");
        }

        // 获取当前库存信息
        LambdaQueryWrapper<Stock> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Stock::getProductId, productId);
        Stock stock = stockMapper.selectOne(queryWrapper);
        if (stock == null) {
            throw new BusinessException("商品库存不存在");
        }

        // 创建盘点记录
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

        log.info("库存盘点记录创建成功, countId: {}, countNo: {}, 预期: {}, 实际: {}, 差异: {}",
                stockCount.getId(), stockCount.getCountNo(),
                stockCount.getExpectedQuantity(), stockCount.getActualQuantity(),
                stockCount.getDifference());

        return stockCount.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmStockCount(Long countId, Long confirmUserId, String confirmUserName) {
        log.info("确认库存盘点, countId: {}, confirmUserId: {}", countId, confirmUserId);

        // 查询盘点记录
        StockCount stockCount = getById(countId);
        if (stockCount == null) {
            throw new BusinessException("盘点记录不存在");
        }

        // 检查状态
        if (!"PENDING".equals(stockCount.getStatus())) {
            throw new BusinessException("盘点记录状态不是待确认，无法确认");
        }

        // 获取当前库存
        LambdaQueryWrapper<Stock> queryWrapper2 = new LambdaQueryWrapper<>();
        queryWrapper2.eq(Stock::getProductId, stockCount.getProductId());
        Stock stock = stockMapper.selectOne(queryWrapper2);
        if (stock == null) {
            throw new BusinessException("商品库存不存在");
        }

        Integer quantityBefore = stock.getStockQuantity();

        // 如果有差异，调整库存
        if (stockCount.getDifference() != 0) {
            log.info("盘点发现差异 {}, 调整库存: {} -> {}",
                    stockCount.getDifference(), quantityBefore, stockCount.getActualQuantity());

            // 直接设置为实际盘点数量
            stock.setStockQuantity(stockCount.getActualQuantity());
            boolean updateSuccess = stockService.updateById(stock);

            if (!updateSuccess) {
                throw new BusinessException("调整库存失败");
            }

            // 记录库存调整日志
            stockLogService.logStockChange(
                    stock.getProductId(),
                    stock.getProductName(),
                    "ADJUST",
                    quantityBefore,
                    stockCount.getActualQuantity(),
                    null,
                    null,
                    String.format("盘点调整, 盘点单号: %s, %s",
                            stockCount.getCountNo(),
                            stockCount.getCountType())
            );
        }

        // 更新盘点记录状态
        stockCount.setStatus("CONFIRMED");
        stockCount.setConfirmUserId(confirmUserId);
        stockCount.setConfirmUserName(confirmUserName);
        stockCount.setConfirmTime(LocalDateTime.now());

        boolean success = updateById(stockCount);

        log.info("库存盘点确认完成, countId: {}, 类型: {}", countId, stockCount.getCountType());

        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelStockCount(Long countId) {
        log.info("取消库存盘点, countId: {}", countId);

        // 查询盘点记录
        StockCount stockCount = getById(countId);
        if (stockCount == null) {
            throw new BusinessException("盘点记录不存在");
        }

        // 检查状态
        if (!"PENDING".equals(stockCount.getStatus())) {
            throw new BusinessException("只能取消待确认的盘点记录");
        }

        // 更新状态为已取消
        stockCount.setStatus("CANCELLED");
        boolean success = updateById(stockCount);

        log.info("库存盘点已取消, countId: {}", countId);

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
        log.info("根据商品ID查询盘点记录, productId: {}, 时间范围: {} ~ {}", productId, startTime, endTime);
        return stockCountMapper.selectByProductId(productId, startTime, endTime);
    }

    @Override
    public List<StockCount> getStockCountsByStatus(String status, LocalDateTime startTime, LocalDateTime endTime) {
        log.info("根据状态查询盘点记录, status: {}, 时间范围: {} ~ {}", status, startTime, endTime);
        return stockCountMapper.selectByStatus(status, startTime, endTime);
    }

    @Override
    public int countPendingRecords() {
        return stockCountMapper.countPendingRecords();
    }

    @Override
    public String generateCountNo() {
        // 格式: COUNT + 日期(yyyyMMdd) + 6位递增序号
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long sequence = COUNTER.incrementAndGet() % 1000000;
        return String.format("COUNT%s%06d", datePart, sequence);
    }
}
