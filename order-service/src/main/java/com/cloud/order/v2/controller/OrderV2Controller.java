package com.cloud.order.v2.controller;

import com.cloud.common.exception.BusinessException;
import com.cloud.common.result.Result;
import com.cloud.common.security.SecurityPermissionUtils;
import com.cloud.order.v2.dto.CreateMainOrderRequest;
import com.cloud.order.v2.dto.OrderAggregateResponse;
import com.cloud.order.v2.entity.AfterSaleV2;
import com.cloud.order.v2.entity.OrderMainV2;
import com.cloud.order.v2.entity.OrderSubV2;
import com.cloud.order.v2.service.OrderV2Service;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v2/orders")
@RequiredArgsConstructor
public class OrderV2Controller {

    private final OrderV2Service orderV2Service;

    @PostMapping
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    public Result<OrderAggregateResponse> createMainOrder(@RequestBody @Valid CreateMainOrderRequest request,
                                                           @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                                           Authentication authentication) {
        if (!StringUtils.hasText(idempotencyKey)) {
            throw new BusinessException("Idempotency-Key header is required");
        }
        Long currentUserId = requireCurrentUserId(authentication);
        if (!isAdmin(authentication)) {
            if (request.getUserId() == null) {
                request.setUserId(currentUserId);
            } else if (!Objects.equals(request.getUserId(), currentUserId)) {
                return Result.forbidden("forbidden to create order for another user");
            }
        }
        request.setIdempotencyKey(idempotencyKey.trim());
        OrderMainV2 mainOrder = orderV2Service.createMainOrder(request);
        return Result.success(orderV2Service.getOrderAggregate(mainOrder.getId()));
    }

    @GetMapping("/main/{mainOrderId}")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasMerchantAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    public Result<OrderAggregateResponse> getMainOrder(@PathVariable Long mainOrderId, Authentication authentication) {
        OrderMainV2 mainOrder = orderV2Service.getMainOrder(mainOrderId);
        if (mainOrder == null || Integer.valueOf(1).equals(mainOrder.getDeleted())) {
            return Result.notFound("main order not found");
        }
        if (!isAdmin(authentication) && !isMerchant(authentication)) {
            Long currentUserId = requireCurrentUserId(authentication);
            if (!Objects.equals(mainOrder.getUserId(), currentUserId)) {
                return Result.forbidden("forbidden to query other user's orders");
            }
        }
        return Result.success(orderV2Service.getOrderAggregate(mainOrderId));
    }

    @GetMapping("/main/{mainOrderId}/sub-orders")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    public Result<List<OrderSubV2>> listSubOrders(@PathVariable Long mainOrderId,
                                                   Authentication authentication) {
        OrderMainV2 mainOrder = orderV2Service.getMainOrder(mainOrderId);
        if (mainOrder == null || Integer.valueOf(1).equals(mainOrder.getDeleted())) {
            return Result.notFound("main order not found");
        }
        if (!isAdmin(authentication)) {
            Long currentUserId = requireCurrentUserId(authentication);
            if (!Objects.equals(currentUserId, mainOrder.getUserId())) {
                return Result.forbidden("forbidden to query other user's orders");
            }
        }
        return Result.success(orderV2Service.listSubOrders(mainOrderId));
    }

    @PostMapping("/sub/{subOrderId}/actions/{action}")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasMerchantAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    public Result<OrderSubV2> advanceSubOrderStatus(@PathVariable Long subOrderId,
                                                     @PathVariable String action,
                                                     Authentication authentication) {
        OrderSubV2 subOrder = requireAccessibleSubOrder(subOrderId, authentication);
        if (subOrder == null) {
            return Result.notFound("sub order not found");
        }
        return Result.success(orderV2Service.advanceSubOrderStatus(subOrderId, action));
    }

    @PostMapping("/after-sales")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    public Result<AfterSaleV2> applyAfterSale(@RequestBody AfterSaleV2 afterSale,
                                               Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        if (!isAdmin(authentication)) {
            if (afterSale.getUserId() == null) {
                afterSale.setUserId(currentUserId);
            } else if (!Objects.equals(afterSale.getUserId(), currentUserId)) {
                return Result.forbidden("forbidden to create after-sale for another user");
            }
        }
        return Result.success(orderV2Service.applyAfterSale(afterSale));
    }

    @PostMapping("/after-sales/{afterSaleId}/actions/{action}")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasMerchantAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    public Result<AfterSaleV2> advanceAfterSaleStatus(@PathVariable Long afterSaleId,
                                                       @PathVariable String action,
                                                       @RequestParam(required = false) String remark,
                                                       Authentication authentication) {
        AfterSaleV2 afterSale = orderV2Service.getAfterSale(afterSaleId);
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
        return Result.success(orderV2Service.advanceAfterSaleStatus(afterSaleId, action, remark));
    }

    private boolean isAdmin(Authentication authentication) {
        return SecurityPermissionUtils.isAdmin(authentication);
    }

    private boolean isMerchant(Authentication authentication) {
        return SecurityPermissionUtils.isMerchant(authentication);
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

    private OrderSubV2 requireAccessibleSubOrder(Long subOrderId, Authentication authentication) {
        OrderSubV2 subOrder = orderV2Service.getSubOrder(subOrderId);
        if (subOrder == null || Integer.valueOf(1).equals(subOrder.getDeleted())) {
            throw new BusinessException("sub order not found");
        }
        if (isAdmin(authentication)) {
            return subOrder;
        }
        Long currentUserId = requireCurrentUserId(authentication);
        if (isMerchant(authentication)) {
            if (!Objects.equals(currentUserId, subOrder.getMerchantId())) {
                throw new BusinessException("forbidden");
            }
            return subOrder;
        }
        OrderMainV2 mainOrder = orderV2Service.getMainOrder(subOrder.getMainOrderId());
        if (mainOrder == null || Integer.valueOf(1).equals(mainOrder.getDeleted())
                || !Objects.equals(mainOrder.getUserId(), currentUserId)) {
            throw new BusinessException("forbidden");
        }
        return subOrder;
    }
}
