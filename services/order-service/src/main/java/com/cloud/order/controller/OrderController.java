package com.cloud.order.controller;

import com.cloud.common.exception.BusinessException;
import com.cloud.common.result.Result;
import com.cloud.common.security.SecurityPermissionUtils;
import com.cloud.order.dto.CreateMainOrderRequest;
import com.cloud.order.dto.OrderAggregateResponse;
import com.cloud.order.entity.AfterSale;
import com.cloud.order.entity.OrderMain;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.service.OrderPlacementService;
import com.cloud.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import cn.hutool.core.util.StrUtil;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private static final Set<String> SUB_ORDER_ACTIONS = Set.of(
            "RESERVE", "PAY", "SHIP", "RECEIVE", "DONE", "CANCEL", "CLOSE"
    );
    private static final Set<String> USER_SUB_ACTIONS = Set.of("CANCEL", "RECEIVE");
    private static final Set<String> MERCHANT_SUB_ACTIONS = Set.of("SHIP");

    private static final Set<String> AFTER_SALE_ACTIONS = Set.of(
            "AUDIT", "APPROVE", "REJECT", "WAIT_RETURN", "RETURN", "RECEIVE",
            "PROCESS", "REFUND", "CANCEL", "CLOSE"
    );
    private static final Set<String> USER_AFTER_SALE_ACTIONS = Set.of("CANCEL", "RETURN");
    private static final Set<String> MERCHANT_AFTER_SALE_ACTIONS = Set.of(
            "AUDIT", "APPROVE", "REJECT", "WAIT_RETURN", "RECEIVE"
    );

    private final OrderService orderService;
    private final OrderPlacementService orderPlacementService;

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
        } else if (request.getUserId() == null) {
            return Result.badRequest("userId is required for admin order creation");
        }
        request.setIdempotencyKey(idempotencyKey.trim());
        return Result.success(orderPlacementService.createOrder(request));
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
        String normalizedAction = normalizeAction(action, SUB_ORDER_ACTIONS, "sub order");
        if (!isAllowedSubOrderAction(normalizedAction, authentication)) {
            return Result.forbidden("forbidden to perform action: " + normalizedAction);
        }
        return Result.success(orderService.advanceSubOrderStatus(subOrderId, normalizedAction));
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
        String normalizedAction = normalizeAction(action, AFTER_SALE_ACTIONS, "after-sale");
        if (!isAllowedAfterSaleAction(normalizedAction, authentication)) {
            return Result.forbidden("forbidden to perform action: " + normalizedAction);
        }
        return Result.success(orderService.advanceAfterSaleStatus(afterSaleId, normalizedAction, remark));
    }

    private boolean isAdmin(Authentication authentication) {
        return SecurityPermissionUtils.isAdmin(authentication);
    }

    private boolean isMerchant(Authentication authentication) {
        return SecurityPermissionUtils.isMerchant(authentication);
    }

    private boolean isAllowedSubOrderAction(String action, Authentication authentication) {
        if (isAdmin(authentication)) {
            return true;
        }
        if (isMerchant(authentication)) {
            return MERCHANT_SUB_ACTIONS.contains(action);
        }
        return USER_SUB_ACTIONS.contains(action);
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


