package com.cloud.payment.controller;

import com.cloud.common.result.Result;
import com.cloud.payment.service.PaymentTestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 支付测试控制器
 * 用于测试支付流程，模拟支付成功场景
 *
 * @author what's up
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payment/test")
@RequiredArgsConstructor
@Tag(name = "支付测试", description = "支付功能测试接口，用于模拟支付成功场景")
public class PaymentTestController {

    private final PaymentTestService paymentTestService;

    /**
     * 模拟支付成功
     * 用于测试完整的支付流程，从创建支付到支付成功的全流程
     */
    @PostMapping("/simulate-success/{orderId}")
    @Operation(summary = "模拟支付成功", description = "模拟指定订单的支付成功，触发完整的支付流程")
    public Result<String> simulatePaymentSuccess(
            @Parameter(description = "订单ID", required = true) @PathVariable Long orderId,
            @Parameter(description = "支付金额") @RequestParam(required = false) BigDecimal amount,
            @Parameter(description = "支付渠道：1-支付宝，2-微信，3-银行卡") @RequestParam(defaultValue = "1") Integer channel) {

        try {
            log.info("🧪 开始模拟支付成功 - 订单ID: {}, 金额: {}, 渠道: {}", orderId, amount, channel);

            String result = paymentTestService.simulatePaymentSuccess(orderId, amount, channel);

            log.info("✅ 模拟支付成功完成 - 订单ID: {}, 结果: {}", orderId, result);
            return Result.success("模拟支付成功", result);

        } catch (Exception e) {
            log.error("❌ 模拟支付成功失败 - 订单ID: {}, 错误: {}", orderId, e.getMessage(), e);
            return Result.error("模拟支付失败: " + e.getMessage());
        }
    }

    /**
     * 模拟支付失败
     */
    @PostMapping("/simulate-failure/{orderId}")
    @Operation(summary = "模拟支付失败", description = "模拟指定订单的支付失败")
    public Result<String> simulatePaymentFailure(
            @Parameter(description = "订单ID", required = true) @PathVariable Long orderId,
            @Parameter(description = "失败原因") @RequestParam(defaultValue = "余额不足") String reason) {

        try {
            log.info("🧪 开始模拟支付失败 - 订单ID: {}, 原因: {}", orderId, reason);

            String result = paymentTestService.simulatePaymentFailure(orderId, reason);

            log.info("✅ 模拟支付失败完成 - 订单ID: {}, 结果: {}", orderId, result);
            return Result.success("模拟支付失败", result);

        } catch (Exception e) {
            log.error("❌ 模拟支付失败异常 - 订单ID: {}, 错误: {}", orderId, e.getMessage(), e);
            return Result.error("模拟支付失败异常: " + e.getMessage());
        }
    }

    /**
     * 查看支付记录状态
     */
    @GetMapping("/payment-status/{orderId}")
    @Operation(summary = "查看支付状态", description = "查看指定订单的支付记录状态")
    public Result<Object> getPaymentStatus(
            @Parameter(description = "订单ID", required = true) @PathVariable Long orderId) {

        try {
            Object status = paymentTestService.getPaymentStatus(orderId);
            return Result.success("查询成功", status);

        } catch (Exception e) {
            log.error("❌ 查询支付状态失败 - 订单ID: {}, 错误: {}", orderId, e.getMessage(), e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 重置支付状态
     * 用于测试时重置支付记录状态
     */
    @PostMapping("/reset-payment/{orderId}")
    @Operation(summary = "重置支付状态", description = "重置指定订单的支付状态为待支付，用于重复测试")
    public Result<String> resetPaymentStatus(
            @Parameter(description = "订单ID", required = true) @PathVariable Long orderId) {

        try {
            log.info("🔄 开始重置支付状态 - 订单ID: {}", orderId);

            String result = paymentTestService.resetPaymentStatus(orderId);

            log.info("✅ 重置支付状态完成 - 订单ID: {}, 结果: {}", orderId, result);
            return Result.success("重置成功", result);

        } catch (Exception e) {
            log.error("❌ 重置支付状态失败 - 订单ID: {}, 错误: {}", orderId, e.getMessage(), e);
            return Result.error("重置失败: " + e.getMessage());
        }
    }

    /**
     * 测试完整支付流程
     * 从创建订单到支付成功的完整流程测试
     */
    @PostMapping("/full-flow-test")
    @Operation(summary = "完整流程测试", description = "测试从创建订单到支付成功的完整流程")
    public Result<Object> testFullPaymentFlow(
            @Parameter(description = "用户ID") @RequestParam(defaultValue = "1001") Long userId,
            @Parameter(description = "支付金额") @RequestParam(defaultValue = "99.99") BigDecimal amount,
            @Parameter(description = "商品名称") @RequestParam(defaultValue = "测试商品") String productName) {

        try {
            log.info("🚀 开始完整流程测试 - 用户ID: {}, 金额: {}, 商品: {}", userId, amount, productName);

            Object result = paymentTestService.testFullPaymentFlow(userId, amount, productName);

            log.info("✅ 完整流程测试完成 - 用户ID: {}, 结果: {}", userId, result);
            return Result.success("流程测试完成", result);

        } catch (Exception e) {
            log.error("❌ 完整流程测试失败 - 用户ID: {}, 错误: {}", userId, e.getMessage(), e);
            return Result.error("流程测试失败: " + e.getMessage());
        }
    }
}
