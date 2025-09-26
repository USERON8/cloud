package com.cloud.order.controller;

import com.cloud.common.result.Result;
import com.cloud.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 订单流程测试控制器
 * 用于测试订单完整生命周期
 *
 * @author what's up
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/order/flow-test")
@RequiredArgsConstructor
@Tag(name = "订单流程测试", description = "订单完整生命周期测试接口")
public class OrderFlowTestController {

    private final OrderService orderService;

    /**
     * 测试订单支付流程
     */
    @PostMapping("/test-payment/{orderId}")
    @Operation(summary = "测试订单支付", description = "测试指定订单的支付流程")
    public Result<String> testOrderPayment(
            @Parameter(description = "订单ID", required = true) @PathVariable Long orderId) {

        try {
            log.info("🧪 开始测试订单支付流程 - 订单ID: {}", orderId);

            Boolean result = orderService.payOrder(orderId);

            if (result) {
                log.info("✅ 订单支付测试成功 - 订单ID: {}", orderId);
                return Result.success("订单支付成功", "订单ID: " + orderId + " 已成功支付");
            } else {
                log.error("❌ 订单支付测试失败 - 订单ID: {}", orderId);
                return Result.error("订单支付失败");
            }

        } catch (Exception e) {
            log.error("❌ 订单支付测试异常 - 订单ID: {}, 错误: {}", orderId, e.getMessage(), e);
            return Result.error("订单支付测试异常: " + e.getMessage());
        }
    }

    /**
     * 测试订单发货流程
     */
    @PostMapping("/test-shipping/{orderId}")
    @Operation(summary = "测试订单发货", description = "测试指定订单的发货流程")
    public Result<String> testOrderShipping(
            @Parameter(description = "订单ID", required = true) @PathVariable Long orderId) {

        try {
            log.info("🧪 开始测试订单发货流程 - 订单ID: {}", orderId);

            Boolean result = orderService.shipOrder(orderId);

            if (result) {
                log.info("✅ 订单发货测试成功 - 订单ID: {}", orderId);
                return Result.success("订单发货成功", "订单ID: " + orderId + " 已成功发货");
            } else {
                log.error("❌ 订单发货测试失败 - 订单ID: {}", orderId);
                return Result.error("订单发货失败");
            }

        } catch (Exception e) {
            log.error("❌ 订单发货测试异常 - 订单ID: {}, 错误: {}", orderId, e.getMessage(), e);
            return Result.error("订单发货测试异常: " + e.getMessage());
        }
    }

    /**
     * 测试订单完成流程
     */
    @PostMapping("/test-completion/{orderId}")
    @Operation(summary = "测试订单完成", description = "测试指定订单的完成流程")
    public Result<String> testOrderCompletion(
            @Parameter(description = "订单ID", required = true) @PathVariable Long orderId) {

        try {
            log.info("🧪 开始测试订单完成流程 - 订单ID: {}", orderId);

            Boolean result = orderService.completeOrder(orderId);

            if (result) {
                log.info("✅ 订单完成测试成功 - 订单ID: {}", orderId);
                return Result.success("订单完成成功", "订单ID: " + orderId + " 已成功完成");
            } else {
                log.error("❌ 订单完成测试失败 - 订单ID: {}", orderId);
                return Result.error("订单完成失败");
            }

        } catch (Exception e) {
            log.error("❌ 订单完成测试异常 - 订单ID: {}, 错误: {}", orderId, e.getMessage(), e);
            return Result.error("订单完成测试异常: " + e.getMessage());
        }
    }

    /**
     * 测试订单取消流程
     */
    @PostMapping("/test-cancellation/{orderId}")
    @Operation(summary = "测试订单取消", description = "测试指定订单的取消流程")
    public Result<String> testOrderCancellation(
            @Parameter(description = "订单ID", required = true) @PathVariable Long orderId) {

        try {
            log.info("🧪 开始测试订单取消流程 - 订单ID: {}", orderId);

            Boolean result = orderService.cancelOrder(orderId);

            if (result) {
                log.info("✅ 订单取消测试成功 - 订单ID: {}", orderId);
                return Result.success("订单取消成功", "订单ID: " + orderId + " 已成功取消");
            } else {
                log.error("❌ 订单取消测试失败 - 订单ID: {}", orderId);
                return Result.error("订单取消失败");
            }

        } catch (Exception e) {
            log.error("❌ 订单取消测试异常 - 订单ID: {}, 错误: {}", orderId, e.getMessage(), e);
            return Result.error("订单取消测试异常: " + e.getMessage());
        }
    }

    /**
     * 测试完整订单流程
     */
    @PostMapping("/test-full-flow/{orderId}")
    @Operation(summary = "测试完整流程", description = "测试订单从支付到完成的完整流程")
    public Result<Map<String, Object>> testFullOrderFlow(
            @Parameter(description = "订单ID", required = true) @PathVariable Long orderId) {

        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("🚀 开始测试完整订单流程 - 订单ID: {}", orderId);

            // Step 1: 支付
            try {
                Boolean payResult = orderService.payOrder(orderId);
                result.put("payment", payResult ? "成功" : "失败");
                if (!payResult) {
                    result.put("error", "支付失败");
                    return Result.error("完整流程测试失败");
                }
                Thread.sleep(1000); // 等待状态同步
            } catch (Exception e) {
                result.put("payment", "异常: " + e.getMessage());
                return Result.error("支付阶段失败");
            }

            // Step 2: 发货
            try {
                Boolean shipResult = orderService.shipOrder(orderId);
                result.put("shipping", shipResult ? "成功" : "失败");
                if (!shipResult) {
                    result.put("error", "发货失败");
                    return Result.error("完整流程测试失败");
                }
                Thread.sleep(1000); // 等待状态同步
            } catch (Exception e) {
                result.put("shipping", "异常: " + e.getMessage());
                return Result.error("发货阶段失败");
            }

            // Step 3: 完成
            try {
                Boolean completeResult = orderService.completeOrder(orderId);
                result.put("completion", completeResult ? "成功" : "失败");
                if (!completeResult) {
                    result.put("error", "完成失败");
                    return Result.error("完整流程测试失败");
                }
            } catch (Exception e) {
                result.put("completion", "异常: " + e.getMessage());
                return Result.error("完成阶段失败");
            }

            result.put("status", "全部成功");
            result.put("message", "订单完整流程测试成功");
            
            log.info("✅ 完整订单流程测试成功 - 订单ID: {}", orderId);
            return Result.success("完整流程测试成功", result);

        } catch (Exception e) {
            log.error("❌ 完整订单流程测试异常 - 订单ID: {}, 错误: {}", orderId, e.getMessage(), e);
            result.put("error", "系统异常: " + e.getMessage());
            return Result.error("完整流程测试异常");
        }
    }

    /**
     * 查看订单状态
     */
    @GetMapping("/order-status/{orderId}")
    @Operation(summary = "查看订单状态", description = "查看指定订单的当前状态")
    public Result<Object> getOrderStatus(
            @Parameter(description = "订单ID", required = true) @PathVariable Long orderId) {

        try {
            var order = orderService.getById(orderId);
            if (order == null) {
                return Result.error("订单不存在");
            }

            Map<String, Object> status = new HashMap<>();
            status.put("orderId", order.getId());
            status.put("userId", order.getUserId());
            status.put("totalAmount", order.getTotalAmount());
            status.put("payAmount", order.getPayAmount());
            status.put("status", order.getStatus());
            status.put("statusName", getStatusName(order.getStatus()));
            status.put("createdAt", order.getCreatedAt());
            status.put("updatedAt", order.getUpdatedAt());

            return Result.success("查询成功", status);

        } catch (Exception e) {
            log.error("❌ 查询订单状态失败 - 订单ID: {}, 错误: {}", orderId, e.getMessage(), e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取状态名称
     */
    private String getStatusName(Integer status) {
        return switch (status) {
            case 0 -> "待支付";
            case 1 -> "已支付";
            case 2 -> "已发货";
            case 3 -> "已完成";
            case 4 -> "已取消";
            case -1 -> "已取消";
            default -> "未知状态";
        };
    }
}
