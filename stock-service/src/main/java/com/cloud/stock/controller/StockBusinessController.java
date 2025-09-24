package com.cloud.stock.controller;

import com.cloud.common.annotation.DistributedLock;
import com.cloud.common.domain.dto.stock.StockDTO;
import com.cloud.common.result.Result;
import com.cloud.stock.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 库存业务控制器
 * 演示分布式锁在库存管理业务场景中的应用
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/stock/business")
@RequiredArgsConstructor
@Tag(name = "库存业务管理", description = "库存业务相关接口，集成分布式锁保护")
public class StockBusinessController {

    private final StockService stockService;

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

        log.info("⚡ 秒杀库存扣减 - 商品ID: {}, 数量: {}, 订单: {}", productId, quantity, orderNo);

        try {
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
     * 批量库存调整 - 使用写锁确保数据一致性
     */
    @PostMapping("/batch-adjust")
    @Operation(summary = "批量库存调整", description = "批量调整商品库存，使用写锁确保数据一致性")
    @DistributedLock(
            key = "'stock:batch:adjust:' + T(String).join(',', #adjustments.![productId])",
            lockType = DistributedLock.LockType.WRITE,
            waitTime = 10,
            leaseTime = 30,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "批量库存调整获取锁失败"
    )
    public Result<String> batchAdjustStock(
            @Parameter(description = "库存调整列表") @RequestBody List<StockAdjustment> adjustments) {

        log.info("🔄 批量库存调整 - 商品数量: {}", adjustments.size());

        try {
            int successCount = 0;
            int failCount = 0;

            for (StockAdjustment adjustment : adjustments) {
                try {
                    boolean result = switch (adjustment.getType()) {
                        case "IN" -> stockService.stockIn(
                                adjustment.getProductId(),
                                adjustment.getQuantity(),
                                adjustment.getRemark()
                        );
                        case "OUT" -> stockService.stockOut(
                                adjustment.getProductId(),
                                adjustment.getQuantity(),
                                null, null,
                                adjustment.getRemark()
                        );
                        case "RESERVE" -> stockService.reserveStock(
                                adjustment.getProductId(),
                                adjustment.getQuantity()
                        );
                        case "RELEASE" -> stockService.releaseReservedStock(
                                adjustment.getProductId(),
                                adjustment.getQuantity()
                        );
                        default -> {
                            log.warn("⚠️ 未知调整类型: {}", adjustment.getType());
                            yield false;
                        }
                    };

                    if (result) {
                        successCount++;
                        log.debug("✅ 商品 {} 库存调整成功", adjustment.getProductId());
                    } else {
                        failCount++;
                        log.warn("❌ 商品 {} 库存调整失败", adjustment.getProductId());
                    }
                } catch (Exception e) {
                    failCount++;
                    log.error("❌ 商品 {} 库存调整异常", adjustment.getProductId(), e);
                }
            }

            String result = String.format("批量库存调整完成 - 成功: %d, 失败: %d", successCount, failCount);
            log.info("✅ {}", result);

            return Result.success(result, "批量调整完成");
        } catch (Exception e) {
            log.error("❌ 批量库存调整失败", e);
            return Result.error("批量调整失败: " + e.getMessage());
        }
    }

    /**
     * 库存盘点 - 使用读锁允许并发查询
     */
    @GetMapping("/inventory-check")
    @Operation(summary = "库存盘点", description = "库存盘点查询，使用读锁允许并发访问")
    @DistributedLock(
            key = "'stock:inventory:check:' + T(String).join(',', #productIds)",
            lockType = DistributedLock.LockType.READ,
            waitTime = 3,
            leaseTime = 10,
            timeUnit = TimeUnit.SECONDS,
            failStrategy = DistributedLock.LockFailStrategy.RETURN_NULL
    )
    public Result<List<StockDTO>> inventoryCheck(
            @Parameter(description = "商品ID列表") @RequestParam List<Long> productIds) {

        log.info("📊 库存盘点 - 商品数量: {}", productIds.size());

        try {
            List<StockDTO> stockList = stockService.getStocksByProductIds(productIds);

            log.info("✅ 库存盘点完成 - 查询商品: {}, 返回结果: {}",
                    productIds.size(), stockList.size());

            return Result.success("盘点完成", stockList);
        } catch (Exception e) {
            log.error("❌ 库存盘点失败", e);
            return Result.error("盘点失败: " + e.getMessage());
        }
    }

    /**
     * 库存预警检查 - 使用快速失败策略
     */
    @PostMapping("/warning-check/{productId}")
    @Operation(summary = "库存预警检查", description = "检查库存预警，使用快速失败策略")
    @DistributedLock(
            key = "'stock:warning:' + #productId",
            waitTime = 0,
            leaseTime = 2,
            timeUnit = TimeUnit.SECONDS,
            failStrategy = DistributedLock.LockFailStrategy.FAIL_FAST,
            failMessage = "库存预警检查系统繁忙"
    )
    public Result<String> checkStockWarning(
            @Parameter(description = "商品ID") @PathVariable Long productId,
            @Parameter(description = "预警阈值") @RequestParam(defaultValue = "10") Integer threshold) {

        log.info("⚠️ 库存预警检查 - 商品ID: {}, 阈值: {}", productId, threshold);

        try {
            StockDTO stock = stockService.getStockByProductId(productId);
            if (stock == null) {
                return Result.error("商品库存信息不存在");
            }

            int availableQuantity = stock.getStockQuantity() - stock.getFrozenQuantity();

            String result;
            if (availableQuantity <= threshold) {
                result = String.format("⚠️ 库存预警 - 商品ID: %d, 可用库存: %d, 预警阈值: %d",
                        productId, availableQuantity, threshold);
                log.warn(result);
            } else {
                result = String.format("✅ 库存正常 - 商品ID: %d, 可用库存: %d",
                        productId, availableQuantity);
                log.info(result);
            }

            return Result.success(result, "检查完成");
        } catch (Exception e) {
            log.error("❌ 库存预警检查失败 - 商品ID: {}", productId, e);
            return Result.error("检查失败: " + e.getMessage());
        }
    }

    /**
     * 库存同步 - 使用可重入锁支持嵌套调用
     */
    @PostMapping("/sync/{productId}")
    @Operation(summary = "库存同步", description = "同步库存数据，使用可重入锁支持嵌套调用")
    @DistributedLock(
            key = "'stock:sync:' + #productId",
            lockType = DistributedLock.LockType.REENTRANT,
            waitTime = 5,
            leaseTime = 15,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "库存同步获取锁失败"
    )
    public Result<String> syncStock(
            @Parameter(description = "商品ID") @PathVariable Long productId,
            @Parameter(description = "同步类型") @RequestParam String syncType) {

        log.info("🔄 库存同步 - 商品ID: {}, 同步类型: {}", productId, syncType);

        try {
            // 模拟同步逻辑
            Thread.sleep(1000);

            String result = switch (syncType) {
                case "full" -> "全量库存同步完成";
                case "increment" -> "增量库存同步完成";
                case "verify" -> "库存数据校验完成";
                default -> "未知同步类型";
            };

            log.info("✅ 库存同步完成 - 商品ID: {}, 结果: {}", productId, result);

            return Result.success(result, "同步完成");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.error("同步被中断");
        } catch (Exception e) {
            log.error("❌ 库存同步失败 - 商品ID: {}", productId, e);
            return Result.error("同步失败: " + e.getMessage());
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
        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }
    }
}
