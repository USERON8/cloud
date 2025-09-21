package com.cloud.stock.controller;

import com.cloud.common.result.Result;
import com.cloud.stock.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

/**
 * 库存管理控制器
 *
 * @author what's up
 */
@Slf4j
@RestController
@RequestMapping("/stock/manage")
@RequiredArgsConstructor
@Tag(name = "库存管理", description = "库存信息管理相关操作")
public class StockManageController {

    private final StockService stockService;


    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除库存信息", description = "根据ID删除库存信息")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> delete(@PathVariable("id")
                                  @Parameter(description = "库存ID")
                                  @NotNull(message = "库存ID不能为空") Long id) {
        log.info("删除库存信息, ID: {}", id);
        boolean result = stockService.deleteStock(id);
        return Result.success("删除成功", result);
    }

    @DeleteMapping("/deleteBatch")
    @Operation(summary = "批量删除库存信息", description = "根据ID列表批量删除库存信息")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> deleteBatch(@RequestBody
                                       @Parameter(description = "库存ID列表")
                                       @Valid @NotNull(message = "库存ID列表不能为空") Collection<Long> ids) {
        log.info("批量删除库存信息, 数量: {}", ids.size());
        boolean result = stockService.deleteStocksByIds(ids);
        return Result.success("批量删除成功", result);
    }

    @PostMapping("/stockIn")
    @Operation(summary = "库存入库", description = "对指定商品进行入库操作")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    public Result<Boolean> stockIn(@RequestParam("productId")
                                   @Parameter(description = "商品ID")
                                   @NotNull(message = "商品ID不能为空") Long productId,
                                   @RequestParam("quantity")
                                   @Parameter(description = "入库数量")
                                   @NotNull(message = "入库数量不能为空")
                                   @Min(value = 1, message = "入库数量必须大于0") Integer quantity,
                                   @RequestParam(value = "remark", required = false)
                                   @Parameter(description = "备注") String remark) {
        log.info("库存入库, 商品ID: {}, 数量: {}, 备注: {}", productId, quantity, remark);
        boolean result = stockService.stockIn(productId, quantity, remark);
        return Result.success("入库成功", result);
    }

    @PostMapping("/stockOut")
    @Operation(summary = "库存出库", description = "对指定商品进行出库操作")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    public Result<Boolean> stockOut(@RequestParam("productId")
                                    @Parameter(description = "商品ID")
                                    @NotNull(message = "商品ID不能为空") Long productId,
                                    @RequestParam("quantity")
                                    @Parameter(description = "出库数量")
                                    @NotNull(message = "出库数量不能为空")
                                    @Min(value = 1, message = "出库数量必须大于0") Integer quantity,
                                    @RequestParam(value = "orderId", required = false)
                                    @Parameter(description = "订单ID") Long orderId,
                                    @RequestParam(value = "orderNo", required = false)
                                    @Parameter(description = "订单号") String orderNo,
                                    @RequestParam(value = "remark", required = false)
                                    @Parameter(description = "备注") String remark) {
        log.info("库存出库, 商品ID: {}, 数量: {}, 订单ID: {}, 订单号: {}, 备注: {}",
                productId, quantity, orderId, orderNo, remark);
        boolean result = stockService.stockOut(productId, quantity, orderId, orderNo, remark);
        return Result.success("出库成功", result);
    }

    @PostMapping("/reserve")
    @Operation(summary = "预留库存", description = "对指定商品进行库存预留")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    public Result<Boolean> reserveStock(@RequestParam("productId")
                                        @Parameter(description = "商品ID")
                                        @NotNull(message = "商品ID不能为空") Long productId,
                                        @RequestParam("quantity")
                                        @Parameter(description = "预留数量")
                                        @NotNull(message = "预留数量不能为空")
                                        @Min(value = 1, message = "预留数量必须大于0") Integer quantity) {
        log.info("预留库存, 商品ID: {}, 数量: {}", productId, quantity);
        boolean result = stockService.reserveStock(productId, quantity);
        return Result.success("预留成功", result);
    }

    @PostMapping("/release")
    @Operation(summary = "释放预留库存", description = "释放指定商品的预留库存")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    public Result<Boolean> releaseReservedStock(@RequestParam("productId")
                                                @Parameter(description = "商品ID")
                                                @NotNull(message = "商品ID不能为空") Long productId,
                                                @RequestParam("quantity")
                                                @Parameter(description = "释放数量")
                                                @NotNull(message = "释放数量不能为空")
                                                @Min(value = 1, message = "释放数量必须大于0") Integer quantity) {
        log.info("释放预留库存, 商品ID: {}, 数量: {}", productId, quantity);
        boolean result = stockService.releaseReservedStock(productId, quantity);
        return Result.success("释放成功", result);
    }
}
