package com.cloud.stock.controller;

import com.cloud.common.domain.dto.stock.StockDTO;
import com.cloud.common.exception.BusinessException;
import com.cloud.common.exception.ResourceNotFoundException;
import com.cloud.common.result.Result;
import com.cloud.stock.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 库存服务Feign客户端控制器
 * 提供内部微服务调用接口
 *
 * @author what's up
 */
@Slf4j
@RestController
@RequestMapping("/internal/stock")
@RequiredArgsConstructor
@Tag(name = "库存服务Feign接口", description = "提供内部微服务间调用的库存相关接口")
public class StockFeignController {

    private final StockService stockService;

    /**
     * 根据库存ID获取库存信息（内部调用）
     */
    @GetMapping("/{stockId}")
    @Operation(summary = "获取库存信息", description = "根据库存ID获取库存信息（内部调用）")
    public Result<StockDTO> getStockById(
            @Parameter(description = "库存ID") @PathVariable Long stockId) {

        log.debug("🔍 Feign调用获取库存信息 - 库存ID: {}", stockId);
        StockDTO stock = stockService.getStockById(stockId);

        if (stock == null) {
            log.warn("⚠️ 库存记录不存在 - 库存ID: {}", stockId);
            throw new ResourceNotFoundException("Stock", String.valueOf(stockId));
        }

        return Result.success(stock);
    }

    /**
     * 根据商品ID获取库存信息（内部调用）
     */
    @GetMapping("/product/{productId}")
    @Operation(summary = "根据商品ID获取库存信息", description = "根据商品ID获取库存信息（内部调用）")
    public Result<StockDTO> getStockByProductId(
            @Parameter(description = "商品ID") @PathVariable Long productId) {

        log.debug("🔍 Feign调用根据商品ID获取库存信息 - 商品ID: {}", productId);
        StockDTO stock = stockService.getStockByProductId(productId);

        if (stock == null) {
            log.warn("⚠️ 商品对应的库存记录不存在 - 商品ID: {}", productId);
            throw new ResourceNotFoundException("Stock for Product", String.valueOf(productId));
        }

        return Result.success(stock);
    }

    /**
     * 批量获取库存信息（内部调用）
     */
    @PostMapping("/batch")
    @Operation(summary = "批量获取库存信息", description = "根据商品ID列表批量获取库存信息（内部调用）")
    public Result<List<StockDTO>> getStocksByProductIds(
            @Parameter(description = "商品ID列表") @RequestBody List<Long> productIds) {

        log.debug("🔍 Feign调用批量获取库存信息 - 商品数量: {}", productIds.size());
        List<StockDTO> stocks = stockService.getStocksByProductIds(productIds);
        log.debug("✅ 批量获取库存信息成功 - 返回数量: {}", stocks.size());
        return Result.success("获取成功", stocks);
    }

    /**
     * 检查库存是否充足（内部调用）
     */
    @GetMapping("/check/{productId}/{quantity}")
    @Operation(summary = "检查库存是否充足", description = "检查库存是否充足（内部调用）")
    public Result<Boolean> checkStockSufficient(
            @Parameter(description = "商品ID") @PathVariable Long productId,
            @Parameter(description = "所需数量") @PathVariable Integer quantity) {

        log.debug("🔍 Feign调用检查库存是否充足 - 商品ID: {}, 数量: {}", productId, quantity);
        boolean sufficient = stockService.checkStockSufficient(productId, quantity);
        log.debug("✅ 库存检查完成 - 商品ID: {}, 数量: {}, 结果: {}", productId, quantity, sufficient);
        return Result.success(sufficient);
    }

    /**
     * 库存扣减（内部调用）
     */
    @PostMapping("/deduct")
    @Operation(summary = "库存扣减", description = "库存扣减（内部调用）")
    public Result<Boolean> deductStock(
            @Parameter(description = "商品ID") @RequestParam Long productId,
            @Parameter(description = "扣减数量") @RequestParam Integer quantity,
            @Parameter(description = "订单ID") @RequestParam(required = false) Long orderId,
            @Parameter(description = "订单号") @RequestParam(required = false) String orderNo) {

        log.info("📤 Feign调用库存扣减 - 商品ID: {}, 数量: {}, 订单: {}/{}", productId, quantity, orderId, orderNo);
        boolean result = stockService.stockOut(productId, quantity, orderId, orderNo, "Feign调用扣减");

        if (!result) {
            log.warn("⚠️ 库存扣减失败 - 商品ID: {}, 数量: {}", productId, quantity);
            throw new BusinessException("库存扣减失败");
        }
        log.info("✅ 库存扣减成功 - 商品ID: {}, 数量: {}", productId, quantity);
        return Result.success("库存扣减成功", true);
    }

    /**
     * 预留库存（内部调用）
     */
    @PostMapping("/reserve")
    @Operation(summary = "预留库存", description = "预留库存（内部调用）")
    public Result<Boolean> reserveStock(
            @Parameter(description = "商品ID") @RequestParam Long productId,
            @Parameter(description = "预留数量") @RequestParam Integer quantity) {

        log.info("🔒 Feign调用预留库存 - 商品ID: {}, 数量: {}", productId, quantity);
        boolean result = stockService.reserveStock(productId, quantity);

        if (!result) {
            log.warn("⚠️ 库存预留失败 - 商品ID: {}, 数量: {}", productId, quantity);
            throw new BusinessException("库存预留失败");
        }
        log.info("✅ 库存预留成功 - 商品ID: {}, 数量: {}", productId, quantity);
        return Result.success("库存预留成功", true);
    }

    /**
     * 释放预留库存（内部调用）
     */
    @PostMapping("/release")
    @Operation(summary = "释放预留库存", description = "释放预留库存（内部调用）")
    public Result<Boolean> releaseReservedStock(
            @Parameter(description = "商品ID") @RequestParam Long productId,
            @Parameter(description = "释放数量") @RequestParam Integer quantity) {

        log.info("🔓 Feign调用释放预留库存 - 商品ID: {}, 数量: {}", productId, quantity);
        boolean result = stockService.releaseReservedStock(productId, quantity);

        if (!result) {
            log.warn("⚠️ 释放预留库存失败 - 商品ID: {}, 数量: {}", productId, quantity);
            throw new BusinessException("释放预留库存失败");
        }
        log.info("✅ 释放预留库存成功 - 商品ID: {}, 数量: {}", productId, quantity);
        return Result.success("释放预留库存成功", true);
    }

    /**
     * 库存入库（内部调用）
     */
    @PostMapping("/stock-in")
    @Operation(summary = "库存入库", description = "库存入库（内部调用）")
    public Result<Boolean> stockIn(
            @Parameter(description = "商品ID") @RequestParam Long productId,
            @Parameter(description = "入库数量") @RequestParam Integer quantity,
            @Parameter(description = "备注") @RequestParam(required = false) String remark) {

        log.info("📦 Feign调用库存入库 - 商品ID: {}, 数量: {}, 备注: {}", productId, quantity, remark);
        boolean result = stockService.stockIn(productId, quantity, remark != null ? remark : "Feign调用入库");

        if (!result) {
            log.warn("⚠️ 库存入库失败 - 商品ID: {}, 数量: {}", productId, quantity);
            throw new BusinessException("库存入库失败");
        }
        log.info("✅ 库存入库成功 - 商品ID: {}, 数量: {}", productId, quantity);
        return Result.success("库存入库成功", true);
    }
}
