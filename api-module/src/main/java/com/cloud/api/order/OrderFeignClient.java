package com.cloud.api.order;

import com.cloud.common.domain.dto.order.OrderDTO;
import com.cloud.common.domain.vo.OrderVO;
import com.cloud.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 订单服务Feign客户端接口
 * 提供订单服务对外提供的Feign接口
 */
@FeignClient(name = "order-service", path = "/order/feign")
public interface OrderFeignClient {

    /**
     * 根据订单ID查询订单信息
     *
     * @param orderId 订单ID
     * @return 订单信息
     */
    @GetMapping("/id/{orderId}")
    Result<OrderVO> getOrderByOrderId(@PathVariable("orderId") Long orderId);

    /**
     * 创建订单
     *
     * @param orderDTO 订单信息
     * @return 订单信息
     */
    @PostMapping("/create")
    Result<OrderVO> createOrder(@RequestBody OrderDTO orderDTO);

    /**
     * 更新订单状态
     *
     * @param orderId 订单ID
     * @param status  订单状态
     * @return 是否更新成功
     */
    @PostMapping("/update/status/{orderId}/{status}")
    Result<Boolean> updateOrderStatus(@PathVariable("orderId") Long orderId,
                                      @PathVariable("status") Integer status);

    /**
     * 完成订单
     *
     * @param orderId 订单ID
     * @return 是否更新成功
     */
    @PostMapping("/complete/{orderId}")
    Result<Boolean> completeOrder(@PathVariable("orderId") Long orderId);
}