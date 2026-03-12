package com.cloud.api.order;

import com.cloud.common.domain.vo.order.OrderSubStatusVO;

public interface OrderDubboApi {

    OrderSubStatusVO getSubOrderStatus(String mainOrderNo, String subOrderNo);
}
