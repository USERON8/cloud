package com.cloud.api.order;

import com.cloud.common.domain.dto.order.OrderCreateDTO;
import com.cloud.common.domain.dto.order.OrderDTO;
import com.cloud.common.domain.vo.order.OrderVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单服务Feign客户端接口
 * 提供订单服务对外提供的Feign接口
 * 直接返回业务对象，仅用于服务内部调用
 *
 * @author cloud
 */
@FeignClient(name = "order-service", path = "/internal/order", contextId = "orderFeignClient")
public interface OrderFeignClient {

    /**
     * 根据订单ID查询订单信息
     *
     * @param orderId 订单ID
     * @return 订单信息，不存在时返回null
     */
    @GetMapping("/{orderId}")
    OrderVO getOrderById(@PathVariable("orderId") Long orderId);

    /**
     * 创建订单
     *
     * @param orderCreateDTO 订单创建信息
     * @return 创建的订单信息
     */
    @PostMapping("/create")
    OrderDTO createOrder(@RequestBody OrderCreateDTO orderCreateDTO);

    /**
     * 更新订单信息
     *
     * @param orderId  订单ID
     * @param orderDTO 订单信息
     * @return 是否更新成功
     */
    @PutMapping("/{orderId}")
    Boolean updateOrder(@PathVariable("orderId") Long orderId, @RequestBody OrderDTO orderDTO);

    /**
     * 删除订单
     *
     * @param orderId 订单ID
     * @return 是否删除成功
     */
    @DeleteMapping("/{orderId}")
    Boolean deleteOrder(@PathVariable("orderId") Long orderId);

    /**
     * 更新订单状态
     *
     * @param orderId 订单ID
     * @param status  订单状态
     * @return 是否更新成功
     */
    @PostMapping("/{orderId}/status/{status}")
    Boolean updateOrderStatus(@PathVariable("orderId") Long orderId,
                              @PathVariable("status") Integer status);

    /**
     * 支付订单
     *
     * @param orderId 订单ID
     * @return 是否支付成功
     */
    @PostMapping("/{orderId}/pay")
    Boolean payOrder(@PathVariable("orderId") Long orderId);

    /**
     * 发货订单
     *
     * @param orderId 订单ID
     * @return 是否发货成功
     */
    @PostMapping("/{orderId}/ship")
    Boolean shipOrder(@PathVariable("orderId") Long orderId);

    /**
     * 完成订单
     *
     * @param orderId 订单ID
     * @return 是否完成成功
     */
    @PostMapping("/{orderId}/complete")
    Boolean completeOrder(@PathVariable("orderId") Long orderId);

    /**
     * 取消订单
     *
     * @param orderId      订单ID
     * @param cancelReason 取消原因
     * @return 是否取消成功
     */
    @PostMapping("/{orderId}/cancel")
    Boolean cancelOrder(@PathVariable("orderId") Long orderId,
                        @RequestParam(required = false) String cancelReason);

    /**
     * 根据用户ID查询订单列表
     *
     * @param userId 用户ID
     * @return 订单列表，无数据时返回空列表
     */
    @GetMapping("/user/{userId}")
    List<OrderDTO> getOrdersByUserId(@PathVariable("userId") Long userId);

    /**
     * 检查订单是否已支付
     *
     * @param orderId 订单ID
     * @return 是否已支付
     */
    @GetMapping("/{orderId}/paid-status")
    Boolean isOrderPaid(@PathVariable("orderId") Long orderId);

    // ==================== 批量操作接口 ====================

    /**
     * 批量删除订单
     *
     * @param orderIds 订单ID列表
     * @return 成功删除的数量
     */
    @DeleteMapping("/batch")
    Integer deleteOrdersBatch(@RequestBody List<Long> orderIds);

    /**
     * 批量取消订单
     *
     * @param orderIds     订单ID列表
     * @param cancelReason 取消原因
     * @return 成功取消的数量
     */
    @PostMapping("/batch/cancel")
    Integer cancelOrdersBatch(@RequestBody List<Long> orderIds,
                              @RequestParam(required = false) String cancelReason);

    /**
     * 批量支付订单
     *
     * @param orderIds 订单ID列表
     * @return 成功支付的数量
     */
    @PostMapping("/batch/pay")
    Integer payOrdersBatch(@RequestBody List<Long> orderIds);

    /**
     * 批量发货订单
     *
     * @param orderIds 订单ID列表
     * @return 成功发货的数量
     */
    @PostMapping("/batch/ship")
    Integer shipOrdersBatch(@RequestBody List<Long> orderIds);

    /**
     * 批量完成订单
     *
     * @param orderIds 订单ID列表
     * @return 成功完成的数量
     */
    @PostMapping("/batch/complete")
    Integer completeOrdersBatch(@RequestBody List<Long> orderIds);
}