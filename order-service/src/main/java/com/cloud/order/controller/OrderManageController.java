package com.cloud.order.controller;

import com.cloud.common.domain.dto.order.OrderDTO;
import com.cloud.common.result.Result;
import com.cloud.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 订单管理控制器
 * 负责订单的修改操作，包括更新、支付、发货、完成、取消等
 *
 * @author cloud
 * @since 1.0.0
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "订单管理接口", description = "负责订单的修改操作，包括更新、支付、发货、完成、取消等")
public class OrderManageController {

    private final OrderService orderService;

    /**
     * 更新订单信息
     *
     * @param orderDTO 订单信息
     * @return 是否更新成功
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新订单", description = "更新订单信息")
    @PreAuthorize("@permissionChecker.checkPermission(authentication, 'order:manage') or @permissionManager.hasAdminAccess(authentication)")
    @CacheEvict(value = {"order", "order-list", "order-page"}, allEntries = true)
    public Result<Boolean> updateOrder(
            @Parameter(description = "订单ID") @PathVariable Long id,
            @Parameter(description = "订单信息", required = true)
            @Valid @RequestBody OrderDTO orderDTO) {
        orderDTO.setId(id);
        log.info("更新订单，ID: {}", id);
        Boolean result = orderService.updateOrder(orderDTO);
        return result ? Result.success(true) : Result.error("订单更新失败");
    }

    /**
     * 支付订单
     *
     * @param orderId       订单ID
     * @param currentUserId 当前用户ID
     * @return 是否支付成功
     */
    @PostMapping("/{orderId}/pay")
    @Operation(summary = "支付订单", description = "将订单状态更新为已支付")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @CacheEvict(value = {"order", "order-list", "order-page"}, allEntries = true)
    public Result<Boolean> payOrder(
            @Parameter(description = "订单ID", required = true)
            @NotNull(message = "订单ID不能为空")
            @PathVariable Long orderId,
            @Parameter(hidden = true)
            @RequestHeader("X-User-ID") String currentUserId) {
        Boolean result = orderService.payOrder(orderId, currentUserId);
        return result ? Result.success(true) : Result.error("订单支付失败");
    }

    /**
     * 发货订单
     *
     * @param orderId       订单ID
     * @param currentUserId 当前用户ID
     * @return 是否发货成功
     */
    @PostMapping("/{orderId}/ship")
    @Operation(summary = "发货订单", description = "将订单状态更新为已发货")
    @PreAuthorize("@permissionChecker.checkPermission(authentication, 'order:manage') or @permissionManager.hasAdminAccess(authentication)")
    public Result<Boolean> shipOrder(
            @Parameter(description = "订单ID", required = true)
            @NotNull(message = "订单ID不能为空")
            @PathVariable Long orderId,
            @Parameter(hidden = true)
            @RequestHeader("X-User-ID") String currentUserId) {
        Boolean result = orderService.shipOrder(orderId, currentUserId);
        return result ? Result.success(true) : Result.error("订单发货失败");
    }

    /**
     * 完成订单
     *
     * @param orderId       订单ID
     * @param currentUserId 当前用户ID
     * @return 是否完成成功
     */
    @PostMapping("/{orderId}/complete")
    @Operation(summary = "完成订单", description = "将订单状态更新为已完成")
    public Result<Boolean> completeOrder(
            @Parameter(description = "订单ID", required = true)
            @NotNull(message = "订单ID不能为空")
            @PathVariable Long orderId,
            @Parameter(hidden = true)
            @RequestHeader("X-User-ID") String currentUserId) {
        Boolean result = orderService.completeOrder(orderId, currentUserId);
        return result ? Result.success(true) : Result.error("订单完成失败");
    }

    /**
     * 删除订单
     *
     * @param id 订单ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除订单", description = "管理员删除订单")
    @PreAuthorize("@permissionManager.hasAdminAccess(authentication)")
    @CacheEvict(value = {"order", "order-list", "order-page"}, allEntries = true)
    public Result<Boolean> deleteOrder(
            @Parameter(description = "订单ID") @NotNull @PathVariable Long id) {
        log.info("删除订单，ID: {}", id);
        Boolean result = orderService.deleteOrder(id);
        return result ? Result.success(true) : Result.error("删除订单失败");
    }

    /**
     * 创建订单
     *
     * @param orderDTO 订单信息
     * @return 创建结果
     */
    @PostMapping
    @Operation(summary = "创建订单", description = "创建新订单")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication)")
    @CacheEvict(value = {"order-list", "order-page"}, allEntries = true)
    public Result<OrderDTO> createOrder(
            @Parameter(description = "订单信息", required = true)
            @Valid @RequestBody OrderDTO orderDTO) {
        log.info("创建订单，用户ID: {}", orderDTO.getUserId());
        OrderDTO result = orderService.createOrder(orderDTO);
        return result != null ? Result.success(result) : Result.error("创建订单失败");
    }
}