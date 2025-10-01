package com.cloud.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.common.annotation.DistributedLock;
import com.cloud.common.domain.dto.order.OrderCreateDTO;
import com.cloud.common.domain.dto.order.OrderDTO;
import com.cloud.common.domain.vo.order.OrderVO;
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
@RequestMapping("/orders")
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
            return Result.error("订单不存在");
        }

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

        try {
            log.info("🛍️ 创建订单请求 - 用户ID: {}, 商品数量: {}",
                    orderCreateDTO.getUserId(), orderCreateDTO.getOrderItems().size());

            OrderDTO orderDTO = orderService.createOrder(orderCreateDTO);
            log.info("✅ 订单创建成功 - 订单ID: {}, 用户ID: {}",
                    orderDTO.getId(), orderDTO.getUserId());

            return Result.success("订单创建成功", orderDTO);
        } catch (Exception e) {
            log.error("❌ 订单创建失败 - 用户ID: {}", orderCreateDTO.getUserId(), e);
            return Result.error("订单创建失败: " + e.getMessage());
        }
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

        try {
            Boolean result = orderService.updateOrder(orderDTO);
            return Result.success("订单更新成功", result);
        } catch (Exception e) {
            log.error("更新订单信息失败，订单ID: {}", id, e);
            return Result.error("更新订单信息失败: " + e.getMessage());
        }
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

        try {
            Boolean result = orderService.deleteOrder(id);
            return Result.success("订单删除成功", result);
        } catch (Exception e) {
            log.error("删除订单失败，订单ID: {}", id, e);
            return Result.error("删除订单失败: " + e.getMessage());
        }
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

        try {
            log.info("💳 接收支付订单请求 - 订单ID: {}", id);
            Boolean result = orderService.payOrder(id);
            
            if (result) {
                log.info("✅ 订单支付成功 - 订单ID: {}", id);
                return Result.success("订单支付成功", result);
            } else {
                log.warn("⚠️ 订单支付失败 - 订单ID: {}", id);
                return Result.error("订单支付失败，请检查订单状态");
            }
        } catch (Exception e) {
            log.error("❌ 支付订单失败 - 订单ID: {}, 错误: {}", id, e.getMessage(), e);
            return Result.error("订单支付失败: " + e.getMessage());
        }
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

        try {
            log.info("📦 接收发货订单请求 - 订单ID: {}", id);
            Boolean result = orderService.shipOrder(id);
            
            if (result) {
                log.info("✅ 订单发货成功 - 订单ID: {}", id);
                return Result.success("订单发货成功", result);
            } else {
                log.warn("⚠️ 订单发货失败 - 订单ID: {}", id);
                return Result.error("订单发货失败，请检查订单状态");
            }
        } catch (Exception e) {
            log.error("❌ 发货订单失败 - 订单ID: {}, 错误: {}", id, e.getMessage(), e);
            return Result.error("订单发货失败: " + e.getMessage());
        }
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

        try {
            log.info("✅ 接收完成订单请求 - 订单ID: {}", id);
            Boolean result = orderService.completeOrder(id);
            
            if (result) {
                log.info("✅ 订单完成成功 - 订单ID: {}", id);
                return Result.success("订单完成成功", result);
            } else {
                log.warn("⚠️ 订单完成失败 - 订单ID: {}", id);
                return Result.error("订单完成失败，请检查订单状态");
            }
        } catch (Exception e) {
            log.error("❌ 完成订单失败 - 订单ID: {}, 错误: {}", id, e.getMessage(), e);
            return Result.error("订单完成失败: " + e.getMessage());
        }
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

        try {
            log.info("❌ 接收取消订单请求 - 订单ID: {}, 取消原因: {}", id, cancelReason);
            Boolean result = orderService.cancelOrder(id);
            
            if (result) {
                log.info("✅ 订单取消成功 - 订单ID: {}", id);
                return Result.success("订单取消成功", result);
            } else {
                log.warn("⚠️ 订单取消失败 - 订单ID: {}", id);
                return Result.error("订单取消失败，请检查订单状态");
            }
        } catch (Exception e) {
            log.error("❌ 取消订单失败 - 订单ID: {}, 错误: {}", id, e.getMessage(), e);
            return Result.error("订单取消失败: " + e.getMessage());
        }
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

        try {
            List<OrderDTO> orders = orderService.getOrdersByUserId(userId);
            return Result.success("查询成功", orders);
        } catch (Exception e) {
            log.error("获取用户订单列表失败，用户ID: {}", userId, e);
            return Result.error("获取用户订单列表失败: " + e.getMessage());
        }
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

        try {
            Boolean isPaid = orderService.isOrderPaid(id);
            return Result.success("查询成功", isPaid);
        } catch (Exception e) {
            log.error("检查订单支付状态失败，订单ID: {}", id, e);
            return Result.error("检查订单支付状态失败: " + e.getMessage());
        }
    }
}
