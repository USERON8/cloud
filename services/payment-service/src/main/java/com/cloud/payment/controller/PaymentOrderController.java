package com.cloud.payment.controller;

import com.cloud.common.annotation.RawResponse;
import com.cloud.common.domain.dto.payment.PaymentCallbackCommandDTO;
import com.cloud.common.domain.dto.payment.PaymentOrderCommandDTO;
import com.cloud.common.domain.dto.payment.PaymentRefundCommandDTO;
import com.cloud.common.domain.vo.payment.PaymentCheckoutSessionVO;
import com.cloud.common.domain.vo.payment.PaymentOrderVO;
import com.cloud.common.domain.vo.payment.PaymentRefundVO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;
import com.cloud.common.exception.BusinessException;
import com.cloud.common.result.Result;
import com.cloud.common.security.SecurityPermissionUtils;
import com.cloud.payment.service.PaymentOrderService;
import com.cloud.payment.service.support.PaymentSecurityCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
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
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Validated
@Tag(name = "Payment API", description = "Payment order and refund APIs")
public class PaymentOrderController {

  private final PaymentOrderService paymentOrderService;
  private final PaymentSecurityCacheService paymentSecurityCacheService;

  @PostMapping("/orders")
  @PreAuthorize("isAuthenticated() and hasAuthority('order:create')")
  @Operation(summary = "Create payment order")
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

  @GetMapping("/orders/{paymentNo}")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get payment order by number")
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

  @GetMapping("/orders/by-order")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get payment order by order numbers")
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

  @PostMapping("/orders/{paymentNo}/checkout-session")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Create checkout session for payment order")
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

  @GetMapping("/orders/{paymentNo}/status")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get payment order status")
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

  @PostMapping("/callbacks")
  @PreAuthorize("hasAuthority('order:refund')")
  @Operation(summary = "Handle internal payment callback")
  public Result<Boolean> handleCallback(@Valid @RequestBody PaymentCallbackCommandDTO command) {
    return Result.success(paymentOrderService.handleInternalPaymentCallback(command));
  }

  @PostMapping("/refunds")
  @PreAuthorize("hasAuthority('order:refund')")
  @Operation(summary = "Create payment refund")
  public Result<Long> createRefund(@Valid @RequestBody PaymentRefundCommandDTO command) {
    return Result.success(paymentOrderService.createRefund(command));
  }

  @RawResponse
  @GetMapping(value = "/checkout/{ticket}", produces = MediaType.TEXT_HTML_VALUE)
  @Operation(summary = "Render payment checkout page")
  public String renderCheckoutPage(@PathVariable String ticket) {
    try {
      return paymentOrderService.renderCheckoutPage(ticket);
    } catch (BusinessException ex) {
      return buildHtmlPage("Payment unavailable", ex.getMessage());
    } catch (RuntimeException ex) {
      return buildHtmlPage("Payment unavailable", "Failed to initialize the payment page.");
    }
  }

  @GetMapping("/refunds/{refundNo}")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get refund by number")
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
    try {
      return Long.parseLong(userId);
    } catch (NumberFormatException ex) {
      throw new BizException("invalid user_id in token");
    }
  }

  private String buildHtmlPage(String title, String message) {
    return """
        <!DOCTYPE html>
        <html lang="en">
          <head>
            <meta charset="UTF-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0" />
            <title>%s</title>
            <style>
              body {
                margin: 0;
                min-height: 100vh;
                display: grid;
                place-items: center;
                background: #f4f7fb;
                color: #1f2a37;
                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
              }
              main {
                max-width: 420px;
                padding: 24px;
                border-radius: 18px;
                background: #ffffff;
                box-shadow: 0 16px 48px rgba(15, 23, 42, 0.08);
                text-align: center;
              }
              h1 {
                margin: 0 0 12px;
                font-size: 24px;
              }
              p {
                margin: 0;
                line-height: 1.6;
                color: #526072;
              }
            </style>
          </head>
          <body>
            <main>
              <h1>%s</h1>
              <p>%s</p>
            </main>
          </body>
        </html>
        """
        .formatted(escapeHtml(title), escapeHtml(title), escapeHtml(message));
  }

  private String escapeHtml(String value) {
    if (value == null) {
      return "";
    }
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }
}
