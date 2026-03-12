package com.cloud.order.rpc;

import com.cloud.api.order.OrderDubboApi;
import com.cloud.common.domain.vo.order.OrderSubStatusVO;
import com.cloud.order.entity.OrderMain;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.mapper.OrderMainMapper;
import com.cloud.order.mapper.OrderSubMapper;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.util.StringUtils;

@DubboService(interfaceClass = OrderDubboApi.class, timeout = 5000, retries = 0)
@RequiredArgsConstructor
public class OrderDubboService implements OrderDubboApi {

    private final OrderMainMapper orderMainMapper;
    private final OrderSubMapper orderSubMapper;

    @Override
    public OrderSubStatusVO getSubOrderStatus(String mainOrderNo, String subOrderNo) {
        if (!StringUtils.hasText(mainOrderNo) || !StringUtils.hasText(subOrderNo)) {
            return null;
        }
        OrderMain mainOrder = orderMainMapper.selectActiveByOrderNo(mainOrderNo.trim());
        if (mainOrder == null) {
            return null;
        }
        OrderSub subOrder = orderSubMapper.selectActiveByMainOrderIdAndSubOrderNo(mainOrder.getId(), subOrderNo.trim());
        if (subOrder == null) {
            return null;
        }

        OrderSubStatusVO vo = new OrderSubStatusVO();
        vo.setMainOrderId(mainOrder.getId());
        vo.setSubOrderId(subOrder.getId());
        vo.setMainOrderNo(mainOrder.getMainOrderNo());
        vo.setSubOrderNo(subOrder.getSubOrderNo());
        vo.setOrderStatus(subOrder.getOrderStatus());
        vo.setUserId(mainOrder.getUserId());
        return vo;
    }
}
