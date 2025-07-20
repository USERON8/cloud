package com.cloud.alibaba.stock.controller;

import com.cloud.alibaba.stock.module.entity.Stock;
import com.cloud.alibaba.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 库存管理控制器
 */
@RestController
@RequestMapping("/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    /**
     * 根据商品ID查询库存
     */
    @GetMapping("/product/{productId}")
    public Stock getStockByProductId(@PathVariable Long productId) {
        return stockService.lambdaQuery()
                .eq(Stock::getProductId, productId)
                .one();
    }

    /**
     * 扣减库存
     */
    @PostMapping("/deduct")
    public boolean deductStock(@RequestParam Long productId, @RequestParam Integer count) {
        return stockService.deductStock(productId, count);
    }

    /**
     * 增加库存
     */
    @PostMapping("/add")
    public boolean addStock(@RequestParam Long productId, @RequestParam Integer count) {
        return stockService.addStock(productId, count);
    }

    /**
     * 查询所有库存
     */
    @GetMapping("/list")
    public List<Stock> getAllStock() {
        return stockService.list();
    }
}
