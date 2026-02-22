package com.cloud.payment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.common.annotation.DistributedLock;
import com.cloud.common.domain.dto.payment.PaymentDTO;
import com.cloud.common.exception.BusinessException;
import com.cloud.common.exception.ResourceNotFoundException;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payment API", description = "Payment management APIs")
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('SCOPE_payment:read')")
    @Operation(summary = "Get payment list", description = "Get paged payment records")
    public Result<PageResult<PaymentDTO>> getPayments(
            @Parameter(description = "Page index") @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "Page index must be greater than or equal to 1") Integer page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "Page size must be greater than or equal to 1")
            @Max(value = 100, message = "Page size must be less than or equal to 100") Integer size,
            @Parameter(description = "User id") @RequestParam(required = false)
            @Positive(message = "User id must be positive") Long userId,
            @Parameter(description = "Payment status") @RequestParam(required = false)
            @Min(value = 0, message = "Status must be greater than or equal to 0")
            @Max(value = 9, message = "Status must be less than or equal to 9") Integer status,
            @Parameter(description = "Payment channel") @RequestParam(required = false)
            @Min(value = 0, message = "Channel must be greater than or equal to 0")
            @Max(value = 9, message = "Channel must be less than or equal to 9") Integer channel) {

        Page<PaymentDTO> pageResult = paymentService.getPaymentsPage(page, size, userId, status, channel);
        PageResult<PaymentDTO> result = PageResult.of(
                pageResult.getCurrent(),
                pageResult.getSize(),
                pageResult.getTotal(),
                pageResult.getRecords()
        );
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('SCOPE_payment:read')")
    @Operation(summary = "Get payment detail", description = "Get payment detail by id")
    public Result<PaymentDTO> getPaymentById(
            @Parameter(description = "Payment id") @PathVariable
            @NotNull(message = "Payment id cannot be null")
            @Positive(message = "Payment id must be positive") Long id) {

        PaymentDTO payment = paymentService.getPaymentById(id);
        if (payment == null) {
            throw new ResourceNotFoundException("Payment", String.valueOf(id));
        }
        return Result.success("Query payment success", payment);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create payment", description = "Create a payment record")
    public Result<Long> createPayment(
            @Parameter(description = "Payment payload") @RequestBody
            @Valid @NotNull(message = "Payment payload cannot be null") PaymentDTO paymentDTO) {

        Long paymentId = paymentService.createPayment(paymentDTO);
        return Result.success("Create payment success", paymentId);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update payment", description = "Update payment by id")
    public Result<Boolean> updatePayment(
            @Parameter(description = "Payment id") @PathVariable Long id,
            @Parameter(description = "Payment payload") @RequestBody
            @Valid @NotNull(message = "Payment payload cannot be null") PaymentDTO paymentDTO) {

        paymentDTO.setId(id);
        Boolean updated = paymentService.updatePayment(paymentDTO);
        return Result.success("Update payment success", updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete payment", description = "Delete payment by id")
    public Result<Boolean> deletePayment(
            @Parameter(description = "Payment id") @PathVariable
            @NotNull(message = "Payment id cannot be null")
            @Positive(message = "Payment id must be positive") Long id) {

        Boolean deleted = paymentService.deletePayment(id);
        return Result.success("Delete payment success", deleted);
    }

    @PostMapping("/{id}/success")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('SCOPE_payment:write')")
    @DistributedLock(
            key = "'payment:success:' + #id",
            waitTime = 5,
            leaseTime = 30,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "Acquire payment success lock failed"
    )
    @Operation(summary = "Mark payment success", description = "Mark payment as success")
    public Result<Boolean> paymentSuccess(@Parameter(description = "Payment id") @PathVariable Long id) {
        Boolean result = paymentService.processPaymentSuccess(id);
        if (!Boolean.TRUE.equals(result)) {
            throw new BusinessException("Process payment success failed");
        }
        return Result.success("Payment marked as success", true);
    }

    @PostMapping("/{id}/fail")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('SCOPE_payment:write')")
    @DistributedLock(
            key = "'payment:fail:' + #id",
            waitTime = 5,
            leaseTime = 30,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "Acquire payment fail lock failed"
    )
    @Operation(summary = "Mark payment failed", description = "Mark payment as failed")
    public Result<Boolean> paymentFail(
            @Parameter(description = "Payment id") @PathVariable Long id,
            @Parameter(description = "Failure reason") @RequestParam(required = false) String failReason) {

        Boolean result = paymentService.processPaymentFailed(id, failReason);
        if (!Boolean.TRUE.equals(result)) {
            throw new BusinessException("Process payment fail failed");
        }
        return Result.success("Payment marked as failed", true);
    }

    @PostMapping("/{id}/refund")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('SCOPE_payment:write')")
    @DistributedLock(
            key = "'payment:refund:' + #id",
            waitTime = 3,
            leaseTime = 20,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "Acquire payment refund lock failed"
    )
    @Operation(summary = "Refund payment", description = "Create payment refund")
    public Result<Boolean> refundPayment(
            @Parameter(description = "Payment id") @PathVariable Long id,
            @Parameter(description = "Refund amount") @RequestParam BigDecimal refundAmount,
            @Parameter(description = "Refund reason") @RequestParam(required = false) String refundReason) {

        Boolean result = paymentService.processRefund(id, refundAmount, refundReason);
        if (!Boolean.TRUE.equals(result)) {
            throw new BusinessException("Process payment refund failed");
        }
        return Result.success("Refund processed", true);
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('SCOPE_payment:read')")
    @Operation(summary = "Get payment by order", description = "Get payment record by order id")
    public Result<PaymentDTO> getPaymentByOrderId(@Parameter(description = "Order id") @PathVariable Long orderId) {
        PaymentDTO payment = paymentService.getPaymentByOrderId(orderId);
        if (payment == null) {
            throw new ResourceNotFoundException("Payment for order", String.valueOf(orderId));
        }
        return Result.success("Query payment success", payment);
    }

    @PostMapping("/risk-check")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('SCOPE_payment:read')")
    @DistributedLock(
            key = "'payment:risk:user:' + #userId",
            waitTime = 0,
            leaseTime = 3,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "Acquire payment risk lock failed"
    )
    @Operation(summary = "Run payment risk check", description = "Run risk checks before payment")
    public Result<Boolean> riskCheck(
            @Parameter(description = "User id") @RequestParam Long userId,
            @Parameter(description = "Payment amount") @RequestParam BigDecimal amount,
            @Parameter(description = "Payment method") @RequestParam String paymentMethod) {

        Boolean riskPassed = paymentService.riskCheck(userId, amount, paymentMethod);
        if (!Boolean.TRUE.equals(riskPassed)) {
            log.warn("Risk check failed: userId={}, amount={}, paymentMethod={}", userId, amount, paymentMethod);
        }
        return Result.success(riskPassed ? "Risk check passed" : "Risk check failed", Boolean.TRUE.equals(riskPassed));
    }
}
