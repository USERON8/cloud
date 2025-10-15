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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * 退款控制器
 *
 * @author CloudDevAgent
 * @since 2025-01-15
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/refund")
@RequiredArgsConstructor
@Tag(name = "退款管理", description = "退款申请、审核、处理相关接口")
public class RefundController {

    private final RefundService refundService;

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('SCOPE_read')")
    @Operation(summary = "创建退款申请", description = "用户提交退款/退货申请")
    public Result<Long> createRefund(@Valid @RequestBody RefundCreateDTO dto) {
        Long userId = getCurrentUserId();
        Long refundId = refundService.createRefund(userId, dto);
        return Result.success("退款申请已提交", refundId);
    }

    @PostMapping("/audit/{refundId}")
    @PreAuthorize("hasAuthority('ROLE_MERCHANT')")
    @Operation(summary = "审核退款申请", description = "商家审核用户的退款申请")
    public Result<Boolean> auditRefund(
            @Parameter(description = "退款单ID") @PathVariable Long refundId,
            @Parameter(description = "是否通过") @RequestParam Boolean approved,
            @Parameter(description = "审核备注") @RequestParam(required = false) String auditRemark) {

        Long merchantId = getCurrentUserId();
        Boolean result = refundService.auditRefund(refundId, merchantId, approved, auditRemark);
        return Result.success(approved ? "审核通过" : "审核拒绝", result);
    }

    @PostMapping("/cancel/{refundId}")
    @PreAuthorize("hasAuthority('SCOPE_read')")
    @Operation(summary = "取消退款申请", description = "用户取消自己的退款申请")
    public Result<Boolean> cancelRefund(@Parameter(description = "退款单ID") @PathVariable Long refundId) {
        Long userId = getCurrentUserId();
        Boolean result = refundService.cancelRefund(refundId, userId);
        return Result.success("退款申请已取消", result);
    }

    @GetMapping("/{refundId}")
    @PreAuthorize("hasAuthority('SCOPE_read')")
    @Operation(summary = "查询退款详情", description = "根据退款单ID查询退款详情")
    public Result<Refund> getRefundById(@Parameter(description = "退款单ID") @PathVariable Long refundId) {
        Refund refund = refundService.getRefundById(refundId);
        if (refund == null) {
            throw new BusinessException("退款单不存在");
        }
        return Result.success(refund);
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAuthority('SCOPE_read')")
    @Operation(summary = "根据订单查询退款", description = "根据订单ID查询退款信息")
    public Result<Refund> getRefundByOrderId(@Parameter(description = "订单ID") @PathVariable Long orderId) {
        Refund refund = refundService.getRefundByOrderId(orderId);
        if (refund == null) {
            return Result.success("该订单暂无退款记录", null);
        }
        return Result.success(refund);
    }

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('SCOPE_read')")
    @Operation(summary = "查询退款列表（用户）", description = "用户查询自己的退款列表")
    public Result<PageResult<RefundVO>> listUserRefunds(RefundPageDTO pageDTO) {
        Long userId = getCurrentUserId();
        pageDTO.setUserId(userId); // 自动填充当前用户ID
        PageResult<RefundVO> result = refundService.pageQuery(pageDTO);
        return Result.success(result);
    }

    @GetMapping("/merchant/list")
    @PreAuthorize("hasAuthority('ROLE_MERCHANT')")
    @Operation(summary = "查询退款列表（商家）", description = "商家查询待处理的退款列表")
    public Result<PageResult<RefundVO>> listMerchantRefunds(RefundPageDTO pageDTO) {
        Long merchantId = getCurrentUserId();
        pageDTO.setMerchantId(merchantId); // 自动填充当前商家ID
        PageResult<RefundVO> result = refundService.pageQuery(pageDTO);
        return Result.success(result);
    }

    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String userId = jwt.getClaim("user_id");
            if (userId != null) {
                return Long.parseLong(userId);
            }
        }
        // 默认返回测试用户ID
        return 1L;
    }
}
