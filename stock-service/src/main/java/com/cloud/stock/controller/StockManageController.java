package com.cloud.stock.controller;

import com.cloud.common.domain.Result;
import com.cloud.common.domain.dto.StockDTO;
import com.cloud.stock.converter.StockConverter;
import com.cloud.stock.module.entity.Stock;
import com.cloud.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 库存管理控制器
 */
@RestController
@RequestMapping("/stock")
@RequiredArgsConstructor
@Slf4j
public class StockManageController {

    private final StockService stockService;
    private final StockConverter stockConverter;

    @PostMapping("/product/add")
    public Result<String> addStock(@RequestBody StockDTO stockDTO) {
        log.info("开始增加库存");
        stockService.save(stockConverter.toEntity(stockDTO));
        return Result.success("增加成功");
    }

    @PostMapping("/product/add/batch")
    public Result<String> addStockBatch(@RequestBody List<StockDTO> list) {
        log.info("开始批量增加库存");
        List<Stock> stockList = list.stream().
                map(stockConverter::toEntity)
                .toList();
        stockService.saveBatch(stockList);
        return Result.success("批量增加成功");
    }

    @DeleteMapping("/product/reduce/{id}")
    public Result<String> reduceStock(@PathVariable Long id) {
        log.info("开始扣减库存");
        stockService.removeById(id);
        return Result.success("扣减成功");
    }

    @DeleteMapping("/product/reduce/batch")
    public Result<String> reduceStockBatch(@RequestBody List<Long> list) {
        log.info("开始批量扣减库存");
        stockService.removeByIds(list);
        return Result.success("批量扣减成功");
    }

    @PostMapping("/product/update/{id}")
    public Result<String> updateStock(@RequestBody StockDTO stockDTO) {
        log.info("开始更新库存");
        stockService.updateById(stockConverter.toEntity(stockDTO));

        return Result.success("更新成功");
    }
}