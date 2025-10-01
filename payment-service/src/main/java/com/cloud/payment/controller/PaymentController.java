package com.cloud.payment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.common.annotation.DistributedLock;
import com.cloud.common.domain.dto.payment.PaymentDTO;
import com.cloud.common.result.PageResult;
import com.cloud.common.result.Result;
import com.cloud.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

/**
 * 支付RESTful API控制器
 * 提供支付资源的CRUD操作，参考User服务标准架构
 *
 * @author what's up
 */
@Slf4j
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "支付服务", description = "支付资源的RESTful API接口")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 获取支付列表（支持查询参数）
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('SCOPE_payment:read')")
    @Operation(summary = "获取支付列表", description = "获取支付列表，支持分页和查询参数")
    public Result<PageResult<PaymentDTO>> getPayments(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") 
            @Min(value = 1, message = "页码必须大于0") Integer page,
            
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "每页数量必须大于0")
            @Max(value = 100, message = "每页数量不能超过100") Integer size,
            
            @Parameter(description = "用户ID") @RequestParam(required = false)
            @Positive(message = "用户ID必须为正整数") Long userId,
            
            @Parameter(description = "支付状态") @RequestParam(required = false)
            @Min(value = 0, message = "支付状态值错误")
            @Max(value = 9, message = "支付状态值错误") Integer status,
            
            @Parameter(description = "支付渠道") @RequestParam(required = false)
            @Min(value = 0, message = "支付渠道值错误")
            @Max(value = 9, message = "支付渠道值错误") Integer channel,
            
            Authentication authentication) {

        try {
            Page<PaymentDTO> pageResult = paymentService.getPaymentsPage(page, size, userId, status, channel);
            
            PageResult<PaymentDTO> result = PageResult.of(
                    pageResult.getCurrent(),
                    pageResult.getSize(),
                    pageResult.getTotal(),
                    pageResult.getRecords()
            );

            return Result.success(result);
        } catch (Exception e) {
            log.error("获取支付列表失败", e);
            return Result.error("获取支付列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取支付详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('SCOPE_payment:read')")
    @Operation(summary = "获取支付详情", description = "根据支付ID获取支付详细信息")
    public Result<PaymentDTO> getPaymentById(
            @Parameter(description = "支付ID") @PathVariable
            @NotNull(message = "支付ID不能为空")
            @Positive(message = "支付ID必须为正整数") Long id,
            Authentication authentication) {

        try {
            PaymentDTO payment = paymentService.getPaymentById(id);
            if (payment == null) {
                return Result.error("支付记录不存在");
            }
            return Result.success("查询成功", payment);
        } catch (Exception e) {
            log.error("获取支付详情失败，支付ID: {}", id, e);
            return Result.error("获取支付详情失败: " + e.getMessage());
        }
    }

    /**
     * 创建支付记录
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "创建支付记录", description = "创建新的支付记录")
    public Result<Long> createPayment(
            @Parameter(description = "支付信息") @RequestBody
            @Valid @NotNull(message = "支付信息不能为空") PaymentDTO paymentDTO) {

        try {
            Long paymentId = paymentService.createPayment(paymentDTO);
            return Result.success("支付记录创建成功", paymentId);
        } catch (Exception e) {
            log.error("创建支付记录失败", e);
            return Result.error("创建支付记录失败: " + e.getMessage());
        }
    }

    /**
     * 更新支付记录
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "更新支付记录", description = "更新支付记录信息")
    public Result<Boolean> updatePayment(
            @Parameter(description = "支付ID") @PathVariable Long id,
            @Parameter(description = "支付信息") @RequestBody
            @Valid @NotNull(message = "支付信息不能为空") PaymentDTO paymentDTO,
            Authentication authentication) {

        // 确保路径参数与请求体中的ID一致
        paymentDTO.setId(id);

        try {
            Boolean result = paymentService.updatePayment(paymentDTO);
            return Result.success("支付记录更新成功", result);
        } catch (Exception e) {
            log.error("更新支付记录失败，支付ID: {}", id, e);
            return Result.error("更新支付记录失败: " + e.getMessage());
        }
    }

    /**
     * 删除支付记录
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "删除支付记录", description = "删除支付记录")
    public Result<Boolean> deletePayment(
            @Parameter(description = "支付ID") @PathVariable
            @NotNull(message = "支付ID不能为空")
            @Positive(message = "支付ID必须为正整数") Long id) {

        try {
            Boolean result = paymentService.deletePayment(id);
            return Result.success("支付记录删除成功", result);
        } catch (Exception e) {
            log.error("删除支付记录失败，支付ID: {}", id, e);
            return Result.error("删除支付记录失败: " + e.getMessage());
        }
    }

    /**
     * 处理支付成功
     */
    @PostMapping("/{id}/success")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('SCOPE_payment:write')")
    @DistributedLock(
            key = "'payment:success:' + #id",
            waitTime = 5,
            leaseTime = 30,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "支付处理中，请勿重复提交"
    )
    @Operation(summary = "支付成功", description = "处理支付成功状态变更")
    public Result<Boolean> paymentSuccess(
            @Parameter(description = "支付ID") @PathVariable Long id,
            Authentication authentication) {

        try {
            log.info("💳 处理支付成功 - 支付ID: {}", id);
            Boolean result = paymentService.processPaymentSuccess(id);
            
            if (result) {
                log.info("✅ 支付成功处理完成 - 支付ID: {}", id);
                return Result.success("支付成功处理完成", result);
            } else {
                log.warn("⚠️ 支付成功处理失败 - 支付ID: {}", id);
                return Result.error("支付成功处理失败，请检查支付状态");
            }
        } catch (Exception e) {
            log.error("❌ 支付成功处理失败 - 支付ID: {}, 错误: {}", id, e.getMessage(), e);
            return Result.error("支付成功处理失败: " + e.getMessage());
        }
    }

    /**
     * 处理支付失败
     */
    @PostMapping("/{id}/fail")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('SCOPE_payment:write')")
    @DistributedLock(
            key = "'payment:fail:' + #id",
            waitTime = 5,
            leaseTime = 30,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "支付处理中，请勿重复提交"
    )
    @Operation(summary = "支付失败", description = "处理支付失败状态变更")
    public Result<Boolean> paymentFail(
            @Parameter(description = "支付ID") @PathVariable Long id,
            @Parameter(description = "失败原因") @RequestParam(required = false) String failReason,
            Authentication authentication) {

        try {
            log.info("💳 处理支付失败 - 支付ID: {}, 失败原因: {}", id, failReason);
            Boolean result = paymentService.processPaymentFailed(id, failReason);
            
            if (result) {
                log.info("✅ 支付失败处理完成 - 支付ID: {}", id);
                return Result.success("支付失败处理完成", result);
            } else {
                log.warn("⚠️ 支付失败处理失败 - 支付ID: {}", id);
                return Result.error("支付失败处理失败，请检查支付状态");
            }
        } catch (Exception e) {
            log.error("❌ 支付失败处理失败 - 支付ID: {}, 错误: {}", id, e.getMessage(), e);
            return Result.error("支付失败处理失败: " + e.getMessage());
        }
    }

    /**
     * 支付退款
     */
    @PostMapping("/{id}/refund")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('SCOPE_payment:write')")
    @DistributedLock(
            key = "'payment:refund:' + #id",
            waitTime = 3,
            leaseTime = 20,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "退款处理中，请勿重复提交"
    )
    @Operation(summary = "支付退款", description = "处理支付退款")
    public Result<Boolean> refundPayment(
            @Parameter(description = "支付ID") @PathVariable Long id,
            @Parameter(description = "退款金额") @RequestParam BigDecimal refundAmount,
            @Parameter(description = "退款原因") @RequestParam(required = false) String refundReason,
            Authentication authentication) {

        try {
            log.info("💰 处理退款请求 - 支付ID: {}, 退款金额: {}, 原因: {}", id, refundAmount, refundReason);
            Boolean result = paymentService.processRefund(id, refundAmount, refundReason);
            
            if (result) {
                log.info("✅ 退款处理完成 - 支付ID: {}", id);
                return Result.success("退款处理完成", result);
            } else {
                log.warn("⚠️ 退款处理失败 - 支付ID: {}", id);
                return Result.error("退款处理失败，请检查支付状态");
            }
        } catch (Exception e) {
            log.error("❌ 退款处理失败 - 支付ID: {}, 错误: {}", id, e.getMessage(), e);
            return Result.error("退款处理失败: " + e.getMessage());
        }
    }

    /**
     * 根据订单ID查询支付信息
     */
    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('SCOPE_payment:read')")
    @Operation(summary = "根据订单ID查询支付信息", description = "根据订单ID获取支付信息")
    public Result<PaymentDTO> getPaymentByOrderId(
            @Parameter(description = "订单ID") @PathVariable Long orderId,
            Authentication authentication) {

        try {
            PaymentDTO payment = paymentService.getPaymentByOrderId(orderId);
            if (payment == null) {
                return Result.error("未找到该订单的支付记录");
            }
            return Result.success("查询成功", payment);
        } catch (Exception e) {
            log.error("根据订单ID查询支付信息失败，订单ID: {}", orderId, e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 支付风控检查
     */
    @PostMapping("/risk-check")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('SCOPE_payment:read')")
    @DistributedLock(
            key = "'payment:risk:user:' + #userId",
            waitTime = 0,
            leaseTime = 3,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "风控检查系统繁忙，请稍后再试"
    )
    @Operation(summary = "支付风控检查", description = "执行支付风控检查")
    public Result<Boolean> riskCheck(
            @Parameter(description = "用户ID") @RequestParam Long userId,
            @Parameter(description = "支付金额") @RequestParam BigDecimal amount,
            @Parameter(description = "支付方式") @RequestParam String paymentMethod,
            Authentication authentication) {

        try {
            log.info("🛡️ 支付风控检查 - 用户ID: {}, 金额: {}, 方式: {}", userId, amount, paymentMethod);
            Boolean riskPassed = paymentService.riskCheck(userId, amount, paymentMethod);
            
            if (riskPassed) {
                log.info("✅ 风控检查通过 - 用户ID: {}, 金额: {}", userId, amount);
                return Result.success("风控检查通过", true);
            } else {
                log.warn("⚠️ 风控检查不通过 - 用户ID: {}, 金额: {}", userId, amount);
                return Result.success("风控检查不通过", false);
            }
        } catch (Exception e) {
            log.error("❌ 支付风控检查失败 - 用户ID: {}", userId, e);
            return Result.error("风控检查失败: " + e.getMessage());
        }
    }
}
