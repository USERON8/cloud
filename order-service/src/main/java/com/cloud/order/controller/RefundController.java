package com.cloud.order.controller;

import com.cloud.common.exception.BusinessException;
import com.cloud.common.result.PageResult;
import com.cloud.common.result.Result;
import com.cloud.common.security.SecurityPermissionUtils;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/refund")
@RequiredArgsConstructor
@Tag(name = "Refund Management", description = "Refund APIs")
public class RefundController {

    private final RefundService refundService;

    @PostMapping("/create")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication)")
    @Operation(summary = "Create refund", description = "Create a refund request")
    public Result<Long> createRefund(@Valid @RequestBody RefundCreateDTO dto, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        Long refundId = refundService.createRefund(userId, dto);
        return Result.success("refund created", refundId);
    }

    @PostMapping("/audit/{refundId}")
    @PreAuthorize("@permissionManager.hasMerchantAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @Operation(summary = "Audit refund", description = "Merchant audits a refund request")
    public Result<Boolean> auditRefund(
            @Parameter(description = "Refund ID") @PathVariable Long refundId,
            @Parameter(description = "Audit decision") @RequestParam Boolean approved,
            @Parameter(description = "Audit remark") @RequestParam(required = false) String auditRemark,
            Authentication authentication) {

        Long merchantId;
        if (isAdmin(authentication)) {
            Refund targetRefund = refundService.getRefundById(refundId);
            if (targetRefund == null) {
                throw new BusinessException("refund not found");
            }
            if (targetRefund.getMerchantId() == null) {
                throw new BusinessException("refund has no merchant owner");
            }
            merchantId = targetRefund.getMerchantId();
        } else {
            merchantId = getCurrentUserId(authentication);
        }
        Boolean result = refundService.auditRefund(refundId, merchantId, approved, auditRemark);
        return Result.success(approved ? "refund approved" : "refund rejected", result);
    }

    @PostMapping("/cancel/{refundId}")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication)")
    @Operation(summary = "Cancel refund", description = "Cancel refund by user owner")
    public Result<Boolean> cancelRefund(@Parameter(description = "Refund ID") @PathVariable Long refundId,
                                        Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        Boolean result = refundService.cancelRefund(refundId, userId);
        return Result.success("refund cancelled", result);
    }

    @GetMapping("/{refundId}")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasMerchantAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @Operation(summary = "Get refund", description = "Get refund detail by ID")
    public Result<Refund> getRefundById(@Parameter(description = "Refund ID") @PathVariable Long refundId,
                                        Authentication authentication) {
        Refund refund = refundService.getRefundById(refundId);
        if (refund == null) {
            throw new BusinessException("refund not found");
        }
        assertCanReadRefund(refund, authentication);
        return Result.success(refund);
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasMerchantAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @Operation(summary = "Get order refund", description = "Get refund by order ID")
    public Result<Refund> getRefundByOrderId(@Parameter(description = "Order ID") @PathVariable Long orderId,
                                             Authentication authentication) {
        Refund refund = refundService.getRefundByOrderId(orderId);
        if (refund == null) {
            return Result.success("refund not found for current order", null);
        }
        assertCanReadRefund(refund, authentication);
        return Result.success(refund);
    }

    @GetMapping("/list")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication)")
    @Operation(summary = "List user refunds", description = "Get current user refunds with pagination")
    public Result<PageResult<RefundVO>> listUserRefunds(RefundPageDTO pageDTO, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        pageDTO.setUserId(userId);
        PageResult<RefundVO> result = refundService.pageQuery(pageDTO);
        return Result.success(result);
    }

    @GetMapping("/merchant/list")
    @PreAuthorize("@permissionManager.hasMerchantAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @Operation(summary = "List merchant refunds", description = "Get current merchant refunds with pagination")
    public Result<PageResult<RefundVO>> listMerchantRefunds(RefundPageDTO pageDTO, Authentication authentication) {
        if (!isAdmin(authentication)) {
            Long merchantId = getCurrentUserId(authentication);
            pageDTO.setMerchantId(merchantId);
        }
        PageResult<RefundVO> result = refundService.pageQuery(pageDTO);
        return Result.success(result);
    }

    private boolean isAdmin(Authentication authentication) {
        return SecurityPermissionUtils.isAdmin(authentication);
    }

    private boolean isMerchant(Authentication authentication) {
        return SecurityPermissionUtils.isMerchant(authentication);
    }

    private Long getCurrentUserId(Authentication authentication) {
        String userId = SecurityPermissionUtils.getCurrentUserId(authentication);
        if (userId == null || userId.isBlank()) {
            throw new BusinessException("current user not found in token");
        }
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException ignored) {
            throw new BusinessException("invalid user_id in token");
        }
    }

    private void assertCanReadRefund(Refund refund, Authentication authentication) {
        if (refund == null || isAdmin(authentication)) {
            return;
        }

        Long currentUserId = getCurrentUserId(authentication);
        if (Objects.equals(currentUserId, refund.getUserId())) {
            return;
        }
        if (isMerchant(authentication) && Objects.equals(currentUserId, refund.getMerchantId())) {
            return;
        }
        throw new BusinessException("forbidden to access other user's refund");
    }
}
