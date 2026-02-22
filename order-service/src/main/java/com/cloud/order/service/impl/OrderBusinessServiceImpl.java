package com.cloud.order.service.impl;

import com.cloud.common.domain.dto.order.OrderDTO;
import com.cloud.order.converter.OrderConverter;
import com.cloud.order.dto.OrderCreateRequestDTO;
import com.cloud.order.enums.OrderStatusEnum;
import com.cloud.order.exception.OrderBusinessException;
import com.cloud.order.module.entity.Order;
import com.cloud.order.module.entity.OrderItem;
import com.cloud.order.service.OrderBusinessService;
import com.cloud.order.service.OrderItemService;
import com.cloud.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderBusinessServiceImpl implements OrderBusinessService {

    private final OrderService orderService;
    private final OrderItemService orderItemService;
    private final OrderConverter orderConverter;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = "order", key = "#result", condition = "#result != null")
    public Long createOrder(OrderCreateRequestDTO createRequest, Long operatorId) {
        validateCreateRequest(createRequest);

        Order order = buildOrder(createRequest);
        boolean orderSaved = orderService.save(order);
        if (!orderSaved || order.getId() == null) {
            throw new OrderBusinessException("Failed to persist order");
        }

        List<OrderItem> items = buildOrderItems(order.getId(), createRequest, operatorId);
        boolean itemsSaved = orderItemService.saveBatch(items);
        if (!itemsSaved) {
            throw new OrderBusinessException(order.getId(), "Failed to persist order items");
        }

        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = "order", key = "#orderId")
    public boolean handlePaymentSuccess(Long orderId, Long paymentId, BigDecimal paymentAmount) {
        Order order = getOrder(orderId);
        if (!order.canPay()) {
            throw new OrderBusinessException(orderId, "Order status does not allow payment");
        }

        order.setStatusEnum(OrderStatusEnum.PAID);
        order.setPayTime(LocalDateTime.now());
        if (paymentAmount != null && paymentAmount.compareTo(BigDecimal.ZERO) > 0) {
            order.setPayAmount(paymentAmount);
        }
        return orderService.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = "order", key = "#orderId")
    public boolean cancelOrder(Long orderId, String cancelReason, Long operatorId) {
        Order order = getOrder(orderId);
        if (!order.canCancel()) {
            throw new OrderBusinessException(orderId, "Order status does not allow cancellation");
        }

        order.setStatusEnum(OrderStatusEnum.CANCELLED);
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason(cancelReason == null || cancelReason.isBlank() ? "Cancelled by business flow" : cancelReason);
        return orderService.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = "order", key = "#orderId")
    public boolean shipOrder(Long orderId, Long operatorId) {
        Order order = getOrder(orderId);
        if (!order.canShip()) {
            throw new OrderBusinessException(orderId, "Order status does not allow shipping");
        }

        order.setStatusEnum(OrderStatusEnum.SHIPPED);
        order.setShipTime(LocalDateTime.now());
        return orderService.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = "order", key = "#orderId")
    public boolean completeOrder(Long orderId, Long operatorId) {
        Order order = getOrder(orderId);
        if (!order.canComplete()) {
            throw new OrderBusinessException(orderId, "Order status does not allow completion");
        }

        order.setStatusEnum(OrderStatusEnum.COMPLETED);
        order.setCompleteTime(LocalDateTime.now());
        return orderService.updateById(order);
    }

    @Override
    @Cacheable(cacheNames = "order", key = "'status:' + #orderId", unless = "#result == null")
    public OrderStatusEnum checkOrderStatus(Long orderId) {
        Order order = orderService.getById(orderId);
        return order == null ? null : order.getStatusEnum();
    }

    @Override
    @Cacheable(cacheNames = "order", key = "'detail:' + #orderId", unless = "#result == null")
    public OrderDTO getOrderWithLock(Long orderId) {
        Order order = orderService.getById(orderId);
        return order == null ? null : orderConverter.toDTO(order);
    }

    @Override
    public boolean handlePaymentFailed(Long orderId, Long paymentId, String failReason) {
        Order order = orderService.getById(orderId);
        if (order == null) {
            return false;
        }
        log.warn("Payment failed for order: orderId={}, paymentId={}, reason={}", orderId, paymentId, failReason);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handleStockShortage(Long orderId, List<Long> productIds) {
        String reason = "Insufficient stock for products: " + productIds;
        return cancelOrder(orderId, reason, 0L);
    }

    private void validateCreateRequest(OrderCreateRequestDTO request) {
        if (request == null) {
            throw new OrderBusinessException("Create request is required");
        }
        if (request.getUserId() == null || request.getUserId() <= 0) {
            throw new OrderBusinessException("userId is invalid");
        }
        if (request.getAddressId() == null || request.getAddressId() <= 0) {
            throw new OrderBusinessException("addressId is invalid");
        }
        if (request.getOrderItems() == null || request.getOrderItems().isEmpty()) {
            throw new OrderBusinessException("orderItems cannot be empty");
        }
        if (request.getTotalAmount() == null || request.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderBusinessException("totalAmount must be greater than 0");
        }
        if (request.getPayAmount() == null || request.getPayAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderBusinessException("payAmount must be greater than 0");
        }

        for (OrderCreateRequestDTO.OrderItemCreateDTO item : request.getOrderItems()) {
            if (item.getProductId() == null || item.getProductId() <= 0) {
                throw new OrderBusinessException("productId is invalid");
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new OrderBusinessException("quantity must be greater than 0");
            }
            if (item.getPrice() == null || item.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new OrderBusinessException("price must be greater than 0");
            }
        }
    }

    private Order buildOrder(OrderCreateRequestDTO request) {
        Order order = new Order();
        order.setOrderNo(Order.generateOrderNo());
        order.setUserId(request.getUserId());
        order.setTotalAmount(request.getTotalAmount());
        order.setPayAmount(request.getPayAmount());
        order.setStatusEnum(OrderStatusEnum.PENDING_PAYMENT);
        order.setAddressId(request.getAddressId());
        order.setRemark(request.getRemark());
        return order;
    }

    private List<OrderItem> buildOrderItems(Long orderId, OrderCreateRequestDTO request, Long operatorId) {
        List<OrderItem> items = new ArrayList<>();
        for (OrderCreateRequestDTO.OrderItemCreateDTO itemDto : request.getOrderItems()) {
            OrderItem item = new OrderItem();
            item.setOrderId(orderId);
            item.setProductId(itemDto.getProductId());
            item.setQuantity(itemDto.getQuantity());
            item.setPrice(itemDto.getPrice());
            item.setProductSnapshot(buildProductSnapshot(itemDto));
            item.setCreateBy(operatorId);
            item.setUpdateBy(operatorId);
            items.add(item);
        }
        return items;
    }

    private String buildProductSnapshot(OrderCreateRequestDTO.OrderItemCreateDTO itemDto) {
        String productName = safeJson(itemDto.getProductName());
        String specification = safeJson(itemDto.getSpecification());
        String imageUrl = safeJson(itemDto.getImageUrl());
        return String.format(
                "{\"productId\":%d,\"productName\":\"%s\",\"price\":%s,\"quantity\":%d,\"specification\":\"%s\",\"imageUrl\":\"%s\",\"snapshotTime\":\"%s\"}",
                itemDto.getProductId(),
                productName,
                itemDto.getPrice(),
                itemDto.getQuantity(),
                specification,
                imageUrl,
                LocalDateTime.now()
        );
    }

    private String safeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private Order getOrder(Long orderId) {
        Order order = orderService.getById(orderId);
        if (order == null) {
            throw new OrderBusinessException(orderId, "Order not found");
        }
        return order;
    }
}
