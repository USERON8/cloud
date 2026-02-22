package com.cloud.stock.controller;

import com.cloud.api.stock.StockFeignClient;
import com.cloud.common.domain.dto.stock.StockDTO;
import com.cloud.common.domain.vo.OperationResultVO;
import com.cloud.common.domain.vo.stock.StockVO;
import com.cloud.common.exception.EntityNotFoundException;
import com.cloud.common.result.Result;
import com.cloud.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Internal stock endpoints for service-to-service calls.
 */
@Slf4j
@RestController
@RequestMapping("/internal/stock")
@RequiredArgsConstructor
public class StockFeignController implements StockFeignClient {

    private final StockService stockService;

    @Override
    @GetMapping("/product/{productId}")
    public StockVO getStockByProductId(@PathVariable("productId") Long productId) {
        StockDTO stock = stockService.getStockByProductId(productId);
        if (stock == null) {
            throw new EntityNotFoundException("Stock", productId);
        }
        StockVO vo = new StockVO();
        vo.setId(stock.getId());
        vo.setProductId(stock.getProductId());
        vo.setProductName(stock.getProductName());
        vo.setStockQuantity(stock.getStockQuantity());
        vo.setFrozenQuantity(stock.getFrozenQuantity());
        vo.setAvailableQuantity(stock.getAvailableQuantity());
        vo.setStockStatus(stock.getStockStatus());
        vo.setCreatedAt(stock.getCreatedAt());
        vo.setUpdatedAt(stock.getUpdatedAt());
        return vo;
    }

    @Override
    @PutMapping("/{productId}")
    public OperationResultVO updateStock(@PathVariable("productId") Long productId,
                                         @RequestParam("quantity") Integer quantity) {
        StockDTO stock = stockService.getStockByProductId(productId);
        if (stock == null) {
            return OperationResultVO.failure("Stock record not found");
        }
        if (quantity == null || quantity < 0) {
            return OperationResultVO.failure("Quantity must be >= 0");
        }

        Integer current = stock.getStockQuantity() == null ? 0 : stock.getStockQuantity();
        int delta = quantity - current;
        if (delta == 0) {
            return OperationResultVO.success("No stock change");
        }

        boolean success = delta > 0
                ? stockService.stockIn(productId, delta, "Feign update stock")
                : stockService.stockOut(productId, -delta, null, null, "Feign update stock");

        return success ? OperationResultVO.success("Stock updated") : OperationResultVO.failure("Stock update failed");
    }

    @GetMapping("/{stockId}")
    public Result<StockDTO> getStockById(@PathVariable Long stockId) {
        return Result.success(stockService.getStockById(stockId));
    }

    @PostMapping("/batch")
    public Result<List<StockDTO>> getStocksByProductIds(@RequestBody List<Long> productIds) {
        return Result.success(stockService.getStocksByProductIds(productIds));
    }

    @GetMapping("/check/{productId}/{quantity}")
    public Result<Boolean> checkStockSufficient(@PathVariable Long productId, @PathVariable Integer quantity) {
        return Result.success(stockService.checkStockSufficient(productId, quantity));
    }

    @PostMapping("/deduct")
    public Result<Boolean> deductStock(@RequestParam Long productId,
                                       @RequestParam Integer quantity,
                                       @RequestParam(required = false) Long orderId,
                                       @RequestParam(required = false) String orderNo) {
        return Result.success(stockService.stockOut(productId, quantity, orderId, orderNo, "Feign deduct stock"));
    }

    @PostMapping("/reserve")
    public Result<Boolean> reserveStock(@RequestParam Long productId, @RequestParam Integer quantity) {
        return Result.success(stockService.reserveStock(productId, quantity));
    }

    @PostMapping("/release")
    public Result<Boolean> releaseReservedStock(@RequestParam Long productId, @RequestParam Integer quantity) {
        return Result.success(stockService.releaseReservedStock(productId, quantity));
    }

    @PostMapping("/stock-in")
    public Result<Boolean> stockIn(@RequestParam Long productId,
                                   @RequestParam Integer quantity,
                                   @RequestParam(required = false) String remark) {
        String finalRemark = (remark == null || remark.isBlank()) ? "Feign stock in" : remark;
        return Result.success(stockService.stockIn(productId, quantity, finalRemark));
    }
}
