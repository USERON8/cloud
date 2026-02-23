package com.cloud.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.common.annotation.DistributedLock;
import com.cloud.common.domain.dto.order.OrderCreateDTO;
import com.cloud.common.domain.dto.order.OrderDTO;
import com.cloud.common.domain.dto.order.OrderItemDTO;
import com.cloud.common.messaging.event.OrderCreatedEvent;
import com.cloud.common.domain.vo.order.OrderVO;
import com.cloud.common.exception.BusinessException;
import com.cloud.common.exception.EntityNotFoundException;
import com.cloud.order.converter.OrderConverter;
import com.cloud.order.dto.OrderPageQueryDTO;
import com.cloud.order.exception.OrderServiceException;
import com.cloud.order.mapper.OrderMapper;
import com.cloud.order.messaging.OrderMessageProducer;
import com.cloud.order.module.entity.Order;
import com.cloud.order.service.OrderService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private final OrderConverter orderConverter;
    private final OrderMessageProducer orderMessageProducer;
    private final MeterRegistry meterRegistry;

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    public Page<OrderVO> pageQuery(OrderPageQueryDTO queryDTO) {
        OrderPageQueryDTO safeQuery = queryDTO == null ? new OrderPageQueryDTO() : queryDTO;
        long current = safeQuery.getCurrent() == null || safeQuery.getCurrent() < 1 ? 1L : safeQuery.getCurrent();
        long size = safeQuery.getSize() == null || safeQuery.getSize() < 1 ? 20L : safeQuery.getSize();

        Page<Order> page = new Page<>(current, size);
        Page<Order> resultPage = this.baseMapper.pageQuery(page, safeQuery);

        List<OrderVO> records = resultPage.getRecords().stream()
                .map(orderConverter::toVO)
                .collect(Collectors.toList());

        Page<OrderVO> voPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @Cacheable(cacheNames = "orderCache", key = "#id", unless = "#result == null")
    public OrderDTO getByOrderEntityId(Long id) {
        Order order = this.baseMapper.selectById(id);
        if (order == null) {
            throw EntityNotFoundException.order(id);
        }
        return orderConverter.toDTO(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @PreAuthorize("@permissionManager.hasUserAccess(authentication)")
    public Boolean saveOrder(OrderDTO orderDTO) {
        if (orderDTO == null) {
            throw new OrderServiceException("orderDTO is required");
        }
        Order order = orderConverter.toEntity(orderDTO);
        initOrderForCreate(order);
        return this.baseMapper.insert(order) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @Caching(evict = {
            @CacheEvict(cacheNames = "orderCache", key = "#orderDTO.id"),
            @CacheEvict(cacheNames = "orderListCache", key = "'user:' + #orderDTO.userId"),
            @CacheEvict(cacheNames = "orderPageCache", allEntries = true)
    })
    public Boolean updateOrder(OrderDTO orderDTO) {
        if (orderDTO == null || orderDTO.getId() == null) {
            throw new OrderServiceException("order id is required");
        }

        Order existing = this.baseMapper.selectById(orderDTO.getId());
        if (existing == null) {
            throw EntityNotFoundException.order(orderDTO.getId());
        }

        Order order = orderConverter.toEntity(orderDTO);
        order.setUpdatedAt(LocalDateTime.now());
        return this.baseMapper.updateById(order) > 0;
    }

    @Override
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @DistributedLock(key = "'order:pay:' + #orderId", waitTime = 5, leaseTime = 15, failMessage = "Acquire order pay lock failed")
    @Transactional(rollbackFor = Exception.class)
    public Boolean payOrder(Long orderId) {
        Order order = requireOrder(orderId);
        if (!Objects.equals(order.getStatus(), 0)) {
            throw new OrderServiceException("Order status does not allow payment");
        }

        int rows = this.baseMapper.updateStatusToPaid(orderId);
        if (rows > 0) {
            return true;
        }

        Order latest = this.baseMapper.selectById(orderId);
        if (latest != null && Objects.equals(latest.getStatus(), 1)) {
            return true;
        }
        throw new OrderServiceException("Pay order failed due to status conflict");
    }

    @Override
    @PreAuthorize("@permissionChecker.checkPermission(authentication, 'order:manage') or @permissionManager.hasAdminAccess(authentication)")
    @DistributedLock(key = "'order:ship:' + #orderId", waitTime = 3, leaseTime = 10, failMessage = "Acquire order ship lock failed")
    @Transactional(rollbackFor = Exception.class)
    public Boolean shipOrder(Long orderId) {
        Order order = requireOrder(orderId);
        if (!Objects.equals(order.getStatus(), 1)) {
            throw new OrderServiceException("Order status does not allow shipping");
        }

        int rows = this.baseMapper.updateStatusToShipped(orderId);
        if (rows > 0) {
            return true;
        }

        Order latest = this.baseMapper.selectById(orderId);
        if (latest != null && Objects.equals(latest.getStatus(), 2)) {
            return true;
        }
        throw new OrderServiceException("Ship order failed due to status conflict");
    }

    @Override
    @DistributedLock(key = "'order:complete:' + #orderId", waitTime = 3, leaseTime = 10, failMessage = "Acquire order complete lock failed")
    @Transactional(rollbackFor = Exception.class)
    public Boolean completeOrder(Long orderId) {
        Order order = requireOrder(orderId);
        if (!Objects.equals(order.getStatus(), 2)) {
            throw new OrderServiceException("Order status does not allow completion");
        }

        int rows = this.baseMapper.updateStatusToCompleted(orderId);
        if (rows > 0) {
            return true;
        }

        Order latest = this.baseMapper.selectById(orderId);
        if (latest != null && Objects.equals(latest.getStatus(), 3)) {
            return true;
        }
        throw new OrderServiceException("Complete order failed due to status conflict");
    }

    @Override
    @DistributedLock(key = "'order:cancel:' + #orderId", waitTime = 5, leaseTime = 20, failMessage = "Acquire order cancel lock failed")
    @Transactional(rollbackFor = Exception.class)
    public Boolean cancelOrder(Long orderId) {
        return cancelOrderInternal(orderId, null);
    }

    @Override
    @DistributedLock(key = "'order:cancel:' + #orderId", waitTime = 5, leaseTime = 20, failMessage = "Acquire order cancel lock failed")
    @Transactional(rollbackFor = Exception.class)
    public Boolean cancelOrderWithReason(Long orderId, String cancelReason) {
        return cancelOrderInternal(orderId, cancelReason);
    }

    private Boolean cancelOrderInternal(Long orderId, String cancelReason) {
        Order order = requireOrder(orderId);
        Integer status = order.getStatus();
        if (!Objects.equals(status, 0) && !Objects.equals(status, 1)) {
            throw new OrderServiceException("Order status does not allow cancellation");
        }

        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<Order> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Order::getId, orderId)
                .eq(Order::getDeleted, 0)
                .eq(Order::getStatus, status)
                .set(Order::getStatus, 4)
                .set(Order::getCancelTime, now);
        if (StringUtils.hasText(cancelReason)) {
            updateWrapper.set(Order::getCancelReason, cancelReason);
        }

        if (update(updateWrapper)) {
            return true;
        }

        Order latest = this.baseMapper.selectById(orderId);
        if (latest != null && Objects.equals(latest.getStatus(), 4)) {
            return true;
        }
        throw new OrderServiceException("Cancel order failed due to status conflict");
    }

    @Override
    public Boolean createOrder(OrderCreateDTO orderCreateDTO, String currentUserId) {
        if (orderCreateDTO == null) {
            throw new OrderServiceException("orderCreateDTO is required");
        }
        if (currentUserId != null && !currentUserId.isBlank()) {
            validateCreateUser(orderCreateDTO.getUserId(), currentUserId);
        }
        OrderDTO created = createOrder(orderCreateDTO);
        return created != null && created.getId() != null;
    }

    @Override
    public Boolean payOrder(Long orderId, String currentUserId) {
        validateOrderUser(orderId, currentUserId);
        return payOrder(orderId);
    }

    @Override
    public Boolean shipOrder(Long orderId, String currentUserId) {
        return shipOrder(orderId);
    }

    @Override
    public Boolean completeOrder(Long orderId, String currentUserId) {
        validateOrderUser(orderId, currentUserId);
        return completeOrder(orderId);
    }

    @Override
    public Boolean cancelOrder(Long orderId, String currentUserId) {
        validateOrderUser(orderId, currentUserId);
        return cancelOrderInternal(orderId, null);
    }

    @Override
    @DistributedLock(key = "'order:delete:' + #id", waitTime = 3, leaseTime = 10, failMessage = "Acquire order delete lock failed")
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteOrder(Long id) {
        return this.baseMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderDTO createOrder(OrderDTO orderDTO) {
        if (orderDTO == null) {
            throw new OrderServiceException("orderDTO is required");
        }

        Order order = orderConverter.toEntity(orderDTO);
        initOrderForCreate(order);
        int rows = this.baseMapper.insert(order);
        if (rows <= 0) {
            throw new OrderServiceException.OrderCreateFailedException("Insert order failed");
        }
        OrderDTO created = orderConverter.toDTO(order);
        publishOrderCreatedEvent(order, buildProductQuantityMapFromOrderDTO(orderDTO));
        return created;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderDTO createOrder(OrderCreateDTO orderCreateDTO) {
        if (orderCreateDTO == null) {
            throw new OrderServiceException("orderCreateDTO is required");
        }
        if (orderCreateDTO.getUserId() == null) {
            throw new OrderServiceException("userId is required");
        }

        BigDecimal totalAmount = normalizeTotalAmount(orderCreateDTO);
        BigDecimal payAmount = normalizePayAmount(orderCreateDTO, totalAmount);

        Order order = new Order();
        order.setOrderNo(Order.generateOrderNo());
        order.setUserId(orderCreateDTO.getUserId());
        order.setTotalAmount(totalAmount);
        order.setPayAmount(payAmount);
        order.setStatus(0);
        order.setAddressId(orderCreateDTO.getAddressId());
        order.setRemark(orderCreateDTO.getRemark());
        order.setShopId(orderCreateDTO.getShopId());

        int rows = this.baseMapper.insert(order);
        if (rows <= 0) {
            throw new OrderServiceException.OrderCreateFailedException("Insert order failed");
        }
        publishOrderCreatedEvent(order, buildProductQuantityMapFromCreateDTO(orderCreateDTO));
        return orderConverter.toDTO(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByUserId(Long userId) {
        List<Order> orders = this.baseMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, userId)
                        .orderByDesc(Order::getCreatedAt)
        );
        return orderConverter.toDTOList(orders);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDTO getOrderByOrderNo(String orderNo) {
        Order order = this.baseMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        return order == null ? null : orderConverter.toDTO(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Boolean isOrderPaid(Long orderId) {
        Order order = this.baseMapper.selectById(orderId);
        if (order == null || order.getStatus() == null) {
            return false;
        }
        return order.getStatus() == 1 || order.getStatus() == 2 || order.getStatus() == 3;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderVO getOrderByOrderIdForFeign(Long orderId) {
        Order order = this.baseMapper.selectById(orderId);
        return order == null ? null : orderConverter.toVO(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrderForFeign(OrderDTO orderDTO) {
        OrderDTO created = createOrder(orderDTO);
        if (created == null || created.getId() == null) {
            throw new BusinessException("Create order failed");
        }
        Order persisted = this.baseMapper.selectById(created.getId());
        if (persisted == null) {
            throw EntityNotFoundException.order(created.getId());
        }
        return orderConverter.toVO(persisted);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateOrderStatusForFeign(Long orderId, Integer status) {
        if (status == null) {
            return false;
        }
        return switch (status) {
            case 1 -> payOrder(orderId);
            case 2 -> shipOrder(orderId);
            case 3 -> completeOrder(orderId);
            case 4 -> cancelOrder(orderId);
            default -> false;
        };
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean completeOrderForFeign(Long orderId) {
        return completeOrder(orderId);
    }

    @Override
    @DistributedLock(key = "'order:payment:callback:' + #orderId", waitTime = 5, leaseTime = 20, failMessage = "Acquire order payment callback lock failed")
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = "orderCache", key = "#orderId")
    public Boolean updateOrderStatusAfterPayment(Long orderId, Long paymentId, String transactionNo) {
        Order order = requireOrder(orderId);
        if (!Objects.equals(order.getStatus(), 0)) {
            return false;
        }

        order.setStatus(1);
        order.setPayTime(LocalDateTime.now());
        boolean updated = this.baseMapper.updateById(order) > 0;
        if (updated) {
            recordOrderMetric("success");
        }
        return updated;
    }

    @Override
    @DistributedLock(key = "'order:stock:freeze:failed:' + #orderId", waitTime = 5, leaseTime = 20, failMessage = "Acquire order stock freeze failed lock failed")
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = "orderCache", key = "#orderId")
    public Boolean cancelOrderDueToStockFreezeFailed(Long orderId, String reason) {
        Order order = requireOrder(orderId);
        if (!Objects.equals(order.getStatus(), 0)) {
            return false;
        }

        order.setStatus(4);
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason(reason);
        boolean updated = this.baseMapper.updateById(order) > 0;
        if (updated) {
            recordOrderMetric("failed");
        }
        return updated;
    }

    @Override
    @DistributedLock(key = "'order:batch:update:' + #orderIds.toString()", waitTime = 10, leaseTime = 30, failMessage = "Acquire order batch update lock failed")
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = "orderCache", allEntries = true)
    public Integer batchUpdateOrderStatus(List<Long> orderIds, Integer status) {
        if (orderIds == null || orderIds.isEmpty()) {
            throw new OrderServiceException("orderIds cannot be empty");
        }
        if (status == null) {
            throw new OrderServiceException("status cannot be null");
        }

        Set<Long> uniqueIds = new LinkedHashSet<>(orderIds);
        int success = 0;
        for (Long orderId : uniqueIds) {
            success += updateOrderStatusDirect(orderId, status) ? 1 : 0;
        }
        return success;
    }

    @Override
    @DistributedLock(key = "'order:batch:delete:' + #orderIds.toString()", waitTime = 10, leaseTime = 30, failMessage = "Acquire order batch delete lock failed")
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = "orderCache", allEntries = true)
    public Integer batchDeleteOrders(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            throw new OrderServiceException("orderIds cannot be empty");
        }

        Set<Long> uniqueIds = new LinkedHashSet<>(orderIds);
        return this.baseMapper.deleteBatchIds(uniqueIds);
    }

    private Order requireOrder(Long orderId) {
        Order order = this.baseMapper.selectById(orderId);
        if (order == null) {
            throw EntityNotFoundException.order(orderId);
        }
        return order;
    }

    private void initOrderForCreate(Order order) {
        if (order == null) {
            throw new OrderServiceException("order entity is required");
        }
        if (order.getOrderNo() == null || order.getOrderNo().isBlank()) {
            order.setOrderNo(Order.generateOrderNo());
        }
        if (order.getStatus() == null) {
            order.setStatus(0);
        }
        if (order.getTotalAmount() == null || order.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderServiceException("totalAmount must be greater than 0");
        }
        if (order.getPayAmount() == null) {
            order.setPayAmount(order.getTotalAmount());
        }
        if (order.getPayAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderServiceException("payAmount must be greater than 0");
        }
    }

    private BigDecimal normalizeTotalAmount(OrderCreateDTO dto) {
        if (dto.getTotalAmount() != null && dto.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
            return dto.getTotalAmount();
        }

        if (dto.getOrderItems() == null || dto.getOrderItems().isEmpty()) {
            throw new OrderServiceException("totalAmount is required when orderItems is empty");
        }

        BigDecimal total = dto.getOrderItems().stream()
                .map(item -> {
                    if (item.getPrice() == null || item.getQuantity() == null) {
                        throw new OrderServiceException("order item price and quantity are required");
                    }
                    return item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderServiceException("totalAmount must be greater than 0");
        }
        return total;
    }

    private BigDecimal normalizePayAmount(OrderCreateDTO dto, BigDecimal totalAmount) {
        if (dto.getPayAmount() != null && dto.getPayAmount().compareTo(BigDecimal.ZERO) > 0) {
            return dto.getPayAmount();
        }

        BigDecimal discount = dto.getDiscountAmount() == null ? BigDecimal.ZERO : dto.getDiscountAmount();
        BigDecimal shippingFee = dto.getShippingFee() == null ? BigDecimal.ZERO : dto.getShippingFee();
        BigDecimal payAmount = totalAmount.subtract(discount).add(shippingFee);

        if (payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderServiceException("payAmount must be greater than 0");
        }
        return payAmount;
    }

    private Boolean updateOrderStatusDirect(Long orderId, Integer status) {
        Order order = this.baseMapper.selectById(orderId);
        if (order == null) {
            return false;
        }

        order.setStatus(status);
        if (status == 1) {
            order.setPayTime(LocalDateTime.now());
        } else if (status == 2) {
            order.setShipTime(LocalDateTime.now());
        } else if (status == 3) {
            order.setCompleteTime(LocalDateTime.now());
        } else if (status == 4) {
            order.setCancelTime(LocalDateTime.now());
        }

        return this.baseMapper.updateById(order) > 0;
    }

    private void validateOrderUser(Long orderId, String currentUserId) {
        if (currentUserId == null || currentUserId.isBlank()) {
            throw new OrderServiceException("currentUserId is required");
        }

        Order order = requireOrder(orderId);
        validateCreateUser(order.getUserId(), currentUserId);
    }

    private void validateCreateUser(Long targetUserId, String currentUserId) {
        try {
            Long current = Long.parseLong(currentUserId);
            if (!Objects.equals(targetUserId, current)) {
                throw new OrderServiceException("Current user does not match target user");
            }
        } catch (NumberFormatException e) {
            throw new OrderServiceException("Invalid currentUserId format");
        }
    }

    private void publishOrderCreatedEvent(Order order, Map<Long, Integer> productQuantityMap) {
        Map<Long, Integer> safeMap = (productQuantityMap == null || productQuantityMap.isEmpty())
                ? Map.of()
                : productQuantityMap;

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("ORDER_CREATED")
                .timestamp(System.currentTimeMillis())
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .remark(order.getRemark())
                .productQuantityMap(safeMap)
                .build();

        boolean sent = orderMessageProducer.sendOrderCreatedEvent(event);
        if (!sent) {
            throw new OrderServiceException.OrderCreateFailedException("Send order-created event failed");
        }
    }

    private Map<Long, Integer> buildProductQuantityMapFromCreateDTO(OrderCreateDTO orderCreateDTO) {
        if (orderCreateDTO.getOrderItems() == null || orderCreateDTO.getOrderItems().isEmpty()) {
            return Map.of();
        }

        Map<Long, Integer> map = new HashMap<>();
        for (OrderCreateDTO.OrderItemDTO item : orderCreateDTO.getOrderItems()) {
            if (item == null || item.getProductId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                continue;
            }
            map.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }
        return map;
    }

    private Map<Long, Integer> buildProductQuantityMapFromOrderDTO(OrderDTO orderDTO) {
        if (orderDTO.getOrderItems() == null || orderDTO.getOrderItems().isEmpty()) {
            return Map.of();
        }

        Map<Long, Integer> map = new HashMap<>();
        for (OrderItemDTO item : orderDTO.getOrderItems()) {
            if (item == null || item.getProductId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                continue;
            }
            map.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }
        return map;
    }

    private void recordOrderMetric(String result) {
        meterRegistry.counter(
                "trade.order",
                "service", "order-service",
                "result", result
        ).increment();
    }
}
