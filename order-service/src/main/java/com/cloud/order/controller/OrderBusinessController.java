package com.cloud.order.controller;

import com.cloud.common.annotation.DistributedLock;
import com.cloud.common.domain.dto.order.OrderCreateDTO;
import com.cloud.common.domain.dto.order.OrderDTO;
import com.cloud.common.result.Result;
import com.cloud.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

/**
 * 订单业务控制器
 * 演示分布式锁在实际业务场景中的应用
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/order/business")
@RequiredArgsConstructor
@Tag(name = "订单业务管理", description = "订单业务相关接口，集成分布式锁保护")
public class OrderBusinessController {

    private final OrderService orderService;

    /**
     * 创建订单 - 使用分布式锁防止重复创建
     */
    @PostMapping("/create")
    @Operation(summary = "创建订单", description = "创建新订单，使用分布式锁防止重复创建")
    @DistributedLock(
            key = "'order:create:user:' + #orderCreateDTO.userId + ':' + T(System).currentTimeMillis() / 60000",
            waitTime = 3,
            leaseTime = 10,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "订单创建过于频繁，请稍后再试"
    )
    public Result<OrderDTO> createOrder(@RequestBody OrderCreateDTO orderCreateDTO) {
        log.info("🛒 创建订单请求 - 用户ID: {}, 商品数量: {}",
                orderCreateDTO.getUserId(), orderCreateDTO.getOrderItems().size());

        try {
            OrderDTO orderDTO = orderService.createOrder(orderCreateDTO);
            log.info("✅ 订单创建成功 - 订单ID: {}, 用户ID: {}",
                    orderDTO.getId(), orderDTO.getUserId());

            return Result.success("订单创建成功", orderDTO);
        } catch (Exception e) {
            log.error("❌ 订单创建失败 - 用户ID: {}", orderCreateDTO.getUserId(), e);
            return Result.error("订单创建失败: " + e.getMessage());
        }
    }

    /**
     * 批量处理订单状态 - 使用分布式锁确保批量操作的原子性
     */
    @PostMapping("/batch-process")
    @Operation(summary = "批量处理订单", description = "批量处理订单状态，使用分布式锁确保原子性")
    @DistributedLock(
            key = "'order:batch:' + #operation + ':' + T(String).join(',', #orderIds)",
            waitTime = 10,
            leaseTime = 30,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "批量订单处理获取锁失败"
    )
    public Result<String> batchProcessOrders(
            @Parameter(description = "订单ID列表") @RequestBody java.util.List<Long> orderIds,
            @Parameter(description = "操作类型") @RequestParam String operation) {

        log.info("🔄 批量处理订单 - 操作: {}, 订单数量: {}", operation, orderIds.size());

        try {
            int successCount = 0;
            int failCount = 0;

            for (Long orderId : orderIds) {
                try {
                    boolean result = switch (operation) {
                        case "pay" -> orderService.payOrder(orderId);
                        case "ship" -> orderService.shipOrder(orderId);
                        case "complete" -> orderService.completeOrder(orderId);
                        case "cancel" -> orderService.cancelOrder(orderId);
                        default -> {
                            log.warn("⚠️ 未知操作类型: {}", operation);
                            yield false;
                        }
                    };

                    if (result) {
                        successCount++;
                        log.debug("✅ 订单 {} 处理成功", orderId);
                    } else {
                        failCount++;
                        log.warn("❌ 订单 {} 处理失败", orderId);
                    }
                } catch (Exception e) {
                    failCount++;
                    log.error("❌ 订单 {} 处理异常", orderId, e);
                }
            }

            String result = String.format("批量处理完成 - 成功: %d, 失败: %d", successCount, failCount);
            log.info("✅ {}", result);

            return Result.success(result, "批量处理完成");
        } catch (Exception e) {
            log.error("❌ 批量处理订单失败", e);
            return Result.error("批量处理失败: " + e.getMessage());
        }
    }

    /**
     * 用户订单操作 - 使用读锁允许并发查询
     */
    @GetMapping("/user/{userId}/orders")
    @Operation(summary = "查询用户订单", description = "查询用户的所有订单，使用读锁允许并发访问")
    @DistributedLock(
            key = "'order:query:user:' + #userId",
            lockType = DistributedLock.LockType.READ,
            waitTime = 2,
            leaseTime = 5,
            timeUnit = TimeUnit.SECONDS,
            failStrategy = DistributedLock.LockFailStrategy.RETURN_NULL
    )
    public Result<java.util.List<OrderDTO>> getUserOrders(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "页大小") @RequestParam(defaultValue = "10") Integer pageSize) {

        log.info("🔍 查询用户订单 - 用户ID: {}, 页码: {}, 页大小: {}", userId, pageNum, pageSize);

        try {
            // 这里简化处理，实际应该调用分页查询方法
            java.util.List<OrderDTO> orders = new java.util.ArrayList<>();

            log.info("✅ 用户订单查询完成 - 用户ID: {}, 订单数量: {}", userId, orders.size());

            return Result.success("查询成功", orders);
        } catch (Exception e) {
            log.error("❌ 查询用户订单失败 - 用户ID: {}", userId, e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 订单数据修复 - 使用写锁确保数据一致性
     */
    @PostMapping("/repair/{orderId}")
    @Operation(summary = "订单数据修复", description = "修复订单数据，使用写锁确保数据一致性")
    @DistributedLock(
            key = "'order:repair:' + #orderId",
            lockType = DistributedLock.LockType.WRITE,
            waitTime = 5,
            leaseTime = 20,
            timeUnit = TimeUnit.SECONDS,
            failStrategy = DistributedLock.LockFailStrategy.FAIL_FAST
    )
    public Result<String> repairOrderData(
            @Parameter(description = "订单ID") @PathVariable Long orderId,
            @Parameter(description = "修复类型") @RequestParam String repairType) {

        log.info("🔧 开始订单数据修复 - 订单ID: {}, 修复类型: {}", orderId, repairType);

        try {
            // 模拟数据修复逻辑
            Thread.sleep(1000); // 模拟修复耗时

            String result = switch (repairType) {
                case "status" -> "订单状态修复完成";
                case "amount" -> "订单金额修复完成";
                case "items" -> "订单商品修复完成";
                default -> "未知修复类型";
            };

            log.info("✅ 订单数据修复完成 - 订单ID: {}, 结果: {}", orderId, result);

            return Result.success(result, "修复完成");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("❌ 订单数据修复被中断 - 订单ID: {}", orderId);
            return Result.error("修复被中断");
        } catch (Exception e) {
            log.error("❌ 订单数据修复失败 - 订单ID: {}", orderId, e);
            return Result.error("修复失败: " + e.getMessage());
        }
    }

    /**
     * 高频操作限制 - 使用快速失败策略
     */
    @PostMapping("/high-frequency/{orderId}")
    @Operation(summary = "高频操作", description = "高频操作接口，使用快速失败策略防止过载")
    @DistributedLock(
            key = "'order:freq:' + #orderId",
            waitTime = 0,
            leaseTime = 2,
            timeUnit = TimeUnit.SECONDS,
            failStrategy = DistributedLock.LockFailStrategy.FAIL_FAST,
            failMessage = "操作过于频繁，请稍后再试"
    )
    public Result<String> highFrequencyOperation(
            @Parameter(description = "订单ID") @PathVariable Long orderId,
            @Parameter(description = "操作参数") @RequestParam String param) {

        log.info("⚡ 高频操作 - 订单ID: {}, 参数: {}", orderId, param);

        try {
            // 模拟快速操作
            Thread.sleep(100);

            String result = "高频操作完成 - 订单ID: " + orderId + ", 参数: " + param;
            log.info("✅ {}", result);

            return Result.success(result, "操作完成");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.error("操作被中断");
        } catch (Exception e) {
            log.error("❌ 高频操作失败 - 订单ID: {}", orderId, e);
            return Result.error("操作失败: " + e.getMessage());
        }
    }

    /**
     * 订单状态同步 - 使用公平锁确保按顺序处理
     */
    @PostMapping("/sync-status/{orderId}")
    @Operation(summary = "订单状态同步", description = "同步订单状态，使用公平锁确保按顺序处理")
    @DistributedLock(
            key = "'order:sync:' + #orderId",
            lockType = DistributedLock.LockType.FAIR,
            waitTime = 5,
            leaseTime = 15,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "订单状态同步获取锁失败"
    )
    public Result<String> syncOrderStatus(
            @Parameter(description = "订单ID") @PathVariable Long orderId,
            @Parameter(description = "目标状态") @RequestParam Integer targetStatus) {

        log.info("🔄 同步订单状态 - 订单ID: {}, 目标状态: {}", orderId, targetStatus);

        try {
            // 模拟状态同步逻辑
            Thread.sleep(500);

            String result = String.format("订单状态同步完成 - 订单ID: %d, 状态: %d", orderId, targetStatus);
            log.info("✅ {}", result);

            return Result.success(result, "同步完成");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.error("同步被中断");
        } catch (Exception e) {
            log.error("❌ 订单状态同步失败 - 订单ID: {}", orderId, e);
            return Result.error("同步失败: " + e.getMessage());
        }
    }
}
