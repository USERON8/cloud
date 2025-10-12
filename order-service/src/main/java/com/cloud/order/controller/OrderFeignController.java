package com.cloud.order.controller;

import com.cloud.api.order.OrderFeignClient;
import com.cloud.common.domain.dto.order.OrderCreateDTO;
import com.cloud.common.domain.dto.order.OrderDTO;
import com.cloud.common.domain.vo.order.OrderVO;
import com.cloud.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    @GetMapping("/{orderId}")
    public OrderVO getOrderById(@PathVariable("orderId") Long orderId) {
        log.info("[订单Feign控制器] 开始处理根据订单ID查询订单请求: orderId={}", orderId);
        try {
            OrderVO orderVO = orderService.getOrderByOrderIdForFeign(orderId);
            log.info("[订单Feign控制器] 根据订单ID查询订单成功: orderId={}", orderId);
            return orderVO;
        } catch (Exception e) {
            log.error("[订单Feign控制器] 根据订单ID查询订单失败: orderId={}", orderId, e);
            return null;
        }
    }

    /**
     * 创建订单
     *
     * @param orderCreateDTO 订单创建信息
     * @return 创建的订单信息
     */
    @Override
    @PostMapping("/create")
    public OrderDTO createOrder(@RequestBody OrderCreateDTO orderCreateDTO) {
        log.info("[订单Feign控制器] 开始处理创建订单请求: userId={}", orderCreateDTO.getUserId());
        try {
            OrderDTO orderDTO = orderService.createOrder(orderCreateDTO);
            log.info("[订单Feign控制器] 创建订单成功: orderId={}", orderDTO.getId());
            return orderDTO;
        } catch (Exception e) {
            log.error("[订单Feign控制器] 创建订单失败: userId={}", orderCreateDTO.getUserId(), e);
            return null;
        }
    }

    /**
     * 更新订单信息
     *
     * @param orderId 订单ID
     * @param orderDTO 订单信息
     * @return 是否更新成功
     */
    @Override
    @PutMapping("/{orderId}")
    public Boolean updateOrder(@PathVariable("orderId") Long orderId, @RequestBody OrderDTO orderDTO) {
        log.info("[订单Feign控制器] 开始处理更新订单请求: orderId={}", orderId);
        try {
            orderDTO.setId(orderId);
            return orderService.updateOrder(orderDTO);
        } catch (Exception e) {
            log.error("[订单Feign控制器] 更新订单失败: orderId={}", orderId, e);
            return false;
        }
    }

    /**
     * 删除订单
     *
     * @param orderId 订单ID
     * @return 是否删除成功
     */
    @Override
    @DeleteMapping("/{orderId}")
    public Boolean deleteOrder(@PathVariable("orderId") Long orderId) {
        log.info("[订单Feign控制器] 开始处理删除订单请求: orderId={}", orderId);
        try {
            return orderService.deleteOrder(orderId);
        } catch (Exception e) {
            log.error("[订单Feign控制器] 删除订单失败: orderId={}", orderId, e);
            return false;
        }
    }

    /**
     * 更新订单状态
     *
     * @param orderId 订单ID
     * @param status  订单状态
     * @return 是否更新成功
     */
    @Override
    @PostMapping("/{orderId}/status/{status}")
    public Boolean updateOrderStatus(@PathVariable("orderId") Long orderId, @PathVariable("status") Integer status) {
        log.info("[订单Feign控制器] 开始处理更新订单状态请求: orderId={}, status={}", orderId, status);
        try {
            return orderService.updateOrderStatusForFeign(orderId, status);
        } catch (Exception e) {
            log.error("[订单Feign控制器] 更新订单状态失败: orderId={}, status={}", orderId, status, e);
            return false;
        }
    }

    /**
     * 支付订单
     *
     * @param orderId 订单ID
     * @return 是否支付成功
     */
    @Override
    @PostMapping("/{orderId}/pay")
    public Boolean payOrder(@PathVariable("orderId") Long orderId) {
        log.info("[订单Feign控制器] 开始处理支付订单请求: orderId={}", orderId);
        try {
            return orderService.payOrder(orderId);
        } catch (Exception e) {
            log.error("[订单Feign控制器] 支付订单失败: orderId={}", orderId, e);
            return false;
        }
    }

    /**
     * 发货订单
     *
     * @param orderId 订单ID
     * @return 是否发货成功
     */
    @Override
    @PostMapping("/{orderId}/ship")
    public Boolean shipOrder(@PathVariable("orderId") Long orderId) {
        log.info("[订单Feign控制器] 开始处理发货订单请求: orderId={}", orderId);
        try {
            return orderService.shipOrder(orderId);
        } catch (Exception e) {
            log.error("[订单Feign控制器] 发货订单失败: orderId={}", orderId, e);
            return false;
        }
    }

    /**
     * 完成订单
     *
     * @param orderId 订单ID
     * @return 是否完成成功
     */
    @Override
    @PostMapping("/{orderId}/complete")
    public Boolean completeOrder(@PathVariable("orderId") Long orderId) {
        log.info("[订单Feign控制器] 开始处理完成订单请求: orderId={}", orderId);
        try {
            return orderService.completeOrderForFeign(orderId);
        } catch (Exception e) {
            log.error("[订单Feign控制器] 完成订单失败: orderId={}", orderId, e);
            return false;
        }
    }

    /**
     * 取消订单
     *
     * @param orderId 订单ID
     * @param cancelReason 取消原因
     * @return 是否取消成功
     */
    @Override
    @PostMapping("/{orderId}/cancel")
    public Boolean cancelOrder(@PathVariable("orderId") Long orderId, @RequestParam(required = false) String cancelReason) {
        log.info("[订单Feign控制器] 开始处理取消订单请求: orderId={}, reason={}", orderId, cancelReason);
        try {
            return orderService.cancelOrder(orderId);
        } catch (Exception e) {
            log.error("[订单Feign控制器] 取消订单失败: orderId={}", orderId, e);
            return false;
        }
    }

    /**
     * 根据用户ID查询订单列表
     *
     * @param userId 用户ID
     * @return 订单列表
     */
    @Override
    @GetMapping("/user/{userId}")
    public List<OrderDTO> getOrdersByUserId(@PathVariable("userId") Long userId) {
        log.info("[订单Feign控制器] 开始处理查询用户订单列表请求: userId={}", userId);
        try {
            return orderService.getOrdersByUserId(userId);
        } catch (Exception e) {
            log.error("[订单Feign控制器] 查询用户订单列表失败: userId={}", userId, e);
            return List.of();
        }
    }

    /**
     * 检查订单是否已支付
     *
     * @param orderId 订单ID
     * @return 是否已支付
     */
    @Override
    @GetMapping("/{orderId}/paid-status")
    public Boolean isOrderPaid(@PathVariable("orderId") Long orderId) {
        log.info("[订单Feign控制器] 开始处理检查订单支付状态请求: orderId={}", orderId);
        try {
            return orderService.isOrderPaid(orderId);
        } catch (Exception e) {
            log.error("[订单Feign控制器] 检查订单支付状态失败: orderId={}", orderId, e);
            return false;
        }
    }

    // ==================== 批量操作接口 ====================

    /**
     * 批量删除订单
     *
     * @param orderIds 订单ID列表
     * @return 成功删除的数量
     */
    @Override
    @DeleteMapping("/batch")
    public Integer deleteOrdersBatch(@RequestBody List<Long> orderIds) {
        log.info("[订单Feign控制器] 开始处理批量删除订单请求: orderIds={}", orderIds);
        try {
            int successCount = 0;
            for (Long orderId : orderIds) {
                if (orderService.deleteOrder(orderId)) {
                    successCount++;
                }
            }
            log.info("[订单Feign控制器] 批量删除订单成功: 成功删除数量={}", successCount);
            return successCount;
        } catch (Exception e) {
            log.error("[订单Feign控制器] 批量删除订单失败: orderIds={}", orderIds, e);
            return 0;
        }
    }

    /**
     * 批量取消订单
     *
     * @param orderIds 订单ID列表
     * @param cancelReason 取消原因
     * @return 成功取消的数量
     */
    @Override
    @PostMapping("/batch/cancel")
    public Integer cancelOrdersBatch(@RequestBody List<Long> orderIds, @RequestParam(required = false) String cancelReason) {
        log.info("[订单Feign控制器] 开始处理批量取消订单请求: orderIds={}, reason={}", orderIds, cancelReason);
        try {
            int successCount = 0;
            for (Long orderId : orderIds) {
                if (orderService.cancelOrder(orderId)) {
                    successCount++;
                }
            }
            log.info("[订单Feign控制器] 批量取消订单成功: 成功取消数量={}", successCount);
            return successCount;
        } catch (Exception e) {
            log.error("[订单Feign控制器] 批量取消订单失败: orderIds={}", orderIds, e);
            return 0;
        }
    }

    /**
     * 批量支付订单
     *
     * @param orderIds 订单ID列表
     * @return 成功支付的数量
     */
    @Override
    @PostMapping("/batch/pay")
    public Integer payOrdersBatch(@RequestBody List<Long> orderIds) {
        log.info("[订单Feign控制器] 开始处理批量支付订单请求: orderIds={}", orderIds);
        try {
            int successCount = 0;
            for (Long orderId : orderIds) {
                if (orderService.payOrder(orderId)) {
                    successCount++;
                }
            }
            log.info("[订单Feign控制器] 批量支付订单成功: 成功支付数量={}", successCount);
            return successCount;
        } catch (Exception e) {
            log.error("[订单Feign控制器] 批量支付订单失败: orderIds={}", orderIds, e);
            return 0;
        }
    }

    /**
     * 批量发货订单
     *
     * @param orderIds 订单ID列表
     * @return 成功发货的数量
     */
    @Override
    @PostMapping("/batch/ship")
    public Integer shipOrdersBatch(@RequestBody List<Long> orderIds) {
        log.info("[订单Feign控制器] 开始处理批量发货订单请求: orderIds={}", orderIds);
        try {
            int successCount = 0;
            for (Long orderId : orderIds) {
                if (orderService.shipOrder(orderId)) {
                    successCount++;
                }
            }
            log.info("[订单Feign控制器] 批量发货订单成功: 成功发货数量={}", successCount);
            return successCount;
        } catch (Exception e) {
            log.error("[订单Feign控制器] 批量发货订单失败: orderIds={}", orderIds, e);
            return 0;
        }
    }

    /**
     * 批量完成订单
     *
     * @param orderIds 订单ID列表
     * @return 成功完成的数量
     */
    @Override
    @PostMapping("/batch/complete")
    public Integer completeOrdersBatch(@RequestBody List<Long> orderIds) {
        log.info("[订单Feign控制器] 开始处理批量完成订单请求: orderIds={}", orderIds);
        try {
            int successCount = 0;
            for (Long orderId : orderIds) {
                if (orderService.completeOrderForFeign(orderId)) {
                    successCount++;
                }
            }
            log.info("[订单Feign控制器] 批量完成订单成功: 成功完成数量={}", successCount);
            return successCount;
        } catch (Exception e) {
            log.error("[订单Feign控制器] 批量完成订单失败: orderIds={}", orderIds, e);
            return 0;
        }
    }
}