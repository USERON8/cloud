package com.cloud.order.controller;

import cn.hutool.core.util.StrUtil;
import com.cloud.common.exception.BusinessException;
import com.cloud.common.result.PageResult;
import com.cloud.common.result.Result;
import com.cloud.common.security.SecurityPermissionUtils;
import com.cloud.order.dto.AfterSaleDTO;
import com.cloud.order.dto.CreateMainOrderRequest;
import com.cloud.order.dto.OrderAggregateResponse;
import com.cloud.order.dto.OrderSummaryDTO;
import com.cloud.order.entity.AfterSale;
import com.cloud.order.entity.OrderMain;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.service.OrderPlacementService;
import com.cloud.order.service.OrderQueryService;
import com.cloud.order.service.OrderService;
import com.cloud.order.service.OrderShippingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
@Tag(name = "Order API", description = "Order creation and after-sale APIs")
public class OrderController {

  private static final Set<String> AFTER_SALE_ACTIONS =
      Set.of(
          "AUDIT",
          "APPROVE",
          "REJECT",
          "WAIT_RETURN",
          "RETURN",
          "RECEIVE",
          "PROCESS",
          "REFUND",
          "CANCEL",
          "CLOSE");
  private static final Set<String> USER_AFTER_SALE_ACTIONS = Set.of("CANCEL");
  private static final Set<String> MERCHANT_AFTER_SALE_ACTIONS =
      Set.of("AUDIT", "APPROVE", "REJECT", "WAIT_RETURN", "RETURN", "RECEIVE");

  private final OrderService orderService;
  private final OrderPlacementService orderPlacementService;
  private final OrderShippingService orderShippingService;
  private final OrderQueryService orderQueryService;

  @PostMapping
  @PreAuthorize("hasAuthority('order:create')")
  @Operation(summary = "Create main order")
  public Result<OrderAggregateResponse> createMainOrder(
      @RequestBody @Valid CreateMainOrderRequest request,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      Authentication authentication) {
    if (StrUtil.isBlank(idempotencyKey)) {
      throw new BusinessException("Idempotency-Key header is required");
    }
    Long currentUserId = requireCurrentUserId(authentication);
    if (!isAdmin(authentication)) {
      if (request.getUserId() == null) {
        request.setUserId(currentUserId);
      } else if (!Objects.equals(request.getUserId(), currentUserId)) {
        return Result.forbidden("forbidden to create order for another user");
      }
    } else if (request.getUserId() == null) {
      return Result.badRequest("userId is required for admin order creation");
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
      @RequestParam(required = false) Long shopId,
      @RequestParam(required = false) Integer status,
      Authentication authentication) {
    return Result.success(
        orderQueryService.listOrders(authentication, page, size, userId, shopId, status));
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
    OrderMain main = orderQueryService.requireAccessibleMainOrder(orderId, authentication);
    List<OrderSub> subs = orderService.listSubOrders(main.getId());
    applySubOrderAction(subs, "PAY", authentication, null, null);
    return Result.success(true);
  }

  @PostMapping("/{orderId}/cancel")
  @PreAuthorize("hasAuthority('order:cancel')")
  @Operation(summary = "Cancel order")
  public Result<Boolean> cancelOrder(
      @PathVariable Long orderId,
      @RequestParam(required = false) String cancelReason,
      Authentication authentication) {
    OrderMain main = orderQueryService.requireAccessibleMainOrder(orderId, authentication);
    List<OrderSub> subs = orderService.listSubOrders(main.getId());
    applySubOrderAction(subs, "CANCEL", authentication, null, null);
    orderQueryService.updateCancelReason(main.getId(), cancelReason);
    return Result.success(true);
  }

  @PostMapping("/{orderId}/ship")
  @PreAuthorize("hasAuthority('order:query')")
  @Operation(summary = "Ship order")
  public Result<Boolean> shipOrderStandard(
      @PathVariable Long orderId,
      @RequestParam(required = false) String shippingCompany,
      @RequestParam(required = false) String trackingNumber,
      Authentication authentication) {
    OrderMain main = orderQueryService.requireAccessibleMainOrder(orderId, authentication);
    List<OrderSub> subs = orderService.listSubOrders(main.getId());
    String company = StrUtil.isBlank(shippingCompany) ? "TEST" : shippingCompany.trim();
    String tracking =
        StrUtil.isBlank(trackingNumber) ? "TEST-" + main.getMainOrderNo() : trackingNumber.trim();
    applySubOrderAction(subs, "SHIP", authentication, company, tracking);
    return Result.success(true);
  }

  @PostMapping("/{orderId}/complete")
  @PreAuthorize("hasAuthority('order:query')")
  @Operation(summary = "Complete order")
  public Result<Boolean> completeOrder(@PathVariable Long orderId, Authentication authentication) {
    OrderMain main = orderQueryService.requireAccessibleMainOrder(orderId, authentication);
    List<OrderSub> subs = orderService.listSubOrders(main.getId());
    applySubOrderAction(subs, "DONE", authentication, null, null);
    return Result.success(true);
  }

  @PostMapping("/batch/pay")
  @PreAuthorize("hasAuthority('order:create')")
  @Operation(summary = "Batch pay orders")
  public Result<Integer> batchPay(@RequestBody List<Long> orderIds, Authentication authentication) {
    return Result.success(batchApply(orderIds, authentication, "PAY", null, null, null));
  }

  @PostMapping("/batch/cancel")
  @PreAuthorize("hasAuthority('order:cancel')")
  @Operation(summary = "Batch cancel orders")
  public Result<Integer> batchCancel(
      @RequestBody List<Long> orderIds,
      @RequestParam(required = false) String cancelReason,
      Authentication authentication) {
    return Result.success(batchApply(orderIds, authentication, "CANCEL", null, null, cancelReason));
  }

  @PostMapping("/batch/ship")
  @PreAuthorize("hasAuthority('order:query')")
  @Operation(summary = "Batch ship orders")
  public Result<Integer> batchShip(
      @RequestBody List<Long> orderIds,
      @RequestParam(required = false) String shippingCompany,
      @RequestParam(required = false) String trackingNumber,
      Authentication authentication) {
    String company = StrUtil.isBlank(shippingCompany) ? "TEST" : shippingCompany.trim();
    String tracking = StrUtil.isBlank(trackingNumber) ? "TEST" : trackingNumber.trim();
    return Result.success(batchApply(orderIds, authentication, "SHIP", company, tracking, null));
  }

  @PostMapping("/batch/complete")
  @PreAuthorize("hasAuthority('order:query')")
  @Operation(summary = "Batch complete orders")
  public Result<Integer> batchComplete(
      @RequestBody List<Long> orderIds, Authentication authentication) {
    return Result.success(batchApply(orderIds, authentication, "DONE", null, null, null));
  }

  @PostMapping("/after-sales")
  @PreAuthorize("hasAuthority('order:refund')")
  @Operation(summary = "Apply after-sale")
  public Result<AfterSaleDTO> applyAfterSale(
      @RequestBody AfterSaleDTO afterSaleDTO, Authentication authentication) {
    if (afterSaleDTO == null) {
      return Result.badRequest("after sale payload is required");
    }
    AfterSale afterSale = toAfterSaleEntity(afterSaleDTO);
    Long currentUserId = requireCurrentUserId(authentication);
    if (!isAdmin(authentication)) {
      if (afterSale.getUserId() == null) {
        afterSale.setUserId(currentUserId);
      } else if (!Objects.equals(afterSale.getUserId(), currentUserId)) {
        return Result.forbidden("forbidden to create after-sale for another user");
      }
    }
    AfterSale created = orderService.applyAfterSale(afterSale);
    return Result.success(toAfterSaleDTO(created));
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
      return Result.notFound("after sale not found");
    }
    if (!isAdmin(authentication)) {
      Long currentUserId = requireCurrentUserId(authentication);
      if (isMerchant(authentication)) {
        if (!Objects.equals(currentUserId, afterSale.getMerchantId())) {
          return Result.forbidden("forbidden to operate another merchant's after-sale");
        }
      } else if (!Objects.equals(currentUserId, afterSale.getUserId())) {
        return Result.forbidden("forbidden to operate another user's after-sale");
      }
    }
    String normalizedAction = normalizeAction(action, AFTER_SALE_ACTIONS, "after-sale");
    if (!isAllowedAfterSaleAction(normalizedAction, authentication)) {
      return Result.forbidden("forbidden to perform action: " + normalizedAction);
    }
    AfterSale updated = orderService.advanceAfterSaleStatus(afterSaleId, normalizedAction, remark);
    return Result.success(toAfterSaleDTO(updated));
  }

  private boolean isAdmin(Authentication authentication) {
    return SecurityPermissionUtils.isAdmin(authentication);
  }

  private boolean isMerchant(Authentication authentication) {
    return SecurityPermissionUtils.isMerchant(authentication);
  }

  private void applySubOrderAction(
      List<OrderSub> subs,
      String action,
      Authentication authentication,
      String shippingCompany,
      String trackingNumber) {
    if (subs == null || subs.isEmpty()) {
      throw new BusinessException("sub orders not found");
    }
    for (OrderSub sub : subs) {
      if (sub == null || sub.getId() == null) {
        continue;
      }
      if (isMerchant(authentication)
          && !Objects.equals(sub.getMerchantId(), requireCurrentUserId(authentication))) {
        continue;
      }
      if ("SHIP".equals(action)) {
        orderShippingService.ship(sub.getId(), shippingCompany, trackingNumber);
      } else {
        orderService.advanceSubOrderStatus(sub.getId(), action);
      }
    }
  }

  private int batchApply(
      List<Long> orderIds,
      Authentication authentication,
      String action,
      String shippingCompany,
      String trackingNumber,
      String cancelReason) {
    if (orderIds == null || orderIds.isEmpty()) {
      return 0;
    }
    int success = 0;
    for (Long orderId : orderIds) {
      if (orderId == null) {
        continue;
      }
      OrderMain main = orderQueryService.requireAccessibleMainOrder(orderId, authentication);
      List<OrderSub> subs = orderService.listSubOrders(main.getId());
      applySubOrderAction(subs, action, authentication, shippingCompany, trackingNumber);
      if ("CANCEL".equals(action)) {
        orderQueryService.updateCancelReason(main.getId(), cancelReason);
      }
      success += 1;
    }
    return success;
  }

  private AfterSale toAfterSaleEntity(AfterSaleDTO afterSaleDTO) {
    if (afterSaleDTO == null) {
      return null;
    }
    AfterSale afterSale = new AfterSale();
    afterSale.setId(afterSaleDTO.getId());
    afterSale.setAfterSaleNo(afterSaleDTO.getAfterSaleNo());
    afterSale.setMainOrderId(afterSaleDTO.getMainOrderId());
    afterSale.setSubOrderId(afterSaleDTO.getSubOrderId());
    afterSale.setUserId(afterSaleDTO.getUserId());
    afterSale.setMerchantId(afterSaleDTO.getMerchantId());
    afterSale.setAfterSaleType(afterSaleDTO.getAfterSaleType());
    afterSale.setStatus(afterSaleDTO.getStatus());
    afterSale.setReason(afterSaleDTO.getReason());
    afterSale.setDescription(afterSaleDTO.getDescription());
    afterSale.setApplyAmount(afterSaleDTO.getApplyAmount());
    afterSale.setApprovedAmount(afterSaleDTO.getApprovedAmount());
    afterSale.setReturnLogisticsCompany(afterSaleDTO.getReturnLogisticsCompany());
    afterSale.setReturnLogisticsNo(afterSaleDTO.getReturnLogisticsNo());
    afterSale.setRefundChannel(afterSaleDTO.getRefundChannel());
    afterSale.setRefundedAt(afterSaleDTO.getRefundedAt());
    afterSale.setClosedAt(afterSaleDTO.getClosedAt());
    afterSale.setCloseReason(afterSaleDTO.getCloseReason());
    afterSale.setCreatedAt(afterSaleDTO.getCreatedAt());
    afterSale.setUpdatedAt(afterSaleDTO.getUpdatedAt());
    afterSale.setDeleted(afterSaleDTO.getDeleted());
    afterSale.setVersion(afterSaleDTO.getVersion());
    return afterSale;
  }

  private AfterSaleDTO toAfterSaleDTO(AfterSale afterSale) {
    if (afterSale == null) {
      return null;
    }
    AfterSaleDTO dto = new AfterSaleDTO();
    dto.setId(afterSale.getId());
    dto.setAfterSaleNo(afterSale.getAfterSaleNo());
    dto.setMainOrderId(afterSale.getMainOrderId());
    dto.setSubOrderId(afterSale.getSubOrderId());
    dto.setUserId(afterSale.getUserId());
    dto.setMerchantId(afterSale.getMerchantId());
    dto.setAfterSaleType(afterSale.getAfterSaleType());
    dto.setStatus(afterSale.getStatus());
    dto.setReason(afterSale.getReason());
    dto.setDescription(afterSale.getDescription());
    dto.setApplyAmount(afterSale.getApplyAmount());
    dto.setApprovedAmount(afterSale.getApprovedAmount());
    dto.setReturnLogisticsCompany(afterSale.getReturnLogisticsCompany());
    dto.setReturnLogisticsNo(afterSale.getReturnLogisticsNo());
    dto.setRefundChannel(afterSale.getRefundChannel());
    dto.setRefundedAt(afterSale.getRefundedAt());
    dto.setClosedAt(afterSale.getClosedAt());
    dto.setCloseReason(afterSale.getCloseReason());
    dto.setCreatedAt(afterSale.getCreatedAt());
    dto.setUpdatedAt(afterSale.getUpdatedAt());
    dto.setDeleted(afterSale.getDeleted());
    dto.setVersion(afterSale.getVersion());
    return dto;
  }

  private boolean isAllowedAfterSaleAction(String action, Authentication authentication) {
    if (isAdmin(authentication)) {
      return true;
    }
    if (isMerchant(authentication)) {
      return MERCHANT_AFTER_SALE_ACTIONS.contains(action);
    }
    return USER_AFTER_SALE_ACTIONS.contains(action);
  }

  private String normalizeAction(String action, Set<String> allowedActions, String scope) {
    if (StrUtil.isBlank(action)) {
      throw new BusinessException(scope + " action is required");
    }
    String normalized = action.trim().toUpperCase();
    if (!allowedActions.contains(normalized)) {
      throw new BusinessException("unsupported " + scope + " action: " + normalized);
    }
    return normalized;
  }

  private Long requireCurrentUserId(Authentication authentication) {
    String userId = SecurityPermissionUtils.getCurrentUserId(authentication);
    if (userId == null || userId.isBlank()) {
      throw new BusinessException("current user not found in token");
    }
    try {
      return Long.parseLong(userId);
    } catch (NumberFormatException ex) {
      throw new BusinessException("invalid user_id in token");
    }
  }
}
