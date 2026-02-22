package com.cloud.api.order;

import com.cloud.common.domain.dto.order.OrderCreateDTO;
import com.cloud.common.domain.dto.order.OrderDTO;
import com.cloud.common.domain.vo.order.OrderVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;








@FeignClient(name = "order-service", path = "/internal/order", contextId = "orderFeignClient")
public interface OrderFeignClient {

    





    @GetMapping("/{orderId}")
    OrderVO getOrderById(@PathVariable("orderId") Long orderId);

    





    @PostMapping("/create")
    OrderDTO createOrder(@RequestBody OrderCreateDTO orderCreateDTO);

    






    @PutMapping("/{orderId}")
    Boolean updateOrder(@PathVariable("orderId") Long orderId, @RequestBody OrderDTO orderDTO);

    





    @DeleteMapping("/{orderId}")
    Boolean deleteOrder(@PathVariable("orderId") Long orderId);

    






    @PostMapping("/{orderId}/status/{status}")
    Boolean updateOrderStatus(@PathVariable("orderId") Long orderId,
                              @PathVariable("status") Integer status);

    





    @PostMapping("/{orderId}/pay")
    Boolean payOrder(@PathVariable("orderId") Long orderId);

    





    @PostMapping("/{orderId}/ship")
    Boolean shipOrder(@PathVariable("orderId") Long orderId);

    





    @PostMapping("/{orderId}/complete")
    Boolean completeOrder(@PathVariable("orderId") Long orderId);

    






    @PostMapping("/{orderId}/cancel")
    Boolean cancelOrder(@PathVariable("orderId") Long orderId,
                        @RequestParam(required = false) String cancelReason);

    





    @GetMapping("/user/{userId}")
    List<OrderDTO> getOrdersByUserId(@PathVariable("userId") Long userId);

    





    @GetMapping("/{orderId}/paid-status")
    Boolean isOrderPaid(@PathVariable("orderId") Long orderId);

    

    





    @DeleteMapping("/batch")
    Integer deleteOrdersBatch(@RequestBody List<Long> orderIds);

    






    @PostMapping("/batch/cancel")
    Integer cancelOrdersBatch(@RequestBody List<Long> orderIds,
                              @RequestParam(required = false) String cancelReason);

    





    @PostMapping("/batch/pay")
    Integer payOrdersBatch(@RequestBody List<Long> orderIds);

    





    @PostMapping("/batch/ship")
    Integer shipOrdersBatch(@RequestBody List<Long> orderIds);

    





    @PostMapping("/batch/complete")
    Integer completeOrdersBatch(@RequestBody List<Long> orderIds);
}
