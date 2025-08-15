package com.cloud.stock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.common.domain.PageResult;
import com.cloud.common.domain.dto.StockPageDTO;
import com.cloud.common.domain.vo.StockVO;
import com.cloud.common.utils.PageUtils;
import com.cloud.stock.converter.StockConverter;
import com.cloud.stock.mapper.StockMapper;
import com.cloud.stock.module.entity.Stock;
import com.cloud.stock.service.StockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class StockServiceImpl extends ServiceImpl<StockMapper, Stock>
        implements StockService {
    private final StockConverter stockConverter;

    public StockServiceImpl(StockConverter stockConverter) {
        this.stockConverter = stockConverter;
    }


    @Override
    public StockVO getByProductId(Long productId) {
        log.info("根据商品ID查询库存: {}", productId);

        if (productId == null) {
            log.warn("商品ID不能为空");
            return null;
        }

        // 构建查询条件
        LambdaQueryWrapper<Stock> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Stock::getProductId, productId);

        // 查询库存信息
        Stock stock = this.getOne(queryWrapper);

        if (stock == null) {
            log.warn("未找到商品ID为 {} 的库存信息", productId);
            return null;
        }

        // 转换为VO对象
        return stockConverter.toVO(stock);
    }

    @Override
    public PageResult<StockVO> pageQuery(StockPageDTO pageDTO) {
        log.info("分页查询库存，查询条件：{}", pageDTO);

        StockPageDTO stockPageDTO = stockConverter.copyStockPageDTO(pageDTO);
        // 1. 使用PageUtils创建MyBatis-Plus分页对象
        Page<Stock> page = PageUtils.buildPage(pageDTO);

        // 2. 构建查询条件
        LambdaQueryWrapper<Stock> queryWrapper = stockConverter.buildQueryWrapper(stockPageDTO);

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