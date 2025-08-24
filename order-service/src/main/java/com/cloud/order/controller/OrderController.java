package com.cloud.order.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.common.domain.Result;
import com.cloud.common.domain.dto.order.OrderDTO;
import com.cloud.common.enums.ResultCode;
import com.cloud.order.converter.OrderConverter;
import com.cloud.order.module.entity.Order;
import com.cloud.order.service.OrderService;
import com.cloud.order.service.impl.OrderMessageProducerImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单管理控制器
 * 提供订单的增删改查及分页查询功能
 */
@Slf4j
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "订单管理", description = "订单管理接口")
public class OrderController {
    private final OrderMessageProducerImpl orderMessageProducerImpl;
    private final OrderService orderService;
    private final OrderConverter orderConverter = OrderConverter.INSTANCE;

    /**
     * 创建订单
     *
     * @param orderDTO 订单信息
     * @return 创建结果
     */
    @PostMapping
    @Operation(summary = "创建订单", description = "创建新的订单")
    @ApiResponse(responseCode = "200", description = "创建成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<OrderDTO> createOrder(
            @Parameter(description = "订单信息") @RequestBody OrderDTO orderDTO,
            @RequestHeader("X-User-ID") String userId
    ) {
        try {
            log.info("创建订单，用户ID: {}", orderDTO.getUserId());

            Order order = orderConverter.toEntity(orderDTO);

            orderMessageProducerImpl.sendOrderChangeMessage(
                    order.getId(),
                    order.getUserId(),
                    null,
                    null,
                    1,
                    "系统");


            boolean saved = orderService.save(order);


            if (saved) {
                Order savedOrder = orderService.getById(order.getId());
                OrderDTO savedOrderDTO = orderConverter.toDTO(savedOrder);
                log.info("创建订单成功，订单ID: {}", savedOrder.getId());
                return Result.success(savedOrderDTO);
            } else {
                log.error("创建订单失败，用户ID: {}", orderDTO.getUserId());
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "创建订单失败");
            }
        } catch (Exception e) {
            log.error("创建订单失败", e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "创建订单失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取订单
     *
     * @param id 订单ID
     * @return 订单信息
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取订单详情", description = "根据ID获取订单详细信息")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<OrderDTO> getOrderById(
            @Parameter(description = "订单ID") @PathVariable Long id) {
        try {
            log.info("获取订单详情，订单ID: {}", id);

            Order order = orderService.getById(id);
            if (order == null) {
                log.warn("订单不存在，订单ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "订单不存在");
            }

            OrderDTO orderDTO = orderConverter.toDTO(order);
            log.info("获取订单详情成功，订单ID: {}", id);
            return Result.success(orderDTO);
        } catch (Exception e) {
            log.error("获取订单详情失败，订单ID: {}", id, e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "获取订单详情失败: " + e.getMessage());
        }
    }

    /**
     * 更新订单
     *
     * @param id       订单ID
     * @param orderDTO 订单信息
     * @return 更新结果
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新订单", description = "更新订单信息")
    @ApiResponse(responseCode = "200", description = "更新成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<OrderDTO> updateOrder(
            @Parameter(description = "订单ID") @PathVariable Long id,
            @Parameter(description = "订单信息") @RequestBody OrderDTO orderDTO) {
        try {
            log.info("更新订单，订单ID: {}", id);

            Order existingOrder = orderService.getById(id);
            if (existingOrder == null) {
                log.warn("订单不存在，订单ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "订单不存在");
            }

            Order order = orderConverter.toEntity(orderDTO);
            order.setId(id); // 确保ID一致
            boolean updated = orderService.updateById(order);

            if (updated) {
                Order updatedOrder = orderService.getById(id);
                OrderDTO updatedOrderDTO = orderConverter.toDTO(updatedOrder);
                log.info("更新订单成功，订单ID: {}", id);
                return Result.success(updatedOrderDTO);
            } else {
                log.error("更新订单失败，订单ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "更新订单失败");
            }
        } catch (Exception e) {
            log.error("更新订单失败，订单ID: {}", id, e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "更新订单失败: " + e.getMessage());
        }
    }

    /**
     * 删除订单
     *
     * @param id 订单ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除订单", description = "删除订单")
    @ApiResponse(responseCode = "200", description = "删除成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<Void> deleteOrder(
            @Parameter(description = "订单ID") @PathVariable Long id) {
        try {
            log.info("删除订单，订单ID: {}", id);

            Order existingOrder = orderService.getById(id);
            if (existingOrder == null) {
                log.warn("订单不存在，订单ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "订单不存在");
            }

            boolean removed = orderService.removeById(id);
            if (removed) {
                log.info("删除订单成功，订单ID: {}", id);
                return Result.success();
            } else {
                log.error("删除订单失败，订单ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "删除订单失败");
            }
        } catch (Exception e) {
            log.error("删除订单失败，订单ID: {}", id, e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "删除订单失败: " + e.getMessage());
        }
    }

    /**
     * 分页查询订单
     *
     * @param page   页码
     * @param size   每页数量
     * @param userId 用户ID（可选）
     * @param status 订单状态（可选）
     * @return 订单列表
     */
    @GetMapping
    @Operation(summary = "分页查询订单", description = "分页查询订单列表")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<Page<OrderDTO>> getOrders(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "用户ID") @RequestParam(required = false) Long userId,
            @Parameter(description = "订单状态") @RequestParam(required = false) Integer status) {
        try {
            log.info("分页查询订单，页码: {}，每页数量: {}，用户ID: {}，状态: {}", page, size, userId, status);

            Page<Order> orderPage = new Page<>(page, size);
            LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();

            // 添加查询条件
            if (userId != null) {
                queryWrapper.eq(Order::getUserId, userId);
            }
            if (status != null) {
                queryWrapper.eq(Order::getStatus, status);
            }

            // 按创建时间倒序排列
            queryWrapper.orderByDesc(Order::getCreatedAt);

            Page<Order> resultPage = orderService.page(orderPage, queryWrapper);

            // 转换为DTO
            Page<OrderDTO> dtoPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
            List<OrderDTO> dtoList = orderConverter.toDTOList(resultPage.getRecords());
            dtoPage.setRecords(dtoList);

            log.info("分页查询订单成功，共{}条记录", dtoPage.getTotal());
            return Result.success(dtoPage);
        } catch (Exception e) {
            log.error("分页查询订单失败", e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "分页查询订单失败: " + e.getMessage());
        }
    }
}