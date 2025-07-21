package com.cloud.stock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.stock.constant.StockConstant;
import com.cloud.stock.converter.StockConverter;
import com.cloud.stock.mapper.StockMapper;
import com.cloud.stock.module.dto.StockPageDTO;
import com.cloud.stock.module.entity.Stock;
import com.cloud.stock.module.vo.StockVO;
import com.cloud.stock.service.StockService;
import domain.PageResult;
import eunms.ResultCode;
import exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utils.PageUtils;

/**
 * 库存服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockServiceImpl extends ServiceImpl<StockMapper, Stock> implements StockService {

    private final StockConverter stockConverter;

    @Override
    public PageResult<StockVO> pageStock(StockPageDTO pageDTO) {
        log.info("开始分页查询库存，查询条件：{}", pageDTO);

        // 创建分页对象
        Page<Stock> page = PageUtils.buildPage(pageDTO);

        // 构建查询条件
        LambdaQueryWrapper<Stock> wrapper = buildQueryWrapper(pageDTO);

        // 执行分页查询
        IPage<Stock> stockPage = page(page, wrapper);

        // 使用MapStruct转换为PageResult
        PageResult<StockVO> result = PageUtils.toPageResult(stockPage, stockConverter::toVO);

        log.info("库存分页查询完成，总记录数：{}，当前页：{}，每页大小：{}",
                result.getTotal(), result.getCurrent(), result.getSize());

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deductStock(Long productId, Integer count) {
        log.info("开始扣减库存，商品ID：{}，扣减数量：{}", productId, count);

        // 查询库存
        Stock stock = lambdaQuery()
                .eq(Stock::getProductId, productId)
                .one();

        if (stock == null) {
            throw new BusinessException(ResultCode.STOCK_NOT_FOUND);
        }

        if (stock.getAvailableCount() < count) {
            throw new BusinessException(ResultCode.STOCK_INSUFFICIENT,
                    String.format("库存不足，可用库存：%d，需要扣减：%d", stock.getAvailableCount(), count));
        }

        // 使用乐观锁更新库存
        boolean result = lambdaUpdate()
                .eq(Stock::getProductId, productId)
                .eq(Stock::getVersion, stock.getVersion())
                .set(Stock::getStockCount, stock.getStockCount() - count)
                .set(Stock::getAvailableCount, stock.getAvailableCount() - count)
                .set(Stock::getVersion, stock.getVersion() + 1)
                .update();

        if (!result) {
            throw new BusinessException(ResultCode.STOCK_DEDUCT_FAILED, "库存扣减失败，可能存在并发冲突");
        }

        log.info("库存扣减成功，商品ID：{}", productId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addStock(Long productId, Integer count) {
        log.info("开始增加库存，商品ID：{}，增加数量：{}", productId, count);

        Stock stock = lambdaQuery()
                .eq(Stock::getProductId, productId)
                .one();

        if (stock == null) {
            log.error("商品不存在，商品ID：{}", productId);
            return false;
        }

        // 使用乐观锁更新库存
        boolean result = lambdaUpdate()
                .eq(Stock::getProductId, productId)
                .eq(Stock::getVersion, stock.getVersion())
                .set(Stock::getStockCount, stock.getStockCount() + count)
                .set(Stock::getAvailableCount, stock.getAvailableCount() + count)
                .set(Stock::getVersion, stock.getVersion() + 1)
                .update();

        if (result) {
            log.info("库存增加成功，商品ID：{}", productId);
        } else {
            log.error("库存增加失败，可能存在并发冲突，商品ID：{}", productId);
        }

        return result;
    }

    @Override
    public boolean checkStock(Long productId, Integer count) {
        Stock stock = lambdaQuery()
                .eq(Stock::getProductId, productId)
                .one();

        return stock != null && stock.getAvailableCount() >= count;
    }

    /**
     * 根据商品ID获取库存VO
     */
    @Override
    public StockVO getStockVOByProductId(Long productId) {
        Stock stock = lambdaQuery()
                .eq(Stock::getProductId, productId)
                .one();

        if (stock == null) {
            throw new BusinessException(ResultCode.STOCK_NOT_FOUND);
        }

        return stockConverter.toVO(stock);
    }

    /**
     * 构建查询条件
     */
    private LambdaQueryWrapper<Stock> buildQueryWrapper(StockPageDTO pageDTO) {
        LambdaQueryWrapper<Stock> wrapper = new LambdaQueryWrapper<>();

        // 商品ID精确查询
        wrapper.eq(pageDTO.getProductId() != null, Stock::getProductId, pageDTO.getProductId());

        // 商品名称模糊查询
        wrapper.like(pageDTO.getProductName() != null && !pageDTO.getProductName().trim().isEmpty(),
                Stock::getProductName, pageDTO.getProductName());

        // 可用库存范围查询
        wrapper.ge(pageDTO.getMinAvailableCount() != null,
                Stock::getAvailableCount, pageDTO.getMinAvailableCount());
        wrapper.le(pageDTO.getMaxAvailableCount() != null,
                Stock::getAvailableCount, pageDTO.getMaxAvailableCount());

        // 库存状态查询
        if (pageDTO.getStockStatus() != null) {
            Integer stockStatus = pageDTO.getStockStatus();
            if (StockConstant.Status.OUT_OF_STOCK.equals(stockStatus)) {
                wrapper.le(Stock::getAvailableCount, 0);
            } else if (StockConstant.Status.LOW_STOCK.equals(stockStatus)) {
                wrapper.gt(Stock::getAvailableCount, 0)
                        .lt(Stock::getAvailableCount, StockConstant.Threshold.LOW_STOCK_THRESHOLD);
            } else if (StockConstant.Status.SUFFICIENT_STOCK.equals(stockStatus)) {
                wrapper.ge(Stock::getAvailableCount, StockConstant.Threshold.LOW_STOCK_THRESHOLD);
            }
        }

        // 动态排序
        addOrderBy(wrapper, pageDTO);

        return wrapper;
    }

    /**
     * 添加排序条件
     */
    private void addOrderBy(LambdaQueryWrapper<Stock> wrapper, StockPageDTO pageDTO) {
        String orderBy = pageDTO.getOrderBy();
        boolean isAsc = "asc".equalsIgnoreCase(pageDTO.getOrderType());

        if (orderBy == null || orderBy.trim().isEmpty()) {
            wrapper.orderByDesc(Stock::getUpdateTime);
            return;
        }

        switch (orderBy) {
            case StockConstant.OrderBy.STOCK_COUNT -> {
                if (isAsc) wrapper.orderByAsc(Stock::getStockCount);
                else wrapper.orderByDesc(Stock::getStockCount);
            }
            case StockConstant.OrderBy.AVAILABLE_COUNT -> {
                if (isAsc) wrapper.orderByAsc(Stock::getAvailableCount);
                else wrapper.orderByDesc(Stock::getAvailableCount);
            }
            case StockConstant.OrderBy.FROZEN_COUNT -> {
                if (isAsc) wrapper.orderByAsc(Stock::getFrozenCount);
                else wrapper.orderByDesc(Stock::getFrozenCount);
            }
            case StockConstant.OrderBy.CREATE_TIME -> {
                if (isAsc) wrapper.orderByAsc(Stock::getCreateTime);
                else wrapper.orderByDesc(Stock::getCreateTime);
            }
            case StockConstant.OrderBy.UPDATE_TIME -> {
                if (isAsc) wrapper.orderByAsc(Stock::getUpdateTime);
                else wrapper.orderByDesc(Stock::getUpdateTime);
            }
            default -> wrapper.orderByDesc(Stock::getUpdateTime);
        }
    }
}
