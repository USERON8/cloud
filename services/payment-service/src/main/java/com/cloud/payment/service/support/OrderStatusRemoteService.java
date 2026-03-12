package com.cloud.payment.service.support;

import com.cloud.api.order.OrderDubboApi;
import com.cloud.common.domain.vo.order.OrderSubStatusVO;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderStatusRemoteService {

    @DubboReference(check = false, timeout = 5000, retries = 0)
    private OrderDubboApi orderDubboApi;

    public OrderSubStatusVO getSubOrderStatus(String mainOrderNo, String subOrderNo) {
        return orderDubboApi.getSubOrderStatus(mainOrderNo, subOrderNo);
    }
}
