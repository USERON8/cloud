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





@Slf4j
@RestController
@RequestMapping("/internal/order")
@RequiredArgsConstructor
public class OrderFeignController implements OrderFeignClient {

    private final OrderService orderService;

    





    @Override
    @GetMapping("/{orderId}")
    public OrderVO getOrderById(@PathVariable("orderId") Long orderId) {
        
        try {
            OrderVO orderVO = orderService.getOrderByOrderIdForFeign(orderId);
            
            return orderVO;
        } catch (Exception e) {
            log.error("[Feign ID: orderId={}", orderId, e);
            return null;
        }
    }

    





    @Override
    @PostMapping("/create")
    public OrderDTO createOrder(@RequestBody OrderCreateDTO orderCreateDTO) {
        
        try {
            OrderDTO orderDTO = orderService.createOrder(orderCreateDTO);
            
            return orderDTO;
        } catch (Exception e) {
            log.error("[Feign : userId={}", orderCreateDTO.getUserId(), e);
            return null;
        }
    }

    






    @Override
    @PutMapping("/{orderId}")
    public Boolean updateOrder(@PathVariable("orderId") Long orderId, @RequestBody OrderDTO orderDTO) {
        
        try {
            orderDTO.setId(orderId);
            return orderService.updateOrder(orderDTO);
        } catch (Exception e) {
            log.error("[Feign : orderId={}", orderId, e);
            return false;
        }
    }

    





    @Override
    @DeleteMapping("/{orderId}")
    public Boolean deleteOrder(@PathVariable("orderId") Long orderId) {
        
        try {
            return orderService.deleteOrder(orderId);
        } catch (Exception e) {
            log.error("[Feign : orderId={}", orderId, e);
            return false;
        }
    }

    






    @Override
    @PostMapping("/{orderId}/status/{status}")
    public Boolean updateOrderStatus(@PathVariable("orderId") Long orderId, @PathVariable("status") Integer status) {
        
        try {
            return orderService.updateOrderStatusForFeign(orderId, status);
        } catch (Exception e) {
            log.error("[Feign ? orderId={}, status={}", orderId, status, e);
            return false;
        }
    }

    





    @Override
    @PostMapping("/{orderId}/pay")
    public Boolean payOrder(@PathVariable("orderId") Long orderId) {
        
        try {
            return orderService.payOrder(orderId);
        } catch (Exception e) {
            log.error("[Feign : orderId={}", orderId, e);
            return false;
        }
    }

    





    @Override
    @PostMapping("/{orderId}/ship")
    public Boolean shipOrder(@PathVariable("orderId") Long orderId) {
        
        try {
            return orderService.shipOrder(orderId);
        } catch (Exception e) {
            log.error("[Feign : orderId={}", orderId, e);
            return false;
        }
    }

    





    @Override
    @PostMapping("/{orderId}/complete")
    public Boolean completeOrder(@PathVariable("orderId") Long orderId) {
        
        try {
            return orderService.completeOrderForFeign(orderId);
        } catch (Exception e) {
            log.error("[Feign : orderId={}", orderId, e);
            return false;
        }
    }

    






    @Override
    @PostMapping("/{orderId}/cancel")
    public Boolean cancelOrder(@PathVariable("orderId") Long orderId, @RequestParam(required = false) String cancelReason) {
        
        try {
            return orderService.cancelOrderWithReason(orderId, cancelReason);
        } catch (Exception e) {
            log.error("[Feign : orderId={}", orderId, e);
            return false;
        }
    }

    





    @Override
    @GetMapping("/user/{userId}")
    public List<OrderDTO> getOrdersByUserId(@PathVariable("userId") Long userId) {
        
        try {
            return orderService.getOrdersByUserId(userId);
        } catch (Exception e) {
            log.error("[Feign : userId={}", userId, e);
            return List.of();
        }
    }

    





    @Override
    @GetMapping("/{orderId}/paid-status")
    public Boolean isOrderPaid(@PathVariable("orderId") Long orderId) {
        
        try {
            return orderService.isOrderPaid(orderId);
        } catch (Exception e) {
            log.error("[Feign ? orderId={}", orderId, e);
            return false;
        }
    }

    

    





    @Override
    @DeleteMapping("/batch")
    public Integer deleteOrdersBatch(@RequestBody List<Long> orderIds) {
        
        try {
            int successCount = 0;
            for (Long orderId : orderIds) {
                if (orderService.deleteOrder(orderId)) {
                    successCount++;
                }
            }
            
            return successCount;
        } catch (Exception e) {
            log.error("[Feign : orderIds={}", orderIds, e);
            return 0;
        }
    }

    






    @Override
    @PostMapping("/batch/cancel")
    public Integer cancelOrdersBatch(@RequestBody List<Long> orderIds, @RequestParam(required = false) String cancelReason) {
        
        try {
            int successCount = 0;
            for (Long orderId : orderIds) {
                if (orderService.cancelOrderWithReason(orderId, cancelReason)) {
                    successCount++;
                }
            }
            
            return successCount;
        } catch (Exception e) {
            log.error("[Feign : orderIds={}", orderIds, e);
            return 0;
        }
    }

    





    @Override
    @PostMapping("/batch/pay")
    public Integer payOrdersBatch(@RequestBody List<Long> orderIds) {
        
        try {
            int successCount = 0;
            for (Long orderId : orderIds) {
                if (orderService.payOrder(orderId)) {
                    successCount++;
                }
            }
            
            return successCount;
        } catch (Exception e) {
            log.error("[Feign : orderIds={}", orderIds, e);
            return 0;
        }
    }

    





    @Override
    @PostMapping("/batch/ship")
    public Integer shipOrdersBatch(@RequestBody List<Long> orderIds) {
        
        try {
            int successCount = 0;
            for (Long orderId : orderIds) {
                if (orderService.shipOrder(orderId)) {
                    successCount++;
                }
            }
            
            return successCount;
        } catch (Exception e) {
            log.error("[Feign : orderIds={}", orderIds, e);
            return 0;
        }
    }

    





    @Override
    @PostMapping("/batch/complete")
    public Integer completeOrdersBatch(@RequestBody List<Long> orderIds) {
        
        try {
            int successCount = 0;
            for (Long orderId : orderIds) {
                if (orderService.completeOrderForFeign(orderId)) {
                    successCount++;
                }
            }
            
            return successCount;
        } catch (Exception e) {
            log.error("[Feign : orderIds={}", orderIds, e);
            return 0;
        }
    }
}
