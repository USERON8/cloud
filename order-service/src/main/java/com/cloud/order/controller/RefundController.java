package com.cloud.order.controller;

import com.cloud.common.exception.BusinessException;
import com.cloud.common.result.PageResult;
import com.cloud.common.result.Result;
import com.cloud.order.dto.RefundCreateDTO;
import com.cloud.order.dto.RefundPageDTO;
import com.cloud.order.module.entity.Refund;
import com.cloud.order.service.RefundService;
import com.cloud.order.vo.RefundVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/refund")
@RequiredArgsConstructor
@Tag(name = "Refund Management", description = "Refund APIs")
public class RefundController {

    private final RefundService refundService;

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('SCOPE_read')")
    @Operation(summary = "Create refund", description = "Create a refund request")
    public Result<Long> createRefund(@Valid @RequestBody RefundCreateDTO dto) {
        Long userId = getCurrentUserId();
        Long refundId = refundService.createRefund(userId, dto);
        return Result.success("refund created", refundId);
    }

    @PostMapping("/audit/{refundId}")
    @PreAuthorize("hasAuthority('ROLE_MERCHANT')")
    @Operation(summary = "Audit refund", description = "Merchant audits a refund request")
    public Result<Boolean> auditRefund(
            @Parameter(description = "Refund ID") @PathVariable Long refundId,
            @Parameter(description = "Audit decision") @RequestParam Boolean approved,
            @Parameter(description = "Audit remark") @RequestParam(required = false) String auditRemark) {

        Long merchantId = getCurrentUserId();
        Boolean result = refundService.auditRefund(refundId, merchantId, approved, auditRemark);
        return Result.success(approved ? "refund approved" : "refund rejected", result);
    }

    @PostMapping("/cancel/{refundId}")
    @PreAuthorize("hasAuthority('SCOPE_read')")
    @Operation(summary = "Cancel refund", description = "Cancel refund by user")
    public Result<Boolean> cancelRefund(@Parameter(description = "Refund ID") @PathVariable Long refundId) {
        Long userId = getCurrentUserId();
        Boolean result = refundService.cancelRefund(refundId, userId);
        return Result.success("refund cancelled", result);
    }

    @GetMapping("/{refundId}")
    @PreAuthorize("hasAuthority('SCOPE_read')")
    @Operation(summary = "Get refund", description = "Get refund detail by ID")
    public Result<Refund> getRefundById(@Parameter(description = "Refund ID") @PathVariable Long refundId) {
        Refund refund = refundService.getRefundById(refundId);
        if (refund == null) {
            throw new BusinessException("refund not found");
        }
        return Result.success(refund);
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAuthority('SCOPE_read')")
    @Operation(summary = "Get order refund", description = "Get refund by order ID")
    public Result<Refund> getRefundByOrderId(@Parameter(description = "Order ID") @PathVariable Long orderId) {
        Refund refund = refundService.getRefundByOrderId(orderId);
        if (refund == null) {
            return Result.success("refund not found for current order", null);
        }
        return Result.success(refund);
    }

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('SCOPE_read')")
    @Operation(summary = "List user refunds", description = "Get current user refunds with pagination")
    public Result<PageResult<RefundVO>> listUserRefunds(RefundPageDTO pageDTO) {
        Long userId = getCurrentUserId();
        pageDTO.setUserId(userId);
        PageResult<RefundVO> result = refundService.pageQuery(pageDTO);
        return Result.success(result);
    }

    @GetMapping("/merchant/list")
    @PreAuthorize("hasAuthority('ROLE_MERCHANT')")
    @Operation(summary = "List merchant refunds", description = "Get current merchant refunds with pagination")
    public Result<PageResult<RefundVO>> listMerchantRefunds(RefundPageDTO pageDTO) {
        Long merchantId = getCurrentUserId();
        pageDTO.setMerchantId(merchantId);
        PageResult<RefundVO> result = refundService.pageQuery(pageDTO);
        return Result.success(result);
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            Object claimValue = jwt.getClaims().get("user_id");
            if (claimValue instanceof Number number) {
                return number.longValue();
            }
            if (claimValue instanceof String value && !value.isBlank()) {
                try {
                    return Long.parseLong(value);
                } catch (NumberFormatException ignored) {
                    throw new BusinessException("invalid user_id in token");
                }
            }
        }
        throw new BusinessException("current user not found in token");
    }
}