package com.cloud.order.service.support;

import com.cloud.common.domain.vo.payment.PaymentOrderVO;
import com.cloud.common.exception.BusinessException;
import com.cloud.order.entity.AfterSale;
import com.cloud.order.entity.OrderMain;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.mapper.OrderMainMapper;
import com.cloud.order.mapper.OrderSubMapper;
import io.seata.saga.engine.StateMachineEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OrderRefundSagaCoordinator {

    private final StateMachineEngine stateMachineEngine;
    private final PaymentOrderRemoteService paymentOrderRemoteService;
    private final OrderMainMapper orderMainMapper;
    private final OrderSubMapper orderSubMapper;

    public void startRefundSaga(AfterSale afterSale, String remark) {
        if (afterSale == null) {
            throw new BusinessException("after sale is required");
        }
        OrderMain mainOrder = requireMainOrder(afterSale.getMainOrderId());
        OrderSub subOrder = requireSubOrder(afterSale.getSubOrderId());

        PaymentOrderVO paymentOrder = paymentOrderRemoteService.getPaymentOrderByOrderNo(
                mainOrder.getMainOrderNo(), subOrder.getSubOrderNo()
        );
        if (paymentOrder == null) {
            throw new BusinessException("payment order not found for refund process");
        }

        Map<String, Object> params = new HashMap<>();
        params.put("afterSaleId", afterSale.getId());
        params.put("afterSaleNo", afterSale.getAfterSaleNo());
        params.put("previousStatus", afterSale.getStatus());
        params.put("refundNo", buildRefundNo(afterSale));
        params.put("paymentNo", paymentOrder.getPaymentNo());
        params.put("refundAmount", resolveRefundAmount(afterSale));
        params.put("reason", buildRefundReason(afterSale, remark));
        params.put("idempotencyKey", "after-sale:refund:" + afterSale.getAfterSaleNo());
        params.put("mainOrderId", afterSale.getMainOrderId());
        params.put("subOrderId", afterSale.getSubOrderId());
        params.put("mainOrderNo", mainOrder.getMainOrderNo());
        params.put("subOrderNo", subOrder.getSubOrderNo());
        params.put("userId", afterSale.getUserId());

        stateMachineEngine.startWithBusinessKey("refundSaga", afterSale.getAfterSaleNo(), "refundApply", params);
    }

    private OrderMain requireMainOrder(Long mainOrderId) {
        if (mainOrderId == null) {
            throw new BusinessException("main order id is required");
        }
        OrderMain mainOrder = orderMainMapper.selectById(mainOrderId);
        if (mainOrder == null || Integer.valueOf(1).equals(mainOrder.getDeleted())) {
            throw new BusinessException("main order not found");
        }
        return mainOrder;
    }

    private OrderSub requireSubOrder(Long subOrderId) {
        if (subOrderId == null) {
            throw new BusinessException("sub order id is required");
        }
        OrderSub subOrder = orderSubMapper.selectById(subOrderId);
        if (subOrder == null || Integer.valueOf(1).equals(subOrder.getDeleted())) {
            throw new BusinessException("sub order not found");
        }
        return subOrder;
    }

    private String buildRefundNo(AfterSale afterSale) {
        return "RF" + afterSale.getAfterSaleNo();
    }

    private BigDecimal resolveRefundAmount(AfterSale afterSale) {
        BigDecimal amount = null;
        if (afterSale.getApprovedAmount() != null) {
            amount = afterSale.getApprovedAmount();
        } else if (afterSale.getApplyAmount() != null) {
            amount = afterSale.getApplyAmount();
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("refund amount must be greater than 0");
        }
        return amount;
    }

    private String buildRefundReason(AfterSale afterSale, String remark) {
        if (remark != null && !remark.isBlank()) {
            return remark;
        }
        if (afterSale.getReason() != null && !afterSale.getReason().isBlank()) {
            return afterSale.getReason();
        }
        return "after-sale refund";
    }
}
