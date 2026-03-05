package com.cloud.order.dto;

import com.cloud.order.entity.OrderItem;
import com.cloud.order.entity.OrderMain;
import com.cloud.order.entity.OrderSub;
import lombok.Data;

import java.util.List;

@Data
public class OrderAggregateResponse {
    private OrderMain mainOrder;
    private List<SubOrderWithItems> subOrders;

    @Data
    public static class SubOrderWithItems {
        private OrderSub subOrder;
        private List<OrderItem> items;
    }
}
