package com.cloud.order.controller;

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

/**
 * 订单管理控制器
 * 提供订单的增删改等管理功能
 */
@Slf4j
@RestController
@RequestMapping("/order/manage")
@RequiredArgsConstructor
@Tag(name = "订单管理", description = "订单管理接口")
public class OrderManageController {

    private final OrderService orderService;
    private final OrderConverter orderConverter = OrderConverter.INSTANCE;
    private final OrderMessageProducerImpl orderMessageProducerImpl;

    /**
     * 创建订单
     *
     * @param orderDTO 订单信息
     * @return 创建结果
     */
    @PostMapping("/create")
    @Operation(summary = "创建订单", description = "创建新的订单")
    @ApiResponse(responseCode = "200", description = "创建成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<OrderDTO> createOrder(
            @Parameter(description = "订单信息") @RequestBody OrderDTO orderDTO) {
        try {
            log.info("创建订单，用户ID: {}", orderDTO.getUserId());

            Order order = orderConverter.toEntity(orderDTO);
            boolean saved = orderService.save(order);

            if (saved) {
                Order savedOrder = orderService.getById(order.getId());
                OrderDTO savedOrderDTO = orderConverter.toDTO(savedOrder);

                // 异步发送订单变更消息到日志服务
                orderMessageProducerImpl.sendOrderChangeMessage(
                        savedOrder.getId(),
                        savedOrder.getUserId(),
                        null, // 创建订单前状态为null
                        savedOrder.getStatus(), // 创建订单后状态
                        1, // 1表示创建订单
                        "system" // 操作人，实际项目中应该从上下文中获取
                );

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
     * 更新订单
     *
     * @param id       订单ID
     * @param orderDTO 订单信息
     * @return 更新结果
     */
    @PutMapping("/update/{id}")
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

                // 异步发送订单变更消息到日志服务
                orderMessageProducerImpl.sendOrderChangeMessage(
                        updatedOrder.getId(),
                        updatedOrder.getUserId(),
                        existingOrder.getStatus(), // 更新前订单状态
                        updatedOrder.getStatus(), // 更新后订单状态
                        2, // 2表示更新订单
                        "system" // 操作人，实际项目中应该从上下文中获取
                );

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
    @DeleteMapping("/delete/{id}")
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
                // 异步发送订单变更消息到日志服务
                orderMessageProducerImpl.sendOrderChangeMessage(
                        existingOrder.getId(),
                        existingOrder.getUserId(),
                        existingOrder.getStatus(), // 删除前订单状态
                        null, // 删除后订单状态为null
                        3, // 3表示删除订单
                        "system" // 操作人，实际项目中应该从上下文中获取
                );

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
}