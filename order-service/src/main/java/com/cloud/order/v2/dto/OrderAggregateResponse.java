package com.cloud.order.v2.dto;

import com.cloud.order.v2.entity.OrderItemV2;
import com.cloud.order.v2.entity.OrderMainV2;
import com.cloud.order.v2.entity.OrderSubV2;
import lombok.Data;

import java.util.List;

@Data
public class OrderAggregateResponse {
    private OrderMainV2 mainOrder;
    private List<SubOrderWithItems> subOrders;

    @Data
    public static class SubOrderWithItems {
        private OrderSubV2 subOrder;
        private List<OrderItemV2> items;
    }
}
