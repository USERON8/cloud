package com.cloud.order.service.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.common.domain.dto.payment.PaymentRefundCommandDTO;
import com.cloud.common.exception.BusinessException;
import com.cloud.order.entity.AfterSale;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.mapper.AfterSaleMapper;
import com.cloud.order.mapper.OrderSubMapper;
import com.cloud.order.service.support.OrderAggregateCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Component("orderRefundSagaService")
@RequiredArgsConstructor
public class OrderRefundSagaService {

    private final AfterSaleMapper afterSaleMapper;
    private final OrderSubMapper orderSubMapper;
    private final PaymentOrderRemoteService paymentOrderRemoteService;
    private final OrderAggregateCacheService orderAggregateCacheService;

    @Transactional(rollbackFor = Exception.class)
    public boolean applyRefund(Map<String, Object> params) {
        Long afterSaleId = asLong(params.get("afterSaleId"));
        String previousStatus = asText(params.get("previousStatus"));
        if (afterSaleId == null) {
            throw new BusinessException("after sale id is required");
        }
        AfterSale afterSale = afterSaleMapper.selectById(afterSaleId);
        if (afterSale == null || Integer.valueOf(1).equals(afterSale.getDeleted())) {
            throw new BusinessException("after sale not found");
        }
        if ("REFUNDING".equals(afterSale.getStatus())) {
            return true;
        }
        if (previousStatus != null && !previousStatus.equals(afterSale.getStatus())) {
            throw new BusinessException("after sale status mismatch for saga: " + afterSale.getStatus());
        }

        afterSale.setStatus("REFUNDING");
        afterSaleMapper.updateById(afterSale);
        syncSubOrderAfterSaleStatus(afterSale.getSubOrderId(), "REFUNDING");
        orderAggregateCacheService.evict(afterSale.getMainOrderId());
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean createRefund(Map<String, Object> params) {
        String refundNo = asText(params.get("refundNo"));
        String paymentNo = asText(params.get("paymentNo"));
        String afterSaleNo = asText(params.get("afterSaleNo"));
        BigDecimal refundAmount = asBigDecimal(params.get("refundAmount"));
        String reason = asText(params.get("reason"));
        String idempotencyKey = asText(params.get("idempotencyKey"));

        if (refundNo == null || paymentNo == null || afterSaleNo == null) {
            throw new BusinessException("refund parameters missing");
        }

        PaymentRefundCommandDTO command = new PaymentRefundCommandDTO();
        command.setRefundNo(refundNo);
        command.setPaymentNo(paymentNo);
        command.setAfterSaleNo(afterSaleNo);
        command.setRefundAmount(refundAmount == null ? BigDecimal.ZERO : refundAmount);
        command.setReason(reason);
        command.setIdempotencyKey(idempotencyKey);

        Long refundId = paymentOrderRemoteService.createRefund(command);
        if (refundId == null) {
            throw new BusinessException("refund request failed");
        }
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean cancelRefund(Map<String, Object> params) {
        Long afterSaleId = asLong(params.get("afterSaleId"));
        String previousStatus = asText(params.get("previousStatus"));
        if (afterSaleId == null) {
            return true;
        }
        AfterSale afterSale = afterSaleMapper.selectById(afterSaleId);
        if (afterSale == null || Integer.valueOf(1).equals(afterSale.getDeleted())) {
            return true;
        }

        if ("CLOSED".equals(afterSale.getStatus()) || "CANCELLED".equals(afterSale.getStatus())) {
            return true;
        }
        String targetStatus = previousStatus == null ? "CLOSED" : previousStatus;
        afterSale.setStatus(targetStatus);
        afterSaleMapper.updateById(afterSale);
        syncSubOrderAfterSaleStatus(afterSale.getSubOrderId(), targetStatus);
        orderAggregateCacheService.evict(afterSale.getMainOrderId());
        return true;
    }

    private void syncSubOrderAfterSaleStatus(Long subOrderId, String status) {
        if (subOrderId == null || status == null) {
            return;
        }
        OrderSub subOrder = orderSubMapper.selectById(subOrderId);
        if (subOrder == null || Integer.valueOf(1).equals(subOrder.getDeleted())) {
            return;
        }
        subOrder.setAfterSaleStatus(status);
        orderSubMapper.updateById(subOrder);
    }

    private String asText(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long asLong(Object value) {
        if (value instanceof Long v) {
            return v;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        return value == null ? null : Long.parseLong(String.valueOf(value));
    }

    private BigDecimal asBigDecimal(Object value) {
        if (value instanceof BigDecimal v) {
            return v;
        }
        if (value instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        if (value == null) {
            return null;
        }
        return new BigDecimal(String.valueOf(value));
    }
}
