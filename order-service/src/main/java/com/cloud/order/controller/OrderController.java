package com.cloud.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.common.annotation.DistributedLock;
import com.cloud.common.domain.dto.order.OrderCreateDTO;
import com.cloud.common.domain.dto.order.OrderDTO;
import com.cloud.common.domain.vo.order.OrderVO;
import com.cloud.common.exception.BusinessException;
import com.cloud.common.exception.ResourceNotFoundException;
import com.cloud.common.result.PageResult;
import com.cloud.common.result.Result;
import com.cloud.common.security.SecurityPermissionUtils;
import com.cloud.order.dto.OrderPageQueryDTO;
import com.cloud.order.module.entity.Order;
import com.cloud.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Order Management", description = "Order REST APIs")
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or "
            + "@permissionManager.hasMerchantAccess(authentication) or "
            + "@permissionManager.hasAdminAccess(authentication)")
    @Operation(summary = "Get orders", description = "Get order list with pagination")
    public Result<PageResult<OrderVO>> getOrders(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "User ID") @RequestParam(required = false) Long userId,
            @Parameter(description = "Shop ID") @RequestParam(required = false) Long shopId,
            @Parameter(description = "Order status") @RequestParam(required = false) Integer status,
            Authentication authentication) {

        OrderPageQueryDTO queryDTO = new OrderPageQueryDTO();
        queryDTO.setCurrent(page.longValue());
        queryDTO.setSize(size.longValue());
        if (isAdmin(authentication)) {
            queryDTO.setUserId(userId);
            queryDTO.setShopId(shopId);
        } else if (isMerchant(authentication)) {
            Long currentMerchantId = requireCurrentUserId(authentication);
            if (shopId != null && !Objects.equals(shopId, currentMerchantId)) {
                throw new BusinessException("forbidden to query other merchant's orders");
            }
            if (userId != null) {
                throw new BusinessException("merchant cannot filter by userId");
            }
            queryDTO.setShopId(currentMerchantId);
        } else {
            Long currentUserId = requireCurrentUserId(authentication);
            if (userId != null && !Objects.equals(userId, currentUserId)) {
                throw new BusinessException("forbidden to query other user's orders");
            }
            queryDTO.setUserId(currentUserId);
            if (shopId != null) {
                throw new BusinessException("user cannot filter by shopId");
            }
        }
        queryDTO.setStatus(status);

        Page<OrderVO> pageResult = orderService.pageQuery(queryDTO);
        PageResult<OrderVO> result = PageResult.of(
                pageResult.getCurrent(),
                pageResult.getSize(),
                pageResult.getTotal(),
                pageResult.getRecords()
        );
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @Operation(summary = "Get order by ID", description = "Get order details by order ID")
    public Result<OrderDTO> getOrderById(
            @Parameter(description = "Order ID") @PathVariable @Positive(message = "order id must be positive") Long id,
            Authentication authentication) {

        OrderDTO order = orderService.getByOrderEntityId(id);
        if (order == null) {
            throw new ResourceNotFoundException("Order", String.valueOf(id));
        }
        assertCanAccessOrder(order, authentication);
        return Result.success("query successful", order);
    }

    @PostMapping
    @PreAuthorize("@permissionManager.hasUserAccess(authentication)")
    @DistributedLock(
            key = "'order:create:user:' + #orderCreateDTO.userId + ':' + T(System).currentTimeMillis() / 60000",
            waitTime = 3,
            leaseTime = 10,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "failed to acquire order create lock"
    )
    @Operation(summary = "Create order", description = "Create a new order")
    public Result<OrderDTO> createOrder(
            @Parameter(description = "Order payload") @RequestBody
            @Valid @NotNull(message = "order payload is required") OrderCreateDTO orderCreateDTO,
            Authentication authentication) {

        Long currentUserId = requireCurrentUserId(authentication);
        if (orderCreateDTO.getUserId() == null) {
            orderCreateDTO.setUserId(currentUserId);
        } else if (!Objects.equals(orderCreateDTO.getUserId(), currentUserId)) {
            throw new BusinessException("forbidden to create order for another user");
        }

        OrderDTO orderDTO = orderService.createOrder(orderCreateDTO);
        return Result.success("order created", orderDTO);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @Operation(summary = "Update order", description = "Update order details")
    public Result<Boolean> updateOrder(
            @Parameter(description = "Order ID") @PathVariable Long id,
            @Parameter(description = "Order payload") @RequestBody
            @Valid @NotNull(message = "order payload is required") OrderDTO orderDTO,
            Authentication authentication) {

        if (!isAdmin(authentication)) {
            OrderDTO existingOrder = orderService.getByOrderEntityId(id);
            assertCanAccessOrder(existingOrder, authentication);
            orderDTO.setUserId(existingOrder.getUserId());
            orderDTO.setOrderNo(existingOrder.getOrderNo());
            orderDTO.setTotalAmount(existingOrder.getTotalAmount());
            orderDTO.setPayAmount(existingOrder.getPayAmount());
            orderDTO.setStatus(existingOrder.getStatus());
            orderDTO.setPayTime(existingOrder.getPayTime());
            orderDTO.setShipTime(existingOrder.getShipTime());
            orderDTO.setCompleteTime(existingOrder.getCompleteTime());
            orderDTO.setCancelTime(existingOrder.getCancelTime());
            orderDTO.setCancelReason(existingOrder.getCancelReason());
        }
        orderDTO.setId(id);
        Boolean result = orderService.updateOrder(orderDTO);
        return Result.success("order updated", result);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionManager.hasAdminAccess(authentication)")
    @Operation(summary = "Delete order", description = "Delete order by ID")
    public Result<Boolean> deleteOrder(
            @Parameter(description = "Order ID") @PathVariable @Positive(message = "order id must be positive") Long id) {

        Boolean result = orderService.deleteOrder(id);
        return Result.success("order deleted", result);
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @DistributedLock(
            key = "'order:pay:' + #id",
            waitTime = 5,
            leaseTime = 15,
            failMessage = "failed to acquire order pay lock"
    )
    @Operation(summary = "Pay order", description = "Pay one order")
    public Result<Boolean> payOrder(
            @Parameter(description = "Order ID") @PathVariable Long id,
            Authentication authentication) {

        if (!isAdmin(authentication)) {
            assertCanAccessOrderId(id, authentication);
        }
        Boolean result = orderService.payOrder(id);
        if (!Boolean.TRUE.equals(result)) {
            throw new BusinessException("failed to pay order");
        }
        return Result.success("order paid", true);
    }

    @PostMapping("/{id}/ship")
    @PreAuthorize("@permissionChecker.checkPermission(authentication, 'order:manage') or @permissionManager.hasAdminAccess(authentication)")
    @DistributedLock(
            key = "'order:ship:' + #id",
            waitTime = 3,
            leaseTime = 10,
            failMessage = "failed to acquire order ship lock"
    )
    @Operation(summary = "Ship order", description = "Ship one order")
    public Result<Boolean> shipOrder(
            @Parameter(description = "Order ID") @PathVariable Long id,
            Authentication authentication) {

        if (!isAdmin(authentication)) {
            assertCanShipOrderId(id, authentication);
        }
        Boolean result = orderService.shipOrder(id);
        if (!Boolean.TRUE.equals(result)) {
            throw new BusinessException("failed to ship order");
        }
        return Result.success("order shipped", true);
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @DistributedLock(
            key = "'order:complete:' + #id",
            waitTime = 3,
            leaseTime = 10,
            failMessage = "failed to acquire order complete lock"
    )
    @Operation(summary = "Complete order", description = "Complete one order")
    public Result<Boolean> completeOrder(
            @Parameter(description = "Order ID") @PathVariable Long id,
            Authentication authentication) {

        if (!isAdmin(authentication)) {
            assertCanAccessOrderId(id, authentication);
        }
        Boolean result = orderService.completeOrder(id);
        if (!Boolean.TRUE.equals(result)) {
            throw new BusinessException("failed to complete order");
        }
        return Result.success("order completed", true);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @DistributedLock(
            key = "'order:cancel:' + #id",
            waitTime = 3,
            leaseTime = 10,
            failMessage = "failed to acquire order cancel lock"
    )
    @Operation(summary = "Cancel order", description = "Cancel one order")
    public Result<Boolean> cancelOrder(
            @Parameter(description = "Order ID") @PathVariable Long id,
            @Parameter(description = "Cancel reason") @RequestParam(required = false) String cancelReason,
            Authentication authentication) {

        if (!isAdmin(authentication)) {
            assertCanAccessOrderId(id, authentication);
        }
        Boolean result = orderService.cancelOrderWithReason(id, cancelReason);
        if (!Boolean.TRUE.equals(result)) {
            throw new BusinessException("failed to cancel order");
        }
        return Result.success("order cancelled", true);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @Operation(summary = "Get user orders", description = "Get all orders by user ID")
    public Result<List<OrderDTO>> getOrdersByUserId(
            @Parameter(description = "User ID") @PathVariable Long userId,
            Authentication authentication) {

        if (!isAdmin(authentication)) {
            Long currentUserId = requireCurrentUserId(authentication);
            if (!Objects.equals(currentUserId, userId)) {
                throw new BusinessException("forbidden to query other user's orders");
            }
        }
        List<OrderDTO> orders = orderService.getOrdersByUserId(userId);
        return Result.success("query successful", orders);
    }

    @GetMapping("/{id}/paid-status")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @Operation(summary = "Get order paid status", description = "Check whether one order is paid")
    public Result<Boolean> isOrderPaid(
            @Parameter(description = "Order ID") @PathVariable Long id,
            Authentication authentication) {

        if (!isAdmin(authentication)) {
            assertCanAccessOrderId(id, authentication);
        }
        Boolean isPaid = orderService.isOrderPaid(id);
        return Result.success("query successful", isPaid);
    }

    @DeleteMapping("/batch")
    @PreAuthorize("@permissionManager.hasAdminAccess(authentication)")
    @Operation(summary = "Batch delete orders", description = "Delete orders by ID list")
    public Result<Integer> deleteOrdersBatch(
            @Parameter(description = "Order IDs") @RequestBody
            @NotNull(message = "order ids are required") List<Long> ids) {

        if (ids.isEmpty()) {
            return Result.badRequest("order ids cannot be empty");
        }
        if (ids.size() > 100) {
            return Result.badRequest("batch size cannot exceed 100");
        }

        Integer successCount = orderService.batchDeleteOrders(ids);
        return Result.success(String.format("batch delete completed: %d/%d", successCount, ids.size()), successCount);
    }

    @PostMapping("/batch/cancel")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @Operation(summary = "Batch cancel orders", description = "Cancel orders in batch")
    public Result<Integer> cancelOrdersBatch(
            @Parameter(description = "Order IDs") @RequestBody
            @NotNull(message = "order ids are required") List<Long> ids,
            @Parameter(description = "Cancel reason") @RequestParam(required = false) String cancelReason,
            Authentication authentication) {

        if (ids.isEmpty()) {
            return Result.badRequest("order ids cannot be empty");
        }
        if (ids.size() > 100) {
            return Result.badRequest("batch size cannot exceed 100");
        }
        if (!isAdmin(authentication)) {
            assertCanAccessOrderIds(ids, authentication);
        }

        int successCount = 0;
        for (Long orderId : ids) {
            try {
                if (Boolean.TRUE.equals(orderService.cancelOrderWithReason(orderId, cancelReason))) {
                    successCount++;
                }
            } catch (Exception ex) {
                log.warn("Batch cancel order failed: orderId={}", orderId, ex);
            }
        }
        return Result.success(String.format("batch cancel completed: %d/%d", successCount, ids.size()), successCount);
    }

    @PostMapping("/batch/ship")
    @PreAuthorize("@permissionChecker.checkPermission(authentication, 'order:manage') or @permissionManager.hasAdminAccess(authentication)")
    @Operation(summary = "Batch ship orders", description = "Ship orders in batch")
    public Result<Integer> shipOrdersBatch(
            @Parameter(description = "Order IDs") @RequestBody
            @NotNull(message = "order ids are required") List<Long> ids,
            Authentication authentication) {

        if (ids.isEmpty()) {
            return Result.badRequest("order ids cannot be empty");
        }
        if (ids.size() > 100) {
            return Result.badRequest("batch size cannot exceed 100");
        }
        if (!isAdmin(authentication)) {
            assertCanShipOrderIds(ids, authentication);
        }

        Integer successCount = orderService.batchUpdateOrderStatus(ids, 2);
        return Result.success(String.format("batch ship completed: %d/%d", successCount, ids.size()), successCount);
    }

    @PostMapping("/batch/complete")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @Operation(summary = "Batch complete orders", description = "Complete orders in batch")
    public Result<Integer> completeOrdersBatch(
            @Parameter(description = "Order IDs") @RequestBody
            @NotNull(message = "order ids are required") List<Long> ids,
            Authentication authentication) {

        if (ids.isEmpty()) {
            return Result.badRequest("order ids cannot be empty");
        }
        if (ids.size() > 100) {
            return Result.badRequest("batch size cannot exceed 100");
        }
        if (!isAdmin(authentication)) {
            assertCanAccessOrderIds(ids, authentication);
        }

        Integer successCount = orderService.batchUpdateOrderStatus(ids, 3);
        return Result.success(String.format("batch complete completed: %d/%d", successCount, ids.size()), successCount);
    }

    @PostMapping("/batch/pay")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @Operation(summary = "Batch pay orders", description = "Pay orders in batch")
    public Result<Integer> payOrdersBatch(
            @Parameter(description = "Order IDs") @RequestBody
            @NotNull(message = "order ids are required") List<Long> ids,
            Authentication authentication) {

        if (ids.isEmpty()) {
            return Result.badRequest("order ids cannot be empty");
        }
        if (ids.size() > 100) {
            return Result.badRequest("batch size cannot exceed 100");
        }
        if (!isAdmin(authentication)) {
            assertCanAccessOrderIds(ids, authentication);
        }

        Integer successCount = orderService.batchUpdateOrderStatus(ids, 1);
        return Result.success(String.format("batch pay completed: %d/%d", successCount, ids.size()), successCount);
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

    private void assertCanAccessOrderId(Long orderId, Authentication authentication) {
        OrderDTO order = orderService.getByOrderEntityId(orderId);
        assertCanAccessOrder(order, authentication);
    }

    private void assertCanAccessOrder(OrderDTO order, Authentication authentication) {
        if (order == null) {
            return;
        }
        if (isAdmin(authentication)) {
            return;
        }
        Long currentUserId = requireCurrentUserId(authentication);
        if (!Objects.equals(currentUserId, order.getUserId())) {
            throw new BusinessException("forbidden to access other user's order");
        }
    }

    private void assertCanAccessOrderIds(List<Long> orderIds, Authentication authentication) {
        if (orderIds == null || orderIds.isEmpty()) {
            return;
        }
        for (Long orderId : orderIds) {
            assertCanAccessOrderId(orderId, authentication);
        }
    }

    private void assertCanShipOrderId(Long orderId, Authentication authentication) {
        if (!isMerchant(authentication)) {
            throw new BusinessException("forbidden to ship order");
        }
        Long currentUserId = requireCurrentUserId(authentication);
        Order order = orderService.getById(orderId);
        if (order == null) {
            throw new ResourceNotFoundException("Order", String.valueOf(orderId));
        }
        if (!Objects.equals(currentUserId, order.getShopId())) {
            throw new BusinessException("forbidden to ship order of another merchant");
        }
    }

    private void assertCanShipOrderIds(List<Long> orderIds, Authentication authentication) {
        if (orderIds == null || orderIds.isEmpty()) {
            return;
        }
        for (Long orderId : orderIds) {
            assertCanShipOrderId(orderId, authentication);
        }
    }
}
