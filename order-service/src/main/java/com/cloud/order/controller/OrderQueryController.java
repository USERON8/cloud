package com.cloud.order.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.common.domain.PageResult;
import com.cloud.common.domain.Result;
import com.cloud.common.domain.dto.order.OrderDTO;
import com.cloud.common.enums.ResultCode;
import com.cloud.order.converter.OrderConverter;
import com.cloud.order.module.entity.Order;
import com.cloud.order.service.OrderService;
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
 * 订单查询控制器
 * 提供订单查询相关功能
 */
@Slf4j
@RestController
@RequestMapping("/order/query")
@RequiredArgsConstructor
@Tag(name = "订单查询", description = "订单查询接口")
public class OrderQueryController {

    private final OrderService orderService;
    private final OrderConverter orderConverter = OrderConverter.INSTANCE;

    /**
     * 根据ID获取订单详情
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
     * 分页查询订单
     *
     * @param page   页码
     * @param size   每页数量
     * @param userId 用户ID（可选）
     * @param status 订单状态（可选）
     * @return 订单列表
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询订单", description = "分页查询订单列表")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PageResult.class)))
    public PageResult<OrderDTO> getOrders(
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
            List<OrderDTO> dtoList = orderConverter.toDTOList(resultPage.getRecords());

            log.info("分页查询订单成功，共{}条记录", resultPage.getTotal());
            return PageResult.of(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal(), dtoList);
        } catch (Exception e) {
            log.error("分页查询订单失败", e);
            return PageResult.of(page.longValue(), size.longValue(), 0L, null);
        }
    }
}