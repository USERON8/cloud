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
import com.cloud.order.dto.OrderPageQueryDTO;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 订单RESTful API控制器
 * 提供订单资源的CRUD操作，参考User服务标准架构
 *
 * @author what's up
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "订单服务", description = "订单资源的RESTful API接口")
public class OrderController {

    private final OrderService orderService;

    /**
     * 获取订单列表（支持查询参数）
     */
    @GetMapping
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @Operation(summary = "获取订单列表", description = "获取订单列表，支持分页和查询参数")
    public Result<PageResult<OrderVO>> getOrders(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "用户ID") @RequestParam(required = false) Long userId,
            @Parameter(description = "订单状态") @RequestParam(required = false) Integer status) {

        OrderPageQueryDTO queryDTO = new OrderPageQueryDTO();
        queryDTO.setCurrent(page.longValue());
        queryDTO.setSize(size.longValue());
        queryDTO.setUserId(userId);
        queryDTO.setStatus(status);

        Page<OrderVO> pageResult = orderService.pageQuery(queryDTO);

        // 转换为PageResult
        PageResult<OrderVO> result = PageResult.of(
                pageResult.getCurrent(),
                pageResult.getSize(),
                pageResult.getTotal(),
                pageResult.getRecords()
        );

        return Result.success(result);
    }

    /**
     * 根据ID获取订单详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @Operation(summary = "获取订单详情", description = "根据订单ID获取订单详细信息")
    public Result<OrderDTO> getOrderById(
            @Parameter(description = "订单ID") @PathVariable
            @Positive(message = "订单ID必须为正整数") Long id,
            Authentication authentication) {

        OrderDTO order = orderService.getByOrderEntityId(id);
        if (order == null) {
            log.warn("订单不存在，订单ID: {}", id);
            throw new ResourceNotFoundException("Order", String.valueOf(id));
        }
        log.info("查询订单成功，订单ID: {}", id);
        return Result.success("查询成功", order);
    }

    /**
     * 创建订单
     */
    @PostMapping
    @PreAuthorize("@permissionManager.hasUserAccess(authentication)")
    @DistributedLock(
            key = "'order:create:user:' + #orderCreateDTO.userId + ':' + T(System).currentTimeMillis() / 60000",
            waitTime = 3,
            leaseTime = 10,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "订单创建过于频繁，请稍后再试"
    )
    @Operation(summary = "创建订单", description = "创建新订单")
    public Result<OrderDTO> createOrder(
            @Parameter(description = "订单信息") @RequestBody
            @Valid @NotNull(message = "订单信息不能为空") OrderCreateDTO orderCreateDTO) {

        log.info("开始创建订单，用户ID: {}, 商品数量: {}", orderCreateDTO.getUserId(), orderCreateDTO.getOrderItems().size());

        OrderDTO orderDTO = orderService.createOrder(orderCreateDTO);
        log.info("订单创建成功，订单ID: {}, 用户ID: {}", orderDTO.getId(), orderDTO.getUserId());

        return Result.success("订单创建成功", orderDTO);
    }

    /**
     * 更新订单信息
     */
    @PutMapping("/{id}")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @Operation(summary = "更新订单信息", description = "更新订单信息")
    public Result<Boolean> updateOrder(
            @Parameter(description = "订单ID") @PathVariable Long id,
            @Parameter(description = "订单信息") @RequestBody
            @Valid @NotNull(message = "订单信息不能为空") OrderDTO orderDTO,
            Authentication authentication) {

        // 确保路径参数与请求体中的ID一致
        orderDTO.setId(id);
        Boolean result = orderService.updateOrder(orderDTO);
        log.info("订单更新成功，订单ID: {}, 操作人: {}", id, authentication.getName());
        return Result.success("订单更新成功", result);
    }

    /**
     * 删除订单
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionManager.hasAdminAccess(authentication)")
    @Operation(summary = "删除订单", description = "删除订单")
    public Result<Boolean> deleteOrder(
            @Parameter(description = "订单ID") @PathVariable
            @Positive(message = "订单ID必须为正整数") Long id) {

        Boolean result = orderService.deleteOrder(id);
        log.info("订单删除成功: orderId={}", id);
        return Result.success("订单删除成功", result);
    }

    /**
     * 支付订单
     */
    @PostMapping("/{id}/pay")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @DistributedLock(
            key = "'order:pay:' + #id",
            waitTime = 5,
            leaseTime = 15,
            failMessage = "订单支付操作获取锁失败"
    )
    @Operation(summary = "支付订单", description = "将订单状态更新为已支付")
    public Result<Boolean> payOrder(
            @Parameter(description = "订单ID") @PathVariable Long id,
            Authentication authentication) {

        log.info("💳 接收支付订单请求 - 订单ID: {}", id);
        Boolean result = orderService.payOrder(id);

        if (!result) {
            log.warn("⚠️ 订单支付失败 - 订单ID: {}", id);
            throw new BusinessException("订单支付失败，请检查订单状态");
        }
        log.info("✅ 订单支付成功 - 订单ID: {}", id);
        return Result.success("订单支付成功", result);
    }

    /**
     * 发货订单
     */
    @PostMapping("/{id}/ship")
    @PreAuthorize("@permissionChecker.checkPermission(authentication, 'order:manage') or @permissionManager.hasAdminAccess(authentication)")
    @DistributedLock(
            key = "'order:ship:' + #id",
            waitTime = 3,
            leaseTime = 10,
            failMessage = "订单发货操作获取锁失败"
    )
    @Operation(summary = "发货订单", description = "将订单状态更新为已发货")
    public Result<Boolean> shipOrder(
            @Parameter(description = "订单ID") @PathVariable Long id,
            Authentication authentication) {

        log.info("📦 接收发货订单请求 - 订单ID: {}", id);
        Boolean result = orderService.shipOrder(id);

        if (!result) {
            log.warn("⚠️ 订单发货失败 - 订单ID: {}", id);
            throw new BusinessException("订单发货失败，请检查订单状态");
        }
        log.info("✅ 订单发货成功 - 订单ID: {}", id);
        return Result.success("订单发货成功", result);
    }

    /**
     * 完成订单
     */
    @PostMapping("/{id}/complete")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @DistributedLock(
            key = "'order:complete:' + #id",
            waitTime = 3,
            leaseTime = 10,
            failMessage = "订单完成操作获取锁失败"
    )
    @Operation(summary = "完成订单", description = "将订单状态更新为已完成")
    public Result<Boolean> completeOrder(
            @Parameter(description = "订单ID") @PathVariable Long id,
            Authentication authentication) {

        log.info("✅ 接收完成订单请求 - 订单ID: {}", id);
        Boolean result = orderService.completeOrder(id);

        if (!result) {
            log.warn("⚠️ 订单完成失败 - 订单ID: {}", id);
            throw new BusinessException("订单完成失败，请检查订单状态");
        }
        log.info("✅ 订单完成成功 - 订单ID: {}", id);
        return Result.success("订单完成成功", result);
    }

    /**
     * 取消订单
     */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @DistributedLock(
            key = "'order:cancel:' + #id",
            waitTime = 3,
            leaseTime = 10,
            failMessage = "订单取消操作获取锁失败"
    )
    @Operation(summary = "取消订单", description = "将订单状态更新为已取消")
    public Result<Boolean> cancelOrder(
            @Parameter(description = "订单ID") @PathVariable Long id,
            @Parameter(description = "取消原因") @RequestParam(required = false) String cancelReason,
            Authentication authentication) {

        log.info("❌ 接收取消订单请求 - 订单ID: {}, 取消原因: {}", id, cancelReason);
        Boolean result = orderService.cancelOrder(id);

        if (!result) {
            log.warn("⚠️ 订单取消失败 - 订单ID: {}", id);
            throw new BusinessException("订单取消失败，请检查订单状态");
        }
        log.info("✅ 订单取消成功 - 订单ID: {}", id);
        return Result.success("订单取消成功", result);
    }

    /**
     * 获取用户订单列表
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @Operation(summary = "获取用户订单列表", description = "获取指定用户的订单列表")
    public Result<List<OrderDTO>> getOrdersByUserId(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            Authentication authentication) {

        List<OrderDTO> orders = orderService.getOrdersByUserId(userId);
        log.info("查询用户订单列表成功: userId={}, count={}", userId, orders.size());
        return Result.success("查询成功", orders);
    }

    /**
     * 检查订单是否已支付
     */
    @GetMapping("/{id}/paid-status")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @Operation(summary = "检查订单支付状态", description = "检查订单是否已支付")
    public Result<Boolean> isOrderPaid(
            @Parameter(description = "订单ID") @PathVariable Long id,
            Authentication authentication) {

        Boolean isPaid = orderService.isOrderPaid(id);
        log.info("检查订单支付状态: orderId={}, isPaid={}", id, isPaid);
        return Result.success("查询成功", isPaid);
    }

    // ==================== 批量管理接口 ====================

    /**
     * 批量删除订单
     */
    @DeleteMapping("/batch")
    @PreAuthorize("@permissionManager.hasAdminAccess(authentication)")
    @Operation(summary = "批量删除订单", description = "批量删除订单")
    public Result<Integer> deleteOrdersBatch(
            @Parameter(description = "订单ID列表") @RequestBody
            @NotNull(message = "订单ID列表不能为空") List<Long> ids) {

        if (ids == null || ids.isEmpty()) {
            return Result.badRequest("订单ID列表不能为空");
        }

        if (ids.size() > 100) {
            return Result.badRequest("批量删除数量不能超过100个");
        }

        log.info("批量删除订单, count: {}", ids.size());

        // 使用批量删除方法，性能更优
        Integer successCount = orderService.batchDeleteOrders(ids);

        log.info("批量删除订单完成, 成功: {}/{}", successCount, ids.size());
        return Result.success(String.format("批量删除订单成功: %d/%d", successCount, ids.size()), successCount);
    }

    /**
     * 批量取消订单
     */
    @PostMapping("/batch/cancel")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @Operation(summary = "批量取消订单", description = "批量取消多个订单")
    public Result<Integer> cancelOrdersBatch(
            @Parameter(description = "订单ID列表") @RequestBody
            @NotNull(message = "订单ID列表不能为空") List<Long> ids,
            @Parameter(description = "取消原因") @RequestParam(required = false) String cancelReason,
            Authentication authentication) {

        if (ids == null || ids.isEmpty()) {
            return Result.badRequest("订单ID列表不能为空");
        }

        if (ids.size() > 100) {
            return Result.badRequest("批量取消数量不能超过100个");
        }

        log.info("❓ 批量取消订单, count: {}, reason: {}", ids.size(), cancelReason);

        // 使用批量更新方法，将订单状态设置为取消状态（假设4为取消状态）
        Integer successCount = orderService.batchUpdateOrderStatus(ids, 4);

        log.info("✅ 批量取消订单完成, 成功: {}/{}", successCount, ids.size());
        return Result.success(String.format("批量取消订单成功: %d/%d", successCount, ids.size()), successCount);
    }

    /**
     * 批量发货
     */
    @PostMapping("/batch/ship")
    @PreAuthorize("@permissionChecker.checkPermission(authentication, 'order:manage') or @permissionManager.hasAdminAccess(authentication)")
    @Operation(summary = "批量发货订单", description = "批量将订单设置为已发货状态")
    public Result<Integer> shipOrdersBatch(
            @Parameter(description = "订单ID列表") @RequestBody
            @NotNull(message = "订单ID列表不能为空") List<Long> ids,
            Authentication authentication) {

        if (ids == null || ids.isEmpty()) {
            return Result.badRequest("订单ID列表不能为空");
        }

        if (ids.size() > 100) {
            return Result.badRequest("批量发货数量不能超过100个");
        }

        log.info("📦 批量发货订单, count: {}", ids.size());

        // 使用批量更新方法，将订单状态设置为已发货状态（2）
        Integer successCount = orderService.batchUpdateOrderStatus(ids, 2);

        log.info("✅ 批量发货订单完成, 成功: {}/{}", successCount, ids.size());
        return Result.success(String.format("批量发货订单成功: %d/%d", successCount, ids.size()), successCount);
    }

    /**
     * 批量完成订单
     */
    @PostMapping("/batch/complete")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @Operation(summary = "批量完成订单", description = "批量将订单设置为已完成状态")
    public Result<Integer> completeOrdersBatch(
            @Parameter(description = "订单ID列表") @RequestBody
            @NotNull(message = "订单ID列表不能为空") List<Long> ids,
            Authentication authentication) {

        if (ids == null || ids.isEmpty()) {
            return Result.badRequest("订单ID列表不能为空");
        }

        if (ids.size() > 100) {
            return Result.badRequest("批量完成数量不能超过100个");
        }

        log.info("✅ 批量完成订单, count: {}", ids.size());

        // 使用批量更新方法，将订单状态设置为已完成状态（3）
        Integer successCount = orderService.batchUpdateOrderStatus(ids, 3);

        log.info("✅ 批量完成订单完成, 成功: {}/{}", successCount, ids.size());
        return Result.success(String.format("批量完成订单成功: %d/%d", successCount, ids.size()), successCount);
    }

    /**
     * 批量支付订单
     */
    @PostMapping("/batch/pay")
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @Operation(summary = "批量支付订单", description = "批量将订单设置为已支付状态")
    public Result<Integer> payOrdersBatch(
            @Parameter(description = "订单ID列表") @RequestBody
            @NotNull(message = "订单ID列表不能为空") List<Long> ids,
            Authentication authentication) {

        if (ids == null || ids.isEmpty()) {
            return Result.badRequest("订单ID列表不能为空");
        }

        if (ids.size() > 100) {
            return Result.badRequest("批量支付数量不能超过100个");
        }

        log.info("💳 批量支付订单, count: {}", ids.size());

        // 使用批量更新方法，将订单状态设置为已支付状态（1）
        Integer successCount = orderService.batchUpdateOrderStatus(ids, 1);

        log.info("✅ 批量支付订单完成, 成功: {}/{}", successCount, ids.size());
        return Result.success(String.format("批量支付订单成功: %d/%d", successCount, ids.size()), successCount);
    }
}
