package com.cloud.stock.controller;

import com.cloud.stock.converter.StockConverter;
import com.cloud.stock.module.dto.StockPageDTO;
import com.cloud.stock.module.entity.Stock;
import com.cloud.stock.module.vo.StockVO;
import com.cloud.stock.service.StockService;
import domain.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 库存管理控制器
 */
@RestController
@RequestMapping("/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;
    private final StockConverter stockConverter;

    /**
     * 分页查询库存
     */
    @GetMapping("/page")
    public PageResult<StockVO> pageStock(StockPageDTO pageDTO) {
        return stockService.pageStock(pageDTO);
    }

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
     * 根据商品ID查询库存详情（VO格式）
     */
    @GetMapping("/detail/{productId}")
    public StockVO getStockDetail(@PathVariable Long productId) {
        return stockService.getStockVOByProductId(productId);
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
     * 查询所有库存（VO格式）
     */
    @GetMapping("/list")
    public List<StockVO> getAllStock() {
        List<Stock> stockList = stockService.list();
        return stockConverter.toVOList(stockList);
    }

    /**
     * 获取库存统计信息
     */
    @GetMapping("/statistics")
    public Map<String, Object> getStockStatistics() {
        Map<String, Object> statistics = new HashMap<>();

        // 总库存商品数
        long totalCount = stockService.count();
        statistics.put("totalCount", totalCount);

        // 缺货商品数
        long outOfStockCount = stockService.lambdaQuery()
                .le(Stock::getAvailableCount, 0)
                .count();
        statistics.put("outOfStockCount", outOfStockCount);

        // 库存不足商品数
        long lowStockCount = stockService.lambdaQuery()
                .gt(Stock::getAvailableCount, 0)
                .lt(Stock::getAvailableCount, 10)
                .count();
        statistics.put("lowStockCount", lowStockCount);

        // 库存充足商品数
        long sufficientStockCount = stockService.lambdaQuery()
                .ge(Stock::getAvailableCount, 10)
                .count();
        statistics.put("sufficientStockCount", sufficientStockCount);

        return statistics;
    }
}