package com.cloud.order.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cloud.common.domain.dto.order.OrderCreateDTO;
import com.cloud.common.domain.dto.order.OrderDTO;
import com.cloud.common.domain.vo.order.OrderVO;
import com.cloud.order.dto.OrderPageQueryDTO;
import com.cloud.order.module.entity.Order;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;








public interface OrderService extends IService<Order> {

    Page<OrderVO> pageQuery(OrderPageQueryDTO queryDTO);

    OrderDTO getByOrderEntityId(Long id);

    Boolean updateOrder(@Valid OrderDTO orderDTO);

    Boolean saveOrder(@Valid OrderDTO orderDTO);

    





    Boolean payOrder(Long orderId);

    





    Boolean shipOrder(Long orderId);

    





    Boolean completeOrder(Long orderId);

    





    Boolean cancelOrder(Long orderId);

    Boolean cancelOrderWithReason(Long orderId, String cancelReason);

    






    Boolean createOrder(OrderCreateDTO orderCreateDTO, String currentUserId);

    






    Boolean payOrder(Long orderId, String currentUserId);

    






    Boolean shipOrder(Long orderId, String currentUserId);

    






    Boolean completeOrder(Long orderId, String currentUserId);

    






    Boolean cancelOrder(Long orderId, String currentUserId);

    Boolean deleteOrder(@NotNull Long id);

    





    OrderDTO createOrder(OrderDTO orderDTO);

    





    OrderDTO createOrder(OrderCreateDTO orderCreateDTO);

    





    List<OrderDTO> getOrdersByUserId(Long userId);

    





    OrderDTO getOrderByOrderNo(String orderNo);

    





    Boolean isOrderPaid(Long orderId);


    

    





    OrderVO getOrderByOrderIdForFeign(Long orderId);

    





    OrderVO createOrderForFeign(OrderDTO orderDTO);

    






    Boolean updateOrderStatusForFeign(Long orderId, Integer status);

    





    Boolean completeOrderForFeign(Long orderId);

    

    






    Integer batchUpdateOrderStatus(List<Long> orderIds, Integer status);

    





    Integer batchDeleteOrders(List<Long> orderIds);

    

    







    Boolean updateOrderStatusAfterPayment(Long orderId, Long paymentId, String transactionNo);

    






    Boolean cancelOrderDueToStockFreezeFailed(Long orderId, String reason);
}
