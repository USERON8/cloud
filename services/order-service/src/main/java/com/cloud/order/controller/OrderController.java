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
import com.cloud.order.service.support.OrderOperatorSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
@Tag(name = "订单接口", description = "订单创建、查询和售后接口")
@ApiResponses({
  @ApiResponse(responseCode = "400", description = "请求参数或业务状态无效"),
  @ApiResponse(responseCode = "401", description = "需要认证"),
  @ApiResponse(responseCode = "403", description = "权限不足"),
  @ApiResponse(responseCode = "404", description = "订单或售后资源不存在"),
  @ApiResponse(responseCode = "409", description = "并发或状态冲突"),
  @ApiResponse(responseCode = "500", description = "服务内部错误")
})
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
  private final OrderOperatorSupport orderOperatorSupport;

  @PostMapping("/orders")
  @PreAuthorize("hasAuthority('order:create')")
  @Operation(summary = "创建主订单")
  public Result<OrderAggregateResponse> createMainOrder(
      @RequestBody @Valid CreateMainOrderRequest request,
      @Parameter(
              description = "用于防重复提交的请求幂等键",
              required = true)
          @RequestHeader("Idempotency-Key")
          @NotBlank(message = "Idempotency-Key header is required")
          String idempotencyKey,
      Authentication authentication) {
    Long currentUserId = orderOperatorSupport.requireCurrentUserId(authentication);
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

  @GetMapping("/orders")
  @PreAuthorize("hasAuthority('order:query')")
  @Operation(summary = "分页查询订单")
  public Result<PageResult<OrderSummaryDTO>> listOrders(
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size,
      @RequestParam(required = false) Long userId,
      @RequestParam(required = false) Long merchantId,
      @RequestParam(required = false) Integer status,
      Authentication authentication) {
    return Result.success(
        orderQueryService.listOrders(authentication, page, size, userId, merchantId, status));
  }

  @GetMapping("/orders/{orderId}")
  @PreAuthorize("hasAuthority('order:query')")
  @Operation(summary = "查询订单详情")
  public Result<OrderSummaryDTO> getOrder(
      @PathVariable Long orderId, Authentication authentication) {
    return Result.success(orderQueryService.getOrderSummary(orderId, authentication));
  }

  @PostMapping("/orders/{orderId}/cancellation")
  @PreAuthorize("hasAuthority('order:cancel')")
  @Operation(summary = "取消订单")
  public Result<Boolean> cancelOrder(
      @PathVariable Long orderId,
      @RequestParam(required = false) String cancelReason,
      Authentication authentication) {
    orderBatchService.applyOrderAction(
        orderId, authentication, OrderAction.CANCEL, null, null, cancelReason);
    return Result.success(true);
  }

  @PostMapping("/orders/{orderId}/shipments")
  @PreAuthorize("hasAnyRole('ADMIN','MERCHANT')")
  @Operation(summary = "订单发货")
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

  @PostMapping("/orders/{orderId}/completion")
  @PreAuthorize("hasAuthority('order:query')")
  @Operation(summary = "确认订单完成")
  public Result<Boolean> completeOrder(@PathVariable Long orderId, Authentication authentication) {
    orderBatchService.applyOrderAction(orderId, authentication, OrderAction.DONE, null, null, null);
    return Result.success(true);
  }

  @PostMapping("/orders/bulk/cancellations")
  @PreAuthorize("hasAuthority('order:cancel')")
  @Operation(summary = "批量取消订单")
  public Result<Integer> batchCancel(
      @RequestBody List<Long> orderIds,
      @RequestParam(required = false) String cancelReason,
      Authentication authentication) {
    return Result.success(
        orderBatchService.batchApply(
            orderIds, authentication, OrderAction.CANCEL, null, null, cancelReason));
  }

  @PostMapping("/orders/bulk/shipments")
  @PreAuthorize("hasAnyRole('ADMIN','MERCHANT')")
  @Operation(summary = "批量订单发货")
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

  @PostMapping("/orders/bulk/completions")
  @PreAuthorize("hasAuthority('order:query')")
  @Operation(summary = "批量确认订单完成")
  public Result<Integer> batchComplete(
      @RequestBody List<Long> orderIds, Authentication authentication) {
    return Result.success(
        orderBatchService.batchApply(orderIds, authentication, OrderAction.DONE, null, null, null));
  }

  @PostMapping("/after-sales")
  @PreAuthorize("hasAuthority('order:refund')")
  @Operation(summary = "申请售后")
  public Result<AfterSaleDTO> applyAfterSale(
      @Valid @RequestBody AfterSaleDTO afterSaleDTO, Authentication authentication) {
    if (afterSaleDTO == null) {
      throw new BizException(ResultCode.BAD_REQUEST, "after sale payload is required");
    }
    AfterSale afterSale = afterSaleDtoConverter.toEntity(afterSaleDTO);
    Long currentUserId = orderOperatorSupport.requireCurrentUserId(authentication);
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

  @PostMapping("/after-sales/{afterSaleId}/events")
  @PreAuthorize("hasAuthority('order:refund')")
  @Operation(summary = "推进售后状态")
  public Result<AfterSaleDTO> advanceAfterSaleStatus(
      @PathVariable Long afterSaleId,
      @RequestParam String action,
      @RequestParam(required = false) String remark,
      Authentication authentication) {
    AfterSale afterSale = orderService.getAfterSale(afterSaleId);
    if (afterSale == null || Integer.valueOf(1).equals(afterSale.getDeleted())) {
      throw new BizException(ResultCode.NOT_FOUND, "after sale not found");
    }
    if (!isAdmin(authentication)) {
      Long currentUserId = orderOperatorSupport.requireCurrentUserId(authentication);
      if (isMerchant(authentication)) {
        Long currentMerchantId = orderOperatorSupport.requireCurrentMerchantId(authentication);
        if (!Objects.equals(currentMerchantId, afterSale.getMerchantId())) {
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
}
