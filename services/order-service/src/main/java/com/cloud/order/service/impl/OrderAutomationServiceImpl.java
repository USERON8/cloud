package com.cloud.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.order.config.OrderAutomationProperties;
import com.cloud.order.entity.AfterSale;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.mapper.AfterSaleMapper;
import com.cloud.order.mapper.OrderSubMapper;
import com.cloud.order.service.OrderAutomationService;
import com.cloud.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderAutomationServiceImpl implements OrderAutomationService {

    private static final String SUB_STATUS_SHIPPED = "SHIPPED";
    private static final List<String> AUTO_APPROVE_AFTER_SALE_STATUSES = List.of("APPLIED", "AUDITING", "APPROVED");

    private final OrderSubMapper orderSubMapper;
    private final AfterSaleMapper afterSaleMapper;
    private final OrderService orderService;
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
        orderService.advanceAfterSaleStatus(afterSale.getId(), "PROCESS", "system timeout auto refund");
    }

}
