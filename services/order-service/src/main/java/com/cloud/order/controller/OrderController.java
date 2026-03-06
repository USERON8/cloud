package com.cloud.order.controller;

import com.cloud.common.exception.BusinessException;
import com.cloud.common.result.Result;
import com.cloud.common.security.SecurityPermissionUtils;
import com.cloud.order.dto.CreateMainOrderRequest;
import com.cloud.order.dto.OrderAggregateResponse;
import com.cloud.order.entity.AfterSale;
import com.cloud.order.entity.OrderMain;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import cn.hutool.core.util.StrUtil;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    public Result<OrderAggregateResponse> createMainOrder(@RequestBody @Valid CreateMainOrderRequest request,
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
        }
        request.setIdempotencyKey(idempotencyKey.trim());
        OrderMain mainOrder = orderService.createMainOrder(request);
        return Result.success(orderService.getOrderAggregate(mainOrder.getId()));
    }

    @GetMapping("/main/{mainOrderId}")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasMerchantAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    public Result<OrderAggregateResponse> getMainOrder(@PathVariable Long mainOrderId, Authentication authentication) {
        OrderMain mainOrder = orderService.getMainOrder(mainOrderId);
        if (mainOrder == null || Integer.valueOf(1).equals(mainOrder.getDeleted())) {
            return Result.notFound("main order not found");
        }
        if (!isAdmin(authentication) && !isMerchant(authentication)) {
            Long currentUserId = requireCurrentUserId(authentication);
            if (!Objects.equals(mainOrder.getUserId(), currentUserId)) {
                return Result.forbidden("forbidden to query other user's orders");
            }
        }
        return Result.success(orderService.getOrderAggregate(mainOrderId));
    }

    @GetMapping("/main/{mainOrderId}/sub-orders")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    public Result<List<OrderSub>> listSubOrders(@PathVariable Long mainOrderId,
                                                   Authentication authentication) {
        OrderMain mainOrder = orderService.getMainOrder(mainOrderId);
        if (mainOrder == null || Integer.valueOf(1).equals(mainOrder.getDeleted())) {
            return Result.notFound("main order not found");
        }
        if (!isAdmin(authentication)) {
            Long currentUserId = requireCurrentUserId(authentication);
            if (!Objects.equals(currentUserId, mainOrder.getUserId())) {
                return Result.forbidden("forbidden to query other user's orders");
            }
        }
        return Result.success(orderService.listSubOrders(mainOrderId));
    }

    @PostMapping("/sub/{subOrderId}/actions/{action}")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasMerchantAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    public Result<OrderSub> advanceSubOrderStatus(@PathVariable Long subOrderId,
                                                     @PathVariable String action,
                                                     Authentication authentication) {
        OrderSub subOrder = requireAccessibleSubOrder(subOrderId, authentication);
        if (subOrder == null) {
            return Result.notFound("sub order not found");
        }
        return Result.success(orderService.advanceSubOrderStatus(subOrderId, action));
    }

    @PostMapping("/after-sales")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    public Result<AfterSale> applyAfterSale(@RequestBody AfterSale afterSale,
                                               Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        if (!isAdmin(authentication)) {
            if (afterSale.getUserId() == null) {
                afterSale.setUserId(currentUserId);
            } else if (!Objects.equals(afterSale.getUserId(), currentUserId)) {
                return Result.forbidden("forbidden to create after-sale for another user");
            }
        }
        return Result.success(orderService.applyAfterSale(afterSale));
    }

    @PostMapping("/after-sales/{afterSaleId}/actions/{action}")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasMerchantAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    public Result<AfterSale> advanceAfterSaleStatus(@PathVariable Long afterSaleId,
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
        return Result.success(orderService.advanceAfterSaleStatus(afterSaleId, action, remark));
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

    private OrderSub requireAccessibleSubOrder(Long subOrderId, Authentication authentication) {
        OrderSub subOrder = orderService.getSubOrder(subOrderId);
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
        OrderMain mainOrder = orderService.getMainOrder(subOrder.getMainOrderId());
        if (mainOrder == null || Integer.valueOf(1).equals(mainOrder.getDeleted())
                || !Objects.equals(mainOrder.getUserId(), currentUserId)) {
            throw new BusinessException("forbidden");
        }
        return subOrder;
    }
}


