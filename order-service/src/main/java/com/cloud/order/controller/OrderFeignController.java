package com.cloud.order.controller;

import com.cloud.api.order.OrderFeignClient;
import com.cloud.common.domain.dto.order.OrderDTO;
import com.cloud.common.domain.vo.OperationResultVO;
import com.cloud.common.domain.vo.order.OrderVO;
import com.cloud.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 订单服务Feign客户端接口实现控制器
 * 实现订单服务对外提供的Feign接口
 */
@Slf4j
@RestController
@RequestMapping("/internal/order")
@RequiredArgsConstructor
public class OrderFeignController implements OrderFeignClient {

    private final OrderService orderService;

    /**
     * 根据订单ID查询订单信息
     *
     * @param orderId 订单ID
     * @return 订单信息
     */
@Override
@GetMapping("/id/{orderId}")
public OrderVO getOrderByOrderId(@PathVariable("orderId") Long orderId) {
        log.info("[订单Feign控制器] 开始处理根据订单ID查询订单请求，订单ID: {}", orderId);
        try {
            OrderVO orderVO = orderService.getOrderByOrderIdForFeign(orderId);
            log.info("[订单Feign控制器] 根据订单ID查询订单成功，订单ID: {}", orderId);
            return orderVO;
        } catch (Exception e) {
            log.error("[订单Feign控制器] 根据订单ID查询订单异常，订单ID: {}", orderId, e);
            return null;
        }
    }

    /**
     * 创建订单
     *
     * @param orderDTO 订单信息
     * @return 订单信息
     */
@Override
@PostMapping("/create")
public OrderVO createOrder(@RequestBody OrderDTO orderDTO) {
        log.info("[订单Feign控制器] 开始处理创建订单请求，用户ID: {}", orderDTO.getUserId());
        try {
            OrderVO orderVO = orderService.createOrderForFeign(orderDTO);
            if (orderVO != null) {
                log.info("[订单Feign控制器] 创建订单成功，订单ID: {}", orderVO.getId());
                return orderVO;
            } else {
                log.error("[订单Feign控制器] 创建订单失败");
                return null;
            }
        } catch (Exception e) {
            log.error("[订单Feign控制器] 创建订单异常", e);
            return null;
        }
    }

    /**
     * 更新订单状态
     *
     * @param orderId 订单ID
     * @param status  订单状态
     * @return 操作结果
     */
@Override
@PostMapping("/update/status/{orderId}/{status}")
public OperationResultVO updateOrderStatus(@PathVariable("orderId") Long orderId, @PathVariable("status") Integer status) {
        log.info("[订单Feign控制器] 开始处理更新订单状态请求，订单ID: {}，状态: {}", orderId, status);
        try {
            boolean result = orderService.updateOrderStatusForFeign(orderId, status);
            if (result) {
                log.info("[订单Feign控制器] 更新订单状态成功，订单ID: {}，状态: {}", orderId, status);
                return OperationResultVO.success("更新订单状态成功");
            } else {
                log.warn("[订单Feign控制器] 更新订单状态失败，订单ID: {}，状态: {}", orderId, status);
                return OperationResultVO.failure("更新订单状态失败");
            }
        } catch (Exception e) {
            log.error("[订单Feign控制器] 更新订单状态异常，订单ID: {}，状态: {}", orderId, status, e);
            return OperationResultVO.failure("更新订单状态异常: " + e.getMessage());
        }
    }

    /**
     * 完成订单
     *
     * @param orderId 订单ID
     * @return 操作结果
     */
@Override
@PostMapping("/complete/{orderId}")
public OperationResultVO completeOrder(@PathVariable("orderId") Long orderId) {
        log.info("[订单Feign控制器] 开始处理完成订单请求，订单ID: {}", orderId);
        try {
            boolean result = orderService.completeOrderForFeign(orderId);
            if (result) {
                log.info("[订单Feign控制器] 完成订单成功，订单ID: {}", orderId);
                return OperationResultVO.success("完成订单成功");
            } else {
                log.warn("[订单Feign控制器] 完成订单失败，订单ID: {}", orderId);
                return OperationResultVO.failure("完成订单失败");
            }
        } catch (Exception e) {
            log.error("[订单Feign控制器] 完成订单异常，订单ID: {}", orderId, e);
            return OperationResultVO.failure("完成订单异常: " + e.getMessage());
        }
    }
}