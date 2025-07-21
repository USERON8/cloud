package com.cloud.stock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.stock.converter.StockConverter;
import com.cloud.stock.mapper.StockMapper;
import com.cloud.stock.module.dto.StockPageDTO;
import com.cloud.stock.module.entity.Stock;
import com.cloud.stock.service.StockService;
import domain.PageResult;
import domain.vo.StockVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import utils.PageUtils;

import java.util.List;

/**
 * 库存服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockServiceImpl extends ServiceImpl<StockMapper, Stock> implements StockService {

    private final StockConverter stockConverter;

    /**
     * 根据商品ID查询库存
     */
    @Override
    public Stock getByProductId(Long productId) {
        log.info("查询商品库存，productId: {}", productId);

        LambdaQueryWrapper<Stock> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Stock::getProductId, productId);

        Stock stock = this.getOne(queryWrapper);
        if (stock == null) {
            log.warn("未找到商品库存信息，productId: {}", productId);
        }

        return stock;
    }

    /**
     * 分页查询库存
     */
    @Override
    public PageResult<StockVO> pageQuery(StockPageDTO pageDTO) {
        log.info("分页查询库存，查询条件：{}", pageDTO);

        // 1. 使用PageUtils创建MyBatis-Plus分页对象
        Page<Stock> page = PageUtils.buildPage(pageDTO);

        // 2. 构建查询条件
        LambdaQueryWrapper<Stock> queryWrapper = stockConverter.buildQueryWrapper(pageDTO);

        // 3. 执行分页查询
        Page<Stock> resultPage = this.page(page, queryWrapper);

        // 4. 转换实体列表为VO列表
        List<StockVO> stockVOList = stockConverter.toVOList(resultPage.getRecords());

        // 5. 使用PageResult封装分页结果
        PageResult<StockVO> pageResult = PageResult.of(
                resultPage.getCurrent(),
                resultPage.getSize(),
                resultPage.getTotal(),
                stockVOList
        );

        log.info("分页查询完成，总记录数：{}，当前页：{}，每页大小：{}",
                pageResult.getTotal(), pageResult.getCurrent(), pageResult.getSize());

        return pageResult;
    }

}
