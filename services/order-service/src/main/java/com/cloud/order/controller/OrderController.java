package com.cloud.order.controller;

import cn.hutool.core.util.StrUtil;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;
import com.cloud.common.result.PageResult;
import com.cloud.common.result.Result;
import com.cloud.common.security.SecurityPermissionUtils;
import com.cloud.order.converter.AfterSaleDtoConverter;
import com.cloud.order.dto.AfterSaleDTO;
import com.cloud.order.dto.CreateMainOrderRequest;
import com.cloud.order.dto.OrderAggregateResponse;
import com.cloud.order.dto.OrderSummaryDTO;
import com.cloud.order.entity.AfterSale;
import com.cloud.order.enums.AfterSaleAction;
import com.cloud.order.enums.OrderAction;
import com.cloud.order.service.OrderBatchService;
import com.cloud.order.service.OrderPlacementService;
import com.cloud.order.service.OrderQueryService;
import com.cloud.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Validated
@Tag(name = "Order API", description = "Order creation and after-sale APIs")
public class OrderController {

  private static final Set<AfterSaleAction> USER_AFTER_SALE_ACTIONS =
      EnumSet.of(AfterSaleAction.CANCEL, AfterSaleAction.RETURN);
  private static final Set<AfterSaleAction> MERCHANT_AFTER_SALE_ACTIONS =
      EnumSet.of(
          AfterSaleAction.AUDIT,
          AfterSaleAction.APPROVE,
          AfterSaleAction.REJECT,
          AfterSaleAction.WAIT_RETURN,
          AfterSaleAction.RECEIVE,
          AfterSaleAction.PROCESS);

  private final OrderService orderService;
  private final OrderPlacementService orderPlacementService;
  private final OrderBatchService orderBatchService;
  private final OrderQueryService orderQueryService;
  private final AfterSaleDtoConverter afterSaleDtoConverter;

  @PostMapping
  @PreAuthorize("hasAuthority('order:create')")
  @Operation(summary = "Create main order")
  public Result<OrderAggregateResponse> createMainOrder(
      @RequestBody @Valid CreateMainOrderRequest request,
      @Parameter(
              description = "Request idempotency key for duplicate submission protection",
              required = true)
          @RequestHeader("Idempotency-Key")
          @NotBlank(message = "Idempotency-Key header is required")
          String idempotencyKey,
      Authentication authentication) {
    Long currentUserId = requireCurrentUserId(authentication);
    if (!isAdmin(authentication)) {
      if (request.getUserId() == null) {
        request.setUserId(currentUserId);
      } else if (!Objects.equals(request.getUserId(), currentUserId)) {
        throw new BizException(ResultCode.FORBIDDEN, "forbidden to create order for another user");
      }
    } else if (request.getUserId() == null) {
      throw new BizException(ResultCode.BAD_REQUEST, "userId is required for admin order creation");
    }
    request.setIdempotencyKey(idempotencyKey.trim());
    return Result.success(orderPlacementService.createOrder(request));
  }

  @GetMapping
  @PreAuthorize("hasAuthority('order:query')")
  @Operation(summary = "List orders")
  public Result<PageResult<OrderSummaryDTO>> listOrders(
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size,
      @RequestParam(required = false) Long userId,
      @RequestParam(required = false) Long merchantId,
      @RequestParam(name = "shopId", required = false) Long legacyShopId,
      @RequestParam(required = false) Integer status,
      Authentication authentication) {
    Long effectiveMerchantId = resolveMerchantId(merchantId, legacyShopId);
    return Result.success(
        orderQueryService.listOrders(
            authentication, page, size, userId, effectiveMerchantId, status));
  }

  @GetMapping("/{orderId}")
  @PreAuthorize("hasAuthority('order:query')")
  @Operation(summary = "Get order detail")
  public Result<OrderSummaryDTO> getOrder(
      @PathVariable Long orderId, Authentication authentication) {
    return Result.success(orderQueryService.getOrderSummary(orderId, authentication));
  }

  @PostMapping("/{orderId}/pay")
  @PreAuthorize("hasAuthority('order:create')")
  @Operation(summary = "Pay order")
  public Result<Boolean> payOrder(@PathVariable Long orderId, Authentication authentication) {
    orderBatchService.applyOrderAction(orderId, authentication, OrderAction.PAY, null, null, null);
    return Result.success(true);
  }

  @PostMapping("/{orderId}/cancel")
  @PreAuthorize("hasAuthority('order:cancel')")
  @Operation(summary = "Cancel order")
  public Result<Boolean> cancelOrder(
      @PathVariable Long orderId,
      @RequestParam(required = false) String cancelReason,
      Authentication authentication) {
    orderBatchService.applyOrderAction(
        orderId, authentication, OrderAction.CANCEL, null, null, cancelReason);
    return Result.success(true);
  }

  @PostMapping("/{orderId}/ship")
  @PreAuthorize("hasAnyRole('ADMIN','MERCHANT')")
  @Operation(summary = "Ship order")
  public Result<Boolean> shipOrderStandard(
      @PathVariable Long orderId,
      @RequestParam(required = false) String shippingCompany,
      @RequestParam(required = false) String trackingNumber,
      Authentication authentication) {
    requireMerchantOrAdmin(authentication);
    String company = requireShippingValue(shippingCompany, "shipping company");
    String tracking = requireShippingValue(trackingNumber, "tracking number");
    orderBatchService.applyOrderAction(
        orderId, authentication, OrderAction.SHIP, company, tracking, null);
    return Result.success(true);
  }

  @PostMapping("/{orderId}/complete")
  @PreAuthorize("hasAuthority('order:query')")
  @Operation(summary = "Complete order")
  public Result<Boolean> completeOrder(@PathVariable Long orderId, Authentication authentication) {
    orderBatchService.applyOrderAction(orderId, authentication, OrderAction.DONE, null, null, null);
    return Result.success(true);
  }

  @PostMapping("/batch/pay")
  @PreAuthorize("hasAuthority('order:create')")
  @Operation(summary = "Batch pay orders")
  public Result<Integer> batchPay(@RequestBody List<Long> orderIds, Authentication authentication) {
    return Result.success(
        orderBatchService.batchApply(orderIds, authentication, OrderAction.PAY, null, null, null));
  }

  @PostMapping("/batch/cancel")
  @PreAuthorize("hasAuthority('order:cancel')")
  @Operation(summary = "Batch cancel orders")
  public Result<Integer> batchCancel(
      @RequestBody List<Long> orderIds,
      @RequestParam(required = false) String cancelReason,
      Authentication authentication) {
    return Result.success(
        orderBatchService.batchApply(
            orderIds, authentication, OrderAction.CANCEL, null, null, cancelReason));
  }

  @PostMapping("/batch/ship")
  @PreAuthorize("hasAnyRole('ADMIN','MERCHANT')")
  @Operation(summary = "Batch ship orders")
  public Result<Integer> batchShip(
      @RequestBody List<Long> orderIds,
      @RequestParam(required = false) String shippingCompany,
      @RequestParam(required = false) String trackingNumber,
      Authentication authentication) {
    requireMerchantOrAdmin(authentication);
    String company = requireShippingValue(shippingCompany, "shipping company");
    String tracking = requireShippingValue(trackingNumber, "tracking number");
    return Result.success(
        orderBatchService.batchApply(
            orderIds, authentication, OrderAction.SHIP, company, tracking, null));
  }

  @PostMapping("/batch/complete")
  @PreAuthorize("hasAuthority('order:query')")
  @Operation(summary = "Batch complete orders")
  public Result<Integer> batchComplete(
      @RequestBody List<Long> orderIds, Authentication authentication) {
    return Result.success(
        orderBatchService.batchApply(orderIds, authentication, OrderAction.DONE, null, null, null));
  }

  @PostMapping("/after-sales")
  @PreAuthorize("hasAuthority('order:refund')")
  @Operation(summary = "Apply after-sale")
  public Result<AfterSaleDTO> applyAfterSale(
      @RequestBody AfterSaleDTO afterSaleDTO, Authentication authentication) {
    if (afterSaleDTO == null) {
      throw new BizException(ResultCode.BAD_REQUEST, "after sale payload is required");
    }
    AfterSale afterSale = afterSaleDtoConverter.toEntity(afterSaleDTO);
    Long currentUserId = requireCurrentUserId(authentication);
    if (!isAdmin(authentication)) {
      if (afterSale.getUserId() == null) {
        afterSale.setUserId(currentUserId);
      } else if (!Objects.equals(afterSale.getUserId(), currentUserId)) {
        throw new BizException(
            ResultCode.FORBIDDEN, "forbidden to create after-sale for another user");
      }
    }
    AfterSale created = orderService.applyAfterSale(afterSale);
    return Result.success(afterSaleDtoConverter.toDto(created));
  }

  @PostMapping("/after-sales/{afterSaleId}/actions/{action}")
  @PreAuthorize("hasAuthority('order:refund')")
  @Operation(summary = "Advance after-sale status")
  public Result<AfterSaleDTO> advanceAfterSaleStatus(
      @PathVariable Long afterSaleId,
      @PathVariable String action,
      @RequestParam(required = false) String remark,
      Authentication authentication) {
    AfterSale afterSale = orderService.getAfterSale(afterSaleId);
    if (afterSale == null || Integer.valueOf(1).equals(afterSale.getDeleted())) {
      throw new BizException(ResultCode.NOT_FOUND, "after sale not found");
    }
    if (!isAdmin(authentication)) {
      Long currentUserId = requireCurrentUserId(authentication);
      if (isMerchant(authentication)) {
        if (!Objects.equals(currentUserId, afterSale.getMerchantId())) {
          throw new BizException(
              ResultCode.FORBIDDEN, "forbidden to operate another merchant's after-sale");
        }
      } else if (!Objects.equals(currentUserId, afterSale.getUserId())) {
        throw new BizException(
            ResultCode.FORBIDDEN, "forbidden to operate another user's after-sale");
      }
    }
    AfterSaleAction afterSaleAction = AfterSaleAction.fromValue(action);
    if (!isAllowedAfterSaleAction(afterSaleAction, authentication)) {
      throw new BizException(
          ResultCode.FORBIDDEN, "forbidden to perform action: " + afterSaleAction.code());
    }
    AfterSale updated = orderService.advanceAfterSaleStatus(afterSaleId, afterSaleAction, remark);
    return Result.success(afterSaleDtoConverter.toDto(updated));
  }

  private boolean isAdmin(Authentication authentication) {
    return SecurityPermissionUtils.isAdmin(authentication);
  }

  private boolean isMerchant(Authentication authentication) {
    return SecurityPermissionUtils.isMerchant(authentication);
  }

  private boolean isAllowedAfterSaleAction(AfterSaleAction action, Authentication authentication) {
    if (isAdmin(authentication)) {
      return true;
    }
    if (isMerchant(authentication)) {
      return MERCHANT_AFTER_SALE_ACTIONS.contains(action);
    }
    return USER_AFTER_SALE_ACTIONS.contains(action);
  }

  private void requireMerchantOrAdmin(Authentication authentication) {
    if (isAdmin(authentication) || isMerchant(authentication)) {
      return;
    }
    throw new BizException(ResultCode.FORBIDDEN, "shipping requires merchant or admin privileges");
  }

  private String requireShippingValue(String value, String fieldName) {
    if (StrUtil.isBlank(value)) {
      throw new BizException(ResultCode.BAD_REQUEST, fieldName + " is required");
    }
    return value.trim();
  }

  private Long resolveMerchantId(Long merchantId, Long legacyShopId) {
    if (merchantId != null && legacyShopId != null && !Objects.equals(merchantId, legacyShopId)) {
      throw new BizException(
          ResultCode.BAD_REQUEST, "merchantId and shopId must match when both are provided");
    }
    return merchantId != null ? merchantId : legacyShopId;
  }

  private Long requireCurrentUserId(Authentication authentication) {
    String userId = SecurityPermissionUtils.getCurrentUserId(authentication);
    if (userId == null || userId.isBlank()) {
      throw new BizException("current user not found in token");
    }
    if (!StrUtil.isNumeric(userId)) {
      throw new BizException("invalid user_id in token");
    }
    return Long.parseLong(userId);
  }
}
