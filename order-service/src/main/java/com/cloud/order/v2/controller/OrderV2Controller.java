package com.cloud.order.v2.controller;

import com.cloud.common.result.Result;
import com.cloud.common.result.PageResult;
import com.cloud.common.security.SecurityPermissionUtils;
import com.cloud.common.exception.BusinessException;
import com.cloud.order.v2.dto.CreateMainOrderRequest;
import com.cloud.order.v2.entity.AfterSaleV2;
import com.cloud.order.v2.entity.OrderMainV2;
import com.cloud.order.v2.entity.OrderSubV2;
import com.cloud.order.v2.mapper.OrderMainV2Mapper;
import com.cloud.order.v2.mapper.OrderSubV2Mapper;
import com.cloud.order.v2.service.OrderV2Service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v2")
@RequiredArgsConstructor
public class OrderV2Controller {

    private final OrderV2Service orderV2Service;
    private final OrderMainV2Mapper orderMainV2Mapper;
    private final OrderSubV2Mapper orderSubV2Mapper;

    private static final Map<Integer, String> LEGACY_STATUS_TO_V2 = Map.of(
            0, "CREATED",
            1, "PAID",
            2, "SHIPPED",
            3, "DONE",
            4, "CANCELLED"
    );
    private static final Map<String, Integer> V2_STATUS_TO_LEGACY = Map.of(
            "CREATED", 0,
            "STOCK_RESERVED", 0,
            "PAID", 1,
            "SHIPPED", 2,
            "RECEIVED", 3,
            "DONE", 3,
            "CANCELLED", 4,
            "CLOSED", 4
    );

    @PostMapping("/order-main")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    public Result<OrderMainV2> createMainOrder(@RequestBody @Valid CreateMainOrderRequest request,
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
        return Result.success(orderV2Service.createMainOrder(request));
    }

    @GetMapping("/orders")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasMerchantAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    public Result<PageResult<Map<String, Object>>> listOrders(@RequestParam(defaultValue = "1") Long page,
                                                              @RequestParam(defaultValue = "20") Long size,
                                                              @RequestParam(required = false) Integer status,
                                                              Authentication authentication) {
        long current = page == null || page < 1 ? 1L : page;
        long pageSize = size == null || size < 1 ? 20L : size;
        String mappedStatus = status == null ? null : LEGACY_STATUS_TO_V2.get(status);
        if (status != null && mappedStatus == null) {
            throw new BusinessException("unsupported status");
        }

        Page<OrderSubV2> pager = new Page<>(current, pageSize);
        LambdaQueryWrapper<OrderSubV2> subQuery = new LambdaQueryWrapper<OrderSubV2>()
                .eq(OrderSubV2::getDeleted, 0)
                .orderByDesc(OrderSubV2::getCreatedAt);
        if (mappedStatus != null) {
            subQuery.eq(OrderSubV2::getOrderStatus, mappedStatus);
        }

        if (isMerchant(authentication)) {
            subQuery.eq(OrderSubV2::getMerchantId, requireCurrentUserId(authentication));
        } else if (!isAdmin(authentication)) {
            Long currentUserId = requireCurrentUserId(authentication);
            List<Long> mainOrderIds = orderMainV2Mapper.selectList(
                    new LambdaQueryWrapper<OrderMainV2>()
                            .eq(OrderMainV2::getUserId, currentUserId)
                            .eq(OrderMainV2::getDeleted, 0)
                            .select(OrderMainV2::getId)
            ).stream().map(OrderMainV2::getId).toList();
            if (mainOrderIds.isEmpty()) {
                return Result.success(PageResult.of(current, pageSize, 0L, List.of()));
            }
            subQuery.in(OrderSubV2::getMainOrderId, mainOrderIds);
        }

        Page<OrderSubV2> subPage = orderSubV2Mapper.selectPage(pager, subQuery);
        Set<Long> mainIds = subPage.getRecords().stream().map(OrderSubV2::getMainOrderId).collect(Collectors.toSet());
        Map<Long, OrderMainV2> mainMap = mainIds.isEmpty() ? Map.of() : orderMainV2Mapper.selectBatchIds(mainIds).stream()
                .collect(Collectors.toMap(OrderMainV2::getId, v -> v));

        List<Map<String, Object>> records = subPage.getRecords().stream().map(sub -> {
            OrderMainV2 main = mainMap.get(sub.getMainOrderId());
            Long userId = main == null ? null : main.getUserId();
            Integer legacyStatus = V2_STATUS_TO_LEGACY.getOrDefault(sub.getOrderStatus(), 0);
            return Map.<String, Object>of(
                    "id", sub.getId(),
                    "orderNo", sub.getSubOrderNo(),
                    "userId", userId == null ? 0L : userId,
                    "shopId", sub.getMerchantId(),
                    "totalAmount", main == null ? sub.getPayableAmount() : main.getTotalAmount(),
                    "payAmount", sub.getPayableAmount(),
                    "status", legacyStatus,
                    "addressId", 0L,
                    "createdAt", sub.getCreatedAt()
            );
        }).toList();

        return Result.success(PageResult.of(subPage.getCurrent(), subPage.getSize(), subPage.getTotal(), records));
    }

    @GetMapping("/orders/{subOrderId}")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasMerchantAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    public Result<Map<String, Object>> getOrder(@PathVariable Long subOrderId, Authentication authentication) {
        OrderSubV2 sub = requireAccessibleSubOrder(subOrderId, authentication);
        OrderMainV2 main = orderMainV2Mapper.selectById(sub.getMainOrderId());
        Integer legacyStatus = V2_STATUS_TO_LEGACY.getOrDefault(sub.getOrderStatus(), 0);
        return Result.success(Map.of(
                "id", sub.getId(),
                "orderNo", sub.getSubOrderNo(),
                "userId", main == null ? 0L : main.getUserId(),
                "shopId", sub.getMerchantId(),
                "totalAmount", main == null ? sub.getPayableAmount() : main.getTotalAmount(),
                "payAmount", sub.getPayableAmount(),
                "status", legacyStatus,
                "addressId", 0L,
                "createdAt", sub.getCreatedAt()
        ));
    }

    @PostMapping("/orders/{subOrderId}/pay")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    public Result<Boolean> pay(@PathVariable Long subOrderId, Authentication authentication) {
        requireAccessibleSubOrder(subOrderId, authentication);
        orderV2Service.advanceSubOrderStatus(subOrderId, "PAY");
        return Result.success(true);
    }

    @PostMapping("/orders/{subOrderId}/ship")
    @PreAuthorize("@permissionManager.hasMerchantAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    public Result<Boolean> ship(@PathVariable Long subOrderId, Authentication authentication) {
        requireAccessibleSubOrder(subOrderId, authentication);
        orderV2Service.advanceSubOrderStatus(subOrderId, "SHIP");
        return Result.success(true);
    }

    @PostMapping("/orders/{subOrderId}/complete")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    public Result<Boolean> complete(@PathVariable Long subOrderId, Authentication authentication) {
        requireAccessibleSubOrder(subOrderId, authentication);
        orderV2Service.advanceSubOrderStatus(subOrderId, "DONE");
        return Result.success(true);
    }

    @PostMapping("/orders/{subOrderId}/cancel")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    public Result<Boolean> cancel(@PathVariable Long subOrderId, Authentication authentication) {
        requireAccessibleSubOrder(subOrderId, authentication);
        orderV2Service.advanceSubOrderStatus(subOrderId, "CANCEL");
        return Result.success(true);
    }

    @PostMapping("/orders/batch/pay")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    public Result<Integer> batchPay(@RequestBody List<Long> subOrderIds, Authentication authentication) {
        return Result.success(batchAdvance(subOrderIds, "PAY", authentication));
    }

    @PostMapping("/orders/batch/ship")
    @PreAuthorize("@permissionManager.hasMerchantAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    public Result<Integer> batchShip(@RequestBody List<Long> subOrderIds, Authentication authentication) {
        return Result.success(batchAdvance(subOrderIds, "SHIP", authentication));
    }

    @PostMapping("/orders/batch/complete")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    public Result<Integer> batchComplete(@RequestBody List<Long> subOrderIds, Authentication authentication) {
        return Result.success(batchAdvance(subOrderIds, "DONE", authentication));
    }

    @PostMapping("/orders/batch/cancel")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    public Result<Integer> batchCancel(@RequestBody List<Long> subOrderIds, Authentication authentication) {
        return Result.success(batchAdvance(subOrderIds, "CANCEL", authentication));
    }

    @GetMapping("/order-main/{mainOrderId}/sub-orders")
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

    @PostMapping("/order-sub/{subOrderId}/actions/{action}")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasMerchantAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    public Result<OrderSubV2> advanceSubOrderStatus(@PathVariable Long subOrderId,
                                                    @PathVariable String action,
                                                    Authentication authentication) {
        OrderSubV2 subOrder = orderV2Service.getSubOrder(subOrderId);
        if (subOrder == null || Integer.valueOf(1).equals(subOrder.getDeleted())) {
            return Result.notFound("sub order not found");
        }
        if (!isAdmin(authentication)) {
            OrderMainV2 mainOrder = orderV2Service.getMainOrder(subOrder.getMainOrderId());
            if (mainOrder == null || Integer.valueOf(1).equals(mainOrder.getDeleted())) {
                return Result.notFound("main order not found");
            }
            Long currentUserId = requireCurrentUserId(authentication);
            if (isMerchant(authentication)) {
                if (!Objects.equals(currentUserId, subOrder.getMerchantId())) {
                    return Result.forbidden("forbidden to operate another merchant's sub order");
                }
            } else if (!Objects.equals(currentUserId, mainOrder.getUserId())) {
                return Result.forbidden("forbidden to operate another user's sub order");
            }
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

    private int batchAdvance(List<Long> subOrderIds, String action, Authentication authentication) {
        if (subOrderIds == null || subOrderIds.isEmpty()) {
            return 0;
        }
        int success = 0;
        for (Long subOrderId : subOrderIds) {
            try {
                requireAccessibleSubOrder(subOrderId, authentication);
                orderV2Service.advanceSubOrderStatus(subOrderId, action);
                success++;
            } catch (Exception ignored) {
            }
        }
        return success;
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
