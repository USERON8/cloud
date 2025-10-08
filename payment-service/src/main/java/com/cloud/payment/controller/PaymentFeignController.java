package com.cloud.payment.controller;

import com.cloud.common.domain.dto.payment.PaymentDTO;
import com.cloud.common.exception.BusinessException;
import com.cloud.common.exception.ResourceNotFoundException;
import com.cloud.common.result.Result;
import com.cloud.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 支付服务Feign客户端控制器
 * 提供内部微服务调用接口
 *
 * @author what's up
 */
@Slf4j
@RestController
@RequestMapping("/internal/payment")
@RequiredArgsConstructor
@Tag(name = "支付服务Feign接口", description = "提供内部微服务间调用的支付相关接口")
public class PaymentFeignController {

    private final PaymentService paymentService;

    /**
     * 根据支付ID获取支付信息（内部调用）
     */
    @GetMapping("/{paymentId}")
    @Operation(summary = "获取支付信息", description = "根据支付ID获取支付信息（内部调用）")
    public Result<PaymentDTO> getPaymentById(
            @Parameter(description = "支付ID") @PathVariable Long paymentId) {

        log.debug("🔍 Feign调用获取支付信息 - 支付ID: {}", paymentId);
        PaymentDTO payment = paymentService.getPaymentById(paymentId);

        if (payment == null) {
            log.warn("⚠️ 支付记录不存在 - 支付ID: {}", paymentId);
            throw new ResourceNotFoundException("Payment", String.valueOf(paymentId));
        }

        return Result.success(payment);
    }

    /**
     * 根据订单ID获取支付信息（内部调用）
     */
    @GetMapping("/order/{orderId}")
    @Operation(summary = "根据订单ID获取支付信息", description = "根据订单ID获取支付信息（内部调用）")
    public Result<PaymentDTO> getPaymentByOrderId(
            @Parameter(description = "订单ID") @PathVariable Long orderId) {

        log.debug("🔍 Feign调用根据订单ID获取支付信息 - 订单ID: {}", orderId);
        PaymentDTO payment = paymentService.getPaymentByOrderId(orderId);

        if (payment == null) {
            log.warn("⚠️ 订单对应的支付记录不存在 - 订单ID: {}", orderId);
            throw new ResourceNotFoundException("Payment for Order", String.valueOf(orderId));
        }

        return Result.success(payment);
    }

    /**
     * 创建支付记录（内部调用）
     */
    @PostMapping
    @Operation(summary = "创建支付记录", description = "创建支付记录（内部调用）")
    public Result<Long> createPayment(
            @Parameter(description = "支付信息") @RequestBody PaymentDTO paymentDTO) {

        log.info("📝 Feign调用创建支付记录 - 订单ID: {}, 金额: {}", paymentDTO.getOrderId(), paymentDTO.getAmount());
        Long paymentId = paymentService.createPayment(paymentDTO);

        log.info("✅ 支付记录创建成功 - 支付ID: {}", paymentId);
        return Result.success("支付记录创庺成功", paymentId);
    }

    /**
     * 更新支付状态（内部调用）
     */
    @PutMapping("/{paymentId}/status")
    @Operation(summary = "更新支付状态", description = "更新支付状态（内部调用）")
    public Result<Boolean> updatePaymentStatus(
            @Parameter(description = "支付ID") @PathVariable Long paymentId,
            @Parameter(description = "支付状态") @RequestParam Integer status,
            @Parameter(description = "备注信息") @RequestParam(required = false) String remark) {

        log.info("🔄 Feign调用更新支付状态 - 支付ID: {}, 状态: {}, 备注: {}", paymentId, status, remark);
        Boolean result = paymentService.updatePaymentStatus(paymentId, status, remark);

        if (!result) {
            log.warn("⚠️ 支付状态更新失败 - 支付ID: {}", paymentId);
            throw new BusinessException("支付状态更新失败");
        }
        log.info("✅ 支付状态更新成功 - 支付ID: {}, 状态: {}", paymentId, status);
        return Result.success("支付状态更新成功", true);
    }

    /**
     * 支付成功处理（内部调用）
     */
    @PostMapping("/{paymentId}/success")
    @Operation(summary = "支付成功处理", description = "支付成功处理（内部调用）")
    public Result<Boolean> processPaymentSuccess(
            @Parameter(description = "支付ID") @PathVariable Long paymentId,
            @Parameter(description = "交易号") @RequestParam(required = false) String transactionId) {

        log.info("✅ Feign调用支付成功处理 - 支付ID: {}, 交易号: {}", paymentId, transactionId);
        Boolean result = paymentService.processPaymentSuccess(paymentId);

        if (!result) {
            log.warn("⚠️ 支付成功处理失败 - 支付ID: {}", paymentId);
            throw new BusinessException("支付成功处理失败");
        }
        log.info("✅ 支付成功处理完成 - 支付ID: {}", paymentId);
        return Result.success("支付成功处理完成", true);
    }

    /**
     * 支付失败处理（内部调用）
     */
    @PostMapping("/{paymentId}/fail")
    @Operation(summary = "支付失败处理", description = "支付失败处理（内部调用）")
    public Result<Boolean> processPaymentFail(
            @Parameter(description = "支付ID") @PathVariable Long paymentId,
            @Parameter(description = "失败原因") @RequestParam(required = false) String failReason) {

        log.info("❌ Feign调用支付失败处理 - 支付ID: {}, 失败原因: {}", paymentId, failReason);
        Boolean result = paymentService.processPaymentFailed(paymentId, failReason);

        if (!result) {
            log.warn("⚠️ 支付失败处理失败 - 支付ID: {}", paymentId);
            throw new BusinessException("支付失败处理失败");
        }
        log.info("✅ 支付失败处理完成 - 支付ID: {}", paymentId);
        return Result.success("支付失败处理完成", true);
    }

    /**
     * 检查支付状态（内部调用）
     */
    @GetMapping("/{paymentId}/status")
    @Operation(summary = "检查支付状态", description = "检查支付状态（内部调用）")
    public Result<Integer> getPaymentStatus(
            @Parameter(description = "支付ID") @PathVariable Long paymentId) {

        log.debug("🔍 Feign调用检查支付状态 - 支付ID: {}", paymentId);
        Integer status = paymentService.getPaymentStatus(paymentId);

        if (status == null) {
            log.warn("⚠️ 支付记录不存在 - 支付ID: {}", paymentId);
            throw new ResourceNotFoundException("Payment", String.valueOf(paymentId));
        }

        return Result.success(status);
    }

    /**
     * 验证支付金额（内部调用）
     */
    @PostMapping("/validate-amount")
    @Operation(summary = "验证支付金额", description = "验证支付金额是否正确（内部调用）")
    public Result<Boolean> validatePaymentAmount(
            @Parameter(description = "支付ID") @RequestParam Long paymentId,
            @Parameter(description = "期望金额") @RequestParam BigDecimal expectedAmount) {

        log.debug("🔍 Feign调用验证支付金额 - 支付ID: {}, 期望金额: {}", paymentId, expectedAmount);
        Boolean result = paymentService.validatePaymentAmount(paymentId, expectedAmount);

        if (result) {
            log.debug("✅ 支付金额验证通过 - 支付ID: {}", paymentId);
        } else {
            log.warn("⚠️ 支付金额验证失败 - 支付ID: {}, 期望金额: {}", paymentId, expectedAmount);
        }
        return Result.success(result ? "支付金额验证通过" : "支付金额验证失败", result);
    }

    /**
     * 获取用户支付统计（内部调用）
     */
    @GetMapping("/stats/user/{userId}")
    @Operation(summary = "获取用户支付统计", description = "获取用户支付统计信息（内部调用）")
    public Result<?> getUserPaymentStats(
            @Parameter(description = "用户ID") @PathVariable Long userId) {

        log.debug("📊 Feign调用获取用户支付统计 - 用户ID: {}", userId);
        Object stats = paymentService.getUserPaymentStats(userId);

        log.debug("✅ 获取用户支付统计成功 - 用户ID: {}", userId);
        return Result.success("获取成功", stats);
    }
}
