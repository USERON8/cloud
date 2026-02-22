package com.cloud.order.service;

import com.cloud.common.domain.dto.order.OrderDTO;
import com.cloud.order.dto.OrderCreateRequestDTO;
import com.cloud.order.enums.OrderStatusEnum;
import com.cloud.order.exception.OrderBusinessException;








public interface OrderBusinessService {

    














    Long createOrder(OrderCreateRequestDTO createRequest, Long operatorId);

    













    boolean handlePaymentSuccess(Long orderId, Long paymentId, java.math.BigDecimal paymentAmount);

    














    boolean cancelOrder(Long orderId, String cancelReason, Long operatorId);

    












    boolean shipOrder(Long orderId, Long operatorId);

    













    boolean completeOrder(Long orderId, Long operatorId);

    





    OrderStatusEnum checkOrderStatus(Long orderId);

    





    OrderDTO getOrderWithLock(Long orderId);

    













    boolean handlePaymentFailed(Long orderId, Long paymentId, String failReason);

    











    boolean handleStockShortage(Long orderId, java.util.List<Long> productIds);
}
