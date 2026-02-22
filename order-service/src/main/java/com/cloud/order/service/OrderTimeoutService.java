package com.cloud.order.service;

import com.cloud.order.module.entity.Order;

import java.util.List;






public interface OrderTimeoutService {
    





    int checkAndHandleTimeoutOrders();

    





    List<Order> getTimeoutOrders(Integer timeoutMinutes);

    





    boolean cancelTimeoutOrder(Long orderId);

    





    int batchCancelTimeoutOrders(List<Long> orderIds);

    




    Integer getTimeoutConfig();

    





    boolean updateTimeoutConfig(Integer timeoutMinutes);
}
