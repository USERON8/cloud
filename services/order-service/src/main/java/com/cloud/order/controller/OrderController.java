package com.cloud.order.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.common.exception.BusinessException;
import com.cloud.common.result.PageResult;
import com.cloud.common.result.Result;
import com.cloud.common.security.SecurityPermissionUtils;
import com.cloud.order.dto.CreateMainOrderRequest;
import com.cloud.order.dto.OrderAggregateResponse;
import com.cloud.order.dto.OrderSummaryDTO;
import com.cloud.order.entity.AfterSale;
import com.cloud.order.entity.OrderMain;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.mapper.OrderMainMapper;
import com.cloud.order.mapper.OrderSubMapper;
import com.cloud.order.service.OrderPlacementService;
import com.cloud.order.service.OrderService;
import com.cloud.order.service.impl.OrderShippingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import cn.hutool.core.util.StrUtil;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Order API", description = "Order creation and after-sale APIs")
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
    private static final Set<String> USER_AFTER_SALE_ACTIONS = Set.of("CANCEL");
    private static final Set<String> MERCHANT_AFTER_SALE_ACTIONS = Set.of(
            "AUDIT", "APPROVE", "REJECT", "WAIT_RETURN", "RETURN", "RECEIVE"
    );

    private final OrderService orderService;
    private final OrderPlacementService orderPlacementService;
    private final OrderShippingService orderShippingService;
    private final OrderMainMapper orderMainMapper;
    private final OrderSubMapper orderSubMapper;

    @PostMapping
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @Operation(summary = "Create main order")
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

    @GetMapping
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasMerchantAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @Operation(summary = "List orders")
    public Result<PageResult<OrderSummaryDTO>> listOrders(@RequestParam(required = false) Integer page,
                                                          @RequestParam(required = false) Integer size,
                                                          @RequestParam(required = false) Long userId,
                                                          @RequestParam(required = false) Long shopId,
                                                          @RequestParam(required = false) Integer status,
                                                          Authentication authentication) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null || size <= 0 ? 20 : size;
        safeSize = Math.min(safeSize, 100);

        List<String> statusFilters = resolveMainStatusFilters(status);
        IPage<OrderMain> pageResult = queryMainOrders(authentication, safePage, safeSize, userId, shopId, statusFilters);
        List<OrderMain> mains = pageResult == null ? Collections.emptyList() : pageResult.getRecords();
        Map<Long, List<OrderSub>> subOrdersByMainId = loadSubOrdersByMainIds(mains);

        List<OrderSummaryDTO> summaries = new ArrayList<>(mains.size());
        for (OrderMain main : mains) {
            List<OrderSub> subs = subOrdersByMainId.getOrDefault(main.getId(), List.of());
            OrderSummaryDTO summary = toSummary(main, subs);
            if (status != null && !status.equals(summary.getStatus())) {
                continue;
            }
            summaries.add(summary);
        }

        long total = pageResult == null ? 0L : pageResult.getTotal();
        PageResult<OrderSummaryDTO> response = PageResult.of((long) safePage, (long) safeSize, total, summaries);
        return Result.success(response);
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasMerchantAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @Operation(summary = "Get order detail")
    public Result<OrderSummaryDTO> getOrder(@PathVariable Long orderId, Authentication authentication) {
        OrderMain main = requireAccessibleMainOrder(orderId, authentication);
        List<OrderSub> subs = orderService.listSubOrders(orderId);
        return Result.success(toSummary(main, subs));
    }

    @PostMapping("/{orderId}/pay")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @Operation(summary = "Pay order")
    public Result<Boolean> payOrder(@PathVariable Long orderId, Authentication authentication) {
        OrderMain main = requireAccessibleMainOrder(orderId, authentication);
        List<OrderSub> subs = orderService.listSubOrders(main.getId());
        applySubOrderAction(subs, "PAY", authentication, null, null);
        return Result.success(true);
    }

    @PostMapping("/{orderId}/cancel")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @Operation(summary = "Cancel order")
    public Result<Boolean> cancelOrder(@PathVariable Long orderId,
                                       @RequestParam(required = false) String cancelReason,
                                       Authentication authentication) {
        OrderMain main = requireAccessibleMainOrder(orderId, authentication);
        List<OrderSub> subs = orderService.listSubOrders(main.getId());
        applySubOrderAction(subs, "CANCEL", authentication, null, null);
        if (StrUtil.isNotBlank(cancelReason)) {
            main.setCancelReason(cancelReason.trim());
            orderMainMapper.updateById(main);
        }
        return Result.success(true);
    }

    @PostMapping("/{orderId}/ship")
    @PreAuthorize("@permissionManager.hasMerchantAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @Operation(summary = "Ship order")
    public Result<Boolean> shipOrderStandard(@PathVariable Long orderId,
                                             @RequestParam(required = false) String shippingCompany,
                                             @RequestParam(required = false) String trackingNumber,
                                             Authentication authentication) {
        OrderMain main = requireAccessibleMainOrder(orderId, authentication);
        List<OrderSub> subs = orderService.listSubOrders(main.getId());
        String company = StrUtil.isBlank(shippingCompany) ? "TEST" : shippingCompany.trim();
        String tracking = StrUtil.isBlank(trackingNumber) ? "TEST-" + main.getMainOrderNo() : trackingNumber.trim();
        applySubOrderAction(subs, "SHIP", authentication, company, tracking);
        return Result.success(true);
    }

    @PostMapping("/{orderId}/complete")
    @PreAuthorize("@permissionManager.hasMerchantAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @Operation(summary = "Complete order")
    public Result<Boolean> completeOrder(@PathVariable Long orderId, Authentication authentication) {
        OrderMain main = requireAccessibleMainOrder(orderId, authentication);
        List<OrderSub> subs = orderService.listSubOrders(main.getId());
        applySubOrderAction(subs, "DONE", authentication, null, null);
        return Result.success(true);
    }

    @PostMapping("/batch/pay")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @Operation(summary = "Batch pay orders")
    public Result<Integer> batchPay(@RequestBody List<Long> orderIds, Authentication authentication) {
        return Result.success(batchApply(orderIds, authentication, "PAY", null, null, null));
    }

    @PostMapping("/batch/cancel")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @Operation(summary = "Batch cancel orders")
    public Result<Integer> batchCancel(@RequestBody List<Long> orderIds,
                                       @RequestParam(required = false) String cancelReason,
                                       Authentication authentication) {
        return Result.success(batchApply(orderIds, authentication, "CANCEL", null, null, cancelReason));
    }

    @PostMapping("/batch/ship")
    @PreAuthorize("@permissionManager.hasMerchantAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @Operation(summary = "Batch ship orders")
    public Result<Integer> batchShip(@RequestBody List<Long> orderIds,
                                     @RequestParam(required = false) String shippingCompany,
                                     @RequestParam(required = false) String trackingNumber,
                                     Authentication authentication) {
        String company = StrUtil.isBlank(shippingCompany) ? "TEST" : shippingCompany.trim();
        String tracking = StrUtil.isBlank(trackingNumber) ? "TEST" : trackingNumber.trim();
        return Result.success(batchApply(orderIds, authentication, "SHIP", company, tracking, null));
    }

    @PostMapping("/batch/complete")
    @PreAuthorize("@permissionManager.hasMerchantAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @Operation(summary = "Batch complete orders")
    public Result<Integer> batchComplete(@RequestBody List<Long> orderIds, Authentication authentication) {
        return Result.success(batchApply(orderIds, authentication, "DONE", null, null, null));
    }

    @GetMapping("/main/{mainOrderId}")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasMerchantAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @Operation(summary = "Get main order detail")
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
    @Operation(summary = "List sub orders")
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
    @Operation(summary = "Advance sub order status")
    public Result<OrderSub> advanceSubOrderStatus(@PathVariable Long subOrderId,
                                                      @PathVariable String action,
                                                      @RequestParam(required = false) String shippingCompany,
                                                      @RequestParam(required = false) String trackingNumber,
                                                      Authentication authentication) {
        OrderSub subOrder = requireAccessibleSubOrder(subOrderId, authentication);
        if (subOrder == null) {
            return Result.notFound("sub order not found");
        }
        String normalizedAction = normalizeAction(action, SUB_ORDER_ACTIONS, "sub order");
        if (!isAllowedSubOrderAction(normalizedAction, authentication)) {
            return Result.forbidden("forbidden to perform action: " + normalizedAction);
        }
        if ("SHIP".equals(normalizedAction)) {
            return Result.success(orderShippingService.ship(subOrderId, shippingCompany, trackingNumber));
        }
        return Result.success(orderService.advanceSubOrderStatus(subOrderId, normalizedAction));
    }

    @PostMapping("/sub/{subOrderId}/ship")
    @PreAuthorize("@permissionManager.hasMerchantAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @Operation(summary = "Ship sub order")
    public Result<OrderSub> shipOrder(@PathVariable Long subOrderId,
                                      @RequestParam String shippingCompany,
                                      @RequestParam String trackingNumber,
                                      Authentication authentication) {
        OrderSub subOrder = requireAccessibleSubOrder(subOrderId, authentication);
        if (subOrder == null) {
            return Result.notFound("sub order not found");
        }
        return Result.success(orderShippingService.ship(subOrderId, shippingCompany, trackingNumber));
    }

    @PostMapping("/after-sales")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @Operation(summary = "Apply after-sale")
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
    @Operation(summary = "Advance after-sale status")
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

    private IPage<OrderMain> queryMainOrders(Authentication authentication,
                                             int page,
                                             int size,
                                             Long userId,
                                             Long shopId,
                                             List<String> statusFilters) {
        Page<OrderMain> pageData = new Page<>(page, size);
        if (isAdmin(authentication)) {
            if (shopId != null) {
                return orderMainMapper.selectPageByMerchant(pageData, shopId, statusFilters, userId);
            }
            LambdaQueryWrapper<OrderMain> wrapper = new LambdaQueryWrapper<OrderMain>()
                    .eq(OrderMain::getDeleted, 0);
            if (userId != null) {
                wrapper.eq(OrderMain::getUserId, userId);
            }
            if (statusFilters != null && !statusFilters.isEmpty()) {
                wrapper.in(OrderMain::getOrderStatus, statusFilters);
            }
            wrapper.orderByDesc(OrderMain::getId);
            return orderMainMapper.selectPage(pageData, wrapper);
        }

        if (isMerchant(authentication)) {
            Long merchantId = requireCurrentUserId(authentication);
            return orderMainMapper.selectPageByMerchant(pageData, merchantId, statusFilters, null);
        }

        Long currentUserId = requireCurrentUserId(authentication);
        LambdaQueryWrapper<OrderMain> wrapper = new LambdaQueryWrapper<OrderMain>()
                .eq(OrderMain::getDeleted, 0)
                .eq(OrderMain::getUserId, currentUserId);
        if (statusFilters != null && !statusFilters.isEmpty()) {
            wrapper.in(OrderMain::getOrderStatus, statusFilters);
        }
        wrapper.orderByDesc(OrderMain::getId);
        return orderMainMapper.selectPage(pageData, wrapper);
    }

    private Map<Long, List<OrderSub>> loadSubOrdersByMainIds(List<OrderMain> mains) {
        if (mains == null || mains.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> mainIds = mains.stream()
                .map(OrderMain::getId)
                .filter(id -> id != null)
                .toList();
        if (mainIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<OrderSub> subs = orderSubMapper.selectList(new LambdaQueryWrapper<OrderSub>()
                .in(OrderSub::getMainOrderId, mainIds)
                .eq(OrderSub::getDeleted, 0));
        if (subs == null || subs.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, List<OrderSub>> grouped = new java.util.HashMap<>();
        for (OrderSub sub : subs) {
            if (sub == null || sub.getMainOrderId() == null) {
                continue;
            }
            grouped.computeIfAbsent(sub.getMainOrderId(), ignored -> new ArrayList<>()).add(sub);
        }
        return grouped;
    }

    private OrderSummaryDTO toSummary(OrderMain main, List<OrderSub> subs) {
        OrderSummaryDTO summary = new OrderSummaryDTO();
        summary.setId(main.getId());
        summary.setOrderNo(main.getMainOrderNo());
        summary.setUserId(main.getUserId());
        summary.setTotalAmount(main.getTotalAmount());
        summary.setPayAmount(main.getPayableAmount());
        summary.setCreatedAt(main.getCreatedAt());
        summary.setStatus(resolveStatusCode(subs));
        return summary;
    }

    private Integer resolveStatusCode(List<OrderSub> subs) {
        if (subs == null || subs.isEmpty()) {
            return 0;
        }
        boolean allDone = subs.stream().allMatch(sub -> "DONE".equals(sub.getOrderStatus()));
        if (allDone) {
            return 3;
        }
        boolean allClosed = subs.stream().allMatch(sub ->
                "CANCELLED".equals(sub.getOrderStatus()) || "CLOSED".equals(sub.getOrderStatus()));
        if (allClosed) {
            return 4;
        }
        boolean anyShipped = subs.stream().anyMatch(sub -> "SHIPPED".equals(sub.getOrderStatus()));
        if (anyShipped) {
            return 2;
        }
        boolean anyPaid = subs.stream().anyMatch(sub -> "PAID".equals(sub.getOrderStatus()));
        if (anyPaid) {
            return 1;
        }
        return 0;
    }

    private List<String> resolveMainStatusFilters(Integer statusCode) {
        if (statusCode == null) {
            return List.of();
        }
        return switch (statusCode) {
            case 0 -> List.of("CREATED", "STOCK_RESERVED");
            case 1 -> List.of("PAID");
            case 2 -> List.of("PAID", "SHIPPED");
            case 3 -> List.of("DONE");
            case 4 -> List.of("CANCELLED", "CLOSED");
            default -> List.of();
        };
    }

    private void applySubOrderAction(List<OrderSub> subs,
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
            if (isMerchant(authentication) && !Objects.equals(sub.getMerchantId(), requireCurrentUserId(authentication))) {
                continue;
            }
            if ("SHIP".equals(action)) {
                orderShippingService.ship(sub.getId(), shippingCompany, trackingNumber);
            } else {
                orderService.advanceSubOrderStatus(sub.getId(), action);
            }
        }
    }

    private int batchApply(List<Long> orderIds,
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
            OrderMain main = requireAccessibleMainOrder(orderId, authentication);
            List<OrderSub> subs = orderService.listSubOrders(main.getId());
            applySubOrderAction(subs, action, authentication, shippingCompany, trackingNumber);
            if ("CANCEL".equals(action) && StrUtil.isNotBlank(cancelReason)) {
                main.setCancelReason(cancelReason.trim());
                orderMainMapper.updateById(main);
            }
            success += 1;
        }
        return success;
    }

    private OrderMain requireAccessibleMainOrder(Long orderId, Authentication authentication) {
        OrderMain main = orderService.getMainOrder(orderId);
        if (main == null || Integer.valueOf(1).equals(main.getDeleted())) {
            throw new BusinessException("main order not found");
        }
        if (isAdmin(authentication)) {
            return main;
        }
        Long currentUserId = requireCurrentUserId(authentication);
        if (isMerchant(authentication)) {
            boolean belongs = orderSubMapper.selectCount(new LambdaQueryWrapper<OrderSub>()
                    .eq(OrderSub::getMainOrderId, main.getId())
                    .eq(OrderSub::getMerchantId, currentUserId)
                    .eq(OrderSub::getDeleted, 0)) > 0;
            if (!belongs) {
                throw new BusinessException("forbidden");
            }
            return main;
        }
        if (!Objects.equals(main.getUserId(), currentUserId)) {
            throw new BusinessException("forbidden");
        }
        return main;
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


