package com.cloud.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.common.domain.dto.payment.PaymentRefundCommandDTO;
import com.cloud.common.domain.vo.payment.PaymentOrderVO;
import com.cloud.common.exception.BusinessException;
import com.cloud.order.config.OrderAutomationProperties;
import com.cloud.order.entity.AfterSale;
import com.cloud.order.entity.OrderMain;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.mapper.AfterSaleMapper;
import com.cloud.order.mapper.OrderMainMapper;
import com.cloud.order.mapper.OrderSubMapper;
import com.cloud.order.service.OrderAutomationService;
import com.cloud.order.service.OrderService;
import com.cloud.order.service.support.PaymentOrderRemoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderAutomationServiceImpl implements OrderAutomationService {

    private static final String SUB_STATUS_SHIPPED = "SHIPPED";
    private static final List<String> AUTO_APPROVE_AFTER_SALE_STATUSES = List.of("APPLIED", "AUDITING", "APPROVED");

    private final OrderSubMapper orderSubMapper;
    private final OrderMainMapper orderMainMapper;
    private final AfterSaleMapper afterSaleMapper;
    private final OrderService orderService;
    private final PaymentOrderRemoteService paymentOrderRemoteService;
    private final OrderAutomationProperties properties;

    @Override
    public int autoConfirmShippedOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusHours(properties.getAutoConfirm().getAfterHours());
        List<OrderSub> subOrders = orderSubMapper.selectList(new LambdaQueryWrapper<OrderSub>()
                .eq(OrderSub::getOrderStatus, SUB_STATUS_SHIPPED)
                .eq(OrderSub::getDeleted, 0)
                .isNotNull(OrderSub::getShippedAt)
                .le(OrderSub::getShippedAt, deadline)
                .orderByAsc(OrderSub::getShippedAt)
                .last("LIMIT " + properties.getAutoConfirm().getBatchSize()));

        int handledCount = 0;
        for (OrderSub subOrder : subOrders) {
            try {
                orderService.advanceSubOrderStatus(subOrder.getId(), "RECEIVE");
                handledCount++;
            } catch (Exception ex) {
                log.warn("Auto confirm receipt skipped, subOrderId={}, reason={}", subOrder.getId(), ex.getMessage());
            }
        }
        return handledCount;
    }

    @Override
    public int autoApproveTimedOutAfterSales() {
        LocalDateTime deadline = LocalDateTime.now().minusHours(properties.getAfterSale().getAuditTimeoutHours());
        List<AfterSale> afterSales = afterSaleMapper.selectList(new LambdaQueryWrapper<AfterSale>()
                .in(AfterSale::getStatus, AUTO_APPROVE_AFTER_SALE_STATUSES)
                .eq(AfterSale::getDeleted, 0)
                .le(AfterSale::getCreatedAt, deadline)
                .orderByAsc(AfterSale::getCreatedAt)
                .last("LIMIT " + properties.getAfterSale().getBatchSize()));

        int handledCount = 0;
        for (AfterSale afterSale : afterSales) {
            if (processAfterSale(afterSale)) {
                handledCount++;
            }
        }
        return handledCount;
    }

    private boolean processAfterSale(AfterSale afterSale) {
        try {
            AfterSale current = afterSale;
            if ("APPLIED".equals(current.getStatus())) {
                current = orderService.advanceAfterSaleStatus(current.getId(), "AUDIT", "system timeout auto audit");
            }
            if ("AUDITING".equals(current.getStatus())) {
                current = orderService.advanceAfterSaleStatus(current.getId(), "APPROVE", "system timeout auto approve");
            }

            if (!"APPROVED".equals(current.getStatus())) {
                return false;
            }

            if ("RETURN_REFUND".equalsIgnoreCase(current.getAfterSaleType())) {
                orderService.advanceAfterSaleStatus(current.getId(), "WAIT_RETURN", "system timeout auto approve");
                return true;
            }

            triggerRefund(current);
            return true;
        } catch (Exception ex) {
            log.warn("Auto approve after-sale skipped, afterSaleId={}, reason={}", afterSale.getId(), ex.getMessage());
            return false;
        }
    }

    private void triggerRefund(AfterSale afterSale) {
        OrderMain mainOrder = requireOrderMain(afterSale.getMainOrderId());
        OrderSub subOrder = requireSubOrder(afterSale.getSubOrderId());
        PaymentOrderVO paymentOrder = paymentOrderRemoteService.getPaymentOrderByOrderNo(mainOrder.getMainOrderNo(), subOrder.getSubOrderNo());
        if (paymentOrder == null) {
            throw new BusinessException("payment order not found for auto refund");
        }

        PaymentRefundCommandDTO command = new PaymentRefundCommandDTO();
        command.setRefundNo(buildRefundNo(afterSale));
        command.setPaymentNo(paymentOrder.getPaymentNo());
        command.setAfterSaleNo(afterSale.getAfterSaleNo());
        command.setRefundAmount(resolveRefundAmount(afterSale));
        command.setReason(buildRefundReason(afterSale));
        command.setIdempotencyKey("after-sale:auto-refund:" + afterSale.getAfterSaleNo());
        paymentOrderRemoteService.createRefund(command);

        orderService.advanceAfterSaleStatus(afterSale.getId(), "PROCESS", "system timeout auto refund");
    }

    private OrderMain requireOrderMain(Long mainOrderId) {
        OrderMain mainOrder = orderMainMapper.selectById(mainOrderId);
        if (mainOrder == null || Integer.valueOf(1).equals(mainOrder.getDeleted())) {
            throw new BusinessException("main order not found for auto refund");
        }
        return mainOrder;
    }

    private OrderSub requireSubOrder(Long subOrderId) {
        OrderSub subOrder = orderSubMapper.selectById(subOrderId);
        if (subOrder == null || Integer.valueOf(1).equals(subOrder.getDeleted())) {
            throw new BusinessException("sub order not found for auto refund");
        }
        return subOrder;
    }

    private BigDecimal resolveRefundAmount(AfterSale afterSale) {
        if (afterSale.getApprovedAmount() != null && afterSale.getApprovedAmount().compareTo(BigDecimal.ZERO) > 0) {
            return afterSale.getApprovedAmount();
        }
        if (afterSale.getApplyAmount() != null) {
            return afterSale.getApplyAmount();
        }
        return BigDecimal.ZERO;
    }

    private String buildRefundNo(AfterSale afterSale) {
        return "RF" + afterSale.getAfterSaleNo();
    }

    private String buildRefundReason(AfterSale afterSale) {
        if (afterSale.getReason() != null && !afterSale.getReason().isBlank()) {
            return afterSale.getReason();
        }
        String type = afterSale.getAfterSaleType() == null ? "after-sale" : afterSale.getAfterSaleType().toLowerCase(Locale.ROOT);
        return "system auto refund for " + type;
    }
}
