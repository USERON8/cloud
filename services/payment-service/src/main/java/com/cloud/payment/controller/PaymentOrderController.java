package com.cloud.payment.controller;

import com.cloud.common.domain.dto.payment.PaymentOrderCommandDTO;
import com.cloud.common.domain.dto.payment.PaymentRefundCommandDTO;
import com.cloud.common.domain.vo.payment.PaymentCheckoutSessionVO;
import com.cloud.common.domain.vo.payment.PaymentOrderVO;
import com.cloud.common.domain.vo.payment.PaymentRefundVO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;
import com.cloud.common.result.Result;
import com.cloud.common.security.SecurityPermissionUtils;
import com.cloud.payment.service.PaymentOrderService;
import com.cloud.payment.service.support.PaymentSecurityCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
@Tag(name = "支付接口", description = "支付单和退款接口")
@ApiResponses({
  @ApiResponse(responseCode = "400", description = "请求参数或支付状态无效"),
  @ApiResponse(responseCode = "401", description = "需要认证"),
  @ApiResponse(responseCode = "403", description = "权限不足"),
  @ApiResponse(responseCode = "404", description = "支付单或退款单不存在"),
  @ApiResponse(responseCode = "409", description = "支付状态冲突"),
  @ApiResponse(responseCode = "500", description = "支付服务内部错误")
})
public class PaymentOrderController {

  private final PaymentOrderService paymentOrderService;
  private final PaymentSecurityCacheService paymentSecurityCacheService;

  @PostMapping("/payment-orders")
  @PreAuthorize("isAuthenticated() and (hasAuthority('admin:all') or hasAuthority('order:create'))")
  @Operation(summary = "创建支付单")
  public Result<Long> createPaymentOrder(
      @Valid @RequestBody PaymentOrderCommandDTO command, Authentication authentication) {
    Long currentUserId = requireCurrentUserId(authentication);
    if (!SecurityPermissionUtils.isAdmin(authentication)) {
      if (command.getUserId() == null) {
        command.setUserId(currentUserId);
      } else if (!Objects.equals(command.getUserId(), currentUserId)) {
        throw new BizException(
            ResultCode.FORBIDDEN, "forbidden to create payment order for another user");
      }
    } else if (command.getUserId() == null) {
      throw new BizException(
          ResultCode.BAD_REQUEST, "userId is required for admin payment creation");
    }
    return Result.success(paymentOrderService.createPaymentOrder(command));
  }

  @GetMapping("/payment-orders/{paymentNo}")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "按支付单号查询支付单")
  public Result<PaymentOrderVO> getPaymentOrderByNo(
      @PathVariable String paymentNo, Authentication authentication) {
    PaymentOrderVO order = paymentOrderService.getPaymentOrderByNo(paymentNo);
    if (order == null) {
      throw new BizException(ResultCode.NOT_FOUND, "payment order not found");
    }
    if (!canReadOrder(authentication, order)) {
      throw new BizException(ResultCode.FORBIDDEN, "forbidden to query other user's payment order");
    }
    return Result.success(order);
  }

  @GetMapping("/payment-orders")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "按订单号查询支付单")
  public Result<PaymentOrderVO> getPaymentOrderByOrderNo(
      @RequestParam String mainOrderNo,
      @RequestParam String subOrderNo,
      Authentication authentication) {
    PaymentOrderVO order = paymentOrderService.getPaymentOrderByOrderNo(mainOrderNo, subOrderNo);
    if (order == null) {
      throw new BizException(ResultCode.NOT_FOUND, "payment order not found");
    }
    if (!canReadOrder(authentication, order)) {
      throw new BizException(ResultCode.FORBIDDEN, "forbidden to query other user's payment order");
    }
    return Result.success(order);
  }

  @PostMapping("/payment-orders/{paymentNo}/checkout-sessions")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "为支付单创建收银台会话")
  public Result<PaymentCheckoutSessionVO> createCheckoutSession(
      @PathVariable String paymentNo, Authentication authentication) {
    PaymentOrderVO order = paymentOrderService.getPaymentOrderByNo(paymentNo);
    if (order == null) {
      throw new BizException(ResultCode.NOT_FOUND, "payment order not found");
    }
    if (!canReadOrder(authentication, order)) {
      throw new BizException(
          ResultCode.FORBIDDEN, "forbidden to create checkout session for other user's payment");
    }
    return Result.success(paymentOrderService.createCheckoutSession(paymentNo));
  }

  @GetMapping("/payment-orders/{paymentNo}/status")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "查询支付单状态")
  public Result<Map<String, Object>> getPaymentStatus(
      @PathVariable String paymentNo, Authentication authentication) {
    PaymentSecurityCacheService.CachedStatus cached =
        paymentSecurityCacheService.getCachedStatus(paymentNo);
    if (cached != null) {
      if (!canReadStatus(authentication, cached.userId())) {
        throw new BizException(
            ResultCode.FORBIDDEN, "forbidden to query other user's payment status");
      }
      return Result.success(Map.of("paymentNo", paymentNo, "status", cached.status()));
    }

    PaymentOrderVO order = paymentOrderService.getPaymentOrderByNo(paymentNo);
    if (order == null) {
      throw new BizException(ResultCode.NOT_FOUND, "payment order not found");
    }
    if (!canReadOrder(authentication, order)) {
      throw new BizException(
          ResultCode.FORBIDDEN, "forbidden to query other user's payment status");
    }
    if (!paymentSecurityCacheService.isFinalStatus(order.getStatus())) {
      paymentSecurityCacheService.cacheStatus(paymentNo, order.getUserId(), order.getStatus());
    } else {
      paymentSecurityCacheService.evictStatus(paymentNo);
    }
    return Result.success(
        Map.of(
            "paymentNo", order.getPaymentNo(),
            "status", order.getStatus()));
  }

  @PostMapping("/payment-refunds")
  @PreAuthorize("isAuthenticated() and (hasAuthority('admin:all') or hasAuthority('order:refund'))")
  @Operation(summary = "创建退款单")
  public Result<Long> createRefund(@Valid @RequestBody PaymentRefundCommandDTO command) {
    return Result.success(paymentOrderService.createRefund(command));
  }

  @GetMapping("/payment-refunds/{refundNo}")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "按退款单号查询退款单")
  public Result<PaymentRefundVO> getRefundByNo(
      @PathVariable String refundNo, Authentication authentication) {
    PaymentRefundVO refund = paymentOrderService.getRefundByNo(refundNo);
    if (refund == null) {
      throw new BizException(ResultCode.NOT_FOUND, "payment refund not found");
    }
    PaymentOrderVO order = paymentOrderService.getPaymentOrderByNo(refund.getPaymentNo());
    if (order == null) {
      throw new BizException(ResultCode.NOT_FOUND, "payment order not found for refund");
    }
    if (!canReadOrder(authentication, order)) {
      throw new BizException(ResultCode.FORBIDDEN, "forbidden to query other user's refund");
    }
    return Result.success(refund);
  }

  private boolean canReadOrder(Authentication authentication, PaymentOrderVO order) {
    if (SecurityPermissionUtils.isAdmin(authentication)) {
      return true;
    }
    String currentUserId = SecurityPermissionUtils.getCurrentUserId(authentication);
    return currentUserId != null && currentUserId.equals(String.valueOf(order.getUserId()));
  }

  private boolean canReadStatus(Authentication authentication, Long userId) {
    if (SecurityPermissionUtils.isAdmin(authentication)) {
      return true;
    }
    String currentUserId = SecurityPermissionUtils.getCurrentUserId(authentication);
    return currentUserId != null && userId != null && currentUserId.equals(String.valueOf(userId));
  }

  private Long requireCurrentUserId(Authentication authentication) {
    String userId = SecurityPermissionUtils.getCurrentUserId(authentication);
    if (userId == null || userId.isBlank()) {
      throw new BizException("current user not found in token");
    }
    if (!userId.chars().allMatch(Character::isDigit)) {
      throw new BizException("invalid user_id in token");
    }
    return Long.parseLong(userId);
  }
}
