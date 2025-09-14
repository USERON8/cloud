package com.cloud.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.common.annotation.RequiresPermission;
import com.cloud.common.domain.dto.order.OrderDTO;
import com.cloud.order.dto.OrderPageQueryDTO;
import com.cloud.common.domain.vo.OrderVO;
import com.cloud.common.result.Result;
import com.cloud.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequestMapping("/orders")
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
    @RequiresPermission("ORDER_QUERY")
    public Result<Page<OrderVO>> queryOrdersByPage(
            @Parameter(description = "查询条件", required = true)
            @Valid @RequestBody OrderPageQueryDTO queryDTO) {
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
    @RequiresPermission("ORDER_QUERY")
    public Result<OrderDTO> getOrderById(
            @Parameter(description = "订单ID") @NotNull @PathVariable Long id) {
        return Result.success(orderService.getByOrderEntityId(id));
    }

}