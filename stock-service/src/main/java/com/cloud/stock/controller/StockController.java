package com.cloud.stock.controller;

import com.cloud.common.annotation.DistributedLock;
import com.cloud.common.domain.dto.stock.StockDTO;
import com.cloud.common.domain.vo.stock.StockVO;
import com.cloud.common.result.PageResult;
import com.cloud.common.result.Result;
import com.cloud.stock.module.dto.StockPageDTO;
import com.cloud.stock.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 库存RESTful API控制器
 * 提供库存资源的CRUD操作，参考User服务标准架构
 *
 * @author what's up
 */
@Slf4j
@RestController
@RequestMapping("/stocks")
@RequiredArgsConstructor
@Tag(name = "库存服务", description = "库存资源的RESTful API接口")
public class StockController {

    private final StockService stockService;

    /**
     * 分页查询库存
     */
    @PostMapping("/page")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @Operation(summary = "分页查询库存", description = "根据条件分页查询库存信息")
    public Result<PageResult<StockVO>> getStocksPage(
            @Parameter(description = "分页查询条件") @RequestBody
            @Valid @NotNull(message = "分页查询条件不能为空") StockPageDTO pageDTO,
            Authentication authentication) {

        try {
            PageResult<StockVO> pageResult = stockService.pageQuery(pageDTO);
            return Result.success(pageResult);
        } catch (Exception e) {
            log.error("分页查询库存失败", e);
            return Result.error("分页查询库存失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取库存详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @Operation(summary = "获取库存详情", description = "根据库存ID获取详细信息")
    public Result<StockDTO> getStockById(
            @Parameter(description = "库存ID") @PathVariable
            @NotNull(message = "库存ID不能为空")
            @Positive(message = "库存ID必须为正整数") Long id,
            Authentication authentication) {

        try {
            StockDTO stock = stockService.getStockById(id);
            if (stock == null) {
                return Result.error("库存记录不存在");
            }
            return Result.success("查询成功", stock);
        } catch (Exception e) {
            log.error("获取库存详情失败，库存ID: {}", id, e);
            return Result.error("获取库存详情失败: " + e.getMessage());
        }
    }

    /**
     * 根据商品ID获取库存信息
     */
    @GetMapping("/product/{productId}")
    @Operation(summary = "根据商品ID获取库存信息", description = "根据商品ID获取库存详细信息")
    public Result<StockDTO> getByProductId(
            @Parameter(description = "商品ID") @PathVariable
            @NotNull(message = "商品ID不能为空")
            @Positive(message = "商品ID必须为正整数") Long productId,
            Authentication authentication) {

        try {
            StockDTO stock = stockService.getStockByProductId(productId);
            if (stock == null) {
                return Result.error("该商品暂无库存信息");
            }
            return Result.success("查询成功", stock);
        } catch (Exception e) {
            log.error("根据商品ID获取库存信息失败，商品ID: {}", productId, e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 批量获取库存信息
     */
    @PostMapping("/batch")
    @Operation(summary = "批量获取库存信息", description = "根据商品ID列表批量获取库存信息")
    public Result<List<StockDTO>> getByProductIds(
            @Parameter(description = "商品ID列表") @RequestBody
            @NotNull(message = "商品ID列表不能为空")
            @NotEmpty(message = "商品ID列表不能为空") List<Long> productIds) {

        try {
            List<StockDTO> stocks = stockService.getStocksByProductIds(productIds);
            return Result.success("查询成功", stocks);
        } catch (Exception e) {
            log.error("批量获取库存信息失败", e);
            return Result.error("批量查询失败: " + e.getMessage());
        }
    }

    /**
     * 创建库存记录
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "创建库存记录", description = "创建新的库存记录")
    public Result<StockDTO> createStock(
            @Parameter(description = "库存信息") @RequestBody
            @Valid @NotNull(message = "库存信息不能为空") StockDTO stockDTO) {

        try {
            StockDTO createdStock = stockService.createStock(stockDTO);
            return Result.success("库存创建成功", createdStock);
        } catch (Exception e) {
            log.error("创建库存失败", e);
            return Result.error("创建库存失败: " + e.getMessage());
        }
    }

    /**
     * 更新库存信息
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @Operation(summary = "更新库存信息", description = "更新库存信息")
    public Result<Boolean> updateStock(
            @Parameter(description = "库存ID") @PathVariable Long id,
            @Parameter(description = "库存信息") @RequestBody
            @Valid @NotNull(message = "库存信息不能为空") StockDTO stockDTO,
            Authentication authentication) {

        // 确保路径参数与请求体中的ID一致
        stockDTO.setId(id);

        try {
            boolean result = stockService.updateStock(stockDTO);
            return Result.success("库存更新成功", result);
        } catch (Exception e) {
            log.error("更新库存信息失败，库存ID: {}", id, e);
            return Result.error("更新库存信息失败: " + e.getMessage());
        }
    }

    /**
     * 删除库存信息
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "删除库存信息", description = "根据ID删除库存信息")
    public Result<Boolean> deleteStock(
            @Parameter(description = "库存ID") @PathVariable
            @NotNull(message = "库存ID不能为空") Long id) {

        try {
            boolean result = stockService.deleteStock(id);
            return Result.success("删除成功", result);
        } catch (Exception e) {
            log.error("删除库存信息失败，库存ID: {}", id, e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * 批量删除库存信息
     */
    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "批量删除库存信息", description = "根据ID列表批量删除库存信息")
    public Result<Boolean> deleteBatch(
            @Parameter(description = "库存ID列表") @RequestParam("ids")
            @Valid @NotNull(message = "库存ID列表不能为空") Collection<Long> ids) {

        try {
            boolean result = stockService.deleteStocksByIds(ids);
            return Result.success("批量删除成功", result);
        } catch (Exception e) {
            log.error("批量删除库存信息失败，数量: {}", ids.size(), e);
            return Result.error("批量删除失败: " + e.getMessage());
        }
    }

    // ==================== 业务操作接口 ====================

    /**
     * 库存入库
     */
    @PostMapping("/stock-in")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @DistributedLock(
            key = "'stock:in:' + #productId",
            waitTime = 5,
            leaseTime = 15,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "库存入库操作获取锁失败"
    )
    @Operation(summary = "库存入库", description = "对指定商品进行入库操作")
    public Result<Boolean> stockIn(
            @Parameter(description = "商品ID") @RequestParam("productId")
            @NotNull(message = "商品ID不能为空") Long productId,
            @Parameter(description = "入库数量") @RequestParam("quantity")
            @NotNull(message = "入库数量不能为空")
            @Min(value = 1, message = "入库数量必须大于0") Integer quantity,
            @Parameter(description = "备注") @RequestParam(value = "remark", required = false) String remark,
            Authentication authentication) {

        try {
            log.info("📦 库存入库 - 商品ID: {}, 数量: {}, 备注: {}", productId, quantity, remark);
            boolean result = stockService.stockIn(productId, quantity, remark);
            
            if (result) {
                log.info("✅ 库存入库成功 - 商品ID: {}, 数量: {}", productId, quantity);
                return Result.success("入库成功", result);
            } else {
                log.warn("⚠️ 库存入库失败 - 商品ID: {}", productId);
                return Result.error("入库失败，请检查库存信息");
            }
        } catch (Exception e) {
            log.error("❌ 库存入库失败 - 商品ID: {}, 错误: {}", productId, e.getMessage(), e);
            return Result.error("入库失败: " + e.getMessage());
        }
    }

    /**
     * 库存出库
     */
    @PostMapping("/stock-out")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @DistributedLock(
            key = "'stock:out:' + #productId",
            waitTime = 5,
            leaseTime = 15,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "库存出库操作获取锁失败"
    )
    @Operation(summary = "库存出库", description = "对指定商品进行出库操作")
    public Result<Boolean> stockOut(
            @Parameter(description = "商品ID") @RequestParam("productId")
            @NotNull(message = "商品ID不能为空") Long productId,
            @Parameter(description = "出库数量") @RequestParam("quantity")
            @NotNull(message = "出库数量不能为空")
            @Min(value = 1, message = "出库数量必须大于0") Integer quantity,
            @Parameter(description = "订单ID") @RequestParam(value = "orderId", required = false) Long orderId,
            @Parameter(description = "订单号") @RequestParam(value = "orderNo", required = false) String orderNo,
            @Parameter(description = "备注") @RequestParam(value = "remark", required = false) String remark,
            Authentication authentication) {

        try {
            log.info("📤 库存出库 - 商品ID: {}, 数量: {}, 订单: {}/{}, 备注: {}",
                    productId, quantity, orderId, orderNo, remark);
            boolean result = stockService.stockOut(productId, quantity, orderId, orderNo, remark);
            
            if (result) {
                log.info("✅ 库存出库成功 - 商品ID: {}, 数量: {}", productId, quantity);
                return Result.success("出库成功", result);
            } else {
                log.warn("⚠️ 库存出库失败 - 商品ID: {}, 可能库存不足", productId);
                return Result.error("出库失败，库存可能不足");
            }
        } catch (Exception e) {
            log.error("❌ 库存出库失败 - 商品ID: {}, 错误: {}", productId, e.getMessage(), e);
            return Result.error("出库失败: " + e.getMessage());
        }
    }

    /**
     * 预留库存
     */
    @PostMapping("/reserve")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @DistributedLock(
            key = "'stock:reserve:' + #productId",
            waitTime = 3,
            leaseTime = 10,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "库存预留操作获取锁失败"
    )
    @Operation(summary = "预留库存", description = "对指定商品进行库存预留")
    public Result<Boolean> reserveStock(
            @Parameter(description = "商品ID") @RequestParam("productId")
            @NotNull(message = "商品ID不能为空") Long productId,
            @Parameter(description = "预留数量") @RequestParam("quantity")
            @NotNull(message = "预留数量不能为空")
            @Min(value = 1, message = "预留数量必须大于0") Integer quantity,
            Authentication authentication) {

        try {
            log.info("🔒 库存预留 - 商品ID: {}, 数量: {}", productId, quantity);
            boolean result = stockService.reserveStock(productId, quantity);
            
            if (result) {
                log.info("✅ 库存预留成功 - 商品ID: {}, 数量: {}", productId, quantity);
                return Result.success("预留成功", result);
            } else {
                log.warn("⚠️ 库存预留失败 - 商品ID: {}, 可能库存不足", productId);
                return Result.error("预留失败，库存可能不足");
            }
        } catch (Exception e) {
            log.error("❌ 库存预留失败 - 商品ID: {}, 错误: {}", productId, e.getMessage(), e);
            return Result.error("预留失败: " + e.getMessage());
        }
    }

    /**
     * 释放预留库存
     */
    @PostMapping("/release")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @DistributedLock(
            key = "'stock:release:' + #productId",
            waitTime = 3,
            leaseTime = 10,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "库存释放操作获取锁失败"
    )
    @Operation(summary = "释放预留库存", description = "释放指定商品的预留库存")
    public Result<Boolean> releaseReservedStock(
            @Parameter(description = "商品ID") @RequestParam("productId")
            @NotNull(message = "商品ID不能为空") Long productId,
            @Parameter(description = "释放数量") @RequestParam("quantity")
            @NotNull(message = "释放数量不能为空")
            @Min(value = 1, message = "释放数量必须大于0") Integer quantity,
            Authentication authentication) {

        try {
            log.info("🔓 库存释放 - 商品ID: {}, 数量: {}", productId, quantity);
            boolean result = stockService.releaseReservedStock(productId, quantity);
            
            if (result) {
                log.info("✅ 库存释放成功 - 商品ID: {}, 数量: {}", productId, quantity);
                return Result.success("释放成功", result);
            } else {
                log.warn("⚠️ 库存释放失败 - 商品ID: {}", productId);
                return Result.error("释放失败，请检查预留库存");
            }
        } catch (Exception e) {
            log.error("❌ 库存释放失败 - 商品ID: {}, 错误: {}", productId, e.getMessage(), e);
            return Result.error("释放失败: " + e.getMessage());
        }
    }

    /**
     * 检查库存是否充足
     */
    @GetMapping("/check/{productId}/{quantity}")
    @Operation(summary = "检查库存是否充足", description = "检查指定商品的库存是否充足")
    public Result<Boolean> checkStockSufficient(
            @Parameter(description = "商品ID") @PathVariable
            @NotNull(message = "商品ID不能为空")
            @Positive(message = "商品ID必须为正整数") Long productId,
            @Parameter(description = "所需数量") @PathVariable
            @NotNull(message = "所需数量不能为空")
            @Positive(message = "所需数量必须为正整数") Integer quantity) {

        try {
            boolean sufficient = stockService.checkStockSufficient(productId, quantity);
            return Result.success("检查完成", sufficient);
        } catch (Exception e) {
            log.error("检查库存是否充足失败，商品ID: {}, 数量: {}", productId, quantity, e);
            return Result.error("检查失败: " + e.getMessage());
        }
    }

    // ==================== 高级业务功能 ====================

    /**
     * 秒杀商品库存扣减 - 使用公平锁确保公平性
     */
    @PostMapping("/seckill/{productId}")
    @Operation(summary = "秒杀库存扣减", description = "秒杀场景下的库存扣减，使用公平锁确保公平性")
    @DistributedLock(
            key = "'seckill:stock:' + #productId",
            lockType = DistributedLock.LockType.FAIR,
            waitTime = 1,
            leaseTime = 3,
            timeUnit = TimeUnit.SECONDS,
            failStrategy = DistributedLock.LockFailStrategy.FAIL_FAST,
            failMessage = "秒杀商品库存不足或系统繁忙"
    )
    public Result<Boolean> seckillStockOut(
            @Parameter(description = "商品ID") @PathVariable Long productId,
            @Parameter(description = "扣减数量") @RequestParam(defaultValue = "1") Integer quantity,
            @Parameter(description = "订单ID") @RequestParam Long orderId,
            @Parameter(description = "订单号") @RequestParam String orderNo) {

        try {
            log.info("⚡ 秒杀库存扣减 - 商品ID: {}, 数量: {}, 订单: {}", productId, quantity, orderNo);
            
            // 检查库存是否充足
            boolean sufficient = stockService.checkStockSufficient(productId, quantity);
            if (!sufficient) {
                log.warn("❌ 秒杀商品库存不足 - 商品ID: {}, 需要数量: {}", productId, quantity);
                return Result.error("商品库存不足");
            }

            // 执行库存扣减
            boolean result = stockService.stockOut(productId, quantity, orderId, orderNo, "秒杀扣减");
            
            if (result) {
                log.info("✅ 秒杀库存扣减成功 - 商品ID: {}, 订单: {}", productId, orderNo);
                return Result.success("秒杀成功", true);
            } else {
                log.warn("❌ 秒杀库存扣减失败 - 商品ID: {}, 订单: {}", productId, orderNo);
                return Result.error("秒杀失败，库存不足");
            }
        } catch (Exception e) {
            log.error("❌ 秒杀库存扣减异常 - 商品ID: {}, 订单: {}", productId, orderNo, e);
            return Result.error("秒杀失败: " + e.getMessage());
        }
    }

    /**
     * 库存调整请求DTO
     */
    public static class StockAdjustment {
        private Long productId;
        private String type; // IN, OUT, RESERVE, RELEASE
        private Integer quantity;
        private String remark;

        // Getters and Setters
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
    }
}
