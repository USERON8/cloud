package com.cloud.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.common.domain.dto.order.OrderDTO;
import com.cloud.common.domain.vo.OrderVO;
import com.cloud.common.result.Result;
import com.cloud.order.dto.OrderPageQueryDTO;
import com.cloud.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 订单查询控制器
 * 负责订单的查询操作，包括分页查询、根据ID查询等
 *
 * @author cloud
 * @since 1.0.0
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/order/query")
@RequiredArgsConstructor
@Tag(name = "订单查询接口", description = "负责订单的查询操作，包括分页查询、根据ID查询等")
public class OrderQueryController {

    private final OrderService orderService;

    /**
     * 分页查询订单
     *
     * @param queryDTO 查询条件
     * @return 订单分页结果
     */
    @PostMapping("/page")
    @Operation(summary = "分页查询订单", description = "根据条件分页查询订单列表")
    @Cacheable(value = "order-page", key = "'page:' + #queryDTO.page + ':' + #queryDTO.size + ':' + #queryDTO.status")
    public Result<Page<OrderVO>> queryOrdersByPage(
            @Parameter(description = "查询条件", required = true)
            @Valid @RequestBody OrderPageQueryDTO queryDTO) {
        log.debug("分页查询订单，条件: {}", queryDTO);
        Page<OrderVO> pageResult = orderService.pageQuery(queryDTO);
        return Result.success(pageResult);
    }

    /**
     * 根据ID获取订单详情
     *
     * @param id 订单ID
     * @return 订单信息
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取订单详情", description = "根据ID获取订单详细信息")
    @Cacheable(value = "order", key = "#id")
    public Result<OrderDTO> getOrderById(
            @Parameter(description = "订单ID") @NotNull @PathVariable Long id) {
        log.debug("查询订单详情，ID: {}", id);
        return Result.success(orderService.getByOrderEntityId(id));
    }

    /**
     * 根据用户ID查询订单列表
     *
     * @param userId 用户ID
     * @return 订单列表
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "根据用户ID查询订单", description = "查询指定用户的所有订单")
    @Cacheable(value = "order-list", key = "'user:' + #userId")
    public Result<java.util.List<OrderDTO>> getOrdersByUserId(
            @Parameter(description = "用户ID", required = true)
            @NotNull(message = "用户ID不能为空")
            @PathVariable Long userId) {
        log.debug("查询用户订单，用户ID: {}", userId);
        java.util.List<OrderDTO> orders = orderService.getOrdersByUserId(userId);
        return Result.success(orders);
    }

    /**
     * 根据订单号查询订单
     *
     * @param orderNo 订单号
     * @return 订单信息
     */
    @GetMapping("/orderNo/{orderNo}")
    @Operation(summary = "根据订单号查询订单", description = "根据订单号查询订单详细信息")
    @Cacheable(value = "order", key = "'orderNo:' + #orderNo")
    public Result<OrderDTO> getByOrderNo(
            @Parameter(description = "订单号", required = true)
            @NotNull(message = "订单号不能为空")
            @PathVariable String orderNo) {
        log.debug("查询订单，订单号: {}", orderNo);
        OrderDTO order = orderService.getOrderByOrderNo(orderNo);
        return order != null ? Result.success(order) : Result.error("订单不存在");
    }

}