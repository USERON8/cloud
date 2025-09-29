package com.cloud.payment.controller;

import com.cloud.common.domain.event.payment.PaymentSuccessEvent;
import com.cloud.common.result.Result;
import com.cloud.common.utils.StringUtils;
import com.cloud.common.utils.UserContextUtils;
import com.cloud.payment.messaging.producer.PaymentEventProducer;
import com.cloud.payment.module.entity.Payment;
import com.cloud.payment.service.PaymentService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 支付操作控制器
 * 提供支付状态变更相关接口
 */
@Slf4j
@RestController
@RequestMapping("/payment/operation")
@RequiredArgsConstructor
@Tag(name = "支付操作", description = "支付状态变更操作接口")
public class PaymentOperationController {

    private final PaymentService paymentService;
    private final PaymentEventProducer paymentEventProducer;

    /**
     * 支付成功处理
     *
     * @param paymentId     支付ID
     * @param currentUserId 当前用户ID
     * @return 操作结果
     */
    @PostMapping("/success/{paymentId}")
    @Operation(summary = "支付成功", description = "处理支付成功状态变更")
    @Parameters({
            @Parameter(name = "paymentId", description = "支付ID", required = true)
    })
    @ApiResponse(responseCode = "200", description = "操作成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<String> paymentSuccess(@PathVariable Long paymentId) {
        try {
            String currentUserId = UserContextUtils.getCurrentUserId();
            String currentUsername = UserContextUtils.getCurrentUsername();
            
            log.info("处理支付成功，支付ID: {}，操作人: {} (ID: {})", paymentId, currentUsername, currentUserId);

            // 1. 验证支付是否存在
            Payment payment = paymentService.getById(paymentId);
            if (payment == null) {
                return Result.error("支付记录不存在");
            }
            
            // 2. 验证支付状态是否为待支付
            if (!payment.getStatus().equals(0)) {
                return Result.error("支付状态不是待支付，无法执行成功操作");
            }
            
            // 3. 更新支付状态为成功
            boolean updated = paymentService.lambdaUpdate()
                    .eq(Payment::getId, paymentId)
                    .set(Payment::getStatus, 1) // 1-成功
                    .update();
                    
            if (!updated) {
                return Result.error("支付状态更新失败");
            }
            
            // 4. 发送支付成功事件给订单服务
            try {
                // 重新查询支付记录获取完整信息
                Payment updatedPayment = paymentService.getById(paymentId);
                if (updatedPayment != null) {
                    PaymentSuccessEvent successEvent = PaymentSuccessEvent.builder()
                            .paymentId(updatedPayment.getId())
                            .paymentNo("PAY_" + updatedPayment.getId())
                            .orderId(updatedPayment.getOrderId())
                            .orderNo("ORDER_" + updatedPayment.getOrderId())
                            .userId(updatedPayment.getUserId())
                            .userName("User_" + updatedPayment.getUserId())
                            .paymentAmount(updatedPayment.getAmount())
                            .actualAmount(updatedPayment.getAmount())
                            .paymentMethod(updatedPayment.getChannel())
                            .paymentMethodName(getPaymentMethodName(updatedPayment.getChannel()))
                            .paymentStatus(1) // 支付成功
                            .beforeStatus(0) // 待支付
                            .afterStatus(1)  // 已支付
                            .paymentTime(LocalDateTime.now())
                            .completedTime(LocalDateTime.now())
                            .operator(currentUserId)
                            .traceId(StringUtils.generateTraceId())
                            .build();

                    paymentEventProducer.sendPaymentSuccessEvent(successEvent);
                    log.info("支付成功事件发送完成 - 支付ID: {}, 订单ID: {}", paymentId, updatedPayment.getOrderId());
                }
            } catch (Exception e) {
                log.warn("发送支付成功事件失败 - 支付ID: {}, 错误: {}", paymentId, e.getMessage());
            }

            log.info("支付成功处理完成，支付ID: {}，操作人: {}", paymentId, currentUserId);
            return Result.success("支付成功处理完成");
        } catch (Exception e) {
            String currentUserId = UserContextUtils.getCurrentUserId();
            log.error("支付成功处理异常，支付ID: {}，操作人: {}", paymentId, currentUserId, e);
            return Result.error("支付成功处理失败: " + e.getMessage());
        }
    }

    /**
     * 支付失败处理
     *
     * @param paymentId     支付ID
     * @param currentUserId 当前用户ID
     * @return 操作结果
     */
    @PostMapping("/fail/{paymentId}")
    @Operation(summary = "支付失败", description = "处理支付失败状态变更")
    @Parameters({
            @Parameter(name = "paymentId", description = "支付ID", required = true)
    })
    @ApiResponse(responseCode = "200", description = "操作成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<String> paymentFail(@PathVariable Long paymentId) {
        try {
            String currentUserId = UserContextUtils.getCurrentUserId();
            String currentUsername = UserContextUtils.getCurrentUsername();
            log.info("处理支付失败，支付ID: {}，操作人: {} (ID: {})", paymentId, currentUsername, currentUserId);

            // 1. 验证支付是否存在
            Payment payment = paymentService.getById(paymentId);
            if (payment == null) {
                return Result.error("支付记录不存在");
            }

            // 2. 验证支付状态是否为待支付
            if (!payment.getStatus().equals(0)) {
                return Result.error("支付状态不是待支付，无法执行失败操作");
            }

            // 3. 更新支付状态为失败
            boolean updated = paymentService.lambdaUpdate()
                    .eq(Payment::getId, paymentId)
                    .set(Payment::getStatus, 2) // 2-失败
                    .update();

            if (!updated) {
                return Result.error("支付状态更新失败");
            }

            // 4. 发送支付失败事件给订单服务
            try {
                // 重新查询支付记录获取完整信息
                Payment updatedPayment = paymentService.getById(paymentId);
                if (updatedPayment != null) {
                    PaymentSuccessEvent failEvent = PaymentSuccessEvent.builder()
                            .paymentId(updatedPayment.getId())
                            .paymentNo("PAY_" + updatedPayment.getId())
                            .orderId(updatedPayment.getOrderId())
                            .orderNo("ORDER_" + updatedPayment.getOrderId())
                            .userId(updatedPayment.getUserId())
                            .userName("User_" + updatedPayment.getUserId())
                            .paymentAmount(updatedPayment.getAmount())
                            .actualAmount(BigDecimal.ZERO) // 失败时实际支付金额为0
                            .paymentMethod(updatedPayment.getChannel())
                            .paymentMethodName(getPaymentMethodName(updatedPayment.getChannel()))
                            .paymentStatus(2) // 支付失败
                            .beforeStatus(0) // 待支付
                            .afterStatus(2)  // 已失败
                            .paymentTime(LocalDateTime.now())
                            .completedTime(LocalDateTime.now())
                            .operator(currentUserId)
                            .traceId(StringUtils.generateTraceId())
                            .description("支付失败")
                            .build();

                    // 使用支付失败事件发送
                    paymentEventProducer.sendPaymentFailedEvent(
                            com.cloud.common.domain.event.payment.PaymentChangeEvent.builder()
                                    .paymentId(updatedPayment.getId())
                                    .orderId(updatedPayment.getOrderId())
                                    .userId(updatedPayment.getUserId())
                                    .amount(updatedPayment.getAmount())
                                    .paymentMethod(getPaymentMethodName(updatedPayment.getChannel()))
                                    .status("FAILED")
                                    .beforeStatus("PENDING")
                                    .afterStatus("FAILED")
                                    .changeTime(LocalDateTime.now())
                                    .operator(currentUserId)
                                    .traceId(StringUtils.generateTraceId())
                                    .remark("支付失败处理")
                                    .build()
                    );
                    log.info("支付失败事件发送完成 - 支付ID: {}, 订单ID: {}", paymentId, updatedPayment.getOrderId());
                }
            } catch (Exception e) {
                log.warn("发送支付失败事件失败 - 支付ID: {}, 错误: {}", paymentId, e.getMessage());
            }

            log.info("支付失败处理完成，支付ID: {}，操作人: {} (ID: {})", paymentId, currentUsername, currentUserId);
            return Result.success("支付失败处理完成");
        } catch (Exception e) {
            String currentUserId = UserContextUtils.getCurrentUserId();
            log.error("支付失败处理异常，支付ID: {}，操作人: {}", paymentId, currentUserId, e);
            return Result.error("支付失败处理失败: " + e.getMessage());
        }
    }

    /**
     * 支付退款处理
     *
     * @param paymentId     支付ID
     * @param currentUserId 当前用户ID
     * @return 操作结果
     */
    @PostMapping("/refund/{paymentId}")
    @Operation(summary = "支付退款", description = "处理支付退款状态变更")
    @Parameters({
            @Parameter(name = "paymentId", description = "支付ID", required = true)
    })
    @ApiResponse(responseCode = "200", description = "操作成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<String> paymentRefund(@PathVariable Long paymentId) {
        try {
            String currentUserId = UserContextUtils.getCurrentUserId();
            String currentUsername = UserContextUtils.getCurrentUsername();
            log.info("处理支付退款，支付ID: {}，操作人: {} (ID: {})", paymentId, currentUsername, currentUserId);

            // 1. 验证支付是否存在
            Payment payment = paymentService.getById(paymentId);
            if (payment == null) {
                return Result.error("支付记录不存在");
            }

            // 2. 验证支付状态是否为成功
            if (!payment.getStatus().equals(1)) {
                return Result.error("支付状态不是成功，无法执行退款操作");
            }

            // 3. 更新支付状态为已退款
            boolean updated = paymentService.lambdaUpdate()
                    .eq(Payment::getId, paymentId)
                    .set(Payment::getStatus, 3) // 3-已退款
                    .update();

            if (!updated) {
                return Result.error("支付状态更新失败");
            }

            // 4. 发送支付退款事件给订单服务
            try {
                // 重新查询支付记录获取完整信息
                Payment refundedPayment = paymentService.getById(paymentId);
                if (refundedPayment != null) {
                    // 发送退款成功事件
                    paymentEventProducer.sendRefundSuccessEvent(
                            com.cloud.common.domain.event.payment.PaymentChangeEvent.builder()
                                    .paymentId(refundedPayment.getId())
                                    .orderId(refundedPayment.getOrderId())
                                    .userId(refundedPayment.getUserId())
                                    .amount(refundedPayment.getAmount())
                                    .paymentMethod(getPaymentMethodName(refundedPayment.getChannel()))
                                    .status("REFUNDED")
                                    .beforeStatus("SUCCESS")
                                    .afterStatus("REFUNDED")
                                    .changeTime(LocalDateTime.now())
                                    .operator(currentUserId)
                                    .traceId(StringUtils.generateTraceId())
                                    .remark("支付退款处理")
                                    .build()
                    );
                    log.info("支付退款事件发送完成 - 支付ID: {}, 订单ID: {}", paymentId, refundedPayment.getOrderId());
                }
            } catch (Exception e) {
                log.warn("发送支付退款事件失败 - 支付ID: {}, 错误: {}", paymentId, e.getMessage());
            }

            log.info("支付退款处理完成，支付ID: {}，操作人: {} (ID: {})", paymentId, currentUsername, currentUserId);
            return Result.success("支付退款处理完成");
        } catch (Exception e) {
            String currentUserId = UserContextUtils.getCurrentUserId();
            log.error("支付退款处理异常，支付ID: {}，操作人: {}", paymentId, currentUserId, e);
            return Result.error("支付退款处理失败: " + e.getMessage());
        }
    }

    /**
     * 获取支付方式名称
     *
     * @param channel 支付渠道
     * @return 支付方式名称
     */
    private String getPaymentMethodName(Integer channel) {
        if (channel == null) {
            return "未知";
        }
        return switch (channel) {
            case 1 -> "支付宝";
            case 2 -> "微信支付";
            case 3 -> "银行卡";
            default -> "其他";
        };
    }
}